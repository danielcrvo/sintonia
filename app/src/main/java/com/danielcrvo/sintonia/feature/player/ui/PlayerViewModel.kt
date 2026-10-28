package com.danielcrvo.sintonia.feature.player.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danielcrvo.sintonia.core.data.ChannelScope
import com.danielcrvo.sintonia.core.data.DetailsRepository
import com.danielcrvo.sintonia.core.data.ListsRepository
import com.danielcrvo.sintonia.core.data.LiveRepository
import com.danielcrvo.sintonia.core.data.MovieRepository
import com.danielcrvo.sintonia.core.data.ParentalRepository
import com.danielcrvo.sintonia.core.data.ProgressRepository
import com.danielcrvo.sintonia.core.data.SeriesRepository
import com.danielcrvo.sintonia.core.data.StreamUrlResolver
import com.danielcrvo.sintonia.core.datastore.AppPreferences
import com.danielcrvo.sintonia.core.model.Channel
import com.danielcrvo.sintonia.core.model.ContentKind
import com.danielcrvo.sintonia.core.model.Episode
import com.danielcrvo.sintonia.core.model.EpisodeOrder
import com.danielcrvo.sintonia.core.model.IptvList
import com.danielcrvo.sintonia.core.model.LiveScope
import com.danielcrvo.sintonia.core.model.PlayerRequest
import com.danielcrvo.sintonia.core.model.Program
import com.danielcrvo.sintonia.core.model.ProgressKind
import com.danielcrvo.sintonia.core.model.Season
import com.danielcrvo.sintonia.core.model.Series
import com.danielcrvo.sintonia.core.model.WatchProgress
import com.danielcrvo.sintonia.core.player.Playback
import com.danielcrvo.sintonia.core.player.PlaybackKind
import com.danielcrvo.sintonia.core.player.PlaybackSession
import com.danielcrvo.sintonia.core.player.PlaybackState
import com.danielcrvo.sintonia.core.player.PlaybackStatus
import com.danielcrvo.sintonia.core.player.TrackOptions
import com.danielcrvo.sintonia.core.player.VideoFit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel as EventChannel

enum class PlayerPanel { Tracks }

@Immutable
data class UpNext(val episode: Episode, val secondsLeft: Int)

@Immutable
data class PlayerUiState(
    val playback: PlaybackState = PlaybackState(),
    val tracks: TrackOptions = TrackOptions(),
    val controlsVisible: Boolean = true,
    val panel: PlayerPanel? = null,
    val channel: Channel? = null,
    val favorite: Boolean = false,
    val nowProgram: Program? = null,
    val nextProgram: Program? = null,
    val canZap: Boolean = false,
    val upNext: UpNext? = null,
    val videoFit: VideoFit = VideoFit.Fit,
    val fitHint: Boolean = false,
) {
    val session: PlaybackSession? get() = playback.session
    val isLive: Boolean get() = playback.session?.kind == PlaybackKind.Live || channel != null
}

sealed interface PlayerEffect {
    data object Close : PlayerEffect
}

