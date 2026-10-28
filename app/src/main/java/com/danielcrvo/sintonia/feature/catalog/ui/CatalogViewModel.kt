package com.danielcrvo.sintonia.feature.catalog.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.danielcrvo.sintonia.core.common.formatRating
import com.danielcrvo.sintonia.core.data.CatalogSort
import com.danielcrvo.sintonia.core.data.ListsRepository
import com.danielcrvo.sintonia.core.data.MovieRepository
import com.danielcrvo.sintonia.core.data.ParentalRepository
import com.danielcrvo.sintonia.core.data.SeriesRepository
import com.danielcrvo.sintonia.core.model.Category
import com.danielcrvo.sintonia.core.model.ContentKind
import com.danielcrvo.sintonia.core.parental.PinManager
import com.danielcrvo.sintonia.core.parental.PinPromptController
import com.danielcrvo.sintonia.core.parental.PinPromptState
import com.danielcrvo.sintonia.core.parental.PinPurpose
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class PosterItem(
    val id: Long,
    val title: String,
    val meta: String?,
    val imageUrl: String?,
)

@Immutable
data class CatalogUiState(
    val selectedCategory: Category? = null,
    val categories: ImmutableList<Category> = persistentListOf(),
    val sort: CatalogSort = CatalogSort.Name,
    val filterOpen: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
abstract class CatalogViewModel(
    lists: ListsRepository,
    private val parental: ParentalRepository,
    pins: PinManager,
    private val kind: ContentKind,
) : ViewModel() {
    private val pinPrompt = PinPromptController(pins, viewModelScope)

    val pinState: StateFlow<PinPromptState?> = pinPrompt.state

    protected abstract fun categories(listId: Long): Flow<List<Category>>
    protected abstract fun count(listId: Long, categoryId: String?, excluded: List<String>): Flow<Int>
    protected abstract fun items(listId: Long, categoryId: String?, sort: CatalogSort, excluded: List<String>): Flow<PagingData<PosterItem>>

    private val categoryId = MutableStateFlow<String?>(null)
    private val sort = MutableStateFlow(CatalogSort.Name)
    private val filterOpen = MutableStateFlow(false)

    private val listId: Flow<Long> = lists.activeList
        .filterNotNull()
        .map { it.id }
        .distinctUntilChangedBy { it }
    private val blocked: Flow<List<String>> = listId.flatMapLatest { parental.lockedCategories(it, kind) }

    /** Outside the state: it is a flow of pages, not a value. */
    val items: Flow<PagingData<PosterItem>> by lazy {
        combine(listId, categoryId, sort) { id, category, sort -> Triple(id, category, sort) }
            .distinctUntilChangedBy { it }
            .flatMapLatest { (id, category, sort) ->
                blocked.flatMapLatest { hidden -> items(id, category, sort, hidden) }
            }.cachedIn(viewModelScope)
    }

    val itemCount: Flow<Int> by lazy {
        combine(listId, categoryId) { id, category -> id to category }
            .distinctUntilChanged()
            .flatMapLatest { (id, category) -> blocked.flatMapLatest { hidden -> count(id, category, hidden) } }
    }

    val state: StateFlow<CatalogUiState> by lazy {
        combine(
            combine(listId.flatMapLatest { categories(it) }, blocked) { all, locked -> all.map { it.copy(locked = it.externalId in locked) } },
            categoryId,
            sort,
            filterOpen,
        ) { categories, selected, sort, open ->
            CatalogUiState(categories.firstOrNull { it.externalId == selected }, categories.toImmutableList(), sort, open)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), CatalogUiState())
    }

    init {
        viewModelScope.launch {
            blocked.collect { hidden -> if (categoryId.value in hidden) categoryId.value = null }
        }
    }

    fun onCategorySelected(category: Category?) {
        if (category != null && category.locked) {
            pinPrompt.verify(PinPurpose.Unlock) {
                viewModelScope.launch {
                    parental.unlock(listId.first(), kind, category.externalId)
                    categoryId.value = category.externalId
                }
            }
            return
        }
        categoryId.value = category?.externalId
    }

    fun onPinDigit(digit: Int) = pinPrompt.onDigit(digit)
    fun onPinBackspace() = pinPrompt.onBackspace()
    fun onPinDismiss() = pinPrompt.dismiss()

    fun onAllSelected() {
        categoryId.value = null
    }

    fun onSortSelected(selected: CatalogSort) {
        sort.value = selected
    }

    fun onFilterClick() {
        filterOpen.value = true
    }

    fun onFilterDismiss() {
        filterOpen.value = false
    }

    private companion object {
        const val StopTimeoutMillis = 5_000L
    }
}

private fun metaOf(year: String?, rating: Double?): String? {
    val parts = listOfNotNull(year, rating?.let { formatRating(it) })
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

@HiltViewModel
class MoviesViewModel
    @Inject
    constructor(
        lists: ListsRepository,
        parental: ParentalRepository,
        pins: PinManager,
        private val repository: MovieRepository,
    ) : CatalogViewModel(lists, parental, pins, ContentKind.Movie) {
        override fun categories(listId: Long) = repository.categories(listId)

        override fun count(listId: Long, categoryId: String?, excluded: List<String>) = repository.count(listId, categoryId, excluded)

        override fun items(listId: Long, categoryId: String?, sort: CatalogSort, excluded: List<String>) =
            repository.movies(listId, categoryId, sort, excluded).map { data ->
                data.map { PosterItem(it.id, it.name, metaOf(it.year, it.rating), it.posterUrl) }
            }
    }

@HiltViewModel
class SeriesViewModel
    @Inject
    constructor(
        lists: ListsRepository,
        parental: ParentalRepository,
        pins: PinManager,
        private val repository: SeriesRepository,
    ) : CatalogViewModel(lists, parental, pins, ContentKind.Series) {
        override fun categories(listId: Long) = repository.categories(listId)

        override fun count(listId: Long, categoryId: String?, excluded: List<String>) = repository.count(listId, categoryId, excluded)

        override fun items(listId: Long, categoryId: String?, sort: CatalogSort, excluded: List<String>) =
            repository.series(listId, categoryId, sort, excluded).map { data ->
                data.map { PosterItem(it.id, it.name, metaOf(it.year, it.rating), it.posterUrl) }
            }
    }
