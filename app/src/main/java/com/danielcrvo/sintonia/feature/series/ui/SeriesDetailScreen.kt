package com.danielcrvo.sintonia.feature.series.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.danielcrvo.sintonia.R
import com.danielcrvo.sintonia.core.common.formatDuration
import com.danielcrvo.sintonia.core.common.formatRating
import com.danielcrvo.sintonia.core.designsystem.components.ButtonSize
import com.danielcrvo.sintonia.core.designsystem.components.ButtonVariant
import com.danielcrvo.sintonia.core.designsystem.components.SButton
import com.danielcrvo.sintonia.core.designsystem.components.SDetailHero
import com.danielcrvo.sintonia.core.designsystem.components.SEmptyState
import com.danielcrvo.sintonia.core.designsystem.components.SIcons
import com.danielcrvo.sintonia.core.designsystem.components.SImage
import com.danielcrvo.sintonia.core.designsystem.components.SInfoRow
import com.danielcrvo.sintonia.core.designsystem.components.SMetaLine
import com.danielcrvo.sintonia.core.designsystem.components.SProgressBar
import com.danielcrvo.sintonia.core.designsystem.components.SSpinner
import com.danielcrvo.sintonia.core.designsystem.components.STabs
import com.danielcrvo.sintonia.core.designsystem.components.SText
import com.danielcrvo.sintonia.core.designsystem.components.initialFocusOnTv
import com.danielcrvo.sintonia.core.designsystem.components.sintoniaClickable
import com.danielcrvo.sintonia.core.designsystem.theme.DeviceClass
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme
import com.danielcrvo.sintonia.core.model.Episode
import com.danielcrvo.sintonia.core.model.WatchProgress

@Composable
fun SeriesDetailScreen(
    state: SeriesDetailUiState,
    onBack: () -> Unit,
    onPlayEpisode: (Episode) -> Unit,
    onPlaySingle: () -> Unit,
    onSeasonSelected: (Int) -> Unit,
    onRetryInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SintoniaTheme.colors
    val spacing = SintoniaTheme.spacing
    val series = state.series
    if (series == null) {
        if (!state.loading) {
            SEmptyState(
                icon = SIcons.Albums,
                title = stringResource(R.string.detail_not_found),
                modifier = modifier.background(colors.background),
                action = { SButton(stringResource(R.string.action_back), onBack, variant = ButtonVariant.Secondary) },
            )
        }
        return
    }
    val info = state.loadedInfo
    val season = info?.seasons?.getOrNull(state.selectedSeason)
    if (SintoniaTheme.deviceClass == DeviceClass.Tv) {
        Row(modifier.fillMaxSize().background(colors.background)) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.gutter, vertical = spacing.xxl),
                verticalArrangement = Arrangement.spacedBy(spacing.lg),
            ) {
                SeriesInfo(state, series.name, onPlayEpisode, onPlaySingle, onRetryInfo)
            }
            Column(Modifier.weight(1.2f).fillMaxHeight().padding(top = spacing.xxl)) {
                if (info != null && info.seasons.isNotEmpty()) {
                    STabs(
                        tabs = info.seasons.map { stringResource(R.string.detail_season, it.number) },
                        selectedIndex = state.selectedSeason.coerceIn(0, info.seasons.lastIndex),
                        onSelected = onSeasonSelected,
                    )
                    LazyColumn(
                        Modifier.weight(1f).focusRestorer(),
                        contentPadding = androidx.compose.foundation.layout
                            .PaddingValues(bottom = spacing.xxl),
                    ) {
                        items(season?.episodes.orEmpty(), key = { it.externalId }) { episode ->
                            EpisodeRow(episode, state.progress[episode.externalId], onClick = { onPlayEpisode(episode) })
                        }
                    }
                }
            }
        }
        return
    }
    LazyColumn(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .navigationBarsPadding()
            .focusRestorer(),
    ) {
        item(key = "hero") {
            SDetailHero(imageUrl = info?.backdropUrl ?: series.posterUrl, onBack = onBack, backDescription = stringResource(R.string.action_back))
        }
        item(key = "header") {
            Column(
                Modifier.padding(horizontal = spacing.gutter),
                verticalArrangement = Arrangement.spacedBy(spacing.lg),
            ) {
                SeriesInfo(state, series.name, onPlayEpisode, onPlaySingle, onRetryInfo)
            }
        }
        if (info != null && info.seasons.isNotEmpty()) {
            item(key = "seasons") {
                STabs(
                    tabs = info.seasons.map { stringResource(R.string.detail_season, it.number) },
                    selectedIndex = state.selectedSeason.coerceIn(0, info.seasons.lastIndex),
                    onSelected = onSeasonSelected,
                    modifier = Modifier.padding(top = spacing.xl),
                )
            }
            items(season?.episodes.orEmpty(), key = { it.externalId }) { episode ->
                EpisodeRow(episode, state.progress[episode.externalId], onClick = { onPlayEpisode(episode) })
            }
        }
        item(key = "bottom") { Column(Modifier.padding(bottom = spacing.xxxl)) {} }
    }
}

