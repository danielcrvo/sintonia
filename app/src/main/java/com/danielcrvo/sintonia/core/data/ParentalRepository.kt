package com.danielcrvo.sintonia.core.data

import com.danielcrvo.sintonia.core.datastore.AppPreferences
import com.danielcrvo.sintonia.core.model.ContentKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

interface ParentalRepository {
    val enabled: Flow<Boolean>
    suspend fun setEnabled(enabled: Boolean)

    fun blockedCategories(listId: Long, kind: ContentKind): Flow<List<String>>

    fun lockedCategories(listId: Long, kind: ContentKind): Flow<List<String>>

    fun configuredBlocked(listId: Long, kind: ContentKind): Flow<Set<String>>
    suspend fun setBlocked(listId: Long, kind: ContentKind, ids: Set<String>)

    fun unlock(listId: Long, kind: ContentKind, categoryId: String)

    fun relock()

    val relockCount: kotlinx.coroutines.flow.StateFlow<Int>
}

@Singleton
class DataStoreParentalRepository
    @Inject
    constructor(
        private val preferences: AppPreferences,
    ) : ParentalRepository {
        private val unlocked = MutableStateFlow<Set<String>>(emptySet())
        private val _relockCount = MutableStateFlow(0)
        override val relockCount: kotlinx.coroutines.flow.StateFlow<Int> = _relockCount

        override val enabled: Flow<Boolean> = preferences.parentalEnabled

        override suspend fun setEnabled(enabled: Boolean) = preferences.setParentalEnabled(enabled)

        override fun blockedCategories(listId: Long, kind: ContentKind): Flow<List<String>> =
            combine(
                preferences.parentalEnabled,
                preferences.blockedCategories(listId, kind),
                preferences.hiddenCategories(listId, kind),
            ) { enabled, blocked, hidden ->
                ((if (enabled) blocked else emptySet()) + hidden).sorted()
            }.distinctUntilChanged()

        override fun lockedCategories(listId: Long, kind: ContentKind): Flow<List<String>> =
            combine(blockedCategories(listId, kind), preferences.hiddenCategories(listId, kind), unlocked) { blocked, hidden, open ->
                // Hidden categories cannot be unlocked with a PIN.
                blocked.filter { it in hidden || key(listId, kind, it) !in open }
            }.distinctUntilChanged()

        override fun configuredBlocked(listId: Long, kind: ContentKind): Flow<Set<String>> =
            preferences.blockedCategories(listId, kind)

        override suspend fun setBlocked(listId: Long, kind: ContentKind, ids: Set<String>) {
            preferences.setBlockedCategories(listId, kind, ids)
            unlocked.value = emptySet() // takes effect immediately: newly blocked content is never left unlocked by mistake
        }

        override fun unlock(listId: Long, kind: ContentKind, categoryId: String) {
            unlocked.update { it + key(listId, kind, categoryId) }
        }

        override fun relock() {
            unlocked.value = emptySet()
            _relockCount.update { it + 1 }
        }

        private fun key(listId: Long, kind: ContentKind, categoryId: String) = "$listId:${kind.name}:$categoryId"
    }
