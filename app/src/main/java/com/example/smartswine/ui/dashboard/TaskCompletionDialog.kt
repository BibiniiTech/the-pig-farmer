package com.example.smartswine.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.smartswine.model.HealthRecord
import com.example.smartswine.model.Pig
import com.example.smartswine.model.TaskItem
import com.example.smartswine.utils.DateUtils
import com.example.smartswine.utils.LocalAppLanguage
import com.example.smartswine.utils.stringResource
import com.example.smartswine.ui.theme.SmartSwineTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCompletionDialog(
    tasksToEdit: List<TaskItem>,
    allPigs: List<Pig>,
    onDismissRequest: () -> Unit,
    onDeleteTask: () -> Unit,
    onLogHealthActivity: (List<String>, HealthRecord, Boolean, Boolean, Boolean, Map<String, Any>) -> Unit,
) {
    // Side effect to handle empty list - avoids modifying state during composition
    LaunchedEffect(tasksToEdit) {
        if (tasksToEdit.isEmpty()) {
            onDismissRequest()
        }
    }

    if (tasksToEdit.isEmpty()) return

    val firstTask = tasksToEdit.first()
    val activityName = remember(tasksToEdit) { firstTask.name.substringBefore(": ").trim() }
    
    // Use the utility from DashboardUtils.kt
    val translatedActivityName = getTranslatedActivityName(activityName)
    val isHeatDetection = remember(activityName) { activityName.contains("Heat", ignoreCase = true) }
    val isBreeding = remember(activityName) { 
        activityName.contains("Breeding", ignoreCase = true) || activityName.contains("Mating", ignoreCase = true) 
    }
    val isPregnancyCheck = remember(activityName) { activityName.contains("Pregnancy Check", ignoreCase = true) }
    val isFarrowing = remember(activityName) { activityName.contains("Farrowing", ignoreCase = true) }
    val isWeaning = remember(activityName) { activityName.contains("Weaning", ignoreCase = true) }
    val isWeightCheck = remember(activityName) { activityName.contains("Weight Check", ignoreCase = true) }
    val isCulling = remember(activityName) { activityName.contains("Culling", ignoreCase = true) }
    val isMedicationActivity = remember(activityName) {
        activityName.contains("Iron Injection", true) ||
        activityName.contains("Deworming", ignoreCase = true) ||
        activityName.contains("Vaccination", ignoreCase = true) ||
        activityName.contains("Medication", ignoreCase = true)
    }
    

    // State for multi-animal selection and per-animal inputs
    val taskPigIdentifiers = remember(tasksToEdit) {
        tasksToEdit.flatMap { task ->
            val identifierPart = task.name.substringAfter(": ", "")
            if (identifierPart.isNotEmpty()) {
                // Split by comma to handle multiple pigs in one task (e.g. "Pigs 1, 2")
                identifierPart.split(",")
                    .map { it.replace("Pigs", "", ignoreCase = true)
                             .replace("Pig", "", ignoreCase = true)
                             .trim() }
                    .filter { it.isNotEmpty() }
            } else emptyList()
        }
    }

    // Pre-map for performance
    val pigIdMap = remember(allPigs) { allPigs.associateBy { it.id } }
    val pigTagMap = remember(allPigs) { allPigs.associateBy { it.tagNumber } }

    // Resolve identifiers to IDs for state management
    val taskPigIds = remember(taskPigIdentifiers, allPigs, activityName) {
        taskPigIdentifiers.asSequence().map { identifier ->
            pigIdMap[identifier]?.id
                ?: pigTagMap[identifier]?.id
                ?: identifier
        }.filter { id ->
            // Ensure male pigs aren't shown for female-specific tasks
            if (activityName.contains("Pregnancy", ignoreCase = true) || 
                activityName.contains("Farrowing", ignoreCase = true) || 
                activityName.contains("Heat", ignoreCase = true) ||
                activityName.contains("Breeding", ignoreCase = true) ||
                activityName.contains("Mating", ignoreCase = true)) {
                val pig = pigIdMap[id]
                ((pig == null) || (pig.gender.equals("Female", ignoreCase = true)))
            } else true
        }.distinct().toList().sortedBy { id -> 
            pigIdMap[id]?.tagNumber ?: id 
        }
    }

    val isGroup = remember(taskPigIds) { taskPigIds.size > 1 }

    val appLanguage = LocalAppLanguage.current
    val locale = remember(appLanguage) { appLanguage.toLocale() }

    // State for dialog inputs - Keyed to tasksToEdit to reset when the task changes
    val pigOutcomesState = remember(tasksToEdit) { mutableStateOf(mapOf<String, String>()) }
    val pigWeaningLocationsState = remember(tasksToEdit) { mutableStateOf(mapOf<String, String>()) }
    val pigNumMalesState = remember(tasksToEdit) { mutableStateOf(mapOf<String, String>()) }
    val pigNumFemalesState = remember(tasksToEdit) { mutableStateOf(mapOf<String, String>()) }
    val pigMaleTagsState = remember(tasksToEdit) { mutableStateOf(mapOf<String, String>()) }
    val pigFemaleTagsState = remember(tasksToEdit) { mutableStateOf(mapOf<String, String>()) }
    val actualDateState = remember(tasksToEdit, locale) { 
        mutableStateOf(DateUtils.convertToDisplayDate(firstTask.date, locale)) 
    }
    val selectedPigIdsState = remember(taskPigIds) { mutableStateOf(taskPigIds.toSet()) }
    val medicationNameState = remember(tasksToEdit) { mutableStateOf("") }
    val medicationDosageState = remember(tasksToEdit) { mutableStateOf("") }
    val dosageLabel = if (medicationDosageState.value.isNotEmpty()) {
        stringResource("dosage_with_value", medicationDosageState.value)
    } else ""
    val pigScheduleSecondIronState = remember(tasksToEdit) { mutableStateOf(mapOf<String, Boolean>()) }
    val pigWeightsState = remember(tasksToEdit) { mutableStateOf(mapOf<String, String>()) }
    val pigSalePricesState = remember(tasksToEdit) { mutableStateOf(mapOf<String, String>()) }
    val notesState = remember(tasksToEdit) { mutableStateOf("") }
    val cullingReasonState = remember(tasksToEdit) { mutableStateOf("") }
    val scheduleNextState = remember(tasksToEdit) { mutableStateOf(false) }
    val nextDateState = remember(tasksToEdit, locale) { 
        mutableStateOf(DateUtils.formatDateToDisplay(System.currentTimeMillis() + 14L * 24 * 60 * 60 * 1000, locale)) 
    }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = remember(firstTask.date) {
            DateUtils.parseDisplay(firstTask.date, locale)?.time 
                ?: DateUtils.parseProduction(firstTask.date)?.time 
                ?: System.currentTimeMillis()
        }
    )
    val showDatePicker = remember { mutableStateOf(false) }
    val showNextDatePicker = remember { mutableStateOf(false) }

    if (showDatePicker.value) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker.value = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        actualDateState.value = DateUtils.formatDateToDisplay(it, locale)
                    }
                    showDatePicker.value = false
                }) { Text(stringResource("ok")) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker.value = false }) { Text(stringResource("cancel")) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showNextDatePicker.value) {
        val nextDatePickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.parseDisplay(nextDateState.value, locale)?.time ?: (System.currentTimeMillis() + 14L * 24 * 60 * 60 * 1000)
        )
        DatePickerDialog(
            onDismissRequest = { showNextDatePicker.value = false },
            confirmButton = {
                TextButton(onClick = {
                    nextDatePickerState.selectedDateMillis?.let {
                        nextDateState.value = DateUtils.formatDateToDisplay(it, locale)
                    }
                    showNextDatePicker.value = false
                }) { Text(stringResource("ok")) }
            },
            dismissButton = {
                TextButton(onClick = { showNextDatePicker.value = false }) { Text(stringResource("cancel")) }
            }
        ) {
            DatePicker(state = nextDatePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = { },
        dismissButton = { },
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = true,
        ),
        title = {
            // Use getTaskIcon from DashboardUtils.kt
            val icon = remember(activityName) { getTaskIcon(activityName) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (icon) {
                    is TaskIcon.Vector -> Icon(
                        imageVector = icon.imageVector,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp),
                    )
                    is TaskIcon.Resource -> Icon(
                        painter = painterResource(id = icon.resId),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (isGroup) "$translatedActivityName (${tasksToEdit.size})" else translatedActivityName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .padding(top = 8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Animal Information Section
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tag,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${if (isGroup) stringResource("affected_animals") else stringResource("target_animal")}: ",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isGroup) {
                                taskPigIds.joinToString(", ") { id ->
                                    pigIdMap[id]?.tagNumber ?: id
                                }
                            } else {
                                val identifier = taskPigIdentifiers.firstOrNull() ?: "General Task"
                                val resolved = pigIdMap[identifier]?.tagNumber
                                    ?: pigTagMap[identifier]?.tagNumber
                                    ?: identifier
                                if (resolved == "General Task") stringResource("general_task") else resolved
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Date Section
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = stringResource("activity_date"),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = actualDateState.value,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { showDatePicker.value = true }) {
                                    Icon(Icons.Default.DateRange, contentDescription = stringResource("select_date"))
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyLarge,
                            singleLine = true
                        )
                    }
                }

                // Culling Reason Section
                if (isCulling) {
                    val reasons = listOf(
                        stringResource("reason_natural") to "Natural",
                        stringResource("reason_disease") to "Disease",
                        stringResource("reason_sold") to "Sold"
                    )
                    var reasonExpanded by remember { mutableStateOf(false) }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.QuestionMark,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = stringResource("reason"),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            ExposedDropdownMenuBox(
                                expanded = reasonExpanded,
                                onExpandedChange = { reasonExpanded = !reasonExpanded }
                            ) {
                                val currentDisplayLabel = reasons.find { it.second == cullingReasonState.value }?.first ?: ""
                                OutlinedTextField(
                                    value = currentDisplayLabel,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reasonExpanded) },
                                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                                    textStyle = MaterialTheme.typography.bodyLarge,
                                    singleLine = true
                                )
                                ExposedDropdownMenu(
                                    expanded = reasonExpanded,
                                    onDismissRequest = { reasonExpanded = false }
                                ) {
                                    reasons.forEach { (displayLabel, internalValue) ->
                                        DropdownMenuItem(
                                            text = { Text(displayLabel) },
                                            onClick = {
                                                cullingReasonState.value = internalValue
                                                reasonExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Notes Section (if any)
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = stringResource("notes"),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = notesState.value,
                            onValueChange = { notesState.value = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            minLines = 1
                        )
                    }
                }

                // Medication Section (if applicable)
                if (activityName.contains("Iron Injection", true) ||
                    activityName.contains("Deworming", ignoreCase = true) ||
                    activityName.contains("Vaccination", ignoreCase = true) ||
                    activityName.contains("Medication", ignoreCase = true)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = medicationNameState.value,
                            onValueChange = { medicationNameState.value = it },
                            label = { Text(stringResource("medication_name")) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = medicationDosageState.value,
                            onValueChange = { medicationDosageState.value = it },
                            label = { Text(stringResource("dosage")) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                if (isGroup) {
                    Text(
                        text = stringResource("select_animals_to_complete"),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    taskPigIds.forEach { id ->
                        val pig = pigIdMap[id]
                        val isSelected = id in selectedPigIdsState.value
                        
                        if (isGroup) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedPigIdsState.value =
                                            if (!isSelected) selectedPigIdsState.value + id else selectedPigIdsState.value - id
                                    }
                                    .padding(vertical = 2.dp)
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        selectedPigIdsState.value = if (checked) selectedPigIdsState.value + id else selectedPigIdsState.value - id
                                    }
                                )
                                Text(
                                    text = pig?.tagNumber ?: id,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                
                                if (isWeaning && isSelected) {
                                    OutlinedTextField(
                                        value = pigWeaningLocationsState.value[id] ?: "",
                                        onValueChange = { pigWeaningLocationsState.value += (id to it) },
                                        placeholder = { Text(stringResource("pen_hash")) },
                                        modifier = Modifier.width(100.dp).padding(start = 8.dp),
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodySmall
                                    )
                                }
                                
                                if (isCulling && cullingReasonState.value == "Sold" && isSelected) {
                                    OutlinedTextField(
                                        value = pigSalePricesState.value[id] ?: "",
                                        onValueChange = { pigSalePricesState.value += (id to it) },
                                        placeholder = { Text(stringResource("amount")) },
                                        modifier = Modifier.width(100.dp).padding(start = 8.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodySmall
                                    )
                                }
                                
                                if (isWeightCheck && isSelected) {
                                    OutlinedTextField(
                                        value = pigWeightsState.value[id] ?: "",
                                        onValueChange = { pigWeightsState.value += (id to it) },
                                        placeholder = { Text(stringResource("kg")) },
                                        modifier = Modifier.width(100.dp).padding(start = 8.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }

                        if (isSelected && !isHeatDetection) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (!isGroup) {
                                    Text(
                                        text = "${stringResource("target_animal")}: ${pig?.tagNumber ?: id}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                    when {
                                        activityName.contains("Weight Check", ignoreCase = true) -> {
                                            if (!isGroup) {
                                                OutlinedTextField(
                                                    value = pigWeightsState.value[id] ?: "",
                                                    onValueChange = { pigWeightsState.value += (id to it) },
                                                    label = { Text(stringResource("kg")) },
                                                    modifier = Modifier.width(180.dp),
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    singleLine = true
                                                )
                                            }
                                        }
                                    activityName.contains("Culling", ignoreCase = true) -> {
                                        if (!isGroup && cullingReasonState.value == "Sold") {
                                            OutlinedTextField(
                                                value = pigSalePricesState.value[id] ?: "",
                                                onValueChange = { pigSalePricesState.value += (id to it) },
                                                label = { Text(stringResource("amount")) },
                                                modifier = Modifier.width(180.dp),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true
                                            )
                                        }
                                    }
                                    activityName.contains("Weaning", ignoreCase = true) -> {
                                        if (!isGroup) {
                                            OutlinedTextField(
                                                value = pigWeaningLocationsState.value[id] ?: "",
                                                onValueChange = { pigWeaningLocationsState.value += (id to it) },
                                                label = { Text(stringResource("pen_hash")) },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                    activityName.contains("Iron Injection", true) -> {
                                        // Specific per-animal inputs if any (none required currently after generalizing schedule next)
                                    }
                                }
                            }
                        }
                    }
                }

                if (isHeatDetection || isBreeding || isPregnancyCheck || isFarrowing) {
                    Text(
                        text = stringResource("select_outcome"),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        textAlign = TextAlign.Center
                    )
                    
                    if (isFarrowing) {
                        selectedPigIdsState.value.forEach { id ->
                            val pig = pigIdMap[id]
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (isGroup) {
                                    Text(
                                        text = "${stringResource("target_animal")}: ${pig?.tagNumber ?: id}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(0.9f),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedTextField(
                                        value = pigNumMalesState.value[id] ?: "",
                                        onValueChange = { pigNumMalesState.value += (id to it) },
                                        label = { Text(stringResource("males")) },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = pigNumFemalesState.value[id] ?: "",
                                        onValueChange = { pigNumFemalesState.value += (id to it) },
                                        label = { Text(stringResource("females")) },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true
                                    )
                                }
                                
                                val numMales = pigNumMalesState.value[id]?.toIntOrNull() ?: 0
                                val numFemales = pigNumFemalesState.value[id]?.toIntOrNull() ?: 0
                                
                                if (numMales > 0) {
                                    OutlinedTextField(
                                        value = pigMaleTagsState.value[id] ?: "",
                                        onValueChange = { pigMaleTagsState.value += (id to it) },
                                        label = { Text(stringResource("male_piglets")) },
                                        modifier = Modifier.fillMaxWidth(0.9f),
                                        placeholder = { Text(stringResource("tag_help_text")) }
                                    )
                                }
                                
                                if (numFemales > 0) {
                                    OutlinedTextField(
                                        value = pigFemaleTagsState.value[id] ?: "",
                                        onValueChange = { pigFemaleTagsState.value += (id to it) },
                                        label = { Text(stringResource("female_piglets")) },
                                        modifier = Modifier.fillMaxWidth(0.9f),
                                        placeholder = { Text(stringResource("tag_help_text")) }
                                    )
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val selectedOutcome = selectedPigIdsState.value.firstOrNull()?.let { pigOutcomesState.value[it] } ?: ""
                            
                            val outcomeButtons = when {
                                isHeatDetection -> listOf(
                                    stringResource("heat_detected") to "Heat Detected",
                                    stringResource("no_heat") to "No Heat Detected"
                                )
                                isBreeding -> listOf(
                                    stringResource("mating_successful") to "Mating Successful",
                                    stringResource("mating_failed") to "Mating Failed"
                                )
                                else -> listOf( // Pregnancy Check
                                    stringResource("successful") to "Successful",
                                    stringResource("failed") to "Failed"
                                )
                            }

                            outcomeButtons.forEach { (label, value) ->
                                val isSelected = selectedOutcome == value
                                
                                OutlinedButton(
                                    onClick = { 
                                        val newOutcomes = pigOutcomesState.value.toMutableMap()
                                        selectedPigIdsState.value.forEach { id -> newOutcomes[id] = value }
                                        pigOutcomesState.value = newOutcomes
                                    },
                                    modifier = Modifier.fillMaxWidth(0.6f).height(42.dp),
                                    shape = androidx.compose.ui.graphics.RectangleShape,
                                    colors = if (isSelected) {
                                        ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    } else {
                                        ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp, 
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    ),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                if (isMedicationActivity) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = scheduleNextState.value,
                                onCheckedChange = { scheduleNextState.value = it }
                            )
                            Text(stringResource("schedule_next"))
                        }
                        
                        if (scheduleNextState.value) {
                            OutlinedTextField(
                                value = nextDateState.value,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource("next_activity_date")) },
                                trailingIcon = {
                                    IconButton(onClick = { showNextDatePicker.value = true }) {
                                        Icon(Icons.Default.DateRange, contentDescription = null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(0.8f)
                            )
                        }
                    }
                }

                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(stringResource("cancel"))
                    }
                    
                    TextButton(onClick = onDeleteTask) {
                        Text(stringResource("delete"), color = MaterialTheme.colorScheme.error)
                    }

                    Button(
                        onClick = {
                            val finalPigIds = selectedPigIdsState.value.toList()
                            if (finalPigIds.isNotEmpty()) {
                                val data = mutableMapOf<String, Any>()
                                data["notes"] = notesState.value
                                data["medicationName"] = medicationNameState.value
                                data["dosage"] = medicationDosageState.value
                                
                                data["pigWeights"] = pigWeightsState.value
                                data["pigOutcomes"] = pigOutcomesState.value
                                data["pigLocations"] = pigWeaningLocationsState.value
                                data["pigNumMales"] = pigNumMalesState.value
                                data["pigNumFemales"] = pigNumFemalesState.value
                                data["pigMaleTags"] = pigMaleTagsState.value
                                data["pigFemaleTags"] = pigFemaleTagsState.value
                                data["pigSalePrices"] = pigSalePricesState.value
                                data["cullingReason"] = cullingReasonState.value
                                
                                // Calculate total sale price for financials
                                if (isCulling && cullingReasonState.value == "Sold") {
                                    val totalAmount = pigSalePricesState.value.values.sumOf { it.toDoubleOrNull() ?: 0.0 }
                                    data["salePrice"] = totalAmount
                                }

                                // Extract breeding tags if applicable
                                if (isBreeding) {
                                    // Extract from first task name which is usually "Breeding/Mating: Sow TAG-X, Boar TAG-Y"
                                    // or just get the identifiers
                                    val taskName = firstTask.name
                                    val sowTag = taskName.substringAfter("Sow ", "").substringBefore(",").trim()
                                    val boarTag = taskName.substringAfter("Boar ", "").trim()
                                    if (sowTag.isNotEmpty()) data["sowTag"] = sowTag
                                    if (boarTag.isNotEmpty()) data["boarTag"] = boarTag
                                }

                                data["pigScheduleSecondIron"] = pigScheduleSecondIronState.value
                                
                                if (scheduleNextState.value) {
                                    data["nextScheduledDate"] = DateUtils.parseDisplay(nextDateState.value, locale)?.let { 
                                        DateUtils.formatToProduction(it) 
                                    } ?: ""
                                }

                                onLogHealthActivity(
                                    finalPigIds,
                                    HealthRecord(
                                        type = activityName,
                                        date = DateUtils.parseDisplay(actualDateState.value, locale)?.let { DateUtils.formatToProduction(it) } ?: "",
                                        medication = medicationNameState.value,
                                        description = if (medicationDosageState.value.isNotEmpty()) 
                                            "${notesState.value} ($dosageLabel)".trim()
                                            else notesState.value
                                    ),
                                    pigOutcomesState.value.values.any { it == "Heat Detected" },
                                    activityName.contains("Breeding", true) || activityName.contains("Mating", true),
                                    pigOutcomesState.value.values.any { it == "Successful" || it == "Mating Successful" },
                                    data
                                )
                            }
                        },
                        enabled = when {
                            taskPigIds.isNotEmpty() && selectedPigIdsState.value.isEmpty() -> false
                            isHeatDetection -> selectedPigIdsState.value.all { pigOutcomesState.value.containsKey(it) }
                            isBreeding -> selectedPigIdsState.value.all { pigOutcomesState.value.containsKey(it) }
                            isPregnancyCheck -> selectedPigIdsState.value.all { pigOutcomesState.value.containsKey(it) }
                            isFarrowing -> selectedPigIdsState.value.all { id ->
                                val nMales = pigNumMalesState.value[id]?.toIntOrNull() ?: 0
                                val nFemales = pigNumFemalesState.value[id]?.toIntOrNull() ?: 0
                                if (nMales == 0 && nFemales == 0) return@all false
                                
                                val mTags = pigMaleTagsState.value[id]?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
                                val fTags = pigFemaleTagsState.value[id]?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
                                
                                // Validate counts
                                if (nMales != mTags.size || nFemales != fTags.size) return@all false
                                
                                // Validate no duplicates across all selected pigs for this farrowing
                                val allTags = selectedPigIdsState.value.flatMap { pid ->
                                    val mt = pigMaleTagsState.value[pid]?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
                                    val ft = pigFemaleTagsState.value[pid]?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
                                    mt + ft
                                }
                                allTags.size == allTags.distinct().size
                            }
                            isWeaning -> selectedPigIdsState.value.all { id ->
                                pigWeaningLocationsState.value[id]?.isNotEmpty() == true
                            }
                            activityName.contains("Weight Check", ignoreCase = true) -> 
                                selectedPigIdsState.value.all { id -> (pigWeightsState.value[id]?.toDoubleOrNull() ?: 0.0) > 0 }
                            else -> true
                        }
                    ) {
                        Text(stringResource("save"))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    )
}

@Preview(showBackground = true)
@Composable
fun TaskCompletionDialogPreview() {
    SmartSwineTheme {
        TaskCompletionDialog(
            tasksToEdit = listOf(
                TaskItem(
                    id = "1",
                    name = "Iron Injection: Pig 101",
                    date = "2023-10-27",
                    completed = false
                )
            ),
            allPigs = listOf(
                Pig(id = "Pig 101", tagNumber = "101", gender = "Female")
            ),
            onDismissRequest = {},
            onDeleteTask = {},
            onLogHealthActivity = { _, _, _, _, _, _ -> }
        )
    }
}

