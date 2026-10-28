package com.pulse.hotspot.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreLogicTest {

    @Test fun noUpstreamWhenEverythingOff() {
        val s = UpstreamStatus()
        assertFalse(s.canShare)
        assertEquals("Mobile data and Wi-Fi are both turned off.", s.problem)
    }

    @Test fun mobileDataAloneIsEnough() {
        val s = UpstreamStatus(mobileDataEnabled = true, simReady = true)
        assertTrue(s.canShare)
        assertNull(s.problem)
    }

    @Test fun wifiAloneIsEnough() {
        assertTrue(UpstreamStatus(wifiEnabled = true, wifiConnected = true).canShare)
    }

    @Test fun wifiOnButNotConnectedWithoutDataFails() {
        val s = UpstreamStatus(wifiEnabled = true)
        assertFalse(s.canShare)
        assertEquals("Mobile data is off and Wi-Fi isn't connected to a network.", s.problem)
    }

    @Test fun airplaneModeBlocksMobileData() {
        val s = UpstreamStatus(airplaneMode = true, mobileDataEnabled = true, simReady = true)
        assertFalse(s.canShare)
    }

    @Test fun cellularDeltaUsesMobileCounters() {
        val d = TrafficMath.upstreamDelta(TrafficMath.Sample(100, 1000), TrafficMath.Sample(600, 2000), cellular = true)
        assertEquals(500, d)
    }

    @Test fun wifiDeltaIsHalvedNonMobileTraffic() {
        val d = TrafficMath.upstreamDelta(TrafficMath.Sample(0, 0), TrafficMath.Sample(0, 1000), cellular = false)
        assertEquals(500, d)
    }

    @Test fun counterResetNeverGoesNegative() {
        assertEquals(0, TrafficMath.upstreamDelta(TrafficMath.Sample(500, 500), TrafficMath.Sample(10, 10), true))
    }

    @Test fun capForecast() {
        val s = SessionStats(startedAt = 0, bytes = 100 * MB, speedBps = MB)
        assertEquals(400L, s.secondsToCap(500 * MB))
        assertEquals(0L, s.secondsToCap(50 * MB))
        assertNull(s.secondsToCap(0))
        assertNull(s.copy(speedBps = 0).secondsToCap(500 * MB))
    }

    @Test fun costPerGb() {
        assertEquals(20.0, SessionStats(startedAt = 0, bytes = 2 * GB).cost(10.0), 1e-9)
    }

    @Test fun wifiQrPayloadEscapesSpecialCharacters() {
        assertEquals("WIFI:T:WPA;S:My\\;Net;P:pa\\:ss\\\\word;;", WifiQr.payload("My;Net", "pa:ss\\word", "WPA", false))
        assertEquals("WIFI:T:nopass;S:Cafe;H:true;;", WifiQr.payload("Cafe", "ignored", "nopass", true))
    }

    @Test fun hotspotInterfaceNames() {
        assertTrue(HotspotDetector.isHotspotInterfaceName("ap0", null))
        assertTrue(HotspotDetector.isHotspotInterfaceName("swlan0", null))
        assertTrue(HotspotDetector.isHotspotInterfaceName("wlan1", "wlan0"))
        assertFalse(HotspotDetector.isHotspotInterfaceName("wlan1", "wlan1"))
        assertFalse(HotspotDetector.isHotspotInterfaceName("wlan0", null))
        assertFalse(HotspotDetector.isHotspotInterfaceName("rmnet_data0", null))
    }

    @Test fun formatting() {
        assertEquals("1.5 MB", (MB + MB / 2).toBytesLabel())
        assertEquals("2.00 GB", (2 * GB).toBytesLabel())
        assertEquals("8.0 Mbps", 1_000_000L.toSpeedLabel())
        assertEquals("1h 1m", 3660L.toDurationLabel())
    }
}
