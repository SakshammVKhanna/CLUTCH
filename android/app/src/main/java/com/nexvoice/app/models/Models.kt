package com.nexvoice.app.models

/**
 * Models.kt
 *
 * Simple data classes that represent objects in our app.
 * These are like "blueprints" — they describe the shape of data.
 *
 * In Phase 4+, these will be filled with real data from the backend.
 * For now, we use fake/sample data to build the UI.
 */

/**
 * A voice room that users can join.
 */
data class Room(
    val id: String,           // Unique identifier (e.g., "room_abc123")
    val name: String,         // Display name (e.g., "CS2 Ranked Squad")
    val gameName: String,     // Game being played (e.g., "Counter-Strike 2")
    val hostName: String,     // Username of the person who created the room
    val memberCount: Int      // How many people are currently inside (no cap)
)

/**
 * A user in the app.
 */
data class User(
    val id: String,
    val username: String,
    val email: String,
    val avatarUrl: String? = null  // Will be used in Phase 3+
)

/**
 * A member inside a voice room — includes their mic state.
 */
data class RoomMember(
    val id: String,
    val username: String,
    val isMuted: Boolean = false,   // true = mic is off
    val isSpeaking: Boolean = false // true = actively talking (for visual indicator)
)
