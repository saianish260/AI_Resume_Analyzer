package com.pulse.hotspot.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.pulse.hotspot.core.HotspotRepository
import com.pulse.hotspot.core.Notifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Keeps metering the hotspot and firing alerts while the app is in the background. */
class GuardService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var offSince = 0L

    override fun onCreate() {
        super.onCreate()
        HotspotRepository.init(this)
        val r = HotspotRepository
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(
            this, Notifier.ID_GUARD,
            Notifier.ongoing(this, r.session.value, r.settings.value, r.hotspotOn.value), type
        )
        r.acquire()
        offSince = System.currentTimeMillis()
        scope.launch {
            while (isActive) {
                val s = r.session.value
                val g = r.settings.value
                val on = r.hotspotOn.value
                val now = System.currentTimeMillis()
                if (on) offSince = 0L else if (offSince == 0L) offSince = now
                // Stop when the guard is disabled or the hotspot has stayed off for a while.
                if (!g.backgroundGuard || (!on && now - offSince > IDLE_STOP_MS)) {
                    stopSelf()
                    break
                }
                getSystemService(NotificationManager::class.java)
                    .notify(Notifier.ID_GUARD, Notifier.ongoing(this@GuardService, s, g, on))
                delay(2000)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        scope.cancel()
        HotspotRepository.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val IDLE_STOP_MS = 3 * 60_000L

        fun start(context: Context) {
            if (!HotspotRepository.settings.value.backgroundGuard) return
            try {
                ContextCompat.startForegroundService(context, Intent(context, GuardService::class.java))
            } catch (_: Exception) {
                // Android 12+ forbids starting from the background; the UI will start it next time.
            }
        }
    }
}
