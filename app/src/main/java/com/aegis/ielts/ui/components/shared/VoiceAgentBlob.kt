package com.aegis.ielts.ui.components.shared

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

/**
 * Premium Voice Agent kinetic blob with spring physics and fluid morphing.
 *
 * States:
 * - Idle/Thinking: Twilight Slate (#1A1C1E) slow breathing.
 * - Examiner Speaking: Galactic Cyan (#00F5FF) active network nodes.
 * - Candidate Recording: Real-time amplitude ripples tracking decibels.
 */
@Composable
fun VoiceAgentBlob(
    isThinking: Boolean,
    isTalking: Boolean,
    isListening: Boolean,
    amplitude: Float,
    modifier: Modifier = Modifier
) {
    // Spring physics wrapper for amplitude to smooth out raw decibel ripples
    val springAmp by animateFloatAsState(
        targetValue = amplitude,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "BlobSpringAmp"
    )

    // Decibel calculation: rawDb tracking native microphone decibel levels
    val rawDb = remember(springAmp) {
        (springAmp * 96f - 96f).coerceIn(-96f, 0f)
    }

    // Color states transition
    val targetColor = when {
        isListening -> Color(0xFF00FF88) // Vibrant emerald green for recording
        isTalking -> Color(0xFF00F5FF)   // Galactic Cyan for examiner speaking
        else -> Color(0xFF1A1C1E)        // Twilight Slate for idle/thinking
    }
    val blobColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "BlobColor"
    )

    // Infinite breathing cycle for Idle/Thinking
    val infiniteTransition = rememberInfiniteTransition(label = "BlobBreathing")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BlobBreath"
    )

    val timeAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "BlobRotation"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val baseRadius = 80f * density

        // 1. Candidate Recording Ripple Vectors
        if (isListening) {
            // Translate decibels [-96..0] to scale factor
            val dbNormalized = (rawDb + 96f) / 96f // [0..1]
            val rippleCount = 3
            for (i in 0 until rippleCount) {
                val rippleScale = 1.0f + (dbNormalized * 0.8f) + (i * 0.3f)
                val alpha = (1f - (i.toFloat() / rippleCount)) * 0.25f * dbNormalized
                drawCircle(
                    color = Color(0xFF00FF88).copy(alpha = alpha),
                    radius = baseRadius * rippleScale,
                    center = Offset(cx, cy),
                    style = Stroke(width = 2f * density)
                )
            }
        }

        // 2. Fluid Morphing Circle-Net Structure
        val nodeCount = 10
        val points = mutableListOf<Offset>()
        val radOffset = Math.toRadians(timeAngle.toDouble()).toFloat()

        for (i in 0 until nodeCount) {
            val angle = (i * 2 * PI / nodeCount).toFloat() + radOffset
            // Inject wave morphing based on talking/thinking states
            val wave = when {
                isListening -> sin(angle * 3 + radOffset * 2) * 15f * density * springAmp
                isTalking -> sin(angle * 4 + radOffset * 3) * 18f * density
                isThinking -> cos(angle * 5 + radOffset * 4) * 8f * density * breathScale
                else -> sin(angle * 2 + radOffset) * 5f * density * breathScale
            }

            val dynamicRadius = (baseRadius * (if (isThinking || !isTalking && !isListening) breathScale else 1.0f)) + wave
            val px = cx + cos(angle) * dynamicRadius
            val py = cy + sin(angle) * dynamicRadius
            points.add(Offset(px, py))
        }

        // Draw radial lines connecting center to nodes (Circle-Net)
        if (isTalking || isListening) {
            points.forEach { pt ->
                drawLine(
                    color = blobColor.copy(alpha = 0.15f),
                    start = Offset(cx, cy),
                    end = pt,
                    strokeWidth = 1f * density
                )
            }
        }

        // Draw connections between nodes (The Net)
        val path = Path().apply {
            if (points.isNotEmpty()) {
                moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) {
                    val prev = points[i - 1]
                    val curr = points[i]
                    // Quadratic curves for organic fluidity
                    quadraticTo(
                        prev.x, prev.y,
                        (prev.x + curr.x) / 2f, (prev.y + curr.y) / 2f
                    )
                }
                val last = points.last()
                val first = points.first()
                quadraticTo(
                    last.x, last.y,
                    (last.x + first.x) / 2f, (last.y + first.y) / 2f
                )
                close()
            }
        }

        // Draw central fluid shape
        drawPath(
            path = path,
            brush = Brush.radialGradient(
                colors = listOf(
                    blobColor.copy(alpha = if (isThinking || !isTalking && !isListening) 0.05f else 0.4f),
                    blobColor.copy(alpha = 0.1f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = baseRadius * 1.5f
            )
        )

        // Draw structural outline
        drawPath(
            path = path,
            color = blobColor.copy(alpha = if (isThinking || !isTalking && !isListening) 0.3f else 0.8f),
            style = Stroke(width = 2f * density)
        )

        // Draw Active Expansion Nodes
        if (isTalking || isListening) {
            points.forEach { pt ->
                drawCircle(
                    color = blobColor,
                    radius = 4f * density,
                    center = pt
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.5f),
                    radius = 2f * density,
                    center = pt
                )
            }
        }

        // 3. Central Core Glowing Orb
        val coreRadius = baseRadius * 0.45f * (if (isThinking || !isTalking && !isListening) breathScale else 1.0f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.95f),
                    blobColor.copy(alpha = 0.8f),
                    blobColor.copy(alpha = 0.3f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = coreRadius
            ),
            radius = coreRadius,
            center = Offset(cx, cy)
        )
    }
}
