# 🎮 CLUTCH — Session Summary & Work Log

**Date:** Sun Oct 18, 2026  
**Project:** CLUTCH (formerly NexVoice / Parallel Voice)  
**Repository:** https://github.com/SakshammVKhanna/CLUTCH.git  
**Workflow:** Contributor (`Saksham-Khanna`) → Repo Owner (`SakshammVKhanna`)

---

## 🚀 1. Key Completed Work (This Session)

### A. App Rebranding to **CLUTCH**
- **System App Name:** Changed to `CLUTCH` in `strings.xml`.
- **Splash Screen:** Rebranded with title `CLUTCH` and tagline *"Clutch the win. Own the voice."*
- **Auth Screen:** Updated branding to `CLUTCH`.
- **Invite Links:** Rebranded share message to *"Join my CLUTCH squad..."* (`InviteLink.kt`).
- **Top Header Branding:** Removed `"NexVoice"` text completely from the top bar. Only the sleek gaming logo badge (emerald green "C" in dark rounded container) remains on the left.

### B. Dedicated User Profile Interface (`ProfileScreen.kt`)
Replaced the simple logout popup with a full-fledged, modern profile interface inspired by gaming/social apps:
- **Header:** Top action bar with Close (`✕`), `"Edit Profile"` title, and Save (`✓`) icon.
- **Scenic Banner:** Dark twilight gaming gradient with an ambient "CLUTCH" watermark.
- **Overlapping Avatar:** Large circular profile avatar overlapping the bottom edge of the banner with a 4dp clean white border and `"Change photo"` clickable action.
- **User Detail Fields:**
  - 🏷️ **Name:** Editable display name
  - 🆔 **Username:** Readonly `@handle`
  - ✉️ **Email:** Logged-in user email
  - 📝 **Bio:** Editable status / squad tagline
- **Voice Preferences:** Toggle switches for Noise Suppression & Low-Latency Squad Mode.
- **Log Out:** Styled red-accented button with confirmation dialog.
- **Two-way Access:** Opened by tapping Avatar in Top Bar OR by tapping "Profile" in Bottom Nav.
- **Persistence:** Stored in DataStore via `TokenStore` (`user_name`, `user_email`, `user_bio`).

### C. Voice Room UI & Button Wiring (`VoiceRoomScreen.kt`)
- **Self Username Identification:** Added `currentUserName` to `VoiceRoomViewModel`; Host card shows `(You)` badge, and grid member tiles show gold ring highlight + `(You)` suffix.
- **Room Options Bottom Sheet:** Wired both Settings (⚙️) and More (⋯) buttons to open a bottom sheet:
  - Copy Room Code (1-tap clipboard + Toast)
  - Share Invite Link (system share chooser)
  - Add Member by username
  - Leave Room (destructive)
- **Speaker Audio Routing:** Wired Speaker button to real hardware routing via Android `AudioManager.isSpeakerphoneOn`.
- **Status Banner:** Replaced dev message with friendly `"Presence active • Voice connecting…"`.

### D. Header Minimalization
- Removed search bar and search toggle icon from `ParallelTopBar` to keep the header minimal, uncluttered, and sleek.

---

## 🛠️ 2. Git & GitHub Setup Completed

1. **Repository Migration:**
   - Switched remote from `Saksham-Khanna/parallel-voice` to `SakshammVKhanna/CLUTCH`.
   - Created root `.gitignore` to keep git free of `.gradle/` and `build/` artifacts.
   - Pushed full clean source code to `origin/main`.
2. **Collaborative PR Workflow Verified:**
   - Feature branch created: `feature/remove-search-bar`
   - PR sent by: `Saksham-Khanna` (Contributor)
   - PR accepted & merged by: `SakshammVKhanna` (Owner)
   - Local `main` branch synced with merged remote (`git pull origin main`).

---

## 📱 3. Build & Testing Status

- **Build:** Gradle 8.4 + JDK 17 (`app-debug.apk`) compiles clean with zero errors.
- **Emulator:** Installed on `emulator-5554` (Pixel 7).
- **Physical Phone (OnePlus CPH2467):** Connect via USB and run `tools\phone-usb-setup.bat` (configures `adb reverse tcp:5000 tcp:5000`).

---

## 🔮 4. Next Milestone (Upcoming Phase)

- **LiveKit / WebRTC Real Voice Audio:**
  - Configure LiveKit Cloud credentials on backend (`LIVEKIT_API_KEY`, `LIVEKIT_API_SECRET`, `LIVEKIT_URL`).
  - Wire real two-way audio streaming between devices (Emulator ↔ Phone).
  - Verify speaking detection and mic mute across devices.
