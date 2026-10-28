package com.danielcrvo.sintonia.feature.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.danielcrvo.sintonia.R
import com.danielcrvo.sintonia.core.common.formatClock
import com.danielcrvo.sintonia.core.common.formatDuration
import com.danielcrvo.sintonia.core.designsystem.components.ButtonVariant
import com.danielcrvo.sintonia.core.designsystem.components.SButton
import com.danielcrvo.sintonia.core.designsystem.components.SIcon
import com.danielcrvo.sintonia.core.designsystem.components.SIconButton
import com.danielcrvo.sintonia.core.designsystem.components.SIcons
import com.danielcrvo.sintonia.core.designsystem.components.SImage
import com.danielcrvo.sintonia.core.designsystem.components.SLiveBadge
import com.danielcrvo.sintonia.core.designsystem.components.SOptionRow
import com.danielcrvo.sintonia.core.designsystem.components.SProgressBar
import com.danielcrvo.sintonia.core.designsystem.components.SSeekBar
import com.danielcrvo.sintonia.core.designsystem.components.SSpinner
import com.danielcrvo.sintonia.core.designsystem.components.SText
import com.danielcrvo.sintonia.core.designsystem.components.sintoniaClickable
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme
import com.danielcrvo.sintonia.core.player.PlaybackErrorKind
import com.danielcrvo.sintonia.core.player.PlaybackKind
import com.danielcrvo.sintonia.core.player.PlaybackStatus
import com.danielcrvo.sintonia.core.player.VideoFit

class PlayerActions(
    val onBack: () -> Unit,
    val onToggleControls: () -> Unit,
    val onPlayPause: () -> Unit,
    val onSeekBy: (Long) -> Unit,
    val onSeekTo: (Long) -> Unit,
    val onZap: (Int) -> Unit,
    val onToggleFavorite: () -> Unit,
    val onOpenTracks: () -> Unit,
    val onClosePanel: () -> Unit,
    val onSelectAudio: (String?) -> Unit,
    val onSelectText: (String?) -> Unit,
    val onRetry: () -> Unit,
    val onPlayNextNow: () -> Unit = {},
    val onCancelNext: () -> Unit = {},
    val onKeepControls: () -> Unit = {},
    val onCycleFit: () -> Unit = {},
)

@Composable
fun PlayerScreen(
    state: PlayerUiState,
    position: State<Long>,
    actions: PlayerActions,
    modifier: Modifier = Modifier,
    surface: @Composable () -> Unit,
) {
    val focus = remember { FocusRequester() }
    val playPauseFocus = remember { FocusRequester() }
    val panelFocus = remember { FocusRequester() }
    val upNextFocus = remember { FocusRequester() }
    val retryFocus = remember { FocusRequester() }
    val failed = state.playback.status == PlaybackStatus.Error
    val overlayOpen = state.panel != null || state.upNext != null
    val controlsShown = state.controlsVisible && state.playback.status != PlaybackStatus.Error
    // Focus follows what is in front: card, panel, controls, or else the root that handles the D-pad.
    LaunchedEffect(state.panel, state.upNext != null, controlsShown, failed) {
        runCatching {
            when {
                failed -> retryFocus.requestFocus()
                state.panel != null -> panelFocus.requestFocus()
                state.upNext != null -> upNextFocus.requestFocus()
                controlsShown -> playPauseFocus.requestFocus()
                else -> focus.requestFocus()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focus)
            .focusable()
            .onKeyEvent { event -> handleKey(event, state, actions) }
            .pointerInput(Unit) { detectTapGestures(onTap = { actions.onToggleControls() }) },
    ) {
        surface()

        AnimatedVisibility(
            visible = state.fitHint,
            enter = fadeIn(SintoniaTheme.motion.fast()),
            exit = fadeOut(SintoniaTheme.motion.normal()),
            modifier = Modifier.align(Alignment.TopCenter).safeDrawingPadding().padding(top = SintoniaTheme.heights.header),
        ) {
            SText(
                stringResource(fitLabel(state.videoFit)),
                style = SintoniaTheme.typography.subheading,
                color = Color.White,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(SintoniaTheme.radii.control))
                    .padding(horizontal = SintoniaTheme.spacing.lg, vertical = SintoniaTheme.spacing.sm),
            )
        }

        if (state.playback.status == PlaybackStatus.Loading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { SSpinner(size = SintoniaTheme.sizes.spinnerHero, color = Color.White) }
        }

        AnimatedVisibility(
            visible = controlsShown,
            enter = fadeIn(SintoniaTheme.motion.normal()),
            exit = fadeOut(SintoniaTheme.motion.normal()),
        ) {
            Controls(state, position, actions, playPauseFocus, interactive = !overlayOpen)
        }

        state.upNext?.let { next ->
            NextEpisodeCard(
                upNext = next,
                onCancel = actions.onCancelNext,
                onPlayNow = actions.onPlayNextNow,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .safeDrawingPadding()
                    .padding(SintoniaTheme.spacing.xl)
                    .focusRequester(upNextFocus)
                    .focusGroup(),
            )
        }

        if (state.playback.status == PlaybackStatus.Error) {
            ErrorOverlay(state.playback.error, onRetry = actions.onRetry, onBack = actions.onBack, retryModifier = Modifier.focusRequester(retryFocus))
        }

        AnimatedVisibility(
            visible = state.panel == PlayerPanel.Tracks,
            enter = fadeIn(SintoniaTheme.motion.fast()) + slideInHorizontally(SintoniaTheme.motion.normal()) { it / 3 },
            exit = fadeOut(SintoniaTheme.motion.fast()) + slideOutHorizontally(SintoniaTheme.motion.fast()) { it / 3 },
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            TrackPanel(state, actions, Modifier.focusRequester(panelFocus).focusGroup())
        }
    }
}

