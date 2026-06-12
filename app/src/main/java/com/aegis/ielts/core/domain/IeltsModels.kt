package com.aegis.ielts.core.domain

import kotlinx.serialization.Serializable
import kotlin.math.floor

/**
 * Canonical IELTS band score with the mandated official rounding formula:
 *
 *   B_overall(x) = floor(2x + 0.5) / 2
 *
 * This rounds to the nearest 0.5 band, with the exact midpoint (x.25) rounding up.
 *
 * Verified examples:
 *   raw=6.75  → band=7.0   (floor(13.5 + 0.5) / 2 = floor(14.0) / 2 = 7.0)
 *   raw=6.74  → band=6.5   (floor(13.48 + 0.5) / 2 = floor(13.98) / 2 = 6.5)
 *   raw=6.25  → band=6.5   (floor(12.5 + 0.5) / 2 = floor(13.0) / 2 = 6.5)
 *   raw=5.875 → band=6.0   (floor(11.75 + 0.5) / 2 = floor(12.25) / 2 = 6.0)
 */
data class IeltsBandScore(val raw: Float) {

    init {
        require(raw in 0f..9f) {
            "Raw IELTS score must be in [0.0, 9.0]; received: $raw"
        }
    }

    /** Rounded band score on the official 0.0–9.0 scale in 0.5 steps. */
    val band: Float
        get() = floor(2.0 * raw + 0.5).toFloat() / 2.0f

    override fun toString(): String = "%.1f".format(band)
}

// ─── Speaking Score Aggregation ───────────────────────────────────────────────

/**
 * Aggregated IELTS Speaking score computed from the 4 official assessment criteria.
 * Overall band is derived from the arithmetic mean of the 4 criteria bands,
 * then passed through the official rounding formula.
 */
data class OverallSpeakingScore(
    val fluency      : IeltsBandScore,
    val lexical      : IeltsBandScore,
    val grammar      : IeltsBandScore,
    val pronunciation: IeltsBandScore
) {
    val overall: IeltsBandScore
        get() = IeltsBandScore(
            ((fluency.band + lexical.band + grammar.band + pronunciation.band) / 4f)
                .coerceIn(0f, 9f)
        )
}

// ─── Writing Score Aggregation ────────────────────────────────────────────────

/**
 * Aggregated IELTS Writing score from the 4 official assessment criteria.
 */
data class OverallWritingScore(
    val taskAchievement  : IeltsBandScore,
    val coherenceCohesion: IeltsBandScore,
    val lexical          : IeltsBandScore,
    val grammar          : IeltsBandScore
) {
    val overall: IeltsBandScore
        get() = IeltsBandScore(
            ((taskAchievement.band + coherenceCohesion.band + lexical.band + grammar.band) / 4f)
                .coerceIn(0f, 9f)
        )
}

// ─── Silence Telemetry ────────────────────────────────────────────────────────

/**
 * Pause/silence metrics captured by [AudioCaptureEngine] during a speaking session.
 * Emitted by the engine and embedded into the evaluation response payload.
 */
@Serializable
data class SilenceTelemetry(
    val silenceCount       : Int  = 0,
    val maxSilenceDurationMs: Long = 0L,
    val totalSilenceMs     : Long = 0L
)

// ─── Speaking Assessment Response ─────────────────────────────────────────────

@Serializable
data class HesitationProfile(
    val withinClausePauses: Int,
    val betweenClausePauses: Int,
    val totalSilenceMs: Int
)

@Serializable
data class FluencyCoherenceMetric(
    val score: Float,
    val feedback: String,
    val hesitationProfile: HesitationProfile,
    val fillerDensityIndex: Float
)

@Serializable
data class LexicalAssessmentMetric(
    val score: Float,
    val feedback: String,
    val lexicalAsymmetryIndex: Float
)

@Serializable
data class GrammarAssessmentMetric(
    val score: Float,
    val feedback: String
)

@Serializable
data class PronunciationAssessmentMetric(
    val score: Float,
    val feedback: String
)

@Serializable
data class SpeakingDetailedFeedback(
    val fluencyFeedback      : String,
    val coherenceFeedback    : String,
    val lexicalFeedback      : String,
    val grammarFeedback      : String,
    val pronunciationFeedback: String
)

/**
 * Advanced 2026 Structural Examiner Metrics for Speaking.
 */
@Serializable
data class AdvancedSpeakingMetrics(
    val fillerDensityIndex: Float,          // FDI: Filler words per 100 words
    val lexicalAsymmetryIndex: Float,       // LAI: Imbalance in vocabulary complexity
    val withinClausePauses: Int,            // Count of unnatural pauses within clauses
    val preMemorizedSpeechDetected: Boolean // Flag for pre-memorized speech patterns
)

/**
 * Structured JSON response from Gemini for IELTS Speaking evaluation.
 * Scores are raw floats in [0.0, 9.0]; use [overallScore] to access the
 * official [IeltsBandScore] objects with proper rounding.
 */
