package com.example.smartswine.ui.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.example.smartswine.utils.LocalAppLanguage
import com.example.smartswine.utils.Translator
import com.example.smartswine.utils.StylishDivider
import com.example.smartswine.utils.stringResource
import com.example.smartswine.model.FeedIngredient
import com.example.smartswine.ui.navigation.Screen
import com.example.smartswine.ui.theme.SmartSwineTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientListScreen(
    onBack: () -> Unit,
    onNavigateTo: (String) -> Unit,
    viewModel: FeedViewModel = viewModel(),
) {
    val ingredients by viewModel.ingredients.collectAsStateWithLifecycle()
    IngredientListContent(
        ingredients = ingredients,
        onBack = onBack,
        onNavigateTo = onNavigateTo,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientListContent(
    ingredients: List<FeedIngredient>,
    onBack: () -> Unit,
    onNavigateTo: (String) -> Unit,
) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current.code

    // Helper to get translated name
    val getIngredientName = remember(context, lang) {
        { ingredient: FeedIngredient ->
            val customTranslation = ingredient.nameTranslations[lang]
            if (customTranslation != null && customTranslation.isNotBlank()) {
                customTranslation
            } else if (ingredient.nameResourceId != 0) {
                try {
                    val resName = context.resources.getResourceEntryName(ingredient.nameResourceId)
                    Translator.getString(resName, lang)
                } catch (_: Exception) {
                    context.getString(ingredient.nameResourceId)
                }
            } else {
                ingredient.name
            }
        }
    }

    val searchQuery = remember { mutableStateOf("") }
    val filteredIngredients = remember(ingredients, searchQuery.value, getIngredientName) {
        if (searchQuery.value.isEmpty()) {
            ingredients
        } else {
            ingredients.filter {
                getIngredientName(it).contains(searchQuery.value, ignoreCase = true) ||
                it.category.contains(searchQuery.value, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource("back"))
                    }
                    Text(
                        text = stringResource("master_ingredient_list"),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.width(48.dp))
                }
                StylishDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateTo(Screen.AddIngredient.route) },
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource("add_ingredient"),
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            OutlinedTextField(
                value = searchQuery.value,
                onValueChange = { searchQuery.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text(stringResource("search_ingredients")) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.value.isNotEmpty()) {
                        IconButton(onClick = { searchQuery.value = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource("clear_search"))
                        }
                    }
                },
                singleLine = true,
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredIngredients) { ingredient ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onNavigateTo(Screen.EditIngredient.createRoute(ingredient.id)) }
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = getIngredientName(ingredient),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = ingredient.category,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Text(
                                    text = stringResource("protein_pct_label", ingredient.crudeProtein.toString()),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(120.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun IngredientListPreview() {
    SmartSwineTheme {
        IngredientListContent(
            ingredients = listOf(
                FeedIngredient(
                    id = "1",
                    name = "Maize",
                    category = "Cereals",
                    crudeProtein = 8.5
                ),
                FeedIngredient(
                    id = "2",
                    name = "Soybean Meal",
                    category = "Protein Meals",
                    crudeProtein = 44.0
                )
            ),
            onBack = {},
        ) { _ -> }
    }
}
