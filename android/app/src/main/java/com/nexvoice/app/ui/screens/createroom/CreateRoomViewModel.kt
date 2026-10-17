package com.nexvoice.app.ui.screens.createroom

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexvoice.app.network.RoomRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * CreateRoomViewModel.kt
 *
 * Handles the Create Room API call.
 * On success, provides the new room's ID so we can navigate to VoiceRoomScreen.
 */

sealed class CreateRoomUiState {
    object Idle    : CreateRoomUiState()
    object Loading : CreateRoomUiState()
    data class Success(val roomId: String) : CreateRoomUiState()
    data class Error(val message: String)  : CreateRoomUiState()
}

class CreateRoomViewModel(application: Application) : AndroidViewModel(application) {

    private val roomRepo = RoomRepository(application.applicationContext)

    private val _uiState = MutableStateFlow<CreateRoomUiState>(CreateRoomUiState.Idle)
    val uiState: StateFlow<CreateRoomUiState> = _uiState

    fun createRoom(
        name: String,
        gameName: String
    ) {
        viewModelScope.launch {
            _uiState.value = CreateRoomUiState.Loading
            val result = roomRepo.createRoom(name, gameName)
            _uiState.value = result.fold(
                onSuccess = { room -> CreateRoomUiState.Success(room.id) },
                onFailure = { error -> CreateRoomUiState.Error(error.message ?: "Failed to create room") }
            )
        }
    }

    fun resetState() { _uiState.value = CreateRoomUiState.Idle }
}
