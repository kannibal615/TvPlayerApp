package com.smartvision.svplayer.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import coil.compose.AsyncImage
import com.smartvision.svplayer.BuildConfig
import com.smartvision.svplayer.R
import com.smartvision.svplayer.data.home.HomeSlide
import com.smartvision.svplayer.ui.i18n.SmartVisionStrings
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionDimensions
import com.smartvision.svplayer.ui.theme.SmartVisionType
import kotlinx.coroutines.delay

@Composable
fun HomeHeroBanner(
    strings: SmartVisionStrings,
    remoteSlides: List<HomeSlide>,
    modifier: Modifier = Modifier,
) {
    val fallbackSlides = remember(strings) { defaultHomeHeroSlides(strings) }
    val slides = remember(remoteSlides, fallbackSlides) {
        remoteSlides.takeIf { it.isNotEmpty() }
            ?.mapIndexed { index, slide -> slide.toHeroSlide(index, fallbackSlides) }
            ?: fallbackSlides
    }
    var slideIndex by remember { mutableIntStateOf(0) }
    val slide = slides[slideIndex.coerceIn(slides.indices)]
    val shape = RoundedCornerShape(SmartVisionDimensions.HomePanelRadius)

    LaunchedEffect(slides.size) {
        if (slideIndex !in slides.indices) slideIndex = 0
    }
    LaunchedEffect(slideIndex, slides.size) {
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
        if (slide.imageUrl.isNotBlank()) {
            AsyncImage(
                model = slide.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Image(
                painter = painterResource(slide.imageRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF051020).copy(alpha = 0.88f),
                            Color(0xFF051020).copy(alpha = 0.46f),
                            Color.Transparent,
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
        }

        androidx.compose.foundation.layout.Row(
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
                            if (index == slideIndex) SmartVisionColors.Primary
                            else Color.White.copy(alpha = 0.34f),
                        ),
                )
            }
        }
    }

}

private data class HomeHeroSlide(
    val imageRes: Int,
    val imageUrl: String,
    val title: String,
    val subtitle: String,
    val buttonLabel: String,
    val route: String,
)

private fun HomeSlide.toHeroSlide(index: Int, fallbackSlides: List<HomeHeroSlide>): HomeHeroSlide = HomeHeroSlide(
    imageRes = fallbackSlides[index % fallbackSlides.size].imageRes,
    imageUrl = imageUrl.toAbsoluteAssetUrl(),
    title = title,
    subtitle = subtitle,
    buttonLabel = buttonLabel,
    route = buttonRoute,
)

private fun String.toAbsoluteAssetUrl(): String = when {
    isBlank() -> ""
    startsWith("http://") || startsWith("https://") -> this
    else -> BuildConfig.ACTIVATION_BASE_URL.trimEnd('/') + "/" + trimStart('/')
}

private fun defaultHomeHeroSlides(strings: SmartVisionStrings) = listOf(
    HomeHeroSlide(
        R.drawable.home_hero_slide_1,
        "",
        strings.homeHeroWelcomeTitle,
        strings.homeHeroWelcomeSubtitle,
        strings.homeHeroLearnMore,
        "live_tv",
    ),
    HomeHeroSlide(
        R.drawable.home_hero_slide_2,
        "",
        strings.homeHeroLiveTitle,
        strings.homeHeroLiveSubtitle,
        strings.homeHeroViewOffer,
        "live_tv",
    ),
    HomeHeroSlide(
        R.drawable.home_hero_slide_3,
        "",
        strings.homeHeroCatalogTitle,
        strings.homeHeroCatalogSubtitle,
        strings.homeHeroDiscover,
        "movies",
    ),
)
