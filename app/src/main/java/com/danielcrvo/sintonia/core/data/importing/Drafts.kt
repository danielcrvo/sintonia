package com.danielcrvo.sintonia.core.data.importing

data class CategoryDraft(val externalId: String, val name: String)

data class ChannelDraft(
    val externalId: String,
    val number: Int?,
    val name: String,
    val logo: String?,
    val categoryId: String?,
    val epgId: String?,
    val url: String?,
)

data class MovieDraft(
    val externalId: String,
    val name: String,
    val poster: String?,
    val categoryId: String?,
    val year: String?,
    val rating: Double?,
    val containerExtension: String?,
    val url: String?,
)

data class SeriesDraft(
    val externalId: String,
    val name: String,
    val poster: String?,
    val categoryId: String?,
    val year: String?,
    val rating: Double?,
    val url: String?,
)
