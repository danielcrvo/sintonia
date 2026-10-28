package com.danielcrvo.sintonia.feature.lists.ui.importing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.danielcrvo.sintonia.R
import com.danielcrvo.sintonia.core.common.formatCount
import com.danielcrvo.sintonia.core.data.importing.ImportProgress
import com.danielcrvo.sintonia.core.data.importing.ImportStep
import com.danielcrvo.sintonia.core.data.importing.StepState
import com.danielcrvo.sintonia.core.data.importing.StepStatus
import com.danielcrvo.sintonia.core.designsystem.components.ButtonSize
import com.danielcrvo.sintonia.core.designsystem.components.ButtonVariant
import com.danielcrvo.sintonia.core.designsystem.components.SButton
import com.danielcrvo.sintonia.core.designsystem.components.SDialog
import com.danielcrvo.sintonia.core.designsystem.components.SIcon
import com.danielcrvo.sintonia.core.designsystem.components.SIcons
import com.danielcrvo.sintonia.core.designsystem.components.SLogo
import com.danielcrvo.sintonia.core.designsystem.components.SProgressBar
import com.danielcrvo.sintonia.core.designsystem.components.SSpinner
import com.danielcrvo.sintonia.core.designsystem.components.SText
import com.danielcrvo.sintonia.core.designsystem.components.initialFocusOnTv
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme
import com.danielcrvo.sintonia.core.network.ApiException

@Composable
fun ImportScreen(
    state: ImportUiState,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onEditDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SintoniaTheme.colors
    val spacing = SintoniaTheme.spacing
    val scroll = rememberScrollState()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = spacing.gutter, vertical = spacing.xxl),
        contentAlignment = Alignment.Center,
    ) {
        // Scrolls when it does not fit (TV has little height) so the button stays reachable by focus.
        Column(
            modifier = Modifier
                .widthIn(max = SintoniaTheme.sizes.contentMaxWidth)
                .fillMaxWidth()
                .verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SLogo(size = SintoniaTheme.sizes.logoLarge)
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                SText(stringResource(R.string.loading_title), style = SintoniaTheme.typography.heading, textAlign = TextAlign.Center)
                SText(
                    stringResource(R.string.loading_subtitle),
                    style = SintoniaTheme.typography.body,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
            SText(
                stringResource(R.string.loading_percent, state.progress.percent),
                style = SintoniaTheme.typography.display.copy(fontFamily = SintoniaTheme.typography.mono.fontFamily),
            )
            SProgressBar(progress = { state.progress.percent / 100f }, height = SintoniaTheme.sizes.progressRegular)
            StepsCard(state.progress)
            SButton(
                text = stringResource(R.string.loading_cancel),
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth().initialFocusOnTv(),
                variant = ButtonVariant.Secondary,
                size = ButtonSize.Footer,
            )
        }
    }

    val failure = state.failure
    SDialog(visible = failure != null, onDismiss = onCancel, title = null) {
        if (failure != null) ErrorContent(failure, onRetry, onEditDetails)
    }
}

@Composable
private fun StepsCard(progress: ImportProgress) {
    val colors = SintoniaTheme.colors
    val shape = RoundedCornerShape(SintoniaTheme.radii.card)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, shape)
            .border(SintoniaTheme.strokes.hairline, colors.border, shape),
    ) {
        progress.steps.forEachIndexed { index, step ->
            StepRow(step, showDivider = index != progress.steps.lastIndex)
        }
    }
}

@Composable
private fun StepRow(step: StepState, showDivider: Boolean) {
    val colors = SintoniaTheme.colors
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SintoniaTheme.spacing.lg, vertical = SintoniaTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.md),
        ) {
            Box(Modifier.size(SintoniaTheme.sizes.stepSlot), contentAlignment = Alignment.Center) {
                when (step.status) {
                    StepStatus.Done -> SIcon(SIcons.Check, contentDescription = null, tint = colors.text, size = SintoniaTheme.sizes.iconSmall)
                    StepStatus.Running -> SSpinner(size = SintoniaTheme.sizes.spinnerSmall)
                    StepStatus.Error -> SIcon(SIcons.AlertCircle, contentDescription = null, tint = colors.danger, size = SintoniaTheme.sizes.iconSmall)
                    StepStatus.Pending -> Box(Modifier.size(SintoniaTheme.sizes.stepMarker).background(colors.borderStrong, CircleShape))
                }
            }
            SText(
                stringResource(stepLabel(step.step)),
                style = SintoniaTheme.typography.body,
                color = if (step.status == StepStatus.Pending) colors.textMuted else colors.text,
                modifier = Modifier.weight(1f),
                maxLines = 1,
            )
            if (step.status != StepStatus.Pending && step.step != ImportStep.Connect && step.count > 0) {
                SText(formatCount(step.count), style = SintoniaTheme.typography.mono, color = colors.textMuted, maxLines = 1)
            }
        }
        if (showDivider) Box(Modifier.fillMaxWidth().height(SintoniaTheme.strokes.hairline).background(colors.divider))
    }
}

private fun stepLabel(step: ImportStep): Int = when (step) {
    ImportStep.Connect -> R.string.loading_step_connect
    ImportStep.Live -> R.string.loading_step_live
    ImportStep.Movies -> R.string.loading_step_movies
    ImportStep.Series -> R.string.loading_step_series
    ImportStep.Guide -> R.string.loading_step_guide
}

@Composable
private fun ErrorContent(failure: ImportFailure, onRetry: () -> Unit, onEditDetails: () -> Unit) {
    val colors = SintoniaTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.md),
        modifier = Modifier.fillMaxWidth(),
    ) {
        SIcon(SIcons.AlertCircle, contentDescription = null, tint = colors.danger, size = SintoniaTheme.sizes.iconLarge)
        SText(stringResource(R.string.error_title), style = SintoniaTheme.typography.subheading, textAlign = TextAlign.Center)
        SText(stringResource(errorMessage(failure.kind)), color = colors.textSecondary, textAlign = TextAlign.Center)
        SText(errorCode(failure), style = SintoniaTheme.typography.mono, color = colors.textMuted, textAlign = TextAlign.Center)
        Row(Modifier.fillMaxWidth().padding(top = SintoniaTheme.spacing.sm), horizontalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.sm)) {
            SButton(stringResource(R.string.error_edit), onEditDetails, Modifier.weight(1f), variant = ButtonVariant.Secondary)
            SButton(stringResource(R.string.error_retry), onRetry, Modifier.weight(1f))
        }
    }
}

private fun errorMessage(kind: ApiException.Kind): Int = when (kind) {
    ApiException.Kind.Network -> R.string.error_network
    ApiException.Kind.Timeout -> R.string.error_timeout
    ApiException.Kind.Ssl -> R.string.error_ssl
    ApiException.Kind.Auth -> R.string.error_auth
    ApiException.Kind.Http -> R.string.error_http
    ApiException.Kind.Invalid -> R.string.error_invalid
    ApiException.Kind.Empty -> R.string.error_empty
    ApiException.Kind.File -> R.string.error_file
}

private fun errorCode(failure: ImportFailure): String = buildList {
    add(failure.httpCode?.let { "HTTP $it" } ?: failure.kind.name.uppercase())
    failure.host?.let { add(it) }
}.joinToString(" · ")
