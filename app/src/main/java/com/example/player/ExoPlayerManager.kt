package com.example.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.model.AspectRatioMode
import com.example.model.IptvItem
import com.example.model.PlayerUiState
import com.example.model.StreamFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class ExoPlayerManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {

    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setUserAgent("HypnoStream-IPTV/1.0 (Linux; Android; ExoPlayer)")
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(15000)
        .setReadTimeoutMs(25000)

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(DefaultMediaSourceFactory(httpDataSourceFactory))
        .build()

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var progressJob: Job? = null
    private var controlsHideJob: Job? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        _uiState.value = _uiState.value.copy(
                            isBuffering = true,
                            errorMessage = null
                        )
                    }
                    Player.STATE_READY -> {
                        _uiState.value = _uiState.value.copy(
                            isBuffering = false,
                            isPlaying = player.isPlaying,
                            durationMs = player.duration.coerceAtLeast(0L),
                            errorMessage = null
                        )
                    }
                    Player.STATE_ENDED -> {
                        _uiState.value = _uiState.value.copy(
                            isBuffering = false,
                            isPlaying = false
                        )
                    }
                    Player.STATE_IDLE -> {
                        _uiState.value = _uiState.value.copy(
                            isBuffering = false
                        )
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
                if (isPlaying) {
                    startProgressTracker()
                    scheduleControlsAutoHide()
                } else {
                    stopProgressTracker()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val errorDetails = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                        "Network connection failed. Check your internet connection or server status."
                    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                        "Stream server returned an HTTP error (stream might be offline or geo-restricted)."
                    PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
                    PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED ->
                        "Invalid stream format (.m3u8/.ts playlist error)."
                    else -> error.localizedMessage ?: "Playback error occurred"
                }

                _uiState.value = _uiState.value.copy(
                    isBuffering = false,
                    isPlaying = false,
                    errorMessage = errorDetails
                )
            }
        })
    }

    fun playItem(item: IptvItem) {
        _uiState.value = _uiState.value.copy(
            currentItem = item,
            isBuffering = true,
            errorMessage = null,
            isControlsVisible = true
        )

        try {
            val uri = Uri.parse(item.streamUrl)
            val format = item.detectFormat()

            // If explicitly HLS, create HlsMediaSource
            val mediaSource = if (format == StreamFormat.HLS || item.streamUrl.contains(".m3u8", ignoreCase = true)) {
                HlsMediaSource.Factory(httpDataSourceFactory)
                    .setAllowChunklessPreparation(true)
                    .createMediaSource(MediaItem.fromUri(uri))
            } else {
                DefaultMediaSourceFactory(httpDataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(uri))
            }

            player.stop()
            player.clearMediaItems()
            player.setMediaSource(mediaSource)
            player.prepare()
            player.playWhenReady = true

            scheduleControlsAutoHide()
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isBuffering = false,
                errorMessage = "Failed to load stream: ${e.localizedMessage}"
            )
        }
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
            showControlsPermanently()
        } else {
            player.play()
            scheduleControlsAutoHide()
        }
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceIn(0L, player.duration.coerceAtLeast(0L)))
    }

    fun cycleAspectRatio() {
        val current = _uiState.value.aspectRatioMode
        val next = when (current) {
            AspectRatioMode.FIT -> AspectRatioMode.FILL
            AspectRatioMode.FILL -> AspectRatioMode.SIXTEEN_BY_NINE
            AspectRatioMode.SIXTEEN_BY_NINE -> AspectRatioMode.FOUR_BY_THREE
            AspectRatioMode.FOUR_BY_THREE -> AspectRatioMode.ZOOM
            AspectRatioMode.ZOOM -> AspectRatioMode.FIT
        }
        _uiState.value = _uiState.value.copy(aspectRatioMode = next)
        scheduleControlsAutoHide()
    }

    fun toggleControlsVisibility() {
        val next = !_uiState.value.isControlsVisible
        _uiState.value = _uiState.value.copy(isControlsVisible = next)
        if (next && player.isPlaying) {
            scheduleControlsAutoHide()
        }
    }

    fun showControlsPermanently() {
        controlsHideJob?.cancel()
        _uiState.value = _uiState.value.copy(isControlsVisible = true)
    }

    fun showControlsTemporarily() {
        _uiState.value = _uiState.value.copy(isControlsVisible = true)
        scheduleControlsAutoHide()
    }

    fun scheduleControlsAutoHide() {
        controlsHideJob?.cancel()
        controlsHideJob = coroutineScope.launch {
            delay(5000)
            if (isActive && player.isPlaying && !_uiState.value.isQuickChannelListOpen) {
                _uiState.value = _uiState.value.copy(isControlsVisible = false)
            }
        }
    }

    fun toggleQuickChannelList() {
        val next = !_uiState.value.isQuickChannelListOpen
        _uiState.value = _uiState.value.copy(isQuickChannelListOpen = next)
        if (next) {
            controlsHideJob?.cancel()
        } else if (player.isPlaying) {
            scheduleControlsAutoHide()
        }
    }

    fun closeQuickChannelList() {
        _uiState.value = _uiState.value.copy(isQuickChannelListOpen = false)
        if (player.isPlaying) {
            scheduleControlsAutoHide()
        }
    }

    fun retryPlayback() {
        _uiState.value.currentItem?.let { playItem(it) }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = coroutineScope.launch(Dispatchers.Main) {
            while (isActive && player.isPlaying) {
                _uiState.value = _uiState.value.copy(
                    currentPositionMs = player.currentPosition.coerceAtLeast(0L),
                    durationMs = player.duration.coerceAtLeast(0L)
                )
                delay(1000)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
    }

    fun release() {
        progressJob?.cancel()
        controlsHideJob?.cancel()
        player.release()
    }
}
