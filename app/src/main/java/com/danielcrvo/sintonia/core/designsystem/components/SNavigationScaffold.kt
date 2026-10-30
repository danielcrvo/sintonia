package com.danielcrvo.sintonia.core.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import com.danielcrvo.sintonia.core.designsystem.theme.DeviceClass
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme

@Immutable
data class NavItem(
    @DrawableRes val icon: Int,
    val label: String,
)

@Composable
fun SNavigationScaffold(
    items: List<NavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (SintoniaTheme.deviceClass == DeviceClass.Tv) {
        Row(modifier.fillMaxSize().background(SintoniaTheme.colors.background)) {
            SideMenu(items, selectedIndex, onSelect)
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .focusRestorer()
                    .focusGroup(),
            ) { content() }
        }
    } else {
        Column(modifier.fillMaxSize().background(SintoniaTheme.colors.background)) {
            Box(Modifier.weight(1f).fillMaxWidth()) { content() }
            BottomBar(items, selectedIndex, onSelect)
        }
    }
}

@Composable
private fun BottomBar(items: List<NavItem>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    val colors = SintoniaTheme.colors
    val hairline = SintoniaTheme.strokes.hairline
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background)
            .drawBehind {
                drawRect(colors.border, Offset.Zero, Size(size.width, hairline.toPx()))
            }.navigationBarsPadding()
            .height(SintoniaTheme.heights.bottomBar),
    ) {
        items.forEachIndexed { index, item ->
            val active = index == selectedIndex
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .sintoniaClickable(onClick = { onSelect(index) }, cornerRadius = SintoniaTheme.radii.control, role = Role.Tab),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.xs, Alignment.CenterVertically),
            ) {
                SIcon(item.icon, contentDescription = null, tint = if (active) colors.text else colors.textMuted, size = SintoniaTheme.sizes.iconMedium)
                SText(item.label, style = SintoniaTheme.typography.monoSmall, color = if (active) colors.text else colors.textMuted, maxLines = 1)
            }
        }
    }
}

@Composable
private fun SideMenu(items: List<NavItem>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    val colors = SintoniaTheme.colors
    val hairline = SintoniaTheme.strokes.hairline
    val selectedFocus = remember { FocusRequester() }
    Column(
        modifier = Modifier
            .width(SintoniaTheme.heights.sidebar)
            .fillMaxHeight()
            .background(colors.background)
            .drawBehind {
                drawRect(colors.border, Offset(size.width - hairline.toPx(), 0f), Size(hairline.toPx(), size.height))
            }.padding(vertical = SintoniaTheme.spacing.xxl)
            .focusRestorer(selectedFocus)
            .focusGroup(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.sm),
    ) {
        SLogo(size = SintoniaTheme.sizes.logoMedium, modifier = Modifier.padding(bottom = SintoniaTheme.spacing.xl))
        items.forEachIndexed { index, item ->
            val active = index == selectedIndex
            val shape = RoundedCornerShape(SintoniaTheme.radii.menuItem)
            Box(
                modifier = Modifier
                    .size(SintoniaTheme.sizes.menuItem)
                    .then(if (active) Modifier.focusRequester(selectedFocus) else Modifier)
                    .sintoniaClickable(onClick = { onSelect(index) }, cornerRadius = SintoniaTheme.radii.menuItem, role = Role.Tab)
                    .then(
                        if (active) {
                            Modifier
                                .background(colors.selected, shape)
                                .border(hairline, colors.borderStrong, shape)
                        } else {
                            Modifier
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                SIcon(item.icon, contentDescription = item.label, tint = if (active) colors.text else colors.textMuted, size = SintoniaTheme.sizes.iconMenu)
            }
        }
    }
}
