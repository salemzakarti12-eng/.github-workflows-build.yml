package com.example.ui.dashboard

import android.content.Context
import android.widget.ImageView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.model.IptvItem
import com.example.model.IptvSource
import com.example.model.IptvType
import com.example.ui.Screen
import com.example.ui.components.LanguageToggleButton
import com.example.ui.locale.LocalAppLanguage
import com.example.ui.locale.LocalAppStrings
import com.example.ui.locale.LocalMiniPlayerActive
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.MoviesGradient
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SeriesGradient
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.ui.theme.TvChannelsGradient
import java.util.Locale

private val HistoryGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF059669), Color(0xFF0D9488), Color(0xFF0284C7))
)

@Composable
fun DashboardScreen(
    activeSource: IptvSource?,
    liveCount: Int,
    moviesCount: Int,
    seriesCount: Int,
    favorites: List<IptvItem>,
    recentlyWatched: List<IptvItem>,
    onNavigate: (Screen) -> Unit,
    onPlayItem: (IptvItem) -> Unit,
    onToggleLanguage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val isAr = LocalAppLanguage.current == "ar"
    val context = LocalContext.current
    val isMiniPlayerActive = LocalMiniPlayerActive.current
    val layoutDirection = if (isAr) LayoutDirection.Rtl else LayoutDirection.Ltr

    val positionPrefs = remember {
        context.getSharedPreferences("hypnostream_playback_positions", Context.MODE_PRIVATE)
    }

    val continueWatchingItems = remember(recentlyWatched) {
        recentlyWatched.filter { it.type != IptvType.LIVE_TV }.take(12)
    }

    val historyCount = remember(recentlyWatched) {
        recentlyWatched.count { it.type != IptvType.LIVE_TV }
    }

    val historyTitle = if (isAr) "سجل المشاهدة" else "History"
    val historySubtitle = if (isAr) "الأفلام والمسلسلات التي شاهدتها مؤخراً" else "Recently watched movies & TV series"
    val historyCountText = if (isAr) {
        "%,d عنصر محفوظ".format(Locale.US, historyCount)
    } else {
        "%,d Saved Titles".format(Locale.US, historyCount)
    }
    val historyBadge = if (isAr) "المشاهدات" else "WATCH LOG"

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        BoxWithConstraints(modifier = modifier.fillMaxSize().background(Slate950)) {
            val isTv = com.example.util.LocalIsTelevision.current
            val isWideScreen = maxWidth >= 600.dp || isTv

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("dashboard_screen"),
                contentPadding = PaddingValues(bottom = if (isMiniPlayerActive) 160.dp else 80.dp)
            ) {
                item {
                    DashboardHeader(
                        activeSource = activeSource,
                        onOpenSources = { onNavigate(Screen.SOURCES) },
                        onToggleLanguage = onToggleLanguage
                    )
                }

                item {
                    SourceInfoBanner(
                        activeSource = activeSource,
                        onSwitchSource = { onNavigate(Screen.SOURCES) }
                    )
                }

                item {
                    Text(
                        text = strings.exploreEntertainment,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = Slate400,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                if (isWideScreen) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                HypnotixGradientCard(
                                    title = strings.liveTv,
                                    subtitle = strings.liveTvSubtitle,
                                    countText = "%,d %s".format(Locale.US, liveCount, strings.channelsAvailable),
                                    icon = Icons.Default.LiveTv,
                                    gradient = TvChannelsGradient,
                                    testTag = "card_tv_channels",
                                    badgeText = strings.liveStreamingBadge,
                                    isLandscapeCard = true,
                                    onClick = { onNavigate(Screen.LIVE_CHANNELS) },
                                    modifier = Modifier.weight(1f)
                                )
                                HypnotixGradientCard(
                                    title = strings.movies,
                                    subtitle = strings.moviesSubtitle,
                                    countText = "%,d %s".format(Locale.US, moviesCount, strings.vodTitles),
                                    icon = Icons.Default.Movie,
                                    gradient = MoviesGradient,
                                    testTag = "card_movies",
                                    badgeText = strings.cinemaVodBadge,
                                    isLandscapeCard = true,
                                    onClick = { onNavigate(Screen.MOVIES) },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                HypnotixGradientCard(
                                    title = strings.series,
                                    subtitle = strings.seriesSubtitle,
                                    countText = "%,d %s".format(Locale.US, seriesCount, strings.showsCatalog),
                                    icon = Icons.Default.VideoLibrary,
                                    gradient = SeriesGradient,
                                    testTag = "card_series",
                                    badgeText = strings.episodicTvBadge,
                                    isLandscapeCard = true,
                                    onClick = { onNavigate(Screen.SERIES) },
                                    modifier = Modifier.weight(1f)
                                )
                                HypnotixGradientCard(
                                    title = historyTitle,
                                    subtitle = historySubtitle,
                                    countText = historyCountText,
                                    icon = Icons.Default.History,
                                    gradient = HistoryGradient,
                                    testTag = "card_history",
                                    badgeText = historyBadge,
                                    isLandscapeCard = true,
                                    onClick = { onNavigate(Screen.HISTORY) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                } else {
                    item {
                        HypnotixGradientCard(
                            title = strings.liveTv,
                            subtitle = strings.liveTvSubtitle,
                            countText = "%,d %s".format(Locale.US, liveCount, strings.channelsAvailable),
                            icon = Icons.Default.LiveTv,
                            gradient = TvChannelsGradient,
                            testTag = "card_tv_channels",
                            badgeText = strings.liveStreamingBadge,
                            isLandscapeCard = false,
                            onClick = { onNavigate(Screen.LIVE_CHANNELS) }
                        )
                    }

                    item {
                        HypnotixGradientCard(
                            title = strings.movies,
                            subtitle = strings.moviesSubtitle,
                            countText = "%,d %s".format(Locale.US, moviesCount, strings.vodTitles),
                            icon = Icons.Default.Movie,
                            gradient = MoviesGradient,
                            testTag = "card_movies",
                            badgeText = strings.cinemaVodBadge,
                            isLandscapeCard = false,
                            onClick = { onNavigate(Screen.MOVIES) }
                        )
                    }

                    item {
                        HypnotixGradientCard(
                            title = strings.series,
                            subtitle = strings.seriesSubtitle,
                            countText = "%,d %s".format(Locale.US, seriesCount, strings.showsCatalog),
                            icon = Icons.Default.VideoLibrary,
                            gradient = SeriesGradient,
                            testTag = "card_series",
                            badgeText = strings.episodicTvBadge,
                            isLandscapeCard = false,
                            onClick = { onNavigate(Screen.SERIES) }
                        )
                    }

                    item {
                        HypnotixGradientCard(
                            title = historyTitle,
                            subtitle = historySubtitle,
                            countText = historyCountText,
                            icon = Icons.Default.History,
                            gradient = HistoryGradient,
                            testTag = "card_history",
                            badgeText = historyBadge,
                            isLandscapeCard = false,
                            onClick = { onNavigate(Screen.HISTORY) }
                        )
                    }
                }

                // Continue Watching Rail with Cyan Progress Bars & Resume Timestamps
                if (continueWatchingItems.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        SectionHeader(
                            icon = Icons.Default.History,
                            title = if (isAr) "متابعة المشاهدة" else "Continue Watching",
                            count = continueWatchingItems.size,
                            accentColor = AccentCyan,
                            actionLabel = if (isAr) "عرض الكل" else "View All",
                            onActionClick = { onNavigate(Screen.HISTORY) }
                        )
                        HorizontalItemList(
                            items = continueWatchingItems,
                            showProgress = true,
                            getSavedPositionMs = { item ->
                                positionPrefs.getLong("resume_${item.id}_${item.streamUrl.hashCode()}", 0L)
                            },
                            onPlayItem = onPlayItem
                        )
                    }
                }

                // Favorites Row
                if (favorites.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        SectionHeader(
                            icon = Icons.Default.Favorite,
                            title = strings.favoriteChannelsAndShows,
                            count = favorites.size,
                            accentColor = Color(0xFFEF4444)
                        )
                        HorizontalItemList(
                            items = favorites,
                            onPlayItem = onPlayItem
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(70.dp))
                }
            }
        }
    }
}

@Composable
fun DashboardHeader(
    activeSource: IptvSource?,
    onOpenSources: () -> Unit,
    onToggleLanguage: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val strings = LocalAppStrings.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            AndroidView(
                factory = { ctx ->
                    ImageView(ctx).apply {
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        setImageDrawable(ctx.packageManager.getApplicationIcon(ctx.applicationInfo))
                    }
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "HypnoStream",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = PureWhite
                )
                Text(
                    text = strings.modernIptvPlayer,
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LanguageToggleButton(onToggleLanguage = onToggleLanguage)

            IconButton(
                onClick = onOpenSources,
                interactionSource = interactionSource,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isFocused) AccentCyan else Slate850)
                    .border(
                        width = if (isFocused) 3.dp else 1.dp,
                        color = if (isFocused) PureWhite else Slate700,
                        shape = CircleShape
                    )
                    .focusable(interactionSource = interactionSource)
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyUp &&
                            (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                        ) {
                            onOpenSources()
                            true
                        } else false
                    }
                    .testTag("btn_sources_config")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = strings.settings,
                    tint = if (isFocused) Slate950 else AccentCyan,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun SourceInfoBanner(
    activeSource: IptvSource?,
    onSwitchSource: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val strings = LocalAppStrings.current
    val isAr = LocalAppLanguage.current == "ar"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (isFocused) 3.dp else 1.dp,
                color = if (isFocused) AccentCyan else Slate800,
                shape = RoundedCornerShape(14.dp)
            )
            .focusable(interactionSource = interactionSource)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                ) {
                    onSwitchSource()
                    true
                } else false
            }
            .clickable(interactionSource = interactionSource, indication = null) { onSwitchSource() }
            .testTag("source_info_banner"),
        color = if (isFocused) Slate850 else Slate900,
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (activeSource != null) AccentEmerald else Slate600)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = activeSource?.name ?: strings.disconnected,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = PureWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = activeSource?.statusText ?: strings.noActiveSourceHint,
                        fontSize = 11.sp,
                        color = Slate400,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (activeSource != null) strings.switchSource else strings.configure,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentCyan
                )
                Icon(
                    imageVector = if (isAr) Icons.Default.ChevronLeft else Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun HypnotixGradientCard(
    title: String,
    subtitle: String,
    countText: String,
    icon: ImageVector,
    gradient: Brush,
    testTag: String,
    badgeText: String,
    isLandscapeCard: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isAr = LocalAppLanguage.current == "ar"

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "cardScale"
    )

    val cardHeight = if (isLandscapeCard) 150.dp else 134.dp
    val paddingModifier = if (isLandscapeCard) modifier else modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 7.dp)

    Card(
        modifier = paddingModifier
            .height(cardHeight)
            .scale(scale)
            .shadow(
                elevation = if (isFocused) 16.dp else 4.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = if (isFocused) AccentCyan else Color.Transparent,
                ambientColor = if (isFocused) PureWhite else Color.Transparent
            )
            .border(
                width = if (isFocused) 3.5.dp else 1.dp,
                color = if (isFocused) PureWhite else Color(0x33FFFFFF),
                shape = RoundedCornerShape(20.dp)
            )
            .clip(RoundedCornerShape(20.dp))
            .focusable(interactionSource = interactionSource)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                ) {
                    onClick()
                    true
                } else false
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(if (isLandscapeCard) 16.dp else 20.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0x1EFFFFFF),
                modifier = Modifier
                    .size(if (isLandscapeCard) 90.dp else 115.dp)
                    .align(Alignment.CenterEnd)
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isLandscapeCard) 36.dp else 40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x33FFFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = PureWhite,
                                modifier = Modifier.size(if (isLandscapeCard) 20.dp else 24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = title,
                                fontSize = if (isLandscapeCard) 18.sp else 21.sp,
                                fontWeight = FontWeight.Black,
                                color = PureWhite,
                                letterSpacing = 0.3.sp
                            )
                            Text(
                                text = countText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xDDFFFFFF)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x33000000))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = Color(0xEEFFFFFF),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (isAr) Icons.Default.ChevronLeft else Icons.Default.ChevronRight,
                        contentDescription = "Open",
                        tint = PureWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    icon: ImageVector,
    title: String,
    count: Int,
    accentColor: Color,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    val actionInteraction = remember { MutableInteractionSource() }
    val isActionFocused by actionInteraction.collectIsFocusedAsState()
    val isAr = LocalAppLanguage.current == "ar"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PureWhite
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Slate800)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "$count",
                    fontSize = 11.sp,
                    color = Slate300,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (actionLabel != null && onActionClick != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isActionFocused) AccentCyan else Slate850)
                    .border(
                        width = if (isActionFocused) 2.dp else 1.dp,
                        color = if (isActionFocused) PureWhite else Slate700,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .focusable(interactionSource = actionInteraction)
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyUp &&
                            (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                        ) {
                            onActionClick()
                            true
                        } else false
                    }
                    .clickable(interactionSource = actionInteraction, indication = null, onClick = onActionClick)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = actionLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActionFocused) Slate950 else AccentCyan
                )
                Icon(
                    imageVector = if (isAr) Icons.Default.ChevronLeft else Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = if (isActionFocused) Slate950 else AccentCyan,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun HorizontalItemList(
    items: List<IptvItem>,
    showProgress: Boolean = false,
    getSavedPositionMs: ((IptvItem) -> Long)? = null,
    onPlayItem: (IptvItem) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(items, key = { it.id }) { item ->
            val savedPosMs = remember(item.id, showProgress) {
                if (showProgress) getSavedPositionMs?.invoke(item) ?: 0L else 0L
            }
            ItemMiniCard(
                item = item,
                showProgress = showProgress,
                savedPositionMs = savedPosMs,
                onClick = { onPlayItem(item) }
            )
        }
    }
}

