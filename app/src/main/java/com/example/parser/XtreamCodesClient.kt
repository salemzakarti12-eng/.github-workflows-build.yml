package com.example.parser

import com.example.model.IptvItem
import com.example.model.IptvType
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
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {

    suspend fun authenticateAndLoad(
        serverUrl: String,
        username: String,
        password: String
    ): XtreamAuthResult = withContext(Dispatchers.IO) {
        val base = serverUrl.trim().removeSuffix("/")
        val authUrl = "$base/player_api.php?username=$username&password=$password"

        try {
            val req = Request.Builder()
                .url(authUrl)
                .header("User-Agent", "HypnoStream-IPTV/1.0")
                .build()

            val response = client.newCall(req).execute()
            if (!response.isSuccessful) {
                // Try fallback to get.php m3u_plus
                return@withContext tryM3uFallback(base, username, password)
            }

            val body = response.body?.string().orEmpty()
            if (body.isEmpty() || !body.startsWith("{")) {
                return@withContext tryM3uFallback(base, username, password)
            }

            val json = JSONObject(body)
            val userInfo = json.optJSONObject("user_info")
            val auth = userInfo?.optInt("auth", 0) ?: 0
            val status = userInfo?.optString("status", "Unknown") ?: "Unknown"

            if (userInfo == null || (auth != 1 && !status.equals("Active", ignoreCase = true))) {
                return@withContext XtreamAuthResult(
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
            val liveCategories = loadCategories(base, username, password, "get_live_categories")
            val vodCategories = loadCategories(base, username, password, "get_vod_categories")
            val seriesCategories = loadCategories(base, username, password, "get_series_categories")

            val allItems = mutableListOf<IptvItem>()
            allItems.addAll(loadLiveStreams(base, username, password, liveCategories))
            allItems.addAll(loadVodStreams(base, username, password, vodCategories))
            allItems.addAll(loadSeries(base, username, password, seriesCategories))

            // If empty, attempt fallback to m3u_plus
            if (allItems.isEmpty()) {
                val fallback = tryM3uFallback(base, username, password)
                if (fallback.isSuccess && fallback.items.isNotEmpty()) {
                    return@withContext fallback.copy(
                        username = username,
                        status = status,
                        expirationDate = expDate
                    )
                }
            }

            XtreamAuthResult(
                isSuccess = true,
                username = username,
                status = status,
                expirationDate = expDate,
                serverMessage = "Connected successfully. Loaded ${allItems.size} items.",
                items = allItems
            )
        } catch (e: Exception) {
            // Check if fallback succeeds
            try {
                val fallback = tryM3uFallback(base, username, password)
                if (fallback.isSuccess && fallback.items.isNotEmpty()) {
                    return@withContext fallback
                }
            } catch (_: Exception) {}

            XtreamAuthResult(
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
            val req = Request.Builder().url(url).header("User-Agent", "HypnoStream-IPTV/1.0").build()
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
            val req = Request.Builder().url(url).header("User-Agent", "HypnoStream-IPTV/1.0").build()
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
            val req = Request.Builder().url(url).header("User-Agent", "HypnoStream-IPTV/1.0").build()
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
            val req = Request.Builder().url(url).header("User-Agent", "HypnoStream-IPTV/1.0").build()
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
        val req = Request.Builder().url(m3uUrl).header("User-Agent", "HypnoStream-IPTV/1.0").build()
        val resp = client.newCall(req).execute()
        if (resp.isSuccessful) {
            val content = resp.body?.string().orEmpty()
            if (content.contains("#EXTM3U", ignoreCase = true)) {
                val parsed = M3uParser.parse(content)
                return XtreamAuthResult(
                    isSuccess = true,
                    username = user,
                    status = "Active",
                    serverMessage = "Loaded via M3U Plus fallback (${parsed.size} items)",
                    items = parsed
                )
            }
        }
        return XtreamAuthResult(
            isSuccess = false,
            serverMessage = "Authentication failed on Xtream Codes server."
        )
    }
}
