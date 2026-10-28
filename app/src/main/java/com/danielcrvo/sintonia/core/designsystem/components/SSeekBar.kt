package com.danielcrvo.sintonia.core.designsystem.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun SSeekBar(
    positionMs: () -> Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragFraction by remember { mutableFloatStateOf(-1f) }
    val fill = Color.White
    val track = Color.White.copy(alpha = 0.25f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .pointerInput(durationMs) {
                detectTapGestures { offset -> if (durationMs > 0) onSeek((offset.x / size.width * durationMs).toLong()) }
            }.pointerInput(durationMs) {
                detectHorizontalDragGestures(
                    onDragStart = { offset -> dragFraction = (offset.x / size.width).coerceIn(0f, 1f) },
                    onDragEnd = {
                        if (durationMs > 0 && dragFraction >= 0f) onSeek((dragFraction * durationMs).toLong())
                        dragFraction = -1f
                    },
                    onDragCancel = { dragFraction = -1f },
                    onHorizontalDrag = { change, _ -> dragFraction = (change.position.x / size.width).coerceIn(0f, 1f) },
                )
            }.drawBehind {
                val trackHeight = 4.dp.toPx()
                val centerY = size.height / 2f
                val fraction = if (dragFraction >= 0f) {
                    dragFraction
                } else if (durationMs > 0) {
                    (positionMs().toFloat() / durationMs).coerceIn(0f, 1f)
                } else {
                    0f
                }
                val radius = CornerRadius(trackHeight / 2f)
                drawRoundRect(track, Offset(0f, centerY - trackHeight / 2f), Size(size.width, trackHeight), radius)
                drawRoundRect(fill, Offset(0f, centerY - trackHeight / 2f), Size(size.width * fraction, trackHeight), radius)
                drawCircle(fill, radius = 7.dp.toPx(), center = Offset(size.width * fraction, centerY))
            },
    )
}
