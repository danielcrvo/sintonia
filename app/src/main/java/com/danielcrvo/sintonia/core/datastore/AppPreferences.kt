package com.danielcrvo.sintonia.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.danielcrvo.sintonia.core.designsystem.theme.ThemeMode
import com.danielcrvo.sintonia.core.model.ContentKind
import com.danielcrvo.sintonia.core.player.VideoFit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class AppLanguage(val tag: String?) {
    Auto(null),
    PtBr("pt-BR"),
    En("en"),
    Es("es"),
}

enum class TrackLanguage(val tag: String?) {
    Auto(null),
    Off(null),
    Pt("pt"),
    En("en"),
    Es("es"),
}

@Singleton
class AppPreferences
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val store = context.dataStore

        val themeMode: Flow<ThemeMode> = store.data.map { prefs ->
            prefs[Theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.Auto
        }
        val language: Flow<AppLanguage> = store.data.map { prefs ->
            prefs[Language]?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() } ?: AppLanguage.Auto
        }
        val audioLanguage: Flow<TrackLanguage> = store.data.map { prefs ->
            prefs[Audio]?.let { runCatching { TrackLanguage.valueOf(it) }.getOrNull() } ?: TrackLanguage.Auto
        }
        val subtitleLanguage: Flow<TrackLanguage> = store.data.map { prefs ->
            prefs[Subtitles]?.let { runCatching { TrackLanguage.valueOf(it) }.getOrNull() } ?: TrackLanguage.Off
        }
        val videoFit: Flow<VideoFit> = store.data.map { prefs ->
            prefs[Fit]?.let { runCatching { VideoFit.valueOf(it) }.getOrNull() } ?: VideoFit.Fit
        }
        val activeListId: Flow<Long?> = store.data.map { it[ActiveList] }

        val autoUpdate: Flow<Boolean> = store.data.map { it[AutoUpdate] ?: true }
        val parentalEnabled: Flow<Boolean> = store.data.map { it[ParentalEnabled] ?: false }

        suspend fun setThemeMode(mode: ThemeMode) {
            store.edit { it[Theme] = mode.name }
        }
        suspend fun setLanguage(language: AppLanguage) {
            store.edit { it[Language] = language.name }
        }
        suspend fun setAudioLanguage(language: TrackLanguage) {
            store.edit { it[Audio] = language.name }
        }
        suspend fun setSubtitleLanguage(language: TrackLanguage) {
            store.edit { it[Subtitles] = language.name }
        }
        suspend fun setAutoUpdate(enabled: Boolean) {
            store.edit { it[AutoUpdate] = enabled }
        }

        suspend fun guideUpdatedAt(listId: Long): Long? = store.data.first()[guideKey(listId)]

        suspend fun setGuideUpdatedAt(listId: Long, millis: Long) {
            store.edit { it[guideKey(listId)] = millis }
        }

        suspend fun setParentalEnabled(enabled: Boolean) {
            store.edit { it[ParentalEnabled] = enabled }
        }

        suspend fun setVideoFit(fit: VideoFit) {
            store.edit { it[Fit] = fit.name }
        }

        suspend fun setActiveListId(id: Long?) {
            store.edit { if (id == null) it.remove(ActiveList) else it[ActiveList] = id }
        }

        fun blockedCategories(listId: Long, kind: ContentKind): Flow<Set<String>> =
            store.data.map { it[blockedKey(listId, kind)] ?: emptySet() }

        suspend fun setBlockedCategories(listId: Long, kind: ContentKind, ids: Set<String>) {
            store.edit { if (ids.isEmpty()) it.remove(blockedKey(listId, kind)) else it[blockedKey(listId, kind)] = ids }
        }

        fun hiddenCategories(listId: Long, kind: ContentKind): Flow<Set<String>> =
            store.data.map { it[hiddenKey(listId, kind)] ?: emptySet() }

        suspend fun setHiddenCategories(listId: Long, kind: ContentKind, ids: Set<String>) {
            store.edit { if (ids.isEmpty()) it.remove(hiddenKey(listId, kind)) else it[hiddenKey(listId, kind)] = ids }
        }

        fun categoryNames(listId: Long, kind: ContentKind): Flow<Map<String, String>> = store.data.map { prefs ->
            val prefix = namePrefix(listId, kind)
            prefs
                .asMap()
                .entries
                .filter { it.key.name.startsWith(prefix) }
                .associate { it.key.name.removePrefix(prefix) to it.value.toString() }
        }

        suspend fun setCategoryName(listId: Long, kind: ContentKind, categoryId: String, name: String?) {
            val key = stringPreferencesKey(namePrefix(listId, kind) + categoryId)
            store.edit { if (name.isNullOrBlank()) it.remove(key) else it[key] = name.trim() }
        }

        suspend fun clearCategoryPreferences(listId: Long) {
            store.edit { prefs ->
                val stale = prefs.asMap().keys.filter { key ->
                    ContentKind.entries.any { key.name.startsWith(namePrefix(listId, it)) || key.name == hiddenKey(listId, it).name }
                }
                stale.forEach { prefs.remove(it) }
            }
        }

        suspend fun clearBlockedCategories(listId: Long) {
            store.edit { prefs -> ContentKind.entries.forEach { prefs.remove(blockedKey(listId, it)) } }
        }

        val pinLock: Flow<List<Long>> = store.data.map {
            listOf(
                (it[PinFailures] ?: 0).toLong(),
                it[PinLockedAtWall] ?: 0L,
                it[PinLockedAtElapsed] ?: 0L,
                it[PinLockDuration] ?: 0L,
            )
        }

        suspend fun setPinLock(failures: Int, lockedAtWall: Long, lockedAtElapsed: Long, durationMillis: Long) {
            store.edit {
                it[PinFailures] = failures
                it[PinLockedAtWall] = lockedAtWall
                it[PinLockedAtElapsed] = lockedAtElapsed
                it[PinLockDuration] = durationMillis
            }
        }

        private fun guideKey(listId: Long) = longPreferencesKey("guide_at.$listId")

        private fun hiddenKey(listId: Long, kind: ContentKind) = stringSetPreferencesKey("hidden.$listId.${kind.name}")

        private fun namePrefix(listId: Long, kind: ContentKind) = "catname.$listId.${kind.name}."

        private fun blockedKey(listId: Long, kind: ContentKind) = stringSetPreferencesKey("blocked.$listId.${kind.name}")

        private companion object {
            val Theme = stringPreferencesKey("theme")
            val Language = stringPreferencesKey("language")
            val Audio = stringPreferencesKey("audio_language")
            val Subtitles = stringPreferencesKey("subtitle_language")
            val ActiveList = longPreferencesKey("active_list")
            val Fit = stringPreferencesKey("video_fit")
            val AutoUpdate = booleanPreferencesKey("auto_update")
            val ParentalEnabled = booleanPreferencesKey("parental_enabled")
            val PinFailures = intPreferencesKey("pin_failures")
            val PinLockedAtWall = longPreferencesKey("pin_locked_at_wall")
            val PinLockedAtElapsed = longPreferencesKey("pin_locked_at_elapsed")
            val PinLockDuration = longPreferencesKey("pin_lock_duration")
        }
    }
