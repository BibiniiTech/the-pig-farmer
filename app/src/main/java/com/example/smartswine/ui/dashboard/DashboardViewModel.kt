package com.example.smartswine.ui.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartswine.data.FeedRepository
import com.example.smartswine.data.HerdRepository
import com.example.smartswine.data.TaskRepository
import com.example.smartswine.model.*
import com.example.smartswine.utils.AppLanguage
import com.example.smartswine.utils.DateUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

class DashboardViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val taskRepository = TaskRepository(db)
    private val herdRepository = HerdRepository(db)
    private val feedRepository = FeedRepository()
    
    // Active Farm ID for multi-user support
    private var activeFarmId: String? = null

    private var tasksJob: Job? = null
    private var pigsJob: Job? = null
    private var feedJob: Job? = null

    fun setActiveFarmId(uid: String?) {
        if (activeFarmId != uid) {
            activeFarmId = uid
            feedRepository.setActiveFarmId(uid)
            if (uid == null) {
                tasksJob?.cancel()
                tasksJob = null
                pigsJob?.cancel()
                pigsJob = null
                feedJob?.cancel()
                feedJob = null
                _tasks.value = emptyList()
                _pigs.value = emptyList()
                _feedInventoryItems.value = emptyList()
            } else {
                observeTasks()
            }
        }
    }

    private val _tasks = MutableStateFlow<List<TaskItem>>(emptyList())
    val tasks = _tasks.asStateFlow()

    private val _pigs = MutableStateFlow<List<Pig>>(emptyList())
    val pigs = _pigs.asStateFlow()

    private val _feedInventoryItems = MutableStateFlow<List<FeedInventoryItem>>(emptyList())
    val feedInventoryItems = _feedInventoryItems.asStateFlow()

    private val _language = MutableStateFlow(AppLanguage.ENGLISH.code)
    val language = _language.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    val groupedTasks: StateFlow<List<TaskGroup>> = combine(tasks, pigs, language) { tasks, allPigs, langCode ->
        val locale = AppLanguage.entries.find { it.code == langCode }?.toLocale() ?: AppLanguage.ENGLISH.toLocale()
        val now = System.currentTimeMillis()
        
        tasks.filter { task ->
            if (task.isArchived) return@filter false
            if (task.snoozeUntil != null && task.snoozeUntil > now) return@filter false
            
            // Only show tasks that are due, overdue, or upcoming within 2 days (for meat withdrawal, only when cleared on/after date)
            val isWithdrawal = task.name.contains("Withdrawal", ignoreCase = true)
            if (!DateUtils.isTaskDueOrUpcoming(task.date, isWithdrawal = isWithdrawal, maxUpcomingDays = 2, locale = locale)) {
                return@filter false
            }

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
                    val rawIdentifier = it.name.substringAfter(": ", "").replace("Pigs ", "").replace("Pig ", "").trim()
                    if (rawIdentifier.isEmpty()) return@map "General"
                    
                    val resolvedTag = allPigs.find { p -> p.id == rawIdentifier }?.tagNumber 
                        ?: allPigs.find { p -> p.tagNumber == rawIdentifier }?.tagNumber 
                        ?: rawIdentifier
                    
                    resolvedTag
                }.distinct().toList()
                tags.joinToString(", ")
            } else {
                val rawTarget = first.name.substringAfter(": ", "").replace("Pigs ", "").replace("Pig ", "").trim().ifEmpty { "General" }
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

    val taskAlerts: StateFlow<List<FarmAlert.TaskAlert>> = groupedTasks.map { list ->
        list.map { group ->
            FarmAlert.TaskAlert(
                id = group.originalTasks.firstOrNull()?.id ?: (group.activity + group.date),
                title = group.activity,
                activity = group.activity,
                target = group.target,
                date = group.date,
                isOverdue = group.isOverdue,
                originalTasks = group.originalTasks
            )
        }
    }.flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _snoozedWeightAlerts = MutableStateFlow<Map<String, Long>>(emptyMap())
    private val _dismissedWeightAlerts = MutableStateFlow<Set<String>>(emptySet())

    private val _snoozedStockAlerts = MutableStateFlow<Map<String, Long>>(emptyMap())
    private val _dismissedStockAlerts = MutableStateFlow<Set<String>>(emptySet())

    val weightAlerts: StateFlow<List<FarmAlert.WeightAlert>> = combine(pigs, _snoozedWeightAlerts, _dismissedWeightAlerts) { allPigs, snoozed, dismissed ->
        val now = System.currentTimeMillis()
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        allPigs.mapNotNull { pig ->
            if (dismissed.contains(pig.id)) return@mapNotNull null
            val snoozeUntil = snoozed[pig.id] ?: 0L
            if (snoozeUntil > now) return@mapNotNull null

            val lastWeight = if (pig.lastWeightDate.isNotBlank()) {
                pig.lastWeightDate
            } else if (pig.weight <= 0.0) {
                // Pig has never been weighed; check if overdue since birth
                pig.birthDate
            } else {
                // Pig already has a weight recorded; do not falsely trigger based on birthDate
                return@mapNotNull null
            }
            if (lastWeight.isBlank()) return@mapNotNull null
            
            val lastDate = DateUtils.parseSwineDate(lastWeight)
                ?: DateUtils.parseInternal(lastWeight)
                ?: DateUtils.parseProduction(lastWeight)
                ?: return@mapNotNull null

            val diff = today.timeInMillis - lastDate.time
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            if (days >= 30) {
                FarmAlert.WeightAlert(
                    id = pig.id,
                    title = "Weight Check Overdue",
                    pig = pig,
                    daysSinceLastWeigh = days,
                    lastWeightFormatted = lastWeight
                )
            } else null
        }.sortedByDescending { it.daysSinceLastWeigh }
    }.flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stockAlerts: StateFlow<List<FarmAlert.LowStockAlert>> = combine(_feedInventoryItems, _snoozedStockAlerts, _dismissedStockAlerts) { items, snoozed, dismissed ->
        val now = System.currentTimeMillis()
        items.filter { item ->
            item.quantity <= item.minThreshold && item.minThreshold > 0 &&
            !dismissed.contains(item.id) &&
            (snoozed[item.id] ?: 0L) <= now
        }.map { item ->
            FarmAlert.LowStockAlert(
                id = item.id,
                title = "Low Feed Stock",
                item = item,
                currentQty = item.quantity,
                minThreshold = item.minThreshold,
                unit = item.unit
            )
        }.sortedBy { it.currentQty / (it.minThreshold.takeIf { t -> t > 0 } ?: 1.0) }
    }.flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalNotificationCount: StateFlow<Int> = combine(taskAlerts, weightAlerts, stockAlerts) { t, w, s ->
        t.size + w.size + s.size
    }.flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        observeTasks()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            observeTasks()
            kotlinx.coroutines.delay(1000.milliseconds)
            _isRefreshing.value = false
        }
    }

    private fun observeTasks() {
        val userId = activeFarmId ?: auth.currentUser?.uid
        if (userId == null) {
            _tasks.value = emptyList()
            _pigs.value = emptyList()
            _feedInventoryItems.value = emptyList()
            return
        }
        
        tasksJob?.cancel()
        tasksJob = viewModelScope.launch {
            taskRepository.getUncompletedTasks(userId)
                .catch { e -> Log.w("DashboardViewModel", "Error collecting tasks: ${e.message}") }
                .collect {
                    _tasks.value = it
                }
        }

        pigsJob?.cancel()
        pigsJob = viewModelScope.launch {
            herdRepository.getPigs(userId)
                .catch { e -> Log.w("DashboardViewModel", "Error collecting pigs: ${e.message}") }
                .collect {
                    _pigs.value = it
                }
        }

        feedJob?.cancel()
        feedJob = viewModelScope.launch {
            feedRepository.getAllFeedInventoryItems()
                .catch { e -> Log.w("DashboardViewModel", "Error collecting feed inventory: ${e.message}") }
                .collect {
                    _feedInventoryItems.value = it
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
                val batch = db.batch()
                // 1. Delete associated health records from all involved pigs
                task.pigIds.forEach { pigId ->
                    task.healthRecordIds.forEach { hrId ->
                        batch.delete(
                            db.collection("users").document(userId)
                                .collection("pigs").document(pigId)
                                .collection("health_records").document(hrId)
                        )
                    }
                }

                // 2. Delete the task itself
                batch.delete(db.collection("users").document(userId).collection("tasks").document(task.id))
                batch.commit().await()
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

    fun snoozeTask(task: TaskItem, hours: Int) {
        val userId = activeFarmId ?: auth.currentUser?.uid ?: return
        if (task.id.isEmpty()) return
        val snoozeMillis = System.currentTimeMillis() + (hours * 3600 * 1000L)
        viewModelScope.launch {
            try {
                taskRepository.updateTask(userId, task.copy(snoozeUntil = snoozeMillis))
            } catch (e: Exception) {
                _error.value = "Failed to snooze reminder: ${e.message}"
            }
        }
    }

    fun snoozeTasks(tasks: List<TaskItem>, hours: Int) {
        tasks.forEach { snoozeTask(it, hours) }
    }

    fun deleteTaskForPig(tasks: List<TaskItem>, pigIdentifier: String) {
        val userId = activeFarmId ?: auth.currentUser?.uid ?: return
        val cleanId = pigIdentifier.trim().lowercase()
        viewModelScope.launch {
            try {
                for (task in tasks) {
                    val part = task.name.substringAfter(": ", "").replace("Pigs ", "").replace("Pig ", "").trim()
                    val identifiers = part.split(",").map { it.trim().lowercase() }
                    val matches = identifiers.any { it == cleanId } || task.pigIds.any { it.lowercase() == cleanId }
                    if (matches) {
                        if (identifiers.size <= 1) {
                            deleteTask(task)
                        } else {
                            val remaining = identifiers.filter { it != cleanId }
                            val newName = task.name.substringBefore(": ") + ": " + remaining.joinToString(", ")
                            val newPigIds = task.pigIds.filter { it.lowercase() != cleanId }
                            taskRepository.updateTask(userId, task.copy(name = newName, pigIds = newPigIds))
                        }
                    }
                }
            } catch (e: Exception) {
                _error.value = "Failed to remove reminder for pig: ${e.message}"
            }
        }
    }

    fun snoozeWeightAlert(pigId: String, days: Int) {
        val snoozeUntil = System.currentTimeMillis() + (days * 24 * 3600 * 1000L)
        _snoozedWeightAlerts.value = _snoozedWeightAlerts.value + (pigId to snoozeUntil)
    }

    fun dismissWeightAlert(pigId: String) {
        _dismissedWeightAlerts.value = _dismissedWeightAlerts.value + pigId
    }

    fun snoozeStockAlert(itemId: String, hours: Int) {
        val snoozeUntil = System.currentTimeMillis() + (hours * 3600 * 1000L)
        _snoozedStockAlerts.value = _snoozedStockAlerts.value + (itemId to snoozeUntil)
    }

    fun dismissStockAlert(itemId: String) {
        _dismissedStockAlerts.value = _dismissedStockAlerts.value + itemId
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
        feedJob?.cancel()
    }
}
