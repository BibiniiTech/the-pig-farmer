package com.example.smartswine.ui.production

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.smartswine.utils.StylishDivider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import java.util.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bibiniitech.smartswine.R
import com.example.smartswine.model.Pig
import com.example.smartswine.ui.herd.HerdViewModel
import com.example.smartswine.ui.theme.SmartSwineTheme
import com.example.smartswine.ui.theme.DarkBackground
import com.example.smartswine.utils.DateUtils
import com.example.smartswine.utils.LocalAppLanguage
import com.example.smartswine.utils.Translator
import com.example.smartswine.utils.getTranslatedActivityType
import com.example.smartswine.utils.stringResource

@Composable
fun ProductionActivitiesScreen(
    viewModel: ProductionViewModel,
    herdViewModel: HerdViewModel,
    initialActivity: String? = null,
    initialPigId: String? = null,
    onBack: () -> Unit,
) {
    val pigs by herdViewModel.pigs.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    ProductionActivitiesContent(
        pigs = pigs,
        isLoading = isLoading,
        initialActivity = initialActivity,
        initialPigId = initialPigId,
        onLogActivity = { pigIds, activityName, notes, date, trackHeat, checkPregnancy, extra ->
            val wDays = (extra["withdrawalDays"] as? Int) ?: 0
            val safeDate = if (wDays > 0) DateUtils.addDaysToDate(date, wDays) else ""
            viewModel.logHealthActivity(
                pigIds = pigIds,
                record = com.example.smartswine.model.HealthRecord(
                    date = date,
                    type = activityName,
                    description = notes,
                    medication = (extra["medicationName"] as? String) ?: "",
                    dosage = (extra["medicationDosage"] as? String) ?: "",
                    stillbornCount = (extra["stillborns"] as? Int) ?: 0,
                    mummiesCount = (extra["mummies"] as? Int) ?: 0,
                    litterBirthWeightKg = (extra["litterBirthWeight"] as? Double) ?: 0.0,
                    withdrawalPeriodDays = wDays,
                    safeSlaughterDate = safeDate
                ),
                trackHeat = trackHeat,
                checkPregnancy = checkPregnancy,
                pregnancyConfirmed = (extra["pregnancyConfirmed"] as? Boolean) ?: false,
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
    initialActivity: String? = null,
    initialPigId: String? = null,
    onLogActivity: (List<String>, String, String, String, Boolean, Boolean, Map<String, Any>) -> Unit,
    onBack: () -> Unit,
) {
    val categories = remember {
        listOf(
            ProductionActivityType("Heat Detection", iconResId = R.drawable.ic_heat, description = "Heat Detection"),
            ProductionActivityType("Breeding/Mating", iconResId = R.drawable.ic_breeding, description = "Breeding/Mating"),
            ProductionActivityType("Confirm Pregnancy", iconResId = R.drawable.ic_pregnancy_check, description = "Confirm Pregnancy"),
            ProductionActivityType("Farrowing", iconResId = R.drawable.ic_farrowing, description = "Farrowing"),
            ProductionActivityType("Weaning", iconResId = R.drawable.ic_weaning, description = "Weaning"),
            ProductionActivityType("Castration", iconResId = R.drawable.ic_castration, description = "Castration"),
            ProductionActivityType("Teeth Clipping", iconResId = R.drawable.ic_teeth_clipping, description = "Teeth Clipping"),
            ProductionActivityType("Tail Docking", iconResId = R.drawable.ic_tail_docking, description = "Tail Docking"),
            ProductionActivityType("Deworming", iconResId = R.drawable.ic_deworming, description = "Deworming"),
            ProductionActivityType("Iron Injection", iconResId = R.drawable.ic_iron, description = "Iron Injection"),
            ProductionActivityType("Vaccination", iconResId = R.drawable.ic_vaccination, description = "Vaccination"),
            ProductionActivityType("Medication", iconResId = R.drawable.ic_medication, description = "Medication"),
            ProductionActivityType("Culling", iconResId = R.drawable.ic_culling, description = "Culling"),
            ProductionActivityType("Custom", icon = Icons.AutoMirrored.Filled.NoteAdd, description = "Custom Activity")
        )
    }

    val targetActivity = remember(initialActivity) {
        if (initialActivity.isNullOrBlank()) null
        else categories.find { it.name.equals(initialActivity, ignoreCase = true) }
            ?: categories.find { 
                val act = initialActivity.lowercase()
                val cat = it.name.lowercase()
                (act.contains("heat") && cat.contains("heat")) ||
                ((act.contains("breeding") || act.contains("mating")) && cat.contains("breeding")) ||
                ((act.contains("pregnancy") || act.contains("confirm")) && cat.contains("pregnancy")) ||
                (act.contains("farrowing") && cat.contains("farrowing")) ||
                (act.contains("weaning") && cat.contains("weaning")) ||
                (act.contains("castration") && cat.contains("castration")) ||
                (act.contains("teeth") && cat.contains("teeth")) ||
                (act.contains("tail") && cat.contains("tail")) ||
                (act.contains("deworming") && cat.contains("deworming")) ||
                (act.contains("iron") && cat.contains("iron")) ||
                (act.contains("vaccin") && cat.contains("vaccin")) ||
                (act.contains("medication") && cat.contains("medication")) ||
                (act.contains("weight") && cat.contains("weight")) ||
                (act.contains("culling") && cat.contains("culling"))
            }
    }

    var activeDialogActivity by remember(targetActivity) { mutableStateOf(targetActivity) }

    if (activeDialogActivity != null) {
        LogActivityDialog(
            activityType = activeDialogActivity!!,
            pigs = pigs,
            initialPigId = initialPigId,
            onDismiss = {
                if (initialActivity != null) {
                    onBack()
                } else {
                    activeDialogActivity = null
                }
            },
            onLog = { pigIds, details ->
                onLogActivity(
                    pigIds,
                    activeDialogActivity!!.name,
                    details["notes"]?.toString() ?: "",
                    details["date"]?.toString() ?: "",
                    details["trackHeat"] == true,
                    details["checkPregnancy"] == true,
                    details
                )
                onBack()
            }
        )
    }

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

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
            ) {
                items(categories) { activity ->
                    ActivityCard(activity = activity) {
                        activeDialogActivity = activity
                    }
                }
            }
        }
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
                    } else activity.icon?.let {
                        Icon(
                            imageVector = it,
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
    initialPigId: String? = null,
    onDismiss: () -> Unit,
    onLog: (List<String>, Map<String, Any>) -> Unit
) {
    val cleanInitialPigId = remember(initialPigId) {
        initialPigId?.replace(Regex("(?i)^tag:?\\s*"), "")?.trim()
    }
    val initialPig = remember(cleanInitialPigId, initialPigId, pigs) {
        if (!cleanInitialPigId.isNullOrBlank()) {
            pigs.find { 
                it.id == cleanInitialPigId || 
                it.tagNumber.equals(cleanInitialPigId, ignoreCase = true) ||
                it.id == initialPigId ||
                it.tagNumber.equals(initialPigId, ignoreCase = true)
            }
        } else null
    }
    val selectedPigsState = remember(initialPig) {
        mutableStateOf(if (initialPig != null) setOf(initialPig) else emptySet())
    }
    val selectedSowState = remember(initialPig) {
        mutableStateOf(if (initialPig?.gender?.equals("female", ignoreCase = true) == true) initialPig else null)
    }
    val selectedBoarState = remember(initialPig) {
        mutableStateOf(if (initialPig?.gender?.equals("male", ignoreCase = true) == true) initialPig else null)
    }
    val expandedState = remember { mutableStateOf(value = false) }
    val sowExpandedState = remember { mutableStateOf(false) }
    val boarExpandedState = remember { mutableStateOf(false) }
    val isAiOrBorrowedBoarState = remember { mutableStateOf(false) }
    val customBoarTagState = remember { mutableStateOf("") }
    val notesState = remember { mutableStateOf("") }
    val trackHeatState = remember { mutableStateOf(false) }
    val checkPregnancyState = remember { mutableStateOf(false) }
    val pregnancyConfirmedState = remember { mutableStateOf(false) }
    val numMalesState = remember { mutableStateOf("") }
    val numFemalesState = remember { mutableStateOf("") }
    val maleTagsState = remember { mutableStateOf("") }
    val femaleTagsState = remember { mutableStateOf("") }
    val stillbornsState = remember { mutableStateOf("") }
    val mummiesState = remember { mutableStateOf("") }
    val litterBirthWeightState = remember { mutableStateOf("") }
    val withdrawalDaysState = remember { mutableStateOf("") }
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
    val currencySymbol = remember { com.example.smartswine.ui.settings.SettingsViewModel.getInstance().currencySymbol.value }

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

    val isDark = MaterialTheme.colorScheme.background == DarkBackground
    val dialogContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color(0xFFF0FDFE)
    val dialogTitleColor = if (isDark) Color(0xFF4DD0E1) else Color(0xFF006064)
    val dialogTextColor = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF004D40)

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = true),
        containerColor = dialogContainerColor,
        titleContentColor = dialogTitleColor,
        textContentColor = dialogTextColor,
        title = {
            Text(
                stringResource("log_activity_title", getTranslatedActivityType(activityType.name)),
                color = dialogTitleColor,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                val filteredPigs = remember(activityType.name, pigs, showAllPigsForIronState.value, initialPig) {
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
                    val withInitial = if (initialPig != null && activityType.name != "Breeding/Mating" && !baseList.any { it.id == initialPig.id }) {
                        baseList + initialPig
                    } else {
                        baseList
                    }
                    withInitial.sortedBy { it.tagNumber }
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

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = stillbornsState.value,
                            onValueChange = { stillbornsState.value = it },
                            label = { Text(stringResource("stillborns_dead")) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = mummiesState.value,
                            onValueChange = { mummiesState.value = it },
                            label = { Text(stringResource("mummies")) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                        )
                    }

                    OutlinedTextField(
                        value = litterBirthWeightState.value,
                        onValueChange = { litterBirthWeightState.value = it },
                        label = { Text(stringResource("total_litter_birth_weight_kg")) },
                        placeholder = { Text(stringResource("eg_weight_sample")) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                    )

                    // Litter Quality & Viability Summary Card
                    val males = numMalesState.value.toIntOrNull() ?: 0
                    val females = numFemalesState.value.toIntOrNull() ?: 0
                    val still = stillbornsState.value.toIntOrNull() ?: 0
                    val mum = mummiesState.value.toIntOrNull() ?: 0
                    val bornAlive = males + females
                    val totalBorn = bornAlive + still + mum
                    val totalWt = litterBirthWeightState.value.toDoubleOrNull() ?: 0.0

                    if (totalBorn > 0) {
                        val summaryCardBg = if (isDark) Color(0xFF00363A) else Color(0xFFE0F2F1)
                        val summaryCardBorder = if (isDark) Color(0xFF00695C) else Color(0xFF80CBC4)
                        val summaryTitleColor = if (isDark) Color(0xFF80DEEA) else Color(0xFF004D40)
                        val summaryTextColor = if (isDark) Color(0xFF4DD0E1) else Color(0xFF00695C)
                        val summaryBodyColor = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF004D40)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = summaryCardBg),
                            border = BorderStroke(1.dp, summaryCardBorder)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(stringResource("litter_performance_summary"), fontWeight = FontWeight.Bold, color = summaryTitleColor, style = MaterialTheme.typography.titleSmall)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${stringResource("born_alive")}: $bornAlive", color = summaryTextColor)
                                    Text("${stringResource("total_born")}: $totalBorn", fontWeight = FontWeight.SemiBold, color = summaryTitleColor)
                                }
                                if (totalWt > 0.0 && bornAlive > 0) {
                                    val avgWt = totalWt / bornAlive
                                    val avgStatus = if (avgWt >= 1.3) stringResource("good_status") else if (avgWt >= 1.0) stringResource("fair_status") else stringResource("low_viability_status")
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${stringResource("avg_birth_weight")}: ${String.format(Locale.getDefault(), "%.2f", avgWt)} kg", color = summaryBodyColor)
                                        Text(avgStatus, fontWeight = FontWeight.Bold, color = if (avgWt < 1.0) (if (isDark) Color(0xFFEF5350) else Color(0xFFC62828)) else (if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)))
                                    }
                                }
                            }
                        }
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
                            label = { Text(stringResource("sow_tag_help") + " *") },
                            isError = selectedSowState.value == null,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sowExpandedState.value) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = sowExpandedState.value,
                            onDismissRequest = { sowExpandedState.value = false }
                        ) {
                            pigs.asSequence()
                                .filter { (it.gender.equals("Female", ignoreCase = true) && (it.status == "Sow" || it.status == "Gilt" || it.status == "Pregnant" || it.status == "Lactating" || it.status == "Nursing" || it.status == "Finisher")) || (initialPig != null && it.id == initialPig.id && it.gender.equals("Female", ignoreCase = true)) }
                                .sortedBy { it.tagNumber }
                                .forEach { pig ->
                                    DropdownMenuItem(
                                        text = { Text("${pig.tagNumber} (${stringResource(pig.status.lowercase().replace(" ", "_"))})") },
                                        onClick = {
                                            selectedSowState.value = pig
                                            sowExpandedState.value = false
                                        }
                                    )
                                }
                        }
                    }

                    if (selectedSowState.value == null) {
                        Text(
                            text = "* Sow tag is required to log breeding",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }

                    // Boar Selection (Optional - AI / Borrowed Boar support)
                    ExposedDropdownMenuBox(
                        expanded = boarExpandedState.value,
                        onExpandedChange = { boarExpandedState.value = !boarExpandedState.value }
                    ) {
                        val boarDisplay = when {
                            selectedBoarState.value != null -> selectedBoarState.value!!.tagNumber
                            isAiOrBorrowedBoarState.value && customBoarTagState.value.isNotBlank() -> "${stringResource("ai_borrowed_prefix")}: ${customBoarTagState.value}"
                            isAiOrBorrowedBoarState.value -> stringResource("ai_external_boar")
                            else -> stringResource("select_boar_optional")
                        }
                        OutlinedTextField(
                            value = boarDisplay,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource("boar_tag_optional_ai")) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = boarExpandedState.value) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = boarExpandedState.value,
                            onDismissRequest = { boarExpandedState.value = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource("ai_external_boar_dropdown")) },
                                onClick = {
                                    selectedBoarState.value = null
                                    isAiOrBorrowedBoarState.value = true
                                    boarExpandedState.value = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource("none_not_specified")) },
                                onClick = {
                                    selectedBoarState.value = null
                                    isAiOrBorrowedBoarState.value = false
                                    customBoarTagState.value = ""
                                    boarExpandedState.value = false
                                }
                            )
                            HorizontalDivider()
                            val availableBoars = pigs.filter { 
                                (it.gender.equals("Male", ignoreCase = true) && it.status == "Boar") || (initialPig != null && it.id == initialPig.id && it.gender.equals("Male", ignoreCase = true))
                            }.sortedBy { it.tagNumber }

                            if (availableBoars.isNotEmpty()) {
                                availableBoars.forEach { pig ->
                                    DropdownMenuItem(
                                        text = { Text("🐗 ${pig.tagNumber} (${stringResource(pig.status.lowercase().replace(" ", "_"))})") },
                                        onClick = {
                                            selectedBoarState.value = pig
                                            isAiOrBorrowedBoarState.value = false
                                            boarExpandedState.value = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (isAiOrBorrowedBoarState.value) {
                        OutlinedTextField(
                            value = customBoarTagState.value,
                            onValueChange = { customBoarTagState.value = it },
                            label = { Text(stringResource("external_boar_tag_label")) },
                            placeholder = { Text(stringResource("external_boar_tag_placeholder")) },
                            modifier = Modifier.fillMaxWidth()
                        )
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
                                            Text("${pig.tagNumber} (${stringResource(pig.status.lowercase().replace(" ", "_"))})")
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

                    if (activityType.name == "Deworming" || activityType.name == "Medication" || activityType.name == "Vaccination") {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource("meat_withdrawal_title"),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isDark) Color(0xFF4DD0E1) else Color(0xFF006064),
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = withdrawalDaysState.value,
                            onValueChange = { withdrawalDaysState.value = it },
                            label = { Text(stringResource("withdrawal_days_label")) },
                            placeholder = { Text(stringResource("zero_if_none")) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            val presets = listOf("0d" to "0", "7d" to "7", "14d" to "14", "21d" to "21", "28d" to "28")
                            presets.forEach { (label, days) ->
                                AssistChip(
                                    onClick = { withdrawalDaysState.value = days },
                                    label = { Text(label, fontSize = 11.sp) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = if (withdrawalDaysState.value == days) (if (isDark) Color(0xFF00695C) else Color(0xFF80DEEA)) else Color.Transparent
                                    )
                                )
                            }
                        }
                        val wDays = withdrawalDaysState.value.toIntOrNull() ?: 0
                        if (wDays > 0) {
                            val safeDate = DateUtils.addDaysToDate(formattedDate, wDays)
                            Text(
                                "${stringResource("safe_for_meat_on")}: $safeDate",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) Color(0xFFFF8A80) else Color(0xFFD32F2F),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

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

                    val pigsUnderWithdrawal = remember(selectedPigsState.value) {
                        selectedPigsState.value.filter { it.activeWithdrawalUntil.isNotEmpty() && DateUtils.isWithdrawalActive(it.activeWithdrawalUntil, locale) }
                    }
                    if (cullingReasonState.value == stringResource("reason_sold") && pigsUnderWithdrawal.isNotEmpty()) {
                        val warnCardBg = if (isDark) Color(0xFF3E1212) else Color(0xFFFFEBEE)
                        val warnBorder = if (isDark) Color(0xFFE57373) else Color(0xFFEF5350)
                        val warnIconColor = if (isDark) Color(0xFFFF8A80) else Color(0xFFC62828)
                        val warnTextColor = if (isDark) Color(0xFFFFCDD2) else Color(0xFFB71C1C)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = warnCardBg),
                            border = BorderStroke(1.dp, warnBorder)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = warnIconColor)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(stringResource("critical_food_safety_warning"), fontWeight = FontWeight.Black, color = warnIconColor, style = MaterialTheme.typography.labelMedium)
                                    val pigNames = pigsUnderWithdrawal.joinToString { it.tagNumber }
                                    val safeDate = pigsUnderWithdrawal.first().activeWithdrawalUntil
                                    val med = pigsUnderWithdrawal.first().withdrawalMedication
                                    Text(stringResource("meat_withdrawal_warning_msg", pigNames, med, safeDate), color = warnTextColor, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    if (cullingReasonState.value == stringResource("reason_sold")) {
                        OutlinedTextField(
                            value = salePriceState.value,
                            onValueChange = { salePriceState.value = it },
                            label = { Text(stringResource("total_sale_price", currencySymbol)) },
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
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(contentColor = if (isDark) Color(0xFF4DD0E1) else Color(0xFF00838F))
                    ) { Text(stringResource("cancel")) }
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
                                        val boarTag = when {
                                            selectedBoarState.value != null -> selectedBoarState.value!!.tagNumber
                                            isAiOrBorrowedBoarState.value && customBoarTagState.value.isNotBlank() -> "AI (${customBoarTagState.value.trim()})"
                                            isAiOrBorrowedBoarState.value -> "AI / External"
                                            else -> ""
                                        }
                                        if (sowTag.isNotEmpty()) {
                                            if (finalNotes.isNotEmpty()) finalNotes.append("\n")
                                            if (boarTag.isNotEmpty()) {
                                                finalNotes.append(Translator.getString("mated_sow_with_boar", languageCode, sowTag, boarTag))
                                            } else {
                                                finalNotes.append("Inseminated: Sow $sowTag (AI / External)")
                                            }
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
                                        if (pigWeightsState.value.isNotEmpty() && pigIds.size == 1) {
                                            val w = pigWeightsState.value[pigIds[0]] ?: ""
                                            if (w.isNotEmpty()) {
                                                if (finalNotes.isNotEmpty()) finalNotes.append("\n")
                                                finalNotes.append(Translator.getString("weight_updated_to", languageCode, w))
                                            }
                                        }
                                    }
                                    "Culling" -> {
                                        val reason = cullingReasonState.value
                                        if (reason.isNotEmpty()) {
                                            if (finalNotes.isNotEmpty()) finalNotes.append("\n")
                                            finalNotes.append(Translator.getString("culled_reason_detail", languageCode, reason))
                                        }
                                    }
                                    "Custom" -> {
                                        val name = customActivityNameState.value
                                        if (name.isNotEmpty()) {
                                            if (finalNotes.isNotEmpty()) finalNotes.append("\n")
                                            finalNotes.append(Translator.getString("custom_activity_detail", languageCode, name))
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

                                val resolvedBoarTag = when {
                                    selectedBoarState.value != null -> selectedBoarState.value!!.tagNumber
                                    isAiOrBorrowedBoarState.value && customBoarTagState.value.isNotBlank() -> customBoarTagState.value.trim()
                                    isAiOrBorrowedBoarState.value -> "AI / External"
                                    else -> ""
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
                                        "boarTag" to resolvedBoarTag,
                                        "medicationName" to medicationNameState.value,
                                        "medicationDosage" to medicationDosageState.value,
                                        "scheduleSecondIron" to scheduleSecondIronState.value,
                                        "pigLocations" to pigLocationsState.value,
                                        "pigWeights" to pigWeightsState.value,
                                        "cullingReason" to cullingReasonState.value,
                                        "salePrice" to (salePriceState.value.toDoubleOrNull() ?: 0.0),
                                        "customActivityName" to customActivityNameState.value,
                                        "stillborns" to (stillbornsState.value.toIntOrNull() ?: 0),
                                        "mummies" to (mummiesState.value.toIntOrNull() ?: 0),
                                        "litterBirthWeight" to (litterBirthWeightState.value.toDoubleOrNull() ?: 0.0),
                                        "withdrawalDays" to (withdrawalDaysState.value.toIntOrNull() ?: 0)
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00838F),
                            contentColor = Color.White
                        ),
                        enabled = when (activityType.name) {
                            "Breeding/Mating" -> selectedSowState.value != null
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
