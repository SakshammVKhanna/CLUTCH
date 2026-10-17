package com.nexvoice.app.realtime

/**
 * SocketEvents.kt
 *
 * Defines the contract of event names used for real-time Socket.IO communication
 * between Android client and the Node.js backend.
 */
object SocketEvents {
    // ── Client → Server Events ────────────────────────────────────────────────
    const val EVENT_JOIN_ROOM     = "join_room"
    const val EVENT_LEAVE_ROOM    = "leave_room"
    const val EVENT_MUTE_CHANGED  = "mute_changed"

    // ── Server → Client Events ────────────────────────────────────────────────
    const val EVENT_ROOM_STATE       = "room_state"
    const val EVENT_USER_JOINED      = "user_joined"
    const val EVENT_USER_LEFT        = "user_left"
    const val EVENT_USER_MUTE_CHANGED = "user_mute_changed"
    const val EVENT_USER_DISCONNECTED = "user_disconnected"
}
