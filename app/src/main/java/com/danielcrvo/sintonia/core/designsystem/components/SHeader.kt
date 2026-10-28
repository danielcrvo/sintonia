package com.danielcrvo.sintonia.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.danielcrvo.sintonia.R
import com.danielcrvo.sintonia.core.designsystem.theme.DeviceClass
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme

@Composable
fun SHeader(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val tv = SintoniaTheme.deviceClass == DeviceClass.Tv
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(SintoniaTheme.heights.header)
            .padding(horizontal = SintoniaTheme.spacing.gutter),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.sm),
    ) {
        if (onBack != null && !tv) {
            SIconButton(SIcons.ArrowBack, stringResource(R.string.action_back), onBack)
        }
        SText(title, style = SintoniaTheme.typography.subheading, modifier = Modifier.weight(1f), maxLines = 1)
        actions()
    }
}
