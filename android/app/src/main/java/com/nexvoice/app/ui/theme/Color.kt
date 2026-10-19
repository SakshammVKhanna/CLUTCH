package com.nexvoice.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Color.kt
 *
 * NexVoice / CLUTCH Color Palette — clean LIGHT (whitish) theme.
 * Never hard-code colors in Composables — always reference from here.
 *
 * Design philosophy:
 *  - White / near-white surfaces for a clean, minimal look
 *  - Green (brand) accent for primary actions & "live/speaking" states
 *  - Soft grey borders and muted text for hierarchy
 *  - Red for danger/mute states, amber/orange for warnings
 *
 * NOTE: variable names keep their historic "Nex*" names so existing screens
 * keep compiling, but the values now describe a light palette.
 */

// ── Brand / Accent (green) ──────────────────────────────────────────────────
val NexGold        = Color(0xFF16A34A)   // Primary green accent (buttons, highlights)
val NexGoldLight   = Color(0xFF4ADE80)   // Lighter green for tints/glow
val NexGoldDark    = Color(0xFF15803D)   // Darker green for pressed/text states
val NexOrange      = Color(0xFFEA580C)   // Deep orange — warnings / deafen

// ── Backgrounds (whitish) ───────────────────────────────────────────────────
val NexBg          = Color(0xFFF6F7F9)   // App background — very light grey
val NexSurface     = Color(0xFFFFFFFF)   // Elevated card surface — white
val NexSurfaceAlt  = Color(0xFFF1F3F5)   // Input fields / subtle panels
val NexSurfaceCard = Color(0xFFFFFFFF)   // Room cards — white

// ── Gradient Colors ─────────────────────────────────────────────────────────
val NexGradientStart = Color(0xFFE8F5E9)  // Soft green tint for hero backgrounds
val NexGradientMid   = Color(0xFFFFFFFF)  // Fade to white

// ── Text (dark on light) ────────────────────────────────────────────────────
val NexTextPrimary   = Color(0xFF111827)  // Near-black — crisp on white
val NexTextSecondary = Color(0xFF6B7280)  // Muted grey for metadata/subtitles
val NexTextHint      = Color(0xFF9CA3AF)  // Very muted — placeholders

// ── Status / Voice States ───────────────────────────────────────────────────
val NexOnline    = Color(0xFF22C55E)   // Green — user is present / online
val NexSpeaking  = Color(0xFF16A34A)   // Green — actively speaking glow
val NexMuted     = Color(0xFFEF4444)   // Red — microphone muted / danger
val NexIdle      = Color(0xFF6366F1)   // Indigo — user present but silent
val NexDeafened  = Color(0xFF8B5CF6)   // Purple — deafened state

// ── Borders / Dividers ──────────────────────────────────────────────────────
val NexBorder       = Color(0xFFE5E7EB)  // Subtle border for cards
val NexBorderLight  = Color(0xFFD1D5DB)  // Slightly stronger for hover/focus

// ── Overlays ────────────────────────────────────────────────────────────────
val NexOverlayDark  = Color(0x66000000)  // 40% black overlay for modals
val NexOverlayLight = Color(0x14000000)  // Subtle dark tint for glass effects

// ── On-accent text ──────────────────────────────────────────────────────────
val NexOnAccent     = Color(0xFFFFFFFF)  // Text/icons placed ON the green accent
