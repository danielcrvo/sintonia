package com.danielcrvo.sintonia.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.danielcrvo.sintonia.R
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme

data class ChannelRowData(
    val number: String,
    val name: String,
    val logoUrl: String?,
    val program: String?,
    val progress: Float?,
)

@Composable
fun SChannelRow(
    data: ChannelRowData,
    favorite: Boolean,
    playing: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    val colors = SintoniaTheme.colors
    val spacing = SintoniaTheme.spacing
    val radius = SintoniaTheme.radii.control
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(SintoniaTheme.heights.channelRow)
            .sintoniaClickable(onClick = onClick, onLongClick = onLongClick, cornerRadius = radius)
            .background(if (playing) colors.surfaceAlt else Color.Transparent)
            .padding(start = spacing.gutter, end = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        SText(
            text = data.number,
            style = SintoniaTheme.typography.mono,
            color = colors.textMuted,
            modifier = Modifier.width(SintoniaTheme.sizes.channelNumberWidth),
            textAlign = TextAlign.End,
            maxLines = 1,
        )
        Box(
            modifier = Modifier
                .size(SintoniaTheme.sizes.avatar)
                .then(
                    if (playing) {
                        Modifier.border(SintoniaTheme.strokes.focus, colors.text, CircleShape)
                    } else {
                        Modifier.border(SintoniaTheme.strokes.hairline, colors.border, CircleShape)
                    },
                ).padding(if (playing) SintoniaTheme.sizes.ringGap else 0.dp)
                .clip(CircleShape),
        ) {
            SImage(url = data.logoUrl, contentDescription = null, modifier = Modifier.matchParentSize())
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(SintoniaTheme.sizes.tightGap),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                SText(
                    text = data.name,
                    style = SintoniaTheme.typography.bodyLarge,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (playing) SBadge(stringResource(R.string.badge_playing))
            }
            if (data.program != null) {
                SText(
                    text = data.program,
                    style = SintoniaTheme.typography.caption,
                    color = colors.textSecondary,
                    maxLines = 1,
                )
            }
            val progress = data.progress
            if (progress != null) {
                SProgressBar(progress = { progress }, modifier = Modifier.padding(top = SintoniaTheme.sizes.tightGap))
            }
        }
        SIconButton(
            icon = if (favorite) SIcons.Star else SIcons.StarOutline,
            contentDescription = stringResource(if (favorite) R.string.channel_unfavorite else R.string.channel_favorite),
            onClick = onToggleFavorite,
            bordered = false,
        )
    }
}
