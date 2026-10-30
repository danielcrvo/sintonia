package com.danielcrvo.sintonia.feature.categories.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.danielcrvo.sintonia.R
import com.danielcrvo.sintonia.core.designsystem.components.ButtonSize
import com.danielcrvo.sintonia.core.designsystem.components.ButtonVariant
import com.danielcrvo.sintonia.core.designsystem.components.SAccordionHeader
import com.danielcrvo.sintonia.core.designsystem.components.SButton
import com.danielcrvo.sintonia.core.designsystem.components.SDialog
import com.danielcrvo.sintonia.core.designsystem.components.SHeader
import com.danielcrvo.sintonia.core.designsystem.components.SSettingsRow
import com.danielcrvo.sintonia.core.designsystem.components.SSwitch
import com.danielcrvo.sintonia.core.designsystem.components.SText
import com.danielcrvo.sintonia.core.designsystem.components.STextField
import com.danielcrvo.sintonia.core.designsystem.components.initialFocusOnTv
import com.danielcrvo.sintonia.core.designsystem.components.sintoniaItemMotion
import com.danielcrvo.sintonia.core.designsystem.components.tvSafeArea
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme
import com.danielcrvo.sintonia.core.model.ContentKind

@Composable
fun CategoriesScreen(
    state: CategoriesUiState,
    onBack: () -> Unit,
    onToggleSection: (ContentKind) -> Unit,
    onToggleHidden: (ContentKind, String, Boolean) -> Unit,
    onRenameStart: (ContentKind, CategoryItem) -> Unit,
    onRenameTextChange: (String) -> Unit,
    onRenameSave: () -> Unit,
    onRenameReset: () -> Unit,
    onRenameDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SintoniaTheme.colors
    val spacing = SintoniaTheme.spacing
    Column(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .tvSafeArea(),
    ) {
        SHeader(title = stringResource(R.string.categories_title), onBack = onBack)
        LazyColumn(
            Modifier.weight(1f).navigationBarsPadding().focusRestorer(),
            contentPadding = PaddingValues(horizontal = spacing.gutter, vertical = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            item(key = "help") {
                SText(
                    stringResource(R.string.categories_help),
                    style = SintoniaTheme.typography.caption,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(bottom = spacing.md),
                )
            }
            state.sections.forEach { section ->
                val open = state.expanded == section.kind
                item(key = "header-${section.kind}") {
                    val shape = RoundedCornerShape(SintoniaTheme.radii.card)
                    Column(Modifier.fillMaxWidth().background(colors.surface, shape).border(SintoniaTheme.strokes.hairline, colors.border, shape)) {
                        SAccordionHeader(
                            title = stringResource(kindLabel(section.kind)),
                            expanded = open,
                            onToggle = { onToggleSection(section.kind) },
                            summary = if (section.hiddenCount == 0) {
                                stringResource(R.string.categories_none_hidden)
                            } else {
                                pluralStringResource(R.plurals.categories_hidden_count, section.hiddenCount, section.hiddenCount)
                            },
                        )
                    }
                }
                if (open) {
                    if (section.categories.isEmpty()) {
                        item(key = "empty-${section.kind}") {
                            SText(stringResource(R.string.categories_empty), color = colors.textMuted, modifier = Modifier.padding(spacing.lg))
                        }
                    }
                    items(section.categories, key = { "${section.kind}-${it.id}" }) { category ->
                        SSettingsRow(
                            modifier = sintoniaItemMotion(),
                            title = category.name,
                            subtitle = if (category.renamed) stringResource(R.string.categories_original, category.originalName) else null,
                            onClick = { onRenameStart(section.kind, category) },
                            trailing = {
                                SSwitch(checked = !category.hidden, onCheckedChange = { onToggleHidden(section.kind, category.id, !it) })
                            },
                        )
                    }
                }
            }
        }
    }

    val renaming = state.renaming
    SDialog(visible = renaming != null, onDismiss = onRenameDismiss, title = stringResource(R.string.categories_rename_title)) {
        if (renaming != null) {
            STextField(
                value = renaming.text,
                onValueChange = onRenameTextChange,
                label = stringResource(R.string.categories_name_label),
                placeholder = renaming.item.originalName,
                imeAction = ImeAction.Done,
                onImeAction = onRenameSave,
            )
            Row(Modifier.fillMaxWidth().padding(top = spacing.md), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                SButton(
                    stringResource(R.string.action_cancel),
                    onRenameDismiss,
                    Modifier.weight(1f),
                    variant = ButtonVariant.Secondary,
                    size = ButtonSize.Compact,
                )
                // On TV focus starts on Save: a text field would open the keyboard and trap the D-pad.
                SButton(stringResource(R.string.action_save), onRenameSave, Modifier.weight(1f).initialFocusOnTv(), size = ButtonSize.Compact)
            }
            if (renaming.item.renamed) {
                SButton(
                    stringResource(R.string.categories_restore),
                    onRenameReset,
                    Modifier.fillMaxWidth(),
                    variant = ButtonVariant.Secondary,
                    size = ButtonSize.Compact,
                )
            }
        }
    }
}

private fun kindLabel(kind: ContentKind): Int = when (kind) {
    ContentKind.Live -> R.string.tab_live
    ContentKind.Movie -> R.string.tab_movies
    ContentKind.Series -> R.string.tab_series
}
