package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.locale.LocalAppLanguage
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900

@Composable
fun LanguageToggleButton(
    onToggleLanguage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentLanguage = LocalAppLanguage.current
    val isArabic = currentLanguage == "ar"

    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

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
                    onToggleLanguage()
                    true
                } else false
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onToggleLanguage)
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
                contentDescription = "Toggle Language",
                tint = if (isFocused) Slate900 else AccentCyan,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isArabic) "🌐 English" else "🌐 عربي",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isFocused) Slate900 else PureWhite
            )
        }
    }
}
