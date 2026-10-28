package com.danielcrvo.sintonia.core.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.danielcrvo.sintonia.core.designsystem.theme.DeviceClass
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme
import kotlinx.coroutines.delay

@Stable
class ToastState {
    private var counter = 0
    var current: Message? by mutableStateOf(null)
        private set

    @Stable
    data class Message(val id: Int, val text: String)

    fun show(text: String) {
        current = Message(++counter, text)
    }

    fun dismiss() {
        current = null
    }
}

val LocalToast = staticCompositionLocalOf { ToastState() }

private const val ToastMillis = 3000L

@Composable
fun SToastHost(state: ToastState, modifier: Modifier = Modifier) {
    val message = state.current
    LaunchedEffect(message?.id) {
        if (message != null) {
            delay(ToastMillis)
            state.dismiss()
        }
    }
    val lift = if (SintoniaTheme.deviceClass == DeviceClass.Tv) SintoniaTheme.spacing.md else SintoniaTheme.heights.bottomBar + SintoniaTheme.spacing.md
    Box(modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = lift), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
            visible = message != null,
            enter = slideInVertically(SintoniaTheme.motion.normal()) { it / 2 } + fadeIn(SintoniaTheme.motion.normal()),
            exit = slideOutVertically(SintoniaTheme.motion.fast()) { it / 2 } + fadeOut(SintoniaTheme.motion.fast()),
        ) {
            val shape = RoundedCornerShape(SintoniaTheme.radii.control)
            Box(
                Modifier
                    .padding(horizontal = SintoniaTheme.spacing.gutter)
                    .background(SintoniaTheme.colors.surface, shape)
                    .border(SintoniaTheme.strokes.hairline, SintoniaTheme.colors.borderStrong, shape)
                    .padding(horizontal = SintoniaTheme.spacing.lg, vertical = SintoniaTheme.spacing.md)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            ) {
                SText(message?.text.orEmpty(), style = SintoniaTheme.typography.body, maxLines = 3)
            }
        }
    }
}
