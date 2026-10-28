package com.danielcrvo.sintonia.feature.live.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.danielcrvo.sintonia.core.data.ChannelScope
import com.danielcrvo.sintonia.core.data.ChannelSort
import com.danielcrvo.sintonia.core.data.ListsRepository
import com.danielcrvo.sintonia.core.data.LiveRepository
import com.danielcrvo.sintonia.core.data.ParentalRepository
import com.danielcrvo.sintonia.core.model.Category
import com.danielcrvo.sintonia.core.model.Channel
import com.danielcrvo.sintonia.core.model.ContentKind
import com.danielcrvo.sintonia.core.model.Program
import com.danielcrvo.sintonia.core.parental.PinManager
import com.danielcrvo.sintonia.core.parental.PinPromptController
import com.danielcrvo.sintonia.core.parental.PinPromptState
import com.danielcrvo.sintonia.core.parental.PinPurpose
import com.danielcrvo.sintonia.core.player.Playback
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel as EventChannel

const val GuideHours = 6

enum class LiveTab { All, Favorites, Category }

sealed interface LiveSheet {
    data object Filter : LiveSheet
    data class Programs(val channel: Channel) : LiveSheet
}

@Immutable
data class LiveUiState(
    val tab: LiveTab = LiveTab.All,
    val selectedCategory: Category? = null,
    val categories: ImmutableList<Category> = persistentListOf(),
    val sort: ChannelSort = ChannelSort.Number,
    val favoriteIds: Set<String> = emptySet(),
    val playingChannelId: Long? = null,
    val sheet: LiveSheet? = null,
    val todayPrograms: ImmutableList<Program> = persistentListOf(),
)

sealed interface LiveEffect {
    data class FavoriteChanged(val channelName: String, val added: Boolean) : LiveEffect
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class LiveViewModel
    @Inject
    constructor(
        lists: ListsRepository,
        private val live: LiveRepository,
        private val parental: ParentalRepository,
        pins: PinManager,
        playback: Playback,
    ) : ViewModel() {
        private val pinPrompt = PinPromptController(pins, viewModelScope)

        val pinState: StateFlow<PinPromptState?> = pinPrompt.state

        private val tab = MutableStateFlow(LiveTab.All)
        private val categoryId = MutableStateFlow<String?>(null)
        private val sort = MutableStateFlow(ChannelSort.Number)
        private val sheet = MutableStateFlow<LiveSheet?>(null)

        private val listId: Flow<Long> = lists.activeList
            .filterNotNull()
            .map { it.id }
            .distinctUntilChangedBy { it }

        private val blocked: Flow<List<String>> =
            listId.flatMapLatest { parental.lockedCategories(it, ContentKind.Live) }

        private val categories: Flow<List<Category>> =
            combine(listId.flatMapLatest { live.categories(it) }, blocked) { all, locked ->
                all.map { it.copy(locked = it.externalId in locked) }
            }

        /** Outside the state: it is a flow of pages, not a value. */
        val channels: Flow<PagingData<Channel>> = combine(listId, tab, categoryId, sort) { id, tab, category, sort ->
            Query(id, scopeOf(tab, category), sort)
        }.distinctUntilChangedBy { it }
            .flatMapLatest { query ->
                blocked.flatMapLatest { hidden -> live.channels(query.listId, query.scope, query.sort, hidden) }
            }.cachedIn(viewModelScope)

        val channelCount: Flow<Int> = combine(listId, tab, categoryId) { id, tab, category -> id to scopeOf(tab, category) }
            .distinctUntilChanged()
            .flatMapLatest { (id, scope) -> blocked.flatMapLatest { hidden -> live.channelCount(id, scope, hidden) } }

        private val favorites: Flow<Set<String>> = listId.flatMapLatest { live.favoriteIds(it) }

        private val todayPrograms: Flow<List<Program>> = combine(listId, sheet) { id, sheet -> id to sheet }
            .flatMapLatest { (id, sheet) ->
                val channel = (sheet as? LiveSheet.Programs)?.channel
                val epgId = channel?.epgId
                if (epgId == null) {
                    flow { emit(emptyList()) }
                } else {
                    val start = LocalDate
                        .now()
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                    live.programsBetween(id, epgId, start, start + DayMillis)
                }
            }

        val state: StateFlow<LiveUiState> = combine(
            combine(tab, categoryId, sort, sheet) { tab, category, sort, sheet -> arrayOf<Any?>(tab, category, sort, sheet) },
            categories,
            favorites,
            playback.state,
            todayPrograms,
        ) { head, categories, favorites, playing, programs ->
            val selectedId = head[1] as String?
            LiveUiState(
                tab = head[0] as LiveTab,
                selectedCategory = categories.firstOrNull { it.externalId == selectedId },
                categories = categories.toImmutableList(),
                sort = head[2] as ChannelSort,
                favoriteIds = favorites,
                playingChannelId = playing.session?.channelId,
                sheet = head[3] as LiveSheet?,
                todayPrograms = programs.toImmutableList(),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), LiveUiState())

        private val _effects = EventChannel<LiveEffect>(EventChannel.BUFFERED)
        val effects = _effects.receiveAsFlow()

        init {
            viewModelScope.launch {
                blocked.collect { hidden ->
                    if (categoryId.value in hidden) onCategorySelected(null)
                }
            }
        }

        private val guideEpgIds = MutableStateFlow<Set<String>>(emptySet())
        private val guideWindowStart = MutableStateFlow(0L)

        val guide: StateFlow<Map<String, List<Program>>> =
            combine(listId, guideEpgIds.debounce(RequestDebounceMillis), guideWindowStart) { id, ids, start -> Triple(id, ids, start) }
                .flatMapLatest { (id, ids, start) ->
                    if (start == 0L) flow { emit(emptyMap()) } else live.programsFor(id, ids, start, start + GuideHours * HourMillis)
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), emptyMap())

