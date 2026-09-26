package com.example.smartswine.model

import androidx.annotation.Keep

enum class NotificationCategory {
    ALL,
    PROCEDURES,
    WEIGHT,
    STOCK
}

@Keep
sealed class FarmAlert {
    abstract val id: String
    abstract val title: String

    data class TaskAlert(
        override val id: String,
        override val title: String,
        val activity: String,
        val target: String,
        val date: String,
        val isOverdue: Boolean,
        val originalTasks: List<TaskItem>
    ) : FarmAlert()

    data class WeightAlert(
        override val id: String,
        override val title: String,
        val pig: Pig,
        val daysSinceLastWeigh: Long,
        val lastWeightFormatted: String
    ) : FarmAlert()

    data class LowStockAlert(
        override val id: String,
        override val title: String,
        val item: FeedInventoryItem,
        val currentQty: Double,
        val minThreshold: Double,
        val unit: String
    ) : FarmAlert()
}
