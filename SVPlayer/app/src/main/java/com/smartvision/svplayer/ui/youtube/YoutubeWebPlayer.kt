package com.smartvision.svplayer.ui.youtube

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.ConsoleMessage
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.smartvision.svplayer.data.anomaly.AnomalyReporter

internal enum class YoutubePlaybackMode {
    Preview,
    Fullscreen,
}

internal enum class YoutubePlayerCommand {
    TogglePlayback,
    Play,
    Pause,
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun YoutubeWebPlayer(
    videoId: String,
    mode: YoutubePlaybackMode,
    command: YoutubePlayerCommand? = null,
    commandSerial: Int = 0,
    keyboardControlsEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    anomalyReporter: AnomalyReporter? = null,
    onPlayerReady: () -> Unit = {},
    onPlaybackStateChanged: (Boolean) -> Unit = {},
    onPlaybackCompleted: () -> Unit = {},
) {
    val safeVideoId = videoId.sanitizeYoutubeVideoId()
    var renderRestart by remember(safeVideoId, mode) { mutableIntStateOf(0) }
    var handledCommandSerial by remember(safeVideoId, mode) { mutableIntStateOf(0) }
    key(renderRestart) {
        AndroidView(
            modifier = modifier,
            factory = { context ->
                YoutubeTvWebView(context).apply {
                    configureYoutubeWebView(
                        mode = mode,
                        keyboardControlsEnabled = keyboardControlsEnabled,
                        anomalyReporter = anomalyReporter,
                        videoId = safeVideoId,
                        onRenderProcessGone = { renderRestart += 1 },
                    )
                    if (mode == YoutubePlaybackMode.Fullscreen && anomalyReporter != null) {
                        addJavascriptInterface(
                            YoutubePlayerBridge(
                                anomalyReporter = anomalyReporter,
                                videoId = safeVideoId,
                                mode = mode.name,
                                onPlayerReady = onPlayerReady,
                                onPlaybackStateChanged = onPlaybackStateChanged,
                                onPlaybackCompleted = onPlaybackCompleted,
                            ),
                            JavascriptBridgeName,
                        )
                    }
                }
            },
            update = { webView ->
                val tag = "${mode.name}:$safeVideoId"
                val currentTag = webView.tag as? YoutubeWebViewTag
                if (currentTag?.videoKey != tag) {
                    webView.tag = YoutubeWebViewTag(videoKey = tag)
                    webView.loadDataWithBaseURL(
                        YoutubePlayerOrigin,
                        youtubePlayerHtml(
                            videoId = safeVideoId,
                            autoplay = true,
                            muted = mode != YoutubePlaybackMode.Fullscreen,
                            controls = false,
                        ),
                        "text/html",
                        "utf-8",
                        null,
                    )
                }
                if (mode == YoutubePlaybackMode.Fullscreen && keyboardControlsEnabled) {
                    webView.installYoutubeKeyControls()
                }
                if (mode == YoutubePlaybackMode.Fullscreen) {
                    if (command != null && commandSerial > 0 && commandSerial != handledCommandSerial) {
                        handledCommandSerial = commandSerial
                        webView.evaluateJavascript(command.javascriptCall(), null)
                    }
                    if (keyboardControlsEnabled) {
                        webView.postDelayed({ webView.grabYoutubeFocus() }, 80)
                        webView.postDelayed({ webView.grabYoutubeFocus() }, 280)
                        webView.postDelayed({ webView.grabYoutubeFocus() }, 700)
                    }
                }
            },
            onRelease = { webView ->
                runCatching {
                    webView.evaluateJavascript("window.smartVisionStop && window.smartVisionStop();", null)
                    webView.stopLoading()
                    webView.loadUrl("about:blank")
                    webView.removeJavascriptInterface(JavascriptBridgeName)
                    webView.removeAllViews()
                    webView.destroy()
                }
            },
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun WebView.configureYoutubeWebView(
    mode: YoutubePlaybackMode,
    keyboardControlsEnabled: Boolean,
    anomalyReporter: AnomalyReporter?,
    videoId: String,
    onRenderProcessGone: () -> Unit,
) {
    setBackgroundColor(android.graphics.Color.BLACK)
    isFocusable = mode == YoutubePlaybackMode.Fullscreen && keyboardControlsEnabled
    isFocusableInTouchMode = mode == YoutubePlaybackMode.Fullscreen && keyboardControlsEnabled
    descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
    isVerticalScrollBarEnabled = false
    isHorizontalScrollBarEnabled = false
    overScrollMode = View.OVER_SCROLL_NEVER
    setLayerType(View.LAYER_TYPE_HARDWARE, null)
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

        override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
            anomalyReporter?.reportAsync(
                anomalyType = "YOUTUBE_RENDER_PROCESS_GONE",
                message = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && detail?.didCrash() == true) {
                    "WebView renderer crash"
                } else {
                    "WebView renderer gone"
                },
                context = "videoId=$videoId mode=${mode.name}",
            )
            runCatching {
                (view?.parent as? ViewGroup)?.removeView(view)
                view?.destroy()
            }
            onRenderProcessGone()
            return true
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

private fun WebView.installYoutubeKeyControls() {
    setOnKeyListener { view, keyCode, event ->
        if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
        if (!view.hasFocus()) view.requestFocus()
        handleYoutubeKeyCode(keyCode)
    }
}

private class YoutubeTvWebView(context: Context) : WebView(context) {
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && handleYoutubeKeyCode(event.keyCode)) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}

private fun WebView.grabYoutubeFocus() {
    requestFocus()
    requestFocusFromTouch()
    evaluateJavascript("window.smartVisionFocusPlayer && window.smartVisionFocusPlayer();", null)
}

private fun WebView.handleYoutubeKeyCode(keyCode: Int): Boolean =
    when (keyCode) {
        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
            grabYoutubeFocus()
            evaluateJavascript("window.smartVisionTogglePlayback && window.smartVisionTogglePlayback();", null)
            true
        }
        KeyEvent.KEYCODE_DPAD_LEFT -> false
        KeyEvent.KEYCODE_MEDIA_REWIND -> {
            grabYoutubeFocus()
            evaluateJavascript("window.smartVisionSeekBy && window.smartVisionSeekBy(-10);", null)
            true
        }
        KeyEvent.KEYCODE_DPAD_RIGHT,
        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
            grabYoutubeFocus()
            evaluateJavascript("window.smartVisionSeekBy && window.smartVisionSeekBy(10);", null)
            true
        }
        KeyEvent.KEYCODE_MEDIA_PLAY -> {
            grabYoutubeFocus()
            evaluateJavascript("window.smartVisionPlay && window.smartVisionPlay();", null)
            true
        }
        KeyEvent.KEYCODE_MEDIA_PAUSE -> {
            grabYoutubeFocus()
            evaluateJavascript("window.smartVisionPause && window.smartVisionPause();", null)
            true
        }
        KeyEvent.KEYCODE_DPAD_UP -> false
        KeyEvent.KEYCODE_DPAD_DOWN -> {
            grabYoutubeFocus()
            true
        }
        else -> false
    }

private class YoutubePlayerBridge(
    private val anomalyReporter: AnomalyReporter,
    private val videoId: String,
    private val mode: String,
    private val onPlayerReady: () -> Unit,
    private val onPlaybackStateChanged: (Boolean) -> Unit,
    private val onPlaybackCompleted: () -> Unit,
) {
    @JavascriptInterface
    fun onPlayerReady() {
        onPlayerReady.invoke()
    }

    @JavascriptInterface
    fun onPlaybackCompleted() {
        onPlaybackCompleted.invoke()
    }

    @JavascriptInterface
    fun onPlaybackStateChanged(isPlaying: Boolean) {
        onPlaybackStateChanged.invoke(isPlaying)
    }

    @JavascriptInterface
    fun onPlayerError(errorCode: String) {
        anomalyReporter.reportAsync(
            anomalyType = "YOUTUBE_PLAYER_ERROR",
            message = "Erreur lecteur YouTube: $errorCode",
            context = "videoId=$videoId mode=$mode",
        )
    }
}

private fun YoutubePlayerCommand.javascriptCall(): String =
    when (this) {
        YoutubePlayerCommand.TogglePlayback -> "window.smartVisionTogglePlayback && window.smartVisionTogglePlayback();"
        YoutubePlayerCommand.Play -> "window.smartVisionPlay && window.smartVisionPlay();"
        YoutubePlayerCommand.Pause -> "window.smartVisionPause && window.smartVisionPause();"
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
    val disableKeyboardValue = if (controls) 0 else 1
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
              if (!window.queueMicrotask) {
                window.queueMicrotask = function(callback) {
                  Promise.resolve().then(callback).catch(function(error) {
                    setTimeout(function() { throw error; }, 0);
                  });
                };
              }
              var player;
              var playerReady = false;
              var playerState = -1;
              var pendingCommand = null;
              var completedSent = false;
              function onYouTubeIframeAPIReady() {
                player = new YT.Player('player', {
                  width: '100%',
                  height: '100%',
                  videoId: '$videoId',
                  playerVars: {
                    autoplay: $autoplayValue,
                    controls: $controlsValue,
                    cc_load_policy: 0,
                    disablekb: $disableKeyboardValue,
                    enablejsapi: 1,
                    fs: 0,
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
                if (window.$JavascriptBridgeName) {
                  window.$JavascriptBridgeName.onPlayerReady();
                }
                $mutedJs
                event.target.playVideo();
                setTimeout(function() { event.target.playVideo(); }, 450);
                setTimeout(function() { runPendingCommand(); }, 520);
              }
              function onPlayerStateChange(event) {
                playerState = event.data;
                if (window.$JavascriptBridgeName) {
                  window.$JavascriptBridgeName.onPlaybackStateChanged(event.data === YT.PlayerState.PLAYING);
                }
                if (event.data === YT.PlayerState.ENDED && window.$JavascriptBridgeName && !completedSent) {
                  completedSent = true;
                  window.$JavascriptBridgeName.onPlaybackCompleted();
                }
                if (event.data !== YT.PlayerState.ENDED) {
                  completedSent = false;
                }
              }
              function onPlayerError(event) {
                var code = event ? String(event.data) : 'unknown';
                if (window.$JavascriptBridgeName) {
                  window.$JavascriptBridgeName.onPlayerError(code);
                }
                document.getElementById('player').style.display = 'none';
                document.getElementById('error').style.display = 'grid';
              }
              function runWhenReady(command) {
                if (!playerReady || !player) {
                  pendingCommand = command;
                  return;
                }
                try { command(); } catch (err) { console.log('smartvision_youtube_command_error'); }
              }
              function runPendingCommand() {
                if (pendingCommand) {
                  var command = pendingCommand;
                  pendingCommand = null;
                  runWhenReady(command);
                }
              }
              window.smartVisionPlay = function() {
                runWhenReady(function() { if (player.playVideo) player.playVideo(); });
              };
              window.smartVisionPause = function() {
                runWhenReady(function() { if (player.pauseVideo) player.pauseVideo(); });
              };
              window.smartVisionTogglePlayback = function() {
                runWhenReady(function() {
                  if (playerState === YT.PlayerState.PLAYING) {
                    player.pauseVideo();
                  } else {
                    player.playVideo();
                  }
                });
              };
              window.smartVisionSeekBy = function(deltaSeconds) {
                runWhenReady(function() {
                  if (!player.seekTo || !player.getCurrentTime) return;
                  player.seekTo(Math.max(0, player.getCurrentTime() + deltaSeconds), true);
                });
              };
              window.smartVisionStop = function() {
                try {
                  if (player && player.stopVideo) player.stopVideo();
                  if (player && player.destroy) player.destroy();
                } catch (err) {}
              }
              window.smartVisionFocusPlayer = function() {
                try {
                  document.body.tabIndex = 0;
                  document.body.focus();
                  window.focus();
                  var iframe = document.querySelector('iframe');
                  if (iframe) iframe.focus();
                } catch (err) {}
              }
              window.smartVisionSkipAd = function() {
                try {
                  var button = document.querySelector('.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button');
                  if (button) button.click();
                } catch (err) {}
              }
            </script>
          </body>
        </html>
    """.trimIndent()
}

private const val JavascriptBridgeName = "SmartVisionYoutube"
private const val YoutubeBaseUrl = "https://www.youtube.com"
private const val YoutubePlayerOrigin = "https://com.smartvision.svplayer"
private data class YoutubeWebViewTag(val videoKey: String)
private val AllowedYoutubeHosts = setOf(
    "youtube.com",
    "youtube-nocookie.com",
    "youtu.be",
    "ytimg.com",
    "googlevideo.com",
    "google.com",
    "gstatic.com",
)
