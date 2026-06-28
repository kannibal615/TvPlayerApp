package com.smartvision.svplayer.ui.home

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.smartvision.svplayer.BuildConfig
import com.smartvision.svplayer.R
import com.smartvision.svplayer.data.home.HomeSlide
import com.smartvision.svplayer.ui.components.TvButton
import com.smartvision.svplayer.ui.components.TvButtonVariant
import com.smartvision.svplayer.ui.profile.SmartVisionQrDialog
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionDimensions
import com.smartvision.svplayer.ui.theme.SmartVisionType
import kotlinx.coroutines.delay

@Composable
fun HomeHeroBanner(
    remoteSlides: List<HomeSlide>,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val slides = remember(remoteSlides) {
        remoteSlides.takeIf { it.isNotEmpty() }
            ?.mapIndexed { index, slide -> slide.toHeroSlide(index) }
            ?: DefaultHomeHeroSlides
    }
    var slideIndex by remember { mutableIntStateOf(0) }
    var selectedOffer by remember { mutableStateOf<HomeHeroSlide?>(null) }
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
                            Color(0xFF051020).copy(alpha = 0.98f),
                            Color(0xFF051020).copy(alpha = 0.64f),
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
            Spacer(Modifier.height(13.dp))
            TvButton(
                text = slide.buttonLabel.ifBlank { "Voir l'offre" },
                onClick = { selectedOffer = slide },
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
                            if (index == slideIndex) SmartVisionColors.Primary
                            else Color.White.copy(alpha = 0.34f),
                        ),
                )
            }
        }
    }

    selectedOffer?.let { offer ->
        val offerUrl = offer.offerUrl()
        SmartVisionQrDialog(
            title = offer.title,
            subtitle = offer.subtitle,
            qrUrl = offerUrl,
            width = 720.dp,
            actionLabel = "Recevoir l'offre par e-mail",
            onAction = {
                val subject = Uri.encode("Offre SmartVision - ${offer.title}")
                val body = Uri.encode("Bonjour,\n\nJe souhaite recevoir cette offre SmartVision : $offerUrl")
                val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:?subject=$subject&body=$body"))
                try {
                    context.startActivity(emailIntent)
                } catch (_: ActivityNotFoundException) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(offerUrl)))
                }
            },
            onDismiss = { selectedOffer = null },
        )
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

private fun HomeSlide.toHeroSlide(index: Int): HomeHeroSlide = HomeHeroSlide(
    imageRes = DefaultHomeHeroSlides[index % DefaultHomeHeroSlides.size].imageRes,
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

private fun HomeHeroSlide.offerUrl(): String = when {
    route.startsWith("http://") || route.startsWith("https://") -> route
    else -> BuildConfig.ACTIVATION_BASE_URL.trimEnd('/') +
        "/account/?source=tv&intent=offer&offer=" + Uri.encode(title)
}

private val DefaultHomeHeroSlides = listOf(
    HomeHeroSlide(R.drawable.home_hero_slide_1, "", "Welcome to SmartVision", "A smooth premium IPTV player experience built for Android TV.", "Learn more", "live_tv"),
    HomeHeroSlide(R.drawable.home_hero_slide_2, "", "Instant Live TV", "Watch your live channels with simple remote-friendly navigation.", "View offer", "live_tv"),
    HomeHeroSlide(R.drawable.home_hero_slide_3, "", "Movies and series", "Explore your Xtream catalogs with posters, details and resume playback.", "Discover", "movies"),
)
