package com.example.smartswine.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartswine.data.FeedRepository
import com.example.smartswine.model.FeedIngredient
import com.example.smartswine.model.NutritionalRequirement
import com.example.smartswine.model.FeedInventoryItem
import com.example.smartswine.model.FeedInventoryTransaction
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

class FeedViewModel : ViewModel() {
    private val repository = FeedRepository()

    // Active Farm ID for multi-user support
    private var activeFarmId: String? = null

    fun setActiveFarmId(uid: String) {
        if (activeFarmId != uid) {
            activeFarmId = uid
            repository.setActiveFarmId(uid)
            initializeData()
        }
    }

    private val _ingredients = MutableStateFlow<List<FeedIngredient>>(emptyList())
    val ingredients: StateFlow<List<FeedIngredient>> = _ingredients.asStateFlow()

    private val _globalIngredients = MutableStateFlow<List<FeedIngredient>>(emptyList())
    val globalIngredients: StateFlow<List<FeedIngredient>> = _globalIngredients.asStateFlow()

    private val _requirements = MutableStateFlow<List<NutritionalRequirement>>(emptyList())
    val nutritionalRequirements: StateFlow<List<NutritionalRequirement>> = _requirements.asStateFlow()

    private val _feedInventoryItems = MutableStateFlow<List<FeedInventoryItem>>(emptyList())
    val feedInventoryItems: StateFlow<List<FeedInventoryItem>> = _feedInventoryItems.asStateFlow()

    private val _feedInventoryTransactions = MutableStateFlow<List<FeedInventoryTransaction>>(emptyList())
    val feedInventoryTransactions: StateFlow<List<FeedInventoryTransaction>> = _feedInventoryTransactions.asStateFlow()

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

