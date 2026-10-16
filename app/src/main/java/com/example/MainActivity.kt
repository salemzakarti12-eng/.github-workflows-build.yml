package com.example

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.model.IptvItem
import com.example.model.IptvType
import com.example.player.ExoPlayerManager
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.channelbrowser.ContentBrowserScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.locale.LocalAppLanguage
import com.example.ui.locale.LocalAppStrings
import com.example.ui.locale.getAppStrings
import com.example.ui.player.VideoPlayerScreen
import com.example.ui.series.SeriesDetailScreen
import com.example.ui.sources.SourcesScreen
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.AccentRed
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.util.DeviceUtils
import com.example.util.LocalIsTelevision
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private var isInPipModeState by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val isTvDevice = try {
            DeviceUtils.isTelevision(this)
        } catch (_: Exception) {
            false
        }

        try {
            DeviceUtils.configureDeviceOrientation(this)
        } catch (_: Exception) {}

        if (!isTvDevice && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                enableEdgeToEdge()
            } catch (_: Exception) {}
        }

        setContent {
            val isTelevision = remember { isTvDevice }
            val currentLanguage by mainViewModel.currentLanguage.collectAsState()
            val layoutDirection = if (currentLanguage == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
            val strings = remember(currentLanguage) { getAppStrings(currentLanguage) }

            CompositionLocalProvider(
                LocalIsTelevision provides isTelevision,
                LocalAppLanguage provides currentLanguage,
                LocalAppStrings provides strings,
                LocalLayoutDirection provides layoutDirection
            ) {
                MyApplicationTheme {
                    HypnoStreamApp(
                        viewModel = mainViewModel,
                        isInPipMode = isInPipModeState,
                        onFinishApp = { finish() }
                    )
                }
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        try {
            val isTv = DeviceUtils.isTelevision(this)
            val hasActiveStream = mainViewModel.playerUiState.value.currentItem != null
            val isPlaying = mainViewModel.playerUiState.value.isPlaying
            if (!isTv && hasActiveStream && isPlaying) {
                enterPipMode()
            }
        } catch (_: Exception) {}
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipModeState = isInPictureInPictureMode
        if (isInPictureInPictureMode) {
            mainViewModel.playerManager.closeQuickChannelList()
        }
    }

    fun enterPipMode() {
        if (DeviceUtils.isTelevision(this)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            } catch (_: Exception) {}
        }
    }
}

@Composable
fun HypnoStreamApp(
    viewModel: MainViewModel = viewModel(),
    isInPipMode: Boolean = false,
    onFinishApp: () -> Unit = {}
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val activeSource by viewModel.activeSource.collectAsState()
    val sources by viewModel.sources.collectAsState()
    val totalLiveCount by viewModel.totalLiveCount.collectAsState()
    val totalMoviesCount by viewModel.totalMoviesCount.collectAsState()
    val totalSeriesCount by viewModel.totalSeriesCount.collectAsState()
    val currentCategories by viewModel.currentCategories.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val recentlyWatched by viewModel.recentlyWatched.collectAsState()
    val filteredItems by viewModel.currentFilteredItems.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val playerUiState by viewModel.playerUiState.collectAsState()
    val seriesDetails by viewModel.seriesDetails.collectAsState()
    val currentSeriesEpisodes by viewModel.currentSeriesEpisodes.collectAsState()
    val isSeriesLoading by viewModel.isSeriesLoading.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    val totalItemsLoaded = totalLiveCount + totalMoviesCount + totalSeriesCount

    // Startup Splash Gate: Stays visible on cold start until channels are connected & loaded (or up to 6s max timeout)
    var minSplashTimeElapsed by remember { mutableStateOf(false) }
    var maxSplashTimeElapsed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(2200L) // Always show smooth animation for at least 2.2 seconds
        minSplashTimeElapsed = true
        delay(4500L) // Safety timeout (6.7s total) in case user has no internet or no source configured
        maxSplashTimeElapsed = true
    }

    val showStartupSplash = !isInPipMode && (
        !minSplashTimeElapsed ||
        (!maxSplashTimeElapsed && (isLoading || totalItemsLoaded == 0))
    )

    val favoriteItems = recentlyWatched.filter { favorites.contains(it.id) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isTablet = configuration.smallestScreenWidthDp >= 600
    val isTv = LocalIsTelevision.current
    val useSideRailLayout = isLandscape || isTablet || isTv

    val strings = LocalAppStrings.current
    val isAr = LocalAppLanguage.current == "ar"
    val context = LocalContext.current

    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    BackHandler {
        if (!viewModel.handleBackPress()) {
            val now = System.currentTimeMillis()
            if (now - lastBackPressTime < 2000L) {
                onFinishApp()
            } else {
                lastBackPressTime = now
                val exitMsg = if (isAr) "اضغط رجوع مرة أخرى للخروج من التطبيق" else "Press back again to exit"
                Toast.makeText(context, exitMsg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Slate950)) {
        if (currentScreen == Screen.PLAYER || isInPipMode) {
            val activeItem = playerUiState.currentItem
            val sideDrawerItems = when (activeItem?.type) {
                IptvType.LIVE_TV -> viewModel.repository.getAllLiveChannels()
                IptvType.SERIES -> currentSeriesEpisodes
                else -> emptyList()
            }

            VideoPlayerScreen(
                playerManager = viewModel.playerManager,
                uiState = playerUiState,
                allChannels = sideDrawerItems,
                onBack = { viewModel.handleBackPress() },
                onPlayNext = { viewModel.playNextChannel() },
                onPlayPrevious = { viewModel.playPreviousChannel() },
                onSelectChannel = { viewModel.playStream(it) },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val navItems = listOf(
                Triple(Screen.DASHBOARD, Icons.Default.Home, if (isAr) "الرئيسية" else strings.home),
                Triple(Screen.LIVE_CHANNELS, Icons.Default.LiveTv, if (isAr) "البث المباشر" else strings.liveTv),
                Triple(Screen.MOVIES, Icons.Default.Movie, if (isAr) "الأفلام" else strings.movies),
                Triple(Screen.SERIES, Icons.Default.VideoLibrary, if (isAr) "المسلسلات" else strings.series),
                Triple(Screen.SOURCES, Icons.Default.Settings, if (isAr) "المصادر" else strings.sources)
            )

            val screenContent: @Composable () -> Unit = {
                when (currentScreen) {
                    Screen.DASHBOARD -> {
                        DashboardScreen(
                            activeSource = activeSource,
                            liveCount = totalLiveCount,
                            moviesCount = totalMoviesCount,
                            seriesCount = totalSeriesCount,
                            favorites = favoriteItems,
                            recentlyWatched = recentlyWatched,
                            onNavigate = { viewModel.navigateTo(it) },
                            onPlayItem = { item ->
                                if (item.type == IptvType.SERIES) {
                                    viewModel.openSeriesDetail(item)
                                } else {
                                    viewModel.playStream(item)
                                }
                            },
                            onToggleLanguage = { viewModel.toggleLanguage() }
                        )
                    }

                    Screen.LIVE_CHANNELS, Screen.MOVIES, Screen.SERIES -> {
                        ContentBrowserScreen(
                            screen = currentScreen,
                            categories = currentCategories,
                            items = filteredItems,
                            searchQuery = searchQuery,
                            selectedCategory = selectedCategory,
                            totalCount = when (currentScreen) {
                                Screen.LIVE_CHANNELS -> totalLiveCount
                                Screen.MOVIES -> totalMovies
