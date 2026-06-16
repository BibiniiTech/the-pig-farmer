package com.example.smartswine.model

import androidx.annotation.Keep

@Keep
data class HealthRecord(
    val id: String = "",
    val date: String = "",
    val type: String = "", // e.g., Vaccination, Treatment, Checkup
    val description: String = "",
    val medication: String = "",
    val cost: Double = 0.0,
    val taskId: String? = null,
)
