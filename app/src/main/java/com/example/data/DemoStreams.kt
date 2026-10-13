package com.example.data

import com.example.model.IptvItem
import com.example.model.IptvType
import com.example.model.StreamFormat

object DemoStreams {

    val demoChannels = listOf(
        IptvItem(
            id = "demo_live_1",
            name = "NASA TV Live HD",
            streamUrl = "https://ntv1.akamaized.net/hls/live/2014075/NASA-NTV1-HLS/master.m3u8",
            logoUrl = "https://upload.wikimedia.org/wikipedia/commons/e/e5/NASA_logo.svg",
            group = "Science & Space",
            type = IptvType.LIVE_TV,
            streamFormat = StreamFormat.HLS,
            currentProgram = "Live Space Station Downlink & Artemis Briefing",
            isFavorite = true
        ),
        IptvItem(
            id = "demo_live_2",
            name = "Euronews World (English)",
            streamUrl = "https://euronews-euronews-world-1-us.plex.tv/hls/master.m3u8",
            logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4c/Euronews_2016_logo.svg/320px-Euronews_2016_logo.svg.png",
            group = "News",
            type = IptvType.LIVE_TV,
            streamFormat = StreamFormat.HLS,
            currentProgram = "Global Headlines & Market Update",
            isFavorite = true
        ),
        IptvItem(
            id = "demo_live_3",
            name = "France 24 English Live",
            streamUrl = "https://static.france24.com/live/F24_EN_LO_HLS/live_tv.m3u8",
            logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f6/France_24_logo_2013.svg/320px-France_24_logo_2013.svg.png",
            group = "News",
            type = IptvType.LIVE_TV,
            streamFormat = StreamFormat.HLS,
            currentProgram = "Live International News & Perspectives"
        ),
        IptvItem(
            id = "demo_live_4",
            name = "Al Jazeera English HD",
            streamUrl = "https://live-hls-web-aje.getaj.net/AJE/01.m3u8",
            logoUrl = "https://upload.wikimedia.org/wikipedia/en/thumb/f/f2/Al_Jazeera_English_logo.svg/320px-Al_Jazeera_English_logo.svg.png",
            group = "News",
            type = IptvType.LIVE_TV,
            streamFormat = StreamFormat.HLS,
            currentProgram = "NewsHour Live Special Coverage"
        ),
        IptvItem(
            id = "demo_live_5",
            name = "Red Bull TV Live Sports",
            streamUrl = "https://rbmn-live.akamaized.net/hls/live/590964/BoRB-AT/master.m3u8",
            logoUrl = "https://upload.wikimedia.org/wikipedia/en/thumb/f/f5/Red_Bull_TV_logo.svg/320px-Red_Bull_TV_logo.svg.png",
            group = "Sports & Action",
            type = IptvType.LIVE_TV,
            streamFormat = StreamFormat.HLS,
            currentProgram = "Extreme Mountain Biking & Cliff Diving Highlights",
            isFavorite = true
        ),
        IptvItem(
            id = "demo_live_6",
            name = "DW News English Live",
            streamUrl = "https://dwamdstream102.akamaized.net/hls/live/2015525/dwstream102/index.m3u8",
            logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/7/75/Deutsche_Welle_symbol_2012.svg/320px-Deutsche_Welle_symbol_2012.svg.png",
            group = "News",
            type = IptvType.LIVE_TV,
            streamFormat = StreamFormat.HLS,
            currentProgram = "DW News: Journal & In Good Shape"
        ),
        IptvItem(
            id = "demo_live_7",
            name = "Real Stories Documentary TV",
            streamUrl = "https://amg01201-cinedigm-realstories-samsungus-e5i7j.amagi.tv/playlist/amg01201-cinedigm-realstories-samsungus/playlist.m3u8",
            logoUrl = "https://images.unsplash.com/photo-1518173946687-a4c8a383392e?w=200&fit=crop",
            group = "Documentary",
            type = IptvType.LIVE_TV,
            streamFormat = StreamFormat.HLS,
            currentProgram = "Wild Planet Explorers"
        ),
        IptvItem(
            id = "demo_live_8",
            name = "Test Stream (MPEG-TS Live Broadcast)",
            streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            logoUrl = "https://images.unsplash.com/photo-1594909122845-11baa439b7bf?w=200&fit=crop",
            group = "Test & Calibration",
            type = IptvType.LIVE_TV,
            streamFormat = StreamFormat.MPEG_TS,
            currentProgram = "Broadcasting Test Pattern (TS/HLS Demo)"
        )
    )

