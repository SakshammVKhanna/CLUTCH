/**
 * controllers/authController.js
 *
 * Handles all authentication logic:
 *   signup → create account + return JWT
 *   login  → verify credentials + return JWT
 *   me     → return current user info from token
 *
 * bcrypt: a library that converts a plain password into a safe hash.
 *   - bcrypt.hash("mypassword", 10) → "$2b$10$..."   (one-way, can't be reversed)
 *   - bcrypt.compare("mypassword", "$2b$10$...") → true/false
 *
 * jwt: creates and verifies signed tokens.
 *   - jwt.sign({ id: user._id }, secret, { expiresIn: '7d' }) → "eyJhbGc..."
 *   - jwt.verify("eyJhbGc...", secret) → { id: "...", name: "...", ... }
 */

const bcrypt = require('bcryptjs');
const jwt    = require('jsonwebtoken');
const User   = require('../models/User');

// Helper: creates a JWT token for a user
// We put name + email in the token payload so we can read them without a DB query
const generateToken = (user) => {
  return jwt.sign(
    {
      id:    user._id,
      name:  user.name,
      email: user.email,
    },
    process.env.JWT_SECRET,
    { expiresIn: '7d' }   // Token expires in 7 days — user must log in again after that
  );
};

// Helper: returns user object without the password hash
const safeUser = (user) => ({
  id:           user._id,
  name:         user.name,
  email:        user.email,
  profileImage: user.profileImage,
  createdAt:    user.createdAt,
});

// ─────────────────────────────────────────────────────────────────────────────
// POST /api/auth/signup
// Creates a new account.
//
// Request body:
// { "name": "ProSniper99", "email": "pro@example.com", "password": "secret123" }
// ─────────────────────────────────────────────────────────────────────────────
const signup = async (req, res) => {
  try {
    const { name, email, password } = req.body;

    // Validate all fields are present
    if (!name || !email || !password) {
      return res.status(400).json({ error: 'Name, email, and password are required' });
    }

    if (password.length < 6) {
      return res.status(400).json({ error: 'Password must be at least 6 characters' });
    }

    // Check if email is already taken
    const existingUser = await User.findOne({ email: email.toLowerCase() });
    if (existingUser) {
      return res.status(409).json({ error: 'An account with this email already exists' });
    }

    // Hash the password — the "10" is the salt rounds (how much work bcrypt does)
    // More rounds = more secure but slower. 10 is the standard safe value.
    const passwordHash = await bcrypt.hash(password, 10);

    // Save the new user
    const user = await User.create({ name, email, passwordHash });

    // Create a JWT for the new user so they're immediately logged in
    const token = generateToken(user);

    res.status(201).json({
      message: 'Account created successfully! Welcome to NexVoice.',
      token,
      user: safeUser(user),
    });

  } catch (error) {
    if (error.name === 'ValidationError') {
      const messages = Object.values(error.errors).map(e => e.message);
      return res.status(400).json({ error: messages.join('. ') });
    }
    console.error('signup error:', error);
    res.status(500).json({ error: 'Failed to create account' });
  }
};

// ─────────────────────────────────────────────────────────────────────────────
// POST /api/auth/login
// Logs in an existing user.
//
// Request body:
// { "email": "pro@example.com", "password": "secret123" }
// ─────────────────────────────────────────────────────────────────────────────
const login = async (req, res) => {
  try {
    const { email, password } = req.body;

    if (!email || !password) {
      return res.status(400).json({ error: 'Email and password are required' });
    }

    // Find user by email
    const user = await User.findOne({ email: email.toLowerCase() });
    if (!user) {
      // Use a vague message for security — don't reveal whether the email exists
      return res.status(401).json({ error: 'Invalid email or password' });
    }

    // Compare the provided password against the stored hash
    const isMatch = await bcrypt.compare(password, user.passwordHash);
    if (!isMatch) {
      return res.status(401).json({ error: 'Invalid email or password' });
    }

    // Password is correct — generate a token
    const token = generateToken(user);

    res.json({
      message: 'Welcome back!',
      token,
      user: safeUser(user),
    });

  } catch (error) {
    console.error('login error:', error);
    res.status(500).json({ error: 'Login failed' });
  }
};

// ─────────────────────────────────────────────────────────────────────────────
// GET /api/auth/me
// Returns the currently logged-in user's info.
// Requires: Authorization: Bearer <token>
//
// The authMiddleware already verified the token and put the user ID in req.user.id
// ─────────────────────────────────────────────────────────────────────────────
const getMe = async (req, res) => {
  try {
    // req.user is set by the protect middleware in authMiddleware.js
    const user = await User.findById(req.user.id).select('-passwordHash');
    if (!user) {
      return res.status(404).json({ error: 'User not found' });
    }
    res.json({ user: safeUser(user) });

  } catch (error) {
    console.error('getMe error:', error);
    res.status(500).json({ error: 'Failed to get user info' });
  }
};

module.exports = { signup, login, getMe };
