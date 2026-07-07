package com.smartvision.svplayer.ui.media

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.smartvision.svplayer.core.data.LocalAppContainer
import com.smartvision.svplayer.data.private_media.PrivateMediaDetailsDto
import com.smartvision.svplayer.data.private_media.PrivateMediaPlaybackResponse
import com.smartvision.svplayer.ui.components.TvButton
import com.smartvision.svplayer.ui.components.TvButtonVariant
import com.smartvision.svplayer.ui.i18n.SmartVisionStrings
import com.smartvision.svplayer.ui.theme.SmartVisionColors
import com.smartvision.svplayer.ui.theme.SmartVisionType

@Composable
fun PrivateMediaDetailRoute(
    itemId: String,
    strings: SmartVisionStrings,
    onBack: () -> Unit,
    onPlay: (String) -> Unit = {},
) {
    val repository = LocalAppContainer.current.privateMediaRepository
    var loading by remember(itemId) { mutableStateOf(true) }
    var item by remember(itemId) { mutableStateOf<PrivateMediaDetailsDto?>(null) }
    var error by remember(itemId) { mutableStateOf<String?>(null) }

    LaunchedEffect(itemId) {
        loading = true
        error = null
        item = repository.loadDetails(itemId)
            .onFailure { error = it.message ?: strings.mediaPlaybackUnavailable }
            .getOrNull()
        loading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(SmartVisionColors.PrimaryDark.copy(alpha = 0.36f), SmartVisionColors.Background),
                    radius = 1200f,
                ),
            )
            .padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        TvButton(
            text = strings.back,
            onClick = onBack,
            modifier = Modifier.height(40.dp),
        )
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SmartVisionColors.CyanAccent)
            }
            item == null -> PrivateMediaDetailMessage(error ?: strings.mediaPrivateEmpty)
            else -> PrivateMediaDetailContent(strings = strings, item = item!!, onPlay = { onPlay(itemId) })
        }
    }
}