@Composable
private fun SeriesInfo(
    state: SeriesDetailUiState,
    name: String,
    onPlayEpisode: (Episode) -> Unit,
    onPlaySingle: () -> Unit,
    onRetryInfo: () -> Unit,
) {
    val colors = SintoniaTheme.colors
    val series = state.series ?: return
    val info = state.loadedInfo
    SText(name, style = SintoniaTheme.typography.heading, maxLines = 3)
    SMetaLine(
        buildList {
            series.year?.let { add(it) }
            series.rating?.let { add(formatRating(it)) }
            info?.let { add(pluralStringResource(R.plurals.detail_season_count, it.seasons.size, it.seasons.size)) }
        },
    )
    PrimaryAction(state, onPlayEpisode, onPlaySingle)
    when (state.info) {
        SeriesInfoState.Loading -> SSpinner(size = SintoniaTheme.sizes.spinnerInline)
        SeriesInfoState.Failed -> Column(verticalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.sm)) {
            SText(stringResource(R.string.detail_load_error), style = SintoniaTheme.typography.caption, color = colors.textSecondary)
            SButton(stringResource(R.string.error_retry), onRetryInfo, variant = ButtonVariant.Secondary, size = ButtonSize.Compact)
        }
        else -> Unit
    }
    info?.plot?.let { SText(it, color = colors.textSecondary) }
    info?.cast?.let { SInfoRow(stringResource(R.string.detail_cast), it) }
    info?.genre?.let { SInfoRow(stringResource(R.string.detail_genre), it) }
}

@Composable
private fun PrimaryAction(state: SeriesDetailUiState, onPlayEpisode: (Episode) -> Unit, onPlaySingle: () -> Unit) {
    when (state.info) {
        SeriesInfoState.Unavailable -> SButton(
            stringResource(R.string.detail_watch),
            onPlaySingle,
            Modifier.fillMaxWidth().initialFocusOnTv(),
            leadingIcon = SIcons.Play,
            size = ButtonSize.Footer,
        )
        is SeriesInfoState.Loaded -> {
            val resume = state.resumeEpisode ?: return
            val started = state.progress.containsKey(resume.externalId)
            SButton(
                text = stringResource(
                    R.string.detail_continue_episode,
                    stringResource(if (started) R.string.detail_continue else R.string.detail_watch),
                    resume.season,
                    resume.number,
                ),
                onClick = { onPlayEpisode(resume) },
                modifier = Modifier.fillMaxWidth().initialFocusOnTv(),
                leadingIcon = SIcons.Play,
                size = ButtonSize.Footer,
            )
        }
        else -> Unit
    }
}

@Composable
private fun EpisodeRow(episode: Episode, progress: WatchProgress?, onClick: () -> Unit) {
    val colors = SintoniaTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sintoniaClickable(onClick = onClick, cornerRadius = SintoniaTheme.radii.control)
            .padding(horizontal = SintoniaTheme.spacing.gutter, vertical = SintoniaTheme.spacing.md),
        horizontalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.width(SintoniaTheme.sizes.episodeThumbWidth)) {
            SImage(
                episode.imageUrl,
                null,
                Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(
                    androidx.compose.foundation.shape
                        .RoundedCornerShape(SintoniaTheme.radii.thumb),
                ),
            )
            if (progress != null && !progress.finished && progress.positionMillis > 0) {
                SProgressBar(progress = { progress.fraction }, modifier = Modifier.padding(top = SintoniaTheme.spacing.xs))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(SintoniaTheme.sizes.tightGap)) {
            SText(
                stringResource(R.string.detail_episode_label, episode.number),
                style = SintoniaTheme.typography.monoLabel,
                color = colors.textMuted,
            )
            SText(episode.title, style = SintoniaTheme.typography.bodyStrong, maxLines = 2)
            episode.durationSecs?.let {
                SText(formatDuration(it * 1000L), style = SintoniaTheme.typography.mono, color = colors.textMuted)
            }
        }
        if (progress?.finished == true) SIconDone()
    }
}

@Composable
private fun SIconDone() {
    com.danielcrvo.sintonia.core.designsystem.components.SIcon(
        SIcons.Check,
        contentDescription = null,
        tint = SintoniaTheme.colors.textMuted,
    )
}
