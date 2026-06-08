package com.aegis.ielts.features.reading.data

import kotlinx.serialization.Serializable

/**
 * Enumeration of grammatical/syntactic categories for on-device sentence ending validation.
 */
enum class ExpectedType {
    SINGULAR_VERB,  // Expects a singular verb (e.g. "offers", "is", "remains")
    PLURAL_VERB,    // Expects a plural verb (e.g. "provide", "are", "reveal")
    BASE_VERB,      // Expects a base verb form (e.g. "withstand", "generate")
    GERUND          // Expects a gerund -ing phrase (e.g. "harnessing", "drilling")
}

/**
 * Represents a question sentence stem that needs to be matched.
 */
data class SentenceStem(
    val id: String,
    val text: String,
    val expectedType: ExpectedType,
    val correctAnswerId: String
)

/**
 * Represents a sentence ending option, which may be a correct match or a distractor.
 */
data class SentenceEnding(
    val id: String,
    val text: String,
    val type: ExpectedType
)

/**
 * Represents the reading passage content.
 */
data class Passage(
    val title: String,
    val paragraphs: List<String>
)

/**
 * Result of the on-device grammatical/syntactic cohesion validator.
 */
data class CohesionValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)

/**
 * Results of the Reading Assessment grading.
 */
@Serializable
data class ReadingGradingReport(
    val rawScore: Int,
    val totalQuestions: Int,
    val bandScore: Float,
    val userAnswers: Map<String, String?>, // stemId -> selectedEndingId
    val isTimeOut: Boolean
)
