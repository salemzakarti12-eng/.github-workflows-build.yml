package com.example

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
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
import com.example.ui.theme.AccentRed
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.util.DeviceUtils
import com.example.util.LocalIsTelevision
import java.util.Locale
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private var isInPipModeState by mutableStateOf(false)
    private var showExitDialogState by mutableStateOf(false)
    private var lastBrowseScreenState by mutableStateOf(Screen.DASHBOARD)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Intercept ALL system back presses (TV remote Back, gesture back, hardware back)
        // at the Activity level so the app NEVER exits without confirmation.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleSystemBackNavigation()
            }
        })

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
                        showExitDialog = showExitDialogState,
                        onShowExitDialogChange = { showExitDialogState = it },
                        onLastBrowseScreenChange = { lastBrowseScreenState = it },
                        onNavigateBack = { handleSystemBackNavigation() },
                        onFinishApp = {
                            mainViewModel.playerManager.saveCurrentPlaybackPosition()
                            mainViewModel.playerManager.stop()
                            finishAndRemoveTask()
                        }
                    )
                }
            }
        }
    }

    private fun handleSystemBackNavigation() {
        val screen = mainViewModel.currentScreen.value
        when {
            showExitDialogState -> {
                showExitDialogState = false
            }
            screen == Screen.PLAYER -> {
                mainViewModel.playerManager.closeQuickChannelList()
                val target = if (lastBrowseScreenState == Screen.PLAYER) Screen.DASHBOARD else lastBrowseScreenState
                mainViewModel.navigateTo(target)
            }
            screen == Screen.SERIES_DETAIL -> {
                mainViewModel.navigateTo(Screen.SERIES)
            }
            screen != Screen.DASHBOARD -> {
                mainViewModel.navigateTo(Screen.DASHBOARD)
            }
            else -> {
                showExitDialogState = true
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_UP) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_BACK,
                KeyEvent.KEYCODE_ESCAPE -> {
                    handleSystemBackNavigation()
                    return true
                }
                KeyEvent.KEYCODE_HOME -> {
                    if (mainViewModel.currentScreen.value != Screen.DASHBOARD) {
                        mainViewModel.navigateTo(Screen.DASHBOARD)
                        return true
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        try {
            val isTvOrBox = DeviceUtils.isTelevision(this) ||
                resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val hasActiveStream = mainViewModel.playerUiState.value.currentItem != null
            val isPlaying = mainViewModel.playerUiState.value.isPlaying

            if (isTvOrBox) {
                if (hasActiveStream) {
                    mainViewModel.playerManager.saveCurrentPlaybackPosition()
                    mainViewModel.playerManager.stop()
                }
            } else if (hasActiveStream && isPlaying) {
                enterPipMode()
            }
        } catch (_: Exception) {}
    }

    override fun onStop() {
        super.onStop()
        try {
            if (!isInPictureInPictureMode && !isChangingConfigurations) {
                mainViewModel.playerManager.saveCurrentPlaybackPosition()
                mainViewModel.playerManager.stop()
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
    showExitDialog: Boolean = false,
    onShowExitDialogChange: (Boolean) -> Unit = {},
    onLastBrowseScreenChange: (Screen) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onFinishApp: () -> Unit = {}
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    var lastBrowseScreen by remember { mutableStateOf(Screen.DASHBOARD) }

    LaunchedEffect(currentScreen) {
        if (currentScreen != Screen.PLAYER) {
            lastBrowseScreen = currentScreen
            onLastBrowseScreenChange(currentScreen)
        }
    }

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

    val favoriteItems = recentlyWatched.filter { favorites.contains(it.id) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isTablet = configuration.smallestScreenWidthDp >= 600
    val isTv = LocalIsTelevision.current
    val useSideRailLayout = isLandscape || isTablet || isTv

    val strings = LocalAppStrings.current
    val isAr = LocalAppLanguage.current == "ar"

    var pendingResumeItem by remember { mutableStateOf<IptvItem?>(null) }
    var pendingResumePosMs by remember { mutableLongStateOf(0L) }

    val performBack = {
        if (pendingResumeItem != null) {
            pendingResumeItem = null
        } else {
            onNavigateBack()
        }
    }

    val handlePlayWithResumePrompt = { item: IptvItem ->
        val savedPos = viewModel.playerManager.getSavedPosition(item)
        if (savedPos > 5_000L && item.type != IptvType.LIVE_TV) {
            pendingResumeItem = item
            pendingResumePosMs = savedPos
        } else {
            viewModel.playStream(item)
        }
    }

    val handleOpenHistoryOrDashboardItem = { item: IptvItem ->
        if (item.type == IptvType.SERIES) {
            val matchedSeries = viewModel.repository.getAllSeries().firstOrNull {
                it.name.equals(item.name, ignoreCase = true) ||
                (!item.seriesTitle.isNullOrBlank() && it.seriesTitle?.equals(item.seriesTitle, ignoreCase = true) == true)
            } ?: item
            viewModel.openSeriesDetail(matchedSeries)
        } else {
            handlePlayWithResumePrompt(item)
        }
    }

    val totalLoadedItems = totalLiveCount + totalMoviesCount + totalSeriesCount
    var minSplashTimePassed by remember { mutableStateOf(false) }
    var initialLoadCompleted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(2000L)
        minSplashTimePassed = true
        delay(10000L)
        initialLoadCompleted = true
    }

    LaunchedEffect(minSplashTimePassed, isLoading, totalLoadedItems) {
        if (minSplashTimePassed && (!isLoading && (totalLoadedItems > 0 || activeSource == null))) {
            delay(250L)
            initialLoadCompleted = true
        }
    }

    val showSplash = !initialLoadCompleted
    val isPlayerOpen = (currentScreen == Screen.PLAYER && playerUiState.currentItem != null) || isInPipMode
    val displayedBrowseScreen = if (currentScreen == Screen.PLAYER) lastBrowseScreen else currentScreen

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        val navItems = listOf(
            Triple(Screen.DASHBOARD, Icons.Default.Home, if (isAr) "الرئيسية" else strings.home),
            Triple(Screen.LIVE_CHANNELS, Icons.Default.LiveTv, if (isAr) "البث المباشر" else strings.liveTv),
            Triple(Screen.MOVIES, Icons.Default.Movie, if (isAr) "الأفلام" else strings.movies),
            Triple(Screen.SERIES, Icons.Default.VideoLibrary, if (isAr) "المسلسلات" else strings.series),
            Triple(Screen.HISTORY, Icons.Default.History, if (isAr) "السجل" else "History"),
            Triple(Screen.SOURCES, Icons.Default.Settings, if (isAr) "المصادر" else strings.sources)
        )

        val screenContent: @Composable () -> Unit = {
            when (displayedBrowseScreen) {
                Screen.DASHBOARD -> {
                    DashboardScreen(
                        activeSource = activeSource,
                        liveCount = totalLiveCount,
                        moviesCount = totalMoviesCount,
                        seriesCount = totalSeriesCount,
                        favorites = favoriteItems,
                        recentlyWatched = recentlyWatched,
                        onNavigate = { viewModel.navigateTo(it) },
                        onPlayItem = handleOpenHistoryOrDashboardItem,
                        onToggleLanguage = { viewModel.toggleLanguage() }
                    )
                }

                Screen.HISTORY -> {
                    WatchHistoryScreen(
                        items = recentlyWatched,
                        onPlayItem = handleOpenHistoryOrDashboardItem,
                        onRemoveItem = { viewModel.removeFromHistory(it) },
                        onClearAll = { viewModel.clearHistory() },
                        onBack = { performBack() }
                    )
                }

                Screen.LIVE_CHANNELS, Screen.MOVIES, Screen.SERIES -> {
                    ContentBrowserScreen(
                        screen = displayedBrowseScreen,
                        categories = currentCategories,
                        items = filteredItems,
                        searchQuery = searchQuery,
                        selectedCategory = selectedCategory,
                        totalCount = when (displayedBrowseScreen) {
                            Screen.LIVE_CHANNELS -> totalLiveCount
                            Screen.MOVIES -> totalMoviesCount
                            Screen.SERIES -> totalSeriesCount
                            else -> filteredItems.size
                        },
                        favoriteCount = favorites.size,
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onCategoryChange = { viewModel.setSelectedCategory(it) },
                        onLoadMore = { viewModel.loadMoreItems() },
                        onPlayItem = { handlePlayWithResumePrompt(it) },
                        onOpenSeries = { seriesItem -> viewModel.openSeriesDetail(seriesItem) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onBack = { performBack() }
                    )
                }

                Screen.SERIES_DETAIL -> {
                    SeriesDetailScreen(
                        details = seriesDetails,
                        isLoading = isSeriesLoading,
                        onPlayEpisode = { episode ->
                            val series = viewModel.selectedSeries.value
                            val seriesTitle = seriesDetails?.title ?: series?.seriesTitle ?: series?.name ?: if (isAr) "مسلسل" else "TV Series"
                            val group = if (isAr) "الموسم ${episode.seasonNumber}" else "Season ${episode.seasonNumber}"
                            val epItem = episode.toIptvItem(seriesTitle, group).copy(
                                type = IptvType.SERIES,
                                seasonNumber = episode.seasonNumber,
                                episodeNumber = episode.episodeNumber
                            )
                            handlePlayWithResumePrompt(epItem)
                        },
                        onBack = { performBack() }
                    )
                }

                Screen.SOURCES -> {
                    SourcesScreen(
                        sources = sources,
                        activeSource = activeSource,
                        isLoading = isLoading,
                        statusMessage = statusMessage,
                        onConnectXtream = { server, user, pass, name ->
                            viewModel.connectXtream(server, user, pass, name)
                        },
                        onLoadM3u = { url, content, name ->
                            viewModel.loadM3u(url, content, name)
                        },
                        onLoadDemo = { viewModel.loadDemoShowcase() },
                        onSwitchSource = { viewModel.switchActiveSource(it) },
                        onDeleteSource = { viewModel.deleteSource(it) },
                        onBack = { performBack() },
                        onToggleLanguage = { viewModel.toggleLanguage() }
                    )
                }

                Screen.PLAYER -> {}
            }
        }

        if (useSideRailLayout) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Slate950)
            ) {
                NavigationRail(
                    containerColor = Slate900,
                    contentColor = PureWhite,
                    modifier = Modifier
                        .width(if (isTablet || isTv) 88.dp else 76.dp)
                        .fillMaxHeight()
                        .focusGroup()
                        .border(BorderStroke(1.dp, Slate800))
                        .testTag("main_navigation_rail")
                ) {
                    Spacer(modifier = Modifier.height(10.dp))
                    navItems.forEach { (screen, icon, label) ->
                        val isSelected = displayedBrowseScreen == screen || (screen == Screen.SERIES && displayedBrowseScreen == Screen.SERIES_DETAIL)
                        val navInteraction = remember { MutableInteractionSource() }
                        val isNavFocused by navInteraction.collectIsFocusedAsState()

                        NavigationRailItem(
                            selected = isSelected,
                            onClick = { viewModel.navigateTo(screen) },
                            interactionSource = navInteraction,
                            alwaysShowLabel = true,
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isNavFocused) PureWhite else if (isSelected) AccentCyan else Slate400,
                                    modifier = Modifier.size(if (isNavFocused) 24.dp else 20.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected || isNavFocused) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isNavFocused) PureWhite else if (isSelected) AccentCyan else Slate400,
                                    maxLines = 1
                                )
                            },
                            colors = navigationRailItemColors(),
                            modifier = Modifier
                                .padding(vertical = 2.dp)
                                .border(
                                    width = if (isNavFocused) 2.dp else 0.dp,
                                    color = if (isNavFocused) AccentCyan else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        screenContent()
                    }

                    if (!isPlayerOpen && playerUiState.currentItem != null) {
                        MiniPlayerBar(
                            playerManager = viewModel.playerManager,
                            item = playerUiState.currentItem!!,
                            isPlaying = playerUiState.isPlaying,
                            isCompact = !isTablet,
                            onExpand = { viewModel.navigateTo(Screen.PLAYER) },
                            onTogglePlayPause = { viewModel.playerManager.togglePlayPause() },
                            onDismiss = { viewModel.dismissMiniPlayer() }
                        )
                    }
                }
            }
        } else {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Slate950),
                bottomBar = {
                    Column(modifier = Modifier.navigationBarsPadding()) {
                        if (!isPlayerOpen && playerUiState.currentItem != null) {
                            MiniPlayerBar(
                                playerManager = viewModel.playerManager,
                                item = playerUiState.currentItem!!,
                                isPlaying = playerUiState.isPlaying,
                                isCompact = false,
                                onExpand = { viewModel.navigateTo(Screen.PLAYER) },
                                onTogglePlayPause = { viewModel.playerManager.togglePlayPause() },
                                onDismiss = { viewModel.dismissMiniPlayer() }
                            )
                        }

                        NavigationBar(
                            containerColor = Slate900,
                            contentColor = PureWhite,
                            modifier = Modifier
                                .height(68.dp)
                                .focusGroup()
                                .border(BorderStroke(1.dp, Slate800))
                                .testTag("main_navigation_bar")
                        ) {
                            navItems.forEach { (screen, icon, label) ->
                                val isSelected = displayedBrowseScreen == screen || (screen == Screen.SERIES && displayedBrowseScreen == Screen.SERIES_DETAIL)
                                val navInteraction = remember { MutableInteractionSource() }
                                val isNavFocused by navInteraction.collectIsFocusedAsState()

                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { viewModel.navigateTo(screen) },
                                    interactionSource = navInteraction,
                                    alwaysShowLabel = true,
                                    icon = {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = label,
                                            tint = if (isNavFocused) PureWhite else if (isSelected) AccentCyan else Slate400,
                                            modifier = Modifier.size(if (isNavFocused) 24.dp else 20.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = label,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected || isNavFocused) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isNavFocused) PureWhite else if (isSelected) AccentCyan else Slate400,
                                            maxLines = 1
                                        )
                                    },
                                    colors = navigationBarItemColors(),
                                    modifier = Modifier
                                        .border(
                                            width = if (isNavFocused) 2.dp else 0.dp,
                                            color = if (isNavFocused) AccentCyan else Color.Transparent,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .statusBarsPadding()
                ) {
                    screenContent()
                }
            }
        }

        if (isPlayerOpen) {
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
                onBack = { performBack() },
                onPlayNext = { viewModel.playNextChannel() },
                onPlayPrevious = { viewModel.playPreviousChannel() },
                onSelectChannel = { handlePlayWithResumePrompt(it) },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (pendingResumeItem != null) {
            val targetItem = pendingResumeItem!!
            val formattedTime = remember(pendingResumePosMs) {
                val totalSeconds = (pendingResumePosMs / 1000).coerceAtLeast(0)
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60
                val hours = minutes / 60
                if (hours > 0) {
                    String.format(Locale.US, "%d:%02d:%02d", hours, minutes % 60, seconds)
                } else {
                    String.format(Locale.US, "%02d:%02d", minutes, seconds)
                }
            }

            val resumeFocusRequester = remember { FocusRequester() }
            val resumeInteraction = remember { MutableInteractionSource() }
            val isResumeFocused by resumeInteraction.collectIsFocusedAsState()

            val restartInteraction = remember { MutableInteractionSource() }
            val isRestartFocused by restartInteraction.collectIsFocusedAsState()

            LaunchedEffect(targetItem.id) {
                try {
                    delay(80L)
                    resumeFocusRequester.requestFocus()
                } catch (_: Exception) {}
            }

            AlertDialog(
                onDismissRequest = { pendingResumeItem = null },
                containerColor = Slate900,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Text(
                        text = if (isAr) "استئناف المشاهدة؟" else "Resume Playback?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = targetItem.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentCyan,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isAr) {
                                "توقفت عند الدقيقة $formattedTime. هل تريد إكمال المشاهدة أو البدء من البداية؟"
                            } else {
                                "You left off at $formattedTime. Would you like to resume or start from the beginning?"
                            },
                            fontSize = 13.sp,
                            color = Slate300
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                val itemToPlay = targetItem
                                pendingResumeItem = null
                                viewModel.playStream(itemToPlay)
                            },
                            interactionSource = resumeInteraction,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isResumeFocused) PureWhite else AccentCyan,
                                contentColor = Slate950
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .focusRequester(resumeFocusRequester)
                                .border(
                                    width = if (isResumeFocused) 3.dp else 0.dp,
                                    color = PureWhite,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .focusable(interactionSource = resumeInteraction)
                                .onPreviewKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyUp &&
                                        (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                    ) {
                                        val itemToPlay = targetItem
                                        pendingResumeItem = null
                                        viewModel.playStream(itemToPlay)
                                        true
                                    } else false
                                }
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isAr) "استئناف من $formattedTime" else "Resume from $formattedTime",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                val itemToPlay = targetItem
                                pendingResumeItem = null
                                viewModel.playerManager.clearSavedPosition(itemToPlay)
                                viewModel.playStream(itemToPlay)
                            },
                            interactionSource = restartInteraction,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                width = if (isRestartFocused) 2.5.dp else 1.dp,
                                color = if (isRestartFocused) AccentCyan else Slate700
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isRestartFocused) Slate800 else Slate850,
                                contentColor = PureWhite
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .focusable(interactionSource = restartInteraction)
                                .onPreviewKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyUp &&
                                        (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                    ) {
                                        val itemToPlay = targetItem
                                        pendingResumeItem = null
                                        viewModel.playerManager.clearSavedPosition(itemToPlay)
                                        viewModel.playStream(itemToPlay)
                                        true
                                    } else false
                                }
                        ) {
                            Icon(Icons.Default.Replay, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isAr) "البدء من البداية (00:00)" else "Start from Beginning (00:00)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { pendingResumeItem = null }) {
                        Text(
                            text = if (isAr) "إلغاء" else "Cancel",
                            color = Slate400,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        }

        if (showExitDialog) {
            val stayFocusRequester = remember { FocusRequester() }
            val stayInteraction = remember { MutableInteractionSource() }
            val isStayFocused by stayInteraction.collectIsFocusedAsState()

            val exitInteraction = remember { MutableInteractionSource() }
            val isExitFocused by exitInteraction.collectIsFocusedAsState()

            LaunchedEffect(Unit) {
                try {
                    delay(80L)
                    stayFocusRequester.requestFocus()
                } catch (_: Exception) {}
            }

            AlertDialog(
                onDismissRequest = { onShowExitDialogChange(false) },
                containerColor = Slate900,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = null,
                            tint = AccentRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = if (isAr) "الخروج من التطبيق؟" else "Exit HypnoStream?",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = if (isAr) {
                                "هل أنت متأكد من إغلاق التطبيق وإيقاف البث؟"
                            } else {
                                "Are you sure you want to exit the application and stop playback?"
                            },
                            fontSize = 14.sp,
                            color = Slate300
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Button(
                            onClick = { onShowExitDialogChange(false) },
                            interactionSource = stayInteraction,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isStayFocused) PureWhite else AccentCyan,
                                contentColor = Slate950
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .focusRequester(stayFocusRequester)
                                .border(
                                    width = if (isStayFocused) 3.dp else 0.dp,
                                    color = PureWhite,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .focusable(interactionSource = stayInteraction)
                                .onPreviewKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyUp &&
                                        (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                    ) {
                                        onShowExitDialogChange(false)
                                        true
                                    } else false
                                }
                        ) {
                            Text(
                                text = if (isAr) "البقاء في التطبيق" else "Stay in App",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                onShowExitDialogChange(false)
                                onFinishApp()
                            },
                            interactionSource = exitInteraction,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                width = if (isExitFocused) 2.5.dp else 1.5.dp,
                                color = if (isExitFocused) PureWhite else AccentRed
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isExitFocused) AccentRed else Slate850,
                                contentColor = PureWhite
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .focusable(interactionSource = exitInteraction)
                                .onPreviewKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyUp &&
                                        (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                    ) {
                                        onShowExitDialogChange(false)
                                        onFinishApp()
                                        true
                                    } else false
                                }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = null,
                                tint = PureWhite,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isAr) "خروج وإيقاف البث" else "Exit & Stop Playback",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                },
                confirmButton = {}
            )
        }

        AnimatedVisibility(
            visible = showSplash,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(650))
        ) {
            HypnoOrbitalSplashOverlay(
                isAr = isAr,
                statusMessage = statusMessage,
                totalLoadedItems = totalLoadedItems
            )
        }
    }
}

