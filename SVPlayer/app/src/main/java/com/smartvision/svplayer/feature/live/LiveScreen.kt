package com.smartvision.svplayer.feature.live

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.smartvision.svplayer.R
import com.smartvision.svplayer.core.data.LocalAppContainer
import com.smartvision.svplayer.core.designsystem.Badge
import com.smartvision.svplayer.core.designsystem.FocusableButton
import com.smartvision.svplayer.core.designsystem.FocusableCard
import com.smartvision.svplayer.core.designsystem.SVColors
import com.smartvision.svplayer.core.ui.viewModelFactory
import com.smartvision.svplayer.domain.model.Category
import com.smartvision.svplayer.domain.model.LiveChannel

@Composable
fun LiveRoute(openPlayer: (Int) -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: LiveViewModel = viewModel(
        factory = viewModelFactory { LiveViewModel(container.catalogRepository) },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LiveScreen(
        state = state,
        onCategory = viewModel::selectCategory,
        onChannel = viewModel::selectChannel,
        onFavorite = { state.selectedChannel?.let(viewModel::toggleFavorite) },
        onWatch = { state.selectedChannel?.streamId?.let(openPlayer) },
    )
}

@Composable
private fun LiveScreen(
    state: LiveUiState,
    onCategory: (Category) -> Unit,
    onChannel: (LiveChannel) -> Unit,
    onFavorite: () -> Unit,
    onWatch: () -> Unit,
) {
    val selected = state.selectedChannel
    val currentCategory = state.categories.firstOrNull { it.id == state.selectedCategoryId }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp)),
    ) {
        Image(
            painter = painterResource(R.drawable.home_live_stadium),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFA020817),
                            Color(0xE7031020),
                            Color(0xC5072632),
                            Color(0xF5020817),
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(SVColors.Cyan.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(980f, 120f),
                        radius = 820f,
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
        ) {
            LiveHeaderStrip(
                categoryName = currentCategory?.name ?: "Toutes les chaines",
                channelsCount = state.channels.size,
                selected = selected,
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                CategoryRail(
                    categories = state.categories,
                    selectedCategoryId = state.selectedCategoryId,
                    onCategory = onCategory,
                    modifier = Modifier
                        .weight(0.78f)
                        .fillMaxHeight(),
                )
                ChannelGuide(
                    categoryName = currentCategory?.name ?: "Live TV",
                    channels = state.channels,
                    selectedChannelId = selected?.streamId,
                    onChannel = onChannel,
                    modifier = Modifier
                        .weight(1.22f)
                        .fillMaxHeight(),
                )
                LivePreviewPanel(
                    selected = selected,
                    onWatch = onWatch,
                    onFavorite = onFavorite,
                    modifier = Modifier
                        .weight(1.55f)
                        .fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun LiveHeaderStrip(
    categoryName: String,
    channelsCount: Int,
    selected: LiveChannel?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(SVColors.Cyan, Color(0xFF0F766E), SVColors.Blue),
                        ),
                    )
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Tv, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "LIVE TV",
                        color = SVColors.TextPrimary,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black,
                    )
                    Spacer(Modifier.width(12.dp))
                    LiveDotLabel("DIRECT")
                }
                Text(
                    text = "$categoryName | ${channelsCount.toString().padStart(2, '0')} chaines disponibles",
                    color = SVColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HeaderMetric(icon = Icons.Default.CalendarToday, label = "Aujourd'hui")
            HeaderMetric(icon = Icons.Default.Schedule, label = selected?.timeRange ?: "En direct")
        }
    }
}

