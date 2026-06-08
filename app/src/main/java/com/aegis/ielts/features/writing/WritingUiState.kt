package com.aegis.ielts.features.writing

import com.aegis.ielts.core.domain.WritingAssessmentResponse
import com.aegis.ielts.features.writing.data.WritingTask

/**
 * Sealed state hierarchy representing the lifecycle of the IELTS Writing module.
 */
sealed class WritingUiState {

    /** Initial state showing instructions and CTAs. */
    object Idle : WritingUiState()

    /**
     * Active mock test state. Tracks the current task configuration and active lock.
     */
    data class MockTestActive(
        val task: WritingTask,
        val isFrozen: Boolean = false
    ) : WritingUiState()

    /**
     * AI analysis is running in the background.
     */
    object Analyzing : WritingUiState()

    /**
     * Graded evaluation screen state. Renders scorecard, band score,
     * template plagiarism indices, and detailed diagnostic criteria.
     */
    data class EvaluationComplete(
        val task: WritingTask,
        val response: WritingAssessmentResponse
    ) : WritingUiState()

    /**
     * Error state.
     */
    data class Error(val message: String) : WritingUiState()
}
