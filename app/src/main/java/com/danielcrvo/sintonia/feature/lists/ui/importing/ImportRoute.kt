package com.danielcrvo.sintonia.feature.lists.ui.importing

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ImportRoute(
    listId: Long,
    isNewList: Boolean,
    onFinished: () -> Unit,
    onCancelled: () -> Unit,
    onEditDetails: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImportViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val currentOnFinished by rememberUpdatedState(onFinished)
    val currentOnCancelled by rememberUpdatedState(onCancelled)
    val currentOnEditDetails by rememberUpdatedState(onEditDetails)

    LaunchedEffect(listId) { viewModel.start(listId, discardOnExit = isNewList) }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ImportEffect.Finished -> currentOnFinished()
                ImportEffect.Cancelled -> currentOnCancelled()
                ImportEffect.EditDetails -> currentOnEditDetails()
            }
        }
    }
    BackHandler(onBack = viewModel::cancel)

    ImportScreen(
        state = state,
        onCancel = viewModel::cancel,
        onRetry = viewModel::retry,
        onEditDetails = viewModel::editDetails,
        modifier = modifier,
    )
}
