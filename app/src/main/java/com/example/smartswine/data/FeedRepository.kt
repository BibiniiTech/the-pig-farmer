package com.example.smartswine.data

import com.bibiniitech.smartswine.R
import com.example.smartswine.model.FeedIngredient
import com.example.smartswine.model.FeedTransaction
import com.example.smartswine.model.NutritionalRequirement
import com.example.smartswine.model.FeedInventoryItem
import com.example.smartswine.model.FeedInventoryTransaction
import com.example.smartswine.model.FinancialRecord
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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

    suspend fun initializeDefaultIngredients() {
        val energy = "Energy"
        val protein = "Protein"
        val minerals = "Vitamins, Minerals & Salt"

        val defaultIngredients = listOf(
            // ENERGY SOURCES
            FeedIngredient(name = "Maize", nameResourceId = R.string.ingredient_maize, mainCategory = energy, dryMatter = 86.30, crudeProtein = 8.00, crudeFiber = 2.40, calcium = 0.04, phosphorus = 0.29, sodium = 0.05, chloride = 0.00, potassium = 0.36, sulfur = 0.00, metabolizableEnergy = 18.80 * 239, lysine = 2.90, methionine = 1.70, cystine = 2.10, threonine = 4.20, tryptophan = 1.80, arginine = 6.60, isoleucine = 4.00, valine = 5.30, maxStarter = 70.0, maxGrower = 65.0, maxFinisher = 30.0),
            FeedIngredient(name = "Maize bran", nameResourceId = R.string.ingredient_maize_bran, mainCategory = energy, dryMatter = 88.70, crudeProtein = 11.00, crudeFiber = 12.30, calcium = 0.47, phosphorus = 0.34, sodium = 0.08, chloride = 0.00, potassium = 0.73, sulfur = 0.00, metabolizableEnergy = 18.50 * 239, lysine = 4.20, methionine = 1.80, cystine = 2.10, threonine = 4.00, tryptophan = 1.10, arginine = 5.20, isoleucine = 3.30, valine = 5.10, maxStarter = 20.0, maxGrower = 30.0, maxFinisher = 35.0),
            FeedIngredient(name = "Rice bran", nameResourceId = R.string.ingredient_rice_bran, mainCategory = energy, dryMatter = 90.00, crudeProtein = 12.70, crudeFiber = 16.30, calcium = 0.07, phosphorus = 1.38, sodium = 0.02, chloride = 0.00, potassium = 1.23, sulfur = 0.00, metabolizableEnergy = 20.50 * 239, lysine = 4.40, methionine = 1.90, cystine = 1.70, threonine = 3.70, tryptophan = 2.20, arginine = 7.20, isoleucine = 5.30, valine = 5.40, maxStarter = 10.0, maxGrower = 20.0, maxFinisher = 30.0),
            FeedIngredient(name = "Wheat bran", nameResourceId = R.string.ingredient_wheat_bran, mainCategory = energy, dryMatter = 87.00, crudeProtein = 17.30, crudeFiber = 10.40, calcium = 0.14, phosphorus = 1.11, sodium = 0.03, chloride = 0.00, potassium = 1.37, sulfur = 0.00, metabolizableEnergy = 18.90 * 239, lysine = 4.00, methionine = 1.50, cystine = 2.10, threonine = 3.20, tryptophan = 1.40, arginine = 6.80, isoleucine = 3.20, valine = 4.60, maxStarter = 10.0, maxGrower = 20.0, maxFinisher = 30.0),
            FeedIngredient(name = "Cassava (dehydrated)", nameResourceId = R.string.ingredient_cassava_dehydrated, mainCategory = energy, dryMatter = 87.60, crudeProtein = 2.50, crudeFiber = 3.90, calcium = 0.17, phosphorus = 0.11, sodium = 0.03, chloride = 0.00, potassium = 0.99, sulfur = 0.00, metabolizableEnergy = 16.80 * 239, lysine = 3.90, methionine = 1.60, cystine = 1.60, threonine = 2.90, tryptophan = 0.80, arginine = 5.00, isoleucine = 2.70, valine = 4.50, maxStarter = 20.0, maxGrower = 30.0, maxFinisher = 40.0),
            FeedIngredient(name = "Cassava (fresh)", nameResourceId = R.string.ingredient_cassava_fresh, mainCategory = energy, dryMatter = 37.60, crudeProtein = 2.60, crudeFiber = 3.70, calcium = 0.16, phosphorus = 0.12, sodium = 0.00, chloride = 0.00, potassium = 0.77, sulfur = 0.00, metabolizableEnergy = 17.10 * 239, lysine = 6.20, methionine = 0.60, cystine = 0.00, threonine = 3.80, tryptophan = 0.50, arginine = 7.70, isoleucine = 5.30, valine = 4.50, maxStarter = 20.0, maxGrower = 40.0, maxFinisher = 60.0),
            FeedIngredient(name = "Cassava peels", nameResourceId = R.string.ingredient_cassava_peels, mainCategory = energy, dryMatter = 28.20, crudeProtein = 4.80, crudeFiber = 21.00, calcium = 0.12, phosphorus = 0.21, sodium = 0.00, chloride = 0.00, potassium = 0.64, sulfur = 0.00, metabolizableEnergy = 17.70 * 239, lysine = 2.30, methionine = 0.60, cystine = 0.70, threonine = 2.20, tryptophan = 0.00, arginine = 3.40, isoleucine = 2.30, valine = 3.50, maxStarter = 20.0, maxGrower = 30.0, maxFinisher = 40.0),
            FeedIngredient(name = "Sweet potato (fresh)", nameResourceId = R.string.ingredient_sweet_potato_fresh, mainCategory = energy, dryMatter = 30.00, crudeProtein = 5.50, crudeFiber = 3.80, calcium = 0.12, phosphorus = 0.15, sodium = 0.02, chloride = 0.00, potassium = 1.22, sulfur = 0.00, metabolizableEnergy = 17.40 * 239, lysine = 4.00, methionine = 0.70, cystine = 2.40, threonine = 4.70, tryptophan = 0.00, arginine = 0.00, isoleucine = 0.00, valine = 0.00, maxStarter = 10.0, maxGrower = 20.0, maxFinisher = 30.0),
            FeedIngredient(name = "Sweet potato (dehydrated)", nameResourceId = R.string.ingredient_sweet_potato_dehydrated, mainCategory = energy, dryMatter = 88.00, crudeProtein = 4.60, crudeFiber = 2.80, calcium = 0.17, phosphorus = 0.16, sodium = 0.19, chloride = 0.00, potassium = 0.98, sulfur = 0.00, metabolizableEnergy = 17.40 * 239, lysine = 3.60, methionine = 1.20, cystine = 1.50, threonine = 5.40, tryptophan = 0.00, arginine = 3.60, isoleucine = 4.10, valine = 5.30, maxStarter = 30.0, maxGrower = 50.0, maxFinisher = 60.0),
            FeedIngredient(name = "Sorghum", nameResourceId = R.string.ingredient_sorghum, mainCategory = energy, dryMatter = 87.40, crudeProtein = 10.30, crudeFiber = 2.30, calcium = 0.30, phosphorus = 3.30, sodium = 0.20, chloride = 0.00, potassium = 4.10, sulfur = 0.00, metabolizableEnergy = 18.80 * 239, lysine = 2.20, methionine = 1.70, cystine = 1.90, threonine = 3.30, tryptophan = 1.20, arginine = 3.60, isoleucine = 3.80, valine = 4.90, maxStarter = 20.0, maxGrower = 40.0, maxFinisher = 60.0),
            FeedIngredient(name = "Millet", nameResourceId = R.string.ingredient_millet, mainCategory = energy, dryMatter = 89.00, crudeProtein = 8.50, crudeFiber = 5.70, calcium = 4.90, phosphorus = 3.40, sodium = 0.00, chloride = 0.00, potassium = 5.30, sulfur = 0.00, metabolizableEnergy = 17.70 * 239, lysine = 3.00, methionine = 2.30, cystine = 1.80, threonine = 3.90, tryptophan = 1.20, arginine = 6.40, isoleucine = 4.00, valine = 5.80, maxStarter = 25.0, maxGrower = 40.0, maxFinisher = 50.0),
            FeedIngredient(name = "Broken rice", nameResourceId = R.string.ingredient_broken_rice, mainCategory = energy, dryMatter = 87.50, crudeProtein = 9.00, crudeFiber = 1.60, calcium = 0.00, phosphorus = 0.00, sodium = 0.00, chloride = 0.00, potassium = 0.00, sulfur = 0.00, metabolizableEnergy = 18.10 * 239, lysine = 3.80, methionine = 2.40, cystine = 1.80, threonine = 3.80, tryptophan = 1.70, arginine = 7.90, isoleucine = 4.50, valine = 5.80, maxStarter = 50.0, maxGrower = 50.0, maxFinisher = 50.0),
            FeedIngredient(name = "Cocoyam (sun-dried)", nameResourceId = R.string.ingredient_cocoyam_sun_dried, mainCategory = energy, dryMatter = 89.50, crudeProtein = 8.10, crudeFiber = 6.10, calcium = 4.80, phosphorus = 2.60, sodium = 0.00, chloride = 0.00, potassium = 0.00, sulfur = 0.00, metabolizableEnergy = 17.30 * 239, lysine = 4.30, methionine = 2.30, cystine = 3.90, threonine = 3.90, tryptophan = 0.00, arginine = 7.70, isoleucine = 3.40, valine = 5.20, maxStarter = 10.0, maxGrower = 20.0, maxFinisher = 30.0),
            FeedIngredient(name = "Plantain leaves (fresh)", nameResourceId = R.string.ingredient_plantain_leaves_fresh, mainCategory = energy, dryMatter = 20.70, crudeProtein = 9.50, crudeFiber = 28.60, calcium = 16.70, phosphorus = 1.20, sodium = 0.00, chloride = 0.00, potassium = 25.10, sulfur = 0.00, metabolizableEnergy = 18.10 * 239, lysine = 0.00, methionine = 0.00, cystine = 0.00, threonine = 0.00, tryptophan = 0.00, arginine = 0.00, isoleucine = 0.00, valine = 0.00, maxStarter = 0.0, maxGrower = 10.0, maxFinisher = 20.0),
            FeedIngredient(name = "Breadfruit", nameResourceId = R.string.ingredient_breadfruit, mainCategory = energy, dryMatter = 31.10, crudeProtein = 4.80, crudeFiber = 5.30, calcium = 0.80, phosphorus = 1.30, sodium = 0.00, chloride = 0.00, potassium = 10.70, sulfur = 0.00, metabolizableEnergy = 17.10 * 239, lysine = 0.00, methionine = 0.00, cystine = 0.00, threonine = 0.00, tryptophan = 0.00, arginine = 0.00, isoleucine = 0.00, valine = 0.00, maxStarter = 10.0, maxGrower = 10.0, maxFinisher = 10.0),
            FeedIngredient(name = "Dried brewer grain", nameResourceId = R.string.ingredient_dried_brewer_grain, mainCategory = energy, dryMatter = 91.00, crudeProtein = 23.30, crudeFiber = 15.10, calcium = 2.70, phosphorus = 5.70, sodium = 0.30, chloride = 0.00, potassium = 2.90, sulfur = 0.00, metabolizableEnergy = 19.20 * 239, lysine = 3.10, methionine = 1.50, cystine = 1.80, threonine = 3.20, tryptophan = 1.20, arginine = 4.10, isoleucine = 4.20, valine = 4.80, maxStarter = 0.0, maxGrower = 10.0, maxFinisher = 10.0),
            FeedIngredient(name = "Molasses", nameResourceId = R.string.ingredient_molasses, mainCategory = energy, dryMatter = 73.00, crudeProtein = 5.50, crudeFiber = 0.10, calcium = 9.20, phosphorus = 0.70, sodium = 2.40, chloride = 0.00, potassium = 51.00, sulfur = 0.00, metabolizableEnergy = 14.70 * 239, lysine = 0.10, methionine = 0.30, cystine = 1.20, threonine = 1.20, tryptophan = 0.20, arginine = 0.30, isoleucine = 0.80, valine = 3.80, maxStarter = 10.0, maxGrower = 20.0, maxFinisher = 30.0),

            // REGIONAL ENERGY SOURCES
            FeedIngredient(name = "Sweet potato vines (fresh)", nameResourceId = R.string.ingredient_sweet_potato_vines_fresh, mainCategory = energy, dryMatter = 13.00, crudeProtein = 2.20, crudeFiber = 2.60, calcium = 0.16, phosphorus = 0.04, sodium = 0.01, chloride = 0.00, potassium = 0.45, sulfur = 0.00, metabolizableEnergy = 10.50 * 239, lysine = 4.80, methionine = 1.50, cystine = 1.40, threonine = 4.40, tryptophan = 2.10, arginine = 5.70, isoleucine = 4.10, valine = 5.00, maxStarter = 5.0, maxGrower = 15.0, maxFinisher = 25.0),
            FeedIngredient(name = "Yam peels", nameResourceId = R.string.ingredient_yam_peels, mainCategory = energy, dryMatter = 90.00, crudeProtein = 6.00, crudeFiber = 9.00, calcium = 0.25, phosphorus = 0.20, sodium = 0.03, chloride = 0.00, potassium = 1.10, sulfur = 0.00, metabolizableEnergy = 11.00 * 239, lysine = 4.20, methionine = 1.60, cystine = 1.50, threonine = 3.60, tryptophan = 1.20, arginine = 5.80, isoleucine = 3.60, valine = 4.80, maxStarter = 10.0, maxGrower = 20.0, maxFinisher = 30.0),
            FeedIngredient(name = "Pineapple bran (dried)", nameResourceId = R.string.ingredient_pineapple_bran_dried, mainCategory = energy, dryMatter = 90.00, crudeProtein = 5.00, crudeFiber = 20.00, calcium = 0.35, phosphorus = 0.10, sodium = 0.02, chloride = 0.00, potassium = 0.80, sulfur = 0.00, metabolizableEnergy = 9.20 * 239, lysine = 3.50, methionine = 1.50, cystine = 1.20, threonine = 3.20, tryptophan = 1.00, arginine = 5.00, isoleucine = 3.20, valine = 4.50, maxStarter = 5.0, maxGrower = 15.0, maxFinisher = 20.0),
            FeedIngredient(name = "Soybean hulls", nameResourceId = R.string.ingredient_soybean_hulls, mainCategory = energy, dryMatter = 91.00, crudeProtein = 12.00, crudeFiber = 35.00, calcium = 0.55, phosphorus = 0.18, sodium = 0.02, chloride = 0.00, potassium = 1.20, sulfur = 0.00, metabolizableEnergy = 7.50 * 239, lysine = 5.00, methionine = 1.40, cystine = 1.60, threonine = 3.80, tryptophan = 1.20, arginine = 6.50, isoleucine = 4.00, valine = 4.80, maxStarter = 5.0, maxGrower = 10.0, maxFinisher = 15.0),
            FeedIngredient(name = "Banana peels (dried)", nameResourceId = R.string.ingredient_banana_peels_dried, mainCategory = energy, dryMatter = 90.00, crudeProtein = 6.50, crudeFiber = 14.00, calcium = 0.30, phosphorus = 0.20, sodium = 0.05, chloride = 0.00, potassium = 3.50, sulfur = 0.00, metabolizableEnergy = 9.50 * 239, lysine = 3.80, methionine = 1.20, cystine = 1.40, threonine = 3.50, tryptophan = 1.10, arginine = 5.50, isoleucine = 3.40, valine = 4.60, maxStarter = 5.0, maxGrower = 15.0, maxFinisher = 20.0),
            FeedIngredient(name = "Water hyacinth (fresh)", nameResourceId = R.string.ingredient_water_hyacinth_fresh, mainCategory = energy, dryMatter = 8.00, crudeProtein = 1.20, crudeFiber = 1.60, calcium = 0.12, phosphorus = 0.03, sodium = 0.02, chloride = 0.00, potassium = 0.35, sulfur = 0.00, metabolizableEnergy = 6.50 * 239, lysine = 4.50, methionine = 1.40, cystine = 1.10, threonine = 3.80, tryptophan = 1.00, arginine = 5.00, isoleucine = 3.50, valine = 4.50, maxStarter = 2.0, maxGrower = 5.0, maxFinisher = 10.0),
            FeedIngredient(name = "Banana pseudostem (fresh)", nameResourceId = R.string.ingredient_banana_pseudostem_fresh, mainCategory = energy, dryMatter = 7.00, crudeProtein = 0.40, crudeFiber = 1.50, calcium = 0.08, phosphorus = 0.02, sodium = 0.01, chloride = 0.00, potassium = 0.30, sulfur = 0.00, metabolizableEnergy = 6.00 * 239, lysine = 3.50, methionine = 1.20, cystine = 1.00, threonine = 3.00, tryptophan = 0.80, arginine = 4.50, isoleucine = 3.00, valine = 4.00, maxStarter = 2.0, maxGrower = 10.0, maxFinisher = 15.0),
            FeedIngredient(name = "Coffee pulp (dried)", nameResourceId = R.string.ingredient_coffee_pulp_dried, mainCategory = energy, dryMatter = 90.00, crudeProtein = 10.50, crudeFiber = 21.00, calcium = 0.40, phosphorus = 0.12, sodium = 0.03, chloride = 0.00, potassium = 1.80, sulfur = 0.00, metabolizableEnergy = 7.80 * 239, lysine = 3.80, methionine = 1.40, cystine = 1.20, threonine = 3.50, tryptophan = 1.00, arginine = 5.20, isoleucine = 3.40, valine = 4.60, maxStarter = 0.0, maxGrower = 5.0, maxFinisher = 10.0),
            FeedIngredient(name = "Cacao/Cocoa pod husks (dried)", nameResourceId = R.string.ingredient_cacao_cocoa_pod_husks_dried, mainCategory = energy, dryMatter = 90.00, crudeProtein = 8.00, crudeFiber = 32.00, calcium = 0.60, phosphorus = 0.15, sodium = 0.02, chloride = 0.00, potassium = 2.50, sulfur = 0.00, metabolizableEnergy = 7.00 * 239, lysine = 3.50, methionine = 1.30, cystine = 1.10, threonine = 3.20, tryptophan = 0.90, arginine = 4.80, isoleucine = 3.20, valine = 4.40, maxStarter = 0.0, maxGrower = 5.0, maxFinisher = 10.0),
            FeedIngredient(name = "Mango seed kernel meal", nameResourceId = R.string.ingredient_mango_seed_kernel_meal, mainCategory = energy, dryMatter = 90.00, crudeProtein = 6.00, crudeFiber = 4.00, calcium = 0.20, phosphorus = 0.18, sodium = 0.02, chloride = 0.00, potassium = 0.45, sulfur = 0.00, metabolizableEnergy = 12.50 * 239, lysine = 3.20, methionine = 1.20, cystine = 1.00, threonine = 3.00, tryptophan = 0.80, arginine = 4.60, isoleucine = 3.00, valine = 4.20, maxStarter = 5.0, maxGrower = 10.0, maxFinisher = 15.0),
            FeedIngredient(name = "Palm oil decanter cake", nameResourceId = R.string.ingredient_palm_oil_decanter_cake, mainCategory = energy, dryMatter = 90.00, crudeProtein = 12.00, crudeFiber = 15.00, calcium = 0.35, phosphorus = 0.22, sodium = 0.03, chloride = 0.00, potassium = 0.70, sulfur = 0.00, metabolizableEnergy = 13.50 * 239, lysine = 3.00, methionine = 1.50, cystine = 1.20, threonine = 3.20, tryptophan = 0.80, arginine = 8.50, isoleucine = 3.20, valine = 4.50, maxStarter = 5.0, maxGrower = 10.0, maxFinisher = 15.0),
            FeedIngredient(name = "Sugarcane bagasse (dried)", nameResourceId = R.string.ingredient_sugarcane_bagasse_dried, mainCategory = energy, dryMatter = 92.00, crudeProtein = 1.50, crudeFiber = 45.00, calcium = 0.30, phosphorus = 0.05, sodium = 0.01, chloride = 0.00, potassium = 0.20, sulfur = 0.00, metabolizableEnergy = 4.50 * 239, lysine = 2.50, methionine = 0.80, cystine = 0.80, threonine = 2.00, tryptophan = 0.50, arginine = 3.50, isoleucine = 2.00, valine = 3.00, maxStarter = 0.0, maxGrower = 2.0, maxFinisher = 5.0),
            FeedIngredient(name = "Jackfruit waste (fresh)", nameResourceId = R.string.ingredient_jackfruit_waste_fresh, mainCategory = energy, dryMatter = 20.00, crudeProtein = 1.40, crudeFiber = 2.20, calcium = 0.08, phosphorus = 0.05, sodium = 0.01, chloride = 0.00, potassium = 0.35, sulfur = 0.00, metabolizableEnergy = 11.00 * 239, lysine = 3.80, methionine = 1.40, cystine = 1.10, threonine = 3.20, tryptophan = 0.90, arginine = 5.00, isoleucine = 3.20, valine = 4.40, maxStarter = 5.0, maxGrower = 15.0, maxFinisher = 20.0),
            FeedIngredient(name = "Potato peels (dried)", nameResourceId = R.string.ingredient_potato_peels_dried, mainCategory = energy, dryMatter = 90.00, crudeProtein = 12.00, crudeFiber = 7.00, calcium = 0.15, phosphorus = 0.25, sodium = 0.04, chloride = 0.00, potassium = 1.50, sulfur = 0.00, metabolizableEnergy = 12.00 * 239, lysine = 4.80, methionine = 1.60, cystine = 1.40, threonine = 3.80, tryptophan = 1.20, arginine = 5.50, isoleucine = 3.80, valine = 4.80, maxStarter = 5.0, maxGrower = 10.0, maxFinisher = 15.0),
            FeedIngredient(name = "Cowpea pods/husks", nameResourceId = R.string.ingredient_cowpea_pods_husks, mainCategory = energy, dryMatter = 90.00, crudeProtein = 7.50, crudeFiber = 30.00, calcium = 0.70, phosphorus = 0.12, sodium = 0.03, chloride = 0.00, potassium = 1.20, sulfur = 0.00, metabolizableEnergy = 7.20 * 239, lysine = 4.00, methionine = 1.20, cystine = 1.10, threonine = 3.40, tryptophan = 0.90, arginine = 5.00, isoleucine = 3.20, valine = 4.50, maxStarter = 5.0, maxGrower = 10.0, maxFinisher = 15.0),
            FeedIngredient(name = "Groundnut hulls", nameResourceId = R.string.ingredient_groundnut_hulls, mainCategory = energy, dryMatter = 91.00, crudeProtein = 6.00, crudeFiber = 58.00, calcium = 0.35, phosphorus = 0.08, sodium = 0.02, chloride = 0.00, potassium = 0.60, sulfur = 0.00, metabolizableEnergy = 3.80 * 239, lysine = 3.00, methionine = 1.00, cystine = 0.90, threonine = 2.80, tryptophan = 0.80, arginine = 4.50, isoleucine = 2.80, valine = 4.00, maxStarter = 0.0, maxGrower = 2.0, maxFinisher = 5.0),
            FeedIngredient(name = "Jackfruit seeds (dried)", nameResourceId = R.string.ingredient_jackfruit_seeds_dried, mainCategory = energy, dryMatter = 90.00, crudeProtein = 12.00, crudeFiber = 3.50, calcium = 0.15, phosphorus = 0.25, sodium = 0.02, chloride = 0.00, potassium = 0.90, sulfur = 0.00, metabolizableEnergy = 12.00 * 239, lysine = 4.00, methionine = 1.30, cystine = 1.10, threonine = 3.20, tryptophan = 0.90, arginine = 5.50, isoleucine = 3.20, valine = 4.50, maxStarter = 5.0, maxGrower = 15.0, maxFinisher = 25.0),

            // PROTEIN SOURCES
            FeedIngredient(name = "Soybean meal", nameResourceId = R.string.ingredient_soybean_meal, mainCategory = protein, dryMatter = 93.20, crudeProtein = 49.50, crudeFiber = 7.20, calcium = 0.39, phosphorus = 0.71, sodium = 0.00, chloride = 0.01, potassium = 2.45, sulfur = 0.45, metabolizableEnergy = 21.10 * 239, lysine = 6.20, methionine = 1.40, cystine = 1.50, threonine = 3.90, tryptophan = 1.40, arginine = 7.30, isoleucine = 4.60, valine = 4.80, maxStarter = 20.0, maxGrower = 35.0, maxFinisher = 15.0),
            FeedIngredient(name = "Fish meal", nameResourceId = R.string.ingredient_fish_meal, mainCategory = protein, dryMatter = 92.10, crudeProtein = 70.60, crudeFiber = 1.00, calcium = 4.34, phosphorus = 2.79, sodium = 1.13, chloride = 0.00, potassium = 0.87, sulfur = 0.22, metabolizableEnergy = 21.90 * 239, lysine = 7.50, methionine = 2.70, cystine = 0.80, threonine = 4.10, tryptophan = 1.00, arginine = 6.20, isoleucine = 4.20, valine = 4.90, maxStarter = 10.0, maxGrower = 8.0, maxFinisher = 5.0),
            FeedIngredient(name = "PKC", nameResourceId = R.string.ingredient_pkc, mainCategory = protein, dryMatter = 91.20, crudeProtein = 16.70, crudeFiber = 19.80, calcium = 0.28, phosphorus = 0.60, sodium = 0.02, chloride = 0.00, potassium = 0.65, sulfur = 0.00, metabolizableEnergy = 20.10 * 239, lysine = 2.90, methionine = 1.80, cystine = 1.20, threonine = 3.10, tryptophan = 0.70, arginine = 12.70, isoleucine = 3.50, valine = 5.00, maxStarter = 10.0, maxGrower = 20.0, maxFinisher = 30.0),
            FeedIngredient(name = "Copra", nameResourceId = R.string.ingredient_copra, mainCategory = protein, dryMatter = 50.70, crudeProtein = 22.40, crudeFiber = 14.20, calcium = 0.12, phosphorus = 0.58, sodium = 0.06, chloride = 0.00, potassium = 2.00, sulfur = 0.00, metabolizableEnergy = 32.10 * 239, lysine = 2.60, methionine = 1.30, cystine = 1.20, threonine = 3.00, tryptophan = 1.30, arginine = 10.70, isoleucine = 3.00, valine = 4.70, maxStarter = 7.0, maxGrower = 15.0, maxFinisher = 20.0),
            FeedIngredient(name = "Full fat soybeans", nameResourceId = R.string.ingredient_full_fat_soybeans, mainCategory = protein, dryMatter = 88.60, crudeProtein = 39.30, crudeFiber = 6.50, calcium = 3.40, phosphorus = 5.90, sodium = 0.00, chloride = 0.00, potassium = 19.20, sulfur = 0.00, metabolizableEnergy = 23.80 * 239, lysine = 6.00, methionine = 1.40, cystine = 1.50, threonine = 3.90, tryptophan = 1.30, arginine = 7.20, isoleucine = 4.60, valine = 4.70, maxStarter = 15.0, maxGrower = 20.0, maxFinisher = 20.0),
            FeedIngredient(name = "Groundnut cake", nameResourceId = R.string.ingredient_groundnut_cake, mainCategory = protein, dryMatter = 86.80, crudeProtein = 44.80, crudeFiber = 10.80, calcium = 1.40, phosphorus = 2.60, sodium = 0.00, chloride = 0.00, potassium = 14.60, sulfur = 0.00, metabolizableEnergy = 19.40 * 239, lysine = 6.70, methionine = 1.30, cystine = 1.50, threonine = 3.50, tryptophan = 1.20, arginine = 6.80, isoleucine = 4.10, valine = 4.90, maxStarter = 10.0, maxGrower = 15.0, maxFinisher = 25.0),
            FeedIngredient(name = "Cottonseed cake", nameResourceId = R.string.ingredient_cottonseed_cake, mainCategory = protein, dryMatter = 92.90, crudeProtein = 37.40, crudeFiber = 17.50, calcium = 2.20, phosphorus = 11.90, sodium = 0.20, chloride = 0.00, potassium = 17.00, sulfur = 0.00, metabolizableEnergy = 21.50 * 239, lysine = 4.30, methionine = 1.80, cystine = 1.90, threonine = 3.40, tryptophan = 1.30, arginine = 10.30, isoleucine = 3.20, valine = 4.60, maxStarter = 5.0, maxGrower = 15.0, maxFinisher = 20.0),
            FeedIngredient(name = "Sheanut cake", nameResourceId = R.string.ingredient_sheanut_cake, mainCategory = protein, dryMatter = 92.60, crudeProtein = 14.40, crudeFiber = 10.10, calcium = 2.30, phosphorus = 2.10, sodium = 1.60, chloride = 0.00, potassium = 16.20, sulfur = 0.00, metabolizableEnergy = 20.30 * 239, lysine = 3.90, methionine = 2.10, cystine = 1.70, threonine = 3.90, tryptophan = 1.40, arginine = 8.40, isoleucine = 4.50, valine = 5.60, maxStarter = 2.0, maxGrower = 5.0, maxFinisher = 10.0),
            FeedIngredient(name = "Sesame cake", nameResourceId = R.string.ingredient_sesame_cake, mainCategory = protein, dryMatter = 92.80, crudeProtein = 44.50, crudeFiber = 7.30, calcium = 19.20, phosphorus = 12.60, sodium = 0.10, chloride = 0.00, potassium = 10.40, sulfur = 0.00, metabolizableEnergy = 20.60 * 239, lysine = 2.50, methionine = 2.70, cystine = 2.20, threonine = 3.40, tryptophan = 1.30, arginine = 12.80, isoleucine = 3.70, valine = 5.20, maxStarter = 5.0, maxGrower = 8.0, maxFinisher = 10.0),
            FeedIngredient(name = "Cashew Nut Meal", nameResourceId = R.string.ingredient_cashew_nut_meal, mainCategory = protein, dryMatter = 93.50, crudeProtein = 22.80, crudeFiber = 5.70, calcium = 1.30, phosphorus = 4.50, sodium = 1.30, chloride = 0.00, potassium = 3.50, sulfur = 0.00, metabolizableEnergy = 26.90 * 239, lysine = 4.00, methionine = 1.40, cystine = 1.10, threonine = 3.10, tryptophan = 1.10, arginine = 10.30, isoleucine = 3.50, valine = 4.70, maxStarter = 10.0, maxGrower = 15.0, maxFinisher = 15.0),
            FeedIngredient(name = "Cowpea", nameResourceId = R.string.ingredient_cowpea, mainCategory = protein, dryMatter = 89.90, crudeProtein = 25.20, crudeFiber = 5.60, calcium = 1.10, phosphorus = 4.20, sodium = 0.10, chloride = 0.00, potassium = 0.10, sulfur = 0.00, metabolizableEnergy = 18.70 * 239, lysine = 6.50, methionine = 1.40, cystine = 1.10, threonine = 3.80, tryptophan = 1.10, arginine = 6.70, isoleucine = 4.20, valine = 4.80, maxStarter = 5.0, maxGrower = 15.0, maxFinisher = 25.0),
            FeedIngredient(name = "Pigeon Pea", nameResourceId = R.string.ingredient_pigeon_pea, mainCategory = protein, dryMatter = 89.20, crudeProtein = 22.40, crudeFiber = 8.50, calcium = 3.00, phosphorus = 3.30, sodium = 0.00, chloride = 0.00, potassium = 10.10, sulfur = 0.00, metabolizableEnergy = 18.70 * 239, lysine = 6.50, methionine = 1.00, cystine = 1.30, threonine = 3.40, tryptophan = 0.60, arginine = 5.70, isoleucine = 3.70, valine = 4.50, maxStarter = 10.0, maxGrower = 20.0, maxFinisher = 30.0),
            FeedIngredient(name = "Mucuna/Velvet bean", nameResourceId = R.string.ingredient_mucuna_velvet_bean, mainCategory = protein, dryMatter = 90.80, crudeProtein = 27.70, crudeFiber = 7.80, calcium = 1.90, phosphorus = 4.30, sodium = 0.00, chloride = 0.00, potassium = 8.10, sulfur = 0.00, metabolizableEnergy = 19.20 * 239, lysine = 4.20, methionine = 1.30, cystine = 1.20, threonine = 4.00, tryptophan = 1.00, arginine = 3.80, isoleucine = 2.60, valine = 4.20, maxStarter = 0.0, maxGrower = 0.0, maxFinisher = 0.0),
            FeedIngredient(name = "Locust Bean Meal", nameResourceId = R.string.ingredient_locust_bean_meal, mainCategory = protein, dryMatter = 91.30, crudeProtein = 31.10, crudeFiber = 10.80, calcium = 3.70, phosphorus = 2.50, sodium = 0.05, chloride = 0.00, potassium = 0.00, sulfur = 0.00, metabolizableEnergy = 22.50 * 239, lysine = 6.70, methionine = 0.60, cystine = 1.90, threonine = 3.30, tryptophan = 0.90, arginine = 6.70, isoleucine = 3.60, valine = 4.20, maxStarter = 15.0, maxGrower = 20.0, maxFinisher = 25.0),
            FeedIngredient(name = "Leucaena Leaf", nameResourceId = R.string.ingredient_leucaena_leaf, mainCategory = protein, dryMatter = 29.90, crudeProtein = 23.30, crudeFiber = 19.90, calcium = 10.70, phosphorus = 2.10, sodium = 0.20, chloride = 0.00, potassium = 18.90, sulfur = 0.00, metabolizableEnergy = 19.00 * 239, lysine = 5.50, methionine = 1.30, cystine = 2.30, threonine = 4.10, tryptophan = 1.10, arginine = 5.70, isoleucine = 4.70, valine = 5.20, maxStarter = 2.0, maxGrower = 5.0, maxFinisher = 10.0),
            FeedIngredient(name = "Moringa Leaf Meal", nameResourceId = R.string.ingredient_moringa_leaf_meal, mainCategory = protein, dryMatter = 26.20, crudeProtein = 24.30, crudeFiber = 13.60, calcium = 26.50, phosphorus = 3.10, sodium = 0.20, chloride = 0.00, potassium = 19.20, sulfur = 0.00, metabolizableEnergy = 18.60 * 239, lysine = 4.80, methionine = 1.50, cystine = 1.40, threonine = 4.40, tryptophan = 2.10, arginine = 5.70, isoleucine = 4.10, valine = 5.00, maxStarter = 5.0, maxGrower = 5.0, maxFinisher = 10.0),
            FeedIngredient(name = "Cassava Leaf Meal", nameResourceId = R.string.ingredient_cassava_leaf_meal, mainCategory = protein, dryMatter = 89.60, crudeProtein = 25.50, crudeFiber = 17.10, calcium = 20.50, phosphorus = 3.20, sodium = 0.00, chloride = 0.00, potassium = 11.00, sulfur = 0.00, metabolizableEnergy = 19.70 * 239, lysine = 4.80, methionine = 1.40, cystine = 0.80, threonine = 4.40, tryptophan = 1.00, arginine = 5.60, isoleucine = 4.80, valine = 5.00, maxStarter = 10.0, maxGrower = 15.0, maxFinisher = 20.0),
            FeedIngredient(name = "Blood Meal", nameResourceId = R.string.ingredient_blood_meal, mainCategory = protein, dryMatter = 93.80, crudeProtein = 90.40, crudeFiber = 0.50, calcium = 1.10, phosphorus = 2.20, sodium = 4.50, chloride = 0.00, potassium = 3.80, sulfur = 0.00, metabolizableEnergy = 24.10 * 239, lysine = 8.40, methionine = 1.20, cystine = 1.10, threonine = 4.40, tryptophan = 1.10, arginine = 4.20, isoleucine = 1.10, valine = 8.10, maxStarter = 5.0, maxGrower = 5.0, maxFinisher = 5.0),
            FeedIngredient(name = "Maggot Meal", nameResourceId = R.string.ingredient_maggot_meal, mainCategory = protein, dryMatter = 92.40, crudeProtein = 50.40, crudeFiber = 5.70, calcium = 4.70, phosphorus = 16.00, sodium = 5.20, chloride = 0.00, potassium = 5.70, sulfur = 0.00, metabolizableEnergy = 22.90 * 239, lysine = 6.10, methionine = 2.20, cystine = 0.70, threonine = 3.50, tryptophan = 1.50, arginine = 6.10, isoleucine = 3.20, valine = 4.00, maxStarter = 10.0, maxGrower = 10.0, maxFinisher = 10.0),

            // REGIONAL PROTEIN SOURCES
            FeedIngredient(name = "Mustard oil cake", nameResourceId = R.string.ingredient_mustard_oil_cake, mainCategory = protein, dryMatter = 90.00, crudeProtein = 35.00, crudeFiber = 9.50, calcium = 0.70, phosphorus = 1.10, sodium = 0.08, chloride = 0.00, potassium = 1.20, sulfur = 0.80, metabolizableEnergy = 11.50 * 239, lysine = 4.00, methionine = 2.00, cystine = 1.50, threonine = 3.90, tryptophan = 1.20, arginine = 6.20, isoleucine = 3.90, valine = 5.00, maxStarter = 5.0, maxGrower = 10.0, maxFinisher = 15.0),
            FeedIngredient(name = "Rapeseed meal", nameResourceId = R.string.ingredient_rapeseed_meal, mainCategory = protein, dryMatter = 90.00, crudeProtein = 36.00, crudeFiber = 12.00, calcium = 0.65, phosphorus = 1.00, sodium = 0.08, chloride = 0.00, potassium = 1.20, sulfur = 0.60, metabolizableEnergy = 12.20 * 239, lysine = 5.60, methionine = 2.00, cystine = 2.40, threonine = 4.30, tryptophan = 1.30, arginine = 6.00, isoleucine = 4.00, valine = 5.10, maxStarter = 10.0, maxGrower = 15.0, maxFinisher = 20.0),
            FeedIngredient(name = "Sunflower meal", nameResourceId = R.string.ingredient_sunflower_meal, mainCategory = protein, dryMatter = 91.00, crudeProtein = 33.00, crudeFiber = 20.00, calcium = 0.35, phosphorus = 0.95, sodium = 0.05, chloride = 0.00, potassium = 1.10, sulfur = 0.00, metabolizableEnergy = 9.50 * 239, lysine = 3.40, methionine = 2.20, cystine = 1.80, threonine = 3.70, tryptophan = 1.30, arginine = 8.00, isoleucine = 3.80, valine = 4.90, maxStarter = 5.0, maxGrower = 15.0, maxFinisher = 20.0),
            FeedIngredient(name = "Taro leaves (fresh)", nameResourceId = R.string.ingredient_taro_leaves_fresh, mainCategory = protein, dryMatter = 10.00, crudeProtein = 2.50, crudeFiber = 1.80, calcium = 0.15, phosphorus = 0.04, sodium = 0.01, chloride = 0.00, potassium = 0.40, sulfur = 0.00, metabolizableEnergy = 9.20 * 239, lysine = 4.80, methionine = 1.60, cystine = 1.20, threonine = 4.00, tryptophan = 1.10, arginine = 5.50, isoleucine = 3.80, valine = 4.80, maxStarter = 5.0, maxGrower = 10.0, maxFinisher = 15.0),
            FeedIngredient(name = "Azolla (fresh)", nameResourceId = R.string.ingredient_azolla_fresh, mainCategory = protein, dryMatter = 7.50, crudeProtein = 1.80, crudeFiber = 1.10, calcium = 0.10, phosphorus = 0.03, sodium = 0.01, chloride = 0.00, potassium = 0.25, sulfur = 0.00, metabolizableEnergy = 8.50 * 239, lysine = 5.20, methionine = 1.80, cystine = 1.50, threonine = 4.20, tryptophan = 1.30, arginine = 6.00, isoleucine = 4.20, valine = 5.00, maxStarter = 5.0, maxGrower = 10.0, maxFinisher = 15.0),
            FeedIngredient(name = "Duckweed (fresh)", nameResourceId = R.string.ingredient_duckweed_fresh, mainCategory = protein, dryMatter = 8.00, crudeProtein = 2.40, crudeFiber = 0.80, calcium = 0.12, phosphorus = 0.04, sodium = 0.01, chloride = 0.00, potassium = 0.28, sulfur = 0.00, metabolizableEnergy = 9.80 * 239, lysine = 5.50, methionine = 1.90, cystine = 1.60, threonine = 4.40, tryptophan = 1.40, arginine = 6.20, isoleucine = 4.40, valine = 5.20, maxStarter = 5.0, maxGrower = 10.0, maxFinisher = 15.0),
            FeedIngredient(name = "Bambara groundnut meal", nameResourceId = R.string.ingredient_bambara_groundnut_meal, mainCategory = protein, dryMatter = 90.00, crudeProtein = 18.00, crudeFiber = 5.00, calcium = 0.10, phosphorus = 0.30, sodium = 0.02, chloride = 0.00, potassium = 1.10, sulfur = 0.00, metabolizableEnergy = 12.50 * 239, lysine = 6.20, methionine = 1.40, cystine = 1.20, threonine = 3.50, tryptophan = 1.10, arginine = 6.80, isoleucine = 4.00, valine = 4.80, maxStarter = 10.0, maxGrower = 20.0, maxFinisher = 30.0),
            FeedIngredient(name = "Stylosanthes foliage (dried)", nameResourceId = R.string.ingredient_stylosanthes_foliage_dried, mainCategory = protein, dryMatter = 90.00, crudeProtein = 16.00, crudeFiber = 25.00, calcium = 1.20, phosphorus = 0.22, sodium = 0.05, chloride = 0.00, potassium = 1.10, sulfur = 0.00, metabolizableEnergy = 8.50 * 239, lysine = 4.50, methionine = 1.40, cystine = 1.20, threonine = 4.00, tryptophan = 1.10, arginine = 5.20, isoleucine = 3.80, valine = 4.80, maxStarter = 2.0, maxGrower = 10.0, maxFinisher = 15.0),
            FeedIngredient(name = "Gliricidia leaf meal", nameResourceId = R.string.ingredient_gliricidia_leaf_meal, mainCategory = protein, dryMatter = 90.00, crudeProtein = 22.00, crudeFiber = 18.00, calcium = 1.50, phosphorus = 0.25, sodium = 0.04, chloride = 0.00, potassium = 1.30, sulfur = 0.00, metabolizableEnergy = 8.80 * 239, lysine = 4.80, methionine = 1.50, cystine = 1.30, threonine = 4.20, tryptophan = 1.20, arginine = 5.50, isoleucine = 4.00, valine = 5.00, maxStarter = 2.0, maxGrower = 5.0, maxFinisher = 10.0),
            FeedIngredient(name = "Maize gluten feed", nameResourceId = R.string.ingredient_maize_gluten_feed, mainCategory = protein, dryMatter = 90.00, crudeProtein = 20.00, crudeFiber = 8.50, calcium = 0.15, phosphorus = 0.60, sodium = 0.15, chloride = 0.00, potassium = 0.80, sulfur = 0.00, metabolizableEnergy = 11.50 * 239, lysine = 2.80, methionine = 1.80, cystine = 2.00, threonine = 3.50, tryptophan = 0.80, arginine = 4.80, isoleucine = 3.50, valine = 4.80, maxStarter = 10.0, maxGrower = 15.0, maxFinisher = 20.0),
            FeedIngredient(name = "Linseed meal", nameResourceId = R.string.ingredient_linseed_meal, mainCategory = protein, dryMatter = 90.00, crudeProtein = 34.00, crudeFiber = 9.50, calcium = 0.40, phosphorus = 0.85, sodium = 0.10, chloride = 0.00, potassium = 1.25, sulfur = 0.00, metabolizableEnergy = 11.80 * 239, lysine = 3.80, methionine = 1.80, cystine = 1.80, threonine = 3.70, tryptophan = 1.40, arginine = 7.50, isoleucine = 4.00, valine = 5.00, maxStarter = 5.0, maxGrower = 8.0, maxFinisher = 10.0),
            FeedIngredient(name = "Guar meal", nameResourceId = R.string.ingredient_guar_meal, mainCategory = protein, dryMatter = 91.00, crudeProtein = 40.00, crudeFiber = 7.50, calcium = 0.18, phosphorus = 0.65, sodium = 0.05, chloride = 0.00, potassium = 1.10, sulfur = 0.00, metabolizableEnergy = 11.00 * 239, lysine = 4.20, methionine = 1.40, cystine = 1.30, threonine = 3.50, tryptophan = 1.00, arginine = 11.50, isoleucine = 3.60, valine = 4.60, maxStarter = 2.0, maxGrower = 5.0, maxFinisher = 7.0),
            FeedIngredient(name = "Safflower meal", nameResourceId = R.string.ingredient_safflower_meal, mainCategory = protein, dryMatter = 91.00, crudeProtein = 24.00, crudeFiber = 30.00, calcium = 0.32, phosphorus = 0.70, sodium = 0.05, chloride = 0.00, potassium = 1.15, sulfur = 0.00, metabolizableEnergy = 6.80 * 239, lysine = 3.20, methionine = 1.60, cystine = 1.60, threonine = 3.20, tryptophan = 1.20, arginine = 7.50, isoleucine = 3.80, valine = 4.80, maxStarter = 5.0, maxGrower = 10.0, maxFinisher = 15.0),

            // VITAMINS, MINERALS & SALT
            FeedIngredient(name = "Oyster Shell", nameResourceId = R.string.ingredient_oyster_shell, mainCategory = minerals, dryMatter = 92.60, crudeProtein = 0.00, crudeFiber = 0.00, calcium = 400.00, phosphorus = 3.40, sodium = 5.40, chloride = 0.00, potassium = 0.10, sulfur = 0.00, metabolizableEnergy = 0.0, lysine = 0.0, methionine = 0.0, cystine = 0.0, threonine = 0.0, tryptophan = 0.0, arginine = 0.0, isoleucine = 0.0, valine = 0.0, maxStarter = 5.0, maxGrower = 5.0, maxFinisher = 5.0),
            FeedIngredient(name = "Snail Shell Meal", nameResourceId = R.string.ingredient_snail_shell_meal, mainCategory = minerals, dryMatter = 97.00, crudeProtein = 1.00, crudeFiber = 0.00, calcium = 314.00, phosphorus = 0.00, sodium = 2.20, chloride = 0.00, potassium = 2.00, sulfur = 0.00, metabolizableEnergy = 0.0, lysine = 0.0, methionine = 0.0, cystine = 0.0, threonine = 0.0, tryptophan = 0.0, arginine = 0.0, isoleucine = 0.0, valine = 0.0, maxStarter = 5.0, maxGrower = 5.0, maxFinisher = 5.0),
            FeedIngredient(name = "Bone Meal", nameResourceId = R.string.ingredient_bone_meal, mainCategory = minerals, dryMatter = 95.40, crudeProtein = 0.00, crudeFiber = 0.00, calcium = 303.20, phosphorus = 140.20, sodium = 10.00, chloride = 0.00, potassium = 0.40, sulfur = 0.00, metabolizableEnergy = 1.60 * 239, lysine = 0.0, methionine = 0.0, cystine = 0.0, threonine = 0.0, tryptophan = 0.0, arginine = 0.0, isoleucine = 0.0, valine = 0.0, maxStarter = 5.0, maxGrower = 5.0, maxFinisher = 5.0),
            FeedIngredient(name = "Limestone", nameResourceId = R.string.ingredient_limestone, mainCategory = minerals, dryMatter = 99.50, crudeProtein = 0.00, crudeFiber = 0.00, calcium = 340.00, phosphorus = 0.30, sodium = 0.00, chloride = 0.00, potassium = 0.00, sulfur = 0.00, metabolizableEnergy = 0.0, lysine = 0.0, methionine = 0.0, cystine = 0.0, threonine = 0.0, tryptophan = 0.0, arginine = 0.0, isoleucine = 0.0, valine = 0.0, maxStarter = 5.0, maxGrower = 5.0, maxFinisher = 5.0),
            FeedIngredient(name = "Di-Calcium Phosphate", nameResourceId = R.string.ingredient_di_calcium_phosphate, mainCategory = minerals, dryMatter = 93.00, crudeProtein = 0.00, crudeFiber = 0.00, calcium = 220.00, phosphorus = 180.00, sodium = 0.00, chloride = 0.00, potassium = 0.00, sulfur = 0.00, metabolizableEnergy = 0.0, lysine = 0.0, methionine = 0.0, cystine = 0.0, threonine = 0.0, tryptophan = 0.0, arginine = 0.0, isoleucine = 0.0, valine = 0.0, maxStarter = 2.0, maxGrower = 2.0, maxFinisher = 2.0),
            FeedIngredient(name = "Common salt", nameResourceId = R.string.ingredient_common_salt, mainCategory = minerals, dryMatter = 99.50, crudeProtein = 0.00, crudeFiber = 0.00, calcium = 0.00, phosphorus = 0.00, sodium = 387.00, chloride = 0.00, potassium = 0.00, sulfur = 0.00, metabolizableEnergy = 0.0, lysine = 0.0, methionine = 0.0, cystine = 0.0, threonine = 0.0, tryptophan = 0.0, arginine = 0.0, isoleucine = 0.0, valine = 0.0, maxStarter = 2.0, maxGrower = 2.0, maxFinisher = 2.0),
            FeedIngredient(name = "Wood Ash", nameResourceId = R.string.ingredient_wood_ash, mainCategory = minerals, dryMatter = 93.80, crudeProtein = 0.00, crudeFiber = 0.00, calcium = 150.00, phosphorus = 0.60, sodium = 0.97, chloride = 0.00, potassium = 23.00, sulfur = 1.00, metabolizableEnergy = 0.0, lysine = 0.0, methionine = 0.0, cystine = 0.0, threonine = 0.0, tryptophan = 0.0, arginine = 0.0, isoleucine = 0.0, valine = 0.0, maxStarter = 2.0, maxGrower = 2.0, maxFinisher = 2.0),
            FeedIngredient(name = "Eggshell meal", nameResourceId = R.string.ingredient_eggshell_meal, mainCategory = minerals, dryMatter = 98.90, crudeProtein = 5.60, crudeFiber = 0.30, calcium = 366.50, phosphorus = 1.60, sodium = 0.00, chloride = 0.00, potassium = 0.00, sulfur = 0.00, metabolizableEnergy = 1.10 * 239, lysine = 1.50, methionine = 2.50, cystine = 0.00, threonine = 0.00, tryptophan = 0.00, arginine = 0.50, isoleucine = 0.00, valine = 0.00, maxStarter = 2.0, maxGrower = 2.0, maxFinisher = 2.0),
            FeedIngredient(name = "Vitamin Premix", nameResourceId = R.string.ingredient_vitamin_premix, mainCategory = minerals, dryMatter = 95.00, crudeProtein = 2.00, crudeFiber = 1.00, calcium = 0.00, phosphorus = 0.00, sodium = 0.00, chloride = 0.00, potassium = 1.00, sulfur = 0.00, metabolizableEnergy = 4.00 * 239, lysine = 3.00, methionine = 1.20, cystine = 1.50, threonine = 2.80, tryptophan = 1.00, arginine = 5.50, isoleucine = 0.00, valine = 4.00, maxStarter = 2.0, maxGrower = 2.0, maxFinisher = 2.0),
            FeedIngredient(name = "Mycotoxin Binder", nameResourceId = R.string.ingredient_mycotoxin_binder, mainCategory = minerals, dryMatter = 90.0, crudeProtein = 0.0, crudeFiber = 0.0, calcium = 0.0, phosphorus = 0.0, sodium = 0.0, chloride = 0.0, potassium = 0.0, sulfur = 0.0, metabolizableEnergy = 0.0, lysine = 0.0, methionine = 0.0, cystine = 0.0, threonine = 0.0, tryptophan = 0.0, arginine = 0.0, isoleucine = 0.0, valine = 0.0, maxStarter = 2.0, maxGrower = 2.0, maxFinisher = 2.0),
            FeedIngredient(name = "Lysine", nameResourceId = R.string.ingredient_lysine, mainCategory = minerals, dryMatter = 99.00, crudeProtein = 93.00, crudeFiber = 0.00, calcium = 0.00, phosphorus = 0.00, sodium = 158.00, chloride = 0.00, potassium = 0.00, sulfur = 0.00, metabolizableEnergy = 18.80 * 239, lysine = 78.80, methionine = 0.00, cystine = 0.00, threonine = 0.00, tryptophan = 0.00, arginine = 0.00, isoleucine = 0.00, valine = 0.00, maxStarter = 2.0, maxGrower = 2.0, maxFinisher = 2.0),
            FeedIngredient(name = "Methionine", nameResourceId = R.string.ingredient_methionine, mainCategory = minerals, dryMatter = 99.00, crudeProtein = 58.70, crudeFiber = 0.00, calcium = 0.00, phosphorus = 0.00, sodium = 0.00, chloride = 214.00, potassium = 0.00, sulfur = 0.00, metabolizableEnergy = 21.80 * 239, lysine = 0.00, methionine = 99.00, cystine = 0.00, threonine = 0.00, tryptophan = 0.00, arginine = 0.00, isoleucine = 0.00, valine = 0.00, maxStarter = 2.0, maxGrower = 2.0, maxFinisher = 2.0),
        )

        // Seed and update global collection (wrapped in try-catch to support non-admin users)
        try {
            val globalQuery = globalIngredientsCollection.get().await()
            val existingGlobalMap = globalQuery.documents.associateBy { it.getString("name") }

            for (ingredient in defaultIngredients) {
                val existingDoc = existingGlobalMap[ingredient.name]
                if (existingDoc == null) {
                    val docRef = globalIngredientsCollection.document()
                    globalIngredientsCollection.document(docRef.id).set(ingredient.copy(id = docRef.id)).await()
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
                            globalIngredientsCollection.document(existingDoc.id).set(expectedObj).await()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("FeedRepository", "Failed to update global collection (likely non-admin): ${e.message}")
        }

        // Fetch latest from global database
        var latestGlobalList = globalIngredientsCollection.get().await().toObjects(FeedIngredient::class.java)
        
        // Fallback to codebase defaults if the global collection is empty (e.g., cleared/deleted)
        if (latestGlobalList.isEmpty()) {
            latestGlobalList = defaultIngredients
        }

        val defaultIngredientsMap = defaultIngredients.associateBy { it.name }

        for (ingredient in latestGlobalList) {
            // Use codebase definition if available as the ultimate source of truth for categories & nutrients
            val codeDefault = defaultIngredientsMap[ingredient.name]
            val baseIngredient = codeDefault ?: ingredient

            val query = ingredientsCollection
                .whereEqualTo("name", baseIngredient.name)
                .get()
                .await()
            
            if (query.isEmpty) {
                val docRef = ingredientsCollection.document()
                ingredientsCollection.document(docRef.id).set(baseIngredient.copy(id = docRef.id)).await()
            } else {
                // Update existing to ensure new fields like limits and updated nutritional values are applied
                val doc = query.documents.first()
                val docId = doc.id
                val existing = doc.toObject(FeedIngredient::class.java)
                if (existing != null) {
                    val updatedIngredient = baseIngredient.copy(
                        id = docId,
                        quantity = existing.quantity,
                        costPerKg = existing.costPerKg,
                        visible = existing.visible
                    )
                    if (existing != updatedIngredient) {
                        ingredientsCollection.document(docId).set(updatedIngredient).await()
                    }
                } else {
                    ingredientsCollection.document(docId).set(baseIngredient.copy(id = docId)).await()
                }
            }
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
