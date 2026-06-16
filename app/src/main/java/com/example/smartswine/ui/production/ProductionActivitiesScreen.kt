package com.example.smartswine.ui.production

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.smartswine.utils.StylishDivider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bibiniitech.smartswine.R
import com.example.smartswine.model.Pig
import com.example.smartswine.ui.herd.HerdViewModel
import com.example.smartswine.ui.theme.SmartSwineTheme
import com.example.smartswine.utils.DateUtils
import com.example.smartswine.utils.LocalAppLanguage
import com.example.smartswine.utils.Translator
import com.example.smartswine.utils.getTranslatedActivityType
import com.example.smartswine.utils.stringResource

@Composable
fun ProductionActivitiesScreen(
    viewModel: ProductionViewModel,
    herdViewModel: HerdViewModel,
    onBack: () -> Unit
) {
    val pigs by herdViewModel.pigs.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    ProductionActivitiesContent(
        pigs = pigs,
        isLoading = isLoading,
        onLogActivity = { pigIds, activityName, notes, date, trackHeat, checkPregnancy, extra ->
            viewModel.logHealthActivity(
                pigIds = pigIds,
                record = com.example.smartswine.model.HealthRecord(
                    date = date,
                    type = activityName,
                    description = notes
                ),
                trackHeat = trackHeat,
                checkPregnancy = checkPregnancy,
                pregnancyConfirmed = extra["pregnancyConfirmed"] as? Boolean ?: false,
                details = extra
            )
        },
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionActivitiesContent(
    pigs: List<Pig>,
    isLoading: Boolean,
    onLogActivity: (List<String>, String, String, String, Boolean, Boolean, Map<String, Any>) -> Unit,
    onBack: () -> Unit
) {
    val showLogDialogState = remember { mutableStateOf<ProductionActivityType?>(null) }
    val showLogDialog = showLogDialogState.value

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
                        text = stringResource("herd_activities"),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.width(48.dp))
                }
                StylishDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = stringResource("choose_activity_to_execute"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val categories = listOf(
                ProductionActivityType("Heat Detection", iconResId = R.drawable.ic_heat, description = stringResource("heat_detection_desc")),
                ProductionActivityType("Breeding/Mating", iconResId = R.drawable.ic_breeding, description = stringResource("breeding_mating_desc")),
                ProductionActivityType("Confirm Pregnancy", iconResId = R.drawable.ic_pregnancy_check, description = stringResource("confirm_pregnancy_desc")),
                ProductionActivityType("Farrowing", iconResId = R.drawable.ic_farrowing, description = stringResource("farrowing_desc")),
                ProductionActivityType("Weaning", iconResId = R.drawable.ic_weaning, description = stringResource("weaning_desc")),
                ProductionActivityType("Castration", iconResId = R.drawable.ic_castration, description = stringResource("castration_desc")),
                ProductionActivityType("Teeth Clipping", iconResId = R.drawable.ic_teeth_clipping, description = stringResource("teeth_clipping_desc")),
                ProductionActivityType("Tail Docking", iconResId = R.drawable.ic_tail_docking, description = stringResource("tail_docking_desc")),
                ProductionActivityType("Deworming", iconResId = R.drawable.ic_deworming, description = stringResource("deworming_desc")),
                ProductionActivityType("Iron Injection", iconResId = R.drawable.ic_iron, description = stringResource("iron_injection_desc")),
                ProductionActivityType("Vaccination", iconResId = R.drawable.ic_vaccination, description = stringResource("vaccination_desc")),
                ProductionActivityType("Medication", iconResId = R.drawable.ic_medication, description = stringResource("medication_desc")),
                ProductionActivityType("Weight Check", iconResId = R.drawable.ic_weight_checker, description = stringResource("weight_check_desc")),
                ProductionActivityType("Culling", iconResId = R.drawable.ic_culling, description = stringResource("culling_desc")),
                ProductionActivityType("Custom", icon = Icons.AutoMirrored.Filled.NoteAdd, description = stringResource("custom_activity_desc"))
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
            ) {
                items(categories) { activity ->
                    ActivityCard(activity) {
                        showLogDialogState.value = activity
                    }
                }
                item { Spacer(modifier = Modifier.height(120.dp)) }
            }
        }
    }

    showLogDialog?.let { activity ->
        LogActivityDialog(
            activityType = activity,
            pigs = pigs,
            onDismiss = { showLogDialogState.value = null },
            onLog = { pigIds, details ->
                onLogActivity(
                    pigIds,
                    activity.name,
                    details["notes"]?.toString() ?: "",
                    details["date"]?.toString() ?: "",
                    details["trackHeat"] == true,
                    details["checkPregnancy"] == true,
                    details
                )
                showLogDialogState.value = null
            }
        )
    }
}

