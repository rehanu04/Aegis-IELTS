package com.aegis.ielts.features.speaking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.ielts.core.audio.AudioCaptureEngine
import com.aegis.ielts.core.audio.AudioPlaybackEngine
import com.aegis.ielts.core.audio.PlaybackState
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
    private val audioPlaybackEngine : AudioPlaybackEngine
) : ViewModel() {

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
        
        _uiState.value = SpeakingUiState.MockTestActive(
            engineState = ExaminerEngineState.CONNECTING
        )
        
        stateMachineJob?.cancel()
        stateMachineJob = viewModelScope.launch {
            // Step 0: Ping Render backend to wake it up
            val pingResult = geminiRepository.pingBackend()
            if (pingResult.isFailure) {
                _uiState.value = SpeakingUiState.Error("Server is currently unavailable. Please try again.")
                return@launch
            }

            _uiState.value = SpeakingUiState.MockTestActive(
                engineState = ExaminerEngineState.EXAMINER_SPEAKING
            )
            
            // Step 1: Examiner speaking phase
            // We attempt to play an asset. If it fails (e.g. missing file), we catch the error 
            // and proceed to the next state to ensure the pipeline doesn't hang.
            val promptText = "Welcome to the IELTS speaking test. Can you tell me your full name, please?"
            val prompts = listOf(promptText)
            
            try {
                audioPlaybackEngine.playFromAsset("audio/part1_intro.mp3")
            } catch (e: Exception) {
                // Ignore missing asset exceptions during mock phase
            }

            // Wait for playback to finish (or error out if file is missing)
            audioPlaybackEngine.playbackState.collect { state ->
                if (state is PlaybackState.Completed || state is PlaybackState.Error) {
                    // Step 2: Transition to CANDIDATE_RECORDING automatically
                    transitionToRecording(prompts)
                    // Cancel collection once we've transitioned
                    throw kotlinx.coroutines.CancellationException("Playback finished")
                }
            }
        }
    }

    private suspend fun transitionToRecording(prompts: List<String>) {
        _uiState.value = SpeakingUiState.MockTestActive(
            engineState = ExaminerEngineState.CANDIDATE_RECORDING
        )
        
        // Start the elapsed time ticker
        startTimer()

        // Start capturing audio
        audioCaptureEngine.startCapture()

        // For this mock pipeline, we'll record for exactly 10 seconds before simulating the user finishing.
        // In a real scenario, this would be tied to silence detection or a manual "Stop" button.
        delay(10_000L)

        // Step 3: Stop capture and transition to ANALYZING
        val audioBytes = audioCaptureEngine.stopCapture()
        val telemetry = audioCaptureEngine.silenceTelemetry
        stopTimer()

        _uiState.value = SpeakingUiState.MockTestActive(
            engineState = ExaminerEngineState.ANALYZING
        )

        // Simulate Speech-to-Text transcript
        val dummyTranscript = "My name is John Doe. I am taking the IELTS test to study abroad."

        // Step 4: Evaluate via Gemini
        val result = geminiRepository.evaluateSpeaking(
            audioBytes = audioBytes,
            transcript = dummyTranscript,
            prompts = prompts
        )

        result.onSuccess { response ->
            // Inject captured telemetry into the response
            val finalResponse = response.copy(silenceTelemetry = telemetry)
            _uiState.value = SpeakingUiState.EvaluationComplete(finalResponse)
        }.onFailure { error ->
            _uiState.value = SpeakingUiState.Error(error.message ?: "Failed to evaluate speaking performance.")
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
