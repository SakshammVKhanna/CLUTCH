package com.nexvoice.app.auth

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * SessionManager.kt
 *
 * Ek hi jagah se "session khatam ho gaya" ka signal jaata hai.
 *
 * Kyu chahiye?
 *  - JWT 7 din me expire hota hai. Uske baad koi bhi protected API call
 *    (rooms, livekit token, etc.) HTTP 401 deti hai.
 *  - Pehle app us 401 pe "Connection Failed / Try Again" wali dead-end
 *    screen dikha deti thi — user ko samajh hi nahi aata ki dobara login karna hai.
 *
 * Ab flow ye hai:
 *   ApiClient ka interceptor 401 dekhta hai
 *        → SessionManager.notifyExpired()
 *        → NavGraph isko sunta hai
 *        → token clear + Socket/LiveKit/audio band + seedha Login screen
 *
 * MutableSharedFlow (StateFlow nahi) isliye, kyunki ye ek "event" hai — jo ho gaya
 * wo ho gaya. Replay = 0 taki app dobara khulne pe purana logout event dobara fire na ho.
 */
object SessionManager {

    private val _expired = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,               // interceptor non-suspending context se emit karta hai
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /** UI (NavGraph) ise collect karta hai. */
    val expired: SharedFlow<Unit> = _expired

    /** Interceptor se call hota hai — 401 milte hi. */
    fun notifyExpired() {
        _expired.tryEmit(Unit)
    }
}
