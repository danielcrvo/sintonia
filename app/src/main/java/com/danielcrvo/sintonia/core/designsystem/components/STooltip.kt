package com.danielcrvo.sintonia.core.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme
import kotlinx.coroutines.delay

@Stable
class STooltipState {
    internal var visible by mutableStateOf(false)
        private set
    private var token by mutableIntStateOf(0)

    suspend fun show() {
        active?.takeIf { it !== this }?.dismiss()
        active = this
        val mine = ++token
        visible = true
        delay(VisibleMillis)
        if (mine == token) dismiss()
    }

    fun dismiss() {
        token++
        visible = false
        if (active === this) active = null
    }

    private companion object {
        const val VisibleMillis = 2_000L
        var active: STooltipState? = null
    }
}

@Composable
fun rememberSTooltipState(): STooltipState = remember { STooltipState() }

@Composable
fun STooltipBox(
    text: String,
    state: STooltipState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val gap = SintoniaTheme.spacing.lg
    val margin = SintoniaTheme.spacing.sm
    val provider = remember(density, gap, margin) {
        BelowAnchorPositionProvider(with(density) { gap.roundToPx() }, with(density) { margin.roundToPx() })
    }
    val colors = SintoniaTheme.colors
    val alpha = remember { Animatable(0f) }
    val motion = SintoniaTheme.motion
    LaunchedEffect(state.visible) { alpha.animateTo(if (state.visible) 1f else 0f, motion.fast()) }

    Box(modifier) {
        content()
        if (state.visible || alpha.value > 0f) {
            Popup(popupPositionProvider = provider, properties = PopupProperties(focusable = false, clippingEnabled = false)) {
                Box(
                    Modifier
                        .graphicsLayer { this.alpha = alpha.value }
                        .background(colors.text, RoundedCornerShape(SintoniaTheme.radii.thumb))
                        .padding(horizontal = SintoniaTheme.spacing.md, vertical = SintoniaTheme.spacing.sm),
                ) {
                    SText(text, style = SintoniaTheme.typography.caption, color = colors.background, maxLines = 1)
                }
            }
        }
    }
}

private class BelowAnchorPositionProvider(private val gap: Int, private val margin: Int) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val x = (anchorBounds.center.x - popupContentSize.width / 2)
            .coerceIn(margin, (windowSize.width - popupContentSize.width - margin).coerceAtLeast(margin))
        val below = anchorBounds.bottom + gap
        val y = if (below + popupContentSize.height + margin <= windowSize.height) below else anchorBounds.top - gap - popupContentSize.height
        return IntOffset(x, y)
    }
}
