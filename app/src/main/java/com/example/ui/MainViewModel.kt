package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.IptvRepository
import com.example.model.CategoryInfo
import com.example.model.IptvItem
import com.example.model.IptvSource
import com.example.model.IptvType
import com.example.model.PlayerUiState
import com.example.model.SeriesDetails
import com.example.model.SeriesEpisode
import com.example.player.ExoPlayerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class Screen {
    DASHBOARD,
    LIVE_CHANNELS,
    MOVIES,
    SERIES,
    SERIES_DETAIL,
    SOURCES,
    PLAYER
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = IptvRepository(application)
    val playerManager = ExoPlayerManager(application, viewModelScope)

    private val _currentScreen = MutableStateFlow(Screen.DASHBOARD)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _screenHistory = mutableListOf<Screen>()

    // Search, category, and pagination controls
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _displayLimit = MutableStateFlow(100)
    val displayLimit: StateFlow<Int> = _displayLimit.asStateFlow()

    val sources: StateFlow<List<IptvSource>> = repository.sources
    val activeSource: StateFlow<IptvSource?> = repository.activeSource
    val totalLiveCount: StateFlow<Int> = repository.totalLiveCount
    val totalMoviesCount: StateFlow<Int> = repository.totalMoviesCount
    val totalSeriesCount: StateFlow<Int> = repository.totalSeriesCount

    val liveCategories: StateFlow<List<CategoryInfo>> = repository.liveCategories
    val movieCategories: StateFlow<List<CategoryInfo>> = repository.movieCategories
    val seriesCategories: StateFlow<List<CategoryInfo>> = repository.seriesCategories

    val favorites: StateFlow<Set<String>> = repository.favorites
    val recentlyWatched: StateFlow<List<IptvItem>> = repository.recentlyWatched
    val isLoading: StateFlow<Boolean> = repository.isLoading
    val statusMessage: StateFlow<String?> = repository.statusMessage
    val playerUiState: StateFlow<PlayerUiState> = playerManager.uiState

    // Series detail state
    private val _selectedSeries = MutableStateFlow<IptvItem?>(null)
    val selectedSeries: StateFlow<IptvItem?> = _selectedSeries.asStateFlow()

    private val _seriesDetails = MutableStateFlow<SeriesDetails?>(null)
    val seriesDetails: StateFlow<SeriesDetails?> = _seriesDetails.asStateFlow()

    private val _isSeriesLoading = MutableStateFlow(false)
    val isSeriesLoading: StateFlow<Boolean> = _isSeriesLoading.asStateFlow()

    // Categories list for current screen
    val currentCategories: StateFlow<List<CategoryInfo>> = combine(
        _currentScreen,
        repository.liveCategories,
        repository.movieCategories,
        repository.seriesCategories
    ) { screen, liveCats, movCats, serCats ->
        when (screen) {
            Screen.LIVE_CHANNELS -> liveCats
            Screen.MOVIES -> movCats
            Screen.SERIES -> serCats
            else -> emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Memory-protected paginated & filtered channels for active screen and category
    val currentFilteredItems: StateFlow<List<IptvItem>> = combine(
        _currentScreen,
        _selectedCategory,
        _searchQuery,
        _displayLimit,
        repository.totalLiveCount // triggers refresh when new playlist loaded
    ) { screen, category, query, limit, _ ->
        val type = when (screen) {
            Screen.LIVE_CHANNELS -> IptvType.LIVE_TV
            Screen.MOVIES -> IptvType.MOVIE
            Screen.SERIES -> IptvType.SERIES
            else -> IptvType.LIVE_TV
        }
        repository.queryStreams(
            type = type,
            category = category,
            query = query,
            page = 0,
            pageSize = limit
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun navigateTo(screen: Screen) {
        if (_currentScreen.value != screen) {
            _screenHistory.add(_currentScreen.value)
            _currentScreen.value = screen
            _searchQuery.value = ""
            _selectedCategory.value = "All"
            _displayLimit.value = 100
        }
    }

    fun handleBackPress(): Boolean {
        if (_currentScreen.value == Screen.PLAYER) {
            if (playerUiState.value.isQuickChannelListOpen) {
                playerManager.closeQuickChannelList()
                return true
            }
            val previous = _screenHistory.removeLastOrNull() ?: Screen.DASHBOARD
            _currentScreen.value = previous
            return true
        }

        if (_screenHistory.isNotEmpty()) {
            _currentScreen.value = _screenHistory.removeLast()
            return true
        }
        return false
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        _displayLimit.value = 100
    }

    fun setSelectedCategory(cat: String) {
        _selectedCategory.value = cat
        _displayLimit.value = 100
    }

    fun loadMoreItems() {
        _displayLimit.value += 100
    }

    fun playStream(item: IptvItem) {
        repository.recordRecentlyWatched(item)
        playerManager.playItem(item)
        navigateTo(Screen.PLAYER)
    }

    fun openSeriesDetail(item: IptvItem) {
        _selectedSeries.value = item
        _seriesDetails.value = null
        _isSeriesLoading.value = true
        navigateTo(Screen.SERIES_DETAIL)
        viewModelScope.launch {
            val result = repository.getSeriesDetails(item)
            _seriesDetails.value = result.getOrNull()
            _isSeriesLoading.value = false
        }
    }

    fun playEpisode(episode: SeriesEpisode) {
        val series = _selectedSeries.value
        val seriesTitle = series?.seriesTitle ?: series?.name ?: "TV Series"
        val group = series?.group ?: "Series"
        val item = episode.toIptvItem(seriesTitle, group)
        playStream(item)
    }

    fun dismissMiniPlayer() {
        playerManager.stop()
    }

    fun playNextChannel() {
        val current = playerUiState.value.currentItem ?: return
        val list = when (current.type) {
            IptvType.LIVE_TV -> repository.getAllLiveChannels()
            IptvType.MOVIE -> repository.getAllMovies()
            IptvType.SERIES -> repository.getAllSeries()
        }
        if (list.isEmpty()) return

        val currentIndex = list.indexOfFirst { it.id == current.id }
        val nextIndex = if (currentIndex in 0 until list.lastIndex) currentIndex + 1 else 0
        playStream(list[nextIndex])
    }

    fun playPreviousChannel() {
        val current = playerUiState.value.currentItem ?: return
        val list = when (current.type) {
            IptvType.LIVE_TV -> repository.getAllLiveChannels()
            IptvType.MOVIE -> repository.getAllMovies()
            IptvType.SERIES -> repository.getAllSeries()
        }
        if (list.isEmpty()) return

        val currentIndex = list.indexOfFirst { it.id == current.id }
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else list.lastIndex
        playStream(list[prevIndex])
    }

    fun toggleFavorite(item: IptvItem) {
        repository.toggleFavorite(item.id)
    }

    fun connectXtream(serverUrl: String, user: String, pass: String, name: String) {
        viewModelScope.launch {
            repository.addOrUpdateXtreamSource(
                name = name,
                serverUrl = serverUrl,
                username = user,
                password = pass
            )
        }
    }

    fun loadM3u(url: String, content: String, name: String) {
        viewModelScope.launch {
            repository.addOrUpdateM3uSource(
                name = name,
                m3uUrl = url,
                rawContent = content
            )
        }
    }

    fun loadDemoShowcase() {
        repository.loadDemoContent()
    }

    fun switchActiveSource(source: IptvSource) {
        viewModelScope.launch {
            repository.switchSource(source)
        }
    }

    fun deleteSource(sourceId: String) {
        repository.deleteSource(sourceId)
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
