package com.danielcrvo.sintonia.feature.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.danielcrvo.sintonia.core.model.PlayerRequest

@Composable
fun HomeRoute(
    onOpenMovie: (Long) -> Unit,
    onOpenPlayer: (PlayerRequest) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onFeatured = { onOpenMovie(it.movieId) },
        onContinue = { onOpenPlayer(it.request) },
        onChannel = { onOpenPlayer(PlayerRequest.Live(it.id)) },
        modifier = modifier,
    )
}
