package com.example.ui.channelbrowser

import android.content.res.Configuration
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GridView
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.CategoryInfo
import com.example.model.IptvItem
import com.example.model.IptvType
import com.example.model.StreamFormat
import com.example.ui.Screen
import com.example.ui.locale.LocalAppLanguage
import com.example.ui.locale.LocalAppStrings
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
import java.util.Locale

/**
 * Custom Non-Clipping Search Box that centers Arabic and English text vertically
 * without getting cut off by Material3 OutlinedTextField internal padding.
 */
@Composable
fun CompactSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    height: Dp = 42.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        interactionSource = interactionSource,
        textStyle = TextStyle(
            color = PureWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        ),
        cursorBrush = SolidColor(accentColor),
        modifier = modifier.height(height),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Slate900)
                    .border(
                        width = if (isFocused) 1.5.dp else 1.dp,
                        color = if (isFocused) accentColor else Slate700,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = if (isFocused) accentColor else Slate400,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = Slate400,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    innerTextField()
                }
                if (value.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = { onValueChange("") },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = Slate400,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun ContentBrowserScreen(
    screen: Screen,
    categories: List<CategoryInfo>,
    items: List<IptvItem>,
    searchQuery: String,
    selectedCategory: String,
    totalCount: Int,
    favoriteCount: Int = 0,
    onSearchChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onLoadMore: () -> Unit,
    onPlayItem: (IptvItem) -> Unit,
    onOpenSeries: ((IptvItem) -> Unit)? = null,
    onToggleFavorite: (IptvItem) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val isAr = LocalAppLanguage.current == "ar"

    val (title, icon, accentColor) = when (screen) {
        Screen.LIVE_CHANNELS -> Triple(
            if (isAr) "قنوات البث المباشر" else strings.liveChannelsTitle,
            Icons.Default.LiveTv,
            Color(0xFFEF4444)
        )
        Screen.MOVIES -> Triple(
            if (isAr) "الأفلام" else strings.moviesTitle,
            Icons.Default.Movie,
            Color(0xFF8B5CF6)
        )
        Screen.SERIES -> Triple(
            if (isAr) "المسلسلات" else strings.seriesTitle,
            Icons.Default.VideoLibrary,
            Color(0xFF06B6D4)
        )
        else -> Triple(
            if (isAr) "الترفيه" else strings.exploreEntertainment,
            Icons.Default.Tv,
            AccentCyan
        )
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isVOD = screen == Screen.MOVIES || screen == Screen.SERIES

    // Live TV Landscape View Mode: true = Square Logo Grid, false = Full-Width 1-Column List
    var isLiveGridView by remember { mutableStateOf(true) }

    val handleItemClick = { item: IptvItem ->
        if (item.type == IptvType.SERIES) {
            onOpenSeries?.invoke(item) ?: onPlayItem(item)
        } else {
            onPlayItem(item)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .statusBarsPadding()
            .testTag("content_browser_screen")
    ) {
        val isTv = com.example.util.LocalIsTelevision.current
        val isWideScreen = maxWidth >= 600.dp || isTv || isLandscape

        if (isWideScreen) {
            // Master-Detail Split Layout in Landscape / Tablet / TV
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 4.dp)
            ) {
                // Dedicated Vertical Category Sidebar (230.dp width)
                CategorySidebar(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    totalCount = totalCount,
                    favoriteCount = favoriteCount,
                    accentColor = accentColor,
                    onCategoryChange = onCategoryChange
                )

                // Divider line between sidebar and content
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(Slate800)
                )

                // Main Content Area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    // Unified Top Row with plenty of breathing room so nothing clips
                    CompactLandscapeHeader(
                        title = title,
                        icon = icon,
                        accentColor = accentColor,
                        totalCount = totalCount,
                        searchQuery = searchQuery,
                        onSearchChange = onSearchChange,
                        showViewToggle = !isVOD,
                        isGridView = isLiveGridView,
                        onToggleViewMode = { isLiveGridView = !isLiveGridView },
                        onBack = onBack,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )

                    // Content Area
                    if (isVOD) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 130.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 28.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (items.isEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    EmptyCategoryState()
                                }
                            } else {
                                items(items, key = { it.id }) { item ->
                                    VerticalPosterCard(
                                        item = item,
                                        accentColor = accentColor,
                                        onPlay = { handleItemClick(item) },
                                        onToggleFavorite = { onToggleFavorite(item) }
                                    )
                                }

                                if (items.size >= 100) {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        LoadMoreButton(onLoadMore = onLoadMore, accentColor = accentColor)
                                    }
                                }
                            }
                        }
                    } else if (isLiveGridView) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 135.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 28.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (items.isEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    EmptyCategoryState()
                                }
                            } else {
                                items(items, key = { it.id }) { item ->
                                    LiveTvGridCard(
                                        item = item,
                                        accentColor = accentColor,
                                        onPlay = { handleItemClick(item) },
                                        onToggleFavorite = { onToggleFavorite(item) }
                                    )
                                }

                                if (items.size >= 100) {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        LoadMoreButton(onLoadMore = onLoadMore, accentColor = accentColor)
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 28.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (items.isEmpty()) {
                                item {
                                    EmptyCategoryState()
                                }
                            } else {
                                items(items, key = { it.id }) { item ->
                                    StreamItemCard(
                                        item = item,
                                        accentColor = accentColor,
                                        onPlay = { handleItemClick(item) },
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
        } else {
            // Portrait Phone Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
            ) {
                PortraitHeader(
                    title = title,
                    icon = icon,
                    accentColor = accentColor,
                    totalCount = totalCount,
                    selectedCategory = selectedCategory,
                    searchQuery = searchQuery,
                    onSearchChange = onSearchChange,
                    onBack = onBack
                )

                CategoryChipsRow(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    totalCount = totalCount,
                    accentColor = accentColor,
                    onCategoryChange = onCategoryChange
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (isVOD) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 105.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (items.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                EmptyCategoryState()
                            }
                        } else {
                            items(items, key = { it.id }) { item ->
                                VerticalPosterCard(
                                    item = item,
                                    accentColor = accentColor,
                                    onPlay = { handleItemClick(item) },
                                    onToggleFavorite = { onToggleFavorite(item) }
                                )
                            }

                            if (items.size >= 100) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    LoadMoreButton(onLoadMore = onLoadMore, accentColor = accentColor)
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (items.isEmpty()) {
                            item {
                                EmptyCategoryState()
                            }
                        } else {
                            items(items, key = { it.id }) { item ->
                                StreamItemCard(
                                    item = item,
                                    accentColor = accentColor,
                                    onPlay = { handleItemClick(item) },
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
    }
}

@Composable
fun CategorySidebar(
    categories: List<CategoryInfo>,
    selectedCategory: String,
    totalCount: Int,
    favoriteCount: Int,
    accentColor: Color,
    onCategoryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchCategoryQuery by remember { mutableStateOf("") }
    val strings = LocalAppStrings.current
    val isAr = LocalAppLanguage.current == "ar"

    val filteredCategories = remember(categories, searchCategoryQuery) {
        if (searchCategoryQuery.isBlank()) {
            categories
        } else {
            categories.filter { it.name.contains(searchCategoryQuery, ignoreCase = true) }
        }
    }

    Column(
        modifier = modifier
            .width(230.dp)
            .fillMaxHeight()
            .background(Slate950)
            .padding(vertical = 6.dp)
            .testTag("category_sidebar")
    ) {
        // Non-clipping category search bar
        CompactSearchField(
            value = searchCategoryQuery,
            onValueChange = { searchCategoryQuery = it },
            placeholder = if (isAr) "البحث في الأقسام..." else strings.searchInCategories,
            accentColor = AccentCyan,
            height = 40.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .testTag("search_categories_input")
        )

        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            item {
                SidebarCategoryRow(
                    name = if (isAr) "الكل" else strings.all,
                    count = totalCount,
                    isSelected = selectedCategory.equals("All", ignoreCase = true),
                    accentColor = accentColor,
                    onClick = { onCategoryChange("All") }
                )
            }

            item {
                SidebarCategoryRow(
                    name = if (isAr) "المفضلة" else strings.favorites,
                    count = favoriteCount,
                    isSelected = selectedCategory.equals("Favorites", ignoreCase = true),
                    accentColor = Color(0xFFEF4444),
                    onClick = { onCategoryChange("Favorites") }
                )
            }

            items(filteredCategories, key = { it.id }) { cat ->
                SidebarCategoryRow(
                    name = cat.name,
                    count = cat.itemCount,
                    isSelected = selectedCategory.equals(cat.name, ignoreCase = true),
                    accentColor = accentColor,
                    onClick = { onCategoryChange(cat.name) }
                )
            }
        }
    }
}

@Composable
fun SidebarCategoryRow(
    name: String,
    count: Int?,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val highlightColor = if (accentColor == Color(0xFFEF4444)) Color(0xFFEF4444) else AccentCyan

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (isFocused) 2.dp else if (isSelected) 1.dp else 0.dp,
                color = if (isFocused) PureWhite else if (isSelected) highlightColor else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
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
            .testTag("category_sidebar_item_$name"),
        color = when {
            isFocused -> Slate800
            isSelected -> highlightColor.copy(alpha = 0.22f)
            else -> Color.Transparent
        },
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = name,
                fontSize = 12.sp,
                fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) highlightColor else if (isFocused) PureWhite else Slate300,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )

            if (count != null && count >= 0) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) highlightColor else Slate850)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "%,d".format(Locale.US, count),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Slate950 else Slate300,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
fun LiveTvGridCard(
    item: IptvItem,
    accentColor: Color,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "liveGridScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.15f)
            .scale(scale)
            .shadow(
                elevation = if (isFocused) 10.dp else 2.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = if (isFocused) AccentCyan else Color.Transparent
            )
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = if (isFocused) PureWhite else Slate800,
                shape = RoundedCornerShape(12.dp)
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
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .size(26.dp)
                    .align(Alignment.TopEnd)
                    .testTag("fav_button_${item.id}")
            ) {
                Icon(
                    imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (item.isFavorite) AccentRed else Slate400,
                    modifier = Modifier.size(15.dp)
                )
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Slate850)
                        .border(1.dp, Slate700, RoundedCornerShape(10.dp)),
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
                            imageVector = Icons.Default.LiveTv,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Text(
                    text = item.name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    lineHeight = 13.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun VerticalPosterCard(
    item: IptvItem,
    accentColor: Color,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "posterScale"
    )

    val formattedRating = remember(item.rating) {
        item.rating?.toDoubleOrNull()?.let { ratingVal ->
            if (ratingVal > 0.0) String.format(Locale.US, "%.1f", ratingVal) else null
        } ?: item.rating?.takeIf { it.isNotBlank() }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(2f / 3f)
            .scale(scale)
            .shadow(
                elevation = if (isFocused) 12.dp else 2.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = if (isFocused) AccentCyan else Color.Transparent
            )
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = if (isFocused) PureWhite else Slate800,
                shape = RoundedCornerShape(12.dp)
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
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!item.logoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.logoUrl,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Slate850),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (item.type == IptvType.SERIES) Icons.Default.VideoLibrary else Icons.Default.Movie,
                        contentDescription = null,
                        tint = Slate600,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0x22020617),
                                Color(0xCC020617),
                                Color(0xF5020617)
                            ),
                            startY = 100f
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                if (!formattedRating.isNullOrBlank()) {
                    Surface(
                        color = Color(0xD90F172A),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.border(0.5.dp, Slate700, RoundedCornerShape(6.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = formattedRating,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(0x800F172A))
                        .testTag("fav_button_${item.id}")
                ) {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (item.isFavorite) AccentRed else PureWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = item.name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
fun CompactLandscapeHeader(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    totalCount: Int,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    showViewToggle: Boolean = false,
    isGridView: Boolean = true,
    onToggleViewMode: () -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backInteractionSource = remember { MutableInteractionSource() }
    val isBackFocused by backInteractionSource.collectIsFocusedAsState()
    val isAr = LocalAppLanguage.current == "ar"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            interactionSource = backInteractionSource,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (isBackFocused) AccentCyan else Slate850)
                .border(
                    width = if (isBackFocused) 2.5.dp else 1.dp,
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
                tint = if (isBackFocused) Slate950 else PureWhite,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = PureWhite,
            maxLines = 1
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Non-clipping compact search field
        CompactSearchField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = if (isAr) "ابحث في $title..." else "Search $title...",
            accentColor = accentColor,
            height = 40.dp,
            modifier = Modifier
                .weight(1f)
                .testTag("input_search_channels")
        )

        if (showViewToggle) {
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onToggleViewMode,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Slate850)
                    .border(1.dp, Slate700, RoundedCornerShape(10.dp))
            ) {
                Icon(
                    imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                    contentDescription = "Toggle View",
                    tint = AccentCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun PortraitHeader(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    totalCount: Int,
    selectedCategory: String,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backInteractionSource = remember { MutableInteractionSource() }
    val isBackFocused by backInteractionSource.collectIsFocusedAsState()
    val isAr = LocalAppLanguage.current == "ar"

    val displayCategory = when {
        selectedCategory.equals("All", ignoreCase = true) -> if (isAr) "الكل" else "All"
        selectedCategory.equals("Favorites", ignoreCase = true) -> if (isAr) "المفضلة" else "Favorites"
        else -> selectedCategory
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                interactionSource = backInteractionSource,
                modifier = Modifier
                    .size(40.dp)
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
            Spacer(modifier = Modifier.width(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                    Text(
                        text = if (isAr) {
                            "الإجمالي %,d  •  %s".format(Locale.US, totalCount, displayCategory)
                        } else {
                            "%,d total  •  %s".format(Locale.US, totalCount, displayCategory)
                        },
                        fontSize = 11.sp,
                        color = Slate400,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        CompactSearchField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = if (isAr) "ابحث باسم القناة أو الفيلم أو القسم..." else "Search by stream name or category...",
            accentColor = accentColor,
            height = 44.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .testTag("input_search_channels")
        )
    }
}

@Composable
fun CategoryChipsRow(
    categories: List<CategoryInfo>,
    selectedCategory: String,
    totalCount: Int,
    accentColor: Color,
    onCategoryChange: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    val isAr = LocalAppLanguage.current == "ar"

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            CategoryChip(
                name = if (isAr) "الكل" else strings.all,
                count = totalCount,
                isSelected = selectedCategory.equals("All", ignoreCase = true),
                accentColor = accentColor,
                onClick = { onCategoryChange("All") }
            )
        }

        item {
            CategoryChip(
                name = if (isAr) "المفضلة" else strings.favorites,
                count = null,
                isSelected = selectedCategory.equals("Favorites", ignoreCase = true),
                accentColor = Color(0xFFEF4444),
                onClick = { onCategoryChange("Favorites") }
            )
        }

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
}

@Composable
fun EmptyCategoryState() {
    val strings = LocalAppStrings.current
    val isAr = LocalAppLanguage.current == "ar"
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Tv,
                contentDescription = null,
                tint = Slate600,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isAr) "لا توجد قنوات أو عروض في هذا القسم" else strings.noStreamsInCategory,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Slate300
            )
            Text(
                text = if (isAr) "جرب اختيار قسم آخر أو تغيير كلمة البحث" else strings.noStreamsHint,
                fontSize = 12.sp,
                color = Slate400,
                modifier = Modifier.padding(top = 4.dp)
            )
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
                color = PureWhite,
                maxLines = 1,
                softWrap = false
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
                        text = "%,d".format(Locale.US, count),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite,
                        maxLines = 1,
                        softWrap = false
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
    val strings = LocalAppStrings.current
    val isAr = LocalAppLanguage.current == "ar"

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
            .padding(vertical = 10.dp)
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
            text = if (isAr) "عرض المزيد" else strings.loadMore,
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
        targetValue = if (isFocused) 1.02f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "streamCardScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
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
                    onPlay()
                    true
                } else false
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onPlay)
            .testTag("stream_card_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = if (isFocused) Slate850 else Slate900),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Slate850)
                    .border(1.dp, Slate700, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!item.logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.logoUrl,
                        contentDescription = item.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(3.dp)
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
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.padding(top = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Slate800)
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.group,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate300,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    val formatLabel = when (item.type) {
                        IptvType.SERIES -> "Series"
                        else -> when (item.streamFormat) {
                            StreamFormat.HLS -> "HLS"
                            StreamFormat.MPEG_TS -> "MPEG-TS"
                            StreamFormat.MP4 -> "MP4"
                            StreamFormat.UNKNOWN -> if (item.streamUrl.contains(".ts", true)) "TS" else "HLS"
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (item.type == IptvType.SERIES) Color(0x3306B6D4) else Color(0x3338BDF8))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = formatLabel,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    if (!item.rating.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            val cleanRating = item.rating.toDoubleOrNull()?.let {
                                String.format(Locale.US, "%.1f", it)
                            } ?: item.rating
                            Text(
                                text = cleanRating,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate300,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .size(38.dp)
                    .testTag("fav_button_${item.id}")
            ) {
                Icon(
                    imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (item.isFavorite) AccentRed else Slate400,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onPlay,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isFocused) PureWhite else accentColor)
                    .testTag("play_button_${item.id}")
            ) {
                Icon(
                    imageVector = if (item.type == IptvType.SERIES) Icons.Default.VideoLibrary else Icons.Default.PlayArrow,
                    contentDescription = if (item.type == IptvType.SERIES) "View Series" else "Play",
                    tint = if (isFocused) Slate950 else PureWhite,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
