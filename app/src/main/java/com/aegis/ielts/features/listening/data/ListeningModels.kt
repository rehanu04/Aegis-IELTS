package com.aegis.ielts.features.listening.data

import kotlinx.serialization.Serializable

/**
 * The 5 designated speaker accent profiles for IELTS Listening, with their official probabilities.
 */
enum class Accent(val label: String, val weight: Float) {
    SOUTH_ASIAN("South Asian", 0.25f),
    AFRICAN("African", 0.15f),
    EUROPEAN("European", 0.20f),
    AUSTRALIAN("Australian", 0.20f),
    STANDARD("Standard (UK/US)", 0.20f)
}

/**
 * The 4 official IELTS Listening section environments.
 */
enum class ListeningEnvironment(val label: String, val description: String) {
    SOCIAL_DIALOGUE("Social Dialogue", "Section 1: Conversation between two speakers on a general social context."),
    SOCIAL_MONOLOGUE("Social Monologue", "Section 2: Monologue on a general social context (e.g. library tour)."),
    ACADEMIC_DISCUSSION("Academic Discussion", "Section 3: Discussion among up to four people in an academic/training context."),
    ACADEMIC_LECTURE("Academic Lecture", "Section 4: Monologue/lecture on an academic subject.")
}

/**
 * Sealed class representing computer-delivered IELTS Listening question types.
 */
sealed class ListeningQuestion {
    abstract val id: String
    abstract val instruction: String
    abstract val questionText: String
    abstract val correctAnswer: String

    /**
     * Form, table, or flow-chart completion question.
     * Captures text input with strict word and character limit validations.
     */
    data class FormCompletion(
        override val id: String,
        override val instruction: String,
        override val questionText: String, // E.g., "1. Contact Phone: ______"
        override val correctAnswer: String, // E.g., "07700900077" or "SMITH"
        val wordLimit: Int = 1,
        val charLimit: Int = 15
    ) : ListeningQuestion()

    /**
     * Multiple choice question. Renders radio selections.
     */
    data class MultipleChoice(
        override val id: String,
        override val instruction: String,
        override val questionText: String, // E.g., "What is the main reason for the project delay?"
        override val correctAnswer: String, // E.g., "B"
        val options: List<String> // E.g., ["A. Shortage of funds", "B. Lack of laboratory equipment", "C. Disagreement on site location"]
    ) : ListeningQuestion()

    /**
     * Map or plan labeling question.
     * Renders a layout where lettered coordinates are mapped to locations.
     */
    data class MapLabeling(
        override val id: String,
        override val instruction: String,
        override val questionText: String, // E.g., "2. Tourist Information Office"
        override val correctAnswer: String, // E.g., "C"
        val mapLocations: List<String> = listOf("A", "B", "C", "D", "E") // Coordinates available
    ) : ListeningQuestion()

    /**
     * Matching or classification grid matrix question.
     * Links audio items to specific categories using paraphrased associations.
     */
    data class Matching(
        override val id: String,
        override val instruction: String,
        override val questionText: String, // E.g., "3. Biology Research Proposal"
        override val correctAnswer: String, // E.g., "COMPULSORY"
        val categories: List<String> // E.g., ["COMPULSORY", "ELECTIVE", "NOT OFFERED"]
    ) : ListeningQuestion()
}

/**
 * Represents a single Listening section.
 */
data class ListeningSection(
    val sectionNumber: Int,
    val environment: ListeningEnvironment,
    val accent: Accent,
    val questions: List<ListeningQuestion>,
    val audioAssetPath: String
)

/**
 * Diagnostic report of the Listening assessment session.
 */
@Serializable
data class ListeningGradingReport(
    val rawScore: Int,
    val totalQuestions: Int,
    val bandScore: Float,
    val sectionAccents: List<String>, // Accents chosen for the 4 sections
    val userAnswers: Map<String, String>, // questionId -> selected/typed answer
    val correctAnswers: Map<String, String> // questionId -> correct answer
)
