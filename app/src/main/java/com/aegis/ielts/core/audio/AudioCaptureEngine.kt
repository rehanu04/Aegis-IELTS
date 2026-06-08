package com.aegis.ielts.core.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.aegis.ielts.core.domain.SilenceTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Single audio frame emitted by [AudioCaptureEngine] during active recording.
 *
 * @param pcmData       Raw 16-bit PCM samples at 16 000 Hz mono
 * @param amplitudeDb   Normalized amplitude in [0.0, 1.0] mapped from [-96 dBFS, 0 dBFS]
 */
data class AudioFrame(
    val pcmData     : ShortArray,
    val amplitudeDb : Float
) {
    // ShortArray does not auto-implement structural equals/hashCode
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AudioFrame) return false
        return amplitudeDb == other.amplitudeDb && pcmData.contentEquals(other.pcmData)
    }
    override fun hashCode(): Int = 31 * pcmData.contentHashCode() + amplitudeDb.hashCode()
}

/**
 * Low-level PCM audio capture engine backed by [AudioRecord].
 *
 * Specifications:
 *  - Sample rate: 16 000 Hz (IELTS STT-compatible)
 *  - Channels: Mono
 *  - Format: PCM 16-bit
 *  - Output: WAV-encoded [ByteArray] from [stopCapture]
 *
 * Silence detection uses a -40 dBFS threshold. Consecutive frames below
 * this threshold increment [SilenceTelemetry] counters.
 *
 * All captured PCM is buffered in-memory until [stopCapture] is called.
 * Maximum practical recording duration: ~14 minutes (≈27 MB for a full session).
 */
@Singleton
class AudioCaptureEngine @Inject constructor() {

    companion object {
        private const val SAMPLE_RATE        = 16_000
        private const val CHANNEL_CONFIG     = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT       = AudioFormat.ENCODING_PCM_16BIT
        private const val SILENCE_THRESHOLD_DB = -40f
    }

    private val engineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val bufferSize: Int = AudioRecord
        .getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        .takeIf { it > 0 } ?: 4_096

    private var audioRecord : AudioRecord? = null
    private var captureJob  : Job?         = null
    private val capturedFrames = mutableListOf<Short>()

    @Volatile private var isCapturing = false

    // ── Live amplitude stream ─────────────────────────────────────────────────
    private val _audioFrames = MutableSharedFlow<AudioFrame>(extraBufferCapacity = 64)
    val audioFrames: SharedFlow<AudioFrame> = _audioFrames.asSharedFlow()

    // ── Silence telemetry state ───────────────────────────────────────────────
    private var silenceCount             = 0
    private var currentSilenceStartMs    = 0L
    private var maxSilenceDurationMs     = 0L
    private var totalSilenceMs           = 0L
    private var inSilence                = false

    /** Finalized telemetry report; valid after [stopCapture] returns. */
    var silenceTelemetry: SilenceTelemetry = SilenceTelemetry()
        private set

    // ─── Start ────────────────────────────────────────────────────────────────

    /**
     * Begins audio capture. Permission check is enforced at the UI layer
     * before this is invoked; [SuppressLint] suppresses the IDE warning only.
     */
    @SuppressLint("MissingPermission")
    fun startCapture() {
        if (isCapturing) return
        isCapturing = true
        synchronized(capturedFrames) { capturedFrames.clear() }
        resetSilenceTelemetry()

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize * 4   // 4× headroom to prevent buffer overrun on slow devices
        ).also { it.startRecording() }

