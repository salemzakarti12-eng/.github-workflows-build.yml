package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.IptvItem
import com.example.model.IptvSource
import com.example.model.IptvType
import com.example.model.SourceType
import com.example.parser.M3uParser
import com.example.parser.XtreamCodesClient
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
import java.util.concurrent.TimeUnit

class IptvRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("hypnostream_prefs", Context.MODE_PRIVATE)
    private val xtreamClient = XtreamCodesClient()
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val _sources = MutableStateFlow<List<IptvSource>>(emptyList())
    val sources: StateFlow<List<IptvSource>> = _sources.asStateFlow()

    private val _activeSource = MutableStateFlow<IptvSource?>(null)
    val activeSource: StateFlow<IptvSource?> = _activeSource.asStateFlow()

    private val _liveChannels = MutableStateFlow<List<IptvItem>>(emptyList())
    val liveChannels: StateFlow<List<IptvItem>> = _liveChannels.asStateFlow()

    private val _movies = MutableStateFlow<List<IptvItem>>(emptyList())
    val movies: StateFlow<List<IptvItem>> = _movies.asStateFlow()

    private val _series = MutableStateFlow<List<IptvItem>>(emptyList())
    val series: StateFlow<List<IptvItem>> = _series.asStateFlow()

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
        // Load favorite IDs
        val favStrings = prefs.getStringSet("favorites", emptySet()) ?: emptySet()
        _favorites.value = favStrings

        // Load sources
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

        // If no sources exist, create default Demo source
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

        // Determine active source
        val active = loadedSources.firstOrNull { it.isActive } ?: loadedSources.first()
        _activeSource.value = active

        // Load initial stream content for active source
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

        // Update in current lists
        _liveChannels.value = _liveChannels.value.map {
            if (it.id == itemId) it.copy(isFavorite = current.contains(itemId)) else it
        }
        _movies.value = _movies.value.map {
            if (it.id == itemId) it.copy(isFavorite = current.contains(itemId)) else it
        }
        _series.value = _series.value.map {
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

        _liveChannels.value = lives
        _movies.value = movs
        _series.value = sers

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
            val favs = _favorites.value
            val live = items.filter { it.type == IptvType.LIVE_TV }.map { it.copy(isFavorite = favs.contains(it.id)) }
            val movs = items.filter { it.type == IptvType.MOVIE }.map { it.copy(isFavorite = favs.contains(it.id)) }
            val sers = items.filter { it.type == IptvType.SERIES }.map { it.copy(isFavorite = favs.contains(it.id)) }

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

            _liveChannels.value = live
            _movies.value = movs
            _series.value = sers

            updateActiveSource(source)
            _isLoading.value = false
            _statusMessage.value = "Connected! Loaded ${live.size} channels, ${movs.size} movies."
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
        _statusMessage.value = "Parsing M3U playlist..."

        try {
            val content = if (m3uUrl.isNotBlank()) {
                val req = Request.Builder().url(m3uUrl).header("User-Agent", "HypnoStream-IPTV/1.0").build()
                val resp = httpClient.newCall(req).execute()
                if (!resp.isSuccessful) {
                    throw Exception("HTTP ${resp.code}: Failed to download M3U playlist")
                }
                resp.body?.string().orEmpty()
            } else {
                rawContent
            }

            if (content.isBlank() || !content.contains("#EXTM3U", ignoreCase = true)) {
                _isLoading.value = false
                _statusMessage.value = "Invalid M3U playlist (missing #EXTM3U header)"
                return@withContext Result.failure(Exception("Invalid M3U header"))
            }

            val items = M3uParser.parse(content)
            val favs = _favorites.value
            val live = items.filter { it.type == IptvType.LIVE_TV }.map { it.copy(isFavorite = favs.contains(it.id)) }
            val movs = items.filter { it.type == IptvType.MOVIE }.map { it.copy(isFavorite = favs.contains(it.id)) }
            val sers = items.filter { it.type == IptvType.SERIES }.map { it.copy(isFavorite = favs.contains(it.id)) }

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
                statusText = "Active (${items.size} streams)"
            )

            _liveChannels.value = live
            _movies.value = movs
            _series.value = sers

            updateActiveSource(source)
            _isLoading.value = false
            _statusMessage.value = "M3U loaded! Found ${items.size} streams."
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
        // Deactivate all others
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
}
