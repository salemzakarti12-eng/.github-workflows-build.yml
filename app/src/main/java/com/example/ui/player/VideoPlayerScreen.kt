package com.example.ui.player

import android.app.Activity
import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.Typeface
import android.media.AudioManager
import android.net.Uri
import android.view.View
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
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Height
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.media3.ui.SubtitleView
import coil.compose.AsyncImage
import com.example.model.AspectRatioMode
import com.example.model.IptvItem
import com.example.model.IptvType
import com.example.model.PlayerUiState
import com.example.player.ExoPlayerManager
import com.example.ui.locale.LocalAppLanguage
import com.example.ui.locale.LocalAppStrings
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
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlinx.coroutines.delay

private data class PlayerThemeColors(
    val accent: Color,
    val accentSoft: Color,
    val glowBorder: Color,
    val glassSurface: Color
)

private fun getPlayerThemeColors(type: IptvType?): PlayerThemeColors {
    return when (type) {
        IptvType.LIVE_TV -> PlayerThemeColors(
            accent = Color(0xFFFF6B35),
            accentSoft = Color(0x33FF6B35),
            glowBorder = Color(0x66FF6B35),
            glassSurface = Color(0xCC0D0A0B)
        )
        IptvType.MOVIE -> PlayerThemeColors(
            accent = Color(0xFF9D4EDD),
            accentSoft = Color(0x339D4EDD),
            glowBorder = Color(0x669D4EDD),
            glassSurface = Color(0xCC0B0914)
        )
        IptvType.SERIES, null -> PlayerThemeColors(
            accent = Color(0xFF00E5FF),
            accentSoft = Color(0x3300E5FF),
            glowBorder = Color(0x6600E5FF),
            glassSurface = Color(0xCC070E18)
        )
    }
}

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

private val SubtitleColorPalette = listOf(
    Color(0xFFFFFFFF), // White
    Color(0xFFF44336), // Red
    Color(0xFFE91E63), // Pink
    Color(0xFF9C27B0), // Purple
    Color(0xFF673AB7), // Deep Purple
    Color(0xFF3F51B5), // Indigo
    Color(0xFF2196F3), // Blue
    Color(0xFF00BCD4), // Cyan
    Color(0xFF009688), // Teal
    Color(0xFF4CAF50), // Green
    Color(0xFF8BC34A), // Light Green
    Color(0xFFCDDC39), // Lime
    Color(0xFFFFEB3B), // Yellow
    Color(0xFFFFC107), // Amber
    Color(0xFFFF9800), // Orange
    Color(0xFF795548), // Brown
    Color(0xFF607D8B), // Blue Grey
    Color(0xFF000000)  // Black
)

private val TransparencySteps = listOf(0.0f, 0.15f, 0.30f, 0.50f, 0.70f, 0.85f, 1.0f)

private data class FontChoice(
    val id: String,
    val displayName: String,
    val filePath: String? = null
)

private data class SubtitleAppearanceConfig(
    val textColorRgb: Int = 0xFFFFFF,
    val textAlpha: Float = 1.0f,
    val bgColorRgb: Int = 0x000000,
    val bgAlpha: Float = 0.0f,
    val textSizeScale: Float = 1.05f,
    val bottomPaddingFraction: Float = 0.08f,
    val fontId: String = "DEFAULT",
    val customFontPath: String? = null
)

private object SubtitleAppearancePrefs {
    private const val PREFS_NAME = "hypnostream_advanced_subtitle_prefs"

