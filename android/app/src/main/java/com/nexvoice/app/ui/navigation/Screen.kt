package com.nexvoice.app.ui.navigation

/**
 * Screen.kt
 *
 * Defines all the navigation routes in the app as a sealed class.
 *
 * A sealed class is perfect for routes because:
 * - It's type-safe (no typos like "hoem" instead of "home")
 * - Android Studio gives you autocomplete when navigating
 * - Easy to add new screens later
 *
 * The `route` string is what the NavController uses internally.
 * Arguments in routes use {braces} — e.g., "room/{roomId}"
 */
sealed class Screen(val route: String) {

    // Splash screen — shows briefly when app starts
    object Splash : Screen("splash")

    // Login/Signup combined screen
    object Auth : Screen("auth")

    // Main home screen with room list
    object Home : Screen("home")

    // Create a new voice room
    object CreateRoom : Screen("create_room")

    // Voice room screen — takes a roomId argument
    // Usage: navigate("room/abc123")
    object VoiceRoom : Screen("room/{roomId}") {
        // Helper function to build the actual route with a real ID
        fun createRoute(roomId: String) = "room/$roomId"
    }
}
