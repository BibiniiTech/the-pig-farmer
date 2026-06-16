package com.example.smartswine.data

import android.content.Context
import android.util.Log
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import com.google.android.play.core.integrity.IntegrityTokenResponse
import kotlinx.coroutines.tasks.await

object IntegrityManager {

    /**
     * Requests an integrity token from the Google Play Integrity API.
     * In a production app, this token should be sent to your server
     * to be verified via the Google Play server-side API.
     */
    suspend fun checkIntegrity(context: Context, cloudProjectNumber: Long): String? {
        val integrityManager = IntegrityManagerFactory.create(context)

        // Nonce should be unique and ideally come from your server
        val nonce = "smartswine_" + System.currentTimeMillis().toString()

        val integrityTokenRequest = IntegrityTokenRequest.builder()
            .setCloudProjectNumber(cloudProjectNumber)
            .setNonce(nonce)
            .build()

        return try {
            val response: IntegrityTokenResponse = integrityManager.requestIntegrityToken(integrityTokenRequest).await()
            Log.d("Integrity", "Token received successfully")
            response.token()
        } catch (e: Exception) {
            Log.e("Integrity", "Error requesting integrity token: ${e.message}")
            null
        }
    }
}
