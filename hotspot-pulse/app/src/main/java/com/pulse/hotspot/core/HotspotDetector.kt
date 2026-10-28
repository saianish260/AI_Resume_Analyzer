package com.pulse.hotspot.core

import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Detects a running hotspot by looking for the access-point network interface
 * (ap0, swlan0, wlan1, ...) holding a private IPv4 address. Uses only public Java APIs,
 * so it works without special permissions, but some phones hide interfaces from apps;
 * in that case the user can track a session manually.
 */
object HotspotDetector {
    private val apName = Regex("^(ap\\d*|swlan\\d+|softap\\d*|wlan[1-9]\\d*|wigig\\d+)$")

    fun isHotspotInterfaceName(name: String, upstreamWifiIface: String?): Boolean =
        name != upstreamWifiIface && apName.matches(name)

    fun isHotspotUp(upstreamWifiIface: String?): Boolean = try {
        NetworkInterface.getNetworkInterfaces()?.toList().orEmpty().any { ni ->
            ni.isUp && isHotspotInterfaceName(ni.name, upstreamWifiIface) &&
                ni.inetAddresses.toList().any { it is Inet4Address && it.isSiteLocalAddress }
        }
    } catch (_: Throwable) { false }
}
