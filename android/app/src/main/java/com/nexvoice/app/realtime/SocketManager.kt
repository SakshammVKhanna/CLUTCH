package com.nexvoice.app.realtime

import android.util.Log
import com.nexvoice.app.models.RoomMember
import com.nexvoice.app.network.NetworkConfig
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONObject
import java.net.URI
import java.util.Collections

/**
 * SocketManager.kt
 *
 * Singleton manager for real-time WebSocket communication via Socket.IO.
 *
 * Responsibilities:
 *   1. Authenticates connection with the existing JWT token
 *   2. Emits room events (join_room, leave_room, mute_changed)
 *   3. Listens to server events (room_state, user_joined, user_left, user_mute_changed)
 *   4. Exposes reactive StateFlows for Compose UI
 *   5. Safely cleans up listeners to prevent memory leaks
 */
object SocketManager {

    private const val TAG = "NexVoice_Socket"

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        ERROR
    }

    private var socket: Socket? = null
    private var currentRoomId: String? = null

    // ── Reactive State for UI ─────────────────────────────────────────────────
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _roomMembers = MutableStateFlow<List<RoomMember>>(emptyList())
    val roomMembers: StateFlow<List<RoomMember>> = _roomMembers.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    /**
     * Connects to the Socket.IO server using the user's JWT token.
     */
    fun connect(token: String) {
        if (socket?.connected() == true) {
            Log.d(TAG, "Socket already connected.")
            return
        }

        try {
            _connectionState.value = ConnectionState.CONNECTING
            _lastError.value = null

            val serverUrl = NetworkConfig.BASE_URL.trimEnd('/')

            val options = IO.Options().apply {
                auth = Collections.singletonMap("token", token)
                forceNew = true
                reconnection = true
                reconnectionAttempts = 5
                reconnectionDelay = 1000
            }

            socket = IO.socket(URI.create(serverUrl), options).apply {
                on(Socket.EVENT_CONNECT) {
                    Log.i(TAG, "✅ Connected to Socket.IO server")
                    _connectionState.value = ConnectionState.CONNECTED

                    // If we had a roomId queued, re-join
                    currentRoomId?.let { joinRoom(it) }
                }

                on(Socket.EVENT_CONNECT_ERROR) { args ->
                    val error = args.firstOrNull()?.toString() ?: "Connection error"
                    Log.e(TAG, "❌ Connection error: $error")
                    _connectionState.value = ConnectionState.ERROR
                    _lastError.value = error
                }

                on(Socket.EVENT_DISCONNECT) { args ->
                    val reason = args.firstOrNull()?.toString() ?: "Disconnected"
                    Log.w(TAG, "🔌 Disconnected: $reason")
                    _connectionState.value = ConnectionState.DISCONNECTED
                }

                // ── Real-Time Room Event Listeners ───────────────────────────

                // 1. Initial room state received on joining
                on(SocketEvents.EVENT_ROOM_STATE) { args ->
                    handleRoomState(args)
                }

                // 2. Another user joined this room
                on(SocketEvents.EVENT_USER_JOINED) { args ->
                    handleUserJoined(args)
                }

                // 3. A user left this room
                on(SocketEvents.EVENT_USER_LEFT) { args ->
                    handleUserLeft(args)
                }

                // 4. A user disconnected unexpectedly
                on(SocketEvents.EVENT_USER_DISCONNECTED) { args ->
                    handleUserLeft(args)
                }

                // 5. A user toggled their mic mute
                on(SocketEvents.EVENT_USER_MUTE_CHANGED) { args ->
                    handleMuteChanged(args)
                }

                connect()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize socket: ${e.message}", e)
            _connectionState.value = ConnectionState.ERROR
            _lastError.value = e.message
        }
    }

    /**
     * Joins a specific voice room in real-time.
     */
    fun joinRoom(roomId: String) {
        currentRoomId = roomId
        val currentSocket = socket

        if (currentSocket == null || !currentSocket.connected()) {
            Log.d(TAG, "joinRoom: Socket not connected yet, will join on connect.")
            return
        }

        try {
            val payload = JSONObject().apply {
                put("roomId", roomId)
            }
            Log.d(TAG, "Emitting ${SocketEvents.EVENT_JOIN_ROOM} for room: $roomId")
            currentSocket.emit(SocketEvents.EVENT_JOIN_ROOM, payload)
        } catch (e: Exception) {
            Log.e(TAG, "Error emitting join_room: ${e.message}", e)
        }
    }

    /**
     * Leaves the current room and cleans up presence.
     */
    fun leaveRoom(roomId: String) {
        try {
            val payload = JSONObject().apply {
                put("roomId", roomId)
            }
            Log.d(TAG, "Emitting ${SocketEvents.EVENT_LEAVE_ROOM} for room: $roomId")
            socket?.emit(SocketEvents.EVENT_LEAVE_ROOM, payload)
        } catch (e: Exception) {
            Log.e(TAG, "Error emitting leave_room: ${e.message}", e)
        } finally {
            if (currentRoomId == roomId) {
                currentRoomId = null
            }
            _roomMembers.value = emptyList()
        }
    }

    /**
     * Broadcasts local mute state change to other users in the room.
     */
    fun setMute(roomId: String, isMuted: Boolean) {
        try {
            val payload = JSONObject().apply {
                put("roomId", roomId)
                put("isMuted", isMuted)
            }
            Log.d(TAG, "Emitting ${SocketEvents.EVENT_MUTE_CHANGED} -> isMuted: $isMuted")
            socket?.emit(SocketEvents.EVENT_MUTE_CHANGED, payload)
        } catch (e: Exception) {
            Log.e(TAG, "Error emitting mute_changed: ${e.message}", e)
        }
    }

    /**
     * Completely disconnects and removes all listeners.
     */
    fun disconnect() {
        Log.d(TAG, "Disconnecting Socket.IO...")
        currentRoomId = null
        socket?.apply {
            off() // Removes all listeners
            disconnect()
            close()
        }
        socket = null
        _roomMembers.value = emptyList()
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    // ── Internal Event Handlers ──────────────────────────────────────────────

    private fun handleRoomState(args: Array<Any>) {
        try {
            val obj = args.firstOrNull() as? JSONObject ?: return
            val membersArray = obj.optJSONArray("members") ?: return

            val list = mutableListOf<RoomMember>()
            for (i in 0 until membersArray.length()) {
                val memberObj = membersArray.getJSONObject(i)
                val userId = memberObj.optString("userId")
                val name = memberObj.optString("name", "Player")
                val isMuted = memberObj.optBoolean("isMuted", false)

                list.add(
                    RoomMember(
                        id = userId,
                        username = name,
                        isMuted = isMuted,
                        isSpeaking = !isMuted
                    )
                )
            }
            Log.d(TAG, "Received room_state with ${list.size} members")
            _roomMembers.value = list
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing room_state: ${e.message}", e)
        }
    }

    private fun handleUserJoined(args: Array<Any>) {
        try {
            val obj = args.firstOrNull() as? JSONObject ?: return
            val userObj = obj.optJSONObject("user") ?: return

            val userId = userObj.optString("userId")
            val name = userObj.optString("name", "Player")
            val isMuted = userObj.optBoolean("isMuted", false)

            val newMember = RoomMember(
                id = userId,
                username = name,
                isMuted = isMuted,
                isSpeaking = !isMuted
            )

            _roomMembers.update { current ->
                // Avoid duplicate entries
                current.filter { it.id != userId } + newMember
            }
            Log.d(TAG, "User joined: $name ($userId)")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing user_joined: ${e.message}", e)
        }
    }

    private fun handleUserLeft(args: Array<Any>) {
        try {
            val obj = args.firstOrNull() as? JSONObject ?: return
            val userId = obj.optString("userId")
            if (userId.isNotEmpty()) {
                _roomMembers.update { current ->
                    current.filter { it.id != userId }
                }
                Log.d(TAG, "User left: $userId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing user_left: ${e.message}", e)
        }
    }

    private fun handleMuteChanged(args: Array<Any>) {
        try {
            val obj = args.firstOrNull() as? JSONObject ?: return
            val userId = obj.optString("userId")
            val isMuted = obj.optBoolean("isMuted", false)

            if (userId.isNotEmpty()) {
                _roomMembers.update { current ->
                    current.map { member ->
                        if (member.id == userId) {
                            member.copy(
                                isMuted = isMuted,
                                isSpeaking = !isMuted
                            )
                        } else {
                            member
                        }
                    }
                }
                Log.d(TAG, "User $userId mute changed: $isMuted")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing user_mute_changed: ${e.message}", e)
        }
    }
}
