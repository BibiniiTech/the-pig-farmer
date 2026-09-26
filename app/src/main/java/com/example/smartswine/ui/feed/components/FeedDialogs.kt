package com.example.smartswine.ui.feed.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.smartswine.model.FeedIngredient
import com.example.smartswine.model.FeedInventoryItem
import com.example.smartswine.model.NutritionalRequirement
import com.example.smartswine.ui.feed.getIngredientName
import com.example.smartswine.ui.theme.DarkBackground
import com.example.smartswine.utils.LocalAppLanguage
import com.example.smartswine.utils.getCategoryKey
import com.example.smartswine.utils.stringResource
import com.example.smartswine.ui.components.RewardedPassDialog
import com.example.smartswine.utils.LocalIsPremium
import com.example.smartswine.utils.TierLimiter
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.launch

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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource("export_feed_data")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource("select_date_range"), style = MaterialTheme.typography.titleSmall)
                
                ranges.forEach { range ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedRange.value = range }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedRange.value == range,
                            onClick = { selectedRange.value = range }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when(range) {
                                "Current Month" -> stringResource("current_month")
                                "Last 3 Months" -> stringResource("last_3_months")
                                else -> stringResource("custom_range")
                            }
                        )
                    }
                }

                if (selectedRange.value == "Custom") {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = fromDate.value,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource("from_date")) },
                            modifier = Modifier.weight(1f).clickable { showDatePickerFrom.value = true },
                            trailingIcon = {
                                IconButton(onClick = { showDatePickerFrom.value = true }) {
                                    Icon(Icons.Default.DateRange, null)
                                }
                            }
                        )
                        OutlinedTextField(
                            value = toDate.value,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource("to_date")) },
                            modifier = Modifier.weight(1f).clickable { showDatePickerTo.value = true },
                            trailingIcon = {
                                IconButton(onClick = { showDatePickerTo.value = true }) {
                                    Icon(Icons.Default.DateRange, null)
                                }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val title = when (selectedRange.value) {
                        "Current Month" -> "Feed Report - Current Month"
                        "Last 3 Months" -> "Feed Report - Last 3 Months"
                        else -> "Feed Report - ${fromDate.value} to ${toDate.value}"
                    }
                    onExport(feedInventoryItems, title, fromDate.value, toDate.value)
                }
            ) {
                Text(stringResource("export_pdf"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource("cancel")) }
        }
    )
}

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

    val isDark = MaterialTheme.colorScheme.background == DarkBackground
    val cardBg = if (isDark) MaterialTheme.colorScheme.surface else Color(0xFFFFF3E0)
    val cardBorder = if (isDark) Color(0xFFE65100).copy(alpha = 0.5f) else Color(0xFFFFE0B2)
    val trackColor = if (isDark) Color(0xFFE65100).copy(alpha = 0.24f) else Color(0xFFFFE0B2)

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
                    color = Color(0xFFE65100),
                    trackColor = trackColor,
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
                                    leadingIcon = { Icon(icon, null, modifier = Modifier.size(20.dp), tint = Color(0xFFE65100)) },
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
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = BorderStroke(1.dp, cardBorder),
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
                                tint = Color(0xFFE65100)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                stringResource("duration_projections"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
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
    onNavigateToPaywall: () -> Unit = {},
    onDismiss: () -> Unit,
    onFormulate: (String, List<String>) -> Unit
) {
    val isPremium = LocalIsPremium.current
    var showPassDialog by remember { mutableStateOf(false) }

    val standardStageNames = listOf("Creep", "Weaner/Starter", "Grower", "Finisher", "Pregnant", "Lactating")
    val growthStages = remember(requirements) {
        standardStageNames.map { stageName ->
            requirements.find {
                it.stage.equals(stageName, ignoreCase = true) ||
                (stageName == "Weaner/Starter" && (it.stage.equals("Starter", ignoreCase = true) || it.stage.equals("Weaner", ignoreCase = true)))
            } ?: NutritionalRequirement(stage = stageName)
        }
    }
    val selectedStage = remember { mutableStateOf("Weaner/Starter") }
    val selectedIngredients = remember { mutableStateListOf<String>() }
    val expandedStage = remember { mutableStateOf(false) }
    
    val groupedIngredients = remember(ingredients) {
        val categoryOrder = listOf("Energy", "Protein", "Vitamins, Minerals & Salt")
        ingredients.asSequence()
            .groupBy { 
                val cat = it.mainCategory.ifBlank { it.category }
                cat.ifBlank { "Uncategorized" }
            }
            .mapValues { entry -> entry.value.sortedBy { it.name.lowercase() } }
            .toSortedMap(compareBy<String> { 
                val index = categoryOrder.indexOf(it)
                if (index == -1) Int.MAX_VALUE else index 
            }.thenBy { it })
    }
    
    var expandedCategory by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource("mix_feed")) },
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
                
                LazyColumn(
                    state = listState,
                    modifier = Modifier.height(400.dp)
                ) {
                    groupedIngredients.forEach { (category, categoryIngredients) ->
                        item(key = "header_$category") {
                            val isExpanded = expandedCategory == category
                            Surface(
                                onClick = {
                                    val willExpand = !isExpanded
                                    expandedCategory = if (willExpand) category else null
                                    if (willExpand) {
                                        val targetIndex = groupedIngredients.keys.indexOf(category)
                                        if (targetIndex >= 0) {
                                            coroutineScope.launch {
                                                listState.animateScrollToItem(targetIndex)
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFFFFF3E0),
                                border = BorderStroke(1.dp, Color(0xFFFFE0B2)),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = Color(0xFFE65100)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(getCategoryKey(category)),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color(0xFFE65100),
                                        modifier = Modifier.weight(1f)
                                    )
                                    val count = categoryIngredients.count { selectedIngredients.contains(it.id) }
                                    if (count > 0) {
                                        Badge(containerColor = Color(0xFFE65100)) { Text(count.toString(), color = Color.White) }
                                    }
                                }
                            }
                        }
                        
                        if (expandedCategory == category) {
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
                                                if (!isPremium && selectedIngredients.size >= TierLimiter.FREE_MAX_FEED_INGREDIENTS) {
                                                    showPassDialog = true
                                                } else {
                                                    selectedIngredients.add(ingredient.id)
                                                }
                                            }
                                        }
                                        .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = selectedIngredients.contains(ingredient.id),
                                        onCheckedChange = { isChecked ->
                                            if (isChecked) {
                                                if (!isPremium && selectedIngredients.size >= TierLimiter.FREE_MAX_FEED_INGREDIENTS) {
                                                    showPassDialog = true
                                                } else {
                                                    selectedIngredients.add(ingredient.id)
                                                }
                                            } else {
                                                selectedIngredients.remove(ingredient.id)
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFFE65100))
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(getIngredientName(ingredient), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        val costText = if (ingredient.costPerKg > 0) " • ${String.format(Locale.getDefault(), "%.2f", ingredient.costPerKg)}/kg" else ""
                                        Text("CP: ${ingredient.crudeProtein}%$costText", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            }
                        }
                        
                        item(key = "spacer_$category") { Spacer(Modifier.height(4.dp)) }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onFormulate(selectedStage.value, selectedIngredients) },
                enabled = !isFormulating && selectedIngredients.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
            ) {
                if (isFormulating) CircularProgressIndicator(modifier = Modifier.size(16.dp))
                else Text(stringResource("run_formulator"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource("cancel")) }
        }
    )

    if (showPassDialog) {
        RewardedPassDialog(
            title = stringResource("unlock_unlimited_mixing_title"),
            description = stringResource("unlock_unlimited_mixing_desc"),
            onDismiss = { showPassDialog = false },
            onNavigateToPaywall = onNavigateToPaywall,
            onPassActivated = {
                // Pass activated
            }
        )
    }
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
                        category = mainCategory.value
                    )
                    onConfirm(ingredient)
                },
                enabled = name.value.isNotEmpty()
            ) { Text(stringResource("save_ingredient")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource("cancel")) } }
    )
}

