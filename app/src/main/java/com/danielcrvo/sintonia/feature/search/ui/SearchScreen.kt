package com.danielcrvo.sintonia.feature.search.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.danielcrvo.sintonia.R
import com.danielcrvo.sintonia.core.data.SearchResults
import com.danielcrvo.sintonia.core.designsystem.components.SBadge
import com.danielcrvo.sintonia.core.designsystem.components.SEmptyState
import com.danielcrvo.sintonia.core.designsystem.components.SIconButton
import com.danielcrvo.sintonia.core.designsystem.components.SIcons
import com.danielcrvo.sintonia.core.designsystem.components.SImage
import com.danielcrvo.sintonia.core.designsystem.components.SSectionLabel
import com.danielcrvo.sintonia.core.designsystem.components.SText
import com.danielcrvo.sintonia.core.designsystem.components.STextField
import com.danielcrvo.sintonia.core.designsystem.components.sintoniaClickable
import com.danielcrvo.sintonia.core.designsystem.components.sintoniaItemMotion
import com.danielcrvo.sintonia.core.designsystem.theme.DeviceClass
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme
import com.danielcrvo.sintonia.core.model.Channel
import com.danielcrvo.sintonia.core.model.Movie
import com.danielcrvo.sintonia.core.model.Series

@Composable
fun SearchScreen(
    typed: String,
    state: SearchUiState,
    onQueryChange: (String) -> Unit,
    onCancel: () -> Unit,
    onChannelClick: (Channel) -> Unit,
    onMovieClick: (Movie) -> Unit,
    onSeriesClick: (Series) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SintoniaTheme.colors
    val spacing = SintoniaTheme.spacing
    val focus = remember { FocusRequester() }
    // On TV there is no autofocus: it would open the keyboard and trap the D-pad.
    val tv = SintoniaTheme.deviceClass == DeviceClass.Tv
    LaunchedEffect(Unit) { if (!tv) runCatching { focus.requestFocus() } }

    Column(modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.gutter, vertical = spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            if (SintoniaTheme.deviceClass != DeviceClass.Tv) {
                SIconButton(SIcons.ArrowBack, stringResource(R.string.search_cancel), onCancel)
            }
            STextField(
                value = typed,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f).focusRequester(focus),
                placeholder = stringResource(R.string.search_hint),
                leadingIcon = SIcons.Search,
                imeAction = ImeAction.Search,
            )
        }
        when {
            !state.searched -> SEmptyState(
                icon = SIcons.Search,
                title = stringResource(R.string.search_title),
                message = stringResource(R.string.search_idle_message),
            )
            state.results.isEmpty -> SEmptyState(
                icon = SIcons.Search,
                title = stringResource(R.string.search_empty_title),
                message = stringResource(R.string.search_empty_message),
            )
            else -> Results(state.results, onChannelClick, onMovieClick, onSeriesClick)
        }
    }
}

@Composable
private fun Results(
    results: SearchResults,
    onChannelClick: (Channel) -> Unit,
    onMovieClick: (Movie) -> Unit,
    onSeriesClick: (Series) -> Unit,
) {
    val spacing = SintoniaTheme.spacing
    LazyColumn(
        Modifier.fillMaxSize().focusRestorer(),
        contentPadding = androidx.compose.foundation.layout
            .PaddingValues(bottom = spacing.xxxl),
    ) {
        if (results.channels.isNotEmpty()) {
            item(key = "h-channels") { SectionTitle(R.string.tab_live) }
            items(results.channels, key = { "c${it.id}" }) {
                ResultRow(it.name, it.logoUrl, round = true, typeRes = R.string.search_type_channel, modifier = sintoniaItemMotion()) { onChannelClick(it) }
            }
        }
        if (results.movies.isNotEmpty()) {
            item(key = "h-movies") { SectionTitle(R.string.tab_movies) }
            items(results.movies, key = { "m${it.id}" }) {
                ResultRow(
                    it.name,
                    it.posterUrl,
                    round = false,
                    typeRes = R.string.search_type_movie,
                    meta = it.year,
                    modifier = sintoniaItemMotion(),
                ) { onMovieClick(it) }
            }
        }
        if (results.series.isNotEmpty()) {
            item(key = "h-series") { SectionTitle(R.string.tab_series) }
            items(results.series, key = { "s${it.id}" }) {
                ResultRow(
                    it.name,
                    it.posterUrl,
                    round = false,
                    typeRes = R.string.search_type_series,
                    meta = it.year,
                    modifier = sintoniaItemMotion(),
                ) { onSeriesClick(it) }
            }
        }
    }
}

@Composable
private fun SectionTitle(label: Int) {
    SSectionLabel(
        stringResource(label),
        Modifier.padding(start = SintoniaTheme.spacing.gutter, top = SintoniaTheme.spacing.lg),
    )
}

@Composable
private fun ResultRow(
    title: String,
    imageUrl: String?,
    round: Boolean,
    typeRes: Int,
    modifier: Modifier = Modifier,
    meta: String? = null,
    onClick: () -> Unit,
) {
    val colors = SintoniaTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .sintoniaClickable(onClick = onClick, cornerRadius = SintoniaTheme.radii.control)
            .padding(horizontal = SintoniaTheme.spacing.gutter, vertical = SintoniaTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.md),
    ) {
        SImage(
            imageUrl,
            null,
            Modifier
                .size(width = SintoniaTheme.sizes.avatar, height = if (round) SintoniaTheme.sizes.avatar else SintoniaTheme.sizes.thumbTallHeight)
                .clip(if (round) CircleShape else RoundedCornerShape(SintoniaTheme.radii.thumb)),
        )
        Column(Modifier.weight(1f)) {
            SText(title, style = SintoniaTheme.typography.bodyLarge, maxLines = 1)
            if (meta != null) SText(meta, style = SintoniaTheme.typography.mono, color = colors.textMuted)
        }
        SBadge(stringResource(typeRes))
    }
}
