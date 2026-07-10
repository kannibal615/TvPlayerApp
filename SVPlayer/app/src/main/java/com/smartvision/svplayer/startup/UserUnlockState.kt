package com.smartvision.svplayer.startup

import android.content.Context
import android.os.Build
import android.os.UserManager

internal fun Context.isUserUnlockedCompat(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return true
    val userManager = getSystemService(Context.USER_SERVICE) as? UserManager
    return userManager?.isUserUnlocked ?: true
}
