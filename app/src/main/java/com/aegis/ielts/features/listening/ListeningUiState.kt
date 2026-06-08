package com.aegis.ielts.features.listening

import com.aegis.ielts.features.listening.data.ListeningGradingReport
import com.aegis.ielts.features.listening.data.ListeningSection

/**
 * Sealed hierarchy representing UI states of the Listening module.
 */
sealed class ListeningUiState {

    /** Initial state showing instructions and CTAs. */
    object Idle : ListeningUiState()

    /**
     * Active mock test state. Tracks the current section being played/answered.
     */
    data class Active(
        val sections: List<ListeningSection>,
        val currentSectionIndex: Int,
        val isAudioPlaying: Boolean,
        val isFrozen: Boolean = false
    ) : ListeningUiState() {
        val currentSection: ListeningSection
            get() = sections[currentSectionIndex]
    }

    /**
     * Assessment complete, displaying diagnostic grading scorecard.
     */
    data class EvaluationComplete(
        val report: ListeningGradingReport
    ) : ListeningUiState()

    /**
     * Error state.
     */
    data class Error(val message: String) : ListeningUiState()
}
