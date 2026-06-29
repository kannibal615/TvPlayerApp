package com.smartvision.svplayer.startup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val source = when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED -> AutostartSource.BOOT_COMPLETED
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> AutostartSource.LOCKED_BOOT_COMPLETED
            Intent.ACTION_MY_PACKAGE_REPLACED -> AutostartSource.MY_PACKAGE_REPLACED
            ACTION_QUICKBOOT_POWERON -> AutostartSource.QUICKBOOT_POWERON
            else -> AutostartSource.BOOT_COMPLETED
        }
        BackgroundSyncScheduler.scheduleBootSync(context, source)
        AutostartManager(context).attemptAutostart(context, source)
    }

    private companion object {
        const val ACTION_QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"
    }
}

class UserPresentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_USER_PRESENT) {
            AutostartManager(context).attemptAutostart(context, AutostartSource.USER_PRESENT)
        }
    }
}

class AutostartAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        AutostartManager(context).attemptAutostart(context, AutostartSource.ALARM_FALLBACK)
    }
}
