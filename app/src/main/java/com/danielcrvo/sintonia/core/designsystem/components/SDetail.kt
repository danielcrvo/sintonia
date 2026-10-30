package com.danielcrvo.sintonia.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme

@Composable
fun SDetailHero(
    imageUrl: String?,
    onBack: () -> Unit,
    backDescription: String,
    modifier: Modifier = Modifier,
) {
    val background = SintoniaTheme.colors.background
    Box(modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
        SImage(imageUrl, null, Modifier.matchParentSize())
        Box(
            Modifier
                .matchParentSize()
                .background(Brush.verticalGradient(listOf(SintoniaTheme.colors.imageShade.copy(alpha = 0.45f), Color.Transparent, background))),
        )
        if (SintoniaTheme.deviceClass != com.danielcrvo.sintonia.core.designsystem.theme.DeviceClass.Tv) {
            SIconButton(
                icon = SIcons.ArrowBack,
                contentDescription = backDescription,
                onClick = onBack,
                containerColor = background.copy(alpha = 0.6f),
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = SintoniaTheme.spacing.gutter, vertical = SintoniaTheme.spacing.sm)
                    .align(Alignment.TopStart),
            )
        }
    }
}

@Composable
fun SInfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(SintoniaTheme.sizes.tightGap)) {
        SText(label.uppercase(), style = SintoniaTheme.typography.monoLabel, color = SintoniaTheme.colors.textMuted)
        SText(value, style = SintoniaTheme.typography.body, color = SintoniaTheme.colors.textSecondary)
    }
}

@Composable
fun SMetaLine(parts: List<String>, modifier: Modifier = Modifier) {
    if (parts.isEmpty()) return
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.sm)) {
        SText(parts.joinToString(" · "), style = SintoniaTheme.typography.mono, color = SintoniaTheme.colors.textMuted)
    }
}
