package com.example.smartswine.ui.financials

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartswine.model.FinancialRecord
import com.example.smartswine.model.Pig
import com.example.smartswine.utils.DateUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FinancialViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Active Farm ID for multi-user support
    private var activeFarmId: String? = null
    private var recordsListener: ListenerRegistration? = null
    private var pigsListener: ListenerRegistration? = null
    private var archivedPigsListener: ListenerRegistration? = null

    fun setActiveFarmId(uid: String) {
        if (activeFarmId != uid) {
            activeFarmId = uid
            fetchRecords()
            fetchAllPigs()
        }
    }

    private val _records = MutableStateFlow<List<FinancialRecord>>(emptyList())
    val records: StateFlow<List<FinancialRecord>> = _records.asStateFlow()

    private val _allPigs = MutableStateFlow<List<Pig>>(emptyList())
    val allPigs: StateFlow<List<Pig>> = _allPigs.asStateFlow()

    private val _isLoading = MutableStateFlow(value = false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        fetchRecords()
        fetchAllPigs()
    }

    private fun fetchRecords() {
        val userId = activeFarmId ?: auth.currentUser?.uid ?: return
        recordsListener?.remove()
        recordsListener = db.collection("users").document(userId)
            .collection("financials")
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (auth.currentUser == null) {
                    recordsListener?.remove()
                    recordsListener = null
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val recordList = snapshot.toObjects(FinancialRecord::class.java)
                    _records.value = recordList
                }
            }
    }

    private fun fetchAllPigs() {
        val userId = activeFarmId ?: auth.currentUser?.uid ?: return
        pigsListener?.remove()
        pigsListener = db.collection("users").document(userId).collection("pigs")
            .addSnapshotListener { _, _ ->
                if (auth.currentUser == null) {
                    pigsListener?.remove()
                    pigsListener = null
                    return@addSnapshotListener
                }
                syncAllPigs()
            }
        
        archivedPigsListener?.remove()
        archivedPigsListener = db.collection("users").document(userId).collection("archived_pigs")
            .addSnapshotListener { _, _ ->
                if (auth.currentUser == null) {
                    archivedPigsListener?.remove()
                    archivedPigsListener = null
                    return@addSnapshotListener
                }
                syncAllPigs()
            }
    }

    private fun syncAllPigs() {
        val userId = activeFarmId ?: auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val activePigsTask = db.collection("users").document(userId).collection("pigs").get()
                val archivedPigsTask = db.collection("users").document(userId).collection("archived_pigs").get()

                val activePigs = activePigsTask.await().toObjects(Pig::class.java)
                val archivedPigs = archivedPigsTask.await().toObjects(Pig::class.java)

                _allPigs.value = activePigs + archivedPigs
            } catch (_: Exception) {}
        }
    }

    fun addRecord(record: FinancialRecord) {
        val userId = activeFarmId ?: auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val ref = db.collection("users").document(userId)
                    .collection("financials").document()
                val newRecord = record.copy(id = ref.id)
                ref.set(newRecord).await()
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
                db.runTransaction { transaction ->
                    val pigRef = db.collection("users").document(userId).collection("pigs").document(pigId)
                    val archiveRef = db.collection("users").document(userId).collection("archived_pigs").document(pigId)
                    
                    val snapshot = transaction[pigRef]
                    val pig = snapshot.toObject(Pig::class.java)
                    
                    if (pig != null) {
                        val archivedPig = pig.copy(
                            status = "Archived (Sold)",
                            location = "Archived",
                            notes = pig.notes + "\nArchived on: ${DateUtils.getCurrentDateDisplay()} Reason: Sold",
                        )
                        transaction[archiveRef] = archivedPig
                        transaction.delete(pigRef)
                    }
                }.await()
                fetchAllPigs() // Refresh list
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
                db.collection("users").document(userId)
                    .collection("financials").document(recordId)
                    .delete().await()
            } catch (_: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        recordsListener?.remove()
        pigsListener?.remove()
        archivedPigsListener?.remove()
    }
}
