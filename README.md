<div align="center">

# 🎮 CLUTCH

**Clutch the win. Own the voice.**

A high-performance, low-latency squad voice chat and lobby application engineered for competitive gamers. Built with Jetpack Compose on Android, Node.js + Socket.IO on the backend, and LiveKit WebRTC SFU for crystal-clear real-time voice communications.

[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin%201.9-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Node.js](https://img.shields.io/badge/Backend-Node.js%20%7C%20Express-339933?style=for-the-badge&logo=nodedotjs&logoColor=white)](https://nodejs.org/)
[![Socket.IO](https://img.shields.io/badge/Realtime-Socket.IO-010101?style=for-the-badge&logo=socketdotio&logoColor=white)](https://socket.io/)
[![LiveKit](https://img.shields.io/badge/Voice%20SFU-LiveKit%20WebRTC-00E5FF?style=for-the-badge&logo=webrtc&logoColor=black)](https://livekit.io/)
[![MongoDB](https://img.shields.io/badge/Database-MongoDB-47A248?style=for-the-badge&logo=mongodb&logoColor=white)](https://www.mongodb.com/)

[Features](#-key-features) • [Architecture](#-architecture) • [Tech Stack](#-tech-stack) • [Quick Start](#-quick-start) • [Socket & Audio Protocol](#-real-time-protocols)

</div>

---

## ⚡ Highlights

- **Ultra Low-Latency Voice:** Powered by LiveKit's WebRTC Selective Forwarding Unit (SFU) for sub-second lag during intense ranked matches.
- **Instant Presence & Mute Badges:** Full WebSocket state synchronization with Socket.IO so teammates' mute states and speaking rings update in real-time.
- **Invite-Only Squad Lobbies:** Clean lobby model with 1-tap room code copying, shareable deep links (`nexvoice://room/<id>`), and in-room direct user invites.
- **Dynamic Room Discovery:** Pull-to-refresh and lifecycle-aware auto-resumption so invites sent from other devices appear immediately.
- **Modern Whitish UI:** Sleek, minimal light theme with emerald gaming accents, animated voice waves, and intuitive one-thumb dock controls.
- **Security by Design:** Full JWT-authenticated API endpoints and socket handshakes — users can never spoof IDs or enter unauthorized rooms.

---

## 🚀 Key Features

### 🎙️ Crystal-Clear Squad Voice
- Independent media layer using **LiveKit Android SDK** (prefixed WebRTC engine).
- Real hardware microphone capture and mute controls.
- Earpiece / Loudspeaker audio routing via Android `AudioManager`.
- Visual glowing audio indicators that pulse dynamically when a player speaks.

### 👥 Squad Management & Presence
- Solo lobby radar animations when waiting for your team.
- Direct member invitation by username or email.
- Copyable room codes & system share sheet integration (`WhatsApp`, `Discord`, etc.).
- Active member grid highlighting host badges (`👑 HOST`) and self tags (`You`).

### 📱 Gamer Profile & Customization
- Dedicated gamer profile screen with customizable display name, `@handle`, email, and squad bio.
- Quick audio preference toggles (Noise Suppression & Low-Latency Squad Mode).
- Secure token persistence using modern Jetpack DataStore Preferences.

### 🔄 Multi-Device Seamless Sync
- Pull-down-to-refresh on Home Screen for immediate squad discovery.
- `ON_RESUME` lifecycle observers ensure rooms added while the app was backgrounded are loaded instantly.
- Smart auto-reconnection and session restoration if connectivity drops.

---

## 🏗️ Architecture

CLUTCH decouples **presence & state** from **audio media transport** to guarantee reliability:

```
┌─────────────────────────────────────────────────────────────────┐
│                   CLUTCH Android Client                         │
│   (Jetpack Compose UI • StateFlow • Retrofit • OkHttp)          │
└───────────────────────┬───────────────────────────┬─────────────┘
                        │                           │
          REST APIs & JWT Handshake           Low-Latency Media
         Socket.IO Presence & Badges            (SFU Audio)
                        │                           │
                        ▼                           ▼
       ┌─────────────────────────────────┐   ┌───────────────┐
       │     Node.js + Express API       │   │  LiveKit SFU  │
       │   Socket.IO Real-Time Server    │   │ Voice Server  │
       └────────────────┬────────────────┘   └───────────────┘
                        │
                        ▼
               ┌─────────────────┐
               │  MongoDB Atlas  │
               └─────────────────┘
```

1. **REST API:** Handles user authentication, profile data, room creation, and membership.
2. **Socket.IO Layer:** Manages room presence, broadcasts member join/leave events, and synchronizes microphone mute states.
3. **LiveKit SFU:** Ingests local audio tracks from speaking clients and selectively broadcasts audio to room peers with minimal bandwidth consumption.
4. **Token Minting:** Backend signs short-lived (15-min) LiveKit access tokens verified against room membership and user identity.

---

## 🛠️ Tech Stack

### Android Client
| Category | Technology |
|---|---|
| **Language** | Kotlin 1.9.22 (Coroutines, Flow, StateFlow) |
| **UI Framework** | Jetpack Compose (Material Design 3 + Material 2 pull-refresh) |
| **Architecture** | MVVM (Model-View-ViewModel) + Single Activity NavHost |
| **Networking** | Retrofit 2.9, OkHttp 4.12, Gson Converter |
| **WebSockets** | Socket.IO Client Java 2.1.1 |
| **Voice / WebRTC** | LiveKit Android SDK 2.29.0 |
| **Local Storage** | Jetpack DataStore Preferences |
| **Build System** | Gradle 8.11, JDK 17, Android SDK 34 (Android 8.0+) |

### Backend Server
| Category | Technology |
|---|---|
| **Runtime** | Node.js (v18+) with Express 4 |
| **Database** | MongoDB with Mongoose ODM |
| **Authentication** | JSON Web Tokens (JWT) + bcryptjs password hashing |
| **Realtime** | Socket.IO 4.x with custom JWT handshake middleware |
| **SFU Voice** | LiveKit Server SDK (`livekit-server-sdk`) |
| **Security** | CORS protection, sanitized queries, input validation |

---

## 📂 Repository Structure

```
CLUTCH/
├── android/                         # Android Studio Application Project
│   ├── app/
│   │   ├── src/main/java/com/nexvoice/app/
│   │   │   ├── auth/                # DataStore token management & repo
│   │   │   ├── models/              # Room, User, RoomMember models
│   │   │   ├── network/             # Retrofit ApiService, DTOs & Repositories
│   │   │   ├── realtime/            # SocketManager & LiveKitManager
│   │   │   ├── ui/
│   │   │   │   ├── components/      # Reusable UI banners & dialogs
│   │   │   │   ├── navigation/      # Jetpack Compose NavHost & routes
│   │   │   │   ├── screens/         # Auth, Home, CreateRoom, VoiceRoom, Profile, Splash
│   │   │   │   └── theme/           # Color.kt, Theme.kt, Type.kt (Whitish theme)
│   │   │   └── utils/               # Deep links & clipboard utilities
│   │   └── build.gradle             # App-level dependencies & Compose config
│   ├── gradle/wrapper/              # Gradle 8.11 distribution wrapper
│   └── build.gradle                 # Project build config
│
├── backend/                         # Node.js + Express API & Real-time Server
│   ├── src/
│   │   ├── config/                  # MongoDB database connection
│   │   ├── controllers/             # Auth, Room, User & LiveKit controllers
│   │   ├── middleware/              # JWT auth protection middleware
│   │   ├── models/                  # Mongoose Schemas (User, Room)
│   │   ├── realtime/                # Socket.IO event handler & LiveKit room logic
│   │   ├── routes/                  # Express API route declarations
│   │   └── server.js                # Server entry point
│   ├── package.json
│   └── .env.example                 # Environment configuration template
│
├── tools/                           # Developer productivity scripts
│   ├── phone-usb-setup.bat          # 1-click ADB reverse proxy for physical devices
│   └── phone-usb-setup.ps1
└── README.md
```

---

## 🏁 Quick Start

### 1. Prerequisites
- **Android Studio** (Hedgehog 2023.1.1+ or Ladybug)
- **JDK 17** (e.g. `jbr-17` or Eclipse Adoptium Temurin 17)
- **Node.js 18+** & **npm**
- **MongoDB** running locally (`localhost:27017`) or a free [MongoDB Atlas](https://www.mongodb.com/atlas) cluster
- **Android Device** with USB Debugging enabled OR Android Emulator (API 34 recommended)

---

### 2. Backend Setup

```bash
# 1. Navigate to backend directory
cd backend

# 2. Install dependencies
npm install

# 3. Create .env configuration
cp .env.example .env
```

Edit `.env` with your values:
```env
PORT=5000
MONGO_URI=mongodb://localhost:27017/clutch
JWT_SECRET=your_super_secret_jwt_key_here

# LiveKit SFU Config (Optional for presence, required for voice)
LIVEKIT_URL=ws://127.0.0.1:7880
LIVEKIT_API_KEY=devkey
LIVEKIT_API_SECRET=secret
```

Start the development server:
```bash
npm run dev
```

> The server will start on `http://0.0.0.0:5000`. Test it by opening `http://localhost:5000/` in your browser.

---

### 3. Android Client Setup

#### A. Physical Phone Setup (Recommended for 2-Device Testing)
1. Connect your phone via USB with **Developer Options > USB Debugging = ON**.
2. Run the helper script to route port 5000 from phone to your computer:
   ```cmd
   tools\phone-usb-setup.bat
   ```
   *(This executes `adb reverse tcp:5000 tcp:5000` so your phone accesses the backend via `http://127.0.0.1:5000/`)*.
3. Open the `android/` folder in Android Studio.
4. Select your phone from the device toolbar and click **Run** (`Shift + F10`).

#### B. Emulator Setup
1. Launch an emulator from Android Studio Device Manager.
2. `NetworkConfig.kt` automatically detects the emulator and points requests to `http://10.0.2.2:5000/`.
3. Click **Run**.

---

## 📡 Real-Time Protocols

### Socket.IO Presence Events
All socket connections require a valid JWT passed during the initial handshake: `auth: { token: "<jwt>" }`.

| Direction | Event Name | Payload | Description |
|---|---|---|---|
| **Client ➔ Server** | `join_room` | `{ roomId: string }` | Joins a room lobby & subscribes to events |
| **Client ➔ Server** | `leave_room` | `{ roomId: string }` | Exits room & alerts peers |
| **Client ➔ Server** | `mute_changed` | `{ roomId: string, isMuted: boolean }` | Broadcasts hardware mic state changes |
| **Server ➔ Client** | `room_state` | `{ roomId: string, members: RoomMember[] }` | Initial roster state sent to joining client |
| **Server ➔ Client** | `user_joined` | `{ roomId: string, user: RoomMember }` | Broadcasted when a new teammate connects |
| **Server ➔ Client** | `user_left` | `{ roomId: string, userId: string }` | Broadcasted when a member leaves cleanly |
| **Server ➔ Client** | `user_mute_changed`| `{ roomId: string, userId: string, isMuted: boolean }` | Updates member badge without reloading UI |
| **Server ➔ Client** | `user_disconnected`| `{ roomId: string, userId: string }` | Fires automatically on sudden drop/crash |

---

## 🧪 Testing 2-Player Lobbies

1. **Host (Device 1):** Sign up as `Venom` ➔ Click `+` ➔ Create room `"Ranked Grind"` (Game: `Valorant`).
2. **Invite:** In the room lobby, tap **Add Friend** and type `Sakshamm`.
3. **Peer (Device 2):** Log in as `Sakshamm` on the second phone/emulator ➔ On Home screen, **pull down to refresh** ➔ `"Ranked Grind"` appears instantly at the top ➔ Tap **Join**.
4. Both devices now see each other in real-time with speaking rings, mute states, and live squad roster.

---

## 👥 Contributors

- **Owner & Maintainer:** [Sakshamm Vipul Khanna](https://github.com/SakshammVKhanna) (`SakshammVKhanna`)
- **Collaborator:** [Saksham Khanna](https://github.com/Saksham-Khanna) (`Saksham-Khanna`)

---

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
