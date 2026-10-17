package com.nexvoice.app.webrtc

import android.content.Context
import android.util.Log

/**
 * WebRtcAudioManager.kt
 *
 * Phase 4B-2: RETIRED no-op shim.
 *
 * History: in Phase 4B-1 this object owned a local-only microphone capture
 * built on io.getstream:stream-webrtc-android. That artifact was never
 * published to Maven Central / Google Maven / JitPack, so it could not be
 * resolved — and the engine never transmitted audio anyway (no peer
 * connections existed; WebRtcPeerManager was an empty stub).
 *
 * LiveKit (livekit.org.webrtc, via LiveKitManager) is now the single audio
 * engine: mic publish, remote subscribe, mute. This shim keeps the old
 * public API so existing call sites compile, but every call is a no-op.
 * VoiceRoomViewModel gates usage behind LiveKitManager.USE_LIVEKIT_AUDIO.
 */
object WebRtcAudioManager {

    private const val TAG = "NexVoice_WebRTC"

    val isAudioTrackActive: Boolean
        get() = false

    fun getLocalAudioTrack(): Any? = null

    fun startLocalAudio(context: Context) {
        Log.w(TAG, "[WebRTC] startLocalAudio ignored — retired in Phase 4B-2, LiveKit owns the mic.")
    }

    fun setMicrophoneMute(isMuted: Boolean) {
        Log.w(TAG, "[WebRTC] setMicrophoneMute ignored — retired in Phase 4B-2, use LiveKitManager.")
    }

    fun release() {
        Log.d(TAG, "[WebRTC] release ignored — nothing to release (retired shim).")
    }
}
