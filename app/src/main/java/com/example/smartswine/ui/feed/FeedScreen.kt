package com.example.smartswine.ui.feed

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartswine.model.FeedIngredient
import com.example.smartswine.model.FeedInventoryItem
import com.example.smartswine.model.FeedInventoryTransaction
import com.example.smartswine.model.NutritionalRequirement
import com.example.smartswine.ui.feed.components.*
import com.example.smartswine.ui.components.NativeAdCard
import com.example.smartswine.ui.components.RewardedPassDialog
import com.example.smartswine.ui.herd.HerdViewModel
import com.example.smartswine.ui.navigation.Screen
import com.example.smartswine.ui.settings.SettingsViewModel
import com.example.smartswine.ui.theme.DarkBackground
import com.example.smartswine.ui.theme.SmartSwineTheme
import com.example.smartswine.util.PdfGenerator
import com.example.smartswine.utils.LocalAppLanguage
import com.example.smartswine.utils.LocalIsPaidPremium
import com.example.smartswine.utils.LocalIsPremium
import com.example.smartswine.utils.PremiumWrapper
import com.example.smartswine.utils.StylishDivider
import com.example.smartswine.utils.getIngredientNameKey
import com.example.smartswine.utils.stringResource
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun getIngredientName(ingredient: FeedIngredient): String {
    val context = LocalContext.current
    val key = getIngredientNameKey(ingredient, context)
    val name = stringResource(key)
    return if ((name == key) && ingredient.name.isNotEmpty()) {
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
    initiallyShowCalculator: Boolean = false,
    initiallyShowFormulator: Boolean = false
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
    val showExportDialog = remember { mutableStateOf(value = false) }
    val coroutineScope = rememberCoroutineScope()

    val lang = LocalAppLanguage.current.code
    val isPremium = LocalIsPremium.current
    var showRewardedPassDialog by remember { mutableStateOf(false) }

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
        initiallyShowFormulator = initiallyShowFormulator,
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
        onAddFeedInventoryItem = { name, type, qty, unit, bagWeight, threshold, cost, category ->
            viewModel.addFeedInventoryItem(name, type, qty, unit, bagWeight, threshold, cost, category)
        },
        onDeleteFeedInventoryItem = viewModel::deleteFeedInventoryItem,
        onRestockFeedItem = viewModel::restockFeedItem,
        onUseFeedItem = viewModel::useFeedItem,
        onShowExport = { 
            if (!isPremium) showRewardedPassDialog = true
            else showExportDialog.value = true 
        },
        onNavigateToPaywall = onNavigateToPaywall,
        onNavigateTo = onNavigateTo,
        onBack = onBack
    )

    if (showRewardedPassDialog) {
        RewardedPassDialog(
            title = stringResource("unlock_feed_pdf_title"),
            description = stringResource("unlock_feed_pdf_desc"),
            onDismiss = { showRewardedPassDialog = false },
            onNavigateToPaywall = onNavigateToPaywall,
            onPassActivated = {
                showExportDialog.value = true
            }
        )
    }

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
    initiallyShowFormulator: Boolean = false,
    feedInventoryItems: List<FeedInventoryItem> = emptyList(),
    feedInventoryTransactions: List<FeedInventoryTransaction> = emptyList(),
    currencySymbol: String = "$",
    onCalculateRequirements: (Map<String, Any>) -> Unit = {},
    onFormulateFeed: (String, List<String>) -> Unit = { _, _ -> },
    onClearError: () -> Unit = {},
    @Suppress("UNUSED_PARAMETER") onAddIngredient: (FeedIngredient) -> Unit = {},
    onAddFeedInventoryItem: (name: String, feedType: String, initialQty: Double, unit: String, unitWeight: Double, minThreshold: Double, costPerUnit: Double, itemCategory: String) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onDeleteFeedInventoryItem: (String) -> Unit = {},
    onRestockFeedItem: (String, Double, String, Double, String) -> Unit = { _, _, _, _, _ -> },
    onUseFeedItem: (String, Double, String, String) -> Unit = { _, _, _, _ -> },
    onShowExport: () -> Unit = {},
    onNavigateToPaywall: () -> Unit = {},
    onNavigateTo: (String) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val showCalculatorDialog = remember { mutableStateOf(initiallyShowCalculator) }
    val showFormulatorDialog = remember { mutableStateOf(initiallyShowFormulator) }
    val showItemTypeChoiceDialog = remember { mutableStateOf(false) }
    val showAddIngredientDialog = remember { mutableStateOf(false) }
    val showAddFeedDialog = remember { mutableStateOf(false) }
    val showRestockDialogItem = remember { mutableStateOf<FeedInventoryItem?>(null) }
    val showUseDialogItem = remember { mutableStateOf<FeedInventoryItem?>(null) }
    val showDeleteConfirmItem = remember { mutableStateOf<FeedInventoryItem?>(null) }
    var isTxHistoryExpanded by rememberSaveable { mutableStateOf(false) }

    val isDark = MaterialTheme.colorScheme.background == DarkBackground
    val feedPrimary = if (isDark) Color(0xFFFFB74D) else Color(0xFFE65100)
    val feedBg = if (isDark) Color(0xFFE65100) else Color(0xFFFFF3E0)

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
                        text = stringResource("feed_inventory"),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFE65100),
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
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Color(0xFFE65100))
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

            // Feed Inventory Header
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
                                color = Color(0xFFFFF3E0),
                                shape = MaterialTheme.shapes.medium
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory,
                            contentDescription = null,
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource("feed_inventory"),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFE65100)
                        )
                        Text(
                            text = stringResource("manage_stock"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Button(
                    onClick = { showItemTypeChoiceDialog.value = true },
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource("add_item"), style = MaterialTheme.typography.labelMedium)
                }
            }

            // Inventory Items List
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
                                text = stringResource("no_feed_items_title"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource("no_feed_items_desc"),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    feedInventoryItems.forEach { item ->
                        val isLowStock = item.quantity <= item.minThreshold
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isLowStock) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, if (isLowStock) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else feedPrimary.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isLowStock) MaterialTheme.colorScheme.error else feedPrimary
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            val isIngredient = item.itemCategory == "Feed Ingredient"
                                            Surface(
                                                color = if (isIngredient) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = if (isIngredient) stringResource("feed_ingredient") else stringResource("complete_feed"),
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                    color = if (isIngredient) Color(0xFF2E7D32) else Color(0xFFE65100),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            val typeKey = item.feedType.lowercase().replace('/', '_')
                                            val translatedType = stringResource(typeKey)
                                            val displayType = if (translatedType != typeKey) translatedType else item.feedType
                                            Text(
                                                text = "${stringResource("feed_type")}: $displayType",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isLowStock) {
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error),
                                                shape = MaterialTheme.shapes.extraSmall,
                                                modifier = Modifier.padding(end = 6.dp)
                                            ) {
                                                Text(
                                                    text = stringResource("low_stock_warning").uppercase(),
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onError,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        
                                        IconButton(
                                            onClick = { showDeleteConfirmItem.value = item },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = stringResource("delete"), tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }

                                val unitStr = if (item.unit == "bags") stringResource("bags_abbr") else stringResource("kg_abbr")
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = stringResource("in_stock") + ": ",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${String.format(Locale.getDefault(), "%.1f", item.quantity)} $unitStr",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black,
                                            color = if (isLowStock) MaterialTheme.colorScheme.error else feedPrimary
                                        )
                                    }
                                    if (item.unit == "bags" && item.unitWeight > 0.0) {
                                        Text(
                                            text = "(${String.format(Locale.getDefault(), "%.1f", item.quantity * item.unitWeight)} kg total)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { showUseDialogItem.value = item },
                                        shape = MaterialTheme.shapes.medium,
                                        border = BorderStroke(1.dp, feedPrimary),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = feedPrimary),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, null, modifier = Modifier.size(14.dp), tint = feedPrimary)
                                        Spacer(Modifier.width(4.dp))
                                        Text(stringResource("log_usage"), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    }
                                    Button(
                                        onClick = { showRestockDialogItem.value = item },
                                        shape = MaterialTheme.shapes.medium,
                                        colors = ButtonDefaults.buttonColors(containerColor = feedPrimary, contentColor = Color.White),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                    ) {
                                        Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(stringResource("restock"), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }

                // Native Ad
                val isPaidPremium = LocalIsPaidPremium.current
                if (!isPaidPremium) {
                    NativeAdCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }

                // Transaction History
                if (feedInventoryTransactions.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    
                    Surface(
                        onClick = { isTxHistoryExpanded = !isTxHistoryExpanded },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = stringResource("transaction_history"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "${feedInventoryTransactions.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            val rotation by animateFloatAsState(
                                targetValue = if (isTxHistoryExpanded) 180f else 0f,
                                label = "chevron"
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isTxHistoryExpanded) stringResource("collapse") else stringResource("expand"),
                                modifier = Modifier
                                    .size(24.dp)
                                    .rotate(rotation),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    AnimatedVisibility(visible = isTxHistoryExpanded) {
                        Column {
                            Spacer(Modifier.height(8.dp))
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
            onNavigateToPaywall = onNavigateToPaywall,
            onDismiss = { showFormulatorDialog.value = false },
            onFormulate = { stage, selectedIds ->
                onFormulateFeed(stage, selectedIds)
                showFormulatorDialog.value = false
            }
        )
    }

    if (showItemTypeChoiceDialog.value) {
        AddInventoryItemTypeDialog(
            onDismiss = { showItemTypeChoiceDialog.value = false },
            onSelectIngredient = {
                showItemTypeChoiceDialog.value = false
                showAddIngredientDialog.value = true
            },
            onSelectCompleteFeed = {
                showItemTypeChoiceDialog.value = false
                showAddFeedDialog.value = true
            }
        )
    }

    if (showAddIngredientDialog.value) {
        AddFeedIngredientInventoryDialog(
            availableIngredients = ingredients,
            currencySymbol = currencySymbol,
            onDismiss = { showAddIngredientDialog.value = false },
            onConfirm = { name, category, qty, unit, bagWeight, threshold, cost ->
                onAddFeedInventoryItem(name, category, qty, unit, bagWeight, threshold, cost, "Feed Ingredient")
                showAddIngredientDialog.value = false
            }
        )
    }

    if (showAddFeedDialog.value) {
        AddFeedInventoryItemDialog(
            currencySymbol = currencySymbol,
            onDismiss = { showAddFeedDialog.value = false },
            onConfirm = { name, type, qty, unit, bagWeight, threshold, cost ->
                onAddFeedInventoryItem(name, type, qty, unit, bagWeight, threshold, cost, "Complete Feed")
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
