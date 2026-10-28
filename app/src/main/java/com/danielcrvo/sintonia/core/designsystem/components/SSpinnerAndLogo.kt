package com.danielcrvo.sintonia.core.designsystem.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme

@Composable
fun SSpinner(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = SintoniaTheme.colors.text,
) {
    val reduced = SintoniaTheme.motion.isReduced
    val angle = if (reduced) {
        0f
    } else {
        val transition = rememberInfiniteTransition(label = "spinner")
        transition
            .animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
                label = "spinnerAngle",
            ).value
    }
    val trackColor = SintoniaTheme.colors.track
    Canvas(modifier.size(size)) {
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        val inset = stroke.width / 2f
        val arcSize = Size(this.size.width - stroke.width, this.size.height - stroke.width)
        drawArc(trackColor, 0f, 360f, false, Offset(inset, inset), arcSize, style = stroke)
        rotate(angle) {
            drawArc(color, -90f, 90f, false, Offset(inset, inset), arcSize, style = stroke)
        }
    }
}

@Composable
fun SLogo(modifier: Modifier = Modifier, size: Dp = 32.dp) {
    val line = SintoniaTheme.colors.text
    val brand = SintoniaTheme.colors.brand
    Canvas(modifier.size(size)) {
        val unit = this.size.width / 32f
        val stroke = Stroke(width = 2f * unit, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val frame = Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    left = 3f * unit,
                    top = 4f * unit,
                    right = 29f * unit,
                    bottom = 24f * unit,
                    radiusX = 6f * unit,
                    radiusY = 6f * unit,
                ),
            )
        }
        drawPath(frame, line, style = stroke)
        val stand = Path().apply {
            moveTo(11f * unit, 27.5f * unit)
            lineTo(21f * unit, 27.5f * unit)
        }
        drawPath(stand, line, style = stroke)
        val wave = Path().apply {
            moveTo(7f * unit, 14f * unit)
            cubicTo(9.5f * unit, 7f * unit, 13f * unit, 7f * unit, 16f * unit, 14f * unit)
            cubicTo(19f * unit, 21f * unit, 22.5f * unit, 21f * unit, 25f * unit, 14f * unit)
        }
        drawPath(wave, line, style = stroke)
        drawCircle(brand, radius = 2.1f * unit, center = Offset(16f * unit, 14f * unit))
    }
}