@Composable
fun WatchHistoryScreen(
    items: List<IptvItem>,
    onPlayItem: (IptvItem) -> Unit,
    onRemoveItem: (String) -> Unit,
    onClearAll: () -> Unit,
    onBack: () -> Unit
) {
    val isAr = LocalAppLanguage.current == "ar"
    var selectedFilter by remember { mutableStateOf("ALL") }

    val vodItems = remember(items) {
        items.filter { it.type != IptvType.LIVE_TV }
    }

    val filteredItems = remember(vodItems, selectedFilter) {
        when (selectedFilter) {
            "MOVIES" -> vodItems.filter { it.type == IptvType.MOVIE }
            "SERIES" -> vodItems.filter { it.type == IptvType.SERIES }
            else -> vodItems
        }
    }

    val filterTabs = listOf(
        "ALL" to (if (isAr) "الكل (${vodItems.size})" else "All (${vodItems.size})"),
        "SERIES" to (if (isAr) "مسلسلات" else "Series"),
        "MOVIES" to (if (isAr) "أفلام" else "Movies")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Slate850)
                        .border(1.dp, Slate700, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = PureWhite
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isAr) "سجل المشاهدة" else "Watch History",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = PureWhite
                )
            }

            if (vodItems.isNotEmpty()) {
                OutlinedButton(
                    onClick = onClearAll,
                    border = BorderStroke(1.dp, AccentRed),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isAr) "مسح الكل" else "Clear All",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filterTabs) { (key, label) ->
                val isSelected = selectedFilter == key
                Surface(
                    onClick = { selectedFilter = key },
                    color = if (isSelected) AccentCyan else Slate850,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (isSelected) PureWhite else Slate700)
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Slate950 else Slate300,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }

        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isAr) "لا يوجد سجل مشاهدة للأفلام أو المسلسلات بعد" else "No movie or series watch history yet",
                    color = Slate400,
                    fontSize = 15.sp
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 145.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredItems, key = { "${it.id}_${it.streamUrl}" }) { item ->
                    HistoryPosterCard(
                        item = item,
                        isAr = isAr,
                        onClick = { onPlayItem(item) },
                        onRemove = { onRemoveItem(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryPosterCard(
    item: IptvItem,
    isAr: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val cardInteraction = remember { MutableInteractionSource() }
    val isCardFocused by cardInteraction.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isCardFocused) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "historyPosterScale"
    )

    val isSeries = item.type == IptvType.SERIES
    val badgeText = if (isSeries) {
        if (isAr) "مسلسل" else "SERIES"
    } else {
        if (isAr) "فيلم" else "MOVIE"
    }
    val badgeColor = if (isSeries) Color(0xFF0284C7) else Color(0xFF7C3AED)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2f / 3f)
            .scale(scale)
            .shadow(
                elevation = if (isCardFocused) 14.dp else 4.dp,
                shape = RoundedCornerShape(14.dp),
                spotColor = if (isCardFocused) AccentCyan else Color.Transparent
            )
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (isCardFocused) 3.dp else 1.dp,
                color = if (isCardFocused) AccentCyan else Slate800,
                shape = RoundedCornerShape(14.dp)
            )
            .focusable(interactionSource = cardInteraction)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                ) {
                    onClick()
                    true
                } else false
            }
            .clickable(interactionSource = cardInteraction, indication = null, onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!item.logoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.logoUrl,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Slate800, Slate900, Slate950)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSeries) Icons.Default.VideoLibrary else Icons.Default.Movie,
                        contentDescription = null,
                        tint = Slate400,
                        modifier = Modifier.size(42.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x44000000),
                                Color.Transparent,
                                Color(0xCC060A12),
                                Color(0xF5060A12)
                            )
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeColor.copy(alpha = 0.92f))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = badgeText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = PureWhite,
                    letterSpacing = 0.5.sp
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xAA000000))
                    .clickable(onClick = onRemove)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = PureWhite,
                    modifier = Modifier.size(15.dp)
                )
            }

            if (isCardFocused) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(AccentCyan)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Slate950,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Text(
                    text = item.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = item.group,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = AccentCyan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun HypnoOrbitalSplashOverlay(
    isAr: Boolean,
    statusMessage: String?,
    totalLoadedItems: Int
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hypnoSplash")

    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbitRotation"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val barPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "barPhase"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF071A3E),
                        Color(0xFF030917),
                        Color(0xFF000000)
                    ),
                    radius = 950f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(210.dp)
                    .clip(RoundedCornerShape(46.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF071C44),
                                Color(0xFF030B1B),
                                Color(0xFF051533)
                            )
                        )
                    )
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF00E5FF),
                                Color(0xFF1D4ED8),
                                Color(0xFF00E5FF)
                            )
                        ),
                        shape = RoundedCornerShape(46.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(170.dp)) {
                    val w = size.width
                    val h = size.height
                    val cx = w / 2f
                    val cy = h / 2f

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0x4400E5FF),
                                Color.Transparent
                            ),
                            center = Offset(cx, cy),
                            radius = w * 0.55f
                        ),
                        radius = w * 0.55f,
                        center = Offset(cx, cy)
                    )

                    rotate(degrees = -18f, pivot = Offset(cx, cy)) {
                        val ovalTopLeft = Offset(w * 0.04f, h * 0.30f)
                        val ovalSize = Size(w * 0.92f, h * 0.40f)

                        drawArc(
                            color = Color(0xFF0284C7).copy(alpha = 0.65f),
                            startAngle = 180f,
                            sweepAngle = 180f,
                            useCenter = false,
                            topLeft = ovalTopLeft,
                            size = ovalSize,
                            style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    val tvWidth = w * 0.62f
                    val tvHeight = h * 0.48f
                    val tvLeft = cx - (tvWidth / 2f)
                    val tvTop = cy - (tvHeight / 2f) - 6.dp.toPx()

                    drawRoundRect(
                        color = Color(0xFF020712),
                        topLeft = Offset(tvLeft, tvTop),
                        size = Size(tvWidth, tvHeight),
                        cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                    )

                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF00E5FF), Color(0xFF2979FF), Color(0xFF00E5FF))
                        ),
                        topLeft = Offset(tvLeft, tvTop),
                        size = Size(tvWidth, tvHeight),
                        cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                        style = Stroke(width = 5.5.dp.toPx())
                    )

                    val barY = tvTop + 13.dp.toPx()
                    val barWidth = tvWidth * 0.22f
                    val barSpacing = tvWidth * 0.06f
                    val startX = cx - ((barWidth * 3 + barSpacing * 2) / 2f)

                    val redAlpha = if (barPhase.toInt() == 0) 1f else 0.72f
                    val purpleAlpha = if (barPhase.toInt() == 1) 1f else 0.72f
                    val cyanAlpha = if (barPhase.toInt() == 2) 1f else 0.72f

                    drawLine(
                        color = Color(0xFFFF2A54).copy(alpha = redAlpha),
                        start = Offset(startX, barY),
                        end = Offset(startX + barWidth, barY),
                        strokeWidth = 6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color(0xFF8B5CF6).copy(alpha = purpleAlpha),
                        start = Offset(startX + barWidth + barSpacing, barY),
                        end = Offset(startX + barWidth * 2 + barSpacing, barY),
                        strokeWidth = 6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color(0xFF00E5FF).copy(alpha = cyanAlpha),
                        start = Offset(startX + (barWidth + barSpacing) * 2, barY),
                        end = Offset(startX + barWidth * 3 + barSpacing * 2, barY),
                        strokeWidth = 6.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    val playCenterY = tvTop + (tvHeight * 0.58f)
                    scale(scale = pulseScale, pivot = Offset(cx, playCenterY)) {
                        val triSize = 24.dp.toPx()
                        val playPath = Path().apply {
                            moveTo(cx - triSize * 0.42f, playCenterY - triSize * 0.52f)
                            lineTo(cx - triSize * 0.42f, playCenterY + triSize * 0.52f)
                            lineTo(cx + triSize * 0.58f, playCenterY)
                            close()
                        }
                        drawPath(
                            path = playPath,
                            brush = Brush.linearGradient(
                                colors = listOf(Color.White, Color(0xFF80D8FF))
                            )
                        )
                        drawPath(
                            path = playPath,
                            color = Color(0xFF00E5FF),
                            style = Stroke(width = 1.5.dp.toPx(), join = StrokeJoin.Round)
                        )
                    }

                    val standTop = tvTop + tvHeight
                    val standBottom = standTop + 11.dp.toPx()
                    drawLine(
                        color = Color(0xFF00B0FF),
                        start = Offset(cx, standTop),
                        end = Offset(cx, standBottom),
                        strokeWidth = 6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF2979FF), Color(0xFF00E5FF), Color(0xFF2979FF))
                        ),
                        start = Offset(cx - 22.dp.toPx(), standBottom),
                        end = Offset(cx + 22.dp.toPx(), standBottom),
                        strokeWidth = 5.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    rotate(degrees = -18f, pivot = Offset(cx, cy)) {
                        val ovalTopLeft = Offset(w * 0.04f, h * 0.30f)
                        val ovalSize = Size(w * 0.92f, h * 0.40f)

                        drawArc(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF00E5FF),
                                    Color(0xFF7CE8FF),
                                    Color(0xFF00B0FF)
                                )
                            ),
                            startAngle = 0f,
                            sweepAngle = 180f,
                            useCenter = false,
                            topLeft = ovalTopLeft,
                            size = ovalSize,
                            style = Stroke(width = 8.5.dp.toPx(), cap = StrokeCap.Round)
                        )

                        drawArc(
                            color = Color.White,
                            startAngle = orbitAngle,
                            sweepAngle = 55f,
                            useCenter = false,
                            topLeft = ovalTopLeft,
                            size = ovalSize,
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = PureWhite, fontWeight = FontWeight.Black)) {
                        append("Hypno")
                    }
                    withStyle(SpanStyle(color = Color(0xFF00E5FF), fontWeight = FontWeight.Black)) {
                        append("Stream")
                    }
                },
                fontSize = 34.sp,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            val displayStatus = when {
                totalLoadedItems > 0 -> if (isAr) {
                    "تم تجهيز %,d قناة وفيلم ومسلسل...".format(totalLoadedItems)
                } else {
                    "Ready! Loaded %,d channels, movies & series...".format(totalLoadedItems)
                }
                !statusMessage.isNullOrBlank() -> statusMessage
                else -> if (isAr) {
                    "جاري الاتصال وتجهيز قنواتك..."
                } else {
                    "Connecting & preparing your entertainment..."
                }
            }

            Text(
                text = displayStatus,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Slate300,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(24.dp))

            LinearProgressIndicator(
                color = Color(0xFF00E5FF),
                trackColor = Slate800,
                modifier = Modifier
                    .width(190.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun MiniPlayerBar(
    playerManager: ExoPlayerManager,
    item: IptvItem,
    isPlaying: Boolean,
    isCompact: Boolean = false,
    onExpand: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onDismiss: () -> Unit
) {
    val barInteraction = remember { MutableInteractionSource() }
    val isBarFocused by barInteraction.collectIsFocusedAsState()
    val playBtnInteraction = remember { MutableInteractionSource() }
    val isPlayBtnFocused by playBtnInteraction.collectIsFocusedAsState()
    val closeBtnInteraction = remember { MutableInteractionSource() }
    val isCloseBtnFocused by closeBtnInteraction.collectIsFocusedAsState()

    val currentLanguage = LocalAppLanguage.current
    val isAr = currentLanguage == "ar"
    val nowPlayingText = if (isAr) "يعرض الآن" else "Now Playing"

    val previewWidth = if (isCompact) 112.dp else 124.dp
    val previewHeight = if (isCompact) 62.dp else 70.dp

    Surface(
        color = if (isBarFocused) Slate800 else Slate900,
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
        border = BorderStroke(
            width = if (isBarFocused) 2.dp else 1.dp,
            color = if (isBarFocused) AccentCyan else Slate700
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mini_player_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = if (isCompact) 6.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .focusable(interactionSource = barInteraction)
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyUp &&
                            (event.key == Key.DirectionCenter ||
                             event.key == Key.Enter ||
                             event.key == Key.NumPadEnter)
                        ) {
                            onExpand()
                            true
                        } else false
                    }
                    .clickable(interactionSource = barInteraction, indication = null, onClick = onExpand)
            ) {
                Box(
                    modifier = Modifier
                        .width(previewWidth)
                        .height(previewHeight)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black)
                        .border(1.5.dp, AccentCyan.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = playerManager.player
                                useController = false
                                isFocusable = false
                                isFocusableInTouchMode = false
                                keepScreenOn = true
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
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
                        }
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) AccentRed else Slate400)
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(3.dp)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color(0xAA000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Expand",
                            tint = PureWhite,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        fontSize = if (isCompact) 13.sp else 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$nowPlayingText • ${item.group}",
                        fontSize = 11.sp,
                        color = AccentCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        text = if (isAr) "اضغط للتكبير ملء الشاشة" else "Tap video to expand fullscreen",
                        fontSize = 9.sp,
                        color = Slate400,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isPlayBtnFocused) PureWhite else AccentCyan)
                        .border(
                            width = if (isPlayBtnFocused) 2.5.dp else 0.dp,
                            color = PureWhite,
                            shape = CircleShape
                        )
                        .focusable(interactionSource = playBtnInteraction)
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyUp &&
                                (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                            ) {
                                onTogglePlayPause()
                                true
                            } else false
                        }
                        .clickable(interactionSource = playBtnInteraction, indication = null, onClick = onTogglePlayPause)
                        .testTag("btn_mini_player_play_pause")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Slate950,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isCloseBtnFocused) AccentCyan else Slate800)
                        .border(
                            width = if (isCloseBtnFocused) 2.dp else 1.dp,
                            color = if (isCloseBtnFocused) PureWhite else Slate700,
                            shape = CircleShape
                        )
                        .focusable(interactionSource = closeBtnInteraction)
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyUp &&
                                (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                            ) {
                                onDismiss()
                                true
                            } else false
                        }
                        .clickable(interactionSource = closeBtnInteraction, indication = null, onClick = onDismiss)
                        .testTag("btn_mini_player_dismiss")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Mini Player",
                        tint = if (isCloseBtnFocused) Slate950 else PureWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun navigationBarItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = AccentCyan,
    selectedTextColor = AccentCyan,
    unselectedIconColor = Slate400,
    unselectedTextColor = Slate400,
    indicatorColor = Slate800
)

@Composable
private fun navigationRailItemColors() = NavigationRailItemDefaults.colors(
    selectedIconColor = AccentCyan,
    selectedTextColor = AccentCyan,
    unselectedIconColor = Slate400,
    unselectedTextColor = Slate400,
    indicatorColor = Slate800
)
