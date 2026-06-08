package com.aegis.ielts.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// ─── Aegis IELTS is a dark-only application ──────────────────────────────────
// Dynamic color and light themes are intentionally disabled to preserve
// the "Muted Twilight Slate / Galactic Cyan" aesthetic system integrity.

private val AegisDarkColorScheme = darkColorScheme(
    primary            = SurfaceCyan,
    onPrimary          = SurfaceSlate,
    primaryContainer   = SurfaceSlateLight,
    onPrimaryContainer = TextLight,
    secondary          = AccentGold,
    onSecondary        = SurfaceSlate,
    tertiary           = AccentGreen,
    onTertiary         = SurfaceSlate,
    background         = SurfaceSlate,
    onBackground       = TextLight,
    surface            = SurfaceCard,
    onSurface          = TextLight,
    surfaceVariant     = SurfaceSlateLight,
    onSurfaceVariant   = TextMuted,
    error              = AccentError,
    onError            = TextLight,
    outline            = TextMuted.copy(alpha = 0.4f),
    outlineVariant     = SurfaceSlateLight
)

@Composable
fun AegisIELTSTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AegisDarkColorScheme,
        typography  = Typography,
        content     = content
    )
}