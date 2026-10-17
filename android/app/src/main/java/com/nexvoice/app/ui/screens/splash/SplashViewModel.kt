package com.nexvoice.app.ui.screens.splash

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexvoice.app.auth.AuthRepository
import com.nexvoice.app.auth.SessionValidation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
 *     NetworkError → Splash pe hi Retry button (user ko faltu logout nahi karte)
 */

sealed class SplashDestination {
    object Deciding : SplashDestination()
    object GoToHome : SplashDestination()
    object GoToAuth : SplashDestination()
    object NetworkError : SplashDestination()
}

class SplashViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AuthRepository(application.applicationContext)

    private val _destination = MutableStateFlow<SplashDestination>(SplashDestination.Deciding)
    val destination: StateFlow<SplashDestination> = _destination

    init {
        decide()
    }

    /** Token verify karta hai aur destination set karta hai. Retry button bhi isi ko call karta hai. */
    fun decide() {
        viewModelScope.launch {
            _destination.value = SplashDestination.Deciding
            _destination.value = when (repository.validateSession()) {
                SessionValidation.Valid        -> SplashDestination.GoToHome
                SessionValidation.Invalid      -> SplashDestination.GoToAuth
                SessionValidation.NetworkError -> SplashDestination.NetworkError
            }
        }
    }
}
