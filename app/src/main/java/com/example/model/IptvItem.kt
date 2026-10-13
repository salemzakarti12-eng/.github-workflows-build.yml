package com.example.model

enum class IptvType {
    LIVE_TV,
    MOVIE,
    SERIES
}

enum class StreamFormat {
    HLS,
    MPEG_TS,
    MP4,
    UNKNOWN
}

data class IptvItem(
    val id: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val group: String = "General",
    val type: IptvType = IptvType.LIVE_TV,
    val streamFormat: StreamFormat = StreamFormat.UNKNOWN,
    val epgChannelId: String? = null,
    val currentProgram: String? = null,
    val seriesTitle: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val year: String? = null,
    val rating: String? = null,
    val isFavorite: Boolean = false
) {
    fun detectFormat(): StreamFormat {
        val lower = streamUrl.lowercase()
        return when {
            lower.contains(".m3u8") -> StreamFormat.HLS
            lower.contains(".ts") -> StreamFormat.MPEG_TS
            lower.contains(".mp4") -> StreamFormat.MP4
            else -> StreamFormat.UNKNOWN
        }
    }
}
