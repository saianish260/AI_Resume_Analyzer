package com.pulse.hotspot.core

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.TrafficStats
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Single source of truth shared by the UI, the guard service and the Quick Settings tile. */
object HotspotRepository {
    private const val TICK_MS = 2000L
    private const val RESUME_WINDOW_MS = 5 * 60_000L
    private const val HISTORY_SIZE = 30

    private lateinit var app: Context
    private lateinit var prefs: Prefs
    private lateinit var checker: UpstreamChecker
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _upstream = MutableStateFlow(UpstreamStatus())
    val upstream: StateFlow<UpstreamStatus> = _upstream.asStateFlow()
    private val _hotspotOn = MutableStateFlow(false)
    val hotspotOn: StateFlow<Boolean> = _hotspotOn.asStateFlow()
    private val _manual = MutableStateFlow(false)
    val manual: StateFlow<Boolean> = _manual.asStateFlow()
    private val _session = MutableStateFlow<SessionStats?>(null)
    val session: StateFlow<SessionStats?> = _session.asStateFlow()
    private val _settings = MutableStateFlow(GuardSettings())
    val settings: StateFlow<GuardSettings> = _settings.asStateFlow()
    private val _history = MutableStateFlow<List<SessionRecord>>(emptyList())
    val history: StateFlow<List<SessionRecord>> = _history.asStateFlow()
    private val _battery = MutableStateFlow(BatteryInfo())
    val battery: StateFlow<BatteryInfo> = _battery.asStateFlow()

    private var lastSample: TrafficMath.Sample? = null
    private var lastSampleAt = 0L
    private var warnSent = false
    private var capSent = false
    private var batterySent = false
    private var remindersSent = 0

    private var users = 0
    private var loop: Job? = null

    fun init(context: Context) {
        if (::app.isInitialized) return
        app = context.applicationContext
        prefs = Prefs(app)
        checker = UpstreamChecker(app)
        _settings.value = prefs.loadSettings()
        _history.value = prefs.loadHistory()
        _manual.value = prefs.manualSession
        val saved = prefs.loadSession()
        if (saved != null) {
            if (System.currentTimeMillis() - saved.updatedAt < RESUME_WINDOW_MS) {
                _session.value = saved
            } else {
                finishSession(saved)
            }
        }
        tick()
    }

    @Synchronized fun acquire() {
        users++
        if (loop?.isActive != true) loop = scope.launch {
            while (isActive) { runCatching { tick() }; delay(TICK_MS) }
        }
    }

    @Synchronized fun release() {
        users = (users - 1).coerceAtLeast(0)
        if (users == 0) { loop?.cancel(); loop = null }
    }

    fun refreshNow() { scope.launch { runCatching { tick() } } }

    /**
     * Must be called before turning the hotspot on.
     * @throws NoUpstreamException when neither mobile data nor Wi-Fi can provide internet.
     */
    fun checkBeforeStart(): UpstreamStatus {
        val status = checker.check()
        _upstream.value = status
        if (!status.canShare) throw NoUpstreamException(status)
        return status
    }

    fun setManualSession(on: Boolean) {
        prefs.manualSession = on
        _manual.value = on
        refreshNow()
    }

    fun updateSettings(transform: (GuardSettings) -> GuardSettings) {
        val next = transform(_settings.value)
        _settings.value = next
        prefs.saveSettings(next)
    }

    fun clearHistory() {
        _history.value = emptyList()
        prefs.saveHistory(emptyList())
    }

