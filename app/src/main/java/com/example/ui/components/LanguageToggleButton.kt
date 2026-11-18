package com.example.ui.components

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.locale.AppLanguageManager
import com.example.ui.locale.LocalAppLanguage
import com.example.ui.locale.LocalAppStrings
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

        LaunchedEffect(Unit) {
            try {
                delay(80L)
                selectedFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }

        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            containerColor = Slate900,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = AccentCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = strings.switchLanguage,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                }
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                ) {
                    items(AppLanguageManager.supportedLanguages, key = { it.code }) { lang ->
                        val isSelected = lang.code.equals(currentLanguage, ignoreCase = true)
                        val itemInteraction = remember { MutableInteractionSource() }
                        val isItemFocused by itemInteraction.collectIsFocusedAsState()

                        val focusModifier = if (isSelected) {
                            Modifier.focusRequester(selectedFocusRequester)
                        } else {
                            Modifier
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(focusModifier)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    when {
                                        isItemFocused -> AccentCyan
                                        isSelected -> AccentCyan.copy(alpha = 0.16f)
                                        else -> Slate850
                                    }
                                )
                                .border(
                                    width = if (isItemFocused) 2.5.dp else if (isSelected) 1.5.dp else 1.dp,
                                    color = when {
                                        isItemFocused -> PureWhite
                                        isSelected -> AccentCyan
                                        else -> Slate800
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .focusable(interactionSource = itemInteraction)
                                .onPreviewKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyUp &&
                                        (event.key == Key.DirectionCenter ||
                                         event.key == Key.Enter ||
                                         event.key == Key.NumPadEnter)
                                    ) {
                                        onSelectLanguage(lang.code)
                                        showLanguageDialog = false
                                        true
                                    } else false
                                }
                                .clickable(
                                    interactionSource = itemInteraction,
                                    indication = null
                                ) {
                                    onSelectLanguage(lang.code)
                                    showLanguageDialog = false
                                }
                                .padding(horizontal = 14.dp, vertical = 11.dp)
                        ) {
                            Column {
                                Text(
                                    text = lang.nativeName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isItemFocused) Slate950 else PureWhite
                                )
                                if (lang.englishName != lang.nativeName) {
                                    Text(
                                        text = lang.englishName,
                                        fontSize = 11.sp,
                                        color = if (isItemFocused) Slate900 else Slate400
                                    )
                                }
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (isItemFocused) Slate950 else AccentCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(
                        text = strings.close,
                        color = Slate400,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }
}
