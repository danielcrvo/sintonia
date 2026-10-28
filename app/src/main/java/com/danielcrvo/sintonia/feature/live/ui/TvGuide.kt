package com.danielcrvo.sintonia.feature.live.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.danielcrvo.sintonia.R
import com.danielcrvo.sintonia.core.common.formatClock
import com.danielcrvo.sintonia.core.designsystem.components.GuideMetrics
import com.danielcrvo.sintonia.core.designsystem.components.GuideProgram
import com.danielcrvo.sintonia.core.designsystem.components.SGuideRow
import com.danielcrvo.sintonia.core.designsystem.components.SGuideRuler
import com.danielcrvo.sintonia.core.designsystem.components.SImage
import com.danielcrvo.sintonia.core.designsystem.components.SProgressBar
import com.danielcrvo.sintonia.core.designsystem.components.SText
import com.danielcrvo.sintonia.core.designsystem.components.sintoniaClickable
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme
import com.danielcrvo.sintonia.core.model.Channel
import com.danielcrvo.sintonia.core.model.Program

@Composable
fun TvGuide(
    channels: LazyPagingItems<Channel>,
    state: LiveUiState,
    guide: State<Map<String, List<Program>>>,
    nowMillis: Long,
    onChannelClick: (Channel) -> Unit,
    onChannelLongClick: (Channel) -> Unit,
    onGuideWindow: (Long) -> Unit,
    onRequestGuide: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SintoniaTheme.colors
    val spacing = SintoniaTheme.spacing
    // The window starts 30 min before now, aligned to the half hour.
    val windowStart = remember(nowMillis / GuideMetrics.SlotMillis) {
        (nowMillis / GuideMetrics.SlotMillis) * GuideMetrics.SlotMillis - GuideMetrics.SlotMillis
    }
    LaunchedEffect(windowStart) { onGuideWindow(windowStart) }
    val scroll = rememberScrollState()
    var focused by remember { mutableStateOf<Channel?>(null) }

    Column(modifier.fillMaxSize()) {
        GuidePreview(focused ?: channels.peekFirst(), guide, nowMillis, Modifier.fillMaxWidth().padding(horizontal = spacing.gutter, vertical = spacing.md))
        SGuideRuler(windowStart, GuideHours, scroll, label = { formatClock(it) })
        LazyColumn(state = rememberLazyListState(), modifier = Modifier.fillMaxSize().focusRestorer()) {
            items(
                count = channels.itemCount,
                key = channels.itemKey { it.id },
                contentType = channels.itemContentType { "guide-row" },
            ) { index ->
                val channel = channels[index] ?: return@items
                val epgId = channel.epgId
                if (epgId != null) LaunchedEffect(epgId) { onRequestGuide(epgId) }
                val programs = remember(epgId, guide.value[epgId]) {
                    guide.value[epgId].orEmpty().map { GuideProgram(it.startMillis, it.stopMillis, it.title) }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(GuideMetrics.RowHeight)
                        .onFocusChanged { if (it.hasFocus) focused = channel },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ChannelCell(
                        channel,
                        playing = channel.id == state.playingChannelId,
                        onClick = { onChannelClick(channel) },
                        onLongClick = { onChannelLongClick(channel) },
                    )
                    SGuideRow(
                        programs = programs,
                        windowStartMillis = windowStart,
                        hours = GuideHours,
                        nowMillis = nowMillis,
                        scroll = scroll,
                        emptyLabel = stringResource(if (epgId == null) R.string.program_unknown else R.string.program_empty),
                        onClick = { onChannelClick(channel) },
                        onLongClick = { onChannelLongClick(channel) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private fun LazyPagingItems<Channel>.peekFirst(): Channel? = if (itemCount > 0) peek(0) else null

@Composable
private fun ChannelCell(channel: Channel, playing: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val colors = SintoniaTheme.colors
    val spacing = SintoniaTheme.spacing
    val playingLabel = stringResource(R.string.badge_playing)
    Row(
        Modifier
            .width(GuideMetrics.ChannelColumn)
            .height(GuideMetrics.RowHeight)
            .padding(start = spacing.gutter, end = spacing.sm)
            .sintoniaClickable(onClick = onClick, onLongClick = onLongClick, cornerRadius = SintoniaTheme.radii.control)
            .semantics { if (playing) stateDescription = playingLabel }
            .background(if (playing) colors.surfaceAlt else Color.Transparent, RoundedCornerShape(SintoniaTheme.radii.control))
            .padding(horizontal = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        SText(
            channel.number?.toString().orEmpty(),
            style = SintoniaTheme.typography.mono,
            color = colors.textMuted,
            maxLines = 1,
            modifier = Modifier.width(SintoniaTheme.sizes.guideNumberWidth),
        )
        Box(Modifier.size(SintoniaTheme.sizes.avatar).clip(CircleShape).border(SintoniaTheme.strokes.hairline, colors.border, CircleShape)) {
            SImage(channel.logoUrl, null, Modifier.fillMaxSize())
        }
        SText(channel.name, style = SintoniaTheme.typography.bodyLarge, maxLines = 1, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun GuidePreview(channel: Channel?, guide: State<Map<String, List<Program>>>, nowMillis: Long, modifier: Modifier = Modifier) {
    val colors = SintoniaTheme.colors
    val spacing = SintoniaTheme.spacing
    val now = channel?.epgId?.let { id -> guide.value[id]?.firstOrNull { it.isLiveAt(nowMillis) } }
    Row(
        modifier.height(GuideMetrics.RowHeight * 1.6f),
        horizontalArrangement = Arrangement.spacedBy(spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(GuideMetrics.RowHeight).clip(CircleShape).border(SintoniaTheme.strokes.hairline, colors.border, CircleShape)) {
            SImage(channel?.logoUrl, null, Modifier.fillMaxSize())
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            SText(channel?.name.orEmpty(), style = SintoniaTheme.typography.heading, maxLines = 1)
            if (now != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.md), verticalAlignment = Alignment.CenterVertically) {
                    SText(now.title, style = SintoniaTheme.typography.bodyStrong, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                    SText(
                        "${formatClock(now.startMillis)} – ${formatClock(now.stopMillis)}",
                        style = SintoniaTheme.typography.mono,
                        color = colors.textMuted,
                        maxLines = 1,
                    )
                }
                SProgressBar(progress = { now.progressAt(nowMillis) }, modifier = Modifier.fillMaxWidth(0.5f))
                now.description?.let { SText(it, style = SintoniaTheme.typography.caption, color = colors.textSecondary, maxLines = 2) }
            } else {
                SText(stringResource(R.string.program_unknown), style = SintoniaTheme.typography.caption, color = colors.textMuted, maxLines = 1)
            }
        }
    }
}
