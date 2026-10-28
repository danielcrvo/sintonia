package com.danielcrvo.sintonia.core.data.importing

import com.danielcrvo.sintonia.core.data.parser.Programme
import com.danielcrvo.sintonia.core.model.ContentKind

/** Content goes into a new generation; only commit switches the current one. */
interface ImportStore {
    suspend fun begin(listId: Long): Int
    suspend fun insertCategories(listId: Long, generation: Int, kind: ContentKind, items: List<CategoryDraft>)
    suspend fun insertChannels(listId: Long, generation: Int, items: List<ChannelDraft>)
    suspend fun insertMovies(listId: Long, generation: Int, items: List<MovieDraft>)
    suspend fun insertSeries(listId: Long, generation: Int, items: List<SeriesDraft>)
    suspend fun liveEpgIds(listId: Long, generation: Int): Set<String>
    suspend fun currentGeneration(listId: Long): Int
    suspend fun commit(listId: Long, generation: Int)
    suspend fun discard(listId: Long)
    suspend fun setLiveExtension(listId: Long, extension: String)

    suspend fun replaceGuide(listId: Long, items: List<Programme>)
}
