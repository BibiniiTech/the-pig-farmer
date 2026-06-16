package com.example.smartswine.ui.financials

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.smartswine.utils.StylishDivider
import com.example.smartswine.utils.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartswine.ui.theme.SmartSwineTheme
import com.example.smartswine.model.FinancialRecord
import com.example.smartswine.model.Pig
import com.example.smartswine.util.PdfGenerator
import com.example.smartswine.utils.DateUtils
import java.util.Locale

@Composable
fun FinancialsScreen(
    viewModel: FinancialViewModel,
    onNavigateToPaywall: () -> Unit,
    onBack: () -> Unit,
) {
    val records by viewModel.records.collectAsStateWithLifecycle()
    val allPigs by viewModel.allPigs.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    FinancialsScreenContent(
        records = records,
        allPigs = allPigs,
        isLoading = isLoading,
        onNavigateToPaywall = onNavigateToPaywall,
        onBack = onBack,
        onAddRecord = { record, soldPigIds ->
            viewModel.addRecord(record)
            soldPigIds.forEach { viewModel.archiveSoldPig(it) }
        },
        onDeleteRecord = viewModel::deleteRecord,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialsScreenContent(
    records: List<FinancialRecord>,
    allPigs: List<Pig>,
    isLoading: Boolean,
    onNavigateToPaywall: () -> Unit,
    onBack: () -> Unit,
    onAddRecord: (FinancialRecord, List<String>) -> Unit,
    onDeleteRecord: (String) -> Unit,
) {
    val isPremium = com.example.smartswine.utils.LocalIsPremium.current
    val context = LocalContext.current
    val currentLanguageCode = com.example.smartswine.utils.LocalAppLanguage.current.code
    val showAddDialog = remember { mutableStateOf(value = false) }
    val showExportDialog = remember { mutableStateOf(value = false) }

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
                        text = stringResource("financials_upper"),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    com.example.smartswine.utils.PremiumWrapper(
                        isPremium = isPremium,
                        onLockedClick = onNavigateToPaywall
                    ) {
                        IconButton(onClick = { 
                            if (isPremium) showExportDialog.value = true else onNavigateToPaywall() 
                        }) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = stringResource("export_pdf"))
                        }
                    }
                }
                StylishDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog.value = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource("add_entry")) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            FinancialSummaryCard(records)
            
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(records) { record ->
                    FinancialRecordItem(record, allPigs) {
                        onDeleteRecord(record.id)
                    }
                }
                item { Spacer(modifier = Modifier.height(120.dp)) }
            }
        }
    }

    if (showAddDialog.value) {
        AddFinancialRecordDialog(
            pigs = allPigs.filter { it.location != "Archived" },
            onDismiss = { showAddDialog.value = false },
        ) { record, soldPigIds ->
            onAddRecord(record, soldPigIds)
            showAddDialog.value = false
        }
    }

    if (showExportDialog.value) {
        PdfExportOptionsDialog(
            onDismiss = { showExportDialog.value = false },
            onExport = { filter ->
                val filteredRecords = when (filter) {
                    "Daily" -> records.filter { it.date == DateUtils.getCurrentDateDisplay() }
                    "current_month", "Monthly" -> {
                        val cal = java.util.Calendar.getInstance()
                        val currentMonthStr = java.text.SimpleDateFormat("MMMM", Locale.getDefault()).format(cal.time)
                        val currentYearStr = java.text.SimpleDateFormat("yyyy", Locale.getDefault()).format(cal.time)
                        records.filter { it.date.contains(currentMonthStr) && it.date.contains(currentYearStr) }
                    }
                    "Last 3 Months" -> records // TODO: Implement proper date range filtering
                    "Income Only", "income_only" -> records.filter { it.type == "Income" }
                    "Expenses Only", "expenses_only" -> records.filter { it.type == "Expense" }
                    else -> records
                }
                PdfGenerator.generateFinancialReportPdf(context, filteredRecords, allPigs, lang = currentLanguageCode)
                showExportDialog.value = false
            }
        )
    }
}

@Composable
fun getTranslatedFinancialType(type: String): String {
    return when (type) {
        "Income" -> stringResource("income")
        "Expense" -> stringResource("expense")
        else -> type
    }
}

