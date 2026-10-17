package com.nexvoice.app.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexvoice.app.auth.AuthRepository
import com.nexvoice.app.auth.TokenStore
import com.nexvoice.app.models.Room
import com.nexvoice.app.network.RoomRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * HomeViewModel.kt
 *
 * Handles:
 *   - Loading the list of rooms from the backend
 *   - Joining a room
 *   - Logging out
 *   - Reading the logged-in user's name for display
 */

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val rooms: List<Room>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val roomRepo = RoomRepository(application.applicationContext)
    private val authRepo = AuthRepository(application.applicationContext)
    private val ctx      = application.applicationContext

    private val _uiState    = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState

    // The user's name — shown in the top bar
    private val _userName = MutableStateFlow("Gamer")
    val userName: StateFlow<String> = _userName

    // The room ID after a successful join — used to navigate to VoiceRoomScreen
    private val _joinedRoomId = MutableStateFlow<String?>(null)
    val joinedRoomId: StateFlow<String?> = _joinedRoomId

    // Error message from join (e.g., "Room is full")
    private val _joinError = MutableStateFlow<String?>(null)
    val joinError: StateFlow<String?> = _joinError

    init {
        loadUserName()
        loadRooms()
    }

    private fun loadUserName() {
        viewModelScope.launch {
            TokenStore.getUserNameFlow(ctx).collect { name ->
                _userName.value = name ?: "Gamer"
            }
        }
    }

    fun loadRooms() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            val result = roomRepo.getRooms()
            _uiState.value = result.fold(
                onSuccess = { rooms -> HomeUiState.Success(rooms) },
                onFailure = { error -> HomeUiState.Error(error.message ?: "Failed to load rooms") }
            )
        }
    }

    fun joinRoom(roomId: String) {
        viewModelScope.launch {
            val result = roomRepo.joinRoom(roomId)
            result.fold(
                onSuccess = { _joinedRoomId.value = roomId },
                onFailure = { error -> 
                    // Better error messages for user
                    val userFriendlyMessage = when {
                        error.message?.contains("already", ignoreCase = true) == true -> 
                            "Joining room..." // Show as joining instead of error
                        error.message?.contains("full", ignoreCase = true) == true -> 
                            "Room is full. Try again later."
                        error.message?.contains("not found", ignoreCase = true) == true -> 
                            "Room not found. Check the room ID."
                        else -> error.message ?: "Failed to join room"
                    }
                    
                    // If already in room, treat as success
                    if (error.message?.contains("already", ignoreCase = true) == true) {
                        _joinedRoomId.value = roomId
                    } else {
                        _joinError.value = userFriendlyMessage
                    }
                }
            )
        }
    }

    fun clearJoinedRoom() { _joinedRoomId.value = null }
    fun clearJoinError()  { _joinError.value  = null }

    fun logout() {
        viewModelScope.launch {
            com.nexvoice.app.realtime.LiveKitManager.disconnect()
            com.nexvoice.app.webrtc.WebRtcAudioManager.release()
            com.nexvoice.app.realtime.SocketManager.disconnect()
            authRepo.logout()
        }
    }
}
