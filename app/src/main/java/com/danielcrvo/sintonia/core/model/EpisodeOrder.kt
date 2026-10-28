package com.danielcrvo.sintonia.core.model

object EpisodeOrder {
    fun next(seasons: List<Season>, current: Episode): Episode? {
        val ordered = seasons.sortedBy { it.number }
        val seasonIndex = ordered.indexOfFirst { it.number == current.season }
        if (seasonIndex == -1) return null
        val inSeason = ordered[seasonIndex].episodes.sortedBy { it.number }
        val index = inSeason.indexOfFirst { it.externalId == current.externalId }
        if (index != -1 && index + 1 < inSeason.size) return inSeason[index + 1]
        return ordered
            .drop(seasonIndex + 1)
            .firstOrNull { it.episodes.isNotEmpty() }
            ?.episodes
            ?.minByOrNull { it.number }
    }

    fun find(seasons: List<Season>, episodeId: String): Episode? =
        seasons.asSequence().flatMap { it.episodes.asSequence() }.firstOrNull { it.externalId == episodeId }

    fun resume(seasons: List<Season>, progress: List<WatchProgress>): Episode? {
        val all = seasons.sortedBy { it.number }.flatMap { s -> s.episodes.sortedBy { it.number } }
        if (all.isEmpty()) return null
        val latest = progress.maxByOrNull { it.updatedAt }
        val latestEpisode = latest?.let { p -> all.firstOrNull { it.externalId == p.externalId } }
        return when {
            latestEpisode == null -> all.first()
            latest.finished -> next(seasons, latestEpisode) ?: latestEpisode
            else -> latestEpisode
        }
    }
}