    fun load(context: Context): SubtitleAppearanceConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return SubtitleAppearanceConfig(
            textColorRgb = prefs.getInt("text_rgb", 0xFFFFFF),
            textAlpha = prefs.getFloat("text_alpha", 1.0f).coerceIn(0.15f, 1.0f),
            bgColorRgb = prefs.getInt("bg_rgb", 0x000000),
            bgAlpha = prefs.getFloat("bg_alpha", 0.0f).coerceIn(0f, 1f),
            textSizeScale = prefs.getFloat("size_scale", 1.05f).coerceIn(0.6f, 2.0f),
            bottomPaddingFraction = prefs.getFloat("bottom_pad", 0.08f).coerceIn(0.02f, 0.85f),
            fontId = prefs.getString("font_id", "DEFAULT") ?: "DEFAULT",
            customFontPath = prefs.getString("custom_font_path", null)
        )
    }

    fun save(context: Context, config: SubtitleAppearanceConfig) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putInt("text_rgb", config.textColorRgb)
            .putFloat("text_alpha", config.textAlpha)
            .putInt("bg_rgb", config.bgColorRgb)
            .putFloat("bg_alpha", config.bgAlpha)
            .putFloat("size_scale", config.textSizeScale)
            .putFloat("bottom_pad", config.bottomPaddingFraction)
            .putString("font_id", config.fontId)
            .putString("custom_font_path", config.customFontPath)
            .apply()
    }

    fun getAvailableFonts(context: Context, currentCustomPath: String?): List<FontChoice> {
        val list = mutableListOf(
            FontChoice("DEFAULT", "Default"),
            FontChoice("SANS_BOLD", "Sans-Serif Bold"),
            FontChoice("SERIF", "Serif"),
            FontChoice("MONOSPACE", "Monospace")
        )
        if (!currentCustomPath.isNullOrBlank() && File(currentCustomPath).exists()) {
            list.add(FontChoice("CUSTOM_LOCAL", "Custom: ${File(currentCustomPath).name}", currentCustomPath))
        }
        try {
            val sysFontsDir = File("/system/fonts")
            if (sysFontsDir.exists() && sysFontsDir.isDirectory) {
                sysFontsDir.listFiles()
                    ?.filter { it.isFile && (it.name.endsWith(".ttf", true) || it.name.endsWith(".otf", true)) }
                    ?.sortedBy { it.name.lowercase(Locale.US) }
                    ?.take(25)
                    ?.forEach { file ->
                        val cleanName = file.nameWithoutExtension
                        list.add(FontChoice("SYS_${file.name}", cleanName, file.absolutePath))
                    }
            }
        } catch (_: Exception) {}
        return list
    }

    fun resolveTypeface(config: SubtitleAppearanceConfig): Typeface? {
        return try {
            when {
                !config.customFontPath.isNullOrBlank() && File(config.customFontPath).exists() ->
                    Typeface.createFromFile(config.customFontPath)
                config.fontId == "SANS_BOLD" -> Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                config.fontId == "SERIF" -> Typeface.SERIF
                config.fontId == "MONOSPACE" -> Typeface.MONOSPACE
                config.fontId.startsWith("SYS_") -> {
                    val fileName = config.fontId.removePrefix("SYS_")
                    val file = File("/system/fonts/$fileName")
                    if (file.exists()) Typeface.createFromFile(file) else null
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }
}

private fun combineRgbWithAlpha(rgb: Int, alpha: Float): Int {
    val a = (alpha.coerceIn(0f, 1f) * 255).toInt()
    val r = (rgb shr 16) and 0xFF
    val g = (rgb shr 8) and 0xFF
    val b = rgb and 0xFF
    return AndroidColor.argb(a, r, g, b)
}

@OptIn(UnstableApi::class)
private fun PlayerView.applySubtitleAppearance(config: SubtitleAppearanceConfig) {
    val fgColor = combineRgbWithAlpha(config.textColorRgb, config.textAlpha)
    val bgColor = combineRgbWithAlpha(config.bgColorRgb, config.bgAlpha)
    val edgeType = if (config.bgAlpha < 0.25f) {
        CaptionStyleCompat.EDGE_TYPE_OUTLINE
    } else {
        CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW
    }
    val typeface = SubtitleAppearancePrefs.resolveTypeface(config)

    val captionStyle = CaptionStyleCompat(
        fgColor,
        bgColor,
        AndroidColor.TRANSPARENT,
        edgeType,
        AndroidColor.BLACK,
        typeface
    )

    subtitleView?.apply {
        layoutDirection = View.LAYOUT_DIRECTION_LOCALE
        setApplyEmbeddedStyles(false)
        setApplyEmbeddedFontSizes(false)
        setStyle(captionStyle)
        setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * config.textSizeScale)
        setBottomPaddingFraction(config.bottomPaddingFraction)
    }
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
    val strings = LocalAppStrings.current
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
    val playerColors = remember(currentItem?.type) { getPlayerThemeColors(currentItem?.type) }

    // --- True Fullscreen Immersive Mode ---
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

    // --- Binge-Mode Auto-Play Next Episode Countdown ---
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

    // --- Vertical Brightness & Volume Swipe Gestures ---
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

    // Advanced Subtitle Customization Dialogs & Overlay States
    var subtitleStyleConfig by remember { mutableStateOf(SubtitleAppearancePrefs.load(context)) }
    var showTextColorPicker by remember { mutableStateOf(false) }
    var showBgColorPicker by remember { mutableStateOf(false) }
    var showFontPickerDialog by remember { mutableStateOf(false) }
    var isAdjustingSubtitlePosition by remember { mutableStateOf(false) }

    val subtitleOffsetMs by playerManager.subtitleOffsetMs.collectAsState()

    val subtitlePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            playerManager.loadExternalSubtitle(uri, if (isAr) "ملف ترجمة خارجي" else "External Subtitle")
        }
    }

    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val destFile = File(context.filesDir, "custom_sub_font.ttf")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                val updated = subtitleStyleConfig.copy(
                    fontId = "CUSTOM_LOCAL",
                    customFontPath = destFile.absolutePath
                )
                subtitleStyleConfig = updated
                SubtitleAppearancePrefs.save(context, updated)
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(playerRootFocus)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (isAdjustingSubtitlePosition && event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionUp -> {
                            val updated = subtitleStyleConfig.copy(
                                bottomPaddingFraction = (subtitleStyleConfig.bottomPaddingFraction + 0.03f).coerceIn(0.02f, 0.85f)
                            )
                            subtitleStyleConfig = updated
                            SubtitleAppearancePrefs.save(context, updated)
                            return@onPreviewKeyEvent true
                        }
                        Key.DirectionDown -> {
                            val updated = subtitleStyleConfig.copy(
                                bottomPaddingFraction = (subtitleStyleConfig.bottomPaddingFraction - 0.03f).coerceIn(0.02f, 0.85f)
                            )
                            subtitleStyleConfig = updated
                            SubtitleAppearancePrefs.save(context, updated)
                            return@onPreviewKeyEvent true
                        }
                        Key.Back, Key.Escape, Key.DirectionCenter, Key.Enter -> {
                            isAdjustingSubtitlePosition = false
                            return@onPreviewKeyEvent true
                        }
                        else -> return@onPreviewKeyEvent true
                    }
                }

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
            .pointerInput(isLive, uiState.currentItem?.id, isAdjustingSubtitlePosition) {
                if (!isAdjustingSubtitlePosition) {
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
            }
            .pointerInput(isTv, isAdjustingSubtitlePosition) {
                if (!isTv && !isAdjustingSubtitlePosition) {
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
                    applySubtitleAppearance(subtitleStyleConfig)
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
                playerView.applySubtitleAppearance(subtitleStyleConfig)
                playerView.resizeMode = when (uiState.aspectRatioMode) {
                    AspectRatioMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    AspectRatioMode.FILL -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                    AspectRatioMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    AspectRatioMode.SIXTEEN_BY_NINE -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    AspectRatioMode.FOUR_BY_THREE -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            }
        )

        // Interactive Vertical Subtitle Position Overlay
        if (isAdjustingSubtitlePosition) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x33000000))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .fillMaxHeight(subtitleStyleConfig.bottomPaddingFraction.coerceIn(0.02f, 0.85f))
                ) {
                    HorizontalDivider(
                        thickness = 1.5.dp,
                        color = playerColors.accent.copy(alpha = 0.85f),
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }

                Surface(
                    color = playerColors.glassSurface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, playerColors.glowBorder),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 20.dp)
                        .width(56.dp)
                        .height(270.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 10.dp)
                    ) {
                        IconButton(
                            onClick = { isAdjustingSubtitlePosition = false },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = strings.close,
                                tint = PureWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            Slider(
                                value = subtitleStyleConfig.bottomPaddingFraction,
                                onValueChange = { newVal ->
                                    val updated = subtitleStyleConfig.copy(bottomPaddingFraction = newVal)
                                    subtitleStyleConfig = updated
                                    SubtitleAppearancePrefs.save(context, updated)
                                },
                                valueRange = 0.02f..0.85f,
                                colors = SliderDefaults.colors(
                                    thumbColor = PureWhite,
                                    activeTrackColor = playerColors.accent,
                                    inactiveTrackColor = Slate700
                                ),
                                modifier = Modifier
                                    .verticalSlider()
                                    .width(190.dp)
                            )
                        }

                        Text(
                            text = "${(subtitleStyleConfig.bottomPaddingFraction * 100).toInt()}%",
                            color = playerColors.accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Vertical Brightness / Volume Swipe Glassmorphic HUD Pill
        AnimatedVisibility(
            visible = activeSwipeHud != VerticalSwipeHUD.NONE && !isAdjustingSubtitlePosition,
            enter = fadeIn(tween(120)),
            exit = fadeOut(tween(220)),
            modifier = Modifier.align(
                if (activeSwipeHud == VerticalSwipeHUD.BRIGHTNESS) Alignment.CenterStart else Alignment.CenterEnd
            )
        ) {
            Surface(
                color = playerColors.glassSurface,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.5.dp, playerColors.glowBorder),
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .shadow(16.dp, RoundedCornerShape(22.dp), spotColor = playerColors.accent)
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
                        contentDescription = if (activeSwipeHud == VerticalSwipeHUD.BRIGHTNESS) {
                            strings.brightnessLabel
                        } else {
                            strings.volumeLabel
                        },
                        tint = playerColors.accent,
                        modifier = Modifier.size(22.dp)
                    )

                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(110.dp)
                            .clip(CircleShape)
                            .background(Color(0x661E293B))
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .fillMaxHeight(swipeHudLevel.coerceIn(0f, 1f))
                                .clip(CircleShape)
                                .background(playerColors.accent)
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

        // Kinetic Double-Tap Glassmorphic Ripple Pill Overlay
        AnimatedVisibility(
            visible = gestureFeedback != PlayerGestureFeedback.NONE && !isAdjustingSubtitlePosition,
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
                color = playerColors.glassSurface,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.5.dp, playerColors.glowBorder),
                modifier = Modifier
                    .padding(horizontal = 36.dp)
                    .scale(pulseScale)
                    .shadow(18.dp, RoundedCornerShape(24.dp), spotColor = playerColors.accent)
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
                        tint = playerColors.accent,
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

        // Binge-Mode Auto-Play Next Episode Glass Card
        AnimatedVisibility(
            visible = isNearEpisodeEnd && nextEpisodeItem != null && !isAdjustingSubtitlePosition,
            enter = fadeIn(tween(220)) + slideInHorizontally(initialOffsetX = { it }),
            exit = fadeOut(tween(180)) + slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = if (uiState.isControlsVisible) 118.dp else 24.dp)
        ) {
            val nextEp = nextEpisodeItem ?: return@AnimatedVisibility
            Surface(
                color = playerColors.glassSurface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.5.dp, playerColors.glowBorder),
                modifier = Modifier
                    .width(295.dp)
                    .shadow(20.dp, RoundedCornerShape(18.dp), spotColor = playerColors.accent)
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
                            color = playerColors.accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                        IconButton(
                            onClick = { nextEpDismissedForId = true },
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = strings.close, tint = Slate400, modifier = Modifier.size(16.dp))
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
                        color = playerColors.accent,
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
                            colors = ButtonDefaults.buttonColors(containerColor = playerColors.accent, contentColor = Slate950),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = strings.play,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = { nextEpDismissedForId = true },
                            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = strings.close,
                                color = Slate300,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Glassmorphic Buffering Spinner
        if (uiState.isBuffering) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = playerColors.glassSurface,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, playerColors.glowBorder)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp)
                    ) {
                        CircularProgressIndicator(
                            color = playerColors.accent,
                            strokeWidth = 3.5.dp,
                            modifier = Modifier.size(46.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = strings.parsingStreams,
                            color = PureWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Glassmorphic Error Overlay
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
                        .clip(RoundedCornerShape(20.dp))
                        .background(playerColors.glassSurface)
                        .border(1.5.dp, playerColors.glowBorder, RoundedCornerShape(20.dp))
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
                            colors = ButtonDefaults.buttonColors(containerColor = playerColors.accent, contentColor = Slate950)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isAr) "إعادة المحاولة" else "Retry", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onBack,
                            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite)
                        ) {
                            Text(strings.back)
                        }
                    }
                }
            }
        }

        // Top & Bottom Dark Glassmorphic Player Controls Overlay
        AnimatedVisibility(
            visible = uiState.isControlsVisible && !isAdjustingSubtitlePosition,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Glassmorphic Header Pill
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xCC000000), Color.Transparent)
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Surface(
                        color = playerColors.glassSurface,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(
                                listOf(playerColors.glowBorder, Color(0x26FFFFFF), playerColors.glowBorder)
                            )
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PlayerCircleButton(
                                icon = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = strings.back,
                                playerColors = playerColors,
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
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = currentItem?.group ?: "",
                                    color = playerColors.accent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            PlayerCircleButton(
                                icon = Icons.Default.Audiotrack,
                                contentDescription = strings.audio,
                                playerColors = playerColors,
                                onClick = { showAudioDialog = true }
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            PlayerCircleButton(
                                icon = Icons.Default.Subtitles,
                                contentDescription = strings.subtitles,
                                playerColors = playerColors,
                                onClick = { showSubtitleDialog = true }
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            PlayerCircleButton(
                                icon = Icons.Default.AspectRatio,
                                contentDescription = strings.fitScreen,
                                playerColors = playerColors,
                                onClick = { playerManager.cycleAspectRatio() }
                            )

                            if (hasDrawerItems) {
                                Spacer(modifier = Modifier.width(8.dp))
                                PlayerCircleButton(
                                    icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                                    contentDescription = strings.quickChannels,
                                    playerColors = playerColors,
                                    onClick = { playerManager.toggleQuickChannelList() }
                                )
                            }
                        }
                    }
                }

                // Bottom Glassmorphic Transport Dock
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xD9000000))
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Surface(
                        color = playerColors.glassSurface,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(
                                listOf(playerColors.glowBorder, Color(0x26FFFFFF), playerColors.glowBorder)
                            )
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp)
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
                                        fontWeight = FontWeight.Bold
                                    )
                                    Slider(
                                        value = uiState.currentPositionMs.toFloat(),
                                        onValueChange = { playerManager.seekTo(it.toLong()) },
                                        valueRange = 0f..uiState.durationMs.toFloat().coerceAtLeast(1f),
                                        interactionSource = sliderInteraction,
                                        colors = SliderDefaults.colors(
                                            thumbColor = if (isSliderFocused) PureWhite else playerColors.accent,
                                            activeTrackColor = playerColors.accent,
                                            inactiveTrackColor = Color(0x55334155)
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
                                    playerColors = playerColors,
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

                                Spacer(modifier = Modifier.width(22.dp))

                                PlayerCircleButton(
                                    icon = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = strings.play,
                                    isPrimary = true,
                                    playerColors = playerColors,
                                    focusRequester = playPauseFocus,
                                    onClick = { playerManager.togglePlayPause() }
                                )

                                Spacer(modifier = Modifier.width(22.dp))

                                PlayerCircleButton(
                                    icon = if (isLive) Icons.Default.SkipNext else Icons.Default.Forward10,
                                    contentDescription = "Next / Forward 10s",
                                    playerColors = playerColors,
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
            }
        }

        // Glassmorphic Quick Side Drawer
        AnimatedVisibility(
            visible = uiState.isQuickChannelListOpen && hasDrawerItems && !isAdjustingSubtitlePosition,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Surface(
                color = Color(0xEB070C17),
                modifier = Modifier
                    .width(305.dp)
                    .fillMaxHeight()
                    .border(
                        BorderStroke(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                listOf(playerColors.glowBorder, Color(0x26FFFFFF))
                            )
                        )
                    )
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(18.dp)
                                    .clip(CircleShape)
                                    .background(playerColors.accent)
                            )
                            Text(
                                text = if (currentItem?.type == IptvType.SERIES) {
                                    strings.episodes
                                } else {
                                    strings.quickChannels
                                },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                        }
                        IconButton(onClick = { playerManager.closeQuickChannelList() }) {
                            Icon(Icons.Default.Close, contentDescription = strings.close, tint = Slate400)
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
                                    isItemFocused -> Color(0x991E293B)
                                    isCurrent -> playerColors.accentSoft
                                    else -> Color(0x660F172A)
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    width = if (isItemFocused || isCurrent) 1.5.dp else 1.dp,
                                    color = when {
                                        isItemFocused -> PureWhite
                                        isCurrent -> playerColors.accent
                                        else -> Color(0x1AFFFFFF)
                                    }
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
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ch.name,
                                            color = if (isCurrent) playerColors.accent else PureWhite,
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

        // Audio Tracks Dialog
        if (showAudioDialog) {
            AlertDialog(
                onDismissRequest = { showAudioDialog = false },
                containerColor = Slate900,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Text(
                        text = strings.selectAudioTrack,
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
                                    color = if (track.isSelected) playerColors.accentSoft else Slate850,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, if (track.isSelected) playerColors.accent else Slate700),
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
                                            Icon(Icons.Default.Check, contentDescription = null, tint = playerColors.accent, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAudioDialog = false }) {
                        Text(strings.close, color = playerColors.accent)
                    }
                }
            )
        }

        // Main Subtitles, Appearance, Font, Position & Sync Dialog
        if (showSubtitleDialog) {
            AlertDialog(
                onDismissRequest = { showSubtitleDialog = false },
                containerColor = Slate900,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Text(
                        text = strings.selectSubtitleTrack,
                        color = PureWhite,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 390.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // 1. Color & Background Pickers Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { showTextColorPicker = true },
                                border = BorderStroke(1.dp, Slate700),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color(subtitleStyleConfig.textColorRgb or 0xFF000000.toInt()))
                                        .border(1.dp, PureWhite, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isAr) "لون النص" else "Text Color",
                                    color = PureWhite,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }

                            OutlinedButton(
                                onClick = { showBgColorPicker = true },
                                border = BorderStroke(1.dp, Slate700),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.FormatColorFill, contentDescription = null, tint = playerColors.accent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isAr) "لون الخلفية" else "Background",
                                    color = PureWhite,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }

                        // 2. Font Picker & Vertical Position Buttons Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { showFontPickerDialog = true },
                                border = BorderStroke(1.dp, Slate700),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.FontDownload, contentDescription = null, tint = playerColors.accent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isAr) "نوع الخط" else "Font",
                                    color = PureWhite,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    showSubtitleDialog = false
                                    if (uiState.isControlsVisible) {
                                        playerManager.toggleControlsVisibility()
                                    }
                                    isAdjustingSubtitlePosition = true
                                },
                                border = BorderStroke(1.dp, playerColors.accent.copy(alpha = 0.7f)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Height, contentDescription = null, tint = playerColors.accent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isAr) "مكان الترجمة" else "Position",
                                    color = playerColors.accent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }

                        // 3. Smooth Font Size Slider
                        Column {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isAr) "حجم الخط:" else "Font Size:",
                                    color = Slate300,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${(subtitleStyleConfig.textSizeScale * 100).toInt()}%",
                                    color = playerColors.accent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = subtitleStyleConfig.textSizeScale,
                                onValueChange = { scale ->
                                    val updated = subtitleStyleConfig.copy(textSizeScale = scale)
                                    subtitleStyleConfig = updated
                                    SubtitleAppearancePrefs.save(context, updated)
                                },
                                valueRange = 0.6f..2.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = playerColors.accent,
                                    activeTrackColor = playerColors.accent,
                                    inactiveTrackColor = Slate700
                                )
                            )
                        }

                        // 4. Subtitle Delay Sync
                        Text(
                            text = if (isAr) {
                                "مزامنة الترجمة: ${String.format(Locale.US, "%+.1f", subtitleOffsetMs / 1000f)} ثانية"
                            } else {
                                "Subtitle Delay: ${String.format(Locale.US, "%+.1f", subtitleOffsetMs / 1000f)}s"
                            },
                            color = playerColors.accent,
                            fontSize = 12.sp,
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
                                Text("0.0s", color = playerColors.accent, fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { playerManager.adjustSubtitleOffset(500L) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("+0.5s", color = PureWhite, fontSize = 12.sp)
                            }
                        }

                        // 5. External File / URL Loaders
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    showSubtitleDialog = false
                                    subtitlePickerLauncher.launch("*/*")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = playerColors.accent, contentColor = Slate950),
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
                                    focusedBorderColor = playerColors.accent,
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
                                colors = ButtonDefaults.buttonColors(containerColor = playerColors.accent, contentColor = Slate950),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (isAr) "تفعيل الرابط" else "Apply Subtitle URL", fontWeight = FontWeight.Bold)
                            }
                        }

                        // 6. Subtitle Track Selection
                        Surface(
                            color = if (uiState.isSubtitleOff) playerColors.accentSoft else Slate850,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (uiState.isSubtitleOff) playerColors.accent else Slate700),
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
                                Text(strings.off, color = PureWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                if (uiState.isSubtitleOff) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = playerColors.accent, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        uiState.subtitleTracks.forEach { track ->
                            Surface(
                                color = if (track.isSelected && !uiState.isSubtitleOff) playerColors.accentSoft else Slate850,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (track.isSelected && !uiState.isSubtitleOff) playerColors.accent else Slate700),
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
                                        Icon(Icons.Default.Check, contentDescription = null, tint = playerColors.accent, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSubtitleDialog = false }) {
                        Text(strings.close, color = playerColors.accent)
                    }
                }
            )
        }

        // Circular Grid Color & Transparency Picker Dialog (Text Color)
        if (showTextColorPicker) {
            SubtitleColorGridDialog(
                title = if (isAr) "اختر لون الترجمة" else "Select subtitle color",
                selectedRgb = subtitleStyleConfig.textColorRgb,
                selectedAlpha = subtitleStyleConfig.textAlpha,
                accentColor = playerColors.accent,
                isAr = isAr,
                onSelect = { rgb, alpha ->
                    val safeAlpha = alpha.coerceAtLeast(0.15f)
                    val updated = subtitleStyleConfig.copy(textColorRgb = rgb, textAlpha = safeAlpha)
                    subtitleStyleConfig = updated
                    SubtitleAppearancePrefs.save(context, updated)
                },
                onDismiss = { showTextColorPicker = false }
            )
        }

        // Circular Grid Color & Transparency Picker Dialog (Background Color)
        if (showBgColorPicker) {
            SubtitleColorGridDialog(
                title = if (isAr) "اختر لون خلفية الترجمة" else "Select subtitle background color",
                selectedRgb = subtitleStyleConfig.bgColorRgb,
                selectedAlpha = subtitleStyleConfig.bgAlpha,
                accentColor = playerColors.accent,
                isAr = isAr,
                onSelect = { rgb, alpha ->
                    val updated = subtitleStyleConfig.copy(bgColorRgb = rgb, bgAlpha = alpha)
                    subtitleStyleConfig = updated
                    SubtitleAppearancePrefs.save(context, updated)
                },
                onDismiss = { showBgColorPicker = false }
            )
        }

        // System & Local Font Picker Dialog (Full TV D-Pad Focus Support)
        if (showFontPickerDialog) {
            val fonts = remember(subtitleStyleConfig.customFontPath) {
                SubtitleAppearancePrefs.getAvailableFonts(context, subtitleStyleConfig.customFontPath)
            }
            AlertDialog(
                onDismissRequest = { showFontPickerDialog = false },
                containerColor = Slate900,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isAr) "خطوط النظام والمحلية" else "SYSTEM & LOCAL FONTS",
                            color = playerColors.accent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { fontPickerLauncher.launch("*/*") }) {
                            Text(
                                text = if (isAr) "+ ملف خط TTF" else "+ Load TTF",
                                color = PureWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        fonts.forEach { fontItem ->
                            val isSelected = subtitleStyleConfig.fontId == fontItem.id
                            val itemInteraction = remember { MutableInteractionSource() }
                            val isFocused by itemInteraction.collectIsFocusedAsState()

                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when {
                                            isFocused -> Slate800
                                            isSelected -> playerColors.accentSoft
                                            else -> Color.Transparent
                                        }
                                    )
                                    .border(
                                        width = if (isFocused) 2.dp else 0.dp,
                                        color = if (isFocused) PureWhite else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .focusable(interactionSource = itemInteraction)
                                    .onPreviewKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyUp &&
                                            (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                        ) {
                                            val updated = subtitleStyleConfig.copy(
                                                fontId = fontItem.id,
                                                customFontPath = if (fontItem.id == "CUSTOM_LOCAL") fontItem.filePath else subtitleStyleConfig.customFontPath
                                            )
                                            subtitleStyleConfig = updated
                                            SubtitleAppearancePrefs.save(context, updated)
                                            showFontPickerDialog = false
                                            true
                                        } else false
                                    }
                                    .clickable(interactionSource = itemInteraction, indication = null) {
                                        val updated = subtitleStyleConfig.copy(
                                            fontId = fontItem.id,
                                            customFontPath = if (fontItem.id == "CUSTOM_LOCAL") fontItem.filePath else subtitleStyleConfig.customFontPath
                                        )
                                        subtitleStyleConfig = updated
                                        SubtitleAppearancePrefs.save(context, updated)
                                        showFontPickerDialog = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = fontItem.displayName,
                                    color = if (isSelected) playerColors.accent else PureWhite,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = playerColors.accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showFontPickerDialog = false }) {
                        Text(strings.close, color = playerColors.accent)
                    }
                }
            )
        }
    }
}

