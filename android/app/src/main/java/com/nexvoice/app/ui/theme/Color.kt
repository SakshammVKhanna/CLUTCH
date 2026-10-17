package com.nexvoice.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Color.kt
 *
 * NexVoice Gaming Color Palette — Parallel-inspired premium dark theme.
 * Never hard-code colors in Composables — always reference from here.
 *
 * Design philosophy:
 *  - Deep obsidian backgrounds for immersion
 *  - Amber/gold accents for premium feel (like Parallel app)
 *  - Green for "speaking/live" states
 *  - Red for danger/mute states
 */

// ── Brand / Accent ──────────────────────────────────────────────────────────
val NexGold        = Color(0xFFF59E0B)   // Primary amber/gold accent
val NexGoldLight   = Color(0xFFFCD34D)   // Lighter gold for glow effects
val NexGoldDark    = Color(0xFFD97706)   // Darker gold for pressed states
val NexOrange      = Color(0xFFEA580C)   // Deep orange — gradient end

// ── Backgrounds ─────────────────────────────────────────────────────────────
val NexBg          = Color(0xFF0C0D11)   // Near-black — deep space background
val NexSurface     = Color(0xFF161820)   // Elevated card surface
val NexSurfaceAlt  = Color(0xFF1E2030)   // For dialogs/sheets — slightly lighter
val NexSurfaceCard = Color(0xFF12141C)   // Room cards — slightly darker than surface

// ── Gradient Colors ──────────────────────────────────────────────────────────
val NexGradientStart = Color(0xFF1A1200)  // Dark amber tint for hero backgrounds
val NexGradientMid   = Color(0xFF0F1018)  // Transition to deep dark

// ── Text ────────────────────────────────────────────────────────────────────
val NexTextPrimary   = Color(0xFFF0F0F8)  // Near-white — crisp on dark backgrounds
val NexTextSecondary = Color(0xFF7A7D99)  // Muted text for metadata/subtitles
val NexTextHint      = Color(0xFF4A4D66)  // Very muted — placeholders

// ── Status / Voice States ────────────────────────────────────────────────────
val NexOnline    = Color(0xFF22C55E)   // Green — user is speaking / online
val NexSpeaking  = Color(0xFF4ADE80)   // Lighter green — active speaking glow
val NexMuted     = Color(0xFFEF4444)   // Red — microphone muted / danger
val NexIdle      = Color(0xFF6366F1)   // Indigo — user present but silent
val NexDeafened  = Color(0xFF8B5CF6)   // Purple — deafened state

// ── Borders / Dividers ──────────────────────────────────────────────────────
val NexBorder       = Color(0xFF252840)  // Subtle border for cards
val NexBorderLight  = Color(0xFF353860)  // Slightly visible for hover states

// ── Overlays ─────────────────────────────────────────────────────────────────
val NexOverlayDark  = Color(0xCC000000)  // 80% black overlay for modals
val NexOverlayLight = Color(0x1AFFFFFF)  // 10% white — glassmorphism effect
