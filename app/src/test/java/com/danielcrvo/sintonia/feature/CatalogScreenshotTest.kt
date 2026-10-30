package com.danielcrvo.sintonia.feature

import android.content.Context
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.test.core.app.ApplicationProvider
import com.danielcrvo.sintonia.R
import com.danielcrvo.sintonia.core.data.CatalogSort
import com.danielcrvo.sintonia.core.designsystem.components.SIcons
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme
import com.danielcrvo.sintonia.core.designsystem.theme.ThemeMode
import com.danielcrvo.sintonia.core.model.Category
import com.danielcrvo.sintonia.core.model.ContentKind
import com.danielcrvo.sintonia.feature.catalog.ui.CatalogLabels
import com.danielcrvo.sintonia.feature.catalog.ui.CatalogScreen
import com.danielcrvo.sintonia.feature.catalog.ui.CatalogUiState
import com.danielcrvo.sintonia.feature.catalog.ui.PosterItem
import com.danielcrvo.sintonia.feature.categories.ui.CategoriesScreen
import com.danielcrvo.sintonia.feature.categories.ui.CategoriesSection
import com.danielcrvo.sintonia.feature.categories.ui.CategoriesUiState
import com.danielcrvo.sintonia.feature.categories.ui.CategoryItem
import com.danielcrvo.sintonia.feature.categories.ui.RenameTarget
import com.danielcrvo.sintonia.testing.saveScreenshot
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "pt-w400dp-h800dp-xxhdpi")
class CatalogScreenshotTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun disableAnimations() {
        val resolver = ApplicationProvider.getApplicationContext<Context>().contentResolver
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
    }

    private fun shot(name: String, content: @Composable () -> Unit) {
        val mode = mutableStateOf(ThemeMode.Dark)
        rule.setContent { SintoniaTheme(mode = mode.value) { content() } }
        rule.saveScreenshot("$name-dark")
        mode.value = ThemeMode.Light
        rule.saveScreenshot("$name-light")
    }

    private val labels = CatalogLabels(R.string.tab_movies, R.plurals.movies_count, R.string.movies_empty_title, SIcons.Film)
    private val categories = persistentListOf(
        Category("1", "Adventure", ContentKind.Movie, 120),
        Category("2", "Drama", ContentKind.Movie, 86),
        Category("3", "Comedy", ContentKind.Movie, 54),
    )
    private val posters = List(12) { PosterItem(it.toLong(), "Beacon High ${it + 1}", "202${it % 5} · 7.${it % 10}", null) }

    @Test
    fun catalogGrid() = shot("catalog-grid") {
        val items = remember { flowOf(PagingData.from(posters)) }.collectAsLazyPagingItems()
        CatalogScreen(
            labels = labels,
            state = CatalogUiState(categories = categories, sort = CatalogSort.Name),
            items = items,
            count = posters.size,
            onAllSelected = {},
            onCategorySelected = {},
            onFilterClick = {},
            onItemClick = {},
        )
    }

    @Test
    fun catalogEmpty() = shot("catalog-empty") {
        val items = remember { flowOf(PagingData.empty<PosterItem>()) }.collectAsLazyPagingItems()
        CatalogScreen(
            labels = labels,
            state = CatalogUiState(categories = categories, selectedCategory = categories[0]),
            items = items,
            count = 0,
            onAllSelected = {},
            onCategorySelected = {},
            onFilterClick = {},
            onItemClick = {},
        )
    }

    private val sections = persistentListOf(
        CategoriesSection(
            ContentKind.Live,
            listOf(
                CategoryItem("1", "News", "News", hidden = false),
                CategoryItem("2", "Sports Plus", "Sports", hidden = false),
                CategoryItem("3", "Kids", "Kids", hidden = true),
            ).toImmutableList(),
        ),
        CategoriesSection(ContentKind.Movie, listOf(CategoryItem("1", "Adventure", "Adventure", hidden = false)).toImmutableList()),
        CategoriesSection(ContentKind.Series, listOf(CategoryItem("1", "Drama", "Drama", hidden = false)).toImmutableList()),
    )

    private fun categoriesScreen(state: CategoriesUiState) = @Composable {
        CategoriesScreen(
            state = state,
            onBack = {},
            onToggleSection = {},
            onToggleHidden = { _, _, _ -> },
            onRenameStart = { _, _ -> },
            onRenameTextChange = {},
            onRenameSave = {},
            onRenameReset = {},
            onRenameDismiss = {},
        )
    }

    @Test
    fun categoriesExpanded() = shot("categories-expanded", categoriesScreen(CategoriesUiState(sections, expanded = ContentKind.Live)))

    @Test
    fun categoriesRename() = shot(
        "categories-rename",
        categoriesScreen(
            CategoriesUiState(
                sections,
                expanded = ContentKind.Live,
                renaming = RenameTarget(ContentKind.Live, sections[0].categories[1], "Sports Plus"),
            ),
        ),
    )
}
