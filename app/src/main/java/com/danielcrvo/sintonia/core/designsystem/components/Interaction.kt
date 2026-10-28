package com.danielcrvo.sintonia.core.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.danielcrvo.sintonia.core.designsystem.theme.DeviceClass
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme
import kotlinx.coroutines.launch

@Composable
fun Modifier.sintoniaFocusRing(
    interactionSource: MutableInteractionSource,
    cornerRadius: Dp,
    focusScale: Float = 1f,
): Modifier {
    val focused = interactionSource.collectIsFocusedAsState()
    val ringColor = SintoniaTheme.colors.focusRing
    val stroke = SintoniaTheme.strokes
    val inside = SintoniaTheme.deviceClass == DeviceClass.Tv
    val scale = remember { Animatable(1f) }
    val motion = SintoniaTheme.motion
    // State is read outside composition: focus animates and redraws without recomposing.
    LaunchedEffect(interactionSource, focusScale) {
        snapshotFlow { focused.value }.collect { scale.animateTo(if (it) focusScale else 1f, motion.fast()) }
    }
    return this
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }.drawWithContent {
            drawContent()
            if (focused.value) {
                // On TV the ring is drawn inside: lists, tabs and groups clip anything past the border.
                val offset = if (inside) -stroke.focus.toPx() / 2f else stroke.focusOffset.toPx() + stroke.focus.toPx() / 2f
                drawRoundRect(
                    color = ringColor,
                    topLeft = Offset(-offset, -offset),
                    size = Size(size.width + offset * 2, size.height + offset * 2),
                    cornerRadius = CornerRadius((cornerRadius.toPx() + offset).coerceAtLeast(0f)),
                    style = Stroke(width = stroke.focus.toPx()),
                )
            }
        }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Modifier.sintoniaClickable(
    onClick: () -> Unit,
    cornerRadius: Dp,
    enabled: Boolean = true,
    role: Role? = Role.Button,
    onLongClick: (() -> Unit)? = null,
    focusScale: Float = 1f,
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed = interactionSource.collectIsPressedAsState()
    val alpha = remember { Animatable(1f) }
    val pressScale = remember { Animatable(1f) }
    val motion = SintoniaTheme.motion
    LaunchedEffect(interactionSource) {
        snapshotFlow { pressed.value }.collect { down ->
            launch { alpha.animateTo(if (down) PressedAlpha else 1f, motion.fast()) }
            pressScale.animateTo(if (down) PressedScale else 1f, motion.fast())
        }
    }
    return this
        .sintoniaFocusRing(interactionSource, cornerRadius, focusScale)
        .graphicsLayer {
            this.alpha = if (enabled) alpha.value else DisabledAlpha
            scaleX = pressScale.value
            scaleY = pressScale.value
        }.combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = onClick,
            onLongClick = onLongClick,
        )
}

@Composable
fun Modifier.sintoniaFocusable(cornerRadius: Dp = 8.dp, enabled: Boolean = true): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return this
        .sintoniaFocusRing(interactionSource, cornerRadius)
        .focusable(enabled, interactionSource)
}

/** Fails silently if the item is not composed yet, so it never crashes the screen. */
@Composable
fun Modifier.initialFocusOnTv(key: Any? = Unit): Modifier {
    if (SintoniaTheme.deviceClass != DeviceClass.Tv) return this
    val requester = remember { FocusRequester() }
    LaunchedEffect(key) { runCatching { requester.requestFocus() } }
    return this.focusRequester(requester)
}

private const val PressedAlpha = 0.7f
private const val PressedScale = 0.98f
private const val DisabledAlpha = 0.4f
