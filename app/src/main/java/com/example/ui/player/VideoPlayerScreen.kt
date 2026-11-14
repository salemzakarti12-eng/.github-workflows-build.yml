package com.example.ui.player

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
import com.example.util.LocalIsTelevision
import java.util.Locale
import kotlinx.coroutines.delay

private enum class PlayerGestureFeedback {
    NONE,
    REWIND_VOD,
    FORWARD_VOD,
    PREV_CHANNEL,
    NEXT_CHANNEL
}

private enum class VerticalSwipeHUD {
    NONE,
    BRIGHTNESS,
    VOLUME
}

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
    val context = LocalContext.current
    val localView = LocalView.current
    val haptics = LocalHapticFeedback.current
    val isTv = LocalIsTelevision.current
    val playerRootFocus = remember { FocusRequester() }
    val playPauseFocus = remember { FocusRequester() }

    val currentItem = uiState.currentItem
    val isLive = currentItem?.type == IptvType.LIVE_TV || playerManager.isCurrentStreamLive()
    val hasDrawerItems = allChannels.isNotEmpty() && currentItem?.type != IptvType.MOVIE

    // --- True Fullscreen Immersive Mode (Hides Time, Signal, Battery & Nav Bar) ---
    DisposableEffect(localView) {
        val prevKeepScreenOn = localView.keepScreenOn
        localView.keepScreenOn = true

        val window = (context as? Activity)?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, localView) }
        insetsController?.apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            localView.keepScreenOn = prevKeepScreenOn
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // --- Binge-Mode Auto-Play Next Episode Countdown (10 seconds) ---
    val nextEpisodeItem = remember(currentItem?.id, allChannels) {
        if (currentItem?.type == IptvType.SERIES && allChannels.size > 1) {
            val idx = allChannels.indexOfFirst { it.id == currentItem.id || it.streamUrl == currentItem.streamUrl }
            if (idx in 0 until allChannels.lastIndex) allChannels[idx + 1] else null
        } else null
    }

    var nextEpDismissedForId by remember(currentItem?.id) { mutableStateOf(false) }
    var nextEpCountdownSec by remember(currentItem?.id) { mutableIntStateOf(10) }

    val isNearEpisodeEnd = !isLive &&
        currentItem?.type == IptvType.SERIES &&
        nextEpisodeItem != null &&
        !nextEpDismissedForId &&
        uiState.durationMs > 60_000L &&
        (uiState.durationMs - uiState.currentPositionMs) in 1L..18_000L

    LaunchedEffect(isNearEpisodeEnd, currentItem?.id) {
        if (isNearEpisodeEnd) {
            nextEpCountdownSec = 10
            while (nextEpCountdownSec > 0) {
                delay(1000L)
                nextEpCountdownSec--
            }
            if (!nextEpDismissedForId) {
                onPlayNext()
            }
        }
    }

    // --- Vertical Brightness & Volume Swipe Gestures (Mobile/Tablet) ---
    val audioManager = remember {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }

    var activeSwipeHud by remember { mutableStateOf(VerticalSwipeHUD.NONE) }
    var swipeHudLevel by remember { mutableFloatStateOf(0.5f) }
    var swipeHudTriggerKey by remember { mutableIntStateOf(0) }

    var brightnessLevel by remember {
        val windowAttr = (context as? Activity)?.window?.attributes?.screenBrightness ?: -1f
        mutableFloatStateOf(if (windowAttr in 0f..1f) windowAttr else 0.55f)
    }

    LaunchedEffect(swipeHudTriggerKey) {
        if (activeSwipeHud != VerticalSwipeHUD.NONE) {
            delay(850L)
            activeSwipeHud = VerticalSwipeHUD.NONE
        }
    }

    // --- Double-Tap Kinetic Ripple State ---
    var gestureFeedback by remember { mutableStateOf(PlayerGestureFeedback.NONE) }
    var accumulatedSkipSeconds by remember { mutableIntStateOf(10) }
    var feedbackTriggerKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(feedbackTriggerKey) {
        if (gestureFeedback != PlayerGestureFeedback.NONE) {
            delay(650L)
            gestureFeedback = PlayerGestureFeedback.NONE
            accumulatedSkipSeconds = 10
        }
    }

    LaunchedEffect(Unit) {
        try {
            playerRootFocus.requestFocus()
        } catch (_: Exception) {}
    }

    LaunchedEffect(uiState.isControlsVisible) {
        if (uiState.isControlsVisible) {
            try {
                playPauseFocus.requestFocus()
            } catch (_: Exception) {}
        } else {
            try {
                playerRootFocus.requestFocus()
            } catch (_: Exception) {}
        }
    }

    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showSubtitleUrlInput by remember { mutableStateOf(false) }
    var subtitleUrlText by remember { mutableStateOf("") }

    val subtitleOffsetMs by playerManager.subtitleOffsetMs.collectAsState()

    val subtitlePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            playerManager.loadExternalSubtitle(uri, if (isAr) "ملف ترجمة خارجي" else "External Subtitle")
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(playerRootFocus)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.MediaPlayPause -> {
                            playerManager.togglePlayPause()
                            playerManager.showControlsTemporarily()
                            true
                        }
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            if (!uiState.isControlsVisible && !uiState.isQuickChannelListOpen) {
                                playerManager.showControlsTemporarily()
                                true
                            } else false
                        }
                        Key.DirectionUp -> {
                            if (!uiState.isControlsVisible && !uiState.isQuickChannelListOpen) {
                                if (isLive) {
                                    gestureFeedback = PlayerGestureFeedback.PREV_CHANNEL
                                    feedbackTriggerKey++
                                    onPlayPrevious()
                                } else {
                                    playerManager.showControlsTemporarily()
                                }
                                true
                            } else false
                        }
                        Key.DirectionDown -> {
                            if (!uiState.isControlsVisible && !uiState.isQuickChannelListOpen) {
                                if (isLive) {
                                    gestureFeedback = PlayerGestureFeedback.NEXT_CHANNEL
                                    feedbackTriggerKey++
                                    onPlayNext()
                                } else {
                                    playerManager.showControlsTemporarily()
                                }
                                true
                            } else false
                        }
                        Key.DirectionLeft -> {
                            if (!uiState.isControlsVisible && !uiState.isQuickChannelListOpen) {
                                if (isLive && hasDrawerItems) {
                                    playerManager.toggleQuickChannelList()
                                } else if (!isLive) {
                                    accumulatedSkipSeconds = if (gestureFeedback == PlayerGestureFeedback.REWIND_VOD) accumulatedSkipSeconds + 10 else 10
                                    gestureFeedback = PlayerGestureFeedback.REWIND_VOD
                                    feedbackTriggerKey++
                                    playerManager.seekRelative(-10_000L)
                                    playerManager.showControlsTemporarily()
                                }
                                true
                            } else false
                        }
                        Key.DirectionRight -> {
                            if (!uiState.isControlsVisible && !uiState.isQuickChannelListOpen && !isLive) {
                                accumulatedSkipSeconds = if (gestureFeedback == PlayerGestureFeedback.FORWARD_VOD) accumulatedSkipSeconds + 10 else 10
                                gestureFeedback = PlayerGestureFeedback.FORWARD_VOD
                                feedbackTriggerKey++
                                playerManager.seekRelative(10_000L)
                                playerManager.showControlsTemporarily()
                                true
                            } else false
                        }
                        Key.ChannelUp, Key.PageUp -> {
                            gestureFeedback = PlayerGestureFeedback.NEXT_CHANNEL
                            feedbackTriggerKey++
                            onPlayNext()
                            true
                        }
                        Key.ChannelDown, Key.PageDown -> {
                            gestureFeedback = PlayerGestureFeedback.PREV_CHANNEL
                            feedbackTriggerKey++
                            onPlayPrevious()
                            true
                        }
                        else -> {
                            playerManager.showControlsTemporarily()
                            false
                        }
                    }
                } else false
            }
            .pointerInput(isLive, uiState.currentItem?.id) {
                detectTapGestures(
                    onTap = {
                        playerManager.toggleControlsVisibility()
                    },
                    onDoubleTap = { tapOffset ->
                        val isRightSide = tapOffset.x > (size.width / 2f)
                        try {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        } catch (_: Exception) {}

                        if (isLive) {
                            if (isRightSide) {
                                gestureFeedback = PlayerGestureFeedback.NEXT_CHANNEL
                                feedbackTriggerKey++
                                onPlayNext()
                            } else {
                                gestureFeedback = PlayerGestureFeedback.PREV_CHANNEL
                                feedbackTriggerKey++
                                onPlayPrevious()
                            }
                        } else {
                            if (isRightSide) {
                                accumulatedSkipSeconds = if (gestureFeedback == PlayerGestureFeedback.FORWARD_VOD) accumulatedSkipSeconds + 10 else 10
                                gestureFeedback = PlayerGestureFeedback.FORWARD_VOD
                                feedbackTriggerKey++
                                playerManager.seekRelative(10_000L)
                            } else {
                                accumulatedSkipSeconds = if (gestureFeedback == PlayerGestureFeedback.REWIND_VOD) accumulatedSkipSeconds + 10 else 10
                                gestureFeedback = PlayerGestureFeedback.REWIND_VOD
                                feedbackTriggerKey++
                                playerManager.seekRelative(-10_000L)
                            }
                        }
                    }
                )
            }
            .pointerInput(isTv) {
                if (!isTv) {
                    var dragStartIsRight = false
                    var dragAccRatio = 0f
                    detectVerticalDragGestures(
                        onDragStart = { startOffset ->
                            dragStartIsRight = startOffset.x > (size.width / 2f)
                            dragAccRatio = if (dragStartIsRight) {
                                audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume.toFloat()
                            } else {
                                brightnessLevel
                            }
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            val screenHeight = size.height.toFloat().coerceAtLeast(1f)
                            val deltaRatio = (-dragAmount / (screenHeight * 0.65f))
                            dragAccRatio = (dragAccRatio + deltaRatio).coerceIn(0f, 1f)

                            if (dragStartIsRight) {
                                val targetVol = (dragAccRatio * maxVolume).toInt().coerceIn(0, maxVolume)
                                try {
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                                } catch (_: Exception) {}
                                activeSwipeHud = VerticalSwipeHUD.VOLUME
                                swipeHudLevel = dragAccRatio
                                swipeHudTriggerKey++
                            } else {
                                brightnessLevel = dragAccRatio.coerceIn(0.02f, 1f)
                                try {
                                    val activity = context as? Activity
                                    activity?.window?.let { win ->
                                        val lp = win.attributes
                                        lp.screenBrightness = brightnessLevel
                                        win.attributes = lp
                                    }
                                } catch (_: Exception) {}
                                activeSwipeHud = VerticalSwipeHUD.BRIGHTNESS
                                swipeHudLevel = brightnessLevel
                                swipeHudTriggerKey++
                            }
                        }
                    )
                }
            }
            .testTag("video_player_screen")
    ) {
        // ExoPlayer Video Surface
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = playerManager.player
                    useController = false
                    isFocusable = false
                    isFocusableInTouchMode = false
                    keepScreenOn = true
                    setKeepContentOnPlayerReset(true)
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.keepScreenOn = true
                playerView.setKeepContentOnPlayerReset(true)
                if (playerView.player !== playerManager.player) {
                    playerView.player = playerManager.player
                }
                playerView.resizeMode = when (uiState.aspectRatioMode) {
                    AspectRatioMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    AspectRatioMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    AspectRatioMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    AspectRatioMode.SIXTEEN_BY_NINE -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    AspectRatioMode.FOUR_BY_THREE -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            }
        )

        // Vertical Brightness / Volume Swipe HUD Pill
        AnimatedVisibility(
            visible = activeSwipeHud != VerticalSwipeHUD.NONE,
            enter = fadeIn(tween(120)),
            exit = fadeOut(tween(220)),
            modifier = Modifier.align(
                if (activeSwipeHud == VerticalSwipeHUD.BRIGHTNESS) Alignment.CenterStart else Alignment.CenterEnd
            )
        ) {
            Surface(
                color = Color(0xCC040D1A),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.5.dp, AccentCyan.copy(alpha = 0.8f)),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp)
                ) {
                    Icon(
                        imageVector = if (activeSwipeHud == VerticalSwipeHUD.BRIGHTNESS) {
                            Icons.Default.BrightnessHigh
                        } else {
                            Icons.AutoMirrored.Filled.VolumeUp
                        },
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(22.dp)
                    )

                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(110.dp)
                            .clip(CircleShape)
                            .background(Slate800)
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .fillMaxHeight(swipeHudLevel.coerceIn(0f, 1f))
                                .clip(CircleShape)
                                .background(AccentCyan)
                        )
                    }

                    Text(
                        text = "${(swipeHudLevel * 100).toInt()}%",
                        color = PureWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Kinetic Double-Tap Ripple Pill Overlay
        AnimatedVisibility(
            visible = gestureFeedback != PlayerGestureFeedback.NONE,
            enter = fadeIn(tween(140)) + scaleIn(initialScale = 0.85f, animationSpec = tween(180, easing = FastOutSlowInEasing)),
            exit = fadeOut(tween(240)) + scaleOut(targetScale = 1.08f, animationSpec = tween(240)),
            modifier = Modifier.align(
                when (gestureFeedback) {
                    PlayerGestureFeedback.REWIND_VOD, PlayerGestureFeedback.PREV_CHANNEL -> Alignment.CenterStart
                    PlayerGestureFeedback.FORWARD_VOD, PlayerGestureFeedback.NEXT_CHANNEL -> Alignment.CenterEnd
                    PlayerGestureFeedback.NONE -> Alignment.Center
                }
            )
        ) {
            val pulseScale by animateFloatAsState(
                targetValue = if (gestureFeedback != PlayerGestureFeedback.NONE) 1.04f else 0.95f,
                animationSpec = tween(160),
                label = "gesturePulse"
            )

            Surface(
                color = Color(0xCC040D1A),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.5.dp, AccentCyan.copy(alpha = 0.85f)),
                modifier = Modifier
                    .padding(horizontal = 36.dp)
                    .scale(pulseScale)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    val icon = when (gestureFeedback) {
                        PlayerGestureFeedback.REWIND_VOD -> Icons.Default.Replay10
                        PlayerGestureFeedback.FORWARD_VOD -> Icons.Default.Forward10
                        PlayerGestureFeedback.PREV_CHANNEL -> Icons.Default.SkipPrevious
                        PlayerGestureFeedback.NEXT_CHANNEL -> Icons.Default.SkipNext
                        PlayerGestureFeedback.NONE -> Icons.Default.PlayArrow
                    }
                    val label = when (gestureFeedback) {
                        PlayerGestureFeedback.REWIND_VOD -> "-${accumulatedSkipSeconds}s"
                        PlayerGestureFeedback.FORWARD_VOD -> "+${accumulatedSkipSeconds}s"
                        PlayerGestureFeedback.PREV_CHANNEL -> if (isAr) "القناة السابقة" else "Prev Channel"
                        PlayerGestureFeedback.NEXT_CHANNEL -> if (isAr) "القناة التالية" else "Next Channel"
                        PlayerGestureFeedback.NONE -> ""
                    }

                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = label,
                        color = PureWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.4.sp
                    )
                }
            }
        }

        // Binge-Mode Auto-Play Next Episode Card (10-second countdown)
        AnimatedVisibility(
            visible = isNearEpisodeEnd && nextEpisodeItem != null,
            enter = fadeIn(tween(220)) + slideInHorizontally(initialOffsetX = { it }),
            exit = fadeOut(tween(180)) + slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = if (uiState.isControlsVisible) 96.dp else 24.dp)
        ) {
            val nextEp = nextEpisodeItem ?: return@AnimatedVisibility
            Surface(
                color = Color(0xE6071120),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, AccentCyan),
                modifier = Modifier.width(290.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isAr) "الحلقة التالية خلال ${nextEpCountdownSec}ث" else "Next episode in ${nextEpCountdownSec}s",
                            color = AccentCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                        IconButton(
                            onClick = { nextEpDismissedForId = true },
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Slate400, modifier = Modifier.size(16.dp))
                        }
                    }

                    Text(
                        text = nextEp.name,
                        color = PureWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    LinearProgressIndicator(
                        progress = { (10 - nextEpCountdownSec).coerceIn(0, 10) / 10f },
                        color = AccentCyan,
                        trackColor = Slate800,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { onPlayNext() },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = Slate950),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isAr) "تشغيل الآن" else "Play Now",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = { nextEpDismissedForId = true },
                            border = BorderStroke(1.dp, Slate700),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isAr) "إلغاء" else "Cancel",
                                color = Slate300,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Buffering Spinner
        if (uiState.isBuffering) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = AccentCyan,
                        strokeWidth = 3.5.dp,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isAr) "جاري التحميل..." else "Loading stream...",
                        color = PureWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Error Overlay
        if (uiState.errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(24.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Slate900)
                        .border(1.dp, Slate700, RoundedCornerShape(16.dp))
                        .padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = AccentRed,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = uiState.errorMessage,
                        color = PureWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { playerManager.retryPlayback() },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = Slate950)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isAr) "إعادة المحاولة" else "Retry", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onBack,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite)
                        ) {
                            Text(if (isAr) "رجوع" else "Back")
                        }
                    }
                }
            }
        }

        // Top & Bottom Player Controls Overlay
        AnimatedVisibility(
            visible = uiState.isControlsVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xDD000000), Color.Transparent)
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlayerCircleButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isLive) {
                                Surface(
                                    color = AccentRed,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (isAr) "مباشر" else "LIVE",
                                        color = PureWhite,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.6.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = currentItem?.name ?: "",
                                color = PureWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = currentItem?.group ?: "",
                            color = AccentCyan,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    PlayerCircleButton(
                        icon = Icons.Default.Audiotrack,
                        contentDescription = "Audio Tracks",
                        onClick = { showAudioDialog = true }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    PlayerCircleButton(
                        icon = Icons.Default.Subtitles,
                        contentDescription = "Subtitles",
                        onClick = { showSubtitleDialog = true }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    PlayerCircleButton(
                        icon = Icons.Default.AspectRatio,
                        contentDescription = "Aspect Ratio",
                        onClick = { playerManager.cycleAspectRatio() }
                    )

                    if (hasDrawerItems) {
                        Spacer(modifier = Modifier.width(8.dp))
                        PlayerCircleButton(
                            icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                            contentDescription = "Channel List",
                            onClick = { playerManager.toggleQuickChannelList() }
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xEE000000))
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    if (!isLive && uiState.durationMs > 0L) {
                        val sliderInteraction = remember { MutableInteractionSource() }
                        val isSliderFocused by sliderInteraction.collectIsFocusedAsState()

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = formatPlayerTime(uiState.currentPositionMs),
                                color = PureWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Slider(
                                value = uiState.currentPositionMs.toFloat(),
                                onValueChange = { playerManager.seekTo(it.toLong()) },
                                valueRange = 0f..uiState.durationMs.toFloat().coerceAtLeast(1f),
                                interactionSource = sliderInteraction,
                                colors = SliderDefaults.colors(
                                    thumbColor = if (isSliderFocused) PureWhite else AccentCyan,
                                    activeTrackColor = AccentCyan,
                                    inactiveTrackColor = Slate700
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 10.dp)
                                    .border(
                                        width = if (isSliderFocused) 2.dp else 0.dp,
                                        color = if (isSliderFocused) PureWhite else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .focusable(interactionSource = sliderInteraction)
                                    .onPreviewKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyDown) {
                                            playerManager.showControlsTemporarily()
                                            when (event.key) {
                                                Key.DirectionLeft -> {
                                                    playerManager.seekRelative(-15_000L)
                                                    true
                                                }
                                                Key.DirectionRight -> {
                                                    playerManager.seekRelative(15_000L)
                                                    true
                                                }
                                                else -> false
                                            }
                                        } else false
                                    }
                            )
                            Text(
                                text = formatPlayerTime(uiState.durationMs),
                                color = Slate300,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlayerCircleButton(
                            icon = if (isLive) Icons.Default.SkipPrevious else Icons.Default.Replay10,
                            contentDescription = "Previous / Rewind 10s",
                            onClick = {
                                if (isLive) {
                                    gestureFeedback = PlayerGestureFeedback.PREV_CHANNEL
                                    feedbackTriggerKey++
                                    onPlayPrevious()
                                } else {
                                    accumulatedSkipSeconds = if (gestureFeedback == PlayerGestureFeedback.REWIND_VOD) accumulatedSkipSeconds + 10 else 10
                                    gestureFeedback = PlayerGestureFeedback.REWIND_VOD
                                    feedbackTriggerKey++
                                    playerManager.seekRelative(-10_000L)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.width(20.dp))

                        PlayerCircleButton(
                            icon = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            isPrimary = true,
                            focusRequester = playPauseFocus,
                            onClick = { playerManager.togglePlayPause() }
                        )

                        Spacer(modifier = Modifier.width(20.dp))

                        PlayerCircleButton(
                            icon = if (isLive) Icons.Default.SkipNext else Icons.Default.Forward10,
                            contentDescription = "Next / Forward 10s",
                            onClick = {
                                if (isLive) {
                                    gestureFeedback = PlayerGestureFeedback.NEXT_CHANNEL
                                    feedbackTriggerKey++
                                    onPlayNext()
                                } else {
                                    accumulatedSkipSeconds = if (gestureFeedback == PlayerGestureFeedback.FORWARD_VOD) accumulatedSkipSeconds + 10 else 10
                                    gestureFeedback = PlayerGestureFeedback.FORWARD_VOD
                                    feedbackTriggerKey++
                                    playerManager.seekRelative(10_000L)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Quick Side Drawer
        AnimatedVisibility(
            visible = uiState.isQuickChannelListOpen && hasDrawerItems,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Surface(
                color = Slate950.copy(alpha = 0.96f),
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
                    .border(BorderStroke(1.dp, Slate800))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (currentItem?.type == IptvType.SERIES) {
                                if (isAr) "حلقات المسلسل" else "Series Episodes"
                            } else {
                                if (isAr) "قائمة القنوات" else "Channels"
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                        IconButton(onClick = { playerManager.closeQuickChannelList() }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate400)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(allChannels, key = { it.id }) { ch ->
                            val isCurrent = ch.id == currentItem?.id
                            val itemInteraction = remember { MutableInteractionSource() }
                            val isItemFocused by itemInteraction.collectIsFocusedAsState()

                            Surface(
                                color = when {
                                    isItemFocused -> Slate800
                                    isCurrent -> AccentCyan.copy(alpha = 0.2f)
                                    else -> Slate900
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    width = if (isItemFocused) 2.dp else 1.dp,
                                    color = if (isItemFocused) PureWhite else if (isCurrent) AccentCyan else Slate800
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusable(interactionSource = itemInteraction)
                                    .onPreviewKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyUp &&
                                            (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                        ) {
                                            onSelectChannel(ch)
                                            true
                                        } else false
                                    }
                                    .clickable { onSelectChannel(ch) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (!ch.logoUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = ch.logoUrl,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ch.name,
                                            color = if (isCurrent) AccentCyan else PureWhite,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = ch.group,
                                            color = Slate400,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Scrollable Audio Tracks Dialog
        if (showAudioDialog) {
            AlertDialog(
                onDismissRequest = { showAudioDialog = false },
                containerColor = Slate900,
                title = {
                    Text(
                        text = if (isAr) "المسارات الصوتية" else "Audio Tracks",
                        color = PureWhite,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    if (uiState.audioTracks.isEmpty()) {
                        Text(
                            text = if (isAr) "لا توجد مسارات صوتية إضافية" else "No alternate audio tracks found",
                            color = Slate300
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 260.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            uiState.audioTracks.forEach { track ->
                                Surface(
                                    color = if (track.isSelected) AccentCyan.copy(alpha = 0.2f) else Slate850,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, if (track.isSelected) AccentCyan else Slate700),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            playerManager.selectAudioTrack(track)
                                            showAudioDialog = false
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(track.label, color = PureWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        if (track.isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAudioDialog = false }) {
                        Text(if (isAr) "إغلاق" else "Close", color = AccentCyan)
                    }
                }
            )
        }

        // Scrollable Subtitles & Timing Sync Dialog
        if (showSubtitleDialog) {
            AlertDialog(
                onDismissRequest = { showSubtitleDialog = false },
                containerColor = Slate900,
                title = {
                    Text(
                        text = if (isAr) "الترجمة ومزامنة التوقيت" else "Subtitles & Timing Sync",
                        color = PureWhite,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = if (isAr) {
                                "مزامنة الترجمة: ${String.format(Locale.US, "%+.1f", subtitleOffsetMs / 1000f)} ثانية"
                            } else {
                                "Subtitle Delay: ${String.format(Locale.US, "%+.1f", subtitleOffsetMs / 1000f)}s"
                            },
                            color = AccentCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { playerManager.adjustSubtitleOffset(-500L) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("-0.5s", color = PureWhite, fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { playerManager.resetSubtitleOffset() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("0.0s", color = AccentCyan, fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { playerManager.adjustSubtitleOffset(500L) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("+0.5s", color = PureWhite, fontSize = 12.sp)
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    showSubtitleDialog = false
                                    subtitlePickerLauncher.launch("*/*")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = Slate950),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (isAr) "ملف SRT/VTT" else "Load File", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { showSubtitleUrlInput = !showSubtitleUrlInput },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (isAr) "رابط ترجمة" else "Load URL", color = PureWhite, fontSize = 12.sp)
                            }
                        }

                        if (showSubtitleUrlInput) {
                            OutlinedTextField(
                                value = subtitleUrlText,
                                onValueChange = { subtitleUrlText = it },
                                placeholder = { Text("https://.../sub.srt", color = Slate400, fontSize = 12.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = PureWhite,
                                    unfocusedTextColor = PureWhite,
                                    focusedBorderColor = AccentCyan,
                                    unfocusedBorderColor = Slate700
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    if (subtitleUrlText.isNotBlank()) {
                                        playerManager.loadExternalSubtitleUrl(subtitleUrlText)
                                        showSubtitleUrlInput = false
                                        showSubtitleDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = Slate950),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (isAr) "تفعيل الرابط" else "Apply Subtitle URL", fontWeight = FontWeight.Bold)
                            }
                        }

                        Surface(
                            color = if (uiState.isSubtitleOff) AccentCyan.copy(alpha = 0.2f) else Slate850,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (uiState.isSubtitleOff) AccentCyan else Slate700),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    playerManager.disableSubtitles()
                                    showSubtitleDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(if (isAr) "إيقاف الترجمة" else "Off", color = PureWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                if (uiState.isSubtitleOff) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        uiState.subtitleTracks.forEach { track ->
                            Surface(
                                color = if (track.isSelected && !uiState.isSubtitleOff) AccentCyan.copy(alpha = 0.2f) else Slate850,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (track.isSelected && !uiState.isSubtitleOff) AccentCyan else Slate700),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        playerManager.selectSubtitleTrack(track)
                                        showSubtitleDialog = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(track.label, color = PureWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    if (track.isSelected && !uiState.isSubtitleOff) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSubtitleDialog = false }) {
                        Text(if (isAr) "إغلاق" else "Close", color = AccentCyan)
                    }
                }
            )
        }
    }
}

@Composable
private fun PlayerCircleButton(
    icon: ImageVector,
    contentDescription: String,
    isPrimary: Boolean = false,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val btnSize = if (isPrimary) 56.dp else 42.dp
    val iconSize = if (isPrimary) 30.dp else 22.dp

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .size(btnSize)
            .clip(CircleShape)
            .background(
                when {
                    isFocused -> PureWhite
                    isPrimary -> AccentCyan
                    else -> Color(0x990F172A)
                }
            )
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = if (isFocused) AccentCyan else Slate700,
                shape = CircleShape
            )
            .focusable(interactionSource = interactionSource)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                ) {
                    onClick()
                    true
                } else false
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isFocused || isPrimary) Slate950 else PureWhite,
            modifier = Modifier.size(iconSize)
        )
    }
}

private fun formatPlayerTime(millis: Long): String {
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
