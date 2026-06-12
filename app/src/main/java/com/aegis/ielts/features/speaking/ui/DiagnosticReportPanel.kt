package com.aegis.ielts.features.speaking.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegis.ielts.core.domain.IeltsBandScore
import com.aegis.ielts.core.domain.SpeakingAssessmentResponse
import com.aegis.ielts.ui.theme.AccentBlue
import com.aegis.ielts.ui.theme.AccentError
import com.aegis.ielts.ui.theme.AccentGold
import com.aegis.ielts.ui.theme.AccentGreen
import com.aegis.ielts.ui.theme.SurfaceCard
import com.aegis.ielts.ui.theme.SurfaceCyan
import com.aegis.ielts.ui.theme.SurfaceSlateLight
import com.aegis.ielts.ui.theme.TextLight
import com.aegis.ielts.ui.theme.TextMuted

/**
 * Full-page evaluation report panel displayed after [SpeakingUiState.EvaluationComplete].
 *
 * Layout:
 *  1. Overall band score badge (large cyan numeral)
 *  2. Four criteria score bars (Fluency, Lexical, Grammar, Pronunciation)
 *  3. Overall examiner feedback card
 *  4. Silence/pause telemetry metrics (if pauses were detected)
 *  5. Return to Dashboard button
 */
@Composable
fun DiagnosticReportPanel(
    report  : SpeakingAssessmentResponse,
    onReturn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState  = rememberScrollState()
    val overallScore = report.overallScore

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(20.dp))

        // ── Overall Score Badge ────────────────────────────────────────────
        OverallScoreBadge(band = overallScore.overall)

        Spacer(Modifier.height(6.dp))

        Text(
            text  = "IELTS SPEAKING ASSESSMENT",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            letterSpacing = 2.sp
        )

        Spacer(Modifier.height(24.dp))

        // ── Criteria Score Bars ────────────────────────────────────────────
        CriteriaScoreCard {
            BandScoreRow(
                label    = "Fluency & Coherence",
                score    = IeltsBandScore(report.fluencyScore.coerceIn(0f, 9f)),
                feedback = report.detailedFeedback.fluencyFeedback,
                accent   = AccentGreen
            )
            BandScoreRow(
                label    = "Lexical Resource",
                score    = IeltsBandScore(report.lexicalScore.coerceIn(0f, 9f)),
                feedback = report.detailedFeedback.lexicalFeedback,
                accent   = AccentBlue
            )
            BandScoreRow(
                label    = "Grammatical Range",
                score    = IeltsBandScore(report.grammarScore.coerceIn(0f, 9f)),
                feedback = report.detailedFeedback.grammarFeedback,
                accent   = SurfaceCyan
            )
            BandScoreRow(
                label    = "Pronunciation",
                score    = IeltsBandScore(report.pronunciationScore.coerceIn(0f, 9f)),
                feedback = report.detailedFeedback.pronunciationFeedback,
                accent   = AccentGold
            )
        }

        Spacer(Modifier.height(16.dp))

        // ── Examiner Overall Feedback ──────────────────────────────────────
        if (report.feedback.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape    = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text  = "EXAMINER FEEDBACK",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        letterSpacing = 2.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text  = report.feedback,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextLight
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── Pause / Silence Telemetry ─────────────────────────────────────
        val telem = report.silenceTelemetry
        if (telem.silenceCount > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape    = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text  = "PAUSE METRICS",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        letterSpacing = 2.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        TelemetryPill(label = "Total Pauses", value = "${telem.silenceCount}")
                        TelemetryPill(
                            label = "Longest Pause",
                            value = "${"%.1f".format(telem.maxSilenceDurationMs / 1_000f)}s"
                        )
                        TelemetryPill(
                            label = "Total Pause",
                            value = "${"%.1f".format(telem.totalSilenceMs / 1_000f)}s"
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── IELTS Qualitative Feedback Descriptors ────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors   = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape    = RoundedCornerShape(12.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text  = "IELTS QUALITATIVE EVALUATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(12.dp))
                
                QualitativeFeedbackRow(label = "Fluency", feedback = report.detailedFeedback.fluencyFeedback)
                HorizontalDivider(color = SurfaceSlateLight, modifier = Modifier.padding(vertical = 8.dp))
                QualitativeFeedbackRow(label = "Coherence", feedback = report.detailedFeedback.coherenceFeedback)
                HorizontalDivider(color = SurfaceSlateLight, modifier = Modifier.padding(vertical = 8.dp))
                QualitativeFeedbackRow(label = "Lexical Resource", feedback = report.detailedFeedback.lexicalFeedback)
                HorizontalDivider(color = SurfaceSlateLight, modifier = Modifier.padding(vertical = 8.dp))
                QualitativeFeedbackRow(label = "Grammatical Range", feedback = report.detailedFeedback.grammarFeedback)
            }
        }
        Spacer(Modifier.height(16.dp))

        // ── Return Button ──────────────────────────────────────────────────
        Button(
            onClick  = onReturn,
            modifier = Modifier
                .fillMaxWidth(0.65f)
                .height(50.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = SurfaceSlateLight),
            shape    = RoundedCornerShape(8.dp)
        ) {
            Text(
                text          = "Return to Dashboard",
                color         = SurfaceCyan,
                letterSpacing = 1.2.sp
            )
        }

        Spacer(Modifier.height(36.dp))
    }
}

