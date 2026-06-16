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
import com.example.smartswine.model.FeedIngredient
import com.example.smartswine.model.NutritionalRequirement
import com.example.smartswine.ui.auth.UserProfile
import com.example.smartswine.ui.navigation.Screen
import com.example.smartswine.ui.theme.SmartSwineTheme
import com.example.smartswine.ui.theme.DarkBackground
import com.example.smartswine.ui.herd.AddPigDialog
import com.example.smartswine.ui.herd.HerdViewModel
import com.example.smartswine.ui.feed.FeedFormulatorDialog
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
    
    // Grouping tasks by activity name and date for a cleaner view
    val groupedTasks = remember(tasks, allPigs, locale) {
        tasks.filter { task ->
            val activity = task.name.substringBefore(": ", "")
            if (activity.contains("Pregnancy", ignoreCase = true) || 
                activity.contains("Farrowing", ignoreCase = true) || 
                activity.contains("Heat", ignoreCase = true)) {
                val identifier = task.name.substringAfter(": ", "").replace("Pig ", "").trim()
                if (identifier.isNotEmpty()) {
                    val pig = allPigs.find { it.id == identifier } 
                        ?: allPigs.find { it.tagNumber == identifier }
                    // Filter out males for these female-specific activities
                    ((pig == null) || (pig.gender.equals("Female", ignoreCase = true)))
                } else true
            } else true
        }.groupBy {
            val activity = it.name.substringBefore(": ")
            activity + it.date
        }.values.asSequence().map { group ->
            val first = group.first()
            val activity = first.name.substringBefore(": ")
            val isMultiple = group.size > 1
            val target = if (isMultiple) {
                val tags = group.asSequence().map { 
                    val rawIdentifier = it.name.substringAfter(": ", "").replace("Pig ", "").trim()
                    if (rawIdentifier.isEmpty()) return@map "General"
                    
                    // Try to resolve as ID first, then fallback to the raw identifier (which might already be a tag)
                    val resolvedTag = allPigs.find { p -> p.id == rawIdentifier }?.tagNumber 
                        ?: allPigs.find { p -> p.tagNumber == rawIdentifier }?.tagNumber 
                        ?: rawIdentifier
                    
                    resolvedTag
                }.distinct().toList()
                tags.joinToString(", ")
            } else {
                val rawTarget = first.name.substringAfter(": ", "").replace("Pig ", "").trim().ifEmpty { "General" }
                // Try to find by ID first, then by Tag, then fallback to rawTarget
                if (rawTarget == "General") {
                    rawTarget
                } else {
                    allPigs.find { it.id == rawTarget }?.tagNumber 
                        ?: allPigs.find { it.tagNumber == rawTarget }?.tagNumber 
                        ?: rawTarget
                }
            }

            val isOverdue = DateUtils.isTaskOverdue(first.date, locale)

            TaskGroup(
                activity = activity,
                target = target,
                date = DateUtils.convertToTaskDate(first.date, locale),
                isOverdue = isOverdue,
                originalTasks = group,
            )
        }.sortedByDescending { it.isOverdue }.toList()
    }

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

            val managementCategories = remember(currentLanguage) {
                listOf(
                    ManagementCategory(
                        label = Translator.getString("herd_data", currentLanguage.code),
                        iconResId = R.drawable.ic_herd_data,
                        screen = Screen.HerdData,
                        themeColor = Color(0xFF2E7D32),
                        themeColorDark = Color(0xFF81C784),
                        bgColorLight = Color(0xFFE8F5E9),
                        bgColorDark = Color(0xFF1B5E20)
                    ),
                    ManagementCategory(
                        label = Translator.getString("feed", currentLanguage.code),
                        iconResId = R.drawable.ic_feed2,
                        screen = Screen.Feed,
                        themeColor = Color(0xFFE65100),
                        themeColorDark = Color(0xFFFFB74D),
                        bgColorLight = Color(0xFFFFF3E0),
                        bgColorDark = Color(0xFFE65100)
                    ),
                    ManagementCategory(
                        label = Translator.getString("herd_activities", currentLanguage.code),
                        iconResId = R.drawable.ic_herd_activities,
                        screen = Screen.ProductionActivities,
                        themeColor = Color(0xFF00838F),
                        themeColorDark = Color(0xFF4DD0E1),
                        bgColorLight = Color(0xFFE0F7FA),
                        bgColorDark = Color(0xFF006064)
                    ),
                    ManagementCategory(
                        label = Translator.getString("financials", currentLanguage.code),
                        icon = Icons.Default.Payments,
                        screen = Screen.Financials,
                        themeColor = Color(0xFF00796B),
                        themeColorDark = Color(0xFF4DB6AC),
                        bgColorLight = Color(0xFFE0F2F1),
                        bgColorDark = Color(0xFF004D40)
                    ),
                    ManagementCategory(
                        label = Translator.getString("human_resources", currentLanguage.code),
                        icon = Icons.Default.Groups,
                        screen = Screen.HumanResource,
                        themeColor = Color(0xFF7B1FA2),
                        themeColorDark = Color(0xFFBA68C8),
                        bgColorLight = Color(0xFFF3E5F5),
                        bgColorDark = Color(0xFF4A148C)
                    ),
                    ManagementCategory(
                        label = Translator.getString("market", currentLanguage.code),
                        icon = Icons.Default.Storefront,
                        screen = Screen.MarketAccess,
                        themeColor = Color(0xFFC2185B),
                        themeColorDark = Color(0xFFF06292),
                        bgColorLight = Color(0xFFFFEBEE),
                        bgColorDark = Color(0xFF880E4F)
                    ),
                    ManagementCategory(
                        label = Translator.getString("symptoms_analyzer", currentLanguage.code),
                        iconResId = R.drawable.ic_symptoms_analyzer,
                        screen = Screen.DiseaseFinder,
                        themeColor = Color(0xFF3F51B5),
                        themeColorDark = Color(0xFF7986CB),
                        bgColorLight = Color(0xFFE8EAF6),
                        bgColorDark = Color(0xFF1A237E)
                    ),
                    ManagementCategory(
                        label = Translator.getString("weight_checker", currentLanguage.code),
                        iconResId = R.drawable.ic_weight_checker,
                        screen = Screen.WeightChecker,
                        themeColor = Color(0xFF455A64),
                        themeColorDark = Color(0xFF90A4AE),
                        bgColorLight = Color(0xFFECEFF1),
                        bgColorDark = Color(0xFF37474F)
                    ),
                    ManagementCategory(
                        label = Translator.getString("training", currentLanguage.code),
                        icon = Icons.Default.School,
                        screen = Screen.Training,
                        themeColor = Color(0xFF5D4037),
                        themeColorDark = Color(0xFFA1887F),
                        bgColorLight = Color(0xFFEFEBE9),
                        bgColorDark = Color(0xFF3E2723)
                    )
                )
            }

            managementCategories.forEach { item ->
                val isLocked = item.screen == Screen.DiseaseFinder && !LocalIsPremium.current
                val isDark = MaterialTheme.colorScheme.background == DarkBackground
                val primaryColor = if (isDark) item.themeColorDark else item.themeColor
                val bgColor = if (isDark) item.bgColorDark else item.bgColorLight
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 2.dp)
                        .height(105.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(20.dp),
                            clip = false,
                            ambientColor = if (isDark) primaryColor.copy(alpha = 0.3f) else primaryColor.copy(alpha = 0.35f),
                            spotColor = if (isDark) primaryColor.copy(alpha = 0.5f) else primaryColor.copy(alpha = 0.6f)
                        )
                        .border(
                            BorderStroke(
                                width = 1.dp,
                                color = primaryColor.copy(alpha = if (isDark) 0.3f else 0.4f)
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    bgColor.copy(alpha = if (isDark) 0.5f else 0.95f),
                                    bgColor.copy(alpha = if (isDark) 0.2f else 0.4f)
                                )
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            if (isLocked) {
                                onNavigateTo(Screen.Paywall.route)
                            } else {
                                onNavigateTo(item.screen.route) 
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.size(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                item.icon?.let { icon ->
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        tint = primaryColor
                                    )
                                } ?: item.iconResId?.let { resId ->
                                    Icon(
                                        painter = painterResource(id = resId),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        tint = primaryColor
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(20.dp))
                            Column {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = (-0.3).sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = Translator.getString(getCategorySubtitleKey(item.screen), currentLanguage.code),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Icon(
                            imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = if (isLocked) MaterialTheme.colorScheme.error else primaryColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = stringResource("upcoming_activities"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (groupedTasks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource("no_upcoming_activities"),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(groupedTasks) { taskGroup ->
                            ElevatedCard(
                                onClick = { 
                                    tasksToEditState.value = taskGroup.originalTasks 
                                    showNotificationBottomSheet.value = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = MaterialTheme.shapes.medium,
                                colors = if (taskGroup.isOverdue) {
                                    CardDefaults.elevatedCardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                } else {
                                    CardDefaults.elevatedCardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        when (val taskIcon = getTaskIcon(taskGroup.activity)) {
                                            is TaskIcon.Vector -> Icon(
                                                imageVector = taskIcon.imageVector,
                                                contentDescription = null,
                                                modifier = Modifier.size(28.dp),
                                                tint = if (taskGroup.isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                            )
                                            is TaskIcon.Resource -> Icon(
                                                painter = painterResource(id = taskIcon.resId),
                                                contentDescription = null,
                                                modifier = Modifier.size(28.dp),
                                                tint = if (taskGroup.isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = getTranslatedActivityName(taskGroup.activity),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = if (taskGroup.target == "General") stringResource("general") else taskGroup.target,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (taskGroup.isOverdue) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                    Text(
                                        text = taskGroup.date,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (taskGroup.isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}


@Composable
fun QuoteCard(quote: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.FormatQuote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = quote,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

private data class ManagementCategory(
    val label: String,
    val icon: ImageVector? = null,
    val iconResId: Int? = null,
    val screen: Screen,
    val themeColor: Color,
    val themeColorDark: Color,
    val bgColorLight: Color,
    val bgColorDark: Color
)

private data class TaskGroup(
    val activity: String,
    val target: String,
    val date: String,
    val isOverdue: Boolean,
    val originalTasks: List<TaskItem>
)

@Preview(showBackground = true)
@Composable
fun QuoteCardPreview() {
    SmartSwineTheme {
        QuoteCard(quote = "Agriculture is the most healthful, most useful and most noble employment of man. - George Washington")
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

private fun getCategorySubtitleKey(screen: Screen): String {
    return when (screen) {
        Screen.HerdData -> "sub_herd_data"
        Screen.Feed -> "sub_feed"
        Screen.ProductionActivities -> "sub_herd_activities"
        Screen.Financials -> "sub_financials"
        Screen.HumanResource -> "sub_human_resources"
        Screen.MarketAccess -> "sub_market"
        Screen.DiseaseFinder -> "sub_symptoms_analyzer"
        Screen.WeightChecker -> "sub_weight_checker"
        Screen.Training -> "sub_training"
        else -> ""
    }
}
