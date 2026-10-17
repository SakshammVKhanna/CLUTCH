/**
 * routes/livekitRoutes.js
 *
 * Phase 4B-2: LiveKit voice-token endpoint.
 *
 *   POST /api/livekit/token → mint a LiveKit access token (NexVoice JWT required)
 */

const express = require('express');
const router = express.Router();
const { protect } = require('../middleware/authMiddleware');
const { getLiveKitToken } = require('../controllers/livekitController');

router.post('/token', protect, getLiveKitToken);

module.exports = router;
