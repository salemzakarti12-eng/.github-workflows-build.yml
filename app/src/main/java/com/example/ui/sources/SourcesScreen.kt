package com.example.ui.sources

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.IptvSource
import com.example.model.SourceType
import com.example.parser.XtreamUrlDetector
import com.example.ui.components.LanguageToggleButton
import com.example.ui.locale.LocalAppLanguage
import com.example.ui.locale.LocalAppStrings
import com.example.ui.locale.LocalMiniPlayerActive
import com.example.ui.locale.isRtlLanguage
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentEmerald
import com.example.ui.theme.AccentRed
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import java.util.Locale

@Composable
fun SourcesScreen(
    sources: List<IptvSource>,
    activeSource: IptvSource?,
    isLoading: Boolean,
    statusMessage: String?,
    onConnectXtream: (server: String, user: String, pass: String, name: String) -> Unit,
    onLoadM3u: (url: String, content: String, name: String) -> Unit,
    onLoadDemo: () -> Unit,
    onSwitchSource: (IptvSource) -> Unit,
    onDeleteSource: (String) -> Unit,
    onBack: () -> Unit,
    onToggleLanguage: () -> Unit = {},
    onSelectLanguage: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val currentLanguage = LocalAppLanguage.current
    val isAr = currentLanguage == "ar"
    val isRtl = isRtlLanguage(currentLanguage)

    // Dual Login Tabs: 0 = M3U URL, 1 = Xtream Codes API, 2 = Saved Providers
    var selectedTab by remember { mutableIntStateOf(0) }

    // M3U inputs
    var m3uSourceName by remember { mutableStateOf("") }
    var m3uUrl by remember { mutableStateOf("") }
    var m3uRawContent by remember { mutableStateOf("") }

    // Xtream Codes inputs
    var xtreamSourceName by remember { mutableStateOf("") }
    var serverUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Auto-detect Xtream in M3U link
    val detectedXtream = remember(m3uUrl) {
        XtreamUrlDetector.detect(m3uUrl)
    }

    val isErrorStatus = statusMessage?.contains("Error", true) == true ||
            statusMessage?.contains("failed", true) == true ||
            statusMessage?.contains("فشل", true) == true ||
            statusMessage?.contains("خطأ", true) == true

    val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Slate950)
                .testTag("sources_screen")
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val backInteraction = remember { MutableInteractionSource() }
                val isBackFocused by backInteraction.collectIsFocusedAsState()

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
                        .testTag("btn_sources_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.back,
                        tint = if (isBackFocused) Slate950 else PureWhite
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.sourcesAndProviders,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = strings.managePlaylists,
                        fontSize = 11.sp,
                        color = Slate400,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))
                LanguageToggleButton(
                    onToggleLanguage = onToggleLanguage,
                    onSelectLanguage = onSelectLanguage
                )
            }

            // Dual Login Tabs (M3U URL | Xtream Codes API | Saved Sources)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Slate900,
                contentColor = AccentCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = AccentCyan,
                        height = 3.dp
                    )
                }
            ) {
                val tab0Interaction = remember { MutableInteractionSource() }
                val isTab0Focused by tab0Interaction.collectIsFocusedAsState()

                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    interactionSource = tab0Interaction,
                    text = {
                        Text(
                            text = strings.m3uTab,
                            fontWeight = if (selectedTab == 0 || isTab0Focused) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0 || isTab0Focused) AccentCyan else Slate400,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier
                        .focusable(interactionSource = tab0Interaction)
                        .testTag("tab_m3u")
                )

                val tab1Interaction = remember { MutableInteractionSource() }
                val isTab1Focused by tab1Interaction.collectIsFocusedAsState()

                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    interactionSource = tab1Interaction,
                    text = {
                        Text(
                            text = strings.xtreamTab,
                            fontWeight = if (selectedTab == 1 || isTab1Focused) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1 || isTab1Focused) AccentCyan else Slate400,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier
                        .focusable(interactionSource = tab1Interaction)
                        .testTag("tab_xtream")
                )

                val tab2Interaction = remember { MutableInteractionSource() }
                val isTab2Focused by tab2Interaction.collectIsFocusedAsState()

                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    interactionSource = tab2Interaction,
                    text = {
                        Text(
                            text = "${strings.savedTab} (${sources.size})",
                            fontWeight = if (selectedTab == 2 || isTab2Focused) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 2 || isTab2Focused) AccentCyan else Slate400,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier
                        .focusable(interactionSource = tab2Interaction)
                        .testTag("tab_saved_sources")
                )
            }

            // Live Progress Indicator Card with Live Counter
            AnimatedVisibility(visible = isLoading || !statusMessage.isNullOrBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate850),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isLoading) AccentCyan else if (isErrorStatus) AccentRed else Slate700
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = AccentCyan,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Text(
                            text = statusMessage ?: strings.parsingStreams,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isErrorStatus) AccentRed else PureWhite,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            val isMiniPlayerActive = LocalMiniPlayerActive.current

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = if (isMiniPlayerActive) 140.dp else 48.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // TAB 0: M3U URL LOGIN
                        item {
                            Text(
                                text = strings.addM3uTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                            Text(
                                text = strings.addM3uSubtitle,
                                fontSize = 12.sp,
                                color = Slate400,
                                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = m3uSourceName,
                                onValueChange = { m3uSourceName = it },
                                label = { Text(strings.playlistName) },
                                placeholder = { Text("My IPTV Playlist") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_m3u_name"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = PureWhite,
                                    unfocusedTextColor = PureWhite,
                                    focusedBorderColor = AccentCyan,
                                    unfocusedBorderColor = Slate700,
                                    focusedContainerColor = Slate900,
                                    unfocusedContainerColor = Slate900
                                )
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = m3uUrl,
                                onValueChange = { m3uUrl = it },
                                label = { Text(strings.m3uUrlLabel) },
                                leadingIcon = { Icon(Icons.Default.Link, null, tint = AccentCyan) },
                                placeholder = { Text(strings.m3uUrlPlaceholder) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_m3u_url"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = PureWhite,
                                    unfocusedTextColor = PureWhite,
                                    focusedBorderColor = AccentCyan,
                                    unfocusedBorderColor = Slate700,
                                    focusedContainerColor = Slate900,
                                    unfocusedContainerColor = Slate900
                                )
                            )
                        }

                        // Smart M3U-to-Xtream Auto-Detection Banner
                        if (detectedXtream != null) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.5.dp, AccentEmerald, RoundedCornerShape(12.dp)),
                                    colors = CardDefaults.cardColors(containerColor = Slate900)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = AccentEmerald,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = strings.autoDetectTitle,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = AccentEmerald
                                            )
                                            Text(
                                                text = "${strings.autoDetectDesc} (${detectedXtream.username})",
                                                fontSize = 11.sp,
                                                color = Slate300
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            val m3uBtnInteraction = remember { MutableInteractionSource() }
                            val isM3uBtnFocused by m3uBtnInteraction.collectIsFocusedAsState()

                            Button(
                                onClick = {
                                    onLoadM3u(m3uUrl.trim(), m3uRawContent, m3uSourceName.trim())
                                },
                                interactionSource = m3uBtnInteraction,
                                enabled = !isLoading && (m3uUrl.isNotBlank() || m3uRawContent.isNotBlank()),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .border(
                                        width = if (isM3uBtnFocused) 3.dp else 0.dp,
                                        color = PureWhite,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .focusable(interactionSource = m3uBtnInteraction)
                                    .onPreviewKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyUp &&
                                            (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                        ) {
                                            onLoadM3u(m3uUrl.trim(), m3uRawContent, m3uSourceName.trim())
                                            true
                                        } else false
                                    }
                                    .testTag("btn_load_m3u"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isM3uBtnFocused) PureWhite else AccentCyan,
                                    contentColor = Slate950
                                )
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(color = Slate950, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(strings.parsingStreams, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Default.Storage, null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(strings.connectAndLoad, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        item {
                            DemoStreamsBanner(onLoadDemo = onLoadDemo)
                        }
                    }

                    1 -> {
                        // TAB 1: XTREAM CODES API LOGIN
                        item {
                            Text(
                                text = strings.xtreamLoginTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                            Text(
                                text = strings.xtreamLoginSubtitle,
                                fontSize = 12.sp,
                                color = Slate400,
                                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = xtreamSourceName,
                                onValueChange = { xtreamSourceName = it },
                                label = { Text(strings.playlistName) },
                                placeholder = { Text("Premium Xtream Provider") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_xtream_name"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = PureWhite,
                                    unfocusedTextColor = PureWhite,
                                    focusedBorderColor = AccentCyan,
                                    unfocusedBorderColor = Slate700,
                                    focusedContainerColor = Slate900,
                                    unfocusedContainerColor = Slate900
                                )
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = serverUrl,
                                onValueChange = { serverUrl = it },
                                label = { Text(strings.serverUrl) },
                                leadingIcon = { Icon(Icons.Default.Link, null, tint = AccentCyan) },
                                placeholder = { Text("http://server.com:8080") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_xtream_server"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = PureWhite,
                                    unfocusedTextColor = PureWhite,
                                    focusedBorderColor = AccentCyan,
                                    unfocusedBorderColor = Slate700,
                                    focusedContainerColor = Slate900,
                                    unfocusedContainerColor = Slate900
                                )
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text(strings.username) },
                                leadingIcon = { Icon(Icons.Default.Person, null, tint = AccentCyan) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_xtream_username"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = PureWhite,
                                    unfocusedTextColor = PureWhite,
                                    focusedBorderColor = AccentCyan,
                                    unfocusedBorderColor = Slate700,
                                    focusedContainerColor = Slate900,
                                    unfocusedContainerColor = Slate900
                                )
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text(strings.password) },
                                leadingIcon = { Icon(Icons.Default.Lock, null, tint = AccentCyan) },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Toggle password",
                                            tint = Slate400
                                        )
                                    }
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_xtream_password"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = PureWhite,
                                    unfocusedTextColor = PureWhite,
                                    focusedBorderColor = AccentCyan,
                                    unfocusedBorderColor = Slate700,
                                    focusedContainerColor = Slate900,
                                    unfocusedContainerColor = Slate900
                                )
                            )
                        }

                        item {
                            val xtreamBtnInteraction = remember { MutableInteractionSource() }
                            val isXtreamBtnFocused by xtreamBtnInteraction.collectIsFocusedAsState()

                            Button(
                                onClick = {
                                    onConnectXtream(serverUrl.trim(), username.trim(), password.trim(), xtreamSourceName.trim())
                                },
                                interactionSource = xtreamBtnInteraction,
                                enabled = !isLoading && serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .border(
                                        width = if (isXtreamBtnFocused) 3.dp else 0.dp,
                                        color = PureWhite,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .focusable(interactionSource = xtreamBtnInteraction)
                                    .onPreviewKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyUp &&
                                            (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                        ) {
                                            onConnectXtream(serverUrl.trim(), username.trim(), password.trim(), xtreamSourceName.trim())
                                            true
                                        } else false
                                    }
                                    .testTag("btn_connect_xtream"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isXtreamBtnFocused) PureWhite else AccentCyan,
                                    contentColor = Slate950
                                )
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(color = Slate950, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(strings.parsingStreams, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Default.Sensors, null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(strings.connectXtreamButton, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        item {
                            DemoStreamsBanner(onLoadDemo = onLoadDemo)
                        }
                    }

                    2 -> {
                        // TAB 2: SAVED SOURCES
                        item {
                            Text(
                                text = strings.savedTab,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                            Text(
                                text = strings.managePlaylists,
                                fontSize = 12.sp,
                                color = Slate400,
                                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                            )
                        }

                        items(sources, key = { it.id }) { source ->
                            val isActive = source.id == activeSource?.id
                            val sourceCardInteraction = remember { MutableInteractionSource() }
                            val isSourceCardFocused by sourceCardInteraction.collectIsFocusedAsState()

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(
                                        width = if (isSourceCardFocused) 3.dp else if (isActive) 2.dp else 1.dp,
                                        color = if (isSourceCardFocused) PureWhite else if (isActive) AccentEmerald else Slate800,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .focusable(interactionSource = sourceCardInteraction)
                                    .onPreviewKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyUp &&
                                            (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                        ) {
                                            onSwitchSource(source)
                                            true
                                        } else false
                                    }
                                    .clickable(interactionSource = sourceCardInteraction, indication = null) {
                                        onSwitchSource(source)
                                    }
                                    .testTag("source_item_${source.id}"),
                                colors = CardDefaults.cardColors(containerColor = if (isSourceCardFocused) Slate850 else Slate900),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = source.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = PureWhite
                                            )
                                            if (isActive) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Color(0x3310B981))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = strings.active.uppercase(Locale.US),
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = AccentEmerald
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = when (source.type) {
                                                SourceType.XTREAM_CODES -> "Xtream API: ${source.serverUrl}"
                                                SourceType.M3U_URL -> "M3U: ${source.m3uUrl.take(30)}..."
                                                SourceType.M3U_TEXT -> if (isAr) "ملف M3U نصي" else "Raw M3U Playlist"
                                                SourceType.DEMO -> if (isAr) "قنوات تجريبية مجانية" else "Bundled Verified Public Streams"
                                            },
                                            fontSize = 11.sp,
                                            color = Slate400,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )

                                        Text(
                                            text = "%,d ${strings.liveTv}  •  %,d ${strings.movies}  •  %,d ${strings.series}".format(
                                                Locale.US, source.channelCount, source.movieCount, source.seriesCount
                                            ),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = AccentCyan,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (source.type != SourceType.DEMO) {
                                            IconButton(
                                                onClick = { onDeleteSource(source.id) },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = strings.delete,
                                                    tint = Slate400,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        if (!isActive) {
                                            Button(
                                                onClick = { onSwitchSource(source) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Slate800, contentColor = PureWhite),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.padding(start = 4.dp)
                                            ) {
                                                Text(strings.activate, fontSize = 12.sp)
                                            }
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = strings.active,
                                                tint = AccentEmerald,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            DemoStreamsBanner(onLoadDemo = onLoadDemo)
                        }

                        item {
                            Spacer(modifier = Modifier.height(70.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DemoStreamsBanner(onLoadDemo: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val strings = LocalAppStrings.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isFocused) 3.dp else 1.dp,
                color = if (isFocused) AccentEmerald else Slate700,
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("demo_streams_banner"),
        colors = CardDefaults.cardColors(containerColor = Slate850)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.exploreDemoTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = PureWhite
                )
                Text(
                    text = strings.exploreDemoDesc,
                    fontSize = 11.sp,
                    color = Slate300,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onLoadDemo,
                interactionSource = interactionSource,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFocused) PureWhite else AccentEmerald,
                    contentColor = Slate950
                ),
                modifier = Modifier
                    .focusable(interactionSource = interactionSource)
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyUp &&
                            (event.key == Key.DirectionCenter ||
                             event.key == Key.Enter ||
                             event.key == Key.NumPadEnter)
                        ) {
                            onLoadDemo()
                            true
                        } else false
                    }
                    .testTag("btn_load_demo_showcase")
            ) {
                Icon(Icons.Default.PlayCircleOutline, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(strings.testDriveButton, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
