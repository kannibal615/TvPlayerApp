package com.smartvision.svplayer.feature.home

import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartvision.svplayer.R
import com.smartvision.svplayer.core.designsystem.Badge
import com.smartvision.svplayer.core.designsystem.FocusableButton
import com.smartvision.svplayer.core.designsystem.FocusableCard
import com.smartvision.svplayer.core.designsystem.GlassPanel
import com.smartvision.svplayer.core.designsystem.SVColors
import com.smartvision.svplayer.core.designsystem.SVPlayerTheme
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
        delay(1_500)
        showImages = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        HeroBanner(showImage = showImages)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            LiveTvCard(
                onClick = openLive,
                showImage = showImages,
                modifier = Modifier.weight(1.4f)
            )
            CategorySmallCard(
                title = "Films",
                subtitle = "Découvrez des films incontournables",
                badge = "VOD",
                badgeColor = Color(0xFFFACC15), // Yellow/Gold for VOD
                imageRes = R.drawable.home_movies_cinema,
                showImage = showImages,
                onClick = openMovies,
                modifier = Modifier.weight(1f)
            )
            CategorySmallCard(
                title = "Séries",
                subtitle = "Suivez vos séries préférées",
                badge = "SÉRIES",
                badgeColor = SVColors.Blue,
                imageRes = R.drawable.home_series_lounge,
                showImage = showImages,
                onClick = openSeries,
                modifier = Modifier.weight(1f)
            )
        }

        ResumeSection()

        Column {
            Text(
                text = "Tendances",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun HeroBanner(showImage: Boolean) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(shape)
            .background(Color(0xFF071426))
    ) {
        if (showImage) {
            ResourceImage(
                imageRes = R.drawable.home_hero_earth,
                modifier = Modifier.fillMaxSize(),
            )
        }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF020817), Color.Transparent),
                        startX = 0f,
                        endX = 1000f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Bienvenue sur SmartVision",
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp),
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Profitez du meilleur du divertissement avec une\nqualité de streaming exceptionnelle.",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                HeroInfoBadge(Icons.Default.HighQuality, "Haute qualité")
                HeroInfoBadge(Icons.Default.Cloud, "Streaming stable")
                HeroInfoBadge(Icons.Default.Security, "Sécurisé & privé")
            }
        }
    }
}

@Composable
private fun HeroInfoBadge(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SVColors.Blue,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.8f),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun LiveTvCard(
    onClick: () -> Unit,
    showImage: Boolean,
    modifier: Modifier = Modifier,
) {
    FocusableCard(
        onClick = onClick,
        accent = SVColors.Blue,
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(0.dp)
    ) { focused ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (showImage) {
                ResourceImage(
                    imageRes = R.drawable.home_live_stadium,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Black.copy(alpha = 0.9f), Color.Transparent)
                        )
                    )
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Badge("LIVE", SVColors.Blue)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Live TV",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "Regardez vos chaînes\nen direct",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                FocusableButton(
                    text = "Voir maintenant",
                    icon = Icons.Default.Tv,
                    onClick = onClick,
                    accent = Color.White,
                    selected = focused,
                    modifier = Modifier.height(40.dp),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }
    }
}

@Composable
private fun CategorySmallCard(
    title: String,
    subtitle: String,
    badge: String,
    badgeColor: Color,
    @DrawableRes imageRes: Int,
    showImage: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusableCard(
        onClick = onClick,
        accent = badgeColor,
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(0.dp)
    ) { focused ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (showImage) {
                ResourceImage(
                    imageRes = imageRes,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Badge(badge, badgeColor)
                
                Column {
                    Text(
                        title,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        subtitle,
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(12.dp))
                    FocusableButton(
                        text = "Explorer",
                        icon = Icons.Default.VideoLibrary,
                        onClick = onClick,
                        accent = Color.White,
                        selected = focused,
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ResumeSection() {
    Column {
        Text(
            text = "Reprendre la lecture",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ViewAllButton()
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(5) { index ->
                    ResumeCard(index)
                }
            }
            
            Icon(
                Icons.Default.KeyboardArrowRight,
                null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun ViewAllButton() {
    var focused by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .size(width = 60.dp, height = 110.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(BorderStroke(1.dp, if (focused) SVColors.Cyan else Color.White.copy(alpha = 0.1f)), RoundedCornerShape(12.dp))
            .onFocusChanged { focused = it.isFocused }
            .clickable { }
            .focusable(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text("Tout", color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ResumeCard(index: Int) {
    val titles = listOf("THE LAST OF US", "FAST & FURIOUS 10", "DUNE PARTIE 2", "STRANGER THINGS", "KUNG FU PANDA 4")
    val progress = listOf(0.4f, 0.7f, 0.3f, 0.9f, 0.5f)
    
    var focused by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .width(200.dp)
            .onFocusChanged { focused = it.isFocused }
            .clickable { }
            .focusable()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Gray)
                .border(BorderStroke(if (focused) 2.dp else 0.dp, SVColors.Cyan), RoundedCornerShape(12.dp))
        ) {
            // Placeholder for image
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1E293B)))
            
            // Progress bar
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress[index % titles.size])
                        .fillMaxHeight()
                        .background(SVColors.Blue)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            titles[index % titles.size],
            color = Color.White,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("S1 E5", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
            Text("45 min restantes", color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
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

@Preview(device = "id:tv_1080p")
@Composable
fun HomeScreenPreview() {
    SVPlayerTheme {
        HomeRoute(
            openLive = {},
            openMovies = {},
            openSeries = {}
        )
    }
}
