package com.danielcrvo.sintonia.core.database

import androidx.paging.PagingSource
import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ListDao {
    @Query("SELECT * FROM lists ORDER BY createdAt, id")
    fun observeAll(): Flow<List<ListEntity>>

    @Query("SELECT * FROM lists WHERE id = :id")
    fun observe(id: Long): Flow<ListEntity?>

    @Query("SELECT * FROM lists WHERE id = :id")
    suspend fun get(id: Long): ListEntity?

    @Query("SELECT id FROM lists ORDER BY createdAt, id")
    suspend fun ids(): List<Long>

    @Insert
    suspend fun insert(list: ListEntity): Long

    @Query("DELETE FROM lists WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE lists SET liveExtension = :extension WHERE id = :id")
    suspend fun setLiveExtension(id: Long, extension: String)

    @Query("UPDATE lists SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)
}

@Dao
abstract class ImportDao {
    @Query("SELECT generation FROM lists WHERE id = :listId")
    abstract suspend fun currentGeneration(listId: Long): Int?

    @Insert
    abstract suspend fun insertCategories(items: List<CategoryEntity>)

    @Insert
    abstract suspend fun insertChannels(items: List<ChannelEntity>)

    @Insert
    abstract suspend fun insertMovies(items: List<MovieEntity>)

    @Insert
    abstract suspend fun insertSeries(items: List<SeriesEntity>)

    @Query("SELECT DISTINCT epgId FROM channels WHERE listId = :listId AND generation = :generation AND epgId IS NOT NULL")
    abstract suspend fun liveEpgIds(listId: Long, generation: Int): List<String>

    @Query("SELECT COUNT(*) FROM channels WHERE listId = :listId AND generation = :generation")
    abstract suspend fun countChannels(listId: Long, generation: Int): Int

    @Query("SELECT COUNT(*) FROM movies WHERE listId = :listId AND generation = :generation")
    abstract suspend fun countMovies(listId: Long, generation: Int): Int

    @Query("SELECT COUNT(*) FROM series WHERE listId = :listId AND generation = :generation")
    abstract suspend fun countSeries(listId: Long, generation: Int): Int

    @Query(
        "UPDATE lists SET generation = :generation, channelCount = :channels, movieCount = :movies, seriesCount = :series, lastImportedAt = :now WHERE id = :listId",
    )
    abstract suspend fun pointToGeneration(listId: Long, generation: Int, channels: Int, movies: Int, series: Int, now: Long)

    @Query("DELETE FROM categories WHERE listId = :listId AND generation <> :keep")
    abstract suspend fun deleteCategoriesExcept(listId: Long, keep: Int)

    @Query("DELETE FROM channels WHERE listId = :listId AND generation <> :keep")
    abstract suspend fun deleteChannelsExcept(listId: Long, keep: Int)

    @Query("DELETE FROM movies WHERE listId = :listId AND generation <> :keep")
    abstract suspend fun deleteMoviesExcept(listId: Long, keep: Int)

    @Query("DELETE FROM series WHERE listId = :listId AND generation <> :keep")
    abstract suspend fun deleteSeriesExcept(listId: Long, keep: Int)

    @Transaction
    open suspend fun commit(listId: Long, generation: Int, now: Long) {
        pointToGeneration(
            listId,
            generation,
            countChannels(listId, generation),
            countMovies(listId, generation),
            countSeries(listId, generation),
            now,
        )
        deleteCategoriesExcept(listId, generation)
        deleteChannelsExcept(listId, generation)
        deleteMoviesExcept(listId, generation)
        deleteSeriesExcept(listId, generation)
    }

    @Transaction
    open suspend fun discard(listId: Long) {
        val keep = currentGeneration(listId) ?: return
        deleteCategoriesExcept(listId, keep)
        deleteChannelsExcept(listId, keep)
        deleteMoviesExcept(listId, keep)
        deleteSeriesExcept(listId, keep)
    }
}

data class CategoryCountRow(val categoryId: String?, val total: Int)

@Dao
interface CategoryDao {
    @Query(
        "SELECT * FROM categories WHERE listId = :listId AND kind = :kind AND generation = (SELECT generation FROM lists WHERE id = :listId) ORDER BY name COLLATE NOCASE",
    )
    fun observe(listId: Long, kind: String): Flow<List<CategoryEntity>>

    @Query(
        "SELECT categoryId, COUNT(*) AS total FROM channels WHERE listId = :listId AND generation = (SELECT generation FROM lists WHERE id = :listId) GROUP BY categoryId",
    )
    fun observeChannelCounts(listId: Long): Flow<List<CategoryCountRow>>

    @Query(
        "SELECT categoryId, COUNT(*) AS total FROM movies WHERE listId = :listId AND generation = (SELECT generation FROM lists WHERE id = :listId) GROUP BY categoryId",
    )
    fun observeMovieCounts(listId: Long): Flow<List<CategoryCountRow>>

    @Query(
        "SELECT categoryId, COUNT(*) AS total FROM series WHERE listId = :listId AND generation = (SELECT generation FROM lists WHERE id = :listId) GROUP BY categoryId",
    )
    fun observeSeriesCounts(listId: Long): Flow<List<CategoryCountRow>>
}

@Dao
interface ChannelDao {
    @Query(
        "SELECT COUNT(*) FROM channels WHERE listId = :listId AND generation = (SELECT generation FROM lists WHERE id = :listId) AND (categoryId IS NULL OR categoryId NOT IN (:excluded))",
    )
    fun observeCount(listId: Long, excluded: List<String>): Flow<Int>

    @Query(
        "SELECT COUNT(*) FROM channels WHERE listId = :listId AND generation = (SELECT generation FROM lists WHERE id = :listId) AND categoryId = :categoryId AND categoryId NOT IN (:excluded)",
    )
    fun observeCategoryCount(listId: Long, categoryId: String, excluded: List<String>): Flow<Int>

    @Query(
        "SELECT COUNT(*) FROM channels c JOIN favorites f ON f.listId = c.listId AND f.channelExternalId = c.externalId " +
            "WHERE c.listId = :listId AND c.generation = (SELECT generation FROM lists WHERE id = :listId) AND (c.categoryId IS NULL OR c.categoryId NOT IN (:excluded))",
    )
    fun observeFavoritesCount(listId: Long, excluded: List<String>): Flow<Int>

    @Query(
        "SELECT * FROM channels WHERE listId = :listId AND generation = (SELECT generation FROM lists WHERE id = :listId) AND (categoryId IS NULL OR categoryId NOT IN (:excluded)) ORDER BY sortNumber, nameKey, id",
    )
    fun pagingByNumber(listId: Long, excluded: List<String>): PagingSource<Int, ChannelEntity>

    @Query(
        "SELECT * FROM channels WHERE listId = :listId AND generation = (SELECT generation FROM lists WHERE id = :listId) AND (categoryId IS NULL OR categoryId NOT IN (:excluded)) ORDER BY nameKey, id",
    )
    fun pagingByName(listId: Long, excluded: List<String>): PagingSource<Int, ChannelEntity>

    @Query(
        "SELECT * FROM channels WHERE listId = :listId AND generation = (SELECT generation FROM lists WHERE id = :listId) AND categoryId = :categoryId AND categoryId NOT IN (:excluded) ORDER BY sortNumber, nameKey, id",
    )
    fun pagingCategoryByNumber(listId: Long, categoryId: String, excluded: List<String>): PagingSource<Int, ChannelEntity>

    @Query(
        "SELECT * FROM channels WHERE listId = :listId AND generation = (SELECT generation FROM lists WHERE id = :listId) AND categoryId = :categoryId AND categoryId NOT IN (:excluded) ORDER BY nameKey, id",
    )
    fun pagingCategoryByName(listId: Long, categoryId: String, excluded: List<String>): PagingSource<Int, ChannelEntity>

    @Query(
        "SELECT c.* FROM channels c JOIN favorites f ON f.listId = c.listId AND f.channelExternalId = c.externalId " +
            "WHERE c.listId = :listId AND c.generation = (SELECT generation FROM lists WHERE id = :listId) AND (c.categoryId IS NULL OR c.categoryId NOT IN (:excluded)) " +
            "ORDER BY c.sortNumber, c.nameKey, c.id",
    )
    fun pagingFavorites(listId: Long, excluded: List<String>): PagingSource<Int, ChannelEntity>

    @Query(
        "SELECT c.* FROM channels c JOIN favorites f ON f.listId = c.listId AND f.channelExternalId = c.externalId " +
            "WHERE c.listId = :listId AND c.generation = (SELECT generation FROM lists WHERE id = :listId) AND (c.categoryId IS NULL OR c.categoryId NOT IN (:excluded)) " +
            "ORDER BY f.addedAt DESC LIMIT :limit",
    )
    fun observeRecentFavorites(listId: Long, excluded: List<String>, limit: Int): Flow<List<ChannelEntity>>

    @Query(
        "SELECT * FROM channels WHERE listId = :listId AND generation = (SELECT generation FROM lists WHERE id = :listId) AND nameKey LIKE '%' || :query || '%' ESCAPE '\\' AND (categoryId IS NULL OR categoryId NOT IN (:excluded)) ORDER BY nameKey, id LIMIT :limit",
    )
    suspend fun search(listId: Long, query: String, excluded: List<String>, limit: Int): List<ChannelEntity>

    @Query("SELECT * FROM channels WHERE id = :id")
    suspend fun get(id: Long): ChannelEntity?

    @Query(
        "SELECT id FROM channels WHERE listId = :listId AND generation = (SELECT generation FROM lists WHERE id = :listId) AND (categoryId IS NULL OR categoryId NOT IN (:excluded)) ORDER BY sortNumber, nameKey, id",
    )
    suspend fun idsByNumber(listId: Long, excluded: List<String>): List<Long>

    @Query(
        "SELECT id FROM channels WHERE listId = :listId AND generation = (SELECT generation FROM lists WHERE id = :listId) AND categoryId = :categoryId AND categoryId NOT IN (:excluded) ORDER BY sortNumber, nameKey, id",
    )
    suspend fun idsCategoryByNumber(listId: Long, categoryId: String, excluded: List<String>): List<Long>
}

@Dao
interface MovieDao {
    @Query(
        "SELECT COUNT(*) FROM movies WHERE listId = :listId AND generation = (SELECT generation FROM lists WHERE id = :listId) AND (categoryId IS NULL OR categoryId NOT IN (:excluded))",
    )
    fun observeCount(listId: Long, excluded: List<String>): Flow<Int>

    @Query(
        "SELECT COUNT(*) FROM movies WHERE listId = :listId AND generation = (SELECT generation FROM lists WHERE id = :listId) AND categoryId = :categoryId AND categoryId NOT IN (:excluded)",
    )
    fun observeCategoryCount(listId: Long, categoryId: String, excluded: List<String>): Flow<Int>

    @Query(
        "SELECT * FROM movies WHERE listId = :listId AND generation = (SELECT generation FROM lists WHERE id = :listId) AND (categoryId IS NULL OR categoryId NOT IN (:excluded)) ORDER BY nameKey, id",
    )
    fun pagingByName(listId: Long, excluded: List<String>): PagingSource<Int, MovieEntity>

    @Query(
        "SELECT * FROM movies WHERE listId = :listId AND generation = (SELECT generation FROM lists WHERE id = :listId) AND (categoryId IS NULL OR categoryId NOT IN (:excluded)) ORDER BY sortYear DESC, nameKey, id",
    )
    fun pagingByYear(listId: Long, excluded: List<String>): PagingSource<Int, MovieEntity>

    @Query(
        "SELECT * FROM movies WHERE listId = :listId AND generation = (SELECT generation FROM lists WHERE id = :listId) AND categoryId = :categoryId AND categoryId NOT IN (:excluded) ORDER BY nameKey, id",
    )
    fun pagingCategoryByName(listId: Long, categoryId: String, excluded: List<String>): PagingSource<Int, MovieEntity>

    @Query(
        "SELECT * FROM movies WHERE listId = :listId AND generation = (SELECT generation FROM lists WHERE id = :listId) AND categoryId = :categoryId AND categoryId NOT IN (:excluded) ORDER BY sortYear DESC, nameKey, id",
    )
    fun pagingCategoryByYear(listId: Long, categoryId: String, excluded: List<String>): PagingSource<Int, MovieEntity>

    @Query(
        "SELECT * FROM movies WHERE listId = :listId AND generation = (SELECT generation FROM lists WHERE id = :listId) AND nameKey LIKE '%' || :query || '%' ESCAPE '\\' AND (categoryId IS NULL OR categoryId NOT IN (:excluded)) ORDER BY nameKey, id LIMIT :limit",
    )
    suspend fun search(listId: Long, query: String, excluded: List<String>, limit: Int): List<MovieEntity>

    @Query("SELECT * FROM movies WHERE id = :id")
    suspend fun get(id: Long): MovieEntity?

    @Query(
        "SELECT * FROM movies WHERE listId = :listId AND externalId = :externalId AND generation = (SELECT generation FROM lists WHERE id = :listId) LIMIT 1",
    )
    suspend fun getByExternalId(listId: Long, externalId: String): MovieEntity?

    @Query(
        "SELECT * FROM movies WHERE listId = :listId AND generation = (SELECT generation FROM lists WHERE id = :listId) AND (categoryId IS NULL OR categoryId NOT IN (:excluded)) ORDER BY sortYear DESC, nameKey, id LIMIT :limit",
    )
    fun observeFeatured(listId: Long, excluded: List<String>, limit: Int): Flow<List<MovieEntity>>
}

@Dao
interface SeriesDao {
    @Query(
        "SELECT COUNT(*) FROM series WHERE listId = :listId AND generation = (SELECT generation FROM lists WHERE id = :listId) AND (categoryId IS NULL OR categoryId NOT IN (:excluded))",
    )
    fun observeCount(listId: Long, excluded: List<String>): Flow<Int>

    @Query(
        "SELECT COUNT(*) FROM series WHERE listId = :listId AND generation = (SELECT generation FROM lists WHERE id = :listId) AND categoryId = :categoryId AND categoryId NOT IN (:excluded)",
    )
    fun observeCategoryCount(listId: Long, categoryId: String, excluded: List<String>): Flow<Int>

    @Query(
        "SELECT * FROM series WHERE listId = :listId AND generation = (SELECT generation FROM lists WHERE id = :listId) AND (categoryId IS NULL OR categoryId NOT IN (:excluded)) ORDER BY nameKey, id",
    )
    fun pagingByName(listId: Long, excluded: List<String>): PagingSource<Int, SeriesEntity>

    @Query(
        "SELECT * FROM series WHERE listId = :listId AND generation = (SELECT generation FROM lists WHERE id = :listId) AND (categoryId IS NULL OR categoryId NOT IN (:excluded)) ORDER BY sortYear DESC, nameKey, id",
    )
    fun pagingByYear(listId: Long, excluded: List<String>): PagingSource<Int, SeriesEntity>

    @Query(
        "SELECT * FROM series WHERE listId = :listId AND generation = (SELECT generation FROM lists WHERE id = :listId) AND categoryId = :categoryId AND categoryId NOT IN (:excluded) ORDER BY nameKey, id",
    )
    fun pagingCategoryByName(listId: Long, categoryId: String, excluded: List<String>): PagingSource<Int, SeriesEntity>

    @Query(
        "SELECT * FROM series WHERE listId = :listId AND generation = (SELECT generation FROM lists WHERE id = :listId) AND categoryId = :categoryId AND categoryId NOT IN (:excluded) ORDER BY sortYear DESC, nameKey, id",
    )
    fun pagingCategoryByYear(listId: Long, categoryId: String, excluded: List<String>): PagingSource<Int, SeriesEntity>

    @Query(
        "SELECT * FROM series WHERE listId = :listId AND generation = (SELECT generation FROM lists WHERE id = :listId) AND nameKey LIKE '%' || :query || '%' ESCAPE '\\' AND (categoryId IS NULL OR categoryId NOT IN (:excluded)) ORDER BY nameKey, id LIMIT :limit",
    )
    suspend fun search(listId: Long, query: String, excluded: List<String>, limit: Int): List<SeriesEntity>

    @Query("SELECT * FROM series WHERE id = :id")
    suspend fun get(id: Long): SeriesEntity?

    @Query(
        "SELECT * FROM series WHERE listId = :listId AND externalId = :externalId AND generation = (SELECT generation FROM lists WHERE id = :listId) LIMIT 1",
    )
    suspend fun getByExternalId(listId: Long, externalId: String): SeriesEntity?

    @Query(
        "SELECT * FROM series WHERE listId = :listId AND generation = (SELECT generation FROM lists WHERE id = :listId) AND (categoryId IS NULL OR categoryId NOT IN (:excluded)) ORDER BY sortYear DESC, nameKey, id LIMIT :limit",
    )
    fun observeFeatured(listId: Long, excluded: List<String>, limit: Int): Flow<List<SeriesEntity>>
}

@Dao
interface FavoriteDao {
    @Query("SELECT channelExternalId FROM favorites WHERE listId = :listId")
    fun observeIds(listId: Long): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE listId = :listId AND channelExternalId = :externalId")
    suspend fun remove(listId: Long, externalId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE listId = :listId AND channelExternalId = :externalId)")
    suspend fun exists(listId: Long, externalId: String): Boolean
}

@Dao
interface ProgressDao {
    @Upsert
    suspend fun upsert(entry: ProgressEntity)

    @Query("SELECT * FROM progress WHERE listId = :listId AND kind = :kind AND externalId = :externalId")
    suspend fun get(listId: Long, kind: String, externalId: String): ProgressEntity?

    @Query("SELECT * FROM progress WHERE listId = :listId AND kind = :kind AND externalId = :externalId")
    fun observe(listId: Long, kind: String, externalId: String): Flow<ProgressEntity?>

    @Query("SELECT * FROM progress WHERE listId = :listId AND positionMillis > 0 ORDER BY updatedAt DESC LIMIT :limit")
    fun observeRecent(listId: Long, limit: Int): Flow<List<ProgressEntity>>

    @Query("SELECT * FROM progress WHERE listId = :listId AND kind = 'episode' AND parentId = :seriesId")
    fun observeSeriesEpisodes(listId: Long, seriesId: String): Flow<List<ProgressEntity>>

    @Query("DELETE FROM progress WHERE listId = :listId AND kind = :kind AND externalId = :externalId")
    suspend fun remove(listId: Long, kind: String, externalId: String)
}

@Dao
interface EpgDao {
    @Insert
    suspend fun insert(items: List<ProgramEntity>)

    @Query("DELETE FROM epg_programs WHERE listId = :listId")
    suspend fun clear(listId: Long)

    @Transaction
    suspend fun replace(listId: Long, items: List<ProgramEntity>) {
        clear(listId)
        items.chunked(InsertChunk).forEach { insert(it) }
    }

    @Query("SELECT * FROM epg_programs WHERE listId = :listId AND epgId = :epgId AND stop > :from AND start < :to ORDER BY start")
    fun observeRange(listId: Long, epgId: String, from: Long, to: Long): Flow<List<ProgramEntity>>

    @Query("SELECT * FROM epg_programs WHERE listId = :listId AND epgId IN (:epgIds) AND stop > :from AND start < :to ORDER BY epgId, start")
    fun observeRangeFor(listId: Long, epgIds: List<String>, from: Long, to: Long): Flow<List<ProgramEntity>>

    @Query("SELECT * FROM epg_programs WHERE listId = :listId AND epgId = :epgId AND stop > :now ORDER BY start LIMIT 2")
    suspend fun nowAndNext(listId: Long, epgId: String, now: Long): List<ProgramEntity>

    @Query("SELECT * FROM epg_programs WHERE listId = :listId AND epgId IN (:epgIds) AND start <= :now AND stop > :now")
    suspend fun nowFor(listId: Long, epgIds: List<String>, now: Long): List<ProgramEntity>
}

private const val InsertChunk = 1000
