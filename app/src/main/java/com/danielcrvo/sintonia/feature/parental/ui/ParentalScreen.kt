package com.danielcrvo.sintonia.feature.parental.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.danielcrvo.sintonia.R
import com.danielcrvo.sintonia.core.designsystem.components.SAccordionHeader
import com.danielcrvo.sintonia.core.designsystem.components.SCheckbox
import com.danielcrvo.sintonia.core.designsystem.components.SGroup
import com.danielcrvo.sintonia.core.designsystem.components.SHeader
import com.danielcrvo.sintonia.core.designsystem.components.SIcons
import com.danielcrvo.sintonia.core.designsystem.components.SSectionLabel
import com.danielcrvo.sintonia.core.designsystem.components.SSettingsRow
import com.danielcrvo.sintonia.core.designsystem.components.SSwitch
import com.danielcrvo.sintonia.core.designsystem.components.SText
import com.danielcrvo.sintonia.core.designsystem.components.tvSafeArea
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme
import com.danielcrvo.sintonia.core.model.ContentKind

@Composable
fun ParentalScreen(
    state: ParentalUiState,
    onBack: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onChangePin: () -> Unit,
    onToggleSection: (ContentKind) -> Unit,
    onToggleCategory: (ContentKind, String, Boolean) -> Unit,
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
        SHeader(title = stringResource(R.string.parental_title), onBack = onBack)
        if (!state.unlocked) return@Column
        LazyColumn(
            Modifier.weight(1f).navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = spacing.gutter, vertical = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            item(key = "switch") {
                SGroup {
                    SSettingsRow(
                        title = stringResource(R.string.parental_enable),
                        icon = SIcons.Lock,
                        trailing = { SSwitch(checked = state.enabled, onCheckedChange = onToggleEnabled) },
                        showDivider = state.enabled,
                    )
                    if (state.enabled) {
                        SSettingsRow(
                            title = stringResource(R.string.parental_change_pin),
                            onClick = onChangePin,
                            showDivider = false,
                        )
                    }
                }
            }
            item(key = "help") {
                SText(
                    stringResource(if (state.enabled) R.string.parental_help else R.string.parental_needs_pin_first),
                    style = SintoniaTheme.typography.caption,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(vertical = spacing.md),
                )
            }
            if (state.enabled) {
                item(key = "label") { SSectionLabel(stringResource(R.string.parental_categories), Modifier.padding(top = spacing.sm)) }
                state.sections.forEach { section ->
                    val open = state.expanded == section.kind
                    item(key = "header-${section.kind}") {
                        val shape = RoundedCornerShape(SintoniaTheme.radii.card)
                        Column(Modifier.fillMaxWidth().background(colors.surface, shape).border(SintoniaTheme.strokes.hairline, colors.border, shape)) {
                            SAccordionHeader(
                                title = stringResource(kindLabel(section.kind)),
                                expanded = open,
                                onToggle = { onToggleSection(section.kind) },
                                summary = if (section.blockedCount == 0) {
                                    stringResource(R.string.parental_none_blocked)
                                } else {
                                    pluralStringResource(R.plurals.parental_blocked_count, section.blockedCount, section.blockedCount)
                                },
                            )
                        }
                    }
                    if (open) {
                        if (section.categories.isEmpty()) {
                            item(key = "empty-${section.kind}") {
                                SText(stringResource(R.string.parental_no_categories), color = colors.textMuted, modifier = Modifier.padding(spacing.lg))
                            }
                        }
                        items(section.categories, key = { "${section.kind}-${it.id}" }) { category ->
                            SSettingsRow(
                                title = category.name,
                                onClick = { onToggleCategory(section.kind, category.id, !category.blocked) },
                                trailing = {
                                    SCheckbox(
                                        category.blocked,
                                        { onToggleCategory(section.kind, category.id, it) },
                                        contentDescription = category.name,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun kindLabel(kind: ContentKind): Int = when (kind) {
    ContentKind.Live -> R.string.tab_live
    ContentKind.Movie -> R.string.tab_movies
    ContentKind.Series -> R.string.tab_series
}
