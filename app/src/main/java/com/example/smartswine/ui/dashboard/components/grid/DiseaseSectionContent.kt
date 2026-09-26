package com.example.smartswine.ui.dashboard.components.grid

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartswine.model.HealthRecord
import com.example.smartswine.model.Pig
import com.example.smartswine.ui.diseasefinder.*
import com.example.smartswine.utils.LocalAppLanguage
import com.example.smartswine.utils.Translator
import com.example.smartswine.utils.stringResource
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DiseaseSectionContent(
    allPigs: List<Pig>,
    openDiseaseSubOption: String?,
    onToggleSubOption: (String) -> Unit,
    primaryColor: Color,
    onRegisterCoordinates: (String, LayoutCoordinates) -> Unit,
    onItemExpanded: (String) -> Unit,
    onLogHealthActivity: (List<String>, HealthRecord, Boolean, Boolean, Boolean, Map<String, Any>) -> Unit
) {
    val selectedSymptoms = remember { mutableStateOf(setOf<String>()) }
    var diagnosisResults by remember { mutableStateOf<List<DiagnosisMatch>>(emptyList()) }
    var hasDiagnosed by remember { mutableStateOf(false) }
    var selectedPigId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedPig = remember(selectedPigId, allPigs) { allPigs.find { it.id == selectedPigId } }
    var selectedStage by remember { mutableStateOf(PigStage.ALL) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MarketSubOptionCard(
            modifier = Modifier.onGloballyPositioned { onRegisterCoordinates("symptoms", it) },
            title = stringResource("find_with_symptoms"),
            icon = Icons.Default.Search,
            count = if (selectedSymptoms.value.isNotEmpty()) selectedSymptoms.value.size else null,
            isExpanded = openDiseaseSubOption == "symptoms",
            primaryColor = primaryColor,
            onToggle = {
                onToggleSubOption("symptoms")
                if (openDiseaseSubOption != "symptoms") onItemExpanded("symptoms")
            }
        ) {
            InlineSymptomsDiagnosisView(
                allPigs = allPigs,
                selectedPig = selectedPig,
                onSelectPig = { pig ->
                    selectedPigId = pig?.id
                    if (pig != null) {
                        selectedStage = PigStage.fromPig(pig)
                    }
                },
                selectedStage = selectedStage,
                onSelectStage = { selectedStage = it },
                selectedSymptoms = selectedSymptoms.value,
                onToggleSymptom = { sym ->
                    selectedSymptoms.value = if (sym in selectedSymptoms.value) selectedSymptoms.value - sym else selectedSymptoms.value + sym
                },
                onAnalyze = {
                    diagnosisResults = DiseaseDatabase.diagnose(selectedSymptoms.value, selectedStage)
                    hasDiagnosed = true
                },
                onReset = {
                    selectedSymptoms.value = emptySet()
                    diagnosisResults = emptyList()
                    hasDiagnosed = false
                    selectedPigId = null
                    selectedStage = PigStage.ALL
                },
                hasDiagnosed = hasDiagnosed,
                results = diagnosisResults,
                primaryColor = primaryColor,
                onLogHealthActivity = onLogHealthActivity
            )
        }

        MarketSubOptionCard(
            modifier = Modifier.onGloballyPositioned { onRegisterCoordinates("diseases", it) },
            title = stringResource("common_pig_diseases"),
            icon = Icons.AutoMirrored.Filled.MenuBook,
            count = DiseaseDatabase.diseases.size,
            isExpanded = openDiseaseSubOption == "diseases",
            primaryColor = primaryColor,
            onToggle = {
                onToggleSubOption("diseases")
                if (openDiseaseSubOption != "diseases") onItemExpanded("diseases")
            }
        ) {
            InlineCommonDiseasesView(primaryColor = primaryColor)
        }
    }
}

