package com.danielcrvo.sintonia.core.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme

@Composable
fun SEmptyState(
    @DrawableRes icon: Int,
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    code: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    val colors = SintoniaTheme.colors
    val shape = RoundedCornerShape(SintoniaTheme.radii.card)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(SintoniaTheme.spacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.md, Alignment.CenterVertically),
    ) {
        Box(
            Modifier
                .size(56.dp)
                .background(colors.surface, shape)
                .border(SintoniaTheme.strokes.hairline, colors.border, shape),
            contentAlignment = Alignment.Center,
        ) {
            SIcon(icon, contentDescription = null, tint = colors.textMuted, size = 24.dp)
        }
        SText(title, style = SintoniaTheme.typography.subheading, textAlign = TextAlign.Center)
        if (message != null) {
            SText(message, style = SintoniaTheme.typography.body, color = colors.textSecondary, textAlign = TextAlign.Center)
        }
        if (code != null) {
            SText(code, style = SintoniaTheme.typography.mono, color = colors.textMuted, textAlign = TextAlign.Center)
        }
        if (action != null) Box(Modifier.padding(top = SintoniaTheme.spacing.sm)) { action() }
    }
}
