package com.danielcrvo.sintonia.core.player

import androidx.media3.ui.PlayerView
import kotlinx.coroutines.flow.StateFlow

interface Playback {
    val state: StateFlow<PlaybackState>

    /** Separate from state because it changes constantly: read it only where it is drawn. */
    val position: StateFlow<Long>
    val tracks: StateFlow<TrackOptions>

    fun bindSurface(view: PlayerView)
    fun unbindSurface(view: PlayerView)

    fun start(session: PlaybackSession)
    fun stop()
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun seekBy(deltaMs: Long)
    fun retry()

    fun selectAudio(id: String?)

    fun selectText(id: String?)

    fun setLanguagePreferences(audio: String?, subtitle: String?)
}
