package com.smartvision.svplayer.ui.youtube

import android.annotation.SuppressLint
import android.os.Build
import android.view.KeyEvent
import android.view.View
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.ConsoleMessage
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.smartvision.svplayer.data.anomaly.AnomalyReporter

internal enum class YoutubePlaybackMode {
    Preview,
    Fullscreen,
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun YoutubeWebPlayer(
    videoId: String,
    mode: YoutubePlaybackMode,
    modifier: Modifier = Modifier,
    anomalyReporter: AnomalyReporter? = null,
) {
    val safeVideoId = videoId.sanitizeYoutubeVideoId()
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                configureYoutubeWebView(mode, anomalyReporter, safeVideoId)
                if (mode == YoutubePlaybackMode.Fullscreen && anomalyReporter != null) {
                    addJavascriptInterface(
                        YoutubePlayerBridge(anomalyReporter, safeVideoId, mode.name),
                        JavascriptBridgeName,
                    )
                }
            }
        },
        update = { webView ->
            val tag = "${mode.name}:$safeVideoId"
            if (webView.tag != tag) {
                webView.tag = tag
                webView.loadDataWithBaseURL(
                    YoutubePlayerOrigin,
                    youtubePlayerHtml(
                        videoId = safeVideoId,
                        autoplay = true,
                        muted = mode != YoutubePlaybackMode.Fullscreen,
                        controls = mode == YoutubePlaybackMode.Fullscreen,
                    ),
                    "text/html",
                    "utf-8",
                    null,
                )
            }
            if (mode == YoutubePlaybackMode.Fullscreen) {
                webView.requestFocus()
            }
        },
        onRelease = { webView ->
            runCatching {
                webView.stopLoading()
                webView.loadUrl("about:blank")
                webView.removeJavascriptInterface(JavascriptBridgeName)
                webView.removeAllViews()
                webView.destroy()
            }
        },
    )
}

@SuppressLint("SetJavaScriptEnabled")
private fun WebView.configureYoutubeWebView(
    mode: YoutubePlaybackMode,
    anomalyReporter: AnomalyReporter?,
    videoId: String,
) {
    setBackgroundColor(android.graphics.Color.BLACK)
    isFocusable = mode == YoutubePlaybackMode.Fullscreen
    isFocusableInTouchMode = mode == YoutubePlaybackMode.Fullscreen
    if (mode == YoutubePlaybackMode.Fullscreen) {
        setOnKeyListener(YoutubeRemoteKeyListener)
    }
    webChromeClient = object : WebChromeClient() {
        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
            val message = consoleMessage?.message().orEmpty()
            if (mode == YoutubePlaybackMode.Fullscreen && message.contains("error", ignoreCase = true)) {
                anomalyReporter?.reportAsync(
                    anomalyType = "YOUTUBE_WEB_CONSOLE",
                    message = message.take(180),
                    context = "videoId=$videoId line=${consoleMessage?.lineNumber()}",
                )
            }
            return super.onConsoleMessage(consoleMessage)
        }
    }
    webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val host = request?.url?.host.orEmpty().lowercase()
            return host.isNotBlank() && AllowedYoutubeHosts.none { host == it || host.endsWith(".$it") }
        }
    }
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.loadsImagesAutomatically = true
    settings.mediaPlaybackRequiresUserGesture = false
    settings.useWideViewPort = true
    settings.loadWithOverviewMode = true
    settings.cacheMode = WebSettings.LOAD_DEFAULT
    settings.builtInZoomControls = false
    settings.displayZoomControls = false
    CookieManager.getInstance().setAcceptCookie(true)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
    }
    settings.userAgentString = settings.userAgentString
        .replace("; wv", "")
        .replace("Version/4.0 ", "")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
    }
}

private val YoutubeRemoteKeyListener = View.OnKeyListener { view, keyCode, event ->
    if (event.action != KeyEvent.ACTION_UP || view !is WebView) return@OnKeyListener false
    val command = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_NUMPAD_ENTER,
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
        KeyEvent.KEYCODE_SPACE -> "window.smartVisionTogglePlayback && window.smartVisionTogglePlayback();"
        KeyEvent.KEYCODE_MEDIA_PLAY -> "window.smartVisionPlay && window.smartVisionPlay();"
        KeyEvent.KEYCODE_MEDIA_PAUSE -> "window.smartVisionPause && window.smartVisionPause();"
        KeyEvent.KEYCODE_DPAD_LEFT,
        KeyEvent.KEYCODE_MEDIA_REWIND -> "window.smartVisionSeekBy && window.smartVisionSeekBy(-10);"
        KeyEvent.KEYCODE_DPAD_RIGHT,
        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> "window.smartVisionSeekBy && window.smartVisionSeekBy(10);"
        else -> null
    } ?: return@OnKeyListener false
    view.evaluateJavascript(command, null)
    true
}

