package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Dark Slate Theme Colors
val Slate950 = Color(0xFF070B14)
val Slate900 = Color(0xFF0F172A)
val Slate850 = Color(0xFF131D31)
val Slate800 = Color(0xFF1E293B)
val Slate700 = Color(0xFF334155)
val Slate600 = Color(0xFF475569)
val Slate400 = Color(0xFF94A3B8)
val Slate300 = Color(0xFFCBD5E1)
val Slate100 = Color(0xFFF1F5F9)
val PureWhite = Color(0xFFFFFFFF)

// Accent Colors
val AccentCyan = Color(0xFF38BDF8)
val AccentEmerald = Color(0xFF10B981)
val AccentAmber = Color(0xFFF59E0B)
val AccentRed = Color(0xFFEF4444)

// Hypnotix 3 Card Gradients
val TvChannelsGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFE11D48), Color(0xFFEA580C)) // Red to Orange
)
val MoviesGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF8B5CF6), Color(0xFF2563EB)) // Purple to Blue
)
val SeriesGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF06B6D4), Color(0xFF1E3A8A)) // Cyan to Navy
)

// Glow & Card Overlays
val CardGlassBorder = Color(0x33FFFFFF)
val CardOverlayDark = Color(0x66000000)
