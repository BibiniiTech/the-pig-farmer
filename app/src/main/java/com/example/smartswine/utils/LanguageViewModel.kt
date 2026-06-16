package com.example.smartswine.utils

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class LanguageViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore by lazy {
        com.example.smartswine.data.FirestoreManager.configure()
        FirebaseFirestore.getInstance()
    }
    private val sharedPrefs = application.getSharedPreferences("smartswine_settings", Context.MODE_PRIVATE)

    private val _currentLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val currentLanguage = _currentLanguage.asStateFlow()

    private var hasUserSelectedLanguage = false

    private val authListener = FirebaseAuth.AuthStateListener {
        if (auth.currentUser == null) {
            hasUserSelectedLanguage = false
        }
        loadUserLanguage()
    }

    init {
        // Synchronously load from SharedPreferences to prevent cold-start flicker
        val cachedCode = sharedPrefs.getString("app_language", null)
        if (cachedCode != null) {
            AppLanguage.entries.find { it.code == cachedCode }?.let { lang ->
                _currentLanguage.value = lang
            }
        }
        auth.addAuthStateListener(authListener)
    }

    private fun loadUserLanguage() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            if (hasUserSelectedLanguage) {
                // User explicitly selected a language on the Auth screen, persist it to Firestore and local SharedPreferences
                db.collection("users").document(userId)
                    .set(mapOf("appLanguage" to _currentLanguage.value.code), SetOptions.merge())
                saveLanguageToPrefs(_currentLanguage.value)
                return
            }
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    val langCode = document.getString("appLanguage")
                    if (langCode != null) {
                        AppLanguage.entries.find { it.code == langCode }?.let { lang ->
                            if (_currentLanguage.value != lang) {
                                _currentLanguage.value = lang
                                saveLanguageToPrefs(lang)
                            }
                        }
                    }
                }
        }
    }

    fun setLanguage(language: AppLanguage) {
        _currentLanguage.value = language
        hasUserSelectedLanguage = true
        saveLanguageToPrefs(language)
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId).update("appLanguage", language.code)
        }
    }

    private fun saveLanguageToPrefs(language: AppLanguage) {
        sharedPrefs.edit().putString("app_language", language.code).apply()
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authListener)
    }
}
