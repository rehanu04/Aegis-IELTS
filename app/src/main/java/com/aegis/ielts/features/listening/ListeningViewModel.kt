package com.aegis.ielts.features.listening

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.ielts.core.audio.AudioPlaybackEngine
import com.aegis.ielts.core.audio.PlaybackState
import com.aegis.ielts.core.domain.IeltsBandScore
import com.aegis.ielts.features.listening.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject
import kotlin.random.Random
import android.content.Context
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import android.net.Uri
import com.aegis.ielts.core.network.GeminiRepository

@HiltViewModel
class ListeningViewModel @Inject constructor(
    private val geminiRepository: GeminiRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = java.util.Locale.UK
            }
        }
    }

    private var currentScript = ""

    // ─── UI State ─────────────────────────────────────────────────────────────
    private val _uiState = MutableStateFlow<ListeningUiState>(ListeningUiState.Idle)
    val uiState: StateFlow<ListeningUiState> = _uiState.asStateFlow()

    // ─── User Answers ─────────────────────────────────────────────────────────
    private val _answers = MutableStateFlow<Map<String, String>>(emptyMap())
    val answers: StateFlow<Map<String, String>> = _answers.asStateFlow()

    // ─── Word / Character Validation Errors ────────────────────────────────────
    private val _inputErrors = MutableStateFlow<Map<String, String?>>(emptyMap())
    val inputErrors: StateFlow<Map<String, String?>> = _inputErrors.asStateFlow()

    // ─── Audio Playback Progress Indicator ────────────────────────────────────
    private val _audioPlaybackProgress = MutableStateFlow(0f)
    val audioPlaybackProgress: StateFlow<Float> = _audioPlaybackProgress.asStateFlow()

    private val _audioBufferProgress = MutableStateFlow(0f)
    val audioBufferProgress: StateFlow<Float> = _audioBufferProgress.asStateFlow()

    private val _countdownSeconds = MutableStateFlow<Long?>(null)
    val countdownSeconds: StateFlow<Long?> = _countdownSeconds.asStateFlow()

    private var playbackStateJob: Job? = null
    private var progressTickerJob: Job? = null
    private var countdownEndTimeMs: Long? = null

    // ─── Actions ──────────────────────────────────────────────────────────────
    
    // ... rest of actions ...


    /**
     * Samples accents according to probabilities and shuffles sections (Unpredictable Task Router).
     */
    fun startListeningAssessment() {
        val mockTest = com.aegis.ielts.features.listening.data.MockExamData.tests.random()
        val unifiedSection = mockTest.section
        currentScript = mockTest.script

        val activeSections = listOf(unifiedSection)

        // Clear state
        _answers.value = activeSections.flatMap { it.questions }.associate { it.id to "" }
        _inputErrors.value = emptyMap()
        _audioPlaybackProgress.value = 0f
        _audioBufferProgress.value = 0f
        _countdownSeconds.value = null

        _uiState.value = ListeningUiState.Active(
            sections = activeSections,
            currentSectionIndex = 0,
            isAudioPlaying = false,
            isAudioStarted = false
        )
    }

    fun beginPlaybackFromPending() {
        // Obsolete in single-screen container
    }

    fun startSectionAudio() {
        val currentState = _uiState.value as? ListeningUiState.Active ?: return
        if (currentState.isAudioStarted) return
        
        _uiState.value = currentState.copy(
            isAudioStarted = true
        )
        
        playCurrentSectionAudio(currentState.currentSection)
    }

    private fun playCurrentSectionAudio(section: ListeningSection) {
        playbackStateJob?.cancel()
        progressTickerJob?.cancel()

        _uiState.value = (_uiState.value as ListeningUiState.Active).copy(isAudioPlaying = true)
        
        val locale = when (section.accent) {
            Accent.STANDARD -> {
                if (section.sectionNumber == 4) java.util.Locale.US else java.util.Locale.UK
            }
            Accent.AUSTRALIAN -> java.util.Locale.forLanguageTag("en-AU")
            Accent.EUROPEAN -> java.util.Locale.CANADA
            Accent.SOUTH_ASIAN -> java.util.Locale.forLanguageTag("en-IN")
            Accent.AFRICAN -> java.util.Locale.forLanguageTag("en-ZA")
        }
        tts?.setLanguage(locale)

        val lines = currentScript.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isNotEmpty()) {
            speakLine(lines[0], queueMode = TextToSpeech.QUEUE_FLUSH)
            for (i in 1 until lines.size) {
                speakLine(lines[i], queueMode = TextToSpeech.QUEUE_ADD)
            }
        }
        
        startProgressIndicatorTicker()
    }

    private fun speakLine(line: String, queueMode: Int) {
        if (line.startsWith("Speaker A:")) {
            val text = line.substringAfter("Speaker A:").trim()
            tts?.setPitch(0.8f)
            tts?.speak(text, queueMode, null, "TTS_LINE_${System.nanoTime()}")
        } else if (line.startsWith("Speaker B:")) {
            val text = line.substringAfter("Speaker B:").trim()
            tts?.setPitch(1.2f)
            tts?.speak(text, queueMode, null, "TTS_LINE_${System.nanoTime()}")
        } else {
            tts?.setPitch(1.0f)
            tts?.speak(line, queueMode, null, "TTS_LINE_${System.nanoTime()}")
        }
    }

    private fun startProgressIndicatorTicker() {
        progressTickerJob?.cancel()
        
        val totalDurationSeconds = 150L // Approx script duration + 60s
        _countdownSeconds.value = totalDurationSeconds

        progressTickerJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            var elapsed = 0L
            while (true) {
                val currentState = _uiState.value as? ListeningUiState.Active ?: break
                if (currentState.isFrozen) break

                val currentCountdown = _countdownSeconds.value ?: break
                if (currentCountdown > 0) {
                    _countdownSeconds.value = currentCountdown - 1
                    elapsed++
                    
                    _audioPlaybackProgress.value = (elapsed.toFloat() / totalDurationSeconds.toFloat()).coerceIn(0f, 1f)
                    _audioBufferProgress.value = _audioPlaybackProgress.value
                } else {
                    _countdownSeconds.value = 0
                    submitListeningTest() // Freeze everything exactly at 0.00s
                    break
                }

                delay(1000L)
            }
        }
    }

    private fun updateActiveAudioPlayingState(isPlaying: Boolean) {
        val currentState = _uiState.value
        if (currentState is ListeningUiState.Active) {
            _uiState.value = currentState.copy(isAudioPlaying = isPlaying)
        }
    }

    /**
     * Saves answer updates. Handles alphanumeric, word, and character limit validations on-device.
     */
    fun saveAnswer(questionId: String, answer: String) {
        val currentState = _uiState.value
        if (currentState !is ListeningUiState.Active || currentState.isFrozen) return

        // Update Answer Map
        val updatedAnswers = _answers.value.toMutableMap()
        updatedAnswers[questionId] = answer.uppercase()
        _answers.value = updatedAnswers

        // Find the question to validate
        val question = currentState.sections
            .flatMap { it.questions }
            .find { it.id == questionId }

        if (question is ListeningQuestion.FormCompletion) {
            val error = validateFormCompletion(answer, question.wordLimit, question.charLimit)
            val updatedErrors = _inputErrors.value.toMutableMap()
            updatedErrors[questionId] = error
            _inputErrors.value = updatedErrors
        }
    }

    private fun validateFormCompletion(input: String, wordLimit: Int, charLimit: Int): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        // 1. Check Alphanumeric Characters (and spaces/hyphens)
        if (!trimmed.all { it.isLetterOrDigit() || it.isWhitespace() || it == '-' }) {
            return "Contains invalid characters."
        }

        // 2. Check Character Limit
        if (trimmed.length > charLimit) {
            return "Exceeds max limit ($charLimit chars)."
        }

        // 3. Check Word Limit
        val words = trimmed.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.size > wordLimit) {
            return "Exceeds word limit (max $wordLimit)."
        }

        return null
    }

    /**
     * Transition to the next listening section.
     */
    fun advanceToNextSection() {
        // Obsolete: Rebuilt to rely entirely on a unified mock track module with one global timer.
    }

    /**
     * Stops ExoPlayer and grades the test.
     */
    fun submitListeningTest() {
        val currentState = _uiState.value as? ListeningUiState.Active ?: return
        if (currentState.isFrozen) return

        tts?.stop()

        // Freeze interactive nodes
        _uiState.value = currentState.copy(isFrozen = true)

        playbackStateJob?.cancel()
        progressTickerJob?.cancel()

        // Count scores
        val allQuestions = currentState.sections.flatMap { it.questions }
        var correctCount = 0
        allQuestions.forEach { question ->
            val userAns = _answers.value[question.id].orEmpty().trim().uppercase()
            val correctAns = question.correctAnswer.trim().uppercase()
            if (userAns == correctAns) {
                correctCount++
            }
        }

        // Convert correct answers (0-8) to IELTS Band Score using linear scaling mapped to Band Score
        // 8 correct = 9.0, 7 = 8.0, 6 = 7.0, 5 = 6.0, 4 = 5.0, 3 = 4.0, 2 = 3.0, 1 = 2.0, 0 = 0.0
        val rawScale = (correctCount * 1.125f).coerceIn(0f, 9f)
        val band = IeltsBandScore(rawScale).band

        val correctAnswersMap = allQuestions.associate { it.id to it.correctAnswer }

        val report = ListeningGradingReport(
            rawScore = correctCount,
            totalQuestions = allQuestions.size,
            bandScore = band,
            sectionAccents = currentState.sections.map { it.accent.label },
            userAnswers = _answers.value,
            correctAnswers = correctAnswersMap
        )

        _uiState.value = ListeningUiState.EvaluationComplete(report)
    }

    private fun selectAccentByWeights(): Accent {
        val rand = Random.nextFloat()
        var cumulative = 0f
        Accent.values().forEach { accent ->
            cumulative += accent.weight
            if (rand <= cumulative) return accent
        }
        return Accent.STANDARD
    }

    /**
     * Resets the screen to Idle state.
     */
    fun resetToIdle() {
        tts?.stop()
        playbackStateJob?.cancel()
        progressTickerJob?.cancel()
        _answers.value = emptyMap()
        _inputErrors.value = emptyMap()
        _audioPlaybackProgress.value = 0f
        _audioBufferProgress.value = 0f
        _countdownSeconds.value = null
        countdownEndTimeMs = null
        _uiState.value = ListeningUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()

        playbackStateJob?.cancel()
        progressTickerJob?.cancel()
        tts?.stop()
        tts?.shutdown()
    }
}
