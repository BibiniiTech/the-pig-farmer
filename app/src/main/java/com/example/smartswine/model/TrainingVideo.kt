package com.example.smartswine.model

import androidx.annotation.Keep

@Keep
data class TrainingVideo(
    val id: String = "",
    val title: String = "",
    val youtubeId: String = "",
    val createdAt: Long = 0L
)
