package com.danielcrvo.sintonia.feature.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.danielcrvo.sintonia.R
import com.danielcrvo.sintonia.core.designsystem.components.ButtonSize
import com.danielcrvo.sintonia.core.designsystem.components.PosterShape
import com.danielcrvo.sintonia.core.designsystem.components.SButton
import com.danielcrvo.sintonia.core.designsystem.components.SEmptyState
import com.danielcrvo.sintonia.core.designsystem.components.SIcons
import com.danielcrvo.sintonia.core.designsystem.components.SImage
import com.danielcrvo.sintonia.core.designsystem.components.SPoster
import com.danielcrvo.sintonia.core.designsystem.components.SSectionLabel
import com.danielcrvo.sintonia.core.designsystem.components.SText
import com.danielcrvo.sintonia.core.designsystem.components.sintoniaClickable
import com.danielcrvo.sintonia.core.designsystem.theme.SintoniaTheme
import com.danielcrvo.sintonia.core.model.Channel

@Composable
fun HomeScreen(
    state: HomeUiState,
    onFeatured: (FeaturedItem) -> Unit,
    onContinue: (ContinueItem) -> Unit,
    onChannel: (Channel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SintoniaTheme.colors
    val spacing = SintoniaTheme.spacing
    Column(modifier.fillMaxSize().background(colors.background)) {
        if (state.isEmpty) {
            SEmptyState(
                icon = SIcons.Tv,
                title = stringResource(R.string.home_empty_title),
                message = stringResource(R.string.home_empty_message),
            )
            return@Column
        }
        LazyColumn(
            Modifier.fillMaxSize().focusRestorer(),
            contentPadding = PaddingValues(bottom = spacing.xxxl),
            verticalArrangement = Arrangement.spacedBy(spacing.xxl),
        ) {
            state.featured?.let { featured ->
                item(key = "featured") { FeaturedCard(featured, { onFeatured(featured) }, Modifier.padding(horizontal = spacing.gutter)) }
            }
            if (state.continueWatching.isNotEmpty()) {
                item(key = "continue") {
                    Section(R.string.home_continue) {
                        LazyRow(
                            modifier = Modifier.focusRestorer(),
                            contentPadding = PaddingValues(horizontal = spacing.gutter),
                            horizontalArrangement = Arrangement.spacedBy(spacing.md),
                        ) {
                            items(state.continueWatching, key = { it.key }) { item ->
                                SPoster(
                                    title = item.title,
                                    imageUrl = item.imageUrl,
                                    onClick = { onContinue(item) },
                                    modifier = Modifier.width(SintoniaTheme.sizes.featuredCardWidth),
                                    meta = if (item.season != null &&
                                        item.episode != null
                                    ) {
                                        stringResource(R.string.episode_code, item.season, item.episode)
                                    } else {
                                        null
                                    },
                                    progress = item.fraction,
                                    shape = PosterShape.Wide,
                                )
                            }
                        }
                    }
                }
            }
            if (state.favorites.isNotEmpty()) {
                item(key = "favorites") {
                    Section(R.string.home_favorites) {
                        LazyRow(
                            modifier = Modifier.focusRestorer(),
                            contentPadding = PaddingValues(horizontal = spacing.gutter),
                            horizontalArrangement = Arrangement.spacedBy(spacing.lg),
                        ) {
                            items(state.favorites, key = { it.id }) { channel -> ChannelBubble(channel) { onChannel(channel) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(title: Int, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.sm)) {
        SSectionLabel(stringResource(title), Modifier.padding(horizontal = SintoniaTheme.spacing.gutter))
        content()
    }
}

@Composable
private fun FeaturedCard(item: FeaturedItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = SintoniaTheme.colors
    val shape = RoundedCornerShape(SintoniaTheme.radii.card)
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(16f / 10f)
            .clip(shape)
            .border(SintoniaTheme.strokes.hairline, colors.border, shape)
            .sintoniaClickable(onClick = onClick, cornerRadius = SintoniaTheme.radii.card),
    ) {
        SImage(item.imageUrl, null, Modifier.matchParentSize())
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)))))
        Column(
            Modifier.align(Alignment.BottomStart).padding(SintoniaTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.sm),
        ) {
            SText(stringResource(R.string.home_featured).uppercase(), style = SintoniaTheme.typography.monoLabel, color = Color.White.copy(alpha = 0.7f))
            SText(item.title, style = SintoniaTheme.typography.heading, color = Color.White, maxLines = 2)
            item.meta?.let { SText(it, style = SintoniaTheme.typography.mono, color = Color.White.copy(alpha = 0.7f)) }
            SButton(stringResource(R.string.detail_watch), onClick, leadingIcon = SIcons.Play, size = ButtonSize.Hero)
        }
    }
}

@Composable
private fun ChannelBubble(channel: Channel, onClick: () -> Unit) {
    val colors = SintoniaTheme.colors
    Column(
        modifier = Modifier
            .width(SintoniaTheme.sizes.favoriteItemWidth)
            .sintoniaClickable(onClick = onClick, cornerRadius = SintoniaTheme.radii.control),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SintoniaTheme.spacing.sm),
    ) {
        SImage(
            channel.logoUrl,
            null,
            Modifier.size(SintoniaTheme.sizes.avatarLarge).clip(CircleShape).border(SintoniaTheme.strokes.hairline, colors.border, CircleShape),
        )
        SText(channel.name, style = SintoniaTheme.typography.caption, color = colors.textSecondary, maxLines = 1, textAlign = TextAlign.Center)
    }
}
