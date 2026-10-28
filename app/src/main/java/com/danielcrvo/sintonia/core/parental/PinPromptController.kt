package com.danielcrvo.sintonia.core.parental

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PinPurpose { Unlock, EnterSettings, TurnOff, CurrentForChange, Create, CreateNew, ManageLists }

enum class PinError { Wrong, Mismatch, Locked }

@Immutable
data class PinPromptState(
    val purpose: PinPurpose,
    val confirming: Boolean = false,
    val digits: String = "",
    val error: PinError? = null,
    /** Changes on every error so the screen repeats the shake. */
    val errorTick: Int = 0,
    val lockedSeconds: Int = 0,
    /** Keys are ignored until the answer arrives, avoiding concurrent checks. */
    val checking: Boolean = false,
) {
    val creating: Boolean get() = purpose == PinPurpose.Create || purpose == PinPurpose.CreateNew
}

class PinPromptController(
    private val pins: PinManager,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow<PinPromptState?>(null)
    val state: StateFlow<PinPromptState?> = _state.asStateFlow()

    private var onSuccess: () -> Unit = {}
    private var firstEntry: String? = null

    fun verify(purpose: PinPurpose, onSuccess: () -> Unit) {
        this.onSuccess = onSuccess
        firstEntry = null
        _state.value = PinPromptState(purpose)
    }

    fun create(purpose: PinPurpose = PinPurpose.Create, onCreated: () -> Unit) {
        this.onSuccess = onCreated
        firstEntry = null
        _state.value = PinPromptState(purpose)
    }

    fun onDigit(digit: Int) {
        val current = _state.value ?: return
        if (current.checking || current.digits.length >= PinManager.PinLength) return
        val digits = current.digits + digit
        _state.value = current.copy(digits = digits, error = null)
        if (digits.length == PinManager.PinLength) submit(digits)
    }

    fun onBackspace() {
        _state.update { if (it == null || it.checking) it else it.copy(digits = it.digits.dropLast(1), error = null) }
    }

    fun dismiss() {
        _state.value = null
        firstEntry = null
    }

    private fun submit(code: String) {
        val current = _state.value ?: return
        if (current.creating) submitCreate(current, code) else submitVerify(current, code)
    }

    private fun submitCreate(current: PinPromptState, code: String) {
        val first = firstEntry
        if (first == null) {
            firstEntry = code
            _state.value = current.copy(confirming = true, digits = "")
        } else if (first == code) {
            pins.create(code)
            val done = onSuccess
            dismiss()
            done()
        } else {
            firstEntry = null
            _state.value = current.copy(confirming = false, digits = "", error = PinError.Mismatch, errorTick = current.errorTick + 1)
        }
    }

    private fun submitVerify(current: PinPromptState, code: String) {
        _state.value = current.copy(checking = true)
        scope.launch {
            when (val result = pins.verify(code)) {
                PinCheck.Correct -> {
                    val done = onSuccess
                    dismiss()
                    done()
                }
                PinCheck.Wrong -> _state.value = current.copy(digits = "", error = PinError.Wrong, errorTick = current.errorTick + 1, checking = false)
                is PinCheck.Locked -> _state.value = current.copy(
                    digits = "",
                    error = PinError.Locked,
                    errorTick = current.errorTick + 1,
                    checking = false,
                    lockedSeconds = ((result.remainingMillis + 999) / 1000).toInt(),
                )
            }
        }
    }
}
