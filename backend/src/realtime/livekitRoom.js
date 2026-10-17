/**
 * realtime/livekitRoom.js
 *
 * Phase 4B-2: Mapping between NexVoice rooms and LiveKit rooms.
 *
 * NexVoice roomId = MongoDB ObjectId (persistent, source of truth).
 * LiveKit roomName = "nexvoice_<roomId>" (media-layer only, auto-created
 * by LiveKit when the first participant joins — nothing is stored in MongoDB).
 *
 * Keeping the prefix makes LiveKit rooms traceable back to NexVoice
 * and avoids collisions with any other service using the same LiveKit server.
 */

const LIVEKIT_ROOM_PREFIX = 'nexvoice_';

/**
 * Maps a NexVoice MongoDB room ID to its LiveKit room name.
 * @param {string} roomId - NexVoice room ObjectId as string
 * @returns {string} e.g. "nexvoice_68e0a1b2c3d4e5f6a7890123"
 */
const livekitRoomNameFor = (roomId) => {
  return `${LIVEKIT_ROOM_PREFIX}${roomId.toString()}`;
};

module.exports = { livekitRoomNameFor, LIVEKIT_ROOM_PREFIX };
