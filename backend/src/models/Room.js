/**
 * models/Room.js
 *
 * Blueprint for a voice room document in MongoDB.
 *
 * A room has:
 *   - name        → display name (e.g., "Ranked Grind Squad")
 *   - gameName    → which game (e.g., "Valorant")
 *   - owner       → reference to the User who created this room
 *   - members     → array of User references currently in the room
 *   - createdAt   → auto set by timestamps: true
 *
 * NOTE: Public rooms removed — all rooms are now invite-only / personal.
 * NOTE: No member cap — rooms hold unlimited members (Issue 1 resolved).
 *
 * "ref: 'User'" tells Mongoose this field links to a User document.
 * This is called a "reference" — like a foreign key in SQL.
 * Later you can use .populate('owner') to fetch the full User object.
 */

const mongoose = require('mongoose');

const roomSchema = new mongoose.Schema(
  {
    name: {
      type: String,
      required: [true, 'Room name is required'],
      trim: true,
      minlength: [2, 'Room name must be at least 2 characters'],
      maxlength: [60, 'Room name cannot exceed 60 characters'],
    },

    gameName: {
      type: String,
      required: [true, 'Game name is required'],
      trim: true,
      maxlength: [60, 'Game name cannot exceed 60 characters'],
    },

    // Which user created this room
    owner: {
      type: mongoose.Schema.Types.ObjectId,   // MongoDB document ID
      ref: 'User',                             // Links to the User model
      required: [true, 'Room must have an owner'],
    },

    // All rooms are invite-only now — no public flag.

    // No maxMembers — unlimited members per room (Issue 1).

    // Array of User IDs currently in the room
    members: [
      {
        type: mongoose.Schema.Types.ObjectId,
        ref: 'User',
      }
    ],
  },
  {
    timestamps: true,   // Auto-adds createdAt and updatedAt
  }
);

// Virtual field: memberCount
// A "virtual" is a computed property — not stored in the DB,
// but available when you access room.memberCount in your code.
roomSchema.virtual('memberCount').get(function () {
  return this.members.length;
});

module.exports = mongoose.model('Room', roomSchema);