        fun onGuideWindow(startMillis: Long) {
            guideWindowStart.value = startMillis
        }

        fun requestGuide(epgId: String) {
            // Only the most recently seen, so the set and the query stay bounded.
            guideEpgIds.update { if (epgId in it) it else (it + epgId).toList().takeLast(MaxGuideRows).toSet() }
        }

        private val requestedEpgIds = MutableStateFlow<Set<String>>(emptySet())
        private val ticker = flow {
            while (true) {
                emit(Unit)
                delay(ProgramRefreshMillis)
            }
        }

        val programs: StateFlow<Map<String, Program>> = combine(listId, requestedEpgIds.debounce(RequestDebounceMillis), ticker) { id, ids, _ -> id to ids }
            .flatMapLatest { (id, ids) ->
                flow { emit(live.nowFor(id, ids, System.currentTimeMillis())) }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), emptyMap())

        fun requestProgram(epgId: String) {
            // Only the most recently seen, so the set stays bounded while the list scrolls.
            requestedEpgIds.update { if (epgId in it) it else (it + epgId).toList().takeLast(MaxProgramRows).toSet() }
        }

        fun onTabSelected(selected: LiveTab) {
            if (selected != LiveTab.Category) categoryId.value = null
            tab.value = selected
        }

        fun onCategorySelected(category: Category?) {
            if (category != null && category.locked) {
                pinPrompt.verify(PinPurpose.Unlock) {
                    viewModelScope.launch {
                        parental.unlock(listId.firstValue(), ContentKind.Live, category.externalId)
                        select(category)
                    }
                }
                return
            }
            select(category)
        }

        private fun select(category: Category?) {
            categoryId.value = category?.externalId
            tab.value = if (category == null) LiveTab.All else LiveTab.Category
        }

        fun onPinDigit(digit: Int) = pinPrompt.onDigit(digit)
        fun onPinBackspace() = pinPrompt.onBackspace()
        fun onPinDismiss() = pinPrompt.dismiss()

        fun onSortSelected(selected: ChannelSort) {
            sort.value = selected
        }

        fun onFilterClick() {
            sheet.value = LiveSheet.Filter
        }

        fun onChannelLongClick(channel: Channel) {
            sheet.value = LiveSheet.Programs(channel)
        }

        fun onSheetDismiss() {
            sheet.value = null
        }

        fun onToggleFavorite(channel: Channel) {
            viewModelScope.launch {
                val id = listId.firstValue()
                val add = channel.externalId !in state.value.favoriteIds
                live.setFavorite(id, channel.externalId, add)
                _effects.send(LiveEffect.FavoriteChanged(channel.name, add))
            }
        }

        private suspend fun Flow<Long>.firstValue(): Long = first()

        private fun scopeOf(tab: LiveTab, category: String?): ChannelScope = when {
            tab == LiveTab.Favorites -> ChannelScope.Favorites
            tab == LiveTab.Category && category != null -> ChannelScope.InCategory(category)
            else -> ChannelScope.All
        }

        private data class Query(val listId: Long, val scope: ChannelScope, val sort: ChannelSort)

        private companion object {
            const val StopTimeoutMillis = 5_000L
            const val DayMillis = 24 * 3_600_000L
            const val ProgramRefreshMillis = 30_000L
            const val RequestDebounceMillis = 150L
            const val HourMillis = 3_600_000L
            const val MaxGuideRows = 60
            const val MaxProgramRows = 100
        }
    }
