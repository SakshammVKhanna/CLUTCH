package com.nexvoice.app.network

/**
 * ApiModels.kt
 *
 * These are "Data Transfer Objects" (DTOs) — data classes that exactly match
 * the JSON that our backend sends and receives.
 *
 * Gson (the JSON library) automatically converts:
 *   JSON string → Kotlin data class  (when we receive a response)
 *   Kotlin data class → JSON string  (when we send a request)
 *
 * The field names MUST match the JSON keys from the backend.
 * Example: backend sends { "token": "...", "user": { ... } }
 *          → maps to AuthResponse(token = "...", user = UserDto(...))
 */

// ── Request bodies (what we SEND to the backend) ─────────────────────────────

data class SignupRequest(
    val name: String,
    val email: String,
    val password: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class CreateRoomRequest(
    val name: String,
    val gameName: String
)

// ── Response bodies (what we RECEIVE from the backend) ───────────────────────

data class UserDto(
    val id: String,
    val name: String,
    val email: String,
    val profileImage: String? = null,
    val createdAt: String? = null
)

data class AuthResponse(
    val message: String,
    val token: String,
    val user: UserDto
)

data class RoomOwnerDto(
    val name: String,
    val email: String,
    val profileImage: String? = null
)

data class RoomDto(
    val _id: String,          // MongoDB uses "_id" (with underscore)
    val name: String,
    val gameName: String,
    val members: List<UserDto> = emptyList(),
    val owner: RoomOwnerDto? = null,
    val createdAt: String? = null
)

data class RoomsResponse(
    val count: Int,
    val rooms: List<RoomDto>
)

data class RoomResponse(
    val message: String? = null,
    val room: RoomDto
)

// Generic error response from the backend
data class ErrorResponse(
    val error: String
)

// ── Phase 4B-2: LiveKit voice token (what we SEND / RECEIVE) ────────────────

data class LiveKitTokenRequest(
    val roomId: String
)

data class LiveKitTokenResponse(
    val token: String,
    val url: String,
    val roomName: String,
    val identity: String
)

// ── Invite / Add user directly by username, email, or id ────────────────────
data class InviteUserRequest(
    val username: String
)

data class InviteUserResponse(
    val message: String,
    val user: UserDto? = null
)
