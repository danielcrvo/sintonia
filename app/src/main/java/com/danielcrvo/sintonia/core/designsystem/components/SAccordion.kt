package com.danielcrvo.sintonia.core.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme

@Composable
fun SAccordionHeader(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
) {
    val colors = SintoniaTheme.colors
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, SintoniaTheme.motion.normal(), label = "accordionArrow")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = SintoniaTheme.heights.settingsRow)
            .sintoniaClickable(onClick = onToggle, cornerRadius = SintoniaTheme.radii.card, role = Role.Button)
            .padding(horizontal = SintoniaTheme.spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.md),
    ) {
        SText(title, style = SintoniaTheme.typography.bodyStrong, modifier = Modifier.weight(1f), maxLines = 1)
        if (summary != null) SText(summary, style = SintoniaTheme.typography.mono, color = colors.textMuted, maxLines = 1)
        SIcon(
            icon = SIcons.ChevronDown,
            contentDescription = null,
            tint = colors.icon,
            modifier = Modifier.graphicsLayer { rotationZ = rotation },
        )
    }
}

@Composable
fun SAccordion(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = SintoniaTheme.colors
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, SintoniaTheme.motion.normal(), label = "accordionArrow")
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = SintoniaTheme.heights.settingsRow)
                .sintoniaClickable(onClick = onToggle, cornerRadius = SintoniaTheme.radii.card, role = Role.Button)
                .padding(horizontal = SintoniaTheme.spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.md),
        ) {
            SText(title, style = SintoniaTheme.typography.bodyStrong, modifier = Modifier.weight(1f), maxLines = 1)
            if (summary != null) SText(summary, style = SintoniaTheme.typography.mono, color = colors.textMuted, maxLines = 1)
            SIcon(
                icon = SIcons.ChevronDown,
                contentDescription = null,
                tint = colors.icon,
                modifier = Modifier.graphicsLayer { rotationZ = rotation },
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(SintoniaTheme.motion.normal()) + fadeIn(SintoniaTheme.motion.normal()),
            exit = shrinkVertically(SintoniaTheme.motion.normal()) + fadeOut(SintoniaTheme.motion.fast()),
        ) {
            Column(content = content)
        }
    }
}
