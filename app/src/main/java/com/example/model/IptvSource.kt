package com.example.model

enum class SourceType {
    XTREAM_CODES,
    M3U_URL,
    M3U_TEXT,
    DEMO
}

data class IptvSource(
    val id: String,
    val name: String,
    val type: SourceType,
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val m3uUrl: String = "",
    val m3uContent: String = "",
    val isActive: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis(),
    val channelCount: Int = 0,
    val movieCount: Int = 0,
    val seriesCount: Int = 0,
    val expirationDate: String? = null,
    val statusText: String? = null
)
