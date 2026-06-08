package com.aegis.ielts.features.reading

import com.aegis.ielts.features.reading.data.ReadingGradingReport

/**
 * Sealed state hierarchy representing the lifecycle of the IELTS Reading module.
 *
 * Transitions:
 *   Idle -> MockTestActive
 *        -> EvaluationComplete (via submission or timer exhaustion)
 */
sealed class ReadingUiState {

    /**
     * Initial screen state. Displays instructions, length, and the CTA to start the assessment.
     */
    object Idle : ReadingUiState()

    /**
     * Active IELTS Reading assessment state.
     * Contains the live test configuration and answer vector.
     */
    data class MockTestActive(
        val isFrozen: Boolean = false
    ) : ReadingUiState()

    /**
     * Graded evaluation screen state. Renders the scorecard, band score,
     * and a detailed question-by-question grammatical validation breakdown.
     */
    data class EvaluationComplete(
        val report: ReadingGradingReport
    ) : ReadingUiState()

    /**
     * Error screen state.
     */
    data class Error(val message: String) : ReadingUiState()
}
