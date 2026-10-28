package com.danielcrvo.sintonia.core.data

import androidx.compose.runtime.Immutable
import com.danielcrvo.sintonia.core.data.parser.escapeLike
import com.danielcrvo.sintonia.core.data.parser.nameKey
import com.danielcrvo.sintonia.core.database.SintoniaDatabase
import com.danielcrvo.sintonia.core.model.Channel
import com.danielcrvo.sintonia.core.model.Movie
import com.danielcrvo.sintonia.core.model.Series
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Immutable
data class SearchResults(
    val channels: List<Channel> = emptyList(),
    val movies: List<Movie> = emptyList(),
    val series: List<Series> = emptyList(),
) {
    val isEmpty: Boolean get() = channels.isEmpty() && movies.isEmpty() && series.isEmpty()
}

@Immutable
data class HiddenCategories(
    val live: List<String> = emptyList(),
    val movies: List<String> = emptyList(),
    val series: List<String> = emptyList(),
)

interface SearchRepository {
    suspend fun search(listId: Long, query: String, hidden: HiddenCategories, limitPerKind: Int): SearchResults
}

@Singleton
class RoomSearchRepository
    @Inject
    constructor(private val db: SintoniaDatabase) : SearchRepository {
        override suspend fun search(listId: Long, query: String, hidden: HiddenCategories, limitPerKind: Int): SearchResults {
            val key = escapeLike(nameKey(query.trim()))
            if (key.isEmpty()) return SearchResults()
            return coroutineScope {
                val channels = async { db.channels().search(listId, key, hidden.live, limitPerKind).map { it.toModel() } }
                val movies = async { db.movies().search(listId, key, hidden.movies, limitPerKind).map { it.toModel() } }
                val series = async { db.series().search(listId, key, hidden.series, limitPerKind).map { it.toModel() } }
                SearchResults(channels.await(), movies.await(), series.await())
            }
        }
    }
