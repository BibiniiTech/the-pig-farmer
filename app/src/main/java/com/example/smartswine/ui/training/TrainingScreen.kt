package com.example.smartswine.ui.training

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.example.smartswine.utils.StylishDivider
import com.example.smartswine.ui.theme.SmartSwineTheme
import com.example.smartswine.utils.stringResource
import com.example.smartswine.model.TrainingVideo
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingScreen(
    onBack: () -> Unit,
    trainingViewModel: TrainingViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val tips = remember { TrainingTipsData.allTrainingTips }
    val categories = remember {
        listOf(
            "cat_weaning",
            "cat_feeding",
            "cat_breeding",
            "cat_health",
            "cat_housing",
            "cat_waste",
            "cat_general"
        )
    }
    var selectedCategory by remember { mutableStateOf("all") }
    val expandedCategories = remember {
        mutableStateMapOf<String, Boolean>().apply {
            if (isEmpty()) {
                categories.forEach { this[it] = false }
            }
        }
    }

    val videos by trainingViewModel.videos.collectAsStateWithLifecycle()
    val isVideosLoading by trainingViewModel.isLoading.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource("back"))
                    }
                    Text(
                        text = stringResource("training_title"),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.width(48.dp))
                }
                StylishDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Quick Tips Header and Filters
            item {
                Text(
                    text = stringResource("quick_farming_tips"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == "all",
                            onClick = { selectedCategory = "all" },
                            label = { Text(stringResource("all_tips")) }
                        )
                    }
                    items(categories) { catKey ->
                        FilterChip(
                            selected = selectedCategory == catKey,
                            onClick = { selectedCategory = catKey },
                            label = { Text(stringResource(catKey)) }
                        )
                    }
                }
            }

            // Categorized Tips List
            if (selectedCategory == "all") {
                categories.forEach { catKey ->
                    val catTips = tips.filter { it.categoryKey == catKey }
                    if (catTips.isNotEmpty()) {
                        val isExpanded = expandedCategories[catKey] ?: false
                        item(key = "header_$catKey") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedCategories[catKey] = !isExpanded }
                                    .padding(top = 12.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(catKey),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        if (isExpanded) {
                            items(catTips, key = { "tip_${it.id}" }) { tip ->
                                TipCard(tip)
                            }
                        }
                    }
                }
            } else {
                val filteredTips = tips.filter { it.categoryKey == selectedCategory }
                items(filteredTips, key = { "tip_${it.id}" }) { tip ->
                    TipCard(tip)
                }
            }

            // Section 2: Video Tutorials
            item {
                StylishDivider(modifier = Modifier.padding(vertical = 12.dp))
                
                Text(
                    text = stringResource("video_tutorials"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                if (isVideosLoading && videos.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (videos.isEmpty()) {
                    Text(
                        text = "No videos available.",
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        textAlign = TextAlign.Center
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(videos) { video ->
                            VideoThumbnailCard(video) {
                                val intent = Intent(Intent.ACTION_VIEW, "https://www.youtube.com/watch?v=${video.youtubeId}".toUri())
                                context.startActivity(intent)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(120.dp))
            }
        }
    }
}

@Composable
fun resolveTipText(key: String, defaultValue: String): String {
    val translated = stringResource(key)
    val fallbackFormat = key.replace("_", " ").lowercase(java.util.Locale.ROOT).replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(java.util.Locale.ROOT) else it.toString()
    }
    return if (translated == key || translated == fallbackFormat) defaultValue else translated
}

@Composable
fun TipCard(tip: TrainingTip) {
    val expanded = remember { mutableStateOf(false) }
    val resolvedTitle = resolveTipText(tip.titleKey, tip.defaultTitle)
    val resolvedContent = resolveTipText(tip.contentKey, tip.defaultContent)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded.value = !expanded.value },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = resolvedTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded.value) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }
            AnimatedVisibility(visible = expanded.value) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = resolvedContent,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun VideoThumbnailCard(video: TrainingVideo, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .height(160.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // Since we don't have actual thumbnails in resources, we use a placeholder color and icon
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxSize()
                ) {}
                
                Icon(
                    imageVector = Icons.Default.PlayCircleFilled,
                    contentDescription = "Play",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = resolveVideoTitle(video.title),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(12.dp),
                maxLines = 2
            )
        }
    }
}

@Composable
fun resolveVideoTitle(title: String): String {
    if (title.startsWith("video_") && title.endsWith("_title")) {
        val translated = stringResource(title)
        if (translated != title) {
            return translated
        }
    }
    return title
}

@Preview(showBackground = true)
@Composable
fun TrainingScreenPreview() {
    SmartSwineTheme {
        TrainingScreen(onBack = {})
    }
}
