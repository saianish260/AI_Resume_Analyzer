package com.pulse.hotspot.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pulse.hotspot.core.GB
import com.pulse.hotspot.core.GuardSettings
import com.pulse.hotspot.core.MB
import com.pulse.hotspot.core.toBytesLabel
import com.pulse.hotspot.ui.components.GlassCard
import com.pulse.hotspot.ui.components.SectionTitle
import com.pulse.hotspot.ui.theme.Pulse
import kotlin.math.roundToInt

private val capPresets = listOf(0L, 250 * MB, 500 * MB, GB, 2 * GB, 5 * GB, 10 * GB)

@Composable
fun pulseFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Pulse.Cyan,
    unfocusedBorderColor = Pulse.GlassEdge,
    focusedLabelColor = Pulse.Cyan,
    unfocusedLabelColor = Pulse.Muted,
    focusedTextColor = Pulse.Text,
    unfocusedTextColor = Pulse.Text,
    cursorColor = Pulse.Cyan,
)

@Composable
private fun pulseSwitch() = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = Pulse.Violet,
    uncheckedThumbColor = Pulse.Muted,
    uncheckedTrackColor = Pulse.Deep,
)

@Composable
private fun pulseSlider() = SliderDefaults.colors(
    thumbColor = Pulse.Cyan,
    activeTrackColor = Pulse.Cyan,
    inactiveTrackColor = Pulse.GlassEdge,
)

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Pulse.Text, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, color = Pulse.Muted, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange, colors = pulseSwitch())
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GuardScreen(
    settings: GuardSettings,
    onChange: ((GuardSettings) -> GuardSettings) -> Unit,
    onOpenHotspotSettings: () -> Unit,
    onClearHistory: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Guard", style = MaterialTheme.typography.displaySmall, color = Pulse.Text)
        Text("Rules that watch your hotspot while you get on with your day.", color = Pulse.Muted)

        GlassCard {
            SectionTitle("Session data budget", Icons.Rounded.DataUsage, Pulse.Cyan)
            Spacer(Modifier.height(4.dp))
            Text("You get an alert as you approach this and when you hit it.", color = Pulse.Muted)
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                capPresets.forEach { cap ->
                    FilterChip(
                        selected = settings.sessionCapBytes == cap,
                        onClick = { onChange { it.copy(sessionCapBytes = cap) } },
                        label = { Text(if (cap == 0L) "No cap" else cap.toBytesLabel().replace(".0 MB", " MB").replace(".00 GB", " GB")) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Pulse.Violet,
                            selectedLabelColor = Color.White,
                            labelColor = Pulse.Text,
                        ),
                    )
                }
            }
            if (settings.sessionCapBytes > 0) {
                Spacer(Modifier.height(12.dp))
                Text("Early warning at ${settings.warnPercent}%", color = Pulse.Text)
                Slider(
                    value = settings.warnPercent.toFloat(),
                    onValueChange = { v -> onChange { it.copy(warnPercent = v.roundToInt()) } },
                    valueRange = 50f..95f, steps = 8, colors = pulseSlider(),
                )
            }
        }

        GlassCard {
            SectionTitle("Battery guardian", Icons.Rounded.BatteryAlert, Pulse.Amber)
            Spacer(Modifier.height(8.dp))
            ToggleRow(
                "Warn on low battery", "Alerts you at ${settings.batteryThreshold}% while sharing and unplugged",
                settings.batteryGuard,
            ) { v -> onChange { it.copy(batteryGuard = v) } }
            if (settings.batteryGuard) Slider(
                value = settings.batteryThreshold.toFloat(),
                onValueChange = { v -> onChange { it.copy(batteryThreshold = v.roundToInt()) } },
                valueRange = 5f..50f, steps = 8, colors = pulseSlider(),
            )
        }

        GlassCard {
            SectionTitle("Forget-me-not reminder", Icons.Rounded.Timer, Pulse.Pink)
            Spacer(Modifier.height(8.dp))
            ToggleRow(
                "Remind me it's still on", "Every ${settings.reminderMinutes} minutes while sharing",
                settings.onTimeReminder,
            ) { v -> onChange { it.copy(onTimeReminder = v) } }
            if (settings.onTimeReminder) Slider(
                value = settings.reminderMinutes.toFloat(),
                onValueChange = { v -> onChange { it.copy(reminderMinutes = (v / 5).roundToInt() * 5) } },
                valueRange = 10f..180f, colors = pulseSlider(),
            )
        }

        GlassCard {
            SectionTitle("Cost meter", Icons.Rounded.Payments, Pulse.Lime)
            Spacer(Modifier.height(4.dp))
            Text("Enter what your plan charges per GB to see what each session costs and split it fairly.", color = Pulse.Muted)
            Spacer(Modifier.height(12.dp))
            var price by remember { mutableStateOf(if (settings.pricePerGb > 0) settings.pricePerGb.toString() else "") }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = settings.currency,
                    onValueChange = { v -> onChange { it.copy(currency = v.take(3)) } },
                    label = { Text("Currency") }, singleLine = true, colors = pulseFieldColors(),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { v ->
                        price = v
                        onChange { it.copy(pricePerGb = v.toDoubleOrNull() ?: 0.0) }
                    },
                    label = { Text("Price per GB") }, singleLine = true, colors = pulseFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(2f),
                )
            }
        }

        GlassCard {
            SectionTitle("Live meter", Icons.Rounded.NotificationsActive, Pulse.Lavender)
            Spacer(Modifier.height(8.dp))
            ToggleRow(
                "Keep watching in background", "Shows a live usage notification so alerts still fire when the app is closed",
                settings.backgroundGuard,
            ) { v -> onChange { it.copy(backgroundGuard = v) } }
        }

        GlassCard {
            SectionTitle("Per-device limits", Icons.Rounded.Devices, Pulse.Cyan)
            Spacer(Modifier.height(4.dp))
            Text(
                "Android doesn't let regular apps see or limit individual connected devices. Many phones " +
                    "(Samsung, Xiaomi, OnePlus, Pixel and others) have a built-in connected-devices list, " +
                    "data limit or blocklist in the hotspot settings.",
                color = Pulse.Muted,
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onOpenHotspotSettings,
                colors = ButtonDefaults.buttonColors(containerColor = Pulse.Violet),
            ) { Text("Open hotspot settings") }
        }

        OutlinedButton(onClick = onClearHistory, modifier = Modifier.fillMaxWidth()) {
            Text("Clear session history", color = Pulse.Red)
        }
        Spacer(Modifier.height(8.dp))
    }
}
