package com.danielcrvo.sintonia.core.data

import com.danielcrvo.sintonia.core.database.ProgressEntity
import com.danielcrvo.sintonia.core.database.SintoniaDatabase
import com.danielcrvo.sintonia.core.model.ProgressKind
import com.danielcrvo.sintonia.core.model.WatchProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface ProgressRepository {
    suspend fun save(listId: Long, progress: WatchProgress)
    suspend fun get(listId: Long, kind: ProgressKind, externalId: String): WatchProgress?
    fun observe(listId: Long, kind: ProgressKind, externalId: String): Flow<WatchProgress?>

    fun observeSeries(listId: Long, seriesId: String): Flow<List<WatchProgress>>

    fun observeContinueWatching(listId: Long, limit: Int): Flow<List<WatchProgress>>
}

@Singleton
class RoomProgressRepository
    @Inject
    constructor(private val db: SintoniaDatabase) : ProgressRepository {
        private val dao get() = db.progress()

        override suspend fun save(listId: Long, progress: WatchProgress) {
            dao.upsert(progress.toEntity(listId))
        }

        override suspend fun get(listId: Long, kind: ProgressKind, externalId: String): WatchProgress? =
            dao.get(listId, kind.key(), externalId)?.toModel()

        override fun observe(listId: Long, kind: ProgressKind, externalId: String): Flow<WatchProgress?> =
            dao.observe(listId, kind.key(), externalId).map { it?.toModel() }

        override fun observeSeries(listId: Long, seriesId: String): Flow<List<WatchProgress>> =
            dao.observeSeriesEpisodes(listId, seriesId).map { rows -> rows.map { it.toModel() } }

        override fun observeContinueWatching(listId: Long, limit: Int): Flow<List<WatchProgress>> =
            dao.observeRecent(listId, limit * RecentOverfetch).map { rows ->
                rows
                    .map { it.toModel() }
                    .filterNot { it.finished }
                    .distinctBy { if (it.kind == ProgressKind.Episode) "series:${it.seriesId}" else "movie:${it.externalId}" }
                    .take(limit)
            }

        private companion object {
            /** Over-fetches because finished and repeated episodes are dropped afterwards. */
            const val RecentOverfetch = 4
        }
    }

private fun ProgressKind.key() = when (this) {
    ProgressKind.Movie -> "movie"
    ProgressKind.Episode -> "episode"
}

internal fun ProgressEntity.toModel() = WatchProgress(
    kind = if (kind == "episode") ProgressKind.Episode else ProgressKind.Movie,
    externalId = externalId,
    positionMillis = positionMillis,
    durationMillis = durationMillis,
    updatedAt = updatedAt,
    title = title,
    posterUrl = poster,
    seriesId = parentId,
    season = season,
    episode = episode,
    extension = extension,
)

internal fun WatchProgress.toEntity(listId: Long) = ProgressEntity(
    listId = listId,
    kind = kind.key(),
    externalId = externalId,
    positionMillis = positionMillis,
    durationMillis = durationMillis,
    updatedAt = updatedAt,
    title = title,
    poster = posterUrl,
    parentId = seriesId,
    season = season,
    episode = episode,
    extension = extension,
)
