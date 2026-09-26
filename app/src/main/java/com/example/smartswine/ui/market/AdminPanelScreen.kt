package com.example.smartswine.ui.market

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartswine.model.FeedIngredient
import com.example.smartswine.model.TrainingVideo
import com.example.smartswine.ui.feed.FeedViewModel
import com.example.smartswine.ui.market.admin.AdminIngredientsTab
import com.example.smartswine.ui.market.admin.AdminSuggestionsTab
import com.example.smartswine.ui.market.admin.AdminVideosTab
import com.example.smartswine.ui.theme.SmartSwineTheme
import com.example.smartswine.ui.training.TrainingViewModel
import com.example.smartswine.utils.StylishDivider
import com.example.smartswine.utils.stringResource

@Composable
fun AdminPanelScreen(
    marketViewModel: MarketViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    feedViewModel: FeedViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    trainingViewModel: TrainingViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onBack: () -> Unit = {}
) {
    val allSuggestions by marketViewModel.allSuggestions.collectAsStateWithLifecycle()
    val isMarketLoading by marketViewModel.isLoading.collectAsStateWithLifecycle()
    val marketError by marketViewModel.error.collectAsStateWithLifecycle()

    val videos by trainingViewModel.videos.collectAsStateWithLifecycle()
    val isVideosLoading by trainingViewModel.isLoading.collectAsStateWithLifecycle()
    val videosError by trainingViewModel.error.collectAsStateWithLifecycle()

    val globalIngredients by feedViewModel.globalIngredients.collectAsStateWithLifecycle()
    val isFeedLoading by feedViewModel.isLoading.collectAsStateWithLifecycle()

    val providers by marketViewModel.providers.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        marketViewModel.fetchAllSuggestions()
        feedViewModel.loadGlobalIngredients()
        trainingViewModel.fetchVideos()
    }

    AdminPanelContent(
        allSuggestions = allSuggestions,
        isMarketLoading = isMarketLoading,
        marketError = marketError,
        videos = videos,
        isVideosLoading = isVideosLoading,
        videosError = videosError,
        globalIngredients = globalIngredients,
        isFeedLoading = isFeedLoading,
        providers = providers,
        onBack = onBack,
        onApproveSuggestion = marketViewModel::approveSuggestion,
        onRejectSuggestion = marketViewModel::rejectSuggestion,
        onUpdateSuggestion = marketViewModel::updateSuggestion,
        onDeleteSuggestion = marketViewModel::deleteSuggestion,
        onUpdateProvider = marketViewModel::updateProvider,
        onDeleteProvider = marketViewModel::deleteProvider,
        onAddGlobalIngredient = feedViewModel::addGlobalIngredient,
        onUpdateGlobalIngredient = feedViewModel::updateGlobalIngredient,
        onDeleteGlobalIngredient = feedViewModel::deleteGlobalIngredient,
        onAddVideo = trainingViewModel::addVideo,
        onUpdateVideo = trainingViewModel::updateVideo,
        onDeleteVideo = trainingViewModel::deleteVideo
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelContent(
    allSuggestions: List<Suggestion> = emptyList(),
    isMarketLoading: Boolean = false,
    marketError: String? = null,
    videos: List<TrainingVideo> = emptyList(),
    isVideosLoading: Boolean = false,
    videosError: String? = null,
    globalIngredients: List<FeedIngredient> = emptyList(),
    isFeedLoading: Boolean = false,
    providers: List<ProviderListing> = emptyList(),
    onBack: () -> Unit = {},
    onApproveSuggestion: (Suggestion, (Boolean, String?) -> Unit) -> Unit = { _, _ -> },
    onRejectSuggestion: (Suggestion, String, (Boolean, String?) -> Unit) -> Unit = { _, _, _ -> },
    onUpdateSuggestion: (Suggestion, (Boolean, String?) -> Unit) -> Unit = { _, _ -> },
    onDeleteSuggestion: (Suggestion, (Boolean, String?) -> Unit) -> Unit = { _, _ -> },
    onUpdateProvider: (ProviderListing, (Boolean, String?) -> Unit) -> Unit = { _, _ -> },
    onDeleteProvider: (String, (Boolean, String?) -> Unit) -> Unit = { _, _ -> },
    onAddGlobalIngredient: (FeedIngredient, (Boolean, String?) -> Unit) -> Unit = { _, _ -> },
    onUpdateGlobalIngredient: (FeedIngredient, (Boolean, String?) -> Unit) -> Unit = { _, _ -> },
    onDeleteGlobalIngredient: (String, (Boolean, String?) -> Unit) -> Unit = { _, _ -> },
    onAddVideo: (String, String, (Boolean, String?) -> Unit) -> Unit = { _, _, _ -> },
    onUpdateVideo: (TrainingVideo, String, String, (Boolean, String?) -> Unit) -> Unit = { _, _, _, _ -> },
    onDeleteVideo: (String, (Boolean, String?) -> Unit) -> Unit = { _, _ -> }
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Master Tab state: 0 = Provider Suggestions, 1 = Ingredients Manager, 2 = Video Tutorials
    var activeMasterTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource("back"))
                    }
                    Text(
                        text = stringResource("admin_panel").uppercase(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.width(48.dp))
                }
                
                // Master selection: Suggestions vs Ingredients vs Videos
                TabRow(
                    selectedTabIndex = activeMasterTab,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = activeMasterTab == 0,
                        onClick = { activeMasterTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource("suggestions"))
                            }
                        }
                    )
                    Tab(
                        selected = activeMasterTab == 1,
                        onClick = { activeMasterTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource("feed_ingredients"))
                            }
                        }
                    )
                    Tab(
                        selected = activeMasterTab == 2,
                        onClick = { activeMasterTab = 2 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PlayCircleFilled, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource("video_tutorials"))
                            }
                        }
                    )
                }
                StylishDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (activeMasterTab) {
                0 -> AdminSuggestionsTab(
                    allSuggestions = allSuggestions,
                    providers = providers,
                    isMarketLoading = isMarketLoading,
                    marketError = marketError,
                    scope = scope,
                    snackbarHostState = snackbarHostState,
                    onApproveSuggestion = onApproveSuggestion,
                    onRejectSuggestion = onRejectSuggestion,
                    onUpdateSuggestion = onUpdateSuggestion,
                    onDeleteSuggestion = onDeleteSuggestion,
                    onUpdateProvider = onUpdateProvider,
                    onDeleteProvider = onDeleteProvider
                )
                1 -> AdminIngredientsTab(
                    globalIngredients = globalIngredients,
                    isFeedLoading = isFeedLoading,
                    scope = scope,
                    snackbarHostState = snackbarHostState,
                    onAddGlobalIngredient = onAddGlobalIngredient,
                    onUpdateGlobalIngredient = onUpdateGlobalIngredient,
                    onDeleteGlobalIngredient = onDeleteGlobalIngredient
                )
                2 -> AdminVideosTab(
                    videos = videos,
                    isVideosLoading = isVideosLoading,
                    videosError = videosError,
                    scope = scope,
                    snackbarHostState = snackbarHostState,
                    onAddVideo = onAddVideo,
                    onUpdateVideo = onUpdateVideo,
                    onDeleteVideo = onDeleteVideo
                )
            }

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AdminPanelScreenPreview() {
    SmartSwineTheme {
        AdminPanelContent(
            allSuggestions = listOf(
                Suggestion(id = "1", providerName = "Best Feed Supplier", serviceType = "Feed Supplier", status = "pending"),
                Suggestion(id = "2", providerName = "City Butcher", serviceType = "Butcher", status = "approved"),
                Suggestion(id = "3", providerName = "Old Tools", serviceType = "Tools Supplier", status = "rejected", adminFeedback = "Inaccurate info")
            ),
            globalIngredients = listOf(
                FeedIngredient(id = "1", name = "Yellow Maize", crudeProtein = 8.5, metabolizableEnergy = 3300.0, mainCategory = "Energy"),
                FeedIngredient(id = "2", name = "Soybean Meal", crudeProtein = 44.0, metabolizableEnergy = 2200.0, mainCategory = "Protein")
            ),
            videos = listOf(
                TrainingVideo(id = "1", title = "Pig Farming 101", youtubeId = "q_v_tYp6V8M"),
                TrainingVideo(id = "2", title = "Advanced Nutrition", youtubeId = "d6p-T8S8pS0")
            )
        )
    }
}
