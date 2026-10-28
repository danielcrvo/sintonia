package com.danielcrvo.sintonia.core.data

import com.danielcrvo.sintonia.core.common.SecretStore
import com.danielcrvo.sintonia.core.database.ListEntity
import com.danielcrvo.sintonia.core.database.SintoniaDatabase
import com.danielcrvo.sintonia.core.datastore.AppPreferences
import com.danielcrvo.sintonia.core.model.IptvList
import com.danielcrvo.sintonia.core.model.ListType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed interface NewList {
    val name: String

    data class Xtream(override val name: String, val server: String, val username: String, val password: String) : NewList
    data class M3uUrl(override val name: String, val url: String) : NewList

    data class M3uFile(override val name: String, val filePath: String) : NewList
}

data class ListCredentials(val password: String?, val url: String?)

sealed interface RemoveResult {
    data class NextActive(val listId: Long) : RemoveResult

    data object NoneLeft : RemoveResult

    data object Unchanged : RemoveResult
}

interface ListsRepository {
    val lists: Flow<List<IptvList>>

    val activeList: Flow<IptvList?>

    suspend fun get(id: Long): IptvList?
    suspend fun credentials(id: Long): ListCredentials
    suspend fun create(list: NewList): Long
    suspend fun setActive(id: Long)
    suspend fun rename(id: Long, name: String)
    suspend fun remove(id: Long): RemoveResult
}

@Singleton
class RoomListsRepository
    @Inject
    constructor(
        private val db: SintoniaDatabase,
        private val secrets: SecretStore,
        private val preferences: AppPreferences,
    ) : ListsRepository {
        private val dao get() = db.lists()

        override val lists: Flow<List<IptvList>> = dao.observeAll().map { rows -> rows.map { it.toModel() } }

        override val activeList: Flow<IptvList?> = combine(lists, preferences.activeListId) { all, activeId ->
            all.firstOrNull { it.id == activeId } ?: all.firstOrNull()
        }.distinctUntilChanged()

        override suspend fun get(id: Long): IptvList? = dao.get(id)?.toModel()

        override suspend fun credentials(id: Long): ListCredentials = withContext(Dispatchers.IO) {
            ListCredentials(password = secrets.get(passwordKey(id)), url = secrets.get(urlKey(id)))
        }

        override suspend fun create(list: NewList): Long {
            val now = System.currentTimeMillis()
            val entity = when (list) {
                is NewList.Xtream -> ListEntity(
                    name = list.name,
                    type = ListType.Xtream.name,
                    server = list.server,
                    username = list.username,
                    filePath = null,
                    createdAt = now,
                )
                is NewList.M3uUrl -> ListEntity(
                    name = list.name,
                    type = ListType.M3uUrl.name,
                    server = null,
                    username = null,
                    filePath = null,
                    createdAt = now,
                )
                is NewList.M3uFile -> ListEntity(
                    name = list.name,
                    type = ListType.M3uFile.name,
                    server = null,
                    username = null,
                    filePath = list.filePath,
                    createdAt = now,
                )
            }
            val id = dao.insert(entity)
            withContext(Dispatchers.IO) {
                when (list) {
                    is NewList.Xtream -> secrets.put(passwordKey(id), list.password)
                    is NewList.M3uUrl -> secrets.put(urlKey(id), list.url)
                    is NewList.M3uFile -> Unit
                }
            }
            preferences.setActiveListId(id)
            return id
        }

        override suspend fun setActive(id: Long) {
            preferences.setActiveListId(id)
        }

        override suspend fun rename(id: Long, name: String) {
            dao.rename(id, name)
        }

        override suspend fun remove(id: Long): RemoveResult {
            val before = dao.ids()
            val active = activeList.firstValue()?.id
            val position = before.indexOf(id)
            val removed = dao.get(id)
            dao.delete(id)
            withContext(Dispatchers.IO) {
                secrets.remove(passwordKey(id))
                secrets.remove(urlKey(id))
                removed?.filePath?.let { File(it).delete() }
            }
            preferences.clearBlockedCategories(id)
            preferences.clearCategoryPreferences(id)
            val remaining = before - id
            if (remaining.isEmpty()) {
                preferences.setActiveListId(null)
                return RemoveResult.NoneLeft
            }
            if (active != id) return RemoveResult.Unchanged
            val next = remaining.getOrElse(position) { remaining.last() }
            preferences.setActiveListId(next)
            return RemoveResult.NextActive(next)
        }

        private suspend fun Flow<IptvList?>.firstValue(): IptvList? = first()

        private fun passwordKey(id: Long) = "list.$id.password"
        private fun urlKey(id: Long) = "list.$id.url"
    }

internal fun ListEntity.toModel() = IptvList(
    id = id,
    name = name,
    type = ListType.valueOf(type),
    server = server,
    username = username,
    liveExtension = liveExtension,
    filePath = filePath,
    channelCount = channelCount,
    movieCount = movieCount,
    seriesCount = seriesCount,
    createdAt = createdAt,
    lastImportedAt = lastImportedAt,
)
