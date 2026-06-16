package com.example.smartswine.ui.theme

import android.content.Context
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemeViewModel : ViewModel() {
    private val context = com.google.firebase.FirebaseApp.getInstance().applicationContext
    private val prefs = context.getSharedPreferences("smartswine_settings", Context.MODE_PRIVATE)

    private val _useSystemTheme = MutableStateFlow(prefs.getBoolean("use_system_theme", true))
    val useSystemTheme: StateFlow<Boolean> = _useSystemTheme.asStateFlow()

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("is_dark_mode", false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun setUseSystemTheme(enabled: Boolean) {
        _useSystemTheme.value = enabled
        prefs.edit().putBoolean("use_system_theme", enabled).apply()
    }

    fun toggleDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        prefs.edit().putBoolean("is_dark_mode", enabled).apply()
    }
}
