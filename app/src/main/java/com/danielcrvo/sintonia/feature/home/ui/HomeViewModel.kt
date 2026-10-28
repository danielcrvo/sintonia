package com.danielcrvo.sintonia.feature.home.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danielcrvo.sintonia.core.data.ListsRepository
import com.danielcrvo.sintonia.core.data.LiveRepository
import com.danielcrvo.sintonia.core.data.MovieRepository
import com.danielcrvo.sintonia.core.data.ParentalRepository
import com.danielcrvo.sintonia.core.data.ProgressRepository
import com.danielcrvo.sintonia.core.data.SeriesRepository
import com.danielcrvo.sintonia.core.model.Channel
import com.danielcrvo.sintonia.core.model.ContentKind
import com.danielcrvo.sintonia.core.model.PlayerRequest
import com.danielcrvo.sintonia.core.model.ProgressKind
import com.danielcrvo.sintonia.core.model.WatchProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@Immutable
data class FeaturedItem(val movieId: Long, val title: String, val meta: String?, val imageUrl: String?)

@Immutable
data class ContinueItem(
    val key: String,
    val title: String,
    val season: Int?,
    val episode: Int?,
    val imageUrl: String?,
    val fraction: Float,
    val request: PlayerRequest,
)

@Immutable
data class HomeUiState(
    val loaded: Boolean = false,
    val featured: FeaturedItem? = null,
    val continueWatching: ImmutableList<ContinueItem> = persistentListOf(),
    val favorites: ImmutableList<Channel> = persistentListOf(),
) {
    val isEmpty: Boolean get() = loaded && featured == null && continueWatching.isEmpty() && favorites.isEmpty()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        lists: ListsRepository,
        parental: ParentalRepository,
        live: LiveRepository,
        private val movies: MovieRepository,
        private val series: SeriesRepository,
        progress: ProgressRepository,
    ) : ViewModel() {
        private data class Hidden(val live: List<String>, val movies: List<String>, val series: List<String>)

        val state: StateFlow<HomeUiState> = lists.activeList
            .filterNotNull()
            .flatMapLatest { list ->
                combine(
                    parental.blockedCategories(list.id, ContentKind.Live),
                    parental.blockedCategories(list.id, ContentKind.Movie),
                    parental.blockedCategories(list.id, ContentKind.Series),
                ) { l, m, s -> Hidden(l, m, s) }.flatMapLatest { hidden ->
                    combine(
                        progress.observeContinueWatching(list.id, ContinueLimit),
                        live.recentFavorites(list.id, hidden.live, FavoritesLimit),
                        movies.featured(list.id, hidden.movies, 1),
                    ) { watching, favorites, featured ->
                        HomeUiState(
                            loaded = true,
                            featured = featured.firstOrNull()?.let { FeaturedItem(it.id, it.name, it.year, it.posterUrl) },
                            continueWatching = watching.mapNotNull { resolve(list.id, it, hidden) }.toImmutableList(),
                            favorites = favorites.toImmutableList(),
                        )
                    }
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), HomeUiState())

        private suspend fun resolve(listId: Long, item: WatchProgress, hidden: Hidden): ContinueItem? {
            val request: PlayerRequest = when (item.kind) {
                ProgressKind.Movie -> {
                    val movie = movies.movieByExternalId(listId, item.externalId)
                    if (movie != null) {
                        if (movie.categoryId in hidden.movies) return null
                        PlayerRequest.Movie(movie.id)
                    } else {
                        val single = series.seriesByExternalId(listId, item.externalId) ?: return null
                        if (single.categoryId in hidden.series) return null
                        PlayerRequest.SeriesItem(single.id)
                    }
                }
                ProgressKind.Episode -> {
                    val parent = item.seriesId?.let { series.seriesByExternalId(listId, it) } ?: return null
                    if (parent.categoryId in hidden.series) return null
                    PlayerRequest.Episode(parent.id, item.externalId)
                }
            }
            val isEpisode = item.kind == ProgressKind.Episode
            return ContinueItem(
                key = "${item.kind}:${item.externalId}",
                title = item.title.orEmpty(),
                season = item.season.takeIf { isEpisode },
                episode = item.episode.takeIf { isEpisode },
                imageUrl = item.posterUrl,
                fraction = item.fraction,
                request = request,
            )
        }

        private companion object {
            const val ContinueLimit = 12
            const val FavoritesLimit = 12
            const val StopTimeoutMillis = 5_000L
        }
    }
