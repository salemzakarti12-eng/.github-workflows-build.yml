package com.example.parser

import com.example.model.IptvItem
import com.example.model.IptvType
import com.example.model.SeriesDetails
import com.example.model.SeriesEpisode
import com.example.model.SeriesSeason
import com.example.model.StreamFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

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
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) {
    companion object {
        const val USER_AGENT = "IPTVSmartersPlayer"
    }

    suspend fun authenticateAndLoad(
        serverUrl: String,
        username: String,
        password: String
    ): XtreamAuthResult = withContext(Dispatchers.IO) {
        val cleanUser = username.trim()
        val cleanPass = password.trim()
        var rawBase = serverUrl.trim().removeSuffix("/")
        if (!rawBase.startsWith("http://", ignoreCase = true) && !rawBase.startsWith("https://", ignoreCase = true)) {
            rawBase = "http://$rawBase"
        }

        // Try both with :80 and without :80 if applicable
        val candidates = buildList {
            add(rawBase)
            if (rawBase.endsWith(":80")) {
                add(rawBase.removeSuffix(":80"))
            } else if (!rawBase.matches(Regex(""".*:\d+$""")) && rawBase.startsWith("http://", ignoreCase = true)) {
                add("$rawBase:80")
            }
        }.distinct()

        var lastError: String? = null

        for (base in candidates) {
            val result = attemptXtreamLogin(base, cleanUser, cleanPass)
            if (result.isSuccess) {
                return@withContext result
            }
            lastError = result.serverMessage
        }

        XtreamAuthResult(
            isSuccess = false,
            serverMessage = lastError ?: "Authentication failed on Xtream Codes server."
        )
    }

    private fun attemptXtreamLogin(
        base: String,
        cleanUser: String,
        cleanPass: String
    ): XtreamAuthResult {
        val authUrl = "$base/player_api.php?username=$cleanUser&password=$cleanPass"

        try {
            val req = Request.Builder()
                .url(authUrl)
                .header("User-Agent", USER_AGENT)
                .build()

            val response = client.newCall(req).execute()
            if (!response.isSuccessful) {
                return tryM3uFallback(base, cleanUser, cleanPass)
            }

            val body = response.body?.string().orEmpty()
            if (body.isEmpty() || !body.startsWith("{")) {
                return tryM3uFallback(base, cleanUser, cleanPass)
            }

            val json = JSONObject(body)
            val userInfo = json.optJSONObject("user_info") ?: json
            val authInt = userInfo.optInt("auth", -1)
            val authStr = userInfo.optString("auth", "")
            val authBool = userInfo.optBoolean("auth", false)
            val status = userInfo.optString("status", "").ifBlank { json.optString("status", "Unknown") }

            val isAuthValid = (authInt == 1) ||
                    (authStr == "1") ||
                    authBool ||
                    status.equals("Active", ignoreCase = true) ||
                    json.optInt("auth", -1) == 1 ||
                    json.optString("auth", "") == "1"

            if (!isAuthValid) {
                // Try fallback to get.php before declaring invalid credentials
                val fallback = tryM3uFallback(base, cleanUser, cleanPass)
                if (fallback.isSuccess) return fallback

                return XtreamAuthResult(
                    isSuccess = false,
                    serverMessage = "Invalid credentials or account inactive ($status)"
                )
            }

            val expTimestamp = userInfo.optLong("exp_date", 0L)
            val expDate = if (expTimestamp > 0) {
                val date = Date(expTimestamp * 1000)
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
            } else "Unlimited"

            // Now load live categories & streams
            val liveCategories = loadCategories(base, cleanUser, cleanPass, "get_live_categories")
            val vodCategories = loadCategories(base, cleanUser, cleanPass, "get_vod_categories")
            val seriesCategories = loadCategories(base, cleanUser, cleanPass, "get_series_categories")

            val allItems = mutableListOf<IptvItem>()
            allItems.addAll(loadLiveStreams(base, cleanUser, cleanPass, liveCategories))
            allItems.addAll(loadVodStreams(base, cleanUser, cleanPass, vodCategories))
            allItems.addAll(loadSeries(base, cleanUser, cleanPass, seriesCategories))

            // If empty, attempt fallback to m3u_plus
            if (allItems.isEmpty()) {
                val fallback = tryM3uFallback(base, cleanUser, cleanPass)
                if (fallback.isSuccess && fallback.items.isNotEmpty()) {
                    return fallback.copy(
                        username = cleanUser,
                        status = status,
                        expirationDate = expDate
                    )
                }
            }

            return XtreamAuthResult(
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
                return fallback
            }

            return XtreamAuthResult(
                isSuccess = false,
                serverMessage = "Connection failed: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }

    private fun loadCategories(
        base: String,
        user: String,
        pass: String,
        action: String
    ): Map<String, String> {
        val map = mutableMapOf<String, String>()
        try {
            val url = "$base/player_api.php?username=$user&password=$pass&action=$action"
            val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
            val resp = client.newCall(req).execute()
            val text = resp.body?.string().orEmpty()
            if (text.startsWith("[")) {
                val arr = JSONArray(text)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
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
        user: String,
        pass: String,
        categories: Map<String, String>
    ): List<IptvItem> {
        val list = mutableListOf<IptvItem>()
        try {
            val url = "$base/player_api.php?username=$user&password=$pass&action=get_live_streams"
            val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
            val resp = client.newCall(req).execute()
            val text = resp.body?.string().orEmpty()
            if (text.startsWith("[")) {
                val arr = JSONArray(text)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val streamId = obj.optString("stream_id")
                    val name = obj.optString("name")
                    val icon = obj.optString("stream_icon")
                    val catId = obj.optString("category_id")
                    val epgId = obj.optString("epg_channel_id")
                    val group = categories[catId] ?: "Live TV"

                    if (streamId.isNotEmpty() && name.isNotEmpty()) {
                        val streamUrl = "$base/live/$user/$pass/$streamId.ts"
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
        user: String,
        pass: String,
        categories: Map<String, String>
    ): List<IptvItem> {
        val list = mutableListOf<IptvItem>()
        try {
            val url = "$base/player_api.php?username=$user&password=$pass&action=get_vod_streams"
            val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
            val resp = client.newCall(req).execute()
            val text = resp.body?.string().orEmpty()
            if (text.startsWith("[")) {
                val arr = JSONArray(text)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val streamId = obj.optString("stream_id")
                    val name = obj.optString("name")
                    val icon = obj.optString("stream_icon")
                    val catId = obj.optString("category_id")
                    val ext = obj.optString("container_extension", "mp4")
                    val rating = obj.optString("rating_5based", "")
                    val group = categories[catId] ?: "Movies"

                    if (streamId.isNotEmpty() && name.isNotEmpty()) {
                        val streamUrl = "$base/movie/$user/$pass/$streamId.$ext"
                        list.add(
                            IptvItem(
                                id = "xtream_vod_$streamId",
                                name = name,
                                streamUrl = streamUrl,
                                logoUrl = icon.takeIf { it.isNotBlank() },
                                group = group,
                                type = IptvType.MOVIE,
                                streamFormat = if (ext.equals("m3u8", ignoreCase = true)) StreamFormat.HLS else StreamFormat.MP4,
                                rating = rating.takeIf { it.isNotBlank() }
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
        user: String,
        pass: String,
        categories: Map<String, String>
    ): List<IptvItem> {
        val list = mutableListOf<IptvItem>()
        try {
            val url = "$base/player_api.php?username=$user&password=$pass&action=get_series"
            val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
            val resp = client.newCall(req).execute()
            val text = resp.body?.string().orEmpty()
            if (text.startsWith("[")) {
                val arr = JSONArray(text)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val seriesId = obj.optString("series_id")
                    val name = obj.optString("name")
                    val cover = obj.optString("cover")
                    val catId = obj.optString("category_id")
                    val rating = obj.optString("rating", "")
                    val group = categories[catId] ?: "Series"

                    if (seriesId.isNotEmpty() && name.isNotEmpty()) {
                        // For series overview item
                        val streamUrl = "$base/series/$user/$pass/$seriesId.mp4"
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
                                rating = rating.takeIf { it.isNotBlank() }
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}
        return list
    }

    private fun tryM3uFallback(base: String, user: String, pass: String): XtreamAuthResult {
        val m3uUrl = "$base/get.php?username=$user&password=$pass&type=m3u_plus&output=ts"
        try {
            val req = Request.Builder()
                .url(m3uUrl)
                .header("User-Agent", USER_AGENT)
                .build()
            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                val body = resp.body
                if (body != null) {
                    val reader = body.charStream().buffered()
                    val parsed = M3uParser.parseStream(reader)
                    if (parsed.isNotEmpty()) {
                        return XtreamAuthResult(
                            isSuccess = true,
                            username = user,
                            status = "Active",
                            serverMessage = "Connected successfully via M3U Plus fallback (${parsed.size} items)",
                            items = parsed
                        )
                    }
                }
            }
        } catch (_: Exception) {}

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
        val url = "$base/player_api.php?username=$username&password=$password&action=get_series_info&series_id=$seriesId"
        try {
            val req = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
            val resp = client.newCall(req).execute()
            val text = resp.body?.string().orEmpty()
            if (!text.startsWith("{")) return@withContext null
            val root = JSONObject(text)
            val info = root.optJSONObject("info")
            val title = info?.optString("name") ?: "Series $seriesId"
            val cover = info?.optString("cover")
            val backdrop = info?.optJSONArray("backdrop_path")?.optString(0) ?: cover
            val plot = info?.optString("plot")
            val genre = info?.optString("genre")
            val releaseDate = info?.optString("releaseDate")
            val rating = info?.optString("rating")

            val episodesObj = root.optJSONObject("episodes") ?: JSONObject()
            val seasonList = mutableListOf<SeriesSeason>()

            val seasonKeys = episodesObj.keys().asSequence().toList().sortedBy { it.toIntOrNull() ?: 0 }
            for (key in seasonKeys) {
                val seasonNum = key.toIntOrNull() ?: 1
                val epArray = episodesObj.optJSONArray(key) ?: JSONArray()
                val epList = mutableListOf<SeriesEpisode>()
                for (i in 0 until epArray.length()) {
                    val epObj = epArray.getJSONObject(i)
                    val epId = epObj.optString("id")
                    val epNum = epObj.optInt("episode_num", i + 1)
                    val epTitle = epObj.optString("title", "Episode $epNum")
                    val container = epObj.optString("container_extension", "mp4").ifEmpty { "mp4" }
                    val epInfo = epObj.optJSONObject("info")
                    val epPlot = epInfo?.optString("plot")
                    val epDuration = epInfo?.optString("duration")
                    val epCover = epInfo?.optString("movie_image") ?: cover
                    val epRating = epInfo?.optString("rating")

                    val streamUrl = "$base/series/$username/$password/$epId.$container"
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

            SeriesDetails(
                seriesId = seriesId,
                title = title,
                coverUrl = cover?.takeIf { it.isNotBlank() },
                backdropUrl = backdrop?.takeIf { it.isNotBlank() },
                plot = plot?.takeIf { it.isNotBlank() },
                genre = genre?.takeIf { it.isNotBlank() },
                releaseDate = releaseDate?.takeIf { it.isNotBlank() },
                rating = rating?.takeIf { it.isNotBlank() },
                seasons = seasonList
            )
        } catch (_: Exception) {
            null
        }
    }
}
