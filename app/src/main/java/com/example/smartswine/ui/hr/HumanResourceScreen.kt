package com.example.smartswine.ui.hr

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smartswine.utils.StylishDivider
import com.example.smartswine.model.StaffMember
import com.example.smartswine.ui.financials.FinancialViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartswine.ui.theme.SmartSwineTheme
import com.example.smartswine.util.PdfGenerator
import com.example.smartswine.ui.settings.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.*
import android.app.DatePickerDialog
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.smartswine.utils.LocalAppLanguage
import com.example.smartswine.utils.LocalIsPremium
import com.example.smartswine.utils.PremiumWrapper
import com.example.smartswine.utils.stringResource

@Composable
fun HumanResourceScreen(
    viewModel: HumanResourceViewModel,
    onNavigateToPaywall: () -> Unit,
    onBack: () -> Unit,
) {
    val staff by viewModel.staff.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val settingsViewModel = SettingsViewModel.getInstance()
    val currencySymbol by settingsViewModel.currencySymbol.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    val financialViewModel: FinancialViewModel = viewModel()
    val financialRecords by financialViewModel.records.collectAsStateWithLifecycle()
    val showReportOptions = remember { mutableStateOf(value = false) }
    val currentLanguage = LocalAppLanguage.current

    val isPremium = LocalIsPremium.current

    HumanResourceScreenContent(
        staff = staff,
        isLoading = isLoading,
        currencySymbol = currencySymbol,
        isPremium = isPremium,
        onBack = onBack,
        onNavigateToPaywall = onNavigateToPaywall,
        onAddStaff = { viewModel.addStaff(context, it) },
        onUpdateStaff = { viewModel.updateStaff(context, it) },
        onDeleteStaff = { viewModel.deleteStaff(it) },
        onPaySalary = { member, month, notes -> viewModel.logSalaryPayment(member, month, notes, currentLanguage.code) },
    ) {
        if (!isPremium) onNavigateToPaywall()
        else showReportOptions.value = true
    }

    if (showReportOptions.value) {
        HRReportOptionsDialog(
            onDismiss = { showReportOptions.value = false },
        ) { dateFilter ->
            val filteredRecords = when (dateFilter) {
                is HRDateFilter.All -> financialRecords
                is HRDateFilter.Range -> {
                    financialRecords.filter { record ->
                        try {
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val recordDate = sdf.parse(record.date)
                            val start = sdf.parse(dateFilter.start)
                            val end = sdf.parse(dateFilter.end)
                            (recordDate != null) && (start != null) && (end != null) &&
                                    !recordDate.before(start) && !recordDate.after(end)
                        } catch (_: Exception) {
                            false
                        }
                    }
                }
            }
            PdfGenerator.generateHRReportPdf(context, staff, currencySymbol, filteredRecords, currentLanguage.code)
            showReportOptions.value = false
        }
    }
}

