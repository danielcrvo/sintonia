package com.danielcrvo.sintonia.feature.catalog.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.danielcrvo.sintonia.R
import com.danielcrvo.sintonia.core.designsystem.components.SIcons
import com.danielcrvo.sintonia.core.parental.ui.PinPromptSheet

private val MovieLabels = CatalogLabels(R.string.tab_movies, R.plurals.movies_count, R.string.movies_empty_title, SIcons.Film)
private val SeriesLabels = CatalogLabels(R.string.tab_series, R.plurals.series_count, R.string.series_empty_title, SIcons.Albums)

@Composable
fun MoviesRoute(
    onOpenMovie: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MoviesViewModel = hiltViewModel(),
) = CatalogRoute(MovieLabels, viewModel, onOpenMovie, modifier)

@Composable
fun SeriesRoute(
    onOpenSeries: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SeriesViewModel = hiltViewModel(),
) = CatalogRoute(SeriesLabels, viewModel, onOpenSeries, modifier)

@Composable
private fun CatalogRoute(
    labels: CatalogLabels,
    viewModel: CatalogViewModel,
    onOpen: (Long) -> Unit,
    modifier: Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val items = viewModel.items.collectAsLazyPagingItems()
    val itemCount by viewModel.itemCount.collectAsStateWithLifecycle(initialValue = 0)
    CatalogScreen(
        labels = labels,
        state = state,
        items = items,
        count = itemCount,
        onAllSelected = viewModel::onAllSelected,
        onCategorySelected = viewModel::onCategorySelected,
        onFilterClick = viewModel::onFilterClick,
        onItemClick = { onOpen(it.id) },
        modifier = modifier,
    )
    val pin by viewModel.pinState.collectAsStateWithLifecycle()
    PinPromptSheet(pin, viewModel::onPinDigit, viewModel::onPinBackspace, viewModel::onPinDismiss)

    CatalogFilterSheet(
        visible = state.filterOpen,
        sort = state.sort,
        categories = state.categories,
        selectedCategoryId = state.selectedCategory?.externalId,
        onSortSelected = viewModel::onSortSelected,
        onCategorySelected = viewModel::onCategorySelected,
        onDismiss = viewModel::onFilterDismiss,
    )
}
