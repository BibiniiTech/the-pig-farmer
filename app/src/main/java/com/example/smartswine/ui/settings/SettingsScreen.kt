package com.example.smartswine.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.painterResource
import com.bibiniitech.smartswine.R
import com.example.smartswine.ui.theme.ThemeViewModel
import com.example.smartswine.ui.theme.SmartSwineTheme
import com.example.smartswine.utils.NotificationWorker
import com.example.smartswine.utils.AppLanguage
import com.example.smartswine.utils.LanguageSelectionGrid
import com.example.smartswine.utils.LanguageViewModel
import com.example.smartswine.utils.StylishDivider
import com.example.smartswine.utils.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.sp
import com.example.smartswine.utils.LocalAppLanguage
import com.example.smartswine.ui.auth.AuthViewModel
import com.example.smartswine.utils.GlobalNotice
import com.example.smartswine.ui.components.WhatsNewDialog
import com.example.smartswine.ui.components.PlayStoreReviewManager
import com.bibiniitech.smartswine.BuildConfig

data class SettingsState(
    val isDarkMode: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val weaningDays: String = "56",
    val farrowingDays: String = "114",
    val ironDay1: String = "3",
    val ironDay2: String = "10",
    val porkerUseAge: Boolean = true,
    val porkerStarterAge: String = "16",
    val porkerGrowerAge: String = "24",
    val porkerStarterWeight: String = "25",
    val porkerGrowerWeight: String = "60",
    val breederUseAge: Boolean = true,
    val breederPigletAge: String = "8",
    val breederWeanerAge: String = "16",
    val breederGrowerAge: String = "24",
    val breederPigletWeight: String = "10",
    val breederWeanerWeight: String = "25",
    val breederGrowerWeight: String = "60",
    val autoClassifyBarrows: Boolean = true,
    val autoClassifySows: Boolean = true,
    val giltAgeThresholdWeeks: String = "26",
    val selectedCurrency: String = "USD",
    val currencySymbol: String = "$",
    val isSyncing: Boolean = false,
    val lastSyncTime: String? = null,
)

data class SettingsActions(
    val onBack: () -> Unit = {},
    val onNavigateToEditProfile: () -> Unit = {},
    val onNavigateToTerms: () -> Unit = {},
    val onNavigateToGuide: () -> Unit = {},
    val onDarkModeChange: (Boolean) -> Unit = {},
    val onNotificationsEnabledChange: (Boolean) -> Unit = {},
    val onWeaningDaysChange: (String) -> Unit = {},
    val onFarrowingDaysChange: (String) -> Unit = {},
    val onIronDay1Change: (String) -> Unit = {},
    val onIronDay2Change: (String) -> Unit = {},
    val onPorkerUseAgeChange: (Boolean) -> Unit = {},
    val onPorkerStarterAgeChange: (String) -> Unit = {},
    val onPorkerGrowerAgeChange: (String) -> Unit = {},
    val onPorkerStarterWeightChange: (String) -> Unit = {},
    val onPorkerGrowerWeightChange: (String) -> Unit = {},
    val onBreederUseAgeChange: (Boolean) -> Unit = {},
    val onBreederPigletAgeChange: (String) -> Unit = {},
    val onBreederWeanerAgeChange: (String) -> Unit = {},
    val onBreederGrowerAgeChange: (String) -> Unit = {},
    val onBreederPigletWeightChange: (String) -> Unit = {},
    val onBreederWeanerWeightChange: (String) -> Unit = {},
    val onBreederGrowerWeightChange: (String) -> Unit = {},
    val onAutoClassifyBarrowsChange: (Boolean) -> Unit = {},
    val onAutoClassifySowsChange: (Boolean) -> Unit = {},
    val onGiltAgeThresholdWeeksChange: (String) -> Unit = {},
    val onUpdateCurrency: (String) -> Unit = {},
    val onSaveSettings: () -> Unit = {},
    val onClearCollection: (String, () -> Unit) -> Unit = { _, _ -> },
    val onFactoryReset: (() -> Unit) -> Unit = {},
    val onDeleteAccount: (() -> Unit) -> Unit = {},
    val onLanguageChange: (AppLanguage) -> Unit = {}
)

