package com.nexvoice.app.ui.screens.voiceroom

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexvoice.app.auth.TokenStore
import com.nexvoice.app.models.Room
import com.nexvoice.app.models.RoomMember
import com.nexvoice.app.network.LiveKitRepository
import com.nexvoice.app.network.RoomRepository
import com.nexvoice.app.realtime.LiveKitManager
import com.nexvoice.app.realtime.SocketManager
import com.nexvoice.app.webrtc.WebRtcAudioManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * VoiceRoomViewModel.kt
 *
 * Phase 4B-1: Local Microphone + WebRTC Audio Foundation + Real-Time Presence
 *
 * Coordinates:
 *   1. Real-time room presence via SocketManager
 *   2. WebRTC Audio Engine for local microphone capture
 *   3. Real hardware mute/unmute via WebRtcAudioManager
 *   4. Socket.IO mute broadcast
 *   5. Permission handling & resource cleanup
 */
class VoiceRoomViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val roomRepo = RoomRepository(context)
    private val liveKitRepo = LiveKitRepository(context)

    private var activeRoomId: String? = null
    private var voiceStartedFor: String? = null

    // ── Live Room State ───────────────────────────────────────────────────────
    val members: StateFlow<List<RoomMember>> = SocketManager.roomMembers
    val connectionState: StateFlow<SocketManager.ConnectionState> = SocketManager.connectionState

    // ── LiveKit Voice State (media layer; presence stays on Socket.IO) ───────
    val voiceState: StateFlow<LiveKitManager.VoiceState> = LiveKitManager.voiceState
    val speakingIds: StateFlow<Set<String>> = LiveKitManager.speakingIds
    val voiceError: StateFlow<String?> = LiveKitManager.lastError

    private val _roomInfo = MutableStateFlow<Room?>(null)
    val roomInfo: StateFlow<Room?> = _roomInfo.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isDeafened = MutableStateFlow(false)
    val isDeafened: StateFlow<Boolean> = _isDeafened.asStateFlow()

    private val _isSpeaker = MutableStateFlow(true)
    val isSpeaker: StateFlow<Boolean> = _isSpeaker.asStateFlow()

    private val _hasMicPermission = MutableStateFlow(false)
    val hasMicPermission: StateFlow<Boolean> = _hasMicPermission.asStateFlow()

    private val _permissionDeniedNotice = MutableStateFlow(false)
    val permissionDeniedNotice: StateFlow<Boolean> = _permissionDeniedNotice.asStateFlow()

    private val _leftRoom = MutableStateFlow(false)
    val leftRoom: StateFlow<Boolean> = _leftRoom.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Current logged-in username — used to highlight "You" in the member grid
    private val _currentUserName = MutableStateFlow("")
    val currentUserName: StateFlow<String> = _currentUserName.asStateFlow()

    init {
        viewModelScope.launch {
            TokenStore.getUserNameFlow(context).collect { name ->
                _currentUserName.value = name ?: ""
            }
        }
    }

    /**
     * Initializes real-time presence for the given room.
     */
    fun enterRoom(roomId: String) {
        if (activeRoomId == roomId) return
        activeRoomId = roomId

        viewModelScope.launch {
            try {
                // 1. Load room info from REST API
                roomRepo.getRoomById(roomId).fold(
                    onSuccess = { room -> _roomInfo.value = room },
                    onFailure = { err -> Log.w("VoiceRoomVM", "Failed to fetch room info: ${err.message}") }
                )

                // 2. Connect to Socket.IO using JWT
                val token = TokenStore.getTokenFlow(context).first()
                if (!token.isNullOrEmpty()) {
                    SocketManager.connect(token)
                    SocketManager.joinRoom(roomId)
                } else {
                    _error.value = "User not authenticated"
                }

            } catch (e: Exception) {
                Log.e("VoiceRoomVM", "enterRoom error: ${e.message}", e)
                _error.value = e.message
            }
        }
    }

    /**
     * Called when microphone permission is granted by the user.
     * Phase 4B-2: LiveKit owns the microphone (publish to SFU).
     * The legacy WebRtcAudioManager path runs ONLY when LiveKit is disabled,
     * so two engines never capture the mic at the same time.
     */
    fun onMicrophonePermissionGranted() {
        _hasMicPermission.value = true
        _permissionDeniedNotice.value = false

        val roomId = activeRoomId ?: return
        if (LiveKitManager.USE_LIVEKIT_AUDIO) {
            connectLiveKitVoice(roomId)
        } else {
            // Legacy Phase 4B-1 local-audio path (isolated fallback).
            WebRtcAudioManager.startLocalAudio(context)
            WebRtcAudioManager.setMicrophoneMute(_isMuted.value)
        }
    }

    /**
     * Called when microphone permission is denied.
     */
    fun onMicrophonePermissionDenied() {
        _hasMicPermission.value = false
        _permissionDeniedNotice.value = true
    }

    fun dismissPermissionNotice() {
        _permissionDeniedNotice.value = false
    }

    /**
     * Toggles local microphone mute — does BOTH:
     * 1. LiveKit: mutes the actually-published mic track (remote users stop hearing us)
     *    (or legacy WebRtcAudioManager track when LiveKit is disabled)
     * 2. Socket.IO: broadcasts mute state so everyone's badges stay in sync
     */
    fun toggleMute(roomId: String) {
        val newMuteState = !_isMuted.value
        _isMuted.value = newMuteState

        // 1. Media layer: real mic mute
        if (LiveKitManager.USE_LIVEKIT_AUDIO) {
            LiveKitManager.setMicrophoneEnabled(!newMuteState)
        } else {
            WebRtcAudioManager.setMicrophoneMute(newMuteState)
        }

        // 2. Socket.IO: Broadcast mute state to room members
        SocketManager.setMute(roomId, newMuteState)
    }

    /**
     * Toggles deafen state (if deafened, also mute microphone).
     */
    fun toggleDeafen(roomId: String) {
        val newDeafenState = !_isDeafened.value
        _isDeafened.value = newDeafenState

        if (newDeafenState && !_isMuted.value) {
            _isMuted.value = true
            if (LiveKitManager.USE_LIVEKIT_AUDIO) {
                LiveKitManager.setMicrophoneEnabled(false)
            } else {
                WebRtcAudioManager.setMicrophoneMute(true)
            }
            SocketManager.setMute(roomId, true)
        }
    }

    /**
     * Toggles speaker output.
     */
    fun toggleSpeaker() {
        val newState = !_isSpeaker.value
        _isSpeaker.value = newState
        try {
            val audioManager = context.getSystemService(android.content.Context.AUDIO_SERVICE)
                    as android.media.AudioManager
            if (newState) {
                // Speaker ON — loud speaker
                audioManager.isSpeakerphoneOn = true
                audioManager.mode = android.media.AudioManager.MODE_IN_COMMUNICATION
            } else {
                // Speaker OFF — earpiece
                audioManager.isSpeakerphoneOn = false
                audioManager.mode = android.media.AudioManager.MODE_IN_COMMUNICATION
            }
        } catch (e: Exception) {
            android.util.Log.w("VoiceRoomVM", "toggleSpeaker error: ${e.message}")
        }
    }

    /**
     * Fetches a LiveKit token from our backend and joins the SFU voice room.
     * Socket.IO presence is independent — if voice fails, presence still works.
     */
    private fun connectLiveKitVoice(roomId: String) {
        if (voiceStartedFor == roomId) return
        voiceStartedFor = roomId

        viewModelScope.launch {
            try {
                // Apply current mute intent before connect finishes.
                LiveKitManager.setMicrophoneEnabled(!_isMuted.value)

                liveKitRepo.fetchToken(roomId).fold(
                    onSuccess = { creds ->
                        Log.i("VoiceRoomVM", "Got LiveKit token for ${creds.roomName}")
                        LiveKitManager.connect(context, creds.url, creds.token, creds.roomName)
                    },
                    onFailure = { err ->
                        voiceStartedFor = null
                        Log.e("VoiceRoomVM", "LiveKit token failed: ${err.message}")
                        _error.value = "Voice failed: ${err.message} (presence still works)"
                    }
                )
            } catch (e: Exception) {
                voiceStartedFor = null
                Log.e("VoiceRoomVM", "connectLiveKitVoice error: ${e.message}", e)
                _error.value = "Voice failed: ${e.message}"
            }
        }
    }

    /**
     * Leaves the room cleanly:
     * - Disconnects LiveKit (stops mic publish, frees audio resources)
     * - Releases legacy WebRTC engine (only if it was used)
     * - Emits leave_room on Socket.IO
     * - Calls REST leave API
     */
    fun leaveRoom(roomId: String) {
        viewModelScope.launch {
            try {
                // 1. Leave LiveKit voice room
                voiceStartedFor = null
                LiveKitManager.disconnect()

                // 2. Release legacy WebRTC engine (no-op unless legacy path active)
                if (!LiveKitManager.USE_LIVEKIT_AUDIO) {
                    WebRtcAudioManager.release()
                }

                // 3. Leave real-time socket room immediately
                SocketManager.leaveRoom(roomId)

                // 4. Call REST leave endpoint
                roomRepo.leaveRoom(roomId)

            } catch (e: Exception) {
                Log.e("VoiceRoomVM", "leaveRoom error: ${e.message}", e)
            } finally {
                activeRoomId = null
                _leftRoom.value = true
            }
        }
    }

    fun resetLeft() {
        _leftRoom.value = false
    }

    // ── Invite / Add user by username or email ──────────────────────────────
    private val _isInviting = MutableStateFlow(false)
    val isInviting: StateFlow<Boolean> = _isInviting.asStateFlow()

    private val _inviteSuccess = MutableStateFlow<String?>(null)
    val inviteSuccess: StateFlow<String?> = _inviteSuccess.asStateFlow()

    private val _inviteError = MutableStateFlow<String?>(null)
    val inviteError: StateFlow<String?> = _inviteError.asStateFlow()

    fun inviteUser(username: String) {
        val roomId = activeRoomId ?: return
        if (username.isBlank()) return

        viewModelScope.launch {
            _isInviting.value = true
            _inviteError.value = null
            _inviteSuccess.value = null

            val result = roomRepo.inviteUser(roomId, username)
            result.fold(
                onSuccess = { msg ->
                    _inviteSuccess.value = msg
                    // Refresh room info so memberCount updates
                    roomRepo.getRoomById(roomId).onSuccess { _roomInfo.value = it }
                },
                onFailure = { err ->
                    _inviteError.value = err.message ?: "Failed to add user"
                }
            )
            _isInviting.value = false
        }
    }

    fun clearInviteStatus() {
        _inviteSuccess.value = null
        _inviteError.value = null
    }

    override fun onCleared() {
        super.onCleared()
        // Leave voice + presence cleanly (no duplicate connections on re-entry).
        voiceStartedFor = null
        LiveKitManager.disconnect()
        if (!LiveKitManager.USE_LIVEKIT_AUDIO) {
            WebRtcAudioManager.release()
        }

        activeRoomId?.let { roomId ->
            SocketManager.leaveRoom(roomId)
        }
    }
}
