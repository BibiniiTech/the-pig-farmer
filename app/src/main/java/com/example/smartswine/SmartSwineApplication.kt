package com.example.smartswine

import android.app.Application
import com.example.smartswine.data.FirestoreManager

class SmartSwineApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Firestore settings as early as possible
        // We do this before super.onCreate() or as the first line to be safe
        FirestoreManager.configure()
    }
}
