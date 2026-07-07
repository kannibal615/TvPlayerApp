package com.smartvision.svplayer.ui.detail

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.smartvision.svplayer.data.tmdb.TmdbPersonCredit
import com.smartvision.svplayer.data.tmdb.TmdbRecommendation
import com.smartvision.svplayer.data.tmdb.TmdbVideoItem
import com.smartvision.svplayer.ui.focus.rememberTvFocusState
import com.smartvision.svplayer.ui.focus.tvFocusTarget
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionDimensions
import com.smartvision.svplayer.ui.youtube.YoutubePlaybackMode
import com.smartvision.svplayer.ui.youtube.YoutubeWebPlayer

@Composable
fun DetailVideoSection(
    videos: List<TmdbVideoItem>,
    modifier: Modifier = Modifier,
) {
    if (videos.isEmpty()) return
    var selectedIndex by remember(videos) { mutableIntStateOf(0) }
    val selected = videos.getOrNull(selectedIndex) ?: videos.first()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .detailPanel()
            .padding(16.dp),
    ) {
        Text("Trailers & teasers", color = SmartVisionColors.TextPrimary, style = DetailTitleStyle)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                modifier = Modifier
                    .width(620.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black)
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)), RoundedCornerShape(8.dp)),
            ) {
                YoutubeWebPlayer(
                    videoId = selected.key,
                    mode = YoutubePlaybackMode.Preview,
                    keyboardControlsEnabled = false,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selected.name,
                    color = SmartVisionColors.TextPrimary,
                    style = DetailBodyStyle,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    DetailBadge(selected.type)
                    if (selected.official) DetailBadge("Official", color = Color(0xFF1F3B29))
                    selected.language?.let { DetailBadge(it.uppercase(), color = Color(0xFF18253A)) }
                }
                Spacer(Modifier.height(14.dp))
                videos.take(5).forEachIndexed { index, video ->
                    DetailActionButton(
                        text = video.type.ifBlank { "Video" },
                        icon = Icons.Default.PlayArrow,
                        onClick = { selectedIndex = index },
                        selected = index == selectedIndex,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .padding(bottom = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun DetailPeopleSection(
    title: String,
    people: List<TmdbPersonCredit>,
    modifier: Modifier = Modifier,
) {
    if (people.isEmpty()) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .detailPanel()
            .padding(16.dp),
    ) {
        Text(title, color = SmartVisionColors.TextPrimary, style = DetailTitleStyle)
        Spacer(Modifier.height(12.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = 18.dp),
        ) {
            items(people, key = { "${it.name}:${it.role.orEmpty()}" }) { person ->
                PersonCreditCard(person = person)
            }
        }
    }
}

@Composable
private fun PersonCreditCard(person: TmdbPersonCredit) {
    val shape = RoundedCornerShape(7.dp)
    Column(
        modifier = Modifier
            .width(116.dp)
            .height(178.dp)
            .clip(shape)
            .background(Color(0xB0101A2B))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), shape),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(126.dp)
                .background(Color.Black.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center,
        ) {
            if (!person.profileUrl.isNullOrBlank()) {
                AsyncImage(
                    model = person.profileUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(Icons.Default.Person, contentDescription = null, tint = SmartVisionColors.TextSecondary, modifier = Modifier.size(34.dp))
            }
        }
        Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Text(
                text = person.name,
                color = SmartVisionColors.TextPrimary,
                style = DetailMetaStyle,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = person.role.orEmpty(),
                color = SmartVisionColors.TextSecondary,
                style = DetailMetaStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun DetailRecommendationsSection(
    recommendations: List<TmdbRecommendation>,
    modifier: Modifier = Modifier,
) {
    if (recommendations.isEmpty()) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .detailPanel()
            .padding(16.dp),
    ) {
        Text("Recommendations", color = SmartVisionColors.TextPrimary, style = DetailTitleStyle)
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(recommendations, key = { it.tmdbId }) { item ->
                RecommendationCard(item = item)
            }
        }
    }
}

@Composable
private fun RecommendationCard(item: TmdbRecommendation) {
    val shape = RoundedCornerShape(7.dp)
    Box(
        modifier = Modifier
            .width(128.dp)
            .height(186.dp)
            .clip(shape)
            .background(Color(0xB0101A2B))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), shape),
    ) {
        val imageUrl = item.posterUrl ?: item.backdropUrl
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.84f)))),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(9.dp),
        ) {
            Text(
                text = item.title,
                color = Color.White,
                style = DetailMetaStyle,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val meta = listOfNotNull(item.releaseDate?.take(4), item.voteAverage?.let { String.format(java.util.Locale.US, "%.1f", it) })
                .joinToString("  ")
            if (meta.isNotBlank()) {
                Text(meta, color = Color.White.copy(alpha = 0.72f), style = DetailMetaStyle, maxLines = 1)
            }
        }
    }
}

@Composable
fun DetailUserRatingSection(
    contentKey: String,
    tmdbRating: String?,
    voteCount: Int?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences("smartvision_detail_user_ratings", Context.MODE_PRIVATE)
    }
    var rating by remember(contentKey) { mutableIntStateOf(prefs.getInt(contentKey, 0)) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .detailPanel()
            .padding(16.dp),
    ) {
        Text("Rating", color = SmartVisionColors.TextPrimary, style = DetailTitleStyle)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "TMDB ${tmdbRating?.let { "$it/10" } ?: "-"}" + (voteCount?.let { "  $it votes" } ?: ""),
                color = SmartVisionColors.TextSecondary,
                style = DetailBodyStyle,
                modifier = Modifier.width(210.dp),
            )
            (1..5).forEach { value ->
                RatingStarButton(
                    value = value,
                    selected = value <= rating,
                    onClick = {
                        rating = value
                        prefs.edit().putInt(contentKey, value).apply()
                    },
                )
            }
            if (rating > 0) {
                Text("Your rating: $rating/5", color = SmartVisionColors.TextPrimary, style = DetailBodyStyle)
            }
        }
    }
}

@Composable
private fun RatingStarButton(
    value: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val focusState = rememberTvFocusState()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val color = if (selected) Color(0xFFFFC857) else SmartVisionColors.TextSecondary
    Box(
        modifier = Modifier
            .size(38.dp)
            .tvFocusTarget(
                state = focusState,
                pressed = pressed,
                focusedScale = 1.1f,
                glowColor = Color(0xFFFFC857),
                cornerRadius = 6.dp,
            )
            .clip(RoundedCornerShape(6.dp))
            .background(if (focusState.isFocused) Color(0xFF273247) else Color(0x65101A2B))
            .border(BorderStroke(if (focusState.isFocused) 2.dp else 1.dp, if (focusState.isFocused) SmartVisionColors.FocusWhite else Color.White.copy(alpha = 0.10f)), RoundedCornerShape(6.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Default.Star, contentDescription = "Rate $value", tint = color, modifier = Modifier.size(22.dp))
    }
}

private fun Modifier.detailPanel(): Modifier =
    clip(RoundedCornerShape(9.dp))
        .background(Color(0x98101A2B))
        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.13f)), RoundedCornerShape(9.dp))