@Composable
fun getTranslatedFinancialCategory(category: String): String {
    return when (category) {
        "Sale" -> stringResource("cat_sale")
        "Other" -> stringResource("cat_other")
        "Feed" -> stringResource("cat_feed")
        "Medicine" -> stringResource("cat_medicine")
        "Equipment" -> stringResource("cat_equipment")
        "Labor" -> stringResource("cat_labor")
        else -> category
    }
}

@Composable
fun FinancialSummaryCard(records: List<FinancialRecord>) {
    val totalIncome = records.asSequence().filter { it.type == "Income" }.sumOf { it.amount }
    val totalExpense = records.asSequence().filter { it.type == "Expense" }.sumOf { it.amount }
    val netProfit = totalIncome - totalExpense

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource("financial_summary"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource("income") + ":")
                Text(String.format(Locale.getDefault(), "%.2f", totalIncome), color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource("expense") + ":")
                Text(String.format(Locale.getDefault(), "%.2f", totalExpense), color = Color.Red, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource("net_profit") + ":", fontWeight = FontWeight.Bold)
                Text(
                    String.format(Locale.getDefault(), "%.2f", netProfit),
                    color = if (netProfit >= 0) Color(0xFF4CAF50) else Color.Red,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun FinancialRecordItem(record: FinancialRecord, allPigs: List<Pig> = emptyList(), onDelete: () -> Unit) {
    val pigTag = if (!record.pigId.isNullOrEmpty()) {
        allPigs.find { it.id == record.pigId }?.tagNumber ?: (stringResource("tag") + ": ${record.pigId}")
    } else null

    val showDeleteConfirm = remember { mutableStateOf(value = false) }

    if (showDeleteConfirm.value) {
        val translatedType = getTranslatedFinancialType(record.type)
        AlertDialog(
            onDismissRequest = { showDeleteConfirm.value = false },
            title = { Text(stringResource("delete_transaction")) },
            text = { Text(stringResource("delete_transaction_confirm", translatedType, record.amount)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm.value = false
                    }
                ) { Text(stringResource("delete"), color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm.value = false }) {
                    Text(stringResource("cancel"))
                }
            }
        )
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(getTranslatedFinancialCategory(record.category), fontWeight = FontWeight.Bold)
                Text(record.date, style = MaterialTheme.typography.bodySmall)
                if (record.description.isNotEmpty()) {
                    Text(
                        record.description, 
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                pigTag?.let {
                    Text(stringResource("animal_tag", it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${if (record.type == "Income") "+" else "-"}${String.format(Locale.getDefault(), "%.2f", record.amount)}",
                    color = if (record.type == "Income") Color(0xFF4CAF50) else Color.Red,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = { showDeleteConfirm.value = true }) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource("delete"), tint = Color.Gray, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFinancialRecordDialog(
    pigs: List<Pig>,
    onDismiss: () -> Unit,
    onConfirm: (FinancialRecord, List<String>) -> Unit,
) {
    val appLanguage = com.example.smartswine.utils.LocalAppLanguage.current
    val locale = remember(appLanguage) { appLanguage.toLocale() }
    var date by remember(locale) { mutableStateOf(DateUtils.getCurrentDateDisplay(locale)) }
    val showDatePicker = remember { mutableStateOf(value = false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Expense") }
    var category by remember { mutableStateOf("Feed") }
    var customCategory by remember { mutableStateOf("") }
    var expandedType by remember { mutableStateOf(value = false) }
    var expandedCategory by remember { mutableStateOf(false) }
    var expandedPigs by remember { mutableStateOf(false) }
    val selectedPigIds = remember { mutableStateListOf<String>() }
    val scrollState = rememberScrollState()

    val categories = if (type == "Income") listOf("Sale", "Other") else listOf("Feed", "Medicine", "Equipment", "Labor", "Other")

    if (showDatePicker.value) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker.value = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        date = DateUtils.formatDateToDisplay(it, locale)
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
        onDismissRequest = onDismiss,
        title = { Text(stringResource("add_transaction")) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                // Date Picker
                OutlinedTextField(
                    value = date,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource("transaction_date")) },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker.value = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = stringResource("select_date"))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Type Dropdown
                ExposedDropdownMenuBox(expanded = expandedType, onExpandedChange = { expandedType = !expandedType }) {
                    OutlinedTextField(
                        value = getTranslatedFinancialType(type),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource("type")) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expandedType, onDismissRequest = { expandedType = false }) {
                        listOf("Income", "Expense").forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(getTranslatedFinancialType(selectionOption)) },
                                onClick = {
                                    type = selectionOption
                                    category = if (selectionOption == "Income") "Sale" else "Feed"
                                    expandedType = false
                                }
                            )
                        }
                    }
                }

                // Category Dropdown
                ExposedDropdownMenuBox(expanded = expandedCategory, onExpandedChange = { expandedCategory = !expandedCategory }) {
                    OutlinedTextField(
                        value = getTranslatedFinancialCategory(category),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource("category")) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expandedCategory, onDismissRequest = { expandedCategory = false }) {
                        categories.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(getTranslatedFinancialCategory(selectionOption)) },
                                onClick = {
                                    category = selectionOption
                                    expandedCategory = false
                                }
                            )
                        }
                    }
                }

                if (category == "Other") {
                    OutlinedTextField(
                        value = customCategory,
                        onValueChange = { customCategory = it },
                        label = { Text(stringResource("custom_category_name")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (category == "Sale") {
                    val availablePigs = remember(pigs) {
                        pigs.filter { pig ->
                            val isArchived = pig.location.equals("Archived", ignoreCase = true) || 
                                           pig.status.contains("Archived", ignoreCase = true)
                            val isCulled = pig.status.contains("Culled", ignoreCase = true)
                            !isArchived && !isCulled
                        }
                    }
                    Text(stringResource("select_pigs_sold"), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                    ExposedDropdownMenuBox(
                        expanded = expandedPigs,
                        onExpandedChange = { expandedPigs = !expandedPigs }
                    ) {
                        OutlinedTextField(
                            value = if (selectedPigIds.isEmpty()) stringResource("select_pigs") else stringResource("pigs_selected_count", selectedPigIds.size),
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPigs) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedPigs,
                            onDismissRequest = { expandedPigs = false }
                        ) {
                            availablePigs.forEach { pig ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = selectedPigIds.contains(pig.id),
                                                onCheckedChange = null
                                            )
                                            Text(text = "${pig.tagNumber} (${pig.status})", modifier = Modifier.padding(start = 8.dp))
                                        }
                                    },
                                    onClick = {
                                        if (selectedPigIds.contains(pig.id)) {
                                            selectedPigIds.remove(pig.id)
                                        } else {
                                            selectedPigIds.add(pig.id)
                                        }
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.all { char -> (char.isDigit() || char == '.') }) amount = it },
                    label = { Text(stringResource("amount")) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource("description")) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        FinancialRecord(
                            date = date,
                            type = type,
                            category = if (category == "Other" && customCategory.isNotBlank()) customCategory else category,
                            amount = amount.toDoubleOrNull() ?: 0.0,
                            description = description,
                            pigId = if (selectedPigIds.size == 1) selectedPigIds.first() else null
                        ),
                        selectedPigIds.toList()
                    )
                },
                enabled = amount.isNotEmpty() && (category != "Other" || customCategory.isNotBlank())
            ) {
                Text(stringResource("save"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource("cancel")) }
        }
    )
}