@Composable
private fun SubtitleColorGridDialog(
    title: String,
    selectedRgb: Int,
    selectedAlpha: Float,
    accentColor: Color,
    isAr: Boolean,
    onSelect: (rgb: Int, alpha: Float) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF2B2D30),
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = title,
                color = PureWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SubtitleColorPalette.chunked(6).forEach { rowColors ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowColors.forEach { color ->
                                val rgb = color.toArgb() and 0x00FFFFFF
                                val isSelected = rgb == (selectedRgb and 0x00FFFFFF)
                                val checkTint = if (color.luminance() > 0.5f) Color.Black else Color.White
                                val swatchInteraction = remember { MutableInteractionSource() }
                                val isFocused by swatchInteraction.collectIsFocusedAsState()

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(if (isFocused) 38.dp else 34.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isFocused) 3.dp else if (isSelected) 2.dp else 1.dp,
                                            color = when {
                                                isFocused -> PureWhite
                                                isSelected -> accentColor
                                                else -> Color.White.copy(alpha = 0.35f)
                                            },
                                            shape = CircleShape
                                        )
                                        .focusable(interactionSource = swatchInteraction)
                                        .onPreviewKeyEvent { event ->
                                            if (event.type == KeyEventType.KeyUp &&
                                                (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                            ) {
                                                onSelect(rgb, selectedAlpha)
                                                true
                                            } else false
                                        }
                                        .clickable(interactionSource = swatchInteraction, indication = null) {
                                            onSelect(rgb, selectedAlpha)
                                        }
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = checkTint,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

                Text(
                    text = if (isAr) "الشفافية (${(selectedAlpha * 100).toInt()}%)" else "Transparency (${(selectedAlpha * 100).toInt()}%)",
                    color = Slate300,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val baseColor = Color(selectedRgb or 0xFF000000.toInt())
                    TransparencySteps.forEach { alphaStep ->
                        val isSelected = kotlin.math.abs(selectedAlpha - alphaStep) < 0.08f
                        val alphaInteraction = remember { MutableInteractionSource() }
                        val isFocused by alphaInteraction.collectIsFocusedAsState()

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(if (isFocused) 36.dp else 32.dp)
                                .clip(CircleShape)
                                .border(
                                    width = if (isFocused) 3.dp else if (isSelected) 2.dp else 1.dp,
                                    color = when {
                                        isFocused -> PureWhite
                                        isSelected -> accentColor
                                        else -> Color.White.copy(alpha = 0.4f)
                                    },
                                    shape = CircleShape
                                )
                                .focusable(interactionSource = alphaInteraction)
                                .onPreviewKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyUp &&
                                        (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                    ) {
                                        onSelect(selectedRgb, alphaStep)
                                        true
                                    } else false
                                }
                                .clickable(interactionSource = alphaInteraction, indication = null) {
                                    onSelect(selectedRgb, alphaStep)
                                }
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val cell = size.width / 4f
                                for (row in 0..3) {
                                    for (col in 0..3) {
                                        drawRect(
                                            color = if ((row + col) % 2 == 0) Color(0xFFCCCCCC) else Color(0xFF777777),
                                            topLeft = Offset(col * cell, row * cell),
                                            size = Size(cell, cell)
                                        )
                                    }
                                }
                                drawRect(color = baseColor.copy(alpha = alphaStep))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = if (isAr) "تأكيد" else "SELECT",
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

private fun Modifier.verticalSlider(): Modifier =
    this
        .graphicsLayer {
            rotationZ = 270f
            transformOrigin = TransformOrigin(0.5f, 0.5f)
        }
        .layout { measurable, constraints ->
            val placeable = measurable.measure(
                Constraints(
                    minWidth = constraints.minHeight,
                    maxWidth = constraints.maxHeight,
                    minHeight = constraints.minWidth,
                    maxHeight = constraints.maxWidth,
                )
            )
            layout(placeable.height, placeable.width) {
                placeable.place(-placeable.width / 2 + placeable.height / 2, -placeable.height / 2 + placeable.width / 2)
            }
        }

@Composable
private fun PlayerCircleButton(
    icon: ImageVector,
    contentDescription: String,
    playerColors: PlayerThemeColors,
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
                    isPrimary -> playerColors.accent
                    else -> Color(0x88111827)
                }
            )
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = if (isFocused) playerColors.accent else playerColors.glowBorder,
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