        // Try to sync/initialize defaults in the background
        viewModelScope.launch {
            try {
                repository.initializeDefaultIngredients()
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
                    // Normalize categories and filter out duplicates by name as a UI safeguard
                    val normalizedList = list.asSequence()
                        .map { normalizeIngredient(it) }
                        .distinctBy { it.name }
                        .toList()
                    
                    _ingredients.value = normalizedList
                    // Only stop loading if we have some data or error handled above
                    _isLoading.value = false
                }
        }
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
                    _feedInventoryTransactions.value = list.sortedByDescending { it.date }
                }
        }
    }

    fun addFeedInventoryItem(name: String, feedType: String, initialQty: Double, unit: String, unitWeight: Double, minThreshold: Double) {
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
                    lastUpdated = dateStr
                )
                val generatedId = repository.addFeedInventoryItem(item)
                
                if (initialQty > 0.0) {
                    val transaction = FeedInventoryTransaction(
                        itemId = generatedId,
                        itemName = name,
                        type = "Restock",
                        quantity = initialQty,
                        unit = unit,
                        cost = 0.0,
                        date = dateStr,
                        notes = "Initial stock entry"
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
        viewModelScope.launch {
            _isFormulating.value = true
            try {
                val targetRequirement = _requirements.value.find { it.stage == targetStage }
                if (targetRequirement == null) {
                    _error.value = Translator.getString("target_requirements_not_found", currentLanguage)
                    android.util.Log.e("FeedViewModel", "Target requirement not found for stage: $targetStage")
                    _isFormulating.value = false
                    return@launch
                }

                val targetProtein = targetRequirement.digestibleProtein 
                val selectedIngredients = _ingredients.value.filter { it.id in selectedIngredientIds }
                if (selectedIngredients.isEmpty()) {
                    _error.value = Translator.getString("select_at_least_one", currentLanguage)
                    _isFormulating.value = false
                    return@launch
                }

                val mandatoryNames = listOf("Mycotoxin Binder", "Common salt", "Vitamin Premix")
                val mandatoryInclusions = mapOf(
                    "Mycotoxin Binder" to 1.0,
                    "Common salt" to 0.5,
                    "Vitamin Premix" to 1.0,
                )
                
                val currentUsed = mutableMapOf<String, Double>()
                
                var availablePercent = 100.0
                mandatoryInclusions.forEach { (name, percent) ->
                    val ingredient = _ingredients.value.find { it.name.contains(name, ignoreCase = true) }
                    if (ingredient != null) {
                        currentUsed[ingredient.id] = percent
                        availablePercent -= percent
                    } else {
                        currentUsed[name] = percent
                        availablePercent -= percent
                    }
                }

                val supplementalCategory = "Vitamins, Minerals & Salt"
                val supplementalIngredients = selectedIngredients.filter { 
                    it.mainCategory == supplementalCategory && !mandatoryNames.any { name -> it.name.contains(name, ignoreCase = true) }
                }
                val mainIngredients = selectedIngredients.filter { 
                    it.mainCategory != supplementalCategory || mandatoryNames.any { name -> it.name.contains(name, ignoreCase = true) }
                }

                // Veterinary Limits (Max Inclusion %)
                val limits = selectedIngredients.associate { ing ->
                    val limit = when (targetStage.lowercase()) {
                        "starter" -> ing.maxStarter
                        "grower" -> ing.maxGrower
                        "finisher" -> ing.maxFinisher
                        "sow", "boar", "pregnant", "lactating" -> minOf(ing.maxGrower, ing.maxFinisher)
                        else -> minOf(ing.maxStarter, ing.maxGrower, ing.maxFinisher)
                    }
                    ing.id to limit
                }

                // Initial pass: Diversity for Main Ingredients ONLY
                mainIngredients.forEach { ing ->
                    if (mandatoryNames.any { name -> ing.name.contains(name, ignoreCase = true) }) return@forEach
                    val name = ing.name.lowercase()
                    val minInclusion = if (name.contains("bran")) 8.0 else 4.0
                    val limit = limits[ing.id] ?: 50.0
                    val safeStart = minOf(minInclusion, limit)
                    
                    if (availablePercent >= safeStart) {
                        currentUsed[ing.id] = (currentUsed[ing.id] ?: 0.0) + safeStart
                        availablePercent -= safeStart
                    }
                }

                var remainingTotal = availablePercent

                // Pearson Square Pools for Main Ingredients
                val poolLow = mainIngredients.filter { it.crudeProtein < targetProtein && !mandatoryNames.any { name -> it.name.contains(name, ignoreCase = true) } }
                    .let { list ->
                        if (shuffle) list.shuffled() 
                        else list.sortedByDescending { it.metabolizableEnergy }
                    }.toMutableList()
                    
                val poolHigh = mainIngredients.filter { it.crudeProtein >= targetProtein && !mandatoryNames.any { name -> it.name.contains(name, ignoreCase = true) } }
                    .let { list ->
                        if (shuffle) list.shuffled() 
                        else list.sortedByDescending { it.metabolizableEnergy }
                    }.toMutableList()

                // Balance Main Mix to 100%
                while ((remainingTotal > 0.01) && poolLow.isNotEmpty() && poolHigh.isNotEmpty()) {
                    val low = poolLow.first()
                    val high = poolHigh.first()
                    
                    val rLow = high.crudeProtein - targetProtein
                    val rHigh = targetProtein - low.crudeProtein
                    
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

                if (remainingTotal > 0.01) {
                    val remainingPool = if (poolHigh.isNotEmpty()) {
                        if (shuffle) poolHigh.shuffled()
                        else poolHigh.sortedByDescending { it.metabolizableEnergy / (it.crudeProtein + 1.0) }
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

                // SECONDARY PASS: Supplemental Ingredients based on deficits
                // 1. Calculate current levels
                fun getCaPercent(ing: FeedIngredient) = if (ing.calcium > 10.0) ing.calcium / 10.0 else ing.calcium
                fun getPPercent(ing: FeedIngredient) = if (ing.phosphorus > 10.0) ing.phosphorus / 10.0 else ing.phosphorus
                
                fun calculateCurrentNutrients(): Map<String, Double> {
                    var ca = 0.0; var p = 0.0; var lys = 0.0; var met = 0.0
                    currentUsed.forEach { (id, percent) ->
                        val ing = _ingredients.value.find { it.id == id } ?: return@forEach
                        val factor = percent / 100.0
                        ca += getCaPercent(ing) * factor
                        p += getPPercent(ing) * factor
                        lys += ing.lysine * factor
                        met += (ing.methionine + ing.cystine) * factor
                    }
                    return mapOf("ca" to ca, "p" to p, "lys" to lys, "met" to met)
                }

                val targetCa = targetRequirement.calcium
                val targetP = targetRequirement.phosphorus
                val targetLys = (targetRequirement.lysine / 100.0) * targetRequirement.digestibleProtein
                val targetMet = (targetRequirement.methionineCystine / 100.0) * targetRequirement.digestibleProtein

                var totalAddedSupplements = 0.0
                
                // Sort supplements to prioritize those that fill multiple deficits or are more concentrated
                val sortedSupplements = supplementalIngredients.sortedByDescending { it.calcium + it.phosphorus + it.lysine + it.methionine }

                for (ing in sortedSupplements) {
                    val current = calculateCurrentNutrients()
                    val defCa = maxOf(0.0, targetCa - (current["ca"] ?: 0.0))
                    val defP = maxOf(0.0, targetP - (current["p"] ?: 0.0))
                    val defLys = maxOf(0.0, targetLys - (current["lys"] ?: 0.0))
                    val defMet = maxOf(0.0, targetMet - (current["met"] ?: 0.0))
                    
                    if (defCa <= 0 && defP <= 0 && defLys <= 0 && defMet <= 0) break
                    
                    // How much of this ingredient do we need to fill the biggest deficit it addresses?
                    val needCa = if (getCaPercent(ing) > 0) defCa / (getCaPercent(ing) / 100.0) else 0.0
                    val needP = if (getPPercent(ing) > 0) defP / (getPPercent(ing) / 100.0) else 0.0
                    val needLys = if (ing.lysine > 0) defLys / (ing.lysine / 100.0) else 0.0
                    val needMet = if ((ing.methionine + ing.cystine) > 0) defMet / ((ing.methionine + ing.cystine) / 100.0) else 0.0
                    
                    var needed = maxOf(needCa, needP, needLys, needMet)
                    val limit = limits[ing.id] ?: 2.0
                    needed = minOf(needed, limit)
                    
                    if (needed > 0.01) {
                        currentUsed[ing.id] = needed
                        totalAddedSupplements += needed
                    }
                }

                // Final adjustment: Reduce main ingredients to make room for supplements while keeping 100%
                if (totalAddedSupplements > 0) {
                    val mainTotal = 100.0 - (mandatoryInclusions.values.sum()) - totalAddedSupplements
                    val previousMainTotal = 100.0 - (mandatoryInclusions.values.sum())
                    val scaleFactor = mainTotal / previousMainTotal
                    
                    val mainIds = mainIngredients.map { it.id }.filter { !mandatoryNames.any { name -> 
                        val ing = _ingredients.value.find { i -> i.id == it }
                        ing?.name?.contains(name, ignoreCase = true) ?: false
                    } }
                    
                    mainIds.forEach { id ->
                        if (currentUsed.containsKey(id)) {
                            currentUsed[id] = (currentUsed[id] ?: 0.0) * scaleFactor
                        }
                    }
                }

                _formulationResult.value = currentUsed.filter { it.value > 0.001 }
                _targetRequirement.value = targetRequirement
                
                if (remainingTotal > 0.1) {
                    val totalStr = "%.1f".format(Locale.getDefault(), 100.0 - remainingTotal)
                    _error.value = Translator.getString("formula_incomplete", currentLanguage, totalStr)
                }
                android.util.Log.d("FeedViewModel", "Formulation result set: ${_formulationResult.value?.size} ingredients. Remaining: $remainingTotal")
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

    override fun onCleared() {
        super.onCleared()
        android.util.Log.d("FeedViewModel", "ViewModel cleared: ${this.hashCode()}")
    }
}
