package com.pulse.hotspot.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.SignalCellularOff
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pulse.hotspot.core.BatteryInfo
import com.pulse.hotspot.core.GuardSettings
import com.pulse.hotspot.core.SessionRecord
import com.pulse.hotspot.core.SessionStats
import com.pulse.hotspot.core.UpstreamStatus
import com.pulse.hotspot.core.toBytesLabel
import com.pulse.hotspot.core.toDurationLabel
import com.pulse.hotspot.core.toSpeedLabel
import com.pulse.hotspot.ui.components.ArcGauge
import com.pulse.hotspot.ui.components.GlassCard
import com.pulse.hotspot.ui.components.PowerOrb
import com.pulse.hotspot.ui.components.SectionTitle
import com.pulse.hotspot.ui.components.Stat
import com.pulse.hotspot.ui.components.StatusChip
import com.pulse.hotspot.ui.theme.Pulse
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    on: Boolean,
    manual: Boolean,
    upstream: UpstreamStatus,
    battery: BatteryInfo,
    session: SessionStats?,
    settings: GuardSettings,
    history: List<SessionRecord>,
    onPower: () -> Unit,
    onTrackManual: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text("Hotspot Pulse", style = MaterialTheme.typography.displaySmall, color = Pulse.Text)
            Text(
                if (on) "Sharing via ${upstream.sourceLabel}" else "Your personal hotspot, under control",
                color = Pulse.Muted, style = MaterialTheme.typography.bodyMedium,
            )
        }

        PowerOrb(on = on, busy = false, onClick = onPower)

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip(
                if (upstream.mobileUsable) Icons.Rounded.SignalCellularAlt else Icons.Rounded.SignalCellularOff,
                if (upstream.mobileUsable) "Mobile data on" else "Mobile data off", upstream.mobileUsable,
            )
            StatusChip(
                if (upstream.wifiEnabled) Icons.Rounded.Wifi else Icons.Rounded.WifiOff,
                when { upstream.wifiConnected -> "Wi-Fi connected"; upstream.wifiEnabled -> "Wi-Fi on"; else -> "Wi-Fi off" },
                upstream.wifiConnected,
            )
            val lowBattery = !battery.charging && battery.percent <= settings.batteryThreshold
            StatusChip(
                if (lowBattery) Icons.Rounded.BatteryAlert else Icons.Rounded.BatteryChargingFull,
                "${battery.percent}%" + if (battery.charging) " charging" else "", !lowBattery,
            )
        }

        if (!on) {
            Text(
                if (upstream.canShare) "Ready. Tap the orb, then switch on Hotspot in the screen that opens."
                else "⚠ ${upstream.problem} Turn one on before sharing.",
                color = if (upstream.canShare) Pulse.Muted else Pulse.Amber,
                textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onTrackManual) { Text("Hotspot already on? Track this session", color = Pulse.Cyan) }
        } else if (manual) {
            Text("Tracking manually. Tap the orb when you've turned the hotspot off.", color = Pulse.Muted, textAlign = TextAlign.Center)
        }

        AnimatedVisibility(visible = session != null) {
            session?.let { SessionCard(it, settings) }
        }
        AnimatedVisibility(visible = session != null) {
            session?.let { SplitCard(it, settings) }
        }
        HistoryCard(history)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SessionCard(s: SessionStats, g: GuardSettings) {
    GlassCard {
        SectionTitle("Live session", Icons.Rounded.Insights, Pulse.Cyan)
        Spacer(Modifier.height(12.dp))
        val cap = g.sessionCapBytes
        val fraction = if (cap > 0) s.bytes.toFloat() / cap else 0f
        Row(verticalAlignment = Alignment.CenterVertically) {
            ArcGauge(fraction, Modifier.size(150.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(s.bytes.toBytesLabel(), color = Pulse.Text, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                    Text(if (cap > 0) "of ${cap.toBytesLabel()}" else "no cap set", color = Pulse.Muted, style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(Modifier.size(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("Speed", s.speedBps.toSpeedLabel(), Pulse.Cyan)
                Stat("On for", s.durationSec.toDurationLabel(), Pulse.Lavender)
                val eta = s.secondsToCap(cap)
                Stat(
                    "Cap in",
                    when { cap <= 0 -> "—"; eta == 0L -> "Reached!"; eta == null -> "Idle"; else -> "≈ ${eta.toDurationLabel()}" },
                    if (eta == 0L) Pulse.Red else Pulse.Lime,
                )
                if (g.pricePerGb > 0) Stat("Cost", String.format(Locale.US, "%s%.2f", g.currency, s.cost(g.pricePerGb)), Pulse.Amber)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Counts all traffic over your mobile connection while sharing, including this phone's own apps.",
            color = Pulse.Muted, style = MaterialTheme.typography.labelMedium,
        )
    }
}

/** Splits the session's data (and cost, if a price is set) between the people sharing it. */
@Composable
private fun SplitCard(s: SessionStats, g: GuardSettings) {
    var people by remember { mutableIntStateOf(2) }
    GlassCard {
        SectionTitle("Fair split", Icons.Rounded.Groups, Pulse.Pink)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (people > 1) people-- }) { Icon(Icons.Rounded.Remove, "Fewer people", tint = Pulse.Text) }
            Text("$people people", color = Pulse.Text, style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { if (people < 20) people++ }) { Icon(Icons.Rounded.Add, "More people", tint = Pulse.Text) }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text((s.bytes / people).toBytesLabel() + " each", color = Pulse.Pink, fontWeight = FontWeight.Bold)
                if (g.pricePerGb > 0) Text(
                    String.format(Locale.US, "%s%.2f each", g.currency, s.cost(g.pricePerGb) / people),
                    color = Pulse.Amber, style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun HistoryCard(history: List<SessionRecord>) {
    GlassCard {
        SectionTitle("Recent sessions", Icons.Rounded.History)
        Spacer(Modifier.height(12.dp))
        if (history.isEmpty()) {
            Text("Your sessions will show up here.", color = Pulse.Muted)
            return@GlassCard
        }
        val recent = history.take(12).reversed()
        val max = recent.maxOf { it.bytes }.coerceAtLeast(1)
        Canvas(Modifier.fillMaxWidth().height(90.dp)) {
            val gap = 8.dp.toPx()
            val w = (size.width - gap * (recent.size - 1)) / recent.size
            recent.forEachIndexed { i, r ->
                val h = (size.height * r.bytes / max).coerceAtLeast(4.dp.toPx())
                drawRoundRect(
                    Brush.verticalGradient(listOf(Pulse.Cyan, Pulse.Violet)),
                    topLeft = Offset(i * (w + gap), size.height - h),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(6.dp.toPx()),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        val fmt = SimpleDateFormat("d MMM, HH:mm", Locale.getDefault())
        history.take(4).forEach { r ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(Pulse.Violet))
                Spacer(Modifier.size(10.dp))
                Text(fmt.format(Date(r.startedAt)), color = Pulse.Text, modifier = Modifier.weight(1f))
                Text(((r.endedAt - r.startedAt) / 1000).toDurationLabel(), color = Pulse.Muted)
                Spacer(Modifier.size(12.dp))
                Text(r.bytes.toBytesLabel(), color = Pulse.Cyan, fontWeight = FontWeight.SemiBold)
            }
        }
        Text("Total: ${history.sumOf { it.bytes }.toBytesLabel()} over ${history.size} sessions", color = Pulse.Muted, style = MaterialTheme.typography.labelMedium)
    }
}
