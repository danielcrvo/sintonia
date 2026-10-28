package com.danielcrvo.sintonia.feature.settings.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.danielcrvo.sintonia.BuildConfig
import com.danielcrvo.sintonia.R
import com.danielcrvo.sintonia.core.datastore.AppLanguage
import com.danielcrvo.sintonia.core.datastore.TrackLanguage

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onSwitchList: () -> Unit,
    onRefresh: (listId: Long) -> Unit,
    onOpenParental: () -> Unit,
    onOpenCategories: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        versionName = BuildConfig.VERSION_NAME,
        onBack = onBack,
        onSwitchList = onSwitchList,
        onRefresh = { state.activeList?.let { onRefresh(it.id) } },
        onThemeSelected = viewModel::onThemeSelected,
        onOpenSheet = viewModel::onOpenSheet,
        onAutoUpdateToggle = viewModel::onAutoUpdateToggle,
        onOpenParental = onOpenParental,
        onOpenCategories = onOpenCategories,
        modifier = modifier,
    )

    val languages = AppLanguage.entries
    SettingsChoiceSheet(
        visible = state.sheet == SettingsSheet.AppLanguage,
        title = stringResource(R.string.settings_app_language),
        options = languages.map { stringResource(languageLabel(it)) to (it == state.language) },
        onSelected = { viewModel.onLanguageSelected(languages[it]) },
        onDismiss = viewModel::onDismissSheet,
    )
    val audio = listOf(TrackLanguage.Auto, TrackLanguage.Pt, TrackLanguage.En, TrackLanguage.Es)
    SettingsChoiceSheet(
        visible = state.sheet == SettingsSheet.AudioLanguage,
        title = stringResource(R.string.settings_audio_language),
        options = audio.map { stringResource(trackLabel(it)) to (it == state.audio) },
        onSelected = { viewModel.onAudioSelected(audio[it]) },
        onDismiss = viewModel::onDismissSheet,
    )
    val subtitles = listOf(TrackLanguage.Off, TrackLanguage.Pt, TrackLanguage.En, TrackLanguage.Es)
    SettingsChoiceSheet(
        visible = state.sheet == SettingsSheet.Subtitles,
        title = stringResource(R.string.settings_subtitles),
        options = subtitles.map { stringResource(trackLabel(it)) to (it == state.subtitles) },
        onSelected = { viewModel.onSubtitlesSelected(subtitles[it]) },
        onDismiss = viewModel::onDismissSheet,
    )
}
