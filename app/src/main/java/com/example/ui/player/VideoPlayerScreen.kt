package com.example.ui.player

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.ui.locale.LocalAppStrings
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
import kotlin.math.roundToInt
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
    val strings = LocalAppStrings.current
    val isAr = LocalAppLanguage.current == "ar"
    val context = LocalContext.current
    val activity = context as? Activity
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    val currentItem = uiState.currentItem
    val isMovie = currentItem?.type == IptvType.MOVIE
    val isSeries = currentItem?.type == IptvType.SERIES
    val isLiveTv = currentItem?.type == IptvType.LIVE_TV
    val hasDrawerList = !isMovie && allChannels.isNotEmpty()

    // Ensure drawer is closed if playing a Movie
    LaunchedEffect(isMovie) {
        if (isMovie && uiState.isQuickChannelListOpen) {
            playerManager.closeQuickChannelList()
        }
    }

    // Invisible Gesture States (Left = Volume, Right = Brightness)
    var isGestureActive by remember { mutableStateOf(false) }
    var gestureType by remember { mutableStateOf("") }
    var gesturePercent by remember { mutableIntStateOf(0) }
    var gestureHideTrigger by remember { mutableIntStateOf(0) }

    var volumeFraction by remember {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val cur = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        mutableFloatStateOf(cur.toFloat() / max.toFloat())
    }

    var brightnessFraction by remember {
        val winAttr = activity?.window?.attributes?.screenBrightness ?: -1f
        mutableFloatStateOf(if (winAttr in 0.01f..1.0f) winAttr else 0.5f)
    }

    LaunchedEffect(gestureHideTrigger) {
        if (gestureHideTrigger > 0) {
            delay(850)
            isGestureActive = false
        }
    }

    BackHandler {
        if (uiState.isQuickChannelListOpen) {
            playerManager.closeQuickChannelList()
        } else if (uiState.isControlsVisible) {
            playerManager.toggleControlsVisibility()
        } else {
            onBack()
        }
    }

    val playPauseFocusRequester = remember { FocusRequester() }
    val prevFocusRequester = remember { FocusRequester() }
    val nextFocusRequester = remember { FocusRequester() }
    val audioFocusRequester = remember { FocusRequester() }
    val subFocusRequester = remember { FocusRequester() }
    val aspectFocusRequester = remember { FocusRequester() }
    val backFocusRequester = remember { FocusRequester() }

    var lastFocusedControl by remember { mutableStateOf("play") }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }

    var wasControlsVisible by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.isControlsVisible) {
        if (uiState.isControlsVisible && !wasControlsVisible) {
            try {
                when (lastFocusedControl) {
                    "next" -> nextFocusRequester.requestFocus()
                    "prev" -> prevFocusRequester.requestFocus()
                    else -> playPauseFocusRequester.requestFocus()
                }
            } catch (_: Exception) {}
        }
        wasControlsVisible = uiState.isControlsVisible
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (uiState.isControlsVisible && event.type == KeyEventType.KeyDown) {
                    playerManager.scheduleControlsAutoHide()
                }

                if (!uiState.isControlsVisible && !uiState.isQuickChannelListOpen && !showAudioDialog && !showSubtitleDialog) {
                    if (event.type == KeyEventType.KeyUp) {
                        when (event.key) {
                            Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                lastFocusedControl = "play"
                                playerManager.showControlsTemporarily()
                                true
                            }
                            Key.DirectionUp, Key.ChannelUp -> {
                                if (!isMovie) onPlayPrevious()
                                true
                            }
                            Key.DirectionDown, Key.ChannelDown -> {
                                if (!isMovie) onPlayNext()
                                true
                            }
                            Key.Back, Key.Escape -> {
                                onBack()
                                true
                            }
                            else -> false
                        }
                    } else false
                } else {
                    false
                }
            }
            .pointerInput(Unit) {
                var isLeftZone = true
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        isLeftZone = offset.x < (size.width / 2f)
                        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                        val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                        volumeFraction = curVol.toFloat() / maxVol.toFloat()

                        val curBright = activity?.window?.attributes?.screenBrightness ?: -1f
                        if (curBright in 0.01f..1.0f) {
                            brightnessFraction = curBright
                        }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        val heightPx = size.height.coerceAtLeast(1).toFloat()
                        val delta = -dragAmount / (heightPx * 0.75f)

                        if (isLeftZone) {
                            volumeFraction = (volumeFraction + delta).coerceIn(0f, 1f)
                            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                            val targetVol = (volumeFraction * maxVol).roundToInt().coerceIn(0, maxVol)
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                            gestureType = "volume"
                            gesturePercent = (volumeFraction * 100).roundToInt()
                            isGestureActive = true
                        } else {
                            brightnessFraction = (brightnessFraction + delta).coerceIn(0.01f, 1f)
                            activity?.window?.let { win ->
                                val params = win.attributes
                                params.screenBrightness = brightnessFraction
                                win.attributes = params
                            }
                            gestureType = "brightness"
                            gesturePercent = (brightnessFraction * 100).roundToInt()
                            isGestureActive = true
                        }
                    },
                    onDragEnd = { gestureHideTrigger++ },
                    onDragCancel = { gestureHideTrigger++ }
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                playerManager.toggleControlsVisibility()
            }
            .testTag("video_player_screen")
    ) {
        val playerModifier = when (uiState.aspectRatioMode) {
            AspectRatioMode.SIXTEEN_BY_NINE -> Modifier.fillMaxWidth().aspectRatio(16f / 9f).align(Alignment.Center)
            AspectRatioMode.FOUR_BY_THREE -> Modifier.fillMaxWidth().aspectRatio(4f / 3f).align(Alignment.Center)
            else -> Modifier.fillMaxSize()
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = playerModifier,
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = playerManager.player
                        useController = false
                        isFocusable = false
                        isFocusableInTouchMode = false
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        resizeMode = when (uiState.aspectRatioMode) {
                            AspectRatioMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                            AspectRatioMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    }
                },
                update = { playerView ->
                    playerView.player = playerManager.player
                    playerView.resizeMode = when (uiState.aspectRatioMode) {
                        AspectRatioMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                        AspectRatioMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                }
            )
        }

        // Temporary Gesture HUD Overlay
        AnimatedVisibility(
            visible = isGestureActive,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                color = Color(0xD90F172A),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, AccentCyan)
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 18.dp)
                        .width(150.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (gestureType == "volume") Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.BrightnessHigh,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (gestureType == "volume") {
                            if (isAr) "الصوت $gesturePercent%" else "Volume $gesturePercent%"
                        } else {
                            if (isAr) "السطوع $gesturePercent%" else "Brightness $gesturePercent%"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { (gesturePercent / 100f).coerceIn(0f, 1f) },
                        color = AccentCyan,
                        trackColor = Slate700,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
            }
        }

        // Buffering Indicator Overlay
        if (uiState.isBuffering) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 84.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xD90F172A))
                    .border(1.dp, Slate700, RoundedCornerShape(14.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        color = AccentCyan,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isAr) "جاري تحميل البث... ${currentItem?.name.orEmpty()}" else "Buffering... ${currentItem?.name.orEmpty()}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PureWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Playback Error Overlay
        if (uiState.errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x99000000))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = Slate900,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, AccentRed),
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = AccentRed,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isAr) "البث غير متاح حالياً" else "Stream Unavailable",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                        Text(
                            text = uiState.errorMessage,
                            fontSize = 13.sp,
                            color = Slate300,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Surface(
                                color = AccentCyan,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { playerManager.retryPlayback() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Refresh, null, tint = Slate950, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isAr) "إعادة المحاولة" else "Retry Stream",
                                        color = Slate950,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            Surface(
                                color = Slate800,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onBack() }
                            ) {
                                Text(
                                    text = if (isAr) "رجوع" else "Return",
                                    color = PureWhite,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Fullscreen Playback HUD
        AnimatedVisibility(
            visible = uiState.isControlsVisible && uiState.errorMessage == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xD9000000),
                                Color.Transparent,
                                Color(0xEA000000)
                            )
                        )
                    )
            ) {
                // Top Control Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val backBtnInteraction = remember { MutableInteractionSource() }
                    val isBackBtnFocused by backBtnInteraction.collectIsFocusedAsState()

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onBack,
                            interactionSource = backBtnInteraction,
                            modifier = Modifier
                                .size(48.dp)
                                .focusRequester(backFocusRequester)
                                .focusProperties { down = playPauseFocusRequester }
                                .clip(CircleShape)
                                .background(if (isBackBtnFocused) AccentCyan else Color(0x66000000))
                                .border(
                                    width = if (isBackBtnFocused) 3.dp else 1.dp,
                                    color = if (isBackBtnFocused) PureWhite else Slate700,
                                    shape = CircleShape
                                )
                                .focusable(interactionSource = backBtnInteraction)
                                .onPreviewKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyUp &&
                                        (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                    ) {
                                        onBack()
                                        true
                                    } else false
                                }
                                .testTag("btn_player_back")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = if (isBackBtnFocused) Slate950 else PureWhite
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = currentItem?.name ?: "IPTV Stream",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 3.dp)
                            ) {
                                if (isLiveTv) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0x44EF4444))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(AccentRed)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isAr) "مباشر" else "LIVE",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = PureWhite
                                        )
                                    }
                                }

                                Text(
                                    text = currentItem?.group ?: "",
                                    fontSize = 11.sp,
                                    color = Slate300
                                )
                            }
                        }
                    }

                    // Top Action Buttons (Fit, Audio, Subtitles, and Drawer ONLY for Live TV & Series)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val aspectInteraction = remember { MutableInteractionSource() }
                        val isAspectFocused by aspectInteraction.collectIsFocusedAsState()

                        Surface(
                            color = if (isAspectFocused) AccentCyan else Color(0x66000000),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                width = if (isAspectFocused) 3.dp else 1.dp,
                                color = if (isAspectFocused) PureWhite else Slate700
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .focusRequester(aspectFocusRequester)
                                .focusProperties { down = playPauseFocusRequester }
                                .focusable(interactionSource = aspectInteraction)
                                .onPreviewKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyUp &&
                                        (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                    ) {
                                        playerManager.cycleAspectRatio()
                                        true
                                    } else false
                                }
                                .clickable(interactionSource = aspectInteraction, indication = null) {
                                    playerManager.cycleAspectRatio()
                                }
                                .testTag("btn_aspect_ratio")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AspectRatio,
                                    contentDescription = "Aspect Ratio",
                                    tint = if (isAspectFocused) Slate950 else AccentCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = uiState.aspectRatioMode.label.substringBefore(" "),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAspectFocused) Slate950 else PureWhite
                                )
                            }
                        }

                        // Audio Track Selector Button
                        val audioInteraction = remember { MutableInteractionSource() }
                        val isAudioFocused by audioInteraction.collectIsFocusedAsState()

                        Surface(
                            color = if (isAudioFocused) AccentCyan else Color(0x66000000),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                width = if (isAudioFocused) 3.dp else 1.dp,
                                color = if (isAudioFocused) PureWhite else Slate700
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .focusRequester(audioFocusRequester)
                                .focusProperties { down = playPauseFocusRequester }
                                .focusable(interactionSource = audioInteraction)
                                .onPreviewKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyUp &&
                                        (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                    ) {
                                        playerManager.updateTracksState()
                                        showAudioDialog = true
                                        true
                                    } else false
                                }
                                .clickable(interactionSource = audioInteraction, indication = null) {
                                    playerManager.updateTracksState()
                                    showAudioDialog = true
                                }
                                .testTag("btn_player_audio")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Audiotrack,
                                    contentDescription = strings.audio,
                                    tint = if (isAudioFocused) Slate950 else AccentCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isAr) "الصوت" else strings.audio,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAudioFocused) Slate950 else PureWhite
                                )
                            }
                        }

                        // Subtitle (CC) Selector Button
                        val subInteraction = remember { MutableInteractionSource() }
                        val isSubFocused by subInteraction.collectIsFocusedAsState()

                        Surface(
                            color = if (isSubFocused) AccentCyan else Color(0x66000000),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                width = if (isSubFocused) 3.dp else 1.dp,
                                color = if (isSubFocused) PureWhite else Slate700
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .focusRequester(subFocusRequester)
                                .focusProperties { down = playPauseFocusRequester }
                                .focusable(interactionSource = subInteraction)
                                .onPreviewKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyUp &&
                                        (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                    ) {
                                        playerManager.updateTracksState()
                                        showSubtitleDialog = true
                                        true
                                    } else false
                                }
                                .clickable(interactionSource = subInteraction, indication = null) {
                                    playerManager.updateTracksState()
                                    showSubtitleDialog = true
                                }
                                .testTag("btn_player_subtitles")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ClosedCaption,
                                    contentDescription = strings.subtitles,
                                    tint = if (isSubFocused) Slate950 else AccentCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isAr) "الترجمة" else strings.subtitles,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSubFocused) Slate950 else PureWhite
                                )
                            }
                        }

                        // Show Channel List / Episodes Drawer Button ONLY for Live TV and Series (Hidden in Movies!)
                        if (hasDrawerList) {
                            val drawerBtnInteraction = remember { MutableInteractionSource() }
                            val isDrawerFocused by drawerBtnInteraction.collectIsFocusedAsState()

                            Surface(
                                color = if (isDrawerFocused) AccentCyan else Color(0x66000000),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    width = if (isDrawerFocused) 3.dp else 1.dp,
                                    color = if (isDrawerFocused) PureWhite else Slate700
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .focusProperties { down = playPauseFocusRequester }
                                    .focusable(interactionSource = drawerBtnInteraction)
                                    .onPreviewKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyUp &&
                                            (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                        ) {
                                            playerManager.toggleQuickChannelList()
                                            true
                                        } else false
                                    }
                                    .clickable(interactionSource = drawerBtnInteraction, indication = null) {
                                        playerManager.toggleQuickChannelList()
                                    }
                                    .testTag("btn_quick_channel_list")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isSeries) Icons.Default.VideoLibrary else Icons.Default.FormatListBulleted,
                                        contentDescription = null,
                                        tint = if (isDrawerFocused) Slate950 else AccentCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isSeries) {
                                            if (isAr) "الحلقات" else "Episodes"
                                        } else {
                                            if (isAr) "القنوات" else "Channels"
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDrawerFocused) Slate950 else PureWhite
                                    )
                                }
                            }
                        }
                    }
                }

                // Center Play / Next / Previous Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    val prevInteraction = remember { MutableInteractionSource() }
                    val isPrevFocused by prevInteraction.collectIsFocusedAsState()

                    IconButton(
                        onClick = {
                            lastFocusedControl = "prev"
                            onPlayPrevious()
                            playerManager.scheduleControlsAutoHide()
                        },
                        interactionSource = prevInteraction,
                        modifier = Modifier
                            .size(56.dp)
                            .focusRequester(prevFocusRequester)
                            .focusProperties { up = audioFocusRequester }
                            .clip(CircleShape)
                            .background(if (isPrevFocused) AccentCyan else Color(0x66000000))
                            .border(
                                width = if (isPrevFocused) 3.dp else 1.dp,
                                color = if (isPrevFocused) PureWhite else Slate700,
                                shape = CircleShape
                            )
                            .focusable(interactionSource = prevInteraction)
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyUp &&
                                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                ) {
                                    lastFocusedControl = "prev"
                                    onPlayPrevious()
                                    playerManager.scheduleControlsAutoHide()
                                    true
                                } else false
                            }
                            .testTag("btn_player_prev")
                    ) {
                        Icon(
                            imageVector = if (isMovie) Icons.Default.FastRewind else Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = if (isPrevFocused) Slate950 else PureWhite,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    val playInteraction = remember { MutableInteractionSource() }
                    val isPlayFocused by playInteraction.collectIsFocusedAsState()

                    IconButton(
                        onClick = {
                            lastFocusedControl = "play"
                            playerManager.togglePlayPause()
                        },
                        interactionSource = playInteraction,
                        modifier = Modifier
                            .size(76.dp)
                            .focusRequester(playPauseFocusRequester)
                            .focusProperties { up = audioFocusRequester }
                            .clip(CircleShape)
                            .background(if (isPlayFocused) PureWhite else AccentCyan)
                            .border(
                                width = if (isPlayFocused) 4.dp else 0.dp,
                                color = PureWhite,
                                shape = CircleShape
                            )
                            .focusable(interactionSource = playInteraction)
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyUp &&
                                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                ) {
                                    lastFocusedControl = "play"
                                    playerManager.togglePlayPause()
                                    true
                                } else false
                            }
                            .testTag("btn_player_play_pause")
                    ) {
                        Icon(
                            imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                            tint = Slate950,
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    val nextInteraction = remember { MutableInteractionSource() }
                    val isNextFocused by nextInteraction.collectIsFocusedAsState()

                    IconButton(
                        onClick = {
                            lastFocusedControl = "next"
                            onPlayNext()
                            playerManager.scheduleControlsAutoHide()
                        },
                        interactionSource = nextInteraction,
                        modifier = Modifier
                            .size(56.dp)
                            .focusRequester(nextFocusRequester)
                            .focusProperties { up = subFocusRequester }
                            .clip(CircleShape)
                            .background(if (isNextFocused) AccentCyan else Color(0x66000000))
                            .border(
                                width = if (isNextFocused) 3.dp else 1.dp,
                                color = if (isNextFocused) PureWhite else Slate700,
                                shape = CircleShape
                            )
                            .focusable(interactionSource = nextInteraction)
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyUp &&
                                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                ) {
                                    lastFocusedControl = "next"
                                    onPlayNext()
                                    playerManager.scheduleControlsAutoHide()
                                    true
                                } else false
                            }
                            .testTag("btn_player_next")
                    ) {
                        Icon(
                            imageVector = if (isMovie) Icons.Default.FastForward else Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = if (isNextFocused) Slate950 else PureWhite,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Bottom HUD: Seekbar for Movies/Series or Info for Live TV
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    if (!isLiveTv && uiState.durationMs > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatTime(uiState.currentPositionMs),
                                fontSize = 13.sp,
                                color = PureWhite,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = formatTime(uiState.durationMs),
                                fontSize = 13.sp,
                                color = Slate300,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Slider(
                            value = uiState.currentPositionMs.toFloat(),
                            onValueChange = { pos -> playerManager.seekTo(pos.toLong()) },
                            valueRange = 0f..uiState.durationMs.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = AccentCyan,
                                activeTrackColor = AccentCyan,
                                inactiveTrackColor = Slate700
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentItem?.currentProgram ?: if (isAr) "بث مباشر" else "Live Broadcast Stream",
                                fontSize = 14.sp,
                                color = PureWhite,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = if (isAr) "سهم للأعلى: الصوت والترجمة • يمين/يسار: تغيير القناة" else "Up: Audio/Subtitles • Left/Right: Prev/Next",
                                fontSize = 11.sp,
                                color = Slate400
                            )
                        }
                    }
                }
            }
        }

        // Side Drawer: Shows "Episodes" for TV Series and "Channel List" for Live TV (Disabled in Movies!)
        AnimatedVisibility(
            visible = uiState.isQuickChannelListOpen && hasDrawerList,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(340.dp)
        ) {
            Surface(
                color = Slate900,
                border = BorderStroke(1.dp, Slate800),
                modifier = Modifier.fillMaxSize()
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isSeries) {
                                if (isAr) "حلقات المسلسل" else "Series Episodes"
                            } else {
                                if (isAr) "قائمة القنوات" else "Channel List"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = PureWhite
                        )
                        IconButton(
                            onClick = { playerManager.closeQuickChannelList() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Close, "Close", tint = PureWhite)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allChannels, key = { it.id }) { item ->
                            val isCurrent = item.id == currentItem?.id || item.streamUrl == currentItem?.streamUrl
                            val itemInteraction = remember { MutableInteractionSource() }
                            val isItemFocused by itemInteraction.collectIsFocusedAsState()

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            isItemFocused -> AccentCyan
                                            isCurrent -> Slate800
                                            else -> Slate850
                                        }
                                    )
                                    .border(
                                        width = if (isItemFocused) 2.dp else 1.dp,
                                        color = if (isItemFocused) PureWhite else if (isCurrent) AccentCyan else Slate700,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .focusable(interactionSource = itemInteraction)
                                    .onPreviewKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyUp &&
                                            (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                        ) {
                                            onSelectChannel(item)
                                            playerManager.closeQuickChannelList()
                                            true
                                        } else false
                                    }
                                    .clickable(interactionSource = itemInteraction, indication = null) {
                                        onSelectChannel(item)
                                        playerManager.closeQuickChannelList()
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!item.logoUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = item.logoUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        fontSize = 13.sp,
                                        fontWeight = if (isCurrent || isItemFocused) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isItemFocused) Slate950 else if (isCurrent) AccentCyan else PureWhite,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.group,
                                        fontSize = 11.sp,
                                        color = if (isItemFocused) Slate800 else Slate400,
                                        maxLines = 1
                                    )
                                }
                                if (isCurrent) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isItemFocused) Slate950 else AccentCyan)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAudioDialog) {
        AlertDialog(
            onDismissRequest = { showAudioDialog = false },
            containerColor = Slate900,
            title = {
                Text(
                    text = if (isAr) "اختر المسار الصوتي" else strings.selectAudioTrack,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite
                )
            },
            text = {
                if (uiState.audioTracks.isEmpty()) {
                    Text(
                        text = if (isAr) "لا توجد مسارات صوتية إضافية متاحة." else "No alternate audio tracks available.",
                        color = Slate400,
                        fontSize = 14.sp
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.audioTracks) { track ->
                            val itemInteraction = remember { MutableInteractionSource() }
                            val isFocused by itemInteraction.collectIsFocusedAsState()

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isFocused) Slate800 else if (track.isSelected) Slate850 else Color.Transparent)
                                    .border(
                                        width = if (isFocused) 2.dp else 1.dp,
                                        color = if (isFocused) AccentCyan else if (track.isSelected) Slate700 else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .focusable(interactionSource = itemInteraction)
                                    .onPreviewKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyUp &&
                                            (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                        ) {
                                            playerManager.selectAudioTrack(track)
                                            showAudioDialog = false
                                            true
                                        } else false
                                    }
                                    .clickable(interactionSource = itemInteraction, indication = null) {
                                        playerManager.selectAudioTrack(track)
                                        showAudioDialog = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = track.isSelected,
                                    onClick = {
                                        playerManager.selectAudioTrack(track)
                                        showAudioDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = AccentCyan,
                                        unselectedColor = Slate400
                                    )
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = track.label,
                                    color = if (track.isSelected) AccentCyan else PureWhite,
                                    fontSize = 14.sp,
                                    fontWeight = if (track.isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAudioDialog = false }) {
                    Text(if (isAr) "إغلاق" else strings.close, color = AccentCyan, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showSubtitleDialog) {
        AlertDialog(
            onDismissRequest = { showSubtitleDialog = false },
            containerColor = Slate900,
            title = {
                Text(
                    text = if (isAr) "اختر لغة الترجمة" else strings.selectSubtitleTrack,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        val offInteraction = remember { MutableInteractionSource() }
                        val isOffFocused by offInteraction.collectIsFocusedAsState()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isOffFocused) Slate800 else if (uiState.isSubtitleOff) Slate850 else Color.Transparent)
                                .border(
                                    width = if (isOffFocused) 2.dp else 1.dp,
                                    color = if (isOffFocused) AccentCyan else if (uiState.isSubtitleOff) Slate700 else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .focusable(interactionSource = offInteraction)
                                .onPreviewKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyUp &&
                                        (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                    ) {
                                        playerManager.disableSubtitles()
                                        showSubtitleDialog = false
                                        true
                                    } else false
                                }
                                .clickable(interactionSource = offInteraction, indication = null) {
                                    playerManager.disableSubtitles()
                                    showSubtitleDialog = false
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.isSubtitleOff,
                                onClick = {
                                    playerManager.disableSubtitles()
                                    showSubtitleDialog = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = AccentCyan,
                                    unselectedColor = Slate400
                                )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isAr) "إيقاف الترجمة" else strings.off,
                                color = if (uiState.isSubtitleOff) AccentCyan else PureWhite,
                                fontSize = 14.sp,
                                fontWeight = if (uiState.isSubtitleOff) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    if (uiState.subtitleTracks.isEmpty()) {
                        item {
                            Text(
                                text = if (isAr) "لا توجد ترجمة مدمجة في هذا الفيديو." else "No embedded subtitle streams detected.",
                                color = Slate400,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(start = 12.dp, top = 6.dp)
                            )
                        }
                    } else {
                        items(uiState.subtitleTracks) { track ->
                            val itemInteraction = remember { MutableInteractionSource() }
                            val isFocused by itemInteraction.collectIsFocusedAsState()

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isFocused) Slate800 else if (track.isSelected && !uiState.isSubtitleOff) Slate850 else Color.Transparent)
                                    .border(
                                        width = if (isFocused) 2.dp else 1.dp,
                                        color = if (isFocused) AccentCyan else if (track.isSelected && !uiState.isSubtitleOff) Slate700 else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .focusable(interactionSource = itemInteraction)
                                    .onPreviewKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyUp &&
                                            (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                        ) {
                                            playerManager.selectSubtitleTrack(track)
                                            showSubtitleDialog = false
                                            true
                                        } else false
                                    }
                                    .clickable(interactionSource = itemInteraction, indication = null) {
                                        playerManager.selectSubtitleTrack(track)
                                        showSubtitleDialog = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = track.isSelected && !uiState.isSubtitleOff,
                                    onClick = {
                                        playerManager.selectSubtitleTrack(track)
                                        showSubtitleDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = AccentCyan,
                                        unselectedColor = Slate400
                                    )
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = track.label,
                                    color = if (track.isSelected && !uiState.isSubtitleOff) AccentCyan else PureWhite,
                                    fontSize = 14.sp,
                                    fontWeight = if (track.isSelected && !uiState.isSubtitleOff) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSubtitleDialog = false }) {
                    Text(if (isAr) "إغلاق" else strings.close, color = AccentCyan, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val hours = minutes / 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes % 60, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}