@HiltViewModel
class PlayerViewModel
    @Inject
    constructor(
        private val lists: ListsRepository,
        private val live: LiveRepository,
        private val movies: MovieRepository,
        private val seriesRepo: SeriesRepository,
        private val details: DetailsRepository,
        private val progress: ProgressRepository,
        private val parental: ParentalRepository,
        private val resolver: StreamUrlResolver,
        private val preferences: AppPreferences,
        val playback: Playback,
    ) : ViewModel() {
        private val local = MutableStateFlow(Local())
        private var list: IptvList? = null
        private var queue: List<Long> = emptyList()
        private var queueIndex = 0
        private var started: PlayerRequest? = null
        private var zapJob: Job? = null
        private var hideJob: Job? = null
        private var favoriteJob: Job? = null
        private var programJob: Job? = null
        private var saveJob: Job? = null
        private var countdownJob: Job? = null
        private var fitHintJob: Job? = null

        private var currentSeries: Series? = null
        private var seasons: List<Season> = emptyList()

        private val _effects = EventChannel<PlayerEffect>(EventChannel.BUFFERED)
        val effects = _effects.receiveAsFlow()

        /** Position in ms; drawing reads it here without recomposing the whole screen. */
        val position: StateFlow<Long> = playback.position

        val state: StateFlow<PlayerUiState> = combine(playback.state, playback.tracks, local, preferences.videoFit) { play, tracks, local, fit ->
            PlayerUiState(
                playback = play,
                tracks = tracks,
                controlsVisible = local.controlsVisible || play.status == PlaybackStatus.Error,
                panel = local.panel,
                channel = local.channel,
                favorite = local.favorite,
                nowProgram = local.now,
                nextProgram = local.next,
                canZap = queue.size > 1,
                upNext = local.upNext,
                videoFit = fit,
                fitHint = local.fitHint,
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, PlayerUiState())

        init {
            viewModelScope.launch {
                combine(preferences.audioLanguage, preferences.subtitleLanguage) { audio, subtitle ->
                    playback.setLanguagePreferences(audio.tag, subtitle.tag)
                }.collect { }
            }
            viewModelScope.launch {
                playback.state.distinctUntilChangedBy { it.status }.collect { play ->
                    if (play.status == PlaybackStatus.Ended) onEnded(play.session)
                }
            }
        }

        /** Starts once per request; repeated calls (rotation, recomposition) do not restart the video. */
        fun start(request: PlayerRequest) {
            if (started == request) return
            started = request
            viewModelScope.launch {
                val active = lists.activeList.first()
                if (active == null) {
                    _effects.send(PlayerEffect.Close)
                    return@launch
                }
                list = active
                when (request) {
                    is PlayerRequest.Live -> startLive(active, request)
                    is PlayerRequest.Movie -> startMovie(active, request)
                    is PlayerRequest.Episode -> startEpisode(active, request)
                    is PlayerRequest.SeriesItem -> startSeriesItem(active, request)
                }
            }
        }

        private suspend fun startLive(active: IptvList, request: PlayerRequest.Live) {
            val hidden = parental.lockedCategories(active.id, ContentKind.Live).first()
            val scope = when (request.scope.kind) {
                LiveScope.Kind.All -> ChannelScope.All
                LiveScope.Kind.Favorites -> ChannelScope.Favorites
                LiveScope.Kind.Category -> ChannelScope.InCategory(request.scope.categoryId.orEmpty())
            }
            // The queue already excludes blocked categories; a blocked channel request closes the player.
            queue = live.channelIds(active.id, scope, hidden)
            queueIndex = queue.indexOf(request.channelId)
            if (queueIndex == -1) {
                _effects.send(PlayerEffect.Close)
                return
            }
            playChannel(request.channelId)
            followFavorite(active.id)
        }

        private suspend fun playChannel(channelId: Long) {
            val active = list ?: return
            val channel = live.channel(channelId)
            val url = channel?.let { resolver.channel(active, it) }
            if (channel == null || url == null) {
                _effects.send(PlayerEffect.Close)
                return
            }
            local.update { it.copy(channel = channel, now = null, next = null, favorite = false) }
            playback.start(
                PlaybackSession(
                    kind = PlaybackKind.Live,
                    listId = active.id,
                    externalId = channel.externalId,
                    title = channel.name,
                    subtitle = null,
                    artworkUrl = channel.logoUrl,
                    url = url,
                    channelId = channel.id,
                    epgId = channel.epgId,
                ),
            )
            scheduleHide()
            followProgram(active.id, channel)
        }

        private fun followFavorite(listId: Long) {
            favoriteJob?.cancel()
            favoriteJob = viewModelScope.launch {
                live.favoriteIds(listId).collect { ids ->
                    val externalId = local.value.channel?.externalId
                    local.update { it.copy(favorite = externalId != null && externalId in ids) }
                }
            }
        }

        private fun followProgram(listId: Long, channel: Channel) {
            programJob?.cancel()
            programJob = viewModelScope.launch {
                val epgId = channel.epgId ?: return@launch
                while (true) {
                    val upcoming = live.nowAndNext(listId, epgId, System.currentTimeMillis())
                    val now = upcoming.firstOrNull()?.takeIf { it.isLiveAt(System.currentTimeMillis()) }
                    val next = if (now != null) upcoming.getOrNull(1) else upcoming.firstOrNull()
                    local.update { if (it.channel?.id == channel.id) it.copy(now = now, next = next) else it }
                    delay(ProgramRefreshMillis)
                }
            }
        }

        private suspend fun startMovie(active: IptvList, request: PlayerRequest.Movie) {
            val movie = movies.movie(request.movieId)
            val hidden = parental.lockedCategories(active.id, ContentKind.Movie).first()
            if (movie == null || movie.categoryId in hidden) {
                _effects.send(PlayerEffect.Close)
                return
            }
            val url = resolver.movie(active, movie.externalId, movie.url, movie.containerExtension)
            if (url == null) {
                _effects.send(PlayerEffect.Close)
                return
            }
            val saved = progress.get(active.id, ProgressKind.Movie, movie.externalId)
            playVod(
                PlaybackSession(
                    kind = PlaybackKind.Movie,
                    listId = active.id,
                    externalId = movie.externalId,
                    title = movie.name,
                    subtitle = movie.year,
                    artworkUrl = movie.posterUrl,
                    url = url,
                    startPositionMs = resumeAt(saved),
                    extension = movie.containerExtension,
                ),
            )
        }

        private suspend fun startSeriesItem(active: IptvList, request: PlayerRequest.SeriesItem) {
            val item = seriesRepo.series(request.seriesId)
            val hidden = parental.lockedCategories(active.id, ContentKind.Series).first()
            val url = item?.url
            if (item == null || url == null || item.categoryId in hidden) {
                _effects.send(PlayerEffect.Close)
                return
            }
            val saved = progress.get(active.id, ProgressKind.Movie, item.externalId)
            playVod(
                PlaybackSession(
                    kind = PlaybackKind.Movie,
                    listId = active.id,
                    externalId = item.externalId,
                    title = item.name,
                    subtitle = null,
                    artworkUrl = item.posterUrl,
                    url = url,
                    startPositionMs = resumeAt(saved),
                ),
            )
        }

        private suspend fun startEpisode(active: IptvList, request: PlayerRequest.Episode) {
            val series = seriesRepo.series(request.seriesId)
            val hidden = parental.lockedCategories(active.id, ContentKind.Series).first()
            if (series == null || series.categoryId in hidden) {
                _effects.send(PlayerEffect.Close)
                return
            }
            currentSeries = series
            seasons = runCatching { details.seriesInfo(active, series.externalId)?.seasons }.getOrNull().orEmpty()
            val episode = EpisodeOrder.find(seasons, request.episodeId)
            if (episode == null) {
                _effects.send(PlayerEffect.Close)
                return
            }
            playEpisode(active, series, episode)
        }

        private suspend fun playEpisode(active: IptvList, series: Series, episode: Episode) {
            val url = resolver.episode(active, episode.externalId, episode.containerExtension)
            if (url == null) {
                _effects.send(PlayerEffect.Close)
                return
            }
            countdownJob?.cancel()
            local.update { it.copy(upNext = null) }
            val saved = progress.get(active.id, ProgressKind.Episode, episode.externalId)
            playVod(
                PlaybackSession(
                    kind = PlaybackKind.Episode,
                    listId = active.id,
                    externalId = episode.externalId,
                    title = series.name,
                    subtitle = episode.title,
                    artworkUrl = episode.imageUrl ?: series.posterUrl,
                    url = url,
                    startPositionMs = resumeAt(saved),
                    seriesId = series.externalId,
                    season = episode.season,
                    episode = episode.number,
                    extension = episode.containerExtension,
                ),
            )
        }

        private fun playVod(session: PlaybackSession) {
            local.update { it.copy(channel = null, now = null, next = null, favorite = false) }
            playback.start(session)
            scheduleHide()
            startSaving()
        }

        private fun resumeAt(saved: WatchProgress?): Long = when {
            saved == null || saved.finished -> 0L
            saved.positionMillis < MinResumeMillis -> 0L
            else -> saved.positionMillis
        }

        private fun startSaving() {
            saveJob?.cancel()
            saveJob = viewModelScope.launch {
                while (true) {
                    delay(SaveIntervalMillis)
                    if (playback.state.value.status == PlaybackStatus.Playing) saveNow()
                }
            }
        }

        fun saveNow() {
            val session = playback.state.value.session ?: return
            if (session.kind == PlaybackKind.Live) return
            val duration = playback.state.value.durationMs
            if (duration <= 0) return
            val position = if (playback.state.value.status == PlaybackStatus.Ended) duration else playback.position.value
            val entry = WatchProgress(
                kind = if (session.kind == PlaybackKind.Episode) ProgressKind.Episode else ProgressKind.Movie,
                externalId = session.externalId,
                positionMillis = position,
                durationMillis = duration,
                updatedAt = System.currentTimeMillis(),
                title = session.title,
                posterUrl = session.artworkUrl,
                seriesId = session.seriesId,
                season = session.season,
                episode = session.episode,
                extension = session.extension,
            )
            viewModelScope.launch { progress.save(session.listId, entry) }
        }

        private fun onEnded(session: PlaybackSession?) {
            if (session == null || session.kind != PlaybackKind.Episode) return
            saveNow()
            val series = currentSeries ?: return
            val current = EpisodeOrder.find(seasons, session.externalId) ?: return
            val next = EpisodeOrder.next(seasons, current) ?: return
            countdownJob?.cancel()
            countdownJob = viewModelScope.launch {
                for (left in UpNextSeconds downTo 1) {
                    local.update { it.copy(upNext = UpNext(next, left), controlsVisible = true) }
                    delay(1_000)
                }
                val active = list ?: return@launch
                playEpisode(active, series, next)
            }
        }

        fun onPlayNextNow() {
            val next = local.value.upNext?.episode ?: return
            val active = list ?: return
            val series = currentSeries ?: return
            countdownJob?.cancel()
            viewModelScope.launch { playEpisode(active, series, next) }
        }

        fun onCancelNext() {
            countdownJob?.cancel()
            local.update { it.copy(upNext = null) }
        }

        fun onToggleControls() {
            if (local.value.controlsVisible) hideControls() else showControls()
        }

        fun showControls() {
            local.update { it.copy(controlsVisible = true) }
            scheduleHide()
        }

        private fun hideControls() {
            hideJob?.cancel()
            local.update { it.copy(controlsVisible = false) }
        }

        private fun scheduleHide() {
            hideJob?.cancel()
            hideJob = viewModelScope.launch {
                delay(ControlsHideMillis)
                if (local.value.panel == null && playback.state.value.status == PlaybackStatus.Playing) hideControls()
            }
        }

        fun onPlayPause() {
            if (playback.state.value.status == PlaybackStatus.Playing) {
                playback.pause()
                saveNow()
            } else {
                playback.play()
            }
            showControls()
        }

        fun onSeekBy(deltaMs: Long) {
            playback.seekBy(deltaMs)
            showControls()
        }

        fun onSeekTo(positionMs: Long) {
            playback.seekTo(positionMs)
            showControls()
        }

        fun onRetry() = playback.retry()

        fun onZap(delta: Int) {
            if (queue.size < 2) return
            queueIndex = Math.floorMod(queueIndex + delta, queue.size)
            val target = queue[queueIndex]
            zapJob?.cancel()
            // Fast zapping must not open a connection per tap: only the channel it stops on plays.
            zapJob = viewModelScope.launch {
                delay(ZapDebounceMillis)
                playChannel(target)
            }
        }

        fun onToggleFavorite() {
            val channel = local.value.channel ?: return
            val active = list ?: return
            viewModelScope.launch { live.setFavorite(active.id, channel.externalId, !local.value.favorite) }
            showControls()
        }

        fun onCycleFit(current: VideoFit) {
            viewModelScope.launch { preferences.setVideoFit(current.next()) }
            local.update { it.copy(fitHint = true) }
            fitHintJob?.cancel()
            fitHintJob = viewModelScope.launch {
                delay(FitHintMillis)
                local.update { it.copy(fitHint = false) }
            }
            showControls()
        }

        fun onOpenTracks() {
            local.update { it.copy(panel = PlayerPanel.Tracks, controlsVisible = true) }
            hideJob?.cancel()
        }

        fun onClosePanel() {
            local.update { it.copy(panel = null) }
            scheduleHide()
        }

        fun onSelectAudio(id: String?) = playback.selectAudio(id)

        fun onSelectText(id: String?) = playback.selectText(id)

        /** Leaving the player stops the video (no background playback). */
        override fun onCleared() {
            playback.stop()
        }

        private data class Local(
            val controlsVisible: Boolean = true,
            val panel: PlayerPanel? = null,
            val channel: Channel? = null,
            val favorite: Boolean = false,
            val now: Program? = null,
            val next: Program? = null,
            val upNext: UpNext? = null,
            val fitHint: Boolean = false,
        )

        private companion object {
            const val ControlsHideMillis = 4_000L
            const val ProgramRefreshMillis = 30_000L
            const val SaveIntervalMillis = 10_000L
            const val MinResumeMillis = 10_000L
            const val UpNextSeconds = 10
            const val ZapDebounceMillis = 200L
            const val FitHintMillis = 1_500L
        }
    }
