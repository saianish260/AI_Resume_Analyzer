package com.pulse.hotspot.core

import java.util.Locale

const val MB = 1024L * 1024L
const val GB = 1024L * MB

fun Long.toBytesLabel(): String {
    val b = this.coerceAtLeast(0)
    return when {
        b >= GB -> String.format(Locale.US, "%.2f GB", b.toDouble() / GB)
        b >= MB -> String.format(Locale.US, "%.1f MB", b.toDouble() / MB)
        b >= 1024 -> String.format(Locale.US, "%.0f KB", b.toDouble() / 1024)
        else -> "$b B"
    }
}

fun Long.toSpeedLabel(): String {
    val bits = this.coerceAtLeast(0) * 8
    return when {
        bits >= 1_000_000 -> String.format(Locale.US, "%.1f Mbps", bits / 1_000_000.0)
        bits >= 1_000 -> String.format(Locale.US, "%.0f Kbps", bits / 1_000.0)
        else -> "$bits bps"
    }
}

fun Long.toDurationLabel(): String {
    val s = this.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return when {
        h > 0 -> "${h}h ${m}m"
        m > 0 -> "${m}m ${sec}s"
        else -> "${sec}s"
    }
}

object Vendors {
    private val oui = mapOf(
        "00:1a:11" to "Google", "3c:5a:b4" to "Google", "f4:f5:d8" to "Google", "da:a1:19" to "Google",
        "00:03:93" to "Apple", "28:cf:e9" to "Apple", "3c:15:c2" to "Apple", "a4:83:e7" to "Apple",
        "f0:18:98" to "Apple", "ac:bc:32" to "Apple", "dc:a9:04" to "Apple", "bc:d0:74" to "Apple",
        "00:16:32" to "Samsung", "5c:0a:5b" to "Samsung", "8c:77:12" to "Samsung", "cc:07:ab" to "Samsung",
        "f4:7b:09" to "Samsung", "a0:cc:2b" to "Samsung",
        "28:6c:07" to "Xiaomi", "64:09:80" to "Xiaomi", "f8:a4:5f" to "Xiaomi", "50:8f:4c" to "Xiaomi",
        "94:65:2d" to "OnePlus", "c0:ee:fb" to "OnePlus",
        "00:1e:10" to "Huawei", "48:46:fb" to "Huawei",
        "3c:a0:67" to "Realme", "a4:c9:39" to "Oppo", "18:d0:c5" to "Vivo",
        "00:21:6a" to "Intel", "34:13:e8" to "Intel", "a4:34:d9" to "Intel", "7c:b2:7d" to "Intel",
        "00:50:f2" to "Microsoft", "28:18:78" to "Microsoft",
        "b8:27:eb" to "Raspberry Pi", "dc:a6:32" to "Raspberry Pi",
        "00:09:bf" to "Nintendo", "00:d9:d1" to "Sony", "f8:46:1c" to "Sony",
        "18:31:bf" to "ASUS", "00:24:d6" to "Intel", "c8:3a:35" to "Tenda",
        "50:c7:bf" to "TP-Link", "f4:f2:6d" to "TP-Link", "e8:48:b8" to "TP-Link",
        "00:26:bb" to "Apple", "70:3a:cb" to "Google", "fc:a6:67" to "Amazon", "74:c2:46" to "Amazon",
        "40:b4:cd" to "Amazon", "e0:d5:5e" to "Lenovo", "54:ee:75" to "Lenovo", "3c:97:0e" to "Wistron",
    )

    /** Randomized ("private") MACs set the locally-administered bit. */
    fun isPrivate(mac: String): Boolean {
        val second = mac.getOrNull(1)?.lowercaseChar() ?: return false
        return second in charArrayOf('2', '6', 'a', 'e')
    }

    fun lookup(mac: String): String {
        val m = mac.lowercase(Locale.US)
        if (isPrivate(m)) return "Private address"
        return oui[m.take(8)] ?: "Unknown device"
    }
}
