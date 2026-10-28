package com.danielcrvo.sintonia.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.danielcrvo.sintonia.R
import com.danielcrvo.sintonia.core.common.formatCount
import com.danielcrvo.sintonia.core.common.formatDate
import com.danielcrvo.sintonia.core.datastore.AppLanguage
import com.danielcrvo.sintonia.core.datastore.TrackLanguage
import com.danielcrvo.sintonia.core.designsystem.components.ButtonSize
import com.danielcrvo.sintonia.core.designsystem.components.ButtonVariant
import com.danielcrvo.sintonia.core.designsystem.components.SButton
import com.danielcrvo.sintonia.core.designsystem.components.SGroup
import com.danielcrvo.sintonia.core.designsystem.components.SHeader
import com.danielcrvo.sintonia.core.designsystem.components.SIcons
import com.danielcrvo.sintonia.core.designsystem.components.SOptionRow
import com.danielcrvo.sintonia.core.designsystem.components.SOverlay
import com.danielcrvo.sintonia.core.designsystem.components.SSectionLabel
import com.danielcrvo.sintonia.core.designsystem.components.SSettingsRow
import com.danielcrvo.sintonia.core.designsystem.components.SSwitch
import com.danielcrvo.sintonia.core.designsystem.components.SText
import com.danielcrvo.sintonia.core.designsystem.components.SThemeSwitch
import com.danielcrvo.sintonia.core.designsystem.components.initialFocusOnTv
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme
import com.danielcrvo.sintonia.core.designsystem.theme.ThemeMode
import com.danielcrvo.sintonia.core.model.IptvList
import com.danielcrvo.sintonia.core.model.ListType

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    versionName: String,
    onBack: () -> Unit,
    onSwitchList: () -> Unit,
    onRefresh: () -> Unit,
    onThemeSelected: (ThemeMode) -> Unit,
    onOpenSheet: (SettingsSheet) -> Unit,
    onAutoUpdateToggle: (Boolean) -> Unit,
    onOpenParental: () -> Unit,
    onOpenCategories: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SintoniaTheme.colors
    val spacing = SintoniaTheme.spacing
    Column(modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
        SHeader(title = stringResource(R.string.settings_title), onBack = onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = spacing.gutter)
                .padding(bottom = spacing.xxxl),
            verticalArrangement = Arrangement.spacedBy(spacing.xl),
        ) {
            ActiveListCard(state.activeList, onSwitchList, onRefresh)

            Column {
                SSectionLabel(stringResource(R.string.settings_section_appearance))
                SGroup {
                    SSettingsRow(
                        title = stringResource(R.string.settings_theme),
                        icon = SIcons.Contrast,
                        trailing = { SThemeSwitch(state.theme, onThemeSelected) },
                    )
                    SSettingsRow(
                        title = stringResource(R.string.settings_app_language),
                        icon = SIcons.Globe,
                        value = stringResource(languageLabel(state.language)),
                        onClick = { onOpenSheet(SettingsSheet.AppLanguage) },
                        showDivider = false,
                    )
                }
            }
            Column {
                SSectionLabel(stringResource(R.string.settings_section_playback))
                SGroup {
                    SSettingsRow(
                        title = stringResource(R.string.settings_audio_language),
                        icon = SIcons.Volume,
                        value = stringResource(trackLabel(state.audio)),
                        onClick = { onOpenSheet(SettingsSheet.AudioLanguage) },
                    )
                    SSettingsRow(
                        title = stringResource(R.string.settings_subtitles),
                        icon = SIcons.Subtitles,
                        value = stringResource(trackLabel(state.subtitles)),
                        onClick = { onOpenSheet(SettingsSheet.Subtitles) },
                        showDivider = false,
                    )
                }
            }
            Column {
                SSectionLabel(stringResource(R.string.settings_section_content))
                SGroup {
                    SSettingsRow(
                        title = stringResource(R.string.settings_categories),
                        icon = SIcons.List,
                        onClick = onOpenCategories,
                    )
                    SSettingsRow(
                        title = stringResource(R.string.settings_auto_update),
                        subtitle = stringResource(R.string.settings_auto_update_hint),
                        icon = SIcons.Refresh,
                        onClick = { onAutoUpdateToggle(!state.autoUpdate) },
                        showDivider = false,
                        trailing = { SSwitch(state.autoUpdate, onAutoUpdateToggle) },
                    )
                }
            }
            Column {
                SSectionLabel(stringResource(R.string.settings_section_security))
                SGroup {
                    SSettingsRow(
                        title = stringResource(R.string.settings_parental),
                        icon = SIcons.Lock,
                        value = stringResource(if (state.parentalEnabled) R.string.toggle_on else R.string.toggle_off),
                        onClick = onOpenParental,
                        showDivider = false,
                    )
                }
            }
            SText(
                stringResource(R.string.settings_version, versionName),
                style = SintoniaTheme.typography.mono,
                color = colors.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ActiveListCard(list: IptvList?, onSwitch: () -> Unit, onRefresh: () -> Unit) {
    val colors = SintoniaTheme.colors
    val shape = RoundedCornerShape(SintoniaTheme.radii.card)
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.surface, shape)
            .border(SintoniaTheme.strokes.hairline, colors.border, shape)
            .padding(SintoniaTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.md),
    ) {
        SText(stringResource(R.string.settings_active_list).uppercase(), style = SintoniaTheme.typography.monoLabel, color = colors.textMuted)
        if (list == null) {
            SText(stringResource(R.string.settings_no_list), color = colors.textSecondary)
        } else {
            SText(list.name, style = SintoniaTheme.typography.subheading, maxLines = 1)
            SText(listMeta(list), style = SintoniaTheme.typography.mono, color = colors.textMuted)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            SButton(
                stringResource(R.string.settings_switch_list),
                onSwitch,
                Modifier.weight(1f).initialFocusOnTv(),
                variant = ButtonVariant.Secondary,
                size = ButtonSize.Compact,
            )
            if (list != null) {
                SButton(
                    stringResource(R.string.settings_refresh),
                    onRefresh,
                    Modifier.weight(1f),
                    variant = ButtonVariant.Secondary,
                    size = ButtonSize.Compact,
                    leadingIcon = SIcons.Refresh,
                )
            }
        }
    }
}

