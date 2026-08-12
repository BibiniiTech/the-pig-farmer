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
                    close(error)
                    return@addSnapshotListener
                }
                val records = snapshot?.toObjects(FinancialRecord::class.java) ?: emptyList()
                trySend(records)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addFinancialRecord(userId: String, record: FinancialRecord) {
        db.collection("users").document(userId).collection("financials").add(record).await()
    }

    suspend fun deleteFinancialRecord(userId: String, recordId: String) {
        db.collection("users").document(userId).collection("financials").document(recordId).delete().await()
    }
}
