package com.example

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
import androidx.compose.runtime.remember
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.model.IptvItem
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.channelbrowser.ContentBrowserScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.player.VideoPlayerScreen
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

import androidx.compose.runtime.CompositionLocalProvider
import com.example.util.DeviceUtils
import com.example.util.LocalIsTelevision

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Check UiModeManager for Television and configure orientation
        DeviceUtils.configureDeviceOrientation(this)
        enableEdgeToEdge()
        setContent {
            val isTelevision = remember { DeviceUtils.isTelevision(this@MainActivity) }
            CompositionLocalProvider(LocalIsTelevision provides isTelevision) {
                MyApplicationTheme {
                    HypnoStreamApp()
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
    val isLoading by viewModel.isLoading.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    // Favorite items
    val favoriteItems = recentlyWatched.filter { favorites.contains(it.id) }

    // System Back Handler
    BackHandler {
        if (!viewModel.handleBackPress()) {
            // Default activity finish
        }
    }

    if (currentScreen == Screen.PLAYER) {
        // Fullscreen edge-to-edge player without Scaffold navigation bars
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
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(Slate950),
            bottomBar = {
                Column(modifier = Modifier.navigationBarsPadding()) {
                    // Floating Mini Player when navigating outside player screen
                    if (playerUiState.currentItem != null) {
                        MiniPlayerBar(
                            item = playerUiState.currentItem!!,
                            isPlaying = playerUiState.isPlaying,
                            onExpand = { viewModel.navigateTo(Screen.PLAYER) },
                            onTogglePlayPause = { viewModel.playerManager.togglePlayPause() }
                        )
                    }

                    // Main Navigation Bar with TV Remote Navigation
                    NavigationBar(
                        containerColor = Slate900,
                        contentColor = PureWhite,
                        modifier = Modifier
                            .height(68.dp)
                            .border(androidx.compose.foundation.BorderStroke(1.dp, Slate800))
                            .testTag("main_navigation_bar")
                    ) {
                        val navItems = listOf(
                            Triple(Screen.DASHBOARD, Icons.Default.Home, "Home"),
                            Triple(Screen.LIVE_CHANNELS, Icons.Default.LiveTv, "Live TV"),
                            Triple(Screen.MOVIES, Icons.Default.Movie, "Movies"),
                            Triple(Screen.SERIES, Icons.Default.VideoLibrary, "Series"),
                            Triple(Screen.SOURCES, Icons.Default.Settings, "Sources")
                        )

                        navItems.forEach { (screen, icon, label) ->
                            val isSelected = currentScreen == screen
                            val navInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
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
                            onPlayItem = { viewModel.playStream(it) }
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
                            onSearchChange = { viewModel.setSearchQuery(it) },
                            onCategoryChange = { viewModel.setSelectedCategory(it) },
                            onLoadMore = { viewModel.loadMoreItems() },
                            onPlayItem = { viewModel.playStream(it) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
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
                            onBack = { viewModel.handleBackPress() }
                        )
                    }

                    Screen.PLAYER -> {
                        // Handled above in root Box
                    }
                }
            }
        }
    }
}

@Composable
fun MiniPlayerBar(
    item: IptvItem,
    isPlaying: Boolean,
    onExpand: () -> Unit,
    onTogglePlayPause: () -> Unit
) {
    val barInteraction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isBarFocused by barInteraction.collectIsFocusedAsState()

    Surface(
        color = if (isBarFocused) Slate800 else Slate850,
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isBarFocused) 2.5.dp else 1.dp,
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
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (!item.logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.logoUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Column {
                    Text(
                        text = item.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Now Playing • ${item.group}",
                        fontSize = 11.sp,
                        color = AccentCyan,
                        maxLines = 1
                    )
                }
            }

            IconButton(
                onClick = onTogglePlayPause,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(AccentCyan)
                    .testTag("btn_mini_player_play_pause")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Slate950,
                    modifier = Modifier.size(22.dp)
                )
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
