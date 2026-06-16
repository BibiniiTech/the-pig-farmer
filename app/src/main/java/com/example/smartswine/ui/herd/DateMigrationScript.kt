package com.example.smartswine.ui.herd

import com.example.smartswine.model.HealthRecord
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@Suppress("unused")
object DateMigrationScript {
    suspend fun migrateDates(userId: String) {
        val db = FirebaseFirestore.getInstance()
        val pigs = db.collection("users").document(userId).collection("pigs").get().await()
        
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)
        val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        for (pigDoc in pigs) {
            val records = pigDoc.reference.collection("health_records").get().await()
            for (recordDoc in records) {
                val record = recordDoc.toObject(HealthRecord::class.java)
                val originalDate = record.date
                
                // Try to parse the original format "dd/MM/yyyy"
                try {
                    val parsedDate = dateFormat.parse(originalDate)
                    if (parsedDate != null) {
                        val newDate = isoFormat.format(parsedDate)
                        
                        // Update if the date is not already in ISO format
                        if (originalDate != newDate) {
                            recordDoc.reference.update("date", newDate).await()
                        }
                    }
                } catch (_: Exception) {
                    // Already in another format or invalid, ignore
                }
            }
        }
    }
}
