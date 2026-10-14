package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.CategoryInfo
import com.example.model.IptvItem
import com.example.model.IptvSource
import com.example.model.IptvType
import com.example.model.SeriesDetails
import com.example.model.SeriesEpisode
import com.example.model.SeriesSeason
import com.example.model.SourceType
import com.example.parser.M3uParser
import com.example.parser.XtreamCodesClient
import com.example.parser.XtreamUrlDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
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

    private val _appLanguage = MutableStateFlow(prefs.getString("app_language", "en") ?: "en")
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    private val isAr: Boolean
        get() = _appLanguage.value == "ar"

    fun setAppLanguage(lang: String) {
        val normalized = if (lang.equals("ar", ignoreCase = true)) "ar" else "en"
        _appLanguage.value = normalized
        prefs.edit().putString("app_language", normalized).apply()
    }

    fun toggleAppLanguage() {
        val next = if (_appLanguage.value == "ar") "en" else "ar"
        setAppLanguage(next)
    }

    init {
        loadPersistedState()
    }

    private fun normalizeServerUrl(raw: String): String {
        var clean = raw.trim().trimEnd('/')
        if (!clean.startsWith("http://", ignoreCase = true) && !clean.startsWith("https://", ignoreCase = true)) {
            clean = "http://$clean"
        }
        return clean
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
        val savedActiveId = prefs.getString("active_source_id", null)
        val active = loadedSources.firstOrNull { it.id == savedActiveId }
            ?: loadedSources.firstOrNull { it.isActive }
            ?: loadedSources.first()
        _activeSource.value = active

        // Auto-reconnect saved active playlist on app launch
        when (active.type) {
            SourceType.DEMO -> {
                loadDemoContent(active)
            }
            SourceType.XTREAM_CODES -> {
                repositoryScope.launch {
                    addOrUpdateXtreamSource(
                        name = active.name,
                        serverUrl = active.serverUrl,
                        username = active.username,
                        password = active.password
                    )
                }
            }
            SourceType.M3U_URL -> {
                repositoryScope.launch {
                    addOrUpdateM3uSource(
                        name = active.name,
                        m3uUrl = active.m3uUrl
                    )
                }
            }
            SourceType.M3U_TEXT -> {
                repositoryScope.launch {
                    addOrUpdateM3uSource(
                        name = active.name,
                        m3uUrl = "",
                        rawContent = active.m3uContent
                    )
                }
            }
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
                statusText = if (isAr) "نشط" else "Active"
            )
            updateActiveSource(updated)
        }
        _statusMessage.value = if (isAr) "تم تشغيل القنوات التجريبية" else "Demo streams ready"
    }

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

        _liveCategories.value = liveStreamsByCategory.map { (cat, items) ->
            CategoryInfo(id = "cat_live_$cat", name = cat, itemCount = items.size, type = IptvType.LIVE_TV)
        }.sortedByDescending { it.itemCount }

        _movieCategories.value = movieStreamsByCategory.map { (cat, items) ->
            CategoryInfo(id = "cat_mov_$cat", name = cat, itemCount = items.size, type = IptvType.MOVIE)
        }.sortedByDescending { it.itemCount }

        _seriesCategories.value = seriesStreamsByCategory.map { (cat, items) ->
            CategoryInfo(id = "cat_ser_$cat", name = cat, itemCount = items.size, type = IptvType.SERIES)
        }.sortedByDescending { it.itemCount }

        val initialBatch = liveList.take(100)
        _displayedItems.value = initialBatch
    }

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
            category.equals("Favorites", ignoreCase = true) || category == "المفضلة" -> {
                val fullList = when (type) {
                    IptvType.LIVE_TV -> allLiveList
                    IptvType.MOVIE -> allMoviesList
                    IptvType.SERIES -> allSeriesList
                }
                fullList.asSequence().filter { favs.contains(it.id) }
            }
            category.equals("All", ignoreCase = true) || category == "الكل" -> {
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

    private fun downloadM3uStream(url: String): List<IptvItem> {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "IPTVSmartersPlayer")
            .header("Accept", "*/*")
            .build()

        val resp = httpClient.newCall(req).execute()
        if (!resp.isSuccessful) {
            throw Exception("HTTP ${resp.code}: Failed to download playlist")
        }

        val body = resp.body ?: throw Exception("Empty playlist stream")
        val bufferedReader = body.byteStream().bufferedReader(Charsets.UTF_8)
        return M3uParser.parseStream(bufferedReader) { progress ->
            _statusMessage.value = if (isAr) {
                "جاري تحميل القنوات... تم العثور على %,d".format(progress.totalCount)
            } else {
                "Loading streams... %,d found".format(progress.totalCount)
            }
        }
    }

    suspend fun addOrUpdateXtreamSource(
        name: String,
        serverUrl: String,
        username: String,
        password: String
    ): Result<IptvSource> = withContext(Dispatchers.IO) {
        _isLoading.value = true
        val cleanUrl = normalizeServerUrl(serverUrl)
        val cleanUser = username.trim()
        val cleanPass = password.trim()

        _statusMessage.value = if (isAr) "جاري الاتصال بسيرفر Xtream..." else "Connecting to Xtream Codes server..."

        try {
            // Try primary URL, and if it ends with :80 and fails, also test without :80
            var authResult = xtreamClient.authenticateAndLoad(cleanUrl, cleanUser, cleanPass)
            if (!authResult.isSuccess && cleanUrl.endsWith(":80")) {
                val altUrl = cleanUrl.removeSuffix(":80")
                authResult = xtreamClient.authenticateAndLoad(altUrl, cleanUser, cleanPass)
            }

            val items: List<IptvItem>
            val expDate: String?

            if (authResult.isSuccess && authResult.items.isNotEmpty()) {
                items = authResult.items
                expDate = authResult.expirationDate
            } else {
                // Automatic Fallback: Load via Xtream get.php M3U Plus endpoint
                _statusMessage.value = if (isAr) {
                    "جاري التحميل عبر رابط Xtream المباشر..."
                } else {
                    "Loading Xtream playlist via direct stream..."
                }
                val fallbackUrls = buildList {
                    add("$cleanUrl/get.php?username=$cleanUser&password=$cleanPass&type=m3u_plus&output=ts")
                    if (cleanUrl.endsWith(":80")) {
                        val noPort = cleanUrl.removeSuffix(":80")
                        add("$noPort/get.php?username=$cleanUser&password=$cleanPass&type=m3u_plus&output=ts")
                    }
                }

                var loadedFallback: List<IptvItem> = emptyList()
                for (fbUrl in fallbackUrls) {
                    try {
                        loadedFallback = downloadM3uStream(fbUrl)
                        if (loadedFallback.isNotEmpty()) break
                    } catch (_: Exception) {}
                }

                if (loadedFallback.isEmpty()) {
                    _isLoading.value = false
                    val err = if (isAr) {
                        "فشل تسجيل الدخول إلى سيرفر Xtream. تأكد من الرابط واسم المستخدم وكلمة المرور."
                    } else {
                        authResult.serverMessage ?: "Authentication failed on Xtream Codes server."
                    }
                    _statusMessage.value = err
                    return@withContext Result.failure(Exception(err))
                }
                items = loadedFallback
                expDate = null
            }

            val live = items.filter { it.type == IptvType.LIVE_TV }
            val movs = items.filter { it.type == IptvType.MOVIE }
            val sers = items.filter { it.type == IptvType.SERIES }

            val existing = _sources.value.firstOrNull {
                it.type == SourceType.XTREAM_CODES &&
                normalizeServerUrl(it.serverUrl).equals(cleanUrl, ignoreCase = true) &&
                it.username.trim().equals(cleanUser, ignoreCase = true)
            }
            val sourceId = existing?.id ?: UUID.randomUUID().toString()

            val source = IptvSource(
                id = sourceId,
                name = name.trim().ifBlank { existing?.name ?: "Xtream ($cleanUser)" },
                type = SourceType.XTREAM_CODES,
                serverUrl = cleanUrl,
                username = cleanUser,
                password = cleanPass,
                isActive = true,
                channelCount = live.size,
                movieCount = movs.size,
                seriesCount = sers.size,
                expirationDate = expDate,
                statusText = if (isAr) {
                    "متصل (%,d عنصر)".format(live.size + movs.size + sers.size)
                } else {
                    "Connected (${live.size + movs.size + sers.size} items)"
                }
            )

            indexStreams(live, movs, sers)
            updateActiveSource(source)

            _isLoading.value = false
            _statusMessage.value = if (isAr) {
                "تم الاتصال! تم تحميل %,d قناة، %,d فيلم، %,d مسلسل.".format(live.size, movs.size, sers.size)
            } else {
                "Connected! Loaded %,d channels, %,d movies, %,d series.".format(live.size, movs.size, sers.size)
            }
            Result.success(source)
        } catch (e: Exception) {
            _isLoading.value = false
            _statusMessage.value = if (isAr) "خطأ: ${e.localizedMessage}" else "Error: ${e.localizedMessage}"
            Result.failure(e)
        }
    }

    suspend fun addOrUpdateM3uSource(
        name: String,
        m3uUrl: String,
        rawContent: String = ""
    ): Result<IptvSource> = withContext(Dispatchers.IO) {
        _isLoading.value = true
        val cleanM3uUrl = m3uUrl.trim()

        // Smart M3U-to-Xtream Auto-Detection
        if (cleanM3uUrl.isNotBlank()) {
            val detected = XtreamUrlDetector.detect(cleanM3uUrl)
            if (detected != null) {
                _statusMessage.value = if (isAr) {
                    "تم اكتشاف اشتراك Xtream (${detected.username}). جاري التحميل السريع..."
                } else {
                    "Detected Xtream Codes IPTV provider (${detected.username}). Loading via fast API..."
                }
                return@withContext addOrUpdateXtreamSource(
                    name = name.ifBlank { "Xtream (${detected.username})" },
                    serverUrl = detected.serverUrl,
                    username = detected.username,
                    password = detected.password
                )
            }
        }

        _statusMessage.value = if (isAr) "جاري تحميل وقراءة ملف M3U..." else "Streaming & parsing M3U playlist..."

        try {
            val items: List<IptvItem> = if (cleanM3uUrl.isNotBlank()) {
                downloadM3uStream(cleanM3uUrl)
            } else {
                if (rawContent.isBlank() || !rawContent.contains("#EXTM3U", ignoreCase = true)) {
                    throw Exception(if (isAr) "ملف M3U غير صالح" else "Invalid M3U playlist (missing #EXTM3U header)")
                }
                M3uParser.parse(rawContent)
            }

            if (items.isEmpty()) {
                throw Exception(if (isAr) "لم يتم العثور على قنوات في هذا الرابط" else "No playable streams found in M3U playlist")
            }

            val live = items.filter { it.type == IptvType.LIVE_TV }
            val movs = items.filter { it.type == IptvType.MOVIE }
            val sers = items.filter { it.type == IptvType.SERIES }

            val existing = if (cleanM3uUrl.isNotBlank()) {
                _sources.value.firstOrNull {
                    it.type == SourceType.M3U_URL &&
                    it.m3uUrl.trim().equals(cleanM3uUrl, ignoreCase = true)
                }
            } else null
            val sourceId = existing?.id ?: UUID.randomUUID().toString()

            val source = IptvSource(
                id = sourceId,
                name = name.trim().ifBlank { existing?.name ?: if (cleanM3uUrl.isNotBlank()) "M3U (${cleanM3uUrl.take(25)}...)" else "Custom M3U" },
                type = if (cleanM3uUrl.isNotBlank()) SourceType.M3U_URL else SourceType.M3U_TEXT,
                m3uUrl = cleanM3uUrl,
                m3uContent = if (cleanM3uUrl.isBlank()) rawContent else "",
                isActive = true,
                channelCount = live.size,
                movieCount = movs.size,
                seriesCount = sers.size,
                statusText = if (isAr) "نشط (%,d قناة)".format(items.size) else "Active (%,d streams)".format(items.size)
            )

            indexStreams(live, movs, sers)
            updateActiveSource(source)

            _isLoading.value = false
            _statusMessage.value = if (isAr) {
                "تم التحميل! القنوات: %,d | الأفلام: %,d | المسلسلات: %,d".format(live.size, movs.size, sers.size)
            } else {
                "M3U loaded! Found %,d streams (Live: %,d | Movies: %,d | Series: %,d)".format(
                    items.size, live.size, movs.size, sers.size
                )
            }
            Result.success(source)
        } catch (e: Exception) {
            _isLoading.value = false
            _statusMessage.value = if (isAr) "خطأ M3U: ${e.localizedMessage}" else "M3U Error: ${e.localizedMessage}"
            Result.failure(e)
        }
    }

    private fun updateActiveSource(source: IptvSource) {
        val current = _sources.value.toMutableList()
        val index = current.indexOfFirst {
            it.id == source.id ||
            (it.type == SourceType.XTREAM_CODES && source.type == SourceType.XTREAM_CODES &&
             normalizeServerUrl(it.serverUrl).equals(normalizeServerUrl(source.serverUrl), ignoreCase = true) &&
             it.username.trim().equals(source.username.trim(), ignoreCase = true)) ||
            (it.type == SourceType.M3U_URL && source.type == SourceType.M3U_URL &&
             source.m3uUrl.isNotBlank() && it.m3uUrl.trim().equals(source.m3uUrl.trim(), ignoreCase = true))
        }
        val updatedSource = source.copy(isActive = true)
        val updatedList = current.map { it.copy(isActive = false) }.toMutableList()
        if (index >= 0) {
            updatedList[index] = updatedSource
        } else {
            updatedList.add(updatedSource)
        }
        _sources.value = updatedList
        _activeSource.value = updatedSource
        prefs.edit().putString("active_source_id", updatedSource.id).apply()
        persistSources()
    }

    suspend fun getSeriesDetails(seriesItem: IptvItem): Result<SeriesDetails> = withContext(Dispatchers.IO) {
        val active = _activeSource.value
        if (active != null && active.type == SourceType.XTREAM_CODES) {
            val rawSeriesId = seriesItem.id.removePrefix("xtream_series_")
            val details = xtreamClient.getSeriesInfo(
                serverUrl = active.serverUrl,
                username = active.username,
                password = active.password,
                seriesId = rawSeriesId
            )
            if (details != null && details.seasons.isNotEmpty()) {
                return@withContext Result.success(details)
            }
        }

        val targetTitle = seriesItem.seriesTitle ?: seriesItem.name
        val relatedEpisodes = allSeriesList.filter {
            it.seriesTitle?.equals(targetTitle, ignoreCase = true) == true ||
            it.name.contains(targetTitle, ignoreCase = true)
        }

        val seasonMap = mutableMapOf<Int, MutableList<SeriesEpisode>>()
        val itemsToProcess = if (relatedEpisodes.isNotEmpty()) relatedEpisodes else listOf(seriesItem)

        for (item in itemsToProcess) {
            val seasonNum = item.seasonNumber ?: 1
            val epNum = item.episodeNumber ?: (seasonMap[seasonNum]?.size ?: 0) + 1
            val epList = seasonMap.getOrPut(seasonNum) { mutableListOf() }
            val epTitle = item.name.substringAfter("-").substringAfter(":").trim().ifEmpty { item.name }
            epList.add(
                SeriesEpisode(
                    id = item.id,
                    seriesId = seriesItem.id,
                    seasonNumber = seasonNum,
                    episodeNumber = epNum,
                    title = epTitle,
                    streamUrl = item.streamUrl,
                    containerExtension = "mp4",
                    coverUrl = item.logoUrl,
                    plot = if (isAr) "الحلقة $epNum: $epTitle" else "Episode $epNum: $epTitle",
                    rating = item.rating
                )
            )
        }

        val seasons = seasonMap.entries.sortedBy { it.key }.map { (sNum, eps) ->
            SeriesSeason(
                seasonNumber = sNum,
                name = if (isAr) "الموسم $sNum" else "Season $sNum",
                episodes = eps.sortedBy { it.episodeNumber }
            )
        }

        Result.success(
            SeriesDetails(
                seriesId = seriesItem.id,
                title = targetTitle,
                coverUrl = seriesItem.logoUrl,
                backdropUrl = seriesItem.logoUrl,
                plot = if (isAr) "تصفح المواسم والحلقات لمسلسل $targetTitle" else "Stream seasons and episodes for $targetTitle.",
                rating = seriesItem.rating,
                seasons = seasons
            )
        )
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
