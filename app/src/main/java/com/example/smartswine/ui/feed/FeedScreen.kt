package com.example.smartswine.ui.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartswine.utils.StylishDivider
import com.example.smartswine.model.FeedIngredient
import com.example.smartswine.model.NutritionalRequirement
import com.example.smartswine.model.FeedInventoryItem
import com.example.smartswine.model.FeedInventoryTransaction
import com.example.smartswine.ui.navigation.Screen
import com.example.smartswine.ui.theme.SmartSwineTheme
import com.example.smartswine.util.PdfGenerator
import com.example.smartswine.utils.LocalIsPremium
import com.example.smartswine.utils.PremiumWrapper
import com.example.smartswine.utils.stringResource
import com.example.smartswine.utils.getCategoryKey
import com.example.smartswine.utils.getIngredientNameKey
import com.example.smartswine.utils.LocalAppLanguage
import com.example.smartswine.utils.Translator
import com.example.smartswine.ui.herd.HerdViewModel
import com.example.smartswine.ui.settings.SettingsViewModel
import android.util.Log
import java.util.*
import kotlinx.coroutines.launch

@Composable
fun getIngredientName(ingredient: FeedIngredient): String {
    val context = LocalContext.current
    val key = getIngredientNameKey(ingredient, context)
    val name = stringResource(key)
    return if (name == key && ingredient.name.isNotEmpty()) {
        ingredient.name
    } else {
        name
    }
}

@Suppress("unused")
@Composable
fun SimpleFlowRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val layoutWidth = constraints.maxWidth
        
        val rows = mutableListOf<List<androidx.compose.ui.layout.Placeable>>()
        var currentRow = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var currentRowWidth = 0
        val horizontalSpacing = 8.dp.roundToPx()
        val verticalSpacing = 8.dp.roundToPx()
        
        placeables.forEach { placeable ->
            if (currentRowWidth + placeable.width > layoutWidth && currentRow.isNotEmpty()) {
                rows.add(currentRow)
                currentRow = mutableListOf()
                currentRowWidth = 0
            }
            currentRow.add(placeable)
            currentRowWidth += placeable.width + horizontalSpacing
        }
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
        }
        
        val totalHeight = rows.sumOf { row -> row.maxOfOrNull { it.height } ?: 0 } +
                if (rows.size > 1) (rows.size - 1) * verticalSpacing else 0
        
        layout(layoutWidth, totalHeight) {
            var y = 0
            rows.forEach { row ->
                val rowHeight = row.maxOfOrNull { it.height } ?: 0
                var x = 0
                row.forEach { placeable ->
                    placeable.placeRelative(x, y)
                    x += placeable.width + horizontalSpacing
                }
                y += rowHeight + verticalSpacing
            }
        }
    }
}


