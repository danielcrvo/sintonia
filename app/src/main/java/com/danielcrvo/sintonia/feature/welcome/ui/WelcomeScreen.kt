package com.danielcrvo.sintonia.feature.welcome.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.danielcrvo.sintonia.R
import com.danielcrvo.sintonia.core.designsystem.components.ButtonSize
import com.danielcrvo.sintonia.core.designsystem.components.SButton
import com.danielcrvo.sintonia.core.designsystem.components.SIcon
import com.danielcrvo.sintonia.core.designsystem.components.SIcons
import com.danielcrvo.sintonia.core.designsystem.components.SLogo
import com.danielcrvo.sintonia.core.designsystem.components.SText
import com.danielcrvo.sintonia.core.designsystem.components.initialFocusOnTv
import com.danielcrvo.sintonia.core.designsystem.components.tvSafeArea
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme

@Composable
fun WelcomeScreen(onAddList: () -> Unit, modifier: Modifier = Modifier) {
    val colors = SintoniaTheme.colors
    val spacing = SintoniaTheme.spacing
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .tvSafeArea()
            .padding(horizontal = spacing.gutter, vertical = spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = SintoniaTheme.sizes.contentMaxWidth)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.xxl, Alignment.CenterVertically),
        ) {
            Box(
                Modifier
                    .size(SintoniaTheme.sizes.logoBadge)
                    .background(colors.surface, RoundedCornerShape(SintoniaTheme.radii.card))
                    .border(SintoniaTheme.strokes.hairline, colors.border, RoundedCornerShape(SintoniaTheme.radii.card)),
                contentAlignment = Alignment.Center,
            ) { SLogo(size = SintoniaTheme.sizes.logoMedium) }

            SText(stringResource(R.string.welcome_title), style = SintoniaTheme.typography.display)

            Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                Highlight(SIcons.Tv, R.string.welcome_highlight_live)
                Highlight(SIcons.Film, R.string.welcome_highlight_vod)
                Highlight(SIcons.Lock, R.string.welcome_highlight_private)
            }
        }
        Column(
            modifier = Modifier.widthIn(max = SintoniaTheme.sizes.contentMaxWidth).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            SButton(
                text = stringResource(R.string.welcome_add_list),
                onClick = onAddList,
                modifier = Modifier.fillMaxWidth().initialFocusOnTv(),
                size = ButtonSize.Footer,
            )
            SText(
                stringResource(R.string.welcome_disclaimer),
                style = SintoniaTheme.typography.caption,
                color = colors.textMuted,
            )
        }
    }
}

@Composable
private fun Highlight(icon: Int, text: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.md),
    ) {
        SIcon(icon, contentDescription = null, tint = SintoniaTheme.colors.textSecondary)
        SText(stringResource(text), style = SintoniaTheme.typography.body, color = SintoniaTheme.colors.textSecondary)
    }
}
