package com.example.smartswine.ui.market.admin

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.smartswine.model.FeedIngredient
import com.example.smartswine.utils.stringResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminIngredientsTab(
    globalIngredients: List<FeedIngredient>,
    isFeedLoading: Boolean,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    onAddGlobalIngredient: (FeedIngredient, (Boolean, String?) -> Unit) -> Unit,
    onUpdateGlobalIngredient: (FeedIngredient, (Boolean, String?) -> Unit) -> Unit,
    onDeleteGlobalIngredient: (String, (Boolean, String?) -> Unit) -> Unit
) {
    var ingredientSearchQuery by remember { mutableStateOf("") }
    var ingredientToEdit by remember { mutableStateOf<FeedIngredient?>(null) }
    var ingredientToDelete by remember { mutableStateOf<FeedIngredient?>(null) }
    var showAddIngredientDialog by remember { mutableStateOf(false) }
    val expandedCategories = remember { mutableStateMapOf<String, Boolean>() }

    // --- INGREDIENT DELETE DIALOG ---
    if (ingredientToDelete != null) {
        AlertDialog(
            onDismissRequest = { ingredientToDelete = null },
            title = { Text(stringResource("delete_global_ingredient")) },
            text = { Text(stringResource("confirm_delete_ingredient", ingredientToDelete?.name ?: "")) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteGlobalIngredient(ingredientToDelete!!.id) { success, err ->
                            if (success) {
                                ingredientToDelete = null
                                scope.launch { snackbarHostState.showSnackbar("Ingredient deleted successfully.") }
                            } else {
                                scope.launch { snackbarHostState.showSnackbar("Error deleting: $err") }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource("delete"))
                }
            },
            dismissButton = {
                TextButton(onClick = { ingredientToDelete = null }) {
                    Text(stringResource("cancel"))
                }
            }
        )
    }

    // --- INGREDIENT ADD/EDIT DIALOG ---
    if (showAddIngredientDialog || ingredientToEdit != null) {
        val editing = ingredientToEdit != null
        var name by remember { mutableStateOf(if (editing) ingredientToEdit!!.name else "") }
        var category by remember { mutableStateOf(if (editing) ingredientToEdit!!.mainCategory else "Energy") }
        var cp by remember { mutableStateOf(if (editing) ingredientToEdit!!.crudeProtein.toString() else "0.0") }
        var me by remember { mutableStateOf(if (editing) (ingredientToEdit!!.metabolizableEnergy / 239.0).toString() else "0.0") }
        var cf by remember { mutableStateOf(if (editing) ingredientToEdit!!.crudeFiber.toString() else "0.0") }
        var dm by remember { mutableStateOf(if (editing) ingredientToEdit!!.dryMatter.toString() else "0.0") }
        var ca by remember { mutableStateOf(if (editing) ingredientToEdit!!.calcium.toString() else "0.0") }
        var ph by remember { mutableStateOf(if (editing) ingredientToEdit!!.phosphorus.toString() else "0.0") }
        var lys by remember { mutableStateOf(if (editing) ingredientToEdit!!.lysine.toString() else "0.0") }
        var met by remember { mutableStateOf(if (editing) ingredientToEdit!!.methionine.toString() else "0.0") }
        var maxSt by remember { mutableStateOf(if (editing) ingredientToEdit!!.maxStarter.toString() else "100.0") }
        var maxGr by remember { mutableStateOf(if (editing) ingredientToEdit!!.maxGrower.toString() else "100.0") }
        var maxFi by remember { mutableStateOf(if (editing) ingredientToEdit!!.maxFinisher.toString() else "100.0") }
        var dropdownExpanded by remember { mutableStateOf(false) }

        var showTranslations by remember { mutableStateOf(false) }
        var transFr by remember { mutableStateOf(if (editing) ingredientToEdit!!.nameTranslations["fr"] ?: "" else "") }
        var transZh by remember { mutableStateOf(if (editing) ingredientToEdit!!.nameTranslations["zh"] ?: "" else "") }
        var transEs by remember { mutableStateOf(if (editing) ingredientToEdit!!.nameTranslations["es"] ?: "" else "") }
        var transTl by remember { mutableStateOf(if (editing) ingredientToEdit!!.nameTranslations["tl"] ?: "" else "") }
        var transVi by remember { mutableStateOf(if (editing) ingredientToEdit!!.nameTranslations["vi"] ?: "" else "") }
        var transTh by remember { mutableStateOf(if (editing) ingredientToEdit!!.nameTranslations["th"] ?: "" else "") }
        var transPt by remember { mutableStateOf(if (editing) ingredientToEdit!!.nameTranslations["pt"] ?: "" else "") }
        var transHi by remember { mutableStateOf(if (editing) ingredientToEdit!!.nameTranslations["hi"] ?: "" else "") }

        val categories = listOf("Energy", "Protein", "Vitamins, Minerals & Salt")

        AlertDialog(
            onDismissRequest = {
                showAddIngredientDialog = false
                ingredientToEdit = null
            },
            title = { Text(if (editing) "Edit Feed Ingredient" else "Add Feed Ingredient") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource("ingredient_name")) }, modifier = Modifier.fillMaxWidth())

                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource("main_category")) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = dropdownExpanded, onDismissRequest = { dropdownExpanded = false }) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        category = cat
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = cp, onValueChange = { cp = it }, label = { Text(stringResource("crude_protein_pct")) }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = me, onValueChange = { me = it }, label = { Text(stringResource("me_mj_kg")) }, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = cf, onValueChange = { cf = it }, label = { Text(stringResource("crude_fiber_pct")) }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = dm, onValueChange = { dm = it }, label = { Text(stringResource("dry_matter_percent")) }, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = ca, onValueChange = { ca = it }, label = { Text(stringResource("calcium_pct")) }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = ph, onValueChange = { ph = it }, label = { Text(stringResource("phosphorus_pct")) }, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = lys, onValueChange = { lys = it }, label = { Text(stringResource("lysine_pct")) }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = met, onValueChange = { met = it }, label = { Text(stringResource("methionine_pct")) }, modifier = Modifier.weight(1f))
                    }

                    Text(stringResource("inclusion_limits_pct"), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = maxSt, onValueChange = { maxSt = it }, label = { Text(stringResource("starter")) }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = maxGr, onValueChange = { maxGr = it }, label = { Text(stringResource("grower")) }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = maxFi, onValueChange = { maxFi = it }, label = { Text(stringResource("finisher")) }, modifier = Modifier.weight(1f))
                    }

                    TextButton(onClick = { showTranslations = !showTranslations }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (showTranslations) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource("name_translations_optional"))
                        }
                    }

                    if (showTranslations) {
                        OutlinedTextField(value = transFr, onValueChange = { transFr = it }, label = { Text("French (Français)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = transZh, onValueChange = { transZh = it }, label = { Text("Chinese (中文)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = transEs, onValueChange = { transEs = it }, label = { Text("Spanish (Español)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = transTl, onValueChange = { transTl = it }, label = { Text("Filipino (Tagalog)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = transVi, onValueChange = { transVi = it }, label = { Text("Vietnamese (Tiếng Việt)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = transTh, onValueChange = { transTh = it }, label = { Text("Thai (ไทย)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = transPt, onValueChange = { transPt = it }, label = { Text("Portuguese (Português)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = transHi, onValueChange = { transHi = it }, label = { Text("Hindi (हिन्दी)") }, modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedCp = cp.toDoubleOrNull() ?: 0.0
                        val parsedMe = (me.toDoubleOrNull() ?: 0.0) * 239.0
                        val parsedCf = cf.toDoubleOrNull() ?: 0.0
                        val parsedDm = dm.toDoubleOrNull() ?: 0.0
                        val parsedCa = ca.toDoubleOrNull() ?: 0.0
                        val parsedPh = ph.toDoubleOrNull() ?: 0.0
                        val parsedLys = lys.toDoubleOrNull() ?: 0.0
                        val parsedMet = met.toDoubleOrNull() ?: 0.0
                        val parsedMaxSt = maxSt.toDoubleOrNull() ?: 100.0
                        val parsedMaxGr = maxGr.toDoubleOrNull() ?: 100.0
                        val parsedMaxFi = maxFi.toDoubleOrNull() ?: 100.0

                        val translationsMap = mutableMapOf<String, String>()
                        if (transFr.isNotBlank()) translationsMap["fr"] = transFr
                        if (transZh.isNotBlank()) translationsMap["zh"] = transZh
                        if (transEs.isNotBlank()) translationsMap["es"] = transEs
                        if (transTl.isNotBlank()) translationsMap["tl"] = transTl
                        if (transVi.isNotBlank()) translationsMap["vi"] = transVi
                        if (transTh.isNotBlank()) translationsMap["th"] = transTh
                        if (transPt.isNotBlank()) translationsMap["pt"] = transPt
                        if (transHi.isNotBlank()) translationsMap["hi"] = transHi

                        val toSave = FeedIngredient(
                            id = if (editing) ingredientToEdit!!.id else "",
                            name = name,
                            mainCategory = category,
                            crudeProtein = parsedCp,
                            metabolizableEnergy = parsedMe,
                            crudeFiber = parsedCf,
                            dryMatter = parsedDm,
                            calcium = parsedCa,
                            phosphorus = parsedPh,
                            lysine = parsedLys,
                            methionine = parsedMet,
                            nameTranslations = translationsMap,
                            maxStarter = parsedMaxSt,
                            maxGrower = parsedMaxGr,
                            maxFinisher = parsedMaxFi,
                            visible = true,
                            unit = "kg"
                        )

                        if (editing) {
                            onUpdateGlobalIngredient(toSave) { success, err ->
                                if (success) {
                                    ingredientToEdit = null
                                    scope.launch { snackbarHostState.showSnackbar("Ingredient updated successfully.") }
                                } else {
                                    scope.launch { snackbarHostState.showSnackbar("Error saving: $err") }
                                }
                            }
                        } else {
                            onAddGlobalIngredient(toSave) { success, err ->
                                if (success) {
                                    showAddIngredientDialog = false
                                    scope.launch { snackbarHostState.showSnackbar("Ingredient created successfully.") }
                                } else {
                                    scope.launch { snackbarHostState.showSnackbar("Error creating: $err") }
                                }
                            }
                        }
                    },
                    enabled = name.isNotBlank()
                ) {
                    Text(if (editing) "Save Changes" else "Add Ingredient")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddIngredientDialog = false
                    ingredientToEdit = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = ingredientSearchQuery,
            onValueChange = { ingredientSearchQuery = it },
            placeholder = { Text(stringResource("search_ingredients")) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Button(
            onClick = { showAddIngredientDialog = true },
            contentPadding = PaddingValues(horizontal = 12.dp),
            modifier = Modifier.height(56.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource("add"))
        }
    }

    if (isFeedLoading) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }

    val filteredIngredients = globalIngredients.filter {
        it.name.contains(ingredientSearchQuery, ignoreCase = true) ||
        it.mainCategory.contains(ingredientSearchQuery, ignoreCase = true)
    }

    if (filteredIngredients.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(64.dp), contentAlignment = Alignment.Center) {
            Text(stringResource("no_global_ingredients"), color = MaterialTheme.colorScheme.outline)
        }
    } else {
        val grouped = filteredIngredients.groupBy { it.mainCategory }

        grouped.forEach { (cat, list) ->
            val isExpanded = expandedCategories[cat] ?: true
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedCategories[cat] = !isExpanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = cat,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }

            if (isExpanded) {
                val sortedList = list.sortedBy { it.name }
                sortedList.forEach { ing ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = ing.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Row {
                                    IconButton(onClick = { ingredientToEdit = ing }) {
                                        Icon(Icons.Default.Edit, contentDescription = stringResource("edit"), tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { ingredientToDelete = ing }) {
                                        Icon(Icons.Default.Delete, contentDescription = stringResource("delete"), tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }

                            // Nutritional breakdown
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Protein: ${ing.crudeProtein}%", style = MaterialTheme.typography.bodySmall)
                                    Text(text = "ME: ${String.format(Locale.US, "%.2f", ing.metabolizableEnergy / 239.0)} MJ/kg", style = MaterialTheme.typography.bodySmall)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Lysine: ${ing.lysine}%", style = MaterialTheme.typography.bodySmall)
                                    Text(text = "Methionine: ${ing.methionine}%", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
