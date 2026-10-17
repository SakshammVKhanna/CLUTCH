package com.nexvoice.app.network

import android.content.Context
import com.nexvoice.app.auth.TokenStore
import com.nexvoice.app.models.Room
import kotlinx.coroutines.flow.first

/**
 * RoomRepository.kt
 *
 * Handles all room-related API calls.
 * Converts RoomDto (network format) → Room (app model) before returning data.
 *
 * Why convert? Because the backend returns "_id" but our app model uses "id".
 * Also, the backend has a list of full member objects, but our Room model
 * just stores the count and host name. We map here.
 */
class RoomRepository(private val context: Context) {

    private val api = ApiClient.api

    /** Fetches MY rooms (invite-only) from the backend. */
    suspend fun getRooms(): Result<List<Room>> {
        return try {
            val token = TokenStore.getTokenFlow(context).first()
                ?: return Result.failure(Exception("Not logged in"))
            val response = api.getRooms(TokenStore.bearerToken(token))
            if (response.isSuccessful) {
                val rooms = response.body()?.rooms?.map { it.toRoom() } ?: emptyList()
                Result.success(rooms)
            } else {
                val errorMsg = parseError(response.errorBody()?.string())
                val message = if (errorMsg.isNotBlank() && errorMsg != "Something went wrong") {
                    errorMsg
                } else {
                    "Failed to load rooms (HTTP ${response.code()})"
                }
                Result.failure(Exception(message))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Cannot connect to server. Check your internet."))
        }
    }

    /** Creates a new room. Token is read from DataStore automatically. */
    suspend fun createRoom(
        name: String,
        gameName: String
    ): Result<Room> {
        return try {
            val token = TokenStore.getTokenFlow(context).first()
                ?: return Result.failure(Exception("Not logged in"))

            val response = api.createRoom(
                token = TokenStore.bearerToken(token),
                body  = CreateRoomRequest(name, gameName)
            )
            if (response.isSuccessful) {
                val room = response.body()?.room?.toRoom()
                    ?: return Result.failure(Exception("Server returned empty room"))
                Result.success(room)
            } else {
                val errorMsg = parseError(response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Cannot connect to server. Check your internet."))
        }
    }

    /** Joins a room. */
    suspend fun joinRoom(roomId: String): Result<Room> {
        return try {
            val token = TokenStore.getTokenFlow(context).first()
                ?: return Result.failure(Exception("Not logged in"))

            val response = api.joinRoom(
                token  = TokenStore.bearerToken(token),
                roomId = roomId
            )
            if (response.isSuccessful) {
                val room = response.body()?.room?.toRoom()
                    ?: return Result.failure(Exception("Server returned empty room"))
                Result.success(room)
            } else {
                val errorMsg = parseError(response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Cannot connect to server. Check your internet."))
        }
    }

    /** Fetches single room details by ID. */
    suspend fun getRoomById(roomId: String): Result<Room> {
        return try {
            val response = api.getRoomById(roomId)
            if (response.isSuccessful) {
                val room = response.body()?.room?.toRoom()
                    ?: return Result.failure(Exception("Room not found"))
                Result.success(room)
            } else {
                val errorMsg = parseError(response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Cannot connect to server. Check your internet."))
        }
    }

    /** Leaves a room. */
    suspend fun leaveRoom(roomId: String): Result<Unit> {
        return try {
            val token = TokenStore.getTokenFlow(context).first()
                ?: return Result.failure(Exception("Not logged in"))

            val response = api.leaveRoom(
                token  = TokenStore.bearerToken(token),
                roomId = roomId
            )
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMsg = parseError(response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Cannot connect to server. Check your internet."))
        }
    }

    /** Invites/adds a user directly to the room by username, email, or user ID. */
    suspend fun inviteUser(roomId: String, username: String): Result<String> {
        return try {
            val token = TokenStore.getTokenFlow(context).first()
                ?: return Result.failure(Exception("Not logged in"))

            val response = api.inviteUser(
                token = TokenStore.bearerToken(token),
                roomId = roomId,
                body = InviteUserRequest(username.trim())
            )
            if (response.isSuccessful) {
                val msg = response.body()?.message ?: "User added successfully!"
                Result.success(msg)
            } else {
                val errorMsg = parseError(response.errorBody()?.string())
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Cannot connect to server. Check your internet."))
        }
    }

    // Convert network DTO → app model
    private fun RoomDto.toRoom() = Room(
        id          = _id,
        name        = name,
        gameName    = gameName,
        hostName    = owner?.name ?: "Unknown",
        memberCount = members.size
    )

    private fun parseError(errorJson: String?): String {
        return try {
            errorJson
                ?.substringAfter("\"error\":\"")
                ?.substringBefore("\"")
                ?: "Something went wrong"
        } catch (e: Exception) {
            "Something went wrong"
        }
    }
}
