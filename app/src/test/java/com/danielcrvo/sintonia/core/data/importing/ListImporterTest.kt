package com.danielcrvo.sintonia.core.data.importing

import com.danielcrvo.sintonia.core.data.parser.Programme
import com.danielcrvo.sintonia.core.model.ContentKind
import com.danielcrvo.sintonia.core.model.ListType
import com.danielcrvo.sintonia.core.network.ApiException
import com.danielcrvo.sintonia.core.network.XtreamAction
import com.danielcrvo.sintonia.core.network.XtreamAuth
import com.danielcrvo.sintonia.testing.FakeDownloader
import com.danielcrvo.sintonia.testing.FakeLists
import com.danielcrvo.sintonia.testing.FakeStore
import com.danielcrvo.sintonia.testing.FakeXtream
import com.danielcrvo.sintonia.testing.TempImportFiles
import com.danielcrvo.sintonia.testing.sampleList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ListImporterTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val store = FakeStore()
    private val downloader = FakeDownloader()

    private fun importer(
        lists: FakeLists,
        xtream: FakeXtream = FakeXtream(),
    ) = ListImporter(lists, store, xtream, downloader, TempImportFiles(folder.newFolder("tmp"))).apply {
        now = { Instant.parse("2026-10-21T12:00:00Z").toEpochMilli() }
    }

    private val m3u = listOf(
        "#EXTM3U url-tvg=\"http://guia.exemplo/xmltv.xml\"",
        "#EXTINF:-1 tvg-id=\"faro\" group-title=\"Notícias\",Faro Notícias",
        "http://s/live/1.m3u8",
        "#EXTINF:-1 group-title=\"Esportes\",Arena Sport",
        "http://s/live/2.m3u8",
        "#EXTINF:-1 group-title=\"Filmes de Ação\",Eco Distante",
        "http://s/movie/u/p/3.mp4",
        "#EXTINF:-1 group-title=\"Séries\",Rua das Estrelas S01E01",
        "http://s/series/u/p/4.mkv",
    ).joinToString("\n")

    private fun fileList(): Pair<FakeLists, File> {
        val file = folder.newFile("lista.m3u").apply { writeText(m3u) }
        return FakeLists(sampleList(type = ListType.M3uFile, server = null, filePath = file.path)) to file
    }

    @Test
    fun `imports an M3U file splitting live, movies and series`() = runTest {
        val (lists, _) = fileList()
        val progress = importer(lists).import(1).toList()

        assertEquals(1, store.committed)
        assertEquals(listOf("Faro Notícias", "Arena Sport"), store.channels[1]!!.map { it.name })
        assertEquals(listOf("Eco Distante"), store.movies[1]!!.map { it.name })
        assertEquals("mp4", store.movies[1]!![0].containerExtension)
        assertEquals(listOf("Rua das Estrelas S01E01"), store.series[1]!!.map { it.name })
        assertEquals(setOf("Notícias", "Esportes"), store.categories[1 to ContentKind.Live]!!.map { it.name }.toSet())
        assertEquals(100, progress.last().percent)
        assertTrue(progress.map { it.percent }.zipWithNext().all { (a, b) -> b >= a })
    }

    @Test
    fun `imports M3U from a link`() = runTest {
        downloader.serve("http://lista.exemplo/a.m3u", m3u)
        val lists = FakeLists(sampleList(type = ListType.M3uUrl, server = null))
        lists.secrets[1] = com.danielcrvo.sintonia.core.data
            .ListCredentials(null, "http://lista.exemplo/a.m3u")

        importer(lists).import(1).toList()

        assertEquals(2, store.channels[1]!!.size)
        assertEquals("http://lista.exemplo/a.m3u", downloader.requested.first())
    }

    @Test
    fun `a list with no items fails as empty and is discarded`() = runTest {
        val file = folder.newFile("vazia.m3u").apply { writeText("#EXTM3U\nlixo sem endereço\n") }
        val lists = FakeLists(sampleList(type = ListType.M3uFile, server = null, filePath = file.path))

        val error = assertFailsWith<ApiException> { importer(lists).import(1).toList() }

        assertEquals(ApiException.Kind.Empty, error.kind)
        assertNull(store.committed)
        assertTrue(store.discarded >= 1)
    }

    @Test
    fun `missing file becomes a file error`() = runTest {
        val lists = FakeLists(sampleList(type = ListType.M3uFile, server = null, filePath = File(folder.root, "nao-existe.m3u").path))
        val error = assertFailsWith<ApiException> { importer(lists).import(1).toList() }
        assertEquals(ApiException.Kind.File, error.kind)
    }

    private val liveJson = """[{"stream_id":1,"num":1,"name":"Faro Notícias","category_id":"10","epg_channel_id":"faro"},
        {"stream_id":2,"num":2,"name":"Arena Sport","category_id":"11"}]"""
    private val vodJson = """[{"stream_id":7,"name":"Eco Distante","category_id":"20","container_extension":"mkv","year":"2024"}]"""
    private val seriesJson = """[{"series_id":9,"name":"Rua das Estrelas","category_id":"30","cover":"http://img/9.jpg"}]"""
    private val categoriesJson = """[{"category_id":"10","category_name":"Notícias"}]"""

    private fun xtream(auth: XtreamAuth? = XtreamAuth("ts", "http://s/xmltv.php", null)) = FakeXtream(
        auth = auth,
        responses = mapOf(
            XtreamAction.LiveCategories to categoriesJson,
            XtreamAction.LiveStreams to liveJson,
            XtreamAction.VodStreams to vodJson,
            XtreamAction.Series to seriesJson,
        ),
    )

    @Test
    fun `imports Xtream, keeping the live extension and the guide address`() = runTest {
        val progress = importer(FakeLists(sampleList()), xtream()).import(1).toList()

        assertEquals("ts", store.liveExtension)
        assertEquals(listOf(1, 2), store.channels[1]!!.map { it.number })
        assertEquals("mkv", store.movies[1]!![0].containerExtension)
        assertEquals("2024", store.movies[1]!![0].year)
        assertEquals("http://img/9.jpg", store.series[1]!![0].poster)
        assertEquals(1, store.committed)
        assertEquals(100, progress.last().percent)
    }

    @Test
    fun `malformed categories do not break the import`() = runTest {
        val broken = FakeXtream(
            responses = mapOf(
                XtreamAction.LiveCategories to """{"erro":"x"}""",
                XtreamAction.LiveStreams to liveJson,
            ),
        )
        importer(FakeLists(sampleList()), broken).import(1).toList()
        assertEquals(2, store.channels[1]!!.size)
        assertTrue(store.categories[1 to ContentKind.Live].orEmpty().isEmpty())
    }

    @Test
    fun `rejected password becomes an auth error and nothing is written`() = runTest {
        val denied = FakeXtream(authError = ApiException(ApiException.Kind.Auth, "servidor.exemplo"))
        val error = assertFailsWith<ApiException> { importer(FakeLists(sampleList()), denied).import(1).toList() }
        assertEquals(ApiException.Kind.Auth, error.kind)
        assertNull(store.committed)
    }

    @Test
    fun `server with no items fails as empty`() = runTest {
        val error = assertFailsWith<ApiException> { importer(FakeLists(sampleList()), FakeXtream()).import(1).toList() }
        assertEquals(ApiException.Kind.Empty, error.kind)
    }

    @Test
    fun `cancelling midway discards the new generation`() = runTest {
        val (lists, _) = fileList()
        store.current = 4
        store.gate = kotlinx.coroutines.CompletableDeferred()
        importer(lists).import(1).first { it.percent >= 5 }

        assertNull(store.committed)
        assertEquals(4, store.current)
        awaitDiscard()
        assertTrue(store.discarded >= 1)
    }

    /** Cleanup runs on another thread after cancellation: wait (in real time) for it. */
    private suspend fun awaitDiscard() = withContext(Dispatchers.Default) {
        repeat(300) {
            if (store.discarded >= 1) return@withContext
            delay(10)
        }
    }

    @Test
    fun `guide drops unknown channels and programmes outside the window`() = runTest {
        downloader.serve(
            "http://s/xmltv.php",
            """<tv>
            <programme start="20261021110000 +0000" stop="20261021130000 +0000" channel="faro"><title>Agora</title></programme>
            <programme start="20261021130000 +0000" stop="20261021140000 +0000" channel="faro"><title>Depois</title></programme>
            <programme start="20261001110000 +0000" stop="20261001120000 +0000" channel="faro"><title>Mês passado</title></programme>
            <programme start="20261021110000 +0000" stop="20261021130000 +0000" channel="outro"><title>Outro canal</title></programme>
            </tv>""",
        )
        importer(FakeLists(sampleList()), xtream()).import(1).toList()
        assertEquals(listOf("Agora", "Depois"), store.guide.map { it.title })
    }

    @Test
    fun `guide trims long descriptions and stays in the 3h back to 24h ahead window`() = runTest {
        val long = String(CharArray(500) { 'x' })
        downloader.serve(
            "http://s/xmltv.php",
            """<tv>
            <programme start="20261021110000 +0000" stop="20261021130000 +0000" channel="faro"><title>Agora</title><desc>$long</desc></programme>
            <programme start="20261023110000 +0000" stop="20261023130000 +0000" channel="faro"><title>Depois de amanhã</title></programme>
            </tv>""",
        )
        importer(FakeLists(sampleList()), xtream()).import(1).toList()
        assertEquals(listOf("Agora"), store.guide.map { it.title })
        assertEquals(
            200,
            store.guide
                .single()
                .description
                ?.length,
        )
    }

    @Test
    fun `guide download failure keeps the previous guide`() = runTest {
        store.guide += Programme("faro", 1, 2, "Antigo", null)
        downloader.fail("http://s/xmltv.php")
        importer(FakeLists(sampleList()), xtream()).import(1).toList()
        assertEquals(listOf("Antigo"), store.guide.map { it.title })
    }

    @Test
    fun `guide download failure does not undo the import`() = runTest {
        downloader.fail("http://s/xmltv.php")
        val progress = importer(FakeLists(sampleList()), xtream()).import(1).toList()
        assertEquals(1, store.committed)
        assertEquals(100, progress.last().percent)
        assertNotNull(store.channels[1])
    }
}
