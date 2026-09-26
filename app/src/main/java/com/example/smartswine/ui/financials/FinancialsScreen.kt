package com.example.smartswine.ui.financials

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import com.example.smartswine.ui.components.NativeAdCard
import com.example.smartswine.ui.components.RewardedPassDialog
import com.example.smartswine.utils.LocalIsPaidPremium
import com.example.smartswine.utils.TierLimiter
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.smartswine.ui.theme.DarkBackground
import com.example.smartswine.ui.theme.SmartSwineTheme
import com.example.smartswine.model.FinancialRecord
import com.example.smartswine.model.Pig
import com.example.smartswine.util.PdfGenerator
import com.example.smartswine.utils.DateUtils
import com.example.smartswine.utils.LocalAppLanguage
import java.util.Calendar
import java.util.Locale

@Composable
fun FinancialsScreen(
    viewModel: FinancialViewModel,
    initialShowAdd: Boolean = false,
    userCountry: String = "",
    onNavigateToPaywall: () -> Unit,
    onBack: () -> Unit,
) {
    val records by viewModel.records.collectAsStateWithLifecycle()
    val allPigs by viewModel.allPigs.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        FinancialsScreenContent(
            records = records,
            allPigs = allPigs,
            isLoading = isLoading,
            initialShowAdd = initialShowAdd,
            userCountry = userCountry,
            onNavigateToPaywall = onNavigateToPaywall,
            onBack = onBack,
            onAddRecord = { record, soldPigIds ->
                viewModel.addRecord(record)
                soldPigIds.forEach { viewModel.archiveSoldPig(it) }
            },
            onDeleteRecord = viewModel::deleteRecord,
        )

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
fun FinancialsScreenContent(
    records: List<FinancialRecord>,
    allPigs: List<Pig>,
    isLoading: Boolean,
    initialShowAdd: Boolean = false,
    userCountry: String = "",
    onNavigateToPaywall: () -> Unit,
    onBack: () -> Unit,
    onAddRecord: (FinancialRecord, List<String>) -> Unit,
    onDeleteRecord: (String) -> Unit,
) {
    val isPremium = com.example.smartswine.utils.LocalIsPremium.current
    val isPaidPremium = com.example.smartswine.utils.LocalIsPaidPremium.current
    val recordLimitReached = !isPaidPremium && records.size >= TierLimiter.FREE_MAX_FINANCIAL_RECORDS
    var showRewardedPassDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val appLanguage = com.example.smartswine.utils.LocalAppLanguage.current
    val locale = remember(appLanguage) { appLanguage.toLocale() }
    val settingsCurrencySymbol by com.example.smartswine.ui.settings.SettingsViewModel.getInstance().currencySymbol.collectAsState()
    val currencySymbol = remember(userCountry, settingsCurrencySymbol) {
        val countrySymbol = if (userCountry.isNotBlank()) com.example.smartswine.utils.CountryCurrencyHelper.getCurrencyForCountry(userCountry).symbol else ""
        if (countrySymbol.isNotBlank() && countrySymbol != "$") {
            countrySymbol
        } else if (settingsCurrencySymbol.isNotBlank() && settingsCurrencySymbol != "$") {
            settingsCurrencySymbol
        } else if (countrySymbol.isNotBlank()) {
            countrySymbol
        } else {
            settingsCurrencySymbol.ifBlank { "$" }
        }
    }
    val showAddDialog = remember { mutableStateOf(value = initialShowAdd) }
    val showExportDialog = remember { mutableStateOf(value = false) }

    val isDark = MaterialTheme.colorScheme.background == DarkBackground
    val financialPrimary = if (isDark) Color(0xFF4DB6AC) else Color(0xFF00796B)
    val financialBg = if (isDark) Color(0xFF004D40) else Color(0xFFE0F2F1)

    Scaffold(
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource("back"))
                    }
                    Text(
                        text = stringResource("financials_upper"),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = financialPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    com.example.smartswine.utils.PremiumWrapper(
                        isPremium = isPremium,
                        onLockedClick = { showRewardedPassDialog = true }
                    ) {
                        IconButton(onClick = { 
                            if (isPremium) showExportDialog.value = true else showRewardedPassDialog = true 
                        }) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = stringResource("export_pdf"), tint = financialPrimary)
                        }
                    }
                }
                StylishDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { 
                    if (recordLimitReached) onNavigateToPaywall()
                    else showAddDialog.value = true 
                },
                icon = { Icon(if (recordLimitReached) Icons.Default.Lock else Icons.Default.Add, contentDescription = null) },
                text = { Text(if (recordLimitReached) "Limit Reached (${TierLimiter.FREE_MAX_FINANCIAL_RECORDS})" else stringResource("add_entry"), fontWeight = FontWeight.Bold) },
                containerColor = if (recordLimitReached) MaterialTheme.colorScheme.error else financialPrimary,
                contentColor = Color.White
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = financialPrimary)
            }

            val sortedRecords = remember(records, locale) {
                records.sortedByDescending { DateUtils.parseAnyDateNonNull(it.date, locale) }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { FinancialSummaryCard(records, currencySymbol) }
                item { UnitEconomicsCard(records, allPigs, currencySymbol) }
                item { ExpenseCategoryDistributionCard(records, currencySymbol) }
                item {
                    Text(
                        text = stringResource("transactions_count", records.size),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = financialPrimary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(sortedRecords) { record ->
                    FinancialRecordItem(record, allPigs, currencySymbol) {
                        onDeleteRecord(record.id)
                    }
                }
                if (!isPaidPremium) {
                    item {
                        NativeAdCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(120.dp)) }
            }
        }
    }

    if (showRewardedPassDialog) {
        RewardedPassDialog(
            title = stringResource("unlock_financial_pdf_title"),
            description = stringResource("unlock_financial_pdf_desc"),
            onDismiss = { showRewardedPassDialog = false },
            onNavigateToPaywall = onNavigateToPaywall,
            onPassActivated = {
                showExportDialog.value = true
            }
        )
    }

    if (showAddDialog.value) {
        AddFinancialRecordDialog(
            pigs = allPigs.filter { it.location != "Archived" },
            currencySymbol = currencySymbol,
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
                    "Daily" -> records.filter { it.date == DateUtils.getCurrentDateDisplay(locale) }
                    "current_month", "Monthly" -> {
                        val cal = java.util.Calendar.getInstance()
                        val currentMonthStr = java.text.SimpleDateFormat("MMMM", locale).format(cal.time)
                        val currentYearStr = java.text.SimpleDateFormat("yyyy", locale).format(cal.time)
                        records.filter { it.date.contains(currentMonthStr) && it.date.contains(currentYearStr) }
                    }
                    "Last 3 Months" -> {
                        val threeMonthsAgo = java.util.Calendar.getInstance().apply { add(java.util.Calendar.MONTH, -3) }
                        records.filter { record ->
                            val recordDate = DateUtils.parseAnyDate(record.date, locale)
                            (recordDate != null) && recordDate.after(threeMonthsAgo.time)
                        }
                    }
                    "Income Only", "income_only" -> records.filter { it.type == "Income" }
                    "Expenses Only", "expenses_only" -> records.filter { it.type == "Expense" }
                    else -> records
                }
                PdfGenerator.generateFinancialReportPdf(context, filteredRecords, allPigs, lang = appLanguage.code)
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
    val lang = com.example.smartswine.utils.LocalAppLanguage.current.code
    return when (category) {
        "Pig Sale" -> when (lang) {
            "fr" -> "Vente de porcs"
            "zh" -> "猪只销售"
            "es" -> "Venta de cerdos"
            "tl" -> "Benta ng Baboy"
            "vi" -> "Bán lợn"
            "th" -> "ขายหมู"
            "pt" -> "Venda de porcos"
            "hi" -> "सुअर की बिक्री"
            else -> "Pig Sale"
        }
        "Manure Sale" -> when (lang) {
            "fr" -> "Vente de fumier"
            "zh" -> "粪便销售"
            "es" -> "Venta de estiércol"
            "tl" -> "Benta ng Pataba"
            "vi" -> "Bán phân"
            "th" -> "ขายปุ๋ยคอก"
            "pt" -> "Venda de esterco"
            "hi" -> "खाद की बिक्री"
            else -> "Manure Sale"
        }
        "Breeding Service" -> when (lang) {
            "fr" -> "Service d'élevage"
            "zh" -> "配种服务"
            "es" -> "Servicio de cría"
            "tl" -> "Serbisyo sa Pagpaparami"
            "vi" -> "Dịch vụ phối giống"
            "th" -> "บริการผสมพันธุ์"
            "pt" -> "Serviço de reprodução"
            "hi" -> "प्रजनन सेवा"
            else -> "Breeding Service"
        }
        "Equipment Sale" -> when (lang) {
            "fr" -> "Vente d'équipement"
            "zh" -> "设备销售"
            "es" -> "Venta de equipos"
            "tl" -> "Benta ng Kagamitan"
            "vi" -> "Bán thiết bị"
            "th" -> "ขายอุปกรณ์"
            "pt" -> "Venda de equipamentos"
            "hi" -> "उपकरण की बिक्री"
            else -> "Equipment Sale"
        }
        "Vet/Medication" -> when (lang) {
            "fr" -> "Vétérinaire/Médicaments"
            "zh" -> "兽医/药物"
            "es" -> "Veterinario/Medicamentos"
            "tl" -> "Vet/Gamot"
            "vi" -> "Thú y/Thuốc"
            "th" -> "สัตวแพทย์/ยารักษาโรค"
            "pt" -> "Veterinário/Medicamento"
            "hi" -> "पशु चिकित्सक/दवा"
            else -> "Vet/Medication"
        }
        "Labor/Salary" -> when (lang) {
            "fr" -> "Main d'œuvre/Salaire"
            "zh" -> "人工/工资"
            "es" -> "Mano de obra/Salario"
            "tl" -> "Labor/Sahod"
            "vi" -> "Nhân công/Lương"
            "th" -> "แรงงาน/เงินเดือน"
            "pt" -> "Mão de obra/Salário"
            "hi" -> "श्रम/वेतन"
            else -> "Labor/Salary"
        }
        "Transport" -> when (lang) {
            "fr" -> "Transport"
            "zh" -> "运输"
            "es" -> "Transporte"
            "tl" -> "Transportasyon"
            "vi" -> "Vận chuyển"
            "th" -> "การขนส่ง"
            "pt" -> "Transporte"
            "hi" -> "परिवहन"
            else -> "Transport"
        }
        "Rent" -> when (lang) {
            "fr" -> "Loyer"
            "zh" -> "租金"
            "es" -> "Alquiler"
            "tl" -> "Upa"
            "vi" -> "Thuê"
            "th" -> "ค่าเช่า"
            "pt" -> "Aluguel"
            "hi" -> "किराया"
            else -> "Rent"
        }
        "Utility" -> when (lang) {
            "fr" -> "Services publics"
            "zh" -> "水电费"
            "es" -> "Servicios públicos"
            "tl" -> "Kuryente at Tubig"
            "vi" -> "Tiện ích"
            "th" -> "สาธารณูปโภค"
            "pt" -> "Serviços públicos"
            "hi" -> "उपयोगिता"
            else -> "Utility"
        }
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
fun FinancialSummaryCard(
    records: List<FinancialRecord>,
    currencySymbol: String = com.example.smartswine.ui.settings.SettingsViewModel.getInstance().currencySymbol.value
) {
    val totalIncome = records.asSequence().filter { it.type == "Income" }.sumOf { it.amount }
    val totalExpense = records.asSequence().filter { it.type == "Expense" }.sumOf { it.amount }
    val netProfit = totalIncome - totalExpense

    val isDark = MaterialTheme.colorScheme.background == DarkBackground
    val financialPrimary = if (isDark) Color(0xFF4DB6AC) else Color(0xFF00796B)
    val financialBg = if (isDark) Color(0xFF004D40) else Color(0xFFE0F2F1)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = financialBg.copy(alpha = if (isDark) 0.35f else 0.75f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, financialPrimary.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource("financial_summary"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = financialPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource("income") + ":")
                Text("$currencySymbol${String.format(Locale.getDefault(), "%.2f", totalIncome)}", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource("expense") + ":")
                Text("$currencySymbol${String.format(Locale.getDefault(), "%.2f", totalExpense)}", color = Color.Red, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = financialPrimary.copy(alpha = 0.2f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource("net_profit") + ":", fontWeight = FontWeight.Bold)
                Text(
                    "$currencySymbol${String.format(Locale.getDefault(), "%.2f", netProfit)}",
                    color = if (netProfit >= 0) Color(0xFF4CAF50) else Color.Red,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun UnitEconomicsCard(records: List<FinancialRecord>, allPigs: List<Pig>, currencySymbol: String = "$") {
    val isDark = MaterialTheme.colorScheme.background == DarkBackground
    val financialPrimary = if (isDark) Color(0xFF4DB6AC) else Color(0xFF00796B)
    val cardBg = if (isDark) Color(0xFF00382E) else Color(0xFFE8F5E9)

    val activePigs = remember(allPigs) { allPigs.filter { it.location != "Archived" && !it.status.startsWith("Archived", ignoreCase = true) } }
    val totalLiveHerdWeightKg = remember(activePigs) { activePigs.sumOf { it.weight } }
    
    // In commercial pig farming, COP is calculated on operational cycle expenses (~180 days batch/grow-out).
    // Scoping prevents mixing years of capital startup costs with current standing herd weight.
    val (cycleExpenses, isScopedToCycle) = remember(records) {
        val cycleCutoff = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -180) }
        val expenseRecords = records.filter { it.type == "Expense" }
        val recentExpenses = expenseRecords.filter { record ->
            val d = DateUtils.parseAnyDate(record.date)
            d != null && d.after(cycleCutoff.time)
        }
        if (recentExpenses.isNotEmpty()) {
            Pair(recentExpenses.sumOf { it.amount }, true)
        } else {
            Pair(expenseRecords.sumOf { it.amount }, false)
        }
    }
    
    val costOfProductionPerKg = if (totalLiveHerdWeightKg > 0.0) {
        cycleExpenses / totalLiveHerdWeightKg
    } else 0.0

    val breakEvenPerFinisher = costOfProductionPerKg * 90.0 // 90 kg standard market finisher
    val targetSellingPrice20Margin = costOfProductionPerKg * 1.20
    val targetSellingPriceFinisher = breakEvenPerFinisher * 1.20

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBg.copy(alpha = if (isDark) 0.5f else 0.85f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, financialPrimary.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AttachMoney, contentDescription = null, tint = financialPrimary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource("unit_economics_breakeven"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = financialPrimary
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = financialPrimary.copy(alpha = 0.12f)
            ) {
                Text(
                    text = if (isScopedToCycle) {
                        "${activePigs.size} ${stringResource("pigs")} (${String.format(Locale.getDefault(), "%.0f", totalLiveHerdWeightKg)} kg) • 180d ${stringResource("cycle")}"
                    } else {
                        "${activePigs.size} ${stringResource("pigs")} (${String.format(Locale.getDefault(), "%.0f", totalLiveHerdWeightKg)} kg)"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = financialPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            HorizontalDivider(color = financialPrimary.copy(alpha = 0.2f))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource("cop_per_kg_label"), style = MaterialTheme.typography.bodyMedium)
                Text(
                    "$currencySymbol${String.format(Locale.getDefault(), "%.2f / kg", costOfProductionPerKg)}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource("breakeven_finisher_label"), style = MaterialTheme.typography.bodyMedium)
                Text(
                    "$currencySymbol${String.format(Locale.getDefault(), "%.2f", breakEvenPerFinisher)}",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC62828)
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    stringResource("target_selling_price_margin"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "$currencySymbol${String.format(Locale.getDefault(), "%.2f", targetSellingPriceFinisher)} ($currencySymbol${String.format(Locale.getDefault(), "%.2f/kg", targetSellingPrice20Margin)})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
            }
        }
    }
}

@Composable
fun ExpenseCategoryDistributionCard(records: List<FinancialRecord>, currencySymbol: String = "$") {
    val isDark = MaterialTheme.colorScheme.background == DarkBackground
    val financialPrimary = if (isDark) Color(0xFF4DB6AC) else Color(0xFF00796B)
    val cardBg = if (isDark) Color(0xFF00382E) else Color(0xFFE0F2F1)

    val expenses = remember(records) { records.filter { it.type == "Expense" } }
    val totalExpense = remember(expenses) { expenses.sumOf { it.amount } }

    val feedTotal = remember(expenses) { expenses.filter { it.category.equals("Feed", ignoreCase = true) }.sumOf { it.amount } }
    val medicineTotal = remember(expenses) { expenses.filter { it.category.equals("Medicine", ignoreCase = true) || it.category.equals("Vaccination", ignoreCase = true) }.sumOf { it.amount } }
    val laborTotal = remember(expenses) { expenses.filter { it.category.equals("Labor", ignoreCase = true) || it.category.equals("Salary", ignoreCase = true) }.sumOf { it.amount } }
    val otherTotal = (totalExpense - feedTotal - medicineTotal - laborTotal).coerceAtLeast(0.0)

    val feedPct = if (totalExpense > 0.0) (feedTotal / totalExpense * 100.0) else 0.0
    val medPct = if (totalExpense > 0.0) (medicineTotal / totalExpense * 100.0) else 0.0
    val laborPct = if (totalExpense > 0.0) (laborTotal / totalExpense * 100.0) else 0.0
    val otherPct = if (totalExpense > 0.0) (otherTotal / totalExpense * 100.0) else 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBg.copy(alpha = if (isDark) 0.35f else 0.75f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, financialPrimary.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Assessment, contentDescription = null, tint = financialPrimary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource("expense_distribution_benchmarks"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = financialPrimary
                    )
                }
            }

            HorizontalDivider(color = financialPrimary.copy(alpha = 0.2f))

            // Feed distribution
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${stringResource("feed_label")} (${String.format(Locale.getDefault(), "%.1f", feedPct)}%):", style = MaterialTheme.typography.bodySmall)
                Text("$currencySymbol${String.format(Locale.getDefault(), "%.2f", feedTotal)}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
            }
            LinearProgressIndicator(
                progress = { (feedPct / 100f).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = if (feedPct > 80.0) Color(0xFFD32F2F) else Color(0xFF388E3C),
                trackColor = Color(0xFFCFD8DC)
            )

            // Health / Medicine
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${stringResource("vet_health_label")} (${String.format(Locale.getDefault(), "%.1f", medPct)}%):", style = MaterialTheme.typography.bodySmall)
                Text("$currencySymbol${String.format(Locale.getDefault(), "%.2f", medicineTotal)}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
            }

            // Labor
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${stringResource("labor_operations_label")} (${String.format(Locale.getDefault(), "%.1f", laborPct)}%):", style = MaterialTheme.typography.bodySmall)
                Text("$currencySymbol${String.format(Locale.getDefault(), "%.2f", laborTotal)}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
            }

            // Benchmark assessment
            val benchmarkText = when {
                totalExpense == 0.0 -> stringResource("no_expense_records_yet")
                feedPct in 60.0..75.0 -> stringResource("benchmark_feed_optimal")
                feedPct > 75.0 -> stringResource("benchmark_feed_alert")
                else -> stringResource("benchmark_feed_below")
            }
            Text(
                text = benchmarkText,
                style = MaterialTheme.typography.labelSmall,
                color = if (feedPct > 75.0) Color(0xFFC62828) else Color(0xFF2E7D32),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun FinancialRecordItem(record: FinancialRecord, allPigs: List<Pig> = emptyList(), currencySymbol: String = "$", onDelete: () -> Unit) {
    val isDark = MaterialTheme.colorScheme.background == DarkBackground
    val financialPrimary = if (isDark) Color(0xFF4DB6AC) else Color(0xFF00796B)

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

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, financialPrimary.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(getTranslatedFinancialCategory(record.category), fontWeight = FontWeight.Bold)
                val appLanguage = LocalAppLanguage.current
                val formattedDate = remember(record.date, appLanguage) {
                    val parsed = DateUtils.parseAnyDateNonNull(record.date)
                    DateUtils.formatDateToDisplay(parsed, appLanguage.toLocale())
                }
                Text(formattedDate, style = MaterialTheme.typography.bodySmall)
                if (record.description.isNotEmpty()) {
                    Text(
                        record.description, 
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                pigTag?.let {
                    Text(stringResource("animal_tag", it), style = MaterialTheme.typography.bodySmall, color = financialPrimary)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${if (record.type == "Income") "+" else "-"}$currencySymbol${String.format(Locale.getDefault(), "%.2f", record.amount)}",
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
    currencySymbol: String = "$",
    onDismiss: () -> Unit,
    onConfirm: (FinancialRecord, List<String>) -> Unit,
) {
    val isDark = MaterialTheme.colorScheme.background == DarkBackground
    val financialPrimary = if (isDark) Color(0xFF4DB6AC) else Color(0xFF00796B)
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
    var expandedCategory by remember { mutableStateOf(value = false) }
    var expandedPigs by remember { mutableStateOf(value = false) }
    val selectedPigIds = remember { mutableStateListOf<String>() }
    val scrollState = rememberScrollState()

    val categories = if (type == "Income") {
        listOf("Pig Sale", "Manure Sale", "Breeding Service", "Equipment Sale", "Other")
    } else {
        listOf("Feed", "Vet/Medication", "Labor/Salary", "Equipment", "Transport", "Rent", "Utility", "Other")
    }

    if (showDatePicker.value) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker.value = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        date = DateUtils.formatDateToDisplay(it, locale)
                    }
                    showDatePicker.value = false
                }) { Text(stringResource("ok"), color = financialPrimary) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker.value = false }) { Text(stringResource("cancel"), color = financialPrimary) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = { Text(stringResource("add_transaction"), color = financialPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
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
                                    category = if (selectionOption == "Income") "Pig Sale" else "Feed"
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

                if (category == "Pig Sale") {
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
                                                onCheckedChange = null,
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = financialPrimary,
                                                    checkmarkColor = Color.White
                                                )
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
                    label = { Text("${stringResource("amount")} ($currencySymbol)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource("description")) },
                    modifier = Modifier.fillMaxWidth()
                )

                // Keyboard scrolling runway
                Spacer(modifier = Modifier.height(140.dp))
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = financialPrimary,
                    contentColor = Color.White
                ),
                enabled = amount.isNotEmpty() && (category != "Other" || customCategory.isNotBlank())
            ) {
                Text(stringResource("save"))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = financialPrimary)
            ) { Text(stringResource("cancel")) }
        }
    )
}

@Composable
fun PdfExportOptionsDialog(onDismiss: () -> Unit, onExport: (String) -> Unit) {
    val isDark = MaterialTheme.colorScheme.background == DarkBackground
    val financialPrimary = if (isDark) Color(0xFF4DB6AC) else Color(0xFF00796B)
    val options = listOf(
        "all_transactions" to "All Transactions",
        "current_month" to "Monthly", // Assuming "Monthly" means current month based on code
        "last_3_months" to "Last 3 Months",
        "income_only" to "Income Only",
        "expenses_only" to "Expenses Only"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource("export_pdf"), color = financialPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                options.forEach { (key, _) ->
                    TextButton(
                        onClick = { onExport(key) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = financialPrimary)
                    ) {
                        Text(stringResource(key))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = financialPrimary)
            ) { Text(stringResource("cancel")) }
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
