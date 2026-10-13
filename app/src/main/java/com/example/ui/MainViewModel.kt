package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.IptvRepository
import com.example.model.IptvItem
import com.example.model.IptvSource
import com.example.model.IptvType
import com.example.model.PlayerUiState
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
    SOURCES,
    PLAYER
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = IptvRepository(application)
    val playerManager = ExoPlayerManager(application, viewModelScope)

    private val _currentScreen = MutableStateFlow(Screen.DASHBOARD)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _screenHistory = mutableListOf<Screen>()

    // Search and category filtering
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    val sources: StateFlow<List<IptvSource>> = repository.sources
    val activeSource: StateFlow<IptvSource?> = repository.activeSource
    val liveChannels: StateFlow<List<IptvItem>> = repository.liveChannels
    val movies: StateFlow<List<IptvItem>> = repository.movies
    val series: StateFlow<List<IptvItem>> = repository.series
    val favorites: StateFlow<Set<String>> = repository.favorites
    val recentlyWatched: StateFlow<List<IptvItem>> = repository.recentlyWatched
    val isLoading: StateFlow<Boolean> = repository.isLoading
    val statusMessage: StateFlow<String?> = repository.statusMessage
    val playerUiState: StateFlow<PlayerUiState> = playerManager.uiState

    // Filtered lists for the active browsing screen
    private val currentScreenItems = combine(
        _currentScreen,
        repository.liveChannels,
        repository.movies,
        repository.series
    ) { screen, channels, movs, sers ->
        when (screen) {
            Screen.LIVE_CHANNELS -> channels
            Screen.MOVIES -> movs
            Screen.SERIES -> sers
            else -> channels
        }
    }

    val currentFilteredItems: StateFlow<List<IptvItem>> = combine(
        currentScreenItems,
        _searchQuery,
        _selectedCategory
    ) { rawList, query, category ->
        rawList.filter { item ->
            val matchesCategory = (category == "All") || (category == "Favorites" && item.isFavorite) || item.group.equals(category, ignoreCase = true)
            val matchesQuery = query.isBlank() ||
                    item.name.contains(query, ignoreCase = true) ||
                    item.group.contains(query, ignoreCase = true) ||
                    (item.seriesTitle?.contains(query, ignoreCase = true) == true)
            matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun navigateTo(screen: Screen) {
        if (_currentScreen.value != screen) {
            _screenHistory.add(_currentScreen.value)
            _currentScreen.value = screen
            _searchQuery.value = ""
            _selectedCategory.value = "All"
        }
    }

    fun handleBackPress(): Boolean {
        if (_currentScreen.value == Screen.PLAYER) {
            if (playerUiState.value.isQuickChannelListOpen) {
                playerManager.closeQuickChannelList()
                return true
            }
            // Navigate back to the screen we came from or Dashboard
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
    }

    fun setSelectedCategory(cat: String) {
        _selectedCategory.value = cat
    }

    fun playStream(item: IptvItem) {
        repository.recordRecentlyWatched(item)
        playerManager.playItem(item)
        navigateTo(Screen.PLAYER)
    }

    fun playNextChannel() {
        val current = playerUiState.value.currentItem ?: return
        val list = when (current.type) {
            IptvType.LIVE_TV -> liveChannels.value
            IptvType.MOVIE -> movies.value
            IptvType.SERIES -> series.value
        }
        if (list.isEmpty()) return

        val currentIndex = list.indexOfFirst { it.id == current.id }
        val nextIndex = if (currentIndex in 0 until list.lastIndex) currentIndex + 1 else 0
        playStream(list[nextIndex])
    }

    fun playPreviousChannel() {
        val current = playerUiState.value.currentItem ?: return
        val list = when (current.type) {
            IptvType.LIVE_TV -> liveChannels.value
            IptvType.MOVIE -> movies.value
            IptvType.SERIES -> series.value
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
