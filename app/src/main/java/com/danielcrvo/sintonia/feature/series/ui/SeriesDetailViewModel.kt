package com.danielcrvo.sintonia.feature.series.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danielcrvo.sintonia.core.data.DetailsRepository
import com.danielcrvo.sintonia.core.data.ListsRepository
import com.danielcrvo.sintonia.core.data.ProgressRepository
import com.danielcrvo.sintonia.core.data.SeriesRepository
import com.danielcrvo.sintonia.core.model.Episode
import com.danielcrvo.sintonia.core.model.EpisodeOrder
import com.danielcrvo.sintonia.core.model.Series
import com.danielcrvo.sintonia.core.model.SeriesInfo
import com.danielcrvo.sintonia.core.model.WatchProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SeriesInfoState {
    data object Loading : SeriesInfoState
    data class Loaded(val info: SeriesInfo) : SeriesInfoState

    data object Unavailable : SeriesInfoState
    data object Failed : SeriesInfoState
}

@Immutable
data class SeriesDetailUiState(
    val loading: Boolean = true,
    val series: Series? = null,
    val info: SeriesInfoState = SeriesInfoState.Loading,
    val selectedSeason: Int = 0,
    val progress: Map<String, WatchProgress> = emptyMap(),
) {
    val loadedInfo: SeriesInfo? get() = (info as? SeriesInfoState.Loaded)?.info

    val resumeEpisode: Episode?
        get() = loadedInfo?.let { EpisodeOrder.resume(it.seasons, progress.values.toList()) }
}

@HiltViewModel
class SeriesDetailViewModel
    @Inject
    constructor(
        private val lists: ListsRepository,
        private val seriesRepo: SeriesRepository,
        private val details: DetailsRepository,
        private val progress: ProgressRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow(SeriesDetailUiState())
        val state: StateFlow<SeriesDetailUiState> = _state.asStateFlow()
        private var seriesId: Long? = null

        fun start(seriesId: Long) {
            if (this.seriesId == seriesId) return
            this.seriesId = seriesId
            viewModelScope.launch {
                val list = lists.activeList.first()
                val series = seriesRepo.series(seriesId)
                if (list == null || series == null) {
                    _state.update { it.copy(loading = false, series = null) }
                    return@launch
                }
                _state.update { it.copy(loading = false, series = series) }
                launch {
                    progress.observeSeries(list.id, series.externalId).collect { rows ->
                        _state.update { it.copy(progress = rows.associateBy { p -> p.externalId }) }
                    }
                }
                loadInfo()
            }
        }

        fun retryInfo() {
            viewModelScope.launch { loadInfo() }
        }

        fun onSeasonSelected(index: Int) {
            _state.update { it.copy(selectedSeason = index) }
        }

        private suspend fun loadInfo() {
            val series = _state.value.series ?: return
            val list = lists.activeList.first() ?: return
            _state.update { it.copy(info = SeriesInfoState.Loading) }
            val result = runCatching { details.seriesInfo(list, series.externalId) }
            _state.update { current ->
                val next = result.fold(
                    onSuccess = { info -> if (info == null) SeriesInfoState.Unavailable else SeriesInfoState.Loaded(info) },
                    onFailure = { SeriesInfoState.Failed },
                )
                current.copy(info = next)
            }
            val resume = _state.value.resumeEpisode
            val info = _state.value.loadedInfo
            if (resume != null && info != null) {
                val index = info.seasons.indexOfFirst { it.number == resume.season }
                if (index >= 0) _state.update { it.copy(selectedSeason = index) }
            }
        }
    }