sealed class HRDateFilter {
    object All : HRDateFilter()
    data class Range(val start: String, val end: String) : HRDateFilter()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HRReportOptionsDialog(onDismiss: () -> Unit, onExport: (HRDateFilter) -> Unit) {
    var selectedOption by remember { mutableStateOf("All") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    val context = LocalContext.current

    fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                onDateSelected(String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth))
            },
            calendar[Calendar.YEAR],
            calendar[Calendar.MONTH],
            calendar[Calendar.DAY_OF_MONTH]
        ).show()
    }

    AlertDialog(
        onDismissRequest = { /* Only dismiss via buttons */ },
        title = { Text(stringResource("export_hr_report")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selectedOption == "All", onClick = { selectedOption = "All" })
                    Text(stringResource("all_records"))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selectedOption == "Range", onClick = { selectedOption = "Range" })
                    Text(stringResource("select_date_range"))
                }

                if (selectedOption == "Range") {
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = {},
                        label = { Text(stringResource("from_date_label")) },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker { startDate = it } }) {
                                Icon(Icons.Default.DateRange, contentDescription = null)
                            }
                        }
                    )
                    OutlinedTextField(
                        value = endDate,
                        onValueChange = {},
                        label = { Text(stringResource("to_date_label")) },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker { endDate = it } }) {
                                Icon(Icons.Default.DateRange, contentDescription = null)
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedOption == "All") {
                        onExport(HRDateFilter.All)
                    } else if (startDate.isNotEmpty() && endDate.isNotEmpty()) {
                        onExport(HRDateFilter.Range(startDate, endDate))
                    }
                },
                enabled = (selectedOption == "All") || (startDate.isNotEmpty() && endDate.isNotEmpty())
            ) {
                Text(stringResource("generate_pdf"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource("cancel")) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HumanResourceScreenContent(
    staff: List<StaffMember>,
    isLoading: Boolean,
    currencySymbol: String,
    isPremium: Boolean,
    onBack: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    onAddStaff: (StaffMember) -> Unit,
    onUpdateStaff: (StaffMember) -> Unit,
    onDeleteStaff: (String) -> Unit,
    onPaySalary: (StaffMember, String, String) -> Unit,
    onGenerateReport: () -> Unit
) {
    val showAddDialog = remember { mutableStateOf(value = false) }
    val staffToEdit = remember { mutableStateOf<StaffMember?>(null) }

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
                        text = stringResource("human_resources_upper"),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    PremiumWrapper(isPremium = isPremium, onLockedClick = onNavigateToPaywall) {
                        IconButton(onClick = onGenerateReport) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = stringResource("generate_report"))
                        }
                    }
                }
                StylishDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { 
                    if (!isPremium) onNavigateToPaywall()
                    else showAddDialog.value = true 
                },
                icon = { Icon(if (!isPremium) Icons.Default.Lock else Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource("add_employee")) },
                containerColor = if (!isPremium) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                contentColor = if (!isPremium) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            HRSummaryCard(staff, currencySymbol)
            
            StylishDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

            if (isLoading && staff.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (staff.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource("no_staff_found"))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(staff) { member ->
                        StaffMemberItem(
                            member = member,
                            currencySymbol = currencySymbol,
                            onEdit = { staffToEdit.value = member },
                            onDelete = { onDeleteStaff(member.id) },
                            onPaySalary = { m, mon, n -> onPaySalary(m, mon, n) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(120.dp)) }
                }
            }
        }
    }

    if (showAddDialog.value) {
        AddEditStaffDialog(
            onDismiss = { showAddDialog.value = false },
            onConfirm = { 
                onAddStaff(it)
                showAddDialog.value = false
            }
        )
    }

    if (staffToEdit.value != null) {
        AddEditStaffDialog(
            member = staffToEdit.value,
            onDismiss = { staffToEdit.value = null },
            onConfirm = {
                onUpdateStaff(it)
                staffToEdit.value = null
            }
        )
    }
}

