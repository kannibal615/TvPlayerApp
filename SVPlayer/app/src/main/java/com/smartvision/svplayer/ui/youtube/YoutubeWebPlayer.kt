package com.smartvision.svplayer.ui.youtube

import android.annotation.SuppressLint
import android.os.Build
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
                if (mode == YoutubePlaybackMode.Fullscreen) {
                    webView.loadUrl(youtubeWatchUrl(safeVideoId))
                } else {
                    webView.loadDataWithBaseURL(
                        YoutubeBaseUrl,
                        youtubePlayerHtml(
                            videoId = safeVideoId,
                            autoplay = true,
                            muted = true,
                            controls = false,
                        ),
                        "text/html",
                        "utf-8",
                        null,
                    )
                }
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
    settings.userAgentString = settings.userAgentString
        .replace("; wv", "")
        .replace("Version/4.0 ", "")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
    }
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
    val bridgeErrorJs = if (controls) {
        "if (window.$JavascriptBridgeName) { window.$JavascriptBridgeName.onPlayerError(String(event.data)); }"
    } else {
        ""
    }
    return """
        <!doctype html>
        <html>
          <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
            <style>
              html, body, #player {
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
            </style>
          </head>
          <body>
            <div id="player"></div>
            <script src="https://www.youtube.com/iframe_api"></script>
            <script>
              var player;
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
                    origin: '$YoutubeBaseUrl'
                  },
                  events: {
                    'onReady': onPlayerReady,
                    'onError': onPlayerError
                  }
                });
              }
              function onPlayerReady(event) {
                $mutedJs
                event.target.playVideo();
                setTimeout(function() { event.target.playVideo(); }, 450);
              }
              function onPlayerError(event) {
                if (event && (event.data === 101 || event.data === 150 || event.data === 152)) {
                  document.body.innerHTML = '';
                  window.location.href = '${youtubeWatchUrl(videoId)}';
                  return;
                }
                $bridgeErrorJs
              }
            </script>
          </body>
        </html>
    """.trimIndent()
}

private fun youtubeWatchUrl(videoId: String): String =
    "https://www.youtube.com/watch?v=$videoId&autoplay=1&fs=1"

private const val JavascriptBridgeName = "SmartVisionYoutube"
private const val YoutubeBaseUrl = "https://www.youtube.com"
private val AllowedYoutubeHosts = setOf(
    "youtube.com",
    "youtube-nocookie.com",
    "youtu.be",
    "ytimg.com",
    "googlevideo.com",
    "google.com",
    "gstatic.com",
)
