package com.danielcrvo.sintonia.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.danielcrvo.sintonia.R
import com.danielcrvo.sintonia.core.designsystem.components.ButtonSize
import com.danielcrvo.sintonia.core.designsystem.components.ButtonVariant
import com.danielcrvo.sintonia.core.designsystem.components.SBadge
import com.danielcrvo.sintonia.core.designsystem.components.SButton
import com.danielcrvo.sintonia.core.designsystem.components.SDialog
import com.danielcrvo.sintonia.core.designsystem.components.SHeader
import com.danielcrvo.sintonia.core.designsystem.components.SIconButton
import com.danielcrvo.sintonia.core.designsystem.components.SIcons
import com.danielcrvo.sintonia.core.designsystem.components.SRadio
import com.danielcrvo.sintonia.core.designsystem.components.SText
import com.danielcrvo.sintonia.core.designsystem.components.initialFocusOnTv
import com.danielcrvo.sintonia.core.designsystem.components.sintoniaClickable
import com.danielcrvo.sintonia.core.designsystem.components.sintoniaItemMotion
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme
import com.danielcrvo.sintonia.core.model.IptvList

@Composable
fun ListsScreen(
    state: ListsUiState,
    onBack: () -> Unit,
    onSelect: (IptvList) -> Unit,
    onRemoveRequest: (IptvList) -> Unit,
    onRemoveCancel: () -> Unit,
    onRemoveConfirm: () -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SintoniaTheme.colors
    val spacing = SintoniaTheme.spacing
    val hairline = SintoniaTheme.strokes.hairline
    Column(modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
        SHeader(title = stringResource(R.string.lists_title), onBack = onBack)
        LazyColumn(
            Modifier.weight(1f).focusRestorer(),
            contentPadding = androidx.compose.foundation.layout
                .PaddingValues(horizontal = spacing.gutter, vertical = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            items(state.lists, key = { it.id }) { list ->
                ListCard(
                    list,
                    active = list.id == state.activeId,
                    onSelect = { onSelect(list) },
                    onRemove = { onRemoveRequest(list) },
                    modifier = sintoniaItemMotion(),
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .drawBehind { drawRect(colors.border, Offset.Zero, Size(size.width, hairline.toPx())) }
                .navigationBarsPadding()
                .padding(spacing.lg),
        ) {
            SButton(stringResource(R.string.lists_add), onAdd, Modifier.fillMaxWidth().initialFocusOnTv(), size = ButtonSize.Footer, leadingIcon = SIcons.Add)
        }
    }

    val pending = state.pendingRemoval
    SDialog(visible = pending != null, onDismiss = onRemoveCancel, title = stringResource(R.string.lists_remove)) {
        if (pending != null) {
            SText(stringResource(R.string.lists_remove_message, pending.name), color = colors.textSecondary)
            Row(Modifier.fillMaxWidth().padding(top = spacing.md), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                SButton(stringResource(R.string.action_cancel), onRemoveCancel, Modifier.weight(1f), variant = ButtonVariant.Secondary)
                SButton(stringResource(R.string.lists_remove), onRemoveConfirm, Modifier.weight(1f), variant = ButtonVariant.Destructive)
            }
        }
    }
}

@Composable
private fun ListCard(list: IptvList, active: Boolean, onSelect: () -> Unit, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    val colors = SintoniaTheme.colors
    val shape = RoundedCornerShape(SintoniaTheme.radii.card)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .sintoniaClickable(onClick = onSelect, cornerRadius = SintoniaTheme.radii.card)
            .background(colors.surface, shape)
            .border(SintoniaTheme.strokes.hairline, if (active) colors.borderStrong else colors.border, shape)
            .padding(start = SintoniaTheme.spacing.lg, top = SintoniaTheme.spacing.md, bottom = SintoniaTheme.spacing.md, end = SintoniaTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.md),
    ) {
        SRadio(selected = active)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.xs)) {
            Row(horizontalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                SText(list.name, style = SintoniaTheme.typography.bodyLarge, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                if (active) SBadge(stringResource(R.string.lists_active))
            }
            SText(listMeta(list), style = SintoniaTheme.typography.mono, color = colors.textMuted, textAlign = TextAlign.Start)
        }
        SIconButton(SIcons.Trash, stringResource(R.string.lists_remove), onRemove, bordered = false)
    }
}
