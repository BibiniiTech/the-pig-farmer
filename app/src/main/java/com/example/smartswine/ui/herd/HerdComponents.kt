package com.example.smartswine.ui.herd

import com.example.smartswine.utils.stringResource
import com.example.smartswine.model.Pig
import com.example.smartswine.utils.DateUtils
import androidx.compose.foundation.layout.*

import androidx.compose.material3.*
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PigItem(pig: Pig, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val performance = remember(pig.breed, pig.birthDate, pig.weight) {
                val birthDate = DateUtils.parseInternal(pig.birthDate)
                val ageDays = if (birthDate != null) {
                    val diffMs = System.currentTimeMillis() - birthDate.time
                    (diffMs / (1000 * 60 * 60 * 24)).toInt()
                } else -1
                
                com.example.smartswine.utils.SwineGrowthDatabase.evaluatePerformance(
                    breed = pig.breed,
                    ageDays = ageDays,
                    actualWeight = pig.weight
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${stringResource("tag")}: ${pig.tagNumber}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                PerformanceTag(performance)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                val ageMonths = remember(pig.birthDate) { DateUtils.calculateAgeMonths(pig.birthDate) }
                val ageDisplay = when {
                    ageMonths < 0 -> stringResource("future_birth")
                    ageMonths == 0 -> stringResource("less_than_1_month")
                    ageMonths < 12 -> "$ageMonths ${stringResource("months")}"
                    else -> {
                        val years = ageMonths / 12
                        val months = ageMonths % 12
                        if (months == 0) "$years ${stringResource("years_abbr")}" 
                        else "$years ${stringResource("years_abbr")}, $months ${stringResource("months_abbr")}"
                    }
                }
                InfoChip(label = stringResource("age"), value = ageDisplay)
                Spacer(modifier = Modifier.width(8.dp))
                val genderDisplay = (if (pig.gender == "Male") stringResource("male") else stringResource("female")) + 
                    if ((pig.gender == "Male") && (pig.castrated == true)) " ${stringResource("castrated_label")}" else ""
                InfoChip(label = stringResource("gender"), value = genderDisplay)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoChip(label = stringResource("weight"), value = "${pig.weight} ${stringResource("kg")}")
                Spacer(modifier = Modifier.width(8.dp))
                InfoChip(label = stringResource("location_pen"), value = pig.location)
            }
            if (pig.status.startsWith("Archived")) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = pig.status,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagAutoCompleteField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<String>,
) {
    val expanded = remember { mutableStateOf(value = false) }
    val filteredSuggestions = remember(suggestions, value) {
        suggestions.filter { it.contains(value, ignoreCase = true) }
    }

    val shouldShowMenu = expanded.value && filteredSuggestions.isNotEmpty()

    ExposedDropdownMenuBox(
        expanded = shouldShowMenu,
        onExpandedChange = { expanded.value = it }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded.value = true
            },
            label = { Text(label) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryEditable),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = shouldShowMenu) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )
        ExposedDropdownMenu(
            expanded = shouldShowMenu,
            onDismissRequest = { expanded.value = false }
        ) {
            filteredSuggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = { Text(suggestion) },
                    onClick = {
                        onValueChange(suggestion)
                        expanded.value = false
                    }
                )
            }
        }
    }
}

@Composable
fun InfoChip(label: String, value: String) {
    Row {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun InfoRow(label: String, value: String, icon: ImageVector? = null) {
    if (value.isNotEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun PurposeTag(purpose: String) {
    val translatedPurpose = when (purpose) {
        "Breeder" -> stringResource("breeder")
        "Porker" -> stringResource("porker")
        else -> purpose
    }
    Surface(
        color = when (purpose) {
            "Breeder" -> Color(0xFFE1BEE7)
            "Porker" -> Color(0xFFC8E6C9)
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = translatedPurpose,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = when (purpose) {
                "Breeder" -> Color(0xFF4A148C)
                "Porker" -> Color(0xFF2E7D32)
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
fun PerformanceTag(performance: String) {
    val translatedPerformance = when (performance) {
        "Excellent" -> stringResource("excellent")
        "Good" -> stringResource("good")
        "Caution" -> stringResource("caution")
        "Poor" -> stringResource("poor")
        "Blank" -> stringResource("blank")
        else -> performance
    }
    Surface(
        color = when (performance) {
            "Excellent" -> Color(0xFFFEF3C7) // Gold background
            "Good" -> Color(0xFFC8E6C9)      // Green background
            "Caution" -> Color(0xFFFFF9C4)   // Yellow background
            "Poor" -> Color(0xFFFFCDD2)      // Red background
            "Blank" -> Color(0xFFE0E0E0)     // Neutral gray background
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = translatedPerformance,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            color = when (performance) {
                "Excellent" -> Color(0xFFB45309) // Dark gold text
                "Good" -> Color(0xFF2E7D32)      // Dark green text
                "Caution" -> Color(0xFFF57F17)   // Dark yellow/orange text
                "Poor" -> Color(0xFFC62828)      // Dark red text
                "Blank" -> Color(0xFF616161)     // Neutral gray text
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreedDropdownField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = com.example.smartswine.utils.SwineGrowthDatabase.standardOptions
    var expanded by remember { mutableStateOf(false) }

    val selectedOption = remember(value) {
        if (value.isEmpty()) ""
        else if (options.contains(value)) value
        else "Other"
    }

    var customBreedValue by remember { mutableStateOf(if (selectedOption == "Other") value else "") }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box {
            OutlinedTextField(
                value = selectedOption,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource("breed")) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { expanded = true }
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        expanded = false
                        if (option == "Other") {
                            onValueChange(customBreedValue)
                        } else {
                            onValueChange(option)
                        }
                    }
                )
            }
        }

        if (selectedOption == "Other") {
            OutlinedTextField(
                value = customBreedValue,
                onValueChange = {
                    customBreedValue = it
                    onValueChange(it)
                },
                label = { Text(stringResource("specify_breed")) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. Berkshire x Large White") }
            )
        }
    }
}


