package com.nexvoice.app.ui.screens.splash

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexvoice.app.auth.AuthRepository
import com.nexvoice.app.auth.SessionValidation
import com.nexvoice.app.auth.TokenStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * SplashViewModel.kt
 *
 * App khulte hi yahan decide hota hai ki user ko kahan bhejna hai.
 *
 * Pehle sirf token ka *hona* dekha jaata tha:
 *     token != null → Home
 * Isse purana/expire token bhi Home tak pahunch jaata tha, aur wahan
 * "Connection Failed – Token is invalid or expired" wali screen aati thi.
 *
 * Ab token ko server pe VERIFY karte hain (GET /api/auth/me):
 *     Valid        → Home  (auto-login, dobara login nahi maangta)
 *     Invalid      → Auth  (loading animation ke baad seedha Login page)
 *     NetworkError → Splash pe hi Retry button + Auto-retry (user ko faltu logout nahi karte)
 */

sealed class SplashDestination {
    object Deciding : SplashDestination()
    object GoToHome : SplashDestination()
    object GoToAuth : SplashDestination()
    object NetworkError : SplashDestination()
    /** Go directly to voice room (active session restore) */
    data class GoToVoiceRoom(val roomId: String) : SplashDestination()
}

class SplashViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val AUTO_RETRY_SECONDS = 5
        private const val MAX_AUTO_RETRIES = 3
    }

    private val repository = AuthRepository(application.applicationContext)
    private val context = application.applicationContext

    private val _destination = MutableStateFlow<SplashDestination>(SplashDestination.Deciding)
    val destination: StateFlow<SplashDestination> = _destination

    /** Auto-retry countdown (seconds remaining). 0 = no countdown running. */
    private val _retryCountdown = MutableStateFlow(0)
    val retryCountdown: StateFlow<Int> = _retryCountdown

    /** Active room ID for session restore (persisted in DataStore) */
    private val _activeRoomId = MutableStateFlow<String?>(null)
    val activeRoomId: StateFlow<String?> = _activeRoomId.asStateFlow()

    private var autoRetryAttempts = 0
    private var isAutoRetrying = false

    init {
        loadActiveRoom()
        decide()
    }

    /** Load active room ID from DataStore for session restore */
    private fun loadActiveRoom() {
        viewModelScope.launch {
            TokenStore.getActiveRoomIdFlow(context).collect { roomId ->
                if (roomId != null && roomId.isNotBlank()) {
                    _activeRoomId.value = roomId
                }
            }
        }
    }

    /** Token verify karta hai aur destination set karta hai. Retry button bhi isi ko call karta hai. */
    fun decide() {
        viewModelScope.launch {
            // Don't show "Deciding" if we're auto-retrying (keeps countdown alive)
            if (!isAutoRetrying) {
                _destination.value = SplashDestination.Deciding
            }
            val result = repository.validateSession()
            _destination.value = when (result) {
                SessionValidation.Valid        -> {
                    autoRetryAttempts = 0
                    _retryCountdown.value = 0
                    isAutoRetrying = false
                    // Check if there's an active room to restore
                    val roomId = _activeRoomId.value
                    if (roomId != null && roomId.isNotBlank()) {
                        SplashDestination.GoToVoiceRoom(roomId)
                    } else {
                        SplashDestination.GoToHome
                    }
                }
                SessionValidation.Invalid      -> {
                    autoRetryAttempts = 0
                    _retryCountdown.value = 0
                    isAutoRetrying = false
                    clearActiveRoom()
                    SplashDestination.GoToAuth
                }
                SessionValidation.NetworkError -> {
                    if (!isAutoRetrying) {
                        _destination.value = SplashDestination.NetworkError
                        startAutoRetry()
                    } else {
                        // Auto-retry in progress but still NetworkError → continue retrying
                        startAutoRetry()
                    }
                    SplashDestination.NetworkError
                }
            }
        }
    }

    /** Starts auto-retry countdown. User can also tap "Try Again" anytime. */
    private fun startAutoRetry() {
        if (autoRetryAttempts >= MAX_AUTO_RETRIES) return
        autoRetryAttempts++
        isAutoRetrying = true

        viewModelScope.launch {
            for (i in AUTO_RETRY_SECONDS downTo 1) {
                if (!isAutoRetrying) return@launch
                _retryCountdown.value = i
                delay(1000)
            }
            if (isAutoRetrying) {
                decide() // Auto-retry
            }
        }
    }

    /** Called when user manually taps "Try Again" */
    fun onManualRetry() {
        isAutoRetrying = false
        autoRetryAttempts = 0
        _retryCountdown.value = 0
        decide()
    }

    /** Save active room ID for session restore */
    fun setActiveRoom(roomId: String) {
        viewModelScope.launch {
            _activeRoomId.value = roomId
            TokenStore.setActiveRoomId(context, roomId)
        }
    }

    /** Clear active room ID (on leave/logout) */
    fun clearActiveRoom() {
        viewModelScope.launch {
            _activeRoomId.value = null
            TokenStore.clearActiveRoomId(context)
        }
    }
}