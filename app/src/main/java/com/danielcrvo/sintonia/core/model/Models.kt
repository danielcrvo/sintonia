package com.danielcrvo.sintonia.core.model

import androidx.compose.runtime.Immutable

enum class ListType { Xtream, M3uUrl, M3uFile }

enum class ContentKind { Live, Movie, Series }

@Immutable
data class IptvList(
    val id: Long,
    val name: String,
    val type: ListType,
    val server: String?,
    val username: String?,
    val liveExtension: String,
    val filePath: String?,
    val channelCount: Int,
    val movieCount: Int,
    val seriesCount: Int,
    val createdAt: Long,
    val lastImportedAt: Long?,
) {
    val hasContent: Boolean get() = lastImportedAt != null
}

@Immutable
data class Category(
    val externalId: String,
    val name: String,
    val kind: ContentKind,
    val count: Int = 0,
    val locked: Boolean = false,
    val originalName: String = name,
    val hidden: Boolean = false,
)

@Immutable
data class Channel(
    val id: Long,
    val externalId: String,
    val number: Int?,
    val name: String,
    val logoUrl: String?,
    val categoryId: String?,
    val epgId: String?,
    val url: String?,
)

@Immutable
data class Movie(
    val id: Long,
    val externalId: String,
    val name: String,
    val posterUrl: String?,
    val categoryId: String?,
    val year: String?,
    val rating: Double?,
    val containerExtension: String?,
    val url: String?,
)

@Immutable
data class Series(
    val id: Long,
    val externalId: String,
    val name: String,
    val posterUrl: String?,
    val categoryId: String?,
    val year: String?,
    val rating: Double?,
    val url: String?,
)

@Immutable
data class Program(
    val startMillis: Long,
    val stopMillis: Long,
    val title: String,
    val description: String?,
) {
    fun progressAt(nowMillis: Long): Float {
        val total = (stopMillis - startMillis).coerceAtLeast(1)
        return ((nowMillis - startMillis).toFloat() / total).coerceIn(0f, 1f)
    }

    fun isLiveAt(nowMillis: Long) = nowMillis in startMillis until stopMillis
}
