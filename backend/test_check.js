const mongoose = require('mongoose');
require('dotenv').config();
const Room = require('./src/models/Room');

async function checkRooms() {
  try {
    await mongoose.connect(process.env.MONGO_URI);
    const rooms = await Room.find({}).lean();
    console.log(`Found ${rooms.length} rooms in DB:`);
    for (const r of rooms) {
      console.log('Room:', {
        id: r._id,
        name: r.name,
        gameName: r.gameName,
        owner: r.owner,
        members: r.members
      });
    }
  } catch (e) {
    console.error('Error:', e);
  } finally {
    await mongoose.disconnect();
  }
}

checkRooms();
