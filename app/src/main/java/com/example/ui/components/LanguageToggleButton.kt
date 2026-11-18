package com.example.ui.components

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.locale.AppLanguageManager
import com.example.ui.locale.LocalAppLanguage
import com.example.ui.locale.LocalAppStrings
import com.example.ui.locale.SupportedLanguage
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import kotlinx.coroutines.delay

@Composable
fun LanguageToggleButton(
    onToggleLanguage: () -> Unit = {},
    onSelectLanguage: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val currentLanguage = LocalAppLanguage.current
    val strings = LocalAppStrings.current

    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    var showLanguageDialog by remember { mutableStateOf(false) }

    val activeNativeName = remember(currentLanguage) {
        AppLanguageManager.supportedLanguages
            .firstOrNull { it.code.equals(currentLanguage, ignoreCase = true) }
            ?.nativeName ?: "English"
    }

    val handleButtonAction = {
        if (onSelectLanguage != null) {
            showLanguageDialog = true
        } else {
            onToggleLanguage()
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (isFocused) AccentCyan else Slate850)
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = if (isFocused) PureWhite else Slate700,
                shape = RoundedCornerShape(18.dp)
            )
            .focusable(interactionSource = interactionSource)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                ) {
                    handleButtonAction()
                    true
                } else false
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = handleButtonAction)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("btn_language_toggle"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = strings.switchLanguage,
                tint = if (isFocused) Slate900 else AccentCyan,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "🌐 $activeNativeName",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isFocused) Slate900 else PureWhite
            )
        }
    }

    if (showLanguageDialog && onSelectLanguage != null) {
        val selectedFocusRequester = remember { FocusRequester() }
        val languageRows = remember { AppLanguageManager.supportedLanguages.chunked(2) }

        LaunchedEffect(Unit) {
            try {
                delay(80L)
                selectedFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }

        Dialog(
            onDismissRequest = { showLanguageDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                color = Slate900,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Slate700),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 460.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = strings.switchLanguage,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                        }

                        IconButton(
                            onClick = { showLanguageDialog = false },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = strings.close,
                                tint = Slate400,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    languageRows.forEach { rowItems ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowItems.forEach { lang ->
                                val isSelected = lang.code.equals(currentLanguage, ignoreCase = true)
                                CompactLanguageGridItem(
                                    lang = lang,
                                    isSelected = isSelected,
                                    focusRequester = if (isSelected) selectedFocusRequester else null,
                                    onSelect = {
                                        onSelectLanguage(lang.code)
                                        showLanguageDialog = false
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactLanguageGridItem(
    lang: SupportedLanguage,
    isSelected: Boolean,
    focusRequester: FocusRequester?,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val itemInteraction = remember { MutableInteractionSource() }
    val isItemFocused by itemInteraction.collectIsFocusedAsState()

    val focusModifier = if (focusRequester != null) {
        Modifier.focusRequester(focusRequester)
    } else {
        Modifier
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .then(focusModifier)
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    isItemFocused -> AccentCyan
                    isSelected -> AccentCyan.copy(alpha = 0.16f)
                    else -> Slate850
                }
            )
            .border(
                width = if (isItemFocused) 2.dp else if (isSelected) 1.5.dp else 1.dp,
                color = when {
                    isItemFocused -> PureWhite
                    isSelected -> AccentCyan
                    else -> Slate800
                },
                shape = RoundedCornerShape(10.dp)
            )
            .focusable(interactionSource = itemInteraction)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    (event.key == Key.DirectionCenter ||
                     event.key == Key.Enter ||
                     event.key == Key.NumPadEnter)
                ) {
                    onSelect()
                    true
                } else false
            }
            .clickable(
                interactionSource = itemInteraction,
                indication = null,
                onClick = onSelect
            )
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = lang.nativeName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isItemFocused) Slate950 else PureWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = lang.englishName,
                fontSize = 10.sp,
                color = if (isItemFocused) Slate900 else Slate400,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (isSelected) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = if (isItemFocused) Slate950 else AccentCyan,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
