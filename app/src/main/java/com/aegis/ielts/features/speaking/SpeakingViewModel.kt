package com.aegis.ielts.features.speaking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.ielts.core.audio.AudioCaptureEngine
import com.aegis.ielts.core.audio.AudioPlaybackEngine
import com.aegis.ielts.core.audio.PlaybackState
import com.aegis.ielts.core.domain.SpeakingAssessmentResponse
import com.aegis.ielts.core.domain.SpeakingNextQuestionRequest
import com.aegis.ielts.core.domain.SpeakingNextQuestionResponse
import com.aegis.ielts.core.domain.ChatTurn
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.content.Intent
import android.os.Bundle

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

    private val isProcessingResponse = java.util.concurrent.atomic.AtomicBoolean(true)
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

    private val chatHistoryList = mutableListOf<ChatTurn>()
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

    // ─── Audio Amplitude & Speech Recognition ─────────────────────────────────

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    private val _currentAmplitude = MutableStateFlow(0f)
    val currentAmplitudeDb: StateFlow<Float> = _currentAmplitude.asStateFlow()

    private var speechRecognizer: android.speech.SpeechRecognizer? = null

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
        chatHistoryList.clear()
        
        stateMachineJob?.cancel()
        stateMachineJob = viewModelScope.launch {
            // Step 0: Check internet connection
            val connectivityManager = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
            val network = connectivityManager?.activeNetwork
            if (network == null) {
                _uiState.value = SpeakingUiState.Error("Device Offline: Please check your internet connection and try again.")
                return@launch
            }

            // Warm up backend asynchronously in the background so it doesn't block startup
            viewModelScope.launch {
                geminiRepository.pingBackend()
            }

            val promptText = allQuestions[currentQuestionIdx]
            chatHistoryList.add(ChatTurn(role = "Examiner", text = promptText))
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

        // Reset transcript and amplitude
        _liveTranscript.value = ""
        _currentAmplitude.value = 0f

        // VAD parameters
        var lastVoiceActivityTime = System.currentTimeMillis()

        // Start on-device speech recognition on Main thread
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            try {
                if (speechRecognizer == null) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                }
                
                val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault().toString())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }

                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        android.util.Log.d("SpeakingVM", "SpeechRecognizer: Ready")
                    }
                    override fun onBeginningOfSpeech() {
                        lastVoiceActivityTime = System.currentTimeMillis()
                    }
                    override fun onRmsChanged(rmsdB: Float) {
                        // Map rmsdB (typically -2 to 10) to [0f, 1f] for UI visualization
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                        _currentAmplitude.value = normalized
                    }
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) {
                        android.util.Log.e("SpeakingVM", "SpeechRecognizer error: $error")
                    }
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            _liveTranscript.value = matches[0]
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            _liveTranscript.value = matches[0]
                            lastVoiceActivityTime = System.currentTimeMillis()
                        }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
                speechRecognizer?.startListening(recognizerIntent)
            } catch (e: Exception) {
                android.util.Log.e("SpeakingVM", "Failed to start SpeechRecognizer: ${e.message}")
            }
        }

        // Start capturing raw PCM audio in background
        isProcessingResponse.set(false)
        audioCaptureEngine.startCapture()

        // VAD 4.0s silence monitor loop (calibrated to standard IELTS speaking speed)
        val vadJob = viewModelScope.launch {
            audioCaptureEngine.audioFrames.collect { frame ->
                if (isProcessingResponse.get()) return@collect
                
                // Fallback amplitude mapping from PCM capture if SpeechRecognizer isn't active
                if (_currentAmplitude.value == 0f) {
                    _currentAmplitude.value = frame.amplitudeDb
                }

                val rawDb = frame.amplitudeDb * 96f - 96f
                if (rawDb >= -35f) { // Calibrated from -40f to -35f
                    lastVoiceActivityTime = System.currentTimeMillis()
                } else {
                    val silenceDurationMs = System.currentTimeMillis() - lastVoiceActivityTime
                    if (silenceDurationMs >= 4000L) { // Calibrated from 3.5s to 4.0s
                        if (isProcessingResponse.compareAndSet(false, true)) {
                            android.util.Log.d("SpeakingVM", "Silence detection triggered at 4.0s")
                        }
                    }
                }
            }
        }

        // Wait for max 120 seconds or until silence detection triggers
        val maxDurationMs = 120_000L
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < maxDurationMs && !isProcessingResponse.get()) {
            delay(100)
        }
        isProcessingResponse.set(true) // Ensure locked if timeout occurs
        
        // Stop speech recognition
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                android.util.Log.e("SpeakingVM", "Error stopping SpeechRecognizer: ${e.message}")
            }
        }
        vadJob.cancel()

        // Stop capture
        val audioBytes = audioCaptureEngine.stopCapture()
        val telemetry = audioCaptureEngine.silenceTelemetry
        stopTimer()

        // Show loading state while communication with backend takes place
        // Transition to ExaminerEngineState.ANALYZING with empty prompt to suppress TTS thinking audio
        _uiState.value = SpeakingUiState.MockTestActive(
            engineState = ExaminerEngineState.ANALYZING,
            promptText = "",
            currentPart = activePart,
            currentQuestionIndex = activeQuestionIdx
        )

        // Launch network request to get the follow-up question
        val finalTranscript = _liveTranscript.value
        val audioBase64 = android.util.Base64.encodeToString(audioBytes, android.util.Base64.NO_WRAP)
        
        // Add candidate's turn to chatHistoryList before sending
        chatHistoryList.add(ChatTurn(role = "Candidate", text = finalTranscript))

        val request = SpeakingNextQuestionRequest(
            audio_base64 = audioBase64,
            previous_transcript = finalTranscript.ifBlank { null },
            current_question_index = currentQuestionIdx,
            current_part = activePart,
            chat_history = Json.encodeToString(chatHistoryList)
        )

        try {
            val result = geminiRepository.fetchNextSpeakingQuestion(request)
            result.onSuccess { response ->
                // Clean/correct candidate's last turn from the backend's transcribed response
                if (chatHistoryList.isNotEmpty() && chatHistoryList.last().role == "Candidate") {
                    chatHistoryList.removeAt(chatHistoryList.size - 1)
                }
                chatHistoryList.add(ChatTurn(role = "Candidate", text = response.transcript))

                if (!response.is_test_complete) {
                    currentQuestionIdx = response.next_question_index
                    
                    // Add next Examiner question to chatHistoryList
                    chatHistoryList.add(ChatTurn(role = "Examiner", text = response.next_question))

                    _uiState.value = SpeakingUiState.MockTestActive(
                        engineState = ExaminerEngineState.EXAMINER_SPEAKING,
                        promptText = response.next_question,
                        currentPart = response.next_part,
                        currentQuestionIndex = currentQuestionIdx
                    )
                } else {
                    // End of Speaking test. Evaluate the entire combined transcripts and prompts!
                    _uiState.value = SpeakingUiState.MockTestActive(
                        engineState = ExaminerEngineState.ANALYZING,
                        promptText = "Evaluating Speaking Performance...",
                        currentPart = response.next_part,
                        currentQuestionIndex = currentQuestionIdx
                    )

                    val combinedTranscript = chatHistoryList.filter { it.role == "Candidate" }.joinToString("\n") { it.text }
                    val combinedPrompts = chatHistoryList.filter { it.role == "Examiner" }.map { it.text }

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
                            
                            _uiState.value = SpeakingUiState.MockTestActive(
                                engineState = ExaminerEngineState.EXAMINER_SPEAKING,
                                promptText = finalResponse.overallFeedback,
                                currentPart = response.next_part,
                                currentQuestionIndex = currentQuestionIdx
                            )
                        }.onFailure { error ->
                            _uiState.value = SpeakingUiState.Error(error.message ?: "Failed to evaluate speaking performance.")
                        }
                    }
                }
            }.onFailure { error ->
                _uiState.value = SpeakingUiState.Error(error.message ?: "Failed to get next question.")
            }
        } catch (e: Exception) {
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
        
        speechRecognizer?.destroy()
        speechRecognizer = null
        _liveTranscript.value = ""
        
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
        speechRecognizer?.destroy()
        speechRecognizer = null
        audioPlaybackEngine.release()
    }
}
