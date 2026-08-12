package com.example.smartswine.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.bibiniitech.smartswine.R
import com.example.smartswine.model.Pig
import com.example.smartswine.model.HealthRecord
import com.example.smartswine.model.TaskItem
import com.example.smartswine.model.TaskGroup
import com.example.smartswine.model.FeedIngredient
import com.example.smartswine.model.NutritionalRequirement
import com.example.smartswine.ui.auth.UserProfile
import com.example.smartswine.ui.navigation.Screen
import com.example.smartswine.ui.theme.SmartSwineTheme
import com.example.smartswine.ui.theme.DarkBackground
import com.example.smartswine.ui.herd.AddPigDialog
import com.example.smartswine.ui.herd.HerdViewModel
import com.example.smartswine.ui.feed.FeedFormulatorDialog
import com.example.smartswine.ui.dashboard.components.QuoteCard
import com.example.smartswine.ui.dashboard.components.ManagementGrid
import com.example.smartswine.ui.dashboard.components.UpcomingActivitiesList
import com.example.smartswine.utils.LocalAppLanguage
import com.example.smartswine.utils.DateUtils
import com.example.smartswine.utils.StylishDivider
import com.example.smartswine.utils.LocalIsPremium
import com.example.smartswine.utils.QuoteProvider
import com.example.smartswine.utils.Translator
import com.example.smartswine.utils.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    profile: UserProfile?,
    tasks: List<TaskItem>,
    groupedTasks: List<TaskGroup> = emptyList(),
    allPigs: List<Pig> = emptyList(),
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
    sowTags: List<String> = emptyList(),
    boarTags: List<String> = emptyList(),
) {
    val currentLanguage = LocalAppLanguage.current
    val locale = remember(currentLanguage) { currentLanguage.toLocale() }
    val greeting = remember { DateUtils.getGreeting() }
    val currentDate = remember(locale) { DateUtils.getCurrentDateDisplay(locale) }
    val dailyQuote = remember(currentLanguage) { QuoteProvider.getQuoteOfDay(currentLanguage.code) }

    var showAddPigDialog by remember { mutableStateOf(value = false) }
    var showFormulatorDialog by remember { mutableStateOf(value = false) }

    val tasksToEditState = remember { mutableStateOf<List<TaskItem>?>(null) }
    val showNotificationBottomSheet = remember { mutableStateOf(false) }
    
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(30.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource("welcome_to"),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (profile?.farmName.isNullOrBlank()) stringResource("your_farm") else profile.farmName,
                        style = MaterialTheme.typography.headlineLarge, // Slightly smaller than displayMedium for better layout safety
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                Box(modifier = Modifier.padding(top = 8.dp)) {
                    IconButton(
                        onClick = { showNotificationBottomSheet.value = true }
                    ) {
                        BadgedBox(
                            badge = {
                                if (groupedTasks.isNotEmpty()) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ) {
                                        Text(groupedTasks.size.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Upcoming Activities",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "${stringResource(greeting)}, ${stringResource("farmer")} ${profile?.firstName ?: stringResource("user")}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = currentDate,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(40.dp))

            if (tasksToEditState.value != null) {
                TaskCompletionDialog(
                    tasksToEdit = tasksToEditState.value!!,
                    allPigs = allPigs,
                    onDismissRequest = { tasksToEditState.value = null },
                    onDeleteTask = {
                        tasksToEditState.value?.forEach { onDeleteTask(it) }
                        tasksToEditState.value = null
                    }
                ) { selectedPigIds, record, b1, b2, b3, data ->
                    onLogHealthActivity(selectedPigIds, record, b1, b2, b3, data)
                    
                    // Partial completion: only complete tasks for selected pigs
                    tasksToEditState.value?.forEach { task ->
                        val taskPigIdentifier = task.name.substringAfter(": ", "").replace("Pig ", "").trim()
                        val taskPigId = task.pigIds.firstOrNull() 
                            ?: allPigs.find { (it.id == taskPigIdentifier) || (it.tagNumber == taskPigIdentifier) }?.id
                            ?: taskPigIdentifier
                            
                        if (selectedPigIds.contains(taskPigId)) {
                            onCompleteTask(task)
                        }
                    }
                    tasksToEditState.value = null
                }
            }

            StylishDivider()

            Spacer(modifier = Modifier.height(8.dp))

            ManagementGrid(onNavigateTo = onNavigateTo)

            Spacer(modifier = Modifier.height(24.dp))

            StylishDivider()

            Spacer(modifier = Modifier.height(24.dp))

            QuoteCard(quote = dailyQuote)

            Spacer(modifier = Modifier.height(100.dp))
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

    if (showAddPigDialog) {
        AddPigDialog(
            sowTags = sowTags,
            boarTags = boarTags,
            onDismiss = { if (showAddPigDialog) showAddPigDialog = false },
        ) { formData ->
            onAddPig(formData)
            if (showAddPigDialog) showAddPigDialog = false
        }
    }

    if (showFormulatorDialog) {
        FeedFormulatorDialog(
            ingredients = ingredients,
            requirements = nutritionalRequirements,
            isFormulating = isFormulating,
            onDismiss = { if (showFormulatorDialog) showFormulatorDialog = false }
        ) { name, ingredientIds ->
            onFormulateFeed(name, ingredientIds)
            if (showFormulatorDialog) showFormulatorDialog = false
        }
    }

    if (showNotificationBottomSheet.value) {
        ModalBottomSheet(
            onDismissRequest = { showNotificationBottomSheet.value = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            UpcomingActivitiesList(
                groupedTasks = groupedTasks,
                onTaskGroupClick = { originalTasks ->
                    tasksToEditState.value = originalTasks
                    showNotificationBottomSheet.value = false
                }
            )
        }
    }
}


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
