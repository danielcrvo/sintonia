package com.danielcrvo.sintonia.core.data.importing

import com.danielcrvo.sintonia.core.common.CountingInputStream
import com.danielcrvo.sintonia.core.data.ListsRepository
import com.danielcrvo.sintonia.core.data.parser.M3uClassifier
import com.danielcrvo.sintonia.core.data.parser.M3uEntry
import com.danielcrvo.sintonia.core.data.parser.M3uParser
import com.danielcrvo.sintonia.core.data.parser.Programme
import com.danielcrvo.sintonia.core.data.parser.XmltvParser
import com.danielcrvo.sintonia.core.model.ContentKind
import com.danielcrvo.sintonia.core.model.IptvList
import com.danielcrvo.sintonia.core.model.ListType
import com.danielcrvo.sintonia.core.network.ApiException
import com.danielcrvo.sintonia.core.network.Downloader
import com.danielcrvo.sintonia.core.network.Xtream
import com.danielcrvo.sintonia.core.network.XtreamAction
import com.danielcrvo.sintonia.core.network.XtreamCredentials
import com.danielcrvo.sintonia.core.network.XtreamMapper
import com.danielcrvo.sintonia.core.network.XtreamRemote
import com.danielcrvo.sintonia.core.network.hostOf
import com.danielcrvo.sintonia.core.network.toApiException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListImporter
    @Inject
    constructor(
        private val lists: ListsRepository,
        private val store: ImportStore,
        private val xtream: XtreamRemote,
        private val downloader: Downloader,
        private val files: ImportFiles,
    ) {
        var now: () -> Long = System::currentTimeMillis

        fun import(listId: Long): Flow<ImportProgress> = flow {
            val run = Run(this, listId)
            try {
                run.execute()
            } finally {
                files.cleanup()
            }
        }.flowOn(Dispatchers.IO)

        private inner class Run(private val out: FlowCollector<ImportProgress>, private val listId: Long) {
            private var progress = ImportProgress()
            private var current = ImportStep.Connect
            private var host: String? = null

            suspend fun execute() {
                val list = lists.get(listId) ?: throw ApiException(ApiException.Kind.Invalid)
                val credentials = lists.credentials(listId)
                host = hostOf(list.server ?: credentials.url)
                val generation = store.begin(listId)
                var committed = false
                try {
                    val epgUrl = if (list.type == ListType.Xtream) {
                        importXtream(list, credentials.password.orEmpty(), generation)
                    } else {
                        importM3u(list, credentials.url, generation)
                    }
                    currentCoroutineContext().ensureActive()
                    store.commit(listId, generation)
                    committed = true
                    // The guide URL may carry a password: memory only, never the database.
                    loadGuide(epgUrl, generation)
                    report(ImportStep.Guide, StepStatus.Done)
                } catch (cancelled: CancellationException) {
                    withContext(NonCancellable) { runCatching { store.discard(listId) } }
                    throw cancelled
                } catch (error: Throwable) {
                    if (!committed) {
                        runCatching { report(current, StepStatus.Error) }
                        withContext(NonCancellable) { runCatching { store.discard(listId) } }
                    }
                    throw error.toApiException(host)
                }
            }

            private suspend fun report(step: ImportStep, status: StepStatus, count: Int? = null, fraction: Float? = null) {
                current = step
                progress = progress.update(step, status, count, fraction)
                out.emit(progress)
            }

            private suspend fun importXtream(list: IptvList, password: String, generation: Int): String {
                val creds = XtreamCredentials(list.server.orEmpty(), list.username.orEmpty(), password)
                report(ImportStep.Connect, StepStatus.Running)
                val auth = xtream.authenticate(creds)
                store.setLiveExtension(listId, auth.liveExtension)
                report(ImportStep.Connect, StepStatus.Done)

                var total = 0
                total += importKind(
                    creds,
                    generation,
                    ImportStep.Live,
                    ContentKind.Live,
                    XtreamAction.LiveCategories,
                    XtreamAction.LiveStreams,
                    XtreamMapper::channel,
                    store::insertChannels,
                )
                total += importKind(
                    creds,
                    generation,
                    ImportStep.Movies,
                    ContentKind.Movie,
                    XtreamAction.VodCategories,
                    XtreamAction.VodStreams,
                    XtreamMapper::movie,
                    store::insertMovies,
                )
                total += importKind(
                    creds,
                    generation,
                    ImportStep.Series,
                    ContentKind.Series,
                    XtreamAction.SeriesCategories,
                    XtreamAction.Series,
                    XtreamMapper::series,
                    store::insertSeries,
                )
                if (total == 0) throw ApiException(ApiException.Kind.Empty, host)
                return auth.epgUrl
            }

            private suspend fun <T : Any> importKind(
                creds: XtreamCredentials,
                generation: Int,
                step: ImportStep,
                kind: ContentKind,
                categoriesAction: XtreamAction,
                streamsAction: XtreamAction,
                map: (JsonObject) -> T?,
                insert: suspend (Long, Int, List<T>) -> Unit,
            ): Int {
                currentCoroutineContext().ensureActive()
                report(step, StepStatus.Running, 0, 0f)

                val categories = runCatching {
                    val file = xtream.download(creds, categoriesAction, files.tempFile("categories.json"))
                    file.inputStream().buffered().use { stream ->
                        xtream.readItems(stream).mapNotNull(XtreamMapper::category).toList()
                    }
                }.getOrElse { error ->
                    if (error.mustPropagate()) throw error
                    emptyList()
                }
                store.insertCategories(listId, generation, kind, categories)

                val file = xtream.download(creds, streamsAction, files.tempFile("${step.name}.json"))
                val length = file.length().coerceAtLeast(1)
                var count = 0
                CountingInputStream(file.inputStream().buffered()).use { stream ->
                    for (batch in xtream.readItems(stream).mapNotNull(map).chunked(BatchSize)) {
                        currentCoroutineContext().ensureActive()
                        insert(listId, generation, batch)
                        count += batch.size
                        report(step, StepStatus.Running, count, (stream.bytesRead.toFloat() / length).coerceAtMost(1f))
                    }
                }
                file.delete()
                report(step, StepStatus.Done, count)
                return count
            }

            private suspend fun importM3u(list: IptvList, url: String?, generation: Int): String? {
                report(ImportStep.Connect, StepStatus.Running)
                val file = if (list.type == ListType.M3uUrl) {
                    downloader.download(url.orEmpty(), files.tempFile("list.m3u"))
                } else {
                    File(list.filePath.orEmpty()).takeIf { it.isFile && it.canRead() }
                        ?: throw ApiException(ApiException.Kind.File)
                }
                report(ImportStep.Connect, StepStatus.Done)

                val totals = HashMap<ContentKind, Int>()
                val counter = M3uParser()
                file.useLines { lines ->
                    for ((index, entry) in counter.parse(lines).withIndex()) {
                        if (index % BatchSize == 0) currentCoroutineContext().ensureActive()
                        totals.merge(M3uClassifier.classify(entry), 1, Int::plus)
                    }
                }
                if (totals.values.sum() == 0) throw ApiException(ApiException.Kind.Empty, host)

                val steps = mapOf(ContentKind.Live to ImportStep.Live, ContentKind.Movie to ImportStep.Movies, ContentKind.Series to ImportStep.Series)
                val done = HashMap<ContentKind, Int>()
                val groups = ContentKind.entries.associateWith { LinkedHashSet<String>() }
                for ((kind, step) in steps) {
                    val total = totals[kind] ?: 0
                    report(step, if (total == 0) StepStatus.Done else StepStatus.Running, 0, 0f)
                }

                val parser = M3uParser()
                file.useLines { lines ->
                    for (batch in parser.parse(lines).chunked(BatchSize)) {
                        currentCoroutineContext().ensureActive()
                        val byKind = batch.groupBy(M3uClassifier::classify)
                        store.insertChannels(listId, generation, (byKind[ContentKind.Live] ?: emptyList()).map(::channelOf))
                        store.insertMovies(listId, generation, (byKind[ContentKind.Movie] ?: emptyList()).map(::movieOf))
                        store.insertSeries(listId, generation, (byKind[ContentKind.Series] ?: emptyList()).map(::seriesOf))
                        for ((kind, entries) in byKind) {
                            entries.forEach { entry -> entry.group?.let { groups.getValue(kind) += it } }
                            val count = (done[kind] ?: 0) + entries.size
                            done[kind] = count
                            val total = totals[kind] ?: 1
                            report(steps.getValue(kind), StepStatus.Running, count, count.toFloat() / total)
                        }
                    }
                }
                for ((kind, step) in steps) {
                    store.insertCategories(listId, generation, kind, groups.getValue(kind).map { CategoryDraft(it, it) })
                    report(step, StepStatus.Done, done[kind] ?: 0)
                }
                return parser.epgUrl
            }

            private fun channelOf(e: M3uEntry) = ChannelDraft(e.url, null, e.name, e.logo, e.group, e.tvgId, e.url)

            private fun movieOf(e: M3uEntry) =
                MovieDraft(e.url, e.name, e.logo, e.group, null, null, extensionOf(e.url), e.url)

            private fun seriesOf(e: M3uEntry) = SeriesDraft(e.url, e.name, e.logo, e.group, null, null, e.url)

            private fun extensionOf(url: String): String? =
                Regex("\\.([a-z0-9]{2,5})$", RegexOption.IGNORE_CASE)
                    .find(url.substringBefore('?').substringBefore('#'))
                    ?.groupValues
                    ?.get(1)
                    ?.lowercase()

            /** The guide is optional: any failure leaves no schedule but the import still completes. */
            private suspend fun loadGuide(epgUrl: String?, generation: Int) {
                if (epgUrl == null) return
                report(ImportStep.Guide, StepStatus.Running, 0, 0f)
                fetchGuide(listId, epgUrl, generation) { count, fraction -> report(ImportStep.Guide, StepStatus.Running, count, fraction) }
            }
        }

        suspend fun refreshGuide(listId: Long): Boolean = withContext(Dispatchers.IO) {
            try {
                val list = lists.get(listId)?.takeIf { it.type == ListType.Xtream } ?: return@withContext false
                val creds = XtreamCredentials(list.server.orEmpty(), list.username.orEmpty(), lists.credentials(listId).password.orEmpty())
                fetchGuide(
                    listId,
                    com.danielcrvo.sintonia.core.network.Xtream
                        .epgUrl(creds),
                    store.currentGeneration(listId),
                ) { _, _ -> }
            } finally {
                files.cleanup()
            }
        }

        private suspend fun fetchGuide(listId: Long, epgUrl: String, generation: Int, onProgress: suspend (Int, Float) -> Unit): Boolean {
            try {
                val wanted = store.liveEpgIds(listId, generation)
                if (wanted.isEmpty()) return false
                val file = downloader.download(epgUrl, files.tempFile("guide.xml"))
                val length = file.length().coerceAtLeast(1)
                val from = now() - GuidePastMillis
                val to = now() + GuideFutureMillis
                val programmes = ArrayList<Programme>()
                CountingInputStream(file.inputStream().buffered()).use { stream ->
                    val parsed = XmltvParser.parse(stream.reader().buffered()) { channel, start, stop ->
                        channel in wanted && stop > from && start < to
                    }
                    for ((index, programme) in parsed.withIndex()) {
                        programmes += programme
                        if (index % BatchSize == 0) {
                            currentCoroutineContext().ensureActive()
                            onProgress(programmes.size, (stream.bytesRead.toFloat() / length).coerceAtMost(1f))
                        }
                    }
                }
                store.replaceGuide(listId, programmes)
                return true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                return false // optional guide: keep what was already there
            }
        }

        /** Cancellation and network/auth failures are never swallowed. */
        private fun Throwable.mustPropagate() =
            this is CancellationException || (this is ApiException && kind != ApiException.Kind.Invalid)

        private companion object {
            const val BatchSize = 1000
            const val GuidePastMillis = 3 * 3_600_000L
            const val GuideFutureMillis = 24 * 3_600_000L
        }
    }
