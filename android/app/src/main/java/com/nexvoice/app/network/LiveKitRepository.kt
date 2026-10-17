package com.nexvoice.app.network

import android.content.Context
import com.nexvoice.app.auth.TokenStore
import kotlinx.coroutines.flow.first

/**
 * LiveKitRepository.kt
 *
 * Phase 4B-2: Fetches a short-lived LiveKit access token from our backend.
 *
 * The backend (POST /api/livekit/token) verifies the NexVoice JWT + room
 * membership and signs a LiveKit token scoped to "nexvoice_<roomId>".
 * The LiveKit API secret never reaches the app.
 */
class LiveKitRepository(private val context: Context) {

    private val api = ApiClient.api

    /** Requests a LiveKit token + URL for the given NexVoice room. */
    suspend fun fetchToken(roomId: String): Result<LiveKitTokenResponse> {
        return try {
            val token = TokenStore.getTokenFlow(context).first()
                ?: return Result.failure(Exception("Not logged in"))

            val response = api.getLiveKitToken(
                token = TokenStore.bearerToken(token),
                body = LiveKitTokenRequest(roomId)
            )
            if (response.isSuccessful) {
                val body = response.body()
                    ?: return Result.failure(Exception("Server returned empty voice token"))
                Result.success(body)
            } else {
                val errorMsg = try {
                    response.errorBody()?.string()
                        ?.substringAfter("\"error\":\"")
                        ?.substringBefore("\"")
                        ?: "Failed to get voice token"
                } catch (e: Exception) {
                    "Failed to get voice token"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Cannot connect to server. Check your internet."))
        }
    }
}
