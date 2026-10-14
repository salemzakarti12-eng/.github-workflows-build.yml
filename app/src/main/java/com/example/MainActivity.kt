package com.example

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.remember
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DeviceUtils.configureDeviceOrientation(this)
        enableEdgeToEdge()
        setContent {
            val isTelevision = remember { DeviceUtils.isTelevision(this@MainActivity) }
            val viewModel: MainViewModel = viewModel()
            val currentLanguage by viewModel.currentLanguage.collectAsState()
            val layoutDirection = if (currentLanguage == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
            val strings = remember(currentLanguage) { getAppStrings(currentLanguage) }

            CompositionLocalProvider(
                LocalIsTelevision provides isTelevision,
                LocalAppLanguage provides currentLanguage,
                LocalAppStrings provides strings,
                LocalLayoutDirection provides layoutDirection
            ) {
                MyApplicationTheme {
                    HypnoStreamApp(viewModel)
                }
            }
        }
    }
}

@Composable
fun HypnoStreamApp(viewModel: MainViewModel = viewModel()) {
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
    val isSeriesLoading by viewModel.isSeriesLoading.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    val favoriteItems = recentlyWatched.filter { favorites.contains(it.id) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val strings = LocalAppStrings.current

    // System Back Handler
    BackHandler {
        if (!viewModel.handleBackPress()) {
            // Finish activity
        }
    }

    if (currentScreen == Screen.PLAYER) {
        VideoPlayerScreen(
            playerManager = viewModel.playerManager,
            uiState = playerUiState,
            allChannels = viewModel.repository.getAllLiveChannels(),
            onBack = { viewModel.handleBackPress() },
            onPlayNext = { viewModel.playNextChannel() },
            onPlayPrevious = { viewModel.playPreviousChannel() },
            onSelectChannel = { viewModel.playStream(it) },
            modifier = Modifier.fillMaxSize()
        )
    } else {
        val navItems = listOf(
            Triple(Screen.DASHBOARD, Icons.Default.Home, strings.home),
            Triple(Screen.LIVE_CHANNELS, Icons.Default.LiveTv, strings.liveTv),
            Triple(Screen.MOVIES, Icons.Default.Movie, strings.movies),
            Triple(Screen.SERIES, Icons.Default.VideoLibrary, strings.series),
            Triple(Screen.SOURCES, Icons.Default.Settings, strings.sources)
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

                Screen.PLAYER -> {
                    // Handled fullscreen above
                }
            }
        }

        if (isLandscape) {
            // Landscape layout: Compact side NavigationRail
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Slate950)
            ) {
                NavigationRail(
                    containerColor = Slate900,
                    contentColor = PureWhite,
                    modifier = Modifier
                        .width(72.dp)
                        .fillMaxHeight()
                        .border(androidx.compose.foundation.BorderStroke(1.dp, Slate800))
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
                                .testTag("nav_rail_${label.lowercase().replace(" ", "_")}")
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
            // Portrait layout: Standard Scaffold with bottom navigation bar and mini player
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
                                .border(androidx.compose.foundation.BorderStroke(1.dp, Slate800))
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
                                        .testTag("nav_item_${label.lowercase().replace(" ", "_")}")
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
    val currentLanguage = LocalAppLanguage.current
    val nowPlayingText = if (currentLanguage == "ar") "يعرض الآن" else "Now Playing"

    Surface(
        color = if (isBarFocused) Slate800 else Slate850,
        shape = if (isCompact) RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp) else RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isBarFocused) 2.dp else 1.dp,
            color = if (isBarFocused) AccentCyan else Slate700
        ),
        modifier = Modifier
            .fillMaxWidth()
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
            .testTag("mini_player_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = if (isCompact) 4.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (!item.logoUrl.isNullOrBlank() && !isCompact) {
                    AsyncImage(
                        model = item.logoUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Column {
                    Text(
                        text = item.name,
                        fontSize = if (isCompact) 12.sp else 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$nowPlayingText • ${item.group}",
                        fontSize = 10.sp,
                        color = AccentCyan,
                        maxLines = 1
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(if (isCompact) 32.dp else 36.dp)
                        .clip(CircleShape)
                        .background(AccentCyan)
                        .testTag("btn_mini_player_play_pause")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Slate950,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(if (isCompact) 30.dp else 34.dp)
                        .clip(CircleShape)
                        .background(Slate800)
                        .testTag("btn_mini_player_dismiss")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Mini Player",
                        tint = Slate400,
                        modifier = Modifier.size(16.dp)
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
