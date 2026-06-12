package com.aegis.ielts.features.speaking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.ielts.core.audio.AudioCaptureEngine
import com.aegis.ielts.core.audio.AudioPlaybackEngine
import com.aegis.ielts.core.audio.PlaybackState
import com.aegis.ielts.core.domain.SpeakingAssessmentResponse
import com.aegis.ielts.core.domain.SpeakingNextQuestionRequest
import com.aegis.ielts.core.domain.SpeakingNextQuestionResponse
import com.aegis.ielts.core.network.GeminiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel for [IeltsSpeakingAssessmentScreen].
 *
 * Phase 1 delivers the full StateFlow contract required for compilation.
 * Phase 2 activates the full state machine:
 *   Idle → ListeningToPrompt → RecordingResponse → Analyzing → FeedbackDisplay
 *
 * All public [StateFlow]s use [SharingStarted.WhileSubscribed] with a 5-second
 * timeout to survive configuration changes without unnecessary re-subscriptions.
 */
@HiltViewModel
class SpeakingViewModel @Inject constructor(
    private val geminiRepository    : GeminiRepository,
    private val audioCaptureEngine  : AudioCaptureEngine,
    private val audioPlaybackEngine : AudioPlaybackEngine,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val isProcessingResponse = java.util.concurrent.atomic.AtomicBoolean(false)
    private var lastEvaluationResponse: SpeakingAssessmentResponse? = null
    private var isFinalFeedback = false

    private val part1Questions = listOf(
        "Welcome to the IELTS speaking test. Can you tell me your full name, please?",
        "Where are you from, and do you work or study?",
        "Let's talk about your free time. What hobbies do you enjoy the most?"
    )
    private val part2Question = "Describe a book or a movie that had a strong influence on you. You should say what it is, when you saw/read it, and explain why it influenced you."
    private val part3Questions = listOf(
        "In your opinion, how has the type of movies people watch changed over the past few decades?",
        "Do you think films should always have educational value, or is entertainment enough?",
        "Why do you think some local films fail to attract a global audience compared to big budget productions?"
    )
    private val allQuestions = part1Questions + part2Question + part3Questions

    private val dummyTranscripts = listOf(
        "My name is John Doe. I am taking the IELTS test to study abroad.",
        "I am from a small town in the countryside, and currently I am working as a junior software engineer.",
        "In my free time, I really enjoy reading books and playing tennis with my friends.",
        "I would like to describe the movie Inception. It had a strong influence on me because of its unique concept of dreams within dreams and how it explores sub-consciousness. I saw it a few years ago, and it really changed the way I think about storytelling.",
        "In my opinion, movies have become much more visual-effects-driven now compared to the past when character development and storyline were more important.",
        "I think films should primarily entertain, but having some educational or thought-provoking value makes them much more memorable and impactful.",
        "Local films often have very limited budgets and tackle cultural themes that might not translate well to global audiences compared to big Hollywood blockbusters."
    )

    private val accumulatedTranscripts = mutableListOf<String>()
    private val accumulatedPrompts = mutableListOf<String>()
    private var currentQuestionIdx = 0

    // ─── UI State ─────────────────────────────────────────────────────────────

    private val _uiState = MutableStateFlow<SpeakingUiState>(SpeakingUiState.Idle)

    /**
     * Primary screen state. Collected via [collectAsStateWithLifecycle] in
     * [IeltsSpeakingAssessmentScreen] to drive the `when(state)` branch.
     */
    val uiState: StateFlow<SpeakingUiState> = _uiState.asStateFlow()

    // ─── Elapsed Time ─────────────────────────────────────────────────────────

    private val _elapsedSeconds = MutableStateFlow(0)

    /**
     * Elapsed exam time in seconds.
     * Phase 2: driven by a ticker coroutine active during [SpeakingUiState.MockTestActive].
     */
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

    // ─── Audio Amplitude ──────────────────────────────────────────────────────

    /**
     * Normalized amplitude [0.0, 1.0] from the active audio capture session.
     * Derived from [AudioCaptureEngine.audioFrames] via [SharedFlow.map].
     *
     * WhileSubscribed(5000) keeps the upstream active for 5 seconds after the
     * last subscriber disappears (survives configuration change rotation).
     */
    val currentAmplitudeDb: StateFlow<Float> = audioCaptureEngine.audioFrames
        .map { frame -> frame.amplitudeDb }
        .stateIn(
            scope          = viewModelScope,
            started        = SharingStarted.WhileSubscribed(5_000L),
            initialValue   = 0f
        )

    private var stateMachineJob: Job? = null
    private var timerJob: Job? = null

    // ─── Actions ──────────────────────────────────────────────────────────────

    /**
     * Initializes the speaking test pipeline for [testId].
     * Orchestrates TTS examiner delivery → recording → Gemini evaluation.
     */
    fun startMockExamPipeline(testId: String) {
        if (_uiState.value is SpeakingUiState.MockTestActive) return
        
        lastEvaluationResponse = null
        isFinalFeedback = false
        currentQuestionIdx = 0
        accumulatedTranscripts.clear()
        accumulatedPrompts.clear()
        
        _uiState.value = SpeakingUiState.MockTestActive(
            engineState = ExaminerEngineState.CONNECTING,
            currentPart = 1,
            currentQuestionIndex = 0
        )
        
        stateMachineJob?.cancel()
        stateMachineJob = viewModelScope.launch {
            // Step 0: Check internet connection and ping Render backend to wake it up
            val connectivityManager = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            val network = connectivityManager?.activeNetwork
            if (network == null) {
                _uiState.value = SpeakingUiState.Error("Device Offline: Please check your internet connection and try again.")
                return@launch
            }

            val pingResult = geminiRepository.pingBackend()
            if (pingResult.isFailure) {
                _uiState.value = SpeakingUiState.Error("Server Offline: Backend is currently unavailable. Please try again later.")
                return@launch
            }

            val promptText = allQuestions[currentQuestionIdx]
            _uiState.value = SpeakingUiState.MockTestActive(
                engineState = ExaminerEngineState.EXAMINER_SPEAKING,
                promptText = promptText,
                currentPart = 1,
                currentQuestionIndex = currentQuestionIdx
            )
        }
    }

    fun onExaminerSpeakingCompleted() {
        val currentState = _uiState.value as? SpeakingUiState.MockTestActive ?: return
        if (currentState.engineState == ExaminerEngineState.EXAMINER_SPEAKING) {
            if (isFinalFeedback) {
                // Assessment is complete, transition to the scorecard!
                val response = lastEvaluationResponse ?: return
                _uiState.value = SpeakingUiState.EvaluationComplete(response)
            } else {
                viewModelScope.launch {
                    transitionToRecording(listOf(currentState.promptText))
                }
            }
        }
    }

    private suspend fun transitionToRecording(prompts: List<String>) {
        val currentState = _uiState.value as? SpeakingUiState.MockTestActive ?: return
        val activePart = currentState.currentPart
        val activeQuestionIdx = currentState.currentQuestionIndex

        _uiState.value = SpeakingUiState.MockTestActive(
            engineState = ExaminerEngineState.CANDIDATE_RECORDING,
            promptText = currentState.promptText,
            currentPart = activePart,
            currentQuestionIndex = activeQuestionIdx
        )
        
        // Start the elapsed time ticker
        startTimer()

        // Start capturing audio
        audioCaptureEngine.startCapture()

        // VAD 3.5s silence monitor loop (calibrated to standard IELTS speaking speed)
        var lastVoiceActivityTime = System.currentTimeMillis()
        var silenceTriggered = false
        val vadJob = viewModelScope.launch {
            audioCaptureEngine.audioFrames.collect { frame ->
                if (isProcessingResponse.get()) return@collect
                val rawDb = frame.amplitudeDb * 96f - 96f
                if (rawDb >= -40f) {
                    lastVoiceActivityTime = System.currentTimeMillis()
                } else {
                    val silenceDurationMs = System.currentTimeMillis() - lastVoiceActivityTime
                    if (silenceDurationMs >= 3500L) { // 3.5 seconds
                        if (isProcessingResponse.compareAndSet(false, true)) {
                            silenceTriggered = true
                        }
                    }
                }
            }
        }

        // Wait for max 120 seconds or until silence detection triggers
        val maxDurationMs = 120_000L
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < maxDurationMs && !silenceTriggered) {
            delay(100)
        }
        isProcessingResponse.set(true) // Ensure locked if timeout occurs
        vadJob.cancel()

        // Step 3: Stop capture
        val audioBytes = audioCaptureEngine.stopCapture()
        val telemetry = audioCaptureEngine.silenceTelemetry
        stopTimer()

        // Show loading state while communication with backend takes place
        _uiState.value = SpeakingUiState.MockTestActive(
            engineState = ExaminerEngineState.EXAMINER_SPEAKING,
            promptText = "Let me think about that...",
            currentPart = activePart,
            currentQuestionIndex = activeQuestionIdx
        )

        // Launch network request to get the follow-up question
        val audioBase64 = android.util.Base64.encodeToString(audioBytes, android.util.Base64.NO_WRAP)
        val request = SpeakingNextQuestionRequest(
            audio_base64 = audioBase64,
            previous_transcript = null,
            current_question_index = currentQuestionIdx,
            current_part = activePart
        )

        try {
            val result = geminiRepository.fetchNextSpeakingQuestion(request)
            result.onSuccess { response ->
                accumulatedTranscripts.add(response.transcript)
                accumulatedPrompts.add(currentState.promptText)

                if (currentQuestionIdx < 6) {
                    currentQuestionIdx++
                    val nextPart = if (currentQuestionIdx in 0..2) 1 else if (currentQuestionIdx == 3) 2 else 3
                    
                    isProcessingResponse.set(false)
                    _uiState.value = SpeakingUiState.MockTestActive(
                        engineState = ExaminerEngineState.EXAMINER_SPEAKING,
                        promptText = response.next_question,
                        currentPart = nextPart,
                        currentQuestionIndex = currentQuestionIdx
                    )
                } else {
                    // End of Part 3. Evaluate the entire combined transcripts and prompts!
                    _uiState.value = SpeakingUiState.MockTestActive(
                        engineState = ExaminerEngineState.ANALYZING,
                        promptText = "Evaluating Speaking Performance...",
                        currentPart = 3,
                        currentQuestionIndex = currentQuestionIdx
                    )

                    val combinedTranscript = accumulatedTranscripts.joinToString("\n")
                    val combinedPrompts = accumulatedPrompts.toList()

                    viewModelScope.launch {
                        val evalResult = geminiRepository.evaluateSpeaking(
                            audioBytes = audioBytes,
                            transcript = combinedTranscript,
                            prompts = combinedPrompts
                        )

                        evalResult.onSuccess { evalResponse ->
                            val finalResponse = evalResponse.copy(silenceTelemetry = telemetry)
                            lastEvaluationResponse = finalResponse
                            isFinalFeedback = true
                            
                            isProcessingResponse.set(false)
                            _uiState.value = SpeakingUiState.MockTestActive(
                                engineState = ExaminerEngineState.EXAMINER_SPEAKING,
                                promptText = finalResponse.overallFeedback,
                                currentPart = 3,
                                currentQuestionIndex = currentQuestionIdx
                            )
                        }.onFailure { error ->
                            isProcessingResponse.set(false)
                            _uiState.value = SpeakingUiState.Error(error.message ?: "Failed to evaluate speaking performance.")
                        }
                    }
                }
            }.onFailure { error ->
                isProcessingResponse.set(false)
                _uiState.value = SpeakingUiState.Error(error.message ?: "Failed to get next question.")
            }
        } catch (e: Exception) {
            isProcessingResponse.set(false)
            _uiState.value = SpeakingUiState.Error(e.message ?: "An error occurred during communication.")
        }
    }

    private fun startTimer() {
        _elapsedSeconds.value = 0
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _elapsedSeconds.value++
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    /**
     * Hard-terminates the exam session.
     * Called when the user confirms the abort dialog during [SpeakingUiState.MockTestActive].
     */
    fun terminateExamSession() {
        stateMachineJob?.cancel()
        stopTimer()
        audioCaptureEngine.stopCapture()  // Discard audio; no evaluation
        
        viewModelScope.launch {
            audioPlaybackEngine.stop()
        }
        
        _uiState.value    = SpeakingUiState.Idle
        _elapsedSeconds.value = 0
    }

    /**
     * Resets to idle after the [DiagnosticReportPanel] is dismissed.
     */
    fun resetToIdle() {
        _uiState.value    = SpeakingUiState.Idle
        _elapsedSeconds.value = 0
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        audioPlaybackEngine.release()
    }
}
