package com.pulse.hotspot.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pulse.hotspot.ui.theme.Pulse

@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Pulse.Glass)
            .border(1.dp, Brush.linearGradient(listOf(Pulse.GlassEdge, Color.Transparent, Pulse.GlassEdge)), RoundedCornerShape(24.dp))
            .padding(20.dp),
        content = content,
    )
}

@Composable
fun SectionTitle(text: String, icon: ImageVector? = null, tint: Color = Pulse.Lavender) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium, color = Pulse.Text)
    }
}

/** Big glowing power button with radar-style pulse rings while the hotspot is on. */
@Composable
fun PowerOrb(on: Boolean, busy: Boolean, onClick: () -> Unit, orbSize: Dp = 210.dp) {
    val transition = rememberInfiniteTransition(label = "orb")
    val wave by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "wave"
    )
    val breathe by transition.animateFloat(
        0.96f, 1.04f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breathe"
    )
    val ringColor by animateColorAsState(if (on) Pulse.Cyan else Pulse.Violet, label = "ring")
    val press = remember { MutableInteractionSource() }

    Box(Modifier.size(orbSize + 90.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val base = orbSize.toPx() / 2
            val maxR = this.size.minDimension / 2
            if (on || busy) {
                for (i in 0 until 3) {
                    val p = (wave + i / 3f) % 1f
                    drawCircle(ringColor.copy(alpha = (1f - p) * 0.45f), radius = base + (maxR - base) * p, style = Stroke(2.dp.toPx()))
                }
            } else {
                drawCircle(ringColor.copy(alpha = 0.18f), radius = base + 18.dp.toPx(), style = Stroke(1.dp.toPx()))
            }
            drawCircle(
                Brush.radialGradient(listOf(ringColor.copy(alpha = if (on) 0.45f else 0.2f), Color.Transparent), radius = maxR),
                radius = maxR,
            )
        }
        Box(
            Modifier
                .size(orbSize)
                .scale(if (on) breathe else 1f)
                .clip(CircleShape)
                .background(if (on) Pulse.onGradient else Pulse.offGradient)
                .border(2.dp, Brush.sweepGradient(listOf(Pulse.Cyan, Pulse.Violet, Pulse.Pink, Pulse.Cyan)), CircleShape)
                .clickable(interactionSource = press, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.PowerSettingsNew, null, tint = Color.White, modifier = Modifier.size(64.dp))
                Spacer(Modifier.padding(4.dp))
                Text(
                    when { busy -> "CHECKING"; on -> "SHARING"; else -> "TAP TO SHARE" },
                    color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

/** 240° arc gauge. */
@Composable
fun ArcGauge(fraction: Float, modifier: Modifier = Modifier, stroke: Dp = 14.dp, center: @Composable () -> Unit) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(700), label = "gauge")
    val danger = animated >= 0.9f
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = stroke.toPx()
            val arcSize = Size(this.size.width - w, this.size.height - w)
            val topLeft = Offset(w / 2, w / 2)
            drawArc(Pulse.GlassEdge, 150f, 240f, false, topLeft, arcSize, style = Stroke(w, cap = StrokeCap.Round))
            val brush = if (danger) Brush.sweepGradient(listOf(Pulse.Amber, Pulse.Red, Pulse.Amber))
            else Brush.sweepGradient(listOf(Pulse.Pink, Pulse.Cyan, Pulse.Lime, Pulse.Violet, Pulse.Pink))
            if (animated > 0f) drawArc(brush, 150f, 240f * animated, false, topLeft, arcSize, style = Stroke(w, cap = StrokeCap.Round))
        }
        center()
    }
}

@Composable
fun StatusChip(icon: ImageVector, label: String, ok: Boolean, modifier: Modifier = Modifier) {
    val color = if (ok) Pulse.Lime else Pulse.Red
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Text(label, color = Pulse.Text, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun Stat(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label.uppercase(), color = Pulse.Muted, style = MaterialTheme.typography.labelMedium)
        Text(value, color = accent, style = MaterialTheme.typography.titleMedium)
    }
}
