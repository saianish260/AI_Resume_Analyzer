package com.pulse.hotspot.core

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

object WifiQr {
    private fun escape(s: String) = s.replace(Regex("""([\\;,:"])"""), """\\$1""")

    /** Standard Wi-Fi QR payload understood by Android and iOS cameras. */
    fun payload(ssid: String, password: String, security: String, hidden: Boolean): String {
        val type = if (security == "nopass") "nopass" else security
        val sb = StringBuilder("WIFI:T:").append(type).append(";S:").append(escape(ssid)).append(';')
        if (type != "nopass") sb.append("P:").append(escape(password)).append(';')
        if (hidden) sb.append("H:true;")
        return sb.append(';').toString()
    }

    fun bitmap(text: String, size: Int = 640): Bitmap {
        val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, mapOf(EncodeHintType.MARGIN to 1))
        val pixels = IntArray(size * size) { i -> if (matrix[i % size, i / size]) Color.BLACK else Color.WHITE }
        return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
    }
}
