package com.nexvoice.app.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * TokenStore.kt
 *
 * Saves and reads the JWT token using DataStore (a modern, safe storage system).
 *
 * Why DataStore instead of SharedPreferences?
 * - DataStore is asynchronous (won't block the main thread)
 * - Less likely to cause crashes
 * - Google's recommended approach
 *
 * The token is stored under the key "auth_token".
 * When the user logs out, we delete it.
 *
 * Usage:
 *   TokenStore.saveToken(context, "eyJhbGc...")   ← after login
 *   TokenStore.getToken(context)                   ← Flow<String?>
 *   TokenStore.clearToken(context)                 ← after logout
 */

// This creates the DataStore file "nexvoice_prefs.preferences_pb" in app storage
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "nexvoice_prefs"
)

object TokenStore {

    // The key used to store the token in DataStore
    private val TOKEN_KEY   = stringPreferencesKey("auth_token")
    private val USER_ID_KEY = stringPreferencesKey("user_id")
    private val USER_NAME_KEY = stringPreferencesKey("user_name")

    // Saves the JWT token to DataStore
    suspend fun saveToken(context: Context, token: String) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
        }
    }

    // Saves user info alongside the token
    suspend fun saveUserInfo(context: Context, userId: String, userName: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_ID_KEY]   = userId
            prefs[USER_NAME_KEY] = userName
        }
    }

    // Returns a Flow of the token — Flow<String?> means it can be null (not logged in)
    // A Flow automatically updates your UI whenever the value changes
    fun getTokenFlow(context: Context): Flow<String?> {
        return context.dataStore.data.map { prefs -> prefs[TOKEN_KEY] }
    }

    fun getUserNameFlow(context: Context): Flow<String?> {
        return context.dataStore.data.map { prefs -> prefs[USER_NAME_KEY] }
    }

    // Clears the token — used on logout
    suspend fun clearToken(context: Context) {
        context.dataStore.edit { prefs ->
            prefs.remove(TOKEN_KEY)
            prefs.remove(USER_ID_KEY)
            prefs.remove(USER_NAME_KEY)
        }
    }

    // Helper: returns "Bearer <token>" which is what the API expects in the header
    fun bearerToken(token: String) = "Bearer $token"
}
