package com.example.player

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.model.AspectRatioMode
import com.example.model.IptvItem
import com.example.model.IptvType
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

    // Persistent storage for Movie & TV Series resume positions
    private val positionPrefs: SharedPreferences =
        context.getSharedPreferences("hypnostream_playback_positions", Context.MODE_PRIVATE)
    private val appPrefs: SharedPreferences =
        context.getSharedPreferences("hypnostream_prefs", Context.MODE_PRIVATE)

    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setUserAgent("IPTVSmartersPlayer")
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(15000)
        .setReadTimeoutMs(25000)

    private val renderersFactory = DefaultRenderersFactory(context)
        .setEnableDecoderFallback(true)
        .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

    private val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            15_000,
            30_000,
            1_500,
            3_000
        )
        .setPrioritizeTimeOverSizeThresholds(true)
        .build()

    val player: ExoPlayer = ExoPlayer.Builder(context, renderersFactory)
        .setMediaSourceFactory(DefaultMediaSourceFactory(httpDataSourceFactory))
        .setLoadControl(loadControl)
        .build().apply {
            setWakeMode(C.WAKE_MODE_NETWORK)
            setHandleAudioBecomingNoisy(true)
        }

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    // Subtitle timing offset state (compatible with both PlayerUiState and direct calls)
    private val _subtitleOffsetMs = MutableStateFlow(0L)
    val subtitleOffsetMs: StateFlow<Long> = _subtitleOffsetMs.asStateFlow()

    private val _externalSubtitleUri = MutableStateFlow<String?>(null)
    val externalSubtitleUri: StateFlow<String?> = _externalSubtitleUri.asStateFlow()

    private var activeSubtitleConfigs = mutableListOf<MediaItem.SubtitleConfiguration>()

    private var progressJob: Job? = null
    private var controlsHideJob: Job? = null
    private var reconnectJob: Job? = null
    private var retryCount = 0
    private val maxRetries = 4

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
                        reconnectJob?.cancel()
                        _uiState.value = _uiState.value.copy(
                            isBuffering = false,
                            isPlaying = player.isPlaying,
                            durationMs = player.duration.coerceAtLeast(0L),
                            errorMessage = null
                        )
                        updateTracksState(player.currentTracks)
                    }
                    Player.STATE_ENDED -> {
                        _uiState.value.currentItem?.let { clearSavedPosition(it) }
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
                    saveCurrentPlaybackPosition()
                    stopProgressTracker()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                saveCurrentPlaybackPosition()
                if (retryCount < maxRetries) {
                    retryCount++
                    _uiState.value = _uiState.value.copy(
                        isBuffering = true,
                        errorMessage = null
                    )
                    reconnectJob?.cancel()
                    reconnectJob = coroutineScope.launch(Dispatchers.Main) {
                        delay(2000L)
                        val item = _uiState.value.currentItem
                        if (item != null) {
                            loadStreamInternal(item, isReconnect = true)
                        }
                    }
                    return
                }

                val errorDetails = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                        "Stream failed after $maxRetries reconnection attempts (Connection timeout)."
                    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                        "Stream offline after $maxRetries attempts (Server returned HTTP error)."
                    PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
                    PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED ->
                        "Stream format unsupported or broken after $maxRetries attempts."
                    else -> "Stream is not working after $maxRetries reconnection attempts."
                }

                _uiState.value = _uiState.value.copy(
                    isBuffering = false,
                    isPlaying = false,
                    errorMessage = errorDetails
                )
            }

            override fun onTracksChanged(tracks: Tracks) {
                updateTracksState(tracks)
            }
        })
    }

    private fun isVodOrSeries(item: IptvItem): Boolean {
        return item.type == IptvType.MOVIE ||
                item.type == IptvType.SERIES ||
                item.seasonNumber != null ||
                item.episodeNumber != null ||
                item.group.contains("Movie", ignoreCase = true) ||
                item.group.contains("Film", ignoreCase = true) ||
                item.group.contains("VOD", ignoreCase = true) ||
                item.group.contains("Series", ignoreCase = true) ||
                item.group.contains("Season", ignoreCase = true) ||
                item.group.contains("الموسم", ignoreCase = true) ||
                item.group.contains("أفلام", ignoreCase = true) ||
                item.group.contains("مسلسل", ignoreCase = true) ||
                item.name.contains(Regex("""S\d{1,2}E\d{1,3}""", RegexOption.IGNORE_CASE)) ||
                item.streamUrl.contains("/movie/", ignoreCase = true) ||
                item.streamUrl.contains("/series/", ignoreCase = true)
    }

    private fun positionKey(item: IptvItem): String {
        return "resume_${item.id}_${item.streamUrl.hashCode()}"
    }

    fun getSavedPosition(item: IptvItem): Long {
        if (!isVodOrSeries(item)) return 0L
        return positionPrefs.getLong(positionKey(item), 0L).coerceAtLeast(0L)
    }

    fun clearSavedPosition(item: IptvItem) {
        positionPrefs.edit().remove(positionKey(item)).apply()
    }

    fun saveCurrentPlaybackPosition() {
        val currentItem = _uiState.value.currentItem ?: return
        if (!isVodOrSeries(currentItem)) return

        val pos = player.currentPosition
        val dur = player.duration
        if (dur > 60_000L && pos >= (dur * 0.95).toLong()) {
            clearSavedPosition(currentItem)
        } else if (pos > 5_000L) {
            positionPrefs.edit().putLong(positionKey(currentItem), pos).apply()
        }
    }

    fun playItem(item: IptvItem) {
        saveCurrentPlaybackPosition()

        retryCount = 0
        reconnectJob?.cancel()
        activeSubtitleConfigs.clear()
        _externalSubtitleUri.value = null
        _subtitleOffsetMs.value = 0L

        val resumePos = getSavedPosition(item)

        _uiState.value = _uiState.value.copy(
            currentItem = item,
            isBuffering = true,
            isPlaying = true,
            currentPositionMs = resumePos,
            errorMessage = null
        )
        syncSubtitleStateToUiState()
        loadStreamInternal(item, isReconnect = false)
    }

    private fun loadStreamInternal(item: IptvItem, isReconnect: Boolean = false) {
        try {
            val uri = Uri.parse(item.streamUrl.trim())
            val format = item.detectFormat()
            val resumePos = getSavedPosition(item)

            val mediaItemBuilder = MediaItem.Builder().setUri(uri)
            if (activeSubtitleConfigs.isNotEmpty()) {
                mediaItemBuilder.setSubtitleConfigurations(activeSubtitleConfigs.toList())
            }
            val mediaItem = mediaItemBuilder.build()

            val mediaSource = if ( activeSubtitleConfigs.isEmpty() &&
                (format == StreamFormat.HLS || item.streamUrl.contains(".m3u8", ignoreCase = true))
            ) {
                HlsMediaSource.Factory(httpDataSourceFactory)
                    .setAllowChunklessPreparation(true)
                    .createMediaSource(mediaItem)
            } else {
                DefaultMediaSourceFactory(httpDataSourceFactory)
                    .createMediaSource(mediaItem)
            }

            if (resumePos > 5_000L) {
                player.setMediaSource(mediaSource, resumePos)
                if (!isReconnect) {
                    val isAr = appPrefs.getString("app_language", "en") == "ar"
                    val timeStr = formatResumeTime(resumePos)
                    val msg = if (isAr) "تم استئناف التشغيل من $timeStr" else "Resuming from $timeStr"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            } else {
                player.setMediaSource(mediaSource, true)
            }

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

    private fun formatResumeTime(millis: Long): String {
        val totalSeconds = (millis / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val hours = minutes / 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes % 60, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            saveCurrentPlaybackPosition()
            player.pause()
            showControlsPermanently()
        } else {
            player.play()
            scheduleControlsAutoHide()
        }
    }

    fun seekTo(positionMs: Long) {
        val target = positionMs.coerceIn(0L, player.duration.coerceAtLeast(0L))
        player.seekTo(target)
        _uiState.value = _uiState.value.copy(currentPositionMs = target)
        saveCurrentPlaybackPosition()
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
        retryCount = 0
        _uiState.value.currentItem?.let { playItem(it) }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = coroutineScope.launch(Dispatchers.Main) {
            var tickCount = 0
            while (isActive && player.isPlaying) {
                _uiState.value = _uiState.value.copy(
                    currentPositionMs = player.currentPosition.coerceAtLeast(0L),
                    durationMs = player.duration.coerceAtLeast(0L)
                )
                tickCount++
                if (tickCount % 3 == 0) {
                    saveCurrentPlaybackPosition()
                }
                delay(1000)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
    }

    // --- External Subtitle & Subtitle Timing Sync Functions ---

    fun loadExternalSubtitle(uri: Uri, label: String = "External Subtitle") {
        val currentItem = _uiState.value.currentItem ?: return
        saveCurrentPlaybackPosition()
        val currentPos = player.currentPosition.coerceAtLeast(0L)

        try {
            val uriStr = uri.toString().lowercase(Locale.ROOT)
            val mimeType = when {
                uriStr.endsWith(".vtt") -> MimeTypes.TEXT_VTT
                uriStr.endsWith(".ass") || uriStr.endsWith(".ssa") -> MimeTypes.TEXT_SSA
                uriStr.endsWith(".ttml") || uriStr.endsWith(".xml") -> MimeTypes.APPLICATION_TTML
                else -> MimeTypes.APPLICATION_SUBRIP
            }

            val subConfig = MediaItem.SubtitleConfiguration.Builder(uri)
                .setMimeType(mimeType)
                .setLanguage("ar")
                .setLabel(label)
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT or C.SELECTION_FLAG_AUTOSELECT)
                .build()

            activeSubtitleConfigs.clear()
            activeSubtitleConfigs.add(subConfig)
            _externalSubtitleUri.value = uri.toString()
            syncSubtitleStateToUiState()

            val mediaItem = MediaItem.Builder()
                .setUri(Uri.parse(currentItem.streamUrl.trim()))
                .setSubtitleConfigurations(activeSubtitleConfigs.toList())
                .build()

            val mediaSource = DefaultMediaSourceFactory(httpDataSourceFactory)
                .createMediaSource(mediaItem)

            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .build()

            player.setMediaSource(mediaSource, currentPos)
            player.prepare()
            player.playWhenReady = true
            updateTracksState()
        } catch (_: Exception) {}
    }

    fun loadExternalSubtitleUrl(url: String, label: String = "Online Subtitle") {
        val clean = url.trim()
        if (clean.isNotEmpty()) {
            loadExternalSubtitle(Uri.parse(clean), label)
        }
    }

    fun clearExternalSubtitles() {
        activeSubtitleConfigs.clear()
        _externalSubtitleUri.value = null
        syncSubtitleStateToUiState()
    }

    fun adjustSubtitleOffset(deltaMs: Long) {
        val updated = (_subtitleOffsetMs.value + deltaMs).coerceIn(-60_000L, 60_000L)
        _subtitleOffsetMs.value = updated
        syncSubtitleStateToUiState()
    }

    fun setSubtitleOffset(offsetMs: Long) {
        _subtitleOffsetMs.value = offsetMs.coerceIn(-60_000L, 60_000L)
        syncSubtitleStateToUiState()
    }

    fun resetSubtitleOffset() {
        _subtitleOffsetMs.value = 0L
        syncSubtitleStateToUiState()
    }

    /**
     * Dynamically updates `subtitleOffsetMs` and `externalSubtitleUri` on `PlayerUiState`
     * if those fields exist in `PlayerModels.kt` without breaking compilation if they don't.
     */
    private fun syncSubtitleStateToUiState() {
        try {
            val current = _uiState.value
            val cls = current::class.java
            val offsetField = cls.declaredFields.firstOrNull { it.name == "subtitleOffsetMs" }
            val uriField = cls.declaredFields.firstOrNull { it.name == "externalSubtitleUri" }
            if (offsetField != null || uriField != null) {
                offsetField?.let {
                    it.isAccessible = true
                    it.setLong(current, _subtitleOffsetMs.value)
                }
                uriField?.let {
                    it.isAccessible = true
                    it.set(current, _externalSubtitleUri.value)
                }
                // Trigger StateFlow emission
                _uiState.value = current.copy(isBuffering = current.isBuffering)
            }
        } catch (_: Exception) {}
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
        syncSubtitleStateToUiState()
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
        saveCurrentPlaybackPosition()
        reconnectJob?.cancel()
        progressJob?.cancel()
        controlsHideJob?.cancel()
        player.stop()
        player.clearMediaItems()
        _uiState.value = PlayerUiState()
    }

    fun release() {
        saveCurrentPlaybackPosition()
        reconnectJob?.cancel()
        progressJob?.cancel()
        controlsHideJob?.cancel()
        player.release()
    }
}