@Composable
fun HRSummaryCard(staff: List<StaffMember>, currencySymbol: String) {
    val totalStaff = staff.size
    val totalPayroll = staff.sumOf { it.salary }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(stringResource("total_staff"), style = MaterialTheme.typography.labelMedium)
                Text(
                    text = totalStaff.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(stringResource("monthly_payroll"), style = MaterialTheme.typography.labelMedium)
                Text(
                    text = "$currencySymbol${String.format(Locale.getDefault(), "%.2f", totalPayroll)}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffMemberItem(
    member: StaffMember,
    currencySymbol: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPaySalary: (StaffMember, String, String) -> Unit
) {
    val showDeleteConfirm = remember { mutableStateOf(value = false) }
    val showPayDialog = remember { mutableStateOf(value = false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Badge,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(member.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(member.role, style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(member.phone, style = MaterialTheme.typography.bodySmall)
                }
                if (member.allowAppAccess) {
                    Text("${stringResource("allow_app_access")}: ${member.email}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$currencySymbol${String.format(Locale.getDefault(), "%.2f", member.salary)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    IconButton(onClick = { showPayDialog.value = true }) {
                        Icon(Icons.Default.Payments, contentDescription = stringResource("log_salary_payment"), tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource("edit"))
                    }
                    IconButton(onClick = { showDeleteConfirm.value = true }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource("delete"), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (showPayDialog.value) {
        val calendar = Calendar.getInstance()
        val currentYear = calendar[Calendar.YEAR]
        val currentMonth = calendar[Calendar.MONTH] // 0-indexed

        val salaryState = remember { mutableStateOf(member.salary.toString()) }
        val selectedMonthIndex = remember { mutableIntStateOf(currentMonth) }
        val selectedYear = remember { mutableIntStateOf(currentYear) }
        val notesState = remember { mutableStateOf("") }
        
    val currentLanguage = LocalAppLanguage.current

    val months = listOf(
        stringResource("january"), stringResource("february"), stringResource("march"),
        stringResource("april"), stringResource("may"), stringResource("june"),
        stringResource("july"), stringResource("august"), stringResource("september"),
        stringResource("october"), stringResource("november"), stringResource("december")
    )
    val years = ((currentYear - 2)..(currentYear + 1)).map { it.toString() }

    var monthExpanded by remember { mutableStateOf(false) }
    var yearExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showPayDialog.value = false },
            title = { Text(stringResource("log_salary_payment")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource("payment_details_for", member.name), style = MaterialTheme.typography.bodyMedium)
                    
                    OutlinedTextField(
                        value = salaryState.value,
                        onValueChange = { if (it.isEmpty() || (it.toDoubleOrNull() != null)) salaryState.value = it },
                        label = { Text(stringResource("salary_amount_symbol", currencySymbol)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Month Dropdown
                        ExposedDropdownMenuBox(
                            expanded = monthExpanded,
                            onExpandedChange = { monthExpanded = !monthExpanded },
                            modifier = Modifier.weight(1.5f)
                        ) {
                            OutlinedTextField(
                                value = months[selectedMonthIndex.intValue],
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource("month")) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthExpanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = monthExpanded,
                                onDismissRequest = { monthExpanded = false }
                            ) {
                                months.forEachIndexed { index, monthName ->
                                    DropdownMenuItem(
                                        text = { Text(monthName) },
                                        onClick = {
                                            selectedMonthIndex.intValue = index
                                            monthExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Year Dropdown
                        ExposedDropdownMenuBox(
                            expanded = yearExpanded,
                            onExpandedChange = { yearExpanded = !yearExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = selectedYear.intValue.toString(),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource("year")) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearExpanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = yearExpanded,
                                onDismissRequest = { yearExpanded = false }
                            ) {
                                years.forEach { yearStr ->
                                    DropdownMenuItem(
                                        text = { Text(yearStr) },
                                        onClick = {
                                            selectedYear.intValue = yearStr.toInt()
                                            yearExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = notesState.value,
                        onValueChange = { notesState.value = it },
                        label = { Text(stringResource("notes")) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalMonth = if (currentLanguage.code == "en") {
                            "${months[selectedMonthIndex.intValue]} ${selectedYear.intValue}"
                        } else {
                            // Format: Month Year (localized)
                            "${months[selectedMonthIndex.intValue]} ${selectedYear.intValue}"
                        }
                        val finalAmount = salaryState.value.toDoubleOrNull() ?: member.salary
                        onPaySalary(member.copy(salary = finalAmount), finalMonth, notesState.value)
                        showPayDialog.value = false
                    },
                    enabled = salaryState.value.isNotEmpty()
                ) {
                    Text(stringResource("log_payment_btn"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPayDialog.value = false }) {
                    Text(stringResource("cancel"))
                }
            }
        )
    }

    if (showDeleteConfirm.value) {
        AlertDialog(
            onDismissRequest = { /* Only dismiss via buttons */ },
            title = { Text(stringResource("delete_staff_member")) },
            text = { Text(stringResource("delete_staff_confirm", member.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm.value = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource("delete"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm.value = false }) {
                    Text(stringResource("cancel"))
                }
            }
        )
    }
}

@Composable
fun AddEditStaffDialog(
    member: StaffMember? = null,
    onDismiss: () -> Unit,
    onConfirm: (StaffMember) -> Unit
) {
    val nameState = remember { mutableStateOf(member?.name ?: "") }
    val name = nameState.value
    val roleState = remember { mutableStateOf(member?.role ?: "") }
    val role = roleState.value
    val phoneState = remember { mutableStateOf(member?.phone ?: "") }
    val phone = phoneState.value
    val salaryState = remember { mutableStateOf(member?.salary?.toString() ?: "") }
    val salary = salaryState.value
    val joinDateState = remember { mutableStateOf(member?.joinDate ?: "") }
    val joinDate = joinDateState.value
    val allowAppAccessState = remember { mutableStateOf(member?.allowAppAccess ?: false) }
    val allowAppAccess = allowAppAccessState.value
    val emailState = remember { mutableStateOf(member?.email ?: "") }
    val email = emailState.value

    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            joinDateState.value = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth)
        },
        calendar[Calendar.YEAR],
        calendar[Calendar.MONTH],
        calendar[Calendar.DAY_OF_MONTH]
    )

    AlertDialog(
        onDismissRequest = { /* Only dismiss via buttons */ },
        title = { Text(stringResource(if (member == null) "add_staff_member" else "edit_staff_member")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { nameState.value = it },
                    label = { Text(stringResource("full_name")) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = role,
                    onValueChange = { roleState.value = it },
                    label = { Text(stringResource("role_hint")) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phoneState.value = it },
                    label = { Text(stringResource("phone_number")) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = salary,
                    onValueChange = { salaryState.value = it },
                    label = { Text(stringResource("monthly_payroll")) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = joinDate,
                    onValueChange = { },
                    label = { Text(stringResource("join_date")) },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { datePickerDialog.show() }) {
                            Icon(Icons.Default.DateRange, contentDescription = stringResource("select_date"))
                        }
                    }
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = allowAppAccess,
                        onCheckedChange = { allowAppAccessState.value = it }
                    )
                    Text(stringResource("allow_app_access"))
                }

                if (allowAppAccess) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { emailState.value = it },
                        label = { Text(stringResource("staff_email")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        stringResource("invitation_note"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newMember = (member ?: StaffMember()).copy(
                        name = name,
                        role = role,
                        phone = phone,
                        salary = salary.toDoubleOrNull() ?: 0.0,
                        joinDate = joinDate,
                        allowAppAccess = allowAppAccess,
                        email = if (allowAppAccess) email else ""
                    )
                    onConfirm(newMember)
                },
                enabled = name.isNotBlank() && role.isNotBlank() && (!allowAppAccess || email.isNotBlank())
            ) {
                Text(stringResource("save"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource("cancel"))
            }
        }
    )
}

@Preview(showBackground = true, name = "Light Mode")
@Composable
fun HumanResourceScreenPreview() {
    val sampleStaff = listOf(
        StaffMember(id = "1", name = "John Doe", role = "Farm Manager", phone = "0712345678", salary = 1500.0, joinDate = "2023-01-15"),
        StaffMember(id = "2", name = "Jane Smith", role = "Veterinary Assistant", phone = "0787654321", salary = 1200.0, joinDate = "2023-03-20"),
        StaffMember(id = "3", name = "Bob Wilson", role = "General Worker", phone = "0755555555", salary = 800.0, joinDate = "2023-06-10")
    )
    SmartSwineTheme(darkTheme = false) {
        HumanResourceScreenContent(
            staff = sampleStaff,
            isLoading = false,
            currencySymbol = "$",
            isPremium = false,
            onBack = {},
            onNavigateToPaywall = {},
            onAddStaff = {},
            onUpdateStaff = {},
            onDeleteStaff = {},
            onPaySalary = { _, _, _ -> },
            onGenerateReport = {}
        )
    }
}

@Preview(showBackground = true, name = "Dark Mode")
@Composable
fun HumanResourceScreenDarkPreview() {
    val sampleStaff = listOf(
        StaffMember(id = "1", name = "John Doe", role = "Farm Manager", phone = "0712345678", salary = 1500.0, joinDate = "2023-01-15")
    )
    SmartSwineTheme(darkTheme = true) {
        HumanResourceScreenContent(
            staff = sampleStaff,
            isLoading = false,
            currencySymbol = "$",
            isPremium = false,
            onBack = {},
            onNavigateToPaywall = {},
            onAddStaff = {},
            onUpdateStaff = {},
            onDeleteStaff = {},
            onPaySalary = { _, _, _ -> },
            onGenerateReport = {}
        )
    }
}

@Preview(showBackground = true, name = "Empty State")
@Composable
fun HumanResourceScreenEmptyPreview() {
    SmartSwineTheme {
        HumanResourceScreenContent(
            staff = emptyList(),
            isLoading = false,
            currencySymbol = "$",
            isPremium = false,
            onBack = {},
            onNavigateToPaywall = {},
            onAddStaff = {},
            onUpdateStaff = {},
            onDeleteStaff = {},
            onPaySalary = { _, _, _ -> },
            onGenerateReport = {}
        )
    }
}
