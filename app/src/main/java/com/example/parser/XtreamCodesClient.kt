package com.example.parser

import com.example.model.IptvItem
import com.example.model.IptvType
import com.example.model.SeriesDetails
import com.example.model.SeriesEpisode
import com.example.model.SeriesSeason
import com.example.model.StreamFormat
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

data class XtreamAuthResult(
    val isSuccess: Boolean,
    val username: String = "",
    val status: String = "",
    val expirationDate: String? = null,
    val serverMessage: String? = null,
    val items: List<IptvItem> = emptyList()
)

class XtreamCodesClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .build()
) {
    companion object {
        const val USER_AGENT = "IPTVSmartersPlayer"
    }

    private fun encodeParam(value: String): String {
        return try {
            URLEncoder.encode(value, "UTF-8")
        } catch (_: Exception) {
            value
        }
    }

    private fun cleanJsonText(raw: String): String {
        val trimmed = raw.trim().removePrefix("\uFEFF")
        val firstBrace = trimmed.indexOfFirst { it == '{' || it == '[' }
        return if (firstBrace > 0) trimmed.substring(firstBrace) else trimmed
    }

    suspend fun authenticateAndLoad(
        serverUrl: String,
        username: String,
        password: String
    ): XtreamAuthResult = withContext(Dispatchers.IO) {
        val cleanUser = username.trim()
        val cleanPass = password.trim()

        // If user accidentally pasted a full get.php or player_api.php link into Server URL box, extract base URL
        var rawBase = serverUrl.trim()
            .substringBefore("/player_api.php")
            .substringBefore("/get.php")
            .trimEnd('/')

        if (!rawBase.startsWith("http://", ignoreCase = true) && !rawBase.startsWith("https://", ignoreCase = true)) {
            rawBase = "http://$rawBase"
        }

        // Test exact URL first, then without :80, then with :8080
        val candidates = buildList {
            add(rawBase)
            if (rawBase.endsWith(":80")) {
                add(rawBase.removeSuffix(":80"))
                add(rawBase.removeSuffix(":80") + ":8080")
            } else if (!rawBase.matches(Regex(""".*:\d+$""")) && rawBase.startsWith("http://", ignoreCase = true)) {
                add("$rawBase:80")
                add("$rawBase:8080")
            }
        }.distinct()

        var lastError: String? = null

        for (base in candidates) {
            val result = attemptXtreamLogin(base, cleanUser, cleanPass)
            if (result.isSuccess && result.items.isNotEmpty()) {
                return@withContext result
            }
            lastError = result.serverMessage
        }

        XtreamAuthResult(
            isSuccess = false,
            serverMessage = lastError ?: "Authentication failed on Xtream Codes server."
        )
    }

    private suspend fun attemptXtreamLogin(
        base: String,
        cleanUser: String,
        cleanPass: String
    ): XtreamAuthResult = coroutineScope {
        val uEnc = encodeParam(cleanUser)
        val pEnc = encodeParam(cleanPass)
        val authUrl = "$base/player_api.php?username=$uEnc&password=$pEnc"

        try {
            val req = Request.Builder()
                .url(authUrl)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "*/*")
                .build()

            val response = client.newCall(req).execute()
            if (!response.isSuccessful) {
                response.close()
                return@coroutineScope tryM3uFallback(base, cleanUser, cleanPass)
            }

            val body = cleanJsonText(response.body?.string().orEmpty())
            if (body.isEmpty() || !body.startsWith("{")) {
                return@coroutineScope tryM3uFallback(base, cleanUser, cleanPass)
            }

            val json = JSONObject(body)
            val userInfo = json.optJSONObject("user_info") ?: json
            val authInt = userInfo.optInt("auth", -1)
            val authStr = userInfo.optString("auth", "")
            val authBool = userInfo.optBoolean("auth", false)
            val status = userInfo.optString("status", "").ifBlank { json.optString("status", "Active") }

            val isAuthValid = (authInt == 1) ||
                    (authStr == "1") ||
                    authBool ||
                    status.equals("Active", ignoreCase = true) ||
                    userInfo.has("username")

            if (!isAuthValid) {
                val fallback = tryM3uFallback(base, cleanUser, cleanPass)
                if (fallback.isSuccess) return@coroutineScope fallback

                return@coroutineScope XtreamAuthResult(
                    isSuccess = false,
                    serverMessage = "Invalid credentials or inactive account ($status)"
                )
            }

            val expTimestamp = userInfo.optLong("exp_date", 0L)
            val expDate = if (expTimestamp > 0) {
                val date = Date(expTimestamp * 1000)
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
            } else "Unlimited"

            // Fetch Categories & Streams in Parallel so 60,000+ item playlists load 3x faster without timing out
            val liveDeferred = async(Dispatchers.IO) {
                val cats = loadCategories(base, uEnc, pEnc, "get_live_categories")
                loadLiveStreams(base, cleanUser, cleanPass, uEnc, pEnc, cats)
            }
            val vodDeferred = async(Dispatchers.IO) {
                val cats = loadCategories(base, uEnc, pEnc, "get_vod_categories")
                loadVodStreams(base, cleanUser, cleanPass, uEnc, pEnc, cats)
            }
            val seriesDeferred = async(Dispatchers.IO) {
                val cats = loadCategories(base, uEnc, pEnc, "get_series_categories")
                loadSeries(base, cleanUser, cleanPass, uEnc, pEnc, cats)
            }

            val allItems = ArrayList<IptvItem>(65000)
            allItems.addAll(liveDeferred.await())
            allItems.addAll(vodDeferred.await())
            allItems.addAll(seriesDeferred.await())

            if (allItems.isEmpty()) {
                val fallback = tryM3uFallback(base, cleanUser, cleanPass)
                if (fallback.isSuccess && fallback.items.isNotEmpty()) {
                    return@coroutineScope fallback.copy(
                        username = cleanUser,
                        status = status,
                        expirationDate = expDate
                    )
                }
            }

            XtreamAuthResult(
                isSuccess = true,
                username = cleanUser,
                status = status,
                expirationDate = expDate,
                serverMessage = "Connected successfully. Loaded ${allItems.size} items.",
                items = allItems
            )
        } catch (e: Exception) {
            val fallback = tryM3uFallback(base, cleanUser, cleanPass)
            if (fallback.isSuccess && fallback.items.isNotEmpty()) {
                return@coroutineScope fallback
            }

            XtreamAuthResult(
                isSuccess = false,
                serverMessage = "Connection failed: ${e.localizedMessage ?: "Server unreachable"}"
            )
        }
    }

    private fun loadCategories(
        base: String,
        uEnc: String,
        pEnc: String,
        action: String
    ): Map<String, String> {
        val map = HashMap<String, String>()
        try {
            val url = "$base/player_api.php?username=$uEnc&password=$pEnc&action=$action"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "*/*")
                .build()
            val resp = client.newCall(req).execute()
            val text = cleanJsonText(resp.body?.string().orEmpty())
            if (text.startsWith("[")) {
                val arr = JSONArray(text)
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val id = obj.optString("category_id")
                    val name = obj.optString("category_name")
                    if (id.isNotEmpty() && name.isNotEmpty()) {
                        map[id] = name
                    }
                }
            }
        } catch (_: Exception) {}
        return map
    }

    private fun loadLiveStreams(
        base: String,
        rawUser: String,
        rawPass: String,
        uEnc: String,
        pEnc: String,
        categories: Map<String, String>
    ): List<IptvItem> {
        val list = ArrayList<IptvItem>()
        try {
            val url = "$base/player_api.php?username=$uEnc&password=$pEnc&action=get_live_streams"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "*/*")
                .build()
            val resp = client.newCall(req).execute()
            val text = cleanJsonText(resp.body?.string().orEmpty())
            if (text.startsWith("[")) {
                val arr = JSONArray(text)
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val streamId = obj.optString("stream_id")
                    val name = obj.optString("name")
                    val icon = obj.optString("stream_icon")
                    val catId = obj.optString("category_id")
                    val epgId = obj.optString("epg_channel_id")
                    val group = categories[catId] ?: "Live TV"

                    if (streamId.isNotEmpty() && name.isNotEmpty()) {
                        val streamUrl = "$base/live/$rawUser/$rawPass/$streamId.ts"
                        list.add(
                            IptvItem(
                                id = "xtream_live_$streamId",
                                name = name,
                                streamUrl = streamUrl,
                                logoUrl = icon.takeIf { it.isNotBlank() },
                                group = group,
                                type = IptvType.LIVE_TV,
                                streamFormat = StreamFormat.MPEG_TS,
                                epgChannelId = epgId.takeIf { it.isNotBlank() }
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}
        return list
    }

    private fun loadVodStreams(
        base: String,
        rawUser: String,
        rawPass: String,
        uEnc: String,
        pEnc: String,
        categories: Map<String, String>
    ): List<IptvItem> {
        val list = ArrayList<IptvItem>()
        try {
            val url = "$base/player_api.php?username=$uEnc&password=$pEnc&action=get_vod_streams"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "*/*")
                .build()
            val resp = client.newCall(req).execute()
            val text = cleanJsonText(resp.body?.string().orEmpty())
            if (text.startsWith("[")) {
                val arr = JSONArray(text)
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val streamId = obj.optString("stream_id")
                    val name = obj.optString("name")
                    val icon = obj.optString("stream_icon")
                    val catId = obj.optString("category_id")
                    val ext = obj.optString("container_extension", "mp4").ifBlank { "mp4" }
                    val rawRating = obj.optString("rating", "").ifBlank { obj.optString("rating_5based", "") }
                    val cleanRating = rawRating.toDoubleOrNull()?.let {
                        if (it > 0.0) String.format(Locale.US, "%.1f", it) else null
                    }
                    val group = categories[catId] ?: "Movies"

                    if (streamId.isNotEmpty() && name.isNotEmpty()) {
                        val streamUrl = "$base/movie/$rawUser/$rawPass/$streamId.$ext"
                        list.add(
                            IptvItem(
                                id = "xtream_vod_$streamId",
                                name = name,
                                streamUrl = streamUrl,
                                logoUrl = icon.takeIf { it.isNotBlank() },
                                group = group,
                                type = IptvType.MOVIE,
                                streamFormat = if (ext.equals("m3u8", ignoreCase = true)) StreamFormat.HLS else StreamFormat.MP4,
                                rating = cleanRating
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}
        return list
    }

    private fun loadSeries(
        base: String,
        rawUser: String,
        rawPass: String,
        uEnc: String,
        pEnc: String,
        categories: Map<String, String>
    ): List<IptvItem> {
        val list = ArrayList<IptvItem>()
        try {
            val url = "$base/player_api.php?username=$uEnc&password=$pEnc&action=get_series"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "*/*")
                .build()
            val resp = client.newCall(req).execute()
            val text = cleanJsonText(resp.body?.string().orEmpty())
            if (text.startsWith("[")) {
                val arr = JSONArray(text)
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val seriesId = obj.optString("series_id")
                    val name = obj.optString("name")
                    val cover = obj.optString("cover")
                    val catId = obj.optString("category_id")
                    val rawRating = obj.optString("rating", "").ifBlank { obj.optString("rating_5based", "") }
                    val cleanRating = rawRating.toDoubleOrNull()?.let {
                        if (it > 0.0) String.format(Locale.US, "%.1f", it) else null
                    }
                    val group = categories[catId] ?: "Series"

                    if (seriesId.isNotEmpty() && name.isNotEmpty()) {
                        val streamUrl = "$base/series/$rawUser/$rawPass/$seriesId.mp4"
                        list.add(
                            IptvItem(
                                id = "xtream_series_$seriesId",
                                name = name,
                                seriesTitle = name,
                                streamUrl = streamUrl,
                                logoUrl = cover.takeIf { it.isNotBlank() },
                                group = group,
                                type = IptvType.SERIES,
                                streamFormat = StreamFormat.MP4,
                                rating = cleanRating
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}
        return list
    }

    private fun tryM3uFallback(base: String, user: String, pass: String): XtreamAuthResult {
        val uEnc = encodeParam(user)
        val pEnc = encodeParam(pass)
        val urls = listOf(
            "$base/get.php?username=$uEnc&password=$pEnc&type=m3u_plus&output=ts",
            "$base/get.php?username=$uEnc&password=$pEnc&type=m3u_plus"
        )
        for (m3uUrl in urls) {
            try {
                val req = Request.Builder()
                    .url(m3uUrl)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "*/*")
                    .build()
                val resp = client.newCall(req).execute()
                if (resp.isSuccessful) {
                    val body = resp.body
                    if (body != null) {
                        val reader = body.byteStream().bufferedReader(Charsets.UTF_8)
                        val parsed = M3uParser.parseStream(reader)
                        if (parsed.isNotEmpty()) {
                            return XtreamAuthResult(
                                isSuccess = true,
                                username = user,
                                status = "Active",
                                serverMessage = "Connected successfully (${parsed.size} items)",
                                items = parsed
                            )
                        }
                    }
                } else {
                    resp.close()
                }
            } catch (_: Exception) {}
        }

        return XtreamAuthResult(
            isSuccess = false,
            serverMessage = "Authentication failed on Xtream Codes server."
        )
    }

    suspend fun getSeriesInfo(
        serverUrl: String,
        username: String,
        password: String,
        seriesId: String
    ): SeriesDetails? = withContext(Dispatchers.IO) {
        val base = serverUrl.trim().removeSuffix("/")
        val uEnc = encodeParam(username.trim())
        val pEnc = encodeParam(password.trim())
        val url = "$base/player_api.php?username=$uEnc&password=$pEnc&action=get_series_info&series_id=$seriesId"
        try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "*/*")
                .build()
            val resp = client.newCall(req).execute()
            val text = cleanJsonText(resp.body?.string().orEmpty())
            if (!text.startsWith("{")) return@withContext null
            val root = JSONObject(text)
            val info = root.optJSONObject("info")
            val title = info?.optString("name")?.takeIf { it.isNotBlank() } ?: "Series $seriesId"
            val cover = info?.optString("cover")
            val backdrop = info?.optJSONArray("backdrop_path")?.optString(0) ?: cover
            val plot = info?.optString("plot")
            val genre = info?.optString("genre")
            val releaseDate = info?.optString("releaseDate")
            val rawRating = info?.optString("rating", "")
            val cleanRating = rawRating?.toDoubleOrNull()?.let {
                if (it > 0.0) String.format(Locale.US, "%.1f", it) else null
            } ?: rawRating

            val seasonList = mutableListOf<SeriesSeason>()
            val episodesObj = root.optJSONObject("episodes")
            val episodesArr = root.optJSONArray("episodes")

            if (episodesObj != null) {
                val seasonKeys = episodesObj.keys().asSequence().toList().sortedBy { it.toIntOrNull() ?: 0 }
                for (key in seasonKeys) {
                    val seasonNum = key.toIntOrNull() ?: 1
                    val epArray = episodesObj.optJSONArray(key) ?: continue
                    val epList = parseEpisodeArray(epArray, seasonNum, seriesId, base, username.trim(), password.trim(), cover)
                    if (epList.isNotEmpty()) {
                        seasonList.add(
                            SeriesSeason(
                                seasonNumber = seasonNum,
                                name = "Season $seasonNum",
                                episodes = epList.sortedBy { it.episodeNumber }
                            )
                        )
                    }
                }
            } else if (episodesArr != null) {
                for (sIdx in 0 until episodesArr.length()) {
                    val epArray = episodesArr.optJSONArray(sIdx) ?: continue
                    val seasonNum = sIdx + 1
                    val epList = parseEpisodeArray(epArray, seasonNum, seriesId, base, username.trim(), password.trim(), cover)
                    if (epList.isNotEmpty()) {
                        seasonList.add(
                            SeriesSeason(
                                seasonNumber = seasonNum,
                                name = "Season $seasonNum",
                                episodes = epList.sortedBy { it.episodeNumber }
                            )
                        )
                    }
                }
            }

            SeriesDetails(
                seriesId = seriesId,
                title = title,
                coverUrl = cover?.takeIf { it.isNotBlank() },
                backdropUrl = backdrop?.takeIf { it.isNotBlank() },
                plot = plot?.takeIf { it.isNotBlank() },
                genre = genre?.takeIf { it.isNotBlank() },
                releaseDate = releaseDate?.takeIf { it.isNotBlank() },
                rating = cleanRating?.takeIf { it.isNotBlank() },
                seasons = seasonList
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun parseEpisodeArray(
        epArray: JSONArray,
        seasonNum: Int,
        seriesId: String,
        base: String,
        rawUser: String,
        rawPass: String,
        defaultCover: String?
    ): List<SeriesEpisode> {
        val epList = mutableListOf<SeriesEpisode>()
        for (i in 0 until epArray.length()) {
            val epObj = epArray.optJSONObject(i) ?: continue
            val epId = epObj.optString("id")
            if (epId.isEmpty()) continue
            val epNum = epObj.optInt("episode_num", i + 1)
            val epTitle = epObj.optString("title", "Episode $epNum")
            val container = epObj.optString("container_extension", "mp4").trim().removePrefix(".").ifEmpty { "mp4" }
            val epInfo = epObj.optJSONObject("info")
            val epPlot = epInfo?.optString("plot")
            val epDuration = epInfo?.optString("duration")
            val epCover = epInfo?.optString("movie_image") ?: defaultCover
            val epRating = epInfo?.optString("rating")

            val streamUrl = "$base/series/$rawUser/$rawPass/$epId.$container"
            epList.add(
                SeriesEpisode(
                    id = epId,
                    seriesId = seriesId,
                    seasonNumber = seasonNum,
                    episodeNumber = epNum,
                    title = epTitle,
                    streamUrl = streamUrl,
                    containerExtension = container,
                    coverUrl = epCover?.takeIf { it.isNotBlank() },
                    plot = epPlot?.takeIf { it.isNotBlank() },
                    duration = epDuration?.takeIf { it.isNotBlank() },
                    rating = epRating?.takeIf { it.isNotBlank() }
                )
            )
        }
        return epList
    }
}
