package com.nexvoice.app.utils

/**
 * InviteLink.kt
 *
 * YouTube-style room invites: share a link, or paste a link / room ID to join.
 *
 * Link format:  nexvoice://room/<24-hex-mongo-id>
 *   e.g. nexvoice://room/68e0a1b2c3d4e5f6a7890123
 *
 * parseRoomId() accepts:
 *   1. the full link (even inside a longer share message, e.g. from WhatsApp),
 *   2. a bare room ID pasted on its own.
 * Returns null when no valid room ID is found.
 */
object InviteLink {

    const val SCHEME = "nexvoice://room/"

    /** Builds a shareable invite link for a room. */
    fun build(roomId: String): String = "$SCHEME${roomId.trim()}"

    /** Message text used by the system share sheet. */
    fun shareMessage(roomName: String, roomId: String): String =
        "Join my NexVoice room \"$roomName\" 🎮\n${build(roomId)}"

    private val ROOM_ID_PATTERN = Regex("[0-9a-fA-F]{24}")

    /**
     * Extracts a room ID from pasted text.
     * Accepts the full link, a share message containing the link, or a bare ID.
     */
    fun parseRoomId(input: String): String? {
        val text = input.trim()
        if (text.isEmpty()) return null
        // find() tolerates surrounding text like "Join my room <link>"
        return ROOM_ID_PATTERN.find(text)?.value
    }
}