@Composable
fun FeedScreen(
    viewModel: FeedViewModel = viewModel(),
    herdViewModel: HerdViewModel = viewModel(),
    onNavigateToPaywall: () -> Unit = {},
    onBack: () -> Unit = {},
    onNavigateTo: (String) -> Unit = {},
    initiallyShowCalculator: Boolean = false
) {
    val ingredients by viewModel.ingredients.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val isFormulating by viewModel.isFormulating.collectAsStateWithLifecycle()
    val nutritionalRequirements by viewModel.nutritionalRequirements.collectAsStateWithLifecycle()
    val herdStats by herdViewModel.stats.collectAsStateWithLifecycle()
    val feedInventoryItems by viewModel.feedInventoryItems.collectAsStateWithLifecycle()
    val feedInventoryTransactions by viewModel.feedInventoryTransactions.collectAsStateWithLifecycle()

    val currentLanguage = LocalAppLanguage.current
    LaunchedEffect(currentLanguage) {
        viewModel.setLanguage(currentLanguage.code)
    }

    val context = LocalContext.current
    val showExportDialog = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val lang = LocalAppLanguage.current.code

    val isPremium = LocalIsPremium.current

    val settingsViewModel = remember { SettingsViewModel.getInstance() }
    val currencySymbol by settingsViewModel.currencySymbol.collectAsStateWithLifecycle()

    FeedScreenContent(
        ingredients = ingredients,
        nutritionalRequirements = nutritionalRequirements,
        isLoading = isLoading,
        error = error,
        isFormulating = isFormulating,
        herdStats = herdStats,
        isPremium = isPremium,
        initiallyShowCalculator = initiallyShowCalculator,
        feedInventoryItems = feedInventoryItems,
        feedInventoryTransactions = feedInventoryTransactions,
        currencySymbol = currencySymbol,
        onCalculateRequirements = { stats ->
            viewModel.calculateRequirements(stats)
            onNavigateTo(Screen.FeedCalculationResult.route)
        },
        onFormulateFeed = { stage, selectedIds ->
            Log.d("FeedScreen", "Triggering formulation for $stage")
            viewModel.formulateFeed(stage, selectedIds)
            onNavigateTo(Screen.FeedFormulationResult.route)
        },
        onClearError = viewModel::clearError,
        onAddIngredient = viewModel::addIngredient,
        onAddFeedInventoryItem = viewModel::addFeedInventoryItem,
        onDeleteFeedInventoryItem = viewModel::deleteFeedInventoryItem,
        onRestockFeedItem = viewModel::restockFeedItem,
        onUseFeedItem = viewModel::useFeedItem,
        onShowExport = { 
            if (!isPremium) onNavigateToPaywall()
            else showExportDialog.value = true 
        },
        onNavigateToPaywall = onNavigateToPaywall,
        onBack = onBack
    )

    if (showExportDialog.value) {
        FeedExportDialog(
            feedInventoryItems = feedInventoryItems,
            onDismiss = { showExportDialog.value = false },
            onExport = { itemsToExport, title, startDate, endDate ->
                coroutineScope.launch {
                    val transactions = viewModel.getFeedInventoryTransactionsForExport(startDate, endDate)
                    PdfGenerator.generateFeedReportPdf(context, itemsToExport, transactions, title, lang)
                    showExportDialog.value = false
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedExportDialog(
    feedInventoryItems: List<FeedInventoryItem>,
    onDismiss: () -> Unit,
    onExport: (List<FeedInventoryItem>, String, String, String) -> Unit
) {
    val selectedRange = remember { mutableStateOf("Current Month") }
    val ranges = listOf("Current Month", "Last 3 Months", "Custom")
    val showDatePickerFrom = remember { mutableStateOf(false) }
    val showDatePickerTo = remember { mutableStateOf(false) }
    val fromDate = remember { mutableStateOf("") }
    val toDate = remember { mutableStateOf("") }
    
    val datePickerStateFrom = rememberDatePickerState()
    val datePickerStateTo = rememberDatePickerState()

    if (showDatePickerFrom.value) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerFrom.value = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerStateFrom.selectedDateMillis?.let {
                        val calendar = Calendar.getInstance().apply { timeInMillis = it }
                        fromDate.value = String.format(Locale.getDefault(), "%04d-%02d-%02d", 
                            calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))
                    }
                    showDatePickerFrom.value = false
                }) { Text(stringResource("ok")) }
            }
        ) { DatePicker(state = datePickerStateFrom) }
    }

    if (showDatePickerTo.value) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerTo.value = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerStateTo.selectedDateMillis?.let {
                        val calendar = Calendar.getInstance().apply { timeInMillis = it }
                        toDate.value = String.format(Locale.getDefault(), "%04d-%02d-%02d", 
                            calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))
                    }
                    showDatePickerTo.value = false
                }) { Text(stringResource("ok")) }
            }
        ) { DatePicker(state = datePickerStateTo) }
    }

    val lang = LocalAppLanguage.current.code

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource("export_feed_data")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource("select_range"), style = MaterialTheme.typography.titleSmall)
                ranges.forEach { range ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedRange.value == range, onClick = { selectedRange.value = range })
                        Text(when(range) {
                            "Current Month" -> stringResource("current_month")
                            "Last 3 Months" -> stringResource("last_3_months")
                            "Custom" -> stringResource("custom")
                            else -> range
                        })
                    }
                }
                if (selectedRange.value == "Custom") {
                    OutlinedTextField(
                        value = fromDate.value, onValueChange = {}, label = { Text(stringResource("from_date")) }, readOnly = true, modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { IconButton(onClick = { showDatePickerFrom.value = true }) { Icon(Icons.Default.DateRange, null) } }
                    )
                    OutlinedTextField(
                        value = toDate.value, onValueChange = {}, label = { Text(stringResource("to_date")) }, readOnly = true, modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { IconButton(onClick = { showDatePickerTo.value = true }) { Icon(Icons.Default.DateRange, null) } }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val calendar = Calendar.getInstance()
                val endDateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))
                
                val startDateStr = when(selectedRange.value) {
                    "Current Month" -> {
                        String.format(Locale.getDefault(), "%04d-%02d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, 1)
                    }
                    "Last 3 Months" -> {
                        calendar.add(Calendar.MONTH, -3)
                        String.format(Locale.getDefault(), "%04d-%02d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))
                    }
                    "Custom" -> fromDate.value
                    else -> "1970-01-01"
                }
                
                val finalEndDate = if (selectedRange.value == "Custom") toDate.value else endDateStr

                val rangeLabel = when(selectedRange.value) {
                    "Current Month" -> Translator.getString("current_month", lang)
                    "Last 3 Months" -> Translator.getString("last_3_months", lang)
                    "Custom" -> Translator.getString("custom", lang)
                    else -> selectedRange.value
                }
                val title = "${Translator.getString("feed_inventory_report", lang)} - $rangeLabel" + if (selectedRange.value == "Custom") " (${startDateStr} to ${finalEndDate})" else ""
                onExport(feedInventoryItems, title, startDateStr, finalEndDate)
            }) { Text(stringResource("export")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource("cancel")) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FeedScreenContent(
    ingredients: List<FeedIngredient> = emptyList(),
    nutritionalRequirements: List<NutritionalRequirement> = emptyList(),
    isLoading: Boolean = false,
    error: String? = null,
    isFormulating: Boolean = false,
    herdStats: Map<String, Any> = emptyMap(),
    isPremium: Boolean = false,
    initiallyShowCalculator: Boolean = false,
    feedInventoryItems: List<FeedInventoryItem> = emptyList(),
    feedInventoryTransactions: List<FeedInventoryTransaction> = emptyList(),
    currencySymbol: String = "$",
    onCalculateRequirements: (Map<String, Any>) -> Unit = {},
    onFormulateFeed: (String, List<String>) -> Unit = { _, _ -> },
    onClearError: () -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onAddIngredient: (FeedIngredient) -> Unit = {},
    onAddFeedInventoryItem: (String, String, Double, String, Double, Double) -> Unit = { _, _, _, _, _, _ -> },
    onDeleteFeedInventoryItem: (String) -> Unit = {},
    onRestockFeedItem: (String, Double, String, Double, String) -> Unit = { _, _, _, _, _ -> },
    onUseFeedItem: (String, Double, String, String) -> Unit = { _, _, _, _ -> },
    onShowExport: () -> Unit = {},
    onNavigateToPaywall: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val showCalculatorDialog = remember { mutableStateOf(initiallyShowCalculator) }
    val showFormulatorDialog = remember { mutableStateOf(false) }
    
    val showAddFeedDialog = remember { mutableStateOf(false) }
    val showRestockDialogItem = remember { mutableStateOf<FeedInventoryItem?>(null) }
    val showUseDialogItem = remember { mutableStateOf<FeedInventoryItem?>(null) }
    val showDeleteConfirmItem = remember { mutableStateOf<FeedInventoryItem?>(null) }

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
                        text = stringResource("feed"),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    PremiumWrapper(isPremium = isPremium, onLockedClick = onNavigateToPaywall) {
                        IconButton(onClick = onShowExport) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = stringResource("export_pdf"))
                        }
                    }
                }
                StylishDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            error?.let {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(it, color = MaterialTheme.colorScheme.onErrorContainer)
                        IconButton(onClick = onClearError) {
                            Icon(Icons.Default.Close, "Clear Error")
                        }
                    }
                }
            }

            // Formulation - Quick Actions
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { showCalculatorDialog.value = true },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(Icons.Default.Calculate, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource("feed_calculator"), style = MaterialTheme.typography.titleMedium)
                }
                
                StylishDivider(modifier = Modifier.padding(vertical = 8.dp))

                Button(
                    onClick = { 
                        if (!isPremium) onNavigateToPaywall()
                        else showFormulatorDialog.value = true 
                    },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = if (!isPremium) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors()
                ) {
                    Icon(if (!isPremium) Icons.Default.Lock else Icons.Default.Science, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource("feed_formulator"), style = MaterialTheme.typography.titleMedium)
                }
            }

            StylishDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Stylish Header for Feed Inventory
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.medium
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource("feed_inventory"),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource("manage_stock"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Button(
                    onClick = { showAddFeedDialog.value = true },
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource("add_feed_item"), style = MaterialTheme.typography.labelMedium)
                }
            }

            // Contents of the Inventory subheading screen
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (feedInventoryItems.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "No feed items tracked yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Add feed items like 'Starter Feed' or 'Sow Mix' to track restocking and daily usage.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    feedInventoryItems.forEach { item ->
                        val isLowStock = item.quantity <= item.minThreshold
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = if (isLowStock) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${stringResource("feed_type")}: ${stringResource(item.feedType.lowercase())}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isLowStock) {
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error),
                                                shape = MaterialTheme.shapes.extraSmall,
                                                modifier = Modifier.padding(end = 8.dp)
                                            ) {
                                                Text(
                                                    text = stringResource("low_stock_warning").uppercase(),
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onError,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        
                                        IconButton(onClick = { showDeleteConfirmItem.value = item }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.outline)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource("in_stock"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        val unitStr = if (item.unit == "bags") stringResource("bags_abbr") else stringResource("kg_abbr")
                                        Text(
                                            text = "${String.format(Locale.getDefault(), "%.1f", item.quantity)} $unitStr",
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Black,
                                            color = if (isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        )
                                        if (item.unit == "bags" && item.unitWeight > 0.0) {
                                            Text(
                                                text = "(${String.format(Locale.getDefault(), "%.1f", item.quantity * item.unitWeight)} kg total)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Column(
                                        modifier = Modifier.width(IntrinsicSize.Max),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        OutlinedButton(
                                            onClick = { showUseDialogItem.value = item },
                                            shape = MaterialTheme.shapes.medium,
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Remove, null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(stringResource("log_usage"))
                                        }
                                        Button(
                                            onClick = { showRestockDialogItem.value = item },
                                            shape = MaterialTheme.shapes.medium,
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(stringResource("restock"))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (feedInventoryTransactions.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource("transaction_history"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            feedInventoryTransactions.take(10).forEach { tx ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val isRestock = tx.type == "Restock"
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(
                                                if (isRestock) MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.secondaryContainer,
                                                shape = MaterialTheme.shapes.small
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isRestock) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                            contentDescription = null,
                                            tint = if (isRestock) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tx.itemName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                text = if (isRestock) stringResource("restock") else stringResource("log_usage"),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isRestock) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "•",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                            val formattedDate = remember(tx.date) {
                                                try {
                                                    val parts = tx.date.split("T")
                                                    if (parts.isNotEmpty()) parts[0] else tx.date
                                                } catch (_: Exception) {
                                                    tx.date
                                                }
                                            }
                                            Text(
                                                text = formattedDate,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                        if (tx.notes.isNotEmpty()) {
                                            Text(
                                                text = tx.notes,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        val changeSign = if (isRestock) "+" else "-"
                                        val unitStr = if (tx.unit == "bags") stringResource("bags_abbr") else stringResource("kg_abbr")
                                        Text(
                                            text = "$changeSign${String.format(Locale.getDefault(), "%.1f", tx.quantity)} $unitStr",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isRestock) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                        )
                                        if (isRestock && tx.cost > 0.0) {
                                            Text(
                                                text = "$currencySymbol${tx.cost}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(120.dp))
        }
    }

    if (showCalculatorDialog.value) {
        FeedCalculatorDialog(
            herdStats = herdStats,
            onDismiss = { showCalculatorDialog.value = false },
            onCalculate = { stats ->
                onCalculateRequirements(stats)
                showCalculatorDialog.value = false
            }
        )
    }

    if (showFormulatorDialog.value) {
        FeedFormulatorDialog(
            ingredients = ingredients,
            requirements = nutritionalRequirements,
            isFormulating = isFormulating,
            onDismiss = { showFormulatorDialog.value = false },
            onFormulate = { stage, selectedIds ->
                onFormulateFeed(stage, selectedIds)
                showFormulatorDialog.value = false
            }
        )
    }

    if (showAddFeedDialog.value) {
        AddFeedInventoryItemDialog(
            onDismiss = { showAddFeedDialog.value = false },
            onConfirm = { name, type, qty, unit, bagWeight, threshold ->
                onAddFeedInventoryItem(name, type, qty, unit, bagWeight, threshold)
                showAddFeedDialog.value = false
            }
        )
    }

    if (showRestockDialogItem.value != null) {
        val item = showRestockDialogItem.value!!
        RestockFeedDialog(
            item = item,
            currencySymbol = currencySymbol,
            onDismiss = { showRestockDialogItem.value = null },
            onConfirm = { qty, unit, cost, notes ->
                onRestockFeedItem(item.id, qty, unit, cost, notes)
                showRestockDialogItem.value = null
            }
        )
    }

    if (showUseDialogItem.value != null) {
        val item = showUseDialogItem.value!!
        UseFeedDialog(
            item = item,
            onDismiss = { showUseDialogItem.value = null },
            onConfirm = { qty, unit, notes ->
                onUseFeedItem(item.id, qty, unit, notes)
                showUseDialogItem.value = null
            }
        )
    }

    if (showDeleteConfirmItem.value != null) {
        val item = showDeleteConfirmItem.value!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirmItem.value = null },
            title = { Text(stringResource("delete_feed_item")) },
            text = { Text(stringResource("delete_feed_confirm")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteFeedInventoryItem(item.id)
                        showDeleteConfirmItem.value = null
                    }
                ) { Text(stringResource("delete"), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmItem.value = null }) { Text(stringResource("cancel")) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedCalculatorDialog(
    herdStats: Map<String, Any>,
    onDismiss: () -> Unit,
    onCalculate: (Map<String, Any>) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    var daysText by remember { mutableStateOf("1") }

    val categories = listOf(
        "sows" to Icons.Default.Female,
        "boars" to Icons.Default.Male,
        "gilts" to Icons.Default.Girl,
        "Pregnant" to Icons.Default.Favorite,
        "Lactating" to Icons.Default.ChildCare,
        "Starter" to Icons.AutoMirrored.Filled.TrendingUp,
        "Grower" to Icons.Default.Scale,
        "Finisher" to Icons.Default.DoneAll
    )

    val editableStats = remember {
        mutableStateMapOf<String, String>().apply {
            categories.forEach { (key, _) ->
                put(key, ((herdStats[key] as? Number)?.toInt() ?: 0).toString())
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (step == 1) stringResource("animal_inventory") else stringResource("calculation_period"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                LinearProgressIndicator(
                    progress = { if (step == 1) 0.5f else 1f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (step == 1) {
                    Text(
                        stringResource("verify_animal_counts"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    categories.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            row.forEach { (key, icon) ->
                                OutlinedTextField(
                                    value = editableStats[key] ?: "0",
                                    onValueChange = { newValue ->
                                        if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                            editableStats[key] = newValue
                                        }
                                    },
                                    label = { Text(stringResource(getCategoryKey(key))) },
                                    leadingIcon = { Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = MaterialTheme.shapes.medium
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.DateRange,
                                null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                stringResource("duration_projections"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                stringResource("how_many_days"),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(24.dp))
                            OutlinedTextField(
                                value = daysText,
                                onValueChange = { newValue ->
                                    if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                        daysText = newValue
                                    }
                                },
                                label = { Text(stringResource("number_of_days")) },
                                suffix = { Text(stringResource("days")) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (step == 1) {
                        step = 2
                    } else {
                        val finalStats = editableStats.mapValues { it.value.toIntOrNull() ?: 0 }.toMutableMap()
                        finalStats["days"] = daysText.toIntOrNull() ?: 1
                        onCalculate(finalStats)
                    }
                },
                shape = MaterialTheme.shapes.medium,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(if (step == 1) stringResource("next") else stringResource("generate_report"))
                if (step == 1) {
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(16.dp))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (step == 2) step = 1 else onDismiss()
                }
            ) {
                Text(if (step == 2) stringResource("previous") else stringResource("cancel"))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedFormulatorDialog(
    ingredients: List<FeedIngredient>,
    requirements: List<NutritionalRequirement>,
    isFormulating: Boolean,
    onDismiss: () -> Unit,
    onFormulate: (String, List<String>) -> Unit
) {
    val growthStages = remember(requirements) {
        requirements.filter { it.stage in listOf("Starter", "Grower", "Finisher") }
    }
    val selectedStage = remember { mutableStateOf(growthStages.firstOrNull()?.stage ?: "Starter") }
    val selectedIngredients = remember { mutableStateListOf<String>() }
    val expandedStage = remember { mutableStateOf(false) }
    
    val mandatoryNames = listOf("Mycotoxin Binder", "Common salt", "Vitamin Premix")
    
    // Group ingredients by category, filtering out mandatory ones
    val groupedIngredients = remember(ingredients) {
        val categoryOrder = listOf("Energy", "Protein", "Vitamins, Minerals & Salt")
        ingredients
            .filter { ing -> mandatoryNames.none { mandatory -> ing.name.contains(mandatory, ignoreCase = true) } }
            .groupBy { 
                val cat = it.mainCategory.ifBlank { it.category }
                if (cat.isBlank()) "Uncategorized" else cat
            }
            .toSortedMap(compareBy<String> { 
                val index = categoryOrder.indexOf(it)
                if (index == -1) Int.MAX_VALUE else index 
            }.thenBy { it })
    }
    
    // State to track which categories are expanded in the UI
    val expandedCategories = remember { mutableStateMapOf<String, Boolean>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource("feed_formulator")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource("select_target_stage"), style = MaterialTheme.typography.titleSmall)
                
                ExposedDropdownMenuBox(
                    expanded = expandedStage.value,
                    onExpandedChange = { expandedStage.value = !expandedStage.value }
                ) {
                    OutlinedTextField(
                        value = stringResource(getCategoryKey(selectedStage.value)),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource("growth_stage")) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedStage.value) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedStage.value,
                        onDismissRequest = { expandedStage.value = false }
                    ) {
                        growthStages.forEach { req ->
                            DropdownMenuItem(
                                text = { Text(stringResource(getCategoryKey(req.stage))) },
                                onClick = {
                                    selectedStage.value = req.stage
                                    expandedStage.value = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(8.dp))
                Text(stringResource("select_available_ingredients"), style = MaterialTheme.typography.titleSmall)
                
                LazyColumn(modifier = Modifier.height(400.dp)) {
                    groupedIngredients.forEach { (category, categoryIngredients) ->
                        item(key = "header_$category") {
                            val isExpanded = expandedCategories[category] ?: false
                            Surface(
                                onClick = { expandedCategories[category] = !isExpanded },
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(getCategoryKey(category)),
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    val count = categoryIngredients.count { selectedIngredients.contains(it.id) }
                                    if (count > 0) {
                                        Badge { Text("$count") }
                                    }
                                }
                            }
                        }
                        
                        if (expandedCategories[category] == true) {
                            items(
                                items = categoryIngredients,
                                key = { ingredient -> "ingredient_${category}_${ingredient.id}" }
                            ) { ingredient ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (selectedIngredients.contains(ingredient.id)) {
                                                selectedIngredients.remove(ingredient.id)
                                            } else {
                                                selectedIngredients.add(ingredient.id)
                                            }
                                        }
                                        .padding(start = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = selectedIngredients.contains(ingredient.id),
                                        onCheckedChange = {
                                            if (it) selectedIngredients.add(ingredient.id) else selectedIngredients.remove(ingredient.id)
                                        }
                                    )
                                    Text(
                                        text = getIngredientName(ingredient),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(vertical = 12.dp)
                                    )
                                }
                            }
                        }
                        
                        item(key = "spacer_$category") { Spacer(Modifier.height(4.dp)) }
                    }
                }
                
                Text(
                    stringResource("automatic_additives_note"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onFormulate(selectedStage.value, selectedIngredients) },
                enabled = !isFormulating && selectedIngredients.isNotEmpty()
            ) {
                if (isFormulating) CircularProgressIndicator(modifier = Modifier.size(16.dp))
                else Text(stringResource("run_formulator"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource("cancel")) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIngredientDialog(
    onDismiss: () -> Unit,
    onConfirm: (FeedIngredient) -> Unit
) {
    val name = remember { mutableStateOf("") }
    val cp = remember { mutableStateOf("") }
    val energy = remember { mutableStateOf("") }
    val fiber = remember { mutableStateOf("") }
    val calcium = remember { mutableStateOf("") }
    val phos = remember { mutableStateOf("") }
    val salt = remember { mutableStateOf("") }
    val lysine = remember { mutableStateOf("") }
    val methionine = remember { mutableStateOf("") }
    
    val mainCategory = remember { mutableStateOf("Energy") }
    val categories = listOf("Energy", "Protein", "Vitamins, Minerals & Salt")
    val expanded = remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource("new_feed_ingredient")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = name.value, onValueChange = { name.value = it }, label = { Text(stringResource("ingredient_name")) }, modifier = Modifier.fillMaxWidth())
                
                ExposedDropdownMenuBox(
                    expanded = expanded.value,
                    onExpandedChange = { expanded.value = !expanded.value }
                ) {
                    OutlinedTextField(
                        value = stringResource(getCategoryKey(mainCategory.value)),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource("main_category")) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded.value) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded.value,
                        onDismissRequest = { expanded.value = false }
                    ) {
                        categories.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(stringResource(getCategoryKey(selectionOption))) },
                                onClick = {
                                    mainCategory.value = selectionOption
                                    expanded.value = false
                                }
                            )
                        }
                    }
                }
                
                OutlinedTextField(value = cp.value, onValueChange = { cp.value = it }, label = { Text(stringResource("crude_protein_pct")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = energy.value, onValueChange = { energy.value = it }, label = { Text(stringResource("me_kcal_kg")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = fiber.value, onValueChange = { fiber.value = it }, label = { Text(stringResource("crude_fiber_pct")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = calcium.value, onValueChange = { calcium.value = it }, label = { Text(stringResource("calcium_pct")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phos.value, onValueChange = { phos.value = it }, label = { Text(stringResource("phosphorus_pct")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = salt.value, onValueChange = { salt.value = it }, label = { Text(stringResource("salt_pct")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = lysine.value, onValueChange = { lysine.value = it }, label = { Text(stringResource("lysine_pct")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = methionine.value, onValueChange = { methionine.value = it }, label = { Text(stringResource("methionine_pct")) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val ingredient = FeedIngredient(
                        name = name.value,
                        crudeProtein = cp.value.toDoubleOrNull() ?: 0.0,
                        metabolizableEnergy = energy.value.toDoubleOrNull() ?: 0.0,
                        crudeFiber = fiber.value.toDoubleOrNull() ?: 0.0,
                        calcium = calcium.value.toDoubleOrNull() ?: 0.0,
                        phosphorus = phos.value.toDoubleOrNull() ?: 0.0,
                        sodium = (salt.value.toDoubleOrNull() ?: 0.0) * 0.4,
                        lysine = lysine.value.toDoubleOrNull() ?: 0.0,
                        methionine = methionine.value.toDoubleOrNull() ?: 0.0,
                        mainCategory = mainCategory.value,
                        category = mainCategory.value // Populate both for backward compatibility
                    )
                    onConfirm(ingredient)
                },
                enabled = name.value.isNotEmpty()
            ) { Text(stringResource("save_ingredient")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource("cancel")) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFeedInventoryItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: String, qty: Double, unit: String, bagWeight: Double, threshold: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("Starter") }
    var initialStockText by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("bags") }
    var bagWeightText by remember { mutableStateOf("50") }
    var thresholdText by remember { mutableStateOf("5") }
    var typeDropdownExpanded by remember { mutableStateOf(false) }

    val feedTypes = listOf("Starter", "Grower", "Finisher", "Sow", "Boar", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource("add_feed_item")) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource("feed_name")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                ExposedDropdownMenuBox(
                    expanded = typeDropdownExpanded,
                    onExpandedChange = { typeDropdownExpanded = !typeDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = stringResource(selectedType.lowercase()),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource("feed_type")) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = typeDropdownExpanded,
                        onDismissRequest = { typeDropdownExpanded = false }
                    ) {
                        feedTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(stringResource(type.lowercase())) },
                                onClick = {
                                    selectedType = type
                                    typeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Unit:", style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = unit == "bags", onClick = { unit = "bags" })
                        Text(
                            text = stringResource("unit_bags"),
                            modifier = Modifier.clickable { unit = "bags" }.padding(start = 4.dp)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = unit == "kg", onClick = { unit = "kg" })
                        Text(
                            text = stringResource("unit_kg"),
                            modifier = Modifier.clickable { unit = "kg" }.padding(start = 4.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = initialStockText,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) initialStockText = it },
                    label = { Text(stringResource("initial_stock")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (unit == "bags") {
                    OutlinedTextField(
                        value = bagWeightText,
                        onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) bagWeightText = it },
                        label = { Text(stringResource("bag_weight_kg")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = thresholdText,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) thresholdText = it },
                    label = { Text(stringResource("min_threshold")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = initialStockText.toDoubleOrNull() ?: 0.0
                    val bagWeight = if (unit == "bags") (bagWeightText.toDoubleOrNull() ?: 50.0) else 0.0
                    val threshold = thresholdText.toDoubleOrNull() ?: 0.0
                    onConfirm(name, selectedType, qty, unit, bagWeight, threshold)
                },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource("save"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource("cancel"))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestockFeedDialog(
    item: FeedInventoryItem,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (qty: Double, unit: String, cost: Double, notes: String) -> Unit
) {
    var qtyText by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf(item.unit) }
    var costText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource("restock") + ": ${item.name}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) qtyText = it },
                    label = { Text(stringResource("qty")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Unit:", style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = unit == "bags", onClick = { unit = "bags" })
                        Text(
                            text = stringResource("unit_bags"),
                            modifier = Modifier.clickable { unit = "bags" }.padding(start = 4.dp)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = unit == "kg", onClick = { unit = "kg" })
                        Text(
                            text = stringResource("unit_kg"),
                            modifier = Modifier.clickable { unit = "kg" }.padding(start = 4.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = costText,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) costText = it },
                    label = { Text("${stringResource("cost")} ($currencySymbol)") },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource("notes_optional")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            val qty = qtyText.toDoubleOrNull() ?: 0.0
            Button(
                onClick = {
                    val cost = costText.toDoubleOrNull() ?: 0.0
                    onConfirm(qty, unit, cost, notes)
                },
                enabled = qty > 0.0
            ) {
                Text(stringResource("ok"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource("cancel"))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UseFeedDialog(
    item: FeedInventoryItem,
    onDismiss: () -> Unit,
    onConfirm: (qty: Double, unit: String, notes: String) -> Unit
) {
    var qtyText by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf(item.unit) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource("log_usage") + ": ${item.name}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) qtyText = it },
                    label = { Text(stringResource("qty")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Unit:", style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = unit == "bags", onClick = { unit = "bags" })
                        Text(
                            text = stringResource("unit_bags"),
                            modifier = Modifier.clickable { unit = "bags" }.padding(start = 4.dp)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = unit == "kg", onClick = { unit = "kg" })
                        Text(
                            text = stringResource("unit_kg"),
                            modifier = Modifier.clickable { unit = "kg" }.padding(start = 4.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource("notes_optional")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            val qty = qtyText.toDoubleOrNull() ?: 0.0
            Button(
                onClick = {
                    onConfirm(qty, unit, notes)
                },
                enabled = qty > 0.0
            ) {
                Text(stringResource("ok"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource("cancel"))
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun FeedScreenPreview() {
    SmartSwineTheme {
        FeedScreenContent(
            ingredients = listOf(
                FeedIngredient("1", "Maize", 0, 500.0, 1.2),
                FeedIngredient("2", "Soybean Meal", 0, 200.0, 2.5)
            ),
            herdStats = mapOf("sows" to 10, "finishers" to 50)
        )
    }
}
