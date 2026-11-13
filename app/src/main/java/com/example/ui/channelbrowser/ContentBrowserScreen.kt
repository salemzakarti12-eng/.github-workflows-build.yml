package com.example.ui.channelbrowser

import android.content.res.Configuration
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.CategoryInfo
import com.example.model.IptvItem
import com.example.model.IptvType
import com.example.ui.Screen
import com.example.ui.locale.LocalAppLanguage
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentRed
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import com.example.util.CategoryTranslator
import com.example.util.LocalIsTelevision

@Composable
fun ContentBrowserScreen(
    screen: Screen,
    categories: List<CategoryInfo>,
    items: List<IptvItem>,
    searchQuery: String,
    selectedCategory: String,
    totalCount: Int,
    favoriteCount: Int,
    onSearchChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onLoadMore: () -> Unit,
    onPlayItem: (IptvItem) -> Unit,
    onOpenSeries: (IptvItem) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onBack: () -> Unit
) {
    val isAr = LocalAppLanguage.current == "ar"
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isTablet = configuration.smallestScreenWidthDp >= 600
    val isTv = LocalIsTelevision.current
    val useSideCategoryPanel = isLandscape || isTablet || isTv

    var categorySearchQuery by remember(screen) { mutableStateOf("") }

    val filteredCategories = remember(categories, categorySearchQuery, isAr) {
        if (categorySearchQuery.isBlank()) {
            categories
        } else {
            categories.filter { cat ->
                cat.name.contains(categorySearchQuery, ignoreCase = true) ||
                CategoryTranslator.translate(cat.name, isAr).contains(categorySearchQuery, ignoreCase = true)
            }
        }
    }

    val screenTitle = when (screen) {
        Screen.LIVE_CHANNELS -> if (isAr) "البث المباشر" else "Live TV"
        Screen.MOVIES -> if (isAr) "الأفلام" else "Movies"
        Screen.SERIES -> if (isAr) "المسلسلات" else "Series"
        else -> ""
    }

    val searchPlaceholder = when (screen) {
        Screen.LIVE_CHANNELS -> if (isAr) "ابحث في القنوات..." else "Search channels..."
        Screen.MOVIES -> if (isAr) "ابحث في الأفلام..." else "Search movies..."
        Screen.SERIES -> if (isAr) "ابحث في المسلسلات..." else "Search series..."
        else -> if (isAr) "بحث..." else "Search..."
    }

    val isVodPoster = screen == Screen.MOVIES || screen == Screen.SERIES
    val gridState = rememberLazyGridState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= items.size - 16 && items.isNotEmpty()
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            onLoadMore()
        }
    }

    if (useSideCategoryPanel) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Slate950)
        ) {
            Column(
                modifier = Modifier
                    .width(265.dp)
                    .fillMaxHeight()
                    .background(Slate900)
                    .border(BorderStroke(1.dp, Slate800))
                    .padding(10.dp)
            ) {
                OutlinedTextField(
                    value = categorySearchQuery,
                    onValueChange = { categorySearchQuery = it },
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = if (isAr) "البحث في الأقسام..." else "Search categories...",
                            fontSize = 12.sp,
                            color = Slate400,
                            maxLines = 1
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (categorySearchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { categorySearchQuery = "" },
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
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = Slate700,
                        focusedContainerColor = Slate850,
                        unfocusedContainerColor = Slate850,
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item(key = "cat_all") {
                        CategoryRowItem(
                            displayName = if (isAr) "الكل" else "All",
                            count = totalCount,
                            isSelected = selectedCategory.equals("All", true) || selectedCategory == "الكل",
                            onClick = { onCategoryChange("All") }
                        )
                    }

                    item(key = "cat_fav") {
                        CategoryRowItem(
                            displayName = if (isAr) "المفضلة" else "Favorites",
                            count = favoriteCount,
                            isSelected = selectedCategory.equals("Favorites", true) || selectedCategory == "المفضلة",
                            onClick = { onCategoryChange("Favorites") }
                        )
                    }

                    items(filteredCategories, key = { it.id }) { category ->
                        val translatedTitle = remember(category.name, isAr) {
                            CategoryTranslator.translate(category.name, isAr)
                        }
                        CategoryRowItem(
                            displayName = translatedTitle,
                            count = category.itemCount,
                            isSelected = selectedCategory == category.name,
                            onClick = { onCategoryChange(category.name) }
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Slate850)
                            .border(1.dp, Slate700, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PureWhite
                        )
                    }

                    Icon(
                        imageVector = when (screen) {
                            Screen.LIVE_CHANNELS -> Icons.Default.LiveTv
                            Screen.MOVIES -> Icons.Default.Movie
                            else -> Icons.Default.VideoLibrary
                        },
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(22.dp)
                    )

                    Text(
                        text = screenTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = PureWhite
                    )

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        singleLine = true,
                        placeholder = {
                            Text(
                                text = searchPlaceholder,
                                fontSize = 13.sp,
                                color = Slate400,
                                maxLines = 1
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Slate400,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { onSearchChange("") },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = Slate400,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = Slate700,
                            focusedContainerColor = Slate900,
                            unfocusedContainerColor = Slate900,
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(minSize = if (isVodPoster) 135.dp else 175.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(items, key = { idx, item -> "${item.id}_$idx" }) { _, item ->
                        ContentGridCard(
                            item = item,
                            isVodPoster = isVodPoster,
                            onClick = {
                                if (item.type == IptvType.SERIES) {
                                    onOpenSeries(item)
                                } else {
                                    onPlayItem(item)
                                }
                            },
                            onToggleFavorite = { onToggleFavorite(item.id) }
                        )
                    }
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Slate950)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Slate850)
                        .border(1.dp, Slate700, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = PureWhite
                    )
                }

                Text(
                    text = screenTitle,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = PureWhite
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = searchPlaceholder,
                            fontSize = 12.sp,
                            color = Slate400,
                            maxLines = 1
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = Slate700,
                        focusedContainerColor = Slate900,
                        unfocusedContainerColor = Slate900,
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "chip_all") {
                    CategoryChipItem(
                        displayName = if (isAr) "الكل ($totalCount)" else "All ($totalCount)",
                        isSelected = selectedCategory.equals("All", true) || selectedCategory == "الكل",
                        onClick = { onCategoryChange("All") }
                    )
                }
                item(key = "chip_fav") {
                    CategoryChipItem(
                        displayName = if (isAr) "المفضلة ($favoriteCount)" else "Favorites ($favoriteCount)",
                        isSelected = selectedCategory.equals("Favorites", true) || selectedCategory == "المفضلة",
                        onClick = { onCategoryChange("Favorites") }
                    )
                }
                items(filteredCategories, key = { it.id }) { category ->
                    val translatedTitle = remember(category.name, isAr) {
                        CategoryTranslator.translate(category.name, isAr)
                    }
                    CategoryChipItem(
                        displayName = "$translatedTitle (${category.itemCount})",
                        isSelected = selectedCategory == category.name,
                        onClick = { onCategoryChange(category.name) }
                    )
                }
            }

            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(minSize = if (isVodPoster) 120.dp else 160.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(items, key = { idx, item -> "${item.id}_$idx" }) { _, item ->
                    ContentGridCard(
                        item = item,
                        isVodPoster = isVodPoster,
                        onClick = {
                            if (item.type == IptvType.SERIES) {
                                onOpenSeries(item)
                            } else {
                                onPlayItem(item)
                            }
                        },
                        onToggleFavorite = { onToggleFavorite(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryRowItem(
    displayName: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()

    Surface(
        color = when {
            isSelected -> AccentCyan.copy(alpha = 0.2f)
            isFocused -> Slate850
            else -> Color.Transparent
        },
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = if (isFocused || isSelected) 1.5.dp else 0.dp,
            color = when {
                isFocused -> PureWhite
                isSelected -> AccentCyan
                else -> Color.Transparent
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .focusable(interactionSource = interaction)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                ) {
                    onClick()
                    true
                } else false
            }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = displayName,
                fontSize = 13.sp,
                fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isSelected) AccentCyan else PureWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) AccentCyan else Slate800)
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "$count",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Slate950 else Slate300
                )
            }
        }
    }
}

@Composable
private fun CategoryChipItem(
    displayName: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) AccentCyan else Slate850,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (isSelected) PureWhite else Slate700)
    ) {
        Text(
            text = displayName,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Slate950 else Slate300,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun ContentGridCard(
    item: IptvItem,
    isVodPoster: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.04f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "gridCardScale"
    )

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(if (isVodPoster) 2f / 3f else 16f / 10f)
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = if (isFocused) AccentCyan else Slate800,
                shape = RoundedCornerShape(14.dp)
            )
            .focusable(interactionSource = interaction)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                ) {
                    onClick()
                    true
                } else false
            }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!item.logoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.logoUrl,
                    contentDescription = item.name,
                    contentScale = if (isVodPoster) ContentScale.Crop else ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (isVodPoster) 0.dp else 14.dp)
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (item.type) {
                            IptvType.LIVE_TV -> Icons.Default.LiveTv
                            IptvType.MOVIE -> Icons.Default.Movie
                            IptvType.SERIES -> Icons.Default.VideoLibrary
                        },
                        contentDescription = null,
                        tint = Slate400,
                        modifier = Modifier.size(34.dp)
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
                                Color(0xBB060A12),
                                Color(0xEE060A12)
                            )
                        )
                    )
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0x99000000))
                    .clickable(onClick = onToggleFavorite)
            ) {
                Icon(
                    imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (item.isFavorite) AccentRed else PureWhite,
                    modifier = Modifier.size(16.dp)
                )
            }

            if (!item.rating.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xBB000000))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = item.rating,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                }
            }

            if (isFocused) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(AccentCyan)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Slate950,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text(
                    text = item.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
