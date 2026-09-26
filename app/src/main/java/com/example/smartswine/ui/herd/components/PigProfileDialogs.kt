package com.example.smartswine.ui.herd.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.smartswine.model.HealthRecord
import com.example.smartswine.model.Pig
import com.example.smartswine.ui.herd.BreedDropdownField
import com.example.smartswine.ui.herd.TagAutoCompleteField
import com.example.smartswine.ui.settings.SettingsViewModel
import com.example.smartswine.utils.*
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPigDialog(pig: Pig, allPigs: List<Pig>, onDismiss: () -> Unit, onConfirm: (Pig) -> Unit) {
    val tagNumber = remember { mutableStateOf(pig.tagNumber) }
    val birthDate = remember { mutableStateOf(pig.birthDate) }
    val breed = remember { mutableStateOf(pig.breed) }
    val gender = remember { mutableStateOf(pig.gender) }
    val castrated = remember { mutableStateOf(pig.castrated) }
    val hasFarrowed = remember { mutableStateOf(pig.hasFarrowed) }
    val parityState = remember { mutableStateOf(pig.parity.toString()) }
    val weightState = remember { mutableStateOf(pig.weight.toString()) }
    val purpose = remember { mutableStateOf(pig.purpose) }
    val sowTag = remember { mutableStateOf(pig.sowTag) }
    val boarTag = remember { mutableStateOf(pig.boarTag) }
    val location = remember { mutableStateOf(pig.location) }
    val source = remember { mutableStateOf(pig.source) }
    val notes = remember { mutableStateOf(pig.notes) }

    val showDatePicker = remember { mutableStateOf(value = false) }
    val datePickerState = rememberDatePickerState()

    val sowTags = remember(allPigs) { allPigs.asSequence().filter { it.gender == "Female" }.map { it.tagNumber }.distinct().sorted().toList() }
    val boarTags = remember(allPigs) { allPigs.asSequence().filter { it.gender == "Male" }.map { it.tagNumber }.distinct().sorted().toList() }

    val scrollState = rememberScrollState()

    if (showDatePicker.value) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker.value = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val calendar = Calendar.getInstance().apply { timeInMillis = it }
                        birthDate.value = String.format(Locale.getDefault(), "%02d/%02d/%d", 
                            calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR))
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
        onDismissRequest = { },
        properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = true),
        title = { Text(stringResource("edit_pig_title")) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                OutlinedTextField(value = tagNumber.value, onValueChange = { tagNumber.value = it }, label = { Text(stringResource("tag_number")) }, modifier = Modifier.fillMaxWidth())

                OutlinedTextField(
                    value = birthDate.value,
                    onValueChange = { birthDate.value = it },
                    label = { Text(stringResource("dob")) },
                    placeholder = { Text("DD/MM/YYYY") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker.value = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = stringResource("select_date"))
                        }
                    }
                )

                BreedDropdownField(value = breed.value, onValueChange = { breed.value = it })

                Text(stringResource("gender"), style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = gender.value == "Male", onClick = { gender.value = "Male" })
                    Text(stringResource("male"))
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = gender.value == "Female", onClick = { gender.value = "Female"; castrated.value = null })
                    Text(stringResource("female"))
                }

                if (gender.value == "Male") {
                    Text(stringResource("castrated_q"), style = MaterialTheme.typography.labelLarge)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = castrated.value == true, onClick = { castrated.value = true })
                        Text(stringResource("yes"))
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(selected = castrated.value == false, onClick = { castrated.value = false })
                        Text(stringResource("no"))
                    }
                }

                if (gender.value == "Female") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = hasFarrowed.value, onCheckedChange = { hasFarrowed.value = it })
                        Text(stringResource("has_farrowed"))
                    }
                    if (hasFarrowed.value) {
                        OutlinedTextField(
                            value = parityState.value,
                            onValueChange = { parityState.value = it },
                            label = { Text(stringResource("parity_number_of_litters")) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                OutlinedTextField(value = weightState.value, onValueChange = { weightState.value = it }, label = { Text(stringResource("weight_kg_label")) }, modifier = Modifier.fillMaxWidth())

                Text(stringResource("purpose"), style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = purpose.value == "Breeder", onClick = { purpose.value = "Breeder" })
                    Text(stringResource("breeder"))
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = purpose.value == "Porker", onClick = { purpose.value = "Porker" })
                    Text(stringResource("porker"))
                }

                TagAutoCompleteField(label = stringResource("sow_tag"), value = sowTag.value, onValueChange = { sowTag.value = it }, suggestions = sowTags)
                TagAutoCompleteField(label = stringResource("boar_tag"), value = boarTag.value, onValueChange = { boarTag.value = it }, suggestions = boarTags)

                OutlinedTextField(value = location.value, onValueChange = { location.value = it }, label = { Text(stringResource("location_pen")) }, modifier = Modifier.fillMaxWidth())

                Text(stringResource("source"), style = MaterialTheme.typography.labelLarge)
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = source.value == "Born on farm", onClick = { source.value = "Born on farm" })
                        Text(stringResource("born_on_farm"))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = source.value == "Brought to farm", onClick = { source.value = "Brought to farm" })
                        Text(stringResource("brought_to_farm"))
                    }
                }

                OutlinedTextField(
                    value = notes.value,
                    onValueChange = { notes.value = it },
                    label = { Text(stringResource("notes")) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(pig.copy(
                    tagNumber = tagNumber.value,
                    birthDate = birthDate.value,
                    breed = breed.value,
                    gender = gender.value,
                    castrated = if (gender.value == "Male") castrated.value else null,
                    hasFarrowed = hasFarrowed.value,
                    parity = if (gender.value == "Female" && hasFarrowed.value) {
                        parityState.value.toIntOrNull() ?: pig.parity
                    } else if (hasFarrowed.value == false) {
                        0
                    } else {
                        pig.parity
                    },
                    weight = weightState.value.toDoubleOrNull() ?: 0.0,
                    purpose = purpose.value,
                    sowTag = sowTag.value,
                    boarTag = boarTag.value,
                    location = location.value,
                    source = source.value,
                    notes = notes.value
                ))
            }) { Text(stringResource("save")) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource("cancel")) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditHealthRecordDialog(
    pig: Pig,
    onDismiss: () -> Unit,
    onConfirm: (HealthRecord, Boolean, Boolean, Boolean, Map<String, Any>) -> Unit,
    existingRecord: HealthRecord? = null,
    onDelete: (String) -> Unit = {}
) {
    val languageCode = LocalAppLanguage.current.code
    val isPiglet = pig.status.equals("Piglet", ignoreCase = true)
    val isFemale = pig.gender.equals("Female", ignoreCase = true)
    val isMale = pig.gender.equals("Male", ignoreCase = true)
    val currencySymbol = remember { SettingsViewModel.getInstance().currencySymbol.value }
    
    val isBreedingFemale = isFemale && (listOf("Sow", "Gilt", "Pregnant", "Lactating", "Nursing", "Finisher").contains(pig.status))
    val canFarrow = isFemale && (pig.status == "Pregnant" || pig.status == "Sow" || pig.status == "Lactating" || pig.status == "Nursing" || pig.hasFarrowed)
    val canWean = isPiglet || (isFemale && (pig.status == "Lactating" || pig.status == "Nursing"))

    val standardTypes = remember(pig) {
        mutableListOf(
            "Vaccination", "Deworming", "Medication", "Culling", "Custom"
        ).apply {
            if (isPiglet) {
                addAll(listOf("Teeth Clipping", "Tail Docking", "Iron Injection"))
                if (isMale) add("Castration")
            }
            if (canWean) {
                add("Weaning")
            }
            if (isBreedingFemale) {
                addAll(listOf("Heat Detection", "Breeding/Mating", "Pregnancy Check"))
            }
            if (canFarrow) {
                add("Farrowing")
            }
        }.distinct().sorted().toList()
    }

    val type = remember { 
        mutableStateOf(
            if (existingRecord == null) {
                standardTypes.firstOrNull { it != "Custom" } ?: standardTypes.firstOrNull() ?: "Custom"
            } else {
                if (standardTypes.contains(existingRecord.type)) existingRecord.type else "Custom"
            }
        ) 
    }
    
    val initialNotes = remember(existingRecord?.description) {
        existingRecord?.description?.let { desc ->
            var temp = desc
            val prefixes = listOf(
                "\nMated:", "Mated:",
                "\nFarrowed:", "Farrowed:",
                "\nWeight updated", "Weight updated",
                "\nReason:", "Reason:",
                "\nSold for:", "Sold for:",
                "\nMedication/Vaccine:", "Medication/Vaccine:",
                "\nWeaned", "Weaned",
                "\nPregnancy Confirmed", "Pregnancy Confirmed",
                "\nCastrated successfully", "Castrated successfully"
            )
            prefixes.forEach { prefix ->
                temp = temp.substringBefore(prefix)
            }
            temp.trim()
        } ?: ""
    }
    val notes = remember { mutableStateOf(initialNotes) }

    val sowTag = remember { 
        mutableStateOf(
            existingRecord?.description?.let { if (it.contains("Mated: Sow ")) it.substringAfter("Mated: Sow ").substringBefore(" with Boar") else "" } 
                ?: (if (isFemale) pig.tagNumber else "")
        ) 
    }
    val boarTag = remember { 
        mutableStateOf(
            existingRecord?.description?.let { if (it.contains("with Boar ")) it.substringAfter("with Boar ").substringBefore("\n").trim() else "" } 
                ?: (if (isMale) pig.tagNumber else "")
        ) 
    }
    val checkPregnancy = remember { mutableStateOf(false) }
    
    val numMales = remember { mutableStateOf(existingRecord?.description?.let { if (it.contains("Farrowed: ")) it.substringAfter("Farrowed: ").substringBefore(" Males") else "" } ?: "") }
    val numFemales = remember { mutableStateOf(existingRecord?.description?.let { if (it.contains("Males, ")) it.substringAfter("Males, ").substringBefore(" Females") else "" } ?: "") }
    
    val weightState = remember { mutableStateOf(existingRecord?.description?.let { if (it.contains("Weight updated to ")) it.substringAfter("Weight updated to ").substringBefore("kg") else "" } ?: "") }
    val weaningLocation = remember { mutableStateOf(existingRecord?.description?.let { if (it.contains("Weaned and moved to location: ")) it.substringAfter("Weaned and moved to location: ").trim() else "" } ?: "") }
    
    val cullingReason = remember { mutableStateOf(existingRecord?.description?.let { if (it.contains("Reason: ")) it.substringAfter("Reason: ").substringBefore("\n") else "" } ?: "") }
    val salePrice = remember { mutableStateOf(existingRecord?.description?.let { if (it.contains("Sold for:")) it.substringAfter("Sold for:").trim().takeWhile { c -> c.isDigit() || c == '.' } else "" } ?: "") }
    
    val medName = remember {
        val extracted = existingRecord?.description?.let { desc ->
            if (desc.contains("Medication/Vaccine: ")) {
                desc.substringAfter("Medication/Vaccine: ").substringBefore(",").trim()
            } else ""
        }
        mutableStateOf(
            if (extracted.isNullOrEmpty()) existingRecord?.medication ?: ""
            else extracted
        )
    }
    val medDosage = remember { mutableStateOf(existingRecord?.description?.let { if (it.contains("Dosage: ")) it.substringAfter("Dosage: ").trim() else "" } ?: (existingRecord?.dosage ?: "")) }
    val stillbornsState = remember { mutableStateOf(existingRecord?.stillbornCount?.let { if (it > 0) it.toString() else "" } ?: "") }
    val mummiesState = remember { mutableStateOf(existingRecord?.mummiesCount?.let { if (it > 0) it.toString() else "" } ?: "") }
    val litterBirthWeightState = remember { mutableStateOf(existingRecord?.litterBirthWeightKg?.let { if (it > 0.0) it.toString() else "" } ?: "") }
    val withdrawalDaysState = remember { mutableStateOf(existingRecord?.withdrawalPeriodDays?.let { if (it > 0) it.toString() else "" } ?: "") }
    
    val trackHeat = remember { mutableStateOf(value = false) }
    val pregnancyConfirmed = remember { mutableStateOf(existingRecord?.description?.contains("Pregnancy Confirmed") ?: false) }
    
    val customActivityName = remember { mutableStateOf(if (type.value == "Custom") existingRecord?.type ?: "" else "") }

    val appLanguage = LocalAppLanguage.current
    val locale = remember(appLanguage) { appLanguage.toLocale() }

    val initialDateMillis = remember(existingRecord?.date, locale) {
        existingRecord?.date?.let { DateUtils.parseDisplay(it, locale)?.time } ?: System.currentTimeMillis()
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)
    val showDatePicker = remember { mutableStateOf(value = false) }
    
    val scrollState = rememberScrollState()

    if (showDatePicker.value) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker.value = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker.value = false }) { Text(stringResource("ok")) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker.value = false }) { Text(stringResource("cancel")) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val formattedDate = remember(datePickerState.selectedDateMillis, locale) {
        datePickerState.selectedDateMillis?.let {
            DateUtils.formatDateToDisplay(it, locale)
        } ?: DateUtils.getCurrentDateDisplay(locale)
    }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = true),
        title = { 
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val titleText = if (existingRecord == null) {
                    stringResource("log_activity_title", getTranslatedActivityType(type.value))
                } else {
                    stringResource("edit_activity_title")
                }
                Text(titleText)
                if (existingRecord != null) {
                    IconButton(onClick = { onDelete(existingRecord.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource("delete"), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                val expanded = remember { mutableStateOf(false) }

                OutlinedTextField(
                    value = formattedDate,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource("activity_date")) },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker.value = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = stringResource("select_date"))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = pig.tagNumber,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource("target_animal")) },
                    leadingIcon = { Icon(Icons.Default.Tag, null) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    enabled = false
                )

                Box {
                    val translatedTypeLabel = getTranslatedActivityType(type.value)
                    OutlinedTextField(
                        value = translatedTypeLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource("activity_type")) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded.value) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { expanded.value = true })
                }
                
                DropdownMenu(expanded = expanded.value, onDismissRequest = { expanded.value = false }) {
                    standardTypes.forEach { t ->
                        val translatedT = getTranslatedActivityType(t)
                        DropdownMenuItem(text = { Text(translatedT) }, onClick = {
                            type.value = t
                            expanded.value = false
                        })
                    }
                }

                when (type.value) {
                    "Breeding/Mating" -> {
                        OutlinedTextField(
                            value = sowTag.value, 
                            onValueChange = { sowTag.value = it }, 
                            label = { Text(stringResource("sow_tag") + " *") }, 
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = boarTag.value, 
                            onValueChange = { boarTag.value = it }, 
                            label = { Text(stringResource("boar_tag_optional_ai")) }, 
                            placeholder = { Text(stringResource("leave_blank_ai_or_external")) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = checkPregnancy.value, onCheckedChange = { checkPregnancy.value = it })
                            Text(stringResource("schedule_preg_check"))
                        }
                    }
                    "Farrowing" -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = numMales.value, onValueChange = { numMales.value = it }, label = { Text(stringResource("males")) }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            OutlinedTextField(value = numFemales.value, onValueChange = { numFemales.value = it }, label = { Text(stringResource("females")) }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = stillbornsState.value, onValueChange = { stillbornsState.value = it }, label = { Text(stringResource("stillborns")) }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            OutlinedTextField(value = mummiesState.value, onValueChange = { mummiesState.value = it }, label = { Text(stringResource("mummies")) }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            OutlinedTextField(value = litterBirthWeightState.value, onValueChange = { litterBirthWeightState.value = it }, label = { Text(stringResource("litter_wt_kg")) }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                        }
                    }
                    "Vaccination", "Medication", "Deworming", "Iron Injection" -> {
                        OutlinedTextField(value = medName.value, onValueChange = { medName.value = it }, label = { Text(if (type.value == "Vaccination") stringResource("vaccine_name") else stringResource("medication_name")) }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = medDosage.value, onValueChange = { medDosage.value = it }, label = { Text(stringResource("dosage")) }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = withdrawalDaysState.value, onValueChange = { withdrawalDaysState.value = it }, label = { Text(stringResource("withdrawal_period_days")) }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                    "Weight Check" -> {
                        OutlinedTextField(value = weightState.value, onValueChange = { weightState.value = it }, label = { Text(stringResource("weight_kg_label")) }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    }
                    "Weaning" -> {
                        OutlinedTextField(value = weaningLocation.value, onValueChange = { weaningLocation.value = it }, label = { Text(stringResource("pen_number_separated_by_comma")) }, modifier = Modifier.fillMaxWidth())
                    }
                    "Culling" -> {
                        if (pig.activeWithdrawalUntil.isNotEmpty() && DateUtils.isWithdrawalActive(pig.activeWithdrawalUntil)) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "⚠️ FOOD SAFETY WARNING: Pig is under active meat withdrawal until ${pig.activeWithdrawalUntil} (${pig.withdrawalMedication}). Meat is unsafe for slaughter or sale!",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                        OutlinedTextField(value = cullingReason.value, onValueChange = { cullingReason.value = it }, label = { Text(stringResource("reason_sold_disease")) }, modifier = Modifier.fillMaxWidth())
                        if (cullingReason.value.equals("Sold", ignoreCase = true)) {
                            OutlinedTextField(value = salePrice.value, onValueChange = { salePrice.value = it }, label = { Text(stringResource("sale_price_currency", currencySymbol)) }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        }
                    }
                    "Heat Detection" -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = trackHeat.value, onCheckedChange = { trackHeat.value = it })
                            Text(stringResource("track_heat_remind"))
                        }
                    }
                    "Pregnancy Check" -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = pregnancyConfirmed.value, onCheckedChange = { pregnancyConfirmed.value = it })
                            Text(stringResource("preg_confirmed_farrow"))
                        }
                    }
                    "Custom" -> {
                        OutlinedTextField(value = customActivityName.value, onValueChange = { customActivityName.value = it }, label = { Text(stringResource("activity_name")) }, modifier = Modifier.fillMaxWidth())
                    }
                }

                OutlinedTextField(
                    value = notes.value, 
                    onValueChange = { notes.value = it }, 
                    label = { Text(stringResource("notes_details")) }, 
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val finalDescription = StringBuilder(notes.value)
                when (type.value) {
                    "Breeding/Mating" -> {
                        if (sowTag.value.isNotEmpty()) {
                            if (finalDescription.isNotEmpty()) finalDescription.append("\n")
                            if (boarTag.value.isNotEmpty()) {
                                finalDescription.append(Translator.getString("mated_sow_with_boar", languageCode, sowTag.value, boarTag.value))
                            } else {
                                finalDescription.append("Inseminated: Sow ${sowTag.value} (AI / External)")
                            }
                        }
                    }
                    "Farrowing" -> {
                        if (numMales.value.isNotEmpty() || numFemales.value.isNotEmpty()) {
                            if (finalDescription.isNotEmpty()) finalDescription.append("\n")
                            finalDescription.append(Translator.getString("farrowed_males_females", languageCode, numMales.value, numFemales.value))
                        }
                        val extras = mutableListOf<String>()
                        if (stillbornsState.value.isNotEmpty() && (stillbornsState.value.toIntOrNull() ?: 0) > 0) extras.add("Stillborns: ${stillbornsState.value}")
                        if (mummiesState.value.isNotEmpty() && (mummiesState.value.toIntOrNull() ?: 0) > 0) extras.add("Mummies: ${mummiesState.value}")
                        if (litterBirthWeightState.value.isNotEmpty() && (litterBirthWeightState.value.toDoubleOrNull() ?: 0.0) > 0.0) extras.add("Litter Weight: ${litterBirthWeightState.value}kg")
                        if (extras.isNotEmpty()) {
                            finalDescription.append(" (${extras.joinToString(", ")})")
                        }
                    }
                    "Vaccination", "Medication", "Deworming", "Iron Injection" -> {
                        if (medName.value.isNotEmpty()) {
                            if (finalDescription.isNotEmpty()) finalDescription.append("\n")
                            finalDescription.append(Translator.getString("medication_vaccine_dosage", languageCode, medName.value, medDosage.value))
                        }
                        val wDays = withdrawalDaysState.value.toIntOrNull() ?: 0
                        if (wDays > 0) {
                            finalDescription.append("\nMeat Withdrawal Period: $wDays days (Safe: ${DateUtils.addDaysToDate(formattedDate, wDays)})")
                        }
                    }
                    "Weight Check" -> {
                        if (weightState.value.isNotEmpty()) {
                            if (finalDescription.isNotEmpty()) finalDescription.append("\n")
                            finalDescription.append(Translator.getString("weight_updated_to", languageCode, weightState.value))
                        }
                    }
                    "Weaning" -> {
                        if (weaningLocation.value.isNotEmpty()) {
                            if (finalDescription.isNotEmpty()) finalDescription.append("\n")
                            finalDescription.append(Translator.getString("weaned_and_moved", languageCode, weaningLocation.value))
                        }
                    }
                    "Culling" -> {
                        if (cullingReason.value.isNotEmpty()) {
                            if (finalDescription.isNotEmpty()) finalDescription.append("\n")
                            finalDescription.append(Translator.getString("reason_label", languageCode, cullingReason.value))
                        }
                        if (cullingReason.value.equals("Sold", ignoreCase = true) && salePrice.value.isNotEmpty()) {
                            finalDescription.append("\n")
                            finalDescription.append(Translator.getString("sold_for_amount", languageCode, currencySymbol, salePrice.value))
                        }
                    }
                    "Pregnancy Check" -> {
                        if (pregnancyConfirmed.value) {
                            if (finalDescription.isNotEmpty()) finalDescription.append("\n")
                            finalDescription.append(Translator.getString("pregnancy_confirmed", languageCode))
                        }
                    }
                    "Castration" -> {
                        if (finalDescription.isNotEmpty()) finalDescription.append("\n")
                        finalDescription.append(Translator.getString("castrated_successfully", languageCode))
                    }
                }

                val wDays = withdrawalDaysState.value.toIntOrNull() ?: 0
                val safeDate = if (wDays > 0) DateUtils.addDaysToDate(formattedDate, wDays) else ""

                onConfirm(
                    HealthRecord(
                        id = existingRecord?.id ?: "",
                        date = formattedDate,
                        type = if (type.value == "Custom") customActivityName.value else type.value,
                        description = finalDescription.toString().trim(),
                        medication = if (listOf("Vaccination", "Medication", "Deworming", "Iron Injection").contains(type.value)) medName.value else existingRecord?.medication ?: "",
                        dosage = medDosage.value,
                        weight = weightState.value.toDoubleOrNull() ?: (existingRecord?.weight ?: 0.0),
                        cost = if (type.value == "Culling" && cullingReason.value.equals("Sold", ignoreCase = true)) (salePrice.value.toDoubleOrNull() ?: 0.0) else (existingRecord?.cost ?: 0.0),
                        stillbornCount = stillbornsState.value.toIntOrNull() ?: 0,
                        mummiesCount = mummiesState.value.toIntOrNull() ?: 0,
                        litterBirthWeightKg = litterBirthWeightState.value.toDoubleOrNull() ?: 0.0,
                        withdrawalPeriodDays = wDays,
                        safeSlaughterDate = safeDate,
                        taskId = existingRecord?.taskId
                    ),
                    trackHeat.value,
                    checkPregnancy.value,
                    pregnancyConfirmed.value,
                    mapOf(
                        "sowTag" to sowTag.value,
                        "boarTag" to boarTag.value,
                        "numMales" to numMales.value,
                        "numFemales" to numFemales.value,
                        "stillborns" to stillbornsState.value,
                        "mummies" to mummiesState.value,
                        "litterBirthWeight" to litterBirthWeightState.value,
                        "weight" to weightState.value,
                        "weaningLocation" to weaningLocation.value,
                        "cullingReason" to cullingReason.value,
                        "salePrice" to salePrice.value,
                        "medName" to medName.value,
                        "medDosage" to medDosage.value,
                        "withdrawalDays" to withdrawalDaysState.value
                    )
                )
            },
            enabled = if (type.value == "Breeding/Mating") sowTag.value.isNotBlank() else true
            ) { Text(stringResource(if (existingRecord == null) "add" else "update")) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource("cancel")) }
        }
    )
}
