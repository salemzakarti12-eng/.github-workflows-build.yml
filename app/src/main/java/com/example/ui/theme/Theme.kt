package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val HypnoStreamDarkColorScheme = darkColorScheme(
    primary = AccentCyan,
    onPrimary = Slate950,
    primaryContainer = Slate800,
    onPrimaryContainer = PureWhite,
    secondary = AccentEmerald,
    onSecondary = Slate950,
    background = Slate950,
    onBackground = Slate100,
    surface = Slate900,
    onSurface = Slate100,
    surfaceVariant = Slate850,
    onSurfaceVariant = Slate300,
    outline = Slate700,
    error = AccentRed,
    onError = PureWhite
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to true for IPTV cinema dark slate theme
    dynamicColor: Boolean = false, // Keep consistent dark slate styling
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = HypnoStreamDarkColorScheme,
        typography = Typography,
        content = content
    )
}
