package com.example.ui.channelbrowser

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
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
import com.example.model.CategoryInfo
import com.example.model.IptvItem
import com.example.model.IptvType
import com.example.model.StreamFormat
import com.example.ui.Screen
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentRed
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950

@Composable
fun ContentBrowserScreen(
    screen: Screen,
    categories: List<CategoryInfo>,
    items: List<IptvItem>,
    searchQuery: String,
    selectedCategory: String,
    totalCount: Int,
    onSearchChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onLoadMore: () -> Unit,
    onPlayItem: (IptvItem) -> Unit,
    onToggleFavorite: (IptvItem) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (title, icon, accentColor) = when (screen) {
        Screen.LIVE_CHANNELS -> Triple("Live TV Channels", Icons.Default.LiveTv, Color(0xFFEF4444))
        Screen.MOVIES -> Triple("Movies & Cinema", Icons.Default.Movie, Color(0xFF8B5CF6))
        Screen.SERIES -> Triple("Series & Shows", Icons.Default.VideoLibrary, Color(0xFF06B6D4))
        else -> Triple("Browse Content", Icons.Default.Tv, AccentCyan)
    }

    val backInteractionSource = remember { MutableInteractionSource() }
    val isBackFocused by backInteractionSource.collectIsFocusedAsState()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .testTag("content_browser_screen")
    ) {
        val isTv = com.example.util.LocalIsTelevision.current
        val isWideScreen = maxWidth >= 600.dp || isTv

        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    interactionSource = backInteractionSource,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isBackFocused) AccentCyan else Slate850)
                        .border(
                            width = if (isBackFocused) 3.dp else 1.dp,
                            color = if (isBackFocused) PureWhite else Slate700,
                            shape = CircleShape
                        )
                        .focusable(interactionSource = backInteractionSource)
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyUp &&
                                (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                            ) {
                                onBack()
                                true
                            } else false
                        }
                        .testTag("btn_back_to_dashboard")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = if (isBackFocused) Slate950 else PureWhite
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                        Text(
                            text = "%,d total available  •  Showing: %s".format(totalCount, selectedCategory),
                            fontSize = 11.sp,
                            color = Slate400,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("input_search_channels"),
                placeholder = { Text("Search by stream name or category...", color = Slate400, fontSize = 14.sp) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Slate400)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = Slate400)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Slate900,
                    unfocusedContainerColor = Slate900,
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = Slate800,
                    focusedTextColor = PureWhite,
                    unfocusedTextColor = PureWhite
                )
            )

            // Category Chips Row (Category-First with item counts)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // "All" chip
                item {
                    CategoryChip(
                        name = "All",
                        count = totalCount,
                        isSelected = selectedCategory.equals("All", ignoreCase = true),
                        accentColor = accentColor,
                        onClick = { onCategoryChange("All") }
                    )
                }

                // "Favorites" chip
                item {
                    CategoryChip(
                        name = "Favorites",
                        count = null,
                        isSelected = selectedCategory.equals("Favorites", ignoreCase = true),
                        accentColor = Color(0xFFEF4444),
                        onClick = { onCategoryChange("Favorites") }
                    )
                }

                // Categorized chips with live counts
                items(categories, key = { it.id }) { cat ->
                    CategoryChip(
                        name = cat.name,
                        count = cat.itemCount,
                        isSelected = selectedCategory.equals(cat.name, ignoreCase = true),
                        accentColor = accentColor,
                        onClick = { onCategoryChange(cat.name) }
                    )
                }
            }

            // Stream List or Grid
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = null,
                            tint = Slate600,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No streams in this category",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate300
                        )
                        Text(
                            text = "Try switching categories or clearing the search query.",
                            fontSize = 13.sp,
                            color = Slate400,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else if (isWideScreen) {
                // TV / Tablet / Landscape 2-column wide grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        StreamItemCard(
                            item = item,
                            accentColor = accentColor,
                            onPlay = { onPlayItem(item) },
                            onToggleFavorite = { onToggleFavorite(item) }
                        )
                    }

                    // Load More trigger in grid
                    if (items.size >= 100) {
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                            LoadMoreButton(onLoadMore = onLoadMore, accentColor = accentColor)
                        }
                    }
                }
            } else {
                // Portrait single column
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        StreamItemCard(
                            item = item,
                            accentColor = accentColor,
                            onPlay = { onPlayItem(item) },
                            onToggleFavorite = { onToggleFavorite(item) }
                        )
                    }

                    if (items.size >= 100) {
                        item {
                            LoadMoreButton(onLoadMore = onLoadMore, accentColor = accentColor)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryChip(
    name: String,
    count: Int?,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val chipInteractionSource = remember { MutableInteractionSource() }
    val isChipFocused by chipInteractionSource.collectIsFocusedAsState()

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = if (isChipFocused) 2.5.dp else if (isSelected) 0.dp else 1.dp,
                color = if (isChipFocused) PureWhite else Slate700,
                shape = RoundedCornerShape(20.dp)
            )
            .focusable(interactionSource = chipInteractionSource)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                ) {
                    onClick()
                    true
                } else false
            }
            .clickable(interactionSource = chipInteractionSource, indication = null) {
                onClick()
            }
            .testTag("filter_chip_$name"),
        color = if (isChipFocused || isSelected) accentColor else Slate850,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                fontSize = 12.sp,
                fontWeight = if (isSelected || isChipFocused) FontWeight.Bold else FontWeight.Medium,
                color = PureWhite
            )
            if (count != null && count > 0) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0x33000000))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "%,d".format(count),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                }
            }
        }
    }
}

