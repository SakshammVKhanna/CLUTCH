/**
 * controllers/userController.js
 *
 * Controllers contain the actual logic for each API endpoint.
 * When a request comes in, Express calls the right controller function.
 *
 * Each function follows this pattern:
 *   1. Read data from the request (req.body, req.params)
 *   2. Do something with the database (find, create, update, delete)
 *   3. Send a response back (res.json, res.status)
 *
 * We use try/catch to handle errors — if anything goes wrong,
 * we catch it and send a helpful error message back.
 */

const User = require('../models/User');

// ─────────────────────────────────────────────────────────────────────────────
// POST /api/users
// Creates a new user in the database.
//
// Request body (JSON):
// {
//   "name": "ProSniper99",
//   "email": "pro@example.com",
//   "profileImage": "https://..." (optional)
// }
// ─────────────────────────────────────────────────────────────────────────────
const createUser = async (req, res) => {
  try {
    const { name, email, profileImage } = req.body;

    // Basic validation — check required fields are present
    if (!name || !email) {
      return res.status(400).json({ error: 'Name and email are required' });
    }

    // Check if a user with this email already exists
    const existingUser = await User.findOne({ email: email.toLowerCase() });
    if (existingUser) {
      return res.status(409).json({ error: 'A user with this email already exists' });
    }

    // Create and save the new user
    const user = await User.create({ name, email, profileImage });

    // 201 = "Created" — standard status code for successful resource creation
    res.status(201).json({
      message: 'User created successfully',
      user,
    });

  } catch (error) {
    // Mongoose validation errors (e.g., invalid email format) come here
    if (error.name === 'ValidationError') {
      const messages = Object.values(error.errors).map(e => e.message);
      return res.status(400).json({ error: messages.join('. ') });
    }
    console.error('createUser error:', error);
    res.status(500).json({ error: 'Failed to create user' });
  }
};

// ─────────────────────────────────────────────────────────────────────────────
// GET /api/users/:id
// Finds a user by their MongoDB ID.
//
// Example: GET /api/users/6605f9a2b3c4d5e6f7890123
// ─────────────────────────────────────────────────────────────────────────────
const getUserById = async (req, res) => {
  try {
    // req.params.id comes from the :id in the route URL
    const user = await User.findById(req.params.id);

    if (!user) {
      return res.status(404).json({ error: 'User not found' });
    }

    res.json({ user });

  } catch (error) {
    // CastError means the ID format is wrong (not a valid MongoDB ObjectId)
    if (error.name === 'CastError') {
      return res.status(400).json({ error: 'Invalid user ID format' });
    }
    console.error('getUserById error:', error);
    res.status(500).json({ error: 'Failed to get user' });
  }
};

module.exports = { createUser, getUserById };