    val demoMovies = listOf(
        IptvItem(
            id = "demo_vod_1",
            name = "Big Buck Bunny",
            streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            logoUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=400&fit=crop",
            group = "Animation",
            type = IptvType.MOVIE,
            streamFormat = StreamFormat.MP4,
            year = "2024",
            rating = "8.6",
            currentProgram = "A giant rabbit seeks justice against mischievous bullies.",
            isFavorite = true
        ),
        IptvItem(
            id = "demo_vod_2",
            name = "Tears of Steel",
            streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            logoUrl = "https://images.unsplash.com/photo-1478760329108-5c3ed9d495a0?w=400&fit=crop",
            group = "Sci-Fi",
            type = IptvType.MOVIE,
            streamFormat = StreamFormat.MP4,
            year = "2023",
            rating = "7.9",
            currentProgram = "Futuristic warriors clash with rogue robotic machines.",
            isFavorite = true
        ),
        IptvItem(
            id = "demo_vod_3",
            name = "Sintel",
            streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
            logoUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=400&fit=crop",
            group = "Fantasy",
            type = IptvType.MOVIE,
            streamFormat = StreamFormat.MP4,
            year = "2023",
            rating = "8.2",
            currentProgram = "A lonely wanderer embarks on an epic quest for her dragon friend."
        ),
        IptvItem(
            id = "demo_vod_4",
            name = "Elephant's Dream",
            streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
            logoUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=400&fit=crop",
            group = "Surreal Cinema",
            type = IptvType.MOVIE,
            streamFormat = StreamFormat.MP4,
            year = "2022",
            rating = "7.4",
            currentProgram = "Two explorers navigate a living, labyrinthine machine world."
        ),
        IptvItem(
            id = "demo_vod_5",
            name = "For Bigger Blazes",
            streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            logoUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=400&fit=crop",
            group = "Action",
            type = IptvType.MOVIE,
            streamFormat = StreamFormat.MP4,
            year = "2024",
            rating = "8.0",
            currentProgram = "High-octane rally racing across unforgiving terrain."
        )
    )

    val demoSeries = listOf(
        IptvItem(
            id = "demo_series_1_e1",
            name = "S01 E01 - The Great Horizon",
            seriesTitle = "Cosmos Chronicles",
            streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackSeeTheWorld.mp4",
            logoUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=400&fit=crop",
            group = "Space Exploration",
            type = IptvType.SERIES,
            streamFormat = StreamFormat.MP4,
            seasonNumber = 1,
            episodeNumber = 1,
            rating = "9.1",
            currentProgram = "First journey past the outer planetary system.",
            isFavorite = true
        ),
        IptvItem(
            id = "demo_series_1_e2",
            name = "S01 E02 - Signals in the Deep",
            seriesTitle = "Cosmos Chronicles",
            streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4",
            logoUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=400&fit=crop",
            group = "Space Exploration",
            type = IptvType.SERIES,
            streamFormat = StreamFormat.MP4,
            seasonNumber = 1,
            episodeNumber = 2,
            rating = "9.0",
            currentProgram = "Decoding an anomalous electromagnetic beacon."
        ),
        IptvItem(
            id = "demo_series_2_e1",
            name = "S01 E01 - The Ignition",
            seriesTitle = "Velocity Underground",
            streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
            logoUrl = "https://images.unsplash.com/photo-1568605117036-5fe5e7bab0b7?w=400&fit=crop",
            group = "Action & Speed",
            type = IptvType.SERIES,
            streamFormat = StreamFormat.MP4,
            seasonNumber = 1,
            episodeNumber = 1,
            rating = "8.7",
            currentProgram = "Midnight time trials through neon-lit mountain passes."
        ),
        IptvItem(
            id = "demo_series_2_e2",
            name = "S01 E02 - Final Sector",
            seriesTitle = "Velocity Underground",
            streamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            logoUrl = "https://images.unsplash.com/photo-1568605117036-5fe5e7bab0b7?w=400&fit=crop",
            group = "Action & Speed",
            type = IptvType.SERIES,
            streamFormat = StreamFormat.MP4,
            seasonNumber = 1,
            episodeNumber = 2,
            rating = "8.8",
            currentProgram = "The showdown against the reigning street champion."
        )
    )
}
