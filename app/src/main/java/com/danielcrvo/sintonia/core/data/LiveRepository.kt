package com.danielcrvo.sintonia.core.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.danielcrvo.sintonia.core.database.ChannelEntity
import com.danielcrvo.sintonia.core.database.FavoriteEntity
import com.danielcrvo.sintonia.core.database.ProgramEntity
import com.danielcrvo.sintonia.core.database.SintoniaDatabase
import com.danielcrvo.sintonia.core.model.Category
import com.danielcrvo.sintonia.core.model.Channel
import com.danielcrvo.sintonia.core.model.ContentKind
import com.danielcrvo.sintonia.core.model.Program
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ChannelSort { Number, Name }

sealed interface ChannelScope {
    data object All : ChannelScope
    data object Favorites : ChannelScope
    data class InCategory(val categoryId: String) : ChannelScope
}

interface LiveRepository {
    fun categories(listId: Long): Flow<List<Category>>

    fun allCategories(listId: Long): Flow<List<Category>>

    fun channels(listId: Long, scope: ChannelScope, sort: ChannelSort, excluded: List<String>): Flow<PagingData<Channel>>

    fun channelCount(listId: Long, scope: ChannelScope, excluded: List<String>): Flow<Int>
    fun favoriteIds(listId: Long): Flow<Set<String>>
    suspend fun setFavorite(listId: Long, externalId: String, favorite: Boolean)
    fun recentFavorites(listId: Long, excluded: List<String>, limit: Int): Flow<List<Channel>>
    suspend fun channel(id: Long): Channel?

    suspend fun channelIds(listId: Long, scope: ChannelScope, excluded: List<String>): List<Long>

    suspend fun nowAndNext(listId: Long, epgId: String, nowMillis: Long): List<Program>
    suspend fun nowFor(listId: Long, epgIds: Collection<String>, nowMillis: Long): Map<String, Program>
    fun programsBetween(listId: Long, epgId: String, fromMillis: Long, toMillis: Long): Flow<List<Program>>

    fun programsFor(listId: Long, epgIds: Collection<String>, fromMillis: Long, toMillis: Long): Flow<Map<String, List<Program>>>
}

@Singleton
class RoomLiveRepository
    @Inject
    constructor(
        private val db: SintoniaDatabase,
        private val categoryPrefs: CategoryPrefsRepository,
    ) : LiveRepository {
        private val channels get() = db.channels()

        override fun categories(listId: Long): Flow<List<Category>> = categoryPrefs.decorate(listId, ContentKind.Live, stored(listId), includeHidden = false)

        override fun allCategories(listId: Long): Flow<List<Category>> = categoryPrefs.decorate(listId, ContentKind.Live, stored(listId), includeHidden = true)

        private fun stored(listId: Long): Flow<List<Category>> =
            combine(db.categories().observe(listId, ContentKind.Live.name), db.categories().observeChannelCounts(listId)) { categories, counts ->
                val byId = counts.associate { it.categoryId to it.total }
                categories.map { Category(it.externalId, it.name, ContentKind.Live, byId[it.externalId] ?: 0) }
            }

        override fun channels(
            listId: Long,
            scope: ChannelScope,
            sort: ChannelSort,
            excluded: List<String>,
        ): Flow<PagingData<Channel>> {
            val factory = {
                when (scope) {
                    ChannelScope.All -> if (sort == ChannelSort.Number) {
                        channels.pagingByNumber(listId, excluded)
                    } else {
                        channels.pagingByName(listId, excluded)
                    }
                    ChannelScope.Favorites -> channels.pagingFavorites(listId, excluded)
                    is ChannelScope.InCategory -> if (sort == ChannelSort.Number) {
                        channels.pagingCategoryByNumber(listId, scope.categoryId, excluded)
                    } else {
                        channels.pagingCategoryByName(listId, scope.categoryId, excluded)
                    }
                }
            }
            return Pager(
                PagingConfig(pageSize = PageSize, prefetchDistance = PageSize / 2, initialLoadSize = PageSize * 2, enablePlaceholders = false),
                pagingSourceFactory = factory,
            ).flow
                .map { data -> data.map { it.toModel() } }
        }

        override fun channelCount(listId: Long, scope: ChannelScope, excluded: List<String>): Flow<Int> = when (scope) {
            ChannelScope.All -> channels.observeCount(listId, excluded)
            ChannelScope.Favorites -> channels.observeFavoritesCount(listId, excluded)
            is ChannelScope.InCategory -> channels.observeCategoryCount(listId, scope.categoryId, excluded)
        }

        override fun favoriteIds(listId: Long): Flow<Set<String>> = db.favorites().observeIds(listId).map { it.toSet() }

        override suspend fun setFavorite(listId: Long, externalId: String, favorite: Boolean) {
            if (favorite) {
                db.favorites().add(FavoriteEntity(listId, externalId, System.currentTimeMillis()))
            } else {
                db.favorites().remove(listId, externalId)
            }
        }

        override fun recentFavorites(listId: Long, excluded: List<String>, limit: Int): Flow<List<Channel>> =
            channels.observeRecentFavorites(listId, excluded, limit).map { rows -> rows.map { it.toModel() } }

        override suspend fun channel(id: Long): Channel? = channels.get(id)?.toModel()

        override suspend fun channelIds(listId: Long, scope: ChannelScope, excluded: List<String>): List<Long> = when (scope) {
            is ChannelScope.InCategory -> channels.idsCategoryByNumber(listId, scope.categoryId, excluded)
            else -> channels.idsByNumber(listId, excluded)
        }

        override suspend fun nowAndNext(listId: Long, epgId: String, nowMillis: Long): List<Program> =
            db.epg().nowAndNext(listId, epgId, nowMillis).map { it.toModel() }

        override suspend fun nowFor(listId: Long, epgIds: Collection<String>, nowMillis: Long): Map<String, Program> {
            if (epgIds.isEmpty()) return emptyMap()
            return epgIds
                .distinct()
                .chunked(SqlVariableChunk)
                .flatMap { chunk ->
                    db.epg().nowFor(listId, chunk, nowMillis)
                }.associate { it.epgId to it.toModel() }
        }

        override fun programsBetween(listId: Long, epgId: String, fromMillis: Long, toMillis: Long): Flow<List<Program>> =
            db.epg().observeRange(listId, epgId, fromMillis, toMillis).map { rows -> rows.map { it.toModel() } }

        override fun programsFor(listId: Long, epgIds: Collection<String>, fromMillis: Long, toMillis: Long): Flow<Map<String, List<Program>>> {
            if (epgIds.isEmpty()) return kotlinx.coroutines.flow.flowOf(emptyMap())
            val chunks = epgIds.distinct().chunked(SqlVariableChunk).map { db.epg().observeRangeFor(listId, it, fromMillis, toMillis) }
            return combine(chunks) { parts -> parts.flatMap { it }.groupBy({ it.epgId }, { it.toModel() }) }
                .flowOn(kotlinx.coroutines.Dispatchers.Default)
        }

        private companion object {
            const val PageSize = 60

            /** SQLite limits query variables; small chunks avoid the overflow. */
            const val SqlVariableChunk = 500
        }
    }

internal fun ChannelEntity.toModel() = Channel(
    id = id,
    externalId = externalId,
    number = number,
    name = name,
    logoUrl = logo,
    categoryId = categoryId,
    epgId = epgId,
    url = url,
)

internal fun ProgramEntity.toModel() = Program(start, stop, title, description)
