package com.danielcrvo.sintonia.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.danielcrvo.sintonia.R

val Geist = FontFamily(
    Font(R.font.geist_regular, FontWeight.Normal),
    Font(R.font.geist_medium, FontWeight.Medium),
    Font(R.font.geist_semibold, FontWeight.SemiBold),
)

val GeistMono = FontFamily(
    Font(R.font.geist_mono_regular, FontWeight.Normal),
    Font(R.font.geist_mono_medium, FontWeight.Medium),
)

@Immutable
data class SintoniaTypography(
    val display: TextStyle,
    val title: TextStyle,
    val heading: TextStyle,
    val subheading: TextStyle,
    val bodyLarge: TextStyle,
    val body: TextStyle,
    val bodyStrong: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
    val mono: TextStyle,
    val monoSmall: TextStyle,
    val monoLabel: TextStyle,
)

private val TitleTracking = (-0.04).em

private fun sans(size: TextUnit, weight: FontWeight = FontWeight.Normal, tracking: TextUnit = 0.sp) =
    TextStyle(fontFamily = Geist, fontSize = size, fontWeight = weight, letterSpacing = tracking)

private fun mono(size: TextUnit, weight: FontWeight = FontWeight.Normal, tracking: TextUnit = 0.sp) =
    TextStyle(fontFamily = GeistMono, fontSize = size, fontWeight = weight, letterSpacing = tracking)

val PhoneTypography = SintoniaTypography(
    display = sans(32.sp, FontWeight.SemiBold, TitleTracking),
    title = sans(28.sp, FontWeight.SemiBold, TitleTracking),
    heading = sans(20.sp, FontWeight.SemiBold, TitleTracking),
    subheading = sans(16.sp, FontWeight.SemiBold, TitleTracking),
    bodyLarge = sans(15.sp, FontWeight.Medium),
    body = sans(14.sp),
    bodyStrong = sans(14.sp, FontWeight.Medium),
    label = sans(13.sp, FontWeight.Medium),
    caption = sans(13.sp),
    mono = mono(12.sp),
    monoSmall = mono(11.sp),
    monoLabel = mono(11.sp, FontWeight.Medium, 0.04.em),
)

val TvTypography = SintoniaTypography(
    display = sans(48.sp, FontWeight.SemiBold, TitleTracking),
    title = sans(36.sp, FontWeight.SemiBold, TitleTracking),
    heading = sans(26.sp, FontWeight.SemiBold, TitleTracking),
    subheading = sans(22.sp, FontWeight.SemiBold, TitleTracking),
    bodyLarge = sans(18.sp, FontWeight.Medium),
    body = sans(18.sp),
    bodyStrong = sans(18.sp, FontWeight.Medium),
    label = sans(16.sp, FontWeight.Medium),
    caption = sans(16.sp),
    mono = mono(16.sp),
    monoSmall = mono(16.sp),
    monoLabel = mono(16.sp, FontWeight.Medium, 0.04.em),
)
