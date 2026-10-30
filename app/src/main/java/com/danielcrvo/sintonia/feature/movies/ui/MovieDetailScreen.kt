package com.danielcrvo.sintonia.feature.movies.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.danielcrvo.sintonia.core.designsystem.components.SText
import com.danielcrvo.sintonia.core.designsystem.components.initialFocusOnTv
import com.danielcrvo.sintonia.core.designsystem.theme.DeviceClass
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme
import com.danielcrvo.sintonia.core.model.Movie
import com.danielcrvo.sintonia.core.model.VodInfo
import com.danielcrvo.sintonia.core.model.WatchProgress

@Composable
fun MovieDetailScreen(
    state: MovieDetailUiState,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onRetryInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SintoniaTheme.colors
    val spacing = SintoniaTheme.spacing
    val movie = state.movie
    if (movie == null) {
        if (!state.loading) {
            SEmptyState(
                icon = SIcons.Film,
                title = stringResource(R.string.detail_not_found),
                modifier = modifier.background(colors.background),
                action = if (SintoniaTheme.deviceClass == DeviceClass.Tv) {
                    null
                } else {
                    { SButton(stringResource(R.string.action_back), onBack, variant = ButtonVariant.Secondary) }
                },
            )
        }
        return
    }
    val info = (state.info as? InfoState.Loaded)?.info
    val image = info?.backdropUrl ?: movie.posterUrl
    if (SintoniaTheme.deviceClass == DeviceClass.Tv) {
        Row(modifier.fillMaxSize().background(colors.background)) {
            Column(
                Modifier
                    .weight(1.1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.gutter, vertical = spacing.xxl),
                verticalArrangement = Arrangement.spacedBy(spacing.lg),
            ) {
                MovieInfo(movie, info, state, onPlay, onRetryInfo)
            }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                SImage(image, null, Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(colors.background, Color.Transparent))))
            }
        }
        return
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding(),
    ) {
        SDetailHero(imageUrl = image, onBack = onBack, backDescription = stringResource(R.string.action_back))
        Column(
            Modifier.padding(horizontal = spacing.gutter).padding(bottom = spacing.xxxl),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            MovieInfo(movie, info, state, onPlay, onRetryInfo)
        }
    }
}

@Composable
private fun MovieInfo(movie: Movie, info: VodInfo?, state: MovieDetailUiState, onPlay: () -> Unit, onRetryInfo: () -> Unit) {
    SText(movie.name, style = SintoniaTheme.typography.heading, maxLines = 3)
    SMetaLine(metaParts(movie, info))
    PlayBlock(state.progress, onPlay)
    when (state.info) {
        InfoState.Loading -> SSpinner(size = SintoniaTheme.sizes.spinnerInline)
        InfoState.Failed -> FailedInfo(onRetryInfo)
        else -> Unit
    }
    info?.plot?.let { SText(it, color = SintoniaTheme.colors.textSecondary) }
    info?.cast?.let { SInfoRow(stringResource(R.string.detail_cast), it) }
    info?.director?.let { SInfoRow(stringResource(R.string.detail_director), it) }
    info?.genre?.let { SInfoRow(stringResource(R.string.detail_genre), it) }
}

@Composable
private fun metaParts(movie: Movie, info: VodInfo?): List<String> = buildList {
    movie.year?.let { add(it) }
    movie.rating?.let { add(formatRating(it)) }
    info?.durationSecs?.let { add(stringResource(R.string.detail_minutes, it / 60)) }
}

@Composable
private fun PlayBlock(progress: WatchProgress?, onPlay: () -> Unit) {
    val inProgress = progress?.takeIf { it.positionMillis > 0 && !it.finished }
    Column(verticalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.sm)) {
        SButton(
            text = when {
                inProgress != null -> stringResource(R.string.detail_continue_at, formatDuration(inProgress.positionMillis))
                progress?.finished == true -> stringResource(R.string.detail_watch_again)
                else -> stringResource(R.string.detail_watch)
            },
            onClick = onPlay,
            modifier = Modifier.fillMaxWidth().initialFocusOnTv(),
            leadingIcon = SIcons.Play,
            size = ButtonSize.Footer,
        )
        if (inProgress != null) SProgressBar(progress = { inProgress.fraction })
    }
}

@Composable
private fun FailedInfo(onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.sm)) {
        SText(stringResource(R.string.detail_load_error), style = SintoniaTheme.typography.caption, color = SintoniaTheme.colors.textSecondary)
        SButton(stringResource(R.string.error_retry), onRetry, variant = ButtonVariant.Secondary, size = ButtonSize.Compact)
    }
}
