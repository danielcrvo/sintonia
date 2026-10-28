package com.danielcrvo.sintonia.core.designsystem.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme

@Immutable
data class GuideProgram(val startMillis: Long, val stopMillis: Long, val title: String)

object GuideMetrics {
    const val SlotMillis = 30 * 60_000L
    const val HourMillis = 60 * 60_000L
    val HourWidth: Dp = 240.dp
    val ChannelColumn: Dp = 280.dp
    val RowHeight: Dp = 72.dp
    val MinBlockWidth: Dp = 72.dp

    fun width(millis: Long): Dp = HourWidth * (millis.toFloat() / HourMillis)
}

private class GuideBlock(val program: GuideProgram, val offset: Dp, val width: Dp)

private fun layoutBlocks(programs: List<GuideProgram>, windowStartMillis: Long, windowMillis: Long): List<GuideBlock> {
    val minWidth = GuideMetrics.MinBlockWidth
    val blocks = ArrayList<GuideBlock>(programs.size)
    var cursor = 0.dp
    val windowEnd = GuideMetrics.width(windowMillis)
    programs.sortedBy { it.startMillis }.forEach { program ->
        val start = GuideMetrics.width((program.startMillis - windowStartMillis).coerceIn(0, windowMillis))
        val stop = GuideMetrics.width((program.stopMillis - windowStartMillis).coerceIn(0, windowMillis))
        val from = maxOf(start, cursor)
        val to = maxOf(stop, from + minWidth).coerceAtMost(maxOf(windowEnd, from + minWidth))
        if (stop > start && to > from) {
            blocks += GuideBlock(program, from, to - from)
            cursor = to
        }
    }
    return blocks
}

@Composable
fun SGuideRuler(
    windowStartMillis: Long,
    hours: Int,
    scroll: ScrollState,
    label: (Long) -> String,
    modifier: Modifier = Modifier,
) {
    val colors = SintoniaTheme.colors
    Row(modifier.height(SintoniaTheme.heights.tabs)) {
        Box(Modifier.width(GuideMetrics.ChannelColumn))
        Row(Modifier.horizontalScroll(scroll, enabled = false)) {
            repeat(hours * 2) { slot ->
                Box(Modifier.width(GuideMetrics.width(GuideMetrics.SlotMillis)).fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
                    SText(
                        label(windowStartMillis + slot * GuideMetrics.SlotMillis),
                        style = SintoniaTheme.typography.mono,
                        color = colors.textMuted,
                        maxLines = 1,
                        modifier = Modifier.padding(start = SintoniaTheme.spacing.sm),
                    )
                }
            }
        }
    }
}

@Composable
fun SGuideRow(
    programs: List<GuideProgram>,
    windowStartMillis: Long,
    hours: Int,
    nowMillis: Long,
    scroll: ScrollState,
    emptyLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    val colors = SintoniaTheme.colors
    val hairline = SintoniaTheme.strokes.hairline
    val nowWidth = SintoniaTheme.strokes.focus
    val windowMillis = hours * GuideMetrics.HourMillis
    val blocks = remember(programs, windowStartMillis, windowMillis) { layoutBlocks(programs, windowStartMillis, windowMillis) }
    val nowOffset = GuideMetrics.width((nowMillis - windowStartMillis).coerceIn(0, windowMillis))
    val blockShape = RoundedCornerShape(SintoniaTheme.radii.control)
    Box(
        modifier
            .horizontalScroll(scroll)
            .width(GuideMetrics.width(windowMillis))
            .height(GuideMetrics.RowHeight)
            .drawBehind {
                drawRect(colors.brand, Offset(nowOffset.toPx(), 0f), Size(nowWidth.toPx(), size.height))
            },
    ) {
        if (programs.isEmpty()) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .width(GuideMetrics.width(windowMillis))
                    .padding(SintoniaTheme.spacing.xs)
                    .sintoniaClickable(onClick = onClick, onLongClick = onLongClick, cornerRadius = SintoniaTheme.radii.control)
                    .padding(horizontal = SintoniaTheme.spacing.md),
                contentAlignment = Alignment.CenterStart,
            ) {
                SText(emptyLabel, style = SintoniaTheme.typography.caption, color = colors.textMuted, maxLines = 1)
            }
        }
        blocks.forEach { block ->
            val program = block.program
            val live = nowMillis in program.startMillis until program.stopMillis
            Box(
                Modifier
                    .offset(x = block.offset)
                    .width(block.width)
                    .fillMaxHeight()
                    .padding(SintoniaTheme.spacing.xs)
                    .sintoniaClickable(onClick = onClick, onLongClick = onLongClick, cornerRadius = SintoniaTheme.radii.control)
                    .background(if (live) colors.surfaceAlt else colors.surface, blockShape)
                    .border(hairline, if (live) colors.borderStrong else colors.border, blockShape)
                    .padding(horizontal = SintoniaTheme.spacing.md),
                contentAlignment = Alignment.CenterStart,
            ) {
                SText(
                    program.title,
                    style = if (live) SintoniaTheme.typography.bodyStrong else SintoniaTheme.typography.body,
                    color = if (live) colors.text else colors.textSecondary,
                    maxLines = 2,
                )
            }
        }
    }
}
