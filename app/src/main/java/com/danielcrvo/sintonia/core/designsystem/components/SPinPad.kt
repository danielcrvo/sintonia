package com.danielcrvo.sintonia.core.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import com.danielcrvo.sintonia.R
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme

const val PinLength = 4

@Composable
fun SPinBoxes(
    filled: Int,
    error: Boolean,
    errorTick: Int,
    modifier: Modifier = Modifier,
) {
    val colors = SintoniaTheme.colors
    val shake = remember { Animatable(0f) }
    val reduced = SintoniaTheme.motion.isReduced
    val amplitude = with(LocalDensity.current) { SintoniaTheme.spacing.sm.toPx() }
    LaunchedEffect(errorTick) {
        if (errorTick > 0 && !reduced) {
            repeat(3) {
                shake.animateTo(1f, tween(40))
                shake.animateTo(-1f, tween(80))
            }
            shake.animateTo(0f, tween(40))
        }
    }
    Row(
        modifier = modifier.offset { IntOffset((shake.value * amplitude).toInt(), 0) },
        horizontalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.md),
    ) {
        repeat(PinLength) { index ->
            val shape = RoundedCornerShape(SintoniaTheme.radii.control)
            Box(
                modifier = Modifier
                    .size(width = SintoniaTheme.sizes.pinBoxWidth, height = SintoniaTheme.sizes.pinKey)
                    .background(colors.surface, shape)
                    .border(
                        SintoniaTheme.strokes.hairline,
                        when {
                            error -> colors.danger
                            index == filled -> colors.textSecondary
                            else -> colors.border
                        },
                        shape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (index < filled) Box(Modifier.size(SintoniaTheme.sizes.pinDot).background(colors.text, CircleShape))
            }
        }
    }
}

@Composable
fun SPinPad(
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9))
    Column(modifier, verticalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.sm)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.sm)) {
                row.forEach { digit -> DigitKey(digit, Modifier.weight(1f), onDigit) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.sm)) {
            Box(Modifier.weight(1f))
            DigitKey(0, Modifier.weight(1f), onDigit)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(SintoniaTheme.sizes.pinKey)
                    .sintoniaClickable(onClick = onBackspace, cornerRadius = SintoniaTheme.radii.control),
                contentAlignment = Alignment.Center,
            ) {
                SIcon(SIcons.Backspace, contentDescription = stringResource(R.string.pin_backspace), tint = SintoniaTheme.colors.textSecondary)
            }
        }
    }
}

@Composable
private fun DigitKey(digit: Int, modifier: Modifier, onDigit: (Int) -> Unit) {
    val colors = SintoniaTheme.colors
    val shape = RoundedCornerShape(SintoniaTheme.radii.control)
    Box(
        modifier = modifier
            .height(SintoniaTheme.sizes.pinKey)
            .sintoniaClickable(onClick = { onDigit(digit) }, cornerRadius = SintoniaTheme.radii.control)
            .background(colors.surface, shape)
            .border(SintoniaTheme.strokes.hairline, colors.border, shape),
        contentAlignment = Alignment.Center,
    ) {
        SText(digit.toString(), style = SintoniaTheme.typography.heading)
    }
}
