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
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class ListeningViewModel @Inject constructor(
    private val audioPlaybackEngine: AudioPlaybackEngine
) : ViewModel() {

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

    private var playbackStateJob: Job? = null
    private var progressTickerJob: Job? = null

    // ─── Actions ──────────────────────────────────────────────────────────────

    /**
     * Samples accents according to probabilities and shuffles sections (Unpredictable Task Router).
     */
    fun startListeningAssessment() {
        val selectedAccents = List(4) { selectAccentByWeights() }

        // Pool A questions
        val poolA = listOf(
            ListeningSection(
                sectionNumber = 1,
                environment = ListeningEnvironment.SOCIAL_DIALOGUE,
                accent = selectedAccents[0],
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
                    )
                ),
                audioAssetPath = "audio/listening_sec1.mp3"
            ),
            ListeningSection(
                sectionNumber = 2,
                environment = ListeningEnvironment.SOCIAL_MONOLOGUE,
                accent = selectedAccents[1],
                questions = listOf(
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
                    )
                ),
                audioAssetPath = "audio/listening_sec2.mp3"
            ),
            ListeningSection(
                sectionNumber = 3,
                environment = ListeningEnvironment.ACADEMIC_DISCUSSION,
                accent = selectedAccents[2],
                questions = listOf(
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
                    )
                ),
                audioAssetPath = "audio/listening_sec3.mp3"
            ),
            ListeningSection(
                sectionNumber = 4,
                environment = ListeningEnvironment.ACADEMIC_LECTURE,
                accent = selectedAccents[3],
                questions = listOf(
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
                    )
                ),
                audioAssetPath = "audio/listening_sec4.mp3"
            )
        )

        // Pool B questions (reversed or completely shuffled layouts for unpredictable routing)
        val poolB = listOf(
            ListeningSection(
                sectionNumber = 1,
                environment = ListeningEnvironment.SOCIAL_DIALOGUE,
                accent = selectedAccents[0],
                questions = listOf(
                    ListeningQuestion.MultipleChoice(
                        id = "q1_mcq_1",
                        instruction = "Choose the correct letter, A, B or C.",
                        questionText = "Where will the conference delegates meet on the first morning?",
                        options = listOf("A. In the Main Lobby", "B. In the Conference Room C", "C. In the Student Lounge"),
                        correctAnswer = "A"
                    ),
                    ListeningQuestion.MultipleChoice(
                        id = "q1_mcq_2",
                        instruction = "Choose the correct letter, A, B or C.",
                        questionText = "How much is the registration fee for student delegates?",
                        options = listOf("A. £45.00", "B. £60.00", "C. £75.00"),
                        correctAnswer = "B"
                    )
                ),
                audioAssetPath = "audio/listening_sec1_b.mp3"
            ),
            ListeningSection(
                sectionNumber = 2,
                environment = ListeningEnvironment.SOCIAL_MONOLOGUE,
                accent = selectedAccents[1],
                questions = listOf(
                    ListeningQuestion.MapLabeling(
                        id = "q2_map_1",
                        instruction = "Write the correct letter, A-E, next to the reserve landmark.",
                        questionText = "Bird Watching Tower",
                        correctAnswer = "C"
                    ),
                    ListeningQuestion.MapLabeling(
                        id = "q2_map_2",
                        instruction = "Write the correct letter, A-E, next to the reserve landmark.",
                        questionText = "Picnic Area Zone",
                        correctAnswer = "E"
                    )
                ),
                audioAssetPath = "audio/listening_sec2_b.mp3"
            ),
            ListeningSection(
                sectionNumber = 3,
                environment = ListeningEnvironment.ACADEMIC_DISCUSSION,
                accent = selectedAccents[2],
                questions = listOf(
                    ListeningQuestion.Matching(
                        id = "q3_match_1",
                        instruction = "Match the seminar courses with their eligibility criteria.",
                        questionText = "Advanced Machine Learning",
                        categories = listOf("GRADUATES_ONLY", "OPEN_TO_ALL", "PREREQUISITES_REQUIRED"),
                        correctAnswer = "PREREQUISITES_REQUIRED"
                    ),
                    ListeningQuestion.Matching(
                        id = "q3_match_2",
                        instruction = "Match the seminar courses with their eligibility criteria.",
                        questionText = "Introduction to Statistics",
                        categories = listOf("GRADUATES_ONLY", "OPEN_TO_ALL", "PREREQUISITES_REQUIRED"),
                        correctAnswer = "OPEN_TO_ALL"
                    )
                ),
                audioAssetPath = "audio/listening_sec3_b.mp3"
            ),
            ListeningSection(
                sectionNumber = 4,
                environment = ListeningEnvironment.ACADEMIC_LECTURE,
                accent = selectedAccents[3],
                questions = listOf(
                    ListeningQuestion.FormCompletion(
                        id = "q4_form_1",
                        instruction = "Write ONE WORD ONLY for each answer.",
                        questionText = "Material used for shield construction",
                        correctAnswer = "TITANIUM"
                    ),
                    ListeningQuestion.FormCompletion(
                        id = "q4_form_2",
                        instruction = "Write ONE WORD AND/OR A NUMBER for each answer.",
                        questionText = "Maximum operational temperature (Celsius)",
                        correctAnswer = "1500"
                    )
                ),
                audioAssetPath = "audio/listening_sec4_b.mp3"
            )
        )

        // Randomly route either Pool A or Pool B to prevent memorization
        val activeSections = if (Random.nextBoolean()) poolA else poolB

        // Clear state
        _answers.value = activeSections.flatMap { it.questions }.associate { it.id to "" }
        _inputErrors.value = emptyMap()
        _audioPlaybackProgress.value = 0f

        _uiState.value = ListeningUiState.Active(
            sections = activeSections,
            currentSectionIndex = 0,
            isAudioPlaying = false
        )

        // Automatically launch audio for section 0
        playCurrentSectionAudio(activeSections[0])
    }

    private fun playCurrentSectionAudio(section: ListeningSection) {
        playbackStateJob?.cancel()
        progressTickerJob?.cancel()
        _audioPlaybackProgress.value = 0f

        viewModelScope.launch {
            try {
                audioPlaybackEngine.playFromAsset(section.audioAssetPath)
            } catch (e: Exception) {
                // If asset is missing in emulator/build environment, gracefully simulate progress to keep app running
                simulateAudioPlaybackProgress()
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
                    is PlaybackState.Completed, is PlaybackState.Error -> {
                        updateActiveAudioPlayingState(false)
                        progressTickerJob?.cancel()
                        _audioPlaybackProgress.value = 1f
                    }
                    else -> {}
                }
            }
        }
    }

    private fun startProgressIndicatorTicker() {
        progressTickerJob?.cancel()
        progressTickerJob = viewModelScope.launch {
            // Simulate progression ticks from 0f to 1f over a 30-second duration for demo testing
            // In production, this binds directly to ExoPlayer currentPosition / duration
            var elapsed = 0f
            while (elapsed < 30f) {
                kotlinx.coroutines.delay(200)
                elapsed += 0.2f
                _audioPlaybackProgress.value = (elapsed / 30f).coerceIn(0f, 1f)
            }
            _audioPlaybackProgress.value = 1f
            updateActiveAudioPlayingState(false)
        }
    }

    private suspend fun simulateAudioPlaybackProgress() {
        updateActiveAudioPlayingState(true)
        startProgressIndicatorTicker()
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
        val currentState = _uiState.value as? ListeningUiState.Active ?: return
        if (currentState.currentSectionIndex < 3) {
            val nextIndex = currentState.currentSectionIndex + 1
            _uiState.value = currentState.copy(
                currentSectionIndex = nextIndex,
                isAudioPlaying = false
            )
            // Stop previous ExoPlayer audio and start the new one
            viewModelScope.launch {
                audioPlaybackEngine.stop()
            }
            playCurrentSectionAudio(currentState.sections[nextIndex])
        } else {
            // Already at last section, trigger submit
            submitListeningTest()
        }
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
        _uiState.value = ListeningUiState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        audioPlaybackEngine.release()
        playbackStateJob?.cancel()
        progressTickerJob?.cancel()
    }
}
