package com.danielcrvo.sintonia.feature.movies.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.danielcrvo.sintonia.core.model.PlayerRequest

@Composable
fun MovieDetailRoute(
    movieId: Long,
    onBack: () -> Unit,
    onOpenPlayer: (PlayerRequest) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MovieDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(movieId) { viewModel.start(movieId) }
    MovieDetailScreen(
        state = state,
        onBack = onBack,
        onPlay = { onOpenPlayer(PlayerRequest.Movie(movieId)) },
        onRetryInfo = viewModel::retryInfo,
        modifier = modifier,
    )
}
