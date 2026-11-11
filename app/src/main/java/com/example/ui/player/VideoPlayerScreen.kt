package com.example.ui.player

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.model.AspectRatioMode
import com.example.model.IptvItem
import com.example.model.IptvType
import com.example.model.PlayerUiState
import com.example.player.ExoPlayerManager
import com.example.ui.locale.LocalAppLanguage
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentRed
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import java.util.Locale
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    playerManager: ExoPlayerManager,
    uiState: PlayerUiState,
    allChannels: List<IptvItem>,
    onBack: () -> Unit,
    onPlayNext: () -> Unit,
    onPlayPrevious: () -> Unit,
    onSelectChannel: (IptvItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val isAr = LocalAppLanguage.current == "ar"
    val currentItem = uiState.currentItem

    // Reliably detect VOD (Movies & Series) vs Live TV
    val isVod = remember(currentItem, uiState.durationMs) {
        currentItem?.type == IptvType.MOVIE ||
        currentItem?.type == IptvType.SERIES ||
        currentItem?.seasonNumber != null ||
        currentItem?.episodeNumber != null ||
        currentItem?.streamUrl?.contains("/movie/", ignoreCase = true) == true ||
        currentItem?.streamUrl?.contains("/series/", ignoreCase = true) == true ||
        uiState.durationMs > 60_000L
    }

    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    val subtitleOffsetMs by playerManager.subtitleOffsetMs.collectAsState()

    val subtitleFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            playerManager.loadExternalSubtitle(uri, if (isAr) "ملف ترجمة خارجي" else "External Subtitle")
        }
    }

    // Focus Requesters for Android TV D-Pad Navigation
    val rootFocusRequester = remember { FocusRequester() }
    val playPauseFocusRequester = remember { FocusRequester() }
    val firstDrawerItemFocusRequester = remember { FocusRequester() }

    // 1. Grab D-pad focus immediately when VideoPlayerScreen opens so background grid NEVER steals remote keys!
    LaunchedEffect(Unit) {
        delay(80L)
        try {
            if (uiState.isControlsVisible) {
                playPauseFocusRequester.requestFocus()
            } else {
                rootFocusRequester.requestFocus()
            }
        } catch (_: Exception) {}
    }

    // 2. Shift focus cleanly between Play/Pause button and Root player container when OSD controls show/hide
    LaunchedEffect(uiState.isControlsVisible, uiState.isQuickChannelListOpen) {
        delay(60L)
        try {
            when {
                uiState.isQuickChannelListOpen -> firstDrawerItemFocusRequester.requestFocus()
                uiState.isControlsVisible -> playPauseFocusRequester.requestFocus()
                else -> rootFocusRequester.requestFocus()
            }
        } catch (_: Exception) {}
    }

    // Handle TV Remote Back Button: If OSD controls or drawer are visible, hide them first; otherwise exit player
    BackHandler {
        when {
            showAudioDialog -> showAudioDialog = false
            showSubtitleDialog -> showSubtitleDialog = false
            uiState.isQuickChannelListOpen -> playerManager.closeQuickChannelList()
            else -> onBack()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(rootFocusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                // Global TV Remote Key Handler for Video Player
                if (event.type == KeyEventType.KeyDown) {
                    // Always reset the 5-second auto-hide timer whenever the user presses any remote button
                    if (uiState.isControlsVisible) {
                        playerManager.scheduleControlsAutoHide()
                    }

                    // Dedicated Media Keys on TV Remotes (work whether controls are visible or hidden)
                    when (event.key) {
                        Key.MediaPlayPause -> {
                            playerManager.togglePlayPause()
                            playerManager.showControlsTemporarily()
                            return@onPreviewKeyEvent true
                        }
                        Key.MediaPlay -> {
                            if (!uiState.isPlaying) playerManager.togglePlayPause()
                            playerManager.showControlsTemporarily()
                            return@onPreviewKeyEvent true
                        }
                        Key.MediaPause -> {
                            if (uiState.isPlaying) playerManager.togglePlayPause()
                            playerManager.showControlsPermanently()
                            return@onPreviewKeyEvent true
                        }
                        Key.MediaFastForward -> {
                            if (isVod) {
                                playerManager.seekTo(uiState.currentPositionMs + 10_000L)
                                playerManager.showControlsTemporarily()
                                return@onPreviewKeyEvent true
                            }
                        }
                        Key.MediaRewind -> {
                            if (isVod) {
                                playerManager.seekTo((uiState.currentPositionMs - 10_000L).coerceAtLeast(0L))
                                playerManager.showControlsTemporarily()
                                return@onPreviewKeyEvent true
                            }
                        }
                        Key.ChannelUp, Key.PageUp -> {
                            onPlayNext()
                            playerManager.showControlsTemporarily()
                            return@onPreviewKeyEvent true
                        }
                        Key.ChannelDown, Key.PageDown -> {
                            onPlayPrevious()
                            playerManager.showControlsTemporarily()
                            return@onPreviewKeyEvent true
                        }
                        else -> {}
                    }

                    // When OSD Controls are HIDDEN: Any D-pad button wakes them up (and Left/Right seeks immediately!)
                    if (!uiState.isControlsVisible && !uiState.isQuickChannelListOpen) {
                        when (event.key) {
                            Key.DirectionRight -> {
                                if (isVod) {
                                    playerManager.seekTo(uiState.currentPositionMs + 10_000L)
                                }
                                playerManager.showControlsTemporarily()
                                return@onPreviewKeyEvent true
                            }
                            Key.DirectionLeft -> {
                                if (isVod) {
                                    playerManager.seekTo((uiState.currentPositionMs - 10_000L).coerceAtLeast(0L))
                                }
                                playerManager.showControlsTemporarily()
                                return@onPreviewKeyEvent true
                            }
                            Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.DirectionUp, Key.DirectionDown -> {
                                playerManager.showControlsTemporarily()
                                return@onPreviewKeyEvent true
                            }
                            else -> {}
                        }
                    }
                }
                false
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (uiState.isQuickChannelListOpen) {
                            playerManager.closeQuickChannelList()
                        } else {
                            playerManager.toggleControlsVisibility()
                        }
                    },
                    onDoubleTap = { offset ->
                        if (isVod) {
                            val screenWidth = size.width
                            if (offset.x < screenWidth * 0.4f) {
                                playerManager.seekTo((uiState.currentPositionMs - 10_000L).coerceAtLeast(0L))
                                playerManager.showControlsTemporarily()
                            } else if (offset.x > screenWidth * 0.6f) {
                                playerManager.seekTo(uiState.currentPositionMs + 10_000L)
                                playerManager.showControlsTemporarily()
                            } else {
                                playerManager.togglePlayPause()
                            }
                        } else {
                            playerManager.togglePlayPause()
                        }
                    }
                )
            }
    ) {
        // 1. ExoPlayer Video Surface (Non-focusable so it never steals D-pad focus from our Compose buttons)
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = playerManager.player
                    useController = false
                    isFocusable = false
                    isFocusableInTouchMode = false
                    keepScreenOn = true
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.keepScreenOn = true
                if (playerView.player !== playerManager.player) {
                    playerView.player = playerManager.player
                }
                playerView.resizeMode = when (uiState.aspectRatioMode) {
                    AspectRatioMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    AspectRatioMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    AspectRatioMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    AspectRatioMode.SIXTEEN_BY_NINE -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                    AspectRatioMode.FOUR_BY_THREE -> AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
                }
            }
        )

        // 2. Buffering Spinner
        if (uiState.isBuffering && uiState.errorMessage == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = AccentCyan,
                        strokeWidth = 3.5.dp,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isAr) "جاري التحميل..." else "Loading stream...",
                        color = PureWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
