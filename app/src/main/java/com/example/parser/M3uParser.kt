package com.example.parser

import com.example.model.IptvItem
import com.example.model.IptvType
import com.example.model.StreamFormat
import java.io.BufferedReader

object M3uParser {

    private val SERIES_EPISODE_REGEX = Regex("""(?i)S(\d+)\s*E(\d+)""")

    data class ParseProgress(
        val totalCount: Int,
        val liveCount: Int,
        val moviesCount: Int,
        val seriesCount: Int
    )

    /**
     * High-speed streaming parser for M3U / M3U8 playlists.
     * Uses BufferedReader to process streams line-by-line without loading
     * giant multi-megabyte text payloads into RAM.
     */
    fun parseStream(
        reader: BufferedReader,
        onProgress: ((ParseProgress) -> Unit)? = null
    ): List<IptvItem> {
        val items = ArrayList<IptvItem>(5000)
        var lineIndex = 0
        var currentExtInf: String? = null
        var liveCount = 0
        var movieCount = 0
        var seriesCount = 0

        reader.useLines { lines ->
            for (rawLine in lines) {
                lineIndex++
                val line = rawLine.trim()
                if (line.isEmpty() || line.startsWith("#EXTM3U")) {
                    continue
                }

                if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                    currentExtInf = line
                } else if (!line.startsWith("#") && (
                            line.startsWith("http://", ignoreCase = true) ||
                            line.startsWith("https://", ignoreCase = true) ||
                            line.startsWith("rtmp://", ignoreCase = true)
                        )
                ) {
                    val streamUrl = line
                    val extInf = currentExtInf ?: ""

                    // Fast attribute extraction without heavy regexes for high performance
                    val tvgName = extractAttribute(extInf, "tvg-name")
                    val tvgLogo = extractAttribute(extInf, "tvg-logo")
                    val tvgId = extractAttribute(extInf, "tvg-id")
                    val groupTitle = extractAttribute(extInf, "group-title") ?: "General"

                    // Channel display title after last comma in EXTINF line
                    val commaIndex = extInf.lastIndexOf(',')
                    val displayName = if (commaIndex != -1 && commaIndex < extInf.length - 1) {
                        extInf.substring(commaIndex + 1).trim()
                    } else {
                        tvgName ?: "Channel ${items.size + 1}"
                    }.ifEmpty { tvgName ?: "Channel ${items.size + 1}" }

                    // Classification according to requirements:
                    // 1. Movies (VOD): URLs ending in .mp4, .mkv, .avi or group-title containing 'VOD' or 'Movie'
                    // 2. Series: URLs containing /series/ or matching S01E01 patterns
                    // 3. Live TV: Everything else
                    val lowerUrl = streamUrl.lowercase()
                    val lowerGroup = groupTitle.lowercase()
                    val seriesMatch = SERIES_EPISODE_REGEX.find(displayName)

                    val isSeries = lowerUrl.contains("/series/") || seriesMatch != null || lowerGroup.contains("series")
                    val isMovie = !isSeries && (
                            lowerUrl.endsWith(".mp4") ||
                            lowerUrl.endsWith(".mkv") ||
                            lowerUrl.endsWith(".avi") ||
                            lowerGroup.contains("vod") ||
                            lowerGroup.contains("movie") ||
                            lowerUrl.contains("/movie/")
                    )

                    val type = when {
                        isSeries -> {
                            seriesCount++
                            IptvType.SERIES
                        }
                        isMovie -> {
                            movieCount++
                            IptvType.MOVIE
                        }
                        else -> {
                            liveCount++
                            IptvType.LIVE_TV
                        }
                    }

                    val format = when {
                        lowerUrl.contains(".m3u8") -> StreamFormat.HLS
                        lowerUrl.contains(".ts") -> StreamFormat.MPEG_TS
                        lowerUrl.endsWith(".mp4") -> StreamFormat.MP4
                        else -> StreamFormat.UNKNOWN
                    }

                    val seasonNum = seriesMatch?.groupValues?.get(1)?.toIntOrNull()
                    val episodeNum = seriesMatch?.groupValues?.get(2)?.toIntOrNull()
                    val seriesTitle = if (isSeries) {
                        if (seriesMatch != null) {
                            displayName.substring(0, seriesMatch.range.first).trim(' ', '-', ':')
                        } else {
                            displayName
                        }
                    } else null

                    val uniqueId = tvgId?.takeIf { it.isNotBlank() } ?: "str_${items.size + 1}"

                    items.add(
                        IptvItem(
                            id = uniqueId,
                            name = displayName,
                            streamUrl = streamUrl,
                            logoUrl = tvgLogo?.takeIf { it.isNotBlank() },
                            group = groupTitle.ifBlank { "General" },
                            type = type,
                            streamFormat = format,
                            epgChannelId = tvgId,
                            seriesTitle = seriesTitle,
                            seasonNumber = seasonNum,
                            episodeNumber = episodeNum
                        )
                    )

                    currentExtInf = null

                    // Progress reporting every 1,000 items parsed
                    if (items.size % 1000 == 0) {
                        onProgress?.invoke(
                            ParseProgress(
                                totalCount = items.size,
                                liveCount = liveCount,
                                moviesCount = movieCount,
                                seriesCount = seriesCount
                            )
                        )
                    }
                }
            }
        }

        // Final progress dispatch
        onProgress?.invoke(
            ParseProgress(
                totalCount = items.size,
                liveCount = liveCount,
                moviesCount = movieCount,
                seriesCount = seriesCount
            )
        )

        return items
    }

    fun parse(content: String): List<IptvItem> {
        return parseStream(content.reader().buffered())
    }

    private fun extractAttribute(text: String, attributeName: String): String? {
        val target = "$attributeName=\""
        val startIdx = text.indexOf(target, ignoreCase = true)
        if (startIdx == -1) return null
        val valueStart = startIdx + target.length
        val endIdx = text.indexOf('"', valueStart)
        if (endIdx == -1) return null
        return text.substring(valueStart, endIdx).trim()
    }
}
