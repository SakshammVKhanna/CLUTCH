/**
 * controllers/roomController.js
 *
 * Phase 3: userId now comes from the JWT token (req.user.id),
 * NOT from the request body. This means users can't fake being someone else.
 * The protect middleware sets req.user before these functions run.
 */

const mongoose = require('mongoose');
const Room = require('../models/Room');
const User = require('../models/User');

// ─────────────────────────────────────────────────────────────────────────────
// POST /api/rooms  (auth required)
// Body: { "name": "...", "gameName": "..." }
// Owner is taken from the JWT token — no need to send ownerId in the body.
// All rooms are invite-only — no public flag.
// No member cap — unlimited members (Issue 1).
// ─────────────────────────────────────────────────────────────────────────────
const createRoom = async (req, res) => {
  try {
    const { name, gameName } = req.body;
    const ownerId = req.user.id; // comes from JWT via protect middleware

    if (!name || !gameName) {
      return res.status(400).json({ error: 'name and gameName are required' });
    }

    const room = await Room.create({
      name,
      gameName,
      owner:      ownerId,
      members:    [ownerId],
    });

    await room.populate('owner',   'name email profileImage');
    await room.populate('members', 'name email profileImage');

    res.status(201).json({
      message: 'Room created successfully',
      room,
    });

  } catch (error) {
    if (error.name === 'ValidationError') {
      const messages = Object.values(error.errors).map(e => e.message);
      return res.status(400).json({ error: messages.join('. ') });
    }
    console.error('createRoom error:', error);
    res.status(500).json({ error: 'Failed to create room' });
  }
};

// ─────────────────────────────────────────────────────────────────────────────
// GET /api/rooms  (auth required)
// Returns only MY rooms — where I am owner or member.
// Invite-only: no public list. New users see empty list until invited.
// ─────────────────────────────────────────────────────────────────────────────
const getMyRooms = async (req, res) => {
  try {
    const userId = req.user?.id; // from JWT token
    if (!userId) {
      return res.status(401).json({ error: 'User ID missing from token' });
    }

    const userObjectId = mongoose.Types.ObjectId.isValid(userId)
      ? new mongoose.Types.ObjectId(userId)
      : userId;

    const rooms = await Room.find({
      $or: [
        { owner: userId },
        { owner: userObjectId },
        { members: userId },
        { members: userObjectId },
      ],
    })
      .populate('owner',   'name email profileImage')
      .populate('members', 'name email profileImage')
      .sort({ createdAt: -1 });

    const cleanedRooms = rooms.map(room => {
      const r = room.toObject ? room.toObject() : room;
      if (Array.isArray(r.members)) {
        r.members = r.members.filter(Boolean);
      }
      return r;
    });

    res.json({ count: cleanedRooms.length, rooms: cleanedRooms });

  } catch (error) {
    console.error('getMyRooms error:', error);
    res.status(500).json({ error: error.message || 'Failed to get rooms' });
  }
};


// ─────────────────────────────────────────────────────────────────────────────
// GET /api/rooms/:id  (public)
// ─────────────────────────────────────────────────────────────────────────────
const getRoomById = async (req, res) => {
  try {
    const room = await Room.findById(req.params.id)
      .populate('owner',   'name email profileImage')
      .populate('members', 'name email profileImage');

    if (!room) return res.status(404).json({ error: 'Room not found' });
    res.json({ room });

  } catch (error) {
    if (error.name === 'CastError') {
      return res.status(400).json({ error: 'Invalid room ID format' });
    }
    console.error('getRoomById error:', error);
    res.status(500).json({ error: 'Failed to get room' });
  }
};

