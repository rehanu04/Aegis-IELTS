package com.aegis.ielts.ui.components.shared

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.aegis.ielts.ui.theme.AccentGold
import com.aegis.ielts.ui.theme.SurfaceCyan
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ─── Particle Configuration ───────────────────────────────────────────────────

private data class ParticleConfig(
    val baseAngle  : Float,   // Initial orbit angle in radians
    val orbitRadius: Float,   // Orbit radius in dp-equivalent units
    val particleSize: Float,  // Dot radius in dp-equivalent units
    val alpha      : Float,   // Base opacity
    val speedMult  : Float    // Relative angular speed multiplier
)

private const val OUTER_PARTICLE_COUNT = 24
private const val INNER_PARTICLE_COUNT = 12

/**
 * Physics-simulated particle blob orb driven by three state flags.
 *
 * Behavioral states:
 *  - [isListening]: Cyan orb, fast pulse, particles orbit quickly
 *  - [isThinking]:  Gold orb, accelerated orbit (Analyzing state)
 *  - [isTalking]:   White orb, steady medium pulse
 *  - idle:          Dim cyan orb, slow gentle pulse
 *
 * Matches the call sites in [IeltsSpeakingAssessmentScreen] exactly:
 *   `ParticleBlobOrb(isThinking = ..., isTalking = ..., isListening = ...)`
 *
 * Architecture: Stable particle configs are computed once via [remember].
 * All Canvas drawing is isolated in [OrbParticleCanvas] for Composition-Skip.
 */