@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    onNavigateToEditProfile: () -> Unit,
    onNavigateToTerms: () -> Unit,
    onNavigateToGuide: () -> Unit = {},
    themeViewModel: ThemeViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
    languageViewModel: LanguageViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val isDarkMode by themeViewModel.isDarkMode.collectAsStateWithLifecycle()
    
    val notificationsEnabled by settingsViewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val weaningDays by settingsViewModel.weaningDays.collectAsStateWithLifecycle()
    val farrowingDays by settingsViewModel.farrowingDays.collectAsStateWithLifecycle()
    val ironDay1 by settingsViewModel.ironDay1.collectAsStateWithLifecycle()
    val ironDay2 by settingsViewModel.ironDay2.collectAsStateWithLifecycle()
    
    val porkerUseAge by settingsViewModel.porkerUseAge.collectAsStateWithLifecycle()
    val porkerStarterAge by settingsViewModel.porkerStarterAge.collectAsStateWithLifecycle()
    val porkerGrowerAge by settingsViewModel.porkerGrowerAge.collectAsStateWithLifecycle()
    val porkerStarterWeight by settingsViewModel.porkerStarterWeight.collectAsStateWithLifecycle()
    val porkerGrowerWeight by settingsViewModel.porkerGrowerWeight.collectAsStateWithLifecycle()
    
    val breederUseAge by settingsViewModel.breederUseAge.collectAsStateWithLifecycle()
    val breederPigletAge by settingsViewModel.breederPigletAge.collectAsStateWithLifecycle()
    val breederWeanerAge by settingsViewModel.breederWeanerAge.collectAsStateWithLifecycle()
    val breederGrowerAge by settingsViewModel.breederGrowerAge.collectAsStateWithLifecycle()
    val breederPigletWeight by settingsViewModel.breederPigletWeight.collectAsStateWithLifecycle()
    val breederWeanerWeight by settingsViewModel.breederWeanerWeight.collectAsStateWithLifecycle()
    val breederGrowerWeight by settingsViewModel.breederGrowerWeight.collectAsStateWithLifecycle()

    val autoClassifyBarrows by settingsViewModel.autoClassifyBarrows.collectAsStateWithLifecycle()
    val autoClassifySows by settingsViewModel.autoClassifySows.collectAsStateWithLifecycle()
    val giltAgeThresholdWeeks by settingsViewModel.giltAgeThresholdWeeks.collectAsStateWithLifecycle()
    val selectedCurrency by settingsViewModel.selectedCurrency.collectAsStateWithLifecycle()
    val currencySymbol by settingsViewModel.currencySymbol.collectAsStateWithLifecycle()

    val isSyncing by settingsViewModel.isSyncing.collectAsStateWithLifecycle()
    val lastSyncTime by settingsViewModel.lastSyncTime.collectAsStateWithLifecycle()

    val state = SettingsState(
        isDarkMode = isDarkMode,
        notificationsEnabled = notificationsEnabled,
        weaningDays = weaningDays,
        farrowingDays = farrowingDays,
        ironDay1 = ironDay1,
        ironDay2 = ironDay2,
        porkerUseAge = porkerUseAge,
        porkerStarterAge = porkerStarterAge,
        porkerGrowerAge = porkerGrowerAge,
        porkerStarterWeight = porkerStarterWeight,
        porkerGrowerWeight = porkerGrowerWeight,
        breederUseAge = breederUseAge,
        breederPigletAge = breederPigletAge,
        breederWeanerAge = breederWeanerAge,
        breederGrowerAge = breederGrowerAge,
        breederPigletWeight = breederPigletWeight,
        breederWeanerWeight = breederWeanerWeight,
        breederGrowerWeight = breederGrowerWeight,
        autoClassifyBarrows = autoClassifyBarrows,
        autoClassifySows = autoClassifySows,
        giltAgeThresholdWeeks = giltAgeThresholdWeeks,
        selectedCurrency = selectedCurrency,
        currencySymbol = currencySymbol,
        isSyncing = isSyncing,
        lastSyncTime = lastSyncTime
    )

    val actions = SettingsActions(
        onBack = onBack,
        onNavigateToEditProfile = onNavigateToEditProfile,
        onNavigateToTerms = onNavigateToTerms,
        onNavigateToGuide = onNavigateToGuide,
        onDarkModeChange = { themeViewModel.toggleDarkMode(it) },
        onNotificationsEnabledChange = { enabled ->
            settingsViewModel.notificationsEnabled.value = enabled
            if (enabled) {
                NotificationWorker.schedule(context)
            } else {
                NotificationWorker.cancel(context)
            }
        },
        onWeaningDaysChange = { settingsViewModel.weaningDays.value = it },
        onFarrowingDaysChange = { settingsViewModel.farrowingDays.value = it },
        onIronDay1Change = { settingsViewModel.ironDay1.value = it },
        onIronDay2Change = { settingsViewModel.ironDay2.value = it },
        onPorkerUseAgeChange = { settingsViewModel.porkerUseAge.value = it },
        onPorkerStarterAgeChange = { settingsViewModel.porkerStarterAge.value = it },
        onPorkerGrowerAgeChange = { settingsViewModel.porkerGrowerAge.value = it },
        onPorkerStarterWeightChange = { settingsViewModel.porkerStarterWeight.value = it },
        onPorkerGrowerWeightChange = { settingsViewModel.porkerGrowerWeight.value = it },
        onBreederUseAgeChange = { settingsViewModel.breederUseAge.value = it },
        onBreederPigletAgeChange = { settingsViewModel.breederPigletAge.value = it },
        onBreederWeanerAgeChange = { settingsViewModel.breederWeanerAge.value = it },
        onBreederGrowerAgeChange = { settingsViewModel.breederGrowerAge.value = it },
        onBreederPigletWeightChange = { settingsViewModel.breederPigletWeight.value = it },
        onBreederWeanerWeightChange = { settingsViewModel.breederWeanerWeight.value = it },
        onBreederGrowerWeightChange = { settingsViewModel.breederGrowerWeight.value = it },
        onAutoClassifyBarrowsChange = { settingsViewModel.autoClassifyBarrows.value = it },
        onAutoClassifySowsChange = { settingsViewModel.autoClassifySows.value = it },
        onGiltAgeThresholdWeeksChange = { settingsViewModel.giltAgeThresholdWeeks.value = it },
        onUpdateCurrency = { settingsViewModel.updateCurrency(it) },
        onSaveSettings = { settingsViewModel.saveSettings() },
        onClearCollection = { type, onComplete ->
            settingsViewModel.clearCollection(type) {
                if (type == "staff") {
                    settingsViewModel.clearCollection("salaries") {}
                }
                if (type == "ingredients") {
                    settingsViewModel.clearCollection("requirements") {}
                }
                onComplete()
            }
        },
        onFactoryReset = { onComplete -> settingsViewModel.factoryReset(onComplete) },
        onDeleteAccount = { onComplete ->
            authViewModel.deleteAccount { success, error ->
                if (!success) {
                    GlobalNotice.show(error ?: "Failed to delete account")
                }
                onComplete()
            }
        }
    ) { languageViewModel.setLanguage(it) }

    SettingsScreenContent(state = state, actions = actions)
}

