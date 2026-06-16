package com.example.smartswine.model

import androidx.annotation.Keep

@Keep
data class FeedInventoryTransaction(
    val id: String = "",
    val itemId: String = "",
    val itemName: String = "",
    val type: String = "Restock", // "Restock" or "Usage"
    val quantity: Double = 0.0,   // Change amount in the transaction's display unit (bags or kg)
    val unit: String = "bags",    // The unit of the adjustment
    val cost: Double = 0.0,       // Total cost if type is "Restock"
    val date: String = "",        // ISO date string
    val notes: String = ""        // Additional details
)