private fun handleKey(event: androidx.compose.ui.input.key.KeyEvent, state: PlayerUiState, actions: PlayerActions): Boolean {
    if (event.type != KeyEventType.KeyUp) return false
    // Runs after the children: with controls visible the D-pad navigates, hidden it zaps/seeks.
    val direct = !state.controlsVisible && state.panel == null && state.upNext == null
    if (!direct && state.controlsVisible && event.key in DirectionalKeys) actions.onKeepControls()
    return when (event.key) {
        Key.DirectionUp -> if (direct && state.isLive && state.canZap) {
            actions.onZap(-1)
            true
        } else {
            false
        }
        Key.DirectionDown -> if (direct && state.isLive && state.canZap) {
            actions.onZap(1)
            true
        } else {
            false
        }
        Key.DirectionLeft -> if (direct && !state.isLive) {
            actions.onSeekBy(-SeekStepMs)
            true
        } else {
            false
        }
        Key.DirectionRight -> if (direct && !state.isLive) {
            actions.onSeekBy(SeekStepMs)
            true
        } else {
            false
        }
        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> if (direct) {
            actions.onToggleControls()
            true
        } else {
            false
        }
        Key.MediaPlayPause, Key.MediaPlay, Key.MediaPause -> {
            actions.onPlayPause()
            true
        }
        Key.MediaNext, Key.ChannelUp -> {
            actions.onZap(1)
            true
        }
        Key.MediaPrevious, Key.ChannelDown -> {
            actions.onZap(-1)
            true
        }
        else -> false
    }
}

private const val SeekStepMs = 10_000L

private val DirectionalKeys = setOf(Key.DirectionUp, Key.DirectionDown, Key.DirectionLeft, Key.DirectionRight)

@Composable
private fun Controls(state: PlayerUiState, position: State<Long>, actions: PlayerActions, playPauseFocus: FocusRequester, interactive: Boolean) {
    Box(
        Modifier
            .fillMaxSize()
            .focusProperties { canFocus = interactive }
            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent, Color.Black.copy(alpha = 0.8f))))
            .symmetricSafeDrawingPadding()
            .padding(horizontal = SintoniaTheme.spacing.gutter, vertical = SintoniaTheme.spacing.sm),
    ) {
        TopBar(state, actions, Modifier.align(Alignment.TopCenter))
        CenterControls(state, actions, playPauseFocus, Modifier.align(Alignment.Center))
        BottomInfo(state, position, actions, Modifier.align(Alignment.BottomCenter))
    }
}

/** Same horizontal inset on both sides (the larger) so controls are not skewed by a one-sided cutout. */
@Composable
private fun Modifier.symmetricSafeDrawingPadding(): Modifier {
    val insets = WindowInsets.safeDrawing
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val horizontal = with(density) {
        maxOf(insets.getLeft(density, layoutDirection), insets.getRight(density, layoutDirection)).toDp()
    }
    return this
        .windowInsetsPadding(insets.only(WindowInsetsSides.Vertical))
        .padding(horizontal = horizontal)
}

