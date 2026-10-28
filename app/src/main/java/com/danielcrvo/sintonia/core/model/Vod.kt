package com.danielcrvo.sintonia.core.model

import androidx.compose.runtime.Immutable

@Immutable
data class Episode(
    val externalId: String,
    val season: Int,
    val number: Int,
    val title: String,
    val durationSecs: Int?,
    val imageUrl: String?,
    val containerExtension: String?,
)

@Immutable
data class Season(val number: Int, val episodes: List<Episode>)

@Immutable
data class VodInfo(
    val plot: String?,
    val cast: String?,
    val director: String?,
    val genre: String?,
    val durationSecs: Int?,
    val backdropUrl: String?,
    val containerExtension: String?,
)

@Immutable
data class SeriesInfo(
    val plot: String?,
    val cast: String?,
    val director: String?,
    val genre: String?,
    val backdropUrl: String?,
    val seasons: List<Season>,
) {
    val episodeCount: Int get() = seasons.sumOf { it.episodes.size }
}

@Immutable
data class WatchProgress(
    val kind: ProgressKind,
    val externalId: String,
    val positionMillis: Long,
    val durationMillis: Long,
    val updatedAt: Long,
    val title: String?,
    val posterUrl: String?,
    val seriesId: String?,
    val season: Int?,
    val episode: Int?,
    val extension: String?,
) {
    val fraction: Float
        get() = if (durationMillis <= 0) 0f else (positionMillis.toFloat() / durationMillis).coerceIn(0f, 1f)

    val finished: Boolean get() = durationMillis > 0 && fraction >= FinishedFraction

    companion object {
        const val FinishedFraction = 0.95f
    }
}

enum class ProgressKind { Movie, Episode }
