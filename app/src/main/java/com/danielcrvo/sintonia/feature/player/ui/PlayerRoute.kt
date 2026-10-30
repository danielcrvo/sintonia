package com.danielcrvo.sintonia.feature.player.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.danielcrvo.sintonia.core.designsystem.theme.DeviceClass
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme
import com.danielcrvo.sintonia.core.designsystem.theme.ThemeMode
import com.danielcrvo.sintonia.core.model.PlayerRequest
import com.danielcrvo.sintonia.core.player.PlayerSurface

@Composable
fun PlayerRoute(
    request: PlayerRequest,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val position = viewModel.position.collectAsStateWithLifecycle()
    val deviceClass = SintoniaTheme.deviceClass
    val view = LocalView.current
    val context = LocalContext.current

    val currentOnClose by rememberUpdatedState(onClose)

    LaunchedEffect(request) { viewModel.start(request) }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                PlayerEffect.Close -> currentOnClose()
            }
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        viewModel.onStop()
    }

    val close = {
        viewModel.saveNow()
        onClose()
    }

    DisposableEffect(deviceClass, view) {
        val activity = context as? Activity
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previousOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        val previousLightStatus = controller?.isAppearanceLightStatusBars ?: false
        val previousLightNavigation = controller?.isAppearanceLightNavigationBars ?: false
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        if (deviceClass == DeviceClass.Phone) activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = false
            controller?.show(WindowInsetsCompat.Type.systemBars())
            controller?.isAppearanceLightStatusBars = previousLightStatus
            controller?.isAppearanceLightNavigationBars = previousLightNavigation
            activity?.requestedOrientation = previousOrientation
        }
    }

    BackHandler {
        when {
            state.panel != null -> viewModel.onClosePanel()
            state.upNext != null -> viewModel.onCancelNext()
            deviceClass == DeviceClass.Tv && state.controlsVisible -> viewModel.hideControls()
            else -> close()
        }
    }

    val actions = remember(viewModel, onClose) {
        PlayerActions(
            onBack = {
                viewModel.saveNow()
                onClose()
            },
            onToggleControls = viewModel::onToggleControls,
            onKeepControls = viewModel::showControls,
            onPlayPause = viewModel::onPlayPause,
            onSeekBy = viewModel::onSeekBy,
            onSeekTo = viewModel::onSeekTo,
            onZap = viewModel::onZap,
            onToggleFavorite = viewModel::onToggleFavorite,
            onOpenTracks = viewModel::onOpenTracks,
            onClosePanel = viewModel::onClosePanel,
            onSelectAudio = viewModel::onSelectAudio,
            onSelectText = viewModel::onSelectText,
            onRetry = viewModel::onRetry,
            onPlayNextNow = viewModel::onPlayNextNow,
            onCancelNext = viewModel::onCancelNext,
            onCycleFit = { viewModel.onCycleFit(state.videoFit) },
        )
    }

    SintoniaTheme(mode = ThemeMode.Dark, deviceClass = deviceClass) {
        PlayerScreen(state = state, position = position, actions = actions, modifier = modifier) {
            PlayerSurface(playback = viewModel.playback, modifier = Modifier, fit = state.videoFit)
        }
    }
}
