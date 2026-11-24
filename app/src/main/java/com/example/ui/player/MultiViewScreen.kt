package com.example.ui.player

import android.app.Activity
import android.net.Uri
import android.view.TextureView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import coil.compose.AsyncImage
import com.example.model.IptvItem
import com.example.model.StreamFormat
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
import com.example.util.CategoryTranslator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun MultiViewScreen(
    allChannels: List<IptvItem>,
    initialChannel: IptvItem? = null,
    onPlaySingleFullscreen: (IptvItem) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val localView = LocalView.current
    val strings = LocalAppStrings.current
    val currentLanguage = LocalAppLanguage.current
    val isAr = currentLanguage == "ar"
    val scope = rememberCoroutineScope()

    var screenCount by remember { mutableIntStateOf(2) }
    var activeAudioSlot by remember { mutableIntStateOf(0) }
    var fillScreenMode by remember { mutableStateOf(true) }
    var showOverlayHud by remember { mutableStateOf(true) }
    var hudTriggerKey by remember { mutableIntStateOf(0) }
    var pickerSlotIndex by remember { mutableStateOf<Int?>(null) }

    val hudFocusRequester = remember { FocusRequester() }
    val slotFocusRequesters = remember { List(4) { FocusRequester() } }

    val firstChannel = remember(initialChannel, allChannels) {
        initialChannel ?: allChannels.firstOrNull()
    }
    val secondChannel = remember(firstChannel, allChannels) {
        allChannels.firstOrNull { ch ->
            ch.id != firstChannel?.id &&
            ch.streamUrl != firstChannel?.streamUrl &&
            !ch.name.equals(firstChannel?.name, ignoreCase = true)
        }
    }

    val slotChannels = remember {
        mutableStateListOf<IptvItem?>(
            firstChannel,
            secondChannel,
            null,
            null
        )
    }

    val slotBuffering = remember { mutableStateListOf(false, false, false, false) }
    val slotErrors = remember { mutableStateListOf<String?>(null, null, null, null) }
    val slotRetryCounts = remember { mutableStateListOf(0, 0, 0, 0) }
    val slotLoadTriggers = remember { mutableStateListOf(0, 0, 0, 0) }

    val slotHttpFactories = remember {
        val agents = listOf(
            "IPTVSmartersPlayer",
            "VLC/3.0.20 LibVLC/3.0.20",
            "Lavf/60.16.100",
            "ExoPlayer/2.19.1"
        )
        List(4) { idx ->
            DefaultHttpDataSource.Factory()
                .setUserAgent(agents[idx])
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15_000)
                .setReadTimeoutMs(25_000)
        }
    }

    val players = remember {
        List(4) { index ->
            // إنشاء RenderersFactory و LoadControl مستقل لكل مشغل لمنع تعارض Shared LoadControl في Media3
            val renderersFactory = DefaultRenderersFactory(context)
                .setEnableDecoderFallback(true)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

            val individualLoadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(8_000, 20_000, 600, 1_200)
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()

            ExoPlayer.Builder(context, renderersFactory)
                .setMediaSourceFactory(DefaultMediaSourceFactory(slotHttpFactories[index]))
                .setLoadControl(individualLoadControl)
                .build()
                .apply {
                    setWakeMode(C.WAKE_MODE_NETWORK)
                    volume = if (index == 0) 1f else 0f
                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            when (playbackState) {
                                Player.STATE_BUFFERING -> {
                                    slotBuffering[index] = true
                                    slotErrors[index] = null
                                }
                                Player.STATE_READY -> {
                                    slotBuffering[index] = false
                                    slotErrors[index] = null
                                    slotRetryCounts[index] = 0
                                }
                                Player.STATE_IDLE, Player.STATE_ENDED -> {
                                    slotBuffering[index] = false
                                }
                            }
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            val currentItem = slotChannels[index]
                            if (currentItem != null && slotRetryCounts[index] < 2) {
                                slotRetryCounts[index]++
                                slotBuffering[index] = true
                                slotErrors[index] = null
                                scope.launch(Dispatchers.Main) {
                                    delay(1200L * slotRetryCounts[index])
                                    if (slotChannels[index]?.id == currentItem.id) {
                                        slotLoadTriggers[index]++
                                    }
                                }
                                return
                            }

                            slotBuffering[index] = false
                            val causeDetail = error.cause?.localizedMessage?.take(45) ?: error.errorCodeName
                            slotErrors[index] = if (isAr) {
                                "فشل التشغيل ($causeDetail) - اضغط لإعادة المحاولة"
                            } else {
                                "Stream failed ($causeDetail) - Tap to Retry"
                            }
                        }
                    })
                }
        }
    }

    fun loadSlotStream(slotIdx: Int, item: IptvItem?) {
        val p = players[slotIdx]
        if (item == null) {
            try {
                p.stop()
                p.clearMediaItems()
            } catch (_: Exception) {}
            slotBuffering[slotIdx] = false
            slotErrors[slotIdx] = null
            return
        }
        try {
            slotBuffering[slotIdx] = true
            slotErrors[slotIdx] = null
            p.stop()
            p.clearMediaItems()

            val uri = Uri.parse(item.streamUrl.trim())
            val mediaItem = MediaItem.fromUri(uri)
            val isHls = item.detectFormat() == StreamFormat.HLS ||
                    item.streamUrl.contains(".m3u8", ignoreCase = true)

            val factory = slotHttpFactories[slotIdx]
            val mediaSource = if (isHls) {
                HlsMediaSource.Factory(factory)
                    .setAllowChunklessPreparation(true)
                    .createMediaSource(mediaItem)
            } else {
                DefaultMediaSourceFactory(factory)
                    .createMediaSource(mediaItem)
            }

            p.setMediaSource(mediaSource, true)
            p.prepare()
            p.playWhenReady = true
        } catch (e: Exception) {
            slotBuffering[slotIdx] = false
            slotErrors[slotIdx] = e.localizedMessage ?: "Error"
        }
    }

    for (idx in 0..3) {
        val trigger = slotLoadTriggers[idx]
        LaunchedEffect(trigger) {
            if (trigger > 0) {
                loadSlotStream(idx, slotChannels[idx])
            }
        }
    }

    DisposableEffect(localView) {
        val prevKeepScreenOn = localView.keepScreenOn
        localView.keepScreenOn = true
        val window = (context as? Activity)?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, localView) }
        insetsController?.apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }

        val startupJob = scope.launch(Dispatchers.Main) {
            delay(150L)
            for (i in 0 until screenCount) {
                val ch = slotChannels[i]
                if (ch != null) {
                    slotRetryCounts[i] = 0
                    loadSlotStream(i, ch)
                    delay(700L)
                }
            }
        }

        onDispose {
            startupJob.cancel()
            localView.keepScreenOn = prevKeepScreenOn
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
            players.forEach {
                try {
                    it.stop()
                    it.release()
                } catch (_: Exception) {}
            }
        }
    }

    LaunchedEffect(activeAudioSlot, screenCount) {
        players.forEachIndexed { idx, player ->
            try {
                player.volume = if (idx == activeAudioSlot && idx < screenCount) 1f else 0f
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(screenCount) {
        if (screenCount == 2) {
            for (idx in 2..3) {
                try {
                    players[idx].stop()
                    players[idx].clearMediaItems()
                } catch (_: Exception) {}
                slotBuffering[idx] = false
                slotErrors[idx] = null
            }
            if (activeAudioSlot >= 2) {
                activeAudioSlot = 0
            }
        } else if (screenCount == 4) {
            for (idx in 2..3) {
                val ch = slotChannels[idx]
                if (ch != null && !players[idx].isPlaying) {
                    slotRetryCounts[idx] = 0
                    loadSlotStream(idx, ch)
                    delay(700L)
                }
            }
        }
    }

    LaunchedEffect(hudTriggerKey, showOverlayHud, pickerSlotIndex) {
        if (showOverlayHud && pickerSlotIndex == null) {
            delay(4500L)
            showOverlayHud = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    (event.key == Key.Menu || event.key == Key.Info || event.key == Key.Guide)
                ) {
                    showOverlayHud = true
                    hudTriggerKey++
                    try {
                        hudFocusRequester.requestFocus()
                    } catch (_: Exception) {}
                    true
                } else false
            }
    ) {
        if (screenCount == 2) {
            Row(modifier = Modifier.fillMaxSize()) {
                for (slotIdx in 0..1) {
                    MultiViewSlotPane(
                        slotIndex = slotIdx,
                        item = slotChannels[slotIdx],
                        player = players[slotIdx],
                        isAudioActive = activeAudioSlot == slotIdx,
                        isBuffering = slotBuffering[slotIdx],
                        errorMessage = slotErrors[slotIdx],
                        showControls = showOverlayHud,
                        isAr = isAr,
                        focusRequester = slotFocusRequesters[slotIdx],
                        onFocusMoved = {
                            showOverlayHud = true
                            hudTriggerKey++
                        },
                        onMoveUpToHud = {
                            showOverlayHud = true
                            hudTriggerKey++
                            try {
                                hudFocusRequester.requestFocus()
                            } catch (_: Exception) {}
                        },
                        onSelectAudioFocus = {
                            activeAudioSlot = slotIdx
                            showOverlayHud = true
                            hudTriggerKey++
                        },
                        onRetrySlot = {
                            slotRetryCounts[slotIdx] = 0
                            loadSlotStream(slotIdx, slotChannels[slotIdx])
                        },
                        onChangeChannel = {
                            pickerSlotIndex = slotIdx
                        },
                        onExpandFullscreen = {
                            slotChannels[slotIdx]?.let { onPlaySingleFullscreen(it) }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    for (slotIdx in 0..1) {
                        MultiViewSlotPane(
                            slotIndex = slotIdx,
                            item = slotChannels[slotIdx],
                            player = players[slotIdx],
                            isAudioActive = activeAudioSlot == slotIdx,
                            isBuffering = slotBuffering[slotIdx],
                            errorMessage = slotErrors[slotIdx],
                            showControls = showOverlayHud,
                            isAr = isAr,
                            focusRequester = slotFocusRequesters[slotIdx],
                            onFocusMoved = {
                                showOverlayHud = true
                                hudTriggerKey++
                            },
                            onMoveUpToHud = {
                                showOverlayHud = true
                                hudTriggerKey++
                                try {
                                    hudFocusRequester.requestFocus()
                                } catch (_: Exception) {}
                            },
                            onSelectAudioFocus = {
                                activeAudioSlot = slotIdx
                                showOverlayHud = true
                                hudTriggerKey++
                            },
                            onRetrySlot = {
                                slotRetryCounts[slotIdx] = 0
                                loadSlotStream(slotIdx, slotChannels[slotIdx])
                            },
                            onChangeChannel = {
                                pickerSlotIndex = slotIdx
                            },
                            onExpandFullscreen = {
                                slotChannels[slotIdx]?.let { onPlaySingleFullscreen(it) }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    for (slotIdx in 2..3) {
                        MultiViewSlotPane(
                            slotIndex = slotIdx,
                            item = slotChannels[slotIdx],
                            player = players[slotIdx],
                            isAudioActive = activeAudioSlot == slotIdx,
                            isBuffering = slotBuffering[slotIdx],
                            errorMessage = slotErrors[slotIdx],
                            showControls = showOverlayHud,
                            isAr = isAr,
                            focusRequester = slotFocusRequesters[slotIdx],
                            onFocusMoved = {
                                showOverlayHud = true
                                hudTriggerKey++
                            },
                            onMoveUpToHud = null,
                            onSelectAudioFocus = {
                                activeAudioSlot = slotIdx
                                showOverlayHud = true
                                hudTriggerKey++
                            },
                            onRetrySlot = {
                                slotRetryCounts[slotIdx] = 0
                                loadSlotStream(slotIdx, slotChannels[slotIdx])
                            },
                            onChangeChannel = {
                                pickerSlotIndex = slotIdx
                            },
                            onExpandFullscreen = {
                                slotChannels[slotIdx]?.let { onPlaySingleFullscreen(it) }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showOverlayHud,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
        ) {
            Surface(
                color = Color(0xD9070C17),
                shape = CircleShape,
                border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.55f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.back,
                            tint = PureWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    val twoBtnInteraction = remember { MutableInteractionSource() }
                    val isTwoFocused by twoBtnInteraction.collectIsFocusedAsState()
                    Surface(
                        onClick = {
                            screenCount = 2
                            hudTriggerKey++
                        },
                        interactionSource = twoBtnInteraction,
                        color = when {
                            isTwoFocused -> PureWhite
                            screenCount == 2 -> AccentCyan
                            else -> Slate850
                        },
                        shape = CircleShape,
                        border = BorderStroke(
                            width = if (isTwoFocused) 2.dp else 0.dp,
                            color = AccentCyan
                        ),
                        modifier = Modifier.focusRequester(hudFocusRequester)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewColumn,
                                contentDescription = null,
                                tint = if (isTwoFocused || screenCount == 2) Slate950 else PureWhite,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "2",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isTwoFocused || screenCount == 2) Slate950 else PureWhite
                            )
                        }
                    }

                    val fourBtnInteraction = remember { MutableInteractionSource() }
                    val isFourFocused by fourBtnInteraction.collectIsFocusedAsState()
                    Surface(
                        onClick = {
                            screenCount = 4
                            hudTriggerKey++
                        },
                        interactionSource = fourBtnInteraction,
                        color = when {
                            isFourFocused -> PureWhite
                            screenCount == 4 -> AccentCyan
                            else -> Slate850
                        },
                        shape = CircleShape,
                        border = BorderStroke(
                            width = if (isFourFocused) 2.dp else 0.dp,
                            color = AccentCyan
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = null,
                                tint = if (isFourFocused || screenCount == 4) Slate950 else PureWhite,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "4",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isFourFocused || screenCount == 4) Slate950 else PureWhite
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            fillScreenMode = !fillScreenMode
                            hudTriggerKey++
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AspectRatio,
                            contentDescription = "Aspect Ratio",
                            tint = if (fillScreenMode) AccentCyan else PureWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        if (pickerSlotIndex != null) {
            val targetSlot = pickerSlotIndex!!
            MultiViewChannelPickerDialog(
                slotNumber = targetSlot + 1,
                allChannels = allChannels,
                currentLanguage = currentLanguage,
                isAr = isAr,
                onSelect = { chosen ->
                    slotChannels[targetSlot] = chosen
                    activeAudioSlot = targetSlot
                    slotRetryCounts[targetSlot] = 0
                    loadSlotStream(targetSlot, chosen)
                    pickerSlotIndex = null
                    showOverlayHud = true
                    hudTriggerKey++
                },
                onDismiss = { pickerSlotIndex = null }
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun MultiViewSlotPane(
    slotIndex: Int,
    item: IptvItem?,
    player: ExoPlayer,
    isAudioActive: Boolean,
    isBuffering: Boolean,
    errorMessage: String?,
    showControls: Boolean,
    isAr: Boolean,
    focusRequester: FocusRequester,
    onFocusMoved: () -> Unit,
    onMoveUpToHud: (() -> Unit)?,
    onSelectAudioFocus: () -> Unit,
    onRetrySlot: () -> Unit,
    onChangeChannel: () -> Unit,
    onExpandFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()
    var remoteLongPressHandled by remember { mutableStateOf(false) }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            onFocusMoved()
        }
    }

    val borderWidth = when {
        isFocused -> 2.dp
        isAudioActive -> 1.5.dp
        else -> 0.dp
    }
    val borderColor = when {
        isFocused -> PureWhite
        isAudioActive -> AccentCyan
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .border(borderWidth, borderColor)
            .focusRequester(focusRequester)
            .focusable(interactionSource = interaction)
            .onPreviewKeyEvent { event ->
                val isSelectKey = event.key == Key.DirectionCenter ||
                        event.key == Key.Enter ||
                        event.key == Key.NumPadEnter

                when {
                    event.key == Key.DirectionUp && event.type == KeyEventType.KeyDown && onMoveUpToHud != null -> {
                        onMoveUpToHud()
                        true
                    }
                    isSelectKey && event.type == KeyEventType.KeyDown -> {
                        if (event.nativeKeyEvent.isLongPress || event.nativeKeyEvent.repeatCount > 0) {
                            if (!remoteLongPressHandled) {
                                remoteLongPressHandled = true
                                onChangeChannel()
                            }
                            true
                        } else {
                            false
                        }
                    }
                    isSelectKey && event.type == KeyEventType.KeyUp -> {
                        if (remoteLongPressHandled) {
                            remoteLongPressHandled = false
                            true
                        } else {
                            if (item == null) {
                                onChangeChannel()
                            } else if (!isAudioActive) {
                                onSelectAudioFocus()
                            } else {
                                onChangeChannel()
                            }
                            true
                        }
                    }
                    else -> false
                }
            }
            .pointerInput(item?.id, isAudioActive) {
                detectTapGestures(
                    onTap = {
                        if (item == null) {
                            onChangeChannel()
                        } else {
                            onSelectAudioFocus()
                        }
                    },
                    onDoubleTap = {
                        if (item != null) {
                            onExpandFullscreen()
                        }
                    },
                    onLongPress = {
                        onChangeChannel()
                    }
                )
            }
    ) {
        if (item != null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    TextureView(ctx).apply {
                        keepScreenOn = true
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        player.setVideoTextureView(this)
                    }
                },
                update = { textureView ->
                    textureView.keepScreenOn = true
                    player.setVideoTextureView(textureView)
                }
            )

            if (isBuffering) {
                CircularProgressIndicator(
                    color = AccentCyan,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(32.dp)
                )
            }

            if (errorMessage != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 12.dp)
                        .background(Color(0xD9000000), RoundedCornerShape(12.dp))
                        .border(1.dp, AccentRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable(onClick = onRetrySlot)
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = AccentRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = errorMessage,
                        color = PureWhite,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isAr) "إعادة المحاولة" else "Tap to Retry",
                            color = AccentCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = showControls || isFocused,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Surface(
                    color = Color(0xCC060A12),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isAudioActive) AccentCyan else Color(0x33FFFFFF)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (isAudioActive) {
                                Icons.AutoMirrored.Filled.VolumeUp
                            } else {
                                Icons.AutoMirrored.Filled.VolumeOff
                            },
                            contentDescription = null,
                            tint = if (isAudioActive) AccentCyan else Slate400,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = item.name,
                            color = PureWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.width(110.dp)
                        )
                        IconButton(
                            onClick = onChangeChannel,
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Change Channel",
                                tint = AccentCyan,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        IconButton(
                            onClick = onExpandFullscreen,
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "Fullscreen",
                                tint = PureWhite,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF070B14))
                    .clickable(onClick = onChangeChannel)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(AccentCyan.copy(alpha = 0.16f))
                        .border(1.dp, AccentCyan, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isAr) "إضافة قناة (${slotIndex + 1})" else "Add Channel (${slotIndex + 1})",
                    color = Slate300,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun MultiViewChannelPickerDialog(
    slotNumber: Int,
    allChannels: List<IptvItem>,
    currentLanguage: String,
    isAr: Boolean,
    onSelect: (IptvItem) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf("ALL") }
    val firstChannelFocus = remember { FocusRequester() }

    val availableGroups = remember(allChannels) {
        allChannels.map { it.group.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    val filtered = remember(allChannels, query, selectedGroup) {
        allChannels.asSequence()
            .filter { ch ->
                selectedGroup == "ALL" || ch.group.equals(selectedGroup, ignoreCase = true)
            }
            .filter { ch ->
                query.isBlank() ||
                ch.name.contains(query, ignoreCase = true) ||
                ch.group.contains(query, ignoreCase = true)
            }
            .take(150)
            .toList()
    }

    LaunchedEffect(Unit) {
        try {
            delay(100L)
            firstChannelFocus.requestFocus()
        } catch (_: Exception) {}
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = if (isAr) "اختر قناة للشاشة رقم $slotNumber" else "Select Channel for Screen #$slotNumber",
                color = PureWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = if (isAr) "ابحث باسم القناة أو الباقة..." else "Search channel or category...",
                            color = Slate400,
                            fontSize = 12.sp
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = AccentCyan)
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Slate400)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite,
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = Slate700
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (availableGroups.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(vertical = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item(key = "grp_all") {
                            PickerCategoryChip(
                                label = if (isAr) "الكل (${allChannels.size})" else "All (${allChannels.size})",
                                isSelected = selectedGroup == "ALL",
                                onClick = { selectedGroup = "ALL" }
                            )
                        }
                        items(availableGroups, key = { "grp_$it" }) { grp ->
                            val translated = remember(grp, currentLanguage) {
                                CategoryTranslator.translateForLanguage(grp, currentLanguage)
                            }
                            PickerCategoryChip(
                                label = translated,
                                isSelected = selectedGroup == grp,
                                onClick = { selectedGroup = grp }
                            )
                        }
                    }
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 270.dp)
                ) {
                    items(filtered, key = { "${it.id}_${it.streamUrl}" }) { ch ->
                        val isFirst = filtered.firstOrNull()?.id == ch.id
                        val itemInteraction = remember { MutableInteractionSource() }
                        val isItemFocused by itemInteraction.collectIsFocusedAsState()

                        Surface(
                            color = if (isItemFocused) Slate800 else Slate850,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                width = if (isItemFocused) 2.dp else 1.dp,
                                color = if (isItemFocused) PureWhite else Slate700
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (isFirst) Modifier.focusRequester(firstChannelFocus) else Modifier)
                                .focusable(interactionSource = itemInteraction)
                                .onPreviewKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyUp &&
                                        (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                    ) {
                                        onSelect(ch)
                                        true
                                    } else false
                                }
                                .clickable(interactionSource = itemInteraction, indication = null) {
                                    onSelect(ch)
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp)
                            ) {
                                if (!ch.logoUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = ch.logoUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ch.name,
                                        color = if (isItemFocused) AccentCyan else PureWhite,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = ch.group,
                                        color = Slate400,
                                        fontSize = 10.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = if (isAr) "إغلاق" else "Close",
                    color = AccentCyan,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

@Composable
private fun PickerCategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()

    Surface(
        color = when {
            isFocused -> PureWhite
            isSelected -> AccentCyan
            else -> Slate850
        },
        shape = CircleShape,
        border = BorderStroke(
            width = if (isFocused) 2.dp else 1.dp,
            color = when {
                isFocused -> AccentCyan
                isSelected -> PureWhite
                else -> Slate700
            }
        ),
        modifier = Modifier
            .focusable(interactionSource = interaction)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                ) {
                    onClick()
                    true
                } else false
            }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isFocused || isSelected) Slate950 else Slate300,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
        )
    }
}
