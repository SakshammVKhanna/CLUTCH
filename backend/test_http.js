const jwt = require('jsonwebtoken');
require('dotenv').config();

async function testHttp() {
  const token = jwt.sign(
    {
      id: '6ac7a05f640547248f3e2a15',
      name: 'user-test2',
      email: 'test2@gmail.com'
    },
    process.env.JWT_SECRET,
    { expiresIn: '7d' }
  );

  console.log('Testing GET http://localhost:5000/api/rooms');
  try {
    const res = await fetch('http://localhost:5000/api/rooms', {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });
    console.log('Status:', res.status, res.statusText);
    const body = await res.text();
    console.log('Body:', body);
  } catch (err) {
    console.error('Fetch error:', err);
  }
}

testHttp();
