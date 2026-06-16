package com.example.smartswine.ui.herd

import androidx.compose.ui.platform.LocalContext
import com.example.smartswine.util.PdfGenerator
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartswine.utils.LocalIsPremium
import com.example.smartswine.utils.PremiumWrapper
import com.example.smartswine.utils.stringResource
import com.example.smartswine.utils.StylishDivider
import com.example.smartswine.utils.LocalAppLanguage
import com.example.smartswine.model.Pig
import com.example.smartswine.ui.theme.SmartSwineTheme
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds
import java.util.Calendar
import java.util.Locale

@Composable
fun HerdDataScreen(
    viewModel: HerdViewModel,
    onNavigateToPigProfile: (String) -> Unit,
    onNavigateToArchived: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    onBack: () -> Unit
) {
    val pigs by viewModel.filteredPigs.collectAsStateWithLifecycle(initialValue = emptyList())
    val allPigs by viewModel.allPigsIncludingArchived.collectAsStateWithLifecycle()
    val sowTags by viewModel.sowTags.collectAsStateWithLifecycle()
    val boarTags by viewModel.boarTags.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val purposeFilter by viewModel.purposeFilter.collectAsStateWithLifecycle()
    val statusFilter by viewModel.statusFilter.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    val localContext = LocalContext.current
    val currentLanguageCode = LocalAppLanguage.current.code

    val isPremium = LocalIsPremium.current
    val pigLimitReached = !isPremium && allPigs.size >= 20

    Box(modifier = Modifier.fillMaxSize()) {
        HerdDataContent(
            pigs = pigs,
            sowTags = sowTags,
            boarTags = boarTags,
            stats = stats,
            searchQuery = searchQuery,
            purposeFilter = purposeFilter,
            statusFilter = statusFilter,
            isPremium = isPremium,
            pigLimitReached = pigLimitReached,
            onSearchQueryChange = { viewModel.setSearchQuery(it) },
            onPurposeFilterChange = { viewModel.setPurposeFilter(it) },
            onStatusFilterChange = { viewModel.setStatusFilter(it) },
            onAddPigs = { formData -> 
                viewModel.addPigsFromForm(formData) 
            },
            onNavigateToPigProfile = onNavigateToPigProfile,
            onShowArchived = onNavigateToArchived,
            onNavigateToPaywall = onNavigateToPaywall,
            onBack = onBack,
        ) {
            PdfGenerator.generateHerdReportPdf(
                context = localContext,
                pigs = pigs,
                allPigs = allPigs,
                healthRecords = viewModel.getAllHealthRecords().value,
                lang = currentLanguageCode
            )
        }

        error?.let { msg ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp, start = 16.dp, end = 16.dp),
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
fun HerdDataContent(
    pigs: List<Pig>,
    sowTags: List<String>,
    boarTags: List<String>,
    stats: Map<String, Int>,
    searchQuery: String,
    purposeFilter: String?,
    statusFilter: String?,
    isPremium: Boolean,
    pigLimitReached: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onPurposeFilterChange: (String?) -> Unit,
    onStatusFilterChange: (String?) -> Unit,
    onAddPigs: (HerdViewModel.AddPigFormData) -> Unit,
    onNavigateToPigProfile: (String) -> Unit,
    onShowArchived: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    onBack: () -> Unit,
    onExportPdf: () -> Unit
) {
    val showAddDialog = remember { mutableStateOf(false) }
    val showFilterMenu = remember { mutableStateOf(false) }

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
                        text = stringResource("herd_data_title"),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    PremiumWrapper(isPremium = isPremium, onLockedClick = onNavigateToPaywall) {
                        IconButton(onClick = { 
                            if (isPremium) onExportPdf() else onNavigateToPaywall() 
                        }) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = stringResource("export_pdf"))
                        }
                    }
                }
                StylishDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

                StatsRibbon(stats = stats, onShowArchived = onShowArchived)
                
                StylishDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { 
                    if (pigLimitReached) onNavigateToPaywall()
                    else showAddDialog.value = true 
                },
                icon = { Icon(if (pigLimitReached) Icons.Default.Lock else Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource("add_pig_btn")) },
                containerColor = if (pigLimitReached) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                contentColor = if (pigLimitReached) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
            )
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(stringResource("search_placeholder")) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { showFilterMenu.value = !showFilterMenu.value }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = stringResource("filter"),
                            tint = if (showFilterMenu.value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            if (showFilterMenu.value) {
                val allStatuses = listOf("Piglet", "Starter", "Grower", "Finisher", "Boar", "Gilt", "Sow", "Barrow", "Pregnant", "Lactating")
                val purposes = listOf("Breeder", "Porker")
                
                LazyRow(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = (purposeFilter == null) && (statusFilter == null),
                            onClick = {
                                onPurposeFilterChange(null)
                                onStatusFilterChange(null)
                            },
                            label = { Text(stringResource("all_filter")) }
                        )
                    }
                    
                    items(purposes) { purpose ->
                        FilterChip(
                            selected = purposeFilter == purpose,
                            onClick = {
                                onPurposeFilterChange(if (purposeFilter == purpose) null else purpose)
                            },
                            label = { Text(stringResource(purpose.lowercase())) }
                        )
                    }

                    items(allStatuses) { status ->
                        FilterChip(
                            selected = statusFilter == status,
                            onClick = {
                                onStatusFilterChange(if (statusFilter == status) null else status)
                            },
                            label = { Text(stringResource(status.lowercase())) }
                        )
                    }
                }
            }

            if (pigs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isEmpty() && purposeFilter == null && statusFilter == null) 
                            stringResource("no_pigs_yet") 
                        else 
                            stringResource("no_matching_pigs"),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable { onShowArchived() },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Archive, contentDescription = null)
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text(stringResource("archived_pigs_title"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(stringResource("archived_pigs_subtitle"), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    item {
                        StylishDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }

                    items(pigs) { pig ->
                        PigItem(pig, onClick = { onNavigateToPigProfile(pig.id) })
                    }

                    item {
                        Spacer(modifier = Modifier.height(120.dp))
                    }
                }
            }
        }

        if (showAddDialog.value) {
            AddPigDialog(
                sowTags = sowTags,
                boarTags = boarTags,
                onDismiss = { showAddDialog.value = false },
                onConfirm = { formData ->
                    onAddPigs(formData)
                    showAddDialog.value = false
                }
            )
        }
    }
}

