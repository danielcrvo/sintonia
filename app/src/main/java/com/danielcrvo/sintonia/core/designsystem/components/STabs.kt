package com.danielcrvo.sintonia.core.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme

@Composable
fun STabs(
    tabs: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SintoniaTheme.colors
    val strokes = SintoniaTheme.strokes
    val tabPadding = SintoniaTheme.spacing.md - 2.dp
    val lefts = remember(tabs.size) { mutableStateListOf(*Array(tabs.size) { 0f }) }
    val widths = remember(tabs.size) { mutableStateListOf(*Array(tabs.size) { 0f }) }
    val index = selectedIndex.coerceIn(0, (tabs.size - 1).coerceAtLeast(0))
    val indicatorLeft by animateFloatAsState(lefts.getOrElse(index) { 0f }, SintoniaTheme.motion.normal(), label = "tabLeft")
    val indicatorWidth by animateFloatAsState(widths.getOrElse(index) { 0f }, SintoniaTheme.motion.normal(), label = "tabWidth")

    val scroll = rememberScrollState()
    val selectedTab = remember { FocusRequester() }
    var viewportWidth by remember { mutableIntStateOf(0) }
    LaunchedEffect(index, lefts.getOrElse(index) { 0f }, widths.getOrElse(index) { 0f }, viewportWidth) {
        val width = widths.getOrElse(index) { 0f }
        if (width > 0f && viewportWidth > 0) {
            val target = lefts[index] + width / 2f - viewportWidth / 2f
            scroll.animateScrollTo(target.toInt().coerceIn(0, scroll.maxValue))
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(SintoniaTheme.heights.tabs)
            .onSizeChanged { viewportWidth = it.width }
            .drawBehind {
                drawRect(
                    color = colors.border,
                    topLeft = Offset(0f, size.height - strokes.hairline.toPx()),
                    size = Size(size.width, strokes.hairline.toPx()),
                )
            },
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(scroll)
                .focusRestorer(selectedTab)
                .height(SintoniaTheme.heights.tabs)
                .drawBehind {
                    val pad = tabPadding.toPx()
                    drawRect(
                        color = colors.text,
                        topLeft = Offset(indicatorLeft + pad, size.height - strokes.focus.toPx()),
                        size = Size((indicatorWidth - pad * 2).coerceAtLeast(0f), strokes.focus.toPx()),
                    )
                },
        ) {
            tabs.forEachIndexed { i, label ->
                val active = i == index
                Box(
                    modifier = Modifier
                        .height(SintoniaTheme.heights.tabs)
                        .onGloballyPositioned {
                            lefts[i] = it.positionInParent().x
                            widths[i] = it.size.width.toFloat()
                        }.then(if (active) Modifier.focusRequester(selectedTab) else Modifier)
                        .sintoniaClickable(
                            onClick = { onSelected(i) },
                            cornerRadius = SintoniaTheme.radii.control,
                            role = Role.Tab,
                        ).padding(horizontal = tabPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    SText(
                        text = label,
                        style = SintoniaTheme.typography.bodyStrong.copy(
                            fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                        ),
                        color = if (active) colors.text else colors.textMuted,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
