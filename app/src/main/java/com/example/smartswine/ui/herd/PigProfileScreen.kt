package com.example.smartswine.ui.herd

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.rotate
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
import com.example.smartswine.ui.components.RewardedPassDialog
import com.example.smartswine.utils.Translator
import com.example.smartswine.utils.stringResource
import com.example.smartswine.utils.SwineGrowthDatabase
import com.example.smartswine.utils.getTranslatedActivityType
import com.example.smartswine.ui.herd.components.EditPigDialog
import com.example.smartswine.ui.herd.components.AddEditHealthRecordDialog
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
    val context = LocalContext.current
    val currentLanguage = LocalAppLanguage.current

    var showRewardedPassDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        PigProfileContent(
            pig = pig,
            allPigs = allPigs,
            healthRecords = healthRecords,
            currencySymbol = currencySymbol,
            isPremium = isPremium,
            onBack = onBack,
            onNavigateToPaywall = onNavigateToPaywall,
            onLockedExportPdf = { showRewardedPassDialog = true },
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

        if (showRewardedPassDialog) {
            RewardedPassDialog(
                title = stringResource("unlock_pig_profile_pdf_title"),
                description = stringResource("unlock_pig_profile_pdf_desc"),
                onDismiss = { showRewardedPassDialog = false },
                onNavigateToPaywall = onNavigateToPaywall,
                onPassActivated = {
                    val currentLang = currentLanguage.code
                    pig?.let { 
                        PdfGenerator.generateHerdReportPdf(
                            context = context,
                            pigs = listOf(it),
                            allPigs = allPigs,
                            healthRecords = mapOf(it.id to healthRecords),
                            reportTitle = "${Translator.getStringWithDefault("pig_profile_report", currentLang, "Pig Profile Report")} - ${it.tagNumber}",
                            lang = currentLang
                        )
                    }
                }
            )
        }

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
    onLockedExportPdf: () -> Unit = {},
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
    val isHistoryExpanded = remember { mutableStateOf(false) }

    val context = LocalContext.current
    val currentLanguageCode = LocalAppLanguage.current.code
    val isArchived = pig?.status?.startsWith("Archived") == true

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = pig?.tagNumber ?: stringResource("pig_profile"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource("back"))
                    }
                },
                actions = {
                    PremiumWrapper(isPremium = isPremium, onLockedClick = onLockedExportPdf) {
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
                                            reportTitle = "${Translator.getStringWithDefault("pig_profile_report", currentLanguageCode, "Pig Profile Report")} - ${it.tagNumber}",
                                            lang = currentLanguageCode
                                        )
                                    } 
                                } else {
                                    onLockedExportPdf()
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
            if (!isArchived && pig != null) {
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
            val locale = LocalAppLanguage.current.toLocale()
            val ageDays = remember(pig.birthDate) {
                DateUtils.calculateAgeDays(pig.birthDate)
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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(innerPadding)
                    .imePadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Bio Section Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource("bio"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (showWeightUpdateWarning) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF9C4),
                            contentColor = Color(0xFFF57F17)
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

                MeatWithdrawalBanner(pig)

                PigInfoCard(pig, performance)
                
                if (pig.notes.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE8F5E9),
                            contentColor = Color(0xFF1B5E20)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFC8E6C9))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(stringResource("notes"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(pig.notes, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF2E7D32))
                        }
                    }
                }

                // Collapsible History Section (Closed by default)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isHistoryExpanded.value = !isHistoryExpanded.value }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.size(38.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = stringResource("history"),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource("records_count_format", healthRecords.size, if (healthRecords.size == 1) stringResource("record_singular") else stringResource("record_plural")),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { isHistoryExpanded.value = !isHistoryExpanded.value }) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isHistoryExpanded.value) "Collapse" else "Expand",
                                    modifier = Modifier.rotate(if (isHistoryExpanded.value) 180f else 0f),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (isHistoryExpanded.value) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (healthRecords.isEmpty()) {
                                    Text(
                                        text = stringResource("no_history_records"),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 12.dp)
                                    )
                                } else {
                                    healthRecords.sortedByDescending { DateUtils.parseAnyDateNonNull(it.date, locale) }.forEach { record ->
                                        HealthRecordItem(record, currencySymbol, allPigs, onEdit = { editingRecord.value = it })
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
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

        MeatWithdrawalBanner(pig)

        PigInfoCard(pig, performance)
        
        if (pig.notes.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE8F5E9),
                    contentColor = Color(0xFF1B5E20)
                ),
                border = BorderStroke(1.dp, Color(0xFFC8E6C9))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource("notes"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(pig.notes, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF2E7D32))
                }
            }
        }
    }
}

