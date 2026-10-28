package com.danielcrvo.sintonia.core.designsystem.components

import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme

@Suppress("ModifierFactoryExtensionFunction") // animateItem only exists in item scopes, not on Modifier
@Composable
fun LazyItemScope.sintoniaItemMotion(): Modifier {
    val motion = SintoniaTheme.motion
    return Modifier.animateItem(
        fadeInSpec = motion.normal(),
        placementSpec = motion.normal(),
        fadeOutSpec = motion.fast(),
    )
}

@Suppress("ModifierFactoryExtensionFunction") // same reason as above, for grids
@Composable
fun LazyGridItemScope.sintoniaItemMotion(): Modifier {
    val motion = SintoniaTheme.motion
    return Modifier.animateItem(
        fadeInSpec = motion.normal(),
        placementSpec = motion.normal(),
        fadeOutSpec = motion.fast(),
    )
}
