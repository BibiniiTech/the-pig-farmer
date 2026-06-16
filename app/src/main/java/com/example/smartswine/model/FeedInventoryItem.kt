package com.example.smartswine.model

import androidx.annotation.Keep

@Keep
data class FeedInventoryItem(
    val id: String = "",
    val name: String = "",
    val feedType: String = "",     // e.g. Starter, Grower, Finisher, Sow, etc.
    val quantity: Double = 0.0,    // Total quantity in stock (expressed in the selected unit: bags or kg)
    val unit: String = "bags",     // "bags" or "kg"
    val unitWeight: Double = 50.0, // kg per bag
    val minThreshold: Double = 5.0,// Low stock threshold in the selected unit
    val costPerUnit: Double = 0.0, // Last cost per unit
    val lastUpdated: String = ""
)