@Composable
fun StatsRibbon(stats: Map<String, Int>, onShowArchived: () -> Unit) {
    val currentIndexState = remember { mutableIntStateOf(0) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(5.seconds)
            currentIndexState.intValue = (currentIndexState.intValue + 1) % 4
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable { 
                if (currentIndexState.intValue == 3) {
                    onShowArchived()
                } else {
                    currentIndexState.intValue = (currentIndexState.intValue + 1) % 4
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        AnimatedContent(
            targetState = currentIndexState.intValue,
            transitionSpec = {
                (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
            },
            label = "RibbonAnimation"
        ) { index ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (index) {
                    0 -> {
                        Text(
                            text = stringResource("total_pigs", stats["total"] ?: 0),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource("breeders_porkers_summary", stats["breeders_count"] ?: 0, stats["porkers_count"] ?: 0),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    1 -> {
                        Text(
                            text = stringResource("total_breeders", stats["breeders_count"] ?: 0),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource("breeders_stats_detail", 
                                stats["breeders_piglets"] ?: 0, stats["breeders_starter"] ?: 0, stats["breeders_grower"] ?: 0, stats["boars"] ?: 0, stats["gilts"] ?: 0,
                                stats["Pregnant"] ?: 0, stats["Lactating"] ?: 0, stats["sows"] ?: 0),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                    2 -> {
                        Text(
                            text = stringResource("total_porkers", stats["porkers_count"] ?: 0),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource("porkers_stats_detail", stats["Starter"] ?: 0, stats["Grower"] ?: 0, stats["Finisher"] ?: 0),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    3 -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Archive, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource("archived_pigs_title"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = stringResource("archived_pigs_subtitle"),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedPigsPage(
    pigs: List<Pig>,
    onBack: () -> Unit,
    onNavigateToPigProfile: (String) -> Unit
) {
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
                        text = stringResource("archived_pigs_upper"),
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
        if (pigs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource("no_archived_pigs"))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(pigs) { pig ->
                    PigItem(pig = pig, onClick = { onNavigateToPigProfile(pig.id) })
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPigDialog(
    sowTags: List<String>,
    boarTags: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (HerdViewModel.AddPigFormData) -> Unit
) {
    var isMultiple by remember { mutableStateOf(false) }
    
    // Common fields
    val birthDate = remember { mutableStateOf("") }
    val breed = remember { mutableStateOf("") }
    val purpose = remember { mutableStateOf("Breeder") }
    val sowTag = remember { mutableStateOf("") }
    val boarTag = remember { mutableStateOf("") }
    val source = remember { mutableStateOf("Born on farm") }
    val notes = remember { mutableStateOf("") }

    // Single mode specific
    val tagNumber = remember { mutableStateOf("") }
    val gender = remember { mutableStateOf("Male") }
    val castrated = remember { mutableStateOf<Boolean?>(null) }
    val castrationDate = remember { mutableStateOf("") }
    val hasFarrowed = remember { mutableStateOf(false) }
    val weight = remember { mutableStateOf("") }
    val location = remember { mutableStateOf("") }
    val purchasePrice = remember { mutableStateOf("") }

    // Multiple mode specific
    val maleQty = remember { mutableStateOf("0") }
    val femaleQty = remember { mutableStateOf("0") }
    
    val maleData = remember { mutableStateMapOf<Int, HerdViewModel.MultiPigEntry>() }
    val femaleData = remember { mutableStateMapOf<Int, HerdViewModel.MultiPigEntry>() }

    val showDatePicker = remember { mutableStateOf(false) }
    val showCastrationDatePicker = remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    val castrationDatePickerState = rememberDatePickerState()

    val scrollState = rememberScrollState()

    if (showDatePicker.value) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker.value = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            val calendar = Calendar.getInstance().apply { timeInMillis = it }
                            birthDate.value = String.format(Locale.getDefault(), "%02d/%02d/%d", 
                                calendar[Calendar.DAY_OF_MONTH], calendar[Calendar.MONTH] + 1, calendar[Calendar.YEAR])
                        }
                        showDatePicker.value = false
                    }
                ) { Text(stringResource("ok")) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker.value = false }) { Text(stringResource("cancel")) } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showCastrationDatePicker.value) {
        DatePickerDialog(
            onDismissRequest = { showCastrationDatePicker.value = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        castrationDatePickerState.selectedDateMillis?.let {
                            val calendar = Calendar.getInstance().apply { timeInMillis = it }
                            castrationDate.value = String.format(Locale.getDefault(), "%02d/%02d/%d", 
                                calendar[Calendar.DAY_OF_MONTH], calendar[Calendar.MONTH] + 1, calendar[Calendar.YEAR])
                        }
                        showCastrationDatePicker.value = false
                    }
                ) { Text(stringResource("ok")) }
            },
            dismissButton = { TextButton(onClick = { showCastrationDatePicker.value = false }) { Text(stringResource("cancel")) } }
        ) { DatePicker(state = castrationDatePickerState) }
    }

    AlertDialog(
        onDismissRequest = { },
        properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = true),
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isMultiple) stringResource("add_multiple_pigs") else stringResource("add_new_pig"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(stringResource("single"), style = MaterialTheme.typography.labelMedium)
                    Switch(
                        checked = isMultiple,
                        onCheckedChange = { isMultiple = it },
                        modifier = Modifier.padding(horizontal = 12.dp).scale(0.8f)
                    )
                    Text(stringResource("multiple"), style = MaterialTheme.typography.labelMedium)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().verticalScroll(scrollState)) {
                if (!isMultiple) {
                    // Single Mode
                    OutlinedTextField(value = tagNumber.value, onValueChange = { tagNumber.value = it }, label = { Text(stringResource("tag_number")) }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = birthDate.value, onValueChange = { birthDate.value = it }, label = { Text(stringResource("dob")) }, placeholder = { Text("DD/MM/YYYY") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { IconButton(onClick = { showDatePicker.value = true }) { Icon(Icons.Default.DateRange, stringResource("select_date")) } }
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
                            RadioButton(selected = castrated.value == false, onClick = { castrated.value = false; castrationDate.value = "" })
                            Text(stringResource("no"))
                        }
                        if (castrated.value == true) {
                            OutlinedTextField(
                                value = castrationDate.value, onValueChange = { castrationDate.value = it }, label = { Text(stringResource("castration_date")) }, placeholder = { Text("DD/MM/YYYY") },
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = { IconButton(onClick = { showCastrationDatePicker.value = true }) { Icon(Icons.Default.DateRange, stringResource("select_date")) } }
                            )
                        }
                    }

                    if (gender.value == "Female") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = hasFarrowed.value, onCheckedChange = { hasFarrowed.value = it })
                            Text(stringResource("has_farrowed"))
                        }
                    }

                    OutlinedTextField(value = weight.value, onValueChange = { weight.value = it }, label = { Text(stringResource("weight_kg_label")) }, modifier = Modifier.fillMaxWidth())
                    
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
                    if (source.value == "Brought to farm") {
                        OutlinedTextField(value = purchasePrice.value, onValueChange = { purchasePrice.value = it }, label = { Text(stringResource("purchase_price")) }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    }
                } else {
                    // Multiple Mode
                    OutlinedTextField(
                        value = birthDate.value, onValueChange = { birthDate.value = it }, label = { Text(stringResource("dob")) }, placeholder = { Text("DD/MM/YYYY") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { IconButton(onClick = { showDatePicker.value = true }) { Icon(Icons.Default.DateRange, stringResource("select_date")) } }
                    )
                    BreedDropdownField(value = breed.value, onValueChange = { breed.value = it })
                    
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

                    Text(stringResource("source"), style = MaterialTheme.typography.labelLarge)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = source.value == "Born on farm", onClick = { source.value = "Born on farm" })
                            Text(stringResource("born_on_farm"))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = source.value == "Brought to farm", onClick = { source.value = "Brought to farm" })
                            Text(stringResource("brought_to_farm"))
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    
                    // Males Section
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource("males"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = maleQty.value, onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) maleQty.value = it }, label = { Text(stringResource("qty")) }, modifier = Modifier.width(80.dp), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        }
                        val mCount = maleQty.value.toIntOrNull() ?: 0
                        if (mCount > 0) {
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(stringResource("tag_hash"), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Text(stringResource("weight"), modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Text(stringResource("pen_hash"), modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                            repeat(mCount) { i ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    val data = maleData[i] ?: HerdViewModel.MultiPigEntry("", "", "")
                                    OutlinedTextField(value = data.tagNumber, onValueChange = { maleData[i] = data.copy(tagNumber = it) }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(value = data.weight, onValueChange = { maleData[i] = data.copy(weight = it) }, modifier = Modifier.weight(0.8f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                                    OutlinedTextField(value = data.location, onValueChange = { maleData[i] = data.copy(location = it) }, modifier = Modifier.weight(0.8f), singleLine = true)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Females Section
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource("females"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = femaleQty.value, onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) femaleQty.value = it }, label = { Text(stringResource("qty")) }, modifier = Modifier.width(80.dp), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        }
                        val fCount = femaleQty.value.toIntOrNull() ?: 0
                        if (fCount > 0) {
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(stringResource("tag_hash"), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Text(stringResource("weight"), modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Text(stringResource("pen_hash"), modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                            repeat(fCount) { i ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    val data = femaleData[i] ?: HerdViewModel.MultiPigEntry("", "", "")
                                    OutlinedTextField(value = data.tagNumber, onValueChange = { femaleData[i] = data.copy(tagNumber = it) }, modifier = Modifier.weight(1f), singleLine = true)
                                    OutlinedTextField(value = data.weight, onValueChange = { femaleData[i] = data.copy(weight = it) }, modifier = Modifier.weight(0.8f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                                    OutlinedTextField(value = data.location, onValueChange = { femaleData[i] = data.copy(location = it) }, modifier = Modifier.weight(0.8f), singleLine = true)
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(value = notes.value, onValueChange = { notes.value = it }, label = { Text(stringResource("notes")) }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text(stringResource("cancel")) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        val formData = HerdViewModel.AddPigFormData(
                            isMultiple = isMultiple,
                            birthDate = birthDate.value,
                            breed = breed.value,
                            purpose = purpose.value,
                            sowTag = sowTag.value,
                            boarTag = boarTag.value,
                            source = source.value,
                            notes = notes.value,
                            tagNumber = tagNumber.value,
                            gender = gender.value,
                            castrated = castrated.value,
                            castrationDate = castrationDate.value,
                            hasFarrowed = hasFarrowed.value,
                            weight = weight.value,
                            location = location.value,
                            purchasePrice = purchasePrice.value,
                            malePigs = maleData.values.toList(),
                            femalePigs = femaleData.values.toList()
                        )
                        onConfirm(formData)
                    }) { Text(stringResource("add")) }
                }
                
                // Extra spacer to allow buttons to be scrolled higher
                Spacer(modifier = Modifier.height(100.dp))
            }
        },
        confirmButton = { },
        dismissButton = { }
    )

}

@Preview(showBackground = true)
@Composable
fun HerdDataScreenPreview() {
    SmartSwineTheme {
        HerdDataContent(
            pigs = listOf(
                Pig(id = "1", tagNumber = "P001", birthDate = "01/01/2024", gender = "Female", weight = 120.5, purpose = "Breeder", location = "Pen 1"),
                Pig(id = "2", tagNumber = "P002", birthDate = "15/02/2024", gender = "Male", weight = 135.0, purpose = "Porker", location = "Pen 2"),
                Pig(id = "3", tagNumber = "P003", birthDate = "10/03/2024", gender = "Female", weight = 115.2, purpose = "Porker", location = "Pen 3")
            ),
            sowTags = emptyList(),
            boarTags = emptyList(),
            stats = mapOf("total" to 3, "breeders_count" to 1, "porkers_count" to 2),
            searchQuery = "",
            purposeFilter = null,
            statusFilter = null,
            isPremium = false,
            pigLimitReached = false,
            onSearchQueryChange = {},
            onPurposeFilterChange = {},
            onStatusFilterChange = {},
            onAddPigs = { _ -> },
            onNavigateToPigProfile = {},
            onShowArchived = {},
            onNavigateToPaywall = {},
            onBack = {},
            onExportPdf = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AddPigDialogPreview() {
    SmartSwineTheme {
        AddPigDialog(
            sowTags = emptyList(),
            boarTags = emptyList(),
            onDismiss = {},
            onConfirm = { _ -> }
        )
    }
}
