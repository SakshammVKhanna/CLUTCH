package com.nexvoice.app.realtime

import android.content.Context
import android.util.Log
import io.livekit.android.LiveKit
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.*
import io.livekit.android.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * LiveKitManager.kt
 *
 * Phase 4B-2: SFU voice layer (LiveKit). Owns ONLY media:
 *   - connect to the LiveKit room "nexvoice_<roomId>"
 *   - publish local microphone, auto-subscribe to remote audio
 *   - live connection state + active-speaker ids for the UI
 *
 * Application presence (who joined/left, mute badges, member list) stays
 * on SocketManager. No audio ever goes through Socket.IO.
 *
 * Single-mic rule: when this manager is active, the legacy
 * WebRtcAudioManager must NOT capture — VoiceRoomViewModel gates it
 * behind USE_LIVEKIT_AUDIO so two engines never fight for the mic.
 */
object LiveKitManager {

    private const val TAG = "NexVoice_LiveKit"

    /** Set to false only to fall back to the legacy Phase 4B-1 local-audio path. */
    const val USE_LIVEKIT_AUDIO = true

    enum class VoiceState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        RECONNECTING,
        FAILED
    }

    private val lock = Any()

    private var room: Room? = null
    private var scope: CoroutineScope? = null
    private var eventsJob: Job? = null
    private var activeRoomName: String? = null

    /** Current mic intent (false = user muted). Applied on connect + on toggle. */
    @Volatile
    private var micEnabled: Boolean = true

    private val _voiceState = MutableStateFlow(VoiceState.DISCONNECTED)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    /** LiveKit participant identities currently speaking (subset of Socket.IO members). */
    private val _speakingIds = MutableStateFlow<Set<String>>(emptySet())
    val speakingIds: StateFlow<Set<String>> = _speakingIds.asStateFlow()

    /** How many remote participants publish in the LiveKit room. */
    private val _remoteVoiceCount = MutableStateFlow(0)
    val remoteVoiceCount: StateFlow<Int> = _remoteVoiceCount.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    /**
     * Connects to the LiveKit room and publishes the microphone.
     * Safe to call twice for the same room — duplicates are ignored.
     * Call [disconnect] before joining a different room.
     */
    fun connect(appContext: Context, url: String, token: String, roomName: String) {
        synchronized(lock) {
            if (activeRoomName == roomName && room != null) {
                Log.d(TAG, "Already connected/connecting to $roomName, ignoring duplicate connect.")
                return
            }
            if (room != null) {
                Log.w(TAG, "Connect requested for $roomName while another room is active — disconnect first.")
                return
            }
            activeRoomName = roomName
            _voiceState.value = VoiceState.CONNECTING
            _lastError.value = null

            val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            scope = appScope
            val newRoom = LiveKit.create(appContext.applicationContext)
            room = newRoom

            eventsJob = appScope.launch { observeEvents(newRoom) }

            appScope.launch {
                try {
                    Log.i(TAG, "Connecting to LiveKit room: $roomName @ $url")
                    newRoom.connect(url, token)
                    // Publish mic (respects mute pressed before connect finished).
                    newRoom.localParticipant.setMicrophoneEnabled(micEnabled)
                    _voiceState.value = VoiceState.CONNECTED
                    refreshRemoteCount(newRoom)
                    Log.i(TAG, "✅ LiveKit connected + mic published (enabled=$micEnabled)")
                } catch (e: Exception) {
                    // connect() throws if room was released mid-join after a leave — not an error.
                    if (_voiceState.value == VoiceState.CONNECTING) {
                        Log.e(TAG, "❌ LiveKit connect failed: ${e.message}", e)
                        _voiceState.value = VoiceState.FAILED
                        _lastError.value = e.message ?: "Voice connection failed"
                    } else {
                        Log.d(TAG, "LiveKit connect aborted (room left during join).")
                    }
                }
            }
        }
    }

    /**
     * Mutes/unmutes the REAL published mic track.
     * Caller must ALSO broadcast via SocketManager so badges stay in sync.
     */
    fun setMicrophoneEnabled(enabled: Boolean) {
        micEnabled = enabled
        val currentRoom = synchronized(lock) { room }
        if (currentRoom == null) {
            Log.d(TAG, "setMicrophoneEnabled: no active LiveKit room yet (will apply on connect).")
            return
        }
        scope?.launch {
            try {
                currentRoom.localParticipant.setMicrophoneEnabled(enabled)
                Log.i(TAG, if (enabled) "🎙️ LiveKit mic unmuted" else "🎙️ LiveKit mic muted")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set LiveKit mic: ${e.message}", e)
            }
        }
    }

    /** Leaves the voice room: stops mic publish, disconnects, frees native resources. */
    fun disconnect() {
        val leavingRoom: Room?
        synchronized(lock) {
            leavingRoom = room
            room = null
            activeRoomName = null
            eventsJob?.cancel()
            eventsJob = null
            scope?.cancel()
            scope = null
            _speakingIds.value = emptySet()
            _remoteVoiceCount.value = 0
            _voiceState.value = VoiceState.DISCONNECTED
        }
        try {
            // release() disconnects + frees native WebRTC resources. After this the
            // Room object must not be reused — next connect() creates a fresh one.
            leavingRoom?.release()
            Log.i(TAG, "LiveKit disconnected + released.")
        } catch (e: Exception) {
            Log.e(TAG, "Error during LiveKit disconnect: ${e.message}", e)
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private suspend fun observeEvents(liveRoom: Room) {
        try {
            liveRoom.events.collect { event ->
                when (event) {
                    is RoomEvent.Connected, is RoomEvent.Reconnected -> {
                        _voiceState.value = VoiceState.CONNECTED
                        _lastError.value = null
                        refreshRemoteCount(liveRoom)
                    }
                    is RoomEvent.Reconnecting -> {
                        _voiceState.value = VoiceState.RECONNECTING
                    }
                    is RoomEvent.Disconnected -> {
                        // Client-initiated disconnect() also posts this — only mark
                        // FAILED when WE didn't ask to leave.
                        synchronized(lock) {
                            if (room == liveRoom) {
                                _voiceState.value = VoiceState.FAILED
                                _lastError.value = event.error?.message ?: "Voice disconnected"
                            }
                        }
                        _speakingIds.value = emptySet()
                    }
                    is RoomEvent.ParticipantConnected,
                    is RoomEvent.ParticipantDisconnected -> {
                        refreshRemoteCount(liveRoom)
                    }
                    is RoomEvent.ActiveSpeakersChanged -> {
                        _speakingIds.value = event.speakers
                            .mapNotNull { it.identity?.value }
                            .toSet()
                    }
                    else -> { /* media presence beyond this lives on Socket.IO */ }
                }
            }
        } catch (e: Exception) {
            // Scope cancelled on disconnect() — expected, not an error.
            Log.d(TAG, "LiveKit event collection ended: ${e.message}")
        }
    }

    private fun refreshRemoteCount(liveRoom: Room) {
        try {
            _remoteVoiceCount.value = liveRoom.remoteParticipants.size
        } catch (e: Exception) {
            Log.w(TAG, "Could not read remote participant count: ${e.message}")
        }
    }
}
