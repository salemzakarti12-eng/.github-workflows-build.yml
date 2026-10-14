package com.example.ui.series

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.SeriesDetails
import com.example.model.SeriesEpisode
import com.example.ui.locale.LocalAppLanguage
import com.example.ui.locale.LocalAppStrings
import com.example.ui.locale.LocalMiniPlayerActive
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950

@Composable
fun SeriesDetailScreen(
    details: SeriesDetails?,
    isLoading: Boolean,
    onPlayEpisode: (SeriesEpisode) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val isAr = LocalAppLanguage.current == "ar"
    var selectedSeasonIndex by remember { mutableIntStateOf(0) }

    val backInteraction = remember { MutableInteractionSource() }
    val isBackFocused by backInteraction.collectIsFocusedAsState()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .testTag("series_detail_screen")
    ) {
        val isWide = maxWidth >= 600.dp

        if (isLoading && details == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = AccentCyan, strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(strings.loadingSeriesDetails, color = Slate300, fontSize = 14.sp)
                }
            }
            return@BoxWithConstraints
        }

        if (details == null || details.seasons.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(strings.noEpisodesFound, color = Slate400, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(AccentCyan)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back, tint = Slate950)
                    }
                }
            }
            return@BoxWithConstraints
        }

        val seasons = details.seasons
        val activeSeason = seasons.getOrNull(selectedSeasonIndex) ?: seasons.first()

        val isMiniPlayerActive = LocalMiniPlayerActive.current

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = if (isMiniPlayerActive) 140.dp else 48.dp)
        ) {
            // Hero Banner Header
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isWide) 260.dp else 220.dp)
                ) {
                    if (!details.backdropUrl.isNullOrBlank() || !details.coverUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = details.backdropUrl ?: details.coverUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF1E1B4B))))
                        )
                    }

                    // Scrim gradient
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x99070B14),
                                        Color(0xD9070B14),
                                        Slate950
                                    )
                                )
                            )
                    )

                    // Header content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        IconButton(
                            onClick = onBack,
                            interactionSource = backInteraction,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isBackFocused) AccentCyan else Slate850)
                                .border(
                                    width = if (isBackFocused) 3.dp else 1.dp,
                                    color = if (isBackFocused) PureWhite else Slate700,
                                    shape = CircleShape
                                )
                                .focusable(interactionSource = backInteraction)
                                .onPreviewKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyUp &&
                                        (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                    ) {
                                        onBack()
                                        true
                                    } else false
                                }
                                .testTag("btn_series_detail_back")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = if (isBackFocused) Slate950 else PureWhite
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (!details.coverUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = details.coverUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(width = 80.dp, height = 110.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, Slate700, RoundedCornerShape(8.dp))
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = details.title,
                                    fontSize = if (isWide) 22.sp else 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PureWhite,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    if (!details.rating.isNullOrBlank()) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0x33F59E0B))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Star,
                                                null,
                                                tint = Color(0xFFF59E0B),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = details.rating,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PureWhite
                                            )
                                        }
                                    }

                                    if (!details.genre.isNullOrBlank()) {
                                        Text(
                                            text = details.genre,
                                            fontSize = 11.sp,
                                            color = Slate400,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (!details.plot.isNullOrBlank()) {
                                    Text(
                                        text = details.plot,
                                        fontSize = 11.sp,
                                        color = Slate300,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Season Selection Tabs
            item {
                Column(modifier = Modifier.padding(top = 14.dp, bottom = 10.dp)) {
                    Text(
                        text = if (isAr) "المواسم (${seasons.size})" else "Seasons (${seasons.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(seasons.indices.toList()) { index ->
                            val season = seasons[index]
                            val isSelected = index == selectedSeasonIndex
                            val seasonInteraction = remember { MutableInteractionSource() }
                            val isFocused by seasonInteraction.collectIsFocusedAsState()

                            Surface(
                                color = if (isFocused || isSelected) AccentCyan else Slate850,
                                shape = RoundedCornerShape(20.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isFocused) 2.5.dp else 1.dp,
                                    color = if (isFocused) PureWhite else if (isSelected) AccentCyan else Slate700
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .focusable(interactionSource = seasonInteraction)
                                    .onPreviewKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyUp &&
                                            (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                        ) {
                                            selectedSeasonIndex = index
                                            true
                                        } else false
                                    }
                                    .clickable(interactionSource = seasonInteraction, indication = null) {
                                        selectedSeasonIndex = index
                                    }
                                    .testTag("tab_season_${season.seasonNumber}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isAr) "الموسم ${season.seasonNumber}" else season.name,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected || isFocused) Slate950 else PureWhite
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(if (isSelected || isFocused) Color(0x33000000) else Slate800)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${season.episodes.size}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected || isFocused) Slate950 else Slate300
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Episode List for Selected Season
            item {
                Text(
                    text = if (isAr) "حلقات الموسم ${activeSeason.seasonNumber} (${activeSeason.episodes.size})" else "${activeSeason.name} Episodes (${activeSeason.episodes.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            items(activeSeason.episodes, key = { it.id }) { episode ->
                EpisodeItemCard(
                    episode = episode,
                    onPlay = { onPlayEpisode(episode) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
fun EpisodeItemCard(
    episode: SeriesEpisode,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) Slate800 else Slate900
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isFocused) 2.5.dp else 1.dp,
            color = if (isFocused) PureWhite else Slate800
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .focusable(interactionSource = interactionSource)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                ) {
                    onPlay()
                    true
                } else false
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onPlay)
            .testTag("episode_${episode.seasonNumber}_${episode.episodeNumber}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Episode Thumbnail / Badge
            Box(
                modifier = Modifier
                    .size(width = 64.dp, height = 48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Slate850),
                contentAlignment = Alignment.Center
            ) {
                if (!episode.coverUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = episode.coverUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x99000000))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    val isAr = LocalAppLanguage.current == "ar"
                    Text(
                        text = if (isAr) "حلقة %d".format(episode.episodeNumber) else "EP %d".format(episode.episodeNumber),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = episode.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!episode.plot.isNullOrBlank()) {
                    Text(
                        text = episode.plot,
                        fontSize = 11.sp,
                        color = Slate400,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                if (!episode.duration.isNullOrBlank()) {
                    Text(
                        text = episode.duration,
                        fontSize = 10.sp,
                        color = AccentCyan,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Play Icon Button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isFocused) PureWhite else AccentCyan),
                contentAlignment = Alignment.Center
            ) {
                val isAr = LocalAppLanguage.current == "ar"
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = if (isAr) "تشغيل الحلقة" else "Play Episode",
                    tint = Slate950,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
