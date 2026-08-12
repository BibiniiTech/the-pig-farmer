package com.example.smartswine.ui.herd

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartswine.model.*
import com.example.smartswine.data.HerdRepository
import com.example.smartswine.data.TaskRepository
import com.example.smartswine.data.FinancialRepository
import com.example.smartswine.ui.settings.SettingsViewModel
import com.example.smartswine.utils.DateUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.concurrent.TimeUnit

class HerdViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val herdRepository = HerdRepository(db)
    private val taskRepository = TaskRepository(db)
    private val financialRepository = FinancialRepository(db)
    
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
            .filter { it.genderEnum == PigGender.FEMALE }
            .map { it.tagNumber }
            .distinct()
            .sorted()
            .toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val boarTags = _pigs.map { activePigs ->
        activePigs.asSequence()
            .filter { it.genderEnum == PigGender.MALE }
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
            ((status == null) || (if (status == PigStatus.PREGNANT.displayName) pig.status.equals(PigStatus.PREGNANT.displayName, ignoreCase = true) else (pig.status == status)))
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
            val s = it.statusEnum
            s == PigStatus.PREGNANT || s == PigStatus.LACTATING || s == PigStatus.NURSING 
        }
        if (pigsToCheck.isEmpty()) return
        
        viewModelScope.launch {
            pigsToCheck.forEach { pig ->
                try {
                    val records = herdRepository.getHealthRecords(userId, pig.id).first()
                    
                    val isValid = when (pig.statusEnum) {
                        PigStatus.PREGNANT -> records.any { r ->
                            r.type == "Breeding/Mating" || r.type == "Confirm Pregnancy" || r.type == "Pregnancy Check"
                        }
                        PigStatus.LACTATING, PigStatus.NURSING -> records.any { r ->
                            r.type == "Farrowing"
                        }
                        else -> true
                    }
                    
                    if (!isValid) {
                        Log.d("HerdViewModel", "Healing pig ${pig.tagNumber}: stuck in ${pig.status} status with no history records.")
                        val revertedStatus = getCalculatedStatus(pig)
                        herdRepository.updatePig(userId, pig.copy(status = revertedStatus.displayName))
                    }
                } catch (e: Exception) {
                    Log.e("HerdViewModel", "Error healing status for pig ${pig.id}: ${e.message}")
                }
            }
        }
    }

    private fun fetchHerd() {
        val userId = activeFarmId ?: auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            herdRepository.getPigs(userId).collect { pigList ->
                val calculatedPigs = pigList.map { calculatePigStatus(it) }
                _pigs.value = calculatedPigs
                healStuckPregnancies(userId, calculatedPigs)
            }
        }

        viewModelScope.launch {
            herdRepository.getArchivedPigs(userId).collect { archivedList ->
                _archivedPigs.value = archivedList
            }
        }
    }

    private fun getCalculatedStatus(pig: Pig): PigStatus {
        val birthDate = DateUtils.parseInternal(pig.birthDate)

        val ageDays = if (birthDate != null) {
            val diff = Date().time - birthDate.time
            TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)
        } else 0L

        if (pig.purposeEnum == PigPurpose.PORKER) {
            return when {
                ageDays <= 28 && !pig.weaned -> PigStatus.PIGLET
                ageDays <= 70 -> PigStatus.STARTER
                ageDays <= 112 -> PigStatus.GROWER
                else -> PigStatus.FINISHER
            }
        } else if (pig.purposeEnum == PigPurpose.BREEDER) {
            if (ageDays > 182) { // 6 months
                if (pig.genderEnum == PigGender.FEMALE) {
                    return if (pig.hasFarrowed) PigStatus.SOW else PigStatus.GILT
                } else if (pig.genderEnum == PigGender.MALE) {
                    return if (pig.castrated == true) PigStatus.BARROW else PigStatus.BOAR
                }
            } else {
                return when {
                    ageDays <= 28 && !pig.weaned -> PigStatus.PIGLET
                    ageDays <= 70 -> PigStatus.STARTER
                    else -> PigStatus.GROWER
                }
            }
        }
        return PigStatus.UNKNOWN
    }

    private fun calculatePigStatus(pig: Pig): Pig {
        // High-priority states that are set manually via activities
        val s = pig.statusEnum
        if (s == PigStatus.PREGNANT || s == PigStatus.LACTATING || s == PigStatus.NURSING) {
            return pig
        }
        return pig.copy(status = getCalculatedStatus(pig).displayName)
    }

    fun getPig(pigId: String): StateFlow<Pig?> {
        val pigState = MutableStateFlow<Pig?>(null)
        val userId = activeFarmId ?: auth.currentUser?.uid ?: return pigState.asStateFlow()
        
        viewModelScope.launch {
            herdRepository.getPig(userId, pigId).collect { pig ->
                pigState.value = pig?.let { calculatePigStatus(it) }
            }
        }
        return pigState.asStateFlow()
    }

    fun fetchHealthRecords(pigId: String) {
        val userId = activeFarmId ?: auth.currentUser?.uid ?: return
        
        // Clear existing records to prevent showing stale data from previous pig
        _healthRecords.value = emptyList()
        
        viewModelScope.launch {
            herdRepository.getHealthRecords(userId, pigId).collect { records ->
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
                herdRepository.addHealthRecordWithLogic(
                    userId, pigId, record, trackHeat, checkPregnancy, pregnancyConfirmed, details
                )
            } catch (e: Exception) {
                _error.value = "Failed to add health record: ${e.message}"
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
                herdRepository.updateHealthRecordWithLogic(
                    userId, pigId, record, trackHeat, checkPregnancy, pregnancyConfirmed, details
                )
            } catch (e: Exception) {
                _error.value = "Failed to update health record: ${e.message}"
            }
        }
    }

    fun deleteHealthRecord(pigId: String, recordId: String) {
        val userId = activeFarmId ?: auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                herdRepository.deleteHealthRecord(userId, pigId, recordId)

                // Recalculate and update the pig's status based on the remaining history
                val pig = herdRepository.getPig(userId, pigId).first()
                if (pig != null) {
                    herdRepository.recalculatePigStatusFromHistory(userId, pigId, getCalculatedStatus(pig))
                }
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

                val pigWithCalculatedStatus = calculatePigStatus(pig)
                
                db.runTransaction { transaction ->
                    val pigRef = db.collection("users").document(userId).collection("pigs").document()
                    val pigWithId = pigWithCalculatedStatus.copy(id = pigRef.id)
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
        val userId = activeFarmId ?: auth.currentUser?.uid ?: return
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

            val totalCost = formData.purchasePrice.toDoubleOrNull() ?: 0.0
            if (formData.source == "Brought to farm" && totalCost > 0.0) {
                viewModelScope.launch {
                    try {
                        val financialRef = db.collection("users").document(userId).collection("financials").document()
                        val financialRecord = FinancialRecord(
                            id = financialRef.id,
                            date = DateUtils.formatToInternal(java.util.Date()),
                            type = "Expense",
                            category = "Livestock Purchase",
                            amount = totalCost,
                            description = "Purchase of batch of pigs (Qty: ${validMales.size + validFemales.size})",
                        )
                        financialRef.set(financialRecord).await()
                    } catch (e: Exception) {
                        _error.value = "Failed to record financial transaction: ${e.message}"
                    }
                }
            }
        }
    }

    fun updatePig(pig: Pig) {
        val userId = activeFarmId ?: auth.currentUser?.uid ?: return
        if (pig.id.isEmpty()) return
        viewModelScope.launch {
            try {
                val currentPig = herdRepository.getPig(userId, pig.id).first()
                val oldWeight = currentPig?.weight ?: 0.0
                
                val updatedPig = calculatePigStatus(pig)
                herdRepository.updatePig(userId, updatedPig)
                
                // If weight was updated manually, add a history record for it to clear warnings
                if (updatedPig.weight != oldWeight && updatedPig.weight > 0) {
                     val record = HealthRecord(
                        date = DateUtils.formatToInternal(Date()),
                        type = "Weight Check",
                        description = "Weight updated manually in pig details",
                    )
                    herdRepository.addHealthRecordWithLogic(userId, pig.id, record)
                }
            } catch (_: Exception) {
                _error.value = "Failed to update pig"
            }
        }
    }

    fun updatePigWeight(pigId: String, weight: Double) {
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
                herdRepository.archivePig(userId, pig.id, archivedPig)
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
                herdRepository.deletePig(userId, pig.id)
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
                
                tasks.forEach { doc ->
                    val task = doc.toObject(TaskItem::class.java)
                    if (task.pigIds.size <= 1) {
                        taskRepository.deleteTask(userId, doc.id)
                    } else {
                        val newPigIds = task.pigIds.filter { it != pigId }
                        taskRepository.updateTask(userId, task.copy(pigIds = newPigIds))
                    }
                }
            } catch (e: Exception) {
                Log.e("HerdViewModel", "Task cleanup error", e)
            }
        }
    }

    // Statistics for the Ribbon
    val stats = pigs.map { allPigs ->
        val breeders = allPigs.filter { it.purposeEnum == PigPurpose.BREEDER }
        val porkers = allPigs.filter { it.purposeEnum == PigPurpose.PORKER }

        mapOf(
            "total" to allPigs.size,
            "breeders_count" to breeders.size,
            "porkers_count" to porkers.size,
            "piglets_total" to allPigs.count { it.statusEnum == PigStatus.PIGLET },
            
            // Breeder specific categories
            "breeders_piglets" to breeders.count { it.statusEnum == PigStatus.PIGLET },
            "breeders_starter" to breeders.count { it.statusEnum == PigStatus.STARTER },
            "breeders_grower" to breeders.count { it.statusEnum == PigStatus.GROWER },
            "boars" to breeders.count { it.statusEnum == PigStatus.BOAR },
            "gilts" to breeders.count { it.statusEnum == PigStatus.GILT },
            "Pregnant" to breeders.count { it.statusEnum == PigStatus.PREGNANT },
            "Lactating" to breeders.count { it.statusEnum == PigStatus.LACTATING },
            "sows" to breeders.count { it.statusEnum == PigStatus.SOW },

            "Finisher" to porkers.count { it.statusEnum == PigStatus.FINISHER },
            "Grower" to porkers.count { it.statusEnum == PigStatus.GROWER },
            "Starter" to porkers.count { it.statusEnum == PigStatus.STARTER },
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
    }
}
