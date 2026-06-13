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
    private val audioPlaybackEngine: AudioPlaybackEngine,
    private val geminiRepository: GeminiRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // initialized
            }
        }
    }

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
        // Pool A questions (unified mock track)
        val unifiedSection = ListeningSection(
            sectionNumber = 1,
            environment = ListeningEnvironment.SOCIAL_DIALOGUE,
            accent = Accent.STANDARD,
            questions = listOf(
                ListeningQuestion.FormCompletion(
                    id = "q1_form_name",
                    instruction = "Write ONE WORD ONLY for each answer.",
                    questionText = "Family Name",
                    correctAnswer = "HEMINGWAY"
                ),
                ListeningQuestion.FormCompletion(
                    id = "q1_form_phone",
                    instruction = "Write NUMBERS ONLY for each answer.",
                    questionText = "Contact Number",
                    correctAnswer = "07700900077",
                    charLimit = 11
                ),
                ListeningQuestion.MultipleChoice(
                    id = "q2_mcq_1",
                    instruction = "Choose the correct letter, A, B or C.",
                    questionText = "What is the primary cause of traffic delays in the nature reserve?",
                    options = listOf("A. Wildlife crossings", "B. Bridge construction", "C. Seasonal flooding"),
                    correctAnswer = "B"
                ),
                ListeningQuestion.MultipleChoice(
                    id = "q2_mcq_2",
                    instruction = "Choose the correct letter, A, B or C.",
                    questionText = "When is the reserve café open to the public?",
                    options = listOf("A. On weekends only", "B. Throughout the year", "C. During summer months"),
                    correctAnswer = "A"
                ),
                ListeningQuestion.MapLabeling(
                    id = "q3_map_1",
                    instruction = "Write the correct letter, A-E, next to the location description.",
                    questionText = "Student Help Center Office",
                    correctAnswer = "B"
                ),
                ListeningQuestion.MapLabeling(
                    id = "q3_map_2",
                    instruction = "Write the correct letter, A-E, next to the location description.",
                    questionText = "Main Lecture Hall Complex",
                    correctAnswer = "C"
                ),
                ListeningQuestion.Matching(
                    id = "q4_match_1",
                    instruction = "Classify the architectural designs under correct historical periods.",
                    questionText = "Gothic Arches System",
                    categories = listOf("MEDIEVAL", "RENAISSANCE", "MODERN"),
                    correctAnswer = "MEDIEVAL"
                ),
                ListeningQuestion.Matching(
                    id = "q4_match_2",
                    instruction = "Classify the architectural designs under correct historical periods.",
                    questionText = "Steel Beam Foundations",
                    categories = listOf("MEDIEVAL", "RENAISSANCE", "MODERN"),
                    correctAnswer = "MODERN"
                ),
                // Added two more questions to make it 10 total
                ListeningQuestion.MultipleChoice(
                    id = "q5_mcq_1",
                    instruction = "Choose the correct letter, A, B or C.",
                    questionText = "What is the new library opening hour on Sundays?",
                    options = listOf("A. 9:00 AM", "B. 10:00 AM", "C. 12:00 PM"),
                    correctAnswer = "B"
                ),
                ListeningQuestion.FormCompletion(
                    id = "q5_form_1",
                    instruction = "Write ONE WORD ONLY for each answer.",
                    questionText = "Book Return Box Location",
                    correctAnswer = "ENTRANCE"
                )
            ),
            audioAssetPath = "audio/section_1.mp3"
        )

        val activeSections = listOf(unifiedSection)

        // Clear state
        _answers.value = activeSections.flatMap { it.questions }.associate { it.id to "" }
        _inputErrors.value = emptyMap()
        _audioPlaybackProgress.value = 0f
        _audioBufferProgress.value = 0f
        _countdownSeconds.value = null
        countdownEndTimeMs = null

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

        viewModelScope.launch {
            try {
                audioPlaybackEngine.playFromAsset(section.audioAssetPath)
            } catch (e: Exception) {
                _uiState.value = ListeningUiState.Error("Failed to load local audio asset.")
            }
        }

        // Track ExoPlayer playback status
        playbackStateJob = viewModelScope.launch {
            audioPlaybackEngine.playbackState.collect { state ->
                when (state) {
                    is PlaybackState.Playing -> {
                        updateActiveAudioPlayingState(true)
                        startProgressIndicatorTicker()
                    }
                    is PlaybackState.Completed -> {
                        updateActiveAudioPlayingState(false)
                        // Do not cancel the ticker here; the countdown must continue!
                    }
                    is PlaybackState.Error -> {
                        updateActiveAudioPlayingState(false)
                        progressTickerJob?.cancel()
                        _uiState.value = ListeningUiState.Error(state.message)
                    }
                    is PlaybackState.Loading, PlaybackState.Idle -> {
                        progressTickerJob?.cancel()
                    }
                }
            }
        }
    }

    private fun startProgressIndicatorTicker() {
        progressTickerJob?.cancel()
        progressTickerJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            while (true) {
                val currentState = _uiState.value as? ListeningUiState.Active ?: break
                if (currentState.isFrozen) break

                val duration = audioPlaybackEngine.getDuration()
                val position = audioPlaybackEngine.getCurrentPosition()
                val buffered = audioPlaybackEngine.getBufferedPosition()

                if (duration > 0) {
                    _audioPlaybackProgress.value = (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                    _audioBufferProgress.value = (buffered.toFloat() / duration.toFloat()).coerceIn(0f, 1f)

                    // Initialize countdown dynamically upon reading file duration
                    if (countdownEndTimeMs == null) {
                        countdownEndTimeMs = System.currentTimeMillis() + duration + 120_000L // Duration + 120 seconds
                    }
                }

                if (countdownEndTimeMs != null) {
                    val remainingMs = countdownEndTimeMs!! - System.currentTimeMillis()
                    if (remainingMs > 0) {
                        _countdownSeconds.value = remainingMs / 1000
                    } else {
                        _countdownSeconds.value = 0
                        submitListeningTest() // Freeze everything exactly at 0.00s
                        break
                    }
                }

                delay(200)
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

        viewModelScope.launch {
            audioPlaybackEngine.stop()
        }

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

        val report = ListeningGradingReport(
            rawScore = correctCount,
            totalQuestions = allQuestions.size,
            bandScore = band,
            sectionAccents = currentState.sections.map { it.accent.label },
            userAnswers = _answers.value
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
        viewModelScope.launch {
            audioPlaybackEngine.stop()
        }
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
        audioPlaybackEngine.release()
        playbackStateJob?.cancel()
        progressTickerJob?.cancel()
        tts?.stop()
        tts?.shutdown()
    }
}
