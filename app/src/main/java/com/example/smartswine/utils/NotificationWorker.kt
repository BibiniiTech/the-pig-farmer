package com.example.smartswine.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.smartswine.MainActivity
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.bibiniitech.smartswine.R
import com.example.smartswine.model.TaskItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
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
            // Fetch user language
            val userDoc = db.collection("users").document(userId).get().await()
            val langCode = userDoc.getString("appLanguage") ?: "en"
            val locale = AppLanguage.entries.find { it.code == langCode }?.toLocale() ?: Locale.getDefault()

            // Fetch incomplete tasks
            Log.d("NotificationWorker", "Fetching tasks for user: $userId")
            val tasksSnapshot = db.collection("users").document(userId)
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
                val dateFromTask = DateUtils.parseTask(task.date, locale)
                if (dateFromTask != null) {
                    val taskDate = Calendar.getInstance().apply {
                        time = dateFromTask
                        set(Calendar.YEAR, today.get(Calendar.YEAR))
                        
                        // Smart year matching: Find if this month/day is closer to last year, this year, or next year
                        // This handles overdue tasks from previous months/years and upcoming ones at year-end.
                        val diffDays = (timeInMillis - today.timeInMillis) / (1000 * 60 * 60 * 24)
                        if (diffDays > 180) {
                            add(Calendar.YEAR, -1)
                        } else if (diffDays < -180) {
                            add(Calendar.YEAR, 1)
                        }
                    }

                    val diffInMillis = taskDate.timeInMillis - today.timeInMillis
                    val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)

                    val titleKey: String
                    val bodyKey: String
                    val showNotification: Boolean

                    when (diffInDays) {
                        2L -> {
                            titleKey = "notif_upcoming"
                            bodyKey = "notif_msg_in_2_days"
                            showNotification = true
                        }
                        1L -> {
                            titleKey = "notif_upcoming"
                            bodyKey = "notif_msg_tomorrow"
                            showNotification = true
                        }
                        0L -> {
                            titleKey = "notif_due_today"
                            bodyKey = "notif_msg_today"
                            showNotification = true
                        }
                        -1L -> {
                            titleKey = "notif_overdue"
                            bodyKey = "notif_msg_yesterday"
                            showNotification = true
                        }
                        -2L -> {
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
                        val message = Translator.getString(bodyKey, langCode, task.name)
                        notificationsToShow.add(Triple(title, message, task.id.hashCode()))
                    }
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
