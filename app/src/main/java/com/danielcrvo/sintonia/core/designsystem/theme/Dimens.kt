package com.danielcrvo.sintonia.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class Spacing(
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 20.dp,
    val xxl: Dp = 24.dp,
    val xxxl: Dp = 32.dp,
    val snug: Dp = 6.dp,
    val cozy: Dp = 10.dp,
    val gutter: Dp = 20.dp,
    val safeVertical: Dp = 0.dp,
)

@Immutable
data class Radii(
    val thumb: Dp = 6.dp,
    val control: Dp = 8.dp,
    val menuItem: Dp = 10.dp,
    val card: Dp = 12.dp,
    val sheet: Dp = 16.dp,
    val pill: Dp = 999.dp,
)

@Immutable
data class Heights(
    val button: Dp = 44.dp,
    val buttonCompact: Dp = 32.dp,
    val buttonHero: Dp = 40.dp,
    val buttonFooter: Dp = 48.dp,
    val field: Dp = 44.dp,
    val header: Dp = 56.dp,
    val tabs: Dp = 44.dp,
    val channelRow: Dp = 72.dp,
    val settingsRow: Dp = 52.dp,
    val bottomBar: Dp = 64.dp,
    val sidebar: Dp = 88.dp,
    val minTouch: Dp = 44.dp,
)

@Immutable
data class Strokes(
    val hairline: Dp = 1.dp,
    val focus: Dp = 2.dp,
    val focusOffset: Dp = 3.dp,
)

@Immutable
data class Sizes(
    val iconInline: Dp = 16.dp,
    val iconCheck: Dp = 14.dp,
    val iconSmall: Dp = 18.dp,
    val iconDefault: Dp = 20.dp,
    val iconMedium: Dp = 24.dp,
    val iconLarge: Dp = 28.dp,
    val iconHero: Dp = 32.dp,
    val iconMenu: Dp = 26.dp,
    val menuItem: Dp = 56.dp,
    val spinnerSmall: Dp = 16.dp,
    val spinnerInline: Dp = 20.dp,
    val spinnerList: Dp = 24.dp,
    val spinnerHero: Dp = 32.dp,
    val logoSmall: Dp = 28.dp,
    val logoMedium: Dp = 32.dp,
    val logoLarge: Dp = 40.dp,
    val logoBadge: Dp = 56.dp,
    val avatarSmall: Dp = 36.dp,
    val avatar: Dp = 40.dp,
    val avatarLarge: Dp = 64.dp,
    val thumbTallHeight: Dp = 56.dp,
    val contentMaxWidth: Dp = 480.dp,
    val dialogMaxWidth: Dp = 420.dp,
    val dialogMaxWidthTv: Dp = 560.dp,
    val dialogMaxHeight: Dp = 560.dp,
    val sheetMaxHeight: Dp = 640.dp,
    val sheetHandleWidth: Dp = 36.dp,
    val sheetHandleHeight: Dp = 4.dp,
    val splitMaxWidth: Dp = 880.dp,
    val padMaxWidth: Dp = 320.dp,
    val sidePanelWidth: Dp = 320.dp,
    val nextEpisodeCardWidth: Dp = 280.dp,
    val featuredCardWidth: Dp = 220.dp,
    val favoriteItemWidth: Dp = 72.dp,
    val episodeThumbWidth: Dp = 120.dp,
    val guideNumberWidth: Dp = 36.dp,
    val programTimeWidth: Dp = 64.dp,
    val sheetListMaxHeight: Dp = 280.dp,
    val programsListMaxHeight: Dp = 320.dp,
    val playButton: Dp = 64.dp,
    val stepSlot: Dp = 20.dp,
    val stepMarker: Dp = 6.dp,
    val progressThin: Dp = 3.dp,
    val progressRegular: Dp = 4.dp,
    val progressHairline: Dp = 2.dp,
    val switchWidth: Dp = 40.dp,
    val switchHeight: Dp = 24.dp,
    val switchThumb: Dp = 18.dp,
    val checkbox: Dp = 20.dp,
    val radioDot: Dp = 10.dp,
    val themeSegmentWidth: Dp = 36.dp,
    val themeSegmentHeight: Dp = 28.dp,
    val seekBarHeight: Dp = 32.dp,
    val seekTrack: Dp = 4.dp,
    val seekThumbRadius: Dp = 7.dp,
    val pinBoxWidth: Dp = 52.dp,
    val pinKey: Dp = 56.dp,
    val pinDot: Dp = 10.dp,
    val channelNumberWidth: Dp = 28.dp,
    val ringGap: Dp = 3.dp,
    val dotSmall: Dp = 6.dp,
    val dotLarge: Dp = 8.dp,
    val errorLineMin: Dp = 18.dp,
    val tightGap: Dp = 2.dp,
)

val TvHeights = Heights(
    button = 52.dp,
    buttonCompact = 40.dp,
    buttonHero = 56.dp,
    buttonFooter = 56.dp,
    field = 56.dp,
    tabs = 48.dp,
    settingsRow = 60.dp,
)

val SpacingTvGutter = 40.dp

// About 5% of a 540dp TV viewport, so content stays clear of the overscan edge.
val SpacingTvSafeVertical = 27.dp
