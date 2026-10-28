package com.pulse.hotspot.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.provider.Settings
import android.telephony.TelephonyManager

/** Checks that mobile data or Wi-Fi is available before the hotspot is allowed to start. */
class UpstreamChecker(private val context: Context) {
    private val cm = context.getSystemService(ConnectivityManager::class.java)
    private val wm = context.applicationContext.getSystemService(WifiManager::class.java)
    private val tm = context.getSystemService(TelephonyManager::class.java)

    @Suppress("DEPRECATION")
    fun check(): UpstreamStatus {
        var wifiConnected = false
        var wifiValidated = false
        var wifiIface: String? = null
        var cellular = false
        try {
            for (n in cm.allNetworks) {
                val caps = cm.getNetworkCapabilities(n) ?: continue
                if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) continue
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    wifiConnected = true
                    wifiValidated = wifiValidated || caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    wifiIface = cm.getLinkProperties(n)?.interfaceName ?: wifiIface
                }
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) cellular = true
            }
        } catch (_: Throwable) { }

        val resolver = context.contentResolver
        val airplane = Settings.Global.getInt(resolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1
        val mobileData = try {
            tm.isDataEnabled
        } catch (_: Throwable) {
            Settings.Global.getInt(resolver, "mobile_data", 0) == 1
        } || cellular
        val simReady = try { tm.simState == TelephonyManager.SIM_STATE_READY } catch (_: Throwable) { false }
        val wifiOn = try { wm.isWifiEnabled } catch (_: Throwable) { wifiConnected }

        return UpstreamStatus(
            airplaneMode = airplane,
            wifiEnabled = wifiOn,
            wifiConnected = wifiConnected,
            wifiValidated = wifiValidated,
            wifiInterface = wifiIface,
            mobileDataEnabled = mobileData,
            cellularConnected = cellular,
            simReady = simReady || cellular,
        )
    }

    /** Throws [NoUpstreamException] when neither mobile data nor Wi-Fi can provide internet. */
    fun requireUpstream(): UpstreamStatus {
        val s = check()
        if (!s.canShare) throw NoUpstreamException(s)
        return s
    }
}
