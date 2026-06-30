package com.smartvision.svplayer.data.xtream

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.smartvision.svplayer.MainActivity
import com.smartvision.svplayer.R

class XtreamConnectionNotifier(
    private val context: Context,
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun showIssue() {
        if (!canNotify()) return
        ensureChannel()
        val intent = Intent(context, MainActivity::class.java).apply {
            action = MainActivity.ACTION_SHOW_XTREAM_CONNECTION_ALERT
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }
        val notification = builder
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle("Probleme de connexion Xtream")
            .setContentText("Vos chaines, films et series sont temporairement indisponibles.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setOngoing(false)
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun clear() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val existing = notificationManager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return
        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Xtream connection",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Alertes lorsque le serveur Xtream configure est indisponible."
            },
        )
    }

    private fun canNotify(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private companion object {
        const val CHANNEL_ID = "xtream_connection"
        const val NOTIFICATION_ID = 7014
    }
}
