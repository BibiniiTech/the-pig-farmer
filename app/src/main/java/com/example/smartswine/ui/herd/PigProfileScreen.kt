package com.example.smartswine.ui.herd

import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.window.DialogProperties
import com.example.smartswine.model.HealthRecord
import com.example.smartswine.model.Pig
import com.example.smartswine.ui.theme.SmartSwineTheme
import com.example.smartswine.utils.DateUtils
import com.example.smartswine.util.PdfGenerator
import com.example.smartswine.utils.LocalAppLanguage
import com.example.smartswine.utils.LocalIsPremium
import com.example.smartswine.utils.PremiumWrapper
import com.example.smartswine.utils.Translator
import com.example.smartswine.utils.stringResource
import com.example.smartswine.utils.SwineGrowthDatabase
import com.example.smartswine.utils.getTranslatedActivityType
import java.util.*

@Composable
fun PigProfileScreen(
    pigId: String,
    viewModel: HerdViewModel,
    onNavigateToPaywall: () -> Unit,
    onBack: () -> Unit,
) {
    val pig by viewModel.getPig(pigId).collectAsStateWithLifecycle(initialValue = null)
    val healthRecords by viewModel.healthRecords.collectAsStateWithLifecycle()
    val allPigs by viewModel.allPigsIncludingArchived.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.settingsViewModel.currencySymbol.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    LaunchedEffect(pigId) {
        viewModel.fetchHealthRecords(pigId)
    }

    val isPremium = LocalIsPremium.current

    Box(modifier = Modifier.fillMaxSize()) {
        PigProfileContent(
            pig = pig,
            allPigs = allPigs,
            healthRecords = healthRecords,
            currencySymbol = currencySymbol,
            isPremium = isPremium,
            onBack = onBack,
            onNavigateToPaywall = onNavigateToPaywall,
            onEditPig = { viewModel.updatePig(it) },
            onDeletePig = {
                viewModel.deletePig(it)
                onBack()
            },
            onArchivePig = { p, reason ->
                viewModel.archivePig(p, reason)
                onBack()
            },
            onAddHealthRecord = { record, heat, check, conf, extra -> viewModel.addHealthRecord(pigId, record, heat, check, conf, extra) },
            onEditHealthRecord = { record, heat, check, conf, extra -> viewModel.updateHealthRecord(pigId, record, heat, check, conf, extra) },
            onDeleteHealthRecord = { viewModel.deleteHealthRecord(pigId, it) },
        )

        error?.let { msg ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text(stringResource("dismiss"))
                    }
                }
            ) {
                Text(msg)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PigProfileContent(
    pig: Pig?,
    allPigs: List<Pig>,
    healthRecords: List<HealthRecord>,
    currencySymbol: String,
    isPremium: Boolean,
    onBack: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    onEditPig: (Pig) -> Unit,
    onDeletePig: (Pig) -> Unit,
    onArchivePig: (Pig, String) -> Unit,
    onAddHealthRecord: (HealthRecord, Boolean, Boolean, Boolean, Map<String, Any>) -> Unit = { _, _, _, _, _ -> },
    onEditHealthRecord: (HealthRecord, Boolean, Boolean, Boolean, Map<String, Any>) -> Unit = { _, _, _, _, _ -> },
    onDeleteHealthRecord: (String) -> Unit,
) {
    val showEditDialog = remember { mutableStateOf(value = false) }
    val showDeleteConfirm = remember { mutableStateOf(value = false) }
    val showDeleteRecordConfirm = remember { mutableStateOf<String?>(null) }
    val showArchiveDialog = remember { mutableStateOf(value = false) }
    val showAddHealthDialog = remember { mutableStateOf(value = false) }
    val editingRecord = remember { mutableStateOf<HealthRecord?>(null) }
    val selectedTabState = remember { mutableIntStateOf(0) }

    val context = LocalContext.current
    val currentLanguageCode = LocalAppLanguage.current.code
    val isArchived = pig?.status?.startsWith("Archived") == true

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    // Use a more stable title even when pig is null
                    Text(
                        text = pig?.tagNumber ?: stringResource("profile"),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource("back"))
                    }
                },
                actions = {
                    PremiumWrapper(isPremium = isPremium, onLockedClick = onNavigateToPaywall) {
                        IconButton(
                            enabled = pig != null,
                            onClick = { 
                                if (isPremium) {
                                    pig?.let { 
                                        PdfGenerator.generateHerdReportPdf(
                                            context = context,
                                            pigs = listOf(it),
                                            allPigs = allPigs,
                                            healthRecords = mapOf(it.id to healthRecords),
                                            reportTitle = "${Translator.getString("pig_profile_report", currentLanguageCode, "Pig Profile Report")} - ${it.tagNumber}",
                                            lang = currentLanguageCode
                                        )
                                    } 
                                } else {
                                    onNavigateToPaywall()
                                }
                            }
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = stringResource("export_pdf"))
                        }
                    }
                    IconButton(
                        enabled = pig != null,
                        onClick = { showEditDialog.value = true }
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource("edit"))
                    }
                    if (!isArchived && pig != null) {
                        IconButton(onClick = { showArchiveDialog.value = true }) {
                            Icon(Icons.Default.Archive, contentDescription = stringResource("archive"))
                        }
                    }
                    IconButton(
                        enabled = pig != null,
                        onClick = { showDeleteConfirm.value = true }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource("delete"))
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTabState.intValue == 1 && !isArchived && pig != null) {
                ExtendedFloatingActionButton(
                    onClick = { showAddHealthDialog.value = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(stringResource("log_activity")) }
                )
            }
        }
    ) { innerPadding ->
        if (pig == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .imePadding()
            ) {
                TabRow(selectedTabIndex = selectedTabState.intValue) {
                    Tab(selected = selectedTabState.intValue == 0, onClick = { selectedTabState.intValue = 0 }, text = { Text(stringResource("overview")) })
                    Tab(selected = selectedTabState.intValue == 1, onClick = { selectedTabState.intValue = 1 }, text = { Text(stringResource("history")) })
                }

                val locale = LocalAppLanguage.current.toLocale()
                val ageDays = remember(pig.birthDate) {
                    try {
                        val birthDate = DateUtils.parseInternal(pig.birthDate)
                        if (birthDate != null) {
                            val diffMs = System.currentTimeMillis() - birthDate.time
                            (diffMs / (1000 * 60 * 60 * 24)).toInt()
                        } else -1
                    } catch (_: Exception) {
                        -1
                    }
                }

                val performance = remember(pig.breed, ageDays, pig.weight) {
                    SwineGrowthDatabase.evaluatePerformance(
                        breed = pig.breed,
                        ageDays = ageDays,
                        actualWeight = pig.weight
                    )
                }

                val weightRecords = remember(healthRecords) {
                    healthRecords.filter { it.type == "Weight Check" }
                }

                val latestWeightUpdateMs = remember(weightRecords, currentLanguageCode) {
                    weightRecords.mapNotNull { record ->
                        val date = DateUtils.parseDisplay(record.date, locale)
                            ?: DateUtils.parseInternal(record.date)
                            ?: DateUtils.parseProduction(record.date)
                        date?.time
                    }.maxOrNull()
                }

                val showWeightUpdateWarning = remember(ageDays, latestWeightUpdateMs, performance) {
                    if (performance == "Blank") {
                        true
                    } else if (ageDays > 25) {
                        if (latestWeightUpdateMs != null) {
                            val diffMs = System.currentTimeMillis() - latestWeightUpdateMs
                            val daysSinceUpdate = diffMs / (1000 * 60 * 60 * 24)
                            daysSinceUpdate > 25
                        } else {
                            true
                        }
                    } else {
                        false
                    }
                }

                when (selectedTabState.intValue) {
                    0 -> OverviewTab(pig, performance, showWeightUpdateWarning)
                    1 -> HistoryTab(healthRecords, currencySymbol, allPigs, onEditRecord = { editingRecord.value = it })
                }

                Spacer(modifier = Modifier.height(120.dp))
            }

            if (showEditDialog.value) {
                EditPigDialog(
                    pig = pig,
                    allPigs = allPigs,
                    onDismiss = { showEditDialog.value = false },
                    onConfirm = { updatedPig ->
                        onEditPig(updatedPig)
                        showEditDialog.value = false
                    }
                )
            }

            if (showDeleteConfirm.value) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm.value = false },
                    title = { Text(stringResource("delete_pig_title")) },
                    text = { Text(stringResource("delete_pig_confirm")) },
                    confirmButton = {
                        TextButton(onClick = {
                            onDeletePig(pig)
                        }) {
                            Text(stringResource("delete"), color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirm.value = false }) {
                            Text(stringResource("cancel"))
                        }
                    }
                )
            }

            if (showAddHealthDialog.value) {
                AddEditHealthRecordDialog(
                    pig = pig,
                    onDismiss = { showAddHealthDialog.value = false },
                    onConfirm = { record, trackHeat, checkPreg, pregConfirmed, details ->
                        onAddHealthRecord(record, trackHeat, checkPreg, pregConfirmed, details)
                        showAddHealthDialog.value = false
                    }
                )
            }

            if (editingRecord.value != null) {
                AddEditHealthRecordDialog(
                    pig = pig,
                    onDismiss = { editingRecord.value = null },
                    onConfirm = { record, trackHeat, checkPreg, pregConfirmed, details ->
                        onEditHealthRecord(record, trackHeat, checkPreg, pregConfirmed, details)
                        editingRecord.value = null
                    },
                    existingRecord = editingRecord.value,
                    onDelete = { recordId ->
                        showDeleteRecordConfirm.value = recordId
                        editingRecord.value = null
                    }
                )
            }

            if (showDeleteRecordConfirm.value != null) {
                AlertDialog(
                    onDismissRequest = { showDeleteRecordConfirm.value = null },
                    title = { Text(stringResource("delete_record_title")) },
                    text = { Text(stringResource("delete_record_confirm")) },
                    confirmButton = {
                        TextButton(onClick = {
                            showDeleteRecordConfirm.value?.let { onDeleteHealthRecord(it) }
                            showDeleteRecordConfirm.value = null
                        }) {
                            Text(stringResource("delete"), color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteRecordConfirm.value = null }) {
                            Text(stringResource("cancel"))
                        }
                    }
                )
            }
            
            if (showArchiveDialog.value) {
                var reason by remember { mutableStateOf("Culled") }
                val reasons = listOf("Culled", "Sold", "Died", "Other")
                var expanded by remember { mutableStateOf(value = false) }
                var otherReason by remember { mutableStateOf("") }

                AlertDialog(
                    onDismissRequest = { showArchiveDialog.value = false },
                    title = { Text(stringResource("archive_pig_title")) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource("reason_for_archiving"))
                            Box {
                                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                                    Text(stringResource(reason.lowercase()))
                                }
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    reasons.forEach { r ->
                                        DropdownMenuItem(text = { Text(stringResource(r.lowercase())) }, onClick = {
                                            reason = r
                                            expanded = false
                                        })
                                    }
                                }
                            }
                            if (reason == "Other") {
                                OutlinedTextField(
                                    value = otherReason,
                                    onValueChange = { otherReason = it },
                                    label = { Text(stringResource("specify_reason")) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val finalReason = if (reason == "Other") otherReason else reason
                            onArchivePig(pig, finalReason)
                            showArchiveDialog.value = false
                        }) {
                            Text(stringResource("archive"))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showArchiveDialog.value = false }) {
                            Text(stringResource("cancel"))
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun OverviewTab(pig: Pig, performance: String, showWeightUpdateWarning: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (showWeightUpdateWarning) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF9C4), // Light yellow background
                    contentColor = Color(0xFFF57F17)    // Dark orange text
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFF57F17),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource("weight_update_warning"),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        PigInfoCard(pig, performance)
        
        if (pig.notes.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource("notes"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(pig.notes, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun HistoryTab(
    healthRecords: List<HealthRecord>,
    currencySymbol: String,
    allPigs: List<Pig> = emptyList(),
    onEditRecord: (HealthRecord) -> Unit
) {
    if (healthRecords.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource("no_history_records"))
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(healthRecords.sortedByDescending { it.date }) { record ->
                HealthRecordItem(record, currencySymbol, allPigs, onEditRecord)
            }
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun PigInfoCard(pig: Pig, performance: String) {
    val ageMonths = remember(pig.birthDate) { DateUtils.calculateAgeMonths(pig.birthDate) }
    val ageDisplay = when {
        ageMonths < 0 -> stringResource("future_birth")
        ageMonths == 0 -> stringResource("less_than_1_month")
        ageMonths < 12 -> "$ageMonths ${stringResource("months")}"
        else -> {
            val years = ageMonths / 12
            val months = ageMonths % 12
            if (months == 0) "$years ${stringResource("years_abbr")}" 
            else "$years ${stringResource("years_abbr")}, $months ${stringResource("months_abbr")}"
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("${stringResource("tag")}: ${pig.tagNumber}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                PerformanceTag(performance)
            }
            HorizontalDivider()
            InfoRow(stringResource("dob"), pig.birthDate, Icons.Default.CalendarToday)
            InfoRow(stringResource("age"), ageDisplay, Icons.Default.Update)
            val breedDisplay = pig.breed.ifEmpty { stringResource("not_specified") }
            InfoRow(stringResource("breed"), breedDisplay, Icons.Default.Pets)
            InfoRow(stringResource("status"), stringResource(pig.status.lowercase()), Icons.Default.Info)
            InfoRow(stringResource("gender"), (stringResource(pig.gender.lowercase()) + if (pig.gender == "Male" && pig.castrated == true) " ${stringResource("castrated_label")}" else ""), Icons.Default.Transgender)
            InfoRow(stringResource("weight"), "${pig.weight} ${stringResource("kg")}", Icons.Default.MonitorWeight)
            InfoRow(stringResource("location_pen"), pig.location, Icons.Default.LocationOn)
            InfoRow(stringResource("source"), stringResource(pig.source.lowercase().replace(" ", "_")), Icons.Default.Store)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.AutoMirrored.Filled.Assignment,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource("purpose"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                PurposeTag(pig.purpose)
            }
        }
    }
}

@Composable
fun HealthRecordItem(
    record: HealthRecord,
    currencySymbol: String,
    allPigs: List<Pig> = emptyList(),
    onEdit: (HealthRecord) -> Unit
) {
    val appLanguage = LocalAppLanguage.current
    val locale = remember(appLanguage) { appLanguage.toLocale() }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                val translatedType = getTranslatedActivityType(record.type)
                Text(translatedType, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val date = remember(record.date, locale) {
                        try {
                            // First try parsing as the internal format (dd/MM/yyyy)
                            val internalDate = DateUtils.parseInternal(record.date)
                            if (internalDate != null) {
                                DateUtils.formatDateToDisplay(internalDate.time, locale)
                            } else {
                                // Fallback to production format (yyyy-MM-dd)
                                val prodDate = DateUtils.parseProduction(record.date)
                                if (prodDate != null) {
                                    DateUtils.formatDateToDisplay(prodDate.time, locale)
                                } else {
                                    record.date
                                }
                            }
                        } catch (_: Exception) {
                            record.date
                        }
                    }
                    Text(date, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { onEdit(record) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource("edit_activity"), modifier = Modifier.size(16.dp))
                    }
                }
            }
            
            val displayDescription = remember(record.description, allPigs) {
                var desc = record.description
                if (desc.contains("Pig ")) {
                    val parts = desc.split("Pig ")
                    val newDesc = StringBuilder(parts[0])
                    for (i in 1 until parts.size) {
                        val remaining = parts[i]
                        val idCandidate = remaining.takeWhile { it.isLetterOrDigit() || it == '-' || it == '_' }
                        val rest = remaining.substring(idCandidate.length)
                        
                        val tag = allPigs.find { it.id == idCandidate }?.tagNumber ?: idCandidate
                        newDesc.append("Pig ").append(tag).append(rest)
                    }
                    desc = newDesc.toString()
                }
                desc
            }

            Text(displayDescription, style = MaterialTheme.typography.bodyMedium)
            if (record.medication.isNotEmpty()) {
                Text("${stringResource("medication")}: ${record.medication}", style = MaterialTheme.typography.bodySmall)
            }
            if (record.cost > 0) {
                Text("${stringResource("cost")}: $currencySymbol${record.cost}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPigDialog(pig: Pig, allPigs: List<Pig>, onDismiss: () -> Unit, onConfirm: (Pig) -> Unit) {
    val tagNumber = remember { mutableStateOf(pig.tagNumber) }
    val birthDate = remember { mutableStateOf(pig.birthDate) }
    val breed = remember { mutableStateOf(pig.breed) }
    val gender = remember { mutableStateOf(pig.gender) }
    val castrated = remember { mutableStateOf(pig.castrated) }
    val hasFarrowed = remember { mutableStateOf(pig.hasFarrowed) }
    val weightState = remember { mutableStateOf(pig.weight.toString()) }
    val purpose = remember { mutableStateOf(pig.purpose) }
    val sowTag = remember { mutableStateOf(pig.sowTag) }
    val boarTag = remember { mutableStateOf(pig.boarTag) }
    val location = remember { mutableStateOf(pig.location) }
    val source = remember { mutableStateOf(pig.source) }
    val notes = remember { mutableStateOf(pig.notes) }

    val showDatePicker = remember { mutableStateOf(value = false) }
    val datePickerState = rememberDatePickerState()

    val sowTags = remember(allPigs) { allPigs.asSequence().filter { it.gender == "Female" }.map { it.tagNumber }.distinct().sorted().toList() }
    val boarTags = remember(allPigs) { allPigs.asSequence().filter { it.gender == "Male" }.map { it.tagNumber }.distinct().sorted().toList() }

    val scrollState = rememberScrollState()

    if (showDatePicker.value) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker.value = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val calendar = Calendar.getInstance().apply { timeInMillis = it }
                        birthDate.value = String.format(Locale.getDefault(), "%02d/%02d/%d", 
                            calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR))
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

    AlertDialog(
        onDismissRequest = { /* Prevent dismiss on outside click */ },
        properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = true),
        title = { Text(stringResource("edit_pig_title")) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                OutlinedTextField(value = tagNumber.value, onValueChange = { tagNumber.value = it }, label = { Text(stringResource("tag_number")) }, modifier = Modifier.fillMaxWidth())

                OutlinedTextField(
                    value = birthDate.value,
                    onValueChange = { birthDate.value = it },
                    label = { Text(stringResource("dob")) },
                    placeholder = { Text("DD/MM/YYYY") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker.value = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = stringResource("select_date"))
                        }
                    }
                )

                BreedDropdownField(value = breed.value, onValueChange = { breed.value = it })

                Text(stringResource("gender"), style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = gender.value == "Male", onClick = { gender.value = "Male" })
                    Text(stringResource("male"))
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = gender.value == "Female", onClick = { gender.value = "Female"; castrated.value = null })
                    Text(stringResource("female"))
                }

                if (gender.value == "Male") {
                    Text(stringResource("castrated_q"), style = MaterialTheme.typography.labelLarge)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = castrated.value == true, onClick = { castrated.value = true })
                        Text(stringResource("yes"))
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(selected = castrated.value == false, onClick = { castrated.value = false })
                        Text(stringResource("no"))
                    }
                }

                if (gender.value == "Female") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = hasFarrowed.value, onCheckedChange = { hasFarrowed.value = it })
                        Text(stringResource("has_farrowed"))
                    }
                }

                OutlinedTextField(value = weightState.value, onValueChange = { weightState.value = it }, label = { Text(stringResource("weight_kg_label")) }, modifier = Modifier.fillMaxWidth())

                Text(stringResource("purpose"), style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = purpose.value == "Breeder", onClick = { purpose.value = "Breeder" })
                    Text(stringResource("breeder"))
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = purpose.value == "Porker", onClick = { purpose.value = "Porker" })
                    Text(stringResource("porker"))
                }

                TagAutoCompleteField(label = stringResource("sow_tag"), value = sowTag.value, onValueChange = { sowTag.value = it }, suggestions = sowTags)
                TagAutoCompleteField(label = stringResource("boar_tag"), value = boarTag.value, onValueChange = { boarTag.value = it }, suggestions = boarTags)

                OutlinedTextField(value = location.value, onValueChange = { location.value = it }, label = { Text(stringResource("location_pen")) }, modifier = Modifier.fillMaxWidth())

                Text(stringResource("source"), style = MaterialTheme.typography.labelLarge)
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = source.value == "Born on farm", onClick = { source.value = "Born on farm" })
                        Text(stringResource("born_on_farm"))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = source.value == "Brought to farm", onClick = { source.value = "Brought to farm" })
                        Text(stringResource("brought_to_farm"))
                    }
                }

                OutlinedTextField(
                    value = notes.value,
                    onValueChange = { notes.value = it },
                    label = { Text(stringResource("notes")) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(pig.copy(
                    tagNumber = tagNumber.value,
                    birthDate = birthDate.value,
                    breed = breed.value,
                    gender = gender.value,
                    castrated = if (gender.value == "Male") castrated.value else null,
                    hasFarrowed = hasFarrowed.value,
                    weight = weightState.value.toDoubleOrNull() ?: 0.0,
                    purpose = purpose.value,
                    sowTag = sowTag.value,
                    boarTag = boarTag.value,
                    location = location.value,
                    source = source.value,
                    notes = notes.value
                ))
            }) { Text(stringResource("save")) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource("cancel")) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditHealthRecordDialog(
    pig: Pig,
    onDismiss: () -> Unit,
    onConfirm: (HealthRecord, Boolean, Boolean, Boolean, Map<String, Any>) -> Unit,
    existingRecord: HealthRecord? = null,
    onDelete: (String) -> Unit = {}
) {
    val languageCode = LocalAppLanguage.current.code
    val isPiglet = pig.status.equals("Piglet", ignoreCase = true)
    val isFemale = pig.gender.equals("Female", ignoreCase = true)
    val isMale = pig.gender.equals("Male", ignoreCase = true)
    
    val isBreedingFemale = isFemale && (listOf("Sow", "Gilt", "Pregnant", "Lactating", "Nursing", "Finisher").contains(pig.status))
    val canFarrow = isFemale && (pig.status == "Pregnant" || pig.status == "Sow" || pig.status == "Lactating" || pig.status == "Nursing" || pig.hasFarrowed)
    val canWean = isPiglet || (isFemale && (pig.status == "Lactating" || pig.status == "Nursing"))

    val standardTypes = remember(pig) {
        mutableListOf(
            "Vaccination", "Deworming", "Medication", "Weight Check", "Culling", "Custom"
        ).apply {
            if (isPiglet) {
                addAll(listOf("Teeth Clipping", "Tail Docking", "Iron Injection"))
                if (isMale) add("Castration")
            }
            if (canWean) {
                add("Weaning")
            }
            if (isBreedingFemale) {
                addAll(listOf("Heat Detection", "Breeding/Mating", "Pregnancy Check"))
            }
            if (canFarrow) {
                add("Farrowing")
            }
        }.distinct().sorted().toList()
    }

    val type = remember { 
        mutableStateOf(
            if (existingRecord == null) {
                standardTypes.firstOrNull { it != "Custom" } ?: standardTypes.firstOrNull() ?: "Custom"
            } else {
                if (standardTypes.contains(existingRecord.type)) existingRecord.type else "Custom"
            }
        ) 
    }
    
    // Logic to extract original notes and feature values from the description string
    val initialNotes = remember(existingRecord?.description) {
        existingRecord?.description?.let { desc ->
            var temp = desc
            val prefixes = listOf(
                "\nMated:", "Mated:",
                "\nFarrowed:", "Farrowed:",
                "\nWeight updated", "Weight updated",
                "\nReason:", "Reason:",
                "\nSold for:", "Sold for:",
                "\nMedication/Vaccine:", "Medication/Vaccine:",
                "\nWeaned", "Weaned",
                "\nPregnancy Confirmed", "Pregnancy Confirmed",
                "\nCastrated successfully", "Castrated successfully"
            )
            prefixes.forEach { prefix ->
                temp = temp.substringBefore(prefix)
            }
            temp.trim()
        } ?: ""
    }
    val notes = remember { mutableStateOf(initialNotes) }

    // Feature states
    val sowTag = remember { 
        mutableStateOf(
            existingRecord?.description?.let { if (it.contains("Mated: Sow ")) it.substringAfter("Mated: Sow ").substringBefore(" with Boar") else "" } 
                ?: (if (isFemale) pig.tagNumber else "")
        ) 
    }
    val boarTag = remember { 
        mutableStateOf(
            existingRecord?.description?.let { if (it.contains("with Boar ")) it.substringAfter("with Boar ").substringBefore("\n").trim() else "" } 
                ?: (if (isMale) pig.tagNumber else "")
        ) 
    }
    val checkPregnancy = remember { mutableStateOf(false) }
    
    val numMales = remember { mutableStateOf(existingRecord?.description?.let { if (it.contains("Farrowed: ")) it.substringAfter("Farrowed: ").substringBefore(" Males") else "" } ?: "") }
    val numFemales = remember { mutableStateOf(existingRecord?.description?.let { if (it.contains("Males, ")) it.substringAfter("Males, ").substringBefore(" Females") else "" } ?: "") }
    
    val weightState = remember { mutableStateOf(existingRecord?.description?.let { if (it.contains("Weight updated to ")) it.substringAfter("Weight updated to ").substringBefore("kg") else "" } ?: "") }
    val weaningLocation = remember { mutableStateOf(existingRecord?.description?.let { if (it.contains("Weaned and moved to location: ")) it.substringAfter("Weaned and moved to location: ").trim() else "" } ?: "") }
    
    val cullingReason = remember { mutableStateOf(existingRecord?.description?.let { if (it.contains("Reason: ")) it.substringAfter("Reason: ").substringBefore("\n") else "" } ?: "") }
    val salePrice = remember { mutableStateOf(existingRecord?.description?.let { if (it.contains("Sold for:")) it.substringAfter("Sold for:").trim().takeWhile { c -> c.isDigit() || c == '.' } else "" } ?: "") }
    
    val medName = remember {
        val extracted = existingRecord?.description?.let { desc ->
            if (desc.contains("Medication/Vaccine: ")) {
                desc.substringAfter("Medication/Vaccine: ").substringBefore(",").trim()
            } else ""
        }
        mutableStateOf(
            if (extracted.isNullOrEmpty()) existingRecord?.medication ?: ""
            else extracted
        )
    }
    val medDosage = remember { mutableStateOf(existingRecord?.description?.let { if (it.contains("Dosage: ")) it.substringAfter("Dosage: ").trim() else "" } ?: "") }
    
    val trackHeat = remember { mutableStateOf(value = false) }
    val pregnancyConfirmed = remember { mutableStateOf(existingRecord?.description?.contains("Pregnancy Confirmed") ?: false) }
    
    val customActivityName = remember { mutableStateOf(if (type.value == "Custom") existingRecord?.type ?: "" else "") }

    val appLanguage = LocalAppLanguage.current
    val locale = remember(appLanguage) { appLanguage.toLocale() }

    val initialDateMillis = remember(existingRecord?.date, locale) {
        existingRecord?.date?.let { DateUtils.parseDisplay(it, locale)?.time } ?: System.currentTimeMillis()
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)
    val showDatePicker = remember { mutableStateOf(value = false) }
    
    val scrollState = rememberScrollState()

    if (showDatePicker.value) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker.value = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker.value = false }) { Text(stringResource("ok")) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker.value = false }) { Text(stringResource("cancel")) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val formattedDate = remember(datePickerState.selectedDateMillis, locale) {
        datePickerState.selectedDateMillis?.let {
            DateUtils.formatDateToDisplay(it, locale)
        } ?: DateUtils.getCurrentDateDisplay(locale)
    }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = true),
        title = { 
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val titleText = if (existingRecord == null) {
                    stringResource("log_activity_title", when (type.value) {
                        "Vaccination" -> stringResource("vaccination")
                        "Deworming" -> stringResource("deworming")
                        "Medication" -> stringResource("medication")
                        "Weight Check" -> stringResource("weight_check")
                        "Culling" -> stringResource("culling")
                        "Teeth Clipping" -> stringResource("teeth_clipping")
                        "Tail Docking" -> stringResource("tail_docking")
                        "Iron Injection" -> stringResource("iron_injection")
                        "Weaning" -> stringResource("weaning")
                        "Castration" -> stringResource("castration")
                        "Heat Detection" -> stringResource("heat_detection")
                        "Breeding/Mating" -> stringResource("breeding_mating")
                        "Pregnancy Check" -> stringResource("pregnancy_check")
                        "Farrowing" -> stringResource("farrowing")
                        else -> type.value
                    })
                } else {
                    stringResource("edit_activity_title")
                }
                Text(titleText)
                if (existingRecord != null) {
                    IconButton(onClick = { onDelete(existingRecord.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource("delete"), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                val expanded = remember { mutableStateOf(false) }

                OutlinedTextField(
                    value = formattedDate,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource("activity_date")) },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker.value = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = stringResource("select_date"))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = pig.tagNumber,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource("target_animal")) },
                    leadingIcon = { Icon(Icons.Default.Tag, null) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    enabled = false
                )

                Box {
                    val translatedTypeLabel = getTranslatedActivityType(type.value)
                    OutlinedTextField(
                        value = translatedTypeLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource("activity_type")) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded.value) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { expanded.value = true })
                }
                
                DropdownMenu(expanded = expanded.value, onDismissRequest = { expanded.value = false }) {
                    standardTypes.forEach { t ->
                        val translatedT = getTranslatedActivityType(t)
                        DropdownMenuItem(text = { Text(translatedT) }, onClick = {
                            type.value = t
                            expanded.value = false
                        })
                    }
                }

                when (type.value) {
                    "Breeding/Mating" -> {
                        OutlinedTextField(value = sowTag.value, onValueChange = { sowTag.value = it }, label = { Text(stringResource("sow_tag")) }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = boarTag.value, onValueChange = { boarTag.value = it }, label = { Text(stringResource("boar_tag")) }, modifier = Modifier.fillMaxWidth())
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = checkPregnancy.value, onCheckedChange = { checkPregnancy.value = it })
                            Text(stringResource("schedule_preg_check"))
                        }
                    }
                    "Farrowing" -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = numMales.value, onValueChange = { numMales.value = it }, label = { Text(stringResource("males")) }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            OutlinedTextField(value = numFemales.value, onValueChange = { numFemales.value = it }, label = { Text(stringResource("females")) }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        }
                    }
                    "Vaccination", "Medication", "Deworming", "Iron Injection" -> {
                        OutlinedTextField(value = medName.value, onValueChange = { medName.value = it }, label = { Text(if (type.value == "Vaccination") stringResource("vaccine_name") else stringResource("medication_name")) }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = medDosage.value, onValueChange = { medDosage.value = it }, label = { Text(stringResource("dosage")) }, modifier = Modifier.fillMaxWidth())
                    }
                    "Weight Check" -> {
                        OutlinedTextField(value = weightState.value, onValueChange = { weightState.value = it }, label = { Text(stringResource("weight_kg_label")) }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    }
                    "Weaning" -> {
                        OutlinedTextField(value = weaningLocation.value, onValueChange = { weaningLocation.value = it }, label = { Text(stringResource("pen_number_separated_by_comma")) }, modifier = Modifier.fillMaxWidth())
                    }
                    "Culling" -> {
                        OutlinedTextField(value = cullingReason.value, onValueChange = { cullingReason.value = it }, label = { Text(stringResource("reason_sold_disease")) }, modifier = Modifier.fillMaxWidth())
                        if (cullingReason.value.equals("Sold", ignoreCase = true)) {
                            OutlinedTextField(value = salePrice.value, onValueChange = { salePrice.value = it }, label = { Text(stringResource("sale_price_currency", "Ksh")) }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        }
                    }
                    "Heat Detection" -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = trackHeat.value, onCheckedChange = { trackHeat.value = it })
                            Text(stringResource("track_heat_remind"))
                        }
                    }
                    "Pregnancy Check" -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = pregnancyConfirmed.value, onCheckedChange = { pregnancyConfirmed.value = it })
                            Text(stringResource("preg_confirmed_farrow"))
                        }
                    }
                    "Custom" -> {
                        OutlinedTextField(value = customActivityName.value, onValueChange = { customActivityName.value = it }, label = { Text(stringResource("activity_name")) }, modifier = Modifier.fillMaxWidth())
                    }
                }

                OutlinedTextField(
                    value = notes.value, 
                    onValueChange = { notes.value = it }, 
                    label = { Text(stringResource("notes_details")) }, 
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val finalDescription = StringBuilder(notes.value)
                when (type.value) {
                    "Breeding/Mating" -> {
                        if (sowTag.value.isNotEmpty() || boarTag.value.isNotEmpty()) {
                            if (finalDescription.isNotEmpty()) finalDescription.append("\n")
                            finalDescription.append(Translator.getString("mated_sow_with_boar", languageCode, sowTag.value, boarTag.value))
                        }
                    }
                    "Farrowing" -> {
                        if (numMales.value.isNotEmpty() || numFemales.value.isNotEmpty()) {
                            if (finalDescription.isNotEmpty()) finalDescription.append("\n")
                            finalDescription.append(Translator.getString("farrowed_males_females", languageCode, numMales.value, numFemales.value))
                        }
                    }
                    "Vaccination", "Medication", "Deworming", "Iron Injection" -> {
                        if (medName.value.isNotEmpty()) {
                            if (finalDescription.isNotEmpty()) finalDescription.append("\n")
                            finalDescription.append(Translator.getString("medication_vaccine_dosage", languageCode, medName.value, medDosage.value))
                        }
                    }
                    "Weight Check" -> {
                        if (weightState.value.isNotEmpty()) {
                            if (finalDescription.isNotEmpty()) finalDescription.append("\n")
                            finalDescription.append(Translator.getString("weight_updated_to", languageCode, weightState.value))
                        }
                    }
                    "Weaning" -> {
                        if (weaningLocation.value.isNotEmpty()) {
                            if (finalDescription.isNotEmpty()) finalDescription.append("\n")
                            finalDescription.append(Translator.getString("weaned_and_moved", languageCode, weaningLocation.value))
                        }
                    }
                    "Culling" -> {
                        if (cullingReason.value.isNotEmpty()) {
                            if (finalDescription.isNotEmpty()) finalDescription.append("\n")
                            finalDescription.append(Translator.getString("reason_label", languageCode, cullingReason.value))
                        }
                        if (cullingReason.value.equals("Sold", ignoreCase = true) && salePrice.value.isNotEmpty()) {
                            finalDescription.append("\n")
                            finalDescription.append(Translator.getString("sold_for_amount", languageCode, "Ksh", salePrice.value))
                        }
                    }
                    "Pregnancy Check" -> {
                        if (pregnancyConfirmed.value) {
                            if (finalDescription.isNotEmpty()) finalDescription.append("\n")
                            finalDescription.append(Translator.getString("pregnancy_confirmed", languageCode))
                        }
                    }
                    "Castration" -> {
                        if (finalDescription.isNotEmpty()) finalDescription.append("\n")
                        finalDescription.append(Translator.getString("castrated_successfully", languageCode))
                    }
                }

                onConfirm(
                    HealthRecord(
                        id = existingRecord?.id ?: "",
                        date = formattedDate,
                        type = if (type.value == "Custom") customActivityName.value else type.value,
                        description = finalDescription.toString().trim(),
                        medication = if (listOf("Vaccination", "Medication", "Deworming", "Iron Injection").contains(type.value)) medName.value else existingRecord?.medication ?: "",
                        cost = if (type.value == "Culling" && cullingReason.value.equals("Sold", ignoreCase = true)) (salePrice.value.toDoubleOrNull() ?: 0.0) else (existingRecord?.cost ?: 0.0),
                        taskId = existingRecord?.taskId
                    ),
                    trackHeat.value,
                    checkPregnancy.value,
                    pregnancyConfirmed.value,
                    mapOf(
                        "sowTag" to sowTag.value,
                        "boarTag" to boarTag.value,
                        "numMales" to numMales.value,
                        "numFemales" to numFemales.value,
                        "weight" to weightState.value,
                        "weaningLocation" to weaningLocation.value,
                        "cullingReason" to cullingReason.value,
                        "salePrice" to salePrice.value,
                        "medName" to medName.value,
                        "medDosage" to medDosage.value
                    )
                )
            }) { Text(stringResource(if (existingRecord == null) "add" else "update")) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource("cancel")) }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun PigProfileScreenPreview() {
    val samplePig = Pig(
        id = "1",
        tagNumber = "P001",
        birthDate = "15/10/2023",
        gender = "Female",
        weight = 120.5,
        purpose = "Breeder",
        location = "Pen 1"
    )
    SmartSwineTheme {
        PigProfileContent(
            pig = samplePig,
            allPigs = emptyList(),
            healthRecords = listOf(
                HealthRecord(
                    id = "h1",
                    date = "2024-01-20",
                    type = "Vaccination",
                    description = "Regular swine flu vaccination",
                    medication = "SwineFlu-X",
                    cost = 25.0
                )
            ),
            currencySymbol = "$",
            isPremium = false,
            onBack = {},
            onNavigateToPaywall = {},
            onEditPig = {},
            onDeletePig = {},
            onArchivePig = { _, _ -> },
            onAddHealthRecord = { _, _, _, _, _ -> },
            onEditHealthRecord = { _, _, _, _, _ -> },
            onDeleteHealthRecord = {}
        )
    }
}
