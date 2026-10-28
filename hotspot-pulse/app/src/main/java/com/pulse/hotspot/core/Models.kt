package com.pulse.hotspot.core

enum class ApState { DISABLED, ENABLING, ENABLED, DISABLING, UNKNOWN;
    val isOn get() = this == ENABLED || this == ENABLING
}

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

class NoUpstreamException(val status: UpstreamStatus) :
    IllegalStateException(status.problem ?: "No internet source")

data class DevicePolicy(
    val mac: String,
    val nickname: String = "",
    val limitBytes: Long = 0,
    val usedBytes: Long = 0,
    val downBytes: Long = 0,
    val upBytes: Long = 0,
    val manualBlock: Boolean = false,
    val passExpiresAt: Long = 0,
    val firstSeen: Long = System.currentTimeMillis(),
    val lastSeen: Long = System.currentTimeMillis(),
)

enum class BlockReason(val label: String) { NONE(""), MANUAL("Paused by you"), LIMIT("Data limit reached"), PASS("Time pass expired"), FAIR("Fair share used up") }

data class Client(
    val mac: String,
    val ip: String,
    val iface: String,
    val name: String,
    val vendor: String,
    val policy: DevicePolicy,
    val effectiveLimit: Long,
    val blockReason: BlockReason,
    val speedBps: Long,
    val online: Boolean,
) {
    val blocked get() = blockReason != BlockReason.NONE
}

data class GuardSettings(
    val sessionCapBytes: Long = 0,
    val autoOffAtCap: Boolean = true,
    val fairShare: Boolean = false,
    val defaultLimitBytes: Long = 0,
    val forceIpv4Metered: Boolean = true,
    val batteryGuard: Boolean = false,
    val batteryThreshold: Int = 20,
    val idleOff: Boolean = false,
    val idleMinutes: Int = 10,
    val backgroundGuard: Boolean = true,
    val rootEnabled: Boolean = false,
    val ssid: String = "",
    val password: String = "",
    val security: String = "WPA",
)

data class SessionStats(
    val startedAt: Long,
    val bytes: Long = 0,
    val speedBps: Long = 0,
    val peakClients: Int = 0,
    val lastClientSeenAt: Long = startedAt,
) {
    /** Seconds until the cap is reached at the current speed, or null when not meaningful. */
    fun secondsToCap(capBytes: Long): Long? {
        if (capBytes <= 0 || speedBps <= 0) return null
        val left = capBytes - bytes
        if (left <= 0) return 0
        return left / speedBps
    }
}

data class SessionRecord(val startedAt: Long, val endedAt: Long, val bytes: Long, val peakClients: Int)

data class BatteryInfo(val percent: Int = 100, val charging: Boolean = false)
