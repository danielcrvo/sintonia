package com.danielcrvo.sintonia.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.danielcrvo.sintonia.R
import com.danielcrvo.sintonia.core.designsystem.components.NavItem
import com.danielcrvo.sintonia.core.designsystem.components.SHeader
import com.danielcrvo.sintonia.core.designsystem.components.SHeaderActions
import com.danielcrvo.sintonia.core.designsystem.components.SIcons
import com.danielcrvo.sintonia.core.designsystem.components.SLogo
import com.danielcrvo.sintonia.core.designsystem.components.SNavigationScaffold
import com.danielcrvo.sintonia.core.designsystem.components.SText
import com.danielcrvo.sintonia.core.designsystem.components.tvSafeArea
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme
import com.danielcrvo.sintonia.core.model.PlayerRequest
import com.danielcrvo.sintonia.feature.catalog.ui.MoviesRoute
import com.danielcrvo.sintonia.feature.catalog.ui.SeriesRoute
import com.danielcrvo.sintonia.feature.home.ui.HomeRoute
import com.danielcrvo.sintonia.feature.live.ui.LiveRoute

class MainActions(
    val onOpenPlayer: (PlayerRequest) -> Unit,
    val onOpenMovie: (Long) -> Unit,
    val onOpenSeries: (Long) -> Unit,
    val onSearch: () -> Unit,
    val onSettings: () -> Unit,
)

@Composable
fun MainScreen(actions: MainActions, modifier: Modifier = Modifier) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val stateHolder = rememberSaveableStateHolder()
    val items = listOf(
        NavItem(SIcons.Home, stringResource(R.string.tab_home)),
        NavItem(SIcons.Tv, stringResource(R.string.tab_live)),
        NavItem(SIcons.Film, stringResource(R.string.tab_movies)),
        NavItem(SIcons.Albums, stringResource(R.string.tab_series)),
    )
    SNavigationScaffold(items = items, selectedIndex = selected, onSelect = { selected = it }, modifier = modifier) {
        val motion = SintoniaTheme.motion
        Column(
            Modifier
                .fillMaxSize()
                .background(SintoniaTheme.colors.background)
                .statusBarsPadding()
                .tvSafeArea(),
        ) {
            MainHeader(selected, actions.onSearch, actions.onSettings)
            AnimatedContent(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                targetState = selected,
                transitionSpec = {
                    val direction = if (targetState > initialState) 1 else -1
                    (slideInHorizontally(motion.slow()) { direction * it / 4 } + fadeIn(motion.normal())) togetherWith
                        (slideOutHorizontally(motion.slow()) { -direction * it / 4 } + fadeOut(motion.fast())) using
                        SizeTransform(clip = false) { _, _ -> snap() }
                },
                label = "mainTab",
            ) { tab ->
                // Scroll and selection per tab survive switching (the tab recomposes from scratch).
                stateHolder.SaveableStateProvider(tab) {
                    when (tab) {
                        0 -> HomeRoute(actions.onOpenMovie, actions.onOpenPlayer)
                        1 -> LiveRoute(onOpenPlayer = actions.onOpenPlayer)
                        2 -> MoviesRoute(onOpenMovie = actions.onOpenMovie)
                        else -> SeriesRoute(onOpenSeries = actions.onOpenSeries)
                    }
                }
            }
        }
    }
}

@Composable
private fun MainHeader(selected: Int, onSearch: () -> Unit, onSettings: () -> Unit) {
    val actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = { SHeaderActions(onSearch, onSettings) }
    if (selected == 0) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(SintoniaTheme.heights.header)
                .padding(horizontal = SintoniaTheme.spacing.gutter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.md),
        ) {
            SLogo(size = SintoniaTheme.sizes.logoSmall)
            SText(stringResource(R.string.app_name), style = SintoniaTheme.typography.heading, modifier = Modifier.weight(1f))
            SHeaderActions(onSearch, onSettings)
        }
    } else {
        val title = when (selected) {
            1 -> R.string.live_title
            2 -> R.string.tab_movies
            else -> R.string.tab_series
        }
        SHeader(title = stringResource(title), actions = actions)
    }
}
