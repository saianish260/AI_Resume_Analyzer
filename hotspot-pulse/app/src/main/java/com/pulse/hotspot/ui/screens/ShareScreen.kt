package com.pulse.hotspot.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.pulse.hotspot.core.GuardSettings
import com.pulse.hotspot.core.WifiQr
import com.pulse.hotspot.ui.components.GlassCard
import com.pulse.hotspot.ui.components.SectionTitle
import com.pulse.hotspot.ui.theme.Pulse

private val securities = listOf("WPA" to "WPA2/WPA3", "WEP" to "WEP", "nopass" to "Open")

@Composable
fun ShareScreen(
    settings: GuardSettings,
    onChange: ((GuardSettings) -> GuardSettings) -> Unit,
    onOpenHotspotSettings: () -> Unit,
) {
    val context = LocalContext.current
    var showPass by remember { mutableStateOf(false) }
    val ready = settings.ssid.isNotBlank() && (settings.security == "nopass" || settings.password.length >= 8)
    val payload = WifiQr.payload(settings.ssid, settings.password, settings.security, settings.hiddenNetwork)
    val qr = remember(payload, ready) { if (ready) WifiQr.bitmap(payload).asImageBitmap() else null }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Share", style = MaterialTheme.typography.displaySmall, color = Pulse.Text)
        Text("Friends scan the code with their camera and join instantly, with no typing.", color = Pulse.Muted)

        GlassCard {
            SectionTitle("Instant join code", Icons.Rounded.QrCode2, Pulse.Cyan)
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (qr != null) {
                    Box(
                        Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White).padding(14.dp)
                    ) { Image(qr, "Wi-Fi QR code", Modifier.size(230.dp)) }
                } else {
                    Box(
                        Modifier.size(230.dp).clip(RoundedCornerShape(20.dp)).background(Pulse.Glass),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Enter your hotspot name and password\n(8+ characters) below",
                            color = Pulse.Muted, modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
            if (ready) {
                Spacer(Modifier.height(12.dp))
                Text(settings.ssid, color = Pulse.Text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }

        GlassCard {
            SectionTitle("Hotspot details")
            Spacer(Modifier.height(4.dp))
            Text("Copy these from your hotspot settings. They're stored only on this phone.", color = Pulse.Muted)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = settings.ssid,
                onValueChange = { v -> onChange { it.copy(ssid = v) } },
                label = { Text("Hotspot name") }, singleLine = true, colors = pulseFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            if (settings.security != "nopass") OutlinedTextField(
                value = settings.password,
                onValueChange = { v -> onChange { it.copy(password = v) } },
                label = { Text("Password") }, singleLine = true, colors = pulseFieldColors(),
                visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    Row {
                        IconButton(onClick = { showPass = !showPass }) {
                            Icon(if (showPass) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, "Show password", tint = Pulse.Muted)
                        }
                        IconButton(onClick = { copy(context, settings.password) }) {
                            Icon(Icons.Rounded.ContentCopy, "Copy password", tint = Pulse.Muted)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                securities.forEach { (key, label) ->
                    FilterChip(
                        selected = settings.security == key,
                        onClick = { onChange { it.copy(security = key) } },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Pulse.Violet, selectedLabelColor = Color.White, labelColor = Pulse.Text,
                        ),
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Hidden network", color = Pulse.Text, modifier = Modifier.weight(1f))
                Switch(
                    checked = settings.hiddenNetwork,
                    onCheckedChange = { v -> onChange { it.copy(hiddenNetwork = v) } },
                    colors = SwitchDefaults.colors(checkedTrackColor = Pulse.Violet),
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onOpenHotspotSettings,
                colors = ButtonDefaults.buttonColors(containerColor = Pulse.Violet),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Find them in hotspot settings") }
        }
        Spacer(Modifier.height(8.dp))
    }
}

private fun copy(context: Context, text: String) {
    val cm = context.getSystemService(ClipboardManager::class.java)
    cm.setPrimaryClip(ClipData.newPlainText("Hotspot password", text))
    Toast.makeText(context, "Password copied", Toast.LENGTH_SHORT).show()
}
