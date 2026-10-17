# NexVoice — Phase 2 Complete ✅

## What Was Built

```
backend/
├── .env                          ← Your secret config (never commit this)
├── .env.example                  ← Template to share with others
├── .gitignore                    ← Tells Git to ignore .env and node_modules
├── package.json                  ← Lists all dependencies
└── src/
    ├── server.js                 ← Main entry point — starts Express
    ├── config/
    │   └── db.js                 ← MongoDB connection
    ├── models/
    │   ├── User.js               ← User schema (name, email, profileImage)
    │   └── Room.js               ← Room schema (name, game, members, etc.)
    ├── controllers/
    │   ├── userController.js     ← Logic for user endpoints
    │   └── roomController.js     ← Logic for room endpoints
    └── routes/
        ├── userRoutes.js         ← URL patterns for /api/users
        └── roomRoutes.js         ← URL patterns for /api/rooms
```

---

## Step 1 — Install Dependencies

Open a terminal inside the `backend` folder and run:

```bash
npm install
```

This downloads Express, Mongoose, dotenv, cors, and nodemon into `node_modules/`.

---

## Step 2 — Set Up the .env File

The `.env` file is already created for you at `backend/.env`:

```
MONGO_URI=mongodb://localhost:27017/nexvoice
PORT=5000
```

**If you want to use MongoDB Atlas (free cloud DB) instead of local:**
1. Go to https://www.mongodb.com/atlas and create a free account
2. Create a cluster → Get the connection string
3. Replace the MONGO_URI line with your Atlas string:
   ```
   MONGO_URI=mongodb+srv://youruser:yourpass@cluster.mongodb.net/nexvoice
   ```

---

## Step 3 — Connect MongoDB

**Option A — Local MongoDB (Easiest to start)**
1. Install MongoDB Community: https://www.mongodb.com/try/download/community
2. After install, MongoDB runs automatically as a service
3. Your `.env` already points to `localhost:27017` — no changes needed

**Option B — MongoDB Atlas (Free cloud, no install)**
1. Sign up at https://cloud.mongodb.com
2. Create a free M0 cluster
3. Add your IP to the allowed list (Network Access → Add IP)
4. Get the connection string and put it in `.env`

---

## Step 4 — Start the Server

```bash
# For development (auto-restarts when you change files)
npm run dev

# For production
npm start
```

You should see:
```
✅ MongoDB connected: localhost
✅ NexVoice server running on http://localhost:5000
```

Open http://localhost:5000 in your browser — you should see:
```json
{
  "message": "🎮 NexVoice API is running!",
  "version": "1.0.0"
}
```

---

## Step 5 — Test with Postman

Download Postman from https://www.postman.com/downloads/

Set the base URL to: `http://localhost:5000`

### USER ENDPOINTS

---

#### 1. Create a User
- **Method:** POST
- **URL:** `http://localhost:5000/api/users`
- **Headers:** `Content-Type: application/json`
- **Body (raw JSON):**
```json
{
  "name": "ProSniper99",
  "email": "pro@example.com"
}
```
- **Expected response (201):**
```json
{
  "message": "User created successfully",
  "user": {
    "_id": "6605f9a2b3c4d5e6f7890123",
    "name": "ProSniper99",
    "email": "pro@example.com",
    "profileImage": null,
    "createdAt": "2024-01-01T10:00:00.000Z"
  }
}
```
> **IMPORTANT:** Copy the `_id` value — you need it for the room tests.

---

#### 2. Get a User by ID
- **Method:** GET
- **URL:** `http://localhost:5000/api/users/<paste-user-id-here>`
- **Expected response (200):**
```json
{
  "user": {
    "_id": "...",
    "name": "ProSniper99",
    "email": "pro@example.com"
  }
}
```

---

### ROOM ENDPOINTS

---

#### 3. Create a Room
- **Method:** POST
- **URL:** `http://localhost:5000/api/rooms`
- **Headers:** `Content-Type: application/json`
- **Body:**
```json
{
  "name": "Ranked Grind Squad",
  "gameName": "Valorant",
  "ownerId": "<paste-user-id-here>",
  "isPublic": true,
  "maxMembers": 8
}
```
- **Expected response (201):**
```json
{
  "message": "Room created successfully",
  "room": {
    "_id": "6605f9b2c3d4e5f6a7890456",
    "name": "Ranked Grind Squad",
    "gameName": "Valorant",
    "owner": { "name": "ProSniper99", ... },
    "members": [{ "name": "ProSniper99", ... }],
    "isPublic": true,
    "maxMembers": 8
  }
}
```
> **IMPORTANT:** Copy the room `_id` — you need it for join/leave tests.

---

