package com.danielcrvo.sintonia.core.designsystem.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme

@Composable
fun SText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = SintoniaTheme.typography.body,
    color: Color = SintoniaTheme.colors.text,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    textAlign: TextAlign? = null,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = if (textAlign != null) style.copy(color = color, textAlign = textAlign) else style.copy(color = color),
        maxLines = maxLines,
        overflow = overflow,
    )
}
