package com.nexvoice.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
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
 * The app uses a clean LIGHT (whitish) color scheme with a green accent.
 * The Material 3 lightColorScheme maps our custom colors to the M3 roles:
 *   primary       → main brand color (buttons, highlights)
 *   background    → app background
 *   surface       → card / sheet background
 *   onPrimary     → text ON a primary-colored button
 *   etc.
 */

private val NexLightColorScheme = lightColorScheme(
    primary            = NexGold,
    onPrimary          = NexOnAccent,
    primaryContainer   = NexGoldLight,
    onPrimaryContainer = NexGoldDark,

    secondary          = NexOrange,
    onSecondary        = NexOnAccent,

    background         = NexBg,
    onBackground       = NexTextPrimary,

    surface            = NexSurface,
    onSurface          = NexTextPrimary,
    surfaceVariant     = NexSurfaceAlt,
    onSurfaceVariant   = NexTextSecondary,

    outline            = NexBorder,
    outlineVariant     = NexBorderLight,
    error              = NexMuted,
    onError            = NexOnAccent
)

@Composable
fun NexVoiceTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = NexLightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            // White status bar with dark icons to match the light UI
            val window = (view.context as Activity).window
            window.statusBarColor = NexSurface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = NexVoiceTypography,
        content     = content
    )
}
