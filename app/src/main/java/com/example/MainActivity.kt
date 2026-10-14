package com.example

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.model.IptvItem
import com.example.model.IptvType
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.channelbrowser.ContentBrowserScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.locale.LocalAppLanguage
import com.example.ui.locale.LocalAppStrings
import com.example.ui.locale.LocalIsInPipMode
import com.example.ui.locale.getAppStrings
import com.example.ui.player.VideoPlayerScreen
import com.example.ui.series.SeriesDetailScreen
import com.example.ui.sources.SourcesScreen
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.util.DeviceUtils
import com.example.util.LocalIsTelevision

class MainActivity : ComponentActivity() {
    private var mainViewModel: MainViewModel? = null
    private var isInPipMode by mutableStateOf(false)

    fun enterPipMode() {
        if (DeviceUtils.isTelevision(this)) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                @Suppress("DEPRECATION")
                enterPictureInPictureMode()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val isTv = DeviceUtils.isTelevision(this)
        val vm = mainViewModel ?: return
        if (!isTv && vm.currentScreen.value == Screen.PLAYER && vm.playerUiState.value.isPlaying) {
            enterPipMode()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DeviceUtils.configureDeviceOrientation(this)
        enableEdgeToEdge()
        setContent {
            val isTelevision = remember { DeviceUtils.isTelevision(this@MainActivity) }
            val viewModel: MainViewModel = viewModel()
            mainViewModel = viewModel
            val currentLanguage by viewModel.currentLanguage.collectAsState()
            val layoutDirection = if (currentLanguage == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
            val strings = remember(currentLanguage) { getAppStrings(currentLanguage) }

            CompositionLocalProvider(
                LocalIsTelevision provides isTelevision,
                LocalAppLanguage provides currentLanguage,
                LocalAppStrings provides strings,
                LocalLayoutDirection provides layoutDirection,
                LocalIsInPipMode provides isInPipMode
            ) {
                MyApplicationTheme {
                    HypnoStreamApp(
                        viewModel = viewModel,
                        onFinishApp = { finish() },
                        onEnterPip = { enterPipMode() }
                    )
                }
            }
        }
    }
}

@Composable
fun HypnoStreamApp(
    viewModel: MainViewModel = viewModel(),
    onFinishApp: () -> Unit = {},
    onEnterPip: () -> Unit = {}
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

    val favoriteItems = recentlyWatched.filter { favorites.contains(it.id) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
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

    if (currentScreen == Screen.PLAYER) {
        val activeItem = playerUiState.currentItem
        // Pass Live Channels for Live TV, Series Episodes for Series, and Empty List for Movies!
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
            onEnterPip = onEnterPip,
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
                            Screen.MOVIES -> totalMoviesCount
                            Screen.SERIES -> totalSeriesCount
                            else -> filteredItems.size
                        },
                        favoriteCount = favorites.size,
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onCategoryChange = { viewModel.setSelectedCategory(it) },
                        onLoadMore = { viewModel.loadMoreItems() },
                        onPlayItem = { viewModel.playStream(it) },
                        onOpenSeries = { seriesItem -> viewModel.openSeriesDetail(seriesItem) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onBack = { viewModel.handleBackPress() }
                    )
                }

                Screen.SERIES_DETAIL -> {
                    SeriesDetailScreen(
                        details = seriesDetails,
                        isLoading = isSeriesLoading,
                        onPlayEpisode = { episode -> viewModel.playEpisode(episode) },
                        onBack = { viewModel.handleBackPress() }
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
                        onBack = { viewModel.handleBackPress() },
                        onToggleLanguage = { viewModel.toggleLanguage() }
                    )
                }

                Screen.PLAYER -> {}
            }
        }

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Slate950)
            ) {
                NavigationRail(
                    containerColor = Slate900,
                    contentColor = PureWhite,
                    modifier = Modifier
                        .width(76.dp)
                        .fillMaxHeight()
                        .border(BorderStroke(1.dp, Slate800))
                        .testTag("main_navigation_rail")
                ) {
                    Spacer(modifier = Modifier.height(10.dp))
                    navItems.forEach { (screen, icon, label) ->
                        val isSelected = currentScreen == screen || (screen == Screen.SERIES && currentScreen == Screen.SERIES_DETAIL)
                        val navInteraction = remember { MutableInteractionSource() }
                        val isNavFocused by navInteraction.collectIsFocusedAsState()

                        NavigationRailItem(
                            selected = isSelected,
                            onClick = { viewModel.navigateTo(screen) },
                            interactionSource = navInteraction,
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
                                .focusable(interactionSource = navInteraction)
                                .border(
                                    width = if (isNavFocused) 2.dp else 0.dp,
                                    color = if (isNavFocused) AccentCyan else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .onPreviewKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyUp &&
                                        (event.key == Key.DirectionCenter ||
                                         event.key == Key.Enter ||
                                         event.key == Key.NumPadEnter)
                                    ) {
                                        viewModel.navigateTo(screen)
                                        true
                                    } else false
                                }
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

                    if (playerUiState.currentItem != null) {
                        MiniPlayerBar(
                            item = playerUiState.currentItem!!,
                            isPlaying = playerUiState.isPlaying,
                            isCompact = true,
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
                        if (playerUiState.currentItem != null) {
                            MiniPlayerBar(
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
                                .border(BorderStroke(1.dp, Slate800))
                                .testTag("main_navigation_bar")
                        ) {
                            navItems.forEach { (screen, icon, label) ->
                                val isSelected = currentScreen == screen || (screen == Screen.SERIES && currentScreen == Screen.SERIES_DETAIL)
                                val navInteraction = remember { MutableInteractionSource() }
                                val isNavFocused by navInteraction.collectIsFocusedAsState()

                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = { viewModel.navigateTo(screen) },
                                    interactionSource = navInteraction,
                                    icon = {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = label,
                                            tint = if (isNavFocused) PureWhite else if (isSelected) AccentCyan else Slate400,
                                            modifier = Modifier.size(if (isNavFocused) 26.dp else 22.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected || isNavFocused) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isNavFocused) PureWhite else if (isSelected) AccentCyan else Slate400
                                        )
                                    },
                                    colors = navigationBarItemColors(),
                                    modifier = Modifier
                                        .focusable(interactionSource = navInteraction)
                                        .border(
                                            width = if (isNavFocused) 2.dp else 0.dp,
                                            color = if (isNavFocused) AccentCyan else Color.Transparent,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .onPreviewKeyEvent { event ->
                                            if (event.type == KeyEventType.KeyUp &&
                                                (event.key == Key.DirectionCenter ||
                                                 event.key == Key.Enter ||
                                                 event.key == Key.NumPadEnter)
                                            ) {
                                                viewModel.navigateTo(screen)
                                                true
                                            } else false
                                        }
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
    }
}

@Composable
fun MiniPlayerBar(
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
    val nowPlayingText = if (currentLanguage == "ar") "يعرض الآن" else "Now Playing"

    Surface(
        color = if (isBarFocused) Slate800 else Slate900,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
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
                .padding(horizontal = 16.dp, vertical = if (isCompact) 8.dp else 10.dp),
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
                if (!item.logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.logoUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(if (isCompact) 32.dp else 38.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
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
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

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
                            width = if (isCloseBtnFocused) 2.5.dp else 1.dp,
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
