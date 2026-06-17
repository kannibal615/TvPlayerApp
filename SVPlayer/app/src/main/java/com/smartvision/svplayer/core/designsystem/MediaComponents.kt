package com.smartvision.svplayer.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.smartvision.svplayer.domain.model.Category

@Composable
fun SearchFieldPlaceholder(
    text: String,
    modifier: Modifier = Modifier,
) {
    GlassPanel(modifier = modifier.height(52.dp), shape = RoundedCornerShape(9.dp)) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = SVColors.TextSecondary)
            Spacer(Modifier.width(12.dp))
            Text(text, color = SVColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun CategoryRow(
    category: Category,
    selected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusableCard(
        onClick = onClick,
        selected = selected,
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp),
        shape = RoundedCornerShape(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = if (selected || it) SVColors.Cyan else SVColors.TextSecondary)
            Spacer(Modifier.width(16.dp))
            Text(
                text = category.name,
                color = SVColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = category.count.toString(),
                color = SVColors.TextSecondary,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
fun MediaListItem(
    number: String,
    title: String,
    subtitle: String,
    imageUrl: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = SVColors.Cyan,
    trailing: String? = null,
) {
    FocusableCard(
        onClick = onClick,
        selected = selected,
        accent = accent,
        modifier = modifier
            .fillMaxWidth()
            .height(66.dp),
        shape = RoundedCornerShape(9.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp),
    ) { focused ->
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                number,
                color = SVColors.TextSecondary,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.width(48.dp),
            )
            MediaThumb(imageUrl = imageUrl, modifier = Modifier.size(width = 74.dp, height = 42.dp))
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = SVColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    color = SVColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (trailing != null) {
                Text(
                    trailing,
                    color = if (selected || focused) accent else SVColors.TextSecondary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (focused) {
                Spacer(Modifier.width(12.dp))
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = accent, modifier = Modifier.size(26.dp))
            }
        }
    }
}

@Composable
fun PreviewPanel(
    title: String,
    subtitle: String,
    description: String,
    imageUrl: String?,
    status: String,
    primaryAction: String,
    onPrimary: () -> Unit,
    onFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = SVColors.Cyan,
    metadata: List<Pair<ImageVector, String>> = emptyList(),
) {
    GlassPanel(modifier = modifier, shape = RoundedCornerShape(14.dp)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
        ) {
            HeroPreviewImage(imageUrl = imageUrl, accent = accent)
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MediaThumb(imageUrl = imageUrl, modifier = Modifier.size(74.dp))
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = SVColors.TextPrimary, style = MaterialTheme.typography.headlineMedium, maxLines = 2)
                    Text(subtitle, color = SVColors.TextSecondary, style = MaterialTheme.typography.bodyLarge, maxLines = 2)
                }
                Text(status, color = accent, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(18.dp))
            if (metadata.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    metadata.take(3).forEach { (icon, label) ->
                        InfoPill(icon = icon, label = label, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            Text(
                text = description,
                color = SVColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FocusableButton(
                    text = primaryAction,
                    icon = Icons.Default.PlayArrow,
                    onClick = onPrimary,
                    accent = accent,
                    modifier = Modifier.weight(1f),
                )
                FocusableButton(
                    text = "Favori",
                    icon = Icons.Default.Favorite,
                    onClick = onFavorite,
                    accent = SVColors.Purple,
                    modifier = Modifier.weight(1f),
                )
                FocusableButton(
                    text = "Infos",
                    icon = Icons.Default.Info,
                    onClick = {},
                    accent = SVColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
fun MediaThumb(
    imageUrl: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(7.dp))
            .background(
                Brush.linearGradient(
                    listOf(SVColors.SurfaceLight, SVColors.Blue.copy(alpha = 0.45f), SVColors.Purple.copy(alpha = 0.35f)),
                ),
            )
            .border(BorderStroke(1.dp, SVColors.Border), RoundedCornerShape(7.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = SVColors.TextPrimary, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun HeroPreviewImage(
    imageUrl: String?,
    accent: Color,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.radialGradient(
                    listOf(accent.copy(alpha = 0.4f), SVColors.SurfaceLight, SVColors.BackgroundDeep),
                ),
            )
            .border(BorderStroke(1.dp, SVColors.Border), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.BottomStart,
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Badge("LIVE", SVColors.Danger)
            Badge("HD", Color(0xFF111827))
        }
    }
}

@Composable
fun Badge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = Color.White,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Black,
        modifier = modifier
            .background(color, RoundedCornerShape(5.dp))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)), RoundedCornerShape(5.dp))
            .padding(horizontal = 7.dp, vertical = 4.dp),
    )
}

@Composable
fun InfoPill(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(58.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SVColors.Surface.copy(alpha = 0.55f))
            .border(BorderStroke(1.dp, SVColors.Border), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = SVColors.TextSecondary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = SVColors.TextSecondary, style = MaterialTheme.typography.labelMedium, maxLines = 2)
    }
}
