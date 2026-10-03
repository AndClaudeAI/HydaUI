package com.hydaui.launcher.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import org.json.JSONObject

/** The aurora wallpaper's palette and pace. */
object AuroraLook {
    var top by mutableStateOf(Color(0xFFEDF0F6))
    var middle by mutableStateOf(Color(0xFFE1E6EF))
    var bottom by mutableStateOf(Color(0xFFD0D7E6))
    var glowA by mutableStateOf(Color(0xFFFAFBFD))
    var glowB by mutableStateOf(Color(0xFFB7C3E8))
    var glowC by mutableStateOf(Color(0xFFF8D7C2))
    var glowD by mutableStateOf(Color(0xFFD9C9F4))
    var prism by mutableFloatStateOf(0.11f)
    var driftSeconds by mutableIntStateOf(26)
}

/** How much light each grade of glass lets through. */
object GlassLook {
    var frostTop by mutableFloatStateOf(0.62f)
    var frostBottom by mutableFloatStateOf(0.32f)
    var milkTop by mutableFloatStateOf(0.96f)
    var milkBottom by mutableFloatStateOf(0.84f)
}

/** Motion tuning for the drawer and presses. */
object MotionLook {
    var drawerDamping by mutableFloatStateOf(0.88f)
    var drawerStiffness by mutableFloatStateOf(320f)
    /** Fraction of the screen height a finger travels to open the drawer fully. */
    var drawerTravel by mutableFloatStateOf(0.42f)
    var homeBlurDp by mutableFloatStateOf(22f)
    var pressScale by mutableFloatStateOf(0.95f)
}

object TextLook {
    var greetingSubtitle by mutableStateOf("Your summary for today")
    var assistantPrompt by mutableStateOf("How can I help?")
}

object WidgetLook {
    var battery by mutableStateOf(true)
    var alarm by mutableStateOf(true)
}

/**
 * Applies a look-and-feel config (config/hyda-config.json in the repo). Every key is optional;
 * anything missing or malformed falls back to the built-in default, so a typo can't break the
 * home screen.
 */
object Look {
    fun apply(json: JSONObject) {
        val colors = json.optJSONObject("colors") ?: JSONObject()
        Hyda.Ink = colors.color("ink", 0xFF14161B)
        Hyda.InkSoft = colors.color("inkSoft", 0xFF5E6472)
        Hyda.InkFaint = colors.color("inkFaint", 0xFF9097A6)
        Hyda.Accent = colors.color("accent", 0xFF1F7BFF)
        Hyda.Ember = colors.color("ember", 0xFFFF6B3D)
        Hyda.Lilac = colors.color("lilac", 0xFFB58CFF)
        Hyda.Ice = colors.color("ice", 0xFF6FE3FF)

        val aurora = json.optJSONObject("aurora") ?: JSONObject()
        AuroraLook.top = aurora.color("top", 0xFFEDF0F6)
        AuroraLook.middle = aurora.color("middle", 0xFFE1E6EF)
        AuroraLook.bottom = aurora.color("bottom", 0xFFD0D7E6)
        AuroraLook.glowA = aurora.color("glowA", 0xFFFAFBFD)
        AuroraLook.glowB = aurora.color("glowB", 0xFFB7C3E8)
        AuroraLook.glowC = aurora.color("glowC", 0xFFF8D7C2)
        AuroraLook.glowD = aurora.color("glowD", 0xFFD9C9F4)
        AuroraLook.prism = aurora.number("prism", 0.11f, 0f..0.5f)
        AuroraLook.driftSeconds = aurora.number("driftSeconds", 26f, 4f..300f).toInt()

        val glass = json.optJSONObject("glass") ?: JSONObject()
        GlassLook.frostTop = glass.number("frostTop", 0.62f, 0f..1f)
        GlassLook.frostBottom = glass.number("frostBottom", 0.32f, 0f..1f)
        GlassLook.milkTop = glass.number("milkTop", 0.96f, 0f..1f)
        GlassLook.milkBottom = glass.number("milkBottom", 0.84f, 0f..1f)

        val motion = json.optJSONObject("motion") ?: JSONObject()
        MotionLook.drawerDamping = motion.number("drawerDamping", 0.88f, 0.2f..1.5f)
        MotionLook.drawerStiffness = motion.number("drawerStiffness", 320f, 50f..3000f)
        MotionLook.drawerTravel = motion.number("drawerTravel", 0.42f, 0.15f..0.9f)
        MotionLook.homeBlurDp = motion.number("homeBlurDp", 22f, 0f..60f)
        MotionLook.pressScale = motion.number("pressScale", 0.95f, 0.7f..1f)

        val text = json.optJSONObject("text") ?: JSONObject()
        TextLook.greetingSubtitle = text.text("greetingSubtitle", "Your summary for today")
        TextLook.assistantPrompt = text.text("assistantPrompt", "How can I help?")

        val widgets = json.optJSONObject("widgets") ?: JSONObject()
        WidgetLook.battery = widgets.optBoolean("battery", true)
        WidgetLook.alarm = widgets.optBoolean("alarm", true)
    }

    private fun JSONObject.color(key: String, default: Long): Color {
        val raw = optString(key).removePrefix("#")
        val argb = when (raw.length) {
            6 -> raw.toLongOrNull(16)?.or(0xFF000000)
            8 -> raw.toLongOrNull(16)
            else -> null
        }
        return Color(argb ?: default)
    }

    private fun JSONObject.number(key: String, default: Float, range: ClosedFloatingPointRange<Float>): Float {
        val v = optDouble(key, Double.NaN)
        return if (v.isNaN()) default else v.toFloat().coerceIn(range)
    }

    private fun JSONObject.text(key: String, default: String): String =
        optString(key).trim().takeIf { it.isNotEmpty() }?.take(80) ?: default
}
