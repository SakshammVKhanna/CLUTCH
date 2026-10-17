package com.nexvoice.app.network

import retrofit2.Response
import retrofit2.http.*

/**
 * ApiService.kt
 *
 * This interface defines all API calls the app can make.
 * Retrofit reads these annotations and automatically creates the
 * actual HTTP implementation for us — we don't write the HTTP code ourselves.
 *
 * @POST("/api/auth/login") = send HTTP POST to /api/auth/login
 * @Body = put this Kotlin object as JSON in the request body
 * @GET = send HTTP GET request
 * @Header = add this header to the request
 * @Path("id") = replace {id} in the URL with this value
 *
 * Response<T> wraps the result so we can check:
 *   response.isSuccessful  → true if HTTP status is 200-299
 *   response.body()        → the actual data (T)
 *   response.errorBody()   → the error JSON if not successful
 */
interface ApiService {

    // ── Auth ──────────────────────────────────────────────────────────────────

    @POST("api/auth/signup")
    suspend fun signup(@Body body: SignupRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<AuthResponse>

    @GET("api/auth/me")
    suspend fun getMe(@Header("Authorization") token: String): Response<UserResponse>

    // ── Rooms ─────────────────────────────────────────────────────────────────

    @GET("api/rooms")
    suspend fun getRooms(@Header("Authorization") token: String): Response<RoomsResponse>

    @GET("api/rooms/{id}")
    suspend fun getRoomById(@Path("id") id: String): Response<RoomResponse>

    @POST("api/rooms")
    suspend fun createRoom(
        @Header("Authorization") token: String,
        @Body body: CreateRoomRequest
    ): Response<RoomResponse>

    @POST("api/rooms/{id}/join")
    suspend fun joinRoom(
        @Header("Authorization") token: String,
        @Path("id") roomId: String
    ): Response<RoomResponse>

    @POST("api/rooms/{id}/leave")
    suspend fun leaveRoom(
        @Header("Authorization") token: String,
        @Path("id") roomId: String
    ): Response<RoomResponse>

    @POST("api/rooms/{id}/invite")
    suspend fun inviteUser(
        @Header("Authorization") token: String,
        @Path("id") roomId: String,
        @Body body: InviteUserRequest
    ): Response<InviteUserResponse>

    // ── Phase 4B-2: LiveKit voice token ──────────────────────────────────────
    // Backend verifies NexVoice JWT + room membership, returns LiveKit token + URL.
    // The LiveKit API secret never reaches the app — only this short-lived token.

    @POST("api/livekit/token")
    suspend fun getLiveKitToken(
        @Header("Authorization") token: String,
        @Body body: LiveKitTokenRequest
    ): Response<LiveKitTokenResponse>
}

// Wrapper for the /me response
data class UserResponse(val user: UserDto)
