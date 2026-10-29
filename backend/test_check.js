const mongoose = require('mongoose');
const dns = require('dns');
require('dotenv').config();

try {
  dns.setServers(['8.8.8.8', '1.1.1.1']);
} catch (e) {}

const Room = require('./src/models/Room');

const User = require('./src/models/User');

async function checkRooms() {
  try {
    await mongoose.connect(process.env.MONGO_URI);
    const users = await User.find({}).lean();
    console.log('=== USERS ===');
    for (const u of users) {
      console.log(`User: ${u._id} | name: "${u.name}" | email: "${u.email}"`);
    }

    const sakshamUsers = await User.find({ name: /^saksham+$/i });
    console.log(`Found ${sakshamUsers.length} Saksham accounts.`);
    const roomFF = await Room.findById('6ad4d1803fc82d9beea689a1');
    if (roomFF) {
      for (const su of sakshamUsers) {
        if (!roomFF.members.some(m => m.toString() === su._id.toString())) {
          roomFF.members.push(su._id);
          console.log(`Added user ${su.name} (${su.email}) to Room FF`);
        }
      }
      await roomFF.save();
      console.log('Room FF now has members:', roomFF.members);
    }

  } catch (e) {
    console.error('Error:', e);
  } finally {
    await mongoose.disconnect();
  }
}

checkRooms();
