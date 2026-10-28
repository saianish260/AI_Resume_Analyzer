package com.pulse.hotspot.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.pulse.hotspot.MainActivity
import com.pulse.hotspot.core.HotspotRepository
import com.pulse.hotspot.core.toBytesLabel

/** Quick Settings tile: shows live usage and jumps straight into the start/stop flow. */
class HotspotTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        HotspotRepository.init(applicationContext)
        HotspotRepository.refreshNow()
        render()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java)
            .setAction(MainActivity.ACTION_TOGGLE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(
                PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun render() {
        val tile = qsTile ?: return
        val on = HotspotRepository.hotspotOn.value
        tile.state = if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (on) (HotspotRepository.session.value?.bytes ?: 0L).toBytesLabel() else "Off"
        }
        tile.updateTile()
    }
}
