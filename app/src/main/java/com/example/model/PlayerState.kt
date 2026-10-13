package com.example.model

enum class AspectRatioMode(val label: String) {
    FIT("Fit (100%)"),
    FILL("Fill Screen"),
    SIXTEEN_BY_NINE("16:9 Widescreen"),
    FOUR_BY_THREE("4:3 Standard"),
    ZOOM("Zoom Crop")
}

data class TrackOption(
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val language: String? = null,
    val isSelected: Boolean = false
)

data class PlayerUiState(
    val currentItem: IptvItem? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isControlsVisible: Boolean = true,
    val aspectRatioMode: AspectRatioMode = AspectRatioMode.FIT,
    val errorMessage: String? = null,
    val durationMs: Long = 0L,
    val currentPositionMs: Long = 0L,
    val isQuickChannelListOpen: Boolean = false,
    val audioTracks: List<TrackOption> = emptyList(),
    val subtitleTracks: List<TrackOption> = emptyList(),
    val isSubtitleOff: Boolean = false
)
