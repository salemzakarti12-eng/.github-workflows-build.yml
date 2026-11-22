package com.example.ui.epg

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.CategoryInfo
import com.example.model.IptvItem
import com.example.ui.locale.LocalAppLanguage
import com.example.ui.locale.LocalAppStrings
import com.example.ui.player.EpgManager
import com.example.ui.player.EpgProgram
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val DP_PER_MINUTE = 3.2.dp
private val CHANNEL_COLUMN_WIDTH = 168.dp
private val ROW_HEIGHT = 64.dp

@Composable
fun EpgGuideScreen(
    channels: List<IptvItem>,
    categories: List<CategoryInfo>,
    onPlayChannel: (IptvItem) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val currentLanguage = LocalAppLanguage.current
    val isAr = currentLanguage == "ar"
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Update current time every 30 seconds so the red live indicator advances smoothly
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            nowMs = System.currentTimeMillis()
        }
    }

    // Timeline window: from 2 hours ago (rounded to hour) up to +14 hours
    val timelineStartMs = remember {
        Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, -2)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val totalTimelineMinutes = 16 * 60 // 16 hours total
    val timelineEndMs = timelineStartMs + (totalTimelineMinutes * 60_000L)
    val totalTimelineWidth: Dp = DP_PER_MINUTE * totalTimelineMinutes

    val timelineScrollState = rememberScrollState()

    val jumpToNow: suspend () -> Unit = {
        val minutesFromStart = ((System.currentTimeMillis() - timelineStartMs) / 60_000f)
            .coerceIn(0f, totalTimelineMinutes.toFloat())
        val targetDp = (DP_PER_MINUTE * minutesFromStart) - 80.dp
        val targetPx = with(density) { targetDp.coerceAtLeast(0.dp).toPx().toInt() }
        timelineScrollState.animateScrollTo(targetPx)
    }

    LaunchedEffect(Unit) {
        delay(120L)
        jumpToNow()
    }

    val filteredChannels = remember(channels, selectedCategory, searchQuery) {
        channels.filter { item ->
            val matchesCat = when {
                selectedCategory.equals("All", true) -> true
                selectedCategory.equals("Favorites", true) -> item.isFavorite
                else -> item.group == selectedCategory
            }
            val matchesQuery = searchQuery.isBlank() ||
                item.name.contains(searchQuery, ignoreCase = true)
            matchesCat && matchesQuery
        }.take(250)
    }

    var focusedChannel by remember { mutableStateOf<IptvItem?>(null) }
    var focusedProgram by remember { mutableStateOf<EpgProgram?>(null) }

    LaunchedEffect(filteredChannels) {
        if (focusedChannel == null && filteredChannels.isNotEmpty()) {
            val first = filteredChannels.first()
            focusedChannel = first
            focusedProgram = EpgManager.getNowAndNext(first.epgChannelId).now
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        // 1. Top Bar: Back, Title, Search, Jump to Now
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
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
                    contentDescription = strings.back,
                    tint = PureWhite
                )
            }

            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                tint = AccentCyan,
                modifier = Modifier.size(22.dp)
            )

            Text(
                text = if (isAr) "دليل البرامج (EPG)" else "TV Guide (EPG)",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = PureWhite
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                singleLine = true,
                placeholder = {
                    Text(
                        text = if (isAr) "ابحث عن قناة..." else "Filter channels...",
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
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
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

            Button(
                onClick = { coroutineScope.launch { jumpToNow() } },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentCyan,
                    contentColor = Slate950
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isAr) "الآن" else "Now",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // 2. Selected Program Details Banner
        SelectedProgramBanner(
            channel = focusedChannel,
            program = focusedProgram,
            nowMs = nowMs,
            isAr = isAr,
            onWatchChannel = { ch -> onPlayChannel(ch) }
        )

        // 3. Category Filter Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "epg_cat_all") {
                EpgCategoryChip(
                    label = "${strings.all} (${channels.size})",
                    isSelected = selectedCategory == "All",
                    onClick = { selectedCategory = "All" }
                )
            }
            item(key = "epg_cat_fav") {
                EpgCategoryChip(
                    label = strings.favorites,
                    isSelected = selectedCategory == "Favorites",
                    onClick = { selectedCategory = "Favorites" }
                )
            }
            items(categories, key = { it.id }) { cat ->
                val translated = remember(cat.name, currentLanguage) {
                    CategoryTranslator.translateForLanguage(cat.name, currentLanguage)
                }
                EpgCategoryChip(
                    label = translated,
                    isSelected = selectedCategory == cat.name,
                    onClick = { selectedCategory = cat.name }
                )
            }
        }

        // 4. Synchronized Timeline Header + Channels EPG Grid
        if (filteredChannels.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = if (isAr) "لا توجد قنوات مطابقة" else "No matching channels found",
                    color = Slate400,
                    fontSize = 15.sp
                )
            }
        } else {
            // Keep chronological time axis left-to-right so horizontal scrolling math is exact
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Time Ruler Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .background(Slate900)
                            .border(BorderStroke(1.dp, Slate800))
                    ) {
                        Box(
                            contentAlignment = Alignment.CenterStart,
                            modifier = Modifier
                                .width(CHANNEL_COLUMN_WIDTH)
                                .fillMaxHeight()
                                .padding(horizontal = 12.dp)
                                .border(BorderStroke(1.dp, Slate800))
                        ) {
                            Text(
                                text = if (isAr) "القنوات (${filteredChannels.size})" else "Channels (${filteredChannels.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentCyan
                            )
                        }

                        TimeRulerHeader(
                            timelineStartMs = timelineStartMs,
                            totalMinutes = totalTimelineMinutes,
                            totalWidth = totalTimelineWidth,
                            nowMs = nowMs,
                            scrollState = timelineScrollState
                        )
                    }

                    // Channels + Programs Rows
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        itemsIndexed(
                            items = filteredChannels,
                            key = { idx, item -> "epg_${item.id}_$idx" }
                        ) { index, channel ->
                            EpgChannelRow(
                                index = index + 1,
                                channel = channel,
                                timelineStartMs = timelineStartMs,
                                timelineEndMs = timelineEndMs,
                                totalTimelineWidth = totalTimelineWidth,
                                nowMs = nowMs,
                                isAr = isAr,
                                scrollState = timelineScrollState,
                                onProgramFocused = { prog ->
                                    focusedChannel = channel
                                    focusedProgram = prog
                                },
                                onSelectChannel = { onPlayChannel(channel) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedProgramBanner(
    channel: IptvItem?,
    program: EpgProgram?,
    nowMs: Long,
    isAr: Boolean,
    onWatchChannel: (IptvItem) -> Unit
) {
    if (channel == null) return

    Surface(
        color = Slate900,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = channel.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = AccentCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (program != null) {
                        Text(
                            text = "• ${program.formattedTimeRange()}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate300
                        )
                        if (program.isLiveAt(nowMs)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(AccentRed)
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = if (isAr) "مباشر" else "LIVE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = PureWhite
                                )
                            }
                        }
                    }
                }

                Text(
                    text = program?.title ?: (if (isAr) "لا يوجد وصف متاح للبرنامج الحالي" else "No program information available"),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!program?.description.isNullOrBlank()) {
                    Text(
                        text = program!!.description,
                        fontSize = 11.sp,
                        color = Slate400,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (program != null && program.isLiveAt(nowMs)) {
                    LinearProgressIndicator(
                        progress = { program.progressFraction(nowMs) },
                        color = AccentCyan,
                        trackColor = Slate800,
                        modifier = Modifier
                            .fillMaxWidth(0.65f)
                            .height(3.dp)
                            .clip(CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Button(
                onClick = { onWatchChannel(channel) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Slate850,
                    contentColor = AccentCyan
                ),
                border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.6f)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isAr) "مشاهدة" else "Watch",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TimeRulerHeader(
    timelineStartMs: Long,
    totalMinutes: Int,
    totalWidth: Dp,
    nowMs: Long,
    scrollState: ScrollState
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.US) }
    val slotCount = totalMinutes / 30 // 30-minute ticks
    val slotWidth = DP_PER_MINUTE * 30

    val nowOffsetMinutes = ((nowMs - timelineStartMs) / 60_000f).coerceIn(0f, totalMinutes.toFloat())
    val nowOffsetDp = DP_PER_MINUTE * nowOffsetMinutes

    Box(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(scrollState)
    ) {
        Row(modifier = Modifier.width(totalWidth).fillMaxHeight()) {
            for (slot in 0 until slotCount) {
                val slotTimeMs = timelineStartMs + (slot * 30 * 60_000L)
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        .width(slotWidth)
                        .fillMaxHeight()
                        .border(BorderStroke(0.5.dp, Slate800))
                        .padding(start = 8.dp)
                ) {
                    Text(
                        text = timeFormat.format(slotTimeMs),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate300
                    )
                }
            }
        }

        // Current Time Red Marker on Ruler
        Box(
            modifier = Modifier
                .offset(x = nowOffsetDp)
                .width(2.dp)
                .fillMaxHeight()
                .background(AccentRed)
        )
    }
}

@Composable
private fun EpgChannelRow(
    index: Int,
    channel: IptvItem,
    timelineStartMs: Long,
    timelineEndMs: Long,
    totalTimelineWidth: Dp,
    nowMs: Long,
    isAr: Boolean,
    scrollState: ScrollState,
    onProgramFocused: (EpgProgram?) -> Unit,
    onSelectChannel: () -> Unit
) {
    val programs = remember(channel.epgChannelId, timelineStartMs, timelineEndMs) {
        EpgManager.getChannelSchedule(channel.epgChannelId).filter { prog ->
            prog.endTimeMs > timelineStartMs && prog.startTimeMs < timelineEndMs
        }
    }

    val channelInteraction = remember { MutableInteractionSource() }
    val isChannelFocused by channelInteraction.collectIsFocusedAsState()

    val nowOffsetMinutes = ((nowMs - timelineStartMs) / 60_000f)
        .coerceIn(0f, ((timelineEndMs - timelineStartMs) / 60_000f))
    val nowOffsetDp = DP_PER_MINUTE * nowOffsetMinutes

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT)
            .border(BorderStroke(0.5.dp, Slate800))
    ) {
        // Fixed Channel Info Cell
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .width(CHANNEL_COLUMN_WIDTH)
                .fillMaxHeight()
                .background(if (isChannelFocused) AccentCyan.copy(alpha = 0.22f) else Slate900)
                .border(
                    width = if (isChannelFocused) 2.dp else 0.5.dp,
                    color = if (isChannelFocused) AccentCyan else Slate800
                )
                .onFocusChanged { if (it.isFocused) onProgramFocused(programs.firstOrNull { p -> p.isLiveAt(nowMs) }) }
                .focusable(interactionSource = channelInteraction)
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyUp &&
                        (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                    ) {
                        onSelectChannel()
                        true
                    } else false
                }
                .clickable(interactionSource = channelInteraction, indication = null, onClick = onSelectChannel)
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = "$index",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Slate400
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Slate850)
            ) {
                if (!channel.logoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = channel.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(3.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.LiveTv,
                        contentDescription = null,
                        tint = Slate400,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = channel.name,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isChannelFocused) AccentCyan else PureWhite,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp,
                modifier = Modifier.weight(1f)
            )
        }

        // Scrollable Programs Timeline Strip
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .horizontalScroll(scrollState)
        ) {
            Box(
                modifier = Modifier
                    .width(totalTimelineWidth)
                    .fillMaxHeight()
                    .background(Slate950)
            ) {
                if (programs.isEmpty()) {
                    // Fallback clickable cell when channel has no EPG entries
                    val emptyInteraction = remember { MutableInteractionSource() }
                    val isEmptyFocused by emptyInteraction.collectIsFocusedAsState()

                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(if (isEmptyFocused) Slate850 else Slate950)
                            .border(
                                width = if (isEmptyFocused) 2.dp else 0.5.dp,
                                color = if (isEmptyFocused) AccentCyan else Slate800
                            )
                            .onFocusChanged { if (it.isFocused) onProgramFocused(null) }
                            .focusable(interactionSource = emptyInteraction)
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyUp &&
                                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                ) {
                                    onSelectChannel()
                                    true
                                } else false
                            }
                            .clickable(interactionSource = emptyInteraction, indication = null, onClick = onSelectChannel)
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = if (isAr) "لا تتوفر بيانات جدول البرامج • اضغط للتشغيل المباشر" else "No EPG data available • Select to watch live",
                            fontSize = 11.5.sp,
                            color = if (isEmptyFocused) AccentCyan else Slate400
                        )
                    }
                } else {
                    programs.forEach { prog ->
                        val clampedStart = prog.startTimeMs.coerceAtLeast(timelineStartMs)
                        val clampedEnd = prog.endTimeMs.coerceAtMost(timelineEndMs)
                        val offsetMinutes = ((clampedStart - timelineStartMs) / 60_000f).coerceAtLeast(0f)
                        val durationMinutes = ((clampedEnd - clampedStart) / 60_000f).coerceAtLeast(5f)

                        val startOffsetDp = DP_PER_MINUTE * offsetMinutes
                        val blockWidthDp = DP_PER_MINUTE * durationMinutes
                        val isLiveNow = prog.isLiveAt(nowMs)

                        val progInteraction = remember { MutableInteractionSource() }
                        val isProgFocused by progInteraction.collectIsFocusedAsState()

                        Box(
                            modifier = Modifier
                                .offset(x = startOffsetDp)
                                .width(blockWidthDp)
                                .fillMaxHeight()
                                .padding(1.5.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        isProgFocused -> AccentCyan.copy(alpha = 0.28f)
                                        isLiveNow -> Color(0xFF0E2942)
                                        else -> Slate900
                                    }
                                )
                                .border(
                                    width = if (isProgFocused) 2.dp else 1.dp,
                                    color = when {
                                        isProgFocused -> PureWhite
                                        isLiveNow -> AccentCyan.copy(alpha = 0.55f)
                                        else -> Slate800
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .onFocusChanged { if (it.isFocused) onProgramFocused(prog) }
                                .focusable(interactionSource = progInteraction)
                                .onPreviewKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyUp &&
                                        (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                    ) {
                                        onSelectChannel()
                                        true
                                    } else false
                                }
                                .clickable(interactionSource = progInteraction, indication = null, onClick = onSelectChannel)
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = prog.title,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isLiveNow || isProgFocused) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isLiveNow || isProgFocused) PureWhite else Slate300,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = prog.formattedTimeRange(),
                                    fontSize = 10.sp,
                                    color = if (isLiveNow) AccentCyan else Slate400,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Vertical Red Current-Time Line across row
                Box(
                    modifier = Modifier
                        .offset(x = nowOffsetDp)
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(AccentRed.copy(alpha = 0.85f))
                )
            }
        }
    }
}

@Composable
private fun EpgCategoryChip(
    label: String,
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
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Slate950 else Slate300,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}
