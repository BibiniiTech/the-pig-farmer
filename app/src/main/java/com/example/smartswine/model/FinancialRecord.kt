package com.example.smartswine.model

import androidx.annotation.Keep

@Keep
data class FinancialRecord(
    val id: String = "",
    val date: String = "",
    val type: String = "Expense", // Income or Expense
    val category: String = "", // Feed, Medicine, Sale, Equipment, etc.
    val amount: Double = 0.0,
    val description: String = "",
    val pigId: String? = null, // Optional: link to a specific pig
)
