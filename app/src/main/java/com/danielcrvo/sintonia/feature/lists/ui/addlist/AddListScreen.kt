package com.danielcrvo.sintonia.feature.lists.ui.addlist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.danielcrvo.sintonia.R
import com.danielcrvo.sintonia.core.data.PickedFile
import com.danielcrvo.sintonia.core.designsystem.components.ButtonSize
import com.danielcrvo.sintonia.core.designsystem.components.ButtonVariant
import com.danielcrvo.sintonia.core.designsystem.components.SButton
import com.danielcrvo.sintonia.core.designsystem.components.SHeader
import com.danielcrvo.sintonia.core.designsystem.components.SIcon
import com.danielcrvo.sintonia.core.designsystem.components.SIcons
import com.danielcrvo.sintonia.core.designsystem.components.STabs
import com.danielcrvo.sintonia.core.designsystem.components.SText
import com.danielcrvo.sintonia.core.designsystem.components.STextField
import com.danielcrvo.sintonia.core.designsystem.components.tvSafeArea
import com.danielcrvo.sintonia.core.designsystem.theme.DeviceClass
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme
import com.danielcrvo.sintonia.feature.lists.domain.FieldError
import com.danielcrvo.sintonia.feature.lists.domain.FormField
import java.util.Locale

@Composable
fun AddListScreen(
    state: AddListUiState,
    onBack: () -> Unit,
    onTabChange: (AddListTab) -> Unit,
    onNameChange: (String) -> Unit,
    onServerChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onPickFile: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    canGoBack: Boolean = true,
) {
    val colors = SintoniaTheme.colors
    val spacing = SintoniaTheme.spacing
    val tv = SintoniaTheme.deviceClass == DeviceClass.Tv
    val hairline = SintoniaTheme.strokes.hairline
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .tvSafeArea(),
    ) {
        SHeader(title = stringResource(R.string.addlist_title), onBack = onBack.takeIf { canGoBack })
        STabs(
            tabs = listOf(
                stringResource(R.string.addlist_tab_xtream),
                stringResource(R.string.addlist_tab_url),
                stringResource(R.string.addlist_tab_file),
            ),
            selectedIndex = state.tab.ordinal,
            onSelected = { onTabChange(AddListTab.entries[it]) },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.gutter, vertical = spacing.xl),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            STextField(
                value = state.name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(R.string.addlist_name),
                placeholder = stringResource(R.string.addlist_name_hint),
            )
            when (state.tab) {
                AddListTab.Xtream -> {
                    STextField(
                        value = state.server,
                        onValueChange = onServerChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = stringResource(R.string.addlist_server),
                        placeholder = stringResource(R.string.addlist_server_hint),
                        keyboardType = KeyboardType.Uri,
                        error = state.errors[FormField.Server]?.let { errorText(it) },
                    )
                    STextField(
                        value = state.username,
                        onValueChange = onUsernameChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = stringResource(R.string.addlist_username),
                        error = state.errors[FormField.Username]?.let { errorText(it) },
                    )
                    STextField(
                        value = state.password,
                        onValueChange = onPasswordChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = stringResource(R.string.addlist_password),
                        password = true,
                        imeAction = ImeAction.Done,
                        onImeAction = onSubmit,
                        error = state.errors[FormField.Password]?.let { errorText(it) },
                    )
                }
                AddListTab.Url -> STextField(
                    value = state.url,
                    onValueChange = onUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(R.string.addlist_url),
                    placeholder = stringResource(R.string.addlist_url_hint),
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done,
                    onImeAction = onSubmit,
                    error = state.errors[FormField.Url]?.let { errorText(it) },
                )
                AddListTab.File -> FileCard(
                    file = state.file,
                    onPick = onPickFile,
                    error = state.errors[FormField.File]?.let { errorText(it) },
                )
            }
            NoteCard(
                text = stringResource(
                    when (state.tab) {
                        AddListTab.Xtream -> R.string.addlist_note_xtream
                        AddListTab.Url -> R.string.addlist_note_url
                        AddListTab.File -> R.string.addlist_note_file
                    },
                ),
                extra = if (tv && state.tab == AddListTab.File) stringResource(R.string.addlist_note_file_tv) else null,
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRect(colors.border, Offset.Zero, Size(size.width, hairline.toPx()))
                }.navigationBarsPadding()
                .imePadding()
                .padding(spacing.lg),
        ) {
            SButton(
                text = stringResource(R.string.addlist_connect),
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth(),
                size = ButtonSize.Footer,
                enabled = !state.busy,
            )
        }
    }
}

@Composable
private fun errorText(error: FieldError): String = stringResource(
    when (error) {
        FieldError.Required -> R.string.field_required
        FieldError.InvalidUrl -> R.string.field_invalid_url
    },
)

@Composable
private fun NoteCard(text: String, extra: String?) {
    val colors = SintoniaTheme.colors
    val shape = RoundedCornerShape(SintoniaTheme.radii.card)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, shape)
            .border(SintoniaTheme.strokes.hairline, colors.border, shape)
            .padding(SintoniaTheme.spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.md),
    ) {
        SIcon(SIcons.Info, contentDescription = null, tint = colors.textMuted)
        Column(verticalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.sm)) {
            SText(text, style = SintoniaTheme.typography.caption, color = colors.textSecondary)
            if (extra != null) SText(extra, style = SintoniaTheme.typography.caption, color = colors.textSecondary)
        }
    }
}

@Composable
private fun FileCard(file: PickedFile?, onPick: () -> Unit, error: String?) {
    val colors = SintoniaTheme.colors
    val shape = RoundedCornerShape(SintoniaTheme.radii.card)
    Column(verticalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.sm)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface, shape)
                .border(SintoniaTheme.strokes.hairline, if (error != null) colors.danger else colors.border, shape)
                .padding(SintoniaTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.md),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.md),
            ) {
                SIcon(
                    if (file ==
                        null
                    ) {
                        SIcons.Folder
                    } else {
                        SIcons.Document
                    },
                    contentDescription = null,
                    tint = colors.textMuted,
                    size = SintoniaTheme.sizes.iconMedium,
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(SintoniaTheme.sizes.tightGap)) {
                    SText(
                        text = file?.displayName ?: stringResource(R.string.addlist_file_empty),
                        style = SintoniaTheme.typography.bodyStrong,
                        color = if (file == null) colors.textSecondary else colors.text,
                        maxLines = 1,
                    )
                    if (file != null) {
                        SText(
                            text = stringResource(
                                R.string.meta_two,
                                formatBytes(file.sizeBytes),
                                pluralStringResource(R.plurals.addlist_file_items, file.itemCount, file.itemCount),
                            ),
                            style = SintoniaTheme.typography.mono,
                            color = colors.textMuted,
                            maxLines = 1,
                        )
                    }
                }
                if (file != null) {
                    SButton(
                        text = stringResource(R.string.addlist_file_change),
                        onClick = onPick,
                        variant = ButtonVariant.Secondary,
                        size = ButtonSize.Compact,
                    )
                }
            }
            if (file == null) {
                SButton(
                    text = stringResource(R.string.addlist_file_choose),
                    onClick = onPick,
                    modifier = Modifier.fillMaxWidth(),
                    variant = ButtonVariant.Secondary,
                )
            }
        }
        if (error != null) SText(error, style = SintoniaTheme.typography.caption, color = colors.danger)
    }
}

internal fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000 -> String.format(Locale.getDefault(), "%.1f MB", bytes / 1_000_000.0)
    bytes >= 1_000 -> String.format(Locale.getDefault(), "%d KB", bytes / 1_000)
    else -> "$bytes B"
}
