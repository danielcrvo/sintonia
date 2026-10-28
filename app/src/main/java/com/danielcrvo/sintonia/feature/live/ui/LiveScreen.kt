package com.danielcrvo.sintonia.feature.live.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.danielcrvo.sintonia.R
import com.danielcrvo.sintonia.core.common.formatCount
import com.danielcrvo.sintonia.core.designsystem.components.ButtonVariant
import com.danielcrvo.sintonia.core.designsystem.components.ChannelRowData
import com.danielcrvo.sintonia.core.designsystem.components.SButton
import com.danielcrvo.sintonia.core.designsystem.components.SChannelRow
import com.danielcrvo.sintonia.core.designsystem.components.SEmptyState
import com.danielcrvo.sintonia.core.designsystem.components.SIcon
import com.danielcrvo.sintonia.core.designsystem.components.SIcons
import com.danielcrvo.sintonia.core.designsystem.components.SSpinner
import com.danielcrvo.sintonia.core.designsystem.components.STabs
import com.danielcrvo.sintonia.core.designsystem.components.SText
import com.danielcrvo.sintonia.core.designsystem.components.sintoniaClickable
import com.danielcrvo.sintonia.core.designsystem.components.sintoniaItemMotion
import com.danielcrvo.sintonia.core.designsystem.theme.DeviceClass
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme
import com.danielcrvo.sintonia.core.model.Category
import com.danielcrvo.sintonia.core.model.Channel
import com.danielcrvo.sintonia.core.model.Program

@Composable
fun LiveScreen(
    state: LiveUiState,
    channels: LazyPagingItems<Channel>,
    modifier: Modifier = Modifier,
    count: Int = channels.itemCount,
    programs: State<Map<String, Program>>,
    nowMillis: Long,
    onTabSelected: (LiveTab) -> Unit,
    onCategorySelected: (Category) -> Unit,
    onFilterClick: () -> Unit,
    onChannelClick: (Channel) -> Unit,
    onChannelLongClick: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    onRequestProgram: (String) -> Unit,
    guide: State<Map<String, List<Program>>>,
    onGuideWindow: (Long) -> Unit,
    onRequestGuide: (String) -> Unit,
) {
    val colors = SintoniaTheme.colors
    val allLabel = stringResource(R.string.live_tab_all)
    val favoritesLabel = stringResource(R.string.live_tab_favorites)
    val categories = remember(state.categories) { state.categories.filter { it.count > 0 || it.name != it.originalName } }
    val labels = remember(categories, allLabel, favoritesLabel) {
        listOf(allLabel, favoritesLabel) + categories.map { it.name }
    }
    val selectedIndex = when (state.tab) {
        LiveTab.All -> 0
        LiveTab.Favorites -> 1
        LiveTab.Category -> 2 + categories.indexOfFirst { it.externalId == state.selectedCategory?.externalId }.coerceAtLeast(0)
    }
    Column(modifier.fillMaxSize().background(colors.background)) {
        STabs(
            tabs = labels,
            selectedIndex = selectedIndex,
            onSelected = { index ->
                when (index) {
                    0 -> onTabSelected(LiveTab.All)
                    1 -> onTabSelected(LiveTab.Favorites)
                    else -> onCategorySelected(categories[index - 2])
                }
            },
        )
        FilterBar(count = count, onFilterClick = onFilterClick)

        Box(Modifier.weight(1f).fillMaxWidth()) {
            val refresh = channels.loadState.refresh
            when {
                refresh is LoadState.Loading && channels.itemCount == 0 -> Box(
                    Modifier.fillMaxSize(),
                    Alignment.Center,
                ) { SSpinner(size = SintoniaTheme.sizes.spinnerList) }
                refresh is LoadState.Error && channels.itemCount == 0 -> SEmptyState(
                    icon = SIcons.AlertCircle,
                    title = stringResource(R.string.load_error_title),
                    action = {
                        SButton(stringResource(R.string.error_retry), { channels.retry() }, variant = ButtonVariant.Secondary)
                    },
                )
                channels.itemCount == 0 && state.tab == LiveTab.Favorites -> SEmptyState(
                    icon = SIcons.StarOutline,
                    title = stringResource(R.string.live_empty_favorites_title),
                    message = stringResource(R.string.live_empty_favorites_message),
                    action = {
                        SButton(stringResource(R.string.live_see_all), { onTabSelected(LiveTab.All) }, variant = ButtonVariant.Secondary)
                    },
                )
                channels.itemCount == 0 -> SEmptyState(
                    icon = SIcons.Tv,
                    title = stringResource(R.string.live_empty_title),
                    message = stringResource(R.string.live_empty_message),
                )
                SintoniaTheme.deviceClass == DeviceClass.Tv -> TvGuide(
                    channels = channels,
                    state = state,
                    guide = guide,
                    nowMillis = nowMillis,
                    onChannelClick = onChannelClick,
                    onChannelLongClick = onChannelLongClick,
                    onGuideWindow = onGuideWindow,
                    onRequestGuide = onRequestGuide,
                )
                else -> ChannelList(
                    channels,
                    state,
                    programs,
                    nowMillis,
                    onChannelClick,
                    onChannelLongClick,
                    onToggleFavorite,
                    onRequestProgram,
                )
            }
        }
    }
}

@Composable
private fun ChannelList(
    channels: LazyPagingItems<Channel>,
    state: LiveUiState,
    programs: State<Map<String, Program>>,
    nowMillis: Long,
    onChannelClick: (Channel) -> Unit,
    onChannelLongClick: (Channel) -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    onRequestProgram: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().focusRestorer()) {
        items(
            count = channels.itemCount,
            key = channels.itemKey { it.id },
            contentType = channels.itemContentType { "channel" },
        ) { index ->
            val channel = channels[index] ?: return@items
            val epgId = channel.epgId
            if (epgId != null) {
                androidx.compose.runtime.LaunchedEffect(epgId) { onRequestProgram(epgId) }
            }
            // The program is read in here so only visible rows recompose when the guide updates.
            val program = epgId?.let { programs.value[it] }
            val data = remember(channel, program, nowMillis) {
                ChannelRowData(
                    number = channel.number?.toString().orEmpty(),
                    name = channel.name,
                    logoUrl = channel.logoUrl,
                    program = program?.title,
                    progress = program?.progressAt(nowMillis),
                )
            }
            SChannelRow(
                modifier = sintoniaItemMotion(),
                data = data,
                favorite = channel.externalId in state.favoriteIds,
                playing = channel.id == state.playingChannelId,
                onClick = { onChannelClick(channel) },
                onLongClick = { onChannelLongClick(channel) },
                onToggleFavorite = { onToggleFavorite(channel) },
            )
        }
    }
}

@Composable
private fun FilterBar(count: Int, onFilterClick: () -> Unit) {
    val colors = SintoniaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SintoniaTheme.spacing.gutter, vertical = SintoniaTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        SText(
            text = pluralStringResource(R.plurals.live_channel_count, count, formatCount(count)),
            style = SintoniaTheme.typography.mono,
            color = colors.textMuted,
        )
        Row(
            modifier = Modifier
                .sintoniaClickable(onClick = onFilterClick, cornerRadius = SintoniaTheme.radii.control)
                .padding(horizontal = SintoniaTheme.spacing.sm)
                .padding(vertical = SintoniaTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.sm),
        ) {
            SIcon(SIcons.Options, contentDescription = null, tint = colors.textSecondary, size = SintoniaTheme.sizes.iconSmall)
            SText(stringResource(R.string.live_filter), style = SintoniaTheme.typography.label, color = colors.textSecondary)
        }
    }
}
