package com.aegis.ielts.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Muted Twilight Slate / Galactic Cyan Design System ──────────────────────
// These are the canonical semantic tokens for the entire Aegis IELTS application.
// All UI components MUST reference these tokens — never raw hex values in feature code.

// ── Primary Surfaces ─────────────────────────────────────────────────────────
val SurfaceSlate      = Color(0xFF1A1C1E)   // Primary background
val SurfaceSlateLight = Color(0xFF2C2F33)   // Elevated containers, cards
val SurfaceCard       = Color(0xFF23272B)   // Module cards, sheets

// ── Brand Accent ─────────────────────────────────────────────────────────────
val SurfaceCyan       = Color(0xFF00F5FF)   // Primary interactive + active states

// ── Text ─────────────────────────────────────────────────────────────────────
val TextLight         = Color(0xFFFFFFFF)   // Primary body text
val TextMuted         = Color(0xFFA0A5AD)   // Secondary / label text

// ── Semantic Status Colors ────────────────────────────────────────────────────
val AccentGold        = Color(0xFFD4AF37)   // Analyzing / thinking states
val AccentGreen       = Color(0xFF10B981)   // Recording / success states
val AccentError       = Color(0xFFEF4444)   // Error / abort states
val AccentBlue        = Color(0xFF3B82F6)   // Reading module accent

// ── Transparency Helpers ─────────────────────────────────────────────────────
val CyanTransparent   = Color(0x0000F5FF)
val SlateTransparent  = Color(0x001A1C1E)