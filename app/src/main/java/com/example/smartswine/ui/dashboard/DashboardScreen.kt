package com.example.smartswine.ui.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext
import com.example.smartswine.ui.components.NativeAdCard
import com.example.smartswine.ui.components.PlayStoreReviewDialog
import com.example.smartswine.ui.components.PlayStoreReviewManager
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.bibiniitech.smartswine.R
import com.example.smartswine.model.*
import com.example.smartswine.ui.auth.UserProfile
import com.example.smartswine.ui.components.FarmLogoImage
import com.example.smartswine.ui.dashboard.components.ManagementGrid
import com.example.smartswine.ui.dashboard.components.QuoteCard
import com.example.smartswine.ui.dashboard.components.UpcomingActivitiesList
import com.example.smartswine.ui.feed.components.FeedCalculatorDialog
import com.example.smartswine.ui.feed.components.FeedFormulatorDialog
import com.example.smartswine.ui.herd.AddPigDialog
import com.example.smartswine.ui.herd.HerdViewModel
import com.example.smartswine.ui.navigation.Screen
import com.example.smartswine.ui.theme.SmartSwineTheme
import com.example.smartswine.utils.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    profile: UserProfile?,
    tasks: List<TaskItem>,
    groupedTasks: List<TaskGroup> = emptyList(),
    taskAlerts: List<FarmAlert.TaskAlert> = emptyList(),
    weightAlerts: List<FarmAlert.WeightAlert> = emptyList(),
    stockAlerts: List<FarmAlert.LowStockAlert> = emptyList(),
    totalNotificationCount: Int = 0,
    allPigs: List<Pig> = emptyList(),
    financialRecords: List<FinancialRecord> = emptyList(),
    staff: List<com.example.smartswine.model.StaffMember> = emptyList(),
    onCompleteTask: (TaskItem) -> Unit,
    onDeleteTask: (TaskItem) -> Unit,
    onNavigateTo: (String) -> Unit = {},
    error: String? = null,
    onClearError: () -> Unit = {},
    onLogHealthActivity: (List<String>, HealthRecord, Boolean, Boolean, Boolean, Map<String, Any>) -> Unit = { _, _, _, _, _, _ -> },
    onAddPig: (HerdViewModel.AddPigFormData) -> Unit = {},
    ingredients: List<FeedIngredient> = emptyList(),
    nutritionalRequirements: List<NutritionalRequirement> = emptyList(),
    isFormulating: Boolean = false,
    @Suppress("UNUSED_PARAMETER") herdStats: Map<String, Int> = emptyMap(),
    @Suppress("UNUSED_PARAMETER") onCalculateRequirements: (Map<String, Any>) -> Unit = {},
    onFormulateFeed: (String, List<String>) -> Unit = { _, _ -> },
    onRefresh: () -> Unit = {},
    isRefreshing: Boolean = false,
    onLogout: () -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    sowTags: List<String> = emptyList(),
    boarTags: List<String> = emptyList(),
    targetSection: String? = null,
    targetSubOption: String? = null,
    targetTag: String? = null,
    onAddStaff: (com.example.smartswine.model.StaffMember) -> Unit = {},
    onUpdateStaff: (com.example.smartswine.model.StaffMember) -> Unit = {},
    onArchiveStaff: (com.example.smartswine.model.StaffMember) -> Unit = {},
    onPaySalary: (com.example.smartswine.model.StaffMember, String, String, Double, Double, String) -> Unit = { _, _, _, _, _, _ -> },
    onUpdatePigWeight: (String, Double) -> Unit = { _, _ -> },
    onSnoozeTasks: (List<TaskItem>, Int) -> Unit = { _, _ -> },
    onDeleteTaskForPig: (List<TaskItem>, String) -> Unit = { _, _ -> },
    onSnoozeWeightAlert: (String, Int) -> Unit = { _, _ -> },
    onDismissWeightAlert: (String) -> Unit = {},
    onSnoozeStockAlert: (String, Int) -> Unit = { _, _ -> },
    onDismissStockAlert: (String) -> Unit = {},
    isFinancialsRestricted: Boolean = false,
) {
    val currentLanguage = LocalAppLanguage.current
    val dailyQuote = remember(currentLanguage) { QuoteProvider.getQuoteOfDay(currentLanguage.code) }

    var expandedSection by rememberSaveable { mutableStateOf(targetSection) }
    var subOptionState by rememberSaveable { mutableStateOf(targetSubOption) }
    var targetTagState by rememberSaveable { mutableStateOf(targetTag) }

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    var scrollContainerCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val itemCoordinates = remember { mutableMapOf<String, LayoutCoordinates>() }
    var scrollJob by remember { mutableStateOf<Job?>(null) }

    fun scrollToItem(itemId: String, topOffsetDp: Int = 16, isAnyCollapseExpected: Boolean = true) {
        scrollJob?.cancel()
        scrollJob = coroutineScope.launch {
            val topMargin = with(density) { topOffsetDp.dp.toPx() }
            val initialDelay = if (isAnyCollapseExpected) 190L else 40L
            delay(initialDelay)

            for (attempt in 0..12) {
                val container = scrollContainerCoordinates
                val target = itemCoordinates[itemId]
                if (container != null && container.isAttached && target != null && target.isAttached) {
                    val offset = container.localPositionOf(target, Offset.Zero)
                    val targetScroll = (scrollState.value + offset.y - topMargin).toInt().coerceAtLeast(0)
                    scrollState.animateScrollTo(targetScroll)
                    break
                }
                delay(30)
            }

            // Post-scroll settling adjustment: Ensure heading rests precisely at the top
            delay(180)
            val container = scrollContainerCoordinates
            val target = itemCoordinates[itemId]
            if (container != null && container.isAttached && target != null && target.isAttached) {
                val offset = container.localPositionOf(target, Offset.Zero)
                val diff = offset.y - topMargin
                if (kotlin.math.abs(diff) > with(density) { 6.dp.toPx() }) {
                    val targetScroll = (scrollState.value + diff).toInt().coerceAtLeast(0)
                    scrollState.animateScrollTo(targetScroll)
                }
            }
        }
    }

    LaunchedEffect(targetSection) {
        if (targetSection != null) {
            expandedSection = targetSection
            if (targetSubOption == null || targetSubOption == "add") {
                scrollToItem(targetSection, isAnyCollapseExpected = false)
            }
        }
    }

    LaunchedEffect(targetSubOption) {
        if (targetSubOption != null) {
            subOptionState = targetSubOption
            if (targetSubOption != "add") {
                scrollToItem(targetSubOption, isAnyCollapseExpected = false)
            }
        }
    }

    LaunchedEffect(targetTag) {
        if (targetTag != null) {
            targetTagState = targetTag
        }
    }

    BackHandler(enabled = expandedSection != null) {
        expandedSection = null
    }

    val context = LocalContext.current
    var showReviewDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1500L)
        if (PlayStoreReviewManager.shouldShowReviewReminder(context)) {
            showReviewDialog = true
            PlayStoreReviewManager.recordPromptShown(context)
        }
    }

    var showAddPigDialog by remember { mutableStateOf(false) }
    var showFormulatorDialog by remember { mutableStateOf(false) }
    var showFeedCalculatorDialog by remember { mutableStateOf(false) }

    val tasksToEditState = remember { mutableStateOf<List<TaskItem>?>(null) }
    val showNotificationBottomSheet = remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Fixed background watermark for SmartSwine app logo
        Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = null,
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.Center)
                .graphicsLayer(alpha = 0.12f),
            contentScale = ContentScale.Fit
        )

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { scrollContainerCoordinates = it }
                    .padding(20.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Top,
            ) {
                // Header Row: Upper Left (Farm Logo + Name -> Settings) & Upper Right (Notification Bell + Hamburger Menu)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Upper Left: Farm Logo + Farm Name (Clickable to open Settings/Profile)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onNavigateTo(Screen.Settings.route) }
                            .padding(vertical = 4.dp, horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                            shadowElevation = 1.dp
                        ) {
                            FarmLogoImage(
                                farmLogo = profile?.farmLogo,
                                contentDescription = stringResource("farm_logo"),
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = if (profile?.farmName.isNullOrBlank()) stringResource("your_farm") else profile.farmName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (profile?.firstName.isNullOrBlank()) stringResource("settings") else "${stringResource("welcome")}, ${stringResource("farmer")} ${profile.firstName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                // Upper Right: Notification Bell + How-To Guide + Hamburger Menu
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = { showNotificationBottomSheet.value = true }
                    ) {
                        BadgedBox(
                            badge = {
                                if (totalNotificationCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ) {
                                        Text(totalNotificationCount.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = stringResource("upcoming_activities"),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { onNavigateTo(Screen.HowToGuide.route) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = stringResource("how_to_guide"),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenDrawer
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = stringResource("menu"),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
            
            if (tasksToEditState.value != null) {
                TaskCompletionDialog(
                    tasksToEdit = tasksToEditState.value!!,
                    allPigs = allPigs,
                    onDismissRequest = { tasksToEditState.value = null },
                    onDeleteTask = {
                        tasksToEditState.value?.forEach { onDeleteTask(it) }
                        tasksToEditState.value = null
                        if (PlayStoreReviewManager.shouldShowAfterAction(context)) {
                            showReviewDialog = true
                            PlayStoreReviewManager.recordPromptShown(context)
                        }
                    },
                    onSnoozeTask = { hours ->
                        tasksToEditState.value?.let { onSnoozeTasks(it, hours) }
                        tasksToEditState.value = null
                    },
                    onDeleteTaskForPig = { pigIdentifier ->
                        tasksToEditState.value?.let { onDeleteTaskForPig(it, pigIdentifier) }
                        tasksToEditState.value = null
                    },
                    onLogHealthActivity = { selectedPigIds, record, b1, b2, b3, data ->
                        onLogHealthActivity(selectedPigIds, record, b1, b2, b3, data)
                        
                        // Complete tasks for selected pigs, or complete general task if no pigs selected
                        tasksToEditState.value?.forEach { task ->
                            val taskPigIdentifier = task.name.substringAfter(": ", "").replace("Pig ", "").replace("Tag: ", "").trim()
                            val taskPigId = task.pigIds.firstOrNull() 
                                ?: allPigs.find { (it.id == taskPigIdentifier) || (it.tagNumber == taskPigIdentifier) }?.id
                                ?: taskPigIdentifier
                                
                            if (selectedPigIds.isEmpty() || selectedPigIds.contains(taskPigId) || task.pigIds.any { selectedPigIds.contains(it) }) {
                                onCompleteTask(task)
                            }
                        }
                        tasksToEditState.value = null
                        if (PlayStoreReviewManager.shouldShowAfterAction(context)) {
                            showReviewDialog = true
                            PlayStoreReviewManager.recordPromptShown(context)
                        }
                    }
                )
            }

            StylishDivider()

            Spacer(modifier = Modifier.height(12.dp))

            val passRemainingTime = LocalPassRemainingTime.current
            if (!passRemainingTime.isNullOrBlank()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = androidx.compose.ui.graphics.Color(0xFF2E7D32).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFF2E7D32).copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = androidx.compose.ui.graphics.Color(0xFF2E7D32),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "3-Hour Universal Pass: $passRemainingTime remaining",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = androidx.compose.ui.graphics.Color(0xFF2E7D32)
                        )
                    }
                }
            }

            ManagementGrid(
                expandedSection = expandedSection,
                onToggleSection = { sectionId ->
                    val isExpanding = expandedSection != sectionId
                    val wasAnotherSectionOpen = expandedSection != null && expandedSection != sectionId
                    expandedSection = if (expandedSection == sectionId) null else sectionId
                    if (isExpanding) {
                        scrollToItem(sectionId, isAnyCollapseExpected = wasAnotherSectionOpen)
                    }
                },
                targetSubOption = subOptionState,
                onSubOptionChange = { subOptionState = it },
                targetPigTag = targetTagState,
                allPigs = allPigs,
                herdStats = herdStats,
                financialRecords = financialRecords,
                staff = staff,
                userCountry = profile?.country ?: "",
                isFinancialsRestricted = isFinancialsRestricted,
                onAddPigClick = { showAddPigDialog = true },
                onShowFeedCalculator = { showFeedCalculatorDialog = true },
                onShowFeedFormulator = { showFormulatorDialog = true },
                onAddStaff = onAddStaff,
                onUpdateStaff = onUpdateStaff,
                onArchiveStaff = onArchiveStaff,
                onPaySalary = { member, p1, p2, p3, p4, p5 ->
                    onPaySalary(member, p1, p2, p3, p4, p5)
                    if (PlayStoreReviewManager.shouldShowAfterAction(context)) {
                        showReviewDialog = true
                        PlayStoreReviewManager.recordPromptShown(context)
                    }
                },
                onUpdatePigWeight = { pigId, weight ->
                    onUpdatePigWeight(pigId, weight)
                    if (PlayStoreReviewManager.shouldShowAfterAction(context)) {
                        showReviewDialog = true
                        PlayStoreReviewManager.recordPromptShown(context)
                    }
                },
                onLogHealthActivity = { pigIds, record, b1, b2, b3, data ->
                    onLogHealthActivity(pigIds, record, b1, b2, b3, data)
                    if (PlayStoreReviewManager.shouldShowAfterAction(context)) {
                        showReviewDialog = true
                        PlayStoreReviewManager.recordPromptShown(context)
                    }
                },
                onRegisterCoordinates = { id, coords -> itemCoordinates[id] = coords },
                onItemExpanded = { id -> scrollToItem(id, isAnyCollapseExpected = true) },
                onNavigateTo = onNavigateTo
            )

            Spacer(modifier = Modifier.height(24.dp))

            StylishDivider()

            Spacer(modifier = Modifier.height(24.dp))

            QuoteCard(quote = dailyQuote)

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource("app_copyright", "2026"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(if (expandedSection != null) 600.dp else 80.dp))
        }

        error?.let { msg ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp, start = 16.dp, end = 16.dp),
                action = {
                    TextButton(onClick = onClearError) {
                        Text(stringResource("dismiss"))
                    }
                }
            ) {
                Text(msg)
            }
        }
    }
    }

    if (showAddPigDialog) {
        AddPigDialog(
            sowTags = sowTags,
            boarTags = boarTags,
            onDismiss = { if (showAddPigDialog) showAddPigDialog = false },
        ) { formData ->
            onAddPig(formData)
            if (showAddPigDialog) showAddPigDialog = false
            if (PlayStoreReviewManager.shouldShowAfterAction(context)) {
                showReviewDialog = true
                PlayStoreReviewManager.recordPromptShown(context)
            }
        }
    }

    if (showFormulatorDialog) {
        FeedFormulatorDialog(
            ingredients = ingredients,
            requirements = nutritionalRequirements,
            isFormulating = isFormulating,
            onNavigateToPaywall = { onNavigateTo(Screen.Paywall.route) },
            onDismiss = { if (showFormulatorDialog) showFormulatorDialog = false }
        ) { name, ingredientIds ->
            onFormulateFeed(name, ingredientIds)
            if (showFormulatorDialog) showFormulatorDialog = false
            if (PlayStoreReviewManager.shouldShowAfterAction(context)) {
                showReviewDialog = true
                PlayStoreReviewManager.recordPromptShown(context)
            }
        }
    }

    if (showFeedCalculatorDialog) {
        FeedCalculatorDialog(
            herdStats = herdStats,
            onDismiss = { if (showFeedCalculatorDialog) showFeedCalculatorDialog = false },
            onCalculate = { calcData ->
                onCalculateRequirements(calcData)
                if (showFeedCalculatorDialog) showFeedCalculatorDialog = false
                if (PlayStoreReviewManager.shouldShowAfterAction(context)) {
                    showReviewDialog = true
                    PlayStoreReviewManager.recordPromptShown(context)
                }
            }
        )
    }

    if (showNotificationBottomSheet.value) {
        ModalBottomSheet(
            onDismissRequest = { showNotificationBottomSheet.value = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            UpcomingActivitiesList(
                taskAlerts = taskAlerts,
                weightAlerts = weightAlerts,
                stockAlerts = stockAlerts,
                onTaskAlertClick = { taskAlert ->
                    showNotificationBottomSheet.value = false
                    tasksToEditState.value = taskAlert.originalTasks
                },
                onSnoozeTask = { tasks ->
                    onSnoozeTasks(tasks, 24)
                },
                onSnoozeTaskDuration = { tasks, hours ->
                    onSnoozeTasks(tasks, hours)
                },
                onDeleteTask = { tasks ->
                    tasks.forEach { onDeleteTask(it) }
                },
                onSnoozeWeightAlert = onSnoozeWeightAlert,
                onDismissWeightAlert = onDismissWeightAlert,
                onSnoozeStockAlert = onSnoozeStockAlert,
                onDismissStockAlert = onDismissStockAlert,
                onNavigateToWeightChecker = { pigTag ->
                    showNotificationBottomSheet.value = false
                    expandedSection = "weight_checker"
                    subOptionState = "tape"
                    targetTagState = pigTag
                    scrollToItem("tape")
                },
                onNavigateToFeed = {
                    showNotificationBottomSheet.value = false
                    onNavigateTo(Screen.Feed.route)
                },
                onDismiss = {
                    showNotificationBottomSheet.value = false
                }
            )
        }
    }

    if (showReviewDialog) {
        PlayStoreReviewDialog(
            onDismiss = { showReviewDialog = false }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    SmartSwineTheme {
        DashboardScreen(
            profile = UserProfile(
                firstName = "John",
                lastName = "Doe",
                farmName = "Happy Pig Farm",
                country = "USA",
                email = "john@example.com"
            ),
            tasks = listOf(
                TaskItem(id = "1", name = "Feed Pigs: Pig 101", date = "Dec 24", notes = "Morning session"),
                TaskItem(id = "2", name = "Clean Pens", date = "Dec 25", notes = "Full cleaning")
            ),
            groupedTasks = listOf(
                TaskGroup(
                    activity = "Feeding",
                    target = "TAG-001",
                    date = "Dec 24",
                    isOverdue = false,
                    originalTasks = listOf(TaskItem(id = "1", name = "Feed Pigs: Pig 101", date = "Dec 24", notes = "Morning session"))
                )
            ),
            totalNotificationCount = 1,
            allPigs = listOf(
                Pig(id = "101", tagNumber = "TAG-001", breed = "Large White")
            ),
            onCompleteTask = {},
            onDeleteTask = {},
            onNavigateTo = {},
            onRefresh = {},
            isRefreshing = false
        )
    }
}
