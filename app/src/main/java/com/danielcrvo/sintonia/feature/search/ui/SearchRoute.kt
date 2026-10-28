package com.danielcrvo.sintonia.feature.search.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.danielcrvo.sintonia.core.model.PlayerRequest

@Composable
fun SearchRoute(
    onBack: () -> Unit,
    onOpenPlayer: (PlayerRequest) -> Unit,
    onOpenMovie: (Long) -> Unit,
    onOpenSeries: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val typed by viewModel.typed.collectAsStateWithLifecycle()
    SearchScreen(
        typed = typed,
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onCancel = onBack,
        onChannelClick = { onOpenPlayer(PlayerRequest.Live(it.id)) },
        onMovieClick = { onOpenMovie(it.id) },
        onSeriesClick = { onOpenSeries(it.id) },
        modifier = modifier,
    )
}
