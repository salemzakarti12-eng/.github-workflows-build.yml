package com.example

import android.app.PictureInPictureParams
import android.content.Context
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
import androidx.compose.animation.core.CubicBezierEasing
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
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.TouchApp
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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
import com.example.ui.epg.EpgGuideScreen
import com.example.ui.locale.AppLanguageManager
import com.example.ui.locale.LocalAppLanguage
import com.example.ui.locale.LocalAppStrings
import com.example.ui.locale.getAppStrings
import com.example.ui.locale.isRtlLanguage
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
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private var isInPipModeState by mutableStateOf(false)
    private var showExitDialogState by mutableStateOf(false)
    private var lastBrowseScreenState by mutableStateOf(Screen.DASHBOARD)
    private var previousScreenBeforeSeriesDetailState by mutableStateOf(Screen.SERIES)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

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

        val isDexDevice = try {
            DeviceUtils.isSamsungDexMode(this)
        } catch (_: Exception) {
            false
        }

        try {
            DeviceUtils.configureDeviceOrientation(this)
        } catch (_: Exception) {}

        if (!isTvDevice && !isDexDevice && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                enableEdgeToEdge()
            } catch (_: Exception) {}
        }

        if (!isDexDevice) {
            hideSystemStatusBar()
        }

        setContent {
            val isTelevision = remember { isTvDevice }
            val currentLanguage by mainViewModel.currentLanguage.collectAsState()
            val layoutDirection = if (isRtlLanguage(currentLanguage)) LayoutDirection.Rtl else LayoutDirection.Ltr
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
                        onSeriesDetailOpenedFrom = { origin ->
                            previousScreenBeforeSeriesDetailState = origin
                        },
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

    private fun hideSystemStatusBar() {
        try {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, window.decorView).apply {
                hide(WindowInsetsCompat.Type.statusBars())
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } catch (_: Exception) {}
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && !DeviceUtils.isSamsungDexMode(this)) {
            hideSystemStatusBar()
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
                val returnTarget = when (previousScreenBeforeSeriesDetailState) {
                    Screen.HISTORY, Screen.DASHBOARD, Screen.SERIES -> previousScreenBeforeSeriesDetailState
                    else -> Screen.SERIES
                }
                mainViewModel.navigateTo(returnTarget)
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
        if (!DeviceUtils.isSamsungDexMode(this)) {
            hideSystemStatusBar()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        try {
            if (DeviceUtils.isSamsungDexMode(this)) return

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
        if (DeviceUtils.isTelevision(this) || DeviceUtils.isSamsungDexMode(this)) return
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
    onSeriesDetailOpenedFrom: (Screen) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onFinishApp: () -> Unit = {}
) {
    val context = LocalContext.current
    val onboardingPrefs = remember {
        context.getSharedPreferences("hypnostream_prefs", Context.MODE_PRIVATE)
    }
    var showFirstRunOnboarding by remember {
        mutableStateOf(!onboardingPrefs.getBoolean("has_completed_onboarding", false))
    }

    val currentScreen by viewModel.currentScreen.collectAsState()
    var lastBrowseScreen by remember { mutableStateOf(Screen.DASHBOARD) }
    var lastPlayingSourceScreen by remember { mutableStateOf<Screen?>(null) }
    var lastPlayingSourceCategory by remember { mutableStateOf<String?>(null) }

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
    val liveCategories by viewModel.liveCategories.collectAsState()
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

    val favoriteItems = remember(recentlyWatched, favorites) {
        recentlyWatched.filter { item ->
            favorites.contains("${item.type.name}::${item.id}") || favorites.contains(item.id)
        }
    }

    val liveFavCount = remember(favorites, totalLiveCount) {
        val scoped = favorites.count { it.startsWith("${IptvType.LIVE_TV.name}::") }
        if (scoped > 0 || favorites.any { it.contains("::") }) scoped
        else viewModel.repository.getAllLiveChannels().count { favorites.contains(it.id) }
    }

    val movieFavCount = remember(favorites, totalMoviesCount) {
        val scoped = favorites.count { it.startsWith("${IptvType.MOVIE.name}::") }
        if (scoped > 0 || favorites.any { it.contains("::") }) scoped
        else viewModel.repository.getAllMovies().count { favorites.contains(it.id) }
    }

    val seriesFavCount = remember(favorites, totalSeriesCount) {
        val scoped = favorites.count { it.startsWith("${IptvType.SERIES.name}::") }
        if (scoped > 0 || favorites.any { it.contains("::") }) scoped
        else viewModel.repository.getAllSeries().count { favorites.contains(it.id) }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isTablet = configuration.smallestScreenWidthDp >= 600
    val isTv = LocalIsTelevision.current
    val useSideRailLayout = isLandscape || isTablet || isTv

    val strings = LocalAppStrings.current
    val currentLang = LocalAppLanguage.current
    val isAr = currentLang == "ar"

    var pendingResumeItem by remember { mutableStateOf<IptvItem?>(null) }
    var pendingResumePosMs by remember { mutableLongStateOf(0L) }

    val performBack = {
        if (pendingResumeItem != null) {
            pendingResumeItem = null
        } else {
            onNavigateBack()
        }
    }

    val startPlaybackAndTrackOrigin = { item: IptvItem ->
        val originScreen = when (currentScreen) {
            Screen.SERIES_DETAIL -> Screen.SERIES_DETAIL
            Screen.LIVE_CHANNELS, Screen.MOVIES, Screen.SERIES, Screen.EPG_GUIDE -> currentScreen
            else -> when (item.type) {
                IptvType.SERIES -> Screen.SERIES_DETAIL
                IptvType.MOVIE -> Screen.MOVIES
                IptvType.LIVE_TV -> Screen.LIVE_CHANNELS
            }
        }
        lastPlayingSourceScreen = originScreen
        lastPlayingSourceCategory = if (currentScreen in listOf(Screen.LIVE_CHANNELS, Screen.MOVIES, Screen.SERIES)) {
            selectedCategory
        } else if (item.type != IptvType.SERIES && item.group.isNotBlank()) {
            item.group
        } else {
            null
        }
        viewModel.playStream(item)
    }

    val handlePlayWithResumePrompt = { item: IptvItem ->
        val savedPos = viewModel.playerManager.getSavedPosition(item)
        if (savedPos > 5_000L && item.type != IptvType.LIVE_TV) {
            pendingResumeItem = item
            pendingResumePosMs = savedPos
        } else {
            startPlaybackAndTrackOrigin(item)
        }
    }

    val handleExpandMiniPlayer = {
        val activeItem = playerUiState.currentItem
        if (activeItem != null) {
            val targetReturnScreen = when {
                activeItem.type == IptvType.SERIES -> {
                    if (seriesDetails == null) {
                        val matchedSeries = viewModel.repository.getAllSeries().firstOrNull {
                            it.name.equals(activeItem.seriesTitle ?: activeItem.name, ignoreCase = true) ||
                            (!activeItem.seriesTitle.isNullOrBlank() && it.seriesTitle?.equals(activeItem.seriesTitle, ignoreCase = true) == true)
                        }
                        if (matchedSeries != null) {
                            viewModel.openSeriesDetail(matchedSeries)
                        }
                    }
                    Screen.SERIES_DETAIL
                }
                lastPlayingSourceScreen != null && lastPlayingSourceScreen != Screen.PLAYER -> {
                    lastPlayingSourceScreen!!
                }
                activeItem.type == IptvType.MOVIE -> Screen.MOVIES
                else -> Screen.LIVE_CHANNELS
            }

            if (targetReturnScreen in listOf(Screen.LIVE_CHANNELS, Screen.MOVIES, Screen.SERIES)) {
                val targetCat = lastPlayingSourceCategory ?: activeItem.group.takeIf { it.isNotBlank() }
                if (targetCat != null) {
                    viewModel.setSelectedCategory(targetCat)
                }
            }

            lastBrowseScreen = targetReturnScreen
            onLastBrowseScreenChange(targetReturnScreen)
        }
        viewModel.navigateTo(Screen.PLAYER)
    }

    val handleOpenHistoryOrDashboardItem = { item: IptvItem ->
        if (item.type == IptvType.SERIES) {
            onSeriesDetailOpenedFrom(currentScreen)
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
        delay(900L)
        minSplashTimePassed = true
        delay(10000L)
        initialLoadCompleted = true
    }

    LaunchedEffect(minSplashTimePassed, isLoading, totalLoadedItems) {
        if (minSplashTimePassed && (!isLoading && (totalLoadedItems > 0 || activeSource == null))) {
            delay(150L)
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
            Triple(Screen.DASHBOARD, Icons.Default.Home, strings.home),
            Triple(Screen.LIVE_CHANNELS, Icons.Default.LiveTv, strings.liveTv),
            Triple(Screen.EPG_GUIDE, Icons.Default.DateRange, if (isAr) "الدليل" else if (useSideRailLayout) "TV Guide" else "Guide"),
            Triple(Screen.MOVIES, Icons.Default.Movie, strings.movies),
            Triple(Screen.SERIES, Icons.Default.VideoLibrary, strings.series),
            Triple(Screen.HISTORY, Icons.Default.History, strings.history),
            Triple(Screen.SOURCES, Icons.Default.Settings, strings.sources)
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
                        onToggleLanguage = { viewModel.toggleLanguage() },
                        onSelectLanguage = { viewModel.setLanguage(it) },
                        isRefreshing = isLoading,
                        statusMessage = statusMessage,
                        onRefreshSource = { viewModel.refreshActiveSource() }
                    )
                }

                Screen.EPG_GUIDE -> {
                    EpgGuideScreen(
                        channels = viewModel.repository.getAllLiveChannels(),
                        categories = liveCategories,
                        onPlayChannel = { handlePlayWithResumePrompt(it) },
                        onBack = { performBack() }
                    )
                }

                Screen.HISTORY -> {
                    WatchHistoryScreen(
                        items = recentlyWatched,
                        favorites = favorites,
                        onPlayItem = handleOpenHistoryOrDashboardItem,
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
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
                        favoriteCount = when (displayedBrowseScreen) {
                            Screen.LIVE_CHANNELS -> liveFavCount
                            Screen.MOVIES -> movieFavCount
                            Screen.SERIES -> seriesFavCount
                            else -> favorites.size
                        },
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onCategoryChange = { viewModel.setSelectedCategory(it) },
                        onLoadMore = { viewModel.loadMoreItems() },
                        onPlayItem = { handlePlayWithResumePrompt(it) },
                        onOpenSeries = { seriesItem ->
                            onSeriesDetailOpenedFrom(Screen.SERIES)
                            viewModel.openSeriesDetail(seriesItem)
                        },
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
                        onToggleLanguage = { viewModel.toggleLanguage() },
                        onSelectLanguage = { viewModel.setLanguage(it) }
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
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
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
                            onExpand = handleExpandMiniPlayer,
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
                                onExpand = handleExpandMiniPlayer,
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
                                            fontSize = 9.sp,
                                            fontWeight = if (isSelected || isNavFocused) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isNavFocused) PureWhite else if (isSelected) AccentCyan else Slate400,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Clip
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
                        .displayCutoutPadding()
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
                                startPlaybackAndTrackOrigin(itemToPlay)
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
                                        startPlaybackAndTrackOrigin(itemToPlay)
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
                                startPlaybackAndTrackOrigin(itemToPlay)
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
                                        startPlaybackAndTrackOrigin(itemToPlay)
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

        if (!showSplash && showFirstRunOnboarding) {
            FirstRunLanguageAndTutorialOverlay(
                selectedLanguage = currentLang,
                onSelectLanguage = { langCode -> viewModel.setLanguage(langCode) },
                onComplete = {
                    onboardingPrefs.edit().putBoolean("has_completed_onboarding", true).apply()
                    showFirstRunOnboarding = false
                }
            )
        }

        AnimatedVisibility(
            visible = showSplash,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(420)) + scaleOut(
                targetScale = 1.08f,
                animationSpec = tween(420, easing = FastOutSlowInEasing)
            )
        ) {
            HypnoOrbitalSplashOverlay(
                isAr = isAr,
                statusMessage = statusMessage,
                totalLoadedItems = totalLoadedItems
            )
        }
    }
}

private data class TutorialCardData(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val highlightTip: String
)

private data class TupleTutorial(
    val chooseLangTitle: String,
    val chooseLangSubtitle: String,
    val skipLabel: String,
    val nextLabel: String,
    val backLabel: String,
    val startLabel: String,
    val cards: List<TutorialCardData>
)

private fun getTutorialContentForLanguage(langCode: String): TupleTutorial {
    return when (langCode.lowercase(Locale.US)) {
        "ar" -> TupleTutorial(
            chooseLangTitle = "اختر لغة العرض المفضلة",
            chooseLangSubtitle = "سيتم ضبط واجهة التطبيق وترجمة الأقسام والدليل التعريفي حسب اختيارك",
            skipLabel = "تخطي",
            nextLabel = "التالي",
            backLabel = "السابق",
            startLabel = "ابدأ المشاهدة الآن",
            cards = listOf(
                TutorialCardData(
                    icon = Icons.Default.LiveTv,
                    title = "إضافة اشتراكك ودليل البرامج",
                    description = "أضف حساب Xtream Codes أو رابط M3U من قسم (المصادر)، وسيقوم التطبيق بترجمة الأقسام وتجهيز جدول البرامج الشامل (TV Guide) تلقائياً.",
                    highlightTip = "تصفح جدول البرامج الزمني بالكامل واضغط على أي برنامج للتشغيل الفوري"
                ),
                TutorialCardData(
                    icon = Icons.Default.TouchApp,
                    title = "تحكم سريع باللمس والريموت",
                    description = "اسحب للأعلى والأسفل على يسار الشاشة للسطوع وعلى اليمين للصوت، واضغط مطولاً على أي قناة أو فيلم أو مسلسل لإضافته للمفضلة.",
                    highlightTip = "افتح القائمة الجانبية داخل المشغل للتبديل السريع بين القنوات والحلقات"
                ),
                TutorialCardData(
                    icon = Icons.Default.Subtitles,
                    title = "ترجمة احترافية وتذكر تلقائي",
                    description = "ارفع أو اخفض مكان الترجمة حياً على الشاشة، واختر خطك المفضل. التطبيق يتذكر لغة الصوت والترجمة ونقطة توقفك بين الحلقات تلقائياً.",
                    highlightTip = "اختيارك للصوت والترجمة يستمر معك في جميع الحلقات التالية"
                )
            )
        )
        "fr" -> TupleTutorial(
            chooseLangTitle = "Choisissez votre langue",
            chooseLangSubtitle = "L'interface, les catégories et ce guide s'adapteront immédiatement",
            skipLabel = "Passer",
            nextLabel = "Suivant",
            backLabel = "Précédent",
            startLabel = "Commencer",
            cards = listOf(
                TutorialCardData(
                    icon = Icons.Default.LiveTv,
                    title = "Ajoutez vos sources & Guide TV",
                    description = "Connectez votre compte Xtream Codes ou lien M3U dans Sources. Les catégories sont traduites et la grille EPG complète se charge automatiquement.",
                    highlightTip = "Parcourez le guide TV horaire et lancez n'importe quelle chaîne en un clic"
                ),
                TutorialCardData(
                    icon = Icons.Default.TouchApp,
                    title = "Gestes tactiles & Télécommande TV",
                    description = "Glissez à gauche pour la luminosité, à droite pour le volume, et appuyez longuement sur un élément pour l'ajouter aux favoris.",
                    highlightTip = "Ouvrez le tiroir latéral pour changer de chaîne ou d'épisode"
                ),
                TutorialCardData(
                    icon = Icons.Default.Subtitles,
                    title = "Sous-titres & Mémoire automatique",
                    description = "Ajustez la position et la police des sous-titres en direct. Vos préférences audio, sous-titres et reprise sont mémorisées entre les épisodes.",
                    highlightTip = "Vos choix de langue suivent automatiquement chaque épisode"
                )
            )
        )
        "es" -> TupleTutorial(
            chooseLangTitle = "Elige tu idioma preferido",
            chooseLangSubtitle = "La interfaz, las categorías y esta guía se adaptarán al instante",
            skipLabel = "Omitir",
            nextLabel = "Siguiente",
            backLabel = "Atrás",
            startLabel = "Empezar ahora",
            cards = listOf(
                TutorialCardData(
                    icon = Icons.Default.LiveTv,
                    title = "Conecta tus listas y Guía EPG",
                    description = "Añade tu cuenta Xtream Codes o enlace M3U en Fuentes. Las categorías se traducen y la guía completa de programación se sincroniza automáticamente.",
                    highlightTip = "Explora la parrilla EPG por horarios y reproduce al instante"
                ),
                TutorialCardData(
                    icon = Icons.Default.TouchApp,
                    title = "Control táctil y mando TV",
                    description = "Desliza a la izquierda para el brillo, a la derecha para el volumen, y mantén pulsado cualquier canal, película o serie para añadir a favoritos.",
                    highlightTip = "Usa la lista lateral en el reproductor para cambiar rápido de episodio"
                ),
                TutorialCardData(
                    icon = Icons.Default.Subtitles,
                    title = "Subtítulos y memoria inteligente",
                    description = "Mueve la posición de los subtítulos en vivo y elige tu fuente. La app recuerda tu idioma de audio, subtítulos y punto de reproducción.",
                    highlightTip = "Tu idioma elegido se mantiene en todos los siguientes episodios"
                )
            )
        )
        "de" -> TupleTutorial(
            chooseLangTitle = "Anzeigesprache wählen",
            chooseLangSubtitle = "Oberfläche, Kategorien und diese Einführung werden sofort angepasst",
            skipLabel = "Überspringen",
            nextLabel = "Weiter",
            backLabel = "Zurück",
            startLabel = "Jetzt starten",
            cards = listOf(
                TutorialCardData(
                    icon = Icons.Default.LiveTv,
                    title = "IPTV-Quellen & TV-Programmführer",
                    description = "Fügen Sie Xtream Codes oder M3U-Links unter Quellen hinzu. Kategorien werden übersetzt und das vollständige EPG-Raster automatisch geladen.",
                    highlightTip = "Durchsuchen Sie das komplette TV-Programm und starten Sie Sender direkt"
                ),
                TutorialCardData(
                    icon = Icons.Default.TouchApp,
                    title = "Gesten & TV-Fernbedienung",
                    description = "Links wischen für Helligkeit, rechts für Lautstärke – und lange drücken, um Sender, Filme oder Serien zu den Favoriten hinzuzufügen.",
                    highlightTip = "Seitenmenü im Player für schnellen Sender- und Episodenwechsel"
                ),
                TutorialCardData(
                    icon = Icons.Default.Subtitles,
                    title = "Untertitel & Auto-Speicher",
                    description = "Untertitel-Position und Schriftart live anpassen. Audio- und Untertitelsprache sowie Wiedergabeposition bleiben über Episoden erhalten.",
                    highlightTip = "Ihre Sprachauswahl gilt automatisch für alle weiteren Folgen"
                )
            )
        )
        "tr" -> TupleTutorial(
            chooseLangTitle = "Görüntüleme Dilini Seçin",
            chooseLangSubtitle = "Arayüz, kategoriler ve bu rehber seçiminize göre ayarlanacaktır",
            skipLabel = "Atla",
            nextLabel = "İleri",
            backLabel = "Geri",
            startLabel = "İzlemeye Başla",
            cards = listOf(
                TutorialCardData(
                    icon = Icons.Default.LiveTv,
                    title = "IPTV Kaynakları ve EPG Rehberi",
                    description = "Kaynaklar bölümünden Xtream Codes veya M3U bağlantınızı ekleyin. Kategoriler çevrilir ve tam zamanlı EPG yayın akışı otomatik yüklenir.",
                    highlightTip = "Saatlik yayın akışı tablosundan kanalları anında başlatın"
                ),
                TutorialCardData(
                    icon = Icons.Default.TouchApp,
                    title = "Dokunmatik ve TV Kumanda Kontrolü",
                    description = "Parlaklık için sol, ses için sağ tarafı kaydırın; favorilere eklemek için herhangi bir içeriğe uzun basın.",
                    highlightTip = "Hızlı kanal ve bölüm geçişi için yan çekmeceyi açın"
                ),
                TutorialCardData(
                    icon = Icons.Default.Subtitles,
                    title = "Gelişmiş Altyazı ve Otomatik Hafıza",
                    description = "Altyazı konumunu ve yazı tipini canlı ayarlayın. Ses, altyazı dili ve kaldığınız yer sonraki bölümlerde otomatik hatırlanır.",
                    highlightTip = "Seçtiğiniz dil sonraki tüm bölümlerde otomatik uygulanır"
                )
            )
        )
        "pl" -> TupleTutorial(
            chooseLangTitle = "Wybierz język wyświetlania",
            chooseLangSubtitle = "Interfejs, kategorie i ten samouczek dostosują się natychmiast",
            skipLabel = "Pomiń",
            nextLabel = "Dalej",
            backLabel = "Wstecz",
            startLabel = "Rozpocznij",
            cards = listOf(
                TutorialCardData(
                    icon = Icons.Default.LiveTv,
                    title = "Źródła IPTV i Pełny Przewodnik EPG",
                    description = "Połącz konto Xtream Codes lub link M3U w zakładce Źródła. Kategorie są tłumaczone, a pełna siatka programu TV ładuje się automatycznie.",
                    highlightTip = "Przeglądaj harmonogram programów i włączaj kanały jednym kliknięciem"
                ),
                TutorialCardData(
                    icon = Icons.Default.TouchApp,
                    title = "Gesty dotykowe i pilot TV",
                    description = "Przesuń po lewej dla jasności, po prawej dla głośności, a przytrzymanie przycisku dodaje kanał, film lub serial do ulubionych.",
                    highlightTip = "Otwórz panel boczny w odtwarzaczu, aby szybko zmieniać odcinki"
                ),
                TutorialCardData(
                    icon = Icons.Default.Subtitles,
                    title = "Napisy na żywo i auto-zapamiętywanie",
                    description = "Zmieniaj pozycję i czcionkę napisów na żywo. Wybrany język dźwięku, napisów i moment odtwarzania są zapamiętywane między odcinkami.",
                    highlightTip = "Wybrany język przechodzi automatycznie na kolejne odcinki"
                )
            )
        )
        else -> TupleTutorial(
            chooseLangTitle = "Choose Your Display Language",
            chooseLangSubtitle = "The app interface, category translations, and this quick guide will adapt immediately",
            skipLabel = "Skip",
            nextLabel = "Next",
            backLabel = "Back",
            startLabel = "Start Watching",
            cards = listOf(
                TutorialCardData(
                    icon = Icons.Default.LiveTv,
                    title = "IPTV Sources & Full TV Guide Grid",
                    description = "Add your Xtream Codes login or M3U playlist in Sources. Categories auto-translate and the full timeline EPG TV Guide loads automatically.",
                    highlightTip = "Browse the full hourly TV Guide grid and launch any channel instantly"
                ),
                TutorialCardData(
                    icon = Icons.Default.TouchApp,
                    title = "Touch Gestures & TV Remote Ready",
                    description = "Swipe left for brightness, right for volume, and long-press any channel, movie, or series card to add it to Favorites.",
                    highlightTip = "Open the side drawer inside the player to switch channels or episodes fast"
                ),
                TutorialCardData(
                    icon = Icons.Default.Subtitles,
                    title = "Pro Subtitles & Smart Track Memory",
                    description = "Adjust subtitle height and custom fonts live on screen. Your audio language, subtitle choice, and resume position carry over across episodes.",
                    highlightTip = "Your chosen audio & subtitle tracks automatically follow you to the next episode"
                )
            )
        )
    }
}

private fun getNativeLanguageDisplayName(code: String): String {
    return when (code.lowercase(Locale.US)) {
        "ar" -> "العربية"
        "en" -> "English"
        "fr" -> "Français"
        "es" -> "Español"
        "de" -> "Deutsch"
        "tr" -> "Türkçe"
        "pl" -> "Polski"
        "pt" -> "Português"
        "it" -> "Italiano"
        "ru" -> "Русский"
        "hi" -> "हिन्दी"
        "ur" -> "اردو"
        else -> Locale(code).getDisplayLanguage(Locale(code))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale(code)) else it.toString() }
    }
}

@Composable
private fun FirstRunLanguageAndTutorialOverlay(
    selectedLanguage: String,
    onSelectLanguage: (String) -> Unit,
    onComplete: () -> Unit
) {
    var stepIndex by remember { mutableIntStateOf(0) }
    val tutorial = remember(selectedLanguage) { getTutorialContentForLanguage(selectedLanguage) }
    val supportedCodes = remember {
        AppLanguageManager.supportedLanguages.map { it.code }.distinct().ifEmpty { listOf("ar", "en") }
    }

    val primaryButtonFocusRequester = remember { FocusRequester() }

    LaunchedEffect(stepIndex) {
        try {
            delay(100L)
            primaryButtonFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF2030712))
            .padding(20.dp)
    ) {
        Surface(
            color = Slate900,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.5.dp, AccentCyan.copy(alpha = 0.6f)),
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val totalSteps = tutorial.cards.size + 1
                        for (i in 0 until totalSteps) {
                            Box(
                                modifier = Modifier
                                    .width(if (i == stepIndex) 24.dp else 8.dp)
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(if (i == stepIndex) AccentCyan else Slate700)
                            )
                        }
                    }

                    TextButton(onClick = onComplete) {
                        Text(
                            text = tutorial.skipLabel,
                            color = Slate400,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (stepIndex == 0) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(AccentCyan.copy(alpha = 0.15f))
                            .border(1.dp, AccentCyan.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = tutorial.chooseLangTitle,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = PureWhite,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = tutorial.chooseLangSubtitle,
                        fontSize = 13.sp,
                        color = Slate300,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        supportedCodes.chunked(2).forEach { rowCodes ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                rowCodes.forEach { code ->
                                    val isSelected = selectedLanguage.equals(code, ignoreCase = true)
                                    val langInteraction = remember { MutableInteractionSource() }
                                    val isFocused by langInteraction.collectIsFocusedAsState()

                                    Surface(
                                        color = when {
                                            isSelected -> AccentCyan.copy(alpha = 0.2f)
                                            isFocused -> Slate800
                                            else -> Slate850
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(
                                            width = if (isFocused || isSelected) 2.dp else 1.dp,
                                            color = when {
                                                isFocused -> PureWhite
                                                isSelected -> AccentCyan
                                                else -> Slate700
                                            }
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .focusable(interactionSource = langInteraction)
                                            .onPreviewKeyEvent { event ->
                                                if (event.type == KeyEventType.KeyUp &&
                                                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                                ) {
                                                    onSelectLanguage(code)
                                                    true
                                                } else false
                                            }
                                            .clickable(interactionSource = langInteraction, indication = null) {
                                                onSelectLanguage(code)
                                            }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                                        ) {
                                            Text(
                                                text = getNativeLanguageDisplayName(code),
                                                fontSize = 14.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                color = if (isSelected) AccentCyan else PureWhite
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = AccentCyan,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                if (rowCodes.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    val nextBtnInteraction = remember { MutableInteractionSource() }
                    val isNextFocused by nextBtnInteraction.collectIsFocusedAsState()

                    Button(
                        onClick = { stepIndex = 1 },
                        interactionSource = nextBtnInteraction,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isNextFocused) PureWhite else AccentCyan,
                            contentColor = Slate950
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .focusRequester(primaryButtonFocusRequester)
                    ) {
                        Text(
                            text = tutorial.nextLabel,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                } else {
                    val card = tutorial.cards[(stepIndex - 1).coerceIn(0, tutorial.cards.lastIndex)]
                    val isLastCard = stepIndex >= tutorial.cards.size

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(AccentCyan.copy(alpha = 0.15f))
                            .border(1.5.dp, AccentCyan, CircleShape)
                    ) {
                        Icon(
                            imageVector = card.icon,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = card.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = PureWhite,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = card.description,
                        fontSize = 14.sp,
                        color = Slate300,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        color = Slate850,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "✦ ${card.highlightTip}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentCyan,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { stepIndex = (stepIndex - 1).coerceAtLeast(0) },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Slate700),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Slate850,
                                contentColor = PureWhite
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text(
                                text = tutorial.backLabel,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val actionInteraction = remember { MutableInteractionSource() }
                        val isActionFocused by actionInteraction.collectIsFocusedAsState()

                        Button(
                            onClick = {
                                if (isLastCard) onComplete() else stepIndex++
                            },
                            interactionSource = actionInteraction,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isActionFocused) PureWhite else AccentCyan,
                                contentColor = Slate950
                            ),
                            modifier = Modifier
                                .weight(1.4f)
                                .height(48.dp)
                                .focusRequester(primaryButtonFocusRequester)
                        ) {
                            Text(
                                text = if (isLastCard) tutorial.startLabel else tutorial.nextLabel,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WatchHistoryScreen(
    items: List<IptvItem>,
    favorites: Set<String> = emptySet(),
    onPlayItem: (IptvItem) -> Unit,
    onToggleFavorite: (IptvItem) -> Unit = {},
    onRemoveItem: (String) -> Unit,
    onClearAll: () -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
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
        "ALL" to "${strings.all} (${vodItems.size})",
        "SERIES" to strings.series,
        "MOVIES" to strings.movies
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
                        contentDescription = strings.back,
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
                    text = strings.historyTitle,
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
                    val isFav = favorites.contains("${item.type.name}::${item.id}") ||
                        favorites.contains(item.id) ||
                        item.isFavorite

                    HistoryPosterCard(
                        item = item,
                        isFavorite = isFav,
                        isAr = isAr,
                        onClick = { onPlayItem(item) },
                        onToggleFavorite = { onToggleFavorite(item) },
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
    isFavorite: Boolean = item.isFavorite,
    isAr: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit = {},
    onRemove: () -> Unit
) {
    val cardInteraction = remember { MutableInteractionSource() }
    val isCardFocused by cardInteraction.collectIsFocusedAsState()

    val favInteraction = remember { MutableInteractionSource() }
    val isFavFocused by favInteraction.collectIsFocusedAsState()

    val removeInteraction = remember { MutableInteractionSource() }
    val isRemoveFocused by removeInteraction.collectIsFocusedAsState()

    val anyFocused = isCardFocused || isFavFocused || isRemoveFocused
    var okPressStartTime by remember { mutableLongStateOf(0L) }
    var longPressHandled by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (anyFocused) 1.05f else 1f,
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
                elevation = if (anyFocused) 14.dp else 4.dp,
                shape = RoundedCornerShape(14.dp),
                spotColor = if (anyFocused) AccentCyan else Color.Transparent
            )
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (isCardFocused) 3.dp else 1.dp,
                color = if (isCardFocused) AccentCyan else Slate800,
                shape = RoundedCornerShape(14.dp)
            )
            .focusable(interactionSource = cardInteraction)
            .onPreviewKeyEvent { event ->
                val isCenterKey = event.key == Key.DirectionCenter ||
                    event.key == Key.Enter ||
                    event.key == Key.NumPadEnter

                if (isCenterKey) {
                    when (event.type) {
                        KeyEventType.KeyDown -> {
                            if (okPressStartTime == 0L) {
                                okPressStartTime = System.currentTimeMillis()
                                longPressHandled = false
                            } else if (!longPressHandled && System.currentTimeMillis() - okPressStartTime >= 450L) {
                                longPressHandled = true
                                onToggleFavorite()
                            }
                            true
                        }
                        KeyEventType.KeyUp -> {
                            val wasLong = longPressHandled ||
                                (okPressStartTime > 0L && System.currentTimeMillis() - okPressStartTime >= 450L)
                            okPressStartTime = 0L
                            longPressHandled = false
                            if (!wasLong) {
                                onClick()
                            }
                            true
                        }
                        else -> false
                    }
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

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (isFavFocused) PureWhite else Color(0xCC060A12))
                        .border(
                            width = if (isFavFocused) 2.dp else 1.dp,
                            color = if (isFavFocused) AccentCyan else Color(0x33FFFFFF),
                            shape = CircleShape
                        )
                        .focusable(interactionSource = favInteraction)
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyUp &&
                                (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                            ) {
                                onToggleFavorite()
                                true
                            } else false
                        }
                        .clickable(interactionSource = favInteraction, indication = null, onClick = onToggleFavorite)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = when {
                            isFavFocused -> if (isFavorite) AccentRed else Slate950
                            isFavorite -> AccentRed
                            else -> PureWhite
                        },
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (isRemoveFocused) AccentRed else Color(0xCC000000))
                        .border(
                            width = if (isRemoveFocused) 2.dp else 1.dp,
                            color = if (isRemoveFocused) PureWhite else Color(0x33FFFFFF),
                            shape = CircleShape
                        )
                        .focusable(interactionSource = removeInteraction)
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyUp &&
                                (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                            ) {
                                onRemove()
                                true
                            } else false
                        }
                        .clickable(interactionSource = removeInteraction, indication = null, onClick = onRemove)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = PureWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }
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
            animation = tween(
                durationMillis = 1850,
                easing = CubicBezierEasing(0.35f, 0.0f, 0.25f, 1.0f)
            ),
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
            .background(Color(0xFF03060E)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x24FF6B35), Color.Transparent),
                    center = Offset(size.width * 0.22f, size.height * 0.28f),
                    radius = size.maxDimension * 0.42f
                ),
                radius = size.maxDimension * 0.42f,
                center = Offset(size.width * 0.22f, size.height * 0.28f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x269D4EDD), Color.Transparent),
                    center = Offset(size.width * 0.78f, size.height * 0.30f),
                    radius = size.maxDimension * 0.42f
                ),
                radius = size.maxDimension * 0.42f,
                center = Offset(size.width * 0.78f, size.height * 0.30f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x2B00E5FF), Color.Transparent),
                    center = Offset(size.width * 0.50f, size.height * 0.65f),
                    radius = size.maxDimension * 0.50f
                ),
                radius = size.maxDimension * 0.50f,
                center = Offset(size.width * 0.50f, size.height * 0.65f)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(204.dp)
                    .shadow(
                        elevation = 28.dp,
                        shape = RoundedCornerShape(46.dp),
                        spotColor = Color(0xFF00E5FF)
                    )
                    .clip(RoundedCornerShape(46.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xCC0B1324),
                                Color(0xE6050914),
                                Color(0xCC09162B)
                            )
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0x9900E5FF),
                                Color(0x669D4EDD),
                                Color(0x88FF6B35),
                                Color(0x9900E5FF)
                            )
                        ),
                        shape = RoundedCornerShape(46.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(166.dp)) {
                    val w = size.width
                    val h = size.height
                    val cx = w / 2f
                    val cy = h / 2f
                    val normalizedDeg = ((orbitAngle % 360f) + 360f) % 360f

                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0x3800E5FF),
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
                        val rx = ovalSize.width / 2f
                        val ry = ovalSize.height / 2f

                        drawArc(
                            color = Color(0xFF0284C7).copy(alpha = 0.45f),
                            startAngle = 180f,
                            sweepAngle = 180f,
                            useCenter = false,
                            topLeft = ovalTopLeft,
                            size = ovalSize,
                            style = Stroke(width = 6.5.dp.toPx(), cap = StrokeCap.Round)
                        )

                        for (i in 0..14) {
                            val trailDeg = ((normalizedDeg - i * 3.5f) + 360f) % 360f
                            if (trailDeg > 180f && trailDeg < 360f) {
                                val rad = Math.toRadians(trailDeg.toDouble())
                                val dotX = cx + rx * cos(rad).toFloat()
                                val dotY = cy + ry * sin(rad).toFloat()
                                val fraction = 1f - (i / 15f)
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.65f * fraction),
                                    radius = 3.dp.toPx() * fraction,
                                    center = Offset(dotX, dotY)
                                )
                            }
                        }
                    }

                    val tvWidth = w * 0.62f
                    val tvHeight = h * 0.48f
                    val tvLeft = cx - (tvWidth / 2f)
                    val tvTop = cy - (tvHeight / 2f) - 6.dp.toPx()

                    drawRoundRect(
                        color = Color(0xE6040812),
                        topLeft = Offset(tvLeft, tvTop),
                        size = Size(tvWidth, tvHeight),
                        cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                    )

                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF00E5FF), Color(0xFF9D4EDD), Color(0xFF00E5FF))
                        ),
                        topLeft = Offset(tvLeft, tvTop),
                        size = Size(tvWidth, tvHeight),
                        cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                        style = Stroke(width = 4.5.dp.toPx())
                    )

                    val barY = tvTop + 13.dp.toPx()
                    val barWidth = tvWidth * 0.22f
                    val barSpacing = tvWidth * 0.06f
                    val startX = cx - ((barWidth * 3 + barSpacing * 2) / 2f)

                    val orangeAlpha = if (barPhase.toInt() == 0) 1f else 0.72f
                    val purpleAlpha = if (barPhase.toInt() == 1) 1f else 0.72f
                    val cyanAlpha = if (barPhase.toInt() == 2) 1f else 0.72f

                    drawLine(
                        color = Color(0xFFFF6B35).copy(alpha = orangeAlpha),
                        start = Offset(startX, barY),
                        end = Offset(startX + barWidth, barY),
                        strokeWidth = 5.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color(0xFF9D4EDD).copy(alpha = purpleAlpha),
                        start = Offset(startX + barWidth + barSpacing, barY),
                        end = Offset(startX + barWidth * 2 + barSpacing, barY),
                        strokeWidth = 5.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color(0xFF00E5FF).copy(alpha = cyanAlpha),
                        start = Offset(startX + (barWidth + barSpacing) * 2, barY),
                        end = Offset(startX + barWidth * 3 + barSpacing * 2, barY),
                        strokeWidth = 5.5.dp.toPx(),
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
                        color = Color(0xFF00E5FF),
                        start = Offset(cx, standTop),
                        end = Offset(cx, standBottom),
                        strokeWidth = 5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF9D4EDD), Color(0xFF00E5FF), Color(0xFFFF6B35))
                        ),
                        start = Offset(cx - 22.dp.toPx(), standBottom),
                        end = Offset(cx + 22.dp.toPx(), standBottom),
                        strokeWidth = 4.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    rotate(degrees = -18f, pivot = Offset(cx, cy)) {
                        val ovalTopLeft = Offset(w * 0.04f, h * 0.30f)
                        val ovalSize = Size(w * 0.92f, h * 0.40f)
                        val rx = ovalSize.width / 2f
                        val ry = ovalSize.height / 2f

                        drawArc(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF00E5FF),
                                    Color(0xFF7CE8FF),
                                    Color(0xFF9D4EDD)
                                )
                            ),
                            startAngle = 0f,
                            sweepAngle = 180f,
                            useCenter = false,
                            topLeft = ovalTopLeft,
                            size = ovalSize,
                            style = Stroke(width = 7.5.dp.toPx(), cap = StrokeCap.Round)
                        )

                        for (i in 0..16) {
                            val trailDeg = ((normalizedDeg - i * 3.5f) + 360f) % 360f
                            if (trailDeg in 0f..180f) {
                                val rad = Math.toRadians(trailDeg.toDouble())
                                val dotX = cx + rx * cos(rad).toFloat()
                                val dotY = cy + ry * sin(rad).toFloat()
                                val fraction = 1f - (i / 17f)

                                drawCircle(
                                    color = Color(0xFF00E5FF).copy(alpha = 0.45f * fraction),
                                    radius = 6.dp.toPx() * fraction,
                                    center = Offset(dotX, dotY)
                                )
                                drawCircle(
                                    color = Color.White.copy(alpha = fraction),
                                    radius = 3.5.dp.toPx() * fraction,
                                    center = Offset(dotX, dotY)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

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

            Spacer(modifier = Modifier.height(14.dp))

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

            Surface(
                color = Color(0x880B1221),
                shape = CircleShape,
                border = BorderStroke(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(Color(0x44FF6B35), Color(0x5500E5FF), Color(0x449D4EDD))
                    )
                )
            ) {
                Text(
                    text = displayStatus,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate300,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            LinearProgressIndicator(
                color = Color(0xFF00E5FF),
                trackColor = Color(0x441E293B),
                modifier = Modifier
                    .width(200.dp)
                    .height(4.5.dp)
                    .clip(CircleShape)
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

    val strings = LocalAppStrings.current
    val isAr = LocalAppLanguage.current == "ar"

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
                        text = "${strings.nowPlaying} • ${item.group}",
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
