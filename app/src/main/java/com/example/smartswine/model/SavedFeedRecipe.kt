package com.example.smartswine.model

import androidx.annotation.Keep

@Keep
data class SavedFeedRecipe(
    val id: String = "",
    val name: String = "",
    val stage: String = "",
    val dateCreated: String = "",
    val ingredients: Map<String, Double> = emptyMap(),
    val isPercentage: Boolean = true,
    val costPerKg: Double = 0.0,
    val targetBatchKg: Double = 1000.0,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
