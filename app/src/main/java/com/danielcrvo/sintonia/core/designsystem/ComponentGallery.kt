package com.danielcrvo.sintonia.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.danielcrvo.sintonia.core.designsystem.components.BadgeStyle
import com.danielcrvo.sintonia.core.designsystem.components.ButtonSize
import com.danielcrvo.sintonia.core.designsystem.components.ButtonVariant
import com.danielcrvo.sintonia.core.designsystem.components.ChannelRowData
import com.danielcrvo.sintonia.core.designsystem.components.SAccordion
import com.danielcrvo.sintonia.core.designsystem.components.SBadge
import com.danielcrvo.sintonia.core.designsystem.components.SButton
import com.danielcrvo.sintonia.core.designsystem.components.SChannelRow
import com.danielcrvo.sintonia.core.designsystem.components.SCheckbox
import com.danielcrvo.sintonia.core.designsystem.components.SGroup
import com.danielcrvo.sintonia.core.designsystem.components.SHeader
import com.danielcrvo.sintonia.core.designsystem.components.SIconButton
import com.danielcrvo.sintonia.core.designsystem.components.SIcons
import com.danielcrvo.sintonia.core.designsystem.components.SLiveBadge
import com.danielcrvo.sintonia.core.designsystem.components.SLogo
import com.danielcrvo.sintonia.core.designsystem.components.SPinBoxes
import com.danielcrvo.sintonia.core.designsystem.components.SPoster
import com.danielcrvo.sintonia.core.designsystem.components.SProgressBar
import com.danielcrvo.sintonia.core.designsystem.components.SRadio
import com.danielcrvo.sintonia.core.designsystem.components.SSectionLabel
import com.danielcrvo.sintonia.core.designsystem.components.SSettingsRow
import com.danielcrvo.sintonia.core.designsystem.components.SSpinner
import com.danielcrvo.sintonia.core.designsystem.components.SSwitch
import com.danielcrvo.sintonia.core.designsystem.components.STabs
import com.danielcrvo.sintonia.core.designsystem.components.SText
import com.danielcrvo.sintonia.core.designsystem.components.STextField
import com.danielcrvo.sintonia.core.designsystem.components.SThemeSwitch
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme
import com.danielcrvo.sintonia.core.designsystem.theme.ThemeMode

@Composable
fun ComponentGallery(modifier: Modifier = Modifier) {
    var tab by remember { mutableIntStateOf(0) }
    var checked by remember { mutableStateOf(true) }
    var mode by remember { mutableStateOf(ThemeMode.Dark) }
    var text by remember { mutableStateOf("") }
    var open by remember { mutableStateOf(true) }
    Column(
        modifier = modifier
            .background(SintoniaTheme.colors.background)
            .padding(vertical = SintoniaTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.lg),
    ) {
        Row(
            Modifier.padding(horizontal = SintoniaTheme.spacing.gutter),
            horizontalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.md),
        ) {
            SLogo()
            SText("Sintonia", style = SintoniaTheme.typography.title)
        }
        SHeader(title = "Settings", onBack = {}) {
            SIconButton(SIcons.Search, "Search", {})
        }
        STabs(listOf("All", "Favorites", "Sports", "News"), tab, { tab = it })
        Column(
            Modifier.padding(horizontal = SintoniaTheme.spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.md),
        ) {
            SButton("Connect and load", {}, Modifier.fillMaxWidth(), size = ButtonSize.Footer)
            SButton("Switch list", {}, Modifier.fillMaxWidth(), variant = ButtonVariant.Secondary)
            Row(
                horizontalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.sm),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                SLiveBadge("LIVE")
                SBadge("4K")
                SBadge("New", style = BadgeStyle.OnImage)
                SSwitch(checked, { checked = it })
                SCheckbox(checked, { checked = it })
                SRadio(checked)
            }
            SThemeSwitch(mode, { mode = it })
            STextField(text, { text = it }, Modifier.fillMaxWidth(), label = "Server", placeholder = "http://example.com:8080")
            STextField("segredo", {}, Modifier.fillMaxWidth(), label = "Password", password = true)
            SProgressBar(progress = { 0.4f })
            Row(horizontalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.md)) {
                SPoster("Lost Horizon", null, {}, Modifier.width(100.dp), meta = "2026 · Adventure", progress = 0.5f, badge = "New")
                SPoster("Star Street", null, {}, Modifier.width(100.dp), meta = "3 seasons")
                SSpinner()
            }
            SPinBoxes(filled = 2, error = false, errorTick = 0)
        }
        SChannelRow(
            data = ChannelRowData("12", "Horizon Network", null, "Evening News", 0.6f),
            favorite = true,
            playing = true,
            onClick = {},
            onToggleFavorite = {},
        )
        SChannelRow(
            data = ChannelRowData("13", "Aurora Channel", null, "Live Music", 0.2f),
            favorite = false,
            playing = false,
            onClick = {},
            onToggleFavorite = {},
        )
        Column(Modifier.padding(horizontal = SintoniaTheme.spacing.gutter)) {
            SSectionLabel("Appearance")
            SGroup {
                SSettingsRow("Theme", icon = SIcons.Sun, trailing = { SThemeSwitch(mode, { mode = it }) })
                SSettingsRow("App language", icon = SIcons.Globe, value = "Automatic", onClick = {})
                SSettingsRow("Remove list", icon = SIcons.Trash, destructive = true, onClick = {}, showDivider = false)
            }
            SAccordion("Movies", open, { open = !open }, summary = "2 blocked") {
                SSettingsRow("Action", trailing = { SCheckbox(true, {}) })
                SSettingsRow("Drama", trailing = { SCheckbox(false, {}) }, showDivider = false)
            }
        }
    }
}