@Composable
internal fun listMeta(list: IptvList): String {
    val type = stringResource(typeLabel(list.type))
    val channels = pluralStringResource(R.plurals.lists_counts_channels, list.channelCount, formatCount(list.channelCount))
    val updated = list.lastImportedAt?.let { stringResource(R.string.settings_updated, formatDate(it)) }
        ?: stringResource(R.string.settings_never_updated)
    return stringResource(R.string.meta_three, type, channels, updated)
}

internal fun typeLabel(type: ListType): Int = when (type) {
    ListType.Xtream -> R.string.lists_type_xtream
    ListType.M3uUrl -> R.string.lists_type_url
    ListType.M3uFile -> R.string.lists_type_file
}

internal fun languageLabel(language: AppLanguage): Int = when (language) {
    AppLanguage.Auto -> R.string.language_auto
    AppLanguage.PtBr -> R.string.language_pt
    AppLanguage.En -> R.string.language_en
    AppLanguage.Es -> R.string.language_es
}

internal fun trackLabel(language: TrackLanguage): Int = when (language) {
    TrackLanguage.Auto -> R.string.track_language_auto
    TrackLanguage.Off -> R.string.track_language_off
    TrackLanguage.Pt -> R.string.language_pt
    TrackLanguage.En -> R.string.language_en
    TrackLanguage.Es -> R.string.language_es
}

@Composable
fun SettingsChoiceSheet(
    visible: Boolean,
    title: String,
    options: List<Pair<String, Boolean>>,
    onSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    SOverlay(visible = visible, onDismiss = onDismiss, title = title) {
        options.forEachIndexed { index, (label, selected) -> SOptionRow(label, selected, { onSelected(index) }) }
    }
}
