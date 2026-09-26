package com.example.smartswine.ui.dashboard.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bibiniitech.smartswine.R
import com.example.smartswine.model.FinancialRecord
import com.example.smartswine.model.HealthRecord
import com.example.smartswine.model.Pig
import com.example.smartswine.model.StaffMember
import com.example.smartswine.ui.components.NativeAdCard
import com.example.smartswine.ui.components.RewardedPassDialog
import com.example.smartswine.ui.dashboard.components.grid.*
import com.example.smartswine.ui.hr.AddEditStaffDialog
import com.example.smartswine.ui.hr.LogSalaryDialog
import com.example.smartswine.ui.hr.StaffFullDetailsDialog
import com.example.smartswine.ui.hr.StaffProfileDialog
import com.example.smartswine.ui.navigation.Screen
import com.example.smartswine.ui.settings.SettingsViewModel
import com.example.smartswine.ui.theme.DarkBackground
import com.example.smartswine.util.PdfGenerator
import com.example.smartswine.utils.*

@Composable
fun ManagementGrid(
    expandedSection: String?,
    onToggleSection: (String) -> Unit,
    allPigs: List<Pig>,
    herdStats: Map<String, Int>,
    financialRecords: List<FinancialRecord> = emptyList(),
    staff: List<StaffMember> = emptyList(),
    userCountry: String = "",
    targetSubOption: String? = null,
    onSubOptionChange: (String?) -> Unit = {},
    targetPigTag: String? = null,
    isFinancialsRestricted: Boolean = false,
    onAddPigClick: () -> Unit,
    onShowFeedCalculator: () -> Unit,
    onShowFeedFormulator: () -> Unit,
    onAddStaff: (StaffMember) -> Unit = {},
    onUpdateStaff: (StaffMember) -> Unit = {},
    onArchiveStaff: (StaffMember) -> Unit = {},
    onPaySalary: (StaffMember, String, String, Double, Double, String) -> Unit = { _, _, _, _, _, _ -> },
    onUpdatePigWeight: (String, Double) -> Unit = { _, _ -> },
    onLogHealthActivity: (List<String>, HealthRecord, Boolean, Boolean, Boolean, Map<String, Any>) -> Unit = { _, _, _, _, _, _ -> },
    onRegisterCoordinates: (String, LayoutCoordinates) -> Unit = { _, _ -> },
    onItemExpanded: (String) -> Unit = {},
    onNavigateTo: (String) -> Unit
) {
    val currentLanguage = LocalAppLanguage.current
    val isPremium = LocalIsPremium.current
    val isPaidPremium = LocalIsPaidPremium.current
    val context = LocalContext.current

    var showDiseaseFinderPassDialog by remember { mutableStateOf(false) }
    var showHRPdfPassDialog by remember { mutableStateOf(false) }

    // Sub-option accordion states
    var openMarketSubOption by rememberSaveable { mutableStateOf<String?>(null) }
    var openDiseaseSubOption by rememberSaveable { mutableStateOf<String?>(null) }
    var openWeightSubOption by rememberSaveable { mutableStateOf<String?>(null) }
    var openTrainingCategory by rememberSaveable { mutableStateOf<String?>(null) }

    // HR dialog states
    var showAddStaffDialog by remember { mutableStateOf(false) }
    var staffToEdit by remember { mutableStateOf<StaffMember?>(null) }
    var staffToPay by remember { mutableStateOf<StaffMember?>(null) }
    var staffToViewProfileId by remember { mutableStateOf<String?>(null) }
    var staffToViewFullDetails by remember { mutableStateOf<StaffMember?>(null) }

    val settingsCurrencySymbol by SettingsViewModel.getInstance().currencySymbol.collectAsState()
    val currencySymbol = remember(userCountry, settingsCurrencySymbol) {
        val countrySymbol = if (userCountry.isNotBlank()) CountryCurrencyHelper.getCurrencyForCountry(userCountry).symbol else ""
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

    LaunchedEffect(targetSubOption, expandedSection) {
        if (expandedSection == "weight_checker" && targetSubOption != null) {
            openWeightSubOption = targetSubOption
        }
        if (expandedSection == "human_resources" && targetSubOption == "add") {
            showAddStaffDialog = true
            onSubOptionChange(null)
        }
    }

    val categories = remember(currentLanguage, isFinancialsRestricted) {
        listOf(
            AccordionCategory(
                id = "herd_data",
                label = Translator.getString("herd_data", currentLanguage.code),
                subtitle = Translator.getString("sub_herd_data", currentLanguage.code),
                iconResId = R.drawable.ic_herd_data,
                screen = Screen.HerdData,
                themeColor = Color(0xFF2E7D32),
                themeColorDark = Color(0xFF81C784),
                bgColorLight = Color(0xFFE8F5E9),
                bgColorDark = Color(0xFF1B5E20)
            ),
            AccordionCategory(
                id = "feed",
                label = Translator.getString("feed", currentLanguage.code),
                subtitle = Translator.getString("sub_feed", currentLanguage.code),
                iconResId = R.drawable.ic_feed2,
                screen = Screen.Feed,
                themeColor = Color(0xFFE65100),
                themeColorDark = Color(0xFFFFB74D),
                bgColorLight = Color(0xFFFFF3E0),
                bgColorDark = Color(0xFFE65100)
            ),
            AccordionCategory(
                id = "weight_checker",
                label = Translator.getString("weigh_pigs", currentLanguage.code, Translator.getString("weight_checker", currentLanguage.code, "Weigh Pigs")),
                subtitle = Translator.getString("sub_weight_checker", currentLanguage.code),
                iconResId = R.drawable.ic_weight_checker,
                screen = Screen.WeightChecker,
                themeColor = Color(0xFF455A64),
                themeColorDark = Color(0xFF90A4AE),
                bgColorLight = Color(0xFFECEFF1),
                bgColorDark = Color(0xFF37474F)
            ),
            AccordionCategory(
                id = "symptoms_analyzer",
                label = Translator.getString("disease_finder", currentLanguage.code, Translator.getString("symptoms_analyzer", currentLanguage.code, "Disease Finder")),
                subtitle = Translator.getString("sub_symptoms_analyzer", currentLanguage.code),
                iconResId = R.drawable.ic_symptoms_analyzer,
                screen = Screen.DiseaseFinder,
                themeColor = Color(0xFF3F51B5),
                themeColorDark = Color(0xFF7986CB),
                bgColorLight = Color(0xFFE8EAF6),
                bgColorDark = Color(0xFF1A237E)
            ),
            AccordionCategory(
                id = "production_activities",
                label = Translator.getString("herd_activities", currentLanguage.code),
                subtitle = Translator.getString("sub_herd_activities", currentLanguage.code),
                iconResId = R.drawable.ic_herd_activities,
                screen = Screen.ProductionActivities,
                themeColor = Color(0xFF00838F),
                themeColorDark = Color(0xFF4DD0E1),
                bgColorLight = Color(0xFFE0F7FA),
                bgColorDark = Color(0xFF006064)
            ),
            AccordionCategory(
                id = "financials",
                label = Translator.getString("financials", currentLanguage.code),
                subtitle = Translator.getString("sub_financials", currentLanguage.code),
                icon = Icons.Default.Payments,
                screen = Screen.Financials,
                themeColor = Color(0xFF00796B),
                themeColorDark = Color(0xFF4DB6AC),
                bgColorLight = Color(0xFFE0F2F1),
                bgColorDark = Color(0xFF004D40)
            ),
            AccordionCategory(
                id = "human_resources",
                label = Translator.getString("human_resources", currentLanguage.code),
                subtitle = Translator.getString("sub_human_resources", currentLanguage.code),
                icon = Icons.Default.Groups,
                screen = Screen.HumanResource,
                themeColor = Color(0xFF7B1FA2),
                themeColorDark = Color(0xFFBA68C8),
                bgColorLight = Color(0xFFF3E5F5),
                bgColorDark = Color(0xFF4A148C)
            ),
            AccordionCategory(
                id = "market",
                label = Translator.getString("market_hub", currentLanguage.code, Translator.getString("market_links", currentLanguage.code, "Market Hub")),
                subtitle = Translator.getString("sub_market", currentLanguage.code),
                icon = Icons.Default.Storefront,
                screen = Screen.MarketAccess,
                themeColor = Color(0xFFC2185B),
                themeColorDark = Color(0xFFF06292),
                bgColorLight = Color(0xFFFFEBEE),
                bgColorDark = Color(0xFF880E4F)
            ),
            AccordionCategory(
                id = "training",
                label = Translator.getString("pig_farming_tips", currentLanguage.code, Translator.getString("training", currentLanguage.code, "Pig Farming Tips")),
                subtitle = Translator.getString("sub_training", currentLanguage.code),
                icon = Icons.Default.School,
                screen = Screen.Training,
                themeColor = Color(0xFF5D4037),
                themeColorDark = Color(0xFFA1887F),
                bgColorLight = Color(0xFFEFEBE9),
                bgColorDark = Color(0xFF3E2723)
            )
        ).filter { !(isFinancialsRestricted && (it.id == "financials" || it.id == "human_resources")) }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        categories.forEach { item ->
            if (item.id == "financials" && !isPaidPremium) {
                NativeAdCard()
            }
            val isExpanded = expandedSection == item.id
            val isLocked = item.screen == Screen.DiseaseFinder && !isPremium
            val isDark = MaterialTheme.colorScheme.background == DarkBackground
            val primaryColor = if (isDark) item.themeColorDark else item.themeColor
            val bgColor = if (isDark) item.bgColorDark else item.bgColorLight

            val rotationAngle by animateFloatAsState(
                targetValue = if (isExpanded) 180f else 0f,
                label = "chevronRotation"
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { onRegisterCoordinates(item.id, it) }
                    .shadow(
                        elevation = if (isExpanded) 8.dp else 3.dp,
                        shape = RoundedCornerShape(20.dp),
                        ambientColor = primaryColor.copy(alpha = 0.2f),
                        spotColor = primaryColor.copy(alpha = 0.35f)
                    )
                    .border(
                        BorderStroke(
                            width = if (isExpanded) 1.5.dp else 1.dp,
                            color = primaryColor.copy(alpha = if (isExpanded) 0.8f else 0.35f)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White
                )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header Bar (Clickable) - Clean and Bold without subtitle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isLocked) {
                                    showDiseaseFinderPassDialog = true
                                } else {
                                    val willExpand = expandedSection != item.id
                                    onToggleSection(item.id)
                                    if (willExpand) {
                                        onItemExpanded(item.id)
                                    }
                                }
                            }
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                modifier = Modifier.size(58.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = bgColor.copy(alpha = if (isDark) 0.35f else 0.85f),
                                border = BorderStroke(1.2.dp, primaryColor.copy(alpha = 0.35f))
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    item.icon?.let { icon ->
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp),
                                            tint = primaryColor
                                        )
                                    } ?: item.iconResId?.let { resId ->
                                        Icon(
                                            painter = painterResource(id = resId),
                                            contentDescription = null,
                                            modifier = Modifier.size(32.dp),
                                            tint = primaryColor
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(18.dp))

                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 22.sp,
                                    letterSpacing = (-0.3).sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f, fill = false),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        if (isLocked) {
                            IconButton(onClick = { showDiseaseFinderPassDialog = true }, modifier = Modifier.size(40.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Locked",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    val willExpand = expandedSection != item.id
                                    onToggleSection(item.id)
                                    if (willExpand) {
                                        onItemExpanded(item.id)
                                    }
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                                    tint = primaryColor,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .rotate(rotationAngle)
                                )
                            }
                        }
                    }

                    // Expanded Content Panel
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically(animationSpec = tween(220)) + fadeIn(animationSpec = tween(220)),
                        exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(animationSpec = tween(150))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(bgColor.copy(alpha = if (isDark) 0.15f else 0.35f))
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            when (item.id) {
                                "herd_data" -> {
                                    HerdSectionContent(
                                        allPigs = allPigs,
                                        herdStats = herdStats,
                                        primaryColor = primaryColor,
                                        onAddPigClick = onAddPigClick,
                                        onNavigateTo = onNavigateTo
                                    )
                                }

                                "feed" -> {
                                    FeedSectionContent(
                                        primaryColor = primaryColor,
                                        onShowFeedCalculator = onShowFeedCalculator,
                                        onShowFeedFormulator = onShowFeedFormulator,
                                        onNavigateTo = onNavigateTo
                                    )
                                }

                                "production_activities" -> {
                                    ProductionSectionContent(
                                        primaryColor = primaryColor,
                                        onNavigateTo = onNavigateTo
                                    )
                                }

                                "financials" -> {
                                    FinancialsSectionContent(
                                        financialRecords = financialRecords,
                                        currencySymbol = currencySymbol,
                                        primaryColor = primaryColor,
                                        onNavigateTo = onNavigateTo
                                    )
                                }

                                "human_resources" -> {
                                    HumanResourcesSectionContent(
                                        staff = staff,
                                        financialRecords = financialRecords,
                                        currencySymbol = currencySymbol,
                                        primaryColor = primaryColor,
                                        isPremium = isPremium,
                                        isPaidPremium = isPaidPremium,
                                        onAddStaffClick = { showAddStaffDialog = true },
                                        onExportPdfClick = {
                                            if (!isPremium) {
                                                showHRPdfPassDialog = true
                                            } else {
                                                PdfGenerator.generateHRReportPdf(
                                                    context = context,
                                                    staff = staff,
                                                    currencySymbol = currencySymbol,
                                                    financialRecords = financialRecords,
                                                    languageCode = currentLanguage.code
                                                )
                                            }
                                        },
                                        onViewStaffProfile = { staffToViewProfileId = it },
                                        onPaySalary = { staffToPay = it },
                                        onEditStaff = { staffToEdit = it },
                                        onNavigateTo = onNavigateTo
                                    )
                                }

                                "market" -> {
                                    MarketSectionContent(
                                        userCountry = userCountry,
                                        openMarketSubOption = openMarketSubOption,
                                        onToggleSubOption = { sub ->
                                            openMarketSubOption = if (openMarketSubOption == sub) null else sub
                                        },
                                        primaryColor = primaryColor,
                                        onRegisterCoordinates = onRegisterCoordinates,
                                        onItemExpanded = onItemExpanded
                                    )
                                }

                                "symptoms_analyzer" -> {
                                    DiseaseSectionContent(
                                        allPigs = allPigs,
                                        openDiseaseSubOption = openDiseaseSubOption,
                                        onToggleSubOption = { sub ->
                                            openDiseaseSubOption = if (openDiseaseSubOption == sub) null else sub
                                        },
                                        primaryColor = primaryColor,
                                        onRegisterCoordinates = onRegisterCoordinates,
                                        onItemExpanded = onItemExpanded,
                                        onLogHealthActivity = onLogHealthActivity
                                    )
                                }

                                "weight_checker" -> {
                                    WeightSectionContent(
                                        allPigs = allPigs,
                                        targetPigTag = targetPigTag,
                                        openWeightSubOption = openWeightSubOption,
                                        onToggleSubOption = { sub ->
                                            openWeightSubOption = if (openWeightSubOption == sub) null else sub
                                        },
                                        onSubOptionChange = onSubOptionChange,
                                        primaryColor = primaryColor,
                                        onRegisterCoordinates = onRegisterCoordinates,
                                        onItemExpanded = onItemExpanded,
                                        onUpdatePigWeight = onUpdatePigWeight
                                    )
                                }

                                "training" -> {
                                    TrainingSectionContent(
                                        openTrainingCategory = openTrainingCategory,
                                        onToggleCategory = { cat ->
                                            openTrainingCategory = if (openTrainingCategory == cat) null else cat
                                        },
                                        primaryColor = primaryColor,
                                        onRegisterCoordinates = onRegisterCoordinates,
                                        onItemExpanded = onItemExpanded
                                    )
                                }

                                else -> {
                                    Button(
                                        onClick = { onNavigateTo(item.screen.route) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Text(item.label, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Spacer(Modifier.width(8.dp))
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddStaffDialog) {
        AddEditStaffDialog(
            member = null,
            onDismiss = { showAddStaffDialog = false },
            onConfirm = { newStaff ->
                onAddStaff(newStaff)
                showAddStaffDialog = false
            }
        )
    }

    staffToEdit?.let { member ->
        AddEditStaffDialog(
            member = member,
            onDismiss = { staffToEdit = null },
            onConfirm = { updated ->
                onUpdateStaff(updated)
                staffToEdit = null
            },
            onArchive = { archived ->
                onArchiveStaff(archived)
                staffToEdit = null
            }
        )
    }

    staffToPay?.let { member ->
        LogSalaryDialog(
            member = member,
            onDismiss = { staffToPay = null },
            onConfirm = { staffMember, date, notes, base, bonus, total ->
                onPaySalary(staffMember, date, notes, base, bonus, total)
                staffToPay = null
            }
        )
    }

    staffToViewProfileId?.let { id ->
        val member = staff.find { it.id == id }
        if (member != null) {
            StaffProfileDialog(
                member = member,
                onDismiss = { staffToViewProfileId = null },
                onViewDetails = { selected ->
                    staffToViewProfileId = null
                    staffToViewFullDetails = selected
                }
            )
        }
    }

    staffToViewFullDetails?.let { member ->
        StaffFullDetailsDialog(
            member = member,
            financialRecords = financialRecords,
            currencySymbol = currencySymbol,
            onDismiss = { staffToViewFullDetails = null },
            onExportPdf = {
                if (!isPremium) {
                    showHRPdfPassDialog = true
                } else {
                    PdfGenerator.generateStaffDetailPdf(
                        context = context,
                        member = member,
                        currencySymbol = currencySymbol,
                        financialRecords = financialRecords,
                        languageCode = currentLanguage.code
                    )
                }
            }
        )
    }

    if (showHRPdfPassDialog) {
        RewardedPassDialog(
            title = stringResource("unlock_hr_reports_title"),
            description = stringResource("unlock_hr_reports_desc"),
            onDismiss = { showHRPdfPassDialog = false },
            onNavigateToPaywall = { onNavigateTo(Screen.Paywall.route) },
            onPassActivated = {
                // Pass activated
            }
        )
    }

    if (showDiseaseFinderPassDialog) {
        RewardedPassDialog(
            title = stringResource("unlock_disease_finder_title"),
            description = stringResource("unlock_disease_finder_desc"),
            onDismiss = { showDiseaseFinderPassDialog = false },
            onNavigateToPaywall = {
                showDiseaseFinderPassDialog = false
                onNavigateTo(Screen.Paywall.route)
            },
            onPassActivated = {
                showDiseaseFinderPassDialog = false
                onToggleSection("symptoms_analyzer")
                onItemExpanded("symptoms_analyzer")
            }
        )
    }
}
