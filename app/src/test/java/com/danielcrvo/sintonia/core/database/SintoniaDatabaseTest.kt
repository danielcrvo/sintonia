package com.danielcrvo.sintonia.core.database

import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.testing.asSnapshot
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import com.danielcrvo.sintonia.core.data.importing.CategoryDraft
import com.danielcrvo.sintonia.core.data.importing.ChannelDraft
import com.danielcrvo.sintonia.core.data.importing.MovieDraft
import com.danielcrvo.sintonia.core.data.importing.RoomImportStore
import com.danielcrvo.sintonia.core.data.parser.nameKey
import com.danielcrvo.sintonia.core.model.ContentKind
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SintoniaDatabaseTest {
    private lateinit var db: SintoniaDatabase
    private lateinit var store: RoomImportStore

    @BeforeTest
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room
            .inMemoryDatabaseBuilder(context, SintoniaDatabase::class.java)
            .setDriver(AndroidSQLiteDriver())
            .build()
        store = RoomImportStore(db)
    }

    @AfterTest
    fun tearDown() = db.close()

    private suspend fun newList(): Long =
        db.lists().insert(ListEntity(name = "Teste", type = "M3uUrl", server = null, username = null, filePath = null, createdAt = 1))

    private fun channel(id: String, name: String, number: Int?, category: String?) =
        ChannelDraft(id, number, name, null, category, null, "http://x/$id")

    private suspend fun importChannels(listId: Long, vararg channels: ChannelDraft) {
        val generation = store.begin(listId)
        store.insertChannels(listId, generation, channels.toList())
        store.commit(listId, generation)
    }

    /** The Paging source is recreated on every invalidation, hence the factory. */
    private suspend fun <T : Any> names(name: (T) -> String, factory: () -> PagingSource<Int, T>): List<String> =
        Pager(PagingConfig(pageSize = 20), pagingSourceFactory = factory).flow.asSnapshot { }.map(name)

    private suspend fun channelNames(factory: () -> PagingSource<Int, ChannelEntity>) = names({ it.name }, factory)

    @Test
    fun `sorts by number with unnumbered last, and by name ignoring accents`() = runTest {
        val listId = newList()
        importChannels(
            listId,
            channel("a", "Zeta", 2, null),
            channel("b", "Ágata", null, null),
            channel("c", "Beta", 1, null),
            channel("d", "alfa", null, null),
        )
        assertEquals(listOf("Beta", "Zeta", "Ágata", "alfa"), channelNames { db.channels().pagingByNumber(listId, emptyList()) })
        assertEquals(listOf("Ágata", "alfa", "Beta", "Zeta"), channelNames { db.channels().pagingByName(listId, emptyList()) })
    }

    @Test
    fun `blocked categories vanish from lists, and uncategorized items stay`() = runTest {
        val listId = newList()
        importChannels(
            listId,
            channel("a", "Livre", 1, "abertos"),
            channel("b", "Adulto", 2, "adulto"),
            channel("c", "Solto", 3, null),
        )
        assertEquals(listOf("Livre", "Solto"), channelNames { db.channels().pagingByNumber(listId, listOf("adulto")) })
        assertEquals(listOf("Adulto"), channelNames { db.channels().pagingCategoryByNumber(listId, "adulto", emptyList()) })
        assertEquals(emptyList(), channelNames { db.channels().pagingCategoryByNumber(listId, "adulto", listOf("adulto")) })
        assertEquals(emptyList(), db.channels().idsCategoryByNumber(listId, "adulto", listOf("adulto")))
    }

    @Test
    fun `the count is the database total and respects excluded categories and favorites`() = runTest {
        val listId = newList()
        val many = (1..150).map { channel("c$it", "Canal $it", it, "abertos") }
        importChannels(listId, *many.toTypedArray(), channel("x", "Adulto", 999, "adulto"), channel("y", "Solto", 1000, null))
        db.favorites().add(FavoriteEntity(listId, "c1", 1))
        db.favorites().add(FavoriteEntity(listId, "x", 2))

        assertEquals(152, db.channels().observeCount(listId, emptyList()).first())
        assertEquals(151, db.channels().observeCount(listId, listOf("adulto")).first())
        assertEquals(150, db.channels().observeCategoryCount(listId, "abertos", emptyList()).first())
        assertEquals(0, db.channels().observeCategoryCount(listId, "adulto", listOf("adulto")).first())
        assertEquals(2, db.channels().observeFavoritesCount(listId, emptyList()).first())
        assertEquals(1, db.channels().observeFavoritesCount(listId, listOf("adulto")).first())
    }

    @Test
    fun `reimport swaps the generation at once and deletes the old one`() = runTest {
        val listId = newList()
        importChannels(listId, channel("a", "Antigo 1", 1, null), channel("b", "Antigo 2", 2, null))
        assertEquals(2, db.lists().get(listId)!!.channelCount)

        val next = store.begin(listId)
        store.insertChannels(listId, next, listOf(channel("n", "Novo", 1, null)))
        assertEquals(listOf("Antigo 1", "Antigo 2"), channelNames { db.channels().pagingByNumber(listId, emptyList()) })

        store.commit(listId, next)
        assertEquals(listOf("Novo"), channelNames { db.channels().pagingByNumber(listId, emptyList()) })
        assertEquals(1, db.lists().get(listId)!!.channelCount)
    }

    @Test
    fun `discard removes only what the interrupted import left`() = runTest {
        val listId = newList()
        importChannels(listId, channel("a", "Fica", 1, null))
        val next = store.begin(listId)
        store.insertChannels(listId, next, listOf(channel("n", "Meio caminho", 1, null)))

        store.discard(listId)

        assertEquals(listOf("Fica"), channelNames { db.channels().pagingByNumber(listId, emptyList()) })
        importChannels(listId, channel("m", "Nova", 1, null))
        assertEquals(listOf("Nova"), channelNames { db.channels().pagingByNumber(listId, emptyList()) })
    }

    @Test
    fun `search ignores accents and case and escapes wildcards`() = runTest {
        val listId = newList()
        importChannels(listId, channel("a", "Cine Ação", 1, null), channel("b", "100% Música", 2, null), channel("c", "Outro", 3, null))
        assertEquals(listOf("Cine Ação"), db.channels().search(listId, nameKey("acao"), emptyList(), 10).map { it.name })
        assertEquals(listOf("100% Música"), db.channels().search(listId, "100\\%", emptyList(), 10).map { it.name })
    }

    @Test
    fun `favorites follow channel order and drop out with a blocked channel`() = runTest {
        val listId = newList()
        importChannels(listId, channel("a", "Primeiro", 1, "x"), channel("b", "Segundo", 2, "y"))
        db.favorites().add(FavoriteEntity(listId, "b", 10))
        db.favorites().add(FavoriteEntity(listId, "a", 20))
        assertEquals(listOf("Primeiro", "Segundo"), channelNames { db.channels().pagingFavorites(listId, emptyList()) })
        assertEquals(listOf("Segundo"), channelNames { db.channels().pagingFavorites(listId, listOf("x")) })
        assertEquals(
            setOf("a", "b"),
            db
                .favorites()
                .observeIds(listId)
                .first()
                .toSet(),
        )
    }

    @Test
    fun `movies sort by year descending, then by name`() = runTest {
        val listId = newList()
        val generation = store.begin(listId)
        store.insertMovies(
            listId,
            generation,
            listOf(
                MovieDraft("1", "Beta", null, "c", "2020", null, null, null),
                MovieDraft("2", "Alfa", null, "c", "2024", null, null, null),
                MovieDraft("3", "Gama", null, "c", null, null, null, null),
                MovieDraft("4", "Delta", null, "c", "2024", null, null, null),
            ),
        )
        store.commit(listId, generation)
        val result = names<MovieEntity>({ it.name }) { db.movies().pagingByYear(listId, emptyList()) }
        assertEquals(listOf("Alfa", "Delta", "Beta", "Gama"), result)
    }

    @Test
    fun `deleting the list takes all its content with it`() = runTest {
        val listId = newList()
        val generation = store.begin(listId)
        store.insertCategories(listId, generation, ContentKind.Live, listOf(CategoryDraft("x", "X")))
        store.insertChannels(listId, generation, listOf(channel("a", "Canal", 1, "x")))
        store.commit(listId, generation)
        db.favorites().add(FavoriteEntity(listId, "a", 1))

        db.lists().delete(listId)

        assertTrue(
            db
                .favorites()
                .observeIds(listId)
                .first()
                .isEmpty(),
        )
        assertTrue(
            db
                .categories()
                .observe(listId, "Live")
                .first()
                .isEmpty(),
        )
        assertTrue(channelNames { db.channels().pagingByNumber(listId, emptyList()) }.isEmpty())
    }
}
