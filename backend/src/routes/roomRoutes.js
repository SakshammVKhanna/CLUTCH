/**
 * routes/roomRoutes.js
 *
 * Invite-only: no public room list.
 *
 *   POST   /api/rooms          → create a room   (auth required)
 *   GET    /api/rooms          → list MY rooms    (auth required)
 *   GET    /api/rooms/:id      → get one room     (public — for invite link preview)
 *   POST   /api/rooms/:id/join  → join a room     (auth required)
 *   POST   /api/rooms/:id/leave → leave a room    (auth required)
 */

const express  = require('express');
const router   = express.Router();
const { protect } = require('../middleware/authMiddleware');

const {
  createRoom,
  getMyRooms,
  getRoomById,
  joinRoom,
  leaveRoom,
  inviteUser,
} = require('../controllers/roomController');

router.post('/', protect, createRoom);          // Must be logged in to create a room
router.get('/',  protect, getMyRooms);          // Invite-only — only my rooms
router.get('/:id', getRoomById);               // Public — anyone can see room details
router.post('/:id/join',   protect, joinRoom);   // Must be logged in to join
router.post('/:id/leave',  protect, leaveRoom);  // Must be logged in to leave
router.post('/:id/invite', protect, inviteUser); // Add friend to room by username/email/id

module.exports = router;