@Composable
private fun PrivateMediaDetailContent(
    strings: SmartVisionStrings,
    item: PrivateMediaDetailsDto,
    onPlay: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(0.42f)
                .fillMaxWidth()
                .height(360.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black.copy(alpha = 0.45f))
                .border(BorderStroke(1.dp, SmartVisionColors.Border), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (!item.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(Icons.Default.Theaters, contentDescription = null, tint = SmartVisionColors.TextSecondary, modifier = Modifier.size(72.dp))
            }
        }
        Column(
            modifier = Modifier.weight(0.58f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = item.title,
                color = SmartVisionColors.TextPrimary,
                style = SmartVisionType.TitleL,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            DetailRow(strings.mediaDuration, item.durationLabel.orEmpty().ifBlank { "-" })
            DetailRow(strings.mediaPrivateViews, item.views?.let { "%,d".format(java.util.Locale.US, it) } ?: "-")
            DetailRow(strings.mediaPrivateRating, item.rating?.let { "%.1f".format(java.util.Locale.US, it) } ?: "-")
            DetailRow(strings.mediaPlaybackType, item.playbackType)
            if (item.tags.isNotEmpty()) {
                Text(
                    text = item.tags.take(10).joinToString("  "),
                    color = SmartVisionColors.CyanAccent,
                    style = SmartVisionType.Label,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(6.dp))
            TvButton(
                text = if (item.isPlayable) strings.mediaPlay else strings.mediaPlaybackUnavailable,
                onClick = onPlay,
                enabled = item.isPlayable || !item.embedUrl.isNullOrBlank(),
                leadingIcon = Icons.Default.PlayArrow,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
            )
        }
    }
}

@Composable
fun PrivateMediaPlayerRoute(
    itemId: String,
    strings: SmartVisionStrings,
    onBack: () -> Unit,
) {
    val repository = LocalAppContainer.current.privateMediaRepository
    var loading by remember(itemId) { mutableStateOf(true) }
    var playback by remember(itemId) { mutableStateOf<PrivateMediaPlaybackResponse?>(null) }
    var error by remember(itemId) { mutableStateOf<String?>(null) }

    LaunchedEffect(itemId) {
        loading = true
        error = null
        playback = repository.loadPlayback(itemId)
            .onFailure { error = it.message ?: strings.mediaPlaybackUnavailable }
            .getOrNull()
        loading = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        when {
            loading -> CircularProgressIndicator(
                color = SmartVisionColors.CyanAccent,
                modifier = Modifier.align(Alignment.Center),
            )
            playback == null -> PrivateMediaPlayerMessage(error ?: strings.mediaPlaybackUnavailable)
            else -> PrivateMediaPlayerSurface(playback = playback!!, strings = strings)
        }
        TvButton(
            text = strings.back,
            onClick = onBack,
            variant = TvButtonVariant.Secondary,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(18.dp)
                .height(40.dp),
        )
    }
}

@Composable
private fun PrivateMediaPlayerSurface(
    playback: PrivateMediaPlaybackResponse,
    strings: SmartVisionStrings,
) {
    val stream = playback.streams.firstOrNull { stream ->
        stream.url.isNotBlank() && (stream.type.equals("HLS", ignoreCase = true) || stream.type.equals("MP4", ignoreCase = true))
    }
    when {
        stream != null -> PrivateMediaFullscreenExo(stream.url)
        !playback.embedUrl.isNullOrBlank() -> PrivateMediaFullscreenWeb(playback.embedUrl)
        else -> PrivateMediaPlayerMessage(playback.error ?: strings.mediaPlaybackUnavailable)
    }
}

@Composable
private fun PrivateMediaFullscreenExo(streamUrl: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var errorText by remember(streamUrl) { mutableStateOf<String?>(null) }
    val player = remember(streamUrl) {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    LaunchedEffect(streamUrl) {
        errorText = null
        player.setMediaItem(MediaItem.fromUri(Uri.parse(streamUrl)))
        player.prepare()
        player.playWhenReady = true
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                errorText = error.message ?: "Playback unavailable"
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    useController = true
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    this.player = player
                }
            },
            update = { it.player = player },
            modifier = Modifier.fillMaxSize(),
        )
        errorText?.let { PrivateMediaPlayerMessage(it) }
    }
}

@Composable
private fun PrivateMediaFullscreenWeb(embedUrl: String) {
    AndroidView(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        factory = { context ->
            WebView(context).apply {
                configurePrivateMediaFullscreenWebView()
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                loadUrl(embedUrl)
            }
        },
        update = { if (it.url != embedUrl) it.loadUrl(embedUrl) },
    )
}

@SuppressLint("SetJavaScriptEnabled")
private fun WebView.configurePrivateMediaFullscreenWebView() {
    setBackgroundColor(android.graphics.Color.BLACK)
    isFocusable = true
    isFocusableInTouchMode = true
    descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
    isVerticalScrollBarEnabled = false
    isHorizontalScrollBarEnabled = false
    overScrollMode = View.OVER_SCROLL_NEVER
    setLayerType(View.LAYER_TYPE_HARDWARE, null)
    webViewClient = WebViewClient()
    webChromeClient = WebChromeClient()
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.loadsImagesAutomatically = true
    settings.mediaPlaybackRequiresUserGesture = false
    settings.useWideViewPort = true
    settings.loadWithOverviewMode = true
    settings.cacheMode = WebSettings.LOAD_DEFAULT
    settings.builtInZoomControls = false
    settings.displayZoomControls = false
    settings.userAgentString = settings.userAgentString
        .replace("; wv", "")
        .replace("Version/4.0 ", "")
    CookieManager.getInstance().setAcceptCookie(true)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
    }
}

@Composable
private fun PrivateMediaPlayerMessage(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            color = SmartVisionColors.TextSecondary,
            style = SmartVisionType.TitleS,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Text(
        text = "$label: $value",
        color = SmartVisionColors.TextSecondary,
        style = SmartVisionType.Label,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun PrivateMediaDetailMessage(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            color = SmartVisionColors.TextSecondary,
            style = SmartVisionType.TitleS,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
