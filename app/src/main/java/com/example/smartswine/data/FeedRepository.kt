package com.example.smartswine.data

import android.content.Context
import com.bibiniitech.smartswine.R
import com.example.smartswine.model.FeedIngredient
import com.example.smartswine.model.FeedTransaction
import com.example.smartswine.model.NutritionalRequirement
import com.example.smartswine.model.FeedInventoryItem
import com.example.smartswine.model.FeedInventoryTransaction
import com.example.smartswine.model.FinancialRecord
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FeedRepository {
    private val firestore: FirebaseFirestore by lazy {
        FirestoreManager.configure()
        FirebaseFirestore.getInstance()
    }
    private val auth = FirebaseAuth.getInstance()
    
    // Active Farm ID for multi-user support
    private var activeFarmId: String? = null

    fun setActiveFarmId(uid: String) {
        activeFarmId = uid
    }

    private val globalIngredientsCollection = firestore.collection("global_feed_ingredients")

    private val ingredientsCollection
        get() = firestore.collection("users")
            .document(activeFarmId ?: auth.currentUser?.uid ?: "anonymous")
            .collection("feed_ingredients")

    private val requirementsCollection
        get() = firestore.collection("users")
            .document(activeFarmId ?: auth.currentUser?.uid ?: "anonymous")
            .collection("nutritional_requirements")

    private val transactionsCollection
        get() = firestore.collection("users")
            .document(activeFarmId ?: auth.currentUser?.uid ?: "anonymous")
            .collection("feed_transactions")

    private val feedInventoryCollection
        get() = firestore.collection("users")
            .document(activeFarmId ?: auth.currentUser?.uid ?: "anonymous")
            .collection("feed_inventory")

    private val feedInventoryTransactionsCollection
        get() = firestore.collection("users")
            .document(activeFarmId ?: auth.currentUser?.uid ?: "anonymous")
            .collection("feed_inventory_transactions")

    private val financialsCollection
        get() = firestore.collection("users")
            .document(activeFarmId ?: auth.currentUser?.uid ?: "anonymous")
            .collection("financials")

    fun getAllFeedInventoryItems(): Flow<List<FeedInventoryItem>> = callbackFlow {
        val subscription = feedInventoryCollection.addSnapshotListener { snapshot, error ->
            if (auth.currentUser == null) {
                return@addSnapshotListener
            }
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val items = snapshot.toObjects(FeedInventoryItem::class.java)
                trySend(items)
            }
        }
        awaitClose { subscription.remove() }
    }

    fun getAllFeedInventoryTransactions(): Flow<List<FeedInventoryTransaction>> = callbackFlow {
        val subscription = feedInventoryTransactionsCollection.addSnapshotListener { snapshot, error ->
            if (auth.currentUser == null) {
                return@addSnapshotListener
            }
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val transactions = snapshot.toObjects(FeedInventoryTransaction::class.java)
                trySend(transactions)
            }
        }
        awaitClose { subscription.remove() }
    }

    suspend fun addFeedInventoryItem(item: FeedInventoryItem): String {
        val docRef = feedInventoryCollection.document()
        val itemWithId = item.copy(id = docRef.id)
        docRef.set(itemWithId).await()
        return docRef.id
    }

    suspend fun updateFeedInventoryItem(item: FeedInventoryItem) {
        if (item.id.isNotEmpty()) {
            feedInventoryCollection.document(item.id).set(item).await()
        }
    }

    suspend fun deleteFeedInventoryItem(itemId: String) {
        if (itemId.isNotEmpty()) {
            feedInventoryCollection.document(itemId).delete().await()
        }
    }

    suspend fun addFeedInventoryTransaction(transaction: FeedInventoryTransaction) {
        val docRef = feedInventoryTransactionsCollection.document()
        val transactionWithId = transaction.copy(id = docRef.id)
        docRef.set(transactionWithId).await()
    }

    suspend fun addFinancialExpense(amount: Double, description: String, date: String) {
        val docRef = financialsCollection.document()
        val record = FinancialRecord(
            id = docRef.id,
            date = date,
            type = "Expense",
            category = "Feed",
            amount = amount,
            description = description
        )
        docRef.set(record).await()
    }

    fun getAllIngredients(): Flow<List<FeedIngredient>> = callbackFlow {
        val subscription = ingredientsCollection.addSnapshotListener { snapshot, error ->
            if (auth.currentUser == null) {
                return@addSnapshotListener
            }
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val ingredients = snapshot.toObjects(FeedIngredient::class.java)
                trySend(ingredients)
            }
        }
        awaitClose { subscription.remove() }
    }

    suspend fun addIngredient(ingredient: FeedIngredient) {
        val docRef = ingredientsCollection.document()
        val ingredientWithId = ingredient.copy(id = docRef.id)
        docRef.set(ingredientWithId).await()
    }

    suspend fun addTransaction(transaction: FeedTransaction) {
        val docRef = transactionsCollection.document()
        val transactionWithId = transaction.copy(id = docRef.id)
        docRef.set(transactionWithId).await()
    }

    suspend fun getTransactionsByDateRange(startDate: String, endDate: String): List<FeedTransaction> {
        return try {
            transactionsCollection
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate + "\uf8ff")
                .get()
                .await()
                .toObjects(FeedTransaction::class.java)
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getFeedInventoryTransactionsByDateRange(startDate: String, endDate: String): List<FeedInventoryTransaction> {
        return try {
            feedInventoryTransactionsCollection
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate + "\uf8ff")
                .get()
                .await()
                .toObjects(FeedInventoryTransaction::class.java)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun loadIngredientsFromJson(context: Context): List<FeedIngredient> {
        return try {
            val inputStream = context.resources.openRawResource(R.raw.ingredients)
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val gson = Gson()
            val type = object : TypeToken<List<Map<String, Any>>>() {}.type
            val rawList: List<Map<String, Any>> = gson.fromJson(jsonString, type)

            rawList.map { map ->
                val nameResourceIdStr = map["nameResourceId"] as? String
                val resId = if (nameResourceIdStr != null) {
                    context.resources.getIdentifier(nameResourceIdStr, "string", context.packageName)
                } else 0

                FeedIngredient(
                    name = map["name"] as? String ?: "",
                    nameResourceId = resId,
                    mainCategory = map["mainCategory"] as? String ?: "",
                    dryMatter = (map["dryMatter"] as? Number)?.toDouble() ?: 0.0,
                    crudeProtein = (map["crudeProtein"] as? Number)?.toDouble() ?: 0.0,
                    crudeFiber = (map["crudeFiber"] as? Number)?.toDouble() ?: 0.0,
                    calcium = (map["calcium"] as? Number)?.toDouble() ?: 0.0,
                    phosphorus = (map["phosphorus"] as? Number)?.toDouble() ?: 0.0,
                    sodium = (map["sodium"] as? Number)?.toDouble() ?: 0.0,
                    chloride = (map["chloride"] as? Number)?.toDouble() ?: 0.0,
                    potassium = (map["potassium"] as? Number)?.toDouble() ?: 0.0,
                    sulfur = (map["sulfur"] as? Number)?.toDouble() ?: 0.0,
                    metabolizableEnergy = (map["metabolizableEnergy"] as? Number)?.toDouble() ?: 0.0,
                    lysine = (map["lysine"] as? Number)?.toDouble() ?: 0.0,
                    methionine = (map["methionine"] as? Number)?.toDouble() ?: 0.0,
                    cystine = (map["cystine"] as? Number)?.toDouble() ?: 0.0,
                    threonine = (map["threonine"] as? Number)?.toDouble() ?: 0.0,
                    tryptophan = (map["tryptophan"] as? Number)?.toDouble() ?: 0.0,
                    arginine = (map["arginine"] as? Number)?.toDouble() ?: 0.0,
                    isoleucine = (map["isoleucine"] as? Number)?.toDouble() ?: 0.0,
                    valine = (map["valine"] as? Number)?.toDouble() ?: 0.0,
                    fat = (map["fat"] as? Number)?.toDouble() ?: 0.0,
                    category = map["category"] as? String ?: "",
                    maxStarter = (map["maxStarter"] as? Number)?.toDouble() ?: 0.0,
                    maxGrower = (map["maxGrower"] as? Number)?.toDouble() ?: 0.0,
                    maxFinisher = (map["maxFinisher"] as? Number)?.toDouble() ?: 0.0
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("FeedRepository", "Error loading ingredients from JSON", e)
            emptyList()
        }
    }

    suspend fun initializeDefaultIngredients(context: Context) {
        val defaultIngredients = loadIngredientsFromJson(context)

        // Seed and update global collection (wrapped in try-catch to support non-admin users)
        try {
            val globalQuery = globalIngredientsCollection.get().await()
            val existingGlobalMap = globalQuery.documents.associateBy { it.getString("name") }
            val globalBatch: WriteBatch = firestore.batch()
            var globalBatchNeeded = false

            for (ingredient in defaultIngredients) {
                val existingDoc = existingGlobalMap[ingredient.name]
                if (existingDoc == null) {
                    val docRef = globalIngredientsCollection.document()
                    globalBatch.set(docRef, ingredient.copy(id = docRef.id))
                    globalBatchNeeded = true
                } else {
                    val existingObj = existingDoc.toObject(FeedIngredient::class.java)
                    if (existingObj != null) {
                        val expectedObj = ingredient.copy(
                            id = existingDoc.id,
                            quantity = existingObj.quantity,
                            costPerKg = existingObj.costPerKg,
                            visible = existingObj.visible
                        )
                        if (existingObj != expectedObj) {
                            globalBatch.set(globalIngredientsCollection.document(existingDoc.id), expectedObj)
                            globalBatchNeeded = true
                        }
                    }
                }
            }
            if (globalBatchNeeded) {
                globalBatch.commit().await()
            }
        } catch (e: Exception) {
            android.util.Log.w("FeedRepository", "Failed to update global collection (likely non-admin): ${e.message}")
        }

        // Fetch latest from global database
        var latestGlobalList = try {
            globalIngredientsCollection.get().await().toObjects(FeedIngredient::class.java)
        } catch (e: Exception) {
            emptyList<FeedIngredient>()
        }
        
        // Fallback to defaults if the global collection is empty or inaccessible
        if (latestGlobalList.isEmpty()) {
            latestGlobalList = defaultIngredients
        }

        val defaultIngredientsMap = defaultIngredients.associateBy { it.name }

        // Optimize user collection update: fetch all first to avoid N queries in a loop
        val userQuery = ingredientsCollection.get().await()
        val existingUserMap = userQuery.documents.associateBy { it.getString("name") }
        val userBatch: WriteBatch = firestore.batch()
        var userBatchNeeded = false

        for (ingredient in latestGlobalList) {
            // Use codebase definition if available as the ultimate source of truth for categories & nutrients
            val baseIngredient = defaultIngredientsMap[ingredient.name] ?: ingredient
            val existingDoc = existingUserMap[baseIngredient.name]
            
            if (existingDoc == null) {
                val docRef = ingredientsCollection.document()
                userBatch.set(docRef, baseIngredient.copy(id = docRef.id))
                userBatchNeeded = true
            } else {
                val docId = existingDoc.id
                val existing = existingDoc.toObject(FeedIngredient::class.java)
                if (existing != null) {
                    val updatedIngredient = baseIngredient.copy(
                        id = docId,
                        quantity = existing.quantity,
                        costPerKg = existing.costPerKg,
                        visible = existing.visible
                    )
                    if (existing != updatedIngredient) {
                        userBatch.set(ingredientsCollection.document(docId), updatedIngredient)
                        userBatchNeeded = true
                    }
                } else {
                    userBatch.set(ingredientsCollection.document(docId), baseIngredient.copy(id = docId))
                    userBatchNeeded = true
                }
            }
        }
        
        if (userBatchNeeded) {
            userBatch.commit().await()
        }
    }

    suspend fun updateIngredient(ingredient: FeedIngredient) {
        if (ingredient.id.isNotEmpty()) {
            ingredientsCollection.document(ingredient.id).set(ingredient).await()
        }
    }

    fun getAllRequirements(): Flow<List<NutritionalRequirement>> = callbackFlow {
        val subscription = requirementsCollection.addSnapshotListener { snapshot, error ->
            if (auth.currentUser == null) {
                return@addSnapshotListener
            }
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val requirements = snapshot.toObjects(NutritionalRequirement::class.java)
                trySend(requirements)
            }
        }
        awaitClose { subscription.remove() }
    }

    suspend fun initializeDefaultRequirements() {
        val defaultRequirements = listOf(
            NutritionalRequirement("Starter", 17.0, 3350.0, 0.90, 0.75, 7.90, 5.20, 1.25, 3.0, 0.35, 0.85),
            NutritionalRequirement("Grower", 14.5, 3300.0, 0.75, 0.50, 6.10, 4.00, 1.10, 5.0, 0.75, 1.50),
            NutritionalRequirement("Finisher", 13.0, 3300.0, 0.75, 0.50, 5.70, 3.00, 1.00, 6.0, 1.50, 2.50),
        )

        for (requirement in defaultRequirements) {
            // Use set() which will update existing or create new
            requirementsCollection.document(requirement.stage).set(requirement).await()
        }
    }

    fun getGlobalIngredients(): Flow<List<FeedIngredient>> = callbackFlow {
        val subscription = globalIngredientsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val list = snapshot.toObjects(FeedIngredient::class.java)
                trySend(list)
            }
        }
        awaitClose { subscription.remove() }
    }

    suspend fun addGlobalIngredient(ingredient: FeedIngredient) {
        val docRef = globalIngredientsCollection.document()
        val toSave = ingredient.copy(id = docRef.id)
        docRef.set(toSave).await()
    }

    suspend fun updateGlobalIngredient(ingredient: FeedIngredient) {
        if (ingredient.id.isNotEmpty()) {
            globalIngredientsCollection.document(ingredient.id).set(ingredient).await()
        }
    }

    suspend fun deleteGlobalIngredient(ingredientId: String) {
        if (ingredientId.isNotEmpty()) {
            globalIngredientsCollection.document(ingredientId).delete().await()
        }
    }
}
