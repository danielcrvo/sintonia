package com.danielcrvo.sintonia.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.danielcrvo.sintonia.core.designsystem.theme.DeviceClass
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme

enum class BadgeStyle { Neutral, OnImage }

@Composable
fun SBadge(
    text: String,
    modifier: Modifier = Modifier,
    style: BadgeStyle = BadgeStyle.Neutral,
) {
    val colors = SintoniaTheme.colors
    val shape = RoundedCornerShape(SintoniaTheme.radii.pill)
    val (background, border, content) = when (style) {
        BadgeStyle.Neutral -> Triple(colors.surfaceAlt, colors.border, colors.textSecondary)
        BadgeStyle.OnImage -> Triple(Color.Black.copy(alpha = 0.6f), Color.White.copy(alpha = 0.14f), Color.White)
    }
    Box(
        modifier = modifier
            .background(background, shape)
            .border(SintoniaTheme.strokes.hairline, border, shape)
            .padding(horizontal = SintoniaTheme.spacing.sm, vertical = SintoniaTheme.spacing.xs / 2),
        contentAlignment = Alignment.Center,
    ) {
        SText(text = text, style = SintoniaTheme.typography.monoLabel, color = content, maxLines = 1)
    }
}

@Composable
fun SLiveBadge(text: String, modifier: Modifier = Modifier) {
    val colors = SintoniaTheme.colors
    val shape = RoundedCornerShape(SintoniaTheme.radii.pill)
    val dot = if (SintoniaTheme.deviceClass == DeviceClass.Tv) 8.dp else 6.dp
    Row(
        modifier = modifier
            .background(colors.liveBg, shape)
            .padding(horizontal = SintoniaTheme.spacing.sm, vertical = SintoniaTheme.spacing.xs / 2),
        horizontalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.xs + 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(dot).background(colors.brand, CircleShape))
        SText(text = text, style = SintoniaTheme.typography.monoLabel, color = colors.liveText, maxLines = 1)
    }
}
