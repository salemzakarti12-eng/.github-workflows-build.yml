package com.example.ui.series

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.SeriesDetails
import com.example.model.SeriesEpisode
import com.example.ui.locale.LocalAppLanguage
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
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
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isAr = LocalAppLanguage.current == "ar"
    val prefs = remember { context.getSharedPreferences("hypnostream_prefs", Context.MODE_PRIVATE) }

    var watchedEpisodeIds by remember {
        mutableStateOf(prefs.getStringSet("watched_episodes", emptySet()) ?: emptySet())
    }

    fun markEpisodeWatched(episodeKey: String) {
        val updated = watchedEpisodeIds.toMutableSet().apply { add(episodeKey) }
        watchedEpisodeIds = updated
        prefs.edit().putStringSet("watched_episodes", updated).apply()
    }

    fun toggleEpisodeWatched(episodeKey: String) {
        val updated = watchedEpisodeIds.toMutableSet().apply {
            if (contains(episodeKey)) remove(episodeKey) else add(episodeKey)
        }
        watchedEpisodeIds = updated
        prefs.edit().putStringSet("watched_episodes", updated).apply()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        if (isLoading || details == null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = AccentCyan)
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = if (isAr) "جاري تحميل المواسم والحلقات..." else "Loading season & episode data...",
                    color = Slate300,
                    fontSize = 14.sp
                )
            }
        } else {
            var selectedSeasonNum by remember(details.seriesId) {
                mutableIntStateOf(details.seasons.firstOrNull()?.seasonNumber ?: 1)
            }

            LaunchedEffect(details.seasons) {
                if (details.seasons.none { it.seasonNumber == selectedSeasonNum }) {
                    selectedSeasonNum = details.seasons.firstOrNull()?.seasonNumber ?: 1
                }
            }

            val currentSeason = details.seasons.firstOrNull { it.seasonNumber == selectedSeasonNum }
            val episodes = currentSeason?.episodes ?: emptyList()

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Hero Header
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                    ) {
                        val bannerUrl = details.backdropUrl ?: details.coverUrl
                        if (!bannerUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = bannerUrl,
                                contentDescription = details.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0x88060A12),
                                            Color(0xCC060A12),
                                            Slate950
                                        )
                                    )
                                )
                        )

                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .padding(16.dp)
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xAA000000))
                                .border(1.dp, Slate700, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = PureWhite
                            )
                        }

                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            if (!details.coverUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = details.coverUrl,
                                    contentDescription = details.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .width(92.dp)
                                        .aspectRatio(2f / 3f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.5.dp, Slate700, RoundedCornerShape(12.dp))
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = details.title,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = PureWhite,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (!details.rating.isNullOrBlank()) {
                                        Surface(
                                            color = Color(0x33F59E0B),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, Color(0xFFF59E0B))
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = null,
                                                    tint = Color(0xFFF59E0B),
                                                    modifier = Modifier.size(13.dp)
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
                                        Spacer(modifier = Modifier.width(10.dp))
                                    }
                                    if (!details.genre.isNullOrBlank()) {
                                        Text(
                                            text = details.genre,
                                            fontSize = 12.sp,
                                            color = AccentCyan,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (!details.plot.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = details.plot,
                                        fontSize = 12.sp,
                                        color = Slate300,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Seasons Selector
                item {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Text(
                            text = if (isAr) "المواسم (${details.seasons.size})" else "Seasons (${details.seasons.size})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(details.seasons, key = { it.seasonNumber }) { season ->
                                val isSelected = season.seasonNumber == selectedSeasonNum
                                Surface(
                                    onClick = { selectedSeasonNum = season.seasonNumber },
                                    color = if (isSelected) AccentCyan else Slate850,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, if (isSelected) PureWhite else Slate700)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = if (isAr) "الموسم ${season.seasonNumber}" else "Season ${season.seasonNumber}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Slate950 else PureWhite
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(if (isSelected) Color(0x33000000) else Slate800)
                                                .padding(horizontal = 6.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = "${season.episodes.size}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Slate950 else Slate300
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Season Episode Count & Watched Summary Header
                item {
                    val watchedInSeason = episodes.count { ep ->
                        val key = "${details.seriesId}_S${ep.seasonNumber}_E${ep.episodeNumber}_${ep.id}"
                        watchedEpisodeIds.contains(key) || watchedEpisodeIds.contains(ep.id)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isAr) {
                                "حلقات الموسم $selectedSeasonNum (${episodes.size})"
                            } else {
                                "Season $selectedSeasonNum Episodes (${episodes.size})"
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )

                        if (watchedInSeason > 0) {
                            Surface(
                                color = Color(0x2210B981),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, AccentEmerald)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = AccentEmerald,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isAr) {
                                            "تمت مشاهدة $watchedInSeason/${episodes.size}"
                                        } else {
                                            "$watchedInSeason/${episodes.size} Watched"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentEmerald
                                    )
                                }
                            }
                        }
                    }
                }

                // Episode Cards
                items(episodes, key = { "${it.id}_${it.seasonNumber}_${it.episodeNumber}" }) { episode ->
                    val episodeKey = "${details.seriesId}_S${episode.seasonNumber}_E${episode.episodeNumber}_${episode.id}"
                    val isWatched = watchedEpisodeIds.contains(episodeKey) || watchedEpisodeIds.contains(episode.id)

                    EpisodeRowCard(
                        episode = episode,
                        fallbackCover = details.coverUrl,
                        isWatched = isWatched,
                        isAr = isAr,
                        onPlay = {
                            markEpisodeWatched(episodeKey)
                            onPlayEpisode(episode)
                        },
                        onToggleWatched = {
                            toggleEpisodeWatched(episodeKey)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EpisodeRowCard(
    episode: SeriesEpisode,
    fallbackCover: String?,
    isWatched: Boolean,
    isAr: Boolean,
    onPlay: () -> Unit,
    onToggleWatched: () -> Unit
) {
    val cardInteraction = remember { MutableInteractionSource() }
    val isCardFocused by cardInteraction.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isCardFocused) 1.02f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "episodeCardScale"
    )

    Surface(
        color = if (isCardFocused) Slate850 else Slate900,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = if (isCardFocused) 2.5.dp else 1.dp,
            color = when {
                isCardFocused -> AccentCyan
                isWatched -> AccentEmerald.copy(alpha = 0.55f)
                else -> Slate800
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .scale(scale)
            .focusable(interactionSource = cardInteraction)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                ) {
                    onPlay()
                    true
                } else false
            }
            .clickable(interactionSource = cardInteraction, indication = null, onClick = onPlay)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Episode Thumbnail
            Box(
                modifier = Modifier
                    .width(116.dp)
                    .height(68.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Slate850),
                contentAlignment = Alignment.Center
            ) {
                val thumbUrl = episode.coverUrl ?: fallbackCover
                if (!thumbUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = thumbUrl,
                        contentDescription = episode.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x55000000))
                )

                Text(
                    text = "EP ${episode.episodeNumber}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = PureWhite
                )

                // Green Watched Badge in top-left corner of thumbnail
                if (isWatched) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(AccentEmerald)
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Slate950,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = if (isAr) "تمت" else "WATCHED",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = Slate950
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Episode Title & Plot
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = episode.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isWatched) Slate300 else PureWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!episode.plot.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = episode.plot,
                        fontSize = 12.sp,
                        color = Slate400,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 15.sp
                    )
                }
                if (!episode.duration.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = episode.duration,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isWatched) AccentEmerald else AccentCyan
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Toggle Watched Checkmark Button
            IconButton(
                onClick = onToggleWatched,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isWatched) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = if (isWatched) "Marked as watched" else "Mark as watched",
                    tint = if (isWatched) AccentEmerald else Slate400,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Play Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isCardFocused) PureWhite else AccentCyan)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Episode",
                    tint = Slate950,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