@Composable
fun SettingsScreenContent(
    state: SettingsState,
    actions: SettingsActions
) {
    val scrollState = rememberScrollState()
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    val currentAppLanguage = LocalAppLanguage.current
    var showPreferredDatesDialog by remember { mutableStateOf(false) }
    var showSetStatusDialog by remember { mutableStateOf(false) }
    var showPorkersDialog by remember { mutableStateOf(false) }
    var showBreedersDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showWhatsNewDialog by remember { mutableStateOf(false) }
    var clearType by remember { mutableStateOf("") } // "pigs", "financials", "ingredients", "staff", "all", "account"

    Scaffold(
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = actions.onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource("back")
                        )
                    }
                    Text(
                        text = stringResource("settings"),
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp),
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }
                StylishDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .imePadding()
        ) {

        // ─── 1. HEADER: EDIT PROFILE ─────────────────────────────────────────
        SettingsSection(title = stringResource("account")) {
            SettingsItem(
                icon = Icons.Default.Person,
                title = stringResource("edit_profile"),
                subtitle = stringResource("edit_profile_subtitle"),
                onClick = actions.onNavigateToEditProfile
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ─── 2. DARK THEME & LANGUAGE (SIDE-BY-SIDE ON SAME LINE) ───────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Dark theme toggle pill
            Surface(
                onClick = { actions.onDarkModeChange(!state.isDarkMode) },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (state.isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = if (state.isDarkMode) Color(0xFFFFD54F) else Color(0xFFFFB74D),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = if (state.isDarkMode) stringResource("dark") else stringResource("light"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Switch(
                        checked = state.isDarkMode,
                        onCheckedChange = { actions.onDarkModeChange(it) },
                        modifier = Modifier
                            .scale(0.85f)
                            .height(26.dp)
                    )
                }
            }

            // Language flag dropdown
            var showLangMenu by remember { mutableStateOf(false) }
            Box {
                Surface(
                    onClick = { showLangMenu = true },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = currentAppLanguage.flag, fontSize = 22.sp)
                        Text(
                            text = currentAppLanguage.code.uppercase(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Language",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = showLangMenu,
                    onDismissRequest = { showLangMenu = false }
                ) {
                    AppLanguage.values().forEach { lang ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(text = lang.flag, fontSize = 20.sp)
                                    Text(
                                        text = lang.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontSize = 16.sp,
                                        fontWeight = if (lang == currentAppLanguage) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            },
                            onClick = {
                                showLangMenu = false
                                actions.onLanguageChange(lang)
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ─── 3. PREFERRED DATES (POPUP TRIGGER) ──────────────────────────────
        SettingsSection(title = stringResource("preferred_dates")) {
            SettingsItem(
                icon = Icons.Default.DateRange,
                title = stringResource("preferred_dates"),
                subtitle = stringResource("preferred_dates_subtitle"),
                onClick = { showPreferredDatesDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ─── 4. SET STATUS (POPUP TRIGGER) ───────────────────────────────────
        SettingsSection(title = stringResource("set_status")) {
            SettingsItem(
                icon = Icons.Default.Tune,
                title = stringResource("set_status"),
                subtitle = stringResource("set_status_subtitle"),
                onClick = { showSetStatusDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ─── 5. DATA MANAGEMENT ──────────────────────────────────────────────
        SettingsSection(title = stringResource("data_management")) {
            SettingsItem(
                icon = if (state.isSyncing) Icons.Default.Sync else Icons.Default.CloudDone,
                title = if (state.isSyncing) stringResource("syncing") else stringResource("sync_with_cloud"),
                subtitle = if (state.isSyncing) null else stringResource("last_synced", state.lastSyncTime ?: stringResource("never")),
                onClick = actions.onSaveSettings
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            SettingsItem(
                icon = Icons.Default.DeleteSweep,
                title = stringResource("clear_herd_data"),
                subtitle = stringResource("clear_herd_subtitle"),
                onClick = { clearType = "pigs"; showClearDialog = true }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            SettingsItem(
                icon = Icons.Default.AccountBalanceWallet,
                title = stringResource("clear_financials"),
                subtitle = stringResource("clear_financials_subtitle"),
                onClick = { clearType = "financials"; showClearDialog = true }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            SettingsItem(
                icon = Icons.Default.Agriculture,
                title = stringResource("clear_feed_data"),
                subtitle = stringResource("clear_feed_subtitle"),
                onClick = { clearType = "ingredients"; showClearDialog = true }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            SettingsItem(
                icon = Icons.Default.Badge,
                title = stringResource("clear_hr_staff"),
                subtitle = stringResource("clear_hr_subtitle"),
                onClick = { clearType = "staff"; showClearDialog = true }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            SettingsItem(
                icon = Icons.Default.Warning,
                title = stringResource("factory_reset"),
                subtitle = stringResource("factory_reset_subtitle"),
                textColor = MaterialTheme.colorScheme.error,
                onClick = { clearType = "all"; showClearDialog = true }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            SettingsItem(
                icon = Icons.Default.PersonRemove,
                title = stringResource("delete_account"),
                subtitle = stringResource("delete_account_subtitle"),
                textColor = MaterialTheme.colorScheme.error,
                onClick = { clearType = "account"; showClearDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ─── 6. LEGAL & POLICIES (STANDALONE) ────────────────────────────────
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                SettingsItem(
                    icon = Icons.Default.MenuBook,
                    title = stringResource("how_to_guide"),
                    onClick = actions.onNavigateToGuide
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                SettingsItem(
                    icon = Icons.Default.AutoAwesome,
                    title = stringResource("whats_new_title"),
                    onClick = { showWhatsNewDialog = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                SettingsItem(
                    icon = Icons.Default.Star,
                    title = stringResource("rate_5_stars"),
                    subtitle = "SmartSwine on Google Play Store",
                    onClick = { PlayStoreReviewManager.openPlayStoreForReview(context) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                SettingsItem(
                    icon = Icons.Default.Description,
                    title = stringResource("terms_of_service"),
                    onClick = actions.onNavigateToTerms
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                SettingsItem(
                    icon = Icons.Default.PrivacyTip,
                    title = stringResource("privacy_policy"),
                    onClick = { uriHandler.openUri("https://sites.google.com/view/smartswine-privacypolicy/home") }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "v${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource("developed_by"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Text(
                text = "Goshen Agrifirm &\nBibinii Tech",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable {
                    uriHandler.openUri("https://wa.me/233544737870")
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "WhatsApp",
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFF25D366)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "+233544737870",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable {
                    uriHandler.openUri("https://t.me/BibiniiTech")
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Telegram",
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFF24A1DE)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "t.me/BibiniiTech",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource("app_copyright", "2026"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.height(140.dp))
    }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // POPUP DIALOGS
    // ═════════════════════════════════════════════════════════════════════════

    if (showWhatsNewDialog) {
        WhatsNewDialog(
            onDismiss = { showWhatsNewDialog = false },
            onExploreGuide = {
                showWhatsNewDialog = false
                actions.onNavigateToGuide()
            }
        )
    }

    // ─── A. PREFERRED DATES POPUP DIALOG ──────────────────────────────────────
    if (showPreferredDatesDialog) {
        AlertDialog(
            onDismissRequest = {
                actions.onSaveSettings()
                showPreferredDatesDialog = false
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource("preferred_dates"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp)
                        .verticalScroll(rememberScrollState())
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Weaning Card
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(painter = painterResource(id = R.drawable.ic_weaning), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(stringResource("weaning"), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp))
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource("weaning_explanation"),
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource("wean_at"), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp), modifier = Modifier.weight(1f))
                                OutlinedTextField(
                                    value = state.weaningDays,
                                    onValueChange = actions.onWeaningDaysChange,
                                    modifier = Modifier.width(88.dp),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp, textAlign = TextAlign.Center),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource("days"), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp))
                            }
                        }
                    }

                    // Farrowing Card
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(painter = painterResource(id = R.drawable.ic_farrowing), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(stringResource("farrowing"), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp))
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource("farrowing_explanation"),
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource("sows_farrow_at"), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp), modifier = Modifier.weight(1f))
                                OutlinedTextField(
                                    value = state.farrowingDays,
                                    onValueChange = actions.onFarrowingDaysChange,
                                    modifier = Modifier.width(88.dp),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp, textAlign = TextAlign.Center),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource("days"), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp))
                            }
                        }
                    }

                    // Iron Injection Card
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(painter = painterResource(id = R.drawable.ic_iron), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(stringResource("iron_injection"), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp))
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource("iron_explanation"),
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(stringResource("iron_day_1_first_dose"), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedTextField(
                                            value = state.ironDay1,
                                            onValueChange = actions.onIronDay1Change,
                                            modifier = Modifier.width(78.dp),
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp, textAlign = TextAlign.Center),
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(stringResource("days"), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp))
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(stringResource("iron_day_2_second_dose"), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedTextField(
                                            value = state.ironDay2,
                                            onValueChange = actions.onIronDay2Change,
                                            modifier = Modifier.width(78.dp),
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp, textAlign = TextAlign.Center),
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(stringResource("days"), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        actions.onSaveSettings()
                        showPreferredDatesDialog = false
                    }
                ) {
                    Text(stringResource("save"), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPreferredDatesDialog = false }) {
                    Text(stringResource("cancel"), fontSize = 16.sp)
                }
            }
        )
    }

    // ─── B. SET STATUS SELECTION POPUP DIALOG ────────────────────────────────
    if (showSetStatusDialog) {
        AlertDialog(
            onDismissRequest = { showSetStatusDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource("set_status"), style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp), fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource("set_status_subtitle"),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Option 1: Porkers
                    Surface(
                        onClick = {
                            showSetStatusDialog = false
                            showPorkersDialog = true
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Scale, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = stringResource("porkers"), style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp), fontWeight = FontWeight.Bold)
                                Text(text = stringResource("porkers_subtitle"), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(22.dp))
                        }
                    }

                    // Option 2: Breeders
                    Surface(
                        onClick = {
                            showSetStatusDialog = false
                            showBreedersDialog = true
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Pets, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = stringResource("breeders"), style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp), fontWeight = FontWeight.Bold)
                                Text(text = stringResource("breeders_subtitle"), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSetStatusDialog = false }) {
                    Text(stringResource("cancel"), fontSize = 16.sp)
                }
            }
        )
    }

    // ─── C. PORKERS STATUS DETAILS POPUP DIALOG ──────────────────────────────
    if (showPorkersDialog) {
        AlertDialog(
            onDismissRequest = {
                actions.onSaveSettings()
                showPorkersDialog = false
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Scale, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource("porker_status"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = stringResource("porkers_explanation"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Age vs Weight Mode Switch
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                stringResource("age_weeks"),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (state.porkerUseAge) FontWeight.Bold else FontWeight.Normal,
                                color = if (state.porkerUseAge) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Switch(checked = !state.porkerUseAge, onCheckedChange = { actions.onPorkerUseAgeChange(!it) })
                            Text(
                                stringResource("weight_kg"),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (!state.porkerUseAge) FontWeight.Bold else FontWeight.Normal,
                                color = if (!state.porkerUseAge) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Thresholds
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column {
                            if (state.porkerUseAge) {
                                StatusRangeItem(label = stringResource("starter"), range = "0 to ", value = state.porkerStarterAge, onValueChange = actions.onPorkerStarterAgeChange, unit = stringResource("weeks"))
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                                StatusRangeItem(label = stringResource("grower"), range = "${state.porkerStarterAge} to ", value = state.porkerGrowerAge, onValueChange = actions.onPorkerGrowerAgeChange, unit = stringResource("weeks"))
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                                StatusRangeItem(label = stringResource("finisher"), range = "${state.porkerGrowerAge} ", value = stringResource("above"), onValueChange = {}, readOnly = true, unit = stringResource("weeks"))
                            } else {
                                StatusRangeItem(label = stringResource("starter"), range = "0 to ", value = state.porkerStarterWeight, onValueChange = actions.onPorkerStarterWeightChange, unit = stringResource("kg"))
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                                StatusRangeItem(label = stringResource("grower"), range = "${state.porkerStarterWeight} to ", value = state.porkerGrowerWeight, onValueChange = actions.onPorkerGrowerWeightChange, unit = stringResource("kg"))
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                                StatusRangeItem(label = stringResource("finisher"), range = "${state.porkerGrowerWeight} ", value = stringResource("above"), onValueChange = {}, readOnly = true, unit = stringResource("kg"))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        actions.onSaveSettings()
                        showPorkersDialog = false
                    }
                ) {
                    Text(stringResource("save"))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPorkersDialog = false
                        showSetStatusDialog = true
                    }
                ) {
                    Text(stringResource("previous"))
                }
            }
        )
    }

    // ─── D. BREEDERS STATUS DETAILS POPUP DIALOG ─────────────────────────────
    if (showBreedersDialog) {
        AlertDialog(
            onDismissRequest = {
                actions.onSaveSettings()
                showBreedersDialog = false
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Pets, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource("breeder_status"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = stringResource("breeders_explanation"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Age vs Weight Mode Switch
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                stringResource("age_weeks"),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (state.breederUseAge) FontWeight.Bold else FontWeight.Normal,
                                color = if (state.breederUseAge) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Switch(checked = !state.breederUseAge, onCheckedChange = { actions.onBreederUseAgeChange(!it) })
                            Text(
                                stringResource("weight_kg"),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (!state.breederUseAge) FontWeight.Bold else FontWeight.Normal,
                                color = if (!state.breederUseAge) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Thresholds
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column {
                            if (state.breederUseAge) {
                                StatusRangeItem(label = stringResource("piglet"), range = "0 to ", value = state.breederPigletAge, onValueChange = actions.onBreederPigletAgeChange, unit = stringResource("weeks"))
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                                StatusRangeItem(label = stringResource("weaners"), range = "${state.breederPigletAge} to ", value = state.breederWeanerAge, onValueChange = actions.onBreederWeanerAgeChange, unit = stringResource("weeks"))
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                                StatusRangeItem(label = stringResource("grower"), range = "${state.breederWeanerAge} to ", value = state.breederGrowerAge, onValueChange = actions.onBreederGrowerAgeChange, unit = stringResource("weeks"))
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                                StatusRangeItem(label = stringResource("boar_gilt"), range = "${state.breederGrowerAge} ", value = stringResource("above"), onValueChange = {}, readOnly = true, unit = stringResource("weeks"))
                            } else {
                                StatusRangeItem(label = stringResource("piglet"), range = "0 to ", value = state.breederPigletWeight, onValueChange = actions.onBreederPigletWeightChange, unit = stringResource("kg"))
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                                StatusRangeItem(label = stringResource("weaners"), range = "${state.breederPigletWeight} to ", value = state.breederWeanerWeight, onValueChange = actions.onBreederWeanerWeightChange, unit = stringResource("kg"))
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                                StatusRangeItem(label = stringResource("grower"), range = "${state.breederWeanerWeight} to ", value = state.breederGrowerWeight, onValueChange = actions.onBreederGrowerWeightChange, unit = stringResource("kg"))
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                                StatusRangeItem(label = stringResource("boar_gilt"), range = "${state.breederGrowerWeight} ", value = stringResource("above"), onValueChange = {}, readOnly = true, unit = stringResource("kg"))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        actions.onSaveSettings()
                        showBreedersDialog = false
                    }
                ) {
                    Text(stringResource("save"))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showBreedersDialog = false
                        showSetStatusDialog = true
                    }
                ) {
                    Text(stringResource("previous"))
                }
            }
        )
    }

    // ─── E. CONFIRMATION ALERT DIALOG FOR DATA CLEARING ──────────────────────
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = {
                Text(
                    when (clearType) {
                        "account" -> stringResource("delete_account") + "?"
                        "all" -> stringResource("factory_reset") + "?"
                        else -> stringResource("confirm_deletion")
                    }
                )
            },
            text = {
                Text(
                    when (clearType) {
                        "account" -> stringResource("delete_account_confirm_msg")
                        "all" -> stringResource("factory_reset_confirm_msg")
                        else -> stringResource("clear_records_confirm_msg")
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (clearType) {
                            "account" -> actions.onDeleteAccount { showClearDialog = false }
                            "all" -> actions.onFactoryReset { showClearDialog = false }
                            else -> actions.onClearCollection(clearType) { showClearDialog = false }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (clearType == "account") stringResource("delete_account") else stringResource("delete_everything"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource("cancel"))
                }
            }
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    isCollapsible: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    var isExpanded by remember { mutableStateOf(!isCollapsible) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isCollapsible) Modifier.clickable { isExpanded = !isExpanded } else Modifier)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp)
            )
            if (isCollapsible) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        if (isExpanded) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                )
            ) {
                Column {
                    content()
                }
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (textColor == MaterialTheme.colorScheme.error) textColor else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                    color = textColor,
                    fontWeight = FontWeight.Medium
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.width(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                fontWeight = FontWeight.Medium
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun StatusRangeItem(
    label: String,
    range: String,
    value: String,
    onValueChange: (String) -> Unit,
    unit: String,
    readOnly: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp), fontWeight = FontWeight.Bold)
            Text(text = "$range $value $unit", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!readOnly) {
            OutlinedTextField(
                value = value,
                onValueChange = { if (it.all { c -> c.isDigit() }) onValueChange(it) },
                modifier = Modifier.width(80.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp, textAlign = TextAlign.Center),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(8.dp)
            )
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.width(70.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    SmartSwineTheme {
        SettingsScreenContent(
            state = SettingsState(),
            actions = SettingsActions()
        )
    }
}
