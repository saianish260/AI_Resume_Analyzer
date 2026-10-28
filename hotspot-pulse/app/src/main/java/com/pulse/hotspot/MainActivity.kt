package com.pulse.hotspot

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CellTower
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WifiTetheringError
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pulse.hotspot.core.HotspotRepository
import com.pulse.hotspot.core.NoUpstreamException
import com.pulse.hotspot.core.UpstreamStatus
import com.pulse.hotspot.service.GuardService
import com.pulse.hotspot.ui.screens.GuardScreen
import com.pulse.hotspot.ui.screens.HomeScreen
import com.pulse.hotspot.ui.screens.ShareScreen
import com.pulse.hotspot.ui.theme.Pulse
import com.pulse.hotspot.ui.theme.PulseTheme

class MainActivity : ComponentActivity() {

    companion object {
        const val ACTION_TOGGLE = "com.pulse.hotspot.TOGGLE"
        const val ACTION_OPEN_SETTINGS = "com.pulse.hotspot.OPEN_SETTINGS"
    }

    private val repo = HotspotRepository
    private var upstreamError by mutableStateOf<UpstreamStatus?>(null)

    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        repo.refreshNow()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        repo.init(this)
        val wanted = buildList {
            add(Manifest.permission.READ_PHONE_STATE)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (savedInstanceState == null) permissions.launch(wanted.toTypedArray())

        setContent { PulseTheme { Root() } }
        if (savedInstanceState == null) handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        repo.acquire()
        if (repo.hotspotOn.value) GuardService.start(this)
    }

    override fun onStop() {
        repo.release()
        super.onStop()
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_TOGGLE -> onPower()
            ACTION_OPEN_SETTINGS -> repo.openHotspotSettings(this)
        }
    }

    /** Start flow: verify an internet source first, then hand off to the system hotspot switch. */
    private fun onPower() {
        if (repo.hotspotOn.value) {
            if (repo.manual.value) repo.setManualSession(false) else repo.openHotspotSettings(this)
            return
        }
        try {
            repo.checkBeforeStart()
            GuardService.start(this)
            repo.openHotspotSettings(this)
        } catch (e: NoUpstreamException) {
            upstreamError = e.status
        }
    }

    @Composable
    private fun Root() {
        var tab by rememberSaveable { mutableIntStateOf(0) }
        val on by repo.hotspotOn.collectAsStateWithLifecycle()
        val manual by repo.manual.collectAsStateWithLifecycle()
        val upstream by repo.upstream.collectAsStateWithLifecycle()
        val battery by repo.battery.collectAsStateWithLifecycle()
        val session by repo.session.collectAsStateWithLifecycle()
        val settings by repo.settings.collectAsStateWithLifecycle()
        val history by repo.history.collectAsStateWithLifecycle()

        Box(Modifier.fillMaxSize().background(Pulse.background)) {
            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = {
                    NavigationBar(containerColor = Pulse.Night.copy(alpha = 0.92f)) {
                        val items = listOf(
                            Triple("Hotspot", Icons.Rounded.CellTower, 0),
                            Triple("Guard", Icons.Rounded.Shield, 1),
                            Triple("Share", Icons.Rounded.QrCode2, 2),
                        )
                        items.forEach { (label, icon, index) ->
                            NavigationBarItem(
                                selected = tab == index,
                                onClick = { tab = index },
                                icon = { Icon(icon, label) },
                                label = { Text(label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = Pulse.Cyan,
                                    indicatorColor = Pulse.Violet,
                                    unselectedIconColor = Pulse.Muted,
                                    unselectedTextColor = Pulse.Muted,
                                ),
                            )
                        }
                    }
                },
            ) { padding ->
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    modifier = Modifier.padding(padding),
                    label = "tabs",
                ) { current ->
                    when (current) {
                        0 -> HomeScreen(
                            on = on, manual = manual, upstream = upstream, battery = battery,
                            session = session, settings = settings, history = history,
                            onPower = { onPower() },
                            onTrackManual = {
                                try {
                                    repo.checkBeforeStart()
                                    repo.setManualSession(true)
                                    GuardService.start(this@MainActivity)
                                } catch (e: NoUpstreamException) {
                                    upstreamError = e.status
                                }
                            },
                        )
                        1 -> GuardScreen(
                            settings = settings,
                            onChange = { repo.updateSettings(it) },
                            onOpenHotspotSettings = { repo.openHotspotSettings(this@MainActivity) },
                            onClearHistory = { repo.clearHistory() },
                        )
                        else -> ShareScreen(
                            settings = settings,
                            onChange = { repo.updateSettings(it) },
                            onOpenHotspotSettings = { repo.openHotspotSettings(this@MainActivity) },
                        )
                    }
                }
            }
        }

        upstreamError?.let { status -> NoInternetDialog(status) }
    }

    @Composable
    private fun NoInternetDialog(status: UpstreamStatus) {
        AlertDialog(
            onDismissRequest = { upstreamError = null },
            containerColor = Pulse.Deep,
            icon = { Icon(Icons.Rounded.WifiTetheringError, null, tint = Pulse.Red) },
            title = { Text("Can't start the hotspot", color = Pulse.Text) },
            text = {
                Column {
                    Text(status.problem ?: "No internet source is available.", color = Pulse.Text)
                    Spacer(Modifier.height(8.dp))
                    Text("Turn on mobile data or connect to Wi-Fi, then try again.", color = Pulse.Muted)
                }
            },
            confirmButton = {
                Button(
                    onClick = { upstreamError = null; repo.openInternetPanel(this@MainActivity) },
                    colors = ButtonDefaults.buttonColors(containerColor = Pulse.Violet),
                ) { Text("Turn on internet") }
            },
            dismissButton = {
                TextButton(onClick = { upstreamError = null; onPower() }) { Text("Try again", color = Pulse.Cyan) }
            },
        )
    }
}
