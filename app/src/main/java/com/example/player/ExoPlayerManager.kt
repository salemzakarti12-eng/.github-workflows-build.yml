package com.example.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.model.AspectRatioMode
import com.example.model.IptvItem
import com.example.model.PlayerUiState
import com.example.model.StreamFormat
import com.example.model.TrackOption
import java.util.Locale
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
        .setUserAgent("IPTVSmartersPlayer")
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(20000)
        .setReadTimeoutMs(30000)

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(DefaultMediaSourceFactory(httpDataSourceFactory))
        .build()

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var progressJob: Job? = null
    private var controlsHideJob: Job? = null
    private var retryCount = 0
    private var retryJob: Job? = null

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
                        retryCount = 0
                        retryJob?.cancel()
                        _uiState.value = _uiState.value.copy(
                            isBuffering = false,
                            isPlaying = player.isPlaying,
                            durationMs = player.duration.coerceAtLeast(0L),
                            errorMessage = null,
                            reconnectAttempt = 0
                        )
                        updateTracksState(player.currentTracks)
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
                val current = _uiState.value.currentItem
                // Automatically attempt to reconnect up to 4 times with a 2-second delay
                if (current != null && retryCount < 4) {
                    retryCount++
                    _uiState.value = _uiState.value.copy(
                        isBuffering = true,
                        isPlaying = false,
                        errorMessage = null,
                        reconnectAttempt = retryCount
                    )
                    retryJob?.cancel()
                    retryJob = coroutineScope.launch {
                        delay(2000)
                        if (isActive && _uiState.value.currentItem?.id == current.id) {
                            try {
                                player.stop()
                            } catch (_: Exception) {}
                            executePlay(current)
                        }
                    }
                    return
                }

                // Only if all 4 reconnection attempts fail, display the error overlay
                val errorDetails = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                        "Network connection failed. Check your internet connection or server status."
                    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                        "Stream server returned an HTTP error (stream might be offline or geo-restricted)."
                    PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
                    PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED ->
                        "Invalid stream format (.m3u8/.ts playlist error)."
                    else -> error.localizedMessage ?: "Stream not working / Playback error occurred"
                }

                _uiState.value = _uiState.value.copy(
                    isBuffering = false,
                    isPlaying = false,
                    errorMessage = errorDetails,
                    reconnectAttempt = 0
                )
            }

            override fun onTracksChanged(tracks: Tracks) {
                updateTracksState(tracks)
            }
        })
    }

    fun playItem(item: IptvItem) {
        retryCount = 0
        retryJob?.cancel()
        executePlay(item)
    }

    private fun executePlay(item: IptvItem) {
        _uiState.value = _uiState.value.copy(
            currentItem = item,
            isBuffering = true,
            errorMessage = null,
            isControlsVisible = true,
            reconnectAttempt = retryCount
        )

        try {
            val uri = Uri.parse(item.streamUrl.trim())
            val format = item.detectFormat()

            val mediaItem = MediaItem.Builder()
                .setUri(uri)
                .setMediaId(item.id)
                .build()

            // Instant Channel Zapping (Zero Pause / No Lag):
            // Hot-swap stream seamlessly in the background without auto-pausing or black screen freezes
            if (format == StreamFormat.HLS || item.streamUrl.contains(".m3u8", ignoreCase = true)) {
                val mediaSource = HlsMediaSource.Factory(httpDataSourceFactory)
                    .setAllowChunklessPreparation(true)
                    .createMediaSource(mediaItem)
                player.setMediaSource(mediaSource, /* resetPosition = */ true)
            } else {
                player.setMediaItem(mediaItem, /* resetPosition = */ true)
            }

            player.prepare()
            player.playWhenReady = true

            scheduleControlsAutoHide()
        } catch (e: Exception) {
            if (retryCount < 4) {
                retryCount++
                _uiState.value = _uiState.value.copy(
                    isBuffering = true,
                    reconnectAttempt = retryCount
                )
                retryJob?.cancel()
                retryJob = coroutineScope.launch {
                    delay(2000)
                    if (isActive && _uiState.value.currentItem?.id == item.id) {
                        executePlay(item)
                    }
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isBuffering = false,
                    errorMessage = "Failed to load stream: ${e.localizedMessage}",
                    reconnectAttempt = 0
                )
            }
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

    fun updateTracksState(tracks: Tracks = player.currentTracks) {
        val audioList = mutableListOf<TrackOption>()
        val subList = mutableListOf<TrackOption>()

        val groups = tracks.groups
        for (gIdx in groups.indices) {
            val group = groups[gIdx]
            val type = group.type
            for (tIdx in 0 until group.length) {
                val format = group.getTrackFormat(tIdx)
                val isSelected = group.isTrackSelected(tIdx)
                val lang = format.language?.let {
                    try { Locale(it).displayLanguage } catch (_: Exception) { it }
                } ?: format.language
                val channels = if (format.channelCount > 0) "${format.channelCount}ch" else null
                val label = listOfNotNull(
                    format.label?.takeIf { it.isNotBlank() },
                    lang?.takeIf { it.isNotBlank() },
                    channels
                ).joinToString(" • ").ifEmpty {
                    if (type == C.TRACK_TYPE_AUDIO) "Audio Track ${audioList.size + 1}" else "Subtitle ${subList.size + 1}"
                }

                val option = TrackOption(
                    groupIndex = gIdx,
                    trackIndex = tIdx,
                    label = label,
                    language = format.language,
                    isSelected = isSelected
                )

                if (type == C.TRACK_TYPE_AUDIO) {
                    audioList.add(option)
                } else if (type == C.TRACK_TYPE_TEXT) {
                    subList.add(option)
                }
            }
        }

        val isSubDisabled = player.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT) ||
                subList.none { it.isSelected }

        _uiState.value = _uiState.value.copy(
            audioTracks = audioList,
            subtitleTracks = subList,
            isSubtitleOff = isSubDisabled
        )
    }

    fun selectAudioTrack(track: TrackOption) {
        val groups = player.currentTracks.groups
        if (track.groupIndex in groups.indices) {
            val group = groups[track.groupIndex]
            val override = TrackSelectionOverride(group.mediaTrackGroup, track.trackIndex)
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                .setOverrideForType(override)
                .build()
            updateTracksState()
        }
    }

    fun selectSubtitleTrack(track: TrackOption) {
        val groups = player.currentTracks.groups
        if (track.groupIndex in groups.indices) {
            val group = groups[track.groupIndex]
            val override = TrackSelectionOverride(group.mediaTrackGroup, track.trackIndex)
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .setOverrideForType(override)
                .build()
            updateTracksState()
        }
    }

    fun disableSubtitles() {
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .build()
        updateTracksState()
    }

    fun stop() {
        progressJob?.cancel()
        controlsHideJob?.cancel()
        retryJob?.cancel()
        retryCount = 0
        player.stop()
        player.clearMediaItems()
        _uiState.value = PlayerUiState()
    }

    fun release() {
        progressJob?.cancel()
        controlsHideJob?.cancel()
        retryJob?.cancel()
        retryCount = 0
        player.release()
    }
}
