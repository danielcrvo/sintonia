package com.danielcrvo.sintonia

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danielcrvo.sintonia.core.data.ListsRepository
import com.danielcrvo.sintonia.core.datastore.AppLanguage
import com.danielcrvo.sintonia.core.datastore.AppPreferences
import com.danielcrvo.sintonia.core.designsystem.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@Immutable
data class AppState(
    val themeMode: ThemeMode,
    val language: AppLanguage,
    val hasLists: Boolean,
)

@HiltViewModel
class AppViewModel
    @Inject
    constructor(
        lists: ListsRepository,
        preferences: AppPreferences,
    ) : ViewModel() {
        val state: StateFlow<AppState?> = combine(
            preferences.themeMode,
            preferences.language,
            lists.lists.map { it.isNotEmpty() },
        ) { theme, language, hasLists -> AppState(theme, language, hasLists) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    }
