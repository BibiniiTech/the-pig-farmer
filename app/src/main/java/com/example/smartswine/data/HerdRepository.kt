package com.example.smartswine.data

import com.example.smartswine.model.*
import com.example.smartswine.utils.DateUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.util.*

class HerdRepository(private val db: FirebaseFirestore) {

    fun getPigs(userId: String): Flow<List<Pig>> = callbackFlow {
        val listener = db.collection("users").document(userId).collection("pigs")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.w("HerdRepository", "Error listening to pigs: ${error.message}")
                    close()
                    return@addSnapshotListener
                }
                val pigs = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Pig::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(pigs)
            }
        awaitClose { listener.remove() }
    }

    fun getArchivedPigs(userId: String): Flow<List<Pig>> = callbackFlow {
        val listener = db.collection("users").document(userId).collection("archived_pigs")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.w("HerdRepository", "Error listening to archived pigs: ${error.message}")
                    close()
                    return@addSnapshotListener
                }
                val pigs = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Pig::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(pigs)
            }
        awaitClose { listener.remove() }
    }

    fun getAllPigs(userId: String): Flow<List<Pig>> = kotlinx.coroutines.flow.combine(
        getPigs(userId),
        getArchivedPigs(userId)
    ) { active, archived ->
        active + archived
    }

    fun getPig(userId: String, pigId: String): Flow<Pig?> = callbackFlow {
        var archiveListener: com.google.firebase.firestore.ListenerRegistration? = null
        val activeListener = db.collection("users").document(userId).collection("pigs").document(pigId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.w("HerdRepository", "Error listening to pig $pigId: ${error.message}")
                    close()
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val pig = snapshot.toObject(Pig::class.java)?.copy(id = snapshot.id)
                    trySend(pig)
                } else {
                    // Fallback to archived_pigs if not found in active herd
                    archiveListener?.remove()
                    archiveListener = db.collection("users").document(userId).collection("archived_pigs").document(pigId)
                        .addSnapshotListener { archSnapshot, archError ->
                            if (archError == null && archSnapshot != null && archSnapshot.exists()) {
                                val archPig = archSnapshot.toObject(Pig::class.java)?.copy(id = archSnapshot.id)
                                trySend(archPig)
                            } else {
                                trySend(null)
                            }
                        }
                }
            }
        awaitClose {
            activeListener.remove()
            archiveListener?.remove()
        }
    }

    fun getHealthRecords(userId: String, pigId: String): Flow<List<HealthRecord>> = callbackFlow {
        var archiveListener: com.google.firebase.firestore.ListenerRegistration? = null
        val activeListener = db.collection("users").document(userId).collection("pigs").document(pigId)
            .collection("health_records")
            .orderBy("date", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.w("HerdRepository", "Error listening to health records: ${error.message}")
                    close()
                    return@addSnapshotListener
                }
                val records = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(HealthRecord::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                
                if (records.isNotEmpty()) {
                    trySend(records)
                } else {
                    // Fallback to archived_pigs subcollection
                    archiveListener?.remove()
                    archiveListener = db.collection("users").document(userId).collection("archived_pigs").document(pigId)
                        .collection("health_records")
                        .orderBy("date", Query.Direction.ASCENDING)
                        .addSnapshotListener { archSnap, archErr ->
                            if (archErr == null && archSnap != null && !archSnap.isEmpty) {
                                val archRecords = archSnap.documents.mapNotNull { doc ->
                                    doc.toObject(HealthRecord::class.java)?.copy(id = doc.id)
                                }
                                trySend(archRecords)
                            } else {
                                trySend(records)
                            }
                        }
                }
            }
        awaitClose {
            activeListener.remove()
            archiveListener?.remove()
        }
    }

    suspend fun addPig(userId: String, pig: Pig) {
        db.collection("users").document(userId).collection("pigs").add(pig).await()
    }

    suspend fun updatePig(userId: String, pig: Pig) {
        db.collection("users").document(userId).collection("pigs").document(pig.id).set(pig, com.google.firebase.firestore.SetOptions.merge()).await()
    }

    suspend fun deletePig(userId: String, pigId: String) {
        db.collection("users").document(userId).collection("pigs").document(pigId).delete().await()
    }

    suspend fun archivePig(userId: String, pigId: String, archivedPig: Pig) {
        val batch = db.batch()
        val pigRef = db.collection("users").document(userId).collection("pigs").document(pigId)
        val archiveRef = db.collection("users").document(userId).collection("archived_pigs").document(pigId)
        batch.set(archiveRef, archivedPig)
        batch.delete(pigRef)
        batch.commit().await()
    }

    suspend fun addHealthRecordWithLogic(
        userId: String,
        pigId: String,
        record: HealthRecord,
        trackHeat: Boolean = false,
        checkPregnancy: Boolean = false,
        pregnancyConfirmed: Boolean = false,
        details: Map<String, Any> = emptyMap()
    ) {
        val pigRef = db.collection("users").document(userId).collection("pigs").document(pigId)
        val pigDoc = pigRef.get().await()
        val pigTag = pigDoc.getString("tagNumber") ?: pigId
        
        val isFuture = DateUtils.isFutureDate(record.date)
        
        if (isFuture) {
            val taskRef = db.collection("users").document(userId).collection("tasks").document()
            val task = TaskItem(
                id = taskRef.id,
                name = "${record.type}: Pig $pigTag",
                date = DateUtils.convertToTaskDate(record.date),
                notes = record.description,
                pigIds = listOf(pigId)
            )
            db.collection("users").document(userId).collection("tasks").document(taskRef.id).set(task).await()
            val updatedRecord = record.copy(taskId = taskRef.id)
            pigRef.collection("health_records").add(updatedRecord).await()
        } else {
            val batch = db.batch()
            val isCulling = record.type == "Culling"
            val recordRef = if (isCulling) {
                db.collection("users").document(userId).collection("archived_pigs").document(pigId).collection("health_records").document()
            } else {
                pigRef.collection("health_records").document()
            }
            val updatedRecord = record.copy(id = recordRef.id)
            
            handleSpecializedActivityLogic(batch, userId, pigId, pigTag, updatedRecord, trackHeat, checkPregnancy, pregnancyConfirmed, details)
            
            batch.set(recordRef, updatedRecord)
            batch.commit().await()
        }
    }

    private suspend fun handleSpecializedActivityLogic(
        batch: WriteBatch,
        userId: String,
        pigId: String,
        pigTag: String,
        record: HealthRecord,
        trackHeat: Boolean,
        checkPregnancy: Boolean,
        pregnancyConfirmed: Boolean,
        details: Map<String, Any>
    ) {
        val pigRef = db.collection("users").document(userId).collection("pigs").document(pigId)
        
        when (record.type) {
            "Heat Detection" -> {
                if (trackHeat) {
                    val tRef = db.collection("users").document(userId).collection("tasks").document()
                    val taskDate = DateUtils.addDaysToDate(record.date, 21)
                    batch.set(tRef, TaskItem(
                        id = tRef.id,
                        name = "Heat Detection: Pig $pigTag",
                        date = taskDate,
                        notes = "Auto-created 21 days after heat detection on ${record.date}",
                        pigIds = listOf(pigId)
                    ))
                }
            }
            "Breeding/Mating" -> {
                batch.update(pigRef, "lastBreedingDate", record.date)
                val boarTag = details["boarTag"]?.toString() ?: ""
                if (boarTag.isNotEmpty()) batch.update(pigRef, "lastBoarTag", boarTag)
                batch.update(pigRef, "purpose", "Breeder")

                val day110Date = DateUtils.addDaysToDate(record.date, 110)
                val day114Date = DateUtils.addDaysToDate(record.date, 114)
                batch.update(pigRef, "expectedFarrowingDate", day114Date, "farrowingPenMoveDate", day110Date)

                // 1. Day 21 Return-to-Heat surveillance window (18-24 days)
                val tHeatRef = db.collection("users").document(userId).collection("tasks").document()
                val heatDate = DateUtils.addDaysToDate(record.date, 21)
                batch.set(tHeatRef, TaskItem(
                    id = tHeatRef.id,
                    name = "Check Return-to-Heat / Estrus: Pig $pigTag",
                    date = heatDate,
                    notes = "Check if sow returns to heat 18-24 days post-mating on ${record.date}",
                    pigIds = listOf(pigId)
                ))

                // 2. Day 110 Move Sow to Farrowing Pen & Wash/Deworm
                val tCrateRef = db.collection("users").document(userId).collection("tasks").document()
                batch.set(tCrateRef, TaskItem(
                    id = tCrateRef.id,
                    name = "Move to Farrowing Crate: Pig $pigTag",
                    date = day110Date,
                    notes = "Move sow to sanitized farrowing pen & wash/deworm 4-5 days before due date",
                    pigIds = listOf(pigId)
                ))

                // 3. Day 114 Expected Farrowing Due Date
                batch.update(pigRef, "status", "Pregnant")
                val tFarrowRef = db.collection("users").document(userId).collection("tasks").document()
                batch.set(tFarrowRef, TaskItem(
                    id = tFarrowRef.id,
                    name = "Farrowing: Pig $pigTag",
                    date = day114Date,
                    notes = "Scheduled 114 days after mating on ${record.date}",
                    pigIds = listOf(pigId)
                ))
            }
            "Confirm Pregnancy", "Pregnancy Check" -> {
                batch.update(pigRef, "purpose", "Breeder")
                if (pregnancyConfirmed) {
                    batch.update(pigRef, "status", "Pregnant")
                    val pigDoc = pigRef.get().await()
                    val lastMating = pigDoc.getString("lastBreedingDate") ?: record.date
                    val day110Date = DateUtils.addDaysToDate(lastMating, 110)
                    val day114Date = DateUtils.addDaysToDate(lastMating, 114)
                    batch.update(pigRef, "expectedFarrowingDate", day114Date, "farrowingPenMoveDate", day110Date)

                    val tCrateRef = db.collection("users").document(userId).collection("tasks").document()
                    batch.set(tCrateRef, TaskItem(
                        id = tCrateRef.id,
                        name = "Move to Farrowing Crate: Pig $pigTag",
                        date = day110Date,
                        notes = "Move sow to sanitized farrowing pen & wash/deworm 4-5 days before due date",
                        pigIds = listOf(pigId)
                    ))

                    val tFarrowRef = db.collection("users").document(userId).collection("tasks").document()
                    batch.set(tFarrowRef, TaskItem(
                        id = tFarrowRef.id,
                        name = "Farrowing: Pig $pigTag",
                        date = day114Date,
                        notes = "Scheduled 114 days after mating on $lastMating",
                        pigIds = listOf(pigId)
                    ))
                } else {
                    // Mating failed - reset sow status to open/Sow and schedule re-mating
                    batch.update(pigRef, "status", "Sow", "lastBreedingDate", "", "expectedFarrowingDate", "", "farrowingPenMoveDate", "")
                    val tRemateRef = db.collection("users").document(userId).collection("tasks").document()
                    val remateDate = DateUtils.addDaysToDate(record.date, 3)
                    batch.set(tRemateRef, TaskItem(
                        id = tRemateRef.id,
                        name = "Re-mate / Heat Check: Pig $pigTag",
                        date = remateDate,
                        notes = "Conception check failed on ${record.date}. Monitor for next estrus cycle and re-mate.",
                        pigIds = listOf(pigId)
                    ))
                }
            }
            "Farrowing" -> {
                val numMales = details["numMales"]?.toString()?.toIntOrNull() ?: 0
                val numFemales = details["numFemales"]?.toString()?.toIntOrNull() ?: 0
                val maleTagsStr = details["maleTags"]?.toString() ?: ""
                val femaleTagsStr = details["femaleTags"]?.toString() ?: ""
                
                val maleTags = maleTagsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val femaleTags = femaleTagsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }

                val pigDoc = pigRef.get().await()
                val breed = pigDoc.getString("breed") ?: ""
                val location = pigDoc.getString("location") ?: ""
                val sowTagSnapshot = pigDoc.getString("tagNumber") ?: ""
                val boarTag = pigDoc.getString("lastBoarTag") ?: ""

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

                // Increment Sow Parity
                val currentParity = (pigDoc.getLong("parity") ?: 0L).toInt()
                batch.update(pigRef, 
                    "status", "Lactating", 
                    "hasFarrowed", true, 
                    "weaned", false, 
                    "purpose", "Breeder",
                    "parity", currentParity + 1,
                    "expectedFarrowingDate", "",
                    "farrowingPenMoveDate", ""
                )

                // Auto-schedule Weaning 28 days post-farrowing
                val weanTaskRef = db.collection("users").document(userId).collection("tasks").document()
                val weanDate = DateUtils.addDaysToDate(record.date, 28)
                batch.set(weanTaskRef, TaskItem(
                    id = weanTaskRef.id,
                    name = "Weaning: Pig $pigTag",
                    date = weanDate,
                    notes = "Weaning due 28 days after farrowing on ${record.date}",
                    pigIds = listOf(pigId)
                ))
            }
            "Weaning" -> {
                val pigDoc = pigRef.get().await()
                val statusVal = pigDoc.getString("status") ?: ""
                val isMom = statusVal == "Lactating" || statusVal == "Nursing" || statusVal == "Sow"
                
                if (isMom) {
                    batch.update(pigRef, "status", "Sow")
                    // Auto-schedule Day 5 Post-Weaning Heat Check (Weaning-to-Service Interval: 4-7 days)
                    val heatTaskRef = db.collection("users").document(userId).collection("tasks").document()
                    val heatCheckDate = DateUtils.addDaysToDate(record.date, 5)
                    batch.set(heatTaskRef, TaskItem(
                        id = heatTaskRef.id,
                        name = "Post-Weaning Heat Check: Pig $pigTag",
                        date = heatCheckDate,
                        notes = "Check for estrus/standing heat 4-7 days post-weaning for immediate re-insemination",
                        pigIds = listOf(pigId)
                    ))
                } else {
                    val weaningLoc = details["weaningLocation"]?.toString() ?: ""
                    batch.update(pigRef, "status", "Starter", "weaned", true)
                    if (weaningLoc.isNotEmpty()) {
                        batch.update(pigRef, "location", weaningLoc)
                    }
                    val sowTagVal = pigDoc.getString("sowTag") ?: ""
                    if (sowTagVal.isNotEmpty()) {
                        val otherOffspring = db.collection("users").document(userId).collection("pigs").whereEqualTo("sowTag", sowTagVal).whereEqualTo("weaned", false).get().await()
                        if (otherOffspring.documents.all { it.id == pigId }) {
                            val sowSnapshot = db.collection("users").document(userId).collection("pigs").whereEqualTo("tagNumber", sowTagVal).limit(1).get().await()
                            if (!sowSnapshot.isEmpty) {
                                val sowDocId = sowSnapshot.documents[0].id
                                batch.update(db.collection("users").document(userId).collection("pigs").document(sowDocId), "status", "Sow")
                                val heatTaskRef = db.collection("users").document(userId).collection("tasks").document()
                                val heatCheckDate = DateUtils.addDaysToDate(record.date, 5)
                                batch.set(heatTaskRef, TaskItem(
                                    id = heatTaskRef.id,
                                    name = "Post-Weaning Heat Check: Pig $sowTagVal",
                                    date = heatCheckDate,
                                    notes = "Check for estrus/standing heat 4-7 days post-weaning for immediate re-insemination",
                                    pigIds = listOf(sowDocId)
                                ))
                            }
                        }
                    }
                }
            }
            "Castration" -> {
                val pigDoc = pigRef.get().await()
                if (pigDoc.getString("gender")?.equals("Male", ignoreCase = true) == true) {
                    batch.update(pigRef, "castrated", true, "castrationDate", record.date)
                }
            }
            "Teeth Clipping" -> {
                batch.update(pigRef, "teethClipped", true)
            }
            "Tail Docking" -> {
                batch.update(pigRef, "tailDocked", true)
            }
            "Iron Injection" -> {
                val pigDoc = pigRef.get().await()
                val currentInjections = (pigDoc.getLong("ironInjections") ?: 0L).toInt()
                batch.update(pigRef, "ironInjections", currentInjections + 1)
            }
            "Medication", "Deworming", "Vaccination", "Treatment" -> {
                val withdrawalDays = details["withdrawalDays"]?.toString()?.toIntOrNull() ?: record.withdrawalPeriodDays
                if (withdrawalDays > 0) {
                    val safeDate = DateUtils.addDaysToDate(record.date, withdrawalDays)
                    val medName = details["medication"]?.toString() ?: record.medication.ifEmpty { record.type }
                    batch.update(pigRef, 
                        "activeWithdrawalUntil", safeDate,
                        "withdrawalMedication", medName
                    )
                    val wTaskRef = db.collection("users").document(userId).collection("tasks").document()
                    batch.set(wTaskRef, TaskItem(
                        id = wTaskRef.id,
                        name = "Meat Withdrawal Cleared: Pig $pigTag",
                        date = safeDate,
                        notes = "Safe for slaughter and meat sale. Medication: $medName",
                        pigIds = listOf(pigId)
                    ))
                }
            }
            "Weight Check" -> {
                val weightVal = details["weight"]?.toString()?.toDoubleOrNull() ?: record.weight
                if (weightVal > 0.0) {
                    batch.update(pigRef, "weight", weightVal, "lastWeightDate", record.date)
                }
            }
            "Culling" -> {
                val reason = details["cullingReason"]?.toString() ?: "Unknown"
                val salePriceVal = details["salePrice"]?.toString()?.toDoubleOrNull() ?: 0.0
                
                val pigDoc = pigRef.get().await()
                val pigObj = pigDoc.toObject(Pig::class.java)
                if (pigObj != null) {
                    batch.set(db.collection("users").document(userId).collection("archived_pigs").document(pigId), 
                        pigObj.copy(status = "Culled ($reason)"))
                }
                batch.delete(pigRef)
                
                if (reason == "Sold" && salePriceVal > 0) {
                    val fRef = db.collection("users").document(userId).collection("financials").document()
                    batch.set(fRef, FinancialRecord(
                        id = fRef.id, 
                        type = "Income", 
                        category = "Pig Sale", 
                        amount = salePriceVal, 
                        date = record.date, 
                        description = "Sale of Pig $pigTag", 
                        pigId = pigId
                    ))
                }
            }
        }
    }

    suspend fun updateHealthRecordWithLogic(
        userId: String,
        pigId: String,
        record: HealthRecord,
        trackHeat: Boolean = false,
        checkPregnancy: Boolean = false,
        pregnancyConfirmed: Boolean = false,
        details: Map<String, Any> = emptyMap()
    ) {
        val pigRef = db.collection("users").document(userId).collection("pigs").document(pigId)
        val pigDoc = pigRef.get().await()
        val pigTag = pigDoc.getString("tagNumber") ?: pigId
        
        val isFuture = DateUtils.isFutureDate(record.date)
        var updatedRecord = record
        
        // Sync with Task
        record.taskId?.let { tId ->
            if (isFuture) {
                db.collection("users").document(userId).collection("tasks").document(tId)
                    .update(
                        "date", DateUtils.convertToTaskDate(record.date), 
                        "name", "${record.type}: Pig $pigTag", 
                        "notes", record.description
                    ).await()
            } else {
                db.collection("users").document(userId).collection("tasks").document(tId).delete().await()
                updatedRecord = record.copy(taskId = null)
            }
        } ?: run {
            if (isFuture) {
                val taskRef = db.collection("users").document(userId).collection("tasks").document()
                val task = TaskItem(
                    id = taskRef.id,
                    name = "${record.type}: Pig $pigTag",
                    date = DateUtils.convertToTaskDate(record.date),
                    notes = record.description,
                    pigIds = listOf(pigId)
                )
                db.collection("users").document(userId).collection("tasks").document(taskRef.id).set(task).await()
                updatedRecord = record.copy(taskId = taskRef.id)
            }
        }

        if (!isFuture) {
            val batch = db.batch()
            val isCulling = record.type == "Culling"
            val recordRef = if (isCulling) {
                db.collection("users").document(userId).collection("archived_pigs").document(pigId).collection("health_records").document(record.id)
            } else {
                pigRef.collection("health_records").document(record.id)
            }
            
            handleSpecializedActivityLogic(batch, userId, pigId, pigTag, updatedRecord, trackHeat, checkPregnancy, pregnancyConfirmed, details)
            
            batch.set(recordRef, updatedRecord)
            batch.commit().await()
        } else {
            pigRef.collection("health_records").document(record.id).set(updatedRecord).await()
        }
    }

    suspend fun deleteHealthRecord(userId: String, pigId: String, recordId: String) {
        var docRef = db.collection("users").document(userId).collection("pigs").document(pigId)
            .collection("health_records").document(recordId)
        var doc = docRef.get().await()
        if (!doc.exists()) {
            docRef = db.collection("users").document(userId).collection("archived_pigs").document(pigId)
                .collection("health_records").document(recordId)
            doc = docRef.get().await()
        }
        
        val taskId = doc.getString("taskId")
        if (!taskId.isNullOrEmpty()) {
            db.collection("users").document(userId).collection("tasks").document(taskId).delete().await()
        }

        docRef.delete().await()
    }

    suspend fun recalculatePigStatusFromHistory(userId: String, pigId: String, calculatedStatus: PigStatus) {
        val pigRef = db.collection("users").document(userId).collection("pigs").document(pigId)
        val pigSnapshot = pigRef.get().await()
        if (!pigSnapshot.exists()) return
        val pig = pigSnapshot.toObject(Pig::class.java) ?: return
        
        val records = pigRef.collection("health_records").get().await().toObjects(HealthRecord::class.java)
        val sortedRecords = records.sortedByDescending { parseAnyDate(it.date) }
        
        var newStatus: String? = null
        for (r in sortedRecords) {
            when (r.type) {
                "Farrowing" -> {
                    newStatus = PigStatus.LACTATING.displayName
                    break
                }
                "Breeding/Mating" -> {
                    if (!r.description.contains("Confirm Pregnancy", ignoreCase = true)) {
                        newStatus = PigStatus.PREGNANT.displayName
                        break
                    }
                }
                "Confirm Pregnancy", "Pregnancy Check" -> {
                    if (r.description.contains("Confirmed", ignoreCase = true) || !r.description.contains("Failed", ignoreCase = true)) {
                        newStatus = PigStatus.PREGNANT.displayName
                        break
                    }
                }
                "Weaning" -> {
                    newStatus = if (pig.genderEnum == PigGender.FEMALE) PigStatus.SOW.displayName else PigStatus.STARTER.displayName
                    break
                }
            }
        }
        
        val targetStatus = newStatus ?: calculatedStatus.displayName
        if (pig.status != targetStatus) {
            pigRef.update("status", targetStatus).await()
        }
    }

    private fun parseAnyDate(dateStr: String): Date {
        return DateUtils.parseDisplay(dateStr)
            ?: DateUtils.parseInternal(dateStr)
            ?: DateUtils.parseTask(dateStr)
            ?: Date(0)
    }
}
