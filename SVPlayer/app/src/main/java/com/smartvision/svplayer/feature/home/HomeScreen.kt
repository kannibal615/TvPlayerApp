package com.smartvision.svplayer.feature.home

import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartvision.svplayer.R
import com.smartvision.svplayer.core.designsystem.FocusableButton
import com.smartvision.svplayer.core.designsystem.FocusableCard
import com.smartvision.svplayer.core.designsystem.GlassPanel
import com.smartvision.svplayer.core.designsystem.SVColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun HomeRoute(
    openLive: () -> Unit,
    openMovies: () -> Unit,
    openSeries: () -> Unit,
) {
    var showImages by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(6_500)
        showImages = true
    }

    Column(modifier = Modifier.fillMaxSize()) {
        HeroBanner(showImage = showImages)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Catégories",
            color = SVColors.TextPrimary,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, lineHeight = 26.sp),
            fontWeight = FontWeight.Black,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            HomeCategoryCard(
                title = "LIVE TV",
                subtitle = "Regardez vos chaînes en direct",
                action = "Regarder maintenant",
                icon = Icons.Default.Tv,
                showPlayIcon = true,
                accent = SVColors.Cyan,
                imageRes = R.drawable.home_live_stadium,
                showImage = showImages,
                onClick = openLive,
                modifier = Modifier.weight(1f),
            )
            HomeCategoryCard(
                title = "FILMS",
                subtitle = "Découvrez notre bibliothèque de films",
                action = "Explorer maintenant",
                icon = Icons.Default.Movie,
                accent = SVColors.Purple,
                imageRes = R.drawable.home_movies_cinema,
                showImage = showImages,
                onClick = openMovies,
                modifier = Modifier.weight(1f),
            )
            HomeCategoryCard(
                title = "SÉRIES",
                subtitle = "Explorez vos séries préférées",
                action = "Découvrir maintenant",
                icon = Icons.Default.VideoLibrary,
                showPlayIcon = true,
                accent = SVColors.Blue,
                imageRes = R.drawable.home_series_lounge,
                showImage = showImages,
                onClick = openSeries,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HeroBanner(showImage: Boolean) {
    val shape = RoundedCornerShape(8.dp)
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .height(152.dp),
        shape = shape,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (showImage) {
                ResourceImage(
                    imageRes = R.drawable.home_hero_earth,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFF123C61), Color(0xFF061426), Color(0xFF020817)),
                            ),
                        ),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF020817).copy(alpha = 0.88f),
                                Color(0xFF020817).copy(alpha = 0.44f),
                                Color.Transparent,
                            ),
                            endX = 1100f,
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xFF020817).copy(alpha = 0.58f)),
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 24.dp, top = 19.dp),
            ) {
                AnnouncementBadge()
                Spacer(Modifier.height(17.dp))
                Text(
                    text = "Annonces SmartVision",
                    color = SVColors.TextPrimary,
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 28.sp, lineHeight = 34.sp),
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = "Publicités, informations et nouveautés pour votre écran TV.",
                    color = Color.White.copy(alpha = 0.88f),
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp, lineHeight = 20.sp),
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 13.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                repeat(4) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == 0) 7.dp else 6.dp)
                            .background(
                                if (index == 0) Color.White else SVColors.TextSecondary.copy(alpha = 0.55f),
                                RoundedCornerShape(50),
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun AnnouncementBadge() {
    val shape = RoundedCornerShape(4.dp)
    Text(
        text = "ANNONCE",
        color = SVColors.Cyan,
        style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp, lineHeight = 14.sp),
        fontWeight = FontWeight.Black,
        modifier = Modifier
            .clip(shape)
            .background(Color(0xFF001E24).copy(alpha = 0.72f))
            .border(BorderStroke(1.dp, SVColors.Cyan), shape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun HomeCategoryCard(
    title: String,
    subtitle: String,
    action: String,
    icon: ImageVector,
    accent: Color,
    @DrawableRes imageRes: Int,
    showImage: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showPlayIcon: Boolean = false,
) {
    val shape = RoundedCornerShape(8.dp)
    FocusableCard(
        onClick = onClick,
        accent = accent,
        modifier = modifier.height(230.dp),
        shape = shape,
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(BorderStroke(1.dp, accent.copy(alpha = 0.42f)), shape),
        ) {
            if (showImage) {
                ResourceImage(
                    imageRes = imageRes,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.32f), SVColors.BackgroundDeep))),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color(0xFF020817).copy(alpha = 0.28f),
                                Color(0xFF020817).copy(alpha = 0.88f),
                            ),
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF020817).copy(alpha = 0.22f), Color.Transparent, Color(0xFF020817).copy(alpha = 0.35f)),
                        ),
                    ),
            )
            CategoryIcon(
                icon = icon,
                accent = accent,
                showPlayIcon = showPlayIcon,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 32.dp),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 24.dp, end = 24.dp)
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    title,
                    color = Color.White,
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 28.sp, lineHeight = 32.sp),
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    subtitle,
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, lineHeight = 16.sp),
                    maxLines = 1,
                )
                Spacer(Modifier.height(12.dp))
                FocusableButton(
                    text = action,
                    onClick = onClick,
                    accent = accent,
                    selected = true,
                    modifier = Modifier
                        .fillMaxWidth(0.76f)
                        .height(32.dp),
                    minHeight = 32.dp,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(50),
                )
            }
        }
    }
}

@Composable
private fun ResourceImage(
    @DrawableRes imageRes: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var imageBitmap by remember(imageRes) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(imageRes) {
        imageBitmap = withContext(Dispatchers.IO) {
            context.resources.openRawResource(imageRes).use { input ->
                BitmapFactory.decodeStream(input)?.asImageBitmap()
            }
        }
    }

    imageBitmap?.let { bitmap ->
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    }
}

@Composable
private fun CategoryIcon(
    icon: ImageVector,
    accent: Color,
    showPlayIcon: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape: Shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .size(82.dp)
            .clip(shape)
            .background(
                Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.18f), accent.copy(alpha = 0.18f), Color.Transparent),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(58.dp),
        )
        if (showPlayIcon) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(34.dp),
            )
        }
    }
}
