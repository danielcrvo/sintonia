package com.danielcrvo.sintonia.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import com.danielcrvo.sintonia.R
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme
import com.danielcrvo.sintonia.core.designsystem.theme.ThemeMode

@Composable
fun SSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = SintoniaTheme.colors
    val width = SintoniaTheme.sizes.switchWidth
    val height = SintoniaTheme.sizes.switchHeight
    val thumb = SintoniaTheme.sizes.switchThumb
    val inset = (height - thumb) / 2
    val position by animateFloatAsState(if (checked) 1f else 0f, SintoniaTheme.motion.fast(), label = "switchThumb")
    val trackColor by animateColorAsState(
        if (checked) colors.primary else colors.track,
        SintoniaTheme.motion.fast(),
        label = "switchTrack",
    )
    val thumbColor = if (checked) colors.onPrimary else colors.background
    val travel = with(LocalDensity.current) { (width - thumb - inset * 2).toPx() }
    Box(
        modifier = modifier
            .size(width, height)
            .sintoniaClickable(
                onClick = { onCheckedChange(!checked) },
                cornerRadius = SintoniaTheme.radii.pill,
                enabled = enabled,
                role = Role.Switch,
            ).background(trackColor, CircleShape),
    ) {
        Box(
            Modifier
                .padding(inset)
                .offset { IntOffset((travel * position).toInt(), 0) }
                .size(thumb)
                .background(thumbColor, CircleShape),
        )
    }
}

@Composable
fun SCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val colors = SintoniaTheme.colors
    val radius = SintoniaTheme.radii.thumb
    val shape = RoundedCornerShape(radius)
    Box(
        modifier = modifier
            .size(SintoniaTheme.heights.minTouch)
            .sintoniaClickable(
                onClick = { onCheckedChange(!checked) },
                cornerRadius = radius,
                role = Role.Checkbox,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(SintoniaTheme.sizes.checkbox)
                .clip(shape)
                .background(if (checked) colors.primary else colors.surface)
                .border(SintoniaTheme.strokes.hairline, if (checked) colors.primary else colors.borderStrong, shape),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) SIcon(SIcons.Check, contentDescription, tint = colors.onPrimary, size = SintoniaTheme.sizes.iconCheck)
        }
    }
}

@Composable
fun SRadio(selected: Boolean, modifier: Modifier = Modifier) {
    val colors = SintoniaTheme.colors
    Box(
        modifier = modifier
            .size(SintoniaTheme.sizes.checkbox)
            .border(SintoniaTheme.strokes.hairline, if (selected) colors.text else colors.borderStrong, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Box(Modifier.size(SintoniaTheme.sizes.radioDot).background(colors.text, CircleShape))
    }
}

@Composable
fun SThemeSwitch(
    selected: ThemeMode,
    onSelected: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SintoniaTheme.colors
    val shape = RoundedCornerShape(SintoniaTheme.radii.pill)
    val options = listOf(
        ThemeMode.Auto to (SIcons.Contrast to stringResource(R.string.theme_auto)),
        ThemeMode.Light to (SIcons.Sun to stringResource(R.string.theme_light)),
        ThemeMode.Dark to (SIcons.Moon to stringResource(R.string.theme_dark)),
    )
    Row(
        modifier = modifier
            .border(SintoniaTheme.strokes.hairline, colors.borderStrong, shape)
            .padding(SintoniaTheme.spacing.xs / 2),
    ) {
        options.forEach { (mode, iconAndLabel) ->
            val active = mode == selected
            val background by animateColorAsState(
                if (active) colors.surfaceAlt else colors.surfaceAlt.copy(alpha = 0f),
                SintoniaTheme.motion.fast(),
                label = "themeSegment",
            )
            Box(
                modifier = Modifier
                    .size(width = SintoniaTheme.sizes.themeSegmentWidth, height = SintoniaTheme.sizes.themeSegmentHeight)
                    .sintoniaClickable(
                        onClick = { onSelected(mode) },
                        cornerRadius = SintoniaTheme.radii.pill,
                        role = Role.RadioButton,
                    ).background(background, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                SIcon(
                    icon = iconAndLabel.first,
                    contentDescription = iconAndLabel.second,
                    tint = if (active) colors.text else colors.textMuted,
                    size = SintoniaTheme.sizes.iconInline,
                )
            }
        }
    }
}
