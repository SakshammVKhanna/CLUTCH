/**
 * server.js
 *
 * This is the main entry point of the backend.
 * It does four things:
 *   1. Loads environment variables from the .env file
 *   2. Connects to MongoDB
 *   3. Sets up the Express app with middleware
 *   4. Starts listening for incoming requests
 *
 * Think of Express as the "waiter" that receives requests
 * from the Android app and sends back responses.
 */

const express = require('express');
const http    = require('http');
const cors    = require('cors');
const dotenv  = require('dotenv');

const connectDB    = require('./config/db');
const authRoutes   = require('./routes/authRoutes');
const userRoutes   = require('./routes/userRoutes');
const roomRoutes   = require('./routes/roomRoutes');
const livekitRoutes = require('./routes/livekitRoutes');
const { initSocket } = require('./realtime/socketHandler');

// Load variables from .env file into process.env
dotenv.config();

// Create the Express app
const app = express();

// ── Middleware ────────────────────────────────────────────────────────────────
// Middleware runs on EVERY request before it reaches your route handlers.

// CORS: Allows the Android app (and Postman) to talk to this server.
// Without this, browsers/apps block cross-origin requests.
app.use(cors());

// JSON parser: Lets us read JSON from request bodies (e.g., POST /api/users)
app.use(express.json());

// ── Database Connection ───────────────────────────────────────────────────────
connectDB();

// ── Routes ───────────────────────────────────────────────────────────────────
// These tell Express which file handles each URL prefix.
// All user-related URLs start with /api/users
// All room-related URLs start with /api/rooms
app.use('/api/auth',  authRoutes);   // Phase 3: signup, login, me
app.use('/api/users', userRoutes);
app.use('/api/rooms', roomRoutes);
app.use('/api/livekit', livekitRoutes); // Phase 4B-2: LiveKit voice tokens (JWT required)

// ── Health Check ─────────────────────────────────────────────────────────────
// A simple endpoint to confirm the server is running.
// Open http://localhost:5000/ in your browser — you should see the JSON response.
app.get('/', (req, res) => {
  res.json({
    message: '🎮 NexVoice API is running!',
    version: '1.0.0',
    phase:   'Phase 3',
    endpoints: {
      auth:  '/api/auth',
      users: '/api/users',
      rooms: '/api/rooms',
    }
  });
});

// ── Global Error Handler ──────────────────────────────────────────────────────
// If any route throws an error, it ends up here.
// The four parameters (err, req, res, next) tell Express this is an error handler.
app.use((err, req, res, next) => {
  console.error('Server error:', err.message);
  res.status(500).json({ error: 'Something went wrong on the server.' });
});

// ── Socket.IO Real-Time Server ────────────────────────────────────────────────
// We wrap the Express app with Node's native HTTP server
// so both REST endpoints and Socket.IO connections run on the same port.
const server = http.createServer(app);
initSocket(server);

// ── Start Server ──────────────────────────────────────────────────────────────
const PORT = process.env.PORT || 5000;
server.listen(PORT, '0.0.0.0', () => {
  console.log(`✅ NexVoice server running on http://0.0.0.0:${PORT}`);
  console.log(`📱 Phone access: http://172.20.44.113:${PORT}`);
  console.log(`⚡ Real-time Socket.IO initialized on port ${PORT}`);
  console.log(`🎙️ LiveKit URL configured: ${process.env.LIVEKIT_URL}`);
});

