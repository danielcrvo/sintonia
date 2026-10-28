package com.danielcrvo.sintonia.feature.movies.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danielcrvo.sintonia.core.data.DetailsRepository
import com.danielcrvo.sintonia.core.data.ListsRepository
import com.danielcrvo.sintonia.core.data.MovieRepository
import com.danielcrvo.sintonia.core.data.ProgressRepository
import com.danielcrvo.sintonia.core.model.Movie
import com.danielcrvo.sintonia.core.model.ProgressKind
import com.danielcrvo.sintonia.core.model.VodInfo
import com.danielcrvo.sintonia.core.model.WatchProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface InfoState {
    data object Loading : InfoState
    data class Loaded(val info: VodInfo) : InfoState
    data object Unavailable : InfoState
    data object Failed : InfoState
}

@Immutable
data class MovieDetailUiState(
    val loading: Boolean = true,
    val movie: Movie? = null,
    val info: InfoState = InfoState.Loading,
    val progress: WatchProgress? = null,
)

@HiltViewModel
class MovieDetailViewModel
    @Inject
    constructor(
        private val lists: ListsRepository,
        private val movies: MovieRepository,
        private val details: DetailsRepository,
        private val progress: ProgressRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow(MovieDetailUiState())
        val state: StateFlow<MovieDetailUiState> = _state.asStateFlow()
        private var movieId: Long? = null

        fun start(movieId: Long) {
            if (this.movieId == movieId) return
            this.movieId = movieId
            viewModelScope.launch {
                val list = lists.activeList.first()
                val movie = movies.movie(movieId)
                if (list == null || movie == null) {
                    _state.update { it.copy(loading = false, movie = null) }
                    return@launch
                }
                _state.update { it.copy(loading = false, movie = movie) }
                launch { progress.observe(list.id, ProgressKind.Movie, movie.externalId).collect { p -> _state.update { it.copy(progress = p) } } }
                loadInfo()
            }
        }

        fun retryInfo() {
            viewModelScope.launch { loadInfo() }
        }

        private suspend fun loadInfo() {
            val movie = _state.value.movie ?: return
            val list = lists.activeList.first() ?: return
            _state.update { it.copy(info = InfoState.Loading) }
            val result = runCatching { details.movieInfo(list, movie.externalId) }
            _state.update {
                it.copy(
                    info = result.fold(
                        onSuccess = { info -> if (info == null) InfoState.Unavailable else InfoState.Loaded(info) },
                        onFailure = { InfoState.Failed },
                    ),
                )
            }
        }
    }
