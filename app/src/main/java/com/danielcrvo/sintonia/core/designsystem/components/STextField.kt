package com.danielcrvo.sintonia.core.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.danielcrvo.sintonia.R
import com.danielcrvo.sintonia.core.designsystem.theme.DeviceClass
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme

private const val PasswordMask = '•'

@Composable
fun STextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    password: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null,
    @DrawableRes leadingIcon: Int? = null,
    error: String? = null,
) {
    val colors = SintoniaTheme.colors
    val radius = SintoniaTheme.radii.control
    val shape = RoundedCornerShape(radius)
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    var revealed by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        targetValue = when {
            error != null -> colors.danger
            focused -> colors.textSecondary
            else -> colors.border
        },
        animationSpec = SintoniaTheme.motion.fast(),
        label = "fieldBorder",
    )
    val halo = SintoniaTheme.strokes.focusOffset
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val textStyle = SintoniaTheme.typography.body.copy(color = colors.text)

    val tv = SintoniaTheme.deviceClass == DeviceClass.Tv
    val edit = rememberTvEdit()
    val fieldModifier = Modifier
        .fillMaxWidth()
        .drawBehind {
            if (focused) {
                val h = halo.toPx()
                drawRoundRect(
                    color = colors.focusSoft,
                    topLeft = Offset(-h, -h),
                    size = Size(size.width + h * 2, size.height + h * 2),
                    cornerRadius = CornerRadius(radius.toPx() + h),
                )
            }
        }
    val chrome: @Composable (@Composable () -> Unit) -> Unit = { content ->
        FieldChrome(
            showPlaceholder = value.isEmpty() && placeholder != null,
            placeholder = placeholder,
            leadingIcon = leadingIcon,
            password = password,
            revealed = revealed,
            onToggleReveal = { revealed = !revealed },
            borderColor = borderColor,
            content = content,
        )
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.snug)) {
        if (label != null) {
            SText(label, style = SintoniaTheme.typography.label, color = colors.textSecondary)
        }
        if (tv && !edit.editing) {
            Box(
                modifier = fieldModifier
                    .focusRequester(edit.idleFocus)
                    .onPreviewKeyEvent { event ->
                        val select = isSelectKey(event)
                        if (select && enabled) edit.editing = true
                        select
                    }.focusable(enabled, interactionSource),
            ) {
                chrome {
                    val shown = if (password && !revealed) PasswordMask.toString().repeat(value.length) else value
                    SText(shown, style = SintoniaTheme.typography.body, maxLines = 1)
                }
            }
        } else {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = singleLine,
                interactionSource = interactionSource,
                textStyle = textStyle,
                cursorBrush = SolidColor(colors.text),
                visualTransformation = if (password && !revealed) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (password) KeyboardType.Password else keyboardType,
                    imeAction = imeAction,
                    autoCorrectEnabled = false,
                ),
                keyboardActions = KeyboardActions(
                    onAny = {
                        if (imeAction == ImeAction.Next) {
                            focusManager.moveFocus(FocusDirection.Next)
                        } else {
                            onImeAction?.invoke()
                            keyboard?.hide()
                            if (tv) edit.finish()
                        }
                    },
                ),
                modifier = if (tv) {
                    fieldModifier
                        .focusRequester(edit.editFocus)
                        .onFocusChanged { edit.onEditFocusChanged(it.isFocused) }
                        .onPreviewKeyEvent { event -> handleEditingKey(event, singleLine, keyboard, focusManager) }
                } else {
                    fieldModifier
                },
                decorationBox = { inner -> chrome(inner) },
            )
        }
        if (error != null) {
            SText(error, style = SintoniaTheme.typography.caption, color = colors.danger)
        }
    }
}

private fun isSelectKey(event: KeyEvent): Boolean =
    event.type == KeyEventType.KeyDown && (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)

private fun handleEditingKey(
    event: KeyEvent,
    singleLine: Boolean,
    keyboard: SoftwareKeyboardController?,
    focusManager: FocusManager,
): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    return when (event.key) {
        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
            keyboard?.show()
            true
        }
        Key.DirectionDown -> singleLine && focusManager.moveFocus(FocusDirection.Down)
        Key.DirectionUp -> singleLine && focusManager.moveFocus(FocusDirection.Up)
        else -> false
    }
}

private class TvEdit {
    var editing by mutableStateOf(false)
    var hadFocus = false
    var restoreFocus = false
    val editFocus = FocusRequester()
    val idleFocus = FocusRequester()

    fun finish() {
        restoreFocus = true
        editing = false
    }

    fun onEditFocusChanged(focused: Boolean) {
        if (focused) {
            hadFocus = true
        } else if (hadFocus) {
            hadFocus = false
            editing = false
        }
    }
}

@Composable
private fun rememberTvEdit(): TvEdit {
    val edit = remember { TvEdit() }
    LaunchedEffect(edit.editing) {
        if (edit.editing) {
            edit.hadFocus = false
            runCatching { edit.editFocus.requestFocus() }
        } else if (edit.restoreFocus) {
            edit.restoreFocus = false
            runCatching { edit.idleFocus.requestFocus() }
        }
    }
    return edit
}

@Composable
private fun FieldChrome(
    showPlaceholder: Boolean,
    placeholder: String?,
    @DrawableRes leadingIcon: Int?,
    password: Boolean,
    revealed: Boolean,
    onToggleReveal: () -> Unit,
    borderColor: Color,
    content: @Composable () -> Unit,
) {
    val colors = SintoniaTheme.colors
    val radius = SintoniaTheme.radii.control
    val shape = RoundedCornerShape(radius)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(SintoniaTheme.heights.field)
            .background(colors.surface, shape)
            .border(SintoniaTheme.strokes.hairline, borderColor, shape)
            .padding(horizontal = SintoniaTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.sm),
    ) {
        if (leadingIcon != null) {
            SIcon(leadingIcon, contentDescription = null, tint = colors.textMuted)
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (showPlaceholder && placeholder != null) {
                SText(placeholder, style = SintoniaTheme.typography.body, color = colors.textMuted, maxLines = 1)
            }
            content()
        }
        if (password) {
            Box(
                modifier = Modifier
                    .height(SintoniaTheme.heights.minTouch)
                    .sintoniaClickable(onClick = onToggleReveal, cornerRadius = radius)
                    .padding(horizontal = SintoniaTheme.spacing.xs),
                contentAlignment = Alignment.Center,
            ) {
                SIcon(
                    icon = if (revealed) SIcons.EyeOff else SIcons.Eye,
                    contentDescription = stringResource(if (revealed) R.string.field_hide_password else R.string.field_show_password),
                    tint = colors.textMuted,
                )
            }
        }
    }
}
