# NexVoice — Phase 1 Complete

## Phase 1 Summary
Android project with full UI, navigation, and dark gaming theme.
No backend connection yet — all data is fake/hardcoded.

## Screens Built
1. SplashScreen — animated logo, auto-navigates after 2s
2. AuthScreen — Login/Signup toggle with form validation
3. HomeScreen — room list with hero banner
4. CreateRoomScreen — room creation form with slider
5. VoiceRoomScreen — member grid with mic/leave controls

## File Structure

```
android/
app/build.gradle                          (app dependencies)
app/src/main/AndroidManifest.xml          (permissions + activity)
app/src/main/java/com/nexvoice/app/
  MainActivity.kt                         (entry point)
  models/Models.kt                        (Room, User, RoomMember)
  ui/navigation/Screen.kt                 (route definitions)
  ui/navigation/NexVoiceNavGraph.kt       (navigation map)
  ui/screens/splash/SplashScreen.kt
  ui/screens/auth/AuthScreen.kt
  ui/screens/home/HomeScreen.kt
  ui/screens/createroom/CreateRoomScreen.kt
  ui/screens/voiceroom/VoiceRoomScreen.kt
  ui/theme/Color.kt                       (all colors)
  ui/theme/Type.kt                        (typography)
  ui/theme/Theme.kt                       (MaterialTheme setup)
app/src/main/res/values/colors.xml
app/src/main/res/values/strings.xml
app/src/main/res/values/themes.xml
build.gradle                              (plugin versions)
settings.gradle                           (project name + modules)
gradle.properties
```

## How to Open in Android Studio

1. Open Android Studio (Hedgehog 2023.1.1 or newer)
2. Click File > Open
3. Navigate to: d:\work\parallel-voice\android
4. Click OK
5. Wait for Gradle sync to finish (bottom bar shows progress)
6. If prompted, click "Sync Now"

## How to Run on Your Phone

### Option A - USB Cable (Recommended)
1. Phone: Settings > Developer Options > USB Debugging > ON
   (Enable Developer Options: tap Build Number 7 times in About Phone)
2. Connect phone to PC via USB
3. Select your phone in the device dropdown (top of Android Studio)
4. Click the Run button (green triangle)

### Option B - Wireless (Android 11+)
1. Settings > Developer Options > Wireless Debugging > ON
2. Android Studio: View > Tool Windows > Device Manager > Pair Wirelessly

### Option C - Emulator
1. Device Manager > Create Device
2. Choose Pixel 7, API 34
3. Download system image if needed, then click Run

## Known Phase 1 Limitations
- Login accepts any email/password (no real auth yet)
- Room list is hardcoded fake data
- Mic button toggles local state only (no audio)
- Room creation generates a fake ID

## Next Step: Phase 2
Build the Node.js + Express + MongoDB backend in the /backend folder.
