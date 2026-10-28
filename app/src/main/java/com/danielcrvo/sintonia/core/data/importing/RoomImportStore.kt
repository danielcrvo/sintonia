package com.danielcrvo.sintonia.core.data.importing

import com.danielcrvo.sintonia.core.data.parser.Programme
import com.danielcrvo.sintonia.core.data.parser.nameKey
import com.danielcrvo.sintonia.core.database.CategoryEntity
import com.danielcrvo.sintonia.core.database.ChannelEntity
import com.danielcrvo.sintonia.core.database.MovieEntity
import com.danielcrvo.sintonia.core.database.ProgramEntity
import com.danielcrvo.sintonia.core.database.SeriesEntity
import com.danielcrvo.sintonia.core.database.SintoniaDatabase
import com.danielcrvo.sintonia.core.model.ContentKind
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomImportStore
    @Inject
    constructor(private val db: SintoniaDatabase) : ImportStore {
        private val importer get() = db.importer()

        override suspend fun begin(listId: Long): Int {
            importer.discard(listId)
            return (importer.currentGeneration(listId) ?: 0) + 1
        }

        override suspend fun insertCategories(listId: Long, generation: Int, kind: ContentKind, items: List<CategoryDraft>) {
            importer.insertCategories(
                items.map { CategoryEntity(listId = listId, generation = generation, kind = kind.name, externalId = it.externalId, name = it.name) },
            )
        }

        override suspend fun insertChannels(listId: Long, generation: Int, items: List<ChannelDraft>) {
            if (items.isEmpty()) return
            importer.insertChannels(
                items.map {
                    ChannelEntity(
                        listId = listId,
                        generation = generation,
                        externalId = it.externalId,
                        number = it.number,
                        sortNumber = it.number ?: Int.MAX_VALUE,
                        name = it.name,
                        nameKey = nameKey(it.name),
                        logo = it.logo,
                        categoryId = it.categoryId,
                        epgId = it.epgId,
                        url = it.url,
                    )
                },
            )
        }

        override suspend fun insertMovies(listId: Long, generation: Int, items: List<MovieDraft>) {
            if (items.isEmpty()) return
            importer.insertMovies(
                items.map {
                    MovieEntity(
                        listId = listId,
                        generation = generation,
                        externalId = it.externalId,
                        name = it.name,
                        nameKey = nameKey(it.name),
                        poster = it.poster,
                        categoryId = it.categoryId,
                        year = it.year,
                        sortYear = it.year?.take(4)?.toIntOrNull() ?: 0,
                        rating = it.rating,
                        containerExtension = it.containerExtension,
                        url = it.url,
                    )
                },
            )
        }

        override suspend fun insertSeries(listId: Long, generation: Int, items: List<SeriesDraft>) {
            if (items.isEmpty()) return
            importer.insertSeries(
                items.map {
                    SeriesEntity(
                        listId = listId,
                        generation = generation,
                        externalId = it.externalId,
                        name = it.name,
                        nameKey = nameKey(it.name),
                        poster = it.poster,
                        categoryId = it.categoryId,
                        year = it.year,
                        sortYear = it.year?.take(4)?.toIntOrNull() ?: 0,
                        rating = it.rating,
                        url = it.url,
                    )
                },
            )
        }

        override suspend fun liveEpgIds(listId: Long, generation: Int): Set<String> =
            importer.liveEpgIds(listId, generation).toSet()

        override suspend fun currentGeneration(listId: Long): Int = importer.currentGeneration(listId) ?: 0

        override suspend fun commit(listId: Long, generation: Int) =
            importer.commit(listId, generation, System.currentTimeMillis())

        override suspend fun discard(listId: Long) = importer.discard(listId)

        override suspend fun setLiveExtension(listId: Long, extension: String) = db.lists().setLiveExtension(listId, extension)

        override suspend fun replaceGuide(listId: Long, items: List<Programme>) {
            db.epg().replace(
                listId,
                items.map {
                    ProgramEntity(listId = listId, epgId = it.channel, start = it.start, stop = it.stop, title = it.title, description = it.description)
                },
            )
        }
    }
