package com.aegis.ielts.core.di

import com.aegis.ielts.core.network.GeminiApiClient
import com.aegis.ielts.core.network.GeminiRepository
import com.google.ai.client.generativeai.GenerativeModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing Gemini-related network dependencies.
 *
 * [AudioCaptureEngine] and [AudioPlaybackEngine] are @Singleton classes with
 * @Inject constructors and are auto-provided by Hilt — no explicit bindings needed.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGenerativeModel(): GenerativeModel = GeminiApiClient.model

    @Provides
    @Singleton
    fun provideGeminiRepository(model: GenerativeModel): GeminiRepository =
        GeminiRepository(model)
}
