package com.aegis.ielts.features.home.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegis.ielts.ui.components.shared.StarfieldBackground
import com.aegis.ielts.ui.theme.AccentBlue
import com.aegis.ielts.ui.theme.AccentGold
import com.aegis.ielts.ui.theme.AccentGreen
import com.aegis.ielts.ui.theme.SurfaceCard
import com.aegis.ielts.ui.theme.SurfaceCyan
import com.aegis.ielts.ui.theme.SurfaceSlate
import com.aegis.ielts.ui.theme.SurfaceSlateLight
import com.aegis.ielts.ui.theme.TextLight
import com.aegis.ielts.ui.theme.TextMuted
import java.util.UUID

// ─── Module Configuration ─────────────────────────────────────────────────────

private data class ModuleConfig(
    val title      : String,
    val icon       : ImageVector,
    val accent     : Color,
    val description: String,
    val tag        : String
)

private val MODULES = listOf(
    ModuleConfig(
        title       = "Speaking",
        icon        = Icons.Filled.Mic,
        accent      = SurfaceCyan,
        description = "AI-powered evaluation\nacross 4 official criteria",
        tag         = "LIVE AI GRADING"
    ),
    ModuleConfig(
        title       = "Reading",
        icon        = Icons.Filled.Description,
        accent      = AccentBlue,
        description = "40-question passages\nwith real-time analysis",
        tag         = "SPLIT-SCREEN"
    ),
    ModuleConfig(
        title       = "Listening",
        icon        = Icons.AutoMirrored.Filled.VolumeUp,
        accent      = AccentGreen,
        description = "Multi-accent audio\nwith no rewind controls",
        tag         = "NO REWIND"
    ),
    ModuleConfig(
        title       = "Writing",
        icon        = Icons.Filled.Create,
        accent      = AccentGold,
        description = "Task 1 & 2 with\ntemplate detection flags",
        tag         = "ANTI-TEMPLATE"
    )
)

/**
 * Aegis IELTS home dashboard — four module cards on an animated starfield.
 *
 * Each module card generates a fresh UUID as the testId and navigates to
 * the corresponding module screen.
 */
@Composable
fun AegisHomeScreen(
    onNavigateToSpeaking : (testId: String) -> Unit,
    onNavigateToReading  : (testId: String) -> Unit,
    onNavigateToListening: (testId: String) -> Unit,
    onNavigateToWriting  : (testId: String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceSlate)
    ) {
        // ── Background starfield ───────────────────────────────────────────
        StarfieldBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .systemBarsPadding()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            // ── Brand header ───────────────────────────────────────────────
            AegisBrandHeader()

            Spacer(Modifier.height(48.dp))

            Text(
                text          = "SELECT MODULE",
                style         = MaterialTheme.typography.labelMedium,
                color         = TextMuted,
                letterSpacing = 3.sp
            )

            Spacer(Modifier.height(20.dp))

            // ── 2 × 2 module grid ──────────────────────────────────────────
            val navigators = listOf(
                { onNavigateToSpeaking(UUID.randomUUID().toString()) },
                { onNavigateToReading(UUID.randomUUID().toString()) },
                { onNavigateToListening(UUID.randomUUID().toString()) },
                { onNavigateToWriting(UUID.randomUUID().toString()) }
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModuleCard(
                    config   = MODULES[0],
                    modifier = Modifier.weight(1f),
                    onClick  = navigators[0]
                )
                ModuleCard(
                    config   = MODULES[1],
                    modifier = Modifier.weight(1f),
                    onClick  = navigators[1]
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModuleCard(
                    config   = MODULES[2],
                    modifier = Modifier.weight(1f),
                    onClick  = navigators[2]
                )
                ModuleCard(
                    config   = MODULES[3],
                    modifier = Modifier.weight(1f),
                    onClick  = navigators[3]
                )
            }

            Spacer(Modifier.height(40.dp))

            // ── Footer ────────────────────────────────────────────────────
            Text(
                text          = "COMPUTER-BASED EXAM SIMULATOR  •  v1.0",
                style         = MaterialTheme.typography.labelSmall,
                color         = TextMuted.copy(alpha = 0.4f),
                letterSpacing = 1.sp
            )

            Spacer(Modifier.height(28.dp))
        }
    }
}

// ─── Brand Header ─────────────────────────────────────────────────────────────

@Composable
private fun AegisBrandHeader() {
    val infiniteTransition = rememberInfiniteTransition(label = "BrandPulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.45f,
        targetValue   = 0.95f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2_200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BrandGlowAlpha"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text          = "AEGIS",
            fontSize      = 54.sp,
            fontWeight    = FontWeight.Bold,
            color         = SurfaceCyan.copy(alpha = glowAlpha),
            letterSpacing = 14.sp,
            textAlign     = TextAlign.Center
        )
        Text(
            text          = "IELTS",
            fontSize      = 22.sp,
            fontWeight    = FontWeight.Light,
            color         = TextLight,
            letterSpacing = 10.sp
        )
        Spacer(Modifier.height(10.dp))
        // Cyan gradient divider
        Box(
            modifier = Modifier
                .width(130.dp)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, SurfaceCyan, Color.Transparent)
                    )
                )
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text      = "Computer-Based Examination Simulator",
            style     = MaterialTheme.typography.bodySmall,
            color     = TextMuted,
            textAlign = TextAlign.Center
        )
    }
}

// ─── Module Card ──────────────────────────────────────────────────────────────

@Composable
private fun ModuleCard(
    config  : ModuleConfig,
    modifier: Modifier = Modifier,
    onClick : () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed         by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "CardScale_${config.title}"
    )

    Box(
        modifier = modifier
            .aspectRatio(0.82f)
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCard)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        config.accent.copy(alpha = if (isPressed) 0.55f else 0.22f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            )
    ) {
        // Accent strip at top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, config.accent, Color.Transparent)
                    )
                )
        )

        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon with radial glow background
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier
                    .size(58.dp)
                    .background(
                        brush = Brush.radialGradient(
                            listOf(
                                config.accent.copy(alpha = 0.18f),
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )
            ) {
                Icon(
                    imageVector     = config.icon,
                    contentDescription = config.title,
                    tint            = config.accent,
                    modifier        = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text          = config.title.uppercase(),
                style         = MaterialTheme.typography.titleSmall,
                color         = TextLight,
                fontWeight    = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text      = config.description,
                style     = MaterialTheme.typography.labelSmall,
                color     = TextMuted,
                textAlign = TextAlign.Center,
                lineHeight = 15.sp
            )

            Spacer(Modifier.height(10.dp))

            // Feature tag badge
            Box(
                modifier = Modifier
                    .background(
                        color = config.accent.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text          = config.tag,
                    style         = MaterialTheme.typography.labelSmall,
                    color         = config.accent,
                    fontSize      = 9.sp,
                    letterSpacing = 0.8.sp
                )
            }
        }
    }
}
