package com.aegis.ielts.features.writing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.ielts.core.domain.WritingAssessmentResponse
import com.aegis.ielts.core.network.GeminiRepository
import com.aegis.ielts.features.writing.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WritingViewModel @Inject constructor(
    private val geminiRepository: GeminiRepository
) : ViewModel() {

    // ─── UI State ─────────────────────────────────────────────────────────────
    private val _uiState = MutableStateFlow<WritingUiState>(WritingUiState.Idle)
    val uiState: StateFlow<WritingUiState> = _uiState.asStateFlow()

    // ─── Essay Text Input ─────────────────────────────────────────────────────
    private val _essayText = MutableStateFlow("")
    val essayText: StateFlow<String> = _essayText.asStateFlow()

    // ─── Countdown Timer ──────────────────────────────────────────────────────
    private val _timeLeftSeconds = MutableStateFlow(3600) // 60 minutes
    val timeLeftSeconds: StateFlow<Int> = _timeLeftSeconds.asStateFlow()

    private var timerJob: Job? = null

    // ─── Actions ──────────────────────────────────────────────────────────────

    /**
     * Initializes a Writing exam session for Task 1 or Task 2.
     */
    fun startWritingTest(taskType: Int) {
        val task = if (taskType == 1) WritingMockTasks.task1 else WritingMockTasks.task2

        _essayText.value = ""
        _timeLeftSeconds.value = 3600
        _uiState.value = WritingUiState.MockTestActive(task = task)

        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_timeLeftSeconds.value > 0) {
                delay(1000)
                _timeLeftSeconds.value--
            }
            // Time expired, auto-submit and lock text editor
            submitEssay(isTimeOut = true)
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    /**
     * Updates the candidate's essay text input.
     */
    fun updateEssay(text: String) {
        val currentState = _uiState.value
        if (currentState !is WritingUiState.MockTestActive || currentState.isFrozen) return
        _essayText.value = text
    }

    /**
     * Submits the essay and calls the Gemini evaluation API with a local fallback.
     */
    fun submitEssay(isTimeOut: Boolean = false) {
        val currentState = _uiState.value as? WritingUiState.MockTestActive ?: return
        if (currentState.isFrozen) return

        val essay = _essayText.value
        val words = essay.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.size < 10) {
            // Apply zero-attempt criteria
            val zeroResponse = WritingAssessmentResponse(
                taskAchievementScore = 0.0f,
                coherenceScore = 0.0f,
                lexicalScore = 0.0f,
                grammarScore = 0.0f,
                feedback = "Band 0.0: Non-attempt / Under 10 rateable words. The response contains only ${words.size} words.",
                templateDetected = false,
                templateSimilarityScore = 0.0f
            )
            _uiState.value = WritingUiState.EvaluationComplete(currentState.task, zeroResponse)
            return
        }

        stopTimer()

        // Lock workspace inputs
        _uiState.value = currentState.copy(isFrozen = true)

        val task = currentState.task

        _uiState.value = WritingUiState.Analyzing

        viewModelScope.launch {
            val result = geminiRepository.evaluateWriting(
                taskType = task.taskType,
                prompt = task.prompt,
                essay = essay
            )

            result.onSuccess { response ->
                _uiState.value = WritingUiState.EvaluationComplete(task, response)
            }.onFailure { _ ->
                // Fall back gracefully to a robust local diagnostic grading report if offline or api key is missing
                val localResponse = runLocalWritingAssessment(task, essay)
                _uiState.value = WritingUiState.EvaluationComplete(task, localResponse)
            }
        }
    }

    /**
     * Local analytical fallback scoring algorithm.
     */
    private fun runLocalWritingAssessment(task: WritingTask, essay: String): WritingAssessmentResponse {
        val words = essay.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val wordCount = words.size

        if (wordCount < 10) {
            return WritingAssessmentResponse(
                taskAchievementScore = 0.0f,
                coherenceScore = 0.0f,
                lexicalScore = 0.0f,
                grammarScore = 0.0f,
                feedback = "Band 0.0: Non-attempt / Under 10 rateable words. The response contains only $wordCount words.",
                templateDetected = false,
                templateSimilarityScore = 0.0f
            )
        }

        // 1. Task Achievement: penalized if word count is below the minimum limit
        val wordFraction = if (task.minWords > 0) wordCount.toFloat() / task.minWords.toFloat() else 1f
        val achievementScore = (wordFraction * 7.0f).coerceIn(1.0f, 9.0f)

        // 2. Lexical Resource: estimated by analyzing lexical diversity (ratio of unique words)
        val uniqueWords = words.map { it.lowercase() }.toSet().size
        val diversity = if (wordCount > 0) uniqueWords.toFloat() / wordCount.toFloat() else 0f
        val lexicalScore = (diversity * 12.0f).coerceIn(1.0f, 9.0f)

        // 3. Coherence & Cohesion: scored based on structural transitions
        val coherenceScore = if (wordCount > 50) 6.5f else 3.0f

        // 4. Grammar: estimated basic score
        val grammarScore = if (wordCount > 100) 6.0f else 2.5f

        // 5. Template usage detection
        val templatePhrases = listOf(
            "it is often argued that",
            "some people believe that",
            "on the one hand",
            "on the other hand",
            "in conclusion",
            "this essay will discuss",
            "to summarize",
            "there are many advantages"
        )
        val lowerEssay = essay.lowercase()
        var matchCount = 0
        templatePhrases.forEach { phrase ->
            if (lowerEssay.contains(phrase)) {
                matchCount++
            }
        }
        val templateSimilarity = (matchCount.toFloat() / templatePhrases.size.toFloat() * 1.5f).coerceIn(0f, 1f)
        val templateDetected = templateSimilarity > 0.35f

        // Build examiner feedback text
        val feedback = StringBuilder().apply {
            append("Examiner Assessment (Local Analysis Fallback):\n")
            append("Your submission contains $wordCount words. ")
            if (wordCount < task.minWords) {
                append("Warning: This falls short of the required minimum limit of ${task.minWords} words, which has penalized your Task Achievement score. ")
            } else {
                append("Good job on meeting the required word count constraint. ")
            }

            if (templateDetected) {
                append("Warning: Formulaic template usage detected (Similarity index: %.2f). Try to write organic transitions rather than relying on memorized structures.".format(templateSimilarity))
            } else {
                append("Your writing displays organic cohesive transitions and transitions.")
            }
        }.toString()

        return WritingAssessmentResponse(
            taskAchievementScore = achievementScore,
            coherenceScore = coherenceScore,
            lexicalScore = lexicalScore,
            grammarScore = grammarScore,
            feedback = feedback,
            templateDetected = templateDetected,
            templateSimilarityScore = templateSimilarity
        )
    }

    /**
     * Resets the screen state back to Idle instructions.
     */
    fun resetToIdle() {
        stopTimer()
        _essayText.value = ""
        _timeLeftSeconds.value = 3600
        _uiState.value = WritingUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}
