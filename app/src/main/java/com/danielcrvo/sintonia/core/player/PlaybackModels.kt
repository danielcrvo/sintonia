package com.danielcrvo.sintonia.core.player

import androidx.compose.runtime.Immutable

enum class PlaybackKind { Live, Movie, Episode }

@Immutable
data class PlaybackSession(
    val kind: PlaybackKind,
    val listId: Long,
    val externalId: String,
    val title: String,
    val subtitle: String?,
    val artworkUrl: String?,
    val url: String,
    val startPositionMs: Long = 0,
    val channelId: Long? = null,
    val epgId: String? = null,
    val seriesId: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val extension: String? = null,
) {
    val isLive: Boolean get() = kind == PlaybackKind.Live
}

enum class PlaybackStatus { Idle, Loading, Playing, Paused, Ended, Error }

enum class PlaybackErrorKind { Network, Unsupported, Source, Unknown }

@Immutable
data class PlaybackState(
    val status: PlaybackStatus = PlaybackStatus.Idle,
    val session: PlaybackSession? = null,
    val durationMs: Long = 0,
    val error: PlaybackErrorKind? = null,
) {
    val isActive: Boolean get() = session != null && status != PlaybackStatus.Idle
}

@Immutable
data class TrackOption(
    val id: String,
    val label: String,
    val language: String?,
    val selected: Boolean,
)

@Immutable
data class TrackOptions(
    val audio: List<TrackOption> = emptyList(),
    val text: List<TrackOption> = emptyList(),
    val textEnabled: Boolean = false,
)
