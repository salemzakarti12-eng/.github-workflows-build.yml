package com.example.ui.player

import android.app.Activity
import android.net.Uri
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GridView
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
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
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
import kotlinx.coroutines.delay

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
    val isAr = LocalAppLanguage.current == "ar"

    // 2 = تقسيم ثنائي متلاصق (نصفين), 4 = شبكة رباعية متلاصقة (2x2)
    var screenCount by remember { mutableIntStateOf(2) }
    var activeAudioSlot by remember { mutableIntStateOf(0) }
    var fillScreenMode by remember { mutableStateOf(true) }
    var showOverlayHud by remember { mutableStateOf(true) }
    var hudTriggerKey by remember { mutableIntStateOf(0) }

    // فتح نافذة اختيار قناة لمربع محدد
    var pickerSlotIndex by remember { mutableStateOf<Int?>(null) }

    // قائمة القنوات في المربعات الأربعة
    val slotChannels = remember {
        mutableStateListOf<IptvItem?>(
            initialChannel ?: allChannels.firstOrNull(),
            allChannels.firstOrNull { it.id != initialChannel?.id },
            null,
            null
        )
    }

    // حالات التحميل والأخطاء لكل مربع
    val slotBuffering = remember { mutableStateListOf(false, false, false, false) }
    val slotErrors = remember { mutableStateListOf<String?>(null, null, null, null) }

    // إنشاء 4 مشغلات ExoPlayer خفيفة ومضبوطة لحماية المعالج والـ RAM
    val players = remember {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("IPTVSmartersPlayer")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(12_000)
            .setReadTimeoutMs(20_000)

        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

        // تقليل الـ Buffer لتوفير الذاكرة عند تشغيل 4 قنوات معاً
        val lightweightLoadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                6_000,
                15_000,
                500,
                1_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        List(4) { index ->
            ExoPlayer.Builder(context, renderersFactory)
                .setMediaSourceFactory(DefaultMediaSourceFactory(httpFactory))
                .setLoadControl(lightweightLoadControl)
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
                                }
                                Player.STATE_IDLE, Player.STATE_ENDED -> {
                                    slotBuffering[index] = false
                                }
                            }
                        }

                        override fun onPlayerError(error: PlaybackException) {
                            slotBuffering[index] = false
                            slotErrors[index] = if (isAr) {
                                "تعذّر تشغيل القناة (قد لا يدعم المعالج قنوات إضافية)"
                            } else {
                                "Stream failed (Hardware decoder limit or offline)"
                            }
                        }
                    })
                }
        }
    }

    val httpDataSourceFactory = remember {
        DefaultHttpDataSource.Factory()
            .setUserAgent("IPTVSmartersPlayer")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(12_000)
            .setReadTimeoutMs(20_000)
    }

    // دالة تحميل البث داخل مربع محدد
    fun loadChannelInSlot(slotIdx: Int, item: IptvItem?) {
        val p = players[slotIdx]
        if (item == null) {
            p.stop()
            p.clearMediaItems()
            slotBuffering[slotIdx] = false
            slotErrors[slotIdx] = null
            return
        }
        try {
            slotBuffering[slotIdx] = true
            slotErrors[slotIdx] = null
            val uri = Uri.parse(item.streamUrl.trim())
            val mediaItem = MediaItem.fromUri(uri)
            val isHls = item.detectFormat() == StreamFormat.HLS ||
                    item.streamUrl.contains(".m3u8", ignoreCase = true)

            val mediaSource = if (isHls) {
                HlsMediaSource.Factory(httpDataSourceFactory)
                    .setAllowChunklessPreparation(true)
                    .createMediaSource(mediaItem)
            } else {
                DefaultMediaSourceFactory(httpDataSourceFactory)
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

    // إخفاء شريط النظام لملء الشاشة 100% وتنظيف المشغلات عند الخروج
    DisposableEffect(localView) {
        val prevKeepScreenOn = localView.keepScreenOn
        localView.keepScreenOn = true
        val window = (context as? Activity)?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, localView) }
        insetsController?.apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }

        // تشغيل القنوات المبدئية
        for (i in 0 until screenCount) {
            if (slotChannels[i] != null) {
                loadChannelInSlot(i, slotChannels[i])
            }
        }

        onDispose {
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

    // تحديث الصوت تلقائياً: فقط المربع النشط يعمل بصوت 100% والباقي مكتوم
    LaunchedEffect(activeAudioSlot, screenCount) {
        players.forEachIndexed { idx, player ->
            player.volume = if (idx == activeAudioSlot && idx < screenCount) 1f else 0f
        }
    }

    // إيقاف المشغلات 3 و 4 تلقائياً عند العودة إلى وضع الشاشتين لتوفير المعالج والإنترنت
    LaunchedEffect(screenCount) {
        if (screenCount == 2) {
            for (idx in 2..3) {
                players[idx].stop()
                players[idx].clearMediaItems()
                slotBuffering[idx] = false
                slotErrors[idx] = null
            }
            if (activeAudioSlot >= 2) {
                activeAudioSlot = 0
            }
        } else if (screenCount == 4) {
            for (idx in 2..3) {
                if (slotChannels[idx] != null && !players[idx].isPlaying) {
                    loadChannelInSlot(idx, slotChannels[idx])
                }
            }
        }
    }

    // إخفاء شريط التحكم العلوي تلقائياً بعد 4 ثوانٍ لتبقى الشاشة نظيفة بدون أي بازل
    LaunchedEffect(hudTriggerKey, showOverlayHud, pickerSlotIndex) {
        if (showOverlayHud && pickerSlotIndex == null) {
            delay(4000L)
            showOverlayHud = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // شبكة الفيديو المتلاصقة تماماً (Zero-Gap / No Bezel)
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
                        fillScreenMode = fillScreenMode,
                        showControls = showOverlayHud,
                        isAr = isAr,
                        onSelectAudioFocus = {
                            activeAudioSlot = slotIdx
                            showOverlayHud = true
                            hudTriggerKey++
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
                            fillScreenMode = fillScreenMode,
                            showControls = showOverlayHud,
                            isAr = isAr,
                            onSelectAudioFocus = {
                                activeAudioSlot = slotIdx
                                showOverlayHud = true
                                hudTriggerKey++
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
                            fillScreenMode = fillScreenMode,
                            showControls = showOverlayHud,
                            isAr = isAr,
                            onSelectAudioFocus = {
                                activeAudioSlot = slotIdx
                                showOverlayHud = true
                                hudTriggerKey++
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

        // شريط تحكم عائم علوي يظهر عند اللمس أو الضغط ويختفي تلقائياً
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

                    // زر التبديل إلى 2 شاشات متلاصقة
                    Surface(
                        onClick = {
                            screenCount = 2
                            hudTriggerKey++
                        },
                        color = if (screenCount == 2) AccentCyan else Slate850,
                        shape = CircleShape
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewColumn,
                                contentDescription = null,
                                tint = if (screenCount == 2) Slate950 else PureWhite,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "2",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = if (screenCount == 2) Slate950 else PureWhite
                            )
                        }
                    }

                    // زر التبديل إلى 4 شاشات متلاصقة
                    Surface(
                        onClick = {
                            screenCount = 4
                            hudTriggerKey++
                        },
                        color = if (screenCount == 4) AccentCyan else Slate850,
                        shape = CircleShape
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = null,
                                tint = if (screenCount == 4) Slate950 else PureWhite,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "4",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = if (screenCount == 4) Slate950 else PureWhite
                            )
                        }
                    }

                    // زر ملء الشاشة بدون أشرطة سوداء (FILL vs FIT)
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

        // نافذة اختيار قناة للمربع المحدد
        if (pickerSlotIndex != null) {
            val targetSlot = pickerSlotIndex!!
            MultiViewChannelPickerDialog(
                slotNumber = targetSlot + 1,
                allChannels = allChannels,
                isAr = isAr,
                onSelect = { chosen ->
                    slotChannels[targetSlot] = chosen
                    activeAudioSlot = targetSlot
                    loadChannelInSlot(targetSlot, chosen)
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
    fillScreenMode: Boolean,
    showControls: Boolean,
    isAr: Boolean,
    onSelectAudioFocus: () -> Unit,
    onChangeChannel: () -> Unit,
    onExpandFullscreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()

    // إطار فائق النحافة (1.5dp) فقط عند تفعيل الصوت أو التركيز بالريموت لعدم وجود أي بازل
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
            .focusable(interactionSource = interaction)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                ) {
                    if (item == null) {
                        onChangeChannel()
                    } else if (!isAudioActive) {
                        onSelectAudioFocus()
                    } else {
                        onChangeChannel()
                    }
                    true
                } else false
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
                    }
                )
            }
    ) {
        if (item != null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = false
                        isFocusable = false
                        isFocusableInTouchMode = false
                        keepScreenOn = true
                        resizeMode = if (fillScreenMode) {
                            AspectRatioFrameLayout.RESIZE_MODE_FILL
                        } else {
                            AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { view ->
                    if (view.player !== player) {
                        view.player = player
                    }
                    view.resizeMode = if (fillScreenMode) {
                        AspectRatioFrameLayout.RESIZE_MODE_FILL
                    } else {
                        AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
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
                        .background(Color(0xCC000000), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = AccentRed,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = errorMessage,
                        color = PureWhite,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // شارة صغيرة شفافة تظهر فقط عند لمس الشاشة أو تفعيل الصوت ثم تختفي لتترك الصورة صافية
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
            // مربع فارغ لإضافة قناة جديدة
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
    isAr: Boolean,
    onSelect: (IptvItem) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val searchFocus = remember { FocusRequester() }

    val filtered = remember(allChannels, query) {
        if (query.isBlank()) {
            allChannels.take(80)
        } else {
            allChannels.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.group.contains(query, ignoreCase = true)
            }.take(80)
        }
    }

    LaunchedEffect(Unit) {
        try {
            delay(80L)
            searchFocus.requestFocus()
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
                            text = if (isAr) "ابحث باسم القناة..." else "Search channel name...",
                            color = Slate400,
                            fontSize = 12.sp
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = AccentCyan)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite,
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = Slate700
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(searchFocus)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                ) {
                    items(filtered, key = { it.id }) { ch ->
                        Surface(
                            onClick = { onSelect(ch) },
                            color = Slate850,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Slate700)
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
                                        color = PureWhite,
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
