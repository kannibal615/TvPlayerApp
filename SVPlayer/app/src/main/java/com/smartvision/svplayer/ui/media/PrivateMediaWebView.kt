package com.smartvision.svplayer.ui.media

import android.content.Context
import android.view.KeyEvent
import android.view.MotionEvent
import android.os.SystemClock
import android.webkit.WebView

internal class PrivateMediaTvWebView(context: Context) : WebView(context) {
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val handledByWebView = super.dispatchKeyEvent(event)
        if (event.action == KeyEvent.ACTION_UP && event.keyCode.isPrivateMediaActivationKey()) {
            activatePrivateMediaEmbed()
            return true
        }
        if (handledByWebView) return true
        return false
    }
}

internal fun WebView.installPrivateMediaTvKeyFallback() {
    setOnKeyListener { _, _, _ -> false }
}

internal fun WebView.activatePrivateMediaEmbed() {
    requestFocus()
    requestFocusFromTouch()
    performClick()
    dispatchCenterTap()
    evaluateJavascript(
        """
        (function() {
          var active = document.activeElement;
          if (active && active !== document.body && active.click) {
            active.click();
            return 'active';
          }
          var button = document.querySelector(
            '#EPvideo button, #EPvideo [role="button"], #EPvideo .vjs-big-play-button, ' +
            '[aria-label*="Play" i], [title*="Play" i], button, [role="button"]'
          );
          if (button && button.click) {
            button.click();
            return 'button';
          }
          var video = document.querySelector('video');
          if (video) {
            if (video.paused) {
              var promise = video.play();
              if (promise && promise.catch) promise.catch(function(){});
            } else {
              video.pause();
            }
            return 'video';
          }
          var frame = document.querySelector('iframe');
          if (frame && frame.focus) {
            frame.focus();
            return 'iframe';
          }
          return 'none';
        })();
        """.trimIndent(),
        null,
    )
}

private fun WebView.dispatchCenterTap() {
    if (width <= 0 || height <= 0) return
    val eventTime = SystemClock.uptimeMillis()
    val x = width / 2f
    val y = height / 2f
    val down = MotionEvent.obtain(eventTime, eventTime, MotionEvent.ACTION_DOWN, x, y, 0)
    val up = MotionEvent.obtain(eventTime, eventTime + 80L, MotionEvent.ACTION_UP, x, y, 0)
    try {
        dispatchTouchEvent(down)
        dispatchTouchEvent(up)
    } finally {
        down.recycle()
        up.recycle()
    }
}

private fun Int.isPrivateMediaActivationKey(): Boolean =
    this == KeyEvent.KEYCODE_DPAD_CENTER ||
        this == KeyEvent.KEYCODE_ENTER ||
        this == KeyEvent.KEYCODE_NUMPAD_ENTER ||
        this == KeyEvent.KEYCODE_MEDIA_PLAY ||
        this == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