@Composable
fun AddInventoryItemTypeDialog(
    onDismiss: () -> Unit,
    onSelectIngredient: () -> Unit,
    onSelectCompleteFeed: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource("add_item"),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource("choose_item_type_desc"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Option 1: Feed Ingredient
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            onSelectIngredient()
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, Color(0xFFE65100).copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFFFF3E0), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Grass, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(22.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource("feed_ingredient"),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = stringResource("feed_ingredient_desc"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Option 2: Complete Feed
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            onSelectCompleteFeed()
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, Color(0xFFE65100).copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFFFF3E0), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Inventory, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(22.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource("complete_feed"),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = stringResource("complete_feed_desc"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFE65100))
            ) {
                Text(stringResource("cancel"))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFeedIngredientInventoryDialog(
    availableIngredients: List<FeedIngredient>,
    currencySymbol: String = "$",
    onDismiss: () -> Unit,
    onConfirm: (name: String, category: String, qty: Double, unit: String, bagWeight: Double, threshold: Double, cost: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Energy") }
    var stockQuantityText by remember { mutableStateOf("") }
    val unit = "kg"
    var thresholdText by remember { mutableStateOf("50") }
    var totalCostText by remember { mutableStateOf("") }
    var ingredientDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource("feed_ingredient"), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource("select_from_catalog_or_enter_name"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ExposedDropdownMenuBox(
                    expanded = ingredientDropdownExpanded,
                    onExpandedChange = { ingredientDropdownExpanded = !ingredientDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { 
                            name = it
                            ingredientDropdownExpanded = true
                        },
                        label = { Text(stringResource("ingredient_name_label")) },
                        placeholder = { Text(stringResource("eg_maize_soybean_limestone")) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ingredientDropdownExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth(),
                        singleLine = true
                    )
                    val matching = remember(availableIngredients, name) {
                        if (name.isBlank()) availableIngredients
                        else availableIngredients.filter { it.name.contains(name, ignoreCase = true) }
                    }
                    if (matching.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = ingredientDropdownExpanded,
                            onDismissRequest = { ingredientDropdownExpanded = false }
                        ) {
                            matching.take(8).forEach { ing ->
                                DropdownMenuItem(
                                    text = { 
                                        Column {
                                            Text(getIngredientName(ing), fontWeight = FontWeight.Bold)
                                            Text("${stringResource(getCategoryKey(ing.mainCategory))} • $currencySymbol${String.format(Locale.ENGLISH, "%.2f", ing.costPerKg)}/kg", style = MaterialTheme.typography.labelSmall)
                                        }
                                    },
                                    onClick = {
                                        name = ing.name
                                        if (ing.mainCategory.isNotBlank()) selectedCategory = ing.mainCategory
                                        val currentQty = stockQuantityText.toDoubleOrNull() ?: 0.0
                                        if (ing.costPerKg > 0) {
                                            totalCostText = if (currentQty > 0) {
                                                String.format(Locale.ENGLISH, "%.2f", ing.costPerKg * currentQty)
                                            } else {
                                                String.format(Locale.ENGLISH, "%.2f", ing.costPerKg)
                                            }
                                        }
                                        ingredientDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = stockQuantityText,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) stockQuantityText = it },
                    label = { Text(stringResource("stock_quantity_kgs")) },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = totalCostText,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) totalCostText = it },
                    label = { Text("${stringResource("total_cost")} ($currencySymbol)") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                val qty = stockQuantityText.toDoubleOrNull() ?: 0.0
                val totalCost = totalCostText.toDoubleOrNull() ?: 0.0
                val costPerKg = if (qty > 0.0) totalCost / qty else 0.0

                if (qty > 0.0 && totalCost > 0.0) {
                    Text(
                        text = "${stringResource("cost_per_kg_label")} $currencySymbol${String.format(Locale.getDefault(), "%.2f", costPerKg)} / kg",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                OutlinedTextField(
                    value = thresholdText,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) thresholdText = it },
                    label = { Text(stringResource("min_threshold")) },
                    placeholder = { Text("50") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = stockQuantityText.toDoubleOrNull() ?: 0.0
                    val bagWeight = 0.0
                    val threshold = thresholdText.toDoubleOrNull() ?: 0.0
                    val totalCost = totalCostText.toDoubleOrNull() ?: 0.0
                    val costPerKg = if (qty > 0.0) totalCost / qty else 0.0
                    onConfirm(name, selectedCategory, qty, "kg", bagWeight, threshold, costPerKg)
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100), contentColor = Color.White)
            ) {
                Text(stringResource("save"))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFE65100))
            ) {
                Text(stringResource("cancel"))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFeedInventoryItemDialog(
    currencySymbol: String = "$",
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: String, qty: Double, unit: String, bagWeight: Double, threshold: Double, cost: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("Weaner/Starter") }
    var stockQuantityText by remember { mutableStateOf("") }
    val unit = "kg"
    var totalCostText by remember { mutableStateOf("") }
    var thresholdText by remember { mutableStateOf("50") }
    var typeDropdownExpanded by remember { mutableStateOf(false) }

    val feedTypes = listOf("Creep", "Weaner/Starter", "Grower", "Finisher", "Pregnant", "Lactating", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource("complete_feed"), fontWeight = FontWeight.Bold) },
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
                    val currentDisplayType = if (selectedType.equals("Weaner/Starter", ignoreCase = true)) {
                        stringResource("weaner_starter")
                    } else {
                        stringResource(getCategoryKey(selectedType))
                    }
                    OutlinedTextField(
                        value = currentDisplayType,
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
                            val itemDisplayType = if (type.equals("Weaner/Starter", ignoreCase = true)) {
                                stringResource("weaner_starter")
                            } else {
                                stringResource(getCategoryKey(type))
                            }
                            DropdownMenuItem(
                                text = { Text(itemDisplayType) },
                                onClick = {
                                    selectedType = type
                                    typeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = stockQuantityText,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) stockQuantityText = it },
                    label = { Text(stringResource("stock_quantity_kgs")) },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = totalCostText,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) totalCostText = it },
                    label = { Text("${stringResource("total_cost")} ($currencySymbol)") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                val qty = stockQuantityText.toDoubleOrNull() ?: 0.0
                val totalCost = totalCostText.toDoubleOrNull() ?: 0.0
                val costPerKg = if (qty > 0.0) totalCost / qty else 0.0

                if (qty > 0.0 && totalCost > 0.0) {
                    Text(
                        text = "${stringResource("cost_per_kg_label")} $currencySymbol${String.format(Locale.getDefault(), "%.2f", costPerKg)} / kg",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                OutlinedTextField(
                    value = thresholdText,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) thresholdText = it },
                    label = { Text(stringResource("min_threshold")) },
                    placeholder = { Text("50") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = stockQuantityText.toDoubleOrNull() ?: 0.0
                    val threshold = thresholdText.toDoubleOrNull() ?: 0.0
                    val totalCost = totalCostText.toDoubleOrNull() ?: 0.0
                    val costPerKg = if (qty > 0.0) totalCost / qty else 0.0
                    onConfirm(name, selectedType, qty, "kg", 0.0, threshold, costPerKg)
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100), contentColor = Color.White)
            ) {
                Text(stringResource("save"))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFE65100))
            ) {
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
                enabled = qty > 0.0,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100), contentColor = Color.White)
            ) {
                Text(stringResource("ok"))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFE65100))
            ) {
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
                enabled = qty > 0.0,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100), contentColor = Color.White)
            ) {
                Text(stringResource("ok"))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFE65100))
            ) {
                Text(stringResource("cancel"))
            }
        }
    )
}