@Composable
fun InlineSymptomsDiagnosisView(
    allPigs: List<Pig>,
    selectedPig: Pig?,
    onSelectPig: (Pig?) -> Unit,
    selectedStage: PigStage,
    onSelectStage: (PigStage) -> Unit,
    selectedSymptoms: Set<String>,
    onToggleSymptom: (String) -> Unit,
    onAnalyze: () -> Unit,
    onReset: () -> Unit,
    hasDiagnosed: Boolean,
    results: List<DiagnosisMatch>,
    primaryColor: Color,
    onLogHealthActivity: (List<String>, HealthRecord, Boolean, Boolean, Boolean, Map<String, Any>) -> Unit
) {
    val context = LocalContext.current
    val currentLanguage = LocalAppLanguage.current
    var showPigSelector by remember { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var openGroupKey by rememberSaveable { mutableStateOf<String?>(null) }

    if (showPigSelector) {
        PigSelectorDialog(
            allPigs = allPigs,
            onSelectPig = onSelectPig,
            onDismiss = { showPigSelector = false },
            primaryColor = primaryColor
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 1. Pig Tagging / Herd Integration
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (selectedPig != null) primaryColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, if (selectedPig != null) primaryColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (selectedPig != null) {
                            "${stringResource("affected_pig")}: ${stringResource("tag")} ${selectedPig.tagNumber}"
                        } else {
                            stringResource("herd_link_unassigned")
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedPig != null) primaryColor else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (selectedPig != null) {
                            val stageName = stringResource(selectedPig.status.lowercase(Locale.ROOT))
                            val genderName = stringResource(selectedPig.gender.lowercase(Locale.ROOT))
                            val penInfo = if (selectedPig.location.isNotBlank()) " • ${stringResource("pen")} ${selectedPig.location}" else ""
                            "$stageName • $genderName$penInfo"
                        } else {
                            stringResource("select_pig_autofilter_desc")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (selectedPig != null) {
                        IconButton(onClick = { onSelectPig(null) }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource("clear"), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(onClick = { showPigSelector = true }) {
                        Text(if (selectedPig == null) stringResource("select_pig") else stringResource("change"), color = primaryColor, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. Production Stage Filter Chips
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource("production_stage_filter"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PigStage.entries.forEach { stage ->
                    val isSelected = selectedStage == stage
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectStage(stage) },
                        label = { Text(stringResource(stage.key), fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = primaryColor,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // 3. Active Selected Symptoms Chips Bar
        if (selectedSymptoms.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                border = BorderStroke(0.8.dp, primaryColor.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${stringResource("selected_symptoms")} (${selectedSymptoms.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                        Text(
                            text = stringResource("clear_all"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onReset() }
                        )
                    }

                    SimpleFlowRow(
                        horizontalSpacing = 6.dp,
                        verticalSpacing = 6.dp
                    ) {
                        selectedSymptoms.forEach { symKey ->
                            val isHallmark = DiseaseDatabase.isPathognomonic(symKey)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isHallmark) Color(0xFFFFF8E1) else primaryColor.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, if (isHallmark) Color(0xFFFFB300) else primaryColor.copy(alpha = 0.35f)),
                                modifier = Modifier.clickable { onToggleSymptom(symKey) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (isHallmark) {
                                        Text("⭐", fontSize = 11.sp)
                                    }
                                    Text(
                                        text = stringResource(symKey),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isHallmark) Color(0xFFE65100) else primaryColor
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = if (isHallmark) Color(0xFFE65100) else primaryColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Instant Symptom Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(stringResource("search_symptoms_placeholder"), fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = primaryColor) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            singleLine = true
        )

        // 5. Symptom Selection List (Search Mode vs Categorized Accordion Mode)
        if (searchQuery.isNotBlank()) {
            val allSymptomKeys = remember { DiseaseDatabase.symptomGroups.values.flatten().distinct() }
            val matchingSymptoms = remember(searchQuery, currentLanguage.code) {
                allSymptomKeys.filter { key ->
                    val translated = Translator.getString(key, currentLanguage.code)
                    val subtitle = DiseaseDatabase.getSymptomSubtitle(key) ?: ""
                    translated.contains(searchQuery, ignoreCase = true) ||
                    subtitle.contains(searchQuery, ignoreCase = true) ||
                    key.contains(searchQuery, ignoreCase = true)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource("matching_symptoms_count", matchingSymptoms.size),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )

                if (matchingSymptoms.isEmpty()) {
                    Text(
                        text = stringResource("no_symptoms_match", searchQuery),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    matchingSymptoms.forEach { symptomKey ->
                        val isSelected = symptomKey in selectedSymptoms
                        val isHallmark = DiseaseDatabase.isPathognomonic(symptomKey)
                        val subtitle = DiseaseDatabase.getSymptomSubtitle(symptomKey)

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) primaryColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(0.8.dp, if (isSelected) primaryColor else if (isHallmark) Color(0xFFFFB300) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleSymptom(symptomKey) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = stringResource(symptomKey),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                        if (isHallmark) {
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFFFFF8E1),
                                                border = BorderStroke(0.8.dp, Color(0xFFFFB300))
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = "Hallmark",
                                                    tint = Color(0xFFFFB300),
                                                    modifier = Modifier.padding(2.dp).size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                    if (subtitle != null) {
                                        Text(
                                            text = subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = primaryColor, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Categorized Accordions
            DiseaseDatabase.symptomGroups.forEach { (groupKey, symptoms) ->
                val isGroupOpen = openGroupKey == groupKey
                val countInGroup = symptoms.count { it in selectedSymptoms }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    border = BorderStroke(0.8.dp, if (isGroupOpen) primaryColor else primaryColor.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { openGroupKey = if (isGroupOpen) null else groupKey }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = stringResource(groupKey),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (countInGroup > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = primaryColor
                                    ) {
                                        Text(
                                            text = "$countInGroup",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Icon(
                                imageVector = if (isGroupOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        AnimatedVisibility(visible = isGroupOpen) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                symptoms.forEach { symptomKey ->
                                    val isSelected = symptomKey in selectedSymptoms
                                    val isHallmark = DiseaseDatabase.isPathognomonic(symptomKey)
                                    val subtitle = DiseaseDatabase.getSymptomSubtitle(symptomKey)

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) primaryColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(0.8.dp, if (isSelected) primaryColor else if (isHallmark) Color(0xFFFFB300) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onToggleSymptom(symptomKey) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Text(
                                                        text = stringResource(symptomKey),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurface,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                    if (isHallmark) {
                                                        Surface(
                                                            shape = CircleShape,
                                                            color = Color(0xFFFFF8E1),
                                                            border = BorderStroke(0.8.dp, Color(0xFFFFB300))
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Star,
                                                                contentDescription = "Hallmark",
                                                                tint = Color(0xFFFFB300),
                                                                modifier = Modifier.padding(2.dp).size(12.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                if (subtitle != null) {
                                                    Text(
                                                        text = subtitle,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.outline
                                                    )
                                                }
                                            }
                                            if (isSelected) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = primaryColor, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Action Buttons (Analyze & Reset)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onAnalyze,
                enabled = selectedSymptoms.isNotEmpty(),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("${stringResource("analyze_symptoms")} (${selectedSymptoms.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            if (selectedSymptoms.isNotEmpty() || hasDiagnosed) {
                OutlinedButton(
                    onClick = onReset,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(stringResource("diag_reset"), fontSize = 14.sp)
                }
            }
        }

        // 7. Diagnostic Results
        if (hasDiagnosed) {
            Spacer(Modifier.height(6.dp))

            // Emergency Biosecurity Banner if notifiable disease matched
            val notifiableAlertMatch = results.firstOrNull { it.isNotifiableAlert }
            if (notifiableAlertMatch != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
                            Text(
                                text = Translator.getString("critical_biosecurity_alert", currentLanguage.code),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Text(
                            text = Translator.getString(
                                "critical_biosecurity_desc",
                                currentLanguage.code,
                                Translator.getString(notifiableAlertMatch.disease.nameKey, currentLanguage.code),
                                notifiableAlertMatch.matchPercentage
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                        Text(
                            text = "${Translator.getString("emergency_containment_protocol_title", currentLanguage.code)}\n" +
                                   Translator.getString("emergency_containment_protocol_steps", currentLanguage.code),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Text(
                text = "${Translator.getString("clinical_diagnosis_matches", currentLanguage.code)} (${results.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (results.isEmpty()) {
                Text(
                    text = "No diseases matched the selected symptoms for ${selectedStage.label}. Try adjusting your stage filter or selecting additional symptoms.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                results.forEach { match ->
                    val disease = match.disease
                    var isCardExpanded by remember { mutableStateOf(false) }
                    val sevColor = when (disease.severity) {
                        Severity.CRITICAL -> Color(0xFFD32F2F)
                        Severity.HIGH -> Color(0xFFF44336)
                        Severity.MODERATE -> Color(0xFFFFA000)
                        Severity.LOW -> Color(0xFF388E3C)
                    }

                    val confidenceColor = when {
                        match.matchPercentage >= 70 -> Color(0xFF2E7D32)
                        match.matchPercentage >= 45 -> Color(0xFFF57C00)
                        else -> Color(0xFFE65100)
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (match.isNotifiableAlert) MaterialTheme.colorScheme.error else sevColor.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isCardExpanded = !isCardExpanded }
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Header Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(disease.nameKey),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (disease.scientificName.isNotBlank()) {
                                        Text(disease.scientificName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (match.matchedPathognomonic.isNotEmpty()) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFFFFF8E1),
                                            border = BorderStroke(1.dp, Color(0xFFFFB300))
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = "Hallmark Match",
                                                    tint = Color(0xFFFFB300),
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = stringResource("hallmark_badge"),
                                                    color = Color(0xFFE65100),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    }
                                    Surface(shape = RoundedCornerShape(4.dp), color = sevColor.copy(alpha = 0.15f)) {
                                        Text(
                                            text = stringResource(disease.severity.key).uppercase(),
                                            color = sevColor,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            // Match Confidence Ribbon
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = confidenceColor.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = stringResource("clinical_match_percent", match.matchPercentage),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = confidenceColor,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Text(
                                    text = stringResource("matched_signs_count", match.matchedCount, match.totalSymptoms),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            // Hallmark presence banner
                            if (match.matchedPathognomonic.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFFF8E1),
                                    border = BorderStroke(0.8.dp, Color(0xFFFFB300)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text("⭐", fontSize = 12.sp)
                                        Text(
                                            text = stringResource("hallmark_sign_present", match.matchedPathognomonic.map { Translator.getString(it, currentLanguage.code) }.joinToString(", ")),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFE65100)
                                        )
                                    }
                                }
                            }

                            // Expandable Clinical Details
                            if (isCardExpanded) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Text(
                                    text = stringResource(disease.descriptionKey),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                val unselectedSymptoms = disease.symptomKeys.filter { it !in selectedSymptoms }
                                if (unselectedSymptoms.isNotEmpty()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = stringResource("other_signs_watch"),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    unselectedSymptoms.forEach { symKey ->
                                        Row(
                                            modifier = Modifier.padding(vertical = 1.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text("•", style = MaterialTheme.typography.bodyMedium, color = primaryColor, fontWeight = FontWeight.Bold)
                                            Text(text = stringResource(symKey), style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }

                                if (disease.preventionKey.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = stringResource("prevention_treatment_label", stringResource(disease.preventionKey)),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = primaryColor
                                    )
                                }

                                if (disease.isNotifiable && disease.biosecurityProtocol != null) {
                                    Spacer(Modifier.height(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.error),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = Translator.getString(disease.biosecurityProtocol, currentLanguage.code),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }

                                // Actions: Telehealth Sharing & Health Record Logging
                                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            if (selectedPig == null) {
                                                showPigSelector = true
                                                Toast.makeText(context, Translator.getString("select_pig_health_activity_prompt", currentLanguage.code), Toast.LENGTH_SHORT).show()
                                            } else {
                                                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                                val diseaseName = Translator.getString(disease.nameKey, currentLanguage.code)
                                                val symptomList = selectedSymptoms.map { Translator.getString(it, currentLanguage.code) }.joinToString(", ")
                                                val desc = Translator.getString(
                                                    "suspected_diagnosis_record",
                                                    currentLanguage.code,
                                                    diseaseName,
                                                    match.matchPercentage,
                                                    symptomList
                                                )
                                                val med = Translator.getString(disease.preventionKey, currentLanguage.code)
                                                val record = HealthRecord(
                                                    date = today,
                                                    type = if (disease.isNotifiable || disease.severity == Severity.CRITICAL) "Quarantine" else "Diagnosis / Treatment",
                                                    description = desc,
                                                    medication = med
                                                )
                                                onLogHealthActivity(listOf(selectedPig.id), record, false, false, false, emptyMap())
                                                Toast.makeText(context, "${Translator.getString("logged_to_health_records", currentLanguage.code)} ${selectedPig.tagNumber}!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(stringResource("log_to_herd"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            val today = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                                            val targetText = if (selectedPig != null) "Tag ${selectedPig.tagNumber} (${selectedPig.statusEnum.displayName}, ${selectedPig.gender}${if (selectedPig.location.isNotBlank()) ", Pen ${selectedPig.location}" else ""})" else "General Pen / Unassigned Group"
                                            val summary = buildString {
                                                appendLine("📋 SWINE CLINICAL TELEHEALTH REPORT")
                                                appendLine("------------------------------------")
                                                appendLine("Date: $today")
                                                appendLine("Subject: $targetText")
                                                appendLine("Production Stage: ${selectedStage.label}")
                                                appendLine()
                                                appendLine(Translator.getString("observed_signs_header", currentLanguage.code, selectedSymptoms.size))
                                                selectedSymptoms.forEach { sym ->
                                                    val isHallmark = DiseaseDatabase.isPathognomonic(sym)
                                                    appendLine("• ${Translator.getString(sym, currentLanguage.code)}${if (isHallmark) " [⭐ ${Translator.getString("hallmark_signs_label", currentLanguage.code)}]" else ""}")
                                                }
                                                appendLine()
                                                appendLine(Translator.getString("suspected_diagnosis_header", currentLanguage.code))
                                                appendLine("• ${Translator.getString("disease_label", currentLanguage.code)}: ${Translator.getString(disease.nameKey, currentLanguage.code)} (${disease.scientificName})")
                                                appendLine("• ${Translator.getString("match_confidence_label", currentLanguage.code)}: ${match.matchPercentage}%")
                                                appendLine("• ${Translator.getString("severity_label", currentLanguage.code)}: ${disease.severity.name}")
                                                if (match.matchedPathognomonic.isNotEmpty()) {
                                                    appendLine("• ${Translator.getString("hallmark_signs_label", currentLanguage.code)}: ${match.matchedPathognomonic.map { Translator.getString(it, currentLanguage.code) }.joinToString(", ")}")
                                                }
                                                appendLine()
                                                appendLine(Translator.getString("recommended_action_label", currentLanguage.code))
                                                appendLine(Translator.getString(disease.preventionKey, currentLanguage.code))
                                                if (disease.isNotifiable && disease.biosecurityProtocol != null) {
                                                    appendLine()
                                                    appendLine(Translator.getString("emergency_biosecurity_protocol_header", currentLanguage.code))
                                                    appendLine(Translator.getString(disease.biosecurityProtocol, currentLanguage.code))
                                                }
                                                appendLine()
                                                appendLine(Translator.getString("generated_via_smartswine", currentLanguage.code))
                                            }

                                            val sendIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, summary)
                                                putExtra(Intent.EXTRA_SUBJECT, Translator.getString("share_telehealth_subject", currentLanguage.code, Translator.getString(disease.nameKey, currentLanguage.code)))
                                                type = "text/plain"
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, Translator.getString("share_telehealth_report_chooser", currentLanguage.code)))
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(stringResource("share_with_vet"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    Text(
                        text = stringResource("diag_disclaimer"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
fun InlineCommonDiseasesView(primaryColor: Color) {
    var openCategoryKey by rememberSaveable { mutableStateOf<String?>(null) }
    var openDiseaseKey by rememberSaveable { mutableStateOf<String?>(null) }
    val currentLanguage = LocalAppLanguage.current

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DiseaseDatabase.symptomGroups.keys.forEach { catKey ->
            val catSymptoms = DiseaseDatabase.symptomGroups[catKey] ?: emptyList()
            val catDiseases = remember(catKey, currentLanguage.code) {
                DiseaseDatabase.diseases.filter { disease ->
                    disease.symptomKeys.any { it in catSymptoms }
                }.sortedBy { Translator.getString(it.nameKey, currentLanguage.code) }
            }
            val isCatOpen = openCategoryKey == catKey

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, if (isCatOpen) primaryColor else primaryColor.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { openCategoryKey = if (isCatOpen) null else catKey }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = primaryColor.copy(alpha = 0.12f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("${catDiseases.size}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp), fontWeight = FontWeight.Bold, color = primaryColor)
                                }
                            }
                            Text(
                                text = stringResource(catKey),
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = if (isCatOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    AnimatedVisibility(visible = isCatOpen) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            catDiseases.forEach { disease ->
                                val isDiseaseExpanded = openDiseaseKey == disease.nameKey
                                val sevColor = when (disease.severity) {
                                    Severity.CRITICAL -> Color(0xFFD32F2F)
                                    Severity.HIGH -> Color(0xFFF44336)
                                    Severity.MODERATE -> Color(0xFFFFA000)
                                    Severity.LOW -> Color(0xFF388E3C)
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    border = BorderStroke(0.8.dp, sevColor.copy(alpha = 0.3f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { openDiseaseKey = if (isDiseaseExpanded) null else disease.nameKey }
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = stringResource(disease.nameKey),
                                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                                )
                                                if (disease.scientificName.isNotBlank()) {
                                                    Text(disease.scientificName, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp), color = MaterialTheme.colorScheme.outline)
                                                }
                                            }
                                            Surface(shape = RoundedCornerShape(6.dp), color = sevColor.copy(alpha = 0.15f)) {
                                                Text(
                                                    text = stringResource(disease.severity.key).uppercase(),
                                                    color = sevColor,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }

                                        if (isDiseaseExpanded) {
                                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                            Text(
                                                text = stringResource(disease.descriptionKey),
                                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(Modifier.height(6.dp))
                                            Text(
                                                text = stringResource("diag_observed_symptoms"),
                                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                                                color = primaryColor
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            disease.symptomKeys.forEach { symKey ->
                                                Row(
                                                    modifier = Modifier.padding(vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = primaryColor,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Text(
                                                        text = stringResource(symKey),
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp)
                                                    )
                                                }
                                            }
                                            if (disease.preventionKey.isNotBlank()) {
                                                Spacer(Modifier.height(6.dp))
                                                Text(
                                                    text = "Prevention / Treatment: ${stringResource(disease.preventionKey)}",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                                                    color = primaryColor
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
