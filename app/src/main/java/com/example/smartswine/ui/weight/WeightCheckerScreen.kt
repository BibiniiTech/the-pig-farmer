package com.example.smartswine.ui.weight

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import com.bibiniitech.smartswine.R
import androidx.compose.ui.res.painterResource
import com.example.smartswine.utils.DateUtils
import com.example.smartswine.utils.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartswine.utils.StylishDivider
import com.example.smartswine.ui.theme.SmartSwineTheme
import java.util.Locale
import kotlin.math.roundToInt
import com.example.smartswine.model.Pig
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@Composable
fun WeightConverterCard() {
    var lbsText by remember { mutableStateOf("") }
    var kgsText by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFECEFF1)),
        border = BorderStroke(1.dp, Color(0xFFCFD8DC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Scale, null, tint = Color(0xFF455A64))
                Spacer(Modifier.width(12.dp))
                Text(stringResource("converter_title"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF263238))
            }
            
            Spacer(Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Lbs box
                OutlinedTextField(
                    value = lbsText,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || (newValue.toDoubleOrNull() != null)) {
                            lbsText = newValue
                            if (newValue.isNotEmpty()) {
                                val kgs = newValue.toDouble() * 0.453592
                                kgsText = String.format(Locale.getDefault(), "%.2f", kgs)
                            } else {
                                kgsText = ""
                            }
                        }
                    },
                    label = { Text(stringResource("unit_lbs")) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White.copy(alpha = 0.9f),
                        focusedBorderColor = Color(0xFF455A64),
                        unfocusedBorderColor = Color(0xFFB0BEC5),
                        focusedLabelColor = Color(0xFF455A64)
                    ),
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                )

                Text(
                    "=",
                    modifier = Modifier.padding(horizontal = 12.dp),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF455A64)
                )

                // Kgs box
                OutlinedTextField(
                    value = kgsText,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || (newValue.toDoubleOrNull() != null)) {
                            kgsText = newValue
                            if (newValue.isNotEmpty()) {
                                val lbs = newValue.toDouble() / 0.453592
                                lbsText = String.format(Locale.getDefault(), "%.2f", lbs)
                            } else {
                                lbsText = ""
                            }
                        }
                    },
                    label = { Text(stringResource("unit_kgs")) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White.copy(alpha = 0.9f),
                        focusedBorderColor = Color(0xFF455A64),
                        unfocusedBorderColor = Color(0xFFB0BEC5),
                        focusedLabelColor = Color(0xFF455A64)
                    ),
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TapeMeasurementCard(
    isKg: Boolean,
    onUnitChange: (Boolean) -> Unit,
    pigs: List<Pig> = emptyList(),
    initialPigTag: String? = null,
    onUpdatePigWeight: (String, Double) -> Unit = { _, _ -> },
) {
    var girthText by remember { mutableStateOf("") }
    var lengthText by remember { mutableStateOf("") }
    var isLengthFocused by remember { mutableStateOf(false) }
    var isGirthFocused by remember { mutableStateOf(false) }
    
    var selectedPig by remember { mutableStateOf<Pig?>(null) }
    var pigSearchQuery by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val girth = girthText.toDoubleOrNull() ?: 0.0
    val length = lengthText.toDoubleOrNull() ?: 0.0

    LaunchedEffect(pigs, initialPigTag) {
        if (!initialPigTag.isNullOrBlank()) {
            val found = pigs.find { it.tagNumber.equals(initialPigTag, ignoreCase = true) || it.id == initialPigTag }
            if (found != null) {
                selectedPig = found
                pigSearchQuery = found.tagNumber
            } else {
                pigSearchQuery = initialPigTag
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFECEFF1)),
        border = BorderStroke(1.dp, Color(0xFFCFD8DC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Straighten, null, tint = Color(0xFF455A64))
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource("weigh_with_tape_title"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF263238),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource("weigh_with_tape_subtitle"),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF546E7A),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            // Unit Toggle Centered
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource("unit_lbs"), style = MaterialTheme.typography.labelSmall, color = Color(0xFF263238))
                Switch(
                    checked = isKg,
                    onCheckedChange = onUnitChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF455A64),
                        checkedBorderColor = Color(0xFF455A64)
                    ),
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .scale(0.7f)
                )
                Text(stringResource("unit_kgs"), style = MaterialTheme.typography.labelSmall, color = Color(0xFF263238))
            }
            
            Spacer(Modifier.height(16.dp))

            // Heart-Girth Tape Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource("label_length", if (isKg) stringResource("unit_cm") else stringResource("unit_in")), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF263238))
                OutlinedTextField(
                    value = lengthText,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) lengthText = it },
                    placeholder = {
                        if (!isLengthFocused) {
                            Text(
                                text = if (isKg) "CM" else "INC",
                                color = Color(0xFF78909C),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    modifier = Modifier
                        .width(100.dp)
                        .onFocusChanged { isLengthFocused = it.isFocused },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White.copy(alpha = 0.9f),
                        focusedBorderColor = Color(0xFF455A64),
                        unfocusedBorderColor = Color(0xFFB0BEC5),
                        focusedLabelColor = Color(0xFF455A64)
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, textAlign = TextAlign.Center),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                    .border(BorderStroke(1.dp, Color(0xFFCFD8DC)), RoundedCornerShape(16.dp))
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    PigSilhouetteView(
                        modifier = Modifier.size(320.dp, 200.dp),
                        girthActive = girthText.isNotEmpty(),
                        lengthActive = lengthText.isNotEmpty()
                    )
                }
                
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource("label_girth", if (isKg) stringResource("unit_cm") else stringResource("unit_in")), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF263238))
                    OutlinedTextField(
                        value = girthText,
                        onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) girthText = it },
                        placeholder = {
                            if (!isGirthFocused) {
                                Text(
                                    text = if (isKg) "CM" else "INC",
                                    color = Color(0xFF78909C),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        modifier = Modifier
                            .width(100.dp)
                            .onFocusChanged { isGirthFocused = it.isFocused },
                        textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, textAlign = TextAlign.Center),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White.copy(alpha = 0.9f),
                            focusedBorderColor = Color(0xFF455A64),
                            unfocusedBorderColor = Color(0xFFB0BEC5),
                            focusedLabelColor = Color(0xFF455A64)
                        )
                    )
                }
            }

            // Calculations
            val isEstimated = girth > 0 && length > 0
            val rawWeightLbs = if (isEstimated) calculatePigWeightLbs(girth, length, isKg) else 0.0
            val tapeLiveWeightLbs = if (rawWeightLbs < 150.0 && rawWeightLbs > 0) rawWeightLbs + 7.0 else rawWeightLbs
            val tapeLiveWeightKgs = kotlin.math.round(tapeLiveWeightLbs * 0.453592 * 100.0) / 100.0

            val hasValidWeight = isEstimated
            val effectiveLiveWeightKg = tapeLiveWeightKgs
            val effectiveLiveWeightLbs = kotlin.math.round(tapeLiveWeightLbs * 100.0) / 100.0
            val effectiveCarcassWeightKg = kotlin.math.round(effectiveLiveWeightKg * 0.72 * 100.0) / 100.0
            val effectiveCarcassWeightLbs = kotlin.math.round(effectiveLiveWeightLbs * 0.72 * 100.0) / 100.0

            if (hasValidWeight) {
                Spacer(Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))
                ResultRow(
                    label = stringResource("label_estimated_live_weight"),
                    value = stringResource("weight_format", effectiveLiveWeightKg, effectiveLiveWeightLbs),
                    color = Color(0xFF455A64)
                )
                Spacer(Modifier.height(16.dp))
                ResultRow(
                    label = stringResource("label_estimated_carcass_weight"),
                    value = stringResource("weight_format", effectiveCarcassWeightKg, effectiveCarcassWeightLbs),
                    color = Color(0xFF37474F)
                )

                // Growth Intelligence Card (ADG & Market Projection)
                selectedPig?.let { pig ->
                    val ageDays = DateUtils.calculateAgeDays(pig.birthDate)
                    val birthWeightKg = 1.3
                    val lifetimeAdgGrams = if (ageDays > 0) {
                        ((effectiveLiveWeightKg - birthWeightKg) / ageDays * 1000.0).coerceAtLeast(0.0)
                    } else 0.0

                    val targetWeightKg = 90.0
                    val weightRemainingKg = (targetWeightKg - effectiveLiveWeightKg).coerceAtLeast(0.0)
                    val daysToMarket = if (lifetimeAdgGrams > 50.0 && weightRemainingKg > 0.0) {
                        (weightRemainingKg / (lifetimeAdgGrams / 1000.0)).roundToInt()
                    } else null
                    val targetMarketDate = daysToMarket?.let {
                        DateUtils.addDaysToDate(DateUtils.getCurrentDateDisplay(Locale.getDefault()), it)
                    }

                    Spacer(Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
                        border = BorderStroke(1.dp, Color(0xFFAED581))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource("growth_intelligence"), fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20), style = MaterialTheme.typography.titleSmall)
                                }
                                val (ratingText, ratingColor) = when {
                                    lifetimeAdgGrams >= 700.0 -> stringResource("optimal_growth") to Color(0xFF2E7D32)
                                    lifetimeAdgGrams >= 500.0 -> stringResource("normal_growth") to Color(0xFF1565C0)
                                    else -> stringResource("low_adg") to Color(0xFFE65100)
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = ratingColor.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = ratingText,
                                        color = ratingColor,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "• ${stringResource("lifetime_adg")}: ${String.format(Locale.US, "%.0f", lifetimeAdgGrams)} g/day (${stringResource("age_label")}: $ageDays ${stringResource("days")})",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF33691E)
                            )
                            if (effectiveLiveWeightKg >= 90.0) {
                                Text(
                                    text = "• 🎉 ${stringResource("market_ready_desc")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            } else if (targetMarketDate != null) {
                                Text(
                                    text = "• ${stringResource("projected_market_date")}: $targetMarketDate (~$daysToMarket ${stringResource("days_remaining")})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF33691E)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource("save_to_pig_profile"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF455A64),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            
            Spacer(Modifier.height(12.dp))
            
            val filteredPigs = remember(pigs, pigSearchQuery) {
                pigs.asSequence()
                    .filter { it.tagNumber.isNotBlank() && it.tagNumber.contains(pigSearchQuery, ignoreCase = true) }
                    .sortedBy { it.tagNumber }
                    .toList()
            }
            
            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = !dropdownExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = pigSearchQuery,
                    onValueChange = { 
                        pigSearchQuery = it
                        dropdownExpanded = true
                        if (selectedPig?.tagNumber != it) {
                            selectedPig = null
                        }
                    },
                    label = { Text(stringResource("select_pig_optional")) },
                    placeholder = { Text(stringResource("search_tag_hint")) },
                    trailingIcon = { 
                        if (selectedPig != null) {
                            IconButton(onClick = { 
                                selectedPig = null
                                pigSearchQuery = ""
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = stringResource("clear_selection"))
                            }
                        } else {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White.copy(alpha = 0.9f),
                        focusedBorderColor = Color(0xFF455A64),
                        unfocusedBorderColor = Color(0xFFB0BEC5),
                        focusedLabelColor = Color(0xFF455A64)
                    ),
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth()
                )
                
                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    if (filteredPigs.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text(stringResource("no_pigs_found")) },
                            onClick = {},
                            enabled = false
                        )
                    } else {
                        filteredPigs.forEach { pig ->
                            DropdownMenuItem(
                                text = { Text("${pig.tagNumber} (${pig.status})") },
                                onClick = {
                                    selectedPig = pig
                                    pigSearchQuery = pig.tagNumber
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            
            selectedPig?.let { pig ->
                val weightSavedSuccessMsg = stringResource("weight_saved_success", pig.tagNumber)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource("current_weight_label", pig.weight),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        if (effectiveLiveWeightKg > 0) {
                            onUpdatePigWeight(pig.id, effectiveLiveWeightKg)
                            Toast.makeText(
                                context,
                                weightSavedSuccessMsg,
                                Toast.LENGTH_LONG
                            ).show()
                            selectedPig = null
                            pigSearchQuery = ""
                            lengthText = ""
                            girthText = ""
                        }
                    },
                    enabled = hasValidWeight && effectiveLiveWeightKg > 0,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF455A64),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFB0BEC5),
                        disabledContentColor = Color.White.copy(alpha = 0.7f)
                    )
                ) {
                    Text(
                        text = if (hasValidWeight && effectiveLiveWeightKg > 0)
                            stringResource("save_weight_btn_label", effectiveLiveWeightKg, pig.tagNumber)
                        else "Enter measurements above to save",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScaleMeasurementCard(
    isKg: Boolean,
    onUnitChange: (Boolean) -> Unit,
    pigs: List<Pig> = emptyList(),
    initialPigTag: String? = null,
    onUpdatePigWeight: (String, Double) -> Unit = { _, _ -> },
) {
    var directWeightText by remember { mutableStateOf("") }
    var selectedPig by remember { mutableStateOf<Pig?>(null) }
    var pigSearchQuery by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(pigs, initialPigTag) {
        if (!initialPigTag.isNullOrBlank()) {
            val found = pigs.find { it.tagNumber.equals(initialPigTag, ignoreCase = true) || it.id == initialPigTag }
            if (found != null) {
                selectedPig = found
                pigSearchQuery = found.tagNumber
            } else {
                pigSearchQuery = initialPigTag
            }
        }
    }

    val directWeightVal = directWeightText.toDoubleOrNull() ?: 0.0
    val effectiveLiveWeightKg = if (isKg) directWeightVal else directWeightVal * 0.453592
    val effectiveLiveWeightLbs = if (isKg) directWeightVal / 0.453592 else directWeightVal
    val roundedLiveKg = kotlin.math.round(effectiveLiveWeightKg * 100.0) / 100.0
    val roundedLiveLbs = kotlin.math.round(effectiveLiveWeightLbs * 100.0) / 100.0
    val effectiveCarcassWeightKg = kotlin.math.round(roundedLiveKg * 0.72 * 100.0) / 100.0
    val effectiveCarcassWeightLbs = kotlin.math.round(roundedLiveLbs * 0.72 * 100.0) / 100.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFECEFF1)),
        border = BorderStroke(1.dp, Color(0xFFCFD8DC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Scale, null, tint = Color(0xFF455A64))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource("weigh_with_scale"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF263238),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Enter numeric scale reading directly from hanging balance or platform scale",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF546E7A),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(14.dp))

            // Unit Toggle Centered
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource("unit_lbs"), style = MaterialTheme.typography.labelSmall, color = Color(0xFF263238))
                Switch(
                    checked = isKg,
                    onCheckedChange = onUnitChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF455A64),
                        checkedBorderColor = Color(0xFF455A64)
                    ),
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .scale(0.7f)
                )
                Text(stringResource("unit_kgs"), style = MaterialTheme.typography.labelSmall, color = Color(0xFF263238))
            }

            Spacer(Modifier.height(16.dp))

            // Scale Reading Numeric Input
            OutlinedTextField(
                value = directWeightText,
                onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) directWeightText = it },
                label = { Text("${stringResource("scale_reading")} (${if (isKg) stringResource("kg") else stringResource("lbs")})") },
                placeholder = { Text("0.0", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White.copy(alpha = 0.9f),
                    focusedBorderColor = Color(0xFF455A64),
                    unfocusedBorderColor = Color(0xFFB0BEC5),
                    focusedLabelColor = Color(0xFF455A64)
                )
            )


            if (directWeightVal > 0.0) {
                Spacer(Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))
                ResultRow(
                    label = stringResource("label_estimated_live_weight"),
                    value = stringResource("weight_format", roundedLiveKg, roundedLiveLbs),
                    color = Color(0xFF455A64)
                )
                Spacer(Modifier.height(16.dp))
                ResultRow(
                    label = stringResource("label_estimated_carcass_weight"),
                    value = stringResource("weight_format", effectiveCarcassWeightKg, effectiveCarcassWeightLbs),
                    color = Color(0xFF37474F)
                )

                // Growth Intelligence Card (ADG & Market Projection)
                selectedPig?.let { pig ->
                    val ageDays = DateUtils.calculateAgeDays(pig.birthDate)
                    val birthWeightKg = 1.3
                    val lifetimeAdgGrams = if (ageDays > 0) {
                        ((roundedLiveKg - birthWeightKg) / ageDays * 1000.0).coerceAtLeast(0.0)
                    } else 0.0

                    val targetWeightKg = 90.0
                    val weightRemainingKg = (targetWeightKg - roundedLiveKg).coerceAtLeast(0.0)
                    val daysToMarket = if (lifetimeAdgGrams > 50.0 && weightRemainingKg > 0.0) {
                        (weightRemainingKg / (lifetimeAdgGrams / 1000.0)).roundToInt()
                    } else null
                    val targetMarketDate = daysToMarket?.let {
                        DateUtils.addDaysToDate(DateUtils.getCurrentDateDisplay(Locale.getDefault()), it)
                    }

                    Spacer(Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
                        border = BorderStroke(1.dp, Color(0xFFAED581))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource("growth_intelligence"), fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20), style = MaterialTheme.typography.titleSmall)
                                }
                                val (ratingText, ratingColor) = when {
                                    lifetimeAdgGrams >= 700.0 -> stringResource("optimal_growth") to Color(0xFF2E7D32)
                                    lifetimeAdgGrams >= 500.0 -> stringResource("normal_growth") to Color(0xFF1565C0)
                                    else -> stringResource("low_adg") to Color(0xFFE65100)
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = ratingColor.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = ratingText,
                                        color = ratingColor,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "• ${stringResource("lifetime_adg")}: ${String.format(Locale.US, "%.0f", lifetimeAdgGrams)} g/day (${stringResource("age_label")}: $ageDays ${stringResource("days")})",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF33691E)
                            )
                            if (roundedLiveKg >= 90.0) {
                                Text(
                                    text = "• 🎉 ${stringResource("market_ready_desc")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            } else if (targetMarketDate != null) {
                                Text(
                                    text = "• ${stringResource("projected_market_date")}: $targetMarketDate (~$daysToMarket ${stringResource("days_remaining")})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF33691E)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource("save_to_pig_profile"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF455A64),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            
            Spacer(Modifier.height(12.dp))
            
            val filteredPigs = remember(pigs, pigSearchQuery) {
                pigs.asSequence()
                    .filter { it.tagNumber.isNotBlank() && it.tagNumber.contains(pigSearchQuery, ignoreCase = true) }
                    .sortedBy { it.tagNumber }
                    .toList()
            }
            
            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = !dropdownExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = pigSearchQuery,
                    onValueChange = { 
                        pigSearchQuery = it
                        dropdownExpanded = true
                        if (selectedPig?.tagNumber != it) {
                            selectedPig = null
                        }
                    },
                    label = { Text(stringResource("select_pig_optional")) },
                    placeholder = { Text(stringResource("search_tag_hint")) },
                    trailingIcon = { 
                        if (selectedPig != null) {
                            IconButton(onClick = { 
                                selectedPig = null
                                pigSearchQuery = ""
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = stringResource("clear_selection"))
                            }
                        } else {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White.copy(alpha = 0.9f),
                        focusedBorderColor = Color(0xFF455A64),
                        unfocusedBorderColor = Color(0xFFB0BEC5),
                        focusedLabelColor = Color(0xFF455A64)
                    ),
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth()
                )
                
                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    if (filteredPigs.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text(stringResource("no_pigs_found")) },
                            onClick = {},
                            enabled = false
                        )
                    } else {
                        filteredPigs.forEach { pig ->
                            DropdownMenuItem(
                                text = { Text("${pig.tagNumber} (${pig.status})") },
                                onClick = {
                                    selectedPig = pig
                                    pigSearchQuery = pig.tagNumber
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            
            selectedPig?.let { pig ->
                val weightSavedSuccessMsg = stringResource("weight_saved_success", pig.tagNumber)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource("current_weight_label", pig.weight),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        if (roundedLiveKg > 0.0) {
                            onUpdatePigWeight(pig.id, roundedLiveKg)
                            Toast.makeText(
                                context,
                                weightSavedSuccessMsg,
                                Toast.LENGTH_LONG
                            ).show()
                            selectedPig = null
                            pigSearchQuery = ""
                            directWeightText = ""
                        }
                    },
                    enabled = directWeightVal > 0.0 && roundedLiveKg > 0.0,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF455A64),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFB0BEC5),
                        disabledContentColor = Color.White.copy(alpha = 0.7f)
                    )
                ) {
                    Text(
                        text = if (directWeightVal > 0.0 && roundedLiveKg > 0.0)
                            stringResource("save_weight_btn_label", roundedLiveKg, pig.tagNumber)
                        else "Enter scale reading above to save",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ResultRow(label: String, value: String, color: Color) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = color, textAlign = TextAlign.Center)
    }
}

