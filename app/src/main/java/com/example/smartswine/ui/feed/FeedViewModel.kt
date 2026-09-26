package com.example.smartswine.ui.feed

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartswine.data.FeedRepository
import com.example.smartswine.data.FinancialRepository
import com.example.smartswine.model.FinancialRecord
import com.example.smartswine.model.FeedIngredient
import com.example.smartswine.model.NutritionalRequirement
import com.example.smartswine.model.FeedInventoryItem
import com.example.smartswine.model.FeedInventoryTransaction
import com.example.smartswine.model.SavedFeedRecipe
import com.example.smartswine.utils.DateUtils
import com.example.smartswine.utils.Translator
import com.example.smartswine.utils.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class FeedViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FeedRepository()

    // Active Farm ID for multi-user support
    private var activeFarmId: String? = null

    fun setActiveFarmId(uid: String?) {
        if (activeFarmId != uid) {
            activeFarmId = uid
            repository.setActiveFarmId(uid)
            if (uid == null) {
                _ingredients.value = emptyList()
                _requirements.value = emptyList()
                _feedInventoryItems.value = emptyList()
                _feedInventoryTransactions.value = emptyList()
                _savedRecipes.value = emptyList()
            } else {
                initializeData()
            }
        }
    }

    private val _ingredients = MutableStateFlow<List<FeedIngredient>>(emptyList())
    val ingredients: StateFlow<List<FeedIngredient>> = _ingredients.asStateFlow()

    private val _globalIngredients = MutableStateFlow<List<FeedIngredient>>(emptyList())
    val globalIngredients: StateFlow<List<FeedIngredient>> = _globalIngredients.asStateFlow()

    private val _requirements = MutableStateFlow<List<NutritionalRequirement>>(FeedRepository.DEFAULT_REQUIREMENTS)
    val nutritionalRequirements: StateFlow<List<NutritionalRequirement>> = _requirements.asStateFlow()

    private val _feedInventoryItems = MutableStateFlow<List<FeedInventoryItem>>(emptyList())
    val feedInventoryItems: StateFlow<List<FeedInventoryItem>> = _feedInventoryItems.asStateFlow()

    private val _feedInventoryTransactions = MutableStateFlow<List<FeedInventoryTransaction>>(emptyList())
    val feedInventoryTransactions: StateFlow<List<FeedInventoryTransaction>> = _feedInventoryTransactions.asStateFlow()

    private val _savedRecipes = MutableStateFlow<List<SavedFeedRecipe>>(emptyList())
    val savedRecipes: StateFlow<List<SavedFeedRecipe>> = _savedRecipes.asStateFlow()

    private val _isLoading = MutableStateFlow(value = false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isFormulating = MutableStateFlow(value = false)
    val isFormulating: StateFlow<Boolean> = _isFormulating.asStateFlow()

    private var currentLanguage = AppLanguage.ENGLISH.code
    fun setLanguage(lang: String) {
        currentLanguage = lang
    }

    private val _formulationResult = MutableStateFlow<Map<String, Double>?>(null)
    val formulationResult: StateFlow<Map<String, Double>?> = _formulationResult.asStateFlow()

    private val _targetRequirement = MutableStateFlow<NutritionalRequirement?>(null)
    val targetRequirement: StateFlow<NutritionalRequirement?> = _targetRequirement.asStateFlow()

    private val _feedRequirements = MutableStateFlow<Map<String, Double>?>(null)
    val feedRequirements: StateFlow<Map<String, Double>?> = _feedRequirements.asStateFlow()

    private var lastTargetStage: String? = null
    private var lastSelectedIngredientIds: List<String>? = null

    init {
        android.util.Log.d("FeedViewModel", "ViewModel created: ${this.hashCode()}")
        initializeData()
    }

    fun initializeData() {
        // Start loading data immediately - these use snapshot listeners and work offline
        loadIngredients()
        loadGlobalIngredients()
        loadRequirements()
        loadFeedInventoryItems()
        loadFeedInventoryTransactions()
        loadSavedRecipes()

        // Try to sync/initialize defaults in the background
        viewModelScope.launch {
            try {
                repository.initializeDefaultIngredients(getApplication())
                repository.initializeDefaultRequirements()
            } catch (e: Exception) {
                // Log and ignore initialization errors when offline
                android.util.Log.w("FeedViewModel", "Default data sync failed (likely offline): ${e.message}")
            }
        }
    }

    private fun normalizeIngredient(ingredient: FeedIngredient): FeedIngredient {
        val rawCat = if (ingredient.mainCategory.isNotBlank()) ingredient.mainCategory else ingredient.category
        val cat = when (rawCat.trim().lowercase()) {
            "protein", "proteins" -> "Protein"
            "energy", "energies", "energy source" -> "Energy"
            "vitamins, minerals & salt", "vitamins", "minerals", "salt", "vitamins, minerals and salt", "vitamins, minerals & salt" -> "Vitamins, Minerals & Salt"
            else -> rawCat.trim().ifEmpty { "Uncategorized" }
        }
        
        return if (cat != ingredient.mainCategory) ingredient.copy(mainCategory = cat) else ingredient
    }

    private fun loadIngredients() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getAllIngredients()
                .catch { e ->
                    _error.value = e.message
                    _isLoading.value = false
                    android.util.Log.e("FeedViewModel", "Error loading ingredients: ${e.message}")
                }
                .collect { list ->
                    // Identify any redundant duplicate items that exist in Firestore to prune them
                    val redundantItems = list.filter { it.name.trim().lowercase() in REDUNDANT_INGREDIENT_NAMES }
                    if (redundantItems.isNotEmpty()) {
                        redundantItems.forEach { item ->
                            if (item.id.isNotEmpty()) {
                                launch { repository.deleteIngredient(item.id) }
                            }
                        }
                    }

                    // Normalize categories and filter out duplicates and redundant items
                    val normalizedList = list.asSequence()
                        .filterNot { it.name.trim().lowercase() in REDUNDANT_INGREDIENT_NAMES }
                        .map { normalizeIngredient(it) }
                        .distinctBy { it.name.trim().lowercase() }
                        .toList()
                    
                    _ingredients.value = normalizedList
                    // Only stop loading if we have some data or error handled above
                    _isLoading.value = false
                }
        }
    }

    companion object {
        val REDUNDANT_INGREDIENT_NAMES = setOf("barleyb", "dried brewers grain", "full fat soybean")
    }

    fun loadGlobalIngredients() {
        viewModelScope.launch {
            repository.getGlobalIngredients()
                .catch { e ->
                    android.util.Log.e("FeedViewModel", "Error loading global ingredients: ${e.message}")
                }
                .collect { list ->
                    _globalIngredients.value = list.map { normalizeIngredient(it) }.sortedBy { it.name }
                }
        }
    }

    fun addGlobalIngredient(ingredient: FeedIngredient, onComplete: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            try {
                repository.addGlobalIngredient(ingredient)
                onComplete(true, null)
            } catch (e: Exception) {
                onComplete(false, e.message)
            }
        }
    }

    fun updateGlobalIngredient(ingredient: FeedIngredient, onComplete: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            try {
                repository.updateGlobalIngredient(ingredient)
                onComplete(true, null)
            } catch (e: Exception) {
                onComplete(false, e.message)
            }
        }
    }

    fun deleteGlobalIngredient(ingredientId: String, onComplete: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            try {
                repository.deleteGlobalIngredient(ingredientId)
                onComplete(true, null)
            } catch (e: Exception) {
                onComplete(false, e.message)
            }
        }
    }

    private fun loadRequirements() {
        viewModelScope.launch {
            repository.getAllRequirements()
                .catch { e -> 
                    _error.value = e.message
                    android.util.Log.e("FeedViewModel", "Error loading requirements: ${e.message}")
                }
                .collect { list ->
                    _requirements.value = list
                }
        }
    }

    private fun loadFeedInventoryItems() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getAllFeedInventoryItems()
                .catch { e ->
                    _error.value = e.message
                    _isLoading.value = false
                    android.util.Log.e("FeedViewModel", "Error loading feed inventory items: ${e.message}")
                }
                .collect { list ->
                    _feedInventoryItems.value = list.sortedBy { it.name }
                    _isLoading.value = false
                }
        }
    }

    private fun loadFeedInventoryTransactions() {
        viewModelScope.launch {
            repository.getAllFeedInventoryTransactions()
                .catch { e ->
                    _error.value = e.message
                    android.util.Log.e("FeedViewModel", "Error loading feed inventory transactions: ${e.message}")
                }
                .collect { list ->
                    _feedInventoryTransactions.value = list.sortedByDescending { DateUtils.parseAnyDateNonNull(it.date) }
                }
        }
    }

    private fun loadSavedRecipes() {
        viewModelScope.launch {
            repository.getAllSavedRecipes()
                .catch { e ->
                    android.util.Log.e("FeedViewModel", "Error loading saved recipes: ${e.message}")
                }
                .collect { list ->
                    _savedRecipes.value = list.sortedByDescending { DateUtils.parseAnyDateNonNull(it.dateCreated) }
                }
        }
    }

    fun saveFeedRecipe(
        name: String,
        stage: String,
        ingredients: Map<String, Double>,
        isPercentage: Boolean = true,
        costPerKg: Double = 0.0,
        targetBatchKg: Double = 1000.0,
        notes: String = "",
        onComplete: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            try {
                val dateStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
                val recipe = SavedFeedRecipe(
                    name = name,
                    stage = stage,
                    dateCreated = dateStr,
                    ingredients = ingredients,
                    isPercentage = isPercentage,
                    costPerKg = costPerKg,
                    targetBatchKg = targetBatchKg,
                    notes = notes
                )
                val id = repository.saveFeedRecipe(recipe)
                onComplete(true, id)
            } catch (e: Exception) {
                _error.value = e.message
                onComplete(false, e.message)
            }
        }
    }

    fun deleteSavedRecipe(recipeId: String) {
        viewModelScope.launch {
            try {
                repository.deleteSavedRecipe(recipeId)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun executeBatchMix(
        recipeName: String,
        stage: String,
        batchWeightKg: Double,
        ingredients: Map<String, Double>,
        postToFinancials: Boolean = true,
        onComplete: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            try {
                val dateStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
                var totalBatchCost = 0.0

                // 1. Process each ingredient and deduct stock
                ingredients.forEach { (ingId, percent) ->
                    val neededKg = (percent / 100.0) * batchWeightKg
                    if (neededKg > 0.0001) {
                        val ing = _ingredients.value.find { it.id == ingId }
                        val ingName = ing?.name ?: ingId
                        val ingCost = ing?.costPerKg ?: 0.0
                        totalBatchCost += ingCost * neededKg

                        // Match against feed inventory items
                        val matchingInv = _feedInventoryItems.value.find {
                            it.name.equals(ingName, ignoreCase = true) || it.id == ingId
                        }
                        if (matchingInv != null) {
                            val convertedUsed = if (matchingInv.unit == "bags" && matchingInv.unitWeight > 0.0) {
                                neededKg / matchingInv.unitWeight
                            } else {
                                neededKg
                            }
                            val updatedItem = matchingInv.copy(
                                quantity = (matchingInv.quantity - convertedUsed).coerceAtLeast(0.0),
                                lastUpdated = dateStr
                            )
                            repository.updateFeedInventoryItem(updatedItem)
                        }

                        // Record usage transaction
                        val tx = FeedInventoryTransaction(
                            itemId = matchingInv?.id ?: ingId,
                            itemName = ingName,
                            type = "Usage",
                            quantity = neededKg,
                            unit = "kg",
                            cost = ingCost * neededKg,
                            date = dateStr,
                            notes = "Mixed into $batchWeightKg kg of $stage feed ($recipeName)"
                        )
                        repository.addFeedInventoryTransaction(tx)
                    }
                }

                // 2. Restock or create the finished mixed feed item
                val finishedName = if (recipeName.isNotBlank()) recipeName else "$stage Feed (Mixed)"
                val existingFinished = _feedInventoryItems.value.find {
                    it.name.equals(finishedName, ignoreCase = true)
                }

                val finalCostPerKg = if (batchWeightKg > 0.0) totalBatchCost / batchWeightKg else 0.0

                val finishedId = if (existingFinished != null) {
                    val addedQty = if (existingFinished.unit == "bags" && existingFinished.unitWeight > 0.0) {
                        batchWeightKg / existingFinished.unitWeight
                    } else {
                        batchWeightKg
                    }
                    val updatedFinished = existingFinished.copy(
                        quantity = existingFinished.quantity + addedQty,
                        costPerUnit = if (finalCostPerKg > 0) finalCostPerKg else existingFinished.costPerUnit,
                        lastUpdated = dateStr
                    )
                    repository.updateFeedInventoryItem(updatedFinished)
                    existingFinished.id
                } else {
                    val newItem = FeedInventoryItem(
                        name = finishedName,
                        feedType = stage,
                        quantity = batchWeightKg,
                        unit = "kg",
                        unitWeight = 50.0,
                        minThreshold = 100.0,
                        costPerUnit = finalCostPerKg,
                        lastUpdated = dateStr
                    )
                    repository.addFeedInventoryItem(newItem)
                }

                // Log restock transaction for finished feed
                val restockTx = FeedInventoryTransaction(
                    itemId = finishedId,
                    itemName = finishedName,
                    type = "Restock",
                    quantity = batchWeightKg,
                    unit = "kg",
                    cost = totalBatchCost,
                    date = dateStr,
                    notes = "Finished farm mix: $recipeName ($stage)"
                )
                repository.addFeedInventoryTransaction(restockTx)

                // 3. Auto-post batch expense to financials
                if (postToFinancials && totalBatchCost > 0.0) {
                    val farmId = activeFarmId ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    if (farmId != null) {
                        try {
                            val finRepo = FinancialRepository(com.google.firebase.firestore.FirebaseFirestore.getInstance())
                            val displayDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                            finRepo.addFinancialRecord(
                                farmId,
                                FinancialRecord(
                                    date = displayDate,
                                    type = "Expense",
                                    category = "Feed",
                                    amount = totalBatchCost,
                                    description = "Batch Mix: $recipeName ($stage, ${batchWeightKg}kg)"
                                )
                            )
                        } catch (finEx: Exception) {
                            android.util.Log.e("FeedViewModel", "Error auto-posting batch mix to financials: ${finEx.message}")
                        }
                    }
                }

                onComplete(true, null)
            } catch (e: Exception) {
                _error.value = e.message
                onComplete(false, e.message)
            }
        }
    }

    fun addFeedInventoryItem(
        name: String,
        feedType: String,
        initialQty: Double,
        unit: String,
        unitWeight: Double,
        minThreshold: Double,
        costPerUnit: Double = 0.0,
        itemCategory: String = "Complete Feed"
    ) {
        viewModelScope.launch {
            try {
                val dateStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
                val item = FeedInventoryItem(
                    name = name,
                    feedType = feedType,
                    quantity = initialQty,
                    unit = unit,
                    unitWeight = unitWeight,
                    minThreshold = minThreshold,
                    costPerUnit = costPerUnit,
                    lastUpdated = dateStr,
                    itemCategory = itemCategory
                )
                val generatedId = repository.addFeedInventoryItem(item)
                
                if (initialQty > 0.0) {
                    val transaction = FeedInventoryTransaction(
                        itemId = generatedId,
                        itemName = name,
                        type = "Restock",
                        quantity = initialQty,
                        unit = unit,
                        cost = costPerUnit * initialQty,
                        date = dateStr,
                        notes = "Initial stock entry ($itemCategory)"
                    )
                    repository.addFeedInventoryTransaction(transaction)
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun deleteFeedInventoryItem(itemId: String) {
        viewModelScope.launch {
            try {
                repository.deleteFeedInventoryItem(itemId)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun restockFeedItem(itemId: String, quantityAdded: Double, unit: String, cost: Double, notes: String) {
        viewModelScope.launch {
            try {
                val item = _feedInventoryItems.value.find { it.id == itemId } ?: return@launch
                
                val convertedAdded = when (unit) {
                    item.unit -> quantityAdded
                    "bags" -> if (item.unit == "kg") quantityAdded * item.unitWeight else quantityAdded
                    "kg" -> if (item.unit == "bags") quantityAdded / item.unitWeight else quantityAdded
                    else -> quantityAdded
                }
                
                val dateStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
                val updatedItem = item.copy(
                    quantity = (item.quantity + convertedAdded).coerceAtLeast(0.0),
                    costPerUnit = if (cost > 0.0) cost / quantityAdded else item.costPerUnit,
                    lastUpdated = dateStr
                )
                repository.updateFeedInventoryItem(updatedItem)
                
                val transaction = FeedInventoryTransaction(
                    itemId = itemId,
                    itemName = item.name,
                    type = "Restock",
                    quantity = quantityAdded,
                    unit = unit,
                    cost = cost,
                    date = dateStr,
                    notes = notes
                )
                repository.addFeedInventoryTransaction(transaction)
                
                if (cost > 0.0) {
                    val dateOnly = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val desc = "Purchased Feed: ${item.name} ($quantityAdded $unit)"
                    repository.addFinancialExpense(cost, desc, dateOnly)
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun useFeedItem(itemId: String, quantityUsed: Double, unit: String, notes: String) {
        viewModelScope.launch {
            try {
                val item = _feedInventoryItems.value.find { it.id == itemId } ?: return@launch
                
                val convertedUsed = when (unit) {
                    item.unit -> quantityUsed
                    "bags" -> if (item.unit == "kg") quantityUsed * item.unitWeight else quantityUsed
                    "kg" -> if (item.unit == "bags") quantityUsed / item.unitWeight else quantityUsed
                    else -> quantityUsed
                }
                
                val dateStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
                val updatedItem = item.copy(
                    quantity = (item.quantity - convertedUsed).coerceAtLeast(0.0),
                    lastUpdated = dateStr
                )
                repository.updateFeedInventoryItem(updatedItem)
                
                val transaction = FeedInventoryTransaction(
                    itemId = itemId,
                    itemName = item.name,
                    type = "Usage",
                    quantity = quantityUsed,
                    unit = unit,
                    cost = 0.0,
                    date = dateStr,
                    notes = notes
                )
                repository.addFeedInventoryTransaction(transaction)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun addIngredient(ingredient: FeedIngredient) {
        viewModelScope.launch {
            try {
                repository.addIngredient(ingredient)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun updateIngredient(ingredient: FeedIngredient) {
        viewModelScope.launch {
            try {
                repository.updateIngredient(ingredient)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    @Suppress("unused")
    fun updateInventory(ingredientId: String, qtyChange: Double, newCost: Double? = null) {
        viewModelScope.launch {
            try {
                val ingredient = _ingredients.value.find { it.id == ingredientId } ?: return@launch
                val updatedQty = (ingredient.quantity + qtyChange).coerceAtLeast(0.0)
                val updatedIngredient = ingredient.copy(
                    quantity = updatedQty,
                    costPerKg = newCost ?: ingredient.costPerKg,
                    visible = true // Ensure it stays visible if inventory is updated
                )
                repository.updateIngredient(updatedIngredient)
                
                // Record the transaction
                if (qtyChange != 0.0) {
                    val actualChange = updatedQty - ingredient.quantity
                    if (actualChange != 0.0) {
                        val transaction = com.example.smartswine.model.FeedTransaction(
                            ingredientId = ingredientId,
                            type = if (actualChange > 0) "Addition" else "Usage",
                            quantity = abs(actualChange),
                            date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date()),
                            costPerKg = newCost ?: ingredient.costPerKg
                        )
                        repository.addTransaction(transaction)
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    @Suppress("unused")
    fun toggleIngredientVisibility(ingredientId: String, visible: Boolean) {
        viewModelScope.launch {
            try {
                val ingredient = _ingredients.value.find { it.id == ingredientId } ?: return@launch
                val updatedIngredient = ingredient.copy(visible = visible)
                repository.updateIngredient(updatedIngredient)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun formulateFeed(targetStage: String, selectedIngredientIds: List<String>, shuffle: Boolean = false) {
        lastTargetStage = targetStage
        lastSelectedIngredientIds = selectedIngredientIds
        
        android.util.Log.d("FeedViewModel", "Starting formulation for $targetStage with ${selectedIngredientIds.size} ingredients (shuffle=$shuffle)")
        _error.value = null
        viewModelScope.launch {
            _isFormulating.value = true
            try {
                val targetRequirement = _requirements.value.find { 
                    it.stage.equals(targetStage, ignoreCase = true) ||
                    (targetStage.equals("Weaner/Starter", ignoreCase = true) && (it.stage.equals("Starter", ignoreCase = true) || it.stage.equals("Weaner", ignoreCase = true) || it.stage.equals("Weaner_Starter", ignoreCase = true))) ||
                    (targetStage.equals("Starter", ignoreCase = true) && (it.stage.equals("Weaner/Starter", ignoreCase = true) || it.stage.equals("Weaner_Starter", ignoreCase = true))) ||
                    it.stage.replace("/", "_").equals(targetStage.replace("/", "_"), ignoreCase = true)
                } ?: FeedRepository.DEFAULT_REQUIREMENTS.find {
                    it.stage.equals(targetStage, ignoreCase = true) ||
                    (targetStage.equals("Weaner/Starter", ignoreCase = true) && (it.stage.equals("Starter", ignoreCase = true) || it.stage.equals("Weaner", ignoreCase = true) || it.stage.equals("Weaner_Starter", ignoreCase = true))) ||
                    (targetStage.equals("Starter", ignoreCase = true) && (it.stage.equals("Weaner/Starter", ignoreCase = true) || it.stage.equals("Weaner_Starter", ignoreCase = true))) ||
                    it.stage.replace("/", "_").equals(targetStage.replace("/", "_"), ignoreCase = true)
                }
                if (targetRequirement == null) {
                    _error.value = Translator.getString("target_requirements_not_found", currentLanguage)
                    android.util.Log.e("FeedViewModel", "Target requirement not found for stage: $targetStage")
                    _isFormulating.value = false
                    return@launch
                }

                val targetProtein = targetRequirement.getTargetCrudeProtein()
                val selectedIngredients = _ingredients.value.filter { it.id in selectedIngredientIds }
                if (selectedIngredients.isEmpty()) {
                    _error.value = Translator.getString("select_at_least_one", currentLanguage)
                    _isFormulating.value = false
                    return@launch
                }

                val currentUsed = mutableMapOf<String, Double>()

                val supplementalCategory = "Vitamins, Minerals & Salt"
                val supplementalIngredients = selectedIngredients.filter { 
                    it.mainCategory.equals(supplementalCategory, ignoreCase = true) ||
                    it.name.contains("Salt", ignoreCase = true) ||
                    it.name.contains("Premix", ignoreCase = true) ||
                    it.name.contains("Limestone", ignoreCase = true) ||
                    it.name.contains("Bone Meal", ignoreCase = true) ||
                    it.name.contains("DCP", ignoreCase = true) ||
                    it.name.contains("Oyster", ignoreCase = true) ||
                    it.name.contains("Lysine", ignoreCase = true) ||
                    it.name.contains("Methionine", ignoreCase = true)
                }
                val mainIngredients = selectedIngredients.filter { it !in supplementalIngredients }

                // Veterinary Limits (Max Inclusion %) across all production stages
                val limits: Map<String, Double> = selectedIngredients.associate { ing ->
                    val limit: Double = when (targetStage.lowercase()) {
                        "creep", "pre-starter" -> minOf(ing.maxStarter * 0.7, ing.maxStarter)
                        "weaner/starter", "starter", "weaner" -> ing.maxStarter
                        "grower" -> ing.maxGrower
                        "finisher" -> ing.maxFinisher
                        "pregnant", "gestating" -> minOf(ing.maxGrower, ing.maxFinisher)
                        "lactating" -> ing.maxGrower
                        else -> minOf(ing.maxStarter, minOf(ing.maxGrower, ing.maxFinisher))
                    }
                    ing.id to (if (limit > 0.0) limit else 50.0)
                }

                // STEP 1: MINERAL & SUPPLEMENT FIRST ALLOCATION
                fun getCaPercent(ing: FeedIngredient) = if (ing.calcium > 50.0) ing.calcium / 10.0 else ing.calcium
                fun getPPercent(ing: FeedIngredient) = if (ing.phosphorus > 50.0) ing.phosphorus / 10.0 else ing.phosphorus

                var suppAllocated = 0.0

                // 1a. Essential Salt & Premix
                supplementalIngredients.forEach { ing ->
                    val nameLower = ing.name.lowercase()
                    val limit = limits[ing.id] ?: 2.0
                    if (nameLower.contains("salt")) {
                        val saltAmt = minOf(0.35, limit)
                        currentUsed[ing.id] = saltAmt
                        suppAllocated += saltAmt
                    } else if (nameLower.contains("premix") || nameLower.contains("vitamin")) {
                        val premixAmt = minOf(0.30, limit)
                        currentUsed[ing.id] = premixAmt
                        suppAllocated += premixAmt
                    }
                }

                // 1b. Calcium and Phosphorus balancing
                val mineralSources = supplementalIngredients.filter { ing ->
                    val n = ing.name.lowercase()
                    !n.contains("salt") && !n.contains("premix") && !n.contains("vitamin") &&
                    (getCaPercent(ing) > 5.0 || getPPercent(ing) > 5.0)
                }

                // Sort mineral sources: prefer DCP / Bone meal for P first, Limestone/Oyster for remaining Ca
                val sortedMinerals = mineralSources.sortedByDescending { getPPercent(it) }
                var currentCaFromSupp = currentUsed.entries.sumOf { (id, pct) ->
                    val ing = selectedIngredients.find { it.id == id } ?: return@sumOf 0.0
                    getCaPercent(ing) * (pct / 100.0)
                }
                var currentPFromSupp = currentUsed.entries.sumOf { (id, pct) ->
                    val ing = selectedIngredients.find { it.id == id } ?: return@sumOf 0.0
                    getPPercent(ing) * (pct / 100.0)
                }

                sortedMinerals.forEach { ing ->
                    val caPurity = getCaPercent(ing) / 100.0
                    val pPurity = getPPercent(ing) / 100.0
                    val limit = limits[ing.id] ?: 3.0
                    
                    var neededPct = 0.0
                    if (pPurity > 0.05 && currentPFromSupp < targetRequirement.phosphorus * 0.7) {
                        val deficitP = (targetRequirement.phosphorus * 0.7) - currentPFromSupp
                        neededPct = minOf(deficitP / pPurity, limit)
                    } else if (caPurity > 0.10 && currentCaFromSupp < targetRequirement.calcium * 0.8) {
                        val deficitCa = (targetRequirement.calcium * 0.8) - currentCaFromSupp
                        neededPct = minOf(deficitCa / caPurity, limit)
                    }

                    if (neededPct > 0.05) {
                        currentUsed[ing.id] = neededPct
                        suppAllocated += neededPct
                        currentCaFromSupp += neededPct * caPurity
                        currentPFromSupp += neededPct * pPurity
                    }
                }

                // STEP 2: BUDGET FOR MAIN INGREDIENTS
                val mainBudget = (100.0 - suppAllocated).coerceIn(10.0, 100.0)
                var availablePercent = mainBudget

                // Diversity allocation for main ingredients
                mainIngredients.forEach { ing ->
                    val name = ing.name.lowercase()
                    val minInclusion = if (name.contains("bran")) 4.0 else 3.0
                    val limit = limits[ing.id] ?: 50.0
                    val safeStart = minOf(minInclusion, limit)
                    
                    if (availablePercent >= safeStart) {
                        currentUsed[ing.id] = (currentUsed[ing.id] ?: 0.0) + safeStart
                        availablePercent -= safeStart
                    }
                }

                // Calculate remaining CP required from main ingredients
                val currentCpFromAllocated = currentUsed.entries.sumOf { (id, pct) ->
                    val ing = selectedIngredients.find { it.id == id } ?: return@sumOf 0.0
                    ing.crudeProtein * (pct / 100.0)
                }
                val remainingCpNeeded = targetProtein - currentCpFromAllocated
                val mainTargetCP = if (availablePercent > 0.5) {
                    (remainingCpNeeded / (availablePercent / 100.0)).coerceIn(5.0, 50.0)
                } else targetProtein

                // Cost-Aware Least-Cost Sorting for Energy & Protein Pools
                val poolLow = mainIngredients.filter { it.crudeProtein < mainTargetCP }
                    .let { list ->
                        if (shuffle) list.shuffled()
                        else list.sortedWith(
                            compareBy<FeedIngredient> { 
                                // Lowest cost per unit energy first
                                if (it.costPerKg > 0.0 && it.metabolizableEnergy > 0) it.costPerKg / it.metabolizableEnergy
                                else 999.0
                            }.thenByDescending { it.metabolizableEnergy }
                        )
                    }.toMutableList()
                    
                val poolHigh = mainIngredients.filter { it.crudeProtein >= mainTargetCP }
                    .let { list ->
                        if (shuffle) list.shuffled()
                        else list.sortedWith(
                            compareBy<FeedIngredient> { 
                                // Lowest cost per unit protein first
                                if (it.costPerKg > 0.0 && it.crudeProtein > 0) it.costPerKg / it.crudeProtein
                                else 999.0
                            }.thenByDescending { it.crudeProtein }
                        )
                    }.toMutableList()

                var remainingTotal = availablePercent

                // Balance Main Mix to remaining budget
                while ((remainingTotal > 0.01) && poolLow.isNotEmpty() && poolHigh.isNotEmpty()) {
                    val low = poolLow.first()
                    val high = poolHigh.first()
                    
                    val rLow = (high.crudeProtein - mainTargetCP).coerceAtLeast(0.1)
                    val rHigh = (mainTargetCP - low.crudeProtein).coerceAtLeast(0.1)
                    
                    val x = remainingTotal * (rLow / (rLow + rHigh))
                    val y = remainingTotal * (rHigh / (rLow + rHigh))
                    
                    val capLow = (limits[low.id] ?: 100.0) - (currentUsed[low.id] ?: 0.0)
                    val capHigh = (limits[high.id] ?: 100.0) - (currentUsed[high.id] ?: 0.0)
                    
                    val scaleX = if (x > 0.001) capLow / x else 1.0
                    val scaleY = if (y > 0.001) capHigh / y else 1.0
                    val scale = minOf(1.0, scaleX, scaleY)
                    
                    val useX = x * scale
                    val useY = y * scale
                    
                    currentUsed[low.id] = (currentUsed[low.id] ?: 0.0) + useX
                    currentUsed[high.id] = (currentUsed[high.id] ?: 0.0) + useY
                    remainingTotal -= (useX + useY)
                    
                    if ((currentUsed[low.id] ?: 0.0) >= ((limits[low.id] ?: 100.0) - 0.01)) poolLow.removeAt(0)
                    if ((currentUsed[high.id] ?: 0.0) >= ((limits[high.id] ?: 100.0) - 0.01)) poolHigh.removeAt(0)
                }

                // Top up any residual main budget
                if (remainingTotal > 0.01) {
                    val remainingPool = if (poolHigh.isNotEmpty()) {
                        if (shuffle) poolHigh.shuffled()
                        else poolHigh.sortedByDescending { it.metabolizableEnergy }
                    } else {
                        if (shuffle) poolLow.shuffled()
                        else poolLow.sortedByDescending { it.metabolizableEnergy }
                    }

                    for (ing in remainingPool) {
                        val cap = (limits[ing.id] ?: 100.0) - (currentUsed[ing.id] ?: 0.0)
                        val use = minOf(remainingTotal, cap)
                        currentUsed[ing.id] = (currentUsed[ing.id] ?: 0.0) + use
                        remainingTotal -= use
                        if (remainingTotal <= 0.01) break
                    }
                }

                // STEP 3: GUARANTEE EXACT 100.0% BALANCE
                val totalUsed = currentUsed.values.sum()
                if (Math.abs(totalUsed - 100.0) > 0.001) {
                    // Find main energy ingredient (highest inclusion) to adjust
                    val leadIngredientId = currentUsed.filter { (id, _) ->
                        mainIngredients.any { it.id == id }
                    }.maxByOrNull { it.value }?.key ?: currentUsed.maxByOrNull { it.value }?.key

                    if (leadIngredientId != null) {
                        val diff = 100.0 - totalUsed
                        val adjusted = (currentUsed[leadIngredientId] ?: 0.0) + diff
                        if (adjusted > 0) {
                            currentUsed[leadIngredientId] = adjusted
                        }
                    }
                }

                _formulationResult.value = currentUsed.filter { it.value > 0.001 }
                _targetRequirement.value = targetRequirement
                _error.value = null
                
                android.util.Log.d("FeedViewModel", "Formulation finished with ${_formulationResult.value?.size} ingredients. Sum: ${currentUsed.values.sum()}%")
            } catch (e: Exception) {
                _error.value = Translator.getString("formulation_failed", currentLanguage, e.message ?: "Unknown error")
            } finally {
                _isFormulating.value = false
            }
        }
    }

    fun recalculateFormulation() {
        val stage = lastTargetStage ?: return
        val ids = lastSelectedIngredientIds ?: return
        formulateFeed(stage, ids, shuffle = true)
    }

    fun calculateRequirements(stats: Map<String, Any>) {
        val days = (stats["days"] as? Int) ?: 1
        val requirements = mutableMapOf<String, Double>()
        
        // Define standard intake rates (kg per animal per day) based on updated logic
        val rates = mapOf(
            "Starter" to 0.7,
            "Grower" to 1.8,
            "Finisher" to 2.5,
            "breeders_starter" to 0.7,
            "breeders_grower" to 1.8,
            "gilts" to 2.2,
            "boars" to 2.2,
            "sows" to 2.2, // Standard sow rate (non-pregnant, non-lactating)
            "Pregnant" to 2.2, // Standard average gestation rate (prevents overfeeding)
            "Lactating" to 5.5, // Average lactation intake rate supporting piglet litter
        )

        var totalDaily = 0.0
        rates.forEach { (category, rate) ->
            val count = (stats[category] as? Int) ?: 0
            val amount = count * rate
            if (amount > 0) {
                val label = "${category.replaceFirstChar { it.uppercase() }} ($count)"
                requirements[label] = amount
                totalDaily += amount
            }
        }
        
        // Metadata for the UI header
        requirements["__days"] = days.toDouble()
        
        if (days > 1) {
            requirements["Daily Total"] = totalDaily
            requirements["Total for $days Days"] = totalDaily * days
        } else {
            requirements["Total Daily Requirement"] = totalDaily
        }
        
        _feedRequirements.value = requirements
    }

    fun clearError() {
        _error.value = null
    }

    suspend fun getTransactionsForExport(startDate: String, endDate: String): List<com.example.smartswine.model.FeedTransaction> {
        return try {
            repository.getTransactionsByDateRange(startDate, endDate)
        } catch (e: Exception) {
            _error.value = e.message
            emptyList()
        }
    }

    suspend fun getFeedInventoryTransactionsForExport(startDate: String, endDate: String): List<FeedInventoryTransaction> {
        return try {
            repository.getFeedInventoryTransactionsByDateRange(startDate, endDate)
        } catch (e: Exception) {
            _error.value = e.message
            emptyList()
        }
    }

    fun checkIngredientSafety(
        items: List<Pair<FeedIngredient, Double>>,
        stage: String
    ): List<InclusionSafetyAlert> {
        if (items.isEmpty()) return emptyList()
        val totalQty = items.sumOf { it.second }.coerceAtLeast(0.0001)
        val alerts = mutableListOf<InclusionSafetyAlert>()

        items.forEach { (ing, qty) ->
            val currentPercent = (qty / totalQty) * 100.0
            val maxLimit: Double = when (stage.lowercase()) {
                "creep", "pre-starter" -> minOf(ing.maxStarter * 0.7, ing.maxStarter)
                "weaner/starter", "starter", "weaner" -> ing.maxStarter
                "grower" -> ing.maxGrower
                "finisher" -> ing.maxFinisher
                "pregnant", "gestating" -> minOf(ing.maxGrower, ing.maxFinisher)
                "lactating" -> ing.maxGrower
                else -> minOf(ing.maxStarter, minOf(ing.maxGrower, ing.maxFinisher))
            }

            if (maxLimit in 0.01..99.9 && currentPercent > (maxLimit + 0.05)) {
                val ingNameLower = ing.name.lowercase()
                val (riskKey, risk) = when {
                    ingNameLower.contains("cottonseed") -> "risk_gossypol_toxicity" to "Excess gossypol toxicity risk (heart & liver damage in monogastrics)"
                    ingNameLower.contains("cassava peel") -> "risk_hydrocyanic_acid" to "High hydrocyanic acid & fibrous anti-nutritional factor risk"
                    ingNameLower.contains("salt") && currentPercent > 0.5 -> "risk_salt_toxicity" to "Risk of hypernatremia / salt toxicity; ensure unlimited fresh water"
                    ingNameLower.contains("fish") && currentPercent > 10.0 -> "risk_fishy_taint" to "Fishy taint risk in meat quality and high sodium / mineral load"
                    ingNameLower.contains("wheat bran") || ingNameLower.contains("rice bran") -> "risk_excess_fiber" to "Excess dietary fiber impairs nutrient digestion and feed conversion"
                    ingNameLower.contains("bone meal") || ingNameLower.contains("dcp") -> "risk_excess_mineral" to "Excess mineral inclusion can disrupt calcium-to-phosphorus absorption"
                    else -> "risk_exceeds_limit" to "Exceeds safe recommended inclusion limit ($maxLimit%) for $stage stage"
                }
                alerts.add(
                    InclusionSafetyAlert(
                        ingredientName = ing.name,
                        currentPercent = currentPercent,
                        maxAllowedPercent = maxLimit,
                        stage = stage,
                        riskDescription = risk,
                        riskKey = riskKey
                    )
                )
            }
        }
        return alerts
    }

    fun calculateNutritionalContent(
        items: List<Pair<FeedIngredient, Double>>,
        isPercentageMode: Boolean = false,
        isDryMatterMode: Boolean = false
    ): FeedNutrientProfile {
        if (items.isEmpty()) return FeedNutrientProfile()
        
        val totalQty = items.sumOf { it.second }.coerceAtLeast(0.0001)

        var cp = 0.0
        var me = 0.0
        var cf = 0.0
        var ca = 0.0
        var p = 0.0
        var lys = 0.0
        var met = 0.0
        var weightedDm = 0.0
        var totalCost = 0.0

        items.forEach { (ing, qty) ->
            val fraction = qty / totalQty
            val effectiveQty = if (isPercentageMode) (qty / 100.0) * 100.0 else qty

            cp += ing.crudeProtein * fraction
            me += ing.metabolizableEnergy * fraction
            cf += ing.crudeFiber * fraction
            
            val caPct = if (ing.calcium > 50.0) ing.calcium / 10.0 else ing.calcium
            val pPct = if (ing.phosphorus > 50.0) ing.phosphorus / 10.0 else ing.phosphorus
            ca += caPct * fraction
            p += pPct * fraction

            // True Amino Acid logic (% of diet)
            val isPureLys = ing.name.contains("Lysine", ignoreCase = true) || (ing.crudeProtein > 80.0 && ing.lysine > 50.0)
            val ingDietaryLys = if (isPureLys) ing.lysine else (ing.crudeProtein * (ing.lysine / 100.0))
            lys += ingDietaryLys * fraction

            val isPureMet = ing.name.contains("Methionine", ignoreCase = true) || (ing.crudeProtein > 80.0 && (ing.methionine + ing.cystine) > 50.0)
            val ingDietaryMet = if (isPureMet) (ing.methionine + ing.cystine) else (ing.crudeProtein * ((ing.methionine + ing.cystine) / 100.0))
            met += ingDietaryMet * fraction

            val dmVal = if (ing.dryMatter > 0.0) ing.dryMatter else 90.0
            weightedDm += dmVal * fraction

            if (ing.costPerKg > 0.0) {
                totalCost += ing.costPerKg * effectiveQty
            }
        }

        val totalWeight = if (isPercentageMode) 100.0 else items.sumOf { it.second }
        val costPerKg = if (isPercentageMode) {
            items.sumOf { (ing, qty) -> (qty / totalQty) * (if (ing.costPerKg > 0) ing.costPerKg else 0.0) }
        } else {
            if (totalQty > 0.0001) totalCost / totalQty else 0.0
        }
        val costPer50kgBag = costPerKg * 50.0

        val caPRatio = if (p > 0.0001) ca / p else 0.0

        // Dry Matter Basis Conversion if enabled
        val dmFactor = if (isDryMatterMode && weightedDm > 0.0) 100.0 / weightedDm else 1.0
        val finalCp = cp * dmFactor
        val finalMe = me * dmFactor
        val finalCf = cf * dmFactor
        val finalCa = ca * dmFactor
        val finalP = p * dmFactor
        val finalLys = lys * dmFactor
        val finalMet = met * dmFactor
        val finalDp = finalCp * 0.85

        return FeedNutrientProfile(
            crudeProtein = finalCp,
            metabolizableEnergy = finalMe,
            digestibleProtein = finalDp,
            crudeFiber = finalCf,
            calcium = finalCa,
            phosphorus = finalP,
            lysine = finalLys,
            methionine = finalMet,
            totalWeight = totalWeight,
            costPerKg = costPerKg,
            costPer50kgBag = costPer50kgBag,
            totalCost = if (isPercentageMode) costPerKg * 100.0 else totalCost,
            caPRatio = caPRatio,
            dryMatterPercent = weightedDm
        )
    }

    override fun onCleared() {
        super.onCleared()
        android.util.Log.d("FeedViewModel", "ViewModel cleared: ${this.hashCode()}")
    }
}

data class InclusionSafetyAlert(
    val ingredientName: String,
    val currentPercent: Double,
    val maxAllowedPercent: Double,
    val stage: String,
    val riskDescription: String,
    val riskKey: String = ""
)

typealias IngredientSafetyAlert = InclusionSafetyAlert

data class FeedNutrientProfile(
    val crudeProtein: Double = 0.0,
    val metabolizableEnergy: Double = 0.0,
    val digestibleProtein: Double = 0.0,
    val crudeFiber: Double = 0.0,
    val calcium: Double = 0.0,
    val phosphorus: Double = 0.0,
    val lysine: Double = 0.0,
    val methionine: Double = 0.0,
    val totalWeight: Double = 0.0,
    val costPerKg: Double = 0.0,
    val costPer50kgBag: Double = 0.0,
    val totalCost: Double = 0.0,
    val caPRatio: Double = 0.0,
    val dryMatterPercent: Double = 90.0
)
