package com.danielcrvo.sintonia.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.danielcrvo.sintonia.R
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme

@Composable
fun SHeaderActions(
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.md),
    ) {
        SIconButton(SIcons.Search, stringResource(R.string.home_search), onSearch)
        SIconButton(SIcons.Settings, stringResource(R.string.home_settings), onSettings)
    }
}