@Composable
fun PigSilhouetteView(modifier: Modifier = Modifier, girthActive: Boolean, lengthActive: Boolean) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // New pig silhouette with scale lines already included in the image
        Image(
            painter = painterResource(id = R.drawable.ic_pig_scale),
            contentDescription = stringResource("pig_diagram_content_description"),
            modifier = Modifier.fillMaxSize(),
            // Apply a slight tint or alpha if needed, but keeping it full for clarity of the lines
            alpha = if (lengthActive || girthActive) 1f else 0.5f
        )
    }
}

@Composable
fun CarcassWeightCard(modifier: Modifier = Modifier) {
    var liveWeightText by remember { mutableStateOf("") }
    var isHeadOn by remember { mutableStateOf(true) }

    val liveWeight = liveWeightText.toDoubleOrNull() ?: 0.0
    val activeDressingPct = if (isHeadOn) 72.0 else 68.0
    val carcassKg = liveWeight * (activeDressingPct / 100.0)
    val carcassLbs = carcassKg * 2.20462
    val usableMeatKg = carcassKg * 0.78
    val usableMeatLbs = usableMeatKg * 2.20462
    val visceraKg = maxOf(0.0, liveWeight - carcassKg)
    val visceraLbs = visceraKg * 2.20462

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Scale, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource("find_carcass_weight"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = stringResource("calc_carcass_desc"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // Live weight input
            OutlinedTextField(
                value = liveWeightText,
                onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) liveWeightText = it },
                label = { Text("${stringResource("live_weight_input")} (kg)") },
                placeholder = { Text(stringResource("eg_100")) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Dressing Type Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource("dressing_type"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isHeadOn) Color(0xFF2E7D32) else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, if (isHeadOn) Color(0xFF1B5E20) else MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isHeadOn = true }
                    ) {
                        Text(
                            text = "${stringResource("head_on")} (~74%)",
                            modifier = Modifier.padding(vertical = 10.dp),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isHeadOn) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (!isHeadOn) Color(0xFF2E7D32) else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, if (!isHeadOn) Color(0xFF1B5E20) else MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isHeadOn = false }
                    ) {
                        Text(
                            text = "${stringResource("head_off")} (~68%)",
                            modifier = Modifier.padding(vertical = 10.dp),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (!isHeadOn) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (liveWeight > 0) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Carcass Weight
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Estimated Carcass Weight",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${String.format(Locale.getDefault(), "%.1f", activeDressingPct)}% dressing",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${String.format(Locale.getDefault(), "%.1f", carcassKg)} kg (${String.format(Locale.getDefault(), "%.1f", carcassLbs)} lbs)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF2E7D32)
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Usable Meat Yield
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = stringResource("estimated_usable_meat"),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "~78% of carcass weight",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${String.format(Locale.getDefault(), "%.1f", usableMeatKg)} kg (${String.format(Locale.getDefault(), "%.1f", usableMeatLbs)} lbs)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF1976D2)
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // Viscera / Gut deduction
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = stringResource("viscera_deduction"),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Blood, viscera & gut fill",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "-${String.format(Locale.getDefault(), "%.1f", visceraKg)} kg (${String.format(Locale.getDefault(), "%.1f", visceraLbs)} lbs)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC62828)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun calculatePigWeightLbs(girth: Double, length: Double, isKg: Boolean): Double {
    // Standard Formula: (Heart Girth² * Length) / 400 = Lbs
    // Requirements: girth and length in inches.
    
    val gIn = if (isKg) girth / 2.54 else girth
    val lIn = if (isKg) length / 2.54 else length
    
    return (gIn * gIn * lIn) / 400.0
}

@Preview(showBackground = true)
@Composable
fun TapeMeasurementResultsPreview() {
    SmartSwineTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            // Mocking the ResultRow usage as it appears in TapeMeasurementCard
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource("estimated_results"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))
                    
                    ResultRow(
                        label = stringResource("label_estimated_live_weight"),
                        value = stringResource("weight_format", 150.5, 331.8),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(16.dp))
                    ResultRow(
                        label = stringResource("label_estimated_carcass_weight"),
                        value = stringResource("weight_format", 108.4, 239.0),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}
