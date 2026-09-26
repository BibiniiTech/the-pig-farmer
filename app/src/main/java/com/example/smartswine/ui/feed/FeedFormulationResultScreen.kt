package com.example.smartswine.ui.feed

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartswine.utils.StylishDivider
import com.example.smartswine.model.FeedIngredient
import com.example.smartswine.model.NutritionalRequirement
import com.example.smartswine.ui.theme.SmartSwineTheme
import com.example.smartswine.ui.theme.DarkBackground
import com.example.smartswine.util.PdfGenerator
import com.example.smartswine.utils.stringResource
import com.example.smartswine.utils.Translator
import com.example.smartswine.utils.LocalAppLanguage
import java.util.Locale

import com.example.smartswine.utils.getIngredientNameKey
import com.example.smartswine.utils.getCategoryKey
import com.example.smartswine.ui.feed.components.BatchMixDialog
import com.example.smartswine.ui.feed.components.SaveRecipeDialog
import kotlinx.coroutines.launch

@Composable
fun getTranslatedIngredientName(ingredient: FeedIngredient): String {
    val context = LocalContext.current
    val key = getIngredientNameKey(ingredient, context)
    val name = stringResource(key)
    return if (name == key && ingredient.name.isNotEmpty()) {
        ingredient.name
    } else {
        name
    }
}

