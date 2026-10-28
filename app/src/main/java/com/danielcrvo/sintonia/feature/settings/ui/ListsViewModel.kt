package com.danielcrvo.sintonia.feature.settings.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danielcrvo.sintonia.core.data.ListsRepository
import com.danielcrvo.sintonia.core.data.ParentalRepository
import com.danielcrvo.sintonia.core.model.IptvList
import com.danielcrvo.sintonia.core.parental.PinManager
import com.danielcrvo.sintonia.core.parental.PinPromptController
import com.danielcrvo.sintonia.core.parental.PinPromptState
import com.danielcrvo.sintonia.core.parental.PinPurpose
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class ListsUiState(
    val lists: ImmutableList<IptvList> = persistentListOf(),
    val activeId: Long? = null,
    val pendingRemoval: IptvList? = null,
)

/** Each list has its own blocked categories, so switching or adding a list would bypass the block. */
@HiltViewModel
class ListsViewModel
    @Inject
    constructor(
        private val repository: ListsRepository,
        private val parental: ParentalRepository,
        private val pins: PinManager,
    ) : ViewModel() {
        private val pending = MutableStateFlow<IptvList?>(null)
        private val pinPrompt = PinPromptController(pins, viewModelScope)

        val pinState: StateFlow<PinPromptState?> = pinPrompt.state

        val state: StateFlow<ListsUiState> = combine(repository.lists, repository.activeList, pending) { lists, active, pending ->
            ListsUiState(lists.toImmutableList(), active?.id, pending)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), ListsUiState())

        private fun guarded(action: () -> Unit) {
            viewModelScope.launch {
                if (parental.enabled.first() && pins.hasPin()) {
                    pinPrompt.verify(PinPurpose.ManageLists, action)
                } else {
                    action()
                }
            }
        }

        fun onSelect(list: IptvList) = guarded {
            viewModelScope.launch { repository.setActive(list.id) }
        }

        fun onAddRequest(onAllowed: () -> Unit) = guarded(onAllowed)

        fun onRemoveRequest(list: IptvList) = guarded { pending.value = list }

        fun onRemoveCancel() {
            pending.value = null
        }

        fun onRemoveConfirm() {
            val list = pending.value ?: return
            pending.value = null
            viewModelScope.launch { repository.remove(list.id) }
        }

        fun onPinDigit(digit: Int) = pinPrompt.onDigit(digit)

        fun onPinBackspace() = pinPrompt.onBackspace()

        fun onPinDismiss() = pinPrompt.dismiss()

        private companion object {
            const val StopTimeoutMillis = 5_000L
        }
    }
