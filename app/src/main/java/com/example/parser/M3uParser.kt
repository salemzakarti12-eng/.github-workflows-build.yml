package com.example.parser

import com.example.model.IptvItem
import com.example.model.IptvType
import com.example.model.StreamFormat
import java.util.UUID

object M3uParser {

    private val TVG_NAME_REGEX = Regex("""tvg-name="([^"]*)"""", RegexOption.IGNORE_CASE)
    private val TVG_LOGO_REGEX = Regex("""tvg-logo="([^"]*)"""", RegexOption.IGNORE_CASE)
    private val TVG_ID_REGEX = Regex("""tvg-id="([^"]*)"""", RegexOption.IGNORE_CASE)
    private val GROUP_TITLE_REGEX = Regex("""group-title="([^"]*)"""", RegexOption.IGNORE_CASE)
    private val SERIES_EPISODE_REGEX = Regex("""(?i)S(\d+)\s*E(\d+)""")

    fun parse(content: String): List<IptvItem> {
        val items = mutableListOf<IptvItem>()
        val lines = content.lines()

        var currentExtInf: String? = null

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#EXTM3U")) {
                continue
            }

            if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                currentExtInf = line
            } else if (!line.startsWith("#") && (line.startsWith("http://", ignoreCase = true) || line.startsWith("https://", ignoreCase = true) || line.startsWith("rtmp://", ignoreCase = true))) {
                val streamUrl = line
                val extInf = currentExtInf ?: ""

                // Extract tvg attributes
                val tvgName = TVG_NAME_REGEX.find(extInf)?.groupValues?.get(1)
                val tvgLogo = TVG_LOGO_REGEX.find(extInf)?.groupValues?.get(1)
                val tvgId = TVG_ID_REGEX.find(extInf)?.groupValues?.get(1)
                val groupTitle = GROUP_TITLE_REGEX.find(extInf)?.groupValues?.get(1) ?: "General"

                // Channel display title after last comma in EXTINF line
                val displayName = if (extInf.contains(",")) {
                    extInf.substringAfterLast(",").trim()
                } else {
                    tvgName ?: "Stream ${items.size + 1}"
                }.ifEmpty { tvgName ?: "Stream ${items.size + 1}" }

                // Determine classification
                val lowerGroup = groupTitle.lowercase()
                val lowerUrl = streamUrl.lowercase()
                val lowerName = displayName.lowercase()

                val seriesMatch = SERIES_EPISODE_REGEX.find(displayName)
                val isSeries = seriesMatch != null || lowerGroup.contains("series") || lowerGroup.contains("shows") || lowerGroup.contains("season")
                val isMovie = !isSeries && (lowerGroup.contains("movie") || lowerGroup.contains("vod") || lowerGroup.contains("cinema") || lowerGroup.contains("film") || lowerUrl.contains("/movie/"))

                val type = when {
                    isSeries -> IptvType.SERIES
                    isMovie -> IptvType.MOVIE
                    else -> IptvType.LIVE_TV
                }

                val format = when {
                    lowerUrl.contains(".m3u8") -> StreamFormat.HLS
                    lowerUrl.contains(".ts") -> StreamFormat.MPEG_TS
                    lowerUrl.contains(".mp4") -> StreamFormat.MP4
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

                val id = tvgId?.takeIf { it.isNotBlank() } ?: "item_${UUID.randomUUID().toString().take(8)}"

                items.add(
                    IptvItem(
                        id = id,
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
            }
        }

        return items
    }
}
