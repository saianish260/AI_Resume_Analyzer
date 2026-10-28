package com.pulse.hotspot.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("pulse", Context.MODE_PRIVATE)

    fun loadSettings() = GuardSettings(
        sessionCapBytes = sp.getLong("cap", 0),
        warnPercent = sp.getInt("warn", 80),
        batteryGuard = sp.getBoolean("batt", true),
        batteryThreshold = sp.getInt("battAt", 20),
        onTimeReminder = sp.getBoolean("remind", false),
        reminderMinutes = sp.getInt("remindMin", 60),
        backgroundGuard = sp.getBoolean("bg", true),
        pricePerGb = sp.getFloat("price", 0f).toDouble(),
        currency = sp.getString("cur", "₹") ?: "₹",
        ssid = sp.getString("ssid", "") ?: "",
        password = sp.getString("pass", "") ?: "",
        security = sp.getString("sec", "WPA") ?: "WPA",
        hiddenNetwork = sp.getBoolean("hidden", false),
    )

    fun saveSettings(s: GuardSettings) = sp.edit()
        .putLong("cap", s.sessionCapBytes).putInt("warn", s.warnPercent)
        .putBoolean("batt", s.batteryGuard).putInt("battAt", s.batteryThreshold)
        .putBoolean("remind", s.onTimeReminder).putInt("remindMin", s.reminderMinutes)
        .putBoolean("bg", s.backgroundGuard).putFloat("price", s.pricePerGb.toFloat())
        .putString("cur", s.currency).putString("ssid", s.ssid).putString("pass", s.password)
        .putString("sec", s.security).putBoolean("hidden", s.hiddenNetwork)
        .apply()

    var manualSession: Boolean
        get() = sp.getBoolean("manual", false)
        set(v) = sp.edit().putBoolean("manual", v).apply()

    fun loadSession(): SessionStats? {
        val start = sp.getLong("sStart", 0)
        if (start == 0L) return null
        return SessionStats(startedAt = start, bytes = sp.getLong("sBytes", 0), updatedAt = sp.getLong("sUpd", start))
    }

    fun saveSession(s: SessionStats?) {
        val e = sp.edit()
        if (s == null) e.remove("sStart").remove("sBytes").remove("sUpd")
        else e.putLong("sStart", s.startedAt).putLong("sBytes", s.bytes).putLong("sUpd", s.updatedAt)
        e.apply()
    }

    fun loadHistory(): List<SessionRecord> = try {
        val arr = JSONArray(sp.getString("history", "[]"))
        (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            SessionRecord(o.getLong("s"), o.getLong("e"), o.getLong("b"))
        }
    } catch (_: Throwable) { emptyList() }

    fun saveHistory(list: List<SessionRecord>) {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("s", it.startedAt).put("e", it.endedAt).put("b", it.bytes)) }
        sp.edit().putString("history", arr.toString()).apply()
    }
}