private class YoutubePlayerBridge(
    private val anomalyReporter: AnomalyReporter,
    private val videoId: String,
    private val mode: String,
) {
    @JavascriptInterface
    fun onPlayerError(errorCode: String) {
        anomalyReporter.reportAsync(
            anomalyType = "YOUTUBE_PLAYER_ERROR",
            message = "Erreur lecteur YouTube: $errorCode",
            context = "videoId=$videoId mode=$mode",
        )
    }
}

private fun String.sanitizeYoutubeVideoId(): String =
    filter { it.isLetterOrDigit() || it == '_' || it == '-' }.take(32)

private fun youtubePlayerHtml(
    videoId: String,
    autoplay: Boolean,
    muted: Boolean,
    controls: Boolean,
): String {
    val autoplayValue = if (autoplay) 1 else 0
    val mutedJs = if (muted) "event.target.mute();" else "event.target.unMute();"
    val controlsValue = if (controls) 1 else 0
    return """
        <!doctype html>
        <html>
          <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
            <style>
              html, body, #player, #error {
                margin: 0;
                width: 100%;
                height: 100%;
                background: #000;
                overflow: hidden;
              }
              body {
                position: fixed;
                inset: 0;
              }
              iframe {
                position: absolute;
                inset: 0;
                width: 100%;
                height: 100%;
                border: 0;
              }
              #error {
                display: none;
                place-items: center;
                color: #fff;
                font-family: sans-serif;
                font-size: 18px;
                font-weight: 700;
                text-align: center;
                padding: 32px;
                box-sizing: border-box;
              }
            </style>
          </head>
          <body>
            <div id="player"></div>
            <div id="error">Cette video ne peut pas etre lue dans le lecteur integre YouTube.</div>
            <script src="https://www.youtube.com/iframe_api"></script>
            <script>
              var player;
              var playerReady = false;
              var playerState = -1;
              function onYouTubeIframeAPIReady() {
                player = new YT.Player('player', {
                  width: '100%',
                  height: '100%',
                  videoId: '$videoId',
                  playerVars: {
                    autoplay: $autoplayValue,
                    controls: $controlsValue,
                    disablekb: 0,
                    enablejsapi: 1,
                    fs: 1,
                    iv_load_policy: 3,
                    modestbranding: 1,
                    playsinline: 1,
                    rel: 0,
                    origin: '$YoutubePlayerOrigin'
                  },
                  events: {
                    'onReady': onPlayerReady,
                    'onStateChange': onPlayerStateChange,
                    'onError': onPlayerError
                  }
                });
              }
              function onPlayerReady(event) {
                playerReady = true;
                $mutedJs
                event.target.playVideo();
                setTimeout(function() { event.target.playVideo(); }, 450);
              }
              function onPlayerStateChange(event) {
                playerState = event.data;
              }
              function onPlayerError(event) {
                var code = event ? String(event.data) : 'unknown';
                if (window.$JavascriptBridgeName) {
                  window.$JavascriptBridgeName.onPlayerError(code);
                }
                document.getElementById('player').style.display = 'none';
                document.getElementById('error').style.display = 'grid';
              }
              window.smartVisionPlay = function() {
                if (playerReady && player && player.playVideo) player.playVideo();
              };
              window.smartVisionPause = function() {
                if (playerReady && player && player.pauseVideo) player.pauseVideo();
              };
              window.smartVisionTogglePlayback = function() {
                if (!playerReady || !player) return;
                if (playerState === YT.PlayerState.PLAYING) {
                  player.pauseVideo();
                } else {
                  player.playVideo();
                }
              };
              window.smartVisionSeekBy = function(deltaSeconds) {
                if (!playerReady || !player || !player.seekTo || !player.getCurrentTime) return;
                player.seekTo(Math.max(0, player.getCurrentTime() + deltaSeconds), true);
              }
            </script>
          </body>
        </html>
    """.trimIndent()
}

private const val JavascriptBridgeName = "SmartVisionYoutube"
private const val YoutubeBaseUrl = "https://www.youtube.com"
private const val YoutubePlayerOrigin = "https://com.smartvision.svplayer"
private val AllowedYoutubeHosts = setOf(
    "youtube.com",
    "youtube-nocookie.com",
    "youtu.be",
    "ytimg.com",
    "googlevideo.com",
    "google.com",
    "gstatic.com",
)
