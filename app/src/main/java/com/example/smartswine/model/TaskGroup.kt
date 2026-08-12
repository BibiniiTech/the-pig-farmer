package com.example.smartswine.model

import androidx.annotation.Keep

@Keep
data class TaskGroup(
    val activity: String,
    val target: String,
    val date: String,
    val isOverdue: Boolean,
    val originalTasks: List<TaskItem>
)
