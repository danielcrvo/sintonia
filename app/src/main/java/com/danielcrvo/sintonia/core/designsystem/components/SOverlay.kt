package com.danielcrvo.sintonia.core.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animate
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.danielcrvo.sintonia.core.designsystem.theme.DeviceClass
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme
import kotlinx.coroutines.launch

@Composable
private fun OverlayHost(
    visible: Boolean,
    onDismiss: () -> Unit,
    contentAlignment: Alignment,
    enter: androidx.compose.animation.EnterTransition,
    exit: androidx.compose.animation.ExitTransition,
    content: @Composable () -> Unit,
) {
    val state = remember { MutableTransitionState(false) }
    state.targetState = visible
    if (!state.currentState && !state.targetState) return
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        // The popup recenters above the keyboard so buttons are never hidden.
        Box(Modifier.fillMaxSize().imePadding()) {
            AnimatedVisibility(visibleState = state, enter = fadeIn(SintoniaTheme.motion.normal()), exit = fadeOut(SintoniaTheme.motion.normal())) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(SintoniaTheme.colors.scrim)
                        .focusProperties { canFocus = false }
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
                )
            }
            AnimatedVisibility(
                visibleState = state,
                enter = enter,
                exit = exit,
                modifier = Modifier.align(contentAlignment),
            ) {
                // On TV focus enters the first control of the overlay (the scrim is not focusable).
                Box(Modifier.initialFocusOnTv().focusGroup()) { content() }
            }
        }
    }
}

@Composable
fun SSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    OverlayHost(
        visible = visible,
        onDismiss = onDismiss,
        contentAlignment = Alignment.BottomCenter,
        enter = slideInVertically(SintoniaTheme.motion.normal()) { it } + fadeIn(SintoniaTheme.motion.fast()),
        exit = slideOutVertically(SintoniaTheme.motion.normal()) { it } + fadeOut(SintoniaTheme.motion.fast()),
    ) {
        SheetSurface(onDismiss, modifier, title, scrollable, content)
    }
}

private const val DismissFraction = 0.3f
private const val DismissVelocity = 1500f

@Composable
fun SheetSurface(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = SintoniaTheme.colors
    val shape = RoundedCornerShape(topStart = SintoniaTheme.radii.sheet, topEnd = SintoniaTheme.radii.sheet)
    val motion = SintoniaTheme.motion
    val scope = rememberCoroutineScope()
    var dragY by remember { mutableFloatStateOf(0f) }
    var sheetHeight by remember { mutableFloatStateOf(0f) }
    val settle: (Float) -> Unit = { velocity ->
        if (dragY > sheetHeight * DismissFraction || velocity > DismissVelocity) {
            onDismiss()
        } else {
            scope.launch { animate(dragY, 0f, animationSpec = motion.normal()) { value, _ -> dragY = value } }
        }
    }
    val nested = remember(settle) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0f && dragY > 0f) {
                    val delta = maxOf(available.y, -dragY)
                    dragY += delta
                    return Offset(0f, delta)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0f && source == NestedScrollSource.UserInput) {
                    dragY += available.y
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (dragY <= 0f) return Velocity.Zero
                settle(available.y)
                return available
            }
        }
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { sheetHeight = it.height.toFloat() }
            .graphicsLayer { translationY = dragY }
            .nestedScroll(nested)
            .statusBarsPadding()
            .heightIn(max = 640.dp)
            .clip(shape)
            .background(colors.surface)
            .border(SintoniaTheme.strokes.hairline, colors.border, shape)
            .semantics {
                dismiss {
                    onDismiss()
                    true
                }
            }.navigationBarsPadding()
            .imePadding()
            .padding(horizontal = SintoniaTheme.spacing.gutter)
            .padding(top = SintoniaTheme.spacing.md, bottom = SintoniaTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.md),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta -> dragY = maxOf(0f, dragY + delta) },
                onDragStopped = { velocity -> settle(velocity) },
            ),
            verticalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.md),
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 36.dp, height = 4.dp)
                    .background(colors.borderStrong, RoundedCornerShape(SintoniaTheme.radii.pill)),
            )
            if (title != null) SText(title, style = SintoniaTheme.typography.subheading, maxLines = 2)
        }
        Column(
            modifier = if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier,
            verticalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.sm),
            content = content,
        )
    }
}

@Composable
fun SDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    OverlayHost(
        visible = visible,
        onDismiss = onDismiss,
        contentAlignment = Alignment.Center,
        enter = scaleIn(SintoniaTheme.motion.normal(), initialScale = 0.96f) + fadeIn(SintoniaTheme.motion.normal()),
        exit = scaleOut(SintoniaTheme.motion.fast(), targetScale = 0.96f) + fadeOut(SintoniaTheme.motion.fast()),
    ) {
        DialogSurface(modifier, title, scrollable, content)
    }
}

@Composable
fun DialogSurface(
    modifier: Modifier = Modifier,
    title: String? = null,
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = SintoniaTheme.colors
    val shape = RoundedCornerShape(SintoniaTheme.radii.card)
    val maxWidth = if (SintoniaTheme.deviceClass == DeviceClass.Tv) 560.dp else 420.dp
    Column(
        modifier = modifier
            .padding(SintoniaTheme.spacing.gutter)
            .widthIn(max = maxWidth)
            .fillMaxWidth()
            .heightIn(max = 560.dp)
            .clip(shape)
            .background(colors.surface)
            .border(SintoniaTheme.strokes.hairline, colors.border, shape)
            .padding(SintoniaTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.md),
    ) {
        if (title != null) SText(title, style = SintoniaTheme.typography.subheading, maxLines = 2)
        Column(
            modifier = if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier,
            verticalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.sm),
            content = content,
        )
    }
}

@Composable
fun SOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (SintoniaTheme.deviceClass == DeviceClass.Tv) {
        SDialog(visible, onDismiss, modifier, title, scrollable, content)
    } else {
        SSheet(visible, onDismiss, modifier, title, scrollable, content)
    }
}

@Composable
fun SOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingText: String? = null,
    locked: Boolean = false,
) {
    val colors = SintoniaTheme.colors
    val radius = SintoniaTheme.radii.control
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SintoniaTheme.heights.settingsRow)
            .sintoniaClickable(onClick = onClick, cornerRadius = radius)
            .clip(RoundedCornerShape(radius))
            .background(if (selected) colors.surfaceAlt else androidx.compose.ui.graphics.Color.Transparent)
            .padding(horizontal = SintoniaTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.md),
    ) {
        SText(
            label,
            style = if (selected) SintoniaTheme.typography.bodyStrong else SintoniaTheme.typography.body,
            modifier = Modifier.weight(1f),
            maxLines = 2,
        )
        if (trailingText != null) SText(trailingText, style = SintoniaTheme.typography.mono, color = colors.textMuted)
        if (locked) {
            SIcon(SIcons.Lock, contentDescription = null, tint = colors.textMuted)
        } else if (selected) {
            SIcon(SIcons.Check, contentDescription = null, tint = colors.text)
        }
    }
}
