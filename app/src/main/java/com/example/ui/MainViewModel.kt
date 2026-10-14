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

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _displayLimit = MutableStateFlow(120)
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
    val currentLanguage: StateFlow<String> = repository.appLanguage

    fun setLanguage(lang: String) {
        repository.setAppLanguage(lang)
    }

    fun toggleLanguage() {
        repository.toggleAppLanguage()
    }

    // Series detail state
    private val _selectedSeries = MutableStateFlow<IptvItem?>(null)
    val selectedSeries: StateFlow<IptvItem?> = _selectedSeries.asStateFlow()

    private val _seriesDetails = MutableStateFlow<SeriesDetails?>(null)
    val seriesDetails: StateFlow<SeriesDetails?> = _seriesDetails.asStateFlow()

    private val _isSeriesLoading = MutableStateFlow(false)
    val isSeriesLoading: StateFlow<Boolean> = _isSeriesLoading.asStateFlow()

    // Flattened list of all episodes for the currently opened TV Series (explicitly tagged as IptvType.SERIES)
    val currentSeriesEpisodes: StateFlow<List<IptvItem>> = combine(
        _seriesDetails,
        _selectedSeries,
        currentLanguage
    ) { details, series, lang ->
        val isAr = lang == "ar"
        if (details != null && details.seasons.isNotEmpty()) {
            val seriesTitle = details.title.ifBlank { series?.seriesTitle ?: series?.name ?: if (isAr) "مسلسل" else "TV Series" }
            details.seasons.flatMap { season ->
                val seasonLabel = if (isAr) "الموسم ${season.seasonNumber}" else "Season ${season.seasonNumber}"
                season.episodes.map { ep ->
                    ep.toIptvItem(seriesTitle = seriesTitle, group = seasonLabel).copy(
                        type = IptvType.SERIES,
                        seasonNumber = ep.seasonNumber,
                        episodeNumber = ep.episodeNumber
                    )
                }
            }
        } else if (series != null) {
            val targetTitle = (series.seriesTitle ?: series.name)
                .substringBefore(" - S")
                .substringBefore(" S0")
                .substringBefore(" S1")
                .trim()
            repository.getAllSeries().filter {
                it.seriesTitle?.equals(targetTitle, ignoreCase = true) == true ||
                it.name.contains(targetTitle, ignoreCase = true)
            }.map { it.copy(type = IptvType.SERIES) }
        } else {
            emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    private val _dataRefreshTrigger = combine(
        repository.totalLiveCount,
        repository.totalMoviesCount,
        repository.totalSeriesCount,
        repository.favorites
    ) { live, mov, ser, favs ->
        live + mov + ser + favs.size
    }

    val currentFilteredItems: StateFlow<List<IptvItem>> = combine(
        _currentScreen,
        _selectedCategory,
        _searchQuery,
        _displayLimit,
        _dataRefreshTrigger
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
            _displayLimit.value = 120
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
        _displayLimit.value = 120
    }

    fun setSelectedCategory(cat: String) {
        _selectedCategory.value = cat
        _displayLimit.value = 120
    }

    fun loadMoreItems() {
        _displayLimit.value += 120
    }

    fun playStream(item: IptvItem) {
        // Ensure accurate IptvType based on screen and group/URL so Movies & Series NEVER get treated as Live TV!
        val resolvedType = when {
            item.type == IptvType.MOVIE || item.type == IptvType.SERIES -> item.type
            _currentScreen.value == Screen.MOVIES -> IptvType.MOVIE
            _currentScreen.value == Screen.SERIES || _currentScreen.value == Screen.SERIES_DETAIL -> IptvType.SERIES
            item.group.contains("Series", ignoreCase = true) ||
            item.group.contains("مسلسل", ignoreCase = true) ||
            item.name.contains(Regex("""S\d{1,2}E\d{1,3}""", RegexOption.IGNORE_CASE)) -> IptvType.SERIES
            item.group.contains("Movie", ignoreCase = true) ||
            item.group.contains("Film", ignoreCase = true) ||
            item.group.contains("VOD", ignoreCase = true) ||
            item.group.contains("أفلام", ignoreCase = true) -> IptvType.MOVIE
            else -> item.type
        }

        val normalizedItem = item.copy(type = resolvedType)

        if (resolvedType == IptvType.MOVIE) {
            playerManager.closeQuickChannelList()
        }

        // If user played a Series episode directly (e.g. from Recently Watched or M3U), ensure episodes list is populated
        if (resolvedType == IptvType.SERIES && _seriesDetails.value == null) {
            _selectedSeries.value = normalizedItem
            viewModelScope.launch {
                val result = repository.getSeriesDetails(normalizedItem)
                _seriesDetails.value = result.getOrNull()
            }
        }

        repository.recordRecentlyWatched(normalizedItem)
        playerManager.playItem(normalizedItem)
        navigateTo(Screen.PLAYER)
    }

    fun openSeriesDetail(item: IptvItem) {
        val seriesItem = item.copy(type = IptvType.SERIES)
        _selectedSeries.value = seriesItem
        _seriesDetails.value = null
        _isSeriesLoading.value = true
        navigateTo(Screen.SERIES_DETAIL)
        viewModelScope.launch {
            val result = repository.getSeriesDetails(seriesItem)
            _seriesDetails.value = result.getOrNull()
            _isSeriesLoading.value = false
        }
    }

    fun playEpisode(episode: SeriesEpisode) {
        val series = _selectedSeries.value
        val isAr = currentLanguage.value == "ar"
        val seriesTitle = _seriesDetails.value?.title ?: series?.seriesTitle ?: series?.name ?: if (isAr) "مسلسل" else "TV Series"
        val group = if (isAr) "الموسم ${episode.seasonNumber}" else "Season ${episode.seasonNumber}"
        // Force type = IptvType.SERIES so the player knows this is a Series Episode, NOT a Live Channel!
        val item = episode.toIptvItem(seriesTitle, group).copy(
            type = IptvType.SERIES,
            seasonNumber = episode.seasonNumber,
            episodeNumber = episode.episodeNumber
        )
        playStream(item)
    }

    fun dismissMiniPlayer() {
        playerManager.stop()
    }

    fun playNextChannel() {
        val current = playerUiState.value.currentItem ?: return
        when (current.type) {
            IptvType.LIVE_TV -> {
                val list = repository.getAllLiveChannels()
                if (list.isEmpty()) return
                val currentIndex = list.indexOfFirst { it.id == current.id }
                val nextIndex = if (currentIndex in 0 until list.lastIndex) currentIndex + 1 else 0
                playStream(list[nextIndex])
            }
            IptvType.SERIES -> {
                val eps = currentSeriesEpisodes.value
                if (eps.isEmpty()) return
                val currentIndex = eps.indexOfFirst { it.id == current.id || it.streamUrl == current.streamUrl }
                val nextIndex = if (currentIndex in 0 until eps.lastIndex) currentIndex + 1 else 0
                playStream(eps[nextIndex])
            }
            IptvType.MOVIE -> {
                playerManager.seekTo(playerUiState.value.currentPositionMs + 10_000L)
            }
        }
    }

    fun playPreviousChannel() {
        val current = playerUiState.value.currentItem ?: return
        when (current.type) {
            IptvType.LIVE_TV -> {
                val list = repository.getAllLiveChannels()
                if (list.isEmpty()) return
                val currentIndex = list.indexOfFirst { it.id == current.id }
                val prevIndex = if (currentIndex > 0) currentIndex - 1 else list.lastIndex
                playStream(list[prevIndex])
            }
            IptvType.SERIES -> {
                val eps = currentSeriesEpisodes.value
                if (eps.isEmpty()) return
                val currentIndex = eps.indexOfFirst { it.id == current.id || it.streamUrl == current.streamUrl }
                val prevIndex = if (currentIndex > 0) currentIndex - 1 else eps.lastIndex
                playStream(eps[prevIndex])
            }
            IptvType.MOVIE -> {
                playerManager.seekTo(playerUiState.value.currentPositionMs - 10_000L)
            }
        }
    }

    fun toggleFavorite(item: IptvItem) {
        repository.toggleFavorite(item.id)
    }

    fun connectXtream(serverUrl: String, user: String, pass: String, name: String) {
        viewModelScope.launch {
            repository.addOrUpdateXtreamSource(
                name = name.trim(),
                serverUrl = serverUrl.trim(),
                username = user.trim(),
                password = pass.trim()
            )
        }
    }

    fun loadM3u(url: String, content: String, name: String) {
        viewModelScope.launch {
            repository.addOrUpdateM3uSource(
                name = name.trim(),
                m3uUrl = url.trim(),
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