@Composable
private fun TopBar(state: PlayerUiState, actions: PlayerActions, modifier: Modifier) {
    val tv = SintoniaTheme.deviceClass == com.danielcrvo.sintonia.core.designsystem.theme.DeviceClass.Tv
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.md),
    ) {
        if (!tv) PlayerIconButton(SIcons.ArrowBack, stringResource(R.string.action_back), actions.onBack, bordered = true)
        val channel = state.channel
        if (channel != null) {
            SImage(
                channel.logoUrl,
                null,
                Modifier
                    .size(
                        SintoniaTheme.sizes.avatarSmall,
                    ).clip(CircleShape)
                    .border(SintoniaTheme.strokes.hairline, Color.White.copy(alpha = 0.2f), CircleShape),
            )
        }
        Column(Modifier.weight(1f)) {
            SText(state.session?.title.orEmpty(), style = SintoniaTheme.typography.subheading, color = Color.White, maxLines = 1)
            state.session?.let { session ->
                val episodeText = if (session.kind == PlaybackKind.Episode && session.season != null && session.episode != null) {
                    stringResource(R.string.player_episode_subtitle, session.season, session.episode, session.subtitle.orEmpty())
                } else {
                    session.subtitle
                }
                episodeText?.let {
                    SText(it, style = SintoniaTheme.typography.caption, color = Color.White.copy(alpha = 0.7f), maxLines = 1)
                }
            }
        }
        if (state.isLive) SLiveBadge(stringResource(R.string.player_live))
        if (state.isLive) {
            PlayerIconButton(
                icon = if (state.favorite) SIcons.Star else SIcons.StarOutline,
                description = stringResource(if (state.favorite) R.string.channel_unfavorite else R.string.channel_favorite),
                onClick = actions.onToggleFavorite,
                bordered = true,
            )
        }
        PlayerIconButton(SIcons.Aspect, stringResource(R.string.player_aspect), actions.onCycleFit, bordered = true)
        PlayerIconButton(SIcons.Subtitles, stringResource(R.string.player_tracks), actions.onOpenTracks, bordered = true)
    }
}

@Composable
private fun CenterControls(state: PlayerUiState, actions: PlayerActions, playPauseFocus: FocusRequester, modifier: Modifier) {
    val playing = state.playback.status == PlaybackStatus.Playing || state.playback.status == PlaybackStatus.Loading
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.xxxl), verticalAlignment = Alignment.CenterVertically) {
        if (state.isLive && state.canZap) {
            PlayerIconButton(SIcons.SkipPrevious, stringResource(R.string.player_previous_channel), { actions.onZap(-1) })
        } else if (!state.isLive) {
            PlayerIconButton(SIcons.Refresh, stringResource(R.string.player_rewind), { actions.onSeekBy(-SeekStepMs) }, flip = true)
        }
        PlayPauseButton(playing = playing, onClick = actions.onPlayPause, modifier = Modifier.focusRequester(playPauseFocus))
        if (state.isLive && state.canZap) {
            PlayerIconButton(SIcons.SkipNext, stringResource(R.string.player_next_channel), { actions.onZap(1) })
        } else if (!state.isLive) {
            PlayerIconButton(SIcons.Refresh, stringResource(R.string.player_forward), { actions.onSeekBy(SeekStepMs) })
        }
    }
}

