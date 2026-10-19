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
    private val USER_EMAIL_KEY = stringPreferencesKey("user_email")
    private val USER_BIO_KEY = stringPreferencesKey("user_bio")
    private val ACTIVE_ROOM_ID_KEY = stringPreferencesKey("active_room_id")

    // Saves the JWT token to DataStore
    suspend fun saveToken(context: Context, token: String) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
        }
    }

    // Saves user info alongside the token
    suspend fun saveUserInfo(
        context: Context,
        userId: String,
        userName: String,
        userEmail: String? = null,
        userBio: String? = null
    ) {
        context.dataStore.edit { prefs ->
            prefs[USER_ID_KEY]   = userId
            prefs[USER_NAME_KEY] = userName
            if (!userEmail.isNullOrBlank()) prefs[USER_EMAIL_KEY] = userEmail
            if (!userBio.isNullOrBlank()) prefs[USER_BIO_KEY] = userBio
        }
    }

    suspend fun saveUserBio(context: Context, bio: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_BIO_KEY] = bio
        }
    }

    suspend fun saveUserName(context: Context, name: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_NAME_KEY] = name
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

    fun getUserEmailFlow(context: Context): Flow<String?> {
        return context.dataStore.data.map { prefs -> prefs[USER_EMAIL_KEY] }
    }

    fun getUserBioFlow(context: Context): Flow<String?> {
        return context.dataStore.data.map { prefs -> prefs[USER_BIO_KEY] }
    }

    fun getActiveRoomIdFlow(context: Context): Flow<String?> {
        return context.dataStore.data.map { prefs -> prefs[ACTIVE_ROOM_ID_KEY] }
    }

    suspend fun setActiveRoomId(context: Context, roomId: String) {
        context.dataStore.edit { prefs ->
            prefs[ACTIVE_ROOM_ID_KEY] = roomId
        }
    }

    suspend fun clearActiveRoomId(context: Context) {
        context.dataStore.edit { prefs ->
            prefs.remove(ACTIVE_ROOM_ID_KEY)
        }
    }

    // Clears the token — used on logout
    suspend fun clearToken(context: Context) {
        context.dataStore.edit { prefs ->
            prefs.remove(TOKEN_KEY)
            prefs.remove(USER_ID_KEY)
            prefs.remove(USER_NAME_KEY)
            prefs.remove(USER_EMAIL_KEY)
            prefs.remove(USER_BIO_KEY)
        }
    }

    // Helper: returns "Bearer <token>" which is what the API expects in the header
    fun bearerToken(token: String) = "Bearer $token"
}
