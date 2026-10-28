package com.danielcrvo.sintonia.feature.parental.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.danielcrvo.sintonia.core.parental.ui.PinPromptSheet

@Composable
fun ParentalRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ParentalViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pin by viewModel.pinState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) { viewModel.onOpen() }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ParentalEffect.Leave -> onBack()
            }
        }
    }

    ParentalScreen(
        state = state,
        onBack = onBack,
        onToggleEnabled = viewModel::onToggleEnabled,
        onChangePin = viewModel::onChangePin,
        onToggleSection = viewModel::onToggleSection,
        onToggleCategory = viewModel::onToggleCategory,
        modifier = modifier,
    )
    PinPromptSheet(pin, viewModel::onPinDigit, viewModel::onPinBackspace, viewModel::onPinDismiss)
}
