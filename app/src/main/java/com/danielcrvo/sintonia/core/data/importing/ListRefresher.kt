package com.danielcrvo.sintonia.core.data.importing

import com.danielcrvo.sintonia.core.data.ListsRepository
import com.danielcrvo.sintonia.core.datastore.AppPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListRefresher
    @Inject
    constructor(
        private val lists: ListsRepository,
        private val importer: ListImporter,
        private val preferences: AppPreferences,
    ) {
        private val running = Mutex()
        var now: () -> Long = System::currentTimeMillis

        /** false only when it tried and failed, so the scheduler retries. */
        suspend fun refreshIfStale(): Boolean {
            if (!preferences.autoUpdate.first()) return true
            val list = lists.activeList.first()?.takeIf { it.hasContent } ?: return true
            if (!running.tryLock()) return true
            return try {
                val importedAt = list.lastImportedAt ?: return true
                val guideAt = maxOf(preferences.guideUpdatedAt(list.id) ?: 0L, importedAt)
                when {
                    now() - importedAt > ListMaxAgeMillis -> {
                        importer.import(list.id).collect { }
                        preferences.setGuideUpdatedAt(list.id, now())
                        true
                    }
                    now() - guideAt > GuideMaxAgeMillis -> {
                        if (importer.refreshGuide(list.id)) preferences.setGuideUpdatedAt(list.id, now())
                        true
                    }
                    else -> true
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                false
            } finally {
                running.unlock()
            }
        }

        private companion object {
            const val ListMaxAgeMillis = 24 * 3_600_000L
            const val GuideMaxAgeMillis = 6 * 3_600_000L
        }
    }
