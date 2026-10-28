package com.danielcrvo.sintonia.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.danielcrvo.sintonia.core.datastore.AppPreferences
import com.danielcrvo.sintonia.core.model.Category
import com.danielcrvo.sintonia.core.model.ContentKind
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CategoryPrefsTest {
    private val preferences = AppPreferences(ApplicationProvider.getApplicationContext<Context>())
    private val prefs = CategoryPrefsRepository(preferences)

    private val stored = listOf(
        Category("1", "Esportes", ContentKind.Live, 10),
        Category("2", "Radios", ContentKind.Live, 4),
        Category("3", "Adulto", ContentKind.Live, 2),
    )

    @Test
    fun `rename changes the name but keeps the original`() = runTest {
        prefs.rename(LIST_RENAME, ContentKind.Live, "1", "  Zeta Futebol  ")
        val result = prefs.decorate(LIST_RENAME, ContentKind.Live, flowOf(stored), includeHidden = false).first()
        val renamed = result.first { it.externalId == "1" }
        assertEquals("Zeta Futebol", renamed.name)
        assertEquals("Esportes", renamed.originalName)
        assertEquals("Radios", result.first { it.externalId == "2" }.name)
        assertEquals(listOf("3", "2", "1"), result.map { it.externalId })
    }

    @Test
    fun `categories sort alphabetically ignoring case and accents`() = runTest {
        val unsorted = listOf(
            Category("1", "zebra", ContentKind.Live, 1),
            Category("2", "Ágata", ContentKind.Live, 1),
            Category("3", "beta", ContentKind.Live, 1),
            Category("4", "Álamo", ContentKind.Live, 1),
        )
        val result = prefs.decorate(LIST_ORDER, ContentKind.Live, flowOf(unsorted), includeHidden = false).first()
        assertEquals(listOf("Ágata", "Álamo", "beta", "zebra"), result.map { it.name })
    }

    @Test
    fun `empty name falls back to the provider name`() = runTest {
        prefs.rename(LIST_RESET, ContentKind.Live, "1", "Futebol")
        prefs.rename(LIST_RESET, ContentKind.Live, "1", "   ")
        val result = prefs.decorate(LIST_RESET, ContentKind.Live, flowOf(stored), includeHidden = false).first()
        assertEquals("Esportes", result.first { it.externalId == "1" }.name)
    }

    @Test
    fun `hidden ones leave the normal list and stay in the full one`() = runTest {
        prefs.setHidden(LIST_HIDE, ContentKind.Live, "3", hidden = true)
        val visible = prefs.decorate(LIST_HIDE, ContentKind.Live, flowOf(stored), includeHidden = false).first()
        val all = prefs.decorate(LIST_HIDE, ContentKind.Live, flowOf(stored), includeHidden = true).first()
        assertEquals(listOf("1", "2"), visible.map { it.externalId })
        assertEquals(3, all.size)
        assertTrue(all.first { it.externalId == "3" }.hidden)
        prefs.setHidden(LIST_HIDE, ContentKind.Live, "3", hidden = false)
        assertEquals(3, prefs.decorate(LIST_HIDE, ContentKind.Live, flowOf(stored), includeHidden = false).first().size)
    }

    @Test
    fun `the choice applies per type and per list`() = runTest {
        prefs.setHidden(LIST_SCOPE, ContentKind.Live, "1", hidden = true)
        val movies = prefs.decorate(LIST_SCOPE, ContentKind.Movie, flowOf(stored), includeHidden = false).first()
        val otherList = prefs.decorate(LIST_SCOPE + 1, ContentKind.Live, flowOf(stored), includeHidden = false).first()
        assertEquals(3, movies.size)
        assertEquals(3, otherList.size)
    }

    @Test
    fun `hidden is blocked on Home and search even with parental control off`() = runTest {
        val parental = DataStoreParentalRepository(preferences)
        prefs.setHidden(LIST_PARENTAL, ContentKind.Live, "3", hidden = true)
        assertFalse(parental.enabled.first())
        assertEquals(listOf("3"), parental.blockedCategories(LIST_PARENTAL, ContentKind.Live).first())
        assertEquals(listOf("3"), parental.lockedCategories(LIST_PARENTAL, ContentKind.Live).first())
    }

    @Test
    fun `hidden is not unlocked by the PIN`() = runTest {
        val parental = DataStoreParentalRepository(preferences)
        prefs.setHidden(LIST_UNLOCK, ContentKind.Live, "3", hidden = true)
        parental.unlock(LIST_UNLOCK, ContentKind.Live, "3")
        assertEquals(listOf("3"), parental.lockedCategories(LIST_UNLOCK, ContentKind.Live).first())
    }

    @Test
    fun `removing the list clears names and hidden ones`() = runTest {
        prefs.rename(LIST_CLEAR, ContentKind.Live, "1", "Futebol")
        prefs.setHidden(LIST_CLEAR, ContentKind.Movie, "9", hidden = true)
        preferences.clearCategoryPreferences(LIST_CLEAR)
        assertTrue(preferences.categoryNames(LIST_CLEAR, ContentKind.Live).first().isEmpty())
        assertTrue(preferences.hiddenCategories(LIST_CLEAR, ContentKind.Movie).first().isEmpty())
    }

    private companion object {
        // One DataStore per process: each test uses its own list to avoid inheriting state.
        const val LIST_RENAME = 9_101L
        const val LIST_RESET = 9_102L
        const val LIST_ORDER = 9_105L
        const val LIST_HIDE = 9_103L
        const val LIST_SCOPE = 9_104L
        const val LIST_PARENTAL = 9_106L
        const val LIST_UNLOCK = 9_107L
        const val LIST_CLEAR = 9_108L
    }
}