// ─── Sub-components ───────────────────────────────────────────────────────────

@Composable
private fun OverallScoreBadge(band: IeltsBandScore) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(148.dp)
            .background(
                brush = Brush.radialGradient(
                    listOf(SurfaceCyan.copy(alpha = 0.14f), Color.Transparent)
                ),
                shape = CircleShape
            )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(SurfaceSlateLight)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text       = band.toString(),
                    fontSize   = 38.sp,
                    fontWeight = FontWeight.Bold,
                    color      = SurfaceCyan
                )
                Text(
                    text      = "BAND",
                    style     = MaterialTheme.typography.labelSmall,
                    color     = TextMuted,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

@Composable
private fun CriteriaScoreCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape    = RoundedCornerShape(12.dp),
        content  = { Column(Modifier.padding(16.dp), content = content) }
    )
}

@Composable
private fun BandScoreRow(
    label   : String,
    score   : IeltsBandScore,
    feedback: String,
    accent  : Color
) {
    var expanded by remember { mutableStateOf(false) }

    val animatedProgress by animateFloatAsState(
        targetValue   = score.band / 9f,
        animationSpec = tween(durationMillis = 900),
        label         = "BandProgress_$label"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text  = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted
                )
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress     = { animatedProgress },
                    modifier     = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color        = accent,
                    trackColor   = SurfaceSlateLight,
                    strokeCap    = StrokeCap.Round,
                )
            }
            Spacer(Modifier.width(14.dp))
            Text(
                text       = score.toString(),
                fontWeight = FontWeight.Bold,
                color      = accent,
                fontSize   = 20.sp
            )
        }

        if (feedback.isNotBlank()) {
            TextButton(
                onClick        = { expanded = !expanded },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text  = if (expanded) "▲ Hide detail" else "▼ Show detail",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted.copy(alpha = 0.7f)
                )
            }
            if (expanded) {
                Text(
                    text     = feedback,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = TextLight.copy(alpha = 0.85f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun TelemetryPill(label: String, value: String, color: Color = SurfaceCyan) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text       = value,
            style      = MaterialTheme.typography.titleMedium,
            color      = color,
            fontWeight = FontWeight.Bold
        )
        Text(
            text      = label,
            style     = MaterialTheme.typography.labelSmall,
            color     = TextMuted,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun QualitativeFeedbackRow(label: String, feedback: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = SurfaceCyan,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = feedback,
            style = MaterialTheme.typography.bodyMedium,
            color = TextLight,
            lineHeight = 20.sp
        )
    }
}