#### 4. Get All Public Rooms
- **Method:** GET
- **URL:** `http://localhost:5000/api/rooms`
- **Expected response (200):** List of all public rooms

---

#### 5. Get Room by ID
- **Method:** GET
- **URL:** `http://localhost:5000/api/rooms/<room-id>`
- **Expected response (200):** Full room with members

---

#### 6. Join a Room (Create a second user first!)
- **Method:** POST
- **URL:** `http://localhost:5000/api/rooms/<room-id>/join`
- **Body:**
```json
{
  "userId": "<second-user-id>"
}
```
- **Expected response (200):** Updated room with both users in members

---

#### 7. Leave a Room
- **Method:** POST
- **URL:** `http://localhost:5000/api/rooms/<room-id>/leave`
- **Body:**
```json
{
  "userId": "<user-id>"
}
```
- **Expected response (200):** Updated room with user removed

---

## Error Responses Reference

| Status | Meaning |
|--------|---------|
| 200 | Success |
| 201 | Created |
| 400 | Bad request (missing fields, room full, etc.) |
| 404 | Not found (wrong ID) |
| 409 | Conflict (duplicate email, already in room) |
| 500 | Server error |

---

---

## Phase 4A: Real-Time Room System (Socket.IO) ⚡

Phase 4A introduces real-time WebSockets powered by **Socket.IO** alongside existing REST APIs.

### Socket.IO Architecture & JWT Auth
- **Connection URL:** `ws://localhost:5000` (or `http://localhost:5000`)
- **Authentication:** Requires `auth: { token: "<jwt_token>" }` in the handshake.
- Unauthenticated or expired tokens are rejected with a 401 connection error.

### Socket Events Reference

#### Client → Server
| Event | Payload | Purpose |
|---|---|---|
| `join_room` | `{ "roomId": "..." }` | Joins a room, triggers `room_state` for user and broadcasts `user_joined` |
| `leave_room` | `{ "roomId": "..." }` | Leaves a room, broadcasts `user_left` |
| `mute_changed` | `{ "roomId": "...", "isMuted": true/false }` | Toggles mic state and broadcasts to room |

#### Server → Client
| Event | Payload | Purpose |
|---|---|---|
| `room_state` | `{ "roomId": "...", "members": [...] }` | Initial list of active room members sent to the joiner |
| `user_joined` | `{ "roomId": "...", "user": { "userId", "name", "isMuted" } }` | Broadcast to others when a member enters |
| `user_left` | `{ "roomId": "...", "userId": "..." }` | Broadcast to others when a member leaves |
| `user_mute_changed`| `{ "roomId": "...", "userId": "...", "isMuted": bool }` | Broadcast when a member mutes/unmutes |
| `user_disconnected`| `{ "roomId": "...", "userId": "..." }` | Broadcast on sudden connection drop / crash |

---

## Phase 4B-2: LiveKit Voice (SFU media layer) 🎙️

Socket.IO stays for presence (join/leave/mute badges/member list). Real voice
goes through LiveKit SFU — no audio ever travels over Socket.IO.

### LiveKit server
- **Local dev:** `livekit-server --dev --bind 0.0.0.0`
  (or `docker run --rm -p 7880:7880 -p 7881:7881 -p 7882:7882/udp livekit/livekit-server --dev --bind 0.0.0.0`)
  Dev creds: key `devkey`, secret `secret`.
- **Cloud:** `wss://<project>.livekit.cloud` + its API key/secret.

### Env (.env)
```
LIVEKIT_URL=ws://10.0.2.2:7880          # must be reachable FROM the device!
LIVEKIT_API_KEY=devkey                   # emulator → 10.0.2.2; physical phone → ws://<PC-LAN-IP>:7880
LIVEKIT_API_SECRET=secret
```

### Token endpoint (protected by NexVoice JWT)
- **POST** `/api/livekit/token`, header `Authorization: Bearer <jwt>`, body `{ "roomId": "<nexvoice-id>" }`
- Verifies: room exists → requester is owner/member → signs 15-min LiveKit token
  (identity = NexVoice user ID, grants: roomJoin + publish + subscribe).
- LiveKit room name = `nexvoice_<roomId>` (see `src/realtime/livekitRoom.js`).
- The API secret never leaves the server.

### 2-user voice test (after real `LIVEKIT_API_SECRET` is set)
1. `npm run dev` (backend) + LiveKit server running.
2. Device A: login → create room → enter (grant mic) → "Voice connected" dot.
3. Device B: login → REST-join room (or via invite) → enter → "Voice connected".
4. A speaks → B hears; B speaks → A hears. Mute → remote stops hearing + badge syncs.
5. Leave → mic unpublishes, LiveKit disconnects; rejoin works without restart.

