package com.example.smartswine.ui.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartswine.model.FeedIngredient

import com.example.smartswine.utils.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditIngredientScreen(
    ingredientId: String? = null,
    onBack: () -> Unit,
    viewModel: FeedViewModel = viewModel(),
) {
    val ingredients by viewModel.ingredients.collectAsState()
    val existingIngredient = ingredients.find { it.id == ingredientId }

    AddEditIngredientContent(
        ingredientId = ingredientId,
        existingIngredient = existingIngredient,
        onBack = onBack,
    ) { ingredient ->
        if (ingredientId == null) {
            viewModel.addIngredient(ingredient)
        } else {
            viewModel.updateIngredient(ingredient)
        }
        onBack()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditIngredientContent(
    ingredientId: String? = null,
    existingIngredient: FeedIngredient? = null,
    onBack: () -> Unit,
    onSave: (FeedIngredient) -> Unit,
) {
    val nameState = remember { mutableStateOf(existingIngredient?.name ?: "") }
    val mainCategoryState = remember { mutableStateOf(existingIngredient?.mainCategory ?: "Energy") }
    val subCategoryState = remember { mutableStateOf(existingIngredient?.category ?: "") }
    val dryMatterState = remember { mutableStateOf(existingIngredient?.dryMatter?.toString() ?: "") }
    val crudeProteinState = remember { mutableStateOf(existingIngredient?.crudeProtein?.toString() ?: "") }
    val energyState = remember { mutableStateOf(existingIngredient?.metabolizableEnergy?.toString() ?: "") }
    val lysineState = remember { mutableStateOf(existingIngredient?.lysine?.toString() ?: "") }
    val calciumState = remember { mutableStateOf(existingIngredient?.calcium?.toString() ?: "") }
    val phosphorusState = remember { mutableStateOf(existingIngredient?.phosphorus?.toString() ?: "") }
    val crudeFiberState = remember { mutableStateOf(existingIngredient?.crudeFiber?.toString() ?: "") }
    val fatState = remember { mutableStateOf(existingIngredient?.fat?.toString() ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (ingredientId == null) stringResource("new_feed_ingredient") else stringResource("edit_feed_ingredient")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource("back"))
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val ingredient = FeedIngredient(
                                id = ingredientId ?: "",
                                name = nameState.value,
                                mainCategory = mainCategoryState.value,
                                category = subCategoryState.value,
                                dryMatter = dryMatterState.value.toDoubleOrNull() ?: 0.0,
                                crudeProtein = crudeProteinState.value.toDoubleOrNull() ?: 0.0,
                                metabolizableEnergy = energyState.value.toDoubleOrNull() ?: 0.0,
                                lysine = lysineState.value.toDoubleOrNull() ?: 0.0,
                                calcium = calciumState.value.toDoubleOrNull() ?: 0.0,
                                phosphorus = phosphorusState.value.toDoubleOrNull() ?: 0.0,
                                crudeFiber = crudeFiberState.value.toDoubleOrNull() ?: 0.0,
                                fat = fatState.value.toDoubleOrNull() ?: 0.0,
                            )
                            onSave(ingredient)
                        },
                    ) {
                        Icon(Icons.Default.Save, contentDescription = stringResource("save"))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(value = nameState.value, onValueChange = { nameState.value = it }, label = { Text(stringResource("ingredient_name")) }, modifier = Modifier.fillMaxWidth())
            
            val categories = listOf("Energy", "Protein", "Vitamins, Minerals & Salt")
            val expandedState = remember { mutableStateOf(value = false) }
            
            ExposedDropdownMenuBox(
                expanded = expandedState.value,
                onExpandedChange = { expandedState.value = !expandedState.value },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = stringResource(mainCategoryState.value.lowercase().replace(", ", "_").replace(" & ", "_").replace(" ", "_")),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource("main_category")) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedState.value) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedState.value,
                    onDismissRequest = { expandedState.value = false }
                ) {
                    categories.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(stringResource(selectionOption.lowercase().replace(", ", "_").replace(" & ", "_").replace(" ", "_"))) },
                            onClick = {
                                mainCategoryState.value = selectionOption
                                expandedState.value = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(value = subCategoryState.value, onValueChange = { subCategoryState.value = it }, label = { Text(stringResource("sub_category_optional")) }, modifier = Modifier.fillMaxWidth())
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = dryMatterState.value, onValueChange = { dryMatterState.value = it }, label = { Text(stringResource("dry_matter_pct")) }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = crudeProteinState.value, onValueChange = { crudeProteinState.value = it }, label = { Text(stringResource("crude_protein_pct_short")) }, modifier = Modifier.weight(1f))
            }
            
            OutlinedTextField(value = energyState.value, onValueChange = { energyState.value = it }, label = { Text(stringResource("metabolizable_energy_kcal_kg")) }, modifier = Modifier.fillMaxWidth())
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = lysineState.value, onValueChange = { lysineState.value = it }, label = { Text(stringResource("lysine_pct_short")) }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = calciumState.value, onValueChange = { calciumState.value = it }, label = { Text(stringResource("calcium_pct_short")) }, modifier = Modifier.weight(1f))
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = phosphorusState.value, onValueChange = { phosphorusState.value = it }, label = { Text(stringResource("phosphorus_pct_short")) }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = crudeFiberState.value, onValueChange = { crudeFiberState.value = it }, label = { Text(stringResource("fiber_pct_short")) }, modifier = Modifier.weight(1f))
            }
            
            OutlinedTextField(value = fatState.value, onValueChange = { fatState.value = it }, label = { Text(stringResource("fat_pct_short")) }, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun AddEditIngredientPreview() {
    com.example.smartswine.ui.theme.SmartSwineTheme {
        AddEditIngredientContent(
            ingredientId = null,
            existingIngredient = null,
            onBack = {},
        ) {}
    }
}
