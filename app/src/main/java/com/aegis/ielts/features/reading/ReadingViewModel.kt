package com.aegis.ielts.features.reading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.ielts.core.domain.IeltsBandScore
import com.aegis.ielts.features.reading.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReadingViewModel @Inject constructor() : ViewModel() {

    // ─── UI State ─────────────────────────────────────────────────────────────
    private val _uiState = MutableStateFlow<ReadingUiState>(ReadingUiState.Idle)
    val uiState: StateFlow<ReadingUiState> = _uiState.asStateFlow()

    // ─── Countdown Timer ──────────────────────────────────────────────────────
    private val _timeLeftSeconds = MutableStateFlow(3600) // 60 minutes
    val timeLeftSeconds: StateFlow<Int> = _timeLeftSeconds.asStateFlow()

    // ─── Answers Mapping State ────────────────────────────────────────────────
    private val _answers = MutableStateFlow<Map<String, String?>>(emptyMap())
    val answers: StateFlow<Map<String, String?>> = _answers.asStateFlow()

    // ─── Validation Error State ───────────────────────────────────────────────
    private val _validationError = MutableStateFlow<String?>(null)
    val validationError: StateFlow<String?> = _validationError.asStateFlow()

    private var timerJob: Job? = null

    // ─── Mock Data Definition ────────────────────────────────────────────────
    val passage = Passage(
        title = "Harnessing the Earth: The Future of Geothermal Energy",
        paragraphs = listOf(
            "Geothermal energy, the heat emanating from the Earth's molten core, represents one of the most reliable yet underutilized sources of renewable power. Unlike solar and wind energy, which are subject to diurnal cycles and weather variability, geothermal systems offer a continuous baseload of electricity. This constant output makes it a cornerstone for energy grids seeking stability during transition phases away from fossil fuels. However, unlocking this potential requires navigating complex thermodynamic and geological barriers.",
            "The primary challenge in geothermal extraction is the high initial capital investment required. Deep drilling projects, often extending several kilometers into the Earth's crust, demand specialized equipment capable of enduring extreme environments. Before a plant can even begin generating power, developers must secure substantial funding for geological exploration and exploratory drilling, phases that carry high risks of failure if the thermal reservoir proves insufficient.",
            "To mitigate these financial and technical risks, geological surveys conducted in volcanic zones provide crucial details about subterranean heat flow. Volcanic regions, characterized by shallow magma chambers and highly fractured rock, present prime opportunities for high-enthalpy geothermal systems. By mapping seismic activity and thermal gradients, scientists can pinpoint optimal drilling coordinates, significantly reducing the probability of drilling dry wells.",
            "In order to access deeper reservoirs, drill bits must withstand temperatures exceeding three hundred degrees. Standard materials degrade rapidly under such thermal stress, leading to frequent equipment failures and costly downtime. Material science research is therefore critical, focusing on developing diamond-composite cutters and high-durability alloy shafts that maintain structural integrity under intense heat and abrasive friction.",
            "For regions lacking natural hydrothermal aquifers, engineers are focused on harnessing heat from dry hot rock formations. Known as Enhanced Geothermal Systems (EGS), this technique involves injecting high-pressure water to create artificial fractures in deep basement rocks. The water absorbs heat as it circulates through the newly formed network and is then pumped back to the surface to drive steam turbines. While promising, EGS projects require careful monitoring to prevent minor induced seismic events.",
            "In conclusion, geothermal energy, unlike solar or wind power, offers a continuous and stable supply of electricity. As drilling technologies advance and exploratory risks are minimized, this clean resource is poised to play an increasingly vital role in the global energy matrix. Achieving this requires sustained collaboration between geophysicists, material scientists, and policy makers."
        )
    )

    val stems = listOf(
        SentenceStem(
            id = "stem_1",
            text = "The primary challenge in geothermal extraction",
            expectedType = ExpectedType.SINGULAR_VERB,
            correctAnswerId = "ending_3"
        ),
        SentenceStem(
            id = "stem_2",
            text = "Geothermal energy, unlike solar or wind power,",
            expectedType = ExpectedType.SINGULAR_VERB,
            correctAnswerId = "ending_2"
        ),
        SentenceStem(
            id = "stem_3",
            text = "Geological surveys conducted in volcanic zones",
            expectedType = ExpectedType.PLURAL_VERB,
            correctAnswerId = "ending_5"
        ),
        SentenceStem(
            id = "stem_4",
            text = "In order to access deeper reservoirs, drill bits must",
            expectedType = ExpectedType.BASE_VERB,
            correctAnswerId = "ending_1"
        ),
        SentenceStem(
            id = "stem_5",
            text = "Engineers are focused on",
            expectedType = ExpectedType.GERUND,
            correctAnswerId = "ending_7"
        )
    )

    val endings = listOf(
        SentenceEnding(
            id = "ending_1",
            text = "withstand temperatures exceeding three hundred degrees.",
            type = ExpectedType.BASE_VERB
        ),
        SentenceEnding(
            id = "ending_2",
            text = "offers a continuous and stable supply of electricity.",
            type = ExpectedType.SINGULAR_VERB
        ),
        SentenceEnding(
            id = "ending_3",
            text = "is the high initial capital investment required.",
            type = ExpectedType.SINGULAR_VERB
        ),
        SentenceEnding(
            id = "ending_4",
            text = "are causing environmental disruptions in local ecosystems.",
            type = ExpectedType.PLURAL_VERB // Distractor: grammatically matches stem_3, but semantically incorrect
        ),
        SentenceEnding(
            id = "ending_5",
            text = "provide crucial details about subterranean heat flow.",
            type = ExpectedType.PLURAL_VERB
        ),
        SentenceEnding(
            id = "ending_6",
            text = "to reduce carbon emissions globally by fifty percent.",
            type = ExpectedType.GERUND // Grammatically fails on everything since it expects an infinitive mapping which doesn't exist
        ),
        SentenceEnding(
            id = "ending_7",
            text = "harnessing heat from dry hot rock formations.",
            type = ExpectedType.GERUND
        ),
        SentenceEnding(
            id = "ending_8",
            text = "remains highly dependent on weather patterns.",
            type = ExpectedType.SINGULAR_VERB // Distractor: grammatically matches stem_1 or stem_2, but semantically incorrect
        )
    )

    // ─── Actions ──────────────────────────────────────────────────────────────

    /**
     * Starts the 60-minute IELTS Reading mock exam session.
     */
    fun startMockExam() {
        if (_uiState.value is ReadingUiState.MockTestActive) return

        _answers.value = stems.associate { it.id to null }
        _timeLeftSeconds.value = 3600
        _validationError.value = null
        _uiState.value = ReadingUiState.MockTestActive()

        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_timeLeftSeconds.value > 0) {
                delay(1000)
                _timeLeftSeconds.value--
            }
            // Timer expired, auto-submit and freeze
            gradeAssessment(isTimeOut = true)
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    /**
     * Map a sentence stem to a sentence ending.
     * Enforces on-device grammatical/syntactic cohesion validation before updating the flow.
     */
    fun mapStemToEnding(stemId: String, endingId: String?) {
        val currentState = _uiState.value
        if (currentState !is ReadingUiState.MockTestActive || currentState.isFrozen) return

        if (endingId == null) {
            // Unmapping is always allowed
            val updated = _answers.value.toMutableMap()
            updated[stemId] = null
            _answers.value = updated
            _validationError.value = null
            return
        }

        val stem = stems.find { it.id == stemId } ?: return
        val ending = endings.find { it.id == endingId } ?: return

        // Evaluate grammatical cohesion on-device
        val validation = validateGrammaticalCohesion(stem, ending)

        if (validation.isValid) {
            val updated = _answers.value.toMutableMap()
            updated[stemId] = endingId
            _answers.value = updated
            _validationError.value = null
        } else {
            // Reject update and emit validation error message to UI
            _validationError.value = validation.errorMessage
        }
    }

    /**
     * Explicit on-device grammatical cohesion state validator checking
     * subject-verb agreement and syntactic compatibility.
     */
    private fun validateGrammaticalCohesion(
        stem: SentenceStem,
        ending: SentenceEnding
    ): CohesionValidationResult {
        return if (stem.expectedType == ending.type) {
            CohesionValidationResult(isValid = true)
        } else {
            val errorMsg = when (stem.expectedType) {
                ExpectedType.SINGULAR_VERB -> "Grammatical Mismatch: '${stem.text}' requires a singular verb clause (e.g. 'is', 'offers')."
                ExpectedType.PLURAL_VERB -> "Grammatical Mismatch: '${stem.text}' has a plural subject, requiring a plural verb clause (e.g. 'provide')."
                ExpectedType.BASE_VERB -> "Syntactic Mismatch: '${stem.text}' ends in an auxiliary verb expecting a bare infinitive (e.g. 'withstand')."
                ExpectedType.GERUND -> "Syntactic Mismatch: '${stem.text}' ends in a preposition expecting a gerund phrase (e.g. '-ing' clause)."
            }
            CohesionValidationResult(isValid = false, errorMessage = errorMsg)
        }
    }

    /**
     * Submits the assessment and computes the IELTS Reading band score.
     */
    fun submitAssessment() {
        val currentState = _uiState.value
        if (currentState !is ReadingUiState.MockTestActive || currentState.isFrozen) return
        gradeAssessment(isTimeOut = false)
    }

    private fun gradeAssessment(isTimeOut: Boolean) {
        stopTimer()

        // Freeze interactive nodes
        _uiState.value = ReadingUiState.MockTestActive(isFrozen = true)

        val userAns = _answers.value
        var correctCount = 0

        stems.forEach { stem ->
            if (userAns[stem.id] == stem.correctAnswerId) {
                correctCount++
            }
        }

        // Convert correct answers to IELTS Band Score using domain model
        // Scaling 5 questions to IELTS band score: each correct answer yields 1.8 raw score points
        val rawScale = (correctCount * 1.8f).coerceIn(0f, 9f)
        val band = IeltsBandScore(rawScale).band

        val report = ReadingGradingReport(
            rawScore = correctCount,
            totalQuestions = stems.size,
            bandScore = band,
            userAnswers = userAns,
            isTimeOut = isTimeOut
        )

        _uiState.value = ReadingUiState.EvaluationComplete(report)
    }

    /**
     * Resets the assessment back to the Idle state instructions.
     */
    fun resetToIdle() {
        stopTimer()
        _timeLeftSeconds.value = 3600
        _answers.value = emptyMap()
        _validationError.value = null
        _uiState.value = ReadingUiState.Idle
    }

    /**
     * Clears any active validation error message.
     */
    fun clearValidationError() {
        _validationError.value = null
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
    }
}
