package com.danielcrvo.sintonia.core.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme
import kotlinx.coroutines.launch

enum class ButtonVariant { Primary, Secondary, Destructive }

enum class ButtonSize { Default, Footer, Hero, Compact }

@Composable
fun SButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    size: ButtonSize = ButtonSize.Default,
    @DrawableRes leadingIcon: Int? = null,
    enabled: Boolean = true,
) {
    val colors = SintoniaTheme.colors
    val heights = SintoniaTheme.heights
    val radius = SintoniaTheme.radii.control
    val shape = RoundedCornerShape(radius)
    val height = when (size) {
        ButtonSize.Default -> heights.button
        ButtonSize.Footer -> heights.buttonFooter
        ButtonSize.Hero -> heights.buttonHero
        ButtonSize.Compact -> heights.buttonCompact
    }
    val primary = variant == ButtonVariant.Primary
    val contentColor = when (variant) {
        ButtonVariant.Primary -> colors.onPrimary
        ButtonVariant.Secondary -> colors.text
        ButtonVariant.Destructive -> colors.danger
    }
    Row(
        modifier = modifier
            .height(height)
            .defaultMinSize(minWidth = height)
            .sintoniaClickable(
                onClick = onClick,
                cornerRadius = radius,
                enabled = enabled,
                ringContrast = if (primary) colors.background else Color.Unspecified,
            ).clip(shape)
            .background(if (primary) colors.primary else colors.background)
            .then(if (primary) Modifier else Modifier.border(SintoniaTheme.strokes.hairline, colors.borderStrong, shape))
            .padding(horizontal = SintoniaTheme.spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.sm, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            SIcon(icon = leadingIcon, contentDescription = null, tint = contentColor, size = SintoniaTheme.sizes.iconInline)
        }
        SText(
            text = text,
            style = SintoniaTheme.typography.bodyStrong,
            color = contentColor,
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bordered: Boolean = true,
    enabled: Boolean = true,
    containerColor: Color = Color.Unspecified,
    borderColor: Color = Color.Unspecified,
    tint: Color = Color.Unspecified,
) {
    val colors = SintoniaTheme.colors
    val radius = SintoniaTheme.radii.control
    val shape = RoundedCornerShape(radius)
    val touch = SintoniaTheme.heights.minTouch
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val tooltip = rememberSTooltipState()
    STooltipBox(text = contentDescription, state = tooltip, modifier = modifier) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(touch)
                .sintoniaClickable(
                    onClick = onClick,
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch { tooltip.show() }
                    },
                    cornerRadius = radius,
                    enabled = enabled,
                ).then(if (containerColor.isSpecified) Modifier.background(containerColor, shape) else Modifier)
                .then(
                    if (bordered) {
                        Modifier.border(SintoniaTheme.strokes.hairline, if (borderColor.isSpecified) borderColor else colors.border, shape)
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            SIcon(icon = icon, contentDescription = contentDescription, tint = if (tint.isSpecified) tint else colors.text)
        }
    }
}
