package com.danielcrvo.sintonia.core.designsystem.theme

import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

enum class ThemeMode { Auto, Light, Dark }

fun ThemeMode.resolveDark(systemDark: Boolean): Boolean = when (this) {
    ThemeMode.Auto -> systemDark
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}

val LocalColors = staticCompositionLocalOf { DarkColors }
val LocalSpacing = staticCompositionLocalOf { Spacing() }
val LocalRadii = staticCompositionLocalOf { Radii() }
val LocalHeights = staticCompositionLocalOf { Heights() }
val LocalSizes = staticCompositionLocalOf { Sizes() }
val LocalStrokes = staticCompositionLocalOf { Strokes() }
val LocalTypography = staticCompositionLocalOf { PhoneTypography }
val LocalMotion = compositionLocalOf { Motion(reduced = false) }
val LocalDeviceClass = staticCompositionLocalOf { DeviceClass.Phone }

object SintoniaTheme {
    val colors: SintoniaColors
        @Composable @ReadOnlyComposable
        get() = LocalColors.current
    val spacing: Spacing
        @Composable @ReadOnlyComposable
        get() = LocalSpacing.current
    val radii: Radii
        @Composable @ReadOnlyComposable
        get() = LocalRadii.current
    val heights: Heights
        @Composable @ReadOnlyComposable
        get() = LocalHeights.current
    val sizes: Sizes
        @Composable @ReadOnlyComposable
        get() = LocalSizes.current
    val strokes: Strokes
        @Composable @ReadOnlyComposable
        get() = LocalStrokes.current
    val typography: SintoniaTypography
        @Composable @ReadOnlyComposable
        get() = LocalTypography.current
    val motion: Motion
        @Composable @ReadOnlyComposable
        get() = LocalMotion.current
    val deviceClass: DeviceClass
        @Composable @ReadOnlyComposable
        get() = LocalDeviceClass.current
}

@Composable
fun SintoniaTheme(
    mode: ThemeMode = ThemeMode.Auto,
    deviceClass: DeviceClass = DeviceClass.Phone,
    content: @Composable () -> Unit,
) {
    val dark = mode.resolveDark(isSystemInDarkTheme())
    val context = LocalContext.current
    val reducedMotion = remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val tv = deviceClass == DeviceClass.Tv
    CompositionLocalProvider(
        LocalColors provides if (dark) DarkColors else LightColors,
        LocalSpacing provides if (tv) TvSpacing else Spacing(),
        LocalHeights provides if (tv) TvHeights else Heights(),
        LocalTypography provides if (tv) TvTypography else PhoneTypography,
        LocalMotion provides remember(reducedMotion) { Motion(reducedMotion) },
        LocalDeviceClass provides deviceClass,
        content = content,
    )
}
