package com.example.ui.player

import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.GZIPInputStream

data class EpgProgram(
    val channelId: String,
    val title: String,
    val description: String,
    val startTimeMs: Long,
    val endTimeMs: Long
) {
    fun isLiveAt(nowMs: Long = System.currentTimeMillis()): Boolean =
        nowMs in startTimeMs until endTimeMs

    fun progressFraction(nowMs: Long = System.currentTimeMillis()): Float {
        val total = (endTimeMs - startTimeMs).coerceAtLeast(1L)
        return ((nowMs - startTimeMs).toFloat() / total.toFloat()).coerceIn(0f, 1f)
    }

    fun formattedTimeRange(): String {
        val fmt = SimpleDateFormat("HH:mm", Locale.US)
        return "${fmt.format(startTimeMs)} - ${fmt.format(endTimeMs)}"
    }
}

data class ChannelEpgNowNext(
    val now: EpgProgram?,
    val next: EpgProgram?
)

object EpgManager {
    private val programsByChannel = ConcurrentHashMap<String, List<EpgProgram>>()
    private var lastLoadedUrl: String? = null
    private var lastLoadedTimeMs: Long = 0L

    // 1. بناء رابط XMLTV تلقائياً لحسابات Xtream Codes
    fun buildXtreamEpgUrl(serverUrl: String, username: String, password: String): String {
        val cleanBase = serverUrl.trimEnd('/')
        return "$cleanBase/xmltv.php?username=$username&password=$password"
    }

    // 2. استخراج رابط EPG تلقائياً من ترويسة ملف M3U (#EXTM3U url-tvg="...")
    fun extractM3uEpgUrl(firstLineHeader: String): String? {
        val regex = Regex("""(?:url-tvg|x-tvg-url)\s*=\s*"([^"]+)"""", RegexOption.IGNORE_CASE)
        return regex.find(firstLineHeader)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
    }

    // 3. تحميل وتحليل ملف XMLTV في الخلفية (يدعم .xml و .xml.gz)
    suspend fun loadEpgFromUrl(epgUrl: String, forceRefresh: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        if (epgUrl.isBlank()) return@withContext false

        val threeHoursMs = 3 * 60 * 60 * 1000L
        if (!forceRefresh && epgUrl == lastLoadedUrl &&
            (System.currentTimeMillis() - lastLoadedTimeMs) < threeHoursMs &&
            programsByChannel.isNotEmpty()
        ) {
            return@withContext true
        }

        var connection: HttpURLConnection? = null
        try {
            connection = (URL(epgUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("User-Agent", "HypnoStream/1.0")
                setRequestProperty("Accept-Encoding", "gzip")
            }

            if (connection.responseCode !in 200..299) return@withContext false

            val rawStream = connection.inputStream
            val isGzip = epgUrl.endsWith(".gz", ignoreCase = true) ||
                connection.contentEncoding?.contains("gzip", ignoreCase = true) == true

            val inputStream: InputStream = if (isGzip) GZIPInputStream(rawStream) else rawStream
            inputStream.use { stream ->
                val parsedMap = parseXmltvStream(stream)
                if (parsedMap.isNotEmpty()) {
                    programsByChannel.clear()
                    programsByChannel.putAll(parsedMap)
                    lastLoadedUrl = epgUrl
                    lastLoadedTimeMs = System.currentTimeMillis()
                    return@withContext true
                }
            }
            false
        } catch (_: Exception) {
            false
        } finally {
            connection?.disconnect()
        }
    }

    // 4. جلب البرنامج الحالي والقادم لأي قناة
    fun getNowAndNext(epgChannelId: String?): ChannelEpgNowNext {
        if (epgChannelId.isNullOrBlank()) return ChannelEpgNowNext(null, null)

        val key = epgChannelId.trim().lowercase(Locale.US)
        val list = programsByChannel[key] ?: return ChannelEpgNowNext(null, null)
        val nowMs = System.currentTimeMillis()

        val currentIndex = list.indexOfFirst { nowMs in it.startTimeMs until it.endTimeMs }
        return if (currentIndex != -1) {
            val nowProg = list[currentIndex]
            val nextProg = list.getOrNull(currentIndex + 1)
            ChannelEpgNowNext(nowProg, nextProg)
        } else {
            val upcoming = list.firstOrNull { it.startTimeMs > nowMs }
            ChannelEpgNowNext(null, upcoming)
        }
    }

    // 5. جلب جدول البرامج الكامل لليوم لقناة معينة
    fun getChannelSchedule(epgChannelId: String?): List<EpgProgram> {
        if (epgChannelId.isNullOrBlank()) return emptyList()
        return programsByChannel[epgChannelId.trim().lowercase(Locale.US)].orEmpty()
    }

    private fun parseXmltvStream(inputStream: InputStream): Map<String, List<EpgProgram>> {
        val tempMap = HashMap<String, MutableList<EpgProgram>>()
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            setInput(inputStream, "UTF-8")
        }

        val nowMs = System.currentTimeMillis()
        val minKeepMs = nowMs - (6 * 60 * 60 * 1000L)
        val maxKeepMs = nowMs + (24 * 60 * 60 * 1000L)

        var eventType = parser.eventType
        var currentChannelId = ""
        var currentStartMs = 0L
        var currentEndMs = 0L
        var currentTitle = ""
        var currentDesc = ""
        var insideProgramme = false

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "programme" -> {
                            insideProgramme = true
                            currentChannelId = parser.getAttributeValue(null, "channel")
                                ?.trim()?.lowercase(Locale.US).orEmpty()
                            currentStartMs = parseXmltvDate(parser.getAttributeValue(null, "start"))
                            currentEndMs = parseXmltvDate(parser.getAttributeValue(null, "stop"))
                            currentTitle = ""
                            currentDesc = ""
                        }
                        "title" -> if (insideProgramme) {
                            currentTitle = parser.nextText().trim()
                        }
                        "desc" -> if (insideProgramme) {
                            currentDesc = parser.nextText().trim()
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "programme" && insideProgramme) {
                        if (currentChannelId.isNotEmpty() &&
                            currentTitle.isNotEmpty() &&
                            currentEndMs > minKeepMs &&
                            currentStartMs < maxKeepMs
                        ) {
                            val list = tempMap.getOrPut(currentChannelId) { ArrayList() }
                            list.add(
                                EpgProgram(
                                    channelId = currentChannelId,
                                    title = currentTitle,
                                    description = currentDesc,
                                    startTimeMs = currentStartMs,
                                    endTimeMs = currentEndMs
                                )
                            )
                        }
                        insideProgramme = false
                    }
                }
            }
            eventType = parser.next()
        }

        tempMap.values.forEach { list -> list.sortBy { it.startTimeMs } }
        return tempMap
    }

    private fun parseXmltvDate(raw: String?): Long {
        if (raw.isNullOrBlank()) return 0L
        return try {
            val cleaned = raw.trim()
            val pattern = if (cleaned.contains(" ")) "yyyyMMddHHmmss Z" else "yyyyMMddHHmmss"
            val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            sdf.parse(cleaned)?.time ?: 0L
        } catch (_: Exception) {
            0L
        }
    }
}
