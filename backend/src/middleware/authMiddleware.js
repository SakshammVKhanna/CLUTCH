/**
 * middleware/authMiddleware.js
 *
 * This middleware protects routes that require the user to be logged in.
 *
 * How JWT works (simple explanation):
 *   1. User logs in → server creates a JWT token (a signed string)
 *   2. App stores that token
 *   3. On every protected request, app sends: "Authorization: Bearer <token>"
 *   4. This middleware reads the token, verifies it, and puts the user ID in req.user
 *   5. The route handler can then use req.user.id to know who is making the request
 *
 * If the token is missing or wrong → request is rejected with 401 Unauthorized.
 */

const jwt = require('jsonwebtoken');

const protect = (req, res, next) => {
  // Read the Authorization header: "Bearer eyJhbGc..."
  const authHeader = req.headers['authorization'];

  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Not authorized. Please log in.' });
  }

  // Extract just the token part (after "Bearer ")
  const token = authHeader.split(' ')[1];

  try {
    // jwt.verify checks the token's signature using our secret key
    // If it's valid, it returns the payload we put in during login (userId, name, email)
    const decoded = jwt.verify(token, process.env.JWT_SECRET);

    // Attach the decoded user info to the request so route handlers can use it
    req.user = decoded;

    // Call next() to pass control to the actual route handler
    next();

  } catch (error) {
    // Token is expired or tampered with
    return res.status(401).json({ error: 'Token is invalid or expired. Please log in again.' });
  }
};

module.exports = { protect };
