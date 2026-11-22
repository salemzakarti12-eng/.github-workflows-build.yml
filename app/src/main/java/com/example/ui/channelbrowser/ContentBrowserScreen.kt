package com.example.ui.channelbrowser

import android.content.Context
import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.CategoryInfo
import com.example.model.IptvItem
import com.example.model.IptvType
import com.example.ui.Screen
import com.example.ui.locale.LocalAppLanguage
import com.example.ui.locale.LocalAppStrings
import com.example.ui.player.EpgManager
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
import kotlinx.coroutines.delay
import org.json.JSONArray

private data class SectionThemeColors(
    val accent: Color,
    val accentSoft: Color,
    val glowBorder: Color,
    val ambientBgTop: Color
)

private fun getSectionThemeColors(screen: Screen): SectionThemeColors {
    return when (screen) {
        Screen.LIVE_CHANNELS -> SectionThemeColors(
            accent = Color(0xFFFF6B35),
            accentSoft = Color(0x2EFF6B35),
            glowBorder = Color(0x66FF6B35),
            ambientBgTop = Color(0x1FFF6B35)
        )
        Screen.MOVIES -> SectionThemeColors(
            accent = Color(0xFF9D4EDD),
            accentSoft = Color(0x2E9D4EDD),
            glowBorder = Color(0x669D4EDD),
            ambientBgTop = Color(0x1F9D4EDD)
        )
        Screen.SERIES -> SectionThemeColors(
            accent = Color(0xFF00E5FF),
            accentSoft = Color(0x2E00E5FF),
            glowBorder = Color(0x6600E5FF),
            ambientBgTop = Color(0x1F00E5FF)
        )
        else -> SectionThemeColors(
            accent = Color(0xFF00E5FF),
            accentSoft = Color(0x2E00E5FF),
            glowBorder = Color(0x6600E5FF),
            ambientBgTop = Color(0x1F00E5FF)
        )
    }
}

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
    onToggleFavorite: (IptvItem) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val currentLanguage = LocalAppLanguage.current
    val isAr = currentLanguage == "ar"
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isTablet = configuration.smallestScreenWidthDp >= 600
    val isTv = LocalIsTelevision.current
    val useSideCategoryPanel = isLandscape || isTablet || isTv

    val sectionColors = remember(screen) { getSectionThemeColors(screen) }
    val typeFavCount = favoriteCount

    val searchPrefs = remember {
        context.getSharedPreferences("hypnostream_search_history", Context.MODE_PRIVATE)
    }
    val historyStorageKey = "recent_searches_${screen.name}"
    var recentSearches by remember(screen) {
        mutableStateOf(loadRecentSearches(searchPrefs.getString(historyStorageKey, null)))
    }

    val recordSearchTerm: (String) -> Unit = { raw ->
        val clean = raw.trim()
        if (clean.length >= 2) {
            val updated = (listOf(clean) + recentSearches.filterNot { it.equals(clean, ignoreCase = true) })
                .take(6)
            recentSearches = updated
            searchPrefs.edit().putString(historyStorageKey, JSONArray(updated).toString()).apply()
        }
    }

    val clearRecentSearches: () -> Unit = {
        recentSearches = emptyList()
        searchPrefs.edit().remove(historyStorageKey).apply()
    }

    var categorySearchQuery by remember(screen) { mutableStateOf("") }

    val filteredCategories = remember(categories, categorySearchQuery, currentLanguage) {
        if (categorySearchQuery.isBlank()) {
            categories
        } else {
            categories.filter { cat ->
                cat.name.contains(categorySearchQuery, ignoreCase = true) ||
                CategoryTranslator.translateForLanguage(cat.name, currentLanguage)
                    .contains(categorySearchQuery, ignoreCase = true)
            }
        }
    }

    val screenTitle = when (screen) {
        Screen.LIVE_CHANNELS -> strings.liveTv
        Screen.MOVIES -> strings.movies
        Screen.SERIES -> strings.series
        else -> ""
    }

    val searchPlaceholder = strings.searchByStreamName
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

    val screenBackgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF060B16),
            Color(0xFF04070F),
            Slate950
        )
    )

    if (useSideCategoryPanel) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(screenBackgroundBrush)
        ) {
            // Left Glassmorphic Category Sidebar
            Column(
                modifier = Modifier
                    .width(268.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xEE0B1221),
                                Color(0xF5070C17)
                            )
                        )
                    )
                    .border(
                        BorderStroke(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                colors = listOf(sectionColors.glowBorder, Color(0x22FFFFFF))
                            )
                        )
                    )
                    .padding(11.dp)
            ) {
                OutlinedTextField(
                    value = categorySearchQuery,
                    onValueChange = { categorySearchQuery = it },
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = strings.searchInCategories,
                            fontSize = 12.sp,
                            color = Slate400,
                            maxLines = 1
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = sectionColors.accent,
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
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = sectionColors.accent,
                        unfocusedBorderColor = Color(0x26FFFFFF),
                        focusedContainerColor = Color(0x99111827),
                        unfocusedContainerColor = Color(0x66111827),
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item(key = "cat_all") {
                        CategoryRowItem(
                            displayName = strings.all,
                            count = totalCount,
                            isSelected = selectedCategory.equals("All", true) || selectedCategory == "الكل",
                            sectionColors = sectionColors,
                            onClick = { onCategoryChange("All") }
                        )
                    }

                    item(key = "cat_fav") {
                        CategoryRowItem(
                            displayName = strings.favorites,
                            count = typeFavCount,
                            isSelected = selectedCategory.equals("Favorites", true) || selectedCategory == "المفضلة",
                            sectionColors = sectionColors,
                            isFavoriteRow = true,
                            onClick = { onCategoryChange("Favorites") }
                        )
                    }

                    items(filteredCategories, key = { it.id }) { category ->
                        val translatedTitle = remember(category.name, currentLanguage) {
                            CategoryTranslator.translateForLanguage(category.name, currentLanguage)
                        }
                        CategoryRowItem(
                            displayName = translatedTitle,
                            count = category.itemCount,
                            isSelected = selectedCategory == category.name,
                            sectionColors = sectionColors,
                            onClick = { onCategoryChange(category.name) }
                        )
                    }
                }
            }

            // Right Content Area with Subtle Ambient Top Glow
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(sectionColors.ambientBgTop, Color.Transparent),
                            endY = 320f
                        )
                    )
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Glassmorphic Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0x99111827))
                                .border(1.dp, sectionColors.glowBorder, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = strings.back,
                                tint = PureWhite
                            )
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(sectionColors.accentSoft)
                                .border(1.dp, sectionColors.glowBorder, RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                imageVector = when (screen) {
                                    Screen.LIVE_CHANNELS -> Icons.Default.LiveTv
                                    Screen.MOVIES -> Icons.Default.Movie
                                    else -> Icons.Default.VideoLibrary
                                },
                                contentDescription = null,
                                tint = sectionColors.accent,
                                modifier = Modifier.size(21.dp)
                            )
                        }

                        Column {
                            Text(
                                text = screenTitle,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Black,
                                color = PureWhite
                            )
                            Text(
                                text = if (isAr) "%,d عنصر متاح".format(totalCount) else "%,d titles available".format(totalCount),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = sectionColors.accent
                            )
                        }

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchChange,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    recordSearchTerm(searchQuery)
                                    focusManager.clearFocus()
                                }
                            ),
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
                                    tint = sectionColors.accent,
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
                                focusedBorderColor = sectionColors.accent,
                                unfocusedBorderColor = Color(0x26FFFFFF),
                                focusedContainerColor = Color(0xB30F172A),
                                unfocusedContainerColor = Color(0x800F172A),
                                focusedTextColor = PureWhite,
                                unfocusedTextColor = PureWhite
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (searchQuery.isEmpty() && recentSearches.isNotEmpty()) {
                        RecentSearchesRow(
                            recentSearches = recentSearches,
                            isAr = isAr,
                            sectionColors = sectionColors,
                            onSelectSearch = { term ->
                                onSearchChange(term)
                                recordSearchTerm(term)
                            },
                            onClearAll = clearRecentSearches
                        )
                    }

                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Adaptive(minSize = if (isVodPoster) 138.dp else 178.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(items, key = { idx, item -> "${item.id}_$idx" }) { _, item ->
                            ContentGridCard(
                                item = item,
                                isVodPoster = isVodPoster,
                                isAr = isAr,
                                sectionColors = sectionColors,
                                onClick = {
                                    if (searchQuery.isNotBlank()) {
                                        recordSearchTerm(searchQuery)
                                    }
                                    if (item.type == IptvType.SERIES) {
                                        onOpenSeries(item)
                                    } else {
                                        onPlayItem(item)
                                    }
                                },
                                onToggleFavorite = { onToggleFavorite(item) }
                            )
                        }
                    }
                }
            }
        }
    } else {
        // Mobile Portrait Glassmorphic Layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(screenBackgroundBrush)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(sectionColors.ambientBgTop, Color.Transparent),
                        endY = 260f
                    )
                )
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
                        .background(Color(0x99111827))
                        .border(1.dp, sectionColors.glowBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.back,
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
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            recordSearchTerm(searchQuery)
                            focusManager.clearFocus()
                        }
                    ),
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
                            tint = sectionColors.accent,
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
                        focusedBorderColor = sectionColors.accent,
                        unfocusedBorderColor = Color(0x26FFFFFF),
                        focusedContainerColor = Color(0xB30F172A),
                        unfocusedContainerColor = Color(0x800F172A),
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            if (searchQuery.isEmpty() && recentSearches.isNotEmpty()) {
                RecentSearchesRow(
                    recentSearches = recentSearches,
                    isAr = isAr,
                    sectionColors = sectionColors,
                    onSelectSearch = { term ->
                        onSearchChange(term)
                        recordSearchTerm(term)
                    },
                    onClearAll = clearRecentSearches
                )
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "chip_all") {
                    CategoryChipItem(
                        displayName = "${strings.all} ($totalCount)",
                        isSelected = selectedCategory.equals("All", true) || selectedCategory == "الكل",
                        sectionColors = sectionColors,
                        onClick = { onCategoryChange("All") }
                    )
                }
                item(key = "chip_fav") {
                    CategoryChipItem(
                        displayName = "${strings.favorites} ($typeFavCount)",
                        isSelected = selectedCategory.equals("Favorites", true) || selectedCategory == "المفضلة",
                        sectionColors = sectionColors,
                        onClick = { onCategoryChange("Favorites") }
                    )
                }
                items(filteredCategories, key = { it.id }) { category ->
                    val translatedTitle = remember(category.name, currentLanguage) {
                        CategoryTranslator.translateForLanguage(category.name, currentLanguage)
                    }
                    CategoryChipItem(
                        displayName = "$translatedTitle (${category.itemCount})",
                        isSelected = selectedCategory == category.name,
                        sectionColors = sectionColors,
                        onClick = { onCategoryChange(category.name) }
                    )
                }
            }

            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(minSize = if (isVodPoster) 124.dp else 162.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(items, key = { idx, item -> "${item.id}_$idx" }) { _, item ->
                    ContentGridCard(
                        item = item,
                        isVodPoster = isVodPoster,
                        isAr = isAr,
                        sectionColors = sectionColors,
                        onClick = {
                            if (searchQuery.isNotBlank()) {
                                recordSearchTerm(searchQuery)
                            }
                            if (item.type == IptvType.SERIES) {
                                onOpenSeries(item)
                            } else {
                                onPlayItem(item)
                            }
                        },
                        onToggleFavorite = { onToggleFavorite(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentSearchesRow(
    recentSearches: List<String>,
    isAr: Boolean,
    sectionColors: SectionThemeColors,
    onSelectSearch: (String) -> Unit,
    onClearAll: () -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        items(recentSearches, key = { "recent_$it" }) { term ->
            val interaction = remember { MutableInteractionSource() }
            val isFocused by interaction.collectIsFocusedAsState()

            Surface(
                color = if (isFocused) sectionColors.accentSoft else Color(0x88111827),
                shape = CircleShape,
                border = BorderStroke(
                    width = if (isFocused) 1.5.dp else 1.dp,
                    color = if (isFocused) PureWhite else sectionColors.glowBorder
                ),
                modifier = Modifier
                    .focusable(interactionSource = interaction)
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyUp &&
                            (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                        ) {
                            onSelectSearch(term)
                            true
                        } else false
                    }
                    .clickable(interactionSource = interaction, indication = null) {
                        onSelectSearch(term)
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = sectionColors.accent,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = term,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PureWhite,
                        maxLines = 1
                    )
                }
            }
        }

        item(key = "clear_recent_searches") {
            val clearInteraction = remember { MutableInteractionSource() }
            val isClearFocused by clearInteraction.collectIsFocusedAsState()

            Surface(
                color = if (isClearFocused) AccentRed else Color(0x88111827),
                shape = CircleShape,
                border = BorderStroke(1.dp, if (isClearFocused) PureWhite else Color(0x26FFFFFF)),
                modifier = Modifier
                    .focusable(interactionSource = clearInteraction)
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyUp &&
                            (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                        ) {
                            onClearAll()
                            true
                        } else false
                    }
                    .clickable(interactionSource = clearInteraction, indication = null, onClick = onClearAll)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = if (isClearFocused) PureWhite else Slate400,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = if (isAr) "مسح" else "Clear",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isClearFocused) PureWhite else Slate400
                    )
                }
            }
        }
    }
}

