package com.example.smartswine.ui.settings

import com.example.smartswine.data.FirestoreManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale

class SettingsViewModel : ViewModel() {
    private val db: FirebaseFirestore by lazy {
        FirestoreManager.configure()
        FirebaseFirestore.getInstance()
    }
    private val auth = FirebaseAuth.getInstance()
    private var userListener: ListenerRegistration? = null

    // Weaning & Farrowing
    val weaningDays = MutableStateFlow("56")
    val farrowingDays = MutableStateFlow("114")
    
    // Iron Injection
    val ironDay1 = MutableStateFlow("3")
    val ironDay2 = MutableStateFlow("10")

    // Porker Status
    val porkerUseAge = MutableStateFlow(value = true)
    val porkerStarterAge = MutableStateFlow("16")
    val porkerGrowerAge = MutableStateFlow("24")
    val porkerStarterWeight = MutableStateFlow("25")
    val porkerGrowerWeight = MutableStateFlow("60")

    // Breeder Status
    val breederUseAge = MutableStateFlow(value = true)
    val breederPigletAge = MutableStateFlow("8")
    val breederWeanerAge = MutableStateFlow("16")
    val breederGrowerAge = MutableStateFlow("24")
    val breederPigletWeight = MutableStateFlow("10")
    val breederWeanerWeight = MutableStateFlow("25")
    val breederGrowerWeight = MutableStateFlow("60")

    // Terminology Rules
    val autoClassifyBarrows = MutableStateFlow(value = true)
    val autoClassifySows = MutableStateFlow(value = true)
    val giltAgeThresholdWeeks = MutableStateFlow("26") // 6 months

    // Notifications
    val notificationsEnabled = MutableStateFlow(value = true)

    // Currency
    val selectedCurrency = MutableStateFlow("USD")
    val currencySymbol = MutableStateFlow("$")

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<String?>(null)
    val lastSyncTime = _lastSyncTime.asStateFlow()

    companion object {
        private var instance: SettingsViewModel? = null
        fun getInstance(): SettingsViewModel {
            if (instance == null) {
                instance = SettingsViewModel()
            }
            return instance!!
        }
    }

