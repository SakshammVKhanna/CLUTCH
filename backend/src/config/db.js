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
    const conn = await mongoose.connect(process.env.MONGO_URI);
    console.log(`✅ MongoDB connected: ${conn.connection.host}`);
  } catch (error) {
    console.error(`❌ MongoDB connection failed: ${error.message}`);
    // Exit the process — app can't run without the database
    process.exit(1);
  }
};

module.exports = connectDB;
