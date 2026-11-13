package com.example.ui.player

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
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
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.platform.LocalView
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
    val localView = LocalView.current
    val playerRootFocus = remember { FocusRequester() }
    val playPauseFocus = remember { FocusRequester() }

    DisposableEffect(localView) {
        val prevKeepScreenOn = localView.keepScreenOn
        localView.keepScreenOn = true
        onDispose {
            localView.keepScreenOn = prevKeepScreenOn
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

    val currentItem = uiState.currentItem
    val isLive = currentItem?.type == IptvType.LIVE_TV
    val hasDrawerItems = allChannels.isNotEmpty() && currentItem?.type != IptvType.MOVIE

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
                                    playerManager.seekTo((uiState.currentPositionMs - 10_000L).coerceAtLeast(0L))
                                    playerManager.showControlsTemporarily()
                                }
                                true
                            } else false
                        }
                        Key.DirectionRight -> {
                            if (!uiState.isControlsVisible && !uiState.isQuickChannelListOpen && !isLive) {
                                playerManager.seekTo((uiState.currentPositionMs + 10_000L).coerceAtMost(uiState.durationMs))
                                playerManager.showControlsTemporarily()
                                true
                            } else false
                        }
                        Key.ChannelUp, Key.PageUp -> {
                            onPlayNext()
                            true
                        }
                        Key.ChannelDown, Key.PageDown -> {
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
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                playerManager.toggleControlsVisibility()
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
                    AspectRatioMode.SIXTEEN_BY_NINE -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    AspectRatioMode.FOUR_BY_THREE -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            }
        )

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
                // Top Bar
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
                        Text(
                            text = currentItem?.name ?: "",
                            color = PureWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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

                // Bottom Bar Controls & D-Pad Focusable Seekbar
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
                                                    playerManager.seekTo((uiState.currentPositionMs - 15_000L).coerceAtLeast(0L))
                                                    true
                                                }
                                                Key.DirectionRight -> {
                                                    playerManager.seekTo((uiState.currentPositionMs + 15_000L).coerceAtMost(uiState.durationMs))
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
                                if (isLive) onPlayPrevious()
                                else playerManager.seekTo((uiState.currentPositionMs - 10_000L).coerceAtLeast(0L))
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
                                if (isLive) onPlayNext()
                                else playerManager.seekTo((uiState.currentPositionMs + 10_000L).coerceAtMost(uiState.durationMs))
                            }
                        )
                    }
                }
            }
        }

        // Quick Side Drawer for Live TV Channels or Series Episodes
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

        // Scrollable Audio Tracks Dialog (Works in Landscape & TV)
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

        // Scrollable Subtitles & Timing Sync Dialog (Works in Landscape & TV)
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
