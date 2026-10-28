package com.danielcrvo.sintonia.feature.live.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import com.danielcrvo.sintonia.R
import com.danielcrvo.sintonia.core.common.formatClock
import com.danielcrvo.sintonia.core.data.ChannelSort
import com.danielcrvo.sintonia.core.designsystem.components.ButtonSize
import com.danielcrvo.sintonia.core.designsystem.components.SBadge
import com.danielcrvo.sintonia.core.designsystem.components.SButton
import com.danielcrvo.sintonia.core.designsystem.components.SIconButton
import com.danielcrvo.sintonia.core.designsystem.components.SIcons
import com.danielcrvo.sintonia.core.designsystem.components.SImage
import com.danielcrvo.sintonia.core.designsystem.components.SOptionRow
import com.danielcrvo.sintonia.core.designsystem.components.SOverlay
import com.danielcrvo.sintonia.core.designsystem.components.SProgressBar
import com.danielcrvo.sintonia.core.designsystem.components.SText
import com.danielcrvo.sintonia.core.designsystem.components.sintoniaFocusable
import com.danielcrvo.sintonia.core.designsystem.theme.DeviceClass
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme
import com.danielcrvo.sintonia.core.model.Category
import com.danielcrvo.sintonia.core.model.Channel
import com.danielcrvo.sintonia.core.model.Program

@Composable
fun LiveFilterSheet(
    visible: Boolean,
    sort: ChannelSort,
    categories: List<Category>,
    selectedCategoryId: String?,
    onSortSelected: (ChannelSort) -> Unit,
    onCategorySelected: (Category?) -> Unit,
    onDismiss: () -> Unit,
) {
    SOverlay(visible = visible, onDismiss = onDismiss, title = stringResource(R.string.live_filter), scrollable = false) {
        LiveFilterContent(sort, categories, selectedCategoryId, onSortSelected, onCategorySelected, onDismiss)
    }
}

@Composable
fun ColumnScope.LiveFilterContent(
    sort: ChannelSort,
    categories: List<Category>,
    selectedCategoryId: String?,
    onSortSelected: (ChannelSort) -> Unit,
    onCategorySelected: (Category?) -> Unit,
    onDismiss: () -> Unit,
) {
    run {
        SText(
            stringResource(R.string.filter_sort_by).uppercase(),
            style = SintoniaTheme.typography.monoLabel,
            color = SintoniaTheme.colors.textMuted,
        )
        SOptionRow(stringResource(R.string.sort_number), sort == ChannelSort.Number, { onSortSelected(ChannelSort.Number) })
        SOptionRow(stringResource(R.string.sort_name), sort == ChannelSort.Name, { onSortSelected(ChannelSort.Name) })
        SText(
            stringResource(R.string.filter_category).uppercase(),
            style = SintoniaTheme.typography.monoLabel,
            color = SintoniaTheme.colors.textMuted,
            modifier = Modifier.padding(top = SintoniaTheme.spacing.sm),
        )
        LazyColumn(Modifier.heightIn(max = SintoniaTheme.sizes.sheetListMaxHeight), verticalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.xs)) {
            item(key = "all") {
                SOptionRow(stringResource(R.string.filter_all_categories), selectedCategoryId == null, { onCategorySelected(null) })
            }
            items(categories, key = { it.externalId }) { category ->
                SOptionRow(
                    label = category.name,
                    selected = category.externalId == selectedCategoryId,
                    onClick = { onCategorySelected(category) },
                    trailingText = category.count.toString(),
                    locked = category.locked,
                )
            }
        }
        SButton(
            text = stringResource(R.string.action_done),
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().padding(top = SintoniaTheme.spacing.sm),
            size = ButtonSize.Footer,
        )
    }
}

@Composable
fun ChannelProgramsSheet(
    visible: Boolean,
    channel: Channel?,
    programs: List<Program>,
    favorite: Boolean,
    nowMillis: Long,
    onToggleFavorite: () -> Unit,
    onWatch: () -> Unit,
    onDismiss: () -> Unit,
) {
    SOverlay(visible = visible, onDismiss = onDismiss, scrollable = false) {
        ChannelProgramsContent(channel, programs, favorite, nowMillis, onToggleFavorite, onWatch)
    }
}

@Composable
fun ColumnScope.ChannelProgramsContent(
    channel: Channel?,
    programs: List<Program>,
    favorite: Boolean,
    nowMillis: Long,
    onToggleFavorite: () -> Unit,
    onWatch: () -> Unit,
) {
    run {
        val colors = SintoniaTheme.colors
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.md),
        ) {
            SImage(channel?.logoUrl, null, Modifier.size(SintoniaTheme.sizes.avatar).clip(CircleShape))
            Column(Modifier.weight(1f)) {
                SText(channel?.name.orEmpty(), style = SintoniaTheme.typography.subheading, maxLines = 1)
                SText(stringResource(R.string.program_today), style = SintoniaTheme.typography.mono, color = colors.textMuted)
            }
            SIconButton(
                icon = if (favorite) SIcons.Star else SIcons.StarOutline,
                contentDescription = stringResource(if (favorite) R.string.channel_unfavorite else R.string.channel_favorite),
                onClick = onToggleFavorite,
            )
        }
        if (programs.isEmpty()) {
            SText(
                stringResource(R.string.program_empty),
                color = colors.textSecondary,
                modifier = Modifier.padding(vertical = SintoniaTheme.spacing.lg),
            )
        } else {
            LazyColumn(Modifier.heightIn(max = SintoniaTheme.sizes.programsListMaxHeight)) {
                items(programs, key = { "${it.startMillis}-${it.stopMillis}" }) { program ->
                    ProgramRow(program, nowMillis)
                }
            }
        }
        SButton(
            text = stringResource(R.string.program_watch),
            onClick = onWatch,
            modifier = Modifier.fillMaxWidth().padding(top = SintoniaTheme.spacing.sm),
            size = ButtonSize.Footer,
        )
    }
}

@Composable
private fun ProgramRow(program: Program, nowMillis: Long) {
    val colors = SintoniaTheme.colors
    val live = program.isLiveAt(nowMillis)
    val start = remember(program.startMillis) { formatClock(program.startMillis) }
    val tv = SintoniaTheme.deviceClass == DeviceClass.Tv
    Column(
        // On TV each row is focusable: that is how the D-pad scrolls the schedule.
        Modifier
            .fillMaxWidth()
            .then(
                if (tv) Modifier.sintoniaFocusable(SintoniaTheme.radii.control) else Modifier,
            ).padding(vertical = SintoniaTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.xs),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.md), verticalAlignment = Alignment.CenterVertically) {
            SText(
                start,
                style = SintoniaTheme.typography.mono,
                color = if (live) colors.text else colors.textMuted,
                modifier = Modifier.width(SintoniaTheme.sizes.programTimeWidth),
            )
            SText(
                program.title,
                style = if (live) SintoniaTheme.typography.bodyStrong else SintoniaTheme.typography.body,
                color = if (live) colors.text else colors.textSecondary,
                modifier = Modifier.weight(1f, fill = false),
                maxLines = 2,
            )
            if (live) SBadge(stringResource(R.string.program_now))
        }
        if (live) SProgressBar(progress = { program.progressAt(nowMillis) })
    }
}
