package com.pulse.hotspot

import android.app.Application
import com.pulse.hotspot.core.HotspotRepository
import com.pulse.hotspot.core.Notifier

class PulseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifier.createChannels(this)
        HotspotRepository.init(this)
    }
}
