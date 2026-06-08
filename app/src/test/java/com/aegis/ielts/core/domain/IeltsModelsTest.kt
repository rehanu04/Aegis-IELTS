package com.aegis.ielts.core.domain

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IeltsModelsTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun testIeltsBandScoreRounding() {
        // Test official midpoint rounding formula: B_overall(x) = floor(2x + 0.5) / 2
        assertEquals(7.0f, IeltsBandScore(6.75f).band)
        assertEquals(6.5f, IeltsBandScore(6.74f).band)
        assertEquals(6.5f, IeltsBandScore(6.25f).band)
        assertEquals(6.0f, IeltsBandScore(6.24f).band)
        assertEquals(6.0f, IeltsBandScore(5.875f).band)
        assertEquals(0.0f, IeltsBandScore(0.0f).band)
        assertEquals(9.0f, IeltsBandScore(9.0f).band)
    }

    @Test
    fun testSpeakingAssessmentResponseDeserialization() {
        val jsonText = """
        {
          "fluencyCoherence": {
            "score": 7.5,
            "feedback": "Good fluency",
            "hesitationProfile": {
              "withinClausePauses": 2,
              "betweenClausePauses": 4,
              "totalSilenceMs": 1500
            },
            "fillerDensityIndex": 1.2
          },
          "lexicalResource": {
            "score": 8.0,
            "feedback": "Rich vocabulary",
            "lexicalAsymmetryIndex": 0.35
          },
          "grammaticalRangeAccuracy": {
            "score": 7.0,
            "feedback": "Minor errors"
          },
          "pronunciation": {
            "score": 7.5,
            "feedback": "Clear pronunciation"
          },
          "overallFeedback": "Excellent speaking test."
        }
        """.trimIndent()

        val response = json.decodeFromString<SpeakingAssessmentResponse>(jsonText)
        assertEquals(7.5f, response.fluencyScore)
        assertEquals(8.0f, response.lexicalScore)
        assertEquals(7.0f, response.grammarScore)
        assertEquals(7.5f, response.pronunciationScore)
        assertEquals("Excellent speaking test.", response.feedback)
        assertEquals("Good fluency", response.detailedFeedback.fluencyFeedback)
        assertEquals("Minor errors", response.detailedFeedback.grammarFeedback)
        assertEquals(1.2f, response.advancedMetrics.fillerDensityIndex)
        assertEquals(2, response.advancedMetrics.withinClausePauses)
        assertEquals(0, response.silenceTelemetry.silenceCount)
        
        val copiedResponse = response.copy(silenceTelemetry = SilenceTelemetry(silenceCount = 6))
        assertEquals(6, copiedResponse.silenceTelemetry.silenceCount)
    }

    @Test
    fun testWritingAssessmentResponseDeserialization() {
        val jsonText = """
        {
          "criteriaScores": {
            "taskAchievement": 6.5,
            "coherenceCohesion": 7.0,
            "lexicalResource": 7.5,
            "grammaticalRangeAccuracy": 6.0
          },
          "templateDetection": {
            "templateDetected": true,
            "templateSimilarityScore": 0.45,
            "lexicalAsymmetryIndex": 0.18
          },
          "grammarCorrections": [
            {
              "original": "is be",
              "corrected": "is",
              "explanation": "Verb correction",
              "errorType": "Grammar"
            }
          ],
          "overallFeedback": "Overall essay critique."
        }
        """.trimIndent()

        val response = json.decodeFromString<WritingAssessmentResponse>(jsonText)
        assertEquals(6.5f, response.taskAchievementScore)
        assertEquals(7.0f, response.coherenceScore)
        assertEquals(7.5f, response.lexicalScore)
        assertEquals(6.0f, response.grammarScore)
        assertTrue(response.templateDetected)
        assertEquals(0.45f, response.templateSimilarityScore)
        assertEquals("Overall essay critique.", response.feedback)
        assertEquals(1, response.grammarCorrections.size)
        assertEquals("is be", response.grammarCorrections[0].original)
        assertEquals("is", response.grammarCorrections[0].corrected)
    }

    @Test
    fun testWritingAssessmentResponseCompatibilityConstructor() {
        val response = WritingAssessmentResponse(
            taskAchievementScore = 6.5f,
            coherenceScore = 7.0f,
            lexicalScore = 7.5f,
            grammarScore = 6.0f,
            feedback = "Overall essay critique.",
            templateDetected = true,
            templateSimilarityScore = 0.45f
        )
        assertEquals(6.5f, response.taskAchievementScore)
        assertEquals(7.0f, response.coherenceScore)
        assertEquals(7.5f, response.lexicalScore)
        assertEquals(6.0f, response.grammarScore)
        assertTrue(response.templateDetected)
        assertEquals(0.45f, response.templateSimilarityScore)
        assertEquals("Overall essay critique.", response.feedback)
        assertTrue(response.grammarCorrections.isEmpty())
    }
}