@Composable
private fun PlayPauseButton(playing: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = SintoniaTheme.colors
    Box(
        modifier
            .size(SintoniaTheme.sizes.playButton)
            .sintoniaClickable(onClick = onClick, cornerRadius = SintoniaTheme.sizes.playButton / 2)
            .background(colors.primary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        SIcon(
            icon = if (playing) SIcons.Pause else SIcons.Play,
            contentDescription = stringResource(if (playing) R.string.player_pause else R.string.player_play),
            tint = colors.onPrimary,
            size = SintoniaTheme.sizes.iconLarge,
        )
    }
}

@Composable
private fun PlayerIconButton(icon: Int, description: String, onClick: () -> Unit, flip: Boolean = false, bordered: Boolean = false) {
    val shape = RoundedCornerShape(SintoniaTheme.radii.control)
    Box(
        Modifier
            .size(SintoniaTheme.heights.minTouch)
            .sintoniaClickable(onClick = onClick, cornerRadius = SintoniaTheme.radii.control)
            .then(if (bordered) Modifier.border(SintoniaTheme.strokes.hairline, Color.White.copy(alpha = 0.3f), shape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        SIcon(
            icon = icon,
            contentDescription = description,
            tint = Color.White,
            size = SintoniaTheme.sizes.iconMedium,
            modifier = if (flip) Modifier.graphicsLayer { scaleX = -1f } else Modifier,
        )
    }
}

@Composable
private fun BottomInfo(state: PlayerUiState, position: State<Long>, actions: PlayerActions, modifier: Modifier) {
    if (state.isLive) {
        LiveBottom(state, modifier)
    } else {
        val duration = state.playback.durationMs
        Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.xs)) {
            SSeekBar(positionMs = { position.value }, durationMs = duration, onSeek = actions.onSeekTo)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SText(formatDuration(position.value), style = SintoniaTheme.typography.mono, color = Color.White)
                SText(formatDuration(duration), style = SintoniaTheme.typography.mono, color = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun LiveBottom(state: PlayerUiState, modifier: Modifier) {
    val now = state.nowProgram
    val next = state.nextProgram
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.sm)) {
        if (now != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.md), verticalAlignment = Alignment.CenterVertically) {
                SText(stringResource(R.string.program_now).uppercase(), style = SintoniaTheme.typography.monoLabel, color = Color.White.copy(alpha = 0.7f))
                SText(now.title, style = SintoniaTheme.typography.bodyStrong, color = Color.White, maxLines = 1, modifier = Modifier.weight(1f))
                SText(
                    remember(now.startMillis, now.stopMillis) { "${formatClock(now.startMillis)} – ${formatClock(now.stopMillis)}" },
                    style = SintoniaTheme.typography.mono,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }
            SProgressBar(progress = { now.progressAt(System.currentTimeMillis()) }, height = SintoniaTheme.sizes.progressThin, onImage = true)
        } else {
            SText(stringResource(R.string.program_unknown), style = SintoniaTheme.typography.caption, color = Color.White.copy(alpha = 0.7f))
        }
        if (next != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.md)) {
                SText(stringResource(R.string.program_next).uppercase(), style = SintoniaTheme.typography.monoLabel, color = Color.White.copy(alpha = 0.7f))
                SText(
                    next.title,
                    style = SintoniaTheme.typography.caption,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                SText(
                    remember(next.startMillis) { formatClock(next.startMillis) },
                    style = SintoniaTheme.typography.mono,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
@Suppress("ModifierParameter") // the received modifier belongs to the retry button, not the root
private fun ErrorOverlay(error: PlaybackErrorKind?, onRetry: () -> Unit, onBack: () -> Unit, retryModifier: Modifier = Modifier) {
    Column(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)).padding(SintoniaTheme.spacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.md, Alignment.CenterVertically),
    ) {
        SIcon(SIcons.AlertCircle, null, tint = SintoniaTheme.colors.danger, size = SintoniaTheme.sizes.iconHero)
        SText(stringResource(R.string.player_error_title), style = SintoniaTheme.typography.subheading, color = Color.White, textAlign = TextAlign.Center)
        SText(
            stringResource(
                when (error) {
                    PlaybackErrorKind.Network -> R.string.player_error_network
                    PlaybackErrorKind.Unsupported -> R.string.player_error_unsupported
                    PlaybackErrorKind.Source -> R.string.player_error_source
                    else -> R.string.player_error_unknown
                },
            ),
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.sm), modifier = Modifier.padding(top = SintoniaTheme.spacing.sm)) {
            SButton(stringResource(R.string.action_back), onBack, variant = ButtonVariant.Secondary)
            SButton(stringResource(R.string.error_retry), onRetry, retryModifier)
        }
    }
}

@Composable
private fun TrackPanel(state: PlayerUiState, actions: PlayerActions, modifier: Modifier = Modifier) {
    val colors = SintoniaTheme.colors
    Column(
        modifier
            .fillMaxHeight()
            .width(SintoniaTheme.sizes.sidePanelWidth)
            .background(colors.surface)
            .border(SintoniaTheme.strokes.hairline, colors.border)
            .safeDrawingPadding()
            .padding(SintoniaTheme.spacing.lg)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SText(stringResource(R.string.player_tracks), style = SintoniaTheme.typography.subheading, modifier = Modifier.weight(1f))
            SIconButton(SIcons.Close, stringResource(R.string.player_close), actions.onClosePanel)
        }
        val tracks = state.tracks
        if (tracks.audio.isEmpty() && tracks.text.isEmpty()) {
            SText(stringResource(R.string.player_tracks_empty), color = colors.textSecondary)
        }
        if (tracks.audio.isNotEmpty()) {
            SText(stringResource(R.string.player_audio).uppercase(), style = SintoniaTheme.typography.monoLabel, color = colors.textMuted)
            tracks.audio.forEach { SOptionRow(it.label, it.selected, { actions.onSelectAudio(it.id) }) }
        }
        if (tracks.text.isNotEmpty()) {
            SText(
                stringResource(R.string.player_subtitles).uppercase(),
                style = SintoniaTheme.typography.monoLabel,
                color = colors.textMuted,
                modifier = Modifier.padding(top = SintoniaTheme.spacing.sm),
            )
            SOptionRow(stringResource(R.string.player_subtitles_off), !tracks.textEnabled, { actions.onSelectText(null) })
            tracks.text.forEach { SOptionRow(it.label, it.selected && tracks.textEnabled, { actions.onSelectText(it.id) }) }
        }
    }
}

private fun fitLabel(fit: VideoFit) = when (fit) {
    VideoFit.Fit -> R.string.player_fit_fit
    VideoFit.Zoom -> R.string.player_fit_zoom
    VideoFit.Stretch -> R.string.player_fit_stretch
    VideoFit.Wide -> R.string.player_fit_wide
    VideoFit.Classic -> R.string.player_fit_classic
}