    /** Opens the phone's own hotspot screen; Android only lets the system turn tethering on or off. */
    fun openHotspotSettings(context: Context) {
        val candidates = listOf(
            Intent().setComponent(ComponentName("com.android.settings", "com.android.settings.TetherSettings")),
            Intent().setComponent(ComponentName("com.android.settings", "com.android.settings.Settings\$TetherSettingsActivity")),
            Intent(Settings.ACTION_WIRELESS_SETTINGS),
            Intent(Settings.ACTION_SETTINGS),
        )
        for (intent in candidates) {
            try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return
            } catch (_: ActivityNotFoundException) {
            } catch (_: SecurityException) { }
        }
    }

    /** Opens a panel where the user can switch mobile data or Wi-Fi on. */
    fun openInternetPanel(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
        else Intent(Settings.ACTION_WIRELESS_SETTINGS)
        try { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        catch (_: ActivityNotFoundException) { context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    private fun readSample() = TrafficMath.Sample(
        mobile = TrafficStats.getMobileRxBytes().coerceAtLeast(0) + TrafficStats.getMobileTxBytes().coerceAtLeast(0),
        total = TrafficStats.getTotalRxBytes().coerceAtLeast(0) + TrafficStats.getTotalTxBytes().coerceAtLeast(0),
    )

    private fun readBattery(): BatteryInfo {
        val i = app.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return _battery.value
        val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val plugged = i.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
        val pct = if (level >= 0 && scale > 0) level * 100 / scale else 100
        return BatteryInfo(pct, plugged)
    }

    @Synchronized private fun tick() {
        val up = checker.check()
        _upstream.value = up
        _battery.value = readBattery()

        val on = HotspotDetector.isHotspotUp(up.wifiInterface) || _manual.value
        _hotspotOn.value = on

        val now = System.currentTimeMillis()
        val sample = readSample()
        var s = _session.value
        if (on && s == null) {
            s = SessionStats(startedAt = now, updatedAt = now)
            warnSent = false; capSent = false; batterySent = false; remindersSent = 0
            lastSample = null
        } else if (!on && s != null) {
            finishSession(s)
            s = null
        }

        if (s != null) {
            val prev = lastSample
            if (prev != null && lastSampleAt > 0) {
                val delta = TrafficMath.upstreamDelta(prev, sample, up.cellularConnected)
                val dt = ((now - lastSampleAt).coerceAtLeast(1)) / 1000.0
                val instant = (delta / dt).toLong()
                val speed = TrafficMath.smooth(s.speedBps, instant)
                s = s.copy(bytes = s.bytes + delta, speedBps = speed, peakSpeedBps = maxOf(s.peakSpeedBps, speed), updatedAt = now)
            } else {
                s = s.copy(updatedAt = now)
            }
            guard(s, _settings.value, _battery.value)
        }
        lastSample = sample
        lastSampleAt = now
        _session.value = s
        prefs.saveSession(s)
    }

    private fun finishSession(s: SessionStats) {
        if (s.bytes > 0 || s.durationSec >= 30) {
            val list = (listOf(SessionRecord(s.startedAt, s.updatedAt, s.bytes)) + _history.value).take(HISTORY_SIZE)
            _history.value = list
            prefs.saveHistory(list)
        }
        _session.value = null
        prefs.saveSession(null)
    }

    private fun guard(s: SessionStats, g: GuardSettings, b: BatteryInfo) {
        val cap = g.sessionCapBytes
        if (cap > 0) {
            if (!warnSent && s.bytes >= cap * g.warnPercent / 100) {
                warnSent = true
                Notifier.alert(app, 10, "${g.warnPercent}% of your hotspot budget used",
                    "${s.bytes.toBytesLabel()} of ${cap.toBytesLabel()} shared so far.")
            }
            if (!capSent && s.bytes >= cap) {
                capSent = true
                Notifier.alert(app, 11, "Hotspot data cap reached",
                    "${s.bytes.toBytesLabel()} shared. Turn the hotspot off to stop using more data.")
            }
        }
        if (g.batteryGuard && !b.charging && b.percent <= g.batteryThreshold && !batterySent) {
            batterySent = true
            Notifier.alert(app, 12, "Battery at ${b.percent}%",
                "Your hotspot drains battery fast. Turn it off or plug in your phone.")
        }
        if (b.charging || b.percent > g.batteryThreshold + 5) batterySent = false
        if (g.onTimeReminder && g.reminderMinutes > 0) {
            val due = s.durationSec / 60 / g.reminderMinutes
            if (due > remindersSent) {
                remindersSent = due.toInt()
                Notifier.alert(app, 13, "Hotspot on for ${s.durationSec.toDurationLabel()}",
                    "Still sharing? ${s.bytes.toBytesLabel()} used so far.")
            }
        }
    }
}