// ─────────────────────────────────────────────────────────────────────────────
// POST /api/rooms/:id/join  (auth required)
// No body needed — userId comes from the JWT token.
// ─────────────────────────────────────────────────────────────────────────────
const joinRoom = async (req, res) => {
  try {
    const userId = req.user.id; // from JWT token

    const room = await Room.findById(req.params.id);
    if (!room) return res.status(404).json({ error: 'Room not found' });

    const alreadyIn = room.members.some(id => id.toString() === userId);
    if (alreadyIn) {
      return res.status(409).json({ error: 'You are already in this room' });
    }

    // No capacity check — unlimited members (Issue 1).

    const updatedRoom = await Room.findByIdAndUpdate(
      req.params.id,
      { $addToSet: { members: userId } },
      { new: true }
    )
      .populate('owner',   'name email profileImage')
      .populate('members', 'name email profileImage');

    res.json({
      message: 'Joined room successfully',
      room: updatedRoom,
    });

  } catch (error) {
    if (error.name === 'CastError') {
      return res.status(400).json({ error: 'Invalid room ID format' });
    }
    console.error('joinRoom error:', error);
    res.status(500).json({ error: 'Failed to join room' });
  }
};

// ─────────────────────────────────────────────────────────────────────────────
// POST /api/rooms/:id/leave  (auth required)
// No body needed — userId comes from the JWT token.
// ─────────────────────────────────────────────────────────────────────────────
const leaveRoom = async (req, res) => {
  try {
    const userId = req.user.id; // from JWT token

    const room = await Room.findById(req.params.id);
    if (!room) return res.status(404).json({ error: 'Room not found' });

    const isInRoom = room.members.some(id => id.toString() === userId);
    if (!isInRoom) {
      return res.status(400).json({ error: 'You are not in this room' });
    }

    const updatedRoom = await Room.findByIdAndUpdate(
      req.params.id,
      { $pull: { members: userId } },
      { new: true }
    )
      .populate('owner',   'name email profileImage')
      .populate('members', 'name email profileImage');

    res.json({
      message: 'Left room successfully',
      room: updatedRoom,
    });

  } catch (error) {
    if (error.name === 'CastError') {
      return res.status(400).json({ error: 'Invalid room ID format' });
    }
    console.error('leaveRoom error:', error);
    res.status(500).json({ error: 'Failed to leave room' });
  }
};

// ─────────────────────────────────────────────────────────────────────────────
// POST /api/rooms/:id/invite  (auth required)
// Body: { "username": "..." }  (can be username, email, or userId)
// ─────────────────────────────────────────────────────────────────────────────
const inviteUser = async (req, res) => {
  try {
    const { username } = req.body || {};
    if (!username || !username.trim()) {
      return res.status(400).json({ error: 'Please enter a username or email' });
    }

    const room = await Room.findById(req.params.id);
    if (!room) return res.status(404).json({ error: 'Room not found' });

    const query = username.trim();
    const isObjectId = mongoose.Types.ObjectId.isValid(query);
    const escaped = query.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    const normalizedPattern = escaped.replace(/(.)\1*$/i, '$1+');

    const conditions = [
      { name: new RegExp(`^${escaped}$`, 'i') },
      { name: new RegExp(`^${normalizedPattern}$`, 'i') },
      { email: query.toLowerCase() },
    ];
    if (isObjectId) {
      conditions.push({ _id: query });
    }

    // Find all matching users (handles multiple accounts with same name)
    const targetUsers = await User.find({ $or: conditions });
    if (!targetUsers || targetUsers.length === 0) {
      return res.status(404).json({ error: `User "${query}" not found` });
    }

    let addedUsers = [];
    for (const targetUser of targetUsers) {
      const targetIdStr = targetUser._id.toString();
      const alreadyMember = room.members.some((m) => m.toString() === targetIdStr);
      if (!alreadyMember) {
        room.members.push(targetUser._id);
        addedUsers.push(targetUser);
      }
    }

    if (addedUsers.length > 0) {
      await room.save();
    }

    const primaryUser = addedUsers[0] || targetUsers[0];

    res.json({
      message: `${primaryUser.name} added to the room successfully!`,
      user: {
        id: primaryUser._id,
        name: primaryUser.name,
        email: primaryUser.email,
      },
    });

  } catch (error) {
    console.error('inviteUser error:', error);
    res.status(500).json({ error: error.message || 'Failed to add user' });
  }
};

module.exports = { createRoom, getMyRooms, getRoomById, joinRoom, leaveRoom, inviteUser };

