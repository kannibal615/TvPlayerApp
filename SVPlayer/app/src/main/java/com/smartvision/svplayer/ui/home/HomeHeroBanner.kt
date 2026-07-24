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
    kidsMode: Boolean = false,
    profileKey: String = "",
    modifier: Modifier = Modifier,
) {
    val localizedSlides = remember(strings) { defaultHomeHeroSlides(strings) }
    val slides = remember(remoteSlides, localizedSlides, kidsMode) {
        remoteSlides.mapIndexed { index, slide ->
            slide.toHeroSlide(localizedSlides[index % localizedSlides.size])
        }
    }
    val shape = RoundedCornerShape(SmartVisionDimensions.HomePanelRadius)
    if (slides.isEmpty()) {
        Box(
            modifier = modifier
                .height(SmartVisionDimensions.HomeHeroHeight)
                .clip(shape)
                .background(Color.Transparent),
        )
        return
    }
    var slideIndex by remember(profileKey, kidsMode) { mutableIntStateOf(0) }
    val slide = slides[slideIndex.coerceIn(slides.indices)]
    val hasCopy = slide.title.isNotBlank() || slide.subtitle.isNotBlank()

    LaunchedEffect(profileKey, kidsMode, slides.size) {
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
                placeholder = painterResource(slide.imageRes),
                error = painterResource(slide.imageRes),
                fallback = painterResource(slide.imageRes),
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (hasCopy) {
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
                if (slide.title.isNotBlank()) {
                    Text(
                        text = slide.title,
                        color = SmartVisionColors.TextPrimary,
                        style = SmartVisionType.HomeHeroTitle,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
                if (slide.title.isNotBlank() && slide.subtitle.isNotBlank()) {
                    Spacer(Modifier.height(7.dp))
                }
                if (slide.subtitle.isNotBlank()) {
                    Text(
                        text = slide.subtitle,
                        color = SmartVisionColors.TextSecondary,
                        style = SmartVisionType.Label,
                        maxLines = 2,
                    )
                }
            }
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

private fun HomeSlide.toHeroSlide(localizedFallback: HomeHeroSlide): HomeHeroSlide = HomeHeroSlide(
    imageRes = localizedFallback.imageRes,
    imageUrl = imageUrl.toAbsoluteAssetUrl(),
    title = title.trim(),
    subtitle = subtitle.trim(),
    buttonLabel = buttonLabel.trim(),
    route = buttonRoute.ifBlank { localizedFallback.route },
)

private fun String.toAbsoluteAssetUrl(): String = when {
    isBlank() -> ""
    startsWith("http://") || startsWith("https://") || startsWith("file:") -> this
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
