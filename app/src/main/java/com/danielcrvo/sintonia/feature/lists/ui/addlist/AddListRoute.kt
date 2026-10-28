package com.danielcrvo.sintonia.feature.lists.ui.addlist

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.danielcrvo.sintonia.R
import com.danielcrvo.sintonia.core.designsystem.components.LocalToast

@Composable
fun AddListRoute(
    onBack: () -> Unit,
    onStartImport: (listId: Long) -> Unit,
    canGoBack: Boolean,
    modifier: Modifier = Modifier,
    viewModel: AddListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toast = LocalToast.current
    val readFailed = stringResource(R.string.file_read_error)
    val noPicker = stringResource(R.string.addlist_note_file_tv)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.onFilePicked(uri)
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is AddListEffect.StartImport -> onStartImport(effect.listId)
                AddListEffect.FileReadFailed -> toast.show(readFailed)
            }
        }
    }

    AddListScreen(
        state = state,
        onBack = onBack,
        onTabChange = viewModel::onTabChange,
        onNameChange = viewModel::onNameChange,
        onServerChange = viewModel::onServerChange,
        onUsernameChange = viewModel::onUsernameChange,
        onPasswordChange = viewModel::onPasswordChange,
        onUrlChange = viewModel::onUrlChange,
        onPickFile = {
            try {
                picker.launch(arrayOf("*/*"))
            } catch (_: ActivityNotFoundException) {
                toast.show(noPicker)
            }
        },
        onSubmit = viewModel::onSubmit,
        canGoBack = canGoBack,
        modifier = modifier,
    )
}
