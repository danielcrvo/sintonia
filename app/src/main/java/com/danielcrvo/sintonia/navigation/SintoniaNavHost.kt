package com.danielcrvo.sintonia.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.danielcrvo.sintonia.core.designsystem.components.LocalToast
import com.danielcrvo.sintonia.core.designsystem.components.SToastHost
import com.danielcrvo.sintonia.core.designsystem.components.ToastState
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme
import com.danielcrvo.sintonia.feature.categories.ui.CategoriesRoute
import com.danielcrvo.sintonia.feature.lists.ui.addlist.AddListRoute
import com.danielcrvo.sintonia.feature.lists.ui.importing.ImportRoute
import com.danielcrvo.sintonia.feature.movies.ui.MovieDetailRoute
import com.danielcrvo.sintonia.feature.parental.ui.ParentalRoute
import com.danielcrvo.sintonia.feature.player.ui.PlayerRoute
import com.danielcrvo.sintonia.feature.search.ui.SearchRoute
import com.danielcrvo.sintonia.feature.series.ui.SeriesDetailRoute
import com.danielcrvo.sintonia.feature.settings.ui.ListsRoute
import com.danielcrvo.sintonia.feature.settings.ui.SettingsRoute
import com.danielcrvo.sintonia.feature.welcome.ui.WelcomeScreen

@Composable
fun SintoniaNavHost(hasLists: Boolean) {
    val backStack = rememberNavBackStack(if (hasLists) Main else Welcome)
    val toast = remember { ToastState() }
    val motion = SintoniaTheme.motion

    LaunchedEffect(hasLists) {
        if (!hasLists && backStack.none { it in OnboardingKeys || it is Importing }) {
            backStack.clear()
            backStack.add(Welcome)
        }
    }

    fun resetTo(destination: NavKey) {
        backStack.clear()
        backStack.add(destination)
    }

    CompositionLocalProvider(LocalToast provides toast) {
        Box(Modifier.fillMaxSize()) {
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                transitionSpec = {
                    (slideInHorizontally(motion.slow()) { it / 3 } + fadeIn(motion.normal())) togetherWith
                        (slideOutHorizontally(motion.slow()) { -it / 6 } + fadeOut(motion.normal()))
                },
                popTransitionSpec = {
                    (slideInHorizontally(motion.slow()) { -it / 6 } + fadeIn(motion.normal())) togetherWith
                        (slideOutHorizontally(motion.slow()) { it / 3 } + fadeOut(motion.normal()))
                },
                predictivePopTransitionSpec = {
                    (slideInHorizontally(motion.slow()) { -it / 6 } + fadeIn(motion.normal())) togetherWith
                        (slideOutHorizontally(motion.slow()) { it / 3 } + fadeOut(motion.normal()))
                },
                entryProvider = entryProvider {
                    entry<Welcome> {
                        WelcomeScreen(onAddList = { backStack.add(AddList) })
                    }
                    entry<AddList> {
                        AddListRoute(
                            onBack = { backStack.removeLastOrNull() },
                            onStartImport = { id -> backStack.add(Importing(id, isNewList = true)) },
                            canGoBack = backStack.size > 1,
                        )
                    }
                    entry<Importing> { key ->
                        ImportRoute(
                            listId = key.listId,
                            isNewList = key.isNewList,
                            onFinished = { if (key.isNewList) resetTo(Main) else backStack.removeLastOrNull() },
                            onCancelled = { backStack.removeLastOrNull() },
                            onEditDetails = { backStack.removeLastOrNull() },
                        )
                    }
                    entry<Main> {
                        MainScreen(
                            MainActions(
                                onOpenPlayer = { backStack.add(Player(it)) },
                                onOpenMovie = { backStack.add(MovieDetail(it)) },
                                onOpenSeries = { backStack.add(SeriesDetail(it)) },
                                onSearch = { backStack.add(Search) },
                                onSettings = { backStack.add(Settings) },
                            ),
                        )
                    }
                    entry<MovieDetail> { key ->
                        MovieDetailRoute(
                            movieId = key.movieId,
                            onBack = { backStack.removeLastOrNull() },
                            onOpenPlayer = { backStack.add(Player(it)) },
                        )
                    }
                    entry<SeriesDetail> { key ->
                        SeriesDetailRoute(
                            seriesId = key.seriesId,
                            onBack = { backStack.removeLastOrNull() },
                            onOpenPlayer = { backStack.add(Player(it)) },
                        )
                    }
                    entry<Settings> {
                        SettingsRoute(
                            onBack = { backStack.removeLastOrNull() },
                            onSwitchList = { backStack.add(Lists) },
                            onRefresh = { id -> backStack.add(Importing(id, isNewList = false)) },
                            onOpenParental = { backStack.add(Parental) },
                            onOpenCategories = { backStack.add(Categories) },
                        )
                    }
                    entry<Lists> {
                        ListsRoute(
                            onBack = { backStack.removeLastOrNull() },
                            onAdd = { backStack.add(AddList) },
                        )
                    }
                    entry<Parental> { ParentalRoute(onBack = { backStack.removeLastOrNull() }) }
                    entry<Categories> { CategoriesRoute(onBack = { backStack.removeLastOrNull() }) }
                    entry<Search> {
                        SearchRoute(
                            onBack = { backStack.removeLastOrNull() },
                            onOpenPlayer = { backStack.add(Player(it)) },
                            onOpenMovie = { backStack.add(MovieDetail(it)) },
                            onOpenSeries = { backStack.add(SeriesDetail(it)) },
                        )
                    }
                    // The player fades: it is full-screen and dark, sliding would look odd.
                    entry<Player>(
                        metadata = NavDisplay.transitionSpec { fadeIn(motion.normal()) togetherWith fadeOut(motion.fast()) } +
                            NavDisplay.popTransitionSpec { fadeIn(motion.normal()) togetherWith fadeOut(motion.fast()) } +
                            NavDisplay.predictivePopTransitionSpec { fadeIn(motion.normal()) togetherWith fadeOut(motion.fast()) },
                    ) { key ->
                        PlayerRoute(request = key.request, onClose = { backStack.removeLastOrNull() })
                    }
                },
            )
            SToastHost(toast, Modifier.align(Alignment.BottomCenter))
        }
    }
}

private val OnboardingKeys: Set<NavKey> = setOf(Welcome, AddList)