    private var lastCountry: String? = null

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid
            userListener?.remove()
            if (uid != null) {
                userListener = db.collection("users").document(uid).addSnapshotListener { document, _ ->
                    if (auth.currentUser == null) {
                        userListener?.remove()
                        userListener = null
                        return@addSnapshotListener
                    }
                    if ((document != null) && document.exists()) {
                        val country = document.getString("country") ?: ""
                        val countryCode = document.getString("countryCode") ?: ""
                        if (country != lastCountry) {
                            lastCountry = country
                            updateCurrencyFromCountry(country, countryCode)
                        }

                        // Load settings from cloud
                        @Suppress("UNCHECKED_CAST")
                        val settings = document["settings"] as? Map<String, Any> ?: emptyMap()
                        loadSettingsFromMap(settings)

                        // Also sync to SharedPreferences
                        val context = com.google.firebase.FirebaseApp.getInstance().applicationContext
                        val prefs = context.getSharedPreferences("smartswine_settings", android.content.Context.MODE_PRIVATE)
                        (settings["notificationsEnabled"] as? Boolean)?.let {
                            prefs.edit().putBoolean("notifications_enabled", it).apply()
                        }
                        
                        val formatter = SimpleDateFormat("HH:mm, MMM dd", Locale.getDefault())
                        _lastSyncTime.value = formatter.format(Date())
                    }
                }
            } else {
                lastCountry = null
            }
        }
    }

    private fun loadSettingsFromMap(map: Map<String, Any>) {
        (map["weaningDays"] as? String)?.let { weaningDays.value = it }
        (map["farrowingDays"] as? String)?.let { farrowingDays.value = it }
        (map["ironDay1"] as? String)?.let { ironDay1.value = it }
        (map["ironDay2"] as? String)?.let { ironDay2.value = it }
        (map["porkerUseAge"] as? Boolean)?.let { porkerUseAge.value = it }
        (map["porkerStarterAge"] as? String)?.let { porkerStarterAge.value = it }
        (map["porkerGrowerAge"] as? String)?.let { porkerGrowerAge.value = it }
        (map["porkerStarterWeight"] as? String)?.let { porkerStarterWeight.value = it }
        (map["porkerGrowerWeight"] as? String)?.let { porkerGrowerWeight.value = it }
        (map["breederUseAge"] as? Boolean)?.let { breederUseAge.value = it }
        (map["breederPigletAge"] as? String)?.let { breederPigletAge.value = it }
        (map["breederWeanerAge"] as? String)?.let { breederWeanerAge.value = it }
        (map["breederGrowerAge"] as? String)?.let { breederGrowerAge.value = it }
        (map["breederPigletWeight"] as? String)?.let { breederPigletWeight.value = it }
        (map["breederWeanerWeight"] as? String)?.let { breederWeanerWeight.value = it }
        (map["breederGrowerWeight"] as? String)?.let { breederGrowerWeight.value = it }
        (map["autoClassifyBarrows"] as? Boolean)?.let { autoClassifyBarrows.value = it }
        (map["autoClassifySows"] as? Boolean)?.let { autoClassifySows.value = it }
        (map["notificationsEnabled"] as? Boolean)?.let { notificationsEnabled.value = it }
        (map["giltAgeThresholdWeeks"] as? String)?.let { giltAgeThresholdWeeks.value = it }
        (map["selectedCurrency"] as? String)?.let { selectedCurrency.value = it }
        (map["currencySymbol"] as? String)?.let { currencySymbol.value = it }
    }

    fun saveSettings() {
        val userId = auth.currentUser?.uid ?: return
        
        // Save to SharedPreferences for immediate local access (e.g., BootReceiver)
        val context = com.google.firebase.FirebaseApp.getInstance().applicationContext
        val prefs = context.getSharedPreferences("smartswine_settings", android.content.Context.MODE_PRIVATE)
        prefs.edit().putBoolean("notifications_enabled", notificationsEnabled.value).apply()

        val settingsMap = mapOf(
            "weaningDays" to weaningDays.value,
            "farrowingDays" to farrowingDays.value,
            "ironDay1" to ironDay1.value,
            "ironDay2" to ironDay2.value,
            "porkerUseAge" to porkerUseAge.value,
            "porkerStarterAge" to porkerStarterAge.value,
            "porkerGrowerAge" to porkerGrowerAge.value,
            "porkerStarterWeight" to porkerStarterWeight.value,
            "porkerGrowerWeight" to porkerGrowerWeight.value,
            "breederUseAge" to breederUseAge.value,
            "breederPigletAge" to breederPigletAge.value,
            "breederWeanerAge" to breederWeanerAge.value,
            "breederGrowerAge" to breederGrowerAge.value,
            "breederPigletWeight" to breederPigletWeight.value,
            "breederWeanerWeight" to breederWeanerWeight.value,
            "breederGrowerWeight" to breederGrowerWeight.value,
            "autoClassifyBarrows" to autoClassifyBarrows.value,
            "autoClassifySows" to autoClassifySows.value,
            "notificationsEnabled" to notificationsEnabled.value,
            "giltAgeThresholdWeeks" to giltAgeThresholdWeeks.value,
            "selectedCurrency" to selectedCurrency.value,
            "currencySymbol" to currencySymbol.value,
        )

        viewModelScope.launch {
            _isSyncing.value = true
            try {
                db.collection("users").document(userId)
                    .update("settings", settingsMap).await()
            } catch (_: Exception) {
                // If document doesn't have "settings" field, set it
                db.collection("users").document(userId)
                    .set(mapOf("settings" to settingsMap), com.google.firebase.firestore.SetOptions.merge()).await()
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private fun updateCurrencyFromCountry(countryName: String, countryCode: String) {
        val locale = if (countryCode.isNotEmpty()) {
            try {
                Locale.Builder().setRegion(countryCode).build()
            } catch (e: Exception) {
                Locale.getAvailableLocales().find { it.displayCountry.equals(countryName, ignoreCase = true) }
            }
        } else {
            Locale.getAvailableLocales().find { it.displayCountry.equals(countryName, ignoreCase = true) }
        }

        if (locale != null) {
            try {
                val currency = Currency.getInstance(locale)
                selectedCurrency.value = currency.currencyCode
                currencySymbol.value = currency.getSymbol(Locale.US)
            } catch (_: Exception) {
                // Fallback to USD if currency not found for locale
            }
        }
    }

    fun updateCurrency(currencyCode: String) {
        try {
            val currency = Currency.getInstance(currencyCode)
            selectedCurrency.value = currencyCode
            currencySymbol.value = currency.getSymbol(Locale.US)
        } catch (_: Exception) {
            // Invalid currency code
        }
    }

    fun clearCollection(collectionName: String, onComplete: () -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val collectionRef = db.collection("users").document(userId).collection(collectionName)
                val snapshot = collectionRef.get().await()
                
                val batch = db.batch()
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }
                
                // If clearing pigs, also clear archived_pigs
                if (collectionName == "pigs") {
                    val archiveSnapshot = db.collection("users").document(userId).collection("archived_pigs").get().await()
                    for (doc in archiveSnapshot.documents) {
                        batch.delete(doc.reference)
                    }
                }
                
                batch.commit().await()
                onComplete()
            } catch (_: Exception) {
                // Handle error
            }
        }
    }

    fun factoryReset(onComplete: () -> Unit) {
        val collections = listOf("pigs", "archived_pigs", "financials", "ingredients", "requirements", "staff", "salaries")
        val userId = auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            try {
                val userRef = db.collection("users").document(userId)
                for (coll in collections) {
                    val snapshot = userRef.collection(coll).get().await()
                    val batch = db.batch()
                    for (doc in snapshot.documents) {
                        batch.delete(doc.reference)
                    }
                    batch.commit().await()
                }
                onComplete()
            } catch (_: Exception) {
                // Handle error
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        userListener?.remove()
    }
}