@Composable
fun SaveRecipeDialog(
    initialName: String = "",
    stage: String = "",
    onDismiss: () -> Unit,
    onSave: (name: String, notes: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName.ifBlank { "$stage Mix Recipe" }) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource("save_feed_recipe"), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource("save_recipe_description"), style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource("recipe_name")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource("notes_optional")) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, notes) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
            ) {
                Text(stringResource("save_recipe"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource("cancel")) }
        }
    )
}

@Composable
fun BatchMixDialog(
    recipeName: String,
    stage: String,
    defaultBatchKg: Double = 1000.0,
    requiredIngredients: List<Pair<String, Double>> = emptyList(),
    feedInventoryItems: List<FeedInventoryItem> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (batchKg: Double) -> Unit
) {
    var batchKgText by remember { mutableStateOf(String.format(Locale.ENGLISH, "%.0f", defaultBatchKg)) }
    var isMixing by remember { mutableStateOf(false) }

    val outOfStockIngredients = remember(requiredIngredients, feedInventoryItems) {
        requiredIngredients.mapNotNull { (ingredientName, _) ->
            val cleanName = ingredientName.trim().lowercase(Locale.ENGLISH)
            val invItem = feedInventoryItems.find {
                it.name.trim().lowercase(Locale.ENGLISH) == cleanName ||
                it.feedType.trim().lowercase(Locale.ENGLISH) == cleanName ||
                it.id.trim().lowercase(Locale.ENGLISH) == cleanName
            }
            if (invItem == null || invItem.quantity <= 0.0) {
                ingredientName
            } else {
                null
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isMixing) onDismiss() },
        title = { Text(stringResource("execute_batch_mix"), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    stringResource("batch_mix_description", stage),
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = batchKgText,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) batchKgText = it },
                    label = { Text(stringResource("batch_size_kg")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isMixing
                )

                if (outOfStockIngredients.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = stringResource("out_of_stock_ingredients"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Text(
                                text = stringResource("ingredients_not_in_stock"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            outOfStockIngredients.forEach { name ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(MaterialTheme.colorScheme.error, CircleShape)
                                    )
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val batchKg = batchKgText.toDoubleOrNull() ?: 0.0
            Button(
                onClick = {
                    if (!isMixing && batchKg > 0.0) {
                        isMixing = true
                        onConfirm(batchKg)
                    }
                },
                enabled = batchKg > 0.0 && !isMixing,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
            ) {
                if (isMixing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(stringResource("mix_and_restock"))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isMixing
            ) {
                Text(stringResource("cancel"))
            }
        }
    )
}

