package com.example.smartswine.ui.weight

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import com.bibiniitech.smartswine.R
import androidx.compose.ui.res.painterResource
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
import com.example.smartswine.model.Pig
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightCheckerScreen(
    onBack: () -> Unit,
    pigs: List<Pig> = emptyList(),
    onUpdatePigWeight: (String, Double) -> Unit = { _, _ -> }
) {
    var isKg by remember { mutableStateOf(true) }
    
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
                        text = stringResource("weight_checker_title"),
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            WeightConverterCard()
            
            StylishDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            TapeMeasurementCard(
                isKg = isKg,
                onUnitChange = { isKg = it },
                pigs = pigs,
                onUpdatePigWeight = onUpdatePigWeight
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource("important_notes_title"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource("weight_checker_notes"),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Justify
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun WeightConverterCard() {
    var lbsText by remember { mutableStateOf("") }
    var kgsText by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Scale, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(stringResource("converter_title"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
                        if (newValue.isEmpty() || newValue.toDoubleOrNull() != null) {
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
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                )

                Text(
                    "=",
                    modifier = Modifier.padding(horizontal = 12.dp),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )

                // Kgs box
                OutlinedTextField(
                    value = kgsText,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.toDoubleOrNull() != null) {
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
    onUpdatePigWeight: (String, Double) -> Unit = { _, _ -> }
) {
    var girthText by remember { mutableStateOf("") }
    var lengthText by remember { mutableStateOf("") }
    
    var selectedPig by remember { mutableStateOf<Pig?>(null) }
    var pigSearchQuery by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val girth = girthText.toDoubleOrNull() ?: 0.0
    val length = lengthText.toDoubleOrNull() ?: 0.0

    LaunchedEffect(girth, length) {
        if (girth <= 0 || length <= 0) {
            selectedPig = null
            pigSearchQuery = ""
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource("weigh_with_tape_title"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                stringResource("weigh_with_tape_subtitle"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            // Unit Toggle Centered
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource("unit_lbs"), style = MaterialTheme.typography.labelSmall)
                Switch(
                    checked = isKg,
                    onCheckedChange = onUnitChange,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .scale(0.7f)
                )
                Text(stringResource("unit_kgs"), style = MaterialTheme.typography.labelSmall)
            }
            
            Spacer(Modifier.height(24.dp))
            
            // Length Box Centered Above Diagram
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource("label_length", if (isKg) stringResource("unit_cm") else stringResource("unit_in")), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = lengthText,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) lengthText = it },
                    placeholder = { Text(stringResource("placeholder_a")) },
                    modifier = Modifier.width(100.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, textAlign = TextAlign.Center),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Box for Diagram and Girth Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            ) {
                // Centered Diagram
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    PigSilhouetteView(
                        modifier = Modifier.size(320.dp, 200.dp),
                        girthActive = girthText.isNotEmpty(),
                        lengthActive = lengthText.isNotEmpty()
                    )
                }
                
                // Girth Box - Low Left Side
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource("label_girth", if (isKg) stringResource("unit_cm") else stringResource("unit_in")), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = girthText,
                        onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) girthText = it },
                        placeholder = { Text(stringResource("placeholder_b")) },
                        modifier = Modifier.width(100.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, textAlign = TextAlign.Center),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        )
                    )
                }
            }
            
            if (girth > 0 && length > 0) {
                val rawWeightLbs = calculatePigWeightLbs(girth, length, isKg)
                val liveWeightLbs = if (rawWeightLbs < 150.0 && rawWeightLbs > 0) rawWeightLbs + 7.0 else rawWeightLbs
                val liveWeightKgs = Math.round(liveWeightLbs * 0.453592 * 100.0) / 100.0
                
                val carcassWeightLbs = liveWeightLbs * 0.72
                val carcassWeightKgs = Math.round(liveWeightKgs * 0.72 * 100.0) / 100.0

                Spacer(Modifier.height(24.dp))
                
                HorizontalDivider()
                
                Spacer(Modifier.height(16.dp))
                
                ResultRow(
                    label = stringResource("label_estimated_live_weight"),
                    value = stringResource("weight_format", liveWeightKgs, liveWeightLbs),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(16.dp))
                ResultRow(
                    label = stringResource("label_estimated_carcass_weight"),
                    value = stringResource("weight_format", carcassWeightKgs, carcassWeightLbs),
                    color = MaterialTheme.colorScheme.secondary
                )

                Spacer(Modifier.height(24.dp))
                
                HorizontalDivider()
                
                Spacer(Modifier.height(16.dp))
                
                Text(
                    text = stringResource("save_to_pig_profile"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                
                Spacer(Modifier.height(12.dp))
                
                val filteredPigs = remember(pigs, pigSearchQuery) {
                    pigs.filter { it.tagNumber.isNotBlank() && it.tagNumber.contains(pigSearchQuery, ignoreCase = true) }
                        .sortedBy { it.tagNumber }
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
                        placeholder = { Text("Search tag number...") },
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
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        if (filteredPigs.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No pigs found") },
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
                            onUpdatePigWeight(pig.id, liveWeightKgs)
                            Toast.makeText(
                                context,
                                weightSavedSuccessMsg,
                                Toast.LENGTH_LONG
                            ).show()
                            selectedPig = null
                            pigSearchQuery = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = stringResource("save_weight_btn_label", liveWeightKgs, pig.tagNumber),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.padding(top = 24.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource("msg_enter_measurements"), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
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

private fun calculatePigWeightLbs(girth: Double, length: Double, isKg: Boolean): Double {
    // Standard Formula: (Heart Girth² * Length) / 400 = Lbs
    // Requirements: girth and length in inches.
    
    val gIn = if (isKg) girth / 2.54 else girth
    val lIn = if (isKg) length / 2.54 else length
    
    return (gIn * gIn * lIn) / 400.0
}

@Preview(showBackground = true)
@Composable
fun WeightCheckerPreview() {
    SmartSwineTheme {
        WeightCheckerScreen(onBack = {})
    }
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
