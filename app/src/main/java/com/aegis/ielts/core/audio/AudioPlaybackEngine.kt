package com.aegis.ielts.core.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Discriminated union representing the playback lifecycle state.
 */
sealed class PlaybackState {
    object Idle      : PlaybackState()
    object Loading   : PlaybackState()
    object Playing   : PlaybackState()
    object Completed : PlaybackState()
    data class Error(val message: String) : PlaybackState()
}

/**
 * ExoPlayer-backed audio playback engine for the IELTS examiner voice delivery.
 *
 * Contract:
 *  - Seek and rewind controls are intentionally NOT exposed (exam integrity).
 *  - All ExoPlayer operations are dispatched to the Main thread (ExoPlayer requirement).
 *  - [isPlaying] and [playbackState] are hot [StateFlow]s safe for Compose collection.
 *
 * Injected as a @Singleton — lives for the application lifetime.
 */
@Singleton
class AudioPlaybackEngine @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val engineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var player: ExoPlayer? = null

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    // ─── Player listener ──────────────────────────────────────────────────────

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            _playbackState.value = when (playbackState) {
                Player.STATE_BUFFERING -> PlaybackState.Loading
                Player.STATE_READY     -> PlaybackState.Playing
                Player.STATE_ENDED     -> PlaybackState.Completed
                Player.STATE_IDLE      -> PlaybackState.Idle
                else                   -> PlaybackState.Idle
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            _playbackState.value = PlaybackState.Error(error.message ?: "Unknown playback error")
            _isPlaying.value = false
        }
    }

    // ─── Player initialization ────────────────────────────────────────────────

    /**
     * Returns the existing player or builds a new one.
     * MUST be called on the Main thread.
     */
    private fun ensurePlayer(): ExoPlayer =
        player ?: ExoPlayer.Builder(context).build().also { newPlayer ->
            newPlayer.addListener(playerListener)
            player = newPlayer
        }

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Plays audio from the given [Uri].
     * Stopping any current playback before starting the new item.
     * Seek and rewind are not surfaced (exam integrity contract).
     */
    suspend fun playFromUri(uri: Uri) = withContext(Dispatchers.Main) {
        val exo = ensurePlayer()
        exo.stop()
        exo.clearMediaItems()
        exo.setMediaItem(MediaItem.fromUri(uri))
        exo.prepare()
        exo.play()
        _playbackState.value = PlaybackState.Loading
    }

    /**
     * Plays an audio file bundled in the app's assets directory.
     *
     * @param assetPath  Relative path within assets/, e.g. "audio/part1_intro.mp3"
     */
    suspend fun playFromAsset(assetPath: String) {
        val cleanPath = assetPath.replace("asset:///", "")
        playFromUri(Uri.parse("file:///android_asset/$cleanPath"))
    }

    /** Stops playback and resets state to Idle. */
    suspend fun stop() = withContext(Dispatchers.Main) {
        player?.stop()
        _playbackState.value = PlaybackState.Idle
        _isPlaying.value = false
    }

    /**
     * Releases the ExoPlayer instance.
     * Called from [SpeakingViewModel.onCleared] — safe to call on any thread.
     */
    fun release() {
        engineScope.launch {
            player?.release()
            player = null
            _playbackState.value = PlaybackState.Idle
            _isPlaying.value = false
        }
    }
}
