package com.pulse.hotspot.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object Pulse {
    val Night = Color(0xFF0A0A1F)
    val Deep = Color(0xFF14103A)
    val Violet = Color(0xFF7C4DFF)
    val Lavender = Color(0xFFB388FF)
    val Cyan = Color(0xFF00E5FF)
    val Pink = Color(0xFFFF4FA3)
    val Lime = Color(0xFF3DFFA2)
    val Amber = Color(0xFFFFC94D)
    val Red = Color(0xFFFF5370)
    val Text = Color(0xFFF2F3FF)
    val Muted = Color(0xFF9AA0C3)
    val Glass = Color(0x14FFFFFF)
    val GlassEdge = Color(0x26FFFFFF)

    val background = Brush.verticalGradient(listOf(Deep, Night, Color(0xFF050512)))
    val onGradient = Brush.linearGradient(listOf(Cyan, Violet, Pink))
    val offGradient = Brush.linearGradient(listOf(Color(0xFF2A2550), Color(0xFF1A1638)))
}

private val scheme = darkColorScheme(
    primary = Pulse.Violet,
    onPrimary = Color.White,
    secondary = Pulse.Cyan,
    tertiary = Pulse.Pink,
    background = Pulse.Night,
    surface = Pulse.Deep,
    surfaceVariant = Color(0xFF221C4D),
    onSurface = Pulse.Text,
    onSurfaceVariant = Pulse.Muted,
    error = Pulse.Red,
    outline = Pulse.GlassEdge,
)

private val type = Typography(
    displaySmall = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp),
)

@Composable
fun PulseTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = type, content = content)
}