        captureJob = engineScope.launch {
            val readBuffer = ShortArray(bufferSize)
            while (isCapturing) {
                val samplesRead = audioRecord?.read(readBuffer, 0, bufferSize) ?: 0
                if (samplesRead > 0) {
                    val frame = readBuffer.copyOf(samplesRead)
                    synchronized(capturedFrames) { capturedFrames.addAll(frame.asList()) }

                    val amplitudeDb        = computeAmplitudeDb(frame)
                    val normalizedAmplitude = ((amplitudeDb - (-96f)) / 96f).coerceIn(0f, 1f)
                    updateSilenceTelemetry(amplitudeDb)

                    _audioFrames.emit(AudioFrame(frame, normalizedAmplitude))
                }
            }
        }
    }

    // ─── Stop ─────────────────────────────────────────────────────────────────

    /**
     * Stops capture and returns a WAV-encoded [ByteArray] of all recorded audio.
     * Finalizes [silenceTelemetry] with accumulated metrics.
     */
    fun stopCapture(): ByteArray {
        isCapturing = false
        captureJob?.cancel()
        captureJob = null
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        // Finalize any in-progress silence segment
        if (inSilence && currentSilenceStartMs > 0L) {
            val duration = System.currentTimeMillis() - currentSilenceStartMs
            totalSilenceMs += duration
            if (duration > maxSilenceDurationMs) maxSilenceDurationMs = duration
            inSilence = false
        }

        val pcmData: ShortArray
        synchronized(capturedFrames) {
            pcmData = capturedFrames.toShortArray()
            capturedFrames.clear()
        }

        silenceTelemetry = SilenceTelemetry(
            silenceCount        = silenceCount,
            maxSilenceDurationMs = maxSilenceDurationMs,
            totalSilenceMs      = totalSilenceMs
        )

        return encodeToWav(pcmData)
    }

    // ─── WAV Encoding ─────────────────────────────────────────────────────────

    /**
     * Encodes raw 16-bit PCM samples into a standard RIFF/WAV byte array.
     * Output is suitable for direct submission to the Gemini audio API.
     */
    private fun encodeToWav(pcmData: ShortArray): ByteArray {
        val dataSize = pcmData.size * 2          // 2 bytes per 16-bit sample
        val byteRate = SAMPLE_RATE * 2           // mono × 2 bytes/sample
        val wav      = ByteArray(44 + dataSize)

        // RIFF header
        wav.writeAscii(0,  "RIFF")
        wav.writeInt  (4,  36 + dataSize)
        wav.writeAscii(8,  "WAVE")

        // fmt chunk
        wav.writeAscii(12, "fmt ")
        wav.writeInt  (16, 16)          // Chunk size
        wav.writeShort(20, 1)           // PCM format
        wav.writeShort(22, 1)           // Mono
        wav.writeInt  (24, SAMPLE_RATE)
        wav.writeInt  (28, byteRate)
        wav.writeShort(32, 2)           // Block align (1 channel × 2 bytes)
        wav.writeShort(34, 16)          // Bits per sample

        // data chunk
        wav.writeAscii(36, "data")
        wav.writeInt  (40, dataSize)

        var offset = 44
        for (sample in pcmData) {
            wav[offset++] = (sample.toInt() and 0xFF).toByte()
            wav[offset++] = ((sample.toInt() ushr 8) and 0xFF).toByte()
        }
        return wav
    }

    // ─── Byte-writing helpers ─────────────────────────────────────────────────

    private fun ByteArray.writeAscii(offset: Int, str: String) =
        str.forEachIndexed { i, c -> this[offset + i] = c.code.toByte() }

    private fun ByteArray.writeInt(offset: Int, value: Int) {
        this[offset]     = (value         and 0xFF).toByte()
        this[offset + 1] = ((value ushr 8)  and 0xFF).toByte()
        this[offset + 2] = ((value ushr 16) and 0xFF).toByte()
        this[offset + 3] = ((value ushr 24) and 0xFF).toByte()
    }

    private fun ByteArray.writeShort(offset: Int, value: Int) {
        this[offset]     = (value        and 0xFF).toByte()
        this[offset + 1] = ((value ushr 8) and 0xFF).toByte()
    }

    // ─── Amplitude computation ────────────────────────────────────────────────

    /**
     * Computes RMS amplitude in dBFS for the given PCM buffer.
     * Returns -96 dBFS for silence (zero/near-zero signal).
     */
    private fun computeAmplitudeDb(buffer: ShortArray): Float {
        if (buffer.isEmpty()) return -96f
        val sumOfSquares = buffer.fold(0.0) { acc, s -> acc + s.toDouble() * s }
        val rms          = sqrt(sumOfSquares / buffer.size).toFloat()
        return if (rms > 0f) (20f * log10(rms / 32_768f)).coerceAtLeast(-96f) else -96f
    }

    // ─── Silence telemetry ────────────────────────────────────────────────────

    private fun updateSilenceTelemetry(amplitudeDb: Float) {
        val nowMs = System.currentTimeMillis()
        if (amplitudeDb < SILENCE_THRESHOLD_DB) {
            if (!inSilence) {
                inSilence             = true
                currentSilenceStartMs = nowMs
                silenceCount++
            }
            // Accumulation happens when silence ends, not each frame
        } else {
            if (inSilence) {
                val duration = nowMs - currentSilenceStartMs
                totalSilenceMs += duration
                if (duration > maxSilenceDurationMs) maxSilenceDurationMs = duration
                inSilence = false
            }
        }
    }

    private fun resetSilenceTelemetry() {
        silenceCount          = 0
        currentSilenceStartMs = 0L
        maxSilenceDurationMs  = 0L
        totalSilenceMs        = 0L
        inSilence             = false
    }
}
