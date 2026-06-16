package com.example.smartswine.model

import androidx.annotation.Keep

@Keep
data class TaskItem(
    val id: String = "",
    val name: String = "",
    val date: String = "",
    val notes: String = "",
    val completed: Boolean = false,
    val pigIds: List<String> = emptyList(),
    val healthRecordIds: List<String> = emptyList(),
)
