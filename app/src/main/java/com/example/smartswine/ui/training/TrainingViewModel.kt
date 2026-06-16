package com.example.smartswine.ui.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartswine.model.TrainingVideo
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TrainingViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _videos = MutableStateFlow<List<TrainingVideo>>(emptyList())
    val videos: StateFlow<List<TrainingVideo>> = _videos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var videosListener: ListenerRegistration? = null

    init {
        fetchVideos()
    }

    fun fetchVideos() {
        _isLoading.value = true
        videosListener?.remove()
        videosListener = db.collection("training_videos")
            .addSnapshotListener { snapshot, error ->
                _isLoading.value = false
                if (error != null) {
                    _error.value = error.message
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(TrainingVideo::class.java)?.copy(id = doc.id)
                    }
                    if (list.isEmpty()) {
                        seedDefaultVideos()
                    } else {
                        _videos.value = list.sortedBy { it.createdAt }
                    }
                }
            }
    }

    private fun seedDefaultVideos() {
        viewModelScope.launch {
            try {
                val batch = db.batch()
                val defaults = listOf(
                    TrainingVideo("", "video_1_title", "q_v_tYp6V8M", 1L),
                    TrainingVideo("", "video_2_title", "d6p-T8S8pS0", 2L),
                    TrainingVideo("", "video_3_title", "L-9jYkR_S6k", 3L),
                    TrainingVideo("", "video_4_title", "W6H3-F_P-zU", 4L)
                )
                defaults.forEachIndexed { index, video ->
                    val docRef = db.collection("training_videos").document()
                    // Use index to ensure sequential timestamps
                    batch.set(docRef, video.copy(id = docRef.id, createdAt = System.currentTimeMillis() + index * 10))
                }
                batch.commit().await()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun addVideo(title: String, youtubeLinkOrId: String, onComplete: (Boolean, String?) -> Unit) {
        val youtubeId = extractYoutubeId(youtubeLinkOrId)
        if (youtubeId.isBlank()) {
            onComplete(false, "Invalid YouTube link or ID")
            return
        }

        viewModelScope.launch {
            try {
                val docRef = db.collection("training_videos").document()
                val video = TrainingVideo(
                    id = docRef.id,
                    title = title.trim(),
                    youtubeId = youtubeId,
                    createdAt = System.currentTimeMillis()
                )
                docRef.set(video).await()
                onComplete(true, null)
            } catch (e: Exception) {
                onComplete(false, e.message)
            }
        }
    }

    fun updateVideo(video: TrainingVideo, newTitle: String, newYoutubeLinkOrId: String, onComplete: (Boolean, String?) -> Unit) {
        val youtubeId = extractYoutubeId(newYoutubeLinkOrId)
        if (youtubeId.isBlank()) {
            onComplete(false, "Invalid YouTube link or ID")
            return
        }

        viewModelScope.launch {
            try {
                val updated = video.copy(
                    title = newTitle.trim(),
                    youtubeId = youtubeId
                )
                db.collection("training_videos").document(video.id).set(updated).await()
                onComplete(true, null)
            } catch (e: Exception) {
                onComplete(false, e.message)
            }
        }
    }

    fun deleteVideo(videoId: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                db.collection("training_videos").document(videoId).delete().await()
                onComplete(true, null)
            } catch (e: Exception) {
                onComplete(false, e.message)
            }
        }
    }

    private fun extractYoutubeId(input: String): String {
        val trimmed = input.trim()
        if (trimmed.contains("youtube.com") || trimmed.contains("youtu.be")) {
            val pattern = "^(?:https?:\\/\\/)?(?:www\\.)?(?:youtube\\.com\\/(?:[^\\/\\n\\s]+\\/\\S+\\/|(?:v|e(?:mbed)?)\\/|\\S*?[?&]v=)|youtu\\.be\\/)([a-zA-Z0-9_-]{11})".toRegex()
            val match = pattern.find(trimmed)
            if (match != null && match.groupValues.size > 1) {
                return match.groupValues[1]
            }
        }
        return trimmed
    }

    override fun onCleared() {
        super.onCleared()
        videosListener?.remove()
    }
}
