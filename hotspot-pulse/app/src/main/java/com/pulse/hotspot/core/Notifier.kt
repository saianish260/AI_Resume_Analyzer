package com.pulse.hotspot.core

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.pulse.hotspot.MainActivity
import com.pulse.hotspot.R

object Notifier {
    const val CH_GUARD = "guard"
    const val CH_ALERT = "alerts"
    const val ID_GUARD = 1
    private const val ACCENT = 0xFF7C4DFF.toInt()

    fun createChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_GUARD, "Live hotspot meter", NotificationManager.IMPORTANCE_LOW)
                .apply { description = "Shows data used while your hotspot is on" }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_ALERT, "Hotspot alerts", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Data cap, battery and reminder alerts"; enableVibration(true) }
        )
    }

    private fun openApp(ctx: Context, action: String? = null): PendingIntent {
        val i = Intent(ctx, MainActivity::class.java).setAction(action)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            ctx, action.hashCode(), i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun ongoing(ctx: Context, session: SessionStats?, settings: GuardSettings, on: Boolean): Notification {
        val title = if (on) "Hotspot on · ${(session?.bytes ?: 0).toBytesLabel()} shared" else "Hotspot Pulse is watching"
        val text = when {
            !on -> "Turn your hotspot on and Pulse will meter it."
            settings.sessionCapBytes > 0 ->
                "${session?.speedBps?.toSpeedLabel() ?: "0 bps"} · cap ${settings.sessionCapBytes.toBytesLabel()}"
            else -> "${session?.speedBps?.toSpeedLabel() ?: "0 bps"} · ${(session?.durationSec ?: 0).toDurationLabel()}"
        }
        val b = NotificationCompat.Builder(ctx, CH_GUARD)
            .setSmallIcon(R.drawable.ic_tile)
            .setColor(ACCENT)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openApp(ctx))
            .addAction(0, "Hotspot settings", openApp(ctx, MainActivity.ACTION_OPEN_SETTINGS))
        if (on && settings.sessionCapBytes > 0 && session != null) {
            val pct = (session.bytes * 100 / settings.sessionCapBytes).coerceIn(0, 100).toInt()
            b.setProgress(100, pct, false)
        }
        return b.build()
    }

    fun alert(ctx: Context, id: Int, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val n = NotificationCompat.Builder(ctx, CH_ALERT)
            .setSmallIcon(R.drawable.ic_tile)
            .setColor(ACCENT)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openApp(ctx))
            .addAction(0, "Turn off hotspot", openApp(ctx, MainActivity.ACTION_OPEN_SETTINGS))
            .build()
        try { NotificationManagerCompat.from(ctx).notify(id, n) } catch (_: SecurityException) { }
    }
}