@Composable
fun ParticleBlobOrb(
    isThinking: Boolean,
    isTalking : Boolean,
    isListening: Boolean,
    modifier  : Modifier = Modifier
) {
    // ── Stable particle config arrays (computed once) ─────────────────────────
    val outerParticles = remember {
        List(OUTER_PARTICLE_COUNT) { i ->
            ParticleConfig(
                baseAngle   = (i.toFloat() / OUTER_PARTICLE_COUNT) * (2f * Math.PI.toFloat()),
                orbitRadius = 88f + (i % 4) * 16f,
                particleSize = 3f + (i % 5) * 1.2f,
                alpha       = 0.35f + (i % 6) * 0.1f,
                speedMult   = 0.7f + (i % 4) * 0.35f
            )
        }
    }
    val innerParticles = remember {
        List(INNER_PARTICLE_COUNT) { i ->
            ParticleConfig(
                baseAngle   = (i.toFloat() / INNER_PARTICLE_COUNT) * (2f * Math.PI.toFloat()),
                orbitRadius = 50f + (i % 3) * 12f,
                particleSize = 2f + (i % 3).toFloat(),
                alpha       = 0.18f + (i % 4) * 0.1f,
                speedMult   = 1.1f + (i % 3) * 0.5f
            )
        }
    }

    // ── State-driven orb color ─────────────────────────────────────────────────
    val targetColor = when {
        isListening -> SurfaceCyan
        isThinking  -> AccentGold
        isTalking   -> Color.White
        else        -> SurfaceCyan.copy(alpha = 0.35f)
    }
    val coreColor by animateColorAsState(
        targetValue  = targetColor,
        animationSpec = tween(durationMillis = 500),
        label        = "OrbCoreColor"
    )

    val isActive = isListening || isThinking || isTalking

    // ── Infinite transition (one per orb instance) ────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "OrbTransition")

    // Breathing pulse
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = if (isActive) 1.14f else 1.03f,
        animationSpec = infiniteRepeatable(
            animation  = tween(
                durationMillis = when {
                    isListening -> 600
                    isTalking   -> 900
                    isThinking  -> 750
                    else        -> 1_800
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbPulseScale"
    )

    // Orbital rotation
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 360f,
        animationSpec = infiniteRepeatable(
            animation  = tween(
                durationMillis = if (isThinking) 1_400 else 3_200,
                easing         = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbOrbitAngle"
    )

    // Glow halo alpha
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.06f,
        targetValue  = if (isActive) 0.22f else 0.10f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbGlowAlpha"
    )

    // ── Isolated Canvas composable ────────────────────────────────────────────
    OrbParticleCanvas(
        outerParticles   = outerParticles,
        innerParticles   = innerParticles,
        coreColor        = coreColor,
        pulseScale       = pulseScale,
        orbitAngleDeg    = orbitAngle,
        glowAlpha        = glowAlpha,
        isActive         = isActive,
        modifier         = modifier.fillMaxSize()
    )
}

/**
 * Canvas-isolated drawing composable for the particle orb system.
 *
 * Isolated as a dedicated @Composable function so Compose can skip
 * recomposition when none of the stable parameters have changed.
 * All expensive [DrawScope] operations live here and nowhere else.
 */
@Composable
private fun OrbParticleCanvas(
    outerParticles: List<ParticleConfig>,
    innerParticles: List<ParticleConfig>,
    coreColor     : Color,
    pulseScale    : Float,
    orbitAngleDeg : Float,
    glowAlpha     : Float,
    isActive      : Boolean,
    modifier      : Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cx        = size.width  / 2f
        val cy        = size.height / 2f
        val orbRadius = 36f * density * pulseScale
        val orbitRad  = Math.toRadians(orbitAngleDeg.toDouble()).toFloat()

        // ── Outer glow halo ──────────────────────────────────────────────────
        drawCircle(
            brush  = Brush.radialGradient(
                colors = listOf(
                    coreColor.copy(alpha = glowAlpha),
                    coreColor.copy(alpha = glowAlpha * 0.5f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = orbRadius * 3.8f
            ),
            radius = orbRadius * 3.8f,
            center = Offset(cx, cy)
        )

        // ── Outer particle ring ──────────────────────────────────────────────
        outerParticles.forEach { p ->
            val angle   = p.baseAngle + orbitRad * p.speedMult
            val px      = cx + cos(angle) * p.orbitRadius * density
            val py      = cy + sin(angle) * p.orbitRadius * density
            val alpha   = (if (isActive) p.alpha else p.alpha * 0.25f)
            drawCircle(
                color  = coreColor.copy(alpha = alpha),
                radius = p.particleSize * density * pulseScale,
                center = Offset(px, py)
            )
        }

        // ── Inner counter-rotating ring ──────────────────────────────────────
        innerParticles.forEach { p ->
            val angle   = p.baseAngle - orbitRad * p.speedMult * 1.4f
            val px      = cx + cos(angle) * p.orbitRadius * density
            val py      = cy + sin(angle) * p.orbitRadius * density
            val alpha   = (if (isActive) p.alpha else p.alpha * 0.15f)
            drawCircle(
                color  = Color.White.copy(alpha = alpha),
                radius = p.particleSize * density,
                center = Offset(px, py)
            )
        }

        // ── Orbit ring stroke ────────────────────────────────────────────────
        if (isActive) {
            drawCircle(
                color  = coreColor.copy(alpha = 0.12f),
                radius = 90f * density,
                center = Offset(cx, cy),
                style  = Stroke(width = 1f * density)
            )
        }

        // ── Core orb radial gradient ─────────────────────────────────────────
        drawCircle(
            brush  = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.92f),
                    coreColor.copy(alpha = 0.88f),
                    coreColor.copy(alpha = 0.55f),
                    coreColor.copy(alpha = 0.08f)
                ),
                center = Offset(cx, cy),
                radius = orbRadius
            ),
            radius = orbRadius,
            center = Offset(cx, cy)
        )

        // ── Specular highlight ───────────────────────────────────────────────
        drawCircle(
            color  = Color.White.copy(alpha = 0.65f),
            radius = orbRadius * 0.26f,
            center = Offset(
                cx - orbRadius * 0.20f,
                cy - orbRadius * 0.20f
            )
        )
    }
}
