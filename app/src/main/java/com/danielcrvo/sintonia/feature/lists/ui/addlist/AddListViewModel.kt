package com.danielcrvo.sintonia.feature.lists.ui.addlist

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danielcrvo.sintonia.core.data.ListFileStore
import com.danielcrvo.sintonia.core.data.ListsRepository
import com.danielcrvo.sintonia.core.data.NewList
import com.danielcrvo.sintonia.core.data.PickedFile
import com.danielcrvo.sintonia.core.network.Xtream
import com.danielcrvo.sintonia.feature.lists.domain.FieldErrors
import com.danielcrvo.sintonia.feature.lists.domain.FormField
import com.danielcrvo.sintonia.feature.lists.domain.ListFormValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AddListTab { Xtream, Url, File }

@Immutable
data class AddListUiState(
    val tab: AddListTab = AddListTab.Xtream,
    val name: String = "",
    val server: String = "",
    val username: String = "",
    val password: String = "",
    val url: String = "",
    val file: PickedFile? = null,
    val errors: FieldErrors = emptyMap(),
    val busy: Boolean = false,
)

sealed interface AddListEffect {
    data class StartImport(val listId: Long) : AddListEffect

    data object FileReadFailed : AddListEffect
}

@HiltViewModel
class AddListViewModel
    @Inject
    constructor(
        private val lists: ListsRepository,
        private val files: ListFileStore,
    ) : ViewModel() {
        private val _state = MutableStateFlow(AddListUiState())
        val state: StateFlow<AddListUiState> = _state.asStateFlow()

        private val _effects = Channel<AddListEffect>(Channel.BUFFERED)
        val effects = _effects.receiveAsFlow()

        fun onTabChange(tab: AddListTab) = _state.update { it.copy(tab = tab, errors = emptyMap()) }
        fun onNameChange(value: String) = _state.update { it.copy(name = value) }
        fun onServerChange(value: String) = _state.update { it.copy(server = value, errors = it.errors - FormField.Server) }
        fun onUsernameChange(value: String) = _state.update { it.copy(username = value, errors = it.errors - FormField.Username) }
        fun onPasswordChange(value: String) = _state.update { it.copy(password = value, errors = it.errors - FormField.Password) }
        fun onUrlChange(value: String) = _state.update { it.copy(url = value, errors = it.errors - FormField.Url) }

        fun onFilePicked(uri: Uri) {
            viewModelScope.launch {
                val picked = files.copyFrom(uri)
                if (picked == null) {
                    _effects.send(AddListEffect.FileReadFailed)
                    return@launch
                }
                val previous = _state.value.file
                _state.update { it.copy(file = picked, errors = it.errors - FormField.File) }
                previous?.let { files.delete(it.path) }
            }
        }

        fun onSubmit() {
            val current = _state.value
            if (current.busy) return
            val errors = when (current.tab) {
                AddListTab.Xtream -> ListFormValidator.validateXtream(current.server, current.username, current.password)
                AddListTab.Url -> ListFormValidator.validateLink(current.url)
                AddListTab.File -> ListFormValidator.validateFile(current.file != null)
            }
            if (errors.isNotEmpty()) {
                _state.update { it.copy(errors = errors) }
                return
            }
            _state.update { it.copy(busy = true) }
            viewModelScope.launch {
                val id = lists.create(newListFrom(current))
                _state.update { it.copy(busy = false) }
                _effects.send(AddListEffect.StartImport(id))
            }
        }

        private fun newListFrom(state: AddListUiState): NewList = when (state.tab) {
            AddListTab.Xtream -> {
                val server = Xtream.normalizeServer(state.server)
                NewList.Xtream(
                    name = state.name.trim().ifEmpty { ListFormValidator.nameFromServer(server) },
                    server = server,
                    username = state.username.trim(),
                    password = state.password,
                )
            }
            AddListTab.Url -> NewList.M3uUrl(
                name = state.name.trim().ifEmpty { ListFormValidator.nameFromServer(state.url) },
                url = state.url.trim(),
            )
            AddListTab.File -> {
                val file = checkNotNull(state.file)
                NewList.M3uFile(
                    name = state.name.trim().ifEmpty { ListFormValidator.nameFromFile(file.displayName) },
                    filePath = file.path,
                )
            }
        }
    }
