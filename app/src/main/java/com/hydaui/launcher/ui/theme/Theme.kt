package com.hydaui.launcher.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The HydaUI palette: pale frost, soft ink, one blue for action and one ember for life.
 *
 * Every value is snapshot state, so a new look-and-feel config (see [Look]) repaints the
 * launcher in place — no reinstall, no restart.
 */
object Hyda {
    var Ink by mutableStateOf(Color(0xFF14161B))
    var InkSoft by mutableStateOf(Color(0xFF5E6472))
    var InkFaint by mutableStateOf(Color(0xFF9097A6))
    var Accent by mutableStateOf(Color(0xFF1F7BFF))
    var Ember by mutableStateOf(Color(0xFFFF6B3D))
    var Lilac by mutableStateOf(Color(0xFFB58CFF))
    var Ice by mutableStateOf(Color(0xFF6FE3FF))
    val Mist = Color(0xFFF4F6FA)
}

private val Sans = FontFamily.SansSerif

private val HydaTypography = Typography(
    displayLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Light, fontSize = 52.sp, letterSpacing = (-1.5).sp),
    headlineMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, letterSpacing = (-0.6).sp),
    headlineSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Light, fontSize = 22.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 21.sp, letterSpacing = (-0.3).sp),
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 19.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 11.sp),
)

@Composable
fun HydaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Hyda.Accent,
            onPrimary = Color.White,
            secondary = Hyda.Ember,
            background = Hyda.Mist,
            surface = Hyda.Mist,
            surfaceContainerLow = Color(0xFFF7F8FB),
            surfaceContainer = Color(0xFFF2F4F8),
            onSurface = Hyda.Ink,
            onSurfaceVariant = Hyda.InkSoft,
        ),
        typography = HydaTypography,
    ) {
        CompositionLocalProvider(LocalContentColor provides Hyda.Ink, content = content)
    }
}
