package com.danielcrvo.sintonia.feature.catalog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.danielcrvo.sintonia.R
import com.danielcrvo.sintonia.core.common.formatCount
import com.danielcrvo.sintonia.core.data.CatalogSort
import com.danielcrvo.sintonia.core.designsystem.components.ButtonSize
import com.danielcrvo.sintonia.core.designsystem.components.ButtonVariant
import com.danielcrvo.sintonia.core.designsystem.components.SButton
import com.danielcrvo.sintonia.core.designsystem.components.SEmptyState
import com.danielcrvo.sintonia.core.designsystem.components.SIcon
import com.danielcrvo.sintonia.core.designsystem.components.SIcons
import com.danielcrvo.sintonia.core.designsystem.components.SOptionRow
import com.danielcrvo.sintonia.core.designsystem.components.SOverlay
import com.danielcrvo.sintonia.core.designsystem.components.SPoster
import com.danielcrvo.sintonia.core.designsystem.components.SSpinner
import com.danielcrvo.sintonia.core.designsystem.components.STabs
import com.danielcrvo.sintonia.core.designsystem.components.SText
import com.danielcrvo.sintonia.core.designsystem.components.sintoniaClickable
import com.danielcrvo.sintonia.core.designsystem.components.sintoniaItemMotion
import com.danielcrvo.sintonia.core.designsystem.theme.DeviceClass
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme
import com.danielcrvo.sintonia.core.model.Category

@androidx.compose.runtime.Immutable
data class CatalogLabels(
    val title: Int,
    val countPlural: Int,
    val emptyTitle: Int,
    val emptyIcon: Int,
)

@Composable
fun CatalogScreen(
    labels: CatalogLabels,
    state: CatalogUiState,
    items: LazyPagingItems<PosterItem>,
    modifier: Modifier = Modifier,
    count: Int = items.itemCount,
    onAllSelected: () -> Unit,
    onCategorySelected: (Category) -> Unit,
    onFilterClick: () -> Unit,
    onItemClick: (PosterItem) -> Unit,
) {
    val colors = SintoniaTheme.colors
    val spacing = SintoniaTheme.spacing
    val columns = if (SintoniaTheme.deviceClass == DeviceClass.Tv) 6 else 3
    val allLabel = stringResource(R.string.catalog_all)
    val categories = remember(state.categories) { state.categories.filter { it.count > 0 || it.name != it.originalName } }
    val tabs = remember(categories, allLabel) { listOf(allLabel) + categories.map { it.name } }
    val selectedIndex = 1 + categories.indexOfFirst { it.externalId == state.selectedCategory?.externalId }
    Column(modifier.fillMaxSize().background(colors.background)) {
        STabs(
            tabs = tabs,
            selectedIndex = selectedIndex.coerceAtLeast(0),
            onSelected = { if (it == 0) onAllSelected() else onCategorySelected(categories[it - 1]) },
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.gutter, vertical = spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            SText(
                pluralStringResource(labels.countPlural, count, formatCount(count)),
                style = SintoniaTheme.typography.mono,
                color = colors.textMuted,
            )
            Row(
                modifier = Modifier
                    .sintoniaClickable(onClick = onFilterClick, cornerRadius = SintoniaTheme.radii.control)
                    .padding(horizontal = spacing.sm, vertical = spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                SIcon(SIcons.Options, null, tint = colors.textSecondary, size = SintoniaTheme.sizes.iconSmall)
                SText(stringResource(R.string.live_filter), style = SintoniaTheme.typography.label, color = colors.textSecondary)
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            val refresh = items.loadState.refresh
            when {
                refresh is LoadState.Loading && items.itemCount == 0 -> Box(
                    Modifier.fillMaxSize(),
                    Alignment.Center,
                ) { SSpinner(size = SintoniaTheme.sizes.spinnerList) }
                refresh is LoadState.Error && items.itemCount == 0 -> SEmptyState(
                    icon = SIcons.AlertCircle,
                    title = stringResource(R.string.load_error_title),
                    action = { SButton(stringResource(R.string.error_retry), { items.retry() }, variant = ButtonVariant.Secondary) },
                )
                items.itemCount == 0 -> SEmptyState(
                    icon = labels.emptyIcon,
                    title = stringResource(labels.emptyTitle),
                    message = stringResource(R.string.catalog_empty_message),
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier.fillMaxSize().focusRestorer(),
                    contentPadding = PaddingValues(horizontal = spacing.gutter, vertical = spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.xl),
                ) {
                    items(
                        count = items.itemCount,
                        key = items.itemKey { it.id },
                        contentType = items.itemContentType { "poster" },
                    ) { index ->
                        val item = items[index] ?: return@items
                        SPoster(
                            modifier = sintoniaItemMotion(),
                            title = item.title,
                            imageUrl = item.imageUrl,
                            meta = item.meta,
                            onClick = { onItemClick(item) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CatalogFilterSheet(
    visible: Boolean,
    sort: CatalogSort,
    categories: List<Category>,
    selectedCategoryId: String?,
    onSortSelected: (CatalogSort) -> Unit,
    onCategorySelected: (Category?) -> Unit,
    onDismiss: () -> Unit,
) {
    SOverlay(visible = visible, onDismiss = onDismiss, title = stringResource(R.string.live_filter), scrollable = false) {
        CatalogFilterContent(sort, categories, selectedCategoryId, onSortSelected, onCategorySelected, onDismiss)
    }
}

@Composable
fun ColumnScope.CatalogFilterContent(
    sort: CatalogSort,
    categories: List<Category>,
    selectedCategoryId: String?,
    onSortSelected: (CatalogSort) -> Unit,
    onCategorySelected: (Category?) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = SintoniaTheme.colors
    SText(stringResource(R.string.filter_sort_by).uppercase(), style = SintoniaTheme.typography.monoLabel, color = colors.textMuted)
    SOptionRow(stringResource(R.string.sort_name), sort == CatalogSort.Name, { onSortSelected(CatalogSort.Name) })
    SOptionRow(stringResource(R.string.sort_year), sort == CatalogSort.Newest, { onSortSelected(CatalogSort.Newest) })
    SText(
        stringResource(R.string.filter_category).uppercase(),
        style = SintoniaTheme.typography.monoLabel,
        color = colors.textMuted,
        modifier = Modifier.padding(top = SintoniaTheme.spacing.sm),
    )
    LazyColumn(Modifier.heightIn(max = SintoniaTheme.sizes.sheetListMaxHeight), verticalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.xs)) {
        item(key = "all") {
            SOptionRow(stringResource(R.string.filter_all_categories), selectedCategoryId == null, { onCategorySelected(null) })
        }
        items(categories, key = { it.externalId }) { category ->
            SOptionRow(
                label = category.name,
                selected = category.externalId == selectedCategoryId,
                onClick = { onCategorySelected(category) },
                trailingText = category.count.toString(),
                locked = category.locked,
            )
        }
    }
    SButton(
        text = stringResource(R.string.action_done),
        onClick = onDismiss,
        modifier = Modifier.fillMaxWidth().padding(top = SintoniaTheme.spacing.sm),
        size = ButtonSize.Footer,
    )
}
