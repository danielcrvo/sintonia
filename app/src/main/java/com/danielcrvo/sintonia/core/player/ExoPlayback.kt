package com.danielcrvo.sintonia.core.player

import android.content.Context
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
import androidx.media3.ui.PlayerView
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(UnstableApi::class)
@Singleton
class ExoPlayback
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : Playback {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        private var positionJob: Job? = null
        private var preferredAudio: String? = null
        private var preferredSubtitle: String? = null
        private var appliedPreferencesFor: String? = null

        private val _state = MutableStateFlow(PlaybackState())
        override val state: StateFlow<PlaybackState> = _state.asStateFlow()
        private val _position = MutableStateFlow(0L)
        override val position: StateFlow<Long> = _position.asStateFlow()
        private val _tracks = MutableStateFlow(TrackOptions())
        override val tracks: StateFlow<TrackOptions> = _tracks.asStateFlow()

        private var created = false

        /** Created on first playback so opening the app does not pay for the player. */
        val player: ExoPlayer by lazy { build().also { created = true } }

        private fun build(): ExoPlayer {
            val extractors = DefaultExtractorsFactory()
                .setTsExtractorFlags(DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES or DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS)
                .setConstantBitrateSeekingEnabled(true)
            val http = DefaultHttpDataSource
                .Factory()
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(NetworkTimeoutMs)
                .setReadTimeoutMs(NetworkTimeoutMs)
                .setUserAgent(UserAgent)
            val player = ExoPlayer
                .Builder(context)
                .setMediaSourceFactory(DefaultMediaSourceFactory(context, extractors).setDataSourceFactory(http))
                .setLoadControl(DefaultLoadControl.Builder().setBufferDurationsMs(MinBufferMs, MaxBufferMs, StartBufferMs, RebufferMs).build())
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(C.WAKE_MODE_NETWORK)
                .setLooper(Looper.getMainLooper())
                .build()
            player.setAudioAttributes(
                androidx.media3.common.AudioAttributes
                    .Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true,
            )
            player.addListener(listener)
            return player
        }

        private val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) = publish()
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                publish()
                if (isPlaying) startTicker() else positionJob?.cancel()
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) = publish()

            override fun onPlayerError(error: PlaybackException) {
                _state.update { it.copy(status = PlaybackStatus.Error, error = error.toKind()) }
            }

            override fun onTracksChanged(tracks: Tracks) {
                publishTracks(tracks)
                applyPreferences(tracks)
            }
        }

        override fun bindSurface(view: PlayerView) {
            view.player = player
        }

        override fun unbindSurface(view: PlayerView) {
            if (view.player === player) view.player = null
        }

        override fun start(session: PlaybackSession) {
            _position.value = session.startPositionMs
            _tracks.value = TrackOptions()
            appliedPreferencesFor = null
            _state.value = PlaybackState(PlaybackStatus.Loading, session, 0, null)
            player.setMediaItem(MediaItem.fromUri(session.url), session.startPositionMs.coerceAtLeast(0))
            player.prepare()
            player.playWhenReady = true
        }

        override fun stop() {
            positionJob?.cancel()
            if (playerCreated()) {
                player.stop()
                player.clearMediaItems()
            }
            _state.value = PlaybackState()
            _position.value = 0
            _tracks.value = TrackOptions()
        }

        override fun play() {
            if (_state.value.status == PlaybackStatus.Ended) player.seekTo(0)
            player.play()
        }

        override fun pause() {
            if (playerCreated()) player.pause()
        }

        override fun seekTo(positionMs: Long) {
            player.seekTo(positionMs.coerceAtLeast(0))
            _position.value = positionMs.coerceAtLeast(0)
        }

        override fun seekBy(deltaMs: Long) {
            val duration = player.duration.takeIf { it != C.TIME_UNSET } ?: Long.MAX_VALUE
            seekTo((player.currentPosition + deltaMs).coerceIn(0, duration))
        }

        override fun retry() {
            val session = _state.value.session ?: return
            start(session.copy(startPositionMs = if (session.isLive) 0 else _position.value))
        }

        override fun selectAudio(id: String?) {
            val group = findGroup(C.TRACK_TYPE_AUDIO, id)
            val builder = player.trackSelectionParameters.buildUpon().clearOverridesOfType(C.TRACK_TYPE_AUDIO)
            if (group != null) builder.addOverride(TrackSelectionOverride(group.mediaTrackGroup, 0))
            player.trackSelectionParameters = builder.build()
        }

        override fun selectText(id: String?) {
            val builder = player.trackSelectionParameters.buildUpon().clearOverridesOfType(C.TRACK_TYPE_TEXT)
            val group = findGroup(C.TRACK_TYPE_TEXT, id)
            if (group == null) {
                builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            } else {
                builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false).addOverride(TrackSelectionOverride(group.mediaTrackGroup, 0))
            }
            player.trackSelectionParameters = builder.build()
        }

        override fun setLanguagePreferences(audio: String?, subtitle: String?) {
            preferredAudio = audio
            preferredSubtitle = subtitle
            if (playerCreated()) applyPreferences(player.currentTracks)
        }

        private fun playerCreated(): Boolean = created

        private fun publish() {
            val p = player
            val status = when {
                _state.value.status == PlaybackStatus.Error && p.playbackState != Player.STATE_READY -> PlaybackStatus.Error
                p.playbackState == Player.STATE_IDLE -> if (_state.value.session == null) PlaybackStatus.Idle else _state.value.status
                p.playbackState == Player.STATE_BUFFERING -> PlaybackStatus.Loading
                p.playbackState == Player.STATE_ENDED -> PlaybackStatus.Ended
                p.isPlaying || p.playWhenReady -> PlaybackStatus.Playing
                else -> PlaybackStatus.Paused
            }
            val duration = p.duration.takeIf { it != C.TIME_UNSET && !p.isCurrentMediaItemLive } ?: 0L
            _state.update { it.copy(status = status, durationMs = duration, error = if (status == PlaybackStatus.Error) it.error else null) }
        }

        private fun startTicker() {
            positionJob?.cancel()
            positionJob = scope.launch {
                while (isActive) {
                    _position.value = player.currentPosition.coerceAtLeast(0)
                    publishDuration()
                    delay(TickMs)
                }
            }
        }

        private fun publishDuration() {
            val duration = player.duration.takeIf { it != C.TIME_UNSET && !player.isCurrentMediaItemLive } ?: 0L
            if (duration != _state.value.durationMs) _state.update { it.copy(durationMs = duration) }
        }

        private fun publishTracks(tracks: Tracks) {
            fun options(type: Int) = tracks.groups.filter { it.type == type && it.length > 0 }.mapIndexed { index, group ->
                val format = group.getTrackFormat(0)
                val language = format.language?.takeIf { it != "und" }
                TrackOption(
                    id = trackId(type, index),
                    label =
                        format.label ?: language?.let { Locale.forLanguageTag(it).getDisplayLanguage(Locale.getDefault()).replaceFirstChar(Char::uppercase) }
                            ?: "#${index + 1}",
                    language = language,
                    selected = group.isSelected,
                )
            }
            val text = options(C.TRACK_TYPE_TEXT)
            _tracks.value = TrackOptions(
                audio = options(C.TRACK_TYPE_AUDIO),
                text = text,
                textEnabled = text.any { it.selected } && !player.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT),
            )
        }

        /** Applies the language preference once per media, when tracks appear. */
        private fun applyPreferences(tracks: Tracks) {
            val key = _state.value.session?.url
            if (key == null || appliedPreferencesFor == key || tracks.groups.isEmpty()) return
            appliedPreferencesFor = key
            val params = player.trackSelectionParameters.buildUpon()
            preferredAudio?.let { params.setPreferredAudioLanguage(it) }
            if (preferredSubtitle == null) {
                params.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            } else {
                params.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false).setPreferredTextLanguage(preferredSubtitle)
            }
            player.trackSelectionParameters = params.build()
        }

        private fun findGroup(type: Int, id: String?): Tracks.Group? {
            if (id == null) return null
            val groups = player.currentTracks.groups.filter { it.type == type && it.length > 0 }
            return groups.getOrNull(id.substringAfterLast(':').toIntOrNull() ?: return null)
        }

        private fun trackId(type: Int, index: Int) = "$type:$index"

        private fun PlaybackException.toKind() = when (errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
            -> PlaybackErrorKind.Network
            PlaybackException.ERROR_CODE_DECODING_FAILED,
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
            -> PlaybackErrorKind.Unsupported
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
            -> PlaybackErrorKind.Source
            else -> PlaybackErrorKind.Unknown
        }

        private companion object {
            const val UserAgent = "Sintonia/1.0 (Android)"
            const val NetworkTimeoutMs = 15_000
            const val TickMs = 500L
            const val MinBufferMs = 8_000
            const val MaxBufferMs = 40_000
            const val StartBufferMs = 1_500
            const val RebufferMs = 3_000
        }
    }
