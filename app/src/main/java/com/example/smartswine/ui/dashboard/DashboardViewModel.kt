package com.example.smartswine.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartswine.data.TaskRepository
import com.example.smartswine.data.HerdRepository
import com.example.smartswine.model.TaskItem
import com.example.smartswine.model.Pig
import com.example.smartswine.model.TaskGroup
import com.example.smartswine.utils.DateUtils
import com.example.smartswine.utils.AppLanguage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.time.Duration.Companion.milliseconds

class DashboardViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val taskRepository = TaskRepository(db)
    private val herdRepository = HerdRepository(db)
    
    // Active Farm ID for multi-user support
    private var activeFarmId: String? = null

    private var tasksJob: Job? = null
    private var pigsJob: Job? = null

    fun setActiveFarmId(uid: String) {
        if (activeFarmId != uid) {
            activeFarmId = uid
            observeTasks()
        }
    }

    private val _tasks = MutableStateFlow<List<TaskItem>>(emptyList())
    val tasks = _tasks.asStateFlow()

    private val _pigs = MutableStateFlow<List<Pig>>(emptyList())
    val pigs = _pigs.asStateFlow()

    private val _language = MutableStateFlow(AppLanguage.ENGLISH.code)
    val language = _language.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    val groupedTasks: StateFlow<List<TaskGroup>> = combine(tasks, pigs, language) { tasks, allPigs, langCode ->
        val locale = AppLanguage.entries.find { it.code == langCode }?.toLocale() ?: AppLanguage.ENGLISH.toLocale()
        
        tasks.filter { task ->
            val activity = task.name.substringBefore(": ", "")
            if (activity.contains("Pregnancy", ignoreCase = true) || 
                activity.contains("Farrowing", ignoreCase = true) || 
                activity.contains("Heat", ignoreCase = true)) {
                val identifier = task.name.substringAfter(": ", "").replace("Pig ", "").trim()
                if (identifier.isNotEmpty()) {
                    val pig = allPigs.find { it.id == identifier } 
                        ?: allPigs.find { it.tagNumber == identifier }
                    // Filter out males for these female-specific activities
                    ((pig == null) || (pig.gender.equals("Female", ignoreCase = true)))
                } else true
            } else true
        }.groupBy {
            val activity = it.name.substringBefore(": ")
            activity + it.date
        }.values.asSequence().map { group ->
            val first = group.first()
            val activity = first.name.substringBefore(": ")
            val isMultiple = group.size > 1
            val target = if (isMultiple) {
                val tags = group.asSequence().map { 
                    val rawIdentifier = it.name.substringAfter(": ", "").replace("Pig ", "").trim()
                    if (rawIdentifier.isEmpty()) return@map "General"
                    
                    val resolvedTag = allPigs.find { p -> p.id == rawIdentifier }?.tagNumber 
                        ?: allPigs.find { p -> p.tagNumber == rawIdentifier }?.tagNumber 
                        ?: rawIdentifier
                    
                    resolvedTag
                }.distinct().toList()
                tags.joinToString(", ")
            } else {
                val rawTarget = first.name.substringAfter(": ", "").replace("Pig ", "").trim().ifEmpty { "General" }
                if (rawTarget == "General") {
                    rawTarget
                } else {
                    allPigs.find { it.id == rawTarget }?.tagNumber 
                        ?: allPigs.find { it.tagNumber == rawTarget }?.tagNumber 
                        ?: rawTarget
                }
            }

            val isOverdue = DateUtils.isTaskOverdue(first.date, locale)

            TaskGroup(
                activity = activity,
                target = target,
                date = DateUtils.convertToTaskDate(first.date, locale),
                isOverdue = isOverdue,
                originalTasks = group,
            )
        }.sortedByDescending { it.isOverdue }.toList()
    }.flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        observeTasks()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            observeTasks()
            // Simulate some network delay for better UX if it's too fast
            kotlinx.coroutines.delay(1000.milliseconds)
            _isRefreshing.value = false
        }
    }

    private fun observeTasks() {
        val userId = activeFarmId ?: auth.currentUser?.uid ?: return
        
        tasksJob?.cancel()
        tasksJob = viewModelScope.launch {
            taskRepository.getUncompletedTasks(userId).collect {
                _tasks.value = it
            }
        }

        pigsJob?.cancel()
        pigsJob = viewModelScope.launch {
            herdRepository.getPigs(userId).collect {
                _pigs.value = it
            }
        }
    }

    fun completeTask(task: TaskItem) {
        val userId = activeFarmId ?: auth.currentUser?.uid ?: return
        if (task.id.isEmpty()) return
        viewModelScope.launch {
            try {
                taskRepository.completeTask(userId, task.id)
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
                        task.healthRecordIds.forEach { hrId ->
                            transaction.delete(
                                db.collection("users").document(userId)
                                    .collection("pigs").document(pigId)
                                    .collection("health_records").document(hrId)
                            )
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
                taskRepository.updateTask(userId, task)
            } catch (e: Exception) {
                _error.value = "Failed to update task: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun setLanguage(langCode: String) {
        _language.value = langCode
    }

    override fun onCleared() {
        super.onCleared()
        tasksJob?.cancel()
        pigsJob?.cancel()
    }
}
