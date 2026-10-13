package com.example

import com.example.model.IptvType
import com.example.model.StreamFormat
import com.example.parser.M3uParser
import com.example.parser.XtreamUrlDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun testM3uParserLiveTvAndVod() {
    val sampleM3u = """
      #EXTM3U
      #EXTINF:-1 tvg-id="cnn.us" tvg-name="CNN HD" tvg-logo="https://example.com/cnn.png" group-title="News",CNN USA HD
      http://provider.stream:8080/live/user/pass/101.m3u8
      #EXTINF:-1 tvg-id="mov1" tvg-name="Inception (2010)" tvg-logo="https://example.com/mov1.png" group-title="VOD Movies",Inception (2010)
      http://provider.stream:8080/movie/user/pass/202.mp4
      #EXTINF:-1 tvg-id="ser1" tvg-name="Breaking Bad S01 E01" tvg-logo="https://example.com/bb.png" group-title="TV Shows",Breaking Bad S01 E01
      http://provider.stream:8080/series/user/pass/303.mp4
    """.trimIndent()

    val items = M3uParser.parse(sampleM3u)
    assertEquals(3, items.size)

    val live = items[0]
    assertEquals("CNN USA HD", live.name)
    assertEquals("News", live.group)
    assertEquals(IptvType.LIVE_TV, live.type)
    assertEquals(StreamFormat.HLS, live.streamFormat)

    val movie = items[1]
    assertEquals("Inception (2010)", movie.name)
    assertEquals("VOD Movies", movie.group)
    assertEquals(IptvType.MOVIE, movie.type)

    val series = items[2]
    assertEquals(IptvType.SERIES, series.type)
    assertEquals(1, series.seasonNumber)
    assertEquals(1, series.episodeNumber)
  }

  @Test
  fun testXtreamUrlDetection() {
    val m3uLink = "http://myiptv.com:8080/get.php?username=john_doe&password=secret456&type=m3u_plus&output=ts"
    val detected = XtreamUrlDetector.detect(m3uLink)
    assertNotNull(detected)
    assertEquals("http://myiptv.com:8080", detected!!.serverUrl)
    assertEquals("john_doe", detected.username)
    assertEquals("secret456", detected.password)

    val regularM3u = "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/us.m3u"
    val notDetected = XtreamUrlDetector.detect(regularM3u)
    assertNull(notDetected)
  }

  @Test
  fun testSeriesEpisodeConversion() {
    val ep = com.example.model.SeriesEpisode(
      id = "101",
      seriesId = "ser_bb",
      seasonNumber = 1,
      episodeNumber = 1,
      title = "Pilot",
      streamUrl = "http://iptv.server:8080/series/user/pass/101.mp4",
      containerExtension = "mp4",
      coverUrl = "http://example.com/cover.png",
      plot = "A chemistry teacher turns to crime."
    )

    val item = ep.toIptvItem(seriesTitle = "Breaking Bad", group = "Crime Drama")
    assertEquals("Breaking Bad - S01E01: Pilot", item.name)
    assertEquals(IptvType.SERIES, item.type)
    assertEquals(1, item.seasonNumber)
    assertEquals(1, item.episodeNumber)
    assertEquals("http://iptv.server:8080/series/user/pass/101.mp4", item.streamUrl)
  }

  @Test
  fun testTrackOptionModel() {
    val track = com.example.model.TrackOption(
      groupIndex = 0,
      trackIndex = 1,
      label = "English • 6ch",
      language = "eng",
      isSelected = true
    )
    assertEquals("English • 6ch", track.label)
    assertEquals("eng", track.language)
    assertEquals(true, track.isSelected)
  }
}
