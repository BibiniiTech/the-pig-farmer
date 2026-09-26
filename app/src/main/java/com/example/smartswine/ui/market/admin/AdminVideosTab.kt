package com.example.smartswine.ui.market.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smartswine.model.TrainingVideo
import com.example.smartswine.ui.training.resolveVideoTitle
import com.example.smartswine.utils.stringResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun AdminVideosTab(
    videos: List<TrainingVideo>,
    isVideosLoading: Boolean,
    videosError: String?,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    onAddVideo: (String, String, (Boolean, String?) -> Unit) -> Unit,
    onUpdateVideo: (TrainingVideo, String, String, (Boolean, String?) -> Unit) -> Unit,
    onDeleteVideo: (String, (Boolean, String?) -> Unit) -> Unit
) {
    var videoSearchQuery by remember { mutableStateOf("") }
    var videoToEdit by remember { mutableStateOf<TrainingVideo?>(null) }
    var videoToDelete by remember { mutableStateOf<TrainingVideo?>(null) }
    var showAddVideoDialog by remember { mutableStateOf(false) }

    // --- VIDEO ADD/EDIT DIALOG ---
    if (showAddVideoDialog || videoToEdit != null) {
        val editing = videoToEdit != null
        var title by remember { mutableStateOf(if (editing) videoToEdit!!.title else "") }
        var youtubeLink by remember { mutableStateOf(if (editing) "https://www.youtube.com/watch?v=${videoToEdit!!.youtubeId}" else "") }

        AlertDialog(
            onDismissRequest = {
                showAddVideoDialog = false
                videoToEdit = null
            },
            title = { Text(if (editing) stringResource("edit_video_tutorial") else stringResource("add_video_tutorial")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(stringResource("video_title")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = youtubeLink,
                        onValueChange = { youtubeLink = it },
                        label = { Text(stringResource("youtube_link_or_id")) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource("youtube_placeholder")) }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editing) {
                            onUpdateVideo(videoToEdit!!, title, youtubeLink) { success, err ->
                                if (success) {
                                    videoToEdit = null
                                    scope.launch { snackbarHostState.showSnackbar("Video updated successfully.") }
                                } else {
                                    scope.launch { snackbarHostState.showSnackbar("Error: $err") }
                                }
                            }
                        } else {
                            onAddVideo(title, youtubeLink) { success, err ->
                                if (success) {
                                    showAddVideoDialog = false
                                    scope.launch { snackbarHostState.showSnackbar("Video added successfully.") }
                                } else {
                                    scope.launch { snackbarHostState.showSnackbar("Error: $err") }
                                }
                            }
                        }
                    },
                    enabled = title.isNotBlank() && youtubeLink.isNotBlank()
                ) {
                    Text(if (editing) stringResource("save") else stringResource("add"))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddVideoDialog = false
                    videoToEdit = null
                }) {
                    Text(stringResource("cancel"))
                }
            }
        )
    }

    // --- VIDEO DELETE DIALOG ---
    if (videoToDelete != null) {
        AlertDialog(
            onDismissRequest = { videoToDelete = null },
            title = { Text(stringResource("delete_video_tutorial")) },
            text = { Text("${stringResource("confirm_delete_video")}\n'${resolveVideoTitle(videoToDelete!!.title)}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteVideo(videoToDelete!!.id) { success, err ->
                            if (success) {
                                videoToDelete = null
                                scope.launch { snackbarHostState.showSnackbar("Video deleted successfully.") }
                            } else {
                                scope.launch { snackbarHostState.showSnackbar("Error deleting: $err") }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource("delete"))
                }
            },
            dismissButton = {
                TextButton(onClick = { videoToDelete = null }) {
                    Text(stringResource("cancel"))
                }
            }
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = videoSearchQuery,
            onValueChange = { videoSearchQuery = it },
            placeholder = { Text(stringResource("search_videos")) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Button(
            onClick = { showAddVideoDialog = true },
            contentPadding = PaddingValues(horizontal = 12.dp),
            modifier = Modifier.height(56.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource("add"))
        }
    }

    if (isVideosLoading) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }

    if (videosError != null) {
        Text(
            text = "Error: $videosError",
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(16.dp)
        )
    }

    val filteredVideos = videos.filter {
        resolveVideoTitle(it.title).contains(videoSearchQuery, ignoreCase = true) ||
        it.youtubeId.contains(videoSearchQuery, ignoreCase = true)
    }

    if (filteredVideos.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(64.dp), contentAlignment = Alignment.Center) {
            Text(stringResource("no_video_tutorials"), color = MaterialTheme.colorScheme.outline)
        }
    } else {
        filteredVideos.forEach { video ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = resolveVideoTitle(video.title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "YouTube ID: ${video.youtubeId}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Row {
                        IconButton(onClick = { videoToEdit = video }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource("edit"), tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { videoToDelete = video }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource("delete"), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}
