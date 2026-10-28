package com.danielcrvo.sintonia.feature.categories.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danielcrvo.sintonia.core.data.CategoryPrefsRepository
import com.danielcrvo.sintonia.core.data.ListsRepository
import com.danielcrvo.sintonia.core.data.LiveRepository
import com.danielcrvo.sintonia.core.data.MovieRepository
import com.danielcrvo.sintonia.core.data.SeriesRepository
import com.danielcrvo.sintonia.core.model.Category
import com.danielcrvo.sintonia.core.model.ContentKind
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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class CategoryItem(val id: String, val name: String, val originalName: String, val hidden: Boolean) {
    val renamed: Boolean get() = name != originalName
}

@Immutable
data class CategoriesSection(val kind: ContentKind, val categories: ImmutableList<CategoryItem>) {
    val hiddenCount: Int get() = categories.count { it.hidden }
}

@Immutable
data class RenameTarget(val kind: ContentKind, val item: CategoryItem, val text: String)

@Immutable
data class CategoriesUiState(
    val sections: ImmutableList<CategoriesSection> = persistentListOf(),
    val expanded: ContentKind? = null,
    val renaming: RenameTarget? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CategoriesViewModel
    @Inject
    constructor(
        lists: ListsRepository,
        live: LiveRepository,
        movies: MovieRepository,
        series: SeriesRepository,
        private val prefs: CategoryPrefsRepository,
    ) : ViewModel() {
        private val expanded = MutableStateFlow<ContentKind?>(null)
        private val renaming = MutableStateFlow<RenameTarget?>(null)

        private val listId: Flow<Long> = lists.activeList
            .filterNotNull()
            .map { it.id }
            .distinctUntilChanged()

        private fun section(kind: ContentKind, categories: (Long) -> Flow<List<Category>>): Flow<CategoriesSection> =
            listId.flatMapLatest { id ->
                categories(id).map { all ->
                    CategoriesSection(kind, all.map { CategoryItem(it.externalId, it.name, it.originalName, it.hidden) }.toImmutableList())
                }
            }

        val state: StateFlow<CategoriesUiState> = combine(
            combine(
                section(ContentKind.Live, live::allCategories),
                section(ContentKind.Movie, movies::allCategories),
                section(ContentKind.Series, series::allCategories),
            ) { a, b, c -> persistentListOf(a, b, c) },
            expanded,
            renaming,
        ) { sections, expanded, renaming ->
            CategoriesUiState(sections, expanded, renaming)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), CategoriesUiState())

        fun onToggleSection(kind: ContentKind) {
            expanded.update { if (it == kind) null else kind }
        }

        fun onToggleHidden(kind: ContentKind, categoryId: String, hidden: Boolean) {
            viewModelScope.launch { prefs.setHidden(listId.first(), kind, categoryId, hidden) }
        }

        fun onRenameStart(kind: ContentKind, item: CategoryItem) {
            renaming.value = RenameTarget(kind, item, item.name)
        }

        fun onRenameTextChange(text: String) {
            renaming.update { it?.copy(text = text.take(MaxNameLength)) }
        }

        fun onRenameSave() {
            val target = renaming.value ?: return
            renaming.value = null
            val name = target.text.trim().takeIf { it.isNotEmpty() && it != target.item.originalName }
            viewModelScope.launch { prefs.rename(listId.first(), target.kind, target.item.id, name) }
        }

        fun onRenameReset() {
            val target = renaming.value ?: return
            renaming.value = null
            viewModelScope.launch { prefs.rename(listId.first(), target.kind, target.item.id, null) }
        }

        fun onRenameDismiss() {
            renaming.value = null
        }

        private companion object {
            const val StopTimeoutMillis = 5_000L
            const val MaxNameLength = 60
        }
    }
