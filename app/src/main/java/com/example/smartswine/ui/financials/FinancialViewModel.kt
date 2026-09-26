package com.example.smartswine.ui.financials

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartswine.data.FinancialRepository
import com.example.smartswine.data.HerdRepository
import com.example.smartswine.model.FinancialRecord
import com.example.smartswine.model.Pig
import com.example.smartswine.utils.DateUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import com.example.smartswine.utils.TierLimiter
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class FinancialViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val financialRepository = FinancialRepository(db)
    private val herdRepository = HerdRepository(db)

    // Active Farm ID for multi-user support
    private var activeFarmId: String? = null
    private var recordsJob: Job? = null
    private var pigsJob: Job? = null

    fun setActiveFarmId(uid: String?) {
        if (activeFarmId != uid) {
            activeFarmId = uid
            if (uid == null) {
                recordsJob?.cancel()
                recordsJob = null
                pigsJob?.cancel()
                pigsJob = null
                _records.value = emptyList()
                _allPigs.value = emptyList()
            } else {
                observeRecords()
                observePigs()
            }
        }
    }

    private val _records = MutableStateFlow<List<FinancialRecord>>(emptyList())
    val records: StateFlow<List<FinancialRecord>> = _records.asStateFlow()

    private val _allPigs = MutableStateFlow<List<Pig>>(emptyList())
    val allPigs: StateFlow<List<Pig>> = _allPigs.asStateFlow()

    private val _isLoading = MutableStateFlow(value = false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun clearError() {
        _error.value = null
    }

    init {
        observeRecords()
        observePigs()
    }

    private fun observeRecords() {
        val userId = activeFarmId ?: auth.currentUser?.uid
        if (userId == null) {
            _records.value = emptyList()
            return
        }
        recordsJob?.cancel()
        recordsJob = viewModelScope.launch {
            financialRepository.getFinancialRecords(userId)
                .catch { e -> Log.w("FinancialViewModel", "Error collecting records: ${e.message}") }
                .collect {
                    _records.value = it
                }
        }
    }

    private fun observePigs() {
        val userId = activeFarmId ?: auth.currentUser?.uid
        if (userId == null) {
            _allPigs.value = emptyList()
            return
        }
        pigsJob?.cancel()
        pigsJob = viewModelScope.launch {
            herdRepository.getAllPigs(userId)
                .catch { e -> Log.w("FinancialViewModel", "Error collecting pigs: ${e.message}") }
                .collect {
                    _allPigs.value = it
                }
        }
    }

    fun addRecord(record: FinancialRecord) {
        val userId = activeFarmId ?: auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                try {
                    val userDoc = db.collection("users").document(userId).get().await()
                    val isPremium = userDoc.getBoolean("isPremium") == true
                    if (!isPremium) {
                        if (_records.value.size >= TierLimiter.FREE_MAX_FINANCIAL_RECORDS) {
                            _error.value = "Financial record limit of ${TierLimiter.FREE_MAX_FINANCIAL_RECORDS} reached for free tier. Please upgrade to add more."
                            return@launch
                        }
                    }
                } catch (e: Exception) {
                    Log.w("FinancialViewModel", "Offline/error checking tier limit: ${e.message}")
                }

                financialRepository.addFinancialRecord(userId, record)
            } catch (_: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun archiveSoldPig(pigId: String) {
        val userId = activeFarmId ?: auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                // Get the pig first to create the archived version
                // For simplicity, we use the already loaded list
                val pig = _allPigs.value.find { it.id == pigId }
                if (pig != null) {
                    val archivedPig = pig.copy(
                        status = "Archived (Sold)",
                        location = "Archived",
                        notes = pig.notes + "\nArchived on: ${DateUtils.getCurrentDateDisplay()} Reason: Sold",
                    )
                    herdRepository.archivePig(userId, pigId, archivedPig)
                }
            } catch (_: Exception) {
                // Handle error
            }
        }
    }

    fun deleteRecord(recordId: String) {
        val userId = activeFarmId ?: auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                financialRepository.deleteFinancialRecord(userId, recordId)
            } catch (_: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}
