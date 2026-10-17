package com.nexvoice.app.auth

import android.content.Context
import com.nexvoice.app.network.ApiClient
import com.nexvoice.app.network.LoginRequest
import com.nexvoice.app.network.SignupRequest
import kotlinx.coroutines.flow.first

/**
 * SessionValidation.kt (AuthRepository ke saath rehta hai)
 *
 * App start pe token check karne ka result.
 */
sealed class SessionValidation {
    object Valid : SessionValidation()
    object Invalid : SessionValidation()
    object NetworkError : SessionValidation()
}

/**
 * AuthRepository.kt
 *
 * A Repository is the layer between the UI (screens) and the network/storage.
 * The screen calls the repository, which calls the API and saves the token.
 * This keeps the screen code clean — it doesn't need to know how HTTP works.
 *
 * Pattern:
 *   Screen → ViewModel → Repository → ApiService + TokenStore
 *
 * Each function returns a Result<T>:
 *   Result.success(data)    → everything worked, here's the data
 *   Result.failure(error)   → something went wrong, here's the exception
 */
class AuthRepository(private val context: Context) {

    private val api = ApiClient.api

    /**
     * signup: Creates a new account and saves the token.
     * Returns the user's name on success.
     */
    suspend fun signup(name: String, email: String, password: String): Result<String> {
        return try {
            val response = api.signup(SignupRequest(name, email, password))
            if (response.isSuccessful) {
                val body = response.body()!!
                // Save token and user info so the app stays logged in
                TokenStore.saveToken(context, body.token)
                TokenStore.saveUserInfo(context, body.user.id, body.user.name, body.user.email)
                Result.success(body.user.name)
            } else {
                // Parse the error message from the backend JSON
                val errorMsg = parseError(response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            // Network error (no internet, server down, etc.)
            Result.failure(Exception("Cannot connect to server. Check your internet and try again."))
        }
    }

    /**
     * login: Verifies credentials and saves the token.
     * Returns the user's name on success.
     */
    suspend fun login(email: String, password: String): Result<String> {
        return try {
            val response = api.login(LoginRequest(email, password))
            if (response.isSuccessful) {
                val body = response.body()!!
                TokenStore.saveToken(context, body.token)
                TokenStore.saveUserInfo(context, body.user.id, body.user.name, body.user.email)
                Result.success(body.user.name)
            } else {
                val errorMsg = parseError(response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Cannot connect to server. Check your internet and try again."))
        }
    }

    /**
     * logout: Clears the saved token.
     */
    suspend fun logout() {
        TokenStore.clearToken(context)
    }

    /**
     * validateSession: App start pe saved token ko SERVER se verify karta hai.
     *
     * Pehle sirf ye dekha jaata tha ki "token hai ya nahi" — isliye expire hua token
     * bhi Home tak pahunch jaata tha aur wahan 401 error screen aati thi.
     * Ab GET /api/auth/me call karke pata karte hain ki token sach me valid hai.
     *
     *  Valid        → token theek hai, user ko seedha Home bhejo (auto-login)
     *  Invalid      → token nahi hai ya expire ho gaya (token clear kar diya) → Login
     *  NetworkError → server tak pahunche hi nahi. User ka token galat nahi hai —
     *                 usse logout nahi karte, Retry ka option dete hain.
     */
    suspend fun validateSession(): SessionValidation {
        val token = TokenStore.getTokenFlow(context).first()
            ?: return SessionValidation.Invalid

        return try {
            val response = api.getMe(TokenStore.bearerToken(token))
            when {
                response.isSuccessful -> {
                    response.body()?.user?.let { u ->
                        TokenStore.saveUserInfo(context, u.id, u.name, u.email)
                    }
                    SessionValidation.Valid
                }

                response.code() == 401 || response.code() == 403 -> {
                    // Token ab kaam ka nahi — saaf karke Login bhejo
                    TokenStore.clearToken(context)
                    SessionValidation.Invalid
                }

                else -> SessionValidation.NetworkError
            }
        } catch (e: Exception) {
            // Server band hai / internet nahi / timeout
            SessionValidation.NetworkError
        }
    }

    // Helper: tries to extract the "error" field from JSON like { "error": "..." }
    private fun parseError(errorJson: String?): String {
        return try {
            // Simple manual parse — no extra library needed
            errorJson
                ?.substringAfter("\"error\":\"")
                ?.substringBefore("\"")
                ?: "Something went wrong"
        } catch (e: Exception) {
            "Something went wrong"
        }
    }
}