@Serializable
data class SpeakingAssessmentResponse(
    val fluencyCoherence: FluencyCoherenceMetric,
    val coherenceFeedback: String = "Ideas are logically structured with appropriate cohesive devices.",
    val lexicalResource: LexicalAssessmentMetric,
    val grammaticalRangeAccuracy: GrammarAssessmentMetric,
    val pronunciation: PronunciationAssessmentMetric,
    val overallFeedback: String,
    val silenceTelemetry: SilenceTelemetry = SilenceTelemetry()
) {
    val fluencyScore: Float get() = fluencyCoherence.score
    val lexicalScore: Float get() = lexicalResource.score
    val grammarScore: Float get() = grammaticalRangeAccuracy.score
    val pronunciationScore: Float get() = pronunciation.score
    val feedback: String get() = overallFeedback

    val detailedFeedback: SpeakingDetailedFeedback
        get() = SpeakingDetailedFeedback(
            fluencyFeedback = fluencyCoherence.feedback,
            coherenceFeedback = coherenceFeedback,
            lexicalFeedback = lexicalResource.feedback,
            grammarFeedback = grammaticalRangeAccuracy.feedback,
            pronunciationFeedback = pronunciation.feedback
        )

    val advancedMetrics: AdvancedSpeakingMetrics
        get() = AdvancedSpeakingMetrics(
            fillerDensityIndex = fluencyCoherence.fillerDensityIndex,
            lexicalAsymmetryIndex = lexicalResource.lexicalAsymmetryIndex,
            withinClausePauses = fluencyCoherence.hesitationProfile.withinClausePauses,
            preMemorizedSpeechDetected = false
        )

    val overallScore: OverallSpeakingScore
        get() = OverallSpeakingScore(
            fluency       = IeltsBandScore(fluencyScore.coerceIn(0f, 9f)),
            lexical       = IeltsBandScore(lexicalScore.coerceIn(0f, 9f)),
            grammar       = IeltsBandScore(grammarScore.coerceIn(0f, 9f)),
            pronunciation = IeltsBandScore(pronunciationScore.coerceIn(0f, 9f))
        )
}

// ─── Writing Assessment Response ──────────────────────────────────────────────

@Serializable
data class WritingCriteriaScores(
    val taskAchievement: Float,
    val coherenceCohesion: Float,
    val lexicalResource: Float,
    val grammaticalRangeAccuracy: Float
)

@Serializable
data class TemplateDetectionResult(
    val templateDetected: Boolean,
    val templateSimilarityScore: Float,
    val lexicalAsymmetryIndex: Float
)

@Serializable
data class GrammarCorrection(
    val original: String,
    val corrected: String,
    val explanation: String,
    val errorType: String
)

/**
 * Structured JSON response from Gemini for IELTS Writing evaluation.
 * Includes template detection flags for academic integrity enforcement.
 */
@Serializable
data class WritingAssessmentResponse(
    val criteriaScores: WritingCriteriaScores,
    val templateDetection: TemplateDetectionResult,
    val grammarCorrections: List<GrammarCorrection> = emptyList(),
    val overallFeedback: String
) {
    val taskAchievementScore: Float get() = criteriaScores.taskAchievement
    val coherenceScore: Float get() = criteriaScores.coherenceCohesion
    val lexicalScore: Float get() = criteriaScores.lexicalResource
    val grammarScore: Float get() = criteriaScores.grammaticalRangeAccuracy
    val feedback: String get() = overallFeedback
    val templateDetected: Boolean get() = templateDetection.templateDetected
    val templateSimilarityScore: Float get() = templateDetection.templateSimilarityScore

    val overallScore: OverallWritingScore
        get() = OverallWritingScore(
            taskAchievement   = IeltsBandScore(taskAchievementScore.coerceIn(0f, 9f)),
            coherenceCohesion = IeltsBandScore(coherenceScore.coerceIn(0f, 9f)),
            lexical           = IeltsBandScore(lexicalScore.coerceIn(0f, 9f)),
            grammar           = IeltsBandScore(grammarScore.coerceIn(0f, 9f))
        )

    constructor(
        taskAchievementScore: Float,
        coherenceScore: Float,
        lexicalScore: Float,
        grammarScore: Float,
        feedback: String,
        templateDetected: Boolean,
        templateSimilarityScore: Float
    ) : this(
        criteriaScores = WritingCriteriaScores(
            taskAchievement = taskAchievementScore,
            coherenceCohesion = coherenceScore,
            lexicalResource = lexicalScore,
            grammaticalRangeAccuracy = grammarScore
        ),
        templateDetection = TemplateDetectionResult(
            templateDetected = templateDetected,
            templateSimilarityScore = templateSimilarityScore,
            lexicalAsymmetryIndex = 0.25f
        ),
        grammarCorrections = emptyList(),
        overallFeedback = feedback
    )
}

@Serializable
data class SpeakingNextQuestionRequest(
    val audio_base64: String?,
    val previous_transcript: String?,
    val current_question_index: Int,
    val current_part: Int
)

@Serializable
data class SpeakingNextQuestionResponse(
    val transcript: String,
    val next_question: String
)
