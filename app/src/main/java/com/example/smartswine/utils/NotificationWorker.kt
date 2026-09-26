package com.example.smartswine.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.bibiniitech.smartswine.R
import com.example.smartswine.MainActivity
import com.example.smartswine.model.TaskItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.*
import java.util.concurrent.TimeUnit

class NotificationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d("NotificationWorker", "Worker started")
        // Ensure Firestore is configured before getting an instance
        com.example.smartswine.data.FirestoreManager.configure()

        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: run {
            Log.d("NotificationWorker", "No user logged in")
            return Result.success()
        }
        val db = FirebaseFirestore.getInstance()

        try {
            // 1. Resolve Active Farm ID (Owner vs Staff Member)
            var targetFarmUid = userId
            val userDoc = db.collection("users").document(userId).get().await()
            val langCode = if (userDoc.exists()) {
                userDoc.getString("appLanguage") ?: "en"
            } else {
                val email = auth.currentUser?.email?.trim()?.lowercase()
                if (email != null) {
                    val registryDoc = db.collection("staff_registry").document(email).get().await()
                    if (registryDoc.exists()) {
                        targetFarmUid = registryDoc.getString("managerUid")
                            ?: registryDoc.getString("ownerUid") 
                            ?: registryDoc.getString("farmUid") 
                            ?: userId
                    }
                }
                "en"
            }
            val locale = AppLanguage.entries.find { it.code == langCode }?.toLocale() ?: Locale.getDefault()

            // 2. Fetch incomplete tasks for the farm
            Log.d("NotificationWorker", "Fetching tasks for farm: $targetFarmUid")
            val tasksSnapshot = db.collection("users").document(targetFarmUid)
                .collection("tasks")
                .whereEqualTo("completed", false)
                .get()
                .await()

            val tasks = tasksSnapshot.documents.mapNotNull { doc ->
                doc.toObject(TaskItem::class.java)?.copy(id = doc.id)
            }
            Log.d("NotificationWorker", "Found ${tasks.size} incomplete tasks")

            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val notificationsToShow = mutableListOf<Triple<String, String, Int>>()

            tasks.forEach { task ->
                val dateFromTask = DateUtils.parseAnyDate(task.date, locale)
                
                if (dateFromTask != null) {
                    val taskDate = Calendar.getInstance().apply {
                        time = dateFromTask
                        if (get(Calendar.YEAR) == 1970) {
                            set(Calendar.YEAR, today.get(Calendar.YEAR))
                            val diffDays = (timeInMillis - today.timeInMillis) / (1000 * 60 * 60 * 24)
                            if (diffDays > 180) {
                                add(Calendar.YEAR, -1)
                            } else if (diffDays < -180) {
                                add(Calendar.YEAR, 1)
                            }
                        }
                    }

                    val diffInMillis = taskDate.timeInMillis - today.timeInMillis
                    val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)

                    val isWithdrawalTask = task.name.contains("Withdrawal", ignoreCase = true)
                    val titleKey: String
                    val bodyKey: String
                    val showNotification: Boolean

                    when {
                        !isWithdrawalTask && diffInDays == 2L -> {
                            titleKey = "notif_upcoming"
                            bodyKey = "notif_msg_in_2_days"
                            showNotification = true
                        }
                        !isWithdrawalTask && diffInDays == 1L -> {
                            titleKey = "notif_upcoming"
                            bodyKey = "notif_msg_tomorrow"
                            showNotification = true
                        }
                        diffInDays == 0L -> {
                            titleKey = "notif_due_today"
                            bodyKey = "notif_msg_today"
                            showNotification = true
                        }
                        diffInDays == -1L -> {
                            titleKey = "notif_overdue"
                            bodyKey = "notif_msg_yesterday"
                            showNotification = true
                        }
                        diffInDays < -1L -> {
                            titleKey = "notif_overdue"
                            bodyKey = "notif_msg_overdue"
                            showNotification = true
                        }
                        else -> {
                            titleKey = ""
                            bodyKey = ""
                            showNotification = false
                        }
                    }

                    if (showNotification) {
                        val title = Translator.getString(titleKey, langCode)
                        val message = Translator.getString(bodyKey, langCode, getLocalizedTaskName(task.name, langCode))
                        notificationsToShow.add(Triple(title, message, task.id.hashCode()))
                    }
                }
            }

            // 3. Fetch pigs for weight update reminders
            val pigsSnapshot = db.collection("users").document(targetFarmUid)
                .collection("pigs")
                .get()
                .await()
            
            pigsSnapshot.documents.forEach { doc ->
                val lastWeightDate = doc.getString("lastWeightDate") ?: ""
                val weight = doc.getDouble("weight") ?: 0.0
                val birthDate = doc.getString("birthDate") ?: ""
                val lastWeight = if (lastWeightDate.isNotBlank()) {
                    lastWeightDate
                } else if (weight <= 0.0) {
                    birthDate
                } else {
                    ""
                }
                val tag = doc.getString("tagNumber") ?: "Unknown"
                if (lastWeight.isNotEmpty()) {
                    val lastDate = DateUtils.parseSwineDate(lastWeight)
                        ?: DateUtils.parseInternal(lastWeight)
                        ?: DateUtils.parseProduction(lastWeight)
                    if (lastDate != null) {
                        val diff = today.timeInMillis - lastDate.time
                        val days = TimeUnit.MILLISECONDS.toDays(diff)
                        if (days >= 30) {
                            val title = Translator.getString("weight_update_title", langCode)
                            val message = Translator.getString("weight_update_reminder", langCode, tag)
                            notificationsToShow.add(Triple(title, message, doc.id.hashCode() + 100000))
                        }
                    }
                }
            }

            // 4. Fetch feed inventory for low stock alerts
            val feedSnapshot = db.collection("users").document(targetFarmUid)
                .collection("feed_inventory")
                .get()
                .await()
            
            feedSnapshot.documents.forEach { doc ->
                val qty = doc.getDouble("quantity") ?: 0.0
                val threshold = doc.getDouble("minThreshold") ?: 0.0
                val name = doc.getString("name") ?: "Feed"
                val unit = doc.getString("unit") ?: "bags"
                if (qty <= threshold && threshold > 0) {
                    val title = Translator.getString("low_stock_title", langCode)
                    val message = Translator.getString("low_stock_reminder", langCode, name, "$qty $unit")
                    notificationsToShow.add(Triple(title, message, doc.id.hashCode() + 200000))
                }
            }

            if (notificationsToShow.isNotEmpty()) {
                if (notificationsToShow.size == 1) {
                    val (title, message, id) = notificationsToShow[0]
                    sendNotification(title, message, id)
                } else {
                    // Send individual notifications for the group
                    notificationsToShow.forEach { (title, message, id) ->
                        sendNotification(title, message, id, groupKey = GROUP_KEY)
                    }
                    // Send summary notification
                    val summaryTitle = Translator.getString("herd_activities", langCode)
                    val summaryMessage = Translator.getString("notif_summary", langCode, notificationsToShow.size)
                    sendNotification(summaryTitle, summaryMessage, SUMMARY_ID, groupKey = GROUP_KEY, isSummary = true)
                }
            }

            Log.d("NotificationWorker", "Worker finished successfully")
            return Result.success()
        } catch (e: Exception) {
            Log.e("NotificationWorker", "Error in worker", e)
            return Result.retry()
        }
    }

    private fun getLocalizedTaskName(name: String, langCode: String): String {
        val parts = name.split(": ", limit = 2)
        val activityPart = parts[0]
        val pigPart = parts.getOrNull(1)

        val localizedActivity = when {
            activityPart.contains("Move to Farrowing Crate", ignoreCase = true) || activityPart.contains("Farrowing Pen Move", ignoreCase = true) -> Translator.getString("farrowing_pen_move", langCode)
            activityPart.contains("Check Return-to-Heat", ignoreCase = true) || activityPart.contains("Re-mate", ignoreCase = true) || activityPart.contains("Heat Check", ignoreCase = true) || activityPart.contains("Heat Detection", ignoreCase = true) || activityPart.contains("Estrus", ignoreCase = true) -> Translator.getString("heat_detection", langCode)
            activityPart.contains("Breeding", ignoreCase = true) || activityPart.contains("Mating", ignoreCase = true) -> Translator.getString("breeding_mating", langCode)
            activityPart.contains("Confirm Pregnancy", ignoreCase = true) || activityPart.contains("Pregnancy Check", ignoreCase = true) -> Translator.getString("pregnancy_check", langCode)
            activityPart.contains("Farrowing", ignoreCase = true) -> Translator.getString("farrowing", langCode)
            activityPart.contains("Weaning", ignoreCase = true) -> Translator.getString("weaning", langCode)
            activityPart.contains("Castration", ignoreCase = true) -> Translator.getString("castration", langCode)
            activityPart.contains("Teeth Clipping", ignoreCase = true) -> Translator.getString("teeth_clipping", langCode)
            activityPart.contains("Tail Docking", ignoreCase = true) -> Translator.getString("tail_docking", langCode)
            activityPart.contains("Deworming", ignoreCase = true) -> Translator.getString("deworming", langCode)
            activityPart.contains("Iron Injection", ignoreCase = true) || activityPart.contains("Iron", ignoreCase = true) -> Translator.getString("iron_injection", langCode)
            activityPart.contains("Vaccination", ignoreCase = true) -> Translator.getString("vaccination", langCode)
            activityPart.contains("Medication", ignoreCase = true) -> Translator.getString("medication", langCode)
            activityPart.contains("Weight Check", ignoreCase = true) -> Translator.getString("weight_check", langCode)
            activityPart.contains("Culling", ignoreCase = true) -> Translator.getString("culling", langCode)
            activityPart.contains("Feed", ignoreCase = true) -> Translator.getString("feed_pigs", langCode)
            else -> activityPart
        }

        return if (pigPart != null) {
            val pigLabel = Translator.getString("pig", langCode)
            val pigsLabel = Translator.getString("pigs", langCode)
            val cleanPigPart = when {
                pigPart.startsWith("Pigs ", ignoreCase = true) -> "$pigsLabel ${pigPart.substring(5)}"
                pigPart.startsWith("Pig ", ignoreCase = true) -> "$pigLabel ${pigPart.substring(4)}"
                else -> pigPart
            }
            "$localizedActivity: $cleanPigPart"
        } else {
            localizedActivity
        }
    }

    private fun sendNotification(title: String, message: String, id: Int, groupKey: String? = null, isSummary: Boolean = false) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "farm_activities_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Farm Activities", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        // Check permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_pig_snout)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (groupKey != null) {
            builder.setGroup(groupKey)
            if (isSummary) {
                builder.setGroupSummary(true)
            }
        }

        notificationManager.notify(id, builder.build())
    }

    companion object {
        private const val GROUP_KEY = "com.example.smartswine.TASK_REMINDERS"
        private const val SUMMARY_ID = 9999

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .build()

            // Request for 7am
            val request7am = createPeriodicWorkRequest(7, constraints)
            // Request for 7pm
            val request7pm = createPeriodicWorkRequest(19, constraints)

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "FarmNotification_7AM",
                ExistingPeriodicWorkPolicy.UPDATE,
                request7am
            )
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "FarmNotification_7PM",
                ExistingPeriodicWorkPolicy.UPDATE,
                request7pm
            )
        }

        private fun createPeriodicWorkRequest(hour: Int, constraints: Constraints): PeriodicWorkRequest {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                if (before(now)) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            val delay = target.timeInMillis - now.timeInMillis

            return PeriodicWorkRequestBuilder<NotificationWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .build()
        }
        
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork("FarmNotification_7AM")
            WorkManager.getInstance(context).cancelUniqueWork("FarmNotification_7PM")
        }
    }
}