@Composable
private fun CategoryRail(
    categories: List<Category>,
    selectedCategoryId: String?,
    onCategory: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    LiveSurface(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
            .padding(12.dp),
        ) {
            Text(
                text = "Categories",
                color = SVColors.TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(10.dp))
            SearchStrip("Rechercher")
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 4.dp),
            ) {
                items(categories, key = { it.id }) { category ->
                    LiveCategoryRow(
                        category = category,
                        selected = category.id == selectedCategoryId,
                        onClick = { onCategory(category) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveCategoryRow(
    category: Category,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FocusableCard(
        onClick = onClick,
        selected = selected,
        accent = SVColors.Cyan,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
    ) { focused ->
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(30.dp)
                    .background(
                        if (selected || focused) SVColors.Cyan else SVColors.Border,
                        RoundedCornerShape(50),
                    ),
            )
            Spacer(Modifier.width(12.dp))
            Icon(
                imageVector = if (category.id == "sport") Icons.Default.EmojiEvents else Icons.Default.Tv,
                contentDescription = null,
                tint = if (selected || focused) SVColors.Cyan else SVColors.TextSecondary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = category.name,
                color = SVColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = category.count.toString(),
                color = if (selected || focused) SVColors.Cyan else SVColors.TextSecondary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun ChannelGuide(
    categoryName: String,
    channels: List<LiveChannel>,
    selectedChannelId: Int?,
    onChannel: (LiveChannel) -> Unit,
    modifier: Modifier = Modifier,
) {
    LiveSurface(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
            .padding(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = categoryName,
                    color = SVColors.TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Badge("${channels.size}", SVColors.Cyan.copy(alpha = 0.42f))
            }
            Spacer(Modifier.height(10.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 4.dp),
            ) {
                items(channels, key = { it.streamId }) { channel ->
                    LiveChannelRow(
                        channel = channel,
                        selected = channel.streamId == selectedChannelId,
                        onClick = { onChannel(channel) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveChannelRow(
    channel: LiveChannel,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FocusableCard(
        onClick = onClick,
        selected = selected,
        accent = SVColors.Cyan,
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp),
        shape = RoundedCornerShape(9.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
    ) { focused ->
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = channel.number.toString().padStart(3, '0'),
                color = if (selected || focused) SVColors.Cyan else SVColors.TextSecondary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                modifier = Modifier.width(34.dp),
            )
            ChannelLogo(
                title = channel.name,
                imageUrl = channel.logoUrl,
                modifier = Modifier.size(width = 46.dp, height = 32.dp),
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    color = SVColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = channel.currentProgram ?: channel.categoryName,
                    color = SVColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(4.dp))
            Column(
                modifier = Modifier.width(48.dp),
                horizontalAlignment = Alignment.End,
            ) {
                if (selected) {
                    Text(
                        text = "LIVE",
                        color = SVColors.Danger,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                    )
                }
                Text(
                    text = channel.timeRange?.shortTimeLabel() ?: "Live",
                    color = if (selected || focused) SVColors.Cyan else SVColors.TextSecondary,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun LivePreviewPanel(
    selected: LiveChannel?,
    onWatch: () -> Unit,
    onFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LiveSurface(modifier = modifier) {
        if (selected == null) {
            EmptyPreview(Modifier.fillMaxSize())
            return@LiveSurface
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
        ) {
            LiveVideoFrame(
                channel = selected,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp),
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChannelLogo(
                    title = selected.name,
                    imageUrl = selected.logoUrl,
                    modifier = Modifier.size(width = 62.dp, height = 38.dp),
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selected.name,
                        color = SVColors.TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = selected.currentProgram ?: selected.categoryName,
                        color = SVColors.TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                LiveDotLabel("ON AIR")
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoTile(
                    icon = Icons.Default.CalendarToday,
                    value = "Auj.",
                    modifier = Modifier.weight(1f),
                )
                InfoTile(
                    icon = Icons.Default.Schedule,
                    value = selected.timeRange?.startTimeLabel() ?: "Live",
                    modifier = Modifier.weight(1f),
                )
                InfoTile(
                    icon = Icons.Default.Tv,
                    value = "HD",
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Maintenant",
                color = SVColors.TextSecondary,
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.height(4.dp))
            ProgressStrip()
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FocusableButton(
                    text = "Regarder",
                    icon = Icons.Default.PlayArrow,
                    onClick = onWatch,
                    accent = SVColors.Cyan,
                    modifier = Modifier
                        .weight(1.35f)
                        .height(36.dp),
                    minHeight = 36.dp,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                )
                FocusableButton(
                    text = "Favori",
                    icon = Icons.Default.Favorite,
                    onClick = onFavorite,
                    accent = SVColors.Warning,
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    minHeight = 36.dp,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun LiveVideoFrame(
    channel: LiveChannel,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SVColors.BackgroundDeep)
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)), RoundedCornerShape(12.dp)),
    ) {
        Image(
            painter = painterResource(R.drawable.home_live_stadium),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0xC001040C), Color(0xEF01040C)),
                    ),
                ),
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Badge("LIVE", SVColors.Danger)
            Badge("HD", Color(0xFF111827))
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(94.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.42f))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(54.dp))
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.currentProgram ?: channel.name,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = channel.timeRange ?: "En direct",
                    color = SVColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            ChannelLogo(
                title = channel.name,
                imageUrl = channel.logoUrl,
                modifier = Modifier.size(width = 76.dp, height = 48.dp),
            )
        }
    }
}

@Composable
private fun ChannelLogo(
    title: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(5.dp),
            )
        } else {
            Text(
                text = title.initials(),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun SearchStrip(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.26f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = SVColors.TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, color = SVColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun HeaderMetric(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
) {
    Row(
        modifier = Modifier
            .height(42.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(Color.Black.copy(alpha = 0.28f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)), RoundedCornerShape(7.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = SVColors.Cyan, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            color = SVColors.TextPrimary,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun InfoTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.26f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.11f)), RoundedCornerShape(8.dp))
            .padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = SVColors.Cyan, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = value,
            color = SVColors.TextPrimary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LiveDotLabel(text: String) {
    Row(
        modifier = Modifier
            .height(28.dp)
            .clip(RoundedCornerShape(50))
            .background(SVColors.Danger.copy(alpha = 0.2f))
            .border(BorderStroke(1.dp, SVColors.Danger.copy(alpha = 0.55f)), RoundedCornerShape(50))
            .padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(SVColors.Danger),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun ProgressStrip() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.16f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.62f)
                .fillMaxHeight()
                .background(
                    Brush.horizontalGradient(
                        listOf(SVColors.Danger, SVColors.Warning, SVColors.Cyan),
                    ),
                ),
        )
    }
}

@Composable
private fun EmptyPreview(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Tv, contentDescription = null, tint = SVColors.TextSecondary, modifier = Modifier.size(54.dp))
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Selectionnez une chaine",
                color = SVColors.TextPrimary,
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

@Composable
private fun LiveSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(13.dp),
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color(0xD8071426))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.13f)), shape),
        content = { content() },
    )
}

private fun String.initials(): String {
    val initials = split(" ", "-", "+")
        .mapNotNull { part -> part.firstOrNull()?.uppercaseChar()?.toString() }
        .take(2)
        .joinToString("")

    return initials.ifBlank { "TV" }
}

private fun String.shortTimeLabel(): String =
    when {
        equals("En direct", ignoreCase = true) -> "Live"
        length > 11 -> take(11)
        else -> this
    }

private fun String.startTimeLabel(): String =
    when {
        equals("En direct", ignoreCase = true) -> "Live"
        contains("-") -> substringBefore("-").trim()
        length > 7 -> take(7)
        else -> this
    }
