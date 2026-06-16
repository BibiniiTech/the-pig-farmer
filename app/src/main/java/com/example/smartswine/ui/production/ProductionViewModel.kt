package com.example.smartswine.ui.production

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartswine.model.FinancialRecord
import com.example.smartswine.model.HealthRecord
import com.example.smartswine.model.Pig
import com.example.smartswine.model.TaskItem
import com.example.smartswine.utils.DateUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProductionViewModel : ViewModel() {
    private val db: FirebaseFirestore by lazy {
        com.example.smartswine.data.FirestoreManager.configure()
        FirebaseFirestore.getInstance()
    }
    private val auth = FirebaseAuth.getInstance()

    // Active Farm ID for multi-user support
    private var activeFarmId: String? = null

    fun setActiveFarmId(uid: String) {
        activeFarmId = uid
    }

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun logHealthActivity(
        pigIds: List<String>, 
        record: HealthRecord, 
        trackHeat: Boolean = false,
        checkPregnancy: Boolean = false,
        pregnancyConfirmed: Boolean = false,
        details: Map<String, Any> = emptyMap(),
    ) {
        val userId = activeFarmId ?: auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val batch = db.batch()
                val isFuture = DateUtils.isFutureDate(record.date)
                
                // For Point 4: Every activity (past or future) is added to history.
                // If future, it's ALSO added as a task.
                val taskRef = if (isFuture) db.collection("users").document(userId).collection("tasks").document() else null
                
                val pigTags = mutableListOf<String>()
                val hrIds = mutableListOf<String>()

                if (!isFuture) {
                    // Financial record logic moved to per-pig loop to avoid duplication

                    pigIds.forEach { pigId ->
                        var finalDescription = record.description
                        val pigRef = db.collection("users").document(userId).collection("pigs").document(pigId)
                        val archiveRef = db.collection("users").document(userId).collection("archived_pigs").document(pigId)
                        
                        val snapshot = run {
                            val activeSnap = pigRef.get().await()
                            if (activeSnap.exists()) activeSnap else archiveRef.get().await()
                        }
                        
                        val pigTag = snapshot?.getString("tagNumber") ?: pigId
                        pigTags.add(pigTag)

                        // --- ACTIVITY SPECIFIC METADATA UPDATES ---
                        @Suppress("UNCHECKED_CAST")
                        val pigOutcomes = (details["pigOutcomes"] as? Map<String, String>) ?: emptyMap()

                        if (record.type == "Breeding/Mating" && snapshot != null) {
                            val sowTag = details["sowTag"]?.toString() ?: ""
                            val boarTag = details["boarTag"]?.toString() ?: ""
                            val outcome = pigOutcomes[pigId] ?: "Successful"
                            finalDescription = "${record.description}\nMated: Sow $sowTag with Boar $boarTag\nOutcome: $outcome".trim()
                            
                            if (snapshot.getString("gender")?.equals("Female", ignoreCase = true) == true) {
                                batch.update(pigRef, "lastBreedingDate", record.date)
                                batch.update(pigRef, "lastBoarTag", boarTag)
                                batch.update(pigRef, "purpose", "Breeder")
                                
                                if (outcome == "Successful" || outcome == "Mating Successful") {
                                    if (checkPregnancy) {
                                        val tRef = db.collection("users").document(userId).collection("tasks").document()
                                        val taskDate = DateUtils.addDaysToDate(record.date, 21)
                                        batch.set(tRef, TaskItem(id = tRef.id, name = "Confirm Pregnancy: Pig $pigTag", date = taskDate, notes = "Scheduled 21 days after mating on ${record.date}", pigIds = listOf(pigId)))
                                    } else {
                                        batch.update(pigRef, "status", "Pregnant")
                                        val tRef = db.collection("users").document(userId).collection("tasks").document()
                                        val taskDate = DateUtils.addDaysToDate(record.date, 114)
                                        batch.set(tRef, TaskItem(id = tRef.id, name = "Farrowing: Pig $pigTag", date = taskDate, notes = "Scheduled 114 days after mating on ${record.date}", pigIds = listOf(pigId)))
                                    }
                                }
                            }
                        }

                        if (record.type == "Heat Detection" && snapshot != null && trackHeat) {
                            val outcome = pigOutcomes[pigId] ?: "Heat Detected"
                            finalDescription = "${record.description}\nOutcome: $outcome".trim()
                            if (outcome == "Heat Detected") {
                                val tRef = db.collection("users").document(userId).collection("tasks").document()
                                val taskDate = DateUtils.addDaysToDate(record.date, 21)
                                batch.set(tRef, TaskItem(id = tRef.id, name = "Heat Detection: Pig $pigTag", date = taskDate, notes = "Auto-created 21 days after heat detection on ${record.date}", pigIds = listOf(pigId)))
                            }
                        }

                        if (record.type == "Confirm Pregnancy" && snapshot != null) {
                            val outcome = pigOutcomes[pigId] ?: (if (pregnancyConfirmed) "Successful" else "Failed")
                            if (outcome == "Successful") {
                                batch.update(pigRef, "status", "Pregnant", "purpose", "Breeder")
                                val lastMating = snapshot.getString("lastBreedingDate") ?: record.date
                                val tRef = db.collection("users").document(userId).collection("tasks").document()
                                val taskDate = DateUtils.addDaysToDate(lastMating, 114)
                                batch.set(tRef, TaskItem(id = tRef.id, name = "Farrowing: Pig $pigTag", date = taskDate, notes = "Scheduled 114 days after mating on $lastMating", pigIds = listOf(pigId)))
                                finalDescription = "${record.description}\nPregnancy Confirmed. Scheduled farrowing for $taskDate".trim()
                            } else {
                                finalDescription = "${record.description}\nPregnancy Check Failed.".trim()
                            }
                        }

                        if (record.type == "Farrowing" && snapshot != null) {
                            @Suppress("UNCHECKED_CAST")
                            val pigNumMales = (details["pigNumMales"] as? Map<String, String>) ?: emptyMap()
                            @Suppress("UNCHECKED_CAST")
                            val pigNumFemales = (details["pigNumFemales"] as? Map<String, String>) ?: emptyMap()
                            @Suppress("UNCHECKED_CAST")
                            val pigMaleTags = (details["pigMaleTags"] as? Map<String, String>) ?: emptyMap()
                            @Suppress("UNCHECKED_CAST")
                            val pigFemaleTags = (details["pigFemaleTags"] as? Map<String, String>) ?: emptyMap()

                            val numMales = pigNumMales[pigId]?.toIntOrNull() ?: details["numMales"] as? Int ?: 0
                            val numFemales = pigNumFemales[pigId]?.toIntOrNull() ?: details["numFemales"] as? Int ?: 0
                            val maleTagsStr = pigMaleTags[pigId] ?: details["maleTags"]?.toString() ?: ""
                            val femaleTagsStr = pigFemaleTags[pigId] ?: details["femaleTags"]?.toString() ?: ""
                            
                            val maleTags = maleTagsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            val femaleTags = femaleTagsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            finalDescription = "${record.description}\nFarrowed: $numMales Males, $numFemales Females".trim()

                            val breed = snapshot.getString("breed") ?: ""
                            val location = snapshot.getString("location") ?: ""
                            val sowTagSnapshot = snapshot.getString("tagNumber") ?: ""
                            val boarTag = snapshot.getString("lastBoarTag") ?: ""

                            val finalMaleTags = maleTags.toMutableList()
                            for (i in finalMaleTags.size until numMales) {
                                finalMaleTags.add("${sowTagSnapshot}-M${i + 1}")
                            }

                            val finalFemaleTags = femaleTags.toMutableList()
                            for (i in finalFemaleTags.size until numFemales) {
                                finalFemaleTags.add("${sowTagSnapshot}-F${i + 1}")
                            }

                            finalMaleTags.forEach { tag ->
                                val newPigRef = db.collection("users").document(userId).collection("pigs").document()
                                batch.set(newPigRef, Pig(id = newPigRef.id, tagNumber = tag, gender = "Male", breed = breed, birthDate = record.date, status = "Piglet", sowTag = sowTagSnapshot, boarTag = boarTag, location = location))
                            }
                            finalFemaleTags.forEach { tag ->
                                val newPigRef = db.collection("users").document(userId).collection("pigs").document()
                                batch.set(newPigRef, Pig(id = newPigRef.id, tagNumber = tag, gender = "Female", breed = breed, birthDate = record.date, status = "Piglet", sowTag = sowTagSnapshot, boarTag = boarTag, location = location))
                            }
                            batch.update(pigRef, "status", "Lactating", "hasFarrowed", true, "weaned", false, "purpose", "Breeder")
                        }

                        if (record.type == "Weaning" && snapshot != null) {
                            @Suppress("UNCHECKED_CAST")
                            val pigLocations = details["pigLocations"] as? Map<String, String> ?: emptyMap()
                            val newLocation = pigLocations[pigId] ?: snapshot.getString("location") ?: ""
                            
                            // Weaned pigs (piglets) become "Starter"
                            batch.update(pigRef, "status", "Starter", "weaned", true)
                            if (newLocation.isNotEmpty()) batch.update(pigRef, "location", newLocation)
                            
                            // Mothers (Sow/Lactating) transition back to "Sow" status (ready for next cycle)
                            if (snapshot.getString("status") == "Lactating" || snapshot.getString("status") == "Nursing" || snapshot.getString("status") == "Sow") {
                                batch.update(pigRef, "status", "Sow")
                            } else {
                                val sowTagVal = snapshot.getString("sowTag") ?: ""
                                if (sowTagVal.isNotEmpty()) {
                                    viewModelScope.launch {
                                        val otherOffspring = db.collection("users").document(userId).collection("pigs").whereEqualTo("sowTag", sowTagVal).whereEqualTo("weaned", false).get().await()
                                        if (otherOffspring.documents.all { it.id in pigIds }) {
                                            val sowSnapshot = db.collection("users").document(userId).collection("pigs").whereEqualTo("tagNumber", sowTagVal).limit(1).get().await()
                                            if (!sowSnapshot.isEmpty) db.collection("users").document(userId).collection("pigs").document(sowSnapshot.documents[0].id).update("status", "Sow")
                                        }
                                    }
                                }
                            }
                            finalDescription = "${record.description}\nWeaned and moved to location: $newLocation".trim()
                        }

                        if (record.type == "Castration" && snapshot != null && snapshot.getString("gender")?.equals("Male", ignoreCase = true) == true) {
                            batch.update(pigRef, "castrated", true, "castrationDate", record.date)
                        }

                        if (record.type == "Teeth Clipping" && snapshot != null) batch.update(pigRef, "teethClipped", true)
                        if (record.type == "Tail Docking" && snapshot != null) batch.update(pigRef, "tailDocked", true)

                        if (record.type == "Iron Injection") {
                            val currentInjections = (snapshot?.getLong("ironInjections") ?: 0L).toInt()
                            batch.update(pigRef, "ironInjections", currentInjections + 1)
                            
                            @Suppress("UNCHECKED_CAST")
                            val pigScheduleSecondIron = (details["pigScheduleSecondIron"] as? Map<String, Boolean>) ?: emptyMap()
                            val shouldSchedule = pigScheduleSecondIron[pigId] ?: (details["scheduleSecondIron"] == true)
                            
                            if (shouldSchedule && currentInjections == 0) {
                                val tRef = db.collection("users").document(userId).collection("tasks").document()
                                batch.set(tRef, TaskItem(id = tRef.id, name = "2nd Iron Injection: Pig $pigTag", date = DateUtils.addDaysToDate(record.date, 7), notes = "Scheduled 7 days after 1st injection on ${record.date}", pigIds = listOf(pigId)))
                            }
                        }

                        if (record.type == "Weight Check") {
                            @Suppress("UNCHECKED_CAST")
                            val pigWeights = (details["pigWeights"] as? Map<String, String>) ?: emptyMap()
                            val newWeight = pigWeights[pigId]?.toDoubleOrNull() ?: 0.0
                            if (newWeight > 0) {
                                batch.update(pigRef, "weight", newWeight)
                                finalDescription = "${record.description}\nWeight updated to ${newWeight}kg".trim()
                            }
                        }

                        if (record.type == "Culling") {
                            val reason = details["cullingReason"]?.toString() ?: "Unknown"
                            @Suppress("UNCHECKED_CAST")
                            val pigSalePrices = (details["pigSalePrices"] as? Map<String, String>) ?: emptyMap()
                            val individualSalePrice = pigSalePrices[pigId]?.toDoubleOrNull() ?: 0.0
                            
                            finalDescription = "${record.description}\nReason: $reason".trim()
                            snapshot?.toObject(Pig::class.java)?.let { batch.set(archiveRef, it.copy(status = "Culled ($reason)")) }
                            batch.delete(pigRef)
                            if (reason == "Sold" && individualSalePrice > 0) {
                                val fRef = db.collection("users").document(userId).collection("financials").document()
                                batch.set(fRef, FinancialRecord(id = fRef.id, type = "Income", category = "Pig Sale", amount = individualSalePrice, date = record.date, description = "Sale of Pig ${snapshot?.getString("tagNumber") ?: pigId}", pigId = pigId))
                                finalDescription += "\nSold for: Ksh $individualSalePrice"
                            }
                            cleanupTasksForPig(pigId, userId)
                        }

                        // --- HISTORY RECORDING ---
                        val recordRef = pigRef.collection("health_records").document()
                        hrIds.add(recordRef.id)
                        val finalType = if (record.type == "Custom") details["customActivityName"]?.toString() ?: "Custom" else record.type
                        val finalHealthRecord = record.copy(id = recordRef.id, type = finalType, description = finalDescription)
                        
                        if (record.type == "Culling") {
                            val archRecRef = db.collection("users").document(userId).collection("archived_pigs").document(pigId).collection("health_records").document()
                            batch.set(archRecRef, finalHealthRecord.copy(id = archRecRef.id))
                        } else {
                            batch.set(recordRef, finalHealthRecord)
                        }
                    }

                    // Handle generalized Next Scheduled Activity
                    val nextDate = details["nextScheduledDate"]?.toString() ?: ""
                    if (nextDate.isNotEmpty()) {
                        val tRef = db.collection("users").document(userId).collection("tasks").document()
                        val activityPrefix = if (record.type == "Custom") details["customActivityName"]?.toString() ?: "Custom" else record.type
                        val taskName = when {
                            pigTags.size == 1 -> "$activityPrefix: Pig ${pigTags[0]}"
                            pigTags.size > 1 -> "$activityPrefix: Pigs ${pigTags.joinToString(", ")}"
                            else -> "$activityPrefix: General"
                        }
                        batch.set(tRef, TaskItem(
                            id = tRef.id,
                            name = taskName,
                            date = nextDate,
                            notes = "Auto-scheduled from activity on ${record.date}",
                            pigIds = pigIds
                        ))
                    }
                } else {
                    // Logic for FUTURE activities (Task ONLY)
                    pigIds.forEach { pigId ->
                        val pigDoc = db.collection("users").document(userId).collection("pigs").document(pigId).get().await()
                        pigTags.add(pigDoc.getString("tagNumber") ?: pigId)
                    }

                    val taskNamePrefix = if (record.type == "Custom") details["customActivityName"]?.toString() ?: "Custom" else record.type
                    val taskName = when {
                        pigTags.size == 1 -> "$taskNamePrefix: Pig ${pigTags[0]}"
                        pigTags.size > 1 -> "$taskNamePrefix: Pigs ${pigTags.joinToString(", ")}"
                        else -> "$taskNamePrefix: General"
                    }
                    batch.set(taskRef!!, TaskItem(
                        id = taskRef.id, 
                        name = taskName, 
                        date = DateUtils.convertToTaskDate(record.date), 
                        notes = record.description, 
                        pigIds = pigIds
                    ))
                }
                
                batch.commit().await()
                if (!isFuture) autoCompleteUpcomingTasks(pigIds, record.type, userId)
            } catch (e: Exception) {
                _error.value = e.message
                Log.e("ProductionViewModel", "LogActivity error", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun cleanupTasksForPig(pigId: String, userId: String) {
        viewModelScope.launch {
            try {
                val tasks = db.collection("users").document(userId).collection("tasks").whereArrayContains("pigIds", pigId).get().await()
                val pigDoc = db.collection("users").document(userId).collection("pigs").document(pigId).get().await()
                val pigTag = if (pigDoc.exists()) pigDoc.getString("tagNumber") ?: "" else ""
                
                val batch = db.batch()
                tasks.forEach { doc ->
                    val task = doc.toObject(TaskItem::class.java)
                    if (task.pigIds.size <= 1) {
                        batch.delete(doc.reference)
                    } else {
                        val newPigIds = task.pigIds.filter { it != pigId }
                        val newName = if (pigTag.isNotEmpty()) task.name.replace(pigTag, "").replace(", ,", ",").trim(',', ' ') else task.name
                        batch.update(doc.reference, "pigIds", newPigIds, "name", newName)
                    }
                }
                batch.commit().await()
            } catch (e: Exception) {
                Log.e("ProductionViewModel", "Cleanup error", e)
            }
        }
    }

    private suspend fun autoCompleteUpcomingTasks(pigIds: List<String>, activityType: String, userId: String) {
        try {
            val tasksSnapshot = db.collection("users").document(userId).collection("tasks").whereEqualTo("completed", false).get().await()
            val tasks = tasksSnapshot.documents.mapNotNull { it.toObject(TaskItem::class.java)?.copy(id = it.id) }
            val pigTags = pigIds.mapNotNull { id ->
                val pigDoc = db.collection("users").document(userId).collection("pigs").document(id).get().await()
                if (pigDoc.exists()) pigDoc.getString("tagNumber") else db.collection("users").document(userId).collection("archived_pigs").document(id).get().await().getString("tagNumber")
            }
            val batch = db.batch()
            var hasChanges = false
            tasks.forEach { task ->
                val taskActivity = task.name.substringBefore(":").trim()
                val isActivityMatch = when (activityType) {
                    "Breeding/Mating" -> taskActivity.contains("Breeding", ignoreCase = true) || taskActivity.contains("Mating", ignoreCase = true)
                    "Confirm Pregnancy" -> taskActivity.contains("Pregnancy", ignoreCase = true)
                    "Farrowing" -> taskActivity.contains("Farrowing", ignoreCase = true)
                    "Weaning" -> taskActivity.contains("Weaning", ignoreCase = true)
                    "Iron Injection" -> taskActivity.contains("Iron", ignoreCase = true)
                    "Heat Detection" -> taskActivity.contains("Heat", ignoreCase = true)
                    "Weight Check" -> taskActivity.contains("Weight", ignoreCase = true)
                    "Castration" -> taskActivity.contains("Castration", ignoreCase = true)
                    "Teeth Clipping" -> taskActivity.contains("Teeth", ignoreCase = true)
                    "Tail Docking" -> taskActivity.contains("Tail", ignoreCase = true)
                    "Deworming" -> taskActivity.contains("Deworming", ignoreCase = true)
                    "Vaccination" -> taskActivity.contains("Vaccination", ignoreCase = true)
                    "Medication" -> taskActivity.contains("Medication", ignoreCase = true)
                    "Culling" -> taskActivity.contains("Culling", ignoreCase = true)
                    else -> taskActivity.equals(activityType, ignoreCase = true)
                }
                if (isActivityMatch) {
                    val taskPigPart = task.name.substringAfter(":", "").trim()
                    val tagsInTask = taskPigPart.replace("Pig ", "", ignoreCase = true).replace("Pigs ", "", ignoreCase = true).split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    if (tagsInTask.isEmpty() && taskPigPart.isEmpty()) {
                        batch.update(db.collection("users").document(userId).collection("tasks").document(task.id), "completed", true)
                        hasChanges = true
                    } else {
                        val matchingTags = tagsInTask.filter { it in pigTags }
                        if (matchingTags.isNotEmpty()) {
                            val tRef = db.collection("users").document(userId).collection("tasks").document(task.id)
                            if (matchingTags.size == tagsInTask.size) batch.update(tRef, "completed", true)
                            else {
                                val remainingTags = tagsInTask.filter { it !in matchingTags }
                                val newName = if (remainingTags.size == 1) "$taskActivity: Pig ${remainingTags[0]}" else "$taskActivity: Pigs ${remainingTags.joinToString(", ")}"
                                batch.update(tRef, "name", newName, "pigIds", task.pigIds.filter { it !in pigIds })
                            }
                            hasChanges = true
                        }
                    }
                }
            }
            if (hasChanges) batch.commit().await()
        } catch (e: Exception) { Log.e("ProductionViewModel", "Auto-complete error", e) }
    }
}
