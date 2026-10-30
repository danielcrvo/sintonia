package com.danielcrvo.sintonia.core.designsystem.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme

@Composable
fun Modifier.tvSafeArea(): Modifier = padding(vertical = SintoniaTheme.spacing.safeVertical)
