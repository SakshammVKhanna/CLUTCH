/**
 * routes/userRoutes.js
 *
 * Defines the URL patterns for user-related endpoints.
 *
 * How routes work:
 *   router.post('/') means POST /api/users   (because server.js mounts it at /api/users)
 *   router.get('/:id') means GET /api/users/:id
 *
 * The router just maps URLs to controller functions.
 * The actual logic lives in userController.js.
 */

const express = require('express');
const router  = express.Router();

const { createUser, getUserById } = require('../controllers/userController');

// POST /api/users      → create a new user
router.post('/', createUser);

// GET  /api/users/:id  → get a user by ID
router.get('/:id', getUserById);

module.exports = router;