private fun loadRecentSearches(rawJson: String?): List<String> {
    if (rawJson.isNullOrBlank()) return emptyList()
    return try {
        val arr = JSONArray(rawJson)
        List(arr.length()) { idx -> arr.getString(idx) }.filter { it.isNotBlank() }
    } catch (_: Exception) {
        emptyList()
    }
}

@Composable
private fun CategoryRowItem(
    displayName: String,
    count: Int,
    isSelected: Boolean,
    sectionColors: SectionThemeColors,
    isFavoriteRow: Boolean = false,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()

    val activeAccent = if (isFavoriteRow) AccentRed else sectionColors.accent
    val activeSoft = if (isFavoriteRow) AccentRed.copy(alpha = 0.18f) else sectionColors.accentSoft

    Surface(
        color = when {
            isSelected -> activeSoft
            isFocused -> Color(0x991E293B)
            else -> Color(0x330F172A)
        },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (isFocused || isSelected) 1.5.dp else 1.dp,
            color = when {
                isFocused -> PureWhite
                isSelected -> activeAccent
                else -> Color(0x14FFFFFF)
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
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(16.dp)
                        .clip(CircleShape)
                        .background(activeAccent)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            if (isFavoriteRow) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = AccentRed,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            Text(
                text = displayName,
                fontSize = 13.sp,
                fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isSelected) activeAccent else PureWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) activeAccent else Color(0x881E293B))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "$count",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color(0xFF030712) else Slate300
                )
            }
        }
    }
}

