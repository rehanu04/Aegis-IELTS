package com.aegis.ielts.ui.components.shared

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.graphics.Color
import kotlin.math.sin
import kotlin.random.Random

// ─── Star data model ──────────────────────────────────────────────────────────

private data class StarData(
    val xFraction  : Float,   // Fractional position [0,1] within canvas width
    val yFraction  : Float,   // Fractional position [0,1] within canvas height
    val radius     : Float,   // Pixel radius (unscaled, applied with density)
    val baseAlpha  : Float,   // Base opacity [0.2, 0.8]
    val twinklePhase: Float   // Phase offset [0,1] into the master animation cycle
)

/**
 * Full-canvas animated starfield background for all Aegis IELTS screens.
 *
 * Matches the call site in [IeltsSpeakingAssessmentScreen]:
 *   `StarfieldBackground()`
 *
 * Architecture: Star positions are stable (remembered once); all drawing is
 * isolated in [StarfieldCanvas] to enforce the Composition-Skip pattern.
 * The parent composable skips recomposition as long as [modifier] is stable.
 */
@Composable
fun StarfieldBackground(modifier: Modifier = Modifier) {
    val stars = remember {
        List(130) {
            StarData(
                xFraction   = Random.nextFloat(),
                yFraction   = Random.nextFloat(),
                radius      = 0.6f + Random.nextFloat() * 1.6f,
                baseAlpha   = 0.15f + Random.nextFloat() * 0.65f,
                twinklePhase = Random.nextFloat()
            )
        }
    }
    StarfieldCanvas(stars = stars, modifier = modifier.fillMaxSize())
}

/**
 * Isolated Canvas composable for star rendering.
 *
 * A single [masterTick] drives all 130 stars via their [StarData.twinklePhase]
 * offset — one InfiniteTransition total, zero per-star transitions.
 * Compose will skip this entire composable on recompositions where inputs
 * ([stars], [modifier]) haven't changed.
 */
@Composable
private fun StarfieldCanvas(
    stars   : List<StarData>,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "StarfieldTransition")

    val masterTick by infiniteTransition.animateFloat(
        initialValue   = 0f,
        targetValue    = 1f,
        animationSpec  = infiniteRepeatable(
            animation  = tween(durationMillis = 6_000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "StarfieldMasterTick"
    )

    Canvas(modifier = modifier) {
        stars.forEach { star ->
            // Phase-shifted sine wave gives each star a unique twinkle rhythm
            val phase       = (masterTick + star.twinklePhase) % 1f
            val twinkle     = (0.5f + 0.5f * sin(phase * Math.PI.toFloat() * 2f))
            val alpha       = (star.baseAlpha * twinkle).coerceIn(0.04f, 1f)

            drawCircle(
                color  = Color.White.copy(alpha = alpha),
                radius = star.radius * density,
                center = Offset(star.xFraction * size.width, star.yFraction * size.height)
            )
        }
    }
}
