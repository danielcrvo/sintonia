package com.danielcrvo.sintonia.feature.categories.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CategoriesRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CategoriesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CategoriesScreen(
        state = state,
        onBack = onBack,
        onToggleSection = viewModel::onToggleSection,
        onToggleHidden = viewModel::onToggleHidden,
        onRenameStart = viewModel::onRenameStart,
        onRenameTextChange = viewModel::onRenameTextChange,
        onRenameSave = viewModel::onRenameSave,
        onRenameReset = viewModel::onRenameReset,
        onRenameDismiss = viewModel::onRenameDismiss,
        modifier = modifier,
    )
}
