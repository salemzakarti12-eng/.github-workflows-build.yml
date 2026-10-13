package com.example.model

data class SeriesEpisode(
    val id: String,
    val seriesId: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String,
    val streamUrl: String,
    val containerExtension: String = "mp4",
    val coverUrl: String? = null,
    val plot: String? = null,
    val duration: String? = null,
    val rating: String? = null
) {
    fun toIptvItem(seriesTitle: String, group: String): IptvItem {
        return IptvItem(
            id = "ep_${seriesId}_s${seasonNumber}_e${episodeNumber}_$id",
            name = "$seriesTitle - S%02dE%02d: $title".format(seasonNumber, episodeNumber),
            streamUrl = streamUrl,
            logoUrl = coverUrl,
            group = group,
            type = IptvType.SERIES,
            streamFormat = if (streamUrl.contains(".m3u8", ignoreCase = true)) StreamFormat.HLS else StreamFormat.MP4,
            seriesTitle = seriesTitle,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            rating = rating
        )
    }
}

data class SeriesSeason(
    val seasonNumber: Int,
    val name: String = "Season $seasonNumber",
    val episodes: List<SeriesEpisode>
)

data class SeriesDetails(
    val seriesId: String,
    val title: String,
    val coverUrl: String? = null,
    val backdropUrl: String? = null,
    val plot: String? = null,
    val genre: String? = null,
    val releaseDate: String? = null,
    val rating: String? = null,
    val seasons: List<SeriesSeason>
)
