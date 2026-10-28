package com.danielcrvo.sintonia.core.parental.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.danielcrvo.sintonia.R
import com.danielcrvo.sintonia.core.designsystem.components.SOverlay
import com.danielcrvo.sintonia.core.designsystem.components.SPinBoxes
import com.danielcrvo.sintonia.core.designsystem.components.SPinPad
import com.danielcrvo.sintonia.core.designsystem.components.SText
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme
import com.danielcrvo.sintonia.core.parental.PinError
import com.danielcrvo.sintonia.core.parental.PinPromptState
import com.danielcrvo.sintonia.core.parental.PinPurpose

@Composable
fun PinPromptSheet(
    state: PinPromptState?,
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
    onDismiss: () -> Unit,
) {
    // Keeps the last state while the sheet animates out (state becomes null on close).
    var last by remember { mutableStateOf<PinPromptState?>(null) }
    if (state != null) last = state
    SOverlay(visible = state != null, onDismiss = onDismiss) {
        last?.let { PinPromptContent(it, onDigit, onBackspace) }
    }
}

@Composable
fun PinPromptContent(state: PinPromptState, onDigit: (Int) -> Unit, onBackspace: () -> Unit) {
    val colors = SintoniaTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth().onKeyEvent { event -> handlePinKey(event, onDigit, onBackspace) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.lg),
    ) {
        SText(stringResource(titleOf(state)), style = SintoniaTheme.typography.subheading, textAlign = TextAlign.Center)
        SText(
            stringResource(messageOf(state)),
            style = SintoniaTheme.typography.caption,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        SPinBoxes(filled = state.digits.length, error = state.error != null, errorTick = state.errorTick)
        SText(
            text = when (state.error) {
                PinError.Wrong -> stringResource(R.string.pin_error_wrong)
                PinError.Mismatch -> stringResource(R.string.pin_error_mismatch)
                PinError.Locked -> stringResource(R.string.pin_error_locked, state.lockedSeconds)
                null -> ""
            },
            style = SintoniaTheme.typography.caption,
            color = colors.danger,
            textAlign = TextAlign.Center,
            modifier = Modifier.heightIn(min = SintoniaTheme.sizes.errorLineMin),
        )
        SPinPad(
            onDigit = onDigit,
            onBackspace = onBackspace,
            modifier = Modifier.widthIn(max = SintoniaTheme.sizes.padMaxWidth).padding(bottom = SintoniaTheme.spacing.sm),
        )
    }
}

private fun handlePinKey(event: KeyEvent, onDigit: (Int) -> Unit, onBackspace: () -> Unit): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    val digit = when (event.key) {
        Key.Zero, Key.NumPad0 -> 0
        Key.One, Key.NumPad1 -> 1
        Key.Two, Key.NumPad2 -> 2
        Key.Three, Key.NumPad3 -> 3
        Key.Four, Key.NumPad4 -> 4
        Key.Five, Key.NumPad5 -> 5
        Key.Six, Key.NumPad6 -> 6
        Key.Seven, Key.NumPad7 -> 7
        Key.Eight, Key.NumPad8 -> 8
        Key.Nine, Key.NumPad9 -> 9
        Key.Backspace, Key.Delete -> {
            onBackspace()
            return true
        }
        else -> return false
    }
    onDigit(digit)
    return true
}

private fun titleOf(state: PinPromptState): Int = when (state.purpose) {
    PinPurpose.Unlock -> R.string.pin_title_unlock
    PinPurpose.EnterSettings -> R.string.pin_title_settings
    PinPurpose.TurnOff -> R.string.pin_title_turn_off
    PinPurpose.ManageLists -> R.string.pin_title_lists
    PinPurpose.CurrentForChange -> R.string.pin_title_current
    PinPurpose.Create -> if (state.confirming) R.string.pin_title_confirm else R.string.pin_title_create
    PinPurpose.CreateNew -> if (state.confirming) R.string.pin_title_confirm else R.string.pin_title_new
}

private fun messageOf(state: PinPromptState): Int = when (state.purpose) {
    PinPurpose.Unlock -> R.string.pin_message_unlock
    PinPurpose.EnterSettings -> R.string.pin_message_settings
    PinPurpose.TurnOff -> R.string.pin_message_turn_off
    PinPurpose.ManageLists -> R.string.pin_message_lists
    PinPurpose.CurrentForChange -> R.string.pin_message_current
    PinPurpose.Create, PinPurpose.CreateNew -> if (state.confirming) R.string.pin_message_confirm else R.string.pin_message_create
}
