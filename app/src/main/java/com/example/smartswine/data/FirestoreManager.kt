package com.example.smartswine.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings

object FirestoreManager {
    private var isConfigured = false

    fun configure() {
        if (isConfigured) return
        isConfigured = true
        
        try {
            val db = FirebaseFirestore.getInstance()
            
            // In some environments (like Play Store Release), Firestore might 
            // initialize settings automatically. We attempt to set ours only if possible.
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder()
                    .setSizeBytes(100 * 1024 * 1024) // 100 MB
                    .build())
                .build()
            
            db.firestoreSettings = settings
        } catch (e: IllegalStateException) {
            // Settings already locked - this is fine as long as the app doesn't crash
            android.util.Log.w("FirestoreManager", "Firestore already started, using default settings")
        } catch (e: Exception) {
            android.util.Log.e("FirestoreManager", "Error configuring Firestore", e)
        }
    }
}
