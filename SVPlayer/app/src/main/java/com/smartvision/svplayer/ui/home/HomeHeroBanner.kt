package com.smartvision.svplayer.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartvision.svplayer.R
import com.smartvision.svplayer.ui.components.TvButton
import com.smartvision.svplayer.ui.components.TvButtonVariant
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionDimensions
import com.smartvision.svplayer.ui.theme.SmartVisionType
import kotlinx.coroutines.delay

@Composable
fun HomeHeroBanner(
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val slides = remember { HomeHeroSlides }
    var slideIndex by remember { mutableIntStateOf(0) }
    val slide = slides[slideIndex]
    val shape = RoundedCornerShape(SmartVisionDimensions.HomePanelRadius)

    LaunchedEffect(slideIndex) {
        delay(5_500)
        slideIndex = (slideIndex + 1) % slides.size
    }

    Box(
        modifier = modifier
            .height(SmartVisionDimensions.HomeHeroHeight)
            .clip(shape)
            .background(SmartVisionColors.Surface)
            .border(BorderStroke(1.dp, SmartVisionColors.Border.copy(alpha = 0.82f)), shape),
    ) {
        Image(
            painter = painterResource(slide.imageRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF051020).copy(alpha = 0.98f),
                            Color(0xFF051020).copy(alpha = 0.64f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.08f),
                            Color.Black.copy(alpha = 0.18f),
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 24.dp, end = 430.dp),
        ) {
            Text(
                text = slide.title,
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.HomeHeroTitle,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Spacer(Modifier.height(7.dp))
            Text(
                text = slide.subtitle,
                color = SmartVisionColors.TextSecondary,
                style = SmartVisionType.Label,
                maxLines = 2,
            )
            Spacer(Modifier.height(13.dp))
            TvButton(
                text = "En savoir plus",
                onClick = { onNavigate(slide.route) },
                variant = TvButtonVariant.Primary,
                modifier = Modifier.height(38.dp),
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            slides.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .size(width = if (index == slideIndex) 24.dp else 8.dp, height = 5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (index == slideIndex) {
                                SmartVisionColors.Primary
                            } else {
                                Color.White.copy(alpha = 0.34f)
                            },
                        ),
                )
            }
        }
    }
}

private data class HomeHeroSlide(
    val imageRes: Int,
    val title: String,
    val subtitle: String,
    val route: String,
)

private val HomeHeroSlides = listOf(
    HomeHeroSlide(
        imageRes = R.drawable.home_hero_slide_1,
        title = "Bienvenue sur SmartVision",
        subtitle = "Une experience IPTV fluide, premium et pensee pour Android TV.",
        route = "live_tv",
    ),
    HomeHeroSlide(
        imageRes = R.drawable.home_hero_slide_2,
        title = "Live TV instantanee",
        subtitle = "Retrouvez vos chaines en direct avec une navigation simple a la telecommande.",
        route = "live_tv",
    ),
    HomeHeroSlide(
        imageRes = R.drawable.home_hero_slide_3,
        title = "Films et series",
        subtitle = "Explorez vos catalogues Xtream avec affiches, details et reprise de lecture.",
        route = "movies",
    ),
)
