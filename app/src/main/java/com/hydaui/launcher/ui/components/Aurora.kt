package com.hydaui.launcher.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * The default wallpaper: a cool pearl gradient with slowly drifting pastel light
 * and a faint prismatic streak, like sun through a window onto frosted glass.
 */
@Composable
fun AuroraBackground(modifier: Modifier = Modifier) {
    val drift by rememberInfiniteTransition(label = "aurora").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(26_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift",
    )
    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(
            Brush.verticalGradient(
                listOf(Color(0xFFEDF0F6), Color(0xFFE1E6EF), Color(0xFFD0D7E6)),
            ),
        )
        glow(Offset(w * (0.10f + 0.12f * drift), h * 0.10f), w * 0.95f, Color(0xFFFAFBFD), 0.95f)
        glow(Offset(w * 1.00f, h * (0.30f + 0.08f * drift)), w * 0.80f, Color(0xFFB7C3E8), 0.70f)
        glow(Offset(w * (0.05f + 0.08f * drift), h * 0.88f), w * 0.90f, Color(0xFFF8D7C2), 0.60f)
        glow(Offset(w * 0.95f, h * (0.98f - 0.06f * drift)), w * 0.75f, Color(0xFFD9C9F4), 0.55f)

        // The prismatic streak, laid diagonally across the upper third.
        val a = 0.11f
        drawRect(
            Brush.linearGradient(
                0.00f to Color.Transparent,
                0.30f to Color(0xFFFF9A8B).copy(alpha = a),
                0.42f to Color(0xFFFFE08A).copy(alpha = a),
                0.54f to Color(0xFF9BF0C0).copy(alpha = a * 0.8f),
                0.66f to Color(0xFF8FD8FF).copy(alpha = a),
                0.80f to Color(0xFFC6A6FF).copy(alpha = a),
                1.00f to Color.Transparent,
                start = Offset(w * (0.05f + 0.06f * drift), h * 0.42f),
                end = Offset(w * (0.42f + 0.06f * drift), h * 0.26f),
            ),
        )
    }
}

private fun DrawScope.glow(center: Offset, radius: Float, color: Color, alpha: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            0f to color.copy(alpha = alpha),
            1f to color.copy(alpha = 0f),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}
