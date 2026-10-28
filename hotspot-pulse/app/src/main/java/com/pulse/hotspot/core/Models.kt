package com.pulse.hotspot.core

/** Internet sources the hotspot could share. */
data class UpstreamStatus(
    val airplaneMode: Boolean = false,
    val wifiEnabled: Boolean = false,
    val wifiConnected: Boolean = false,
    val wifiValidated: Boolean = false,
    val wifiInterface: String? = null,
    val mobileDataEnabled: Boolean = false,
    val cellularConnected: Boolean = false,
    val simReady: Boolean = false,
) {
    val mobileUsable get() = cellularConnected || (mobileDataEnabled && simReady && !airplaneMode)

    /** At least one of mobile data or Wi-Fi must be available before the hotspot may start. */
    val canShare get() = mobileUsable || wifiConnected

    val sourceLabel: String
        get() = when {
            cellularConnected -> "Mobile data"
            mobileUsable && wifiConnected -> "Mobile data + Wi-Fi"
            mobileUsable -> "Mobile data"
            wifiConnected -> "Wi-Fi"
            else -> "No internet"
        }

    val problem: String?
        get() = when {
            canShare -> null
            airplaneMode && !wifiEnabled -> "Airplane mode is on and Wi-Fi is off."
            !mobileDataEnabled && !wifiEnabled -> "Mobile data and Wi-Fi are both turned off."
            mobileDataEnabled && !simReady -> "Mobile data is on but no SIM is ready, and Wi-Fi isn't connected."
            !mobileDataEnabled && wifiEnabled -> "Mobile data is off and Wi-Fi isn't connected to a network."
            else -> "No internet source is available."
        }
}

/** Thrown when the hotspot is asked to start while neither mobile data nor Wi-Fi is available. */
class NoUpstreamException(val status: UpstreamStatus) :
    IllegalStateException(status.problem ?: "No internet source")

data class GuardSettings(
    val sessionCapBytes: Long = 0,
    val warnPercent: Int = 80,
    val batteryGuard: Boolean = true,
    val batteryThreshold: Int = 20,
    val onTimeReminder: Boolean = false,
    val reminderMinutes: Int = 60,
    val backgroundGuard: Boolean = true,
    val pricePerGb: Double = 0.0,
    val currency: String = "₹",
    val ssid: String = "",
    val password: String = "",
    val security: String = "WPA",
    val hiddenNetwork: Boolean = false,
)

data class SessionStats(
    val startedAt: Long,
    val bytes: Long = 0,
    val speedBps: Long = 0,
    val peakSpeedBps: Long = 0,
    val updatedAt: Long = startedAt,
) {
    val durationSec get() = (updatedAt - startedAt).coerceAtLeast(0) / 1000

    /** Seconds until the cap is reached at the current speed, or null when not meaningful. */
    fun secondsToCap(capBytes: Long): Long? {
        if (capBytes <= 0) return null
        val left = capBytes - bytes
        if (left <= 0) return 0
        if (speedBps <= 0) return null
        return left / speedBps
    }

    fun cost(pricePerGb: Double): Double = if (pricePerGb <= 0) 0.0 else bytes.toDouble() / GB * pricePerGb
}

data class SessionRecord(val startedAt: Long, val endedAt: Long, val bytes: Long)

data class BatteryInfo(val percent: Int = 100, val charging: Boolean = false)

/** Converts interface byte counters into bytes that actually crossed the shared upstream. */
object TrafficMath {
    data class Sample(val mobile: Long, val total: Long)

    fun upstreamDelta(prev: Sample, now: Sample, cellular: Boolean): Long {
        val dMobile = (now.mobile - prev.mobile).coerceAtLeast(0)
        val dTotal = (now.total - prev.total).coerceAtLeast(0)
        // Shared Wi-Fi traffic is counted on both the client and the hotspot interface.
        return if (cellular) dMobile else ((dTotal - dMobile).coerceAtLeast(0) / 2)
    }

    /** Exponential moving average for a smooth speed readout. */
    fun smooth(previous: Long, instant: Long, alpha: Double = 0.35): Long =
        if (previous <= 0) instant else (alpha * instant + (1 - alpha) * previous).toLong()
}
