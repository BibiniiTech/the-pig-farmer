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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FinancialViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val financialRepository = FinancialRepository(db)
    private val herdRepository = HerdRepository(db)

    // Active Farm ID for multi-user support
    private var activeFarmId: String? = null

    fun setActiveFarmId(uid: String) {
        if (activeFarmId != uid) {
            activeFarmId = uid
            observeRecords()
            observePigs()
        }
    }

    private val _records = MutableStateFlow<List<FinancialRecord>>(emptyList())
    val records: StateFlow<List<FinancialRecord>> = _records.asStateFlow()

    private val _allPigs = MutableStateFlow<List<Pig>>(emptyList())
    val allPigs: StateFlow<List<Pig>> = _allPigs.asStateFlow()

    private val _isLoading = MutableStateFlow(value = false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        observeRecords()
        observePigs()
    }

    private fun observeRecords() {
        val userId = activeFarmId ?: auth.currentUser?.uid ?: return
        viewModelScope.launch {
            financialRepository.getFinancialRecords(userId).collect {
                _records.value = it
            }
        }
    }

    private fun observePigs() {
        val userId = activeFarmId ?: auth.currentUser?.uid ?: return
        viewModelScope.launch {
            herdRepository.getAllPigs(userId).collect {
                _allPigs.value = it
            }
        }
    }

    fun addRecord(record: FinancialRecord) {
        val userId = activeFarmId ?: auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
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
