/**
 * routes/authRoutes.js
 *
 * Maps the 3 auth endpoints to their controller functions.
 *
 * POST /api/auth/signup → create account
 * POST /api/auth/login  → log in
 * GET  /api/auth/me     → get current user (requires token)
 */

const express  = require('express');
const router   = express.Router();
const { signup, login, getMe } = require('../controllers/authController');
const { protect } = require('../middleware/authMiddleware');

router.post('/signup', signup);
router.post('/login',  login);
router.get('/me', protect, getMe);   // protect runs before getMe — checks token first

module.exports = router;