@Composable
fun MeatWithdrawalBanner(pig: Pig) {
    val appLanguage = LocalAppLanguage.current
    val locale = remember(appLanguage) { appLanguage.toLocale() }
    val isWithdrawalActive = remember(pig.activeWithdrawalUntil, locale) {
        DateUtils.isWithdrawalActive(pig.activeWithdrawalUntil, locale)
    }

    if (isWithdrawalActive) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFEBEE),
                contentColor = Color(0xFFC62828)
            ),
            border = BorderStroke(1.5.dp, Color(0xFFEF5350))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFC62828),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "⚠️ ACTIVE MEAT WITHDRAWAL",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB71C1C)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Medication: ${pig.withdrawalMedication.ifEmpty { "Veterinary Treatment" }}\nWithdrawal Active Until: ${pig.activeWithdrawalUntil}\nMeat unsafe for slaughter or sale for human consumption until withdrawal clears.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFC62828)
                    )
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
    val appLanguage = LocalAppLanguage.current
    val locale = remember(appLanguage) { appLanguage.toLocale() }

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
            items(healthRecords.sortedByDescending { DateUtils.parseAnyDateNonNull(it.date, locale) }) { record ->
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
    val lang = LocalAppLanguage.current.code
    val ageDisplay = remember(pig.birthDate, lang) { DateUtils.formatSwineAge(pig.birthDate, lang = lang) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE8F5E9),
            contentColor = Color(0xFF1B5E20)
        ),
        border = BorderStroke(1.dp, Color(0xFFC8E6C9))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${stringResource("tag")}: ${pig.tagNumber}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20)
                )
                PerformanceTag(performance)
            }
            HorizontalDivider(color = Color(0xFFC8E6C9))
            InfoRow(stringResource("dob"), pig.birthDate, Icons.Default.CalendarToday)
            InfoRow(stringResource("age"), ageDisplay, Icons.Default.Update)
            val breedDisplay = pig.breed.ifEmpty { stringResource("not_specified") }
            InfoRow(stringResource("breed"), breedDisplay, Icons.Default.Pets)
            InfoRow(stringResource("status"), stringResource(pig.status.lowercase()), Icons.Default.Info)
            InfoRow(stringResource("gender"), (stringResource(pig.gender.lowercase()) + if (pig.gender == "Male" && pig.castrated == true) " ${stringResource("castrated_label")}" else ""), Icons.Default.Transgender)
            if (pig.gender == "Female" && pig.hasFarrowed) {
                InfoRow(stringResource("sow_parity"), stringResource("parity_litters_format", pig.parity), Icons.Default.Favorite)
            }
            if (pig.expectedFarrowingDate.isNotEmpty()) {
                InfoRow(stringResource("due_date"), pig.expectedFarrowingDate, Icons.Default.DateRange)
            }
            if (pig.farrowingPenMoveDate.isNotEmpty()) {
                InfoRow(stringResource("farrowing_pen_move"), pig.farrowingPenMoveDate, Icons.Default.LocationOn)
            }
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
                        tint = Color(0xFF2E7D32)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource("purpose"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF2E7D32)
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
            
            val displayDescription = remember(record.description, allPigs, appLanguage) {
                var desc = when (record.description.trim()) {
                    "Initial record of castration." -> Translator.getString("initial_castration_record", appLanguage.code)
                    "Initial weight record." -> Translator.getString("initial_weight_record", appLanguage.code)
                    "Weight updated manually in pig details" -> Translator.getString("weight_updated_manually_desc", appLanguage.code)
                    "Weight check recorded" -> Translator.getString("weight_check_recorded_desc", appLanguage.code)
                    else -> record.description
                }
                if (desc.contains("Pig ")) {
                    val pigLabel = Translator.getString("pig", appLanguage.code)
                    val parts = desc.split("Pig ")
                    val newDesc = StringBuilder(parts[0])
                    for (i in 1 until parts.size) {
                        val remaining = parts[i]
                        val idCandidate = remaining.takeWhile { it.isLetterOrDigit() || it == '-' || it == '_' }
                        val rest = remaining.substring(idCandidate.length)
                        
                        val tag = allPigs.find { it.id == idCandidate }?.tagNumber ?: idCandidate
                        newDesc.append(pigLabel).append(" ").append(tag).append(rest)
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