@Composable
fun LoadMoreButton(
    onLoadMore: () -> Unit,
    accentColor: Color
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Button(
        onClick = onLoadMore,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isFocused) PureWhite else Slate850,
            contentColor = if (isFocused) Slate950 else AccentCyan
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = if (isFocused) PureWhite else Slate700,
                shape = RoundedCornerShape(12.dp)
            )
            .focusable(interactionSource = interactionSource)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                ) {
                    onLoadMore()
                    true
                } else false
            }
            .testTag("btn_load_more_channels")
    ) {
        Icon(Icons.Default.ExpandMore, null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Load More Channels",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StreamItemCard(
    item: IptvItem,
    accentColor: Color,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.04f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "streamCardScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(
                elevation = if (isFocused) 14.dp else 2.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = if (isFocused) AccentCyan else Color.Transparent
            )
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isFocused) 3.5.dp else 1.dp,
                color = if (isFocused) PureWhite else Slate800,
                shape = RoundedCornerShape(16.dp)
            )
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
            .testTag("stream_card_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = if (isFocused) Slate850 else Slate900),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Slate850)
                    .border(1.dp, Slate700, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!item.logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.logoUrl,
                        contentDescription = item.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                    )
                } else {
                    Icon(
                        imageVector = when (item.type) {
                            IptvType.LIVE_TV -> Icons.Default.LiveTv
                            IptvType.MOVIE -> Icons.Default.Movie
                            IptvType.SERIES -> Icons.Default.VideoLibrary
                        },
                        contentDescription = null,
                        tint = Slate400,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Slate800)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.group,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate300
                        )
                    }

                    val formatLabel = when (item.streamFormat) {
                        StreamFormat.HLS -> "HLS .m3u8"
                        StreamFormat.MPEG_TS -> "MPEG-TS"
                        StreamFormat.MP4 -> "MP4"
                        StreamFormat.UNKNOWN -> if (item.streamUrl.contains(".ts", true)) "TS" else "HLS"
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x3338BDF8))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = formatLabel,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan
                        )
                    }

                    if (!item.rating.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = item.rating,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate300
                            )
                        }
                    }
                }

                if (!item.currentProgram.isNullOrBlank()) {
                    Text(
                        text = item.currentProgram,
                        fontSize = 11.sp,
                        color = Slate400,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("fav_button_${item.id}")
            ) {
                Icon(
                    imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (item.isFavorite) AccentRed else Slate400,
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(
                onClick = onPlay,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (isFocused) PureWhite else accentColor)
                    .testTag("play_button_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = if (isFocused) Slate950 else PureWhite,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}
