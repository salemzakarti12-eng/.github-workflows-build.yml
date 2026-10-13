package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.PureWhite

/**
 * Reusable modifier for Android TV remote (D-pad) navigation and Mobile touch.
 * Adds glowing focus border, smooth scale enlargement on focus, and handles TV remote OK / Center keys.
 */
@Composable
fun Modifier.tvFocusable(
    onClick: () -> Unit,
    shape: Shape = RoundedCornerShape(16.dp),
    focusedBorderColor: Color = AccentCyan,
    focusedBorderWidth: Dp = 3.5.dp,
    unfocusedBorderColor: Color = Color.Transparent,
    unfocusedBorderWidth: Dp = 1.dp,
    focusedScale: Float = 1.04f,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
): Modifier {
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isFocused) focusedScale else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "tvFocusScale"
    )

    return this
        .scale(scale)
        .shadow(
            elevation = if (isFocused) 12.dp else 0.dp,
            shape = shape,
            ambientColor = focusedBorderColor,
            spotColor = focusedBorderColor
        )
        .border(
            width = if (isFocused) focusedBorderWidth else unfocusedBorderWidth,
            color = if (isFocused) focusedBorderColor else unfocusedBorderColor,
            shape = shape
        )
        .focusable(interactionSource = interactionSource)
        .onPreviewKeyEvent { event ->
            if (event.type == KeyEventType.KeyUp &&
                (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
            ) {
                onClick()
                true
            } else {
                false
            }
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}
