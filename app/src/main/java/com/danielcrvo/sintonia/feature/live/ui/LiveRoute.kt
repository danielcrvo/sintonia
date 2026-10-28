package com.danielcrvo.sintonia.feature.live.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.danielcrvo.sintonia.R
import com.danielcrvo.sintonia.core.common.rememberNowMillis
import com.danielcrvo.sintonia.core.designsystem.components.LocalToast
import com.danielcrvo.sintonia.core.model.Channel
import com.danielcrvo.sintonia.core.model.LiveScope
import com.danielcrvo.sintonia.core.model.PlayerRequest
import com.danielcrvo.sintonia.core.parental.ui.PinPromptSheet

@Composable
fun LiveRoute(
    onOpenPlayer: (PlayerRequest) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LiveViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val channels = viewModel.channels.collectAsLazyPagingItems()
    val channelCount by viewModel.channelCount.collectAsStateWithLifecycle(initialValue = 0)
    val programs = viewModel.programs.collectAsStateWithLifecycle()
    val guide = viewModel.guide.collectAsStateWithLifecycle()
    val now by rememberNowMillis()
    val toast = LocalToast.current
    val resources = LocalResources.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is LiveEffect.FavoriteChanged -> toast.show(
                    resources.getString(
                        if (effect.added) R.string.toast_favorite_added else R.string.toast_favorite_removed,
                        effect.channelName,
                    ),
                )
            }
        }
    }

    fun open(channel: Channel) {
        val scope = when (state.tab) {
            LiveTab.Favorites -> LiveScope(LiveScope.Kind.Favorites)
            LiveTab.Category -> LiveScope(LiveScope.Kind.Category, state.selectedCategory?.externalId)
            LiveTab.All -> LiveScope(LiveScope.Kind.All)
        }
        onOpenPlayer(PlayerRequest.Live(channel.id, scope))
    }

    LiveScreen(
        state = state,
        channels = channels,
        count = channelCount,
        programs = programs,
        nowMillis = now,
        onTabSelected = viewModel::onTabSelected,
        onCategorySelected = viewModel::onCategorySelected,
        onFilterClick = viewModel::onFilterClick,
        onChannelClick = ::open,
        onChannelLongClick = viewModel::onChannelLongClick,
        onToggleFavorite = viewModel::onToggleFavorite,
        onRequestProgram = viewModel::requestProgram,
        guide = guide,
        onGuideWindow = viewModel::onGuideWindow,
        onRequestGuide = viewModel::requestGuide,
        modifier = modifier,
    )

    val pin by viewModel.pinState.collectAsStateWithLifecycle()
    PinPromptSheet(pin, viewModel::onPinDigit, viewModel::onPinBackspace, viewModel::onPinDismiss)

    LiveFilterSheet(
        visible = state.sheet == LiveSheet.Filter,
        sort = state.sort,
        categories = state.categories,
        selectedCategoryId = state.selectedCategory?.externalId,
        onSortSelected = viewModel::onSortSelected,
        onCategorySelected = viewModel::onCategorySelected,
        onDismiss = viewModel::onSheetDismiss,
    )

    val programsSheet = state.sheet as? LiveSheet.Programs
    ChannelProgramsSheet(
        visible = programsSheet != null,
        channel = programsSheet?.channel,
        programs = state.todayPrograms,
        favorite = programsSheet?.channel?.externalId in state.favoriteIds,
        nowMillis = now,
        onToggleFavorite = { programsSheet?.channel?.let(viewModel::onToggleFavorite) },
        onWatch = {
            programsSheet?.channel?.let { channel ->
                viewModel.onSheetDismiss()
                open(channel)
            }
        },
        onDismiss = viewModel::onSheetDismiss,
    )
}
