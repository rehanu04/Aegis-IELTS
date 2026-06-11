package com.aegis.ielts.features.speaking

import com.aegis.ielts.core.domain.SpeakingAssessmentResponse

/**
 * Sealed state hierarchy for the IELTS Speaking module screen.
 *
 * State machine transitions (implemented in Phase 2):
 *   Idle → MockTestActive(EXAMINER_SPEAKING)
 *       → MockTestActive(CANDIDATE_RECORDING)
 *       → MockTestActive(ANALYZING)
 *       → EvaluationComplete
 *       ↓ (abort)
 *   Idle
 */
sealed class SpeakingUiState {

    /** Initial state. Home page is shown with "Initialize Mock Test" CTA. */
    object Idle : SpeakingUiState()

    /**
     * Active exam session. [engineState] drives the orb animation and
     * diagnostic label displayed to the candidate.
     */
    data class MockTestActive(
        val engineState: ExaminerEngineState,
        val promptText: String = "Welcome to the IELTS speaking test. Can you tell me your full name, please?"
    ) : SpeakingUiState()

    /**
     * Gemini evaluation complete. [assessmentResponse] contains scores,
     * feedback, and silence telemetry for display in [DiagnosticReportPanel].
     */
    data class EvaluationComplete(
        val assessmentResponse: SpeakingAssessmentResponse
    ) : SpeakingUiState()

    /** Terminal error state — displayed inline with retry option. */
    data class Error(val message: String) : SpeakingUiState()
}

/**
 * Represents the operational state of the AI examiner engine during
 * an active [SpeakingUiState.MockTestActive] session.
 *
 * Drives:
 *  - [ParticleBlobOrb] animation parameters (isThinking / isTalking / isListening)
 *  - Diagnostic status label text and color
 *  - [AudioWaveformVisualizer] visibility
 */
enum class ExaminerEngineState {

    /** Waking up the Render backend from cold-start dormancy. */
    CONNECTING,

    /** Gemini TTS is delivering the examiner question or cue card. */
    EXAMINER_SPEAKING,

    /** AudioRecord is actively capturing the candidate's spoken response. */
    CANDIDATE_RECORDING,

    /** Audio has been captured; Gemini evaluation request is in-flight. */
    ANALYZING
}
