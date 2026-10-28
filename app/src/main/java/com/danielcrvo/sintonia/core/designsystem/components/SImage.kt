package com.danielcrvo.sintonia.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.danielcrvo.sintonia.core.designsystem.theme.Motion
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme

@Composable
fun SImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    Box(modifier.background(SintoniaTheme.colors.surfaceAlt)) {
        if (!url.isNullOrBlank()) {
            val context = LocalPlatformContext.current
            val fadeMillis = if (SintoniaTheme.motion.isReduced) 0 else Motion.NormalMillis
            val request = remember(url, fadeMillis) {
                ImageRequest
                    .Builder(context)
                    .data(url)
                    .crossfade(fadeMillis)
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}
