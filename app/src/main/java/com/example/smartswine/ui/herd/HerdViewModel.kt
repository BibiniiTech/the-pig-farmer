package com.example.smartswine.ui.herd

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartswine.model.Pig
import com.example.smartswine.model.HealthRecord
import com.example.smartswine.model.FinancialRecord
import com.example.smartswine.model.TaskItem
import com.example.smartswine.ui.settings.SettingsViewModel
import com.example.smartswine.utils.DateUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.concurrent.TimeUnit

class HerdViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    // Data classes for Add Pig Form
    data class MultiPigEntry(
        val tagNumber: String,
        val weight: String,
        val location: String,
    )

    data class AddPigFormData(
        val isMultiple: Boolean,
        val birthDate: String,
        val breed: String,
        val purpose: String,
        val sowTag: String,
        val boarTag: String,
        val source: String,
        val notes: String,
        // Single mode specific
        val tagNumber: String = "",
        val gender: String = "Male",
        val castrated: Boolean? = null,
        val castrationDate: String = "",
        val hasFarrowed: Boolean = false,
        val weight: String = "",
        val location: String = "",
        val purchasePrice: String = "",
        // Multiple mode specific
        val malePigs: List<MultiPigEntry> = emptyList(),
        val femalePigs: List<MultiPigEntry> = emptyList()
    )
    
    // Active Farm ID for multi-user support
    private var activeFarmId: String? = null
    private var pigsListener: ListenerRegistration? = null
    private var archivedPigsListener: ListenerRegistration? = null
    private var singlePigListeners = mutableMapOf<String, ListenerRegistration>()
    private var healthRecordsListeners = mutableMapOf<String, ListenerRegistration>()

    fun setActiveFarmId(uid: String) {
        if (activeFarmId != uid) {
            activeFarmId = uid
            fetchHerd()
        }
    }
    
    // Use singleton SettingsViewModel
    val settingsViewModel = SettingsViewModel.getInstance()

    private val _pigs = MutableStateFlow<List<Pig>>(emptyList())
    val pigs = _pigs.asStateFlow()

    private val _archivedPigs = MutableStateFlow<List<Pig>>(emptyList())
    val archivedPigs = _archivedPigs.asStateFlow()

    val allPigsIncludingArchived = combine(_pigs, _archivedPigs) { active, archived ->
        (active + archived).sortedBy { it.tagNumber }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sowTags = _pigs.map { activePigs ->
        activePigs.asSequence()
            .filter { it.gender == "Female" }
            .map { it.tagNumber }
            .distinct()
            .sorted()
            .toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val boarTags = _pigs.map { activePigs ->
        activePigs.asSequence()
            .filter { it.gender == "Male" }
            .map { it.tagNumber }
            .distinct()
            .sorted()
            .toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _purposeFilter = MutableStateFlow<String?>(null)
    val purposeFilter = _purposeFilter.asStateFlow()

    private val _statusFilter = MutableStateFlow<String?>(null)
    val statusFilter = _statusFilter.asStateFlow()

    val filteredPigs = combine(_pigs, _searchQuery, _purposeFilter, _statusFilter) { pigs, query, purpose, status ->
        pigs.asSequence().filter { pig ->
            (query.isEmpty() ||
                    (pig.tagNumber.contains(query, ignoreCase = true)) ||
                    (pig.breed.contains(query, ignoreCase = true)) ||
                    (pig.location.contains(query, ignoreCase = true))
            ) &&
            ((purpose == null) || (pig.purpose == purpose)) &&
            ((status == null) || (if (status == "Pregnant") pig.status.equals("Pregnant", ignoreCase = true) else (pig.status == status)))
        }.sortedBy { it.tagNumber }.toList()
    }

    private val _healthRecords = MutableStateFlow<List<HealthRecord>>(emptyList())
    val healthRecords = _healthRecords.asStateFlow()

    init {
        fetchHerd()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setPurposeFilter(purpose: String?) {
        _purposeFilter.value = purpose
    }

    fun setStatusFilter(status: String?) {
        _statusFilter.value = status
    }

    private fun healStuckPregnancies(userId: String, pigList: List<Pig>) {
        val pigsToCheck = pigList.filter { 
            it.status == "Pregnant" || it.status == "Lactating" || it.status == "Nursing" 
        }
        if (pigsToCheck.isEmpty()) return
        
        viewModelScope.launch {
            pigsToCheck.forEach { pig ->
                try {
                    val pigRef = db.collection("users").document(userId).collection("pigs").document(pig.id)
                    val records = pigRef.collection("health_records").get().await().toObjects(HealthRecord::class.java)
                    
                    val isValid = when (pig.status) {
                        "Pregnant" -> records.any { r ->
                            r.type == "Breeding/Mating" || r.type == "Confirm Pregnancy" || r.type == "Pregnancy Check"
                        }
                        "Lactating", "Nursing" -> records.any { r ->
                            r.type == "Farrowing"
                        }
                        else -> true
                    }
                    
                    if (!isValid) {
                        Log.d("HerdViewModel", "Healing pig ${pig.tagNumber}: stuck in ${pig.status} status with no history records.")
                        val revertedStatus = getCalculatedStatus(pig)
                        pigRef.update("status", revertedStatus).await()
                    }
                } catch (e: Exception) {
                    Log.e("HerdViewModel", "Error healing status for pig ${pig.id}: ${e.message}")
                }
            }
        }
    }

    private fun fetchHerd() {
        val userId = activeFarmId ?: auth.currentUser?.uid ?: return
        pigsListener?.remove()
        pigsListener = db.collection("users").document(userId).collection("pigs")
            .addSnapshotListener { snapshot, e ->
                if (auth.currentUser == null) {
                    pigsListener?.remove()
                    pigsListener = null
                    return@addSnapshotListener
                }
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    val pigList = snapshot.documents.asSequence().mapNotNull { doc ->
                        doc.toObject(Pig::class.java)?.copy(id = doc.id)
                    }.map { calculatePigStatus(it) }.toList()
                    _pigs.value = pigList
                    healStuckPregnancies(userId, pigList)
                }
            }
        
        archivedPigsListener?.remove()
        archivedPigsListener = db.collection("users").document(userId).collection("archived_pigs")
            .addSnapshotListener { snapshot, e ->
                if (auth.currentUser == null) {
                    archivedPigsListener?.remove()
                    archivedPigsListener = null
                    return@addSnapshotListener
                }
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    val archivedList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Pig::class.java)?.copy(id = doc.id)
                    }
                    _archivedPigs.value = archivedList
                }
            }
    }

    private fun getCalculatedStatus(pig: Pig): String {
        val birthDate = DateUtils.parseInternal(pig.birthDate)

        val ageDays = if (birthDate != null) {
            val diff = Date().time - birthDate.time
            TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)
        } else 0L

        if (pig.purpose == "Porker") {
            return when {
                ageDays <= 28 && !pig.weaned -> "Piglet"
                ageDays <= 70 -> "Starter"
                ageDays <= 112 -> "Grower"
                else -> "Finisher"
            }
        } else if (pig.purpose == "Breeder") {
            if (ageDays > 182) { // 6 months
                if (pig.gender == "Female") {
                    return if (pig.hasFarrowed) "Sow" else "Gilt"
                } else if (pig.gender == "Male") {
                    return if (pig.castrated == true) "Barrow" else "Boar"
                }
            } else {
                return when {
                    ageDays <= 28 && !pig.weaned -> "Piglet"
                    ageDays <= 70 -> "Starter"
                    else -> "Grower"
                }
            }
        }
        return "Unknown"
    }

    private fun calculatePigStatus(pig: Pig): Pig {
        // High-priority states that are set manually via activities
        if (pig.status == "Pregnant" || pig.status == "Lactating" || pig.status == "Nursing") {
            return pig
        }
        return pig.copy(status = getCalculatedStatus(pig))
    }

    private fun parseAnyDate(dateStr: String): Date {
        return DateUtils.parseDisplay(dateStr)
            ?: DateUtils.parseInternal(dateStr)
            ?: DateUtils.parseTask(dateStr)
            ?: Date(0)
    }

    private suspend fun recalculatePigStatusFromHistory(pigId: String, userId: String) {
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
                    newStatus = "Lactating"
                    break
                }
                "Breeding/Mating" -> {
                    if (!r.description.contains("Confirm Pregnancy", ignoreCase = true)) {
                        newStatus = "Pregnant"
                        break
                    }
                }
                "Confirm Pregnancy", "Pregnancy Check" -> {
                    if (r.description.contains("Confirmed", ignoreCase = true) || !r.description.contains("Failed", ignoreCase = true)) {
                        newStatus = "Pregnant"
                        break
                    }
                }
                "Weaning" -> {
                    newStatus = if (pig.gender == "Female") "Sow" else "Starter"
                    break
                }
            }
        }
        
        val targetStatus = newStatus ?: getCalculatedStatus(pig)
        if (pig.status != targetStatus) {
            pigRef.update("status", targetStatus).await()
        }
    }

    fun getPig(pigId: String): StateFlow<Pig?> {
        // Try to find the pig in our already loaded lists to provide an immediate value
        val initialPig = _pigs.value.find { it.id == pigId } ?: _archivedPigs.value.find { it.id == pigId }
        val pigState = MutableStateFlow(initialPig)
        val userId = activeFarmId ?: auth.currentUser?.uid ?: return pigState.asStateFlow()
        
        singlePigListeners[pigId]?.remove()
        singlePigListeners[pigId] = db.collection("users").document(userId).collection("pigs").document(pigId)
            .addSnapshotListener { snapshot, error ->
                if (auth.currentUser == null) {
                    singlePigListeners[pigId]?.remove()
                    singlePigListeners.remove(pigId)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val pig = snapshot.toObject(Pig::class.java)?.copy(id = snapshot.id)
                    pigState.value = pig?.let { calculatePigStatus(it) }
                } else if (error == null && snapshot != null && !snapshot.exists()) {
                    // Document explicitly does not exist
                    pigState.value = null
                }
                // If error is present (e.g. offline and not in cache), we keep the initialPig value
            }
        return pigState.asStateFlow()
    }

    fun fetchHealthRecords(pigId: String) {
        val userId = activeFarmId ?: auth.currentUser?.uid ?: return
        
        // Clear existing records to prevent showing stale data from previous pig
        _healthRecords.value = emptyList()
        
        healthRecordsListeners[pigId]?.remove()
        healthRecordsListeners[pigId] = db.collection("users").document(userId).collection("pigs").document(pigId)
            .collection("health_records")
            .orderBy("date")
            .addSnapshotListener { snapshot, _ ->
                if (auth.currentUser == null) {
                    healthRecordsListeners[pigId]?.remove()
                    healthRecordsListeners.remove(pigId)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val records = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(HealthRecord::class.java)?.copy(id = doc.id)
                    }
                    _healthRecords.value = records
                }
            }
    }

    fun addHealthRecord(
        pigId: String, 
        record: HealthRecord,
        trackHeat: Boolean = false,
        checkPregnancy: Boolean = false,
        pregnancyConfirmed: Boolean = false,
        details: Map<String, Any> = emptyMap()
    ) {
        val userId = activeFarmId ?: auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val isFuture = DateUtils.isFutureDate(record.date)
                
                val pigRef = db.collection("users").document(userId).collection("pigs").document(pigId)
                val pigDoc = pigRef.get().await()
                val pigTag = pigDoc.getString("tagNumber") ?: pigId
                
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
                    
                    handleSpecializedActivityLogic(batch, pigId, updatedRecord, trackHeat, checkPregnancy, pregnancyConfirmed, details, userId, pigTag)
                    
                    batch.set(recordRef, updatedRecord)
                    batch.commit().await()
                }
            } catch (e: Exception) {
                _error.value = "Failed to add health record: ${e.message}"
            }
        }
    }

    private suspend fun handleSpecializedActivityLogic(
        batch: WriteBatch,
        pigId: String,
        record: HealthRecord,
        trackHeat: Boolean,
        checkPregnancy: Boolean,
        pregnancyConfirmed: Boolean,
        details: Map<String, Any>,
        userId: String,
        pigTag: String
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
                if (checkPregnancy) {
                    val tRef = db.collection("users").document(userId).collection("tasks").document()
                    val taskDate = DateUtils.addDaysToDate(record.date, 21)
                    batch.set(tRef, TaskItem(
                        id = tRef.id,
                        name = "Confirm Pregnancy: Pig $pigTag",
                        date = taskDate,
                        notes = "Scheduled 21 days after mating on ${record.date}",
                        pigIds = listOf(pigId)
                    ))
                } else {
                    batch.update(pigRef, "status", "Pregnant")
                    val tRef = db.collection("users").document(userId).collection("tasks").document()
                    val taskDate = DateUtils.addDaysToDate(record.date, 114)
                    batch.set(tRef, TaskItem(
                        id = tRef.id,
                        name = "Farrowing: Pig $pigTag",
                        date = taskDate,
                        notes = "Scheduled 114 days after mating on ${record.date}",
                        pigIds = listOf(pigId)
                    ))
                }
            }
            "Confirm Pregnancy", "Pregnancy Check" -> {
                batch.update(pigRef, "purpose", "Breeder")
                if (pregnancyConfirmed) {
                    batch.update(pigRef, "status", "Pregnant")
                    val pigDoc = pigRef.get().await()
                    val lastMating = pigDoc.getString("lastBreedingDate") ?: record.date
                    val tRef = db.collection("users").document(userId).collection("tasks").document()
                    val taskDate = DateUtils.addDaysToDate(lastMating, 114)
                    batch.set(tRef, TaskItem(
                        id = tRef.id,
                        name = "Farrowing: Pig $pigTag",
                        date = taskDate,
                        notes = "Scheduled 114 days after mating on $lastMating",
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
                batch.update(pigRef, "status", "Lactating", "hasFarrowed", true, "weaned", false, "purpose", "Breeder")
            }
            "Weaning" -> {
                val pigDoc = pigRef.get().await()
                val statusVal = pigDoc.getString("status") ?: ""
                val isMom = statusVal == "Lactating" || statusVal == "Nursing" || statusVal == "Sow"
                
                if (isMom) {
                    batch.update(pigRef, "status", "Sow")
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
                                db.collection("users").document(userId).collection("pigs").document(sowSnapshot.documents[0].id).update("status", "Sow")
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
            "Weight Check" -> {
                val weightVal = details["weight"]?.toString()?.toDoubleOrNull() ?: 0.0
                if (weightVal > 0.0) {
                    batch.update(pigRef, "weight", weightVal)
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
                cleanupTasksForPig(pigId, userId)
            }
        }
    }

    fun updateHealthRecord(
        pigId: String, 
        record: HealthRecord,
        trackHeat: Boolean = false,
        checkPregnancy: Boolean = false,
        pregnancyConfirmed: Boolean = false,
        details: Map<String, Any> = emptyMap()
    ) {
        val userId = activeFarmId ?: auth.currentUser?.uid ?: return
        val recordId = record.id
        if (recordId.isEmpty()) return
        viewModelScope.launch {
            try {
                val isFuture = DateUtils.isFutureDate(record.date)
                
                val pigRef = db.collection("users").document(userId).collection("pigs").document(pigId)
                val pigDoc = pigRef.get().await()
                val pigTag = pigDoc.getString("tagNumber") ?: pigId
                
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
                        db.collection("users").document(userId).collection("archived_pigs").document(pigId).collection("health_records").document(recordId)
                    } else {
                        pigRef.collection("health_records").document(recordId)
                    }
                    
                    handleSpecializedActivityLogic(batch, pigId, updatedRecord, trackHeat, checkPregnancy, pregnancyConfirmed, details, userId, pigTag)
                    
                    batch.set(recordRef, updatedRecord)
                    batch.commit().await()
                } else {
                    pigRef.collection("health_records").document(recordId).set(updatedRecord).await()
                }
            } catch (e: Exception) {
                _error.value = "Failed to update health record: ${e.message}"
            }
        }
    }

    fun deleteHealthRecord(pigId: String, recordId: String) {
        val userId = activeFarmId ?: auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
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

                // Recalculate and update the pig's status based on the remaining history
                recalculatePigStatusFromHistory(pigId, userId)
            } catch (e: Exception) {
                _error.value = "Failed to delete health record: ${e.message}"
            }
        }
    }

    fun addPig(pig: Pig, purchasePrice: Double = 0.0) {
        val userId = activeFarmId ?: auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                // Final safety check for pig limit on free tier
                val userDoc = db.collection("users").document(userId).get().await()
                val isPremium = userDoc.getBoolean("isPremium") == true
                
                if (!isPremium) {
                    val activeSnapshot = db.collection("users").document(userId).collection("pigs").get().await()
                    val archivedSnapshot = db.collection("users").document(userId).collection("archived_pigs").get().await()
                    if (activeSnapshot.size() + archivedSnapshot.size() >= 20) {
                        _error.value = "Pig limit reached for free tier. Please upgrade to add more."
                        return@launch
                    }
                }

                val pigRef = db.collection("users").document(userId).collection("pigs").document()
                val pigWithId = calculatePigStatus(pig).copy(id = pigRef.id)
                
                db.runTransaction { transaction ->
                    transaction.set(pigRef, pigWithId)
                    
                    if (pig.castrated == true && pig.castrationDate.isNotEmpty()) {
                        val recordRef = pigRef.collection("health_records").document()
                        val record = HealthRecord(
                            date = pig.castrationDate,
                            type = "Castration",
                            description = "Initial record of castration.",
                        )
                        transaction.set(recordRef, record)
                    }

                    if (pig.weight > 0) {
                        val recordRef = pigRef.collection("health_records").document()
                        val record = HealthRecord(
                            date = DateUtils.formatToInternal(Date()),
                            type = "Weight Check",
                            description = "Initial weight record.",
                        )
                        transaction.set(recordRef, record)
                    }

                    if (pig.source == "Brought to farm" && purchasePrice > 0) {
                        val financialRef = db.collection("users").document(userId)
                            .collection("financials").document()
                        val financialRecord = FinancialRecord(
                            id = financialRef.id,
                            date = DateUtils.formatToInternal(Date()),
                            type = "Expense",
                            category = "Livestock Purchase",
                            amount = purchasePrice,
                            description = "Purchase of pig with Tag: ${pig.tagNumber}",
                            pigId = pigRef.id,
                        )
                        transaction.set(financialRef, financialRecord)
                    }
                }.await()
            } catch (_: Exception) {
                _error.value = "Failed to add pig"
            }
        }
    }

    fun addPigsFromForm(formData: AddPigFormData) {
        if (!formData.isMultiple) {
            val pig = Pig(
                tagNumber = formData.tagNumber,
                birthDate = formData.birthDate,
                breed = formData.breed,
                gender = formData.gender,
                castrated = if (formData.gender == "Male") formData.castrated else null,
                castrationDate = if (formData.gender == "Male" && formData.castrated == true) formData.castrationDate else "",
                hasFarrowed = formData.hasFarrowed,
                weight = formData.weight.toDoubleOrNull() ?: 0.0,
                purpose = formData.purpose,
                sowTag = formData.sowTag,
                boarTag = formData.boarTag,
                location = formData.location,
                source = formData.source,
                notes = formData.notes
            )
            addPig(pig, formData.purchasePrice.toDoubleOrNull() ?: 0.0)
        } else {
            val validMales = formData.malePigs.filter { it.tagNumber.isNotEmpty() }
            val validFemales = formData.femalePigs.filter { it.tagNumber.isNotEmpty() }
            
            validMales.forEach { entry ->
                val pig = Pig(
                    tagNumber = entry.tagNumber,
                    birthDate = formData.birthDate,
                    breed = formData.breed,
                    gender = "Male",
                    weight = entry.weight.toDoubleOrNull() ?: 0.0,
                    purpose = formData.purpose,
                    sowTag = formData.sowTag,
                    boarTag = formData.boarTag,
                    location = entry.location,
                    source = formData.source,
                    notes = formData.notes
                )
                addPig(pig, 0.0)
            }
            validFemales.forEach { entry ->
                val pig = Pig(
                    tagNumber = entry.tagNumber,
                    birthDate = formData.birthDate,
                    breed = formData.breed,
                    gender = "Female",
                    weight = entry.weight.toDoubleOrNull() ?: 0.0,
                    purpose = formData.purpose,
                    sowTag = formData.sowTag,
                    boarTag = formData.boarTag,
                    location = entry.location,
                    source = formData.source,
                    notes = formData.notes
                )
                addPig(pig, 0.0)
            }
        }
    }

    fun updatePig(pig: Pig) {
        val userId = activeFarmId ?: auth.currentUser?.uid ?: return
        if (pig.id.isEmpty()) return
        viewModelScope.launch {
            try {
                val pigRef = db.collection("users").document(userId).collection("pigs").document(pig.id)
                val currentPigDoc = pigRef.get().await()
                val oldWeight = currentPigDoc.getDouble("weight") ?: 0.0
                
                val updatedPig = calculatePigStatus(pig)
                pigRef.set(updatedPig).await()
                
                // If weight was updated manually, add a history record for it to clear warnings
                if (updatedPig.weight != oldWeight && updatedPig.weight > 0) {
                     val record = HealthRecord(
                        date = DateUtils.formatToInternal(Date()),
                        type = "Weight Check",
                        description = "Weight updated manually in pig details",
                    )
                    // Just add the record directly to health_records subcollection
                    pigRef.collection("health_records").add(record).await()
                }
            } catch (_: Exception) {
                _error.value = "Failed to update pig"
            }
        }
    }

    fun updatePigWeight(pigId: String, weight: Double) {
        val userId = activeFarmId ?: auth.currentUser?.uid ?: return
        if (pigId.isEmpty()) return
        
        // Use the common addHealthRecord logic which handles both the history entry 
        // AND updating the current weight in the pig document via handleSpecializedActivityLogic
        val record = HealthRecord(
            date = DateUtils.formatToInternal(Date()),
            type = "Weight Check",
            description = "Weight updated via Tape Measurement",
        )
        addHealthRecord(pigId, record, details = mapOf("weight" to weight))
    }

    fun archivePig(pig: Pig, reason: String) {
        val userId = activeFarmId ?: auth.currentUser?.uid ?: return
        if (pig.id.isEmpty()) return
        viewModelScope.launch {
            try {
                val archivedPig = pig.copy(
                    status = "Archived ($reason)",
                    notes = pig.notes + "\nArchived on: ${DateUtils.formatToInternal(Date())} Reason: $reason",
                )
                db.runTransaction { transaction ->
                    val pigRef = db.collection("users").document(userId).collection("pigs").document(pig.id)
                    val archiveRef = db.collection("users").document(userId).collection("archived_pigs").document(pig.id)
                    
                    transaction.set(archiveRef, archivedPig)
                    transaction.delete(pigRef)
                }.await()
            } catch (e: Exception) {
                _error.value = "Failed to archive pig: ${e.message}"
            }
        }
    }

    fun deletePig(pig: Pig) {
        val userId = activeFarmId ?: auth.currentUser?.uid ?: return
        if (pig.id.isEmpty()) return
        viewModelScope.launch {
            try {
                db.runTransaction { transaction ->
                    transaction.delete(db.collection("users").document(userId).collection("pigs").document(pig.id))
                    // Note: We could also trigger a background cleanup for tasks here if needed
                }.await()
                // Explicitly cleanup tasks after deletion
                cleanupTasksForPig(pig.id, userId)
            } catch (_: Exception) {
                _error.value = "Failed to delete pig"
            }
        }
    }

    private fun cleanupTasksForPig(pigId: String, userId: String) {
        viewModelScope.launch {
            try {
                val tasks = db.collection("users").document(userId).collection("tasks")
                    .whereArrayContains("pigIds", pigId).get().await()
                
                val batch = db.batch()
                tasks.forEach { doc ->
                    val task = doc.toObject(TaskItem::class.java)
                    if (task.pigIds.size <= 1) {
                        batch.delete(doc.reference)
                    } else {
                        val newPigIds = task.pigIds.filter { it != pigId }
                        // Simplify name by removing the pig's tag if possible
                        // (Requires knowing the tag, but we can just update the IDs for now)
                        batch.update(doc.reference, "pigIds", newPigIds)
                    }
                }
                batch.commit().await()
            } catch (e: Exception) {
                Log.e("HerdViewModel", "Task cleanup error", e)
            }
        }
    }

    // Statistics for the Ribbon
    val stats = pigs.map { allPigs ->
        val breeders = allPigs.filter { it.purpose == "Breeder" }
        val porkers = allPigs.filter { it.purpose == "Porker" }

        mapOf(
            "total" to allPigs.size,
            "breeders_count" to breeders.size,
            "porkers_count" to porkers.size,
            "piglets_total" to allPigs.count { it.status == "Piglet" },
            
            // Breeder specific categories
            "breeders_piglets" to breeders.count { it.status == "Piglet" },
            "breeders_starter" to breeders.count { it.status == "Starter" },
            "breeders_grower" to breeders.count { it.status == "Grower" },
            "boars" to breeders.count { it.status == "Boar" },
            "gilts" to breeders.count { it.status == "Gilt" },
            "Pregnant" to breeders.count { it.status == "Pregnant" },
            "Lactating" to breeders.count { it.status == "Lactating" },
            "sows" to breeders.count { it.status == "Sow" },

            "Finisher" to porkers.count { it.status == "Finisher" },
            "Grower" to porkers.count { it.status == "Grower" },
            "Starter" to porkers.count { it.status == "Starter" },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun getAllHealthRecords(): StateFlow<Map<String, List<HealthRecord>>> {
        val result = MutableStateFlow<Map<String, List<HealthRecord>>>(emptyMap())
        val userId = activeFarmId ?: auth.currentUser?.uid ?: return result

        viewModelScope.launch {
            try {
                // Fetch health records for all active pigs
                val activePigs = pigs.value
                val archivedPigs = db.collection("users").document(userId).collection("archived_pigs").get().await().toObjects(Pig::class.java)
                val allPigs = activePigs + archivedPigs
                
                val allRecords = mutableMapOf<String, List<HealthRecord>>()
                
                for (pig in allPigs) {
                    val records = db.collection("users").document(userId)
                        .collection("pigs").document(pig.id)
                        .collection("health_records").get().await()
                        .toObjects(HealthRecord::class.java)
                    
                    if (records.isNotEmpty()) {
                        allRecords[pig.id] = records
                    }
                }
                result.value = allRecords
            } catch (e: Exception) {
                _error.value = "Failed to fetch all health records: ${e.message}"
            }
        }
        return result
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        pigsListener?.remove()
        archivedPigsListener?.remove()
        singlePigListeners.values.forEach { it.remove() }
        healthRecordsListeners.values.forEach { it.remove() }
    }
}
