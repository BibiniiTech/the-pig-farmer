package com.example.smartswine.data

import com.example.smartswine.model.FinancialRecord
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FinancialRepository(private val db: FirebaseFirestore) {

    fun getFinancialRecords(userId: String): Flow<List<FinancialRecord>> = callbackFlow {
        val listener = db.collection("users").document(userId)
            .collection("financials")
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.w("FinancialRepository", "Error listening to financials: ${error.message}")
                    close()
                    return@addSnapshotListener
                }
                val records = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(FinancialRecord::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(records)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addFinancialRecord(userId: String, record: FinancialRecord) {
        val docRef = db.collection("users").document(userId).collection("financials").document()
        docRef.set(record.copy(id = docRef.id)).await()
    }

    suspend fun deleteFinancialRecord(userId: String, recordId: String) {
        db.collection("users").document(userId).collection("financials").document(recordId).delete().await()
    }
}
