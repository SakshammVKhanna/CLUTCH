/**
 * controllers/livekitController.js
 *
 * Phase 4B-2: LiveKit access-token generation (SFU media layer).
 *
 * POST /api/livekit/token  (protected by the existing NexVoice JWT)
 * Body: { "roomId": "<nexvoice-room-id>" }
 *
 * Steps:
 *   1. `protect` middleware already verified the NexVoice JWT → req.user = { id, name, email }
 *   2. Verify the NexVoice room exists.
 *   3. Verify the user is allowed in (owner or member — rooms are invite-only).
 *   4. Sign a short-lived LiveKit AccessToken (identity = NexVoice user ID).
 *   5. Return { token, url, roomName, identity }.
 *
 * The LIVEKIT_API_SECRET never leaves this server. Android only ever
 * receives the short-lived scoped token.
 */

const Room = require('../models/Room');
const { livekitRoomNameFor } = require('../realtime/livekitRoom');

// Lazy-loaded so a missing/broken SDK surfaces as a clean 500, not a boot crash.
let LiveKitAccessToken = null;
const loadAccessToken = async () => {
  if (LiveKitAccessToken) return LiveKitAccessToken;
  // Dynamic import works whether the installed SDK ships CJS or ESM.
  const sdk = await import('livekit-server-sdk');
  LiveKitAccessToken = sdk.AccessToken;
  if (!LiveKitAccessToken) {
    throw new Error('livekit-server-sdk does not export AccessToken');
  }
  return LiveKitAccessToken;
};

// ─────────────────────────────────────────────────────────────────────────────
// POST /api/livekit/token
// Body: { "roomId": "..." }
// Requires: Authorization: Bearer <nexvoice-jwt>
// ─────────────────────────────────────────────────────────────────────────────
const getLiveKitToken = async (req, res) => {
  try {
    const { roomId } = req.body || {};
    if (!roomId) {
      return res.status(400).json({ error: 'roomId is required' });
    }

    // 1. Server must be configured for LiveKit.
    const { LIVEKIT_URL, LIVEKIT_API_KEY, LIVEKIT_API_SECRET } = process.env;
    if (!LIVEKIT_URL || !LIVEKIT_API_KEY || !LIVEKIT_API_SECRET) {
      console.error('LiveKit env missing: set LIVEKIT_URL, LIVEKIT_API_KEY, LIVEKIT_API_SECRET');
      return res.status(500).json({ error: 'Voice server is not configured. Try again later.' });
    }

    // 2. NexVoice room must exist.
    let room;
    try {
      room = await Room.findById(roomId);
    } catch (err) {
      return res.status(400).json({ error: 'Invalid room ID' });
    }
    if (!room) {
      return res.status(404).json({ error: 'Room not found' });
    }

    // 3. Only owner/members may get a voice token (invite-only rooms).
    const userId = req.user.id.toString();
    const isOwner = room.owner.toString() === userId;
    const isMember = room.members.some((m) => m.toString() === userId);
    if (!isOwner && !isMember) {
      return res.status(403).json({ error: 'You are not a member of this room' });
    }

    // 4. Sign the LiveKit token. Identity = NexVoice user ID so the media
    //    participant maps 1:1 back to the NexVoice user.
    const livekitRoom = livekitRoomNameFor(room._id);
    const AccessToken = await loadAccessToken();
    const at = new AccessToken(LIVEKIT_API_KEY, LIVEKIT_API_SECRET, {
      identity: userId,
      name: req.user.name || 'Player',
      ttl: '15m', // short-lived by design; client re-requests on rejoin
    });
    at.addGrant({
      roomJoin: true,
      room: livekitRoom,
      canPublish: true,     // mic audio → SFU
      canSubscribe: true,   // hear everyone else
      canPublishData: true, // future protocol messages (no media over Socket.IO ever)
    });

    // NOTE: livekit-server-sdk v2 made toJwt() async; awaiting a
    // sync v1 return value is harmless, so this works on both.
    const token = await at.toJwt();

    console.log(`🎫 [LiveKit] Token issued: ${req.user.name} → ${livekitRoom}`);

    res.json({
      token,
      url: LIVEKIT_URL, // must be reachable from the Android device (see .env notes)
      roomName: livekitRoom,
      identity: userId,
    });
  } catch (error) {
    console.error('getLiveKitToken error:', error);
    res.status(500).json({ error: 'Failed to create voice token' });
  }
};

module.exports = { getLiveKitToken };
