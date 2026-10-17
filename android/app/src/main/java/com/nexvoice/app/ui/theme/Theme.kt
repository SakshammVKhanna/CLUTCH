package com.nexvoice.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Theme.kt
 *
 * This is the central theme file.
 * NexVoiceTheme wraps the entire app — it's called in MainActivity.
 *
 * We ONLY provide a dark color scheme because this is a gaming app.
 * The Material 3 darkColorScheme maps our custom colors to the M3 roles:
 *   primary       → main brand color (buttons, highlights)
 *   background    → app background
 *   surface       → card / sheet background
 *   onPrimary     → text ON a primary-colored button
 *   etc.
 */

private val NexDarkColorScheme = darkColorScheme(
    primary            = NexGold,
    onPrimary          = NexBg,
    primaryContainer   = NexGoldDark,
    onPrimaryContainer = NexGoldLight,

    secondary          = NexOrange,
    onSecondary        = NexBg,

    background         = NexBg,
    onBackground       = NexTextPrimary,

    surface            = NexSurface,
    onSurface          = NexTextPrimary,
    surfaceVariant     = NexSurfaceAlt,
    onSurfaceVariant   = NexTextSecondary,

    outline            = NexBorder,
    outlineVariant     = NexBorderLight,
    error              = NexMuted,
    onError            = NexTextPrimary
)

@Composable
fun NexVoiceTheme(
    content: @Composable () -> Unit
) {
    // Always dark — gaming apps don't need a light theme
    val colorScheme = NexDarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            // Make the status bar match our dark background
            val window = (view.context as Activity).window
            window.statusBarColor = NexBg.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = NexVoiceTypography,
        content     = content
    )
}
