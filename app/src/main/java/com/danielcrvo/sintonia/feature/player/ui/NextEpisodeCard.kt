package com.danielcrvo.sintonia.feature.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.danielcrvo.sintonia.R
import com.danielcrvo.sintonia.core.designsystem.components.ButtonSize
import com.danielcrvo.sintonia.core.designsystem.components.ButtonVariant
import com.danielcrvo.sintonia.core.designsystem.components.SButton
import com.danielcrvo.sintonia.core.designsystem.components.SText
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme

@Composable
fun NextEpisodeCard(
    upNext: UpNext,
    onCancel: () -> Unit,
    onPlayNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SintoniaTheme.colors
    val shape = RoundedCornerShape(SintoniaTheme.radii.card)
    Column(
        modifier = modifier
            .width(SintoniaTheme.sizes.nextEpisodeCardWidth)
            .background(colors.surface, shape)
            .border(SintoniaTheme.strokes.hairline, colors.borderStrong, shape)
            .padding(SintoniaTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.sm),
    ) {
        SText(
            stringResource(R.string.player_next_in, upNext.secondsLeft),
            style = SintoniaTheme.typography.monoLabel,
            color = colors.textMuted,
        )
        SText(
            "T${upNext.episode.season} · E${upNext.episode.number}",
            style = SintoniaTheme.typography.mono,
            color = colors.textSecondary,
        )
        SText(upNext.episode.title, style = SintoniaTheme.typography.bodyStrong, maxLines = 2)
        Row(Modifier.padding(top = SintoniaTheme.spacing.xs), horizontalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.sm)) {
            SButton(stringResource(R.string.action_cancel), onCancel, Modifier.weight(1f), variant = ButtonVariant.Secondary, size = ButtonSize.Compact)
            SButton(stringResource(R.string.player_play_now), onPlayNow, Modifier.weight(1f), size = ButtonSize.Compact)
        }
    }
}
