package com.example.parser

import android.net.Uri

data class XtreamCredentials(
    val serverUrl: String,
    val username: String,
    val password: String
)

object XtreamUrlDetector {

    /**
     * Inspects an M3U playlist link to detect if it points to an Xtream Codes backend
     * (/get.php with username and password parameters).
     */
    fun detect(m3uUrl: String): XtreamCredentials? {
        val trimmed = m3uUrl.trim()
        if (!trimmed.contains("/get.php", ignoreCase = true)) {
            return null
        }

        try {
            val uri = Uri.parse(trimmed)
            val username = uri.getQueryParameter("username")
            val password = uri.getQueryParameter("password")

            if (!username.isNullOrBlank() && !password.isNullOrBlank()) {
                val scheme = uri.scheme ?: "http"
                val host = uri.host ?: return null
                val port = uri.port
                val serverUrl = if (port != -1) {
                    "$scheme://$host:$port"
                } else {
                    "$scheme://$host"
                }
                return XtreamCredentials(
                    serverUrl = serverUrl,
                    username = username,
                    password = password
                )
            }
        } catch (_: Exception) {}

        // Fallback with regex if Uri parsing fails on custom IPTV characters
        val usernameRegex = Regex("""(?i)[?&]username=([^&]+)""")
        val passwordRegex = Regex("""(?i)[?&]password=([^&]+)""")
        val uMatch = usernameRegex.find(trimmed)?.groupValues?.get(1)
        val pMatch = passwordRegex.find(trimmed)?.groupValues?.get(1)

        if (!uMatch.isNullOrBlank() && !pMatch.isNullOrBlank()) {
            val baseUrl = trimmed.substringBefore("/get.php")
            if (baseUrl.startsWith("http://", ignoreCase = true) || baseUrl.startsWith("https://", ignoreCase = true)) {
                return XtreamCredentials(
                    serverUrl = baseUrl,
                    username = uMatch,
                    password = pMatch
                )
            }
        }

        return null
    }
}
