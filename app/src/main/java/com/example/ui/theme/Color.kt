package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Pure Black Background & Deep Charcoal Surfaces
val Slate950 = Color(0xFF000000) // Pure Black main background
val Slate900 = Color(0xFF0F172A) // Cards, Navigation Rail & Sidebars
val Slate850 = Color(0xFF131D31) // Elevated surfaces & Search inputs
val Slate800 = Color(0xFF1E293B) // Borders & Dividers
val Slate700 = Color(0xFF334155) // Subtle borders & Inactive tracks
val Slate600 = Color(0xFF475569) // Placeholder icons
val Slate500 = Color(0xFF64748B)
val Slate400 = Color(0xFF94A3B8) // Secondary text
val Slate300 = Color(0xFFCBD5E1) // Subtitle text
val Slate100 = Color(0xFFF1F5F9)

// Accent Colors
val PureWhite = Color(0xFFFFFFFF)
val AccentCyan = Color(0xFF38BDF8)
val AccentRed = Color(0xFFEF4444)
val AccentEmerald = Color(0xFF10B981)
val AccentPurple = Color(0xFF8B5CF6)
val AccentAmber = Color(0xFFF59E0B)

// Hypnotix 3 Card Gradients
val TvChannelsGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFEF4444), Color(0xFFF97316)) // Red to Orange
)

val MoviesGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF8B5CF6), Color(0xFF3B82F6)) // Purple to Blue
)

val SeriesGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF06B6D4), Color(0xFF3B82F6)) // Cyan to Blue
)

// Glow & Card Overlays
val CardGlassBorder = Color(0x33FFFFFF)
val CardOverlayDark = Color(0x66000000)
