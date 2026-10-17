package com.nexvoice.app.ui.screens.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexvoice.app.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * AuthViewModel.kt
 *
 * ViewModel holds UI state and survives screen rotations.
 * It calls AuthRepository to do the actual work and updates the UI state.
 *
 * AndroidViewModel is a ViewModel that has access to the app Context,
 * which we need to access DataStore (for saving the token).
 *
 * UiState sealed class describes what the screen should show:
 *   Idle    = form is ready, nothing happening
 *   Loading = spinner showing, button disabled
 *   Success = login worked → navigate to Home
 *   Error   = something went wrong → show error message
 */

// Describes the current state of the Auth screen
sealed class AuthUiState {
    object Idle    : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val userName: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AuthRepository(application.applicationContext)

    // _uiState is private and mutable — only the ViewModel can change it
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)

    // uiState is public and read-only — the screen observes this
    val uiState: StateFlow<AuthUiState> = _uiState

    fun signup(name: String, email: String, password: String) {
        // viewModelScope.launch runs code in the background (coroutine)
        // so we don't block the main UI thread while making network calls
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = repository.signup(name, email, password)
            _uiState.value = result.fold(
                onSuccess = { userName -> AuthUiState.Success(userName) },
                onFailure = { error  -> AuthUiState.Error(error.message ?: "Signup failed") }
            )
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = repository.login(email, password)
            _uiState.value = result.fold(
                onSuccess = { userName -> AuthUiState.Success(userName) },
                onFailure = { error  -> AuthUiState.Error(error.message ?: "Login failed") }
            )
        }
    }

    // Called when the user dismisses the error message
    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
