package com.danielcrvo.sintonia.core.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme

@Composable
fun SGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(SintoniaTheme.radii.card)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SintoniaTheme.colors.surface)
            .border(SintoniaTheme.strokes.hairline, SintoniaTheme.colors.border, shape),
        content = content,
    )
}

@Composable
fun SSettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    value: String? = null,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    destructive: Boolean = false,
    showDivider: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = SintoniaTheme.colors
    val dividerColor = colors.divider
    val hairline = SintoniaTheme.strokes.hairline
    val content = if (destructive) colors.danger else colors.text
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SintoniaTheme.heights.settingsRow)
            .then(
                if (onClick != null) {
                    Modifier.sintoniaClickable(onClick = onClick, cornerRadius = SintoniaTheme.radii.card)
                } else {
                    Modifier
                },
            ).drawBehind {
                if (showDivider) {
                    drawRect(
                        color = dividerColor,
                        topLeft = Offset(0f, size.height - hairline.toPx()),
                        size = Size(size.width, hairline.toPx()),
                    )
                }
            }.padding(horizontal = SintoniaTheme.spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.md),
    ) {
        if (icon != null) SIcon(icon, contentDescription = null, tint = if (destructive) colors.danger else colors.textMuted)
        Column(Modifier.weight(1f).then(if (subtitle != null) Modifier.padding(vertical = SintoniaTheme.spacing.sm) else Modifier)) {
            SText(title, style = SintoniaTheme.typography.body, color = content, maxLines = 2)
            if (subtitle != null) SText(subtitle, style = SintoniaTheme.typography.mono, color = colors.textMuted, maxLines = 1)
        }
        if (value != null) SText(value, style = SintoniaTheme.typography.mono, color = colors.textMuted, maxLines = 1)
        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            SIcon(SIcons.ChevronRight, contentDescription = null, tint = colors.icon, size = SintoniaTheme.heights.minTouch / 2)
        }
    }
}

@Composable
fun SSectionLabel(text: String, modifier: Modifier = Modifier) {
    Box(modifier.padding(bottom = SintoniaTheme.spacing.sm)) {
        SText(text.uppercase(), style = SintoniaTheme.typography.monoLabel, color = SintoniaTheme.colors.textMuted)
    }
}
