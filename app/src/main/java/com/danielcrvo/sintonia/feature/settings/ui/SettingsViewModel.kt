package com.danielcrvo.sintonia.feature.settings.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danielcrvo.sintonia.core.data.ListsRepository
import com.danielcrvo.sintonia.core.data.ParentalRepository
import com.danielcrvo.sintonia.core.datastore.AppLanguage
import com.danielcrvo.sintonia.core.datastore.AppPreferences
import com.danielcrvo.sintonia.core.datastore.TrackLanguage
import com.danielcrvo.sintonia.core.designsystem.theme.ThemeMode
import com.danielcrvo.sintonia.core.model.IptvList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SettingsSheet { AppLanguage, AudioLanguage, Subtitles }

@Immutable
data class SettingsUiState(
    val activeList: IptvList? = null,
    val theme: ThemeMode = ThemeMode.Auto,
    val language: AppLanguage = AppLanguage.Auto,
    val audio: TrackLanguage = TrackLanguage.Auto,
    val subtitles: TrackLanguage = TrackLanguage.Off,
    val parentalEnabled: Boolean = false,
    val autoUpdate: Boolean = true,
    val sheet: SettingsSheet? = null,
)

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        lists: ListsRepository,
        private val preferences: AppPreferences,
        parental: ParentalRepository,
    ) : ViewModel() {
        private val sheet = MutableStateFlow<SettingsSheet?>(null)

        val state: StateFlow<SettingsUiState> = combine(
            combine(lists.activeList, preferences.themeMode, preferences.language) { list, theme, language -> Triple(list, theme, language) },
            combine(preferences.audioLanguage, preferences.subtitleLanguage, parental.enabled, preferences.autoUpdate) { audio, subs, parentalOn, auto ->
                listOf(audio, subs, parentalOn, auto)
            },
            sheet,
        ) { (list, theme, language), extras, sheet ->
            SettingsUiState(
                list,
                theme,
                language,
                extras[0] as TrackLanguage,
                extras[1] as TrackLanguage,
                extras[2] as Boolean,
                extras[3] as Boolean,
                sheet,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), SettingsUiState())

        fun onThemeSelected(mode: ThemeMode) {
            viewModelScope.launch { preferences.setThemeMode(mode) }
        }

        fun onAutoUpdateToggle(enabled: Boolean) {
            viewModelScope.launch { preferences.setAutoUpdate(enabled) }
        }

        fun onOpenSheet(which: SettingsSheet) {
            sheet.value = which
        }

        fun onDismissSheet() {
            sheet.value = null
        }

        fun onLanguageSelected(language: AppLanguage) {
            viewModelScope.launch { preferences.setLanguage(language) }
            sheet.value = null
        }

        fun onAudioSelected(language: TrackLanguage) {
            viewModelScope.launch { preferences.setAudioLanguage(language) }
            sheet.value = null
        }

        fun onSubtitlesSelected(language: TrackLanguage) {
            viewModelScope.launch { preferences.setSubtitleLanguage(language) }
            sheet.value = null
        }

        private companion object {
            const val StopTimeoutMillis = 5_000L
        }
    }
