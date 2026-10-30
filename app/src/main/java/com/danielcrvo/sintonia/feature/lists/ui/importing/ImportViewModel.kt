package com.danielcrvo.sintonia.feature.lists.ui.importing

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danielcrvo.sintonia.core.data.ListsRepository
import com.danielcrvo.sintonia.core.data.importing.ImportProgress
import com.danielcrvo.sintonia.core.data.importing.ListImporter
import com.danielcrvo.sintonia.core.network.ApiException
import com.danielcrvo.sintonia.core.network.toApiException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class ImportFailure(val kind: ApiException.Kind, val host: String?, val httpCode: Int?)

@Immutable
data class ImportUiState(
    val progress: ImportProgress = ImportProgress(),
    val failure: ImportFailure? = null,
)

sealed interface ImportEffect {
    data object Finished : ImportEffect

    data object Cancelled : ImportEffect

    data object EditDetails : ImportEffect
}

@HiltViewModel
class ImportViewModel
    @Inject
    constructor(
        private val importer: ListImporter,
        private val lists: ListsRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow(ImportUiState())
        val state: StateFlow<ImportUiState> = _state.asStateFlow()

        private val _effects = Channel<ImportEffect>(Channel.BUFFERED)
        val effects = _effects.receiveAsFlow()

        private var listId: Long? = null
        private var discardOnExit = false
        private var job: Job? = null
        private var leaving = false

        /** Starts once per list; repeated calls (recomposition, rotation) do not restart it. */
        fun start(listId: Long, discardOnExit: Boolean) {
            if (this.listId == listId) return
            this.listId = listId
            this.discardOnExit = discardOnExit
            run()
        }

        fun retry() {
            leaving = false
            _state.update { ImportUiState() }
            run()
        }

        fun cancel() {
            if (leaving) return
            leaving = true
            job?.cancel()
            viewModelScope.launch {
                discardIfNew()
                _effects.send(ImportEffect.Cancelled)
            }
        }

        fun editDetails() {
            if (leaving) return
            leaving = true
            viewModelScope.launch {
                discardIfNew()
                _effects.send(ImportEffect.EditDetails)
            }
        }

        private fun run() {
            val id = listId ?: return
            job?.cancel()
            job = viewModelScope.launch {
                importer
                    .import(id)
                    .catch { error ->
                        val api = error.toApiException()
                        _state.update { it.copy(failure = ImportFailure(api.kind, api.host, api.httpCode)) }
                    }.collect { progress -> _state.update { it.copy(progress = progress) } }
                if (_state.value.failure == null) _effects.send(ImportEffect.Finished)
            }
        }

        private suspend fun discardIfNew() {
            val id = listId ?: return
            if (discardOnExit) lists.remove(id)
        }
    }
