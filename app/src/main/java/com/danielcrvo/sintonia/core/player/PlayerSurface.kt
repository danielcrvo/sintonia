package com.danielcrvo.sintonia.core.player

import android.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PlayerSurface(playback: Playback, modifier: Modifier = Modifier, fit: VideoFit = VideoFit.Fit) {
    val frame = fit.ratio?.let { Modifier.aspectRatio(it) } ?: Modifier.fillMaxSize()
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AndroidView(
            modifier = frame,
            factory = { context ->
                PlayerView(context).apply {
                    useController = false
                    setShutterBackgroundColor(Color.BLACK)
                    setBackgroundColor(Color.BLACK)
                    playback.bindSurface(this)
                }
            },
            update = { view ->
                view.resizeMode = when (fit) {
                    VideoFit.Fit -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    VideoFit.Zoom -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    VideoFit.Stretch, VideoFit.Wide, VideoFit.Classic -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                }
            },
            onRelease = { view -> playback.unbindSurface(view) },
        )
    }
}
