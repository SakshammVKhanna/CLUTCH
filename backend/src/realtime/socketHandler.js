/**
 * realtime/socketHandler.js
 *
 * Phase 4A: Real-Time Room System with Socket.IO
 *
 * This file manages:
 *   1. JWT Authentication for incoming socket connections
 *   2. In-memory presence tracking (who is currently connected in which room)
 *   3. Room join / leave events
 *   4. Mic mute state broadcast
 *   5. Automatic cleanup on unexpected disconnect
 *
 * Note: MongoDB remains the source of truth for persistent room/user data.
 * Socket.IO handles the temporary real-time live session.
 */

const { Server } = require('socket.io');
const jwt = require('jsonwebtoken');
const Room = require('../models/Room');

// In-memory real-time state:
// Map<roomId, Map<userId, { userId, name, isMuted, socketId }>>
const roomMembers = new Map();

// Quick lookup: Map<socketId, { roomId, userId, name }>
const socketToRoom = new Map();

/**
 * Initializes Socket.IO with the HTTP server and sets up event listeners.
 */
const initSocket = (httpServer) => {
  const io = new Server(httpServer, {
    cors: {
      origin: '*', // Allow connections from Android emulator & devices
      methods: ['GET', 'POST']
    }
  });

  // ── 1. Authentication Middleware ───────────────────────────────────────────
  // Every socket connection must provide a valid JWT token.
  io.use((socket, next) => {
    try {
      // Token can be sent in socket.handshake.auth.token or headers
      const token =
        socket.handshake.auth?.token ||
        socket.handshake.headers?.authorization?.replace('Bearer ', '');

      if (!token) {
        return next(new Error('Authentication error: No token provided'));
      }

      // Verify the JWT signature using the secret key
      const decoded = jwt.verify(token, process.env.JWT_SECRET);

      // Attach user details to socket instance
      socket.user = {
        id: decoded.id.toString(),
        name: decoded.name,
        email: decoded.email
      };

      next();
    } catch (err) {
      console.error('Socket authentication failed:', err.message);
      return next(new Error('Authentication error: Invalid or expired token'));
    }
  });

  // ── 2. Connection Handler ──────────────────────────────────────────────────
  io.on('connection', (socket) => {
    console.log(`🔌 [Socket] Connected: ${socket.user.name} (${socket.user.id}) | Socket ID: ${socket.id}`);

    // Helper to safely remove user from a room
    const leaveCurrentRoom = (roomIdOverride) => {
      const info = socketToRoom.get(socket.id);
      const roomId = roomIdOverride || info?.roomId;

      if (!roomId) return;

      socket.leave(roomId);
      socketToRoom.delete(socket.id);

      if (roomMembers.has(roomId)) {
        const membersMap = roomMembers.get(roomId);
        const userId = socket.user.id;

        if (membersMap.has(userId)) {
          membersMap.delete(userId);
          console.log(`👋 [Socket] User ${socket.user.name} left room ${roomId}`);

          // Broadcast to remaining room members
          socket.to(roomId).emit('user_left', {
            roomId,
            userId
          });
        }

        // Clean up empty room map from memory
        if (membersMap.size === 0) {
          roomMembers.delete(roomId);
        }
      }
    };

    // ── EVENT: join_room ───────────────────────────────────────────────────────
    // Payload: { roomId: string }
    socket.on('join_room', async (data) => {
      try {
        const roomId = data?.roomId;
        if (!roomId) {
          return socket.emit('error', { message: 'roomId is required' });
        }

        // Verify room exists in MongoDB
        const room = await Room.findById(roomId);
        if (!room) {
          return socket.emit('error', { message: 'Room not found' });
        }

        // If this socket was already in another room, leave it first
        const previousInfo = socketToRoom.get(socket.id);
        if (previousInfo && previousInfo.roomId !== roomId) {
          leaveCurrentRoom(previousInfo.roomId);
        }

        // Join Socket.IO room
        socket.join(roomId);

        // Initialize room map if first user
        if (!roomMembers.has(roomId)) {
          roomMembers.set(roomId, new Map());
        }

        const membersMap = roomMembers.get(roomId);

        // Real-time member representation
        const memberData = {
          userId: socket.user.id,
          name: socket.user.name,
          isMuted: false, // Default unmuted when joining
          socketId: socket.id
        };

        // Add or update this member in the active room
        membersMap.set(socket.user.id, memberData);
        socketToRoom.set(socket.id, {
          roomId,
          userId: socket.user.id,
          name: socket.user.name
        });

        console.log(`✅ [Socket] ${socket.user.name} joined room: ${roomId} (Total in room: ${membersMap.size})`);

        // Send current room members list to the newly joined user
        const currentMembers = Array.from(membersMap.values()).map(m => ({
          userId: m.userId,
          name: m.name,
          isMuted: m.isMuted
        }));

        socket.emit('room_state', {
          roomId,
          members: currentMembers
        });

        // Notify all OTHER users in this room that a new user joined
        socket.to(roomId).emit('user_joined', {
          roomId,
          user: {
            userId: memberData.userId,
            name: memberData.name,
            isMuted: memberData.isMuted
          }
        });

      } catch (err) {
        console.error('Error in join_room:', err);
        socket.emit('error', { message: 'Failed to join room' });
      }
    });

    // ── EVENT: mute_changed ───────────────────────────────────────────────────
    // Payload: { roomId?: string, isMuted: boolean }
    socket.on('mute_changed', (data) => {
      try {
        const info = socketToRoom.get(socket.id);
        const roomId = data?.roomId || info?.roomId;
        const isMuted = !!data?.isMuted;

        if (!roomId || !roomMembers.has(roomId)) return;

        const membersMap = roomMembers.get(roomId);
        const member = membersMap.get(socket.user.id);

        if (member) {
          member.isMuted = isMuted;

          console.log(`🎙️ [Socket] ${socket.user.name} changed mute to: ${isMuted} in room ${roomId}`);

          // Broadcast to everyone else in the room
          socket.to(roomId).emit('user_mute_changed', {
            roomId,
            userId: socket.user.id,
            isMuted
          });
        }
      } catch (err) {
        console.error('Error in mute_changed:', err);
      }
    });

    // ── EVENT: leave_room ──────────────────────────────────────────────────────
    // Payload: { roomId?: string }
    socket.on('leave_room', (data) => {
      try {
        const roomId = data?.roomId;
        leaveCurrentRoom(roomId);
      } catch (err) {
        console.error('Error in leave_room:', err);
      }
    });

    // ── EVENT: disconnect ─────────────────────────────────────────────────────
    // Fired automatically when connection drops or app closes
    socket.on('disconnect', (reason) => {
      console.log(`❌ [Socket] Disconnected: ${socket.user.name} (${reason})`);

      const info = socketToRoom.get(socket.id);
      if (info) {
        const { roomId, userId } = info;

        if (roomMembers.has(roomId)) {
          const membersMap = roomMembers.get(roomId);
          membersMap.delete(userId);

          // Inform remaining members
          socket.to(roomId).emit('user_left', {
            roomId,
            userId
          });
          socket.to(roomId).emit('user_disconnected', {
            roomId,
            userId
          });

          if (membersMap.size === 0) {
            roomMembers.delete(roomId);
          }
        }

        socketToRoom.delete(socket.id);
      }
    });
  });

  return io;
};

module.exports = { initSocket };
