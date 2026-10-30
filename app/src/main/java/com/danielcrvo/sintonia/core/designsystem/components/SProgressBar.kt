package com.danielcrvo.sintonia.core.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme

@Composable
fun SProgressBar(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    height: Dp = SintoniaTheme.sizes.progressHairline,
    onImage: Boolean = false,
) {
    val colors = SintoniaTheme.colors
    val track = if (onImage) SintoniaTheme.colors.onImageTrack else colors.track
    val fill = if (onImage) SintoniaTheme.colors.onImage else colors.text
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .drawBehind {
                val radius = CornerRadius(size.height / 2f)
                drawRoundRect(track, size = size, cornerRadius = radius)
                val value = progress().coerceIn(0f, 1f)
                if (value > 0f) {
                    drawRoundRect(fill, size = Size(size.width * value, size.height), cornerRadius = radius)
                }
            },
    )
}
