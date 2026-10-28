package com.danielcrvo.sintonia.core.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.danielcrvo.sintonia.core.database.MovieEntity
import com.danielcrvo.sintonia.core.database.SeriesEntity
import com.danielcrvo.sintonia.core.database.SintoniaDatabase
import com.danielcrvo.sintonia.core.model.Category
import com.danielcrvo.sintonia.core.model.ContentKind
import com.danielcrvo.sintonia.core.model.Movie
import com.danielcrvo.sintonia.core.model.Series
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class CatalogSort { Name, Newest }

interface MovieRepository {
    fun categories(listId: Long): Flow<List<Category>>
    fun allCategories(listId: Long): Flow<List<Category>>
    fun movies(listId: Long, categoryId: String?, sort: CatalogSort, excluded: List<String>): Flow<PagingData<Movie>>
    fun count(listId: Long, categoryId: String?, excluded: List<String>): Flow<Int>
    suspend fun movie(id: Long): Movie?
    suspend fun movieByExternalId(listId: Long, externalId: String): Movie?
    fun featured(listId: Long, excluded: List<String>, limit: Int): Flow<List<Movie>>
}

interface SeriesRepository {
    fun categories(listId: Long): Flow<List<Category>>
    fun allCategories(listId: Long): Flow<List<Category>>
    fun series(listId: Long, categoryId: String?, sort: CatalogSort, excluded: List<String>): Flow<PagingData<Series>>
    fun count(listId: Long, categoryId: String?, excluded: List<String>): Flow<Int>
    suspend fun series(id: Long): Series?
    suspend fun seriesByExternalId(listId: Long, externalId: String): Series?
    fun featured(listId: Long, excluded: List<String>, limit: Int): Flow<List<Series>>
}

private val GridPaging = PagingConfig(pageSize = 60, prefetchDistance = 30, initialLoadSize = 120, enablePlaceholders = false)

@Singleton
class RoomMovieRepository
    @Inject
    constructor(
        private val db: SintoniaDatabase,
        private val categoryPrefs: CategoryPrefsRepository,
    ) : MovieRepository {
        private val dao get() = db.movies()

        override fun categories(listId: Long): Flow<List<Category>> = categoryPrefs.decorate(listId, ContentKind.Movie, stored(listId), includeHidden = false)

        override fun allCategories(listId: Long): Flow<List<Category>> = categoryPrefs.decorate(listId, ContentKind.Movie, stored(listId), includeHidden = true)

        private fun stored(listId: Long): Flow<List<Category>> =
            combine(db.categories().observe(listId, ContentKind.Movie.name), db.categories().observeMovieCounts(listId)) { rows, counts ->
                val byId = counts.associate { it.categoryId to it.total }
                rows.map { Category(it.externalId, it.name, ContentKind.Movie, byId[it.externalId] ?: 0) }
            }

        override fun movies(listId: Long, categoryId: String?, sort: CatalogSort, excluded: List<String>): Flow<PagingData<Movie>> =
            Pager(GridPaging) {
                when {
                    categoryId == null && sort == CatalogSort.Name -> dao.pagingByName(listId, excluded)
                    categoryId == null -> dao.pagingByYear(listId, excluded)
                    sort == CatalogSort.Name -> dao.pagingCategoryByName(listId, categoryId, excluded)
                    else -> dao.pagingCategoryByYear(listId, categoryId, excluded)
                }
            }.flow.map { data -> data.map { it.toModel() } }

        override fun count(listId: Long, categoryId: String?, excluded: List<String>): Flow<Int> =
            if (categoryId == null) dao.observeCount(listId, excluded) else dao.observeCategoryCount(listId, categoryId, excluded)

        override suspend fun movie(id: Long): Movie? = dao.get(id)?.toModel()

        override suspend fun movieByExternalId(listId: Long, externalId: String): Movie? =
            dao.getByExternalId(listId, externalId)?.toModel()

        override fun featured(listId: Long, excluded: List<String>, limit: Int): Flow<List<Movie>> =
            dao.observeFeatured(listId, excluded, limit).map { rows -> rows.map { it.toModel() } }
    }

@Singleton
class RoomSeriesRepository
    @Inject
    constructor(
        private val db: SintoniaDatabase,
        private val categoryPrefs: CategoryPrefsRepository,
    ) : SeriesRepository {
        private val dao get() = db.series()

        override fun categories(listId: Long): Flow<List<Category>> = categoryPrefs.decorate(listId, ContentKind.Series, stored(listId), includeHidden = false)

        override fun allCategories(listId: Long): Flow<List<Category>> = categoryPrefs.decorate(
            listId,
            ContentKind.Series,
            stored(listId),
            includeHidden = true,
        )

        private fun stored(listId: Long): Flow<List<Category>> =
            combine(db.categories().observe(listId, ContentKind.Series.name), db.categories().observeSeriesCounts(listId)) { rows, counts ->
                val byId = counts.associate { it.categoryId to it.total }
                rows.map { Category(it.externalId, it.name, ContentKind.Series, byId[it.externalId] ?: 0) }
            }

        override fun series(listId: Long, categoryId: String?, sort: CatalogSort, excluded: List<String>): Flow<PagingData<Series>> =
            Pager(GridPaging) {
                when {
                    categoryId == null && sort == CatalogSort.Name -> dao.pagingByName(listId, excluded)
                    categoryId == null -> dao.pagingByYear(listId, excluded)
                    sort == CatalogSort.Name -> dao.pagingCategoryByName(listId, categoryId, excluded)
                    else -> dao.pagingCategoryByYear(listId, categoryId, excluded)
                }
            }.flow.map { data -> data.map { it.toModel() } }

        override fun count(listId: Long, categoryId: String?, excluded: List<String>): Flow<Int> =
            if (categoryId == null) dao.observeCount(listId, excluded) else dao.observeCategoryCount(listId, categoryId, excluded)

        override suspend fun series(id: Long): Series? = dao.get(id)?.toModel()

        override suspend fun seriesByExternalId(listId: Long, externalId: String): Series? =
            dao.getByExternalId(listId, externalId)?.toModel()

        override fun featured(listId: Long, excluded: List<String>, limit: Int): Flow<List<Series>> =
            dao.observeFeatured(listId, excluded, limit).map { rows -> rows.map { it.toModel() } }
    }

internal fun MovieEntity.toModel() = Movie(
    id = id,
    externalId = externalId,
    name = name,
    posterUrl = poster,
    categoryId = categoryId,
    year = year,
    rating = rating,
    containerExtension = containerExtension,
    url = url,
)

internal fun SeriesEntity.toModel() = Series(
    id = id,
    externalId = externalId,
    name = name,
    posterUrl = poster,
    categoryId = categoryId,
    year = year,
    rating = rating,
    url = url,
)
