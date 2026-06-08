package com.aegis.ielts.core.network

import com.aegis.ielts.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.generationConfig

/**
 * Singleton factory providing the Gemini 2.0 Flash [GenerativeModel] instance.
 *
 * Configuration:
 *  - Model: gemini-2.0-flash (fast, multimodal)
 *  - temperature: 0.3f (deterministic, low-creativity scoring)
 *  - responseMimeType: "application/json" (structured output contract)
 *  - Safety: MEDIUM_AND_ABOVE block threshold across all harm categories
 *
 * API key is injected at compile time from local.properties via BuildConfig.GEMINI_API_KEY.
 */
object GeminiApiClient {

    const val BYPASS_DIRECT_GEMINI = true
    const val BACKEND_URL = "http://10.0.2.2:8000"

    private const val MODEL_NAME = "gemini-2.0-flash"

    val model: GenerativeModel by lazy {
        GenerativeModel(
            modelName = MODEL_NAME,
            apiKey = BuildConfig.GEMINI_API_KEY,
            generationConfig = generationConfig {
                temperature    = 0.3f
                topK           = 40
                topP           = 0.95f
                maxOutputTokens = 2048
                responseMimeType = "application/json"
            },
            safetySettings = listOf(
                SafetySetting(HarmCategory.HARASSMENT,       BlockThreshold.MEDIUM_AND_ABOVE),
                SafetySetting(HarmCategory.HATE_SPEECH,      BlockThreshold.MEDIUM_AND_ABOVE),
                SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.MEDIUM_AND_ABOVE),
                SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.MEDIUM_AND_ABOVE),
            )
        )
    }
}
