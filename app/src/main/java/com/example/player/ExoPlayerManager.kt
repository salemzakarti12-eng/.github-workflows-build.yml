package com.example.player

import android.content.Context
import android.net.Uri
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
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class SubtitleCue(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String
)

@OptIn(UnstableApi::class)
class ExoPlayerManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {

    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setUserAgent("IPTVSmartersPlayer")
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
    private var reconnectJob: Job? = null
    private var retryCount = 0
    private val maxRetries = 4

    private var parsedExternalCues: List<SubtitleCue> = emptyList()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
        .build()

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
                // Automatic 4x Reconnection before showing error!
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
                            loadStreamInternal(item)
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

    fun playItem(item: IptvItem) {
        retryCount = 0
        reconnectJob?.cancel()
        parsedExternalCues = emptyList()
        // Keep current controls visibility state if already in player so Next/Prev doesn't reset focus!
        _uiState.value = _uiState.value.copy(
            currentItem = item,
            isBuffering = true,
            isPlaying = true,
            errorMessage = null,
            subtitleOffsetMs = 0L,
            activeSubtitleText = null
        )
        loadStreamInternal(item)
    }

    private fun loadStreamInternal(item: IptvItem) {
        try {
            val uri = Uri.parse(item.streamUrl.trim())
            val format = item.detectFormat()

            val mediaSource = if (format == StreamFormat.HLS || item.streamUrl.contains(".m3u8", ignoreCase = true)) {
                HlsMediaSource.Factory(httpDataSourceFactory)
                    .setAllowChunklessPreparation(true)
                    .createMediaSource(MediaItem.fromUri(uri))
            } else {
                DefaultMediaSourceFactory(httpDataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(uri))
            }

            // Seamless hot-swap without calling player.stop() so it never drops into a paused state
            player.setMediaSource(mediaSource, true)
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
        val target = positionMs.coerceIn(0L, player.duration.coerceAtLeast(0L))
        player.seekTo(target)
        updateActiveSubtitleCue(target)
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
            while (isActive && player.isPlaying) {
                val currentPos = player.currentPosition.coerceAtLeast(0L)
                updateActiveSubtitleCue(currentPos)
                _uiState.value = _uiState.value.copy(
                    currentPositionMs = currentPos,
                    durationMs = player.duration.coerceAtLeast(0L)
                )
                delay(250)
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
        if (track.groupIndex == -1) {
            _uiState.value = _uiState.value.copy(isSubtitleOff = false)
            updateActiveSubtitleCue(player.currentPosition)
            return
        }
        parsedExternalCues = emptyList()
        _uiState.value = _uiState.value.copy(activeSubtitleText = null)
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
        parsedExternalCues = emptyList()
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .build()
        _uiState.value = _uiState.value.copy(
            isSubtitleOff = true,
            activeSubtitleText = null
        )
        updateTracksState()
    }

    fun setSubtitleOffset(offsetMs: Long) {
        _uiState.value = _uiState.value.copy(subtitleOffsetMs = offsetMs)
        updateActiveSubtitleCue(player.currentPosition)
    }

    fun adjustSubtitleOffset(deltaMs: Long) {
        val newOffset = _uiState.value.subtitleOffsetMs + deltaMs
        setSubtitleOffset(newOffset)
    }

    fun resetSubtitleOffset() {
        setSubtitleOffset(0L)
    }

    fun loadExternalSubtitle(uri: Uri, label: String) {
        coroutineScope.launch(Dispatchers.Main) {
            val uriString = uri.toString().lowercase()
            val isVtt = uriString.endsWith(".vtt") || uriString.contains(".vtt?") || label.contains("vtt", ignoreCase = true)
            val mimeType = if (isVtt) MimeTypes.TEXT_VTT else MimeTypes.APPLICATION_SUBRIP

            // 1. Parse text cues in background for offset shifting
            val rawContent = readSubtitleContent(uri)
            if (!rawContent.isNullOrBlank()) {
                parsedExternalCues = parseSubtitles(rawContent)
            }

            // 2. Build SubtitleConfiguration
            val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(uri)
                .setMimeType(mimeType)
                .setLabel(label)
                .setLanguage("und")
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()

            // 3. Attach to current MediaItem preserving playback position
            val currentPos = player.currentPosition
            val wasPlaying = player.isPlaying || player.playWhenReady
            val currentMediaItem = player.currentMediaItem ?: _uiState.value.currentItem?.let {
                MediaItem.fromUri(Uri.parse(it.streamUrl.trim()))
            }

            if (currentMediaItem != null) {
                val newMediaItem = currentMediaItem.buildUpon()
                    .setSubtitleConfigurations(listOf(subtitleConfig))
                    .build()

                val mediaSource = DefaultMediaSourceFactory(httpDataSourceFactory)
                    .createMediaSource(newMediaItem)

                player.setMediaSource(mediaSource, currentPos)
                player.prepare()
                player.playWhenReady = wasPlaying
            }

            // 4. Enable text tracks
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .build()

            // Update tracks list in UI state
            val customOption = TrackOption(
                groupIndex = -1,
                trackIndex = -1,
                label = "★ $label (External)",
                isSelected = true
            )
            val existing = _uiState.value.subtitleTracks.toMutableList()
            existing.removeAll { it.label.contains("(External)") }
            existing.add(0, customOption)

            _uiState.value = _uiState.value.copy(
                subtitleTracks = existing,
                isSubtitleOff = false
            )

            updateActiveSubtitleCue(currentPos)
        }
    }

    private suspend fun readSubtitleContent(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val scheme = uri.scheme?.lowercase()
            if (scheme == "http" || scheme == "https") {
                val req = Request.Builder().url(uri.toString()).build()
                httpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) resp.body?.string() else null
                }
            } else {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader(Charsets.UTF_8).readText()
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseSubtitles(content: String): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        val lines = content.replace("\r\n", "\n").replace("\r", "\n").lines()
        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.contains("-->")) {
                val arrowParts = line.split("-->")
                if (arrowParts.size == 2) {
                    val start = parseTimestampToMs(arrowParts[0].trim().split(" ")[0])
                    val end = parseTimestampToMs(arrowParts[1].trim().split(" ")[0])
                    if (start != null && end != null) {
                        i++
                        val textLines = mutableListOf<String>()
                        while (i < lines.size && lines[i].trim().isNotEmpty()) {
                            textLines.add(lines[i].trim())
                            i++
                        }
                        val text = textLines.joinToString("\n").replace(Regex("<[^>]*>"), "")
                        if (text.isNotBlank()) {
                            cues.add(SubtitleCue(start, end, text))
                        }
                    }
                }
            }
            i++
        }
        return cues
    }

    private fun parseTimestampToMs(timeStr: String): Long? {
        val clean = timeStr.trim().replace(',', '.')
        val parts = clean.split(':')
        return try {
            when (parts.size) {
                3 -> {
                    val hours = parts[0].toLong()
                    val minutes = parts[1].toLong()
                    val secParts = parts[2].split('.')
                    val seconds = secParts[0].toLong()
                    val millis = (if (secParts.size > 1) secParts[1].padEnd(3, '0').take(3) else "0").toLong()
                    (hours * 3600 + minutes * 60 + seconds) * 1000 + millis
                }
                2 -> {
                    val minutes = parts[0].toLong()
                    val secParts = parts[1].split('.')
                    val seconds = secParts[0].toLong()
                    val millis = (if (secParts.size > 1) secParts[1].padEnd(3, '0').take(3) else "0").toLong()
                    (minutes * 60 + seconds) * 1000 + millis
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun updateActiveSubtitleCue(currentPosMs: Long) {
        if (_uiState.value.isSubtitleOff || parsedExternalCues.isEmpty()) {
            if (_uiState.value.activeSubtitleText != null) {
                _uiState.value = _uiState.value.copy(activeSubtitleText = null)
            }
            return
        }

        val offset = _uiState.value.subtitleOffsetMs
        val effectiveTime = currentPosMs - offset
        val matchingCue = parsedExternalCues.firstOrNull { cue ->
            effectiveTime in cue.startTimeMs..cue.endTimeMs
        }

        val text = matchingCue?.text
        if (_uiState.value.activeSubtitleText != text) {
            _uiState.value = _uiState.value.copy(activeSubtitleText = text)
        }
    }

    fun stop() {
        reconnectJob?.cancel()
        progressJob?.cancel()
        controlsHideJob?.cancel()
        player.stop()
        player.clearMediaItems()
        _uiState.value = PlayerUiState()
    }

    fun release() {
        reconnectJob?.cancel()
        progressJob?.cancel()
        controlsHideJob?.cancel()
        player.release()
    }
}
