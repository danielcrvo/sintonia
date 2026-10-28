package com.danielcrvo.sintonia.feature.parental.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danielcrvo.sintonia.core.data.ListsRepository
import com.danielcrvo.sintonia.core.data.LiveRepository
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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class BlockableCategory(val id: String, val name: String, val blocked: Boolean)

@Immutable
data class ParentalSection(
    val kind: ContentKind,
    val categories: ImmutableList<BlockableCategory>,
) {
    val blockedCount: Int get() = categories.count { it.blocked }
}

@Immutable
data class ParentalUiState(
    val enabled: Boolean = false,
    val unlocked: Boolean = false,
    val sections: ImmutableList<ParentalSection> = persistentListOf(),
    val expanded: ContentKind? = null,
)

sealed interface ParentalEffect {
    data object Leave : ParentalEffect
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ParentalViewModel
    @Inject
    constructor(
        lists: ListsRepository,
        private val parental: ParentalRepository,
        live: LiveRepository,
        movies: MovieRepository,
        series: SeriesRepository,
        private val pins: PinManager,
    ) : ViewModel() {
        private val pinPrompt = PinPromptController(pins, viewModelScope)
        val pinState: StateFlow<PinPromptState?> = pinPrompt.state

        private val unlocked = MutableStateFlow(false)
        private val expanded = MutableStateFlow<ContentKind?>(null)

        private val _effects = Channel<ParentalEffect>(Channel.BUFFERED)
        val effects = _effects.receiveAsFlow()

        private val listId: Flow<Long> = lists.activeList
            .filterNotNull()
            .map { it.id }
            .distinctUntilChanged()

        private fun section(kind: ContentKind, categories: (Long) -> Flow<List<Category>>): Flow<ParentalSection> =
            listId.flatMapLatest { id ->
                combine(categories(id), parental.configuredBlocked(id, kind)) { all, blocked ->
                    ParentalSection(kind, all.map { BlockableCategory(it.externalId, it.name, it.externalId in blocked) }.toImmutableList())
                }
            }

        val state: StateFlow<ParentalUiState> = combine(
            parental.enabled,
            unlocked,
            combine(
                section(ContentKind.Live, live::allCategories),
                section(ContentKind.Movie, movies::allCategories),
                section(ContentKind.Series, series::allCategories),
            ) { a, b, c -> persistentListOf(a, b, c) },
            expanded,
        ) { enabled, unlocked, sections, expanded ->
            ParentalUiState(enabled, unlocked || !enabled, sections, expanded)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), ParentalUiState())

        init {
            // Left the app: ask for the PIN again instead of leaving the screen open.
            viewModelScope.launch {
                parental.relockCount.drop(1).collect {
                    unlocked.value = false
                    onOpen()
                }
            }
        }

        fun onOpen() {
            viewModelScope.launch {
                if (parental.enabled.first() && !unlocked.value && pins.hasPin()) {
                    pinPrompt.verify(PinPurpose.EnterSettings) { unlocked.value = true }
                }
            }
        }

        fun onToggleEnabled(on: Boolean) {
            if (on) {
                pinPrompt.create(PinPurpose.Create) {
                    viewModelScope.launch {
                        parental.setEnabled(true)
                        unlocked.value = true
                    }
                }
            } else {
                pinPrompt.verify(PinPurpose.TurnOff) {
                    viewModelScope.launch {
                        parental.setEnabled(false)
                        pins.clear()
                        parental.relock()
                    }
                }
            }
        }

        fun onChangePin() {
            pinPrompt.verify(PinPurpose.CurrentForChange) { pinPrompt.create(PinPurpose.CreateNew) {} }
        }

        fun onToggleSection(kind: ContentKind) {
            expanded.update { if (it == kind) null else kind }
        }

        fun onToggleCategory(kind: ContentKind, categoryId: String, blocked: Boolean) {
            viewModelScope.launch {
                val id = listId.first()
                val current = parental.configuredBlocked(id, kind).first()
                parental.setBlocked(id, kind, if (blocked) current + categoryId else current - categoryId)
            }
        }

        fun onPinDigit(digit: Int) = pinPrompt.onDigit(digit)

        fun onPinBackspace() = pinPrompt.onBackspace()

        /** Closing the entry PIN prompt leaves the screen; closing the others only cancels the action. */
        fun onPinDismiss() {
            val wasEntering = pinPrompt.state.value?.purpose == PinPurpose.EnterSettings
            pinPrompt.dismiss()
            if (wasEntering) viewModelScope.launch { _effects.send(ParentalEffect.Leave) }
        }

        private companion object {
            const val StopTimeoutMillis = 5_000L
        }
    }
