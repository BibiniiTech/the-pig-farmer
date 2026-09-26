package com.example.smartswine.ui.feed

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartswine.model.FeedIngredient
import com.example.smartswine.model.NutritionalRequirement
import com.example.smartswine.ui.feed.components.BatchMixDialog
import com.example.smartswine.ui.feed.components.SaveRecipeDialog
import com.example.smartswine.ui.feed.components.SavedRecipesDialog
import com.example.smartswine.ui.theme.SmartSwineTheme
import com.example.smartswine.util.PdfGenerator
import com.example.smartswine.utils.LocalAppLanguage
import com.example.smartswine.utils.Translator
import com.example.smartswine.utils.getIngredientNameKey
import com.example.smartswine.ui.components.NativeAdCard
import com.example.smartswine.ui.components.RewardedPassDialog
import com.example.smartswine.utils.StylishDivider
import com.example.smartswine.utils.LocalIsPaidPremium
import com.example.smartswine.utils.stringResource
import com.example.smartswine.utils.LocalIsPremium
import com.example.smartswine.utils.PremiumWrapper
import com.example.smartswine.utils.TierLimiter
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

private val FeedOrange = Color(0xFFE65100)
private val FeedOrangeLight = Color(0xFFFFF3E0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyzeFeedScreen(
    viewModel: FeedViewModel,
    onNavigateToPaywall: () -> Unit = {},
    onBack: () -> Unit
) {
    val ingredients by viewModel.ingredients.collectAsStateWithLifecycle()
    val requirements by viewModel.nutritionalRequirements.collectAsStateWithLifecycle()
    val feedInventoryItems by viewModel.feedInventoryItems.collectAsStateWithLifecycle()
    val savedRecipes by viewModel.savedRecipes.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val currentLang = LocalAppLanguage.current.code
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val isPremium = LocalIsPremium.current
    val isPaidPremium = LocalIsPaidPremium.current
    var showRewardedPassDialog by remember { mutableStateOf(false) }
    var passDialogTitle by remember { mutableStateOf("") }
    var passDialogDescription by remember { mutableStateOf("") }
    val currencySymbol by com.example.smartswine.ui.settings.SettingsViewModel.getInstance().currencySymbol.collectAsState()

    // State for selected ingredients with entered amounts (quantity in kg or %)
    var isPercentageMode by remember { mutableStateOf(false) }
    var isDryMatterMode by remember { mutableStateOf(false) }
    val selectedItems = remember { mutableStateListOf<Pair<FeedIngredient, String>>() }
    var selectedTargetStage by remember { mutableStateOf<String?>("Grower") }
    var showIngredientPicker by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showSavedRecipesDialog by remember { mutableStateOf(false) }
    var showBatchMixDialog by remember { mutableStateOf(false) }
    var ingredientSearchQuery by remember { mutableStateOf("") }

    // Calculate current profile & safety checks
    val itemsForCalc = selectedItems.mapNotNull { (ing, qtyStr) ->
        val qty = qtyStr.toDoubleOrNull()
        if (qty != null && qty > 0) Pair(ing, qty) else null
    }
    val profile = remember(itemsForCalc, isPercentageMode, isDryMatterMode) {
        viewModel.calculateNutritionalContent(itemsForCalc, isPercentageMode, isDryMatterMode)
    }
    val safetyAlerts = remember(itemsForCalc, selectedTargetStage) {
        viewModel.checkIngredientSafety(itemsForCalc, selectedTargetStage ?: "Grower")
    }

    val targetReq = requirements.find { 
        it.stage.equals(selectedTargetStage, ignoreCase = true) ||
        (selectedTargetStage.equals("Weaner/Starter", ignoreCase = true) && (it.stage.equals("Starter", ignoreCase = true) || it.stage.equals("Weaner", ignoreCase = true)))
    } ?: NutritionalRequirement(
            stage = selectedTargetStage ?: "Grower",
            crudeProtein = 16.0,
            digestibleProtein = 13.6,
            metabolizableEnergy = 3200.0,
            calcium = 0.75,
            phosphorus = 0.50,
            crudeFiber = 5.0,
            lysine = 5.2,
            methionineCystine = 3.2,
            dietaryLysine = 0.85,
            dietaryMethionine = 0.52
        )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource("back"))
                    }
                    Text(
                        text = stringResource("analyze_feed"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = FeedOrange,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { showSavedRecipesDialog = true }
                    ) {
                        BadgedBox(
                            badge = {
                                if (savedRecipes.isNotEmpty()) {
                                    Badge(
                                        containerColor = FeedOrange,
                                        contentColor = Color.White
                                    ) {
                                        Text(savedRecipes.size.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmarks,
                                contentDescription = stringResource("saved_recipes"),
                                tint = FeedOrange
                            )
                        }
                    }

                    PremiumWrapper(
                        isPremium = isPremium,
                        onLockedClick = {
                            passDialogTitle = Translator.getString("unlock_feed_pdf_title", currentLang)
                            passDialogDescription = Translator.getString("unlock_feed_pdf_desc", currentLang)
                            showRewardedPassDialog = true
                        }
                    ) {
                        IconButton(
                            onClick = {
                                if (!isPremium) {
                                    passDialogTitle = Translator.getString("unlock_feed_pdf_title", currentLang)
                                    passDialogDescription = Translator.getString("unlock_feed_pdf_desc", currentLang)
                                    showRewardedPassDialog = true
                                } else if (itemsForCalc.isNotEmpty()) {
                                    val pairs = itemsForCalc.map { (ing, qty) ->
                                        val key = getIngredientNameKey(ing, context)
                                        val trans = Translator.getString(key, currentLang)
                                        val disp = if (trans == key && ing.name.isNotEmpty()) ing.name else trans
                                        disp to qty
                                    }
                                    PdfGenerator.generateAnalyzedFeedPdf(
                                        context = context,
                                        targetStage = selectedTargetStage ?: "Grower",
                                        ingredients = pairs,
                                        isPercentage = isPercentageMode,
                                        totalWeight = if (isPercentageMode) 100.0 else itemsForCalc.sumOf { it.second },
                                        profile = profile,
                                        target = targetReq,
                                        alerts = safetyAlerts,
                                        lang = currentLang
                                    )
                                }
                            },
                            enabled = itemsForCalc.isNotEmpty()
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = stringResource("export_to_pdf"), tint = if (itemsForCalc.isNotEmpty()) FeedOrange else Color.Gray)
                        }
                    }
                }
                StylishDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Quick Access Banner to Saved Recipes
            if (savedRecipes.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showSavedRecipesDialog = true },
                    color = FeedOrangeLight,
                    border = BorderStroke(1.dp, FeedOrange.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bookmarks,
                                contentDescription = null,
                                tint = FeedOrange,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Saved Formulations (${savedRecipes.size})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = FeedOrange
                            )
                        }
                        Text(
                            text = stringResource("browse_and_load"),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = FeedOrange
                        )
                    }
                }
            }

            // Benchmark Stage Target Card (above Ingredients in Mix)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, FeedOrange.copy(alpha = 0.25f)),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = FeedOrange)
                        Text(
                            text = stringResource("benchmark_stage_target"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    val allStages = listOf(
                        "stage_creep" to "Creep",
                        "stage_weaner_starter" to "Weaner/Starter",
                        "stage_grower" to "Grower",
                        "stage_finisher" to "Finisher",
                        "stage_pregnant" to "Pregnant",
                        "stage_lactating" to "Lactating"
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allStages) { (stageKey, stageDefault) ->
                            val isSelected = selectedTargetStage.equals(stageDefault, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) FeedOrange else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.clickable { selectedTargetStage = stageDefault }
                            ) {
                                Text(
                                    text = stringResource(stageKey),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Ingredient Selection Card with Input Mode directly below heading
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, FeedOrange.copy(alpha = 0.25f)),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${stringResource("ingredients_in_mix")} (${selectedItems.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Button(
                            onClick = { showIngredientPicker = true },
                            colors = ButtonDefaults.buttonColors(containerColor = FeedOrange),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource("add_ingredients"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Input Method directly below heading
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource("input_mode"),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier
                                .background(FeedOrangeLight, RoundedCornerShape(10.dp))
                                .padding(2.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (!isPercentageMode) FeedOrange else Color.Transparent,
                                modifier = Modifier.clickable { isPercentageMode = false }
                            ) {
                                Text(
                                    text = stringResource("weight_kg"),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isPercentageMode) Color.White else FeedOrange,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isPercentageMode) FeedOrange else Color.Transparent,
                                modifier = Modifier.clickable { isPercentageMode = true }
                            ) {
                                Text(
                                    text = stringResource("percentage_pct"),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPercentageMode) Color.White else FeedOrange,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    // As-Fed vs Dry Matter (DM) Evaluation Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(stringResource("moisture_basis"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text(
                                if (isDryMatterMode) stringResource("dry_matter_concentrated") else stringResource("as_fed_standard"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        FilterChip(
                            selected = isDryMatterMode,
                            onClick = { isDryMatterMode = !isDryMatterMode },
                            label = { Text(if (isDryMatterMode) stringResource("dry_matter_chip") else stringResource("as_fed_chip"), fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                        )
                    }

                    if (selectedItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Science, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(36.dp))
                                Text(
                                    text = stringResource("no_ingredients_added_yet"),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = stringResource("click_add_ingredients_hint"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    } else {
                        selectedItems.forEachIndexed { index, (ing, qtyStr) ->
                            val translatedName = remember(ing, currentLang) {
                                val key = getIngredientNameKey(ing, context)
                                val trans = Translator.getString(key, currentLang)
                                if (trans == key && ing.name.isNotEmpty()) ing.name else trans
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = translatedName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val costInfo = if (ing.costPerKg > 0) " • $${String.format(Locale.ENGLISH, "%.2f", ing.costPerKg)}/kg" else ""
                                    Text(
                                        text = "${ing.mainCategory} • CP: ${ing.crudeProtein}% | ME: ${ing.metabolizableEnergy.toInt()} kcal$costInfo",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                OutlinedTextField(
                                    value = qtyStr,
                                    onValueChange = { newVal ->
                                        if (newVal.isEmpty() || newVal.matches(Regex("^\\d*\\.?\\d*$"))) {
                                            selectedItems[index] = Pair(ing, newVal)
                                        }
                                    },
                                    placeholder = { Text(if (isPercentageMode) "%" else "kg") },
                                    trailingIcon = {
                                        Text(
                                            text = if (isPercentageMode) "%" else "kg",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = FeedOrange,
                                            modifier = Modifier.padding(end = 6.dp)
                                        )
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.width(110.dp),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )

                                IconButton(
                                    onClick = { selectedItems.removeAt(index) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = stringResource("remove"), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        // Total Row
                        val totalEntered = selectedItems.sumOf { it.second.toDoubleOrNull() ?: 0.0 }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isPercentageMode) stringResource("total_inclusion") else stringResource("total_batch_weight"),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = String.format(Locale.ENGLISH, "%.1f %s", totalEntered, if (isPercentageMode) "%" else "kg"),
                                fontWeight = FontWeight.Black,
                                color = if (isPercentageMode && abs(totalEntered - 100.0) > 0.5) MaterialTheme.colorScheme.error else FeedOrange,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        if (isPercentageMode && abs(totalEntered - 100.0) > 0.5 && totalEntered > 0) {
                            Text(
                                text = stringResource("total_percentage_recommendation_note", String.format(Locale.ENGLISH, "%.1f", totalEntered)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Veterinary Safety Alerts Card
            if (safetyAlerts.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, FeedOrange.copy(alpha = 0.25f)),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Text(
                                text = stringResource("veterinary_safety_alerts_count", safetyAlerts.size),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        safetyAlerts.forEach { alert ->
                            val ingTranslated = stringResource(getIngredientNameKey(alert.ingredientName))
                            val riskText = if (alert.riskKey.isNotBlank()) {
                                if (alert.riskKey == "risk_exceeds_limit") {
                                    stringResource(alert.riskKey, String.format(Locale.ENGLISH, "%.1f", alert.maxAllowedPercent), alert.stage)
                                } else {
                                    stringResource(alert.riskKey)
                                }
                            } else {
                                alert.riskDescription
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(ingTranslated, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = "${String.format(Locale.ENGLISH, "%.1f", alert.currentPercent)}% (${stringResource("max_allowed")}: ${String.format(Locale.ENGLISH, "%.1f", alert.maxAllowedPercent)}%)",
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                Text(riskText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Calcium-to-Phosphorus (Ca:P) Ratio Gauge Card
            val caPRatio = profile.caPRatio
            val ratioAdequate = caPRatio in 1.2..1.5
            val ratioMarginal = (caPRatio in 1.0..1.2) || (caPRatio in 1.5..2.0)
            val ratioBg = when {
                caPRatio <= 0.001 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ratioAdequate -> Color(0xFFE8F5E9)
                ratioMarginal -> Color(0xFFFFF8E1)
                else -> Color(0xFFFFEBEE)
            }
            val ratioColor = when {
                caPRatio <= 0.001 -> Color.Gray
                ratioAdequate -> Color(0xFF2E7D32)
                ratioMarginal -> Color(0xFFE65100)
                else -> MaterialTheme.colorScheme.error
            }
            val ratioStatus = when {
                caPRatio <= 0.001 -> stringResource("ca_p_status_empty")
                ratioAdequate -> stringResource("ca_p_status_optimal")
                ratioMarginal -> stringResource("ca_p_status_acceptable")
                caPRatio < 1.0 -> stringResource("ca_p_status_critical_inverted")
                else -> stringResource("ca_p_status_warning_excessive")
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, FeedOrange.copy(alpha = 0.25f)),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Scale, contentDescription = null, tint = ratioColor)
                            Text(
                                text = stringResource("calcium_phosphorus_ratio"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = ratioColor
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ratioColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (caPRatio > 0.001) String.format(Locale.ENGLISH, "%.2f : 1", caPRatio) else "—",
                                fontWeight = FontWeight.Black,
                                color = ratioColor,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = ratioStatus,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = ratioColor
                    )
                    Text(
                        text = stringResource("cap_ratio_explanation"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Formulation Economics / Cost Card
            if (profile.costPerKg > 0.0) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, FeedOrange.copy(alpha = 0.3f)),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.AttachMoney, contentDescription = null, tint = FeedOrange)
                            Text(stringResource("formulation_cost_breakdown"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = FeedOrange)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(stringResource("cost_per_kg"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text("$currencySymbol${String.format(Locale.ENGLISH, "%.2f", profile.costPerKg)}", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, color = FeedOrange)
                            }
                            Column {
                                Text(stringResource("cost_per_50kg_bag"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text("$currencySymbol${String.format(Locale.ENGLISH, "%.2f", profile.costPer50kgBag)}", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                            }
                            Column {
                                Text(if (isPercentageMode) stringResource("cost_100kg") else stringResource("batch_cost"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text("$currencySymbol${String.format(Locale.ENGLISH, "%.2f", profile.totalCost)}", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        val topCostDriver = itemsForCalc.maxByOrNull { (ing, qty) -> ing.costPerKg * qty }
                        if (topCostDriver != null && topCostDriver.first.costPerKg > 0) {
                            val driverName = topCostDriver.first.name
                            val driverShare = if (profile.totalCost > 0) (topCostDriver.first.costPerKg * topCostDriver.second / profile.totalCost) * 100.0 else 0.0
                            Text(
                                stringResource("primary_cost_driver_format", driverName, String.format(Locale.ENGLISH, "%.1f", driverShare)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Native Ad
            if (!isPaidPremium) {
                NativeAdCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }

            // Results Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, FeedOrange.copy(alpha = 0.25f)),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Analytics, contentDescription = null, tint = FeedOrange)
                        Text(
                            text = stringResource("resulting_nutritional_content"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = FeedOrange
                        )
                    }

                    if (itemsForCalc.isNotEmpty()) {
                        HorizontalDivider(color = FeedOrange.copy(alpha = 0.2f))

                        NutrientResultRow(
                            label = stringResource("crude_protein_cp"),
                            actual = profile.crudeProtein,
                            unit = "%",
                            target = targetReq.getTargetCrudeProtein(),
                            higherIsBetter = true
                        )
                        NutrientResultRow(
                            label = stringResource("digestible_protein"),
                            actual = profile.digestibleProtein,
                            unit = "%",
                            target = targetReq.getTargetDigestibleProtein(),
                            higherIsBetter = true
                        )
                        NutrientResultRow(
                            label = stringResource("metabolizable_energy_me"),
                            actual = profile.metabolizableEnergy,
                            unit = "kcal/kg",
                            target = targetReq.metabolizableEnergy,
                            higherIsBetter = true
                        )
                        NutrientResultRow(
                            label = stringResource("crude_fiber"),
                            actual = profile.crudeFiber,
                            unit = "%",
                            target = targetReq.crudeFiber,
                            higherIsBetter = false
                        )
                        NutrientResultRow(
                            label = stringResource("calcium_ca"),
                            actual = profile.calcium,
                            unit = "%",
                            target = targetReq.calcium,
                            higherIsBetter = true
                        )
                        NutrientResultRow(
                            label = stringResource("phosphorus_p"),
                            actual = profile.phosphorus,
                            unit = "%",
                            target = targetReq.phosphorus,
                            higherIsBetter = true
                        )
                        NutrientResultRow(
                            label = stringResource("dietary_lysine"),
                            actual = profile.lysine,
                            unit = "%",
                            target = targetReq.getTargetDietaryLysine(),
                            higherIsBetter = true
                        )
                        NutrientResultRow(
                            label = stringResource("dietary_methionine_cystine"),
                            actual = profile.methionine,
                            unit = "%",
                            target = targetReq.getTargetDietaryMethionine(),
                            higherIsBetter = true
                        )
                    }
                }
            }

            // Action Buttons: Save as Recipe & Mix Batch
            if (itemsForCalc.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showSaveDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = FeedOrange)
                    ) {
                        Icon(Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource("save_recipe"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showBatchMixDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FeedOrange)
                    ) {
                        Icon(Icons.Default.Blender, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource("mix_batch"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }

    // Save Recipe Dialog
    if (showSaveDialog) {
        SaveRecipeDialog(
            initialName = "${selectedTargetStage ?: "Custom"} Mix",
            stage = selectedTargetStage ?: "Grower",
            onDismiss = { showSaveDialog = false },
            onSave = { name, notes ->
                val sumWeight = itemsForCalc.sumOf { it.second }.coerceAtLeast(0.0001)
                val map = itemsForCalc.associate { (ing, qty) ->
                    val percent = if (isPercentageMode) qty else (qty / sumWeight) * 100.0
                    ing.id to percent
                }
                viewModel.saveFeedRecipe(
                    name = name,
                    stage = selectedTargetStage ?: "Grower",
                    ingredients = map,
                    isPercentage = true,
                    costPerKg = profile.costPerKg,
                    targetBatchKg = if (isPercentageMode) 1000.0 else sumWeight,
                    notes = notes
                ) { success, _ ->
                    showSaveDialog = false
                    coroutineScope.launch {
                        if (success) snackbarHostState.showSnackbar("Recipe saved successfully!")
                        else snackbarHostState.showSnackbar("Failed to save recipe")
                    }
                }
            }
        )
    }

    // Saved Recipes Dialog
    if (showSavedRecipesDialog) {
        SavedRecipesDialog(
            savedRecipes = savedRecipes,
            allIngredients = ingredients,
            currencySymbol = "$",
            onDismiss = { showSavedRecipesDialog = false },
            onLoadRecipe = { recipe ->
                selectedTargetStage = recipe.stage
                isPercentageMode = recipe.isPercentage
                selectedItems.clear()
                recipe.ingredients.forEach { (ingId, qty) ->
                    val ing = ingredients.find { it.id == ingId || it.name.equals(ingId, ignoreCase = true) }
                    if (ing != null) {
                        val formattedQty = if (qty % 1.0 == 0.0) qty.toInt().toString() else String.format(Locale.US, "%.2f", qty)
                        selectedItems.add(Pair(ing, formattedQty))
                    }
                }
                showSavedRecipesDialog = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Loaded '${recipe.name}' into analyzer!")
                }
            },
            onDeleteRecipe = { recipeId ->
                viewModel.deleteSavedRecipe(recipeId)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Recipe removed")
                }
            }
        )
    }

    // Batch Mix Dialog
    if (showBatchMixDialog) {
        val sumWeight = itemsForCalc.sumOf { it.second }.coerceAtLeast(0.0001)
        BatchMixDialog(
            recipeName = "${selectedTargetStage ?: "Custom"} Mix",
            stage = selectedTargetStage ?: "Grower",
            defaultBatchKg = if (isPercentageMode) 1000.0 else sumWeight,
            requiredIngredients = itemsForCalc.map { (ing, qty) ->
                val percent = if (isPercentageMode) qty else (qty / sumWeight) * 100.0
                (ing.name.ifBlank { ing.id }) to percent
            },
            feedInventoryItems = feedInventoryItems,
            onDismiss = { showBatchMixDialog = false },
            onConfirm = { batchKg ->
                val map = itemsForCalc.associate { (ing, qty) ->
                    val percent = if (isPercentageMode) qty else (qty / sumWeight) * 100.0
                    ing.id to percent
                }
                viewModel.executeBatchMix(
                    recipeName = "${selectedTargetStage ?: "Custom"} Feed Mix",
                    stage = selectedTargetStage ?: "Grower",
                    batchWeightKg = batchKg,
                    ingredients = map
                ) { success, err ->
                    showBatchMixDialog = false
                    coroutineScope.launch {
                        if (success) snackbarHostState.showSnackbar("Batch mixed & inventory updated successfully!")
                        else snackbarHostState.showSnackbar("Batch mix failed: ${err ?: "Unknown error"}")
                    }
                }
            }
        )
    }

    // Ingredient Picker Dialog
    if (showIngredientPicker) {
        val sortedIngredients = remember(ingredients, context, currentLang) {
            ingredients.sortedBy { ing ->
                val key = getIngredientNameKey(ing, context)
                val trans = Translator.getString(key, currentLang)
                (if (trans == key && ing.name.isNotEmpty()) ing.name else trans).lowercase(Locale.getDefault())
            }
        }
        val filteredIngredients = remember(sortedIngredients, ingredientSearchQuery, context, currentLang) {
            if (ingredientSearchQuery.isBlank()) sortedIngredients
            else {
                sortedIngredients.filter { ing ->
                    val key = getIngredientNameKey(ing, context)
                    val trans = Translator.getString(key, currentLang)
                    val dispName = if (trans == key && ing.name.isNotEmpty()) ing.name else trans
                    dispName.contains(ingredientSearchQuery, ignoreCase = true) ||
                    ing.name.contains(ingredientSearchQuery, ignoreCase = true)
                }
            }
        }

        val unlockIngredientsTitle = stringResource("unlock_unlimited_ingredients")
        val unlockIngredientsDesc = stringResource("unlock_ingredients_desc")

        AlertDialog(
            onDismissRequest = {
                showIngredientPicker = false
                ingredientSearchQuery = ""
            },
            title = { Text(stringResource("select_ingredients"), fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.height(400.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ingredientSearchQuery,
                        onValueChange = { ingredientSearchQuery = it },
                        placeholder = { Text(stringResource("search_ingredient_hint")) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredIngredients) { ing ->
                            val isAlreadyAdded = selectedItems.any { it.first.id == ing.id }
                            val translatedName = remember(ing, currentLang) {
                                val key = getIngredientNameKey(ing, context)
                                val trans = Translator.getString(key, currentLang)
                                if (trans == key && ing.name.isNotEmpty()) ing.name else trans
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (isAlreadyAdded) {
                                            selectedItems.removeAll { it.first.id == ing.id }
                                        } else {
                                            if (!isPremium && selectedItems.size >= TierLimiter.FREE_MAX_FEED_INGREDIENTS) {
                                                passDialogTitle = unlockIngredientsTitle
                                                passDialogDescription = unlockIngredientsDesc
                                                showRewardedPassDialog = true
                                            } else {
                                                selectedItems.add(Pair(ing, "0"))
                                            }
                                        }
                                    }
                                    .background(if (isAlreadyAdded) FeedOrangeLight.copy(alpha = 0.6f) else Color.Transparent)
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Checkbox(
                                    checked = isAlreadyAdded,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            if (!isAlreadyAdded) {
                                                if (!isPremium && selectedItems.size >= TierLimiter.FREE_MAX_FEED_INGREDIENTS) {
                                                    passDialogTitle = unlockIngredientsTitle
                                                    passDialogDescription = unlockIngredientsDesc
                                                    showRewardedPassDialog = true
                                                } else {
                                                    selectedItems.add(Pair(ing, "0"))
                                                }
                                            }
                                        } else {
                                            selectedItems.removeAll { it.first.id == ing.id }
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = FeedOrange)
                                )
                                Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                                    Text(
                                        text = translatedName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    val costInfo = if (ing.costPerKg > 0) " • $${String.format(Locale.ENGLISH, "%.2f", ing.costPerKg)}/kg" else ""
                                    Text(
                                        text = "${ing.mainCategory} • CP: ${ing.crudeProtein}%$costInfo",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showIngredientPicker = false
                        ingredientSearchQuery = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FeedOrange, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource("done"), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showRewardedPassDialog) {
        RewardedPassDialog(
            title = passDialogTitle,
            description = passDialogDescription,
            onDismiss = { showRewardedPassDialog = false },
            onNavigateToPaywall = onNavigateToPaywall,
            onPassActivated = {
                // Pass activated
            }
        )
    }
}


@Composable
private fun NutrientResultRow(
    label: String,
    actual: Double,
    unit: String,
    target: Double,
    higherIsBetter: Boolean
) {
    val isAdequate = if (higherIsBetter) actual >= target * 0.95 else actual <= target * 1.05
    val statusText = if (actual <= 0.001) "-" else if (isAdequate) stringResource("ok_status") else if (higherIsBetter) stringResource("deficient") else stringResource("excess")
    val statusColor = if (actual <= 0.001) Color.Gray else if (isAdequate) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            Text("${stringResource("target")}: ${String.format(Locale.ENGLISH, "%.1f", target)} $unit", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "${String.format(Locale.ENGLISH, "%.2f", actual)} $unit",
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = statusColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NutrientResultRowPreview() {
    SmartSwineTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NutrientResultRow(
                label = "Crude Protein (CP)",
                actual = 16.5,
                unit = "%",
                target = 14.5,
                higherIsBetter = true
            )
            NutrientResultRow(
                label = "Lysine",
                actual = 4.2,
                unit = "%",
                target = 6.1,
                higherIsBetter = true
            )
            NutrientResultRow(
                label = "Crude Fiber",
                actual = 6.8,
                unit = "%",
                target = 5.0,
                higherIsBetter = false
            )
        }
    }
}

@Preview(showBackground = true, name = "Nutrient Result Row States")
@Composable
fun NutrientResultRowPreview2() {
    SmartSwineTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NutrientResultRow(
                label = "Crude Protein (CP)",
                actual = 16.5,
                unit = "%",
                target = 14.5,
                higherIsBetter = true
            )
            NutrientResultRow(
                label = "Digestible Protein",
                actual = 11.2,
                unit = "%",
                target = 12.3,
                higherIsBetter = true
            )
            NutrientResultRow(
                label = "Crude Fiber",
                actual = 6.5,
                unit = "%",
                target = 5.0,
                higherIsBetter = false
            )
            NutrientResultRow(
                label = "Calcium (Ca)",
                actual = 0.0,
                unit = "%",
                target = 0.75,
                higherIsBetter = true
            )
        }
    }
}
