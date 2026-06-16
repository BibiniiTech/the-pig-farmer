package com.example.smartswine.ui.feed

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
import com.example.smartswine.util.PdfGenerator
import com.example.smartswine.utils.stringResource
import com.example.smartswine.utils.Translator
import com.example.smartswine.utils.LocalAppLanguage
import java.util.Locale

import com.example.smartswine.utils.getIngredientNameKey
import com.example.smartswine.utils.getCategoryKey

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
    val targetRequirement by viewModel.targetRequirement.collectAsStateWithLifecycle()
    
    val target = targetRequirement
    val context = LocalContext.current

    val lang = LocalAppLanguage.current.code

    FeedFormulationResultContent(
        formulationResult = formulationResult,
        ingredients = ingredients,
        targetRequirement = target,
        onBack = onBack,
        onRecalculate = { viewModel.recalculateFormulation() },
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

        val actual = calculateFormulatedNutrients(result, ingredients)
        val comparison = if (target != null) {
            listOf(
                mapOf("label" to Translator.getString("crude_protein_pct", lang), "target" to target.digestibleProtein, "actual" to (actual["protein"] ?: 0.0), "isDeficient" to ((actual["protein"] ?: 0.0) < target.digestibleProtein)),
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
            lang = lang
        )
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
    onExportPdf: (Map<String, Double>) -> Unit,
) {
    var batchSize by remember { mutableStateOf("1000") }
    val batchSizeDouble = batchSize.toDoubleOrNull() ?: 0.0

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
                        color = MaterialTheme.colorScheme.primary,
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

                Card(colors = CardDefaults.cardColors(containerColor = if (isIncomplete) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource("best_mix_formulation"), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1.5f))
                            Text("%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
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
                                        textStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                }
                                Text(stringResource("kg"), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 2.dp))
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
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                ids.forEach { id ->
                                    val ingredient = ingredients.find { it.id == id }
                                    val percent = result[id] ?: 0.0
                                    val weight = (percent / 100.0) * batchSizeDouble
                                    Row(modifier = Modifier.fillMaxWidth().padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(ingredient?.let { getTranslatedIngredientName(it) } ?: id, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1.5f))
                                        Text("${String.format(Locale.getDefault(), "%.1f", percent)}%", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.7f), textAlign = TextAlign.End)
                                        Text("${String.format(Locale.getDefault(), "%.1f", weight)}kg", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        // Handle any ingredients not in the predefined categories
                        val otherIds = groupedResults.filter { it.key !in categoryOrder }.values.flatten()
                        if (otherIds.isNotEmpty()) {
                            Text(stringResource("other").uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                            otherIds.forEach { id ->
                                val ingredient = ingredients.find { it.id == id }
                                val percent = result[id] ?: 0.0
                                val weight = (percent / 100.0) * batchSizeDouble
                                Row(modifier = Modifier.fillMaxWidth().padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(ingredient?.let { getTranslatedIngredientName(it) } ?: id, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1.5f))
                                    Text("${String.format(Locale.getDefault(), "%.1f", percent)}%", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.7f), textAlign = TextAlign.End)
                                    Text("${String.format(Locale.getDefault(), "%.1f", weight)}kg", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                                }
                            }
                        }
                    }
                }
                
                Button(
                    onClick = onRecalculate,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource("recalculate_formulation"))
                }

                targetRequirement?.let { target ->
                    val actual = calculateFormulatedNutrients(result, ingredients)
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
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource("nutritional_comparison", stringResource(getCategoryKey(target.stage))), style = MaterialTheme.typography.titleMedium)
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)
            
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(stringResource("nutrient"), modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                Text(stringResource("target"), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                Text(stringResource("actual"), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
            }

            ComparisonRow(stringResource("crude_protein_pct"), target.digestibleProtein, actual["protein"] ?: 0.0)
            ComparisonRow(stringResource("me_kcal_kg"), target.metabolizableEnergy, actual["energy"] ?: 0.0)
            ComparisonRow(stringResource("calcium_pct"), target.calcium, actual["calcium"] ?: 0.0)
            ComparisonRow(stringResource("phosphorus_pct"), target.phosphorus, actual["phosphorus"] ?: 0.0)
            ComparisonRow(stringResource("crude_fiber_pct"), target.crudeFiber, actual["fiber"] ?: 0.0, isMaximumLimit = true)
            
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onExportPdf,
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
    val isWarning = if (isMaximumLimit) actual > target else actual < target
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.bodySmall)
        Text(
            text = String.format(Locale.getDefault(), "%.1f", target),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = String.format(Locale.getDefault(), "%.1f", actual),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isWarning) Color.Red else Color(0xFF2E7D32) // Forest Green
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

    result.forEach { (id, percent) ->
        val ing = ingredients.find { it.id == id } ?: return@forEach
        val factor = percent / 100.0
        protein += ing.crudeProtein * factor
        energy += ing.metabolizableEnergy * factor
        
        val ingCa = if (ing.calcium > 10.0) ing.calcium / 10.0 else ing.calcium
        calcium += ingCa * factor
        
        val ingP = if (ing.phosphorus > 10.0) ing.phosphorus / 10.0 else ing.phosphorus
        phosphorus += ingP * factor
        
        fiber += ing.crudeFiber * factor
    }

    return mapOf(
        "protein" to protein,
        "energy" to energy,
        "calcium" to calcium,
        "phosphorus" to phosphorus,
        "fiber" to fiber
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
