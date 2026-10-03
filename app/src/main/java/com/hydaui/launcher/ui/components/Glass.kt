package com.hydaui.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hydaui.launcher.ui.theme.GlassLook

/** How much light a pane of glass lets through. */
enum class GlassTone {
    /** Barely-there frost: the background reads through. */
    Frost,

    /** Milky, almost opaque — for the card you should read first. */
    Milk,

    /** Cool smoked glass, for tiles that carry white type (weather). */
    Smoke,
}

/**
 * Frosted-glass surface: a top-lit translucent fill, a hairline highlight that fades
 * towards the bottom edge, and a soft, cool shadow.
 */
fun Modifier.glass(
    shape: Shape,
    tone: GlassTone = GlassTone.Frost,
    elevation: Dp = 10.dp,
): Modifier {
    val (top, bottom) = when (tone) {
        GlassTone.Frost -> Color.White.copy(alpha = GlassLook.frostTop) to Color.White.copy(alpha = GlassLook.frostBottom)
        GlassTone.Milk -> Color.White.copy(alpha = GlassLook.milkTop) to Color.White.copy(alpha = GlassLook.milkBottom)
        GlassTone.Smoke -> Color(0xFFC3C9D5).copy(alpha = 0.85f) to Color(0xFF9CA5B8).copy(alpha = 0.78f)
    }
    val rim = Brush.verticalGradient(
        listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.12f)),
    )
    val shadowTint = Color(0xFF3B4660)
    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = shadowTint.copy(alpha = 0.10f),
            spotColor = shadowTint.copy(alpha = 0.16f),
        )
        .clip(shape)
        .background(Brush.verticalGradient(listOf(top, bottom)))
        .border(1.dp, rim, shape)
}
