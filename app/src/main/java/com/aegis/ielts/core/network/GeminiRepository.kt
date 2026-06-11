package com.aegis.ielts.core.network

import com.aegis.ielts.core.domain.SpeakingAssessmentResponse
import com.aegis.ielts.core.domain.WritingAssessmentResponse
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository abstracting all Gemini API evaluation requests.
 *
 * Supports:
 *  - [evaluateSpeaking]: Evaluates IELTS Speaking from transcript + examiner prompts
 *  - [evaluateWriting]: Evaluates IELTS Writing with template detection
 *
 * Retry strategy: exponential backoff with max 3 attempts (delays: 1s, 2s, skipped).
 * All responses are deserialized from structured JSON using kotlinx.serialization.
 */
@Singleton
class GeminiRepository @Inject constructor(
    private val model: GenerativeModel
) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient         = true
        coerceInputValues = true
    }

    // ─── Speaking Evaluation ──────────────────────────────────────────────────

    /**
     * Evaluates a speaking session using the transcript and the examiner prompts.
     *
     * Phase 2 will additionally pass [audioBytes] as inline multimodal data.
     * For Phase 1, evaluation uses the text transcript exclusively for band scoring.
     *
     * @param audioBytes  WAV-encoded PCM audio from [AudioCaptureEngine] (Phase 2 active)
     * @param transcript  STT transcript of the candidate's responses
     * @param prompts     Ordered list of examiner questions/cues presented to the candidate
     */
    suspend fun evaluateSpeaking(
        audioBytes: ByteArray,
        transcript: String,
        prompts: List<String>
    ): Result<SpeakingAssessmentResponse> = runWithRetry {
        if (GeminiApiClient.BYPASS_DIRECT_GEMINI) {
            val audioBase64 = android.util.Base64.encodeToString(audioBytes, android.util.Base64.NO_WRAP)
            val request = SpeakingGradeRequest(
                audio_base64 = audioBase64,
                transcript = transcript,
                prompts = prompts
            )
            postRequest<SpeakingGradeRequest, SpeakingAssessmentResponse>("/api/v1/grade/speaking", request)
        } else {
            val response = model.generateContent(buildSpeakingPrompt(transcript, prompts))
            val jsonText = response.text
                ?: throw IOException("Gemini returned an empty response for speaking evaluation")
            json.decodeFromString<SpeakingAssessmentResponse>(jsonText)
        }
    }

    // ─── Writing Evaluation ───────────────────────────────────────────────────

    /**
     * Evaluates an IELTS Writing submission including template plagiarism detection.
     *
     * @param taskType  1 = Academic Task 1 (report/chart), 2 = Task 2 (essay)
     * @param prompt    The original task description shown to the candidate
     * @param essay     The candidate's full written response
     */
    suspend fun evaluateWriting(
        taskType: Int,
        prompt: String,
        essay: String
    ): Result<WritingAssessmentResponse> = runWithRetry {
        if (GeminiApiClient.BYPASS_DIRECT_GEMINI) {
            val request = WritingGradeRequest(
                task_type = taskType,
                prompt = prompt,
                essay = essay
            )
            postRequest<WritingGradeRequest, WritingAssessmentResponse>("/api/v1/grade/writing", request)
        } else {
            val response = model.generateContent(buildWritingPrompt(taskType, prompt, essay))
            val jsonText = response.text
                ?: throw IOException("Gemini returned an empty response for writing evaluation")
            json.decodeFromString<WritingAssessmentResponse>(jsonText)
        }
    }

    // ─── Network HTTP Connection Helper ───────────────────────────────────────

    private suspend inline fun <reified Req, reified Res> postRequest(
        path: String,
        requestBody: Req
    ): Res = withContext(Dispatchers.IO) {
        val url = URL("${GeminiApiClient.BACKEND_URL}$path")
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("Accept", "application/json")
            conn.doOutput = true
            conn.doInput = true
            conn.connectTimeout = 15000
            conn.readTimeout = 60000

            val jsonRequest = json.encodeToString(requestBody)
            conn.outputStream.use { os ->
                val input = jsonRequest.toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
            }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val jsonResponse = conn.inputStream.bufferedReader().use { it.readText() }
                try {
                    json.decodeFromString<Res>(jsonResponse)
                } catch (e: Exception) {
                    throw IOException("JSON Parsing Error: ${e.message}\nRaw Response: $jsonResponse", e)
                }
            } else {
                val errorMsg = conn.errorStream?.bufferedReader()?.use { it.readText() } 
                    ?: "Response code: $responseCode"
                throw IOException("Backend error ($responseCode): $errorMsg")
            }
        } finally {
            conn.disconnect()
        }
    }

    /**
     * Wakes up the Render backend to overcome cold-start dormancy (15 mins inactivity).
     * Fires a lightweight GET request to the /docs endpoint with a high timeout.
     */
    suspend fun pingBackend(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${GeminiApiClient.BACKEND_URL}/api/v1/health")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 60000 // High timeout for cold start
            conn.readTimeout = 60000
            conn.connect()
            
            if (conn.responseCode in 200..499) {
                Result.success(Unit)
            } else {
                Result.failure(IOException("Backend failed to wake up: ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ─── Retry Infrastructure ─────────────────────────────────────────────────

    /**
     * Executes [block] with exponential backoff retry.
     * Delays between attempts: 1 000ms, 2 000ms (doubles each time).
     * Returns [Result.failure] after exhausting all [maxAttempts].
     */
    private suspend fun <T> runWithRetry(
        maxAttempts: Int  = 3,
        initialDelayMs: Long = 1_000L,
        block: suspend () -> T
    ): Result<T> {
        var lastException: Exception? = null
        repeat(maxAttempts) { attempt ->
            try {
                return Result.success(block())
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxAttempts - 1) {
                    delay(initialDelayMs * (1L shl attempt)) // 1s, 2s
                }
            }
        }
        return Result.failure(
            lastException ?: IOException("Unknown error after $maxAttempts attempts")
        )
    }

    // ─── Prompt Builders ──────────────────────────────────────────────────────

    private fun buildSpeakingPrompt(transcript: String, prompts: List<String>): String = """
        You are a certified IELTS Speaking examiner. Evaluate the candidate's performance
        across all 4 official IELTS Speaking assessment criteria, adhering to the strict 2026 structural metrics.

        Examiner questions used in this session:
        ${prompts.mapIndexed { i, q -> "${i + 1}. $q" }.joinToString("\n")}

        Candidate transcript:
        "$transcript"

        Score each criterion on the official 0.0–9.0 scale with 0.5 precision.
        Consider: fluency, coherence, vocabulary range, grammatical accuracy, and pronunciation clarity.
        
        Calculate the 2026 official structural metrics:
        - Filler Density Index (FDI): The frequency of filler words per 100 words.
        - Lexical Asymmetry Index (LAI): Imbalance in vocabulary complexity across the response.
        - Structural within-clause searching pauses: Count of unnatural pauses that occur within clauses (not between clauses).
        - Pre-memorized speech markers: Detect if the speech patterns strongly indicate pre-memorized content.

        Respond ONLY with a valid JSON object conforming exactly to this schema. Any deviation will result in a parsing error:
        {
          "fluencyScore": <number 0.0-9.0>,
          "lexicalScore": <number 0.0-9.0>,
          "grammarScore": <number 0.0-9.0>,
          "pronunciationScore": <number 0.0-9.0>,
          "feedback": "<concise overall examiner summary, 2-3 sentences>",
          "detailedFeedback": {
            "fluencyFeedback": "<specific fluency and coherence observations>",
            "lexicalFeedback": "<specific vocabulary range and accuracy observations>",
            "grammarFeedback": "<specific grammatical range and accuracy observations>",
            "pronunciationFeedback": "<specific phonological feature observations>"
          },
          "silenceTelemetry": {
            "silenceCount": 0,
            "maxSilenceDurationMs": 0,
            "totalSilenceMs": 0
          },
          "advancedMetrics": {
            "fillerDensityIndex": <number>,
            "lexicalAsymmetryIndex": <number>,
            "withinClausePauses": <number integer>,
            "preMemorizedSpeechDetected": <boolean>
          }
        }
    """.trimIndent()

    private fun buildWritingPrompt(taskType: Int, prompt: String, essay: String): String {
        val taskName     = if (taskType == 1) "Academic Writing Task 1" else "Academic Writing Task 2"
        val minWords     = if (taskType == 1) 150 else 250
        return """
            You are a certified IELTS Writing examiner. Evaluate the following $taskName response.

            Task prompt: "$prompt"
            Minimum required word count: $minWords

            Candidate essay:
            "$essay"

            Assess template usage: if the essay follows a formulaic memorized structure
            that suggests it was not written organically for this specific task,
            set templateDetected=true and estimate templateSimilarityScore
            (0.0 = completely original, 1.0 = pure memorized template with no adaptation).

            Respond ONLY with a valid JSON object:
            {
              "taskAchievementScore": <number 0.0-9.0>,
              "coherenceScore": <number 0.0-9.0>,
              "lexicalScore": <number 0.0-9.0>,
              "grammarScore": <number 0.0-9.0>,
              "feedback": "<overall examiner summary with key strengths and areas for improvement>",
              "templateDetected": <boolean>,
              "templateSimilarityScore": <number 0.0-1.0>
            }
        """.trimIndent()
    }
}

@Serializable
data class SpeakingGradeRequest(
    val audio_base64: String,
    val transcript: String,
    val prompts: List<String>
)

@Serializable
data class WritingGradeRequest(
    val task_type: Int,
    val prompt: String,
    val essay: String
)

