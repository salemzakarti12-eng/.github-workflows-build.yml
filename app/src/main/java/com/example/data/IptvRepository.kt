package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.CategoryInfo
import com.example.model.IptvItem
import com.example.model.IptvSource
import com.example.model.IptvType
import com.example.model.SourceType
import com.example.parser.M3uParser
import com.example.parser.XtreamCodesClient
import com.example.parser.XtreamUrlDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class IptvRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("hypnostream_prefs", Context.MODE_PRIVATE)
    private val xtreamClient = XtreamCodesClient()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val _sources = MutableStateFlow<List<IptvSource>>(emptyList())
    val sources: StateFlow<List<IptvSource>> = _sources.asStateFlow()

    private val _activeSource = MutableStateFlow<IptvSource?>(null)
    val activeSource: StateFlow<IptvSource?> = _activeSource.asStateFlow()

    // Counts for the 3 Hypnotix Dashboard cards
    private val _totalLiveCount = MutableStateFlow(0)
    val totalLiveCount: StateFlow<Int> = _totalLiveCount.asStateFlow()

    private val _totalMoviesCount = MutableStateFlow(0)
    val totalMoviesCount: StateFlow<Int> = _totalMoviesCount.asStateFlow()

    private val _totalSeriesCount = MutableStateFlow(0)
    val totalSeriesCount: StateFlow<Int> = _totalSeriesCount.asStateFlow()

    // Category lists for Category-First UI navigation
    private val _liveCategories = MutableStateFlow<List<CategoryInfo>>(emptyList())
    val liveCategories: StateFlow<List<CategoryInfo>> = _liveCategories.asStateFlow()

    private val _movieCategories = MutableStateFlow<List<CategoryInfo>>(emptyList())
    val movieCategories: StateFlow<List<CategoryInfo>> = _movieCategories.asStateFlow()

    private val _seriesCategories = MutableStateFlow<List<CategoryInfo>>(emptyList())
    val seriesCategories: StateFlow<List<CategoryInfo>> = _seriesCategories.asStateFlow()

    // Memory-protected internal maps (grouped by category in background memory)
    private val liveStreamsByCategory = ConcurrentHashMap<String, MutableList<IptvItem>>()
    private val movieStreamsByCategory = ConcurrentHashMap<String, MutableList<IptvItem>>()
    private val seriesStreamsByCategory = ConcurrentHashMap<String, MutableList<IptvItem>>()

    // All channels flat lists for quick access (e.g. next/prev channel)
    private val allLiveList = ArrayList<IptvItem>()
    private val allMoviesList = ArrayList<IptvItem>()
    private val allSeriesList = ArrayList<IptvItem>()

    // Current displayed channels list (capped and paginated to protect Compose)
    private val _displayedItems = MutableStateFlow<List<IptvItem>>(emptyList())
    val displayedItems: StateFlow<List<IptvItem>> = _displayedItems.asStateFlow()

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private val _recentlyWatched = MutableStateFlow<List<IptvItem>>(emptyList())
    val recentlyWatched: StateFlow<List<IptvItem>> = _recentlyWatched.asStateFlow()

    private val _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        loadPersistedState()
    }

    private fun loadPersistedState() {
        val favStrings = prefs.getStringSet("favorites", emptySet()) ?: emptySet()
        _favorites.value = favStrings

        val sourcesJson = prefs.getString("saved_sources", null)
        val loadedSources = mutableListOf<IptvSource>()
        if (!sourcesJson.isNullOrEmpty()) {
            try {
                val array = JSONArray(sourcesJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    loadedSources.add(
                        IptvSource(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            type = SourceType.valueOf(obj.getString("type")),
                            serverUrl = obj.optString("serverUrl", ""),
                            username = obj.optString("username", ""),
                            password = obj.optString("password", ""),
                            m3uUrl = obj.optString("m3uUrl", ""),
                            m3uContent = obj.optString("m3uContent", ""),
                            isActive = obj.optBoolean("isActive", false),
                            channelCount = obj.optInt("channelCount", 0),
                            movieCount = obj.optInt("movieCount", 0),
                            seriesCount = obj.optInt("seriesCount", 0),
                            expirationDate = obj.optString("expirationDate", null)
                        )
                    )
                }
            } catch (_: Exception) {}
        }

        if (loadedSources.isEmpty()) {
            val demo = IptvSource(
                id = "demo_source_default",
                name = "HypnoStream Demo Showcase",
                type = SourceType.DEMO,
                isActive = true,
                channelCount = DemoStreams.demoChannels.size,
                movieCount = DemoStreams.demoMovies.size,
                seriesCount = DemoStreams.demoSeries.size,
                expirationDate = "Permanent Demo"
            )
            loadedSources.add(demo)
        }

        _sources.value = loadedSources
        val active = loadedSources.firstOrNull { it.isActive } ?: loadedSources.first()
        _activeSource.value = active

        if (active.type == SourceType.DEMO) {
            loadDemoContent(active)
        }
    }

    private fun persistSources() {
        val array = JSONArray()
        for (source in _sources.value) {
            val obj = JSONObject().apply {
                put("id", source.id)
                put("name", source.name)
                put("type", source.type.name)
                put("serverUrl", source.serverUrl)
                put("username", source.username)
                put("password", source.password)
                put("m3uUrl", source.m3uUrl)
                put("m3uContent", source.m3uContent)
                put("isActive", source.isActive)
                put("channelCount", source.channelCount)
                put("movieCount", source.movieCount)
                put("seriesCount", source.seriesCount)
                put("expirationDate", source.expirationDate ?: "")
            }
            array.put(obj)
        }
        prefs.edit().putString("saved_sources", array.toString()).apply()
    }

    fun toggleFavorite(itemId: String) {
        val current = _favorites.value.toMutableSet()
        if (current.contains(itemId)) {
            current.remove(itemId)
        } else {
            current.add(itemId)
        }
        _favorites.value = current
        prefs.edit().putStringSet("favorites", current).apply()

        // Update displayed list
        _displayedItems.value = _displayedItems.value.map {
            if (it.id == itemId) it.copy(isFavorite = current.contains(itemId)) else it
        }
    }

    fun recordRecentlyWatched(item: IptvItem) {
        val current = _recentlyWatched.value.toMutableList()
        current.removeAll { it.id == item.id }
        current.add(0, item)
        if (current.size > 15) {
            current.removeAt(current.lastIndex)
        }
        _recentlyWatched.value = current
    }

    fun loadDemoContent(source: IptvSource? = null) {
        val currentFavs = _favorites.value
        val lives = DemoStreams.demoChannels.map { it.copy(isFavorite = currentFavs.contains(it.id)) }
        val movs = DemoStreams.demoMovies.map { it.copy(isFavorite = currentFavs.contains(it.id)) }
        val sers = DemoStreams.demoSeries.map { it.copy(isFavorite = currentFavs.contains(it.id)) }

        indexStreams(lives, movs, sers)

        val targetSource = source ?: _sources.value.firstOrNull { it.type == SourceType.DEMO }
        targetSource?.let {
            val updated = it.copy(
                isActive = true,
                channelCount = lives.size,
                movieCount = movs.size,
                seriesCount = sers.size,
                statusText = "Active"
            )
            updateActiveSource(updated)
        }
        _statusMessage.value = "Demo streams ready"
    }

    /**
     * Internal indexing of streams into background memory grouped by category.
     * Prevents giant 50k item arrays from locking up the UI thread.
     */
    private fun indexStreams(
        liveList: List<IptvItem>,
        movList: List<IptvItem>,
        serList: List<IptvItem>
    ) {
        liveStreamsByCategory.clear()
        movieStreamsByCategory.clear()
        seriesStreamsByCategory.clear()
        allLiveList.clear()
        allMoviesList.clear()
        allSeriesList.clear()

        allLiveList.addAll(liveList)
        allMoviesList.addAll(movList)
        allSeriesList.addAll(serList)

        for (item in liveList) {
            liveStreamsByCategory.getOrPut(item.group) { ArrayList() }.add(item)
        }
        for (item in movList) {
            movieStreamsByCategory.getOrPut(item.group) { ArrayList() }.add(item)
        }
        for (item in serList) {
            seriesStreamsByCategory.getOrPut(item.group) { ArrayList() }.add(item)
        }

        _totalLiveCount.value = liveList.size
        _totalMoviesCount.value = movList.size
        _totalSeriesCount.value = serList.size

        // Build Category summaries with item counts
        _liveCategories.value = liveStreamsByCategory.map { (cat, items) ->
            CategoryInfo(id = "cat_live_$cat", name = cat, itemCount = items.size, type = IptvType.LIVE_TV)
        }.sortedByDescending { it.itemCount }

        _movieCategories.value = movieStreamsByCategory.map { (cat, items) ->
            CategoryInfo(id = "cat_mov_$cat", name = cat, itemCount = items.size, type = IptvType.MOVIE)
        }.sortedByDescending { it.itemCount }

        _seriesCategories.value = seriesStreamsByCategory.map { (cat, items) ->
            CategoryInfo(id = "cat_ser_$cat", name = cat, itemCount = items.size, type = IptvType.SERIES)
        }.sortedByDescending { it.itemCount }

        // Default displayed items to top 100 channels of first category or all live
        val initialBatch = liveList.take(100)
        _displayedItems.value = initialBatch
    }

    /**
     * Category-first memory protection query:
     * Capped to [limit] (default 100) to keep Compose lightweight and prevent garbage collection freezes.
     */
    fun queryStreams(
        type: IptvType,
        category: String,
        query: String,
        page: Int = 0,
        pageSize: Int = 100
    ): List<IptvItem> {
        val categoryMap = when (type) {
            IptvType.LIVE_TV -> liveStreamsByCategory
            IptvType.MOVIE -> movieStreamsByCategory
            IptvType.SERIES -> seriesStreamsByCategory
        }

        val favs = _favorites.value
        val sourceItems: Sequence<IptvItem> = when {
            category.equals("Favorites", ignoreCase = true) -> {
                val fullList = when (type) {
                    IptvType.LIVE_TV -> allLiveList
                    IptvType.MOVIE -> allMoviesList
                    IptvType.SERIES -> allSeriesList
                }
                fullList.asSequence().filter { favs.contains(it.id) }
            }
            category.equals("All", ignoreCase = true) -> {
                val fullList = when (type) {
                    IptvType.LIVE_TV -> allLiveList
                    IptvType.MOVIE -> allMoviesList
                    IptvType.SERIES -> allSeriesList
                }
                fullList.asSequence()
            }
            else -> {
                (categoryMap[category] ?: emptyList()).asSequence()
            }
        }

        val filtered = if (query.isBlank()) {
            sourceItems
        } else {
            sourceItems.filter { item ->
                item.name.contains(query, ignoreCase = true) ||
                item.group.contains(query, ignoreCase = true) ||
                (item.seriesTitle?.contains(query, ignoreCase = true) == true)
            }
        }

        val offset = page * pageSize
        return filtered.drop(offset).take(pageSize).map {
            if (favs.contains(it.id)) it.copy(isFavorite = true) else it
        }.toList()
    }

    suspend fun addOrUpdateXtreamSource(
        name: String,
        serverUrl: String,
        username: String,
        password: String
    ): Result<IptvSource> = withContext(Dispatchers.IO) {
        _isLoading.value = true
        _statusMessage.value = "Connecting to Xtream Codes server..."

        try {
            val authResult = xtreamClient.authenticateAndLoad(serverUrl, username, password)
            if (!authResult.isSuccess) {
                _isLoading.value = false
                _statusMessage.value = authResult.serverMessage ?: "Authentication failed"
                return@withContext Result.failure(Exception(authResult.serverMessage ?: "Authentication failed"))
            }

            val items = authResult.items
            val live = items.filter { it.type == IptvType.LIVE_TV }
            val movs = items.filter { it.type == IptvType.MOVIE }
            val sers = items.filter { it.type == IptvType.SERIES }

            val source = IptvSource(
                id = UUID.randomUUID().toString(),
                name = name.ifBlank { "Xtream ($username)" },
                type = SourceType.XTREAM_CODES,
                serverUrl = serverUrl,
                username = username,
                password = password,
                isActive = true,
                channelCount = live.size,
                movieCount = movs.size,
                seriesCount = sers.size,
                expirationDate = authResult.expirationDate,
                statusText = "Connected (${live.size + movs.size + sers.size} items)"
            )

            indexStreams(live, movs, sers)
            updateActiveSource(source)

            _isLoading.value = false
            _statusMessage.value = "Connected! Loaded %,d channels, %,d movies, %,d series.".format(
                live.size, movs.size, sers.size
            )
            Result.success(source)
        } catch (e: Exception) {
            _isLoading.value = false
            _statusMessage.value = "Error: ${e.localizedMessage}"
            Result.failure(e)
        }
    }

    suspend fun addOrUpdateM3uSource(
        name: String,
        m3uUrl: String,
        rawContent: String = ""
    ): Result<IptvSource> = withContext(Dispatchers.IO) {
        _isLoading.value = true

        // Smart M3U-to-Xtream Auto-Detection
        if (m3uUrl.isNotBlank()) {
            val detected = XtreamUrlDetector.detect(m3uUrl)
            if (detected != null) {
                _statusMessage.value = "Detected Xtream Codes IPTV provider (${detected.username}). Loading via fast API..."
                return@withContext addOrUpdateXtreamSource(
                    name = name.ifBlank { "Xtream (${detected.username})" },
                    serverUrl = detected.serverUrl,
                    username = detected.username,
                    password = detected.password
                )
            }
        }

        _statusMessage.value = "Streaming & parsing M3U playlist..."

        try {
            val items: List<IptvItem> = if (m3uUrl.isNotBlank()) {
                val req = Request.Builder()
                    .url(m3uUrl)
                    .header("User-Agent", "HypnoStream-IPTV/1.0")
                    .build()

                val resp = httpClient.newCall(req).execute()
                if (!resp.isSuccessful) {
                    throw Exception("HTTP ${resp.code}: Failed to download M3U playlist")
                }

                val body = resp.body ?: throw Exception("Empty M3U response stream")
                // STREAMING PARSER: Never call .string() or .readText()!
                val bufferedReader = body.byteStream().bufferedReader(Charsets.UTF_8)
                M3uParser.parseStream(bufferedReader) { progress ->
                    _statusMessage.value = "Loading streams... %,d found".format(progress.totalCount)
                }
            } else {
                if (rawContent.isBlank() || !rawContent.contains("#EXTM3U", ignoreCase = true)) {
                    throw Exception("Invalid M3U playlist (missing #EXTM3U header)")
                }
                M3uParser.parse(rawContent)
            }

            if (items.isEmpty()) {
                throw Exception("No playable streams found in M3U playlist")
            }

            val live = items.filter { it.type == IptvType.LIVE_TV }
            val movs = items.filter { it.type == IptvType.MOVIE }
            val sers = items.filter { it.type == IptvType.SERIES }

            val source = IptvSource(
                id = UUID.randomUUID().toString(),
                name = name.ifBlank { if (m3uUrl.isNotBlank()) "M3U (${m3uUrl.take(25)}...)" else "Custom M3U" },
                type = if (m3uUrl.isNotBlank()) SourceType.M3U_URL else SourceType.M3U_TEXT,
                m3uUrl = m3uUrl,
                m3uContent = if (m3uUrl.isBlank()) rawContent else "",
                isActive = true,
                channelCount = live.size,
                movieCount = movs.size,
                seriesCount = sers.size,
                statusText = "Active (%,d streams)".format(items.size)
            )

            indexStreams(live, movs, sers)
            updateActiveSource(source)

            _isLoading.value = false
            _statusMessage.value = "M3U loaded! Found %,d streams (Live: %,d | Movies: %,d | Series: %,d)".format(
                items.size, live.size, movs.size, sers.size
            )
            Result.success(source)
        } catch (e: Exception) {
            _isLoading.value = false
            _statusMessage.value = "M3U Error: ${e.localizedMessage}"
            Result.failure(e)
        }
    }

    private fun updateActiveSource(source: IptvSource) {
        val current = _sources.value.toMutableList()
        val index = current.indexOfFirst { it.id == source.id }
        val updatedList = current.map { it.copy(isActive = it.id == source.id) }.toMutableList()
        if (index >= 0) {
            updatedList[index] = source.copy(isActive = true)
        } else {
            updatedList.add(source.copy(isActive = true))
        }
        _sources.value = updatedList
        _activeSource.value = source.copy(isActive = true)
        persistSources()
    }

    suspend fun switchSource(source: IptvSource) {
        when (source.type) {
            SourceType.DEMO -> {
                loadDemoContent(source)
            }
            SourceType.XTREAM_CODES -> {
                addOrUpdateXtreamSource(
                    name = source.name,
                    serverUrl = source.serverUrl,
                    username = source.username,
                    password = source.password
                )
            }
            SourceType.M3U_URL -> {
                addOrUpdateM3uSource(
                    name = source.name,
                    m3uUrl = source.m3uUrl
                )
            }
            SourceType.M3U_TEXT -> {
                addOrUpdateM3uSource(
                    name = source.name,
                    m3uUrl = "",
                    rawContent = source.m3uContent
                )
            }
        }
    }

    fun deleteSource(sourceId: String) {
        val current = _sources.value.filterNot { it.id == sourceId }
        _sources.value = current
        persistSources()
        if (_activeSource.value?.id == sourceId) {
            current.firstOrNull()?.let {
                loadDemoContent(it)
            }
        }
    }

    fun getAllLiveChannels(): List<IptvItem> = allLiveList
    fun getAllMovies(): List<IptvItem> = allMoviesList
    fun getAllSeries(): List<IptvItem> = allSeriesList
}
