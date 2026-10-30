package com.danielcrvo.sintonia.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.danielcrvo.sintonia.core.designsystem.theme.DeviceClass
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme

@Composable
fun SSplitPane(
    modifier: Modifier = Modifier,
    start: @Composable ColumnScope.() -> Unit,
    end: @Composable ColumnScope.() -> Unit,
) {
    val spacing = SintoniaTheme.spacing
    val gap = spacing.xl
    if (SintoniaTheme.deviceClass == DeviceClass.Tv) {
        Row(
            modifier = modifier.widthIn(max = SintoniaTheme.sizes.splitMaxWidth).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.xxxl),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), Arrangement.spacedBy(gap), Alignment.CenterHorizontally, start)
            Column(Modifier.weight(1f), Arrangement.spacedBy(gap), Alignment.CenterHorizontally, end)
        }
    } else {
        Column(
            modifier = modifier.widthIn(max = SintoniaTheme.sizes.contentMaxWidth).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(gap),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            start()
            end()
        }
    }
}