@Composable
fun PdfExportOptionsDialog(onDismiss: () -> Unit, onExport: (String) -> Unit) {
    val options = listOf(
        "all_transactions" to "All Transactions",
        "current_month" to "Monthly", // Assuming "Monthly" means current month based on code
        "last_3_months" to "Last 3 Months",
        "income_only" to "Income Only",
        "expenses_only" to "Expenses Only"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource("export_pdf")) },
        text = {
            Column {
                options.forEach { (key, _) ->
                    TextButton(
                        onClick = { onExport(key) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(key))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource("cancel")) }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun FinancialsScreenPreview() {
    val sampleRecords = listOf(
        FinancialRecord(id = "1", date = "2023-10-27", type = "Income", category = "Sale", amount = 1500.0, description = "Sold 2 pigs"),
        FinancialRecord(id = "2", date = "2023-10-26", type = "Expense", category = "Feed", amount = 500.0, description = "Bought 5 bags of feed"),
        FinancialRecord(id = "3", date = "2023-10-25", type = "Expense", category = "Medicine", amount = 100.0, description = "Vaccines")
    )
    SmartSwineTheme {
        FinancialsScreenContent(
            records = sampleRecords,
            allPigs = emptyList(),
            isLoading = false,
            onNavigateToPaywall = {},
            onBack = {},
            onAddRecord = { _, _ -> },
            onDeleteRecord = {}
        )
    }
}