@Composable
fun ItemMiniCard(
    item: IptvItem,
    showProgress: Boolean = false,
    savedPositionMs: Long = 0L,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "itemMiniScale"
    )

    val progressFraction = remember(savedPositionMs, item.type) {
        if (savedPositionMs <= 5_000L) {
            0.18f
        } else {
            val estimatedTotalMs = if (item.type == IptvType.SERIES) 45 * 60_000L else 110 * 60_000L
            (savedPositionMs.toFloat() / estimatedTotalMs.toFloat()).coerceIn(0.08f, 0.94f)
        }
    }

    val resumeTimeBadge = remember(savedPositionMs) {
        if (savedPositionMs > 5_000L) {
            val totalSeconds = savedPositionMs / 1000L
            val minutes = totalSeconds / 60L
            val seconds = totalSeconds % 60L
            val hours = minutes / 60L
            if (hours > 0) {
                String.format(Locale.US, "%d:%02d:%02d", hours, minutes % 60L, seconds)
            } else {
                String.format(Locale.US, "%02d:%02d", minutes, seconds)
            }
        } else null
    }

    Card(
        modifier = Modifier
            .width(170.dp)
            .height(118.dp)
            .scale(scale)
            .shadow(
                elevation = if (isFocused) 12.dp else 2.dp,
                shape = RoundedCornerShape(14.dp),
                spotColor = if (isFocused) AccentCyan else Color.Transparent
            )
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (isFocused) 3.dp else 1.dp,
                color = if (isFocused) PureWhite else Slate800,
                shape = RoundedCornerShape(14.dp)
            )
            .focusable(interactionSource = interactionSource)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                ) {
                    onClick()
                    true
                } else false
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .testTag("mini_card_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(14.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!item.logoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.logoUrl,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xCC090D16), Slate950)
                            )
                        )
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Slate850)
                )
            }

            if (showProgress && resumeTimeBadge != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(7.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xCC030917))
                        .border(1.dp, AccentCyan.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "▶ $resumeTimeBadge",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentCyan
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (isFocused) AccentCyan else Color(0x99000000))
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (isFocused) Slate950 else PureWhite,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = if (showProgress) 10.dp else 8.dp)
            ) {
                Text(
                    text = item.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.group,
                    fontSize = 10.sp,
                    color = Slate300,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Sleek Cyan Progress Bar along bottom edge for Continue Watching
            if (showProgress) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Slate800)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressFraction)
                            .fillMaxHeight()
                            .background(AccentCyan)
                    )
                }
            }
        }
    }
}
