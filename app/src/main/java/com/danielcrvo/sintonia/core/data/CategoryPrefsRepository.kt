package com.danielcrvo.sintonia.core.data

import com.danielcrvo.sintonia.core.datastore.AppPreferences
import com.danielcrvo.sintonia.core.model.Category
import com.danielcrvo.sintonia.core.model.ContentKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.text.Collator
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private val AlphabeticalCollator: Collator = Collator.getInstance(Locale.forLanguageTag("pt-BR")).apply { strength = Collator.PRIMARY }

/** Kept out of the database on purpose: reimport swaps generations and choices key on the provider id. */
@Singleton
class CategoryPrefsRepository
    @Inject
    constructor(private val preferences: AppPreferences) {
        fun decorate(listId: Long, kind: ContentKind, source: Flow<List<Category>>, includeHidden: Boolean): Flow<List<Category>> =
            combine(source, preferences.hiddenCategories(listId, kind), preferences.categoryNames(listId, kind)) { all, hidden, names ->
                val decorated = all.map {
                    it.copy(name = names[it.externalId] ?: it.name, originalName = it.name, hidden = it.externalId in hidden)
                }
                val visible = if (includeHidden) decorated else decorated.filterNot { it.hidden }
                visible.sortedWith(compareBy(AlphabeticalCollator) { it.name })
            }

        suspend fun setHidden(listId: Long, kind: ContentKind, categoryId: String, hidden: Boolean) {
            val current = currentHidden(listId, kind)
            preferences.setHiddenCategories(listId, kind, if (hidden) current + categoryId else current - categoryId)
        }

        suspend fun rename(listId: Long, kind: ContentKind, categoryId: String, name: String?) =
            preferences.setCategoryName(listId, kind, categoryId, name)

        private suspend fun currentHidden(listId: Long, kind: ContentKind): Set<String> =
            preferences.hiddenCategories(listId, kind).first()
    }
