package com.example.smartswine.model

import androidx.annotation.Keep

@Keep
data class HealthRecord(
    val id: String = "",
    val date: String = "",
    val type: String = "", // e.g., Vaccination, Treatment, Checkup
    val description: String = "",
    val medication: String = "",
    val dosage: String = "",
    val cost: Double = 0.0,
    val taskId: String? = null,
    val weight: Double = 0.0,
    val stillbornCount: Int = 0,
    val mummiesCount: Int = 0,
    val litterBirthWeightKg: Double = 0.0,
    val withdrawalPeriodDays: Int = 0,
    val safeSlaughterDate: String = "",
)
