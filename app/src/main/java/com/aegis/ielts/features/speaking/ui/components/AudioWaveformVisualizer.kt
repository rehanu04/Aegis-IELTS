package com.aegis.ielts.features.speaking.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.aegis.ielts.ui.theme.SurfaceCyan
import kotlin.math.abs
import kotlin.math.sin

private const val BAR_COUNT       = 48
private const val SMOOTHING_ALPHA = 0.3f

/**
 * Real-time audio waveform visualizer rendered as a symmetric bar chart.
 *
 * @param amplitudeDb  Normalized amplitude from [AudioCaptureEngine] in [0.0, 1.0].
 *                     0.0 = silence (minimum bar height), 1.0 = peak signal.
 *
 * Architecture: stable [barHeights] buffer allocated once via [remember].
 * All drawing is isolated in [WaveformCanvas] — the parent composable enforces
 * the Composition-Skip pattern by keeping expensive DrawScope calls quarantined.
 */
@Composable
fun AudioWaveformVisualizer(
    amplitudeDb: Float,
    modifier   : Modifier = Modifier
) {
    // Smoothed bar heights — stable reference; mutated in-place by WaveformCanvas
    val barHeights = remember { FloatArray(BAR_COUNT) { 0f } }

    WaveformCanvas(
        amplitudeDb = amplitudeDb,
        barHeights  = barHeights,
        modifier    = modifier
    )
}

/**
 * Isolated Canvas composable for waveform rendering.
 *
 * Applies Gaussian-distributed amplitude across all bars centered on the
 * midpoint, then smooths with exponential moving average (α = 0.3) to
 * eliminate jitter from transient PCM bursts.
 *
 * Each bar is rendered as a rounded rectangle with a vertical cyan gradient.
 */
@Composable
private fun WaveformCanvas(
    amplitudeDb: Float,
    barHeights : FloatArray,
    modifier   : Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val barWidth      = (size.width / BAR_COUNT) * 0.6f
        val gap           = (size.width / BAR_COUNT) * 0.4f
        val centerY       = size.height / 2f
        val maxHalfHeight = size.height * 0.44f
        val centerIndex   = BAR_COUNT / 2
        val normalizedAmp = amplitudeDb.coerceIn(0f, 1f)

        // ── Compute target bar heights with Gaussian falloff from center ──────
        for (i in 0 until BAR_COUNT) {
            val distRatio  = abs(i - centerIndex).toFloat() / centerIndex
            val sineBoost  = 0.3f * sin((i.toFloat() / BAR_COUNT) * Math.PI.toFloat())
            val targetH    = normalizedAmp * (1f - distRatio * 0.68f) + normalizedAmp * sineBoost
            // Exponential smoothing: new = old + α × (target - old)
            barHeights[i]  = barHeights[i] + SMOOTHING_ALPHA * (targetH - barHeights[i])
        }

        // ── Draw bars ─────────────────────────────────────────────────────────
        for (i in 0 until BAR_COUNT) {
            val halfHeight = (barHeights[i] * maxHalfHeight).coerceAtLeast(2f * density)
            val left       = i * (barWidth + gap)
            val barAlpha   = 0.35f + barHeights[i] * 0.65f

            drawRoundRect(
                brush       = Brush.verticalGradient(
                    colors  = listOf(
                        SurfaceCyan.copy(alpha = barAlpha),
                        SurfaceCyan.copy(alpha = barAlpha * 0.25f)
                    ),
                    startY  = centerY - halfHeight,
                    endY    = centerY + halfHeight
                ),
                topLeft     = Offset(left, centerY - halfHeight),
                size        = Size(barWidth, halfHeight * 2f),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