@Composable
fun ActivityCard(
    activity: ProductionActivityType,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (activity.iconResId != null) {
                        Icon(
                            painter = painterResource(id = activity.iconResId),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    } else if (activity.icon != null) {
                        Icon(
                            imageVector = activity.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = getTranslatedActivityType(activity.name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = activity.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogActivityDialog(
    activityType: ProductionActivityType,
    pigs: List<Pig>,
    onDismiss: () -> Unit,
    onLog: (List<String>, Map<String, Any>) -> Unit
) {
    val selectedPigsState = remember { mutableStateOf(setOf<Pig>()) }
    val selectedSowState = remember { mutableStateOf<Pig?>(null) }
    val selectedBoarState = remember { mutableStateOf<Pig?>(null) }
    val expandedState = remember { mutableStateOf(false) }
    val sowExpandedState = remember { mutableStateOf(false) }
    val boarExpandedState = remember { mutableStateOf(false) }
    val notesState = remember { mutableStateOf("") }
    val trackHeatState = remember { mutableStateOf(false) }
    val checkPregnancyState = remember { mutableStateOf(false) }
    val pregnancyConfirmedState = remember { mutableStateOf(false) }
    val numMalesState = remember { mutableStateOf("") }
    val numFemalesState = remember { mutableStateOf("") }
    val maleTagsState = remember { mutableStateOf("") }
    val femaleTagsState = remember { mutableStateOf("") }
    val medicationNameState = remember { mutableStateOf("") }
    val medicationDosageState = remember { mutableStateOf("") }
    val customActivityNameState = remember { mutableStateOf("") }
    val scheduleSecondIronState = remember { mutableStateOf(false) }
    val showAllPigsForIronState = remember { mutableStateOf(false) }
    val pigLocationsState = remember { mutableStateOf(mapOf<String, String>()) }
    val pigWeightsState = remember { mutableStateOf(mapOf<String, String>()) }
    val cullingReasonState = remember { mutableStateOf("") }
    val salePriceState = remember { mutableStateOf("") }
    val showDatePickerState = remember { mutableStateOf(false) }
    val languageCode = LocalAppLanguage.current.code

    val appLanguage = LocalAppLanguage.current
    val locale = remember(appLanguage) { appLanguage.toLocale() }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )
    val formattedDate = remember(datePickerState.selectedDateMillis, locale) {
        datePickerState.selectedDateMillis?.let {
            DateUtils.formatDateToDisplay(it, locale)
        } ?: DateUtils.getCurrentDateDisplay(locale)
    }

    if (showDatePickerState.value) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerState.value = false },
            confirmButton = {
                TextButton(onClick = { showDatePickerState.value = false }) {
                    Text(stringResource("ok"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerState.value = false }) {
                    Text(stringResource("cancel"))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = { },
        properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = true),
        title = { Text(stringResource("log_activity_title", getTranslatedActivityType(activityType.name))) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                val filteredPigs = remember(activityType.name, pigs, showAllPigsForIronState.value) {
                    val baseList = when (activityType.name) {
                        "Heat Detection" -> {
                            pigs.filter { 
                                it.gender.equals("Female", ignoreCase = true) && 
                                (it.status == "Gilt" || it.status == "Sow" || it.status == "Pregnant" || it.status == "Lactating" || it.status == "Nursing" || it.status == "Finisher")
                            }
                        }
                        "Breeding/Mating" -> emptyList() // Handled by Sow/Boar selection
                        "Confirm Pregnancy" -> {
                            pigs.filter { 
                                it.gender.equals("Female", ignoreCase = true) && 
                                (it.status == "Gilt" || it.status == "Sow" || it.status == "Pregnant" || it.status == "Lactating" || it.status == "Nursing" || it.status == "Finisher") &&
                                it.lastBreedingDate.isNotEmpty()
                            }
                        }
                        "Farrowing" -> {
                            pigs.filter { 
                                it.gender.equals("Female", ignoreCase = true) && 
                                it.status.equals("Pregnant", ignoreCase = true)
                            }
                        }
                        "Weaning" -> {
                            pigs.filter { 
                                !it.weaned && it.status != "Sow" && it.status != "Boar" // Target piglets/weaners
                            }
                        }
                        "Castration" -> {
                            pigs.filter { 
                                it.gender.equals("Male", ignoreCase = true) && 
                                (it.castrated == false || it.castrated == null) &&
                                DateUtils.calculateAgeMonths(it.birthDate) < 3
                            }
                        }
                        "Teeth Clipping" -> {
                            pigs.filter { 
                                !it.teethClipped && DateUtils.calculateAgeMonths(it.birthDate) < 2
                            }
                        }
                        "Tail Docking" -> {
                            pigs.filter { 
                                !it.tailDocked && DateUtils.calculateAgeMonths(it.birthDate) < 2
                            }
                        }
                        "Iron Injection" -> {
                            if (showAllPigsForIronState.value) {
                                pigs
                            } else {
                                pigs.filter { DateUtils.calculateAgeMonths(it.birthDate) < 2 }
                            }
                        }
                        "Deworming", "Vaccination", "Medication", "Weight Check", "Culling", "Custom" -> pigs
                        else -> pigs
                    }
                    baseList.sortedBy { it.tagNumber }
                }

                OutlinedTextField(
                    value = formattedDate,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(if (activityType.name == "Farrowing") stringResource("actual_farrowing_date") else stringResource("activity_date")) },
                    trailingIcon = {
                        IconButton(onClick = { showDatePickerState.value = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = stringResource("select_date"))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (activityType.name == "Custom") {
                    OutlinedTextField(
                        value = customActivityNameState.value,
                        onValueChange = { customActivityNameState.value = it },
                        label = { Text(stringResource("activity_name")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (activityType.name == "Farrowing" && selectedPigsState.value.isNotEmpty()) {
                    val sow = selectedPigsState.value.first()
                    val expectedDate = remember(sow.lastBreedingDate, locale) {
                        if (sow.lastBreedingDate.isNotEmpty()) {
                            DateUtils.addDaysToDate(sow.lastBreedingDate, 114, locale)
                        } else "N/A"
                    }
                    Text(
                        text = stringResource("expected_farrowing_date", expectedDate),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = numMalesState.value,
                            onValueChange = { numMalesState.value = it },
                            label = { Text(stringResource("male_piglets")) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = numFemalesState.value,
                            onValueChange = { numFemalesState.value = it },
                            label = { Text(stringResource("female_piglets")) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                        )
                    }

                    if ((numMalesState.value.toIntOrNull() ?: 0) > 0) {
                        OutlinedTextField(
                            value = maleTagsState.value,
                            onValueChange = { maleTagsState.value = it },
                            label = { Text(stringResource("male_tag_numbers")) },
                            placeholder = { Text(stringResource("tag_help_text")) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if ((numFemalesState.value.toIntOrNull() ?: 0) > 0) {
                        OutlinedTextField(
                            value = femaleTagsState.value,
                            onValueChange = { femaleTagsState.value = it },
                            label = { Text(stringResource("female_tag_numbers")) },
                            placeholder = { Text(stringResource("tag_help_text")) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (activityType.name == "Breeding/Mating") {
                    // Sow Selection
                    ExposedDropdownMenuBox(
                        expanded = sowExpandedState.value,
                        onExpandedChange = { sowExpandedState.value = !sowExpandedState.value }
                    ) {
                        OutlinedTextField(
                            value = selectedSowState.value?.tagNumber ?: stringResource("select_sow"),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource("sow_tag_help")) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sowExpandedState.value) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = sowExpandedState.value,
                            onDismissRequest = { sowExpandedState.value = false }
                        ) {
                            pigs.filter { it.gender.equals("Female", ignoreCase = true) && (it.status == "Sow" || it.status == "Gilt" || it.status == "Pregnant" || it.status == "Lactating" || it.status == "Nursing" || it.status == "Finisher") }
                                .sortedBy { it.tagNumber }
                                .forEach { pig ->
                                    DropdownMenuItem(
                                        text = { Text("${pig.tagNumber} (${pig.status})") },
                                        onClick = {
                                            selectedSowState.value = pig
                                            sowExpandedState.value = false
                                        }
                                    )
                                }
                        }
                    }

                    // Boar Selection
                    ExposedDropdownMenuBox(
                        expanded = boarExpandedState.value,
                        onExpandedChange = { boarExpandedState.value = !boarExpandedState.value }
                    ) {
                        OutlinedTextField(
                            value = selectedBoarState.value?.tagNumber ?: stringResource("select_boar"),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource("boar_tag_label")) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = boarExpandedState.value) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = boarExpandedState.value,
                            onDismissRequest = { boarExpandedState.value = false }
                        ) {
                            pigs.filter { 
                                it.gender.equals("Male", ignoreCase = true) && 
                                it.status == "Boar"
                            }
                                .sortedBy { it.tagNumber }
                                .forEach { pig ->
                                    DropdownMenuItem(
                                        text = { Text("${pig.tagNumber} (${pig.status})") },
                                        onClick = {
                                            selectedBoarState.value = pig
                                            boarExpandedState.value = false
                                        }
                                    )
                                }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = checkPregnancyState.value,
                            onCheckedChange = { checkPregnancyState.value = it }
                        )
                        Text(
                            text = stringResource("schedule_preg_check"),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    ExposedDropdownMenuBox(
                        expanded = expandedState.value,
                        onExpandedChange = { expandedState.value = !expandedState.value }
                    ) {
                        val selectionText = when {
                            selectedPigsState.value.isEmpty() -> stringResource("select_pigs")
                            selectedPigsState.value.size == 1 -> selectedPigsState.value.first().tagNumber
                            else -> stringResource("pigs_selected", selectedPigsState.value.size)
                        }
                        OutlinedTextField(
                            value = selectionText,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource("target_pigs")) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedState.value) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedState.value,
                            onDismissRequest = { expandedState.value = false }
                        ) {
                            filteredPigs.forEach { pig ->
                                val isSelected = selectedPigsState.value.contains(pig)
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(checked = isSelected, onCheckedChange = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("${pig.tagNumber} (${pig.status})")
                                        }
                                    },
                                    onClick = {
                                        selectedPigsState.value = if (isSelected) {
                                            selectedPigsState.value - pig
                                        } else {
                                            if (activityType.name == "Farrowing") {
                                                setOf(pig) // Litter details are specific to one sow
                                            } else {
                                                selectedPigsState.value + pig
                                            }
                                        }
                                    }
                                )
                            }
                            if (activityType.name == "Iron Injection" && !showAllPigsForIronState.value) {
                                DropdownMenuItem(
                                    text = { Text(stringResource("more"), color = MaterialTheme.colorScheme.primary) },
                                    onClick = { 
                                        showAllPigsForIronState.value = true
                                        expandedState.value = true
                                    }
                                )
                            }
                        }
                    }
                }

                if (activityType.name == "Weaning" && selectedPigsState.value.isNotEmpty()) {
                    Text(stringResource("enter_new_locations"), style = MaterialTheme.typography.labelLarge)
                    Column(modifier = Modifier.heightIn(max = 200.dp).verticalScroll(rememberScrollState())) {
                        selectedPigsState.value.forEach { pig ->
                            OutlinedTextField(
                                value = pigLocationsState.value[pig.id] ?: "",
                                onValueChange = { loc -> 
                                    pigLocationsState.value += (pig.id to loc)
                                },
                                label = { Text(stringResource("location_for", pig.tagNumber)) },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            )
                        }
                    }
                }

                if (activityType.name == "Weight Check" && selectedPigsState.value.isNotEmpty()) {
                    Text(stringResource("enter_weights"), style = MaterialTheme.typography.labelLarge)
                    Column(modifier = Modifier.heightIn(max = 200.dp).verticalScroll(rememberScrollState())) {
                        selectedPigsState.value.forEach { pig ->
                            OutlinedTextField(
                                value = pigWeightsState.value[pig.id] ?: "",
                                onValueChange = { weight -> 
                                    pigWeightsState.value += (pig.id to weight)
                                },
                                label = { Text(stringResource("weight_for", pig.tagNumber)) },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                            )
                        }
                    }
                }

                if (activityType.name == "Deworming" || activityType.name == "Iron Injection" || activityType.name == "Medication" || activityType.name == "Vaccination") {
                    OutlinedTextField(
                        value = medicationNameState.value,
                        onValueChange = { medicationNameState.value = it },
                        label = { Text(if (activityType.name == "Vaccination") stringResource("vaccine_name") else stringResource("medication_name")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = medicationDosageState.value,
                        onValueChange = { medicationDosageState.value = it },
                        label = { Text(stringResource("dosage")) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (activityType.name == "Iron Injection" && selectedPigsState.value.all { it.ironInjections == 0 }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = scheduleSecondIronState.value,
                                onCheckedChange = { scheduleSecondIronState.value = it }
                            )
                            Text(stringResource("schedule_second_iron"))
                        }
                    }
                }

                if (activityType.name == "Culling" && selectedPigsState.value.isNotEmpty()) {
                    Text(stringResource("culling_details"), style = MaterialTheme.typography.labelLarge)
                    
                    val reasons = listOf(
                        stringResource("reason_natural"),
                        stringResource("reason_disease"),
                        stringResource("reason_sold")
                    )
                    val reasonExpandedState = remember { mutableStateOf(false) }
                    
                    ExposedDropdownMenuBox(
                        expanded = reasonExpandedState.value,
                        onExpandedChange = { reasonExpandedState.value = !reasonExpandedState.value }
                    ) {
                        OutlinedTextField(
                            value = cullingReasonState.value,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource("reason")) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reasonExpandedState.value) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = reasonExpandedState.value,
                            onDismissRequest = { reasonExpandedState.value = false }
                        ) {
                            reasons.forEach { reason ->
                                DropdownMenuItem(
                                    text = { Text(reason) },
                                    onClick = {
                                        cullingReasonState.value = reason
                                        reasonExpandedState.value = false
                                    }
                                )
                            }
                        }
                    }

                    if (cullingReasonState.value == stringResource("reason_sold")) {
                        OutlinedTextField(
                            value = salePriceState.value,
                            onValueChange = { salePriceState.value = it },
                            label = { Text(stringResource("total_sale_price", "Ksh")) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                        )
                    }
                }

                if (activityType.name == "Confirm Pregnancy") {
                    if (DateUtils.isFutureDate(formattedDate, locale)) {
                        Text(
                            text = stringResource("scheduling_preg_check_for", formattedDate),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(stringResource("pregnancy_result"), style = MaterialTheme.typography.labelLarge, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = pregnancyConfirmedState.value, onClick = { pregnancyConfirmedState.value = true })
                                Text(stringResource("successful"))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = !pregnancyConfirmedState.value, onClick = { pregnancyConfirmedState.value = false })
                                Text(stringResource("failed"))
                            }
                        }
                    }
                }

                if (activityType.name == "Heat Detection") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = trackHeatState.value,
                            onCheckedChange = { trackHeatState.value = it }
                        )
                        Text(
                            text = stringResource("track_heat_remind"),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                OutlinedTextField(
                    value = notesState.value,
                    onValueChange = { notesState.value = it },
                    label = { Text(stringResource("notes_details")) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text(stringResource("cancel")) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val pigIds = if (activityType.name == "Breeding/Mating") {
                                listOfNotNull(selectedSowState.value?.id, selectedBoarState.value?.id)
                            } else {
                                selectedPigsState.value.map { it.id }
                            }

                            if (pigIds.isNotEmpty()) {
                                // For descriptions generated here, we'll use stringResource logic similar to PigProfileScreen
                                val finalNotes = StringBuilder(notesState.value)
                                when (activityType.name) {
                                    "Breeding/Mating" -> {
                                        val sowTag = selectedSowState.value?.tagNumber ?: ""
                                        val boarTag = selectedBoarState.value?.tagNumber ?: ""
                                        if (sowTag.isNotEmpty() || boarTag.isNotEmpty()) {
                                            if (finalNotes.isNotEmpty()) finalNotes.append("\n")
                                            finalNotes.append(Translator.getString("mated_sow_with_boar", languageCode, sowTag, boarTag))
                                        }
                                    }
                                    "Farrowing" -> {
                                        val numM = numMalesState.value
                                        val numF = numFemalesState.value
                                        if (numM.isNotEmpty() || numF.isNotEmpty()) {
                                            if (finalNotes.isNotEmpty()) finalNotes.append("\n")
                                            finalNotes.append(Translator.getString("farrowed_males_females", languageCode, numM, numF))
                                        }
                                    }
                                    "Deworming", "Vaccination", "Medication", "Iron Injection" -> {
                                        if (medicationNameState.value.isNotEmpty()) {
                                            if (finalNotes.isNotEmpty()) finalNotes.append("\n")
                                            finalNotes.append(Translator.getString("medication_vaccine_dosage", languageCode, medicationNameState.value, medicationDosageState.value))
                                        }
                                    }
                                    "Weight Check" -> {
                                        // Handle weights map in ViewModel or locally if needed. 
                                        // For simplicity, we just pass the notes. 
                                        // But if we want to add to notes here:
                                        if (pigWeightsState.value.isNotEmpty() && pigIds.size == 1) {
                                            val w = pigWeightsState.value[pigIds[0]] ?: ""
                                            if (w.isNotEmpty()) {
                                                if (finalNotes.isNotEmpty()) finalNotes.append("\n")
                                                finalNotes.append(Translator.getString("weight_updated_to", languageCode, w))
                                            }
                                        }
                                    }
                                    "Confirm Pregnancy" -> {
                                        if (pregnancyConfirmedState.value) {
                                            if (finalNotes.isNotEmpty()) finalNotes.append("\n")
                                            finalNotes.append(Translator.getString("pregnancy_confirmed", languageCode))
                                        } else {
                                            if (finalNotes.isNotEmpty()) finalNotes.append("\n")
                                            finalNotes.append(Translator.getString("pregnancy_check_failed", languageCode))
                                        }
                                    }
                                    "Castration" -> {
                                        if (finalNotes.isNotEmpty()) finalNotes.append("\n")
                                        finalNotes.append(Translator.getString("castrated_successfully", languageCode))
                                    }
                                }

                                onLog(
                                    pigIds,
                                    mapOf(
                                        "notes" to finalNotes.toString().trim(),
                                        "date" to formattedDate,
                                        "trackHeat" to trackHeatState.value,
                                        "checkPregnancy" to checkPregnancyState.value,
                                        "pregnancyConfirmed" to pregnancyConfirmedState.value,
                                        "numMales" to (numMalesState.value.toIntOrNull() ?: 0),
                                        "numFemales" to (numFemalesState.value.toIntOrNull() ?: 0),
                                        "maleTags" to maleTagsState.value,
                                        "femaleTags" to femaleTagsState.value,
                                        "sowTag" to (selectedSowState.value?.tagNumber ?: ""),
                                        "boarTag" to (selectedBoarState.value?.tagNumber ?: ""),
                                        "medicationName" to medicationNameState.value,
                                        "medicationDosage" to medicationDosageState.value,
                                        "scheduleSecondIron" to scheduleSecondIronState.value,
                                        "pigLocations" to pigLocationsState.value,
                                        "pigWeights" to pigWeightsState.value,
                                        "cullingReason" to cullingReasonState.value,
                                        "salePrice" to (salePriceState.value.toDoubleOrNull() ?: 0.0),
                                        "customActivityName" to customActivityNameState.value
                                    )
                                )
                            }
                        },
                        enabled = when (activityType.name) {
                            "Breeding/Mating" -> selectedSowState.value != null && selectedBoarState.value != null
                            "Custom" -> selectedPigsState.value.isNotEmpty() && customActivityNameState.value.isNotBlank()
                            else -> selectedPigsState.value.isNotEmpty()
                        }
                    ) {
                        Text(stringResource("save_activity"))
                    }
                }
                
                Spacer(modifier = Modifier.height(100.dp))
            }
        },
        confirmButton = { },
        dismissButton = { }
    )
}

@Preview(showBackground = true)
@Composable
fun ProductionActivitiesScreenPreview() {
    SmartSwineTheme {
        ProductionActivitiesContent(
            pigs = listOf(
                Pig(id = "1", tagNumber = "P001", status = "Healthy"),
                Pig(id = "2", tagNumber = "P002", status = "Sow")
            ),
            isLoading = false,
            onLogActivity = { _, _, _, _, _, _, _ -> },
            onBack = {}
        )
    }
}

data class ProductionActivityType(
    val name: String,
    val iconResId: Int? = null,
    val icon: ImageVector? = null,
    val description: String
)
