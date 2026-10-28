package com.danielcrvo.sintonia.core.model

import kotlinx.serialization.Serializable

@Serializable
data class LiveScope(val kind: Kind, val categoryId: String? = null) {
    enum class Kind { All, Favorites, Category }
}

@Serializable
sealed interface PlayerRequest {
    @Serializable
    data class Live(val channelId: Long, val scope: LiveScope = LiveScope(LiveScope.Kind.All)) : PlayerRequest

    @Serializable
    data class Movie(val movieId: Long) : PlayerRequest

    @Serializable
    data class Episode(val seriesId: Long, val episodeId: String) : PlayerRequest

    @Serializable
    data class SeriesItem(val seriesId: Long) : PlayerRequest
}
