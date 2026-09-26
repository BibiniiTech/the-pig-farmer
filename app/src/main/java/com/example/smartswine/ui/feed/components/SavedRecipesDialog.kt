package com.example.smartswine.ui.feed.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.smartswine.model.FeedIngredient
import com.example.smartswine.model.SavedFeedRecipe
import com.example.smartswine.utils.DateUtils
import com.example.smartswine.utils.LocalAppLanguage
import com.example.smartswine.utils.Translator
import com.example.smartswine.utils.getIngredientNameKey
import com.example.smartswine.utils.stringResource
import java.util.Locale

private val FeedOrange = Color(0xFFE65100)
private val FeedOrangeLight = Color(0xFFFFF3E0)

@Composable
fun SavedRecipesDialog(
    savedRecipes: List<SavedFeedRecipe>,
    allIngredients: List<FeedIngredient>,
    currencySymbol: String = "$",
    onDismiss: () -> Unit,
    onLoadRecipe: (SavedFeedRecipe) -> Unit,
    onDeleteRecipe: (String) -> Unit
) {
    val context = LocalContext.current
    val currentLang = LocalAppLanguage.current.code
    var recipeToDelete by remember { mutableStateOf<SavedFeedRecipe?>(null) }

    // Confirmation dialog before deleting a recipe
    if (recipeToDelete != null) {
        AlertDialog(
            onDismissRequest = { recipeToDelete = null },
            title = {
                Text(
                    text = stringResource("delete_saved_recipe"),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(stringResource("delete_saved_recipe_confirm", recipeToDelete!!.name))
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteRecipe(recipeToDelete!!.id)
                        recipeToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource("delete"))
                }
            },
            dismissButton = {
                TextButton(onClick = { recipeToDelete = null }) {
                    Text(stringResource("cancel"))
                }
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(42.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = FeedOrangeLight
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Bookmarks,
                                    contentDescription = null,
                                    tint = FeedOrange,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = stringResource("saved_formulations"),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource("recipes_saved_count", savedRecipes.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource("close"),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (savedRecipes.isEmpty()) {
                    // Empty state
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(64.dp),
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.BookmarkBorder,
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = stringResource("no_saved_recipes_title"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource("no_saved_recipes_desc"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Recipes List
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(savedRecipes, key = { it.id }) { recipe ->
                            SavedRecipeCard(
                                recipe = recipe,
                                allIngredients = allIngredients,
                                currencySymbol = currencySymbol,
                                currentLang = currentLang,
                                onLoad = { onLoadRecipe(recipe) },
                                onDelete = { recipeToDelete = recipe }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom dismiss button
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource("close"), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SavedRecipeCard(
    recipe: SavedFeedRecipe,
    allIngredients: List<FeedIngredient>,
    currencySymbol: String,
    currentLang: String,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current

    // Build ingredient summary text
    val ingredientSummary = remember(recipe.ingredients, allIngredients, currentLang) {
        recipe.ingredients.entries.mapNotNull { (ingId, pct) ->
            val found = allIngredients.find { it.id == ingId }
            if (found != null) {
                val key = getIngredientNameKey(found, context)
                val trans = Translator.getString(key, currentLang)
                val name = if (trans == key && found.name.isNotEmpty()) found.name else trans
                val formattedPct = if (pct % 1.0 == 0.0) "${pct.toInt()}%" else String.format(Locale.US, "%.1f%%", pct)
                "$name: $formattedPct"
            } else null
        }.joinToString(", ")
    }

    val appLanguage = LocalAppLanguage.current
    val stageKey = when (recipe.stage.lowercase().trim()) {
        "creep" -> "stage_creep"
        "weaner/starter", "weaner", "starter" -> "stage_weaner_starter"
        "grower" -> "stage_grower"
        "finisher" -> "stage_finisher"
        "pregnant" -> "stage_pregnant"
        "lactating" -> "stage_lactating"
        else -> null
    }
    val localizedStage = if (stageKey != null) stringResource(stageKey) else recipe.stage.ifBlank { stringResource("stage_grower") }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, FeedOrange.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top row: Recipe name and Stage badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = recipe.name.ifBlank { stringResource("custom_mix") },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = FeedOrangeLight
                ) {
                    Text(
                        text = localizedStage,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = FeedOrange,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Metrics row: Ingredient count, cost/kg, batch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🌾 ${stringResource("ingredients_count", recipe.ingredients.size)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (recipe.costPerKg > 0) {
                    Text(
                        text = "•",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "💰 $currencySymbol${String.format(Locale.US, "%.2f", recipe.costPerKg)}/kg",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (recipe.dateCreated.isNotBlank()) {
                    Text(
                        text = "•",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val displayDate = remember(recipe.dateCreated, appLanguage) {
                        val parsed = DateUtils.parseAnyDateNonNull(recipe.dateCreated)
                        DateUtils.formatDateToDisplay(parsed, appLanguage.toLocale())
                    }
                    Text(
                        text = "📅 $displayDate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Ingredient preview
            if (ingredientSummary.isNotBlank()) {
                Text(
                    text = ingredientSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }

            // Notes if available
            if (recipe.notes.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "“${recipe.notes}”",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Action row: Load Mix & Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onLoad,
                    modifier = Modifier.weight(1f).heightIn(min = 40.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FeedOrange)
                ) {
                    Icon(
                        imageVector = Icons.Default.Upload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource("load_into_analyzer"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = stringResource("delete_recipe"),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
