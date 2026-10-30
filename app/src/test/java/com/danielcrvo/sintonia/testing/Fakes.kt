package com.danielcrvo.sintonia.testing

import com.danielcrvo.sintonia.core.data.ListCredentials
import com.danielcrvo.sintonia.core.data.ListsRepository
import com.danielcrvo.sintonia.core.data.NewList
import com.danielcrvo.sintonia.core.data.RemoveResult
import com.danielcrvo.sintonia.core.data.importing.CategoryDraft
import com.danielcrvo.sintonia.core.data.importing.ChannelDraft
import com.danielcrvo.sintonia.core.data.importing.ImportFiles
import com.danielcrvo.sintonia.core.data.importing.ImportStore
import com.danielcrvo.sintonia.core.data.importing.MovieDraft
import com.danielcrvo.sintonia.core.data.importing.SeriesDraft
import com.danielcrvo.sintonia.core.data.parser.Programme
import com.danielcrvo.sintonia.core.model.ContentKind
import com.danielcrvo.sintonia.core.model.IptvList
import com.danielcrvo.sintonia.core.model.ListType
import com.danielcrvo.sintonia.core.network.ApiException
import com.danielcrvo.sintonia.core.network.Downloader
import com.danielcrvo.sintonia.core.network.XtreamAction
import com.danielcrvo.sintonia.core.network.XtreamAuth
import com.danielcrvo.sintonia.core.network.XtreamCredentials
import com.danielcrvo.sintonia.core.network.XtreamRemote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.DecodeSequenceMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeToSequence
import java.io.File
import java.io.InputStream

class FakeLists(vararg initial: IptvList) : ListsRepository {
    private val state = MutableStateFlow(initial.toList())
    val secrets = HashMap<Long, ListCredentials>()

    override val lists: Flow<List<IptvList>> = state
    override val activeList: Flow<IptvList?> = state.map { it.firstOrNull() }

    override suspend fun get(id: Long) = state.value.firstOrNull { it.id == id }
    override suspend fun credentials(id: Long) = secrets[id] ?: ListCredentials(null, null)
    override suspend fun create(list: NewList): Long = error("not used")
    override suspend fun setActive(id: Long) = Unit
    override suspend fun rename(id: Long, name: String) = Unit
    val removed = ArrayList<Long>()

    override suspend fun remove(id: Long): RemoveResult {
        removed += id
        return RemoveResult.Unchanged
    }
}

fun sampleList(
    id: Long = 1,
    type: ListType = ListType.Xtream,
    server: String? = "http://servidor.exemplo:8080",
    filePath: String? = null,
) = IptvList(
    id = id,
    name = "Minha lista",
    type = type,
    server = server,
    username = "ana",
    liveExtension = "m3u8",
    filePath = filePath,
    channelCount = 0,
    movieCount = 0,
    seriesCount = 0,
    createdAt = 0,
    lastImportedAt = null,
)

class FakeStore : ImportStore {
    var current = 0
    val channels = HashMap<Int, MutableList<ChannelDraft>>()
    val movies = HashMap<Int, MutableList<MovieDraft>>()
    val series = HashMap<Int, MutableList<SeriesDraft>>()
    val categories = HashMap<Pair<Int, ContentKind>, MutableList<CategoryDraft>>()
    var committed: Int? = null
    var discarded = 0
    var liveExtension: String? = null
    val guide = ArrayList<Programme>()
    var epgIds: Set<String> = emptySet()

    /** When set, writes wait on it, so the import can be cancelled while stalled mid-way. */
    var gate: kotlinx.coroutines.CompletableDeferred<Unit>? = null

    override suspend fun begin(listId: Long) = current + 1
    override suspend fun insertCategories(listId: Long, generation: Int, kind: ContentKind, items: List<CategoryDraft>) {
        categories.getOrPut(generation to kind) { ArrayList() } += items
    }

    override suspend fun insertChannels(listId: Long, generation: Int, items: List<ChannelDraft>) {
        gate?.await()
        channels.getOrPut(generation) { ArrayList() } += items
    }

    override suspend fun insertMovies(listId: Long, generation: Int, items: List<MovieDraft>) {
        movies.getOrPut(generation) { ArrayList() } += items
    }

    override suspend fun insertSeries(listId: Long, generation: Int, items: List<SeriesDraft>) {
        series.getOrPut(generation) { ArrayList() } += items
    }

    override suspend fun liveEpgIds(listId: Long, generation: Int) =
        epgIds.ifEmpty { channels[generation].orEmpty().mapNotNull { it.epgId }.toSet() }

    override suspend fun commit(listId: Long, generation: Int) {
        current = generation
        committed = generation
    }

    override suspend fun currentGeneration(listId: Long): Int = committed ?: 0

    override suspend fun discard(listId: Long) {
        discarded++
    }

    override suspend fun setLiveExtension(listId: Long, extension: String) {
        liveExtension = extension
    }

    override suspend fun replaceGuide(listId: Long, items: List<Programme>) {
        guide.clear()
        guide += items
    }
}

class FakeDownloader(
    private val responses: MutableMap<String, String> = HashMap(),
    private val failing: MutableSet<String> = HashSet(),
) : Downloader {
    val requested = ArrayList<String>()

    fun serve(url: String, content: String) {
        responses[url] = content
    }

    fun fail(url: String) {
        failing += url
    }

    override suspend fun download(url: String, destination: File, readTimeoutSeconds: Long): File {
        requested += url
        if (url in failing) throw ApiException(ApiException.Kind.Network)
        val content = responses[url] ?: throw ApiException(ApiException.Kind.Http, httpCode = 404)
        destination.parentFile?.mkdirs()
        destination.writeText(content)
        return destination
    }
}

@OptIn(ExperimentalSerializationApi::class)
class FakeXtream(
    private val auth: XtreamAuth? = XtreamAuth("m3u8", "http://servidor.exemplo:8080/xmltv.php?username=ana&password=x", null),
    private val authError: ApiException? = null,
    private val responses: Map<XtreamAction, String> = emptyMap(),
) : XtreamRemote {
    override suspend fun authenticate(creds: XtreamCredentials): XtreamAuth {
        authError?.let { throw it }
        return auth!!
    }

    override suspend fun download(creds: XtreamCredentials, action: XtreamAction, destination: File): File {
        destination.parentFile?.mkdirs()
        destination.writeText(responses[action] ?: "[]")
        return destination
    }

    override fun readItems(stream: InputStream): Sequence<JsonObject> =
        Json { ignoreUnknownKeys = true }.decodeToSequence(stream, JsonObject.serializer(), DecodeSequenceMode.ARRAY_WRAPPED)
}

class TempImportFiles(private val directory: File) : ImportFiles {
    override fun tempFile(name: String) = File(directory, name).also { directory.mkdirs() }
    override fun cleanup() {
        directory.deleteRecursively()
    }
}
