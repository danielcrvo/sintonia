package com.danielcrvo.sintonia.feature.settings.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.danielcrvo.sintonia.core.parental.ui.PinPromptSheet

@Composable
fun ListsRoute(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ListsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ListsScreen(
        state = state,
        onBack = onBack,
        onSelect = viewModel::onSelect,
        onRemoveRequest = viewModel::onRemoveRequest,
        onRemoveCancel = viewModel::onRemoveCancel,
        onRemoveConfirm = viewModel::onRemoveConfirm,
        onAdd = { viewModel.onAddRequest(onAdd) },
        modifier = modifier,
    )
    val pin by viewModel.pinState.collectAsStateWithLifecycle()
    PinPromptSheet(pin, viewModel::onPinDigit, viewModel::onPinBackspace, viewModel::onPinDismiss)
}
