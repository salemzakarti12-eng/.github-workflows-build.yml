package com.example.model

enum class AspectRatioMode(val label: String) {
    FIT("Fit (100%)"),
    FILL("Fill Screen"),
    SIXTEEN_BY_NINE("16:9 Widescreen"),
    FOUR_BY_THREE("4:3 Standard"),
    ZOOM("Zoom Crop")
}

data class PlayerUiState(
    val currentItem: IptvItem? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isControlsVisible: Boolean = true,
    val aspectRatioMode: AspectRatioMode = AspectRatioMode.FIT,
    val errorMessage: String? = null,
    val durationMs: Long = 0L,
    val currentPositionMs: Long = 0L,
    val isQuickChannelListOpen: Boolean = false
)