@Composable
fun FeedFormulationResultScreen(
    viewModel: FeedViewModel,
    onBack: () -> Unit,
) {
    val formulationResult by viewModel.formulationResult.collectAsStateWithLifecycle()
    val ingredients by viewModel.ingredients.collectAsStateWithLifecycle()
    val feedInventoryItems by viewModel.feedInventoryItems.collectAsStateWithLifecycle()
    val targetRequirement by viewModel.targetRequirement.collectAsStateWithLifecycle()
    
    val target = targetRequirement
    val context = LocalContext.current
    val lang = LocalAppLanguage.current.code
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showSaveDialog by remember { mutableStateOf(false) }
    var showBatchMixDialog by remember { mutableStateOf(false) }

    val calculatedNutrients = remember(formulationResult, ingredients) {
        if (formulationResult != null) calculateFormulatedNutrients(formulationResult!!, ingredients)
        else emptyMap()
    }
    val costPerKg = calculatedNutrients["costPerKg"] ?: 0.0
    val costPer50kgBag = costPerKg * 50.0

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { _ ->
        FeedFormulationResultContent(
            formulationResult = formulationResult,
            ingredients = ingredients,
            targetRequirement = target,
            onBack = onBack,
            onRecalculate = { viewModel.recalculateFormulation() },
            onSaveRecipe = { showSaveDialog = true },
            onBatchMix = { showBatchMixDialog = true },
        ) { result ->
            val pairsWithCategories = result.map { (id, percent) ->
                val ingredient = ingredients.find { it.id == id }
                val translatedName = if (ingredient != null) {
                    val key = getIngredientNameKey(ingredient, context)
                    val trans = Translator.getString(key, lang)
                    if (trans == key && ingredient.name.isNotEmpty()) ingredient.name else trans
                } else id
                val translatedCategory = if (ingredient != null) Translator.getString(getCategoryKey(ingredient.mainCategory), lang) else "Other"
                Triple(translatedName, percent, translatedCategory)
            }
            
            val categoryOrder = listOf(
                Translator.getString("energy", lang),
                Translator.getString("protein", lang),
                Translator.getString("vitamins_minerals_salt", lang)
            )
            val sortedPairs = pairsWithCategories.asSequence().sortedWith { a, b ->
                val orderA = categoryOrder.indexOf(a.third).let { if (it == -1) 99 else it }
                val orderB = categoryOrder.indexOf(b.third).let { if (it == -1) 99 else it }
                if (orderA != orderB) orderA.compareTo(orderB)
                else a.first.compareTo(b.first)
            }.map { it.first to it.second }.toList()

            val actual = calculatedNutrients
            val comparison = if (target != null) {
                listOf(
                    mapOf("label" to Translator.getString("crude_protein_pct", lang), "target" to target.getTargetCrudeProtein(), "actual" to (actual["protein"] ?: 0.0), "isDeficient" to ((actual["protein"] ?: 0.0) < target.getTargetCrudeProtein())),
                    mapOf("label" to Translator.getString("me_kcal_kg", lang), "target" to target.metabolizableEnergy, "actual" to (actual["energy"] ?: 0.0), "isDeficient" to ((actual["energy"] ?: 0.0) < target.metabolizableEnergy)),
                    mapOf("label" to Translator.getString("calcium_pct", lang), "target" to target.calcium, "actual" to (actual["calcium"] ?: 0.0), "isDeficient" to ((actual["calcium"] ?: 0.0) < target.calcium)),
                    mapOf("label" to Translator.getString("phosphorus_pct", lang), "target" to target.phosphorus, "actual" to (actual["phosphorus"] ?: 0.0), "isDeficient" to ((actual["phosphorus"] ?: 0.0) < target.phosphorus)),
                    mapOf("label" to Translator.getString("crude_fiber_pct", lang), "target" to target.crudeFiber, "actual" to (actual["fiber"] ?: 0.0), "isDeficient" to ((actual["fiber"] ?: 0.0) > target.crudeFiber)),
                )
            } else emptyList()
            
            PdfGenerator.generateFormulationPdf(
                context = context,
                title = "${Translator.getString("formulation_for", lang)} ${if (target != null) Translator.getString(getCategoryKey(target.stage), lang) else Translator.getString("custom", lang)}",
                ingredients = sortedPairs,
                additives = emptyList(),
                total = result.values.sum(),
                nutritionalComparison = comparison,
                lang = lang,
                costPerKg = costPerKg,
                costPer50kgBag = costPer50kgBag
            )
        }

        if (showSaveDialog && formulationResult != null) {
            val stageName = target?.stage ?: "Custom"
            SaveRecipeDialog(
                initialName = "$stageName Balanced Mix",
                stage = stageName,
                onDismiss = { showSaveDialog = false },
                onSave = { name, notes ->
                    viewModel.saveFeedRecipe(
                        name = name,
                        stage = stageName,
                        ingredients = formulationResult!!,
                        isPercentage = true,
                        costPerKg = costPerKg,
                        targetBatchKg = 1000.0,
                        notes = notes
                    ) { success, _ ->
                        showSaveDialog = false
                        coroutineScope.launch {
                            if (success) snackbarHostState.showSnackbar(Translator.getString("recipe_saved_success", lang))
                            else snackbarHostState.showSnackbar(Translator.getString("recipe_saved_failed", lang))
                        }
                    }
                }
            )
        }

        if (showBatchMixDialog && formulationResult != null) {
            val stageName = target?.stage ?: "Custom"
            BatchMixDialog(
                recipeName = "$stageName Formulation",
                stage = stageName,
                defaultBatchKg = 1000.0,
                requiredIngredients = formulationResult!!.map { (id, percent) ->
                    val ingredient = ingredients.find { it.id == id }
                    (ingredient?.name?.ifBlank { id } ?: id) to percent
                },
                feedInventoryItems = feedInventoryItems,
                onDismiss = { showBatchMixDialog = false },
                onConfirm = { batchKg ->
                    viewModel.executeBatchMix(
                        recipeName = "$stageName Feed Mix",
                        stage = stageName,
                        batchWeightKg = batchKg,
                        ingredients = formulationResult!!
                    ) { success, err ->
                        showBatchMixDialog = false
                        coroutineScope.launch {
                            if (success) snackbarHostState.showSnackbar(Translator.getString("batch_mixed_success", lang))
                            else snackbarHostState.showSnackbar("${Translator.getString("batch_mix_failed", lang)}: ${err ?: Translator.getString("unknown_error", lang)}")
                        }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedFormulationResultContent(
    formulationResult: Map<String, Double>?,
    ingredients: List<FeedIngredient>,
    targetRequirement: NutritionalRequirement?,
    onBack: () -> Unit,
    onRecalculate: () -> Unit,
    onSaveRecipe: () -> Unit = {},
    onBatchMix: () -> Unit = {},
    onExportPdf: (Map<String, Double>) -> Unit,
) {
    var batchSize by remember { mutableStateOf("1000") }
    val batchSizeDouble = batchSize.toDoubleOrNull() ?: 0.0
    val isDark = MaterialTheme.colorScheme.background == DarkBackground
    val cardBg = if (isDark) MaterialTheme.colorScheme.surface else Color(0xFFFFF3E0)
    val cardBorder = if (isDark) Color(0xFFE65100).copy(alpha = 0.5f) else Color(0xFFFFE0B2)
    val headerColor = if (isDark) Color(0xFFFFB74D) else Color(0xFFE65100)
    val subHeaderColor = if (isDark) Color(0xFFFFB74D) else MaterialTheme.colorScheme.secondary
    val currencySymbol by com.example.smartswine.ui.settings.SettingsViewModel.getInstance().currencySymbol.collectAsState()

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
                        text = stringResource("formulation_results"),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = headerColor,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.width(48.dp))
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            formulationResult?.let { result ->
                val totalPercent = result.values.sum()
                val isIncomplete = totalPercent < 99.9

                if (isIncomplete) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                stringResource("formula_incomplete", String.format(Locale.getDefault(), "%.1f", totalPercent)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isIncomplete) MaterialTheme.colorScheme.surfaceVariant else cardBg,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource("best_mix_formulation"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = headerColor,
                                modifier = Modifier.weight(1.5f)
                            )
                            Text("%", style = MaterialTheme.typography.labelSmall, color = subHeaderColor, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.extraSmall)
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), MaterialTheme.shapes.extraSmall)
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    BasicTextField(
                                        value = batchSize,
                                        onValueChange = { if (it.isEmpty() || (it.toDoubleOrNull() != null)) batchSize = it },
                                        modifier = Modifier.width(45.dp),
                                        textStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                }
                                Text(stringResource("kg"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(start = 2.dp))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        
                        val categoryOrder = listOf("Energy", "Protein", "Vitamins, Minerals & Salt")
                        val groupedResults = result.keys.groupBy { id ->
                            ingredients.find { it.id == id }?.mainCategory ?: "Other"
                        }
                        
                        categoryOrder.forEach { category ->
                            val ids = groupedResults[category]
                            if (!ids.isNullOrEmpty()) {
                                Text(
                                    text = stringResource(getCategoryKey(category)).uppercase(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = subHeaderColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                ids.forEach { id ->
                                    val ingredient = ingredients.find { it.id == id }
                                    val percent = result[id] ?: 0.0
                                    val weight = (percent / 100.0) * batchSizeDouble
                                    Row(modifier = Modifier.fillMaxWidth().padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(ingredient?.let { getTranslatedIngredientName(it) } ?: id, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1.5f))
                                        Text("${String.format(Locale.getDefault(), "%.1f", percent)}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(0.7f), textAlign = TextAlign.End)
                                        Text("${String.format(Locale.getDefault(), "%.1f", weight)}kg", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        // Handle any ingredients not in the predefined categories
                        val otherIds = groupedResults.filter { it.key !in categoryOrder }.values.flatten()
                        if (otherIds.isNotEmpty()) {
                            Text(stringResource("other").uppercase(), style = MaterialTheme.typography.labelMedium, color = subHeaderColor, fontWeight = FontWeight.Bold)
                            otherIds.forEach { id ->
                                val ingredient = ingredients.find { it.id == id }
                                val percent = result[id] ?: 0.0
                                val weight = (percent / 100.0) * batchSizeDouble
                                Row(modifier = Modifier.fillMaxWidth().padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(ingredient?.let { getTranslatedIngredientName(it) } ?: id, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1.5f))
                                    Text("${String.format(Locale.getDefault(), "%.1f", percent)}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(0.7f), textAlign = TextAlign.End)
                                    Text("${String.format(Locale.getDefault(), "%.1f", weight)}kg", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                                }
                            }
                        }
                    }
                }

                // Cost Breakdown Card
                val actual = calculateFormulatedNutrients(result, ingredients)
                val costPerKg = actual["costPerKg"] ?: 0.0
                if (costPerKg > 0.0) {
                    val totalBatchCost = costPerKg * batchSizeDouble
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFFE65100).copy(alpha = 0.5f) else Color(0xFFFFB74D))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource("formulation_cost_breakdown"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = headerColor)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(stringResource("cost_per_kg"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    Text("$currencySymbol${String.format(Locale.getDefault(), "%.2f", costPerKg)}", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, color = headerColor)
                                }
                                Column {
                                    Text(stringResource("cost_per_50kg_bag"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    Text("$currencySymbol${String.format(Locale.getDefault(), "%.2f", costPerKg * 50.0)}", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Column {
                                    Text("${stringResource("batch_cost")} (${String.format(Locale.getDefault(), "%.0f", batchSizeDouble)}kg)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    Text("$currencySymbol${String.format(Locale.getDefault(), "%.2f", totalBatchCost)}", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }

                // Action Buttons: Save Recipe & Batch Mix
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onSaveRecipe,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE65100))
                    ) {
                        Text(stringResource("save_recipe"), fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onBatchMix,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                    ) {
                        Text(stringResource("mix_batch"), fontWeight = FontWeight.Bold)
                    }
                }
                
                Button(
                    onClick = onRecalculate,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                ) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource("recalculate_formulation"))
                }

                targetRequirement?.let { target ->
                    NutritionalComparisonCard(target = target, actual = actual, onExportPdf = { onExportPdf(result) })
                }

                Text(
                    text = stringResource("disclaimer_feed"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp)
                )
            } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource("no_formulation_results"))
            }

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

@Composable
fun NutritionalComparisonCard(
    target: NutritionalRequirement,
    actual: Map<String, Double>,
    onExportPdf: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == DarkBackground
    val cardBg = if (isDark) MaterialTheme.colorScheme.surface else Color(0xFFFFF3E0)
    val cardBorder = if (isDark) Color(0xFFE65100).copy(alpha = 0.5f) else Color(0xFFFFE0B2)
    val headerColor = if (isDark) Color(0xFFFFB74D) else Color(0xFFE65100)

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource("nutritional_comparison", stringResource(getCategoryKey(target.stage))),
                style = MaterialTheme.typography.titleMedium,
                color = headerColor
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)
            
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource("nutrient"),
                    modifier = Modifier.weight(1.5f),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.secondary
                )
                Text(
                    stringResource("target"),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.secondary
                )
                Text(
                    stringResource("actual"),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.secondary
                )
            }

            ComparisonRow(stringResource("crude_protein_pct"), target.getTargetCrudeProtein(), actual["protein"] ?: 0.0)
            ComparisonRow(stringResource("me_kcal_kg"), target.metabolizableEnergy, actual["energy"] ?: 0.0)
            ComparisonRow(stringResource("calcium_pct"), target.calcium, actual["calcium"] ?: 0.0)
            ComparisonRow(stringResource("phosphorus_pct"), target.phosphorus, actual["phosphorus"] ?: 0.0)
            ComparisonRow(stringResource("crude_fiber_pct"), target.crudeFiber, actual["fiber"] ?: 0.0)
            
            val caPRatio = actual["caPRatio"] ?: 0.0
            if (caPRatio > 0.0) {
                ComparisonRow("Ca:P Ratio", 1.3, caPRatio)
            }
            
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onExportPdf,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PictureAsPdf, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource("export_formulation_pdf"))
            }
        }
    }
}

@Composable
fun ComparisonRow(label: String, target: Double, actual: Double, isMaximumLimit: Boolean = false) {
    val isDark = MaterialTheme.colorScheme.background == DarkBackground
    val isWarning = if (isMaximumLimit) {
        actual > (target + 2.5)
    } else {
        actual < (target - 0.05)
    }
    val successColor = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
    val warningColor = if (isDark) Color(0xFFEF5350) else Color.Red
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            modifier = Modifier.weight(1.5f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = String.format(Locale.getDefault(), "%.1f", target),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = String.format(Locale.getDefault(), "%.1f", actual),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isWarning) warningColor else successColor
        )
    }
}

private fun calculateFormulatedNutrients(
    result: Map<String, Double>,
    ingredients: List<FeedIngredient>
): Map<String, Double> {
    var protein = 0.0
    var energy = 0.0
    var calcium = 0.0
    var phosphorus = 0.0
    var fiber = 0.0
    var costPerKg = 0.0

    result.forEach { (id, percent) ->
        val ing = ingredients.find { it.id == id } ?: return@forEach
        val factor = percent / 100.0
        protein += ing.crudeProtein * factor
        energy += ing.metabolizableEnergy * factor
        
        val ingCa = if (ing.calcium > 50.0) ing.calcium / 10.0 else ing.calcium
        calcium += ingCa * factor
        
        val ingP = if (ing.phosphorus > 50.0) ing.phosphorus / 10.0 else ing.phosphorus
        phosphorus += ingP * factor
        
        fiber += ing.crudeFiber * factor

        if (ing.costPerKg > 0.0) {
            costPerKg += ing.costPerKg * factor
        }
    }

    val caPRatio = if (phosphorus > 0.0001) calcium / phosphorus else 0.0

    return mapOf(
        "protein" to protein,
        "energy" to energy,
        "calcium" to calcium,
        "phosphorus" to phosphorus,
        "fiber" to fiber,
        "costPerKg" to costPerKg,
        "caPRatio" to caPRatio
    )
}

@Preview(showBackground = true)
@Composable
fun FeedFormulationResultPreview() {
    val sampleIngredients = listOf(
        FeedIngredient(id = "1", name = "Maize", crudeProtein = 8.8, metabolizableEnergy = 3300.0, calcium = 0.01, phosphorus = 0.3, crudeFiber = 2.2),
        FeedIngredient(id = "2", name = "Soybean meal", crudeProtein = 48.0, metabolizableEnergy = 2400.0, calcium = 0.3, phosphorus = 0.6, crudeFiber = 6.0),
        FeedIngredient(id = "3", name = "Fish meal", crudeProtein = 65.0, metabolizableEnergy = 2800.0, calcium = 5.0, phosphorus = 3.0, crudeFiber = 1.0)
    )

    val sampleResult = mapOf(
        "1" to 65.0,
        "2" to 30.0,
        "3" to 5.0
    )

    val sampleTarget = NutritionalRequirement(
        stage = "Finisher",
        digestibleProtein = 14.0,
        metabolizableEnergy = 3000.0,
        calcium = 0.6,
        phosphorus = 0.5,
        crudeFiber = 5.0
    )

    SmartSwineTheme {
        FeedFormulationResultContent(
            formulationResult = sampleResult,
            ingredients = sampleIngredients,
            targetRequirement = sampleTarget,
            onBack = {},
            onRecalculate = {},
            onExportPdf = {}
        )
    }
}
