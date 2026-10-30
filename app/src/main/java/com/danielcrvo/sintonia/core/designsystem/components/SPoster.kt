package com.danielcrvo.sintonia.core.designsystem.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.danielcrvo.sintonia.core.designsystem.theme.DeviceClass
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme

enum class PosterShape(val ratio: Float) { Portrait(2f / 3f), Wide(16f / 9f) }

@Composable
fun SPoster(
    title: String,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    meta: String? = null,
    progress: Float? = null,
    badge: String? = null,
    shape: PosterShape = PosterShape.Portrait,
) {
    val colors = SintoniaTheme.colors
    val radius = SintoniaTheme.radii.control
    val coverShape = RoundedCornerShape(radius)
    val tv = SintoniaTheme.deviceClass == DeviceClass.Tv
    Column(
        modifier = modifier
            .sintoniaClickable(onClick = onClick, cornerRadius = radius, focusScale = if (tv) 1.04f else 1f)
            .padding(SintoniaTheme.spacing.focusInset),
        verticalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.snug),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(shape.ratio)
                .clip(coverShape)
                .border(SintoniaTheme.strokes.hairline, SintoniaTheme.colors.posterBorder, coverShape),
        ) {
            SImage(url = imageUrl, contentDescription = null, modifier = Modifier.matchParentSize())
            if (badge != null) {
                SBadge(badge, style = BadgeStyle.OnImage, modifier = Modifier.align(Alignment.TopStart).padding(SintoniaTheme.spacing.sm))
            }
            if (progress != null) {
                SProgressBar(
                    progress = { progress },
                    height = SintoniaTheme.sizes.progressThin,
                    onImage = true,
                    modifier = Modifier.align(Alignment.BottomStart),
                )
            }
        }
        SText(title, style = SintoniaTheme.typography.bodyStrong, maxLines = 1)
        if (meta != null) {
            SText(meta, style = SintoniaTheme.typography.mono, color = colors.textMuted, maxLines = 1)
        }
    }
}
