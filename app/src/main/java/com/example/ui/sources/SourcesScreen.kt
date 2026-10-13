package com.example.ui.sources

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.IptvSource
import com.example.model.SourceType
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
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Xtream Codes, 1: M3U Playlist, 2: Saved Sources

    // Xtream Codes inputs
    var serverUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var xtreamSourceName by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // M3U inputs
    var m3uUrl by remember { mutableStateOf("") }
    var m3uRawContent by remember { mutableStateOf("") }
    var m3uSourceName by remember { mutableStateOf("") }
    var isRawContentMode by remember { mutableStateOf(false) }

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
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Slate850)
                    .testTag("btn_sources_back")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = PureWhite
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "IPTV Providers & Sources",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite
                )
                Text(
                    text = "Configure Xtream Codes API or M3U playlist",
                    fontSize = 11.sp,
                    color = Slate400
                )
            }
        }

        // Tabs
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
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        "Xtream Codes",
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == 0) AccentCyan else Slate400,
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier.testTag("tab_xtream")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        "M3U Playlist",
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == 1) AccentCyan else Slate400,
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier.testTag("tab_m3u")
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = {
                    Text(
                        "Saved (${sources.size})",
                        fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedTab == 2) AccentCyan else Slate400,
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier.testTag("tab_saved_sources")
            )
        }

        // Status message pill if present
        AnimatedVisibility(visible = !statusMessage.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Slate850)
                    .border(1.dp, Slate700, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = AccentCyan,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = statusMessage ?: "",
                    fontSize = 12.sp,
                    color = if (statusMessage?.contains("Error", true) == true || statusMessage?.contains("failed", true) == true) AccentRed else Slate300
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    // Xtream Codes API form
                    item {
                        Text(
                            text = "Connect with Xtream Codes Credentials",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                        Text(
                            text = "Enter your IPTV provider's server URL, username, and password.",
                            fontSize = 12.sp,
                            color = Slate400,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = serverUrl,
                            onValueChange = { serverUrl = it },
                            label = { Text("Server URL (e.g. http://example.com:8080)") },
                            leadingIcon = { Icon(Icons.Default.Link, null, tint = AccentCyan) },
                            placeholder = { Text("http://provider.domain:8080") },
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
                            label = { Text("Username") },
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
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, null, tint = AccentCyan) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle password visibility",
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
                        OutlinedTextField(
                            value = xtreamSourceName,
                            onValueChange = { xtreamSourceName = it },
                            label = { Text("Playlist Name (Optional)") },
                            placeholder = { Text("My Premium IPTV") },
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
                        Button(
                            onClick = {
                                onConnectXtream(serverUrl, username, password, xtreamSourceName)
                            },
                            enabled = !isLoading && serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("btn_connect_xtream"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = Slate950)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Slate950, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Connecting to Server...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.Sensors, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Connect Xtream Codes", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Demo streams shortcut
                    item {
                        DemoStreamsBanner(onLoadDemo = onLoadDemo)
                    }
                }

                1 -> {
                    // M3U Playlist form
                    item {
                        Text(
                            text = "Add M3U / M3U8 Playlist",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                        Text(
                            text = "Paste a remote playlist URL (.m3u/.m3u8) or paste raw playlist text.",
                            fontSize = 12.sp,
                            color = Slate400,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )
                    }

                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = { isRawContentMode = false },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (!isRawContentMode) Slate800 else Color.Transparent
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (!isRawContentMode) AccentCyan else Slate700),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("URL Link", color = PureWhite)
                            }
                            OutlinedButton(
                                onClick = { isRawContentMode = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isRawContentMode) Slate800 else Color.Transparent
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isRawContentMode) AccentCyan else Slate700),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Paste Text", color = PureWhite)
                            }
                        }
                    }

                    if (!isRawContentMode) {
                        item {
                            OutlinedTextField(
                                value = m3uUrl,
                                onValueChange = { m3uUrl = it },
                                label = { Text("M3U Playlist URL") },
                                leadingIcon = { Icon(Icons.Default.Link, null, tint = AccentCyan) },
                                placeholder = { Text("https://example.com/playlist.m3u") },
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
                    } else {
                        item {
                            OutlinedTextField(
                                value = m3uRawContent,
                                onValueChange = { m3uRawContent = it },
                                label = { Text("Raw M3U Content (#EXTM3U ...)") },
                                placeholder = { Text("#EXTM3U\n#EXTINF:-1 tvg-name=\"Channel 1\",Channel 1\nhttp://...") },
                                minLines = 5,
                                maxLines = 8,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_m3u_raw"),
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
                    }

                    item {
                        OutlinedTextField(
                            value = m3uSourceName,
                            onValueChange = { m3uSourceName = it },
                            label = { Text("Playlist Name (Optional)") },
                            placeholder = { Text("Sports & News M3U") },
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
                        Button(
                            onClick = {
                                onLoadM3u(m3uUrl, m3uRawContent, m3uSourceName)
                            },
                            enabled = !isLoading && (m3uUrl.isNotBlank() || m3uRawContent.isNotBlank()),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("btn_load_m3u"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan, contentColor = Slate950)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Slate950, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Parsing Playlist...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.Storage, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Load M3U Playlist", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    item {
                        DemoStreamsBanner(onLoadDemo = onLoadDemo)
                    }
                }

                2 -> {
                    // Saved Sources List
                    item {
                        Text(
                            text = "Saved IPTV Providers",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PureWhite
                        )
                        Text(
                            text = "Select an IPTV provider to activate or remove old sources.",
                            fontSize = 12.sp,
                            color = Slate400,
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                        )
                    }

                    items(sources, key = { it.id }) { source ->
                        val isActive = source.id == activeSource?.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    width = if (isActive) 2.dp else 1.dp,
                                    color = if (isActive) AccentEmerald else Slate800,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { onSwitchSource(source) }
                                .testTag("source_item_${source.id}"),
                            colors = CardDefaults.cardColors(containerColor = Slate900),
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
                                                    text = "ACTIVE",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = AccentEmerald
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = when (source.type) {
                                            SourceType.XTREAM_CODES -> "Xtream Codes API: ${source.serverUrl}"
                                            SourceType.M3U_URL -> "M3U Link: ${source.m3uUrl.take(30)}..."
                                            SourceType.M3U_TEXT -> "Raw M3U Playlist"
                                            SourceType.DEMO -> "Bundled Verified Public Streams"
                                        },
                                        fontSize = 11.sp,
                                        color = Slate400,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )

                                    Text(
                                        text = "${source.channelCount} Live TV  •  ${source.movieCount} Movies  •  ${source.seriesCount} Series",
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
                                                contentDescription = "Delete",
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
                                            Text("Activate", fontSize = 12.sp)
                                        }
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Active",
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
                }
            }
        }
    }
}

@Composable
fun DemoStreamsBanner(onLoadDemo: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

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
                    text = "Test With Demo Streams",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = PureWhite
                )
                Text(
                    text = "Instant access to NASA TV HD, Euronews, Red Bull Sports, VOD movies & episodic series.",
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
                        if (event.type == androidx.compose.ui.input.key.KeyEventType.KeyUp &&
                            (event.key == androidx.compose.ui.input.key.Key.DirectionCenter ||
                             event.key == androidx.compose.ui.input.key.Key.Enter ||
                             event.key == androidx.compose.ui.input.key.Key.NumPadEnter)
                        ) {
                            onLoadDemo()
                            true
                        } else false
                    }
                    .testTag("btn_load_demo_showcase")
            ) {
                Icon(Icons.Default.PlayCircleOutline, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Load Demo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
