package com.example.smartswine.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartswine.model.TaskItem
import com.example.smartswine.utils.DateUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.time.Duration.Companion.milliseconds

class DashboardViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    // Active Farm ID for multi-user support
    private var activeFarmId: String? = null
    private var tasksListener: ListenerRegistration? = null

    fun setActiveFarmId(uid: String) {
        if (activeFarmId != uid) {
            activeFarmId = uid
            fetchTasks()
        }
    }

    private val _tasks = MutableStateFlow<List<TaskItem>>(emptyList())
    val tasks = _tasks.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    init {
        fetchTasks()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            fetchTasks()
            // Simulate some network delay for better UX if it's too fast
            kotlinx.coroutines.delay(1000.milliseconds)
            _isRefreshing.value = false
        }
    }

    private fun fetchTasks() {
        val userId = activeFarmId ?: auth.currentUser?.uid ?: return
        tasksListener?.remove()
        tasksListener = db.collection("users").document(userId).collection("tasks")
            .whereEqualTo("completed", false)
            .addSnapshotListener { snapshot, e ->
                if (auth.currentUser == null) {
                    tasksListener?.remove()
                    tasksListener = null
                    return@addSnapshotListener
                }
                if (e != null) {
                    _error.value = "Error fetching tasks: ${e.message}"
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val taskList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(TaskItem::class.java)?.copy(id = doc.id)
                    }
                    _tasks.value = taskList
                }
            }
    }

    fun completeTask(task: TaskItem) {
        val userId = activeFarmId ?: auth.currentUser?.uid ?: return
        if (task.id.isEmpty()) return
        viewModelScope.launch {
            try {
                db.collection("users").document(userId).collection("tasks")
                    .document(task.id)
                    .update("completed", true)
                    .await()
            } catch (e: Exception) {
                _error.value = "Failed to complete task: ${e.message}"
            }
        }
    }

    fun deleteTask(task: TaskItem) {
        val userId = activeFarmId ?: auth.currentUser?.uid ?: return
        if (task.id.isEmpty()) return
        viewModelScope.launch {
            try {
                db.runTransaction { transaction ->
                    // 1. Delete associated health records from all involved pigs
                    task.pigIds.forEach { pigId ->
                        val healthRecordsRef = db.collection("users").document(userId)
                            .collection("pigs").document(pigId).collection("health_records")
                        
                        task.healthRecordIds.forEach { hrId ->
                            transaction.delete(healthRecordsRef.document(hrId))
                        }
                    }

                    // 2. Delete the task itself
                    transaction.delete(db.collection("users").document(userId).collection("tasks").document(task.id))
                }.await()
            } catch (e: Exception) {
                _error.value = "Failed to delete task and associated records: ${e.message}"
            }
        }
    }

    @Suppress("unused")
    fun updateTask(task: TaskItem) {
        val userId = activeFarmId ?: auth.currentUser?.uid ?: return
        if (task.id.isEmpty()) return
        viewModelScope.launch {
            try {
                db.runTransaction { transaction ->
                    // 1. Update the task itself
                    transaction.set(db.collection("users").document(userId).collection("tasks").document(task.id), task)

                    // 2. Update associated health records for each pig
                    task.pigIds.forEach { pigId ->
                        task.healthRecordIds.forEach { hrId ->
                            val hrRef = db.collection("users").document(userId)
                                .collection("pigs").document(pigId)
                                .collection("health_records").document(hrId)
                            
                            // Check if it exists in active pigs
                            // Note: Transactional get is required if we want to be safe, 
                            // but for simplicity and common case, we just update it.
                            transaction.update(hrRef, 
                                "date", DateUtils.convertToDisplayDate(task.date),
                                "description", task.notes,
                                "type", task.name.substringBefore(": ").trim()
                            )
                        }
                    }
                }.await()
            } catch (e: Exception) {
                _error.value = "Failed to update task and history: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        tasksListener?.remove()
    }
}
