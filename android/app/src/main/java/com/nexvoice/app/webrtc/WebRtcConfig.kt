package com.nexvoice.app.webrtc

/**
 * WebRtcConfig.kt
 *
 * Phase 4B-2: RETIRED shim (kept for reference).
 *
 * The org.webrtc-based audio constraints lived here in Phase 4B-1. The
 * underlying artifact was unresolvable, and LiveKit now manages audio
 * capture/processing internally — so these constraints are unused.
 */
object WebRtcConfig {
    const val AUDIO_TRACK_ID = "NexVoice_local_audio_track"
}
