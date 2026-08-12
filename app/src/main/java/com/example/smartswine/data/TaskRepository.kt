package com.example.smartswine.data

import com.example.smartswine.model.TaskItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class TaskRepository(private val db: FirebaseFirestore) {

    fun getUncompletedTasks(userId: String): Flow<List<TaskItem>> = callbackFlow {
        val listener = db.collection("users").document(userId).collection("tasks")
            .whereEqualTo("completed", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val tasks = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(TaskItem::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(tasks)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addTask(userId: String, task: TaskItem) {
        db.collection("users").document(userId).collection("tasks").add(task).await()
    }

    suspend fun updateTask(userId: String, task: TaskItem) {
        db.collection("users").document(userId).collection("tasks").document(task.id).set(task).await()
    }

    suspend fun completeTask(userId: String, taskId: String) {
        db.collection("users").document(userId).collection("tasks").document(taskId)
            .update("completed", true).await()
    }

    suspend fun deleteTask(userId: String, taskId: String) {
        db.collection("users").document(userId).collection("tasks").document(taskId).delete().await()
    }
}
