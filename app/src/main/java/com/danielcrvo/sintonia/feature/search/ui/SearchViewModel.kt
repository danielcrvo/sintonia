package com.danielcrvo.sintonia.feature.search.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danielcrvo.sintonia.core.data.HiddenCategories
import com.danielcrvo.sintonia.core.data.ListsRepository
import com.danielcrvo.sintonia.core.data.ParentalRepository
import com.danielcrvo.sintonia.core.data.SearchRepository
import com.danielcrvo.sintonia.core.data.SearchResults
import com.danielcrvo.sintonia.core.model.ContentKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@Immutable
data class SearchUiState(
    val query: String = "",
    val results: SearchResults = SearchResults(),
    val searched: Boolean = false,
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel
    @Inject
    constructor(
        lists: ListsRepository,
        parental: ParentalRepository,
        private val search: SearchRepository,
    ) : ViewModel() {
        private val query = MutableStateFlow("")

        private val listId = lists.activeList.filterNotNull()

        private val hidden = listId.flatMapLatest { list ->
            combine(
                parental.blockedCategories(list.id, ContentKind.Live),
                parental.blockedCategories(list.id, ContentKind.Movie),
                parental.blockedCategories(list.id, ContentKind.Series),
            ) { live, movies, series -> HiddenCategories(live, movies, series) }
        }

        val state: StateFlow<SearchUiState> = combine(listId, query.debounce(DebounceMillis).distinctUntilChanged(), hidden) { list, text, hidden ->
            Triple(list.id, text, hidden)
        }.flatMapLatest { (id, text, hidden) ->
            flow {
                val results = if (text.isBlank()) SearchResults() else search.search(id, text, hidden, LimitPerKind)
                emit(SearchUiState(text, results, searched = text.isNotBlank()))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), SearchUiState())

        val typed: StateFlow<String> = query

        fun onQueryChange(value: String) {
            query.value = value
        }

        private companion object {
            const val DebounceMillis = 250L
            const val StopTimeoutMillis = 5_000L
            const val LimitPerKind = 20
        }
    }
