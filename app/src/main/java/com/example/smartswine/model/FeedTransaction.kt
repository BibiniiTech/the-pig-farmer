package com.example.smartswine.model

import androidx.annotation.Keep

@Keep
data class FeedTransaction(
    val id: String = "",
    val ingredientId: String = "",
    val type: String = "Addition", // "Addition" or "Usage"
    val quantity: Double = 0.0,    // Quantity added or used
    val date: String = "",         // Date string
    val costPerKg: Double = 0.0    // Cost at the time of transaction
)
