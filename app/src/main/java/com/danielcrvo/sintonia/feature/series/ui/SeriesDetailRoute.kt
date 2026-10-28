package com.danielcrvo.sintonia.feature.series.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.danielcrvo.sintonia.core.model.PlayerRequest

@Composable
fun SeriesDetailRoute(
    seriesId: Long,
    onBack: () -> Unit,
    onOpenPlayer: (PlayerRequest) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SeriesDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(seriesId) { viewModel.start(seriesId) }
    SeriesDetailScreen(
        state = state,
        onBack = onBack,
        onPlayEpisode = { onOpenPlayer(PlayerRequest.Episode(seriesId, it.externalId)) },
        onPlaySingle = { onOpenPlayer(PlayerRequest.SeriesItem(seriesId)) },
        onSeasonSelected = viewModel::onSeasonSelected,
        onRetryInfo = viewModel::retryInfo,
        modifier = modifier,
    )
}
