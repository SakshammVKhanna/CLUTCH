package com.nexvoice.app.ui.navigation

import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nexvoice.app.auth.SessionManager
import com.nexvoice.app.auth.TokenStore
import com.nexvoice.app.realtime.LiveKitManager
import com.nexvoice.app.realtime.SocketManager
import com.nexvoice.app.ui.screens.auth.AuthScreen
import com.nexvoice.app.ui.screens.createroom.CreateRoomScreen
import com.nexvoice.app.ui.screens.home.HomeScreen
import com.nexvoice.app.ui.screens.splash.SplashScreen
import com.nexvoice.app.ui.screens.voiceroom.VoiceRoomScreen
import com.nexvoice.app.webrtc.WebRtcAudioManager

/**
 * NexVoiceNavGraph.kt
 *
 * This is the Navigation Graph — the "map" of all screens in the app.
 *
 * How Jetpack Compose Navigation works:
 * 1. NavController — knows current screen, handles back stack
 * 2. NavHost — the container that shows the current screen
 * 3. composable("route") — registers each screen with its route
 *
 * To navigate: navController.navigate(Screen.Home.route)
 * To go back:  navController.popBackStack()
 */
@Composable
fun NexVoiceNavGraph() {
    // rememberNavController keeps the NavController alive across recompositions
    val navController = rememberNavController()
    val context = LocalContext.current

    // ── Session expiry watchdog ───────────────────────────────────────────────
    // App ke chalte-chalte jab bhi token expire ho (JWT 7 din me expire hota hai),
    // ApiClient ka interceptor SessionManager me event bhejta hai. Hum yahan:
    //   1. token clear karte hain
    //   2. live connections (LiveKit / Socket / audio) band karte hain
    //   3. poori back stack hata kar seedha Login screen dikhate hain
    // Pehle is jagah "Connection Failed – Try Again" wali dead-end screen aati thi.
    LaunchedEffect(Unit) {
        SessionManager.expired.collect {
            Log.w("NexVoiceNav", "Session expired → clearing token and going to Login")

            LiveKitManager.disconnect()
            WebRtcAudioManager.release()
            SocketManager.disconnect()
            TokenStore.clearToken(context)

            Toast.makeText(
                context,
                "Session expired. Please log in again.",
                Toast.LENGTH_LONG
            ).show()

            // Sirf tab navigate karo jab pehle se Login screen pe na ho
            if (navController.currentDestination?.route != Screen.Auth.route) {
                navController.navigate(Screen.Auth.route) {
                    popUpTo(navController.graph.id) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route  // App always starts at Splash
    ) {

        // ── Splash Screen ──────────────────────────────────────────────────
        composable(route = Screen.Splash.route) {
            SplashScreen(
                onNavigateToAuth = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    // Already logged in — skip Auth and go straight to Home
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // ── Auth Screen (Login / Signup) ───────────────────────────────────
        composable(route = Screen.Auth.route) {
            AuthScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        // ── Home Screen ────────────────────────────────────────────────────
        composable(route = Screen.Home.route) {
            HomeScreen(
                onCreateRoom = {
                    navController.navigate(Screen.CreateRoom.route)
                },
                onJoinRoom = { roomId ->
                    navController.navigate(Screen.VoiceRoom.createRoute(roomId))
                },
                onLogout = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        // ── Create Room Screen ─────────────────────────────────────────────
        composable(route = Screen.CreateRoom.route) {
            CreateRoomScreen(
                onRoomCreated = { roomId ->
                    // Navigate to the room after creating it
                    navController.navigate(Screen.VoiceRoom.createRoute(roomId)) {
                        popUpTo(Screen.CreateRoom.route) { inclusive = true }
                    }
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        // ── Voice Room Screen ──────────────────────────────────────────────
        // This screen receives a `roomId` argument from the route
        composable(
            route = Screen.VoiceRoom.route,
            arguments = listOf(
                navArgument("roomId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            // Extract the roomId from the navigation arguments
            val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
            VoiceRoomScreen(
                roomId = roomId,
                onLeaveRoom = {
                    navController.popBackStack()
                }
            )
        }
    }
}