@Composable
private fun CategoryChipItem(
    displayName: String,
    isSelected: Boolean,
    sectionColors: SectionThemeColors,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) sectionColors.accent else Color(0x99111827),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isSelected) PureWhite else Color(0x26FFFFFF))
    ) {
        Text(
            text = displayName,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color(0xFF030712) else Slate300,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContentGridCard(
    item: IptvItem,
    isVodPoster: Boolean,
    isAr: Boolean,
    sectionColors: SectionThemeColors,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()

    var showContextDialog by remember { mutableStateOf(false) }
    var remoteLongPressHandled by remember { mutableStateOf(false) }

    val epgInfo = remember(item.epgChannelId, item.type) {
        if (item.type == IptvType.LIVE_TV && !item.epgChannelId.isNullOrBlank()) {
            EpgManager.getNowAndNext(item.epgChannelId)
        } else {
            null
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.045f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "gridCardScale"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B111E)),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(if (isVodPoster) 2f / 3f else 16f / 10f)
            .scale(scale)
            .shadow(
                elevation = if (isFocused) 16.dp else 4.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = if (isFocused) sectionColors.accent else Color.Transparent
            )
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                brush = if (isFocused) {
                    Brush.linearGradient(listOf(sectionColors.accent, PureWhite, sectionColors.accent))
                } else {
                    Brush.verticalGradient(listOf(Color(0x29FFFFFF), Color(0x0DFFFFFF)))
                },
                shape = RoundedCornerShape(16.dp)
            )
            .focusable(interactionSource = interaction)
            .onPreviewKeyEvent { event ->
                val isSelectKey = event.key == Key.DirectionCenter ||
                        event.key == Key.Enter ||
                        event.key == Key.NumPadEnter

                when {
                    isSelectKey && event.type == KeyEventType.KeyDown -> {
                        if (event.nativeKeyEvent.isLongPress || event.nativeKeyEvent.repeatCount > 0) {
                            if (!remoteLongPressHandled) {
                                remoteLongPressHandled = true
                                showContextDialog = true
                            }
                            true
                        } else {
                            false
                        }
                    }
                    isSelectKey && event.type == KeyEventType.KeyUp -> {
                        if (remoteLongPressHandled) {
                            remoteLongPressHandled = false
                            true
                        } else {
                            onClick()
                            true
                        }
                    }
                    (event.key == Key.Menu || event.key == Key.Bookmark) && event.type == KeyEventType.KeyUp -> {
                        showContextDialog = true
                        true
                    }
                    else -> false
                }
            }
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = { showContextDialog = true }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(sectionColors.accentSoft, Color(0xFF090E1A)),
                        radius = 450f
                    )
                )
        ) {
            if (!item.logoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.logoUrl,
                    contentDescription = item.name,
                    contentScale = if (isVodPoster) ContentScale.Crop else ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (isVodPoster) 0.dp else 16.dp)
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
                        tint = sectionColors.accent.copy(alpha = 0.7f),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x22000000),
                                Color.Transparent,
                                Color(0xCC050912),
                                Color(0xF5050912)
                            )
                        )
                    )
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(7.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xAA060A12))
                    .border(1.dp, Color(0x26FFFFFF), CircleShape)
                    .clickable(onClick = onToggleFavorite)
            ) {
                Icon(
                    imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (item.isFavorite) AccentRed else PureWhite,
                    modifier = Modifier.size(15.dp)
                )
            }

            if (!item.rating.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(7.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xCC060A12))
                        .border(1.dp, Color(0x33F59E0B), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
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
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(sectionColors.accent)
                        .border(2.dp, PureWhite, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFF030712),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(9.dp)
            ) {
                Text(
                    text = item.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite,
                    maxLines = if (epgInfo?.now != null) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp
                )

                epgInfo?.now?.let { nowProgram ->
                    Text(
                        text = "${nowProgram.formattedTimeRange()} • ${nowProgram.title}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = sectionColors.accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    LinearProgressIndicator(
                        progress = { nowProgram.progressFraction() },
                        color = sectionColors.accent,
                        trackColor = Slate800,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp)
                            .clip(CircleShape)
                    )
                }
            }
        }
    }

    if (showContextDialog) {
        val favFocusRequester = remember { FocusRequester() }
        val favBtnInteraction = remember { MutableInteractionSource() }
        val isFavBtnFocused by favBtnInteraction.collectIsFocusedAsState()

        val playBtnInteraction = remember { MutableInteractionSource() }
        val isPlayBtnFocused by playBtnInteraction.collectIsFocusedAsState()

        LaunchedEffect(Unit) {
            try {
                delay(80L)
                favFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }

        val typeLabel = when (item.type) {
            IptvType.LIVE_TV -> if (isAr) "قناة بث مباشر" else "Live Channel"
            IptvType.MOVIE -> if (isAr) "فيلم" else "Movie"
            IptvType.SERIES -> if (isAr) "مسلسل" else "Series"
        }

        AlertDialog(
            onDismissRequest = { showContextDialog = false },
            containerColor = Slate900,
            shape = RoundedCornerShape(20.dp),
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = typeLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = sectionColors.accent
                    )
                    Text(
                        text = item.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = PureWhite,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            onToggleFavorite()
                            showContextDialog = false
                        },
                        interactionSource = favBtnInteraction,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when {
                                isFavBtnFocused -> PureWhite
                                item.isFavorite -> AccentRed
                                else -> sectionColors.accent
                            },
                            contentColor = if (!isFavBtnFocused && item.isFavorite) PureWhite else Slate950
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .focusRequester(favFocusRequester)
                            .border(
                                width = if (isFavBtnFocused) 3.dp else 0.dp,
                                color = PureWhite,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .focusable(interactionSource = favBtnInteraction)
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyUp &&
                                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                ) {
                                    onToggleFavorite()
                                    showContextDialog = false
                                    true
                                } else false
                            }
                    ) {
                        Icon(
                            imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (item.isFavorite) {
                                if (isAr) "إزالة من المفضلة" else "Remove from Favorites"
                            } else {
                                if (isAr) "إضافة إلى المفضلة" else "Add to Favorites"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            showContextDialog = false
                            onClick()
                        },
                        interactionSource = playBtnInteraction,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            width = if (isPlayBtnFocused) 2.5.dp else 1.dp,
                            color = if (isPlayBtnFocused) sectionColors.accent else Slate700
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isPlayBtnFocused) Slate800 else Slate850,
                            contentColor = PureWhite
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .focusable(interactionSource = playBtnInteraction)
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyUp &&
                                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                ) {
                                    showContextDialog = false
                                    onClick()
                                    true
                                } else false
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = sectionColors.accent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (item.type == IptvType.SERIES) {
                                if (isAr) "عرض الحلقات" else "Open Episodes"
                            } else {
                                if (isAr) "تشغيل الآن" else "Play Now"
                            },
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showContextDialog = false }) {
                    Text(
                        text = if (isAr) "إغلاق" else "Close",
                        color = Slate400,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }
}
