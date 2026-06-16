package com.example.smartswine.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.smartswine.ui.auth.AuthViewModel
import com.example.smartswine.utils.GlobalNotice
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
    val onNavigateToEditProfile: () -> Unit = {},
    val onNavigateToTerms: () -> Unit = {},
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
    onNavigateToEditProfile: () -> Unit,
    onNavigateToTerms: () -> Unit,
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
        onNavigateToEditProfile = onNavigateToEditProfile,
        onNavigateToTerms = onNavigateToTerms,
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .imePadding()
    ) {
        Text(
            text = stringResource("settings"),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        SettingsSection(title = stringResource("account")) {
            SettingsItem(
                icon = Icons.Default.Person,
                title = stringResource("edit_profile"),
                subtitle = stringResource("edit_profile_subtitle"),
                onClick = actions.onNavigateToEditProfile
            )
        }
        
        StylishDivider(modifier = Modifier.padding(vertical = 12.dp))

        Spacer(modifier = Modifier.height(12.dp))

        SettingsSection(title = stringResource("preferences"), isCollapsible = true) {
            SettingsToggleItem(
                icon = Icons.Default.Notifications,
                title = stringResource("push_notifications"),
                subtitle = stringResource("push_notifications_subtitle"),
                checked = state.notificationsEnabled,
                onCheckedChange = actions.onNotificationsEnabledChange
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            
            SettingsToggleItem(
                icon = Icons.Default.Brightness4,
                title = stringResource("dark_mode"),
                subtitle = stringResource("dark_mode_subtitle"),
                checked = state.isDarkMode,
                onCheckedChange = actions.onDarkModeChange
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsSection(title = stringResource("weaning"), isCollapsible = true) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_weaning),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource("wean_at"),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                
                OutlinedTextField(
                    value = state.weaningDays,
                    onValueChange = actions.onWeaningDaysChange,
                    modifier = Modifier.width(80.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp)
                )
                
                Text(
                    text = stringResource("days"),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsSection(title = stringResource("farrowing"), isCollapsible = true) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_farrowing),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource("sows_farrow_at"),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                
                OutlinedTextField(
                    value = state.farrowingDays,
                    onValueChange = actions.onFarrowingDaysChange,
                    modifier = Modifier.width(80.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp)
                )
                
                Text(
                    text = stringResource("days"),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsSection(title = stringResource("iron_injection"), isCollapsible = true) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_iron),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = stringResource("iron_administered_on"), style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = state.ironDay1,
                            onValueChange = actions.onIronDay1Change,
                            modifier = Modifier.width(60.dp),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = stringResource("and") + " ", style = MaterialTheme.typography.bodyLarge)
                        OutlinedTextField(
                            value = state.ironDay2,
                            onValueChange = actions.onIronDay2Change,
                            modifier = Modifier.width(60.dp),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource("after_birth"), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsSection(title = stringResource("porker_status"), isCollapsible = true) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource("age_weeks"), style = MaterialTheme.typography.bodyMedium, color = if (state.porkerUseAge) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                Switch(checked = !state.porkerUseAge, onCheckedChange = { actions.onPorkerUseAgeChange(!it) })
                Text(stringResource("weight_kg"), style = MaterialTheme.typography.bodyMedium, color = if (!state.porkerUseAge) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            if (state.porkerUseAge) {
                StatusRangeItem(label = stringResource("starter"), range = "0 to ", value = state.porkerStarterAge, onValueChange = actions.onPorkerStarterAgeChange, unit = stringResource("weeks"))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                StatusRangeItem(label = stringResource("grower"), range = "${state.porkerStarterAge} to ", value = state.porkerGrowerAge, onValueChange = actions.onPorkerGrowerAgeChange, unit = stringResource("weeks"))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                StatusRangeItem(label = stringResource("finisher"), range = "${state.porkerGrowerAge} ", value = stringResource("above"), onValueChange = {}, readOnly = true, unit = stringResource("weeks"))
            } else {
                StatusRangeItem(label = stringResource("starter"), range = "0 to ", value = state.porkerStarterWeight, onValueChange = actions.onPorkerStarterWeightChange, unit = stringResource("kg"))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                StatusRangeItem(label = stringResource("grower"), range = "${state.porkerStarterWeight} to ", value = state.porkerGrowerWeight, onValueChange = actions.onPorkerGrowerWeightChange, unit = stringResource("kg"))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                StatusRangeItem(label = stringResource("finisher"), range = "${state.porkerGrowerWeight} ", value = stringResource("above"), onValueChange = {}, readOnly = true, unit = stringResource("kg"))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsSection(title = stringResource("breeder_status"), isCollapsible = true) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource("age_weeks"), style = MaterialTheme.typography.bodyMedium, color = if (state.breederUseAge) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                Switch(checked = !state.breederUseAge, onCheckedChange = { actions.onBreederUseAgeChange(!it) })
                Text(stringResource("weight_kg"), style = MaterialTheme.typography.bodyMedium, color = if (!state.breederUseAge) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            if (state.breederUseAge) {
                StatusRangeItem(label = stringResource("piglet"), range = "0 to ", value = state.breederPigletAge, onValueChange = actions.onBreederPigletAgeChange, unit = stringResource("weeks"))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                StatusRangeItem(label = stringResource("weaners"), range = "${state.breederPigletAge} to ", value = state.breederWeanerAge, onValueChange = actions.onBreederWeanerAgeChange, unit = stringResource("weeks"))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                StatusRangeItem(label = stringResource("grower"), range = "${state.breederWeanerAge} to ", value = state.breederGrowerAge, onValueChange = actions.onBreederGrowerAgeChange, unit = stringResource("weeks"))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                StatusRangeItem(label = stringResource("boar_gilt"), range = "${state.breederGrowerAge} ", value = stringResource("above"), onValueChange = {}, readOnly = true, unit = stringResource("weeks"))
            } else {
                StatusRangeItem(label = stringResource("piglet"), range = "0 to ", value = state.breederPigletWeight, onValueChange = actions.onBreederPigletWeightChange, unit = stringResource("kg"))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                StatusRangeItem(label = stringResource("weaners"), range = "${state.breederPigletWeight} to ", value = state.breederWeanerWeight, onValueChange = actions.onBreederWeanerWeightChange, unit = stringResource("kg"))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                StatusRangeItem(label = stringResource("grower"), range = "${state.breederWeanerWeight} to ", value = state.breederGrowerWeight, onValueChange = actions.onBreederGrowerWeightChange, unit = stringResource("kg"))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                StatusRangeItem(label = stringResource("boar_gilt"), range = "${state.breederGrowerWeight} ", value = stringResource("above"), onValueChange = {}, readOnly = true, unit = stringResource("kg"))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsSection(title = stringResource("terminology_classification"), isCollapsible = true) {
            SettingsToggleItem(
                icon = Icons.Default.Male,
                title = stringResource("castrated_male_is_barrow"),
                subtitle = stringResource("automatically_set_barrow"),
                checked = state.autoClassifyBarrows,
                onCheckedChange = actions.onAutoClassifyBarrowsChange
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            SettingsToggleItem(
                icon = Icons.Default.Female,
                title = stringResource("farrowing_female_is_sow"),
                subtitle = stringResource("automatically_set_sow"),
                checked = state.autoClassifySows,
                onCheckedChange = actions.onAutoClassifySowsChange
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Female, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource("gilt_classification"), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(stringResource("gilt_classification_subtitle", state.giltAgeThresholdWeeks), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedTextField(
                    value = state.giltAgeThresholdWeeks,
                    onValueChange = actions.onGiltAgeThresholdWeeksChange,
                    modifier = Modifier.width(70.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsSection(title = stringResource("currency"), isCollapsible = true) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource("currency_unit"), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(stringResource("currency_display", state.selectedCurrency, state.currencySymbol), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedTextField(
                    value = state.selectedCurrency,
                    onValueChange = { actions.onUpdateCurrency(it.uppercase()) },
                    modifier = Modifier.width(80.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SettingsSection(title = stringResource("data_management"), isCollapsible = true) {
            SettingsItem(
                icon = if (state.isSyncing) Icons.Default.Sync else Icons.Default.CloudDone,
                title = if (state.isSyncing) stringResource("syncing") else stringResource("sync_with_cloud"),
                subtitle = if (state.isSyncing) null else stringResource("last_synced", state.lastSyncTime ?: stringResource("never")),
                onClick = actions.onSaveSettings
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            var showClearDialog by remember { mutableStateOf(value = false) }
            var clearType by remember { mutableStateOf("") } // "pigs", "financials", "ingredients", "staff", "all"

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

        Spacer(modifier = Modifier.height(24.dp))

        SettingsSection(title = stringResource("about"), isCollapsible = true) {
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

        Spacer(modifier = Modifier.height(24.dp))

        SettingsSection(title = stringResource("select_language"), isCollapsible = true) {
            Box(modifier = Modifier.padding(16.dp)) {
                LanguageSelectionGrid(
                    onLanguageChange = actions.onLanguageChange
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = stringResource("app_version", BuildConfig.VERSION_NAME),
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
                text = "Developed By:",
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
                text = "copyright 2026",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
        
        Spacer(modifier = Modifier.height(120.dp))
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
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
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
                shape = RoundedCornerShape(16.dp),
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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (textColor == MaterialTheme.colorScheme.error) textColor else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor,
                    fontWeight = FontWeight.Medium
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
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
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
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
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(text = "$range $value $unit", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!readOnly) {
            OutlinedTextField(
                value = value,
                onValueChange = { if (it.all { c -> c.isDigit() }) onValueChange(it) },
                modifier = Modifier.width(70.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
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
