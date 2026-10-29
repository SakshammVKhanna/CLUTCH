/**
 * config/db.js
 *
 * Handles the MongoDB connection.
 * We keep this in its own file so server.js stays clean.
 *
 * Mongoose is a library that makes it easy to talk to MongoDB.
 * Instead of writing raw database queries, you define "models"
 * (like blueprints) and Mongoose handles the rest.
 *
 * The MONGO_URI comes from your .env file.
 * Example URIs:
 *   Local:  mongodb://localhost:27017/nexvoice
 *   Cloud:  mongodb+srv://username:password@cluster.mongodb.net/nexvoice
 */

const mongoose = require('mongoose');
const dns = require('dns');

// Fix for systems where default DNS (127.0.0.1) blocks SRV lookup.
// Forces MongoDB SRV resolution via Google + Cloudflare DNS.
try {
  dns.setServers(['8.8.8.8', '1.1.1.1']);
} catch (e) {
  // ignore — fall back to system DNS
}

const connectDB = async () => {
  try {
    if (!process.env.MONGO_URI) {
      throw new Error('MONGO_URI is missing from backend/.env');
    }
    const conn = await mongoose.connect(process.env.MONGO_URI);
    console.log(`✅ MongoDB connected: ${conn.connection.host}`);
  } catch (error) {
    // Do not kill the process. Auth/rooms need MongoDB, but the HTTP
    // server must stay up so the phone can reach health checks and
    // LiveKit routes, and so nodemon does not crash-loop on a bad URI.
    console.error(`❌ MongoDB connection failed: ${error.message}`);
    console.error('⚠️  Server will keep running. Fix MONGO_URI in backend/.env and restart.');
    console.error('    Atlas: reset the database user password and paste the new connection string.');
  }
};

module.exports = connectDB;
