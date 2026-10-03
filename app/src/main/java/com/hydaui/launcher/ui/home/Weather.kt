package com.hydaui.launcher.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import com.hydaui.launcher.ui.components.pressable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hydaui.launcher.data.WeatherCondition
import com.hydaui.launcher.data.WeatherState
import com.hydaui.launcher.ui.components.GlassTone
import com.hydaui.launcher.ui.components.glass

/** Smoked-glass temperature tile with the sky drawn spilling over its left edge. */
@Composable
fun WeatherTile(state: WeatherState, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.size(width = 176.dp, height = 124.dp)) {
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .size(118.dp)
                .pressable(onClick = onClick)
                .glass(RoundedCornerShape(30.dp), GlassTone.Smoke, elevation = 14.dp)
                .padding(14.dp),
        ) {
            Crossfade(state, animationSpec = tween(420), label = "weather", modifier = Modifier.matchParentSize()) { state ->
                Box(Modifier.matchParentSize()) {
                    when (state) {
                        is WeatherState.Ready -> Column(Modifier.align(Alignment.TopEnd), horizontalAlignment = Alignment.End) {
                            Text(
                                "${state.weather.temperature}°",
                                color = Color.White,
                                style = MaterialTheme.typography.displayLarge.copy(fontSize = 44.sp),
                            )
                            Text(
                                "Feels like ${state.weather.feelsLike}°",
                                color = Color.White.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }

                        WeatherState.Loading -> Text(
                            "—°",
                            color = Color.White,
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 44.sp),
                            modifier = Modifier.align(Alignment.TopEnd),
                        )

                        WeatherState.NoPermission, WeatherState.Unavailable -> Column(
                            Modifier.align(Alignment.BottomEnd),
                            horizontalAlignment = Alignment.End,
                        ) {
                            Icon(Icons.Rounded.MyLocation, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Text(
                                if (state == WeatherState.NoPermission) "Tap for\nlocal weather" else "Weather\nunavailable",
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                lineHeight = 14.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                            )
                        }
                    }
                }
            }
        }
        val (condition, isDay) = when (state) {
            is WeatherState.Ready -> state.weather.condition to state.weather.isDay
            else -> WeatherCondition.Cloudy to true
        }
        // The sky drifts gently, as skies do.
        val bob = rememberInfiniteTransition(label = "sky").animateFloat(
            initialValue = -1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(4_200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "bob",
        )
        Crossfade(condition to isDay, animationSpec = tween(600), label = "glyph", modifier = Modifier.align(Alignment.CenterStart)) { (c, day) ->
            WeatherGlyph(
                c,
                day,
                Modifier
                    .size(96.dp)
                    .graphicsLayer { translationY = bob.value * 3.dp.toPx(); translationX = bob.value * 1.5.dp.toPx() },
            )
        }
    }
}

@Composable
fun WeatherGlyph(condition: WeatherCondition, isDay: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        when (condition) {
            WeatherCondition.Clear -> if (isDay) sun(center, size.minDimension * 0.24f) else moon()
            WeatherCondition.PartlyCloudy -> {
                if (isDay) sun(Offset(size.width * 0.62f, size.height * 0.36f), size.minDimension * 0.18f) else moon()
                cloud(light = true)
            }
            WeatherCondition.Cloudy, WeatherCondition.Fog -> cloud(light = condition == WeatherCondition.Fog)
            WeatherCondition.Rain -> {
                cloud(light = false)
                drops()
            }
            WeatherCondition.Snow -> {
                cloud(light = true)
                flakes()
            }
            WeatherCondition.Storm -> {
                cloud(light = false)
                bolt()
            }
        }
    }
}

private fun DrawScope.sun(c: Offset, r: Float) {
    drawCircle(
        Brush.radialGradient(0f to Color(0xFFFFD27A).copy(alpha = 0.55f), 1f to Color.Transparent, center = c, radius = r * 2.2f),
        radius = r * 2.2f,
        center = c,
    )
    drawCircle(
        Brush.radialGradient(0f to Color(0xFFFFE7A3), 1f to Color(0xFFFF9E45), center = c - Offset(r * 0.3f, r * 0.3f), radius = r * 1.4f),
        radius = r,
        center = c,
    )
}

private fun DrawScope.moon() {
    val r = size.minDimension * 0.22f
    val c = Offset(size.width * 0.58f, size.height * 0.36f)
    val disc = Path().apply { addOval(Rect(c, r)) }
    val bite = Path().apply { addOval(Rect(c + Offset(r * 0.55f, -r * 0.35f), r)) }
    val crescent = Path().apply { op(disc, bite, PathOperation.Difference) }
    drawPath(crescent, Brush.linearGradient(listOf(Color(0xFFFFF4D6), Color(0xFFE8D7A8)), c - Offset(r, r), c + Offset(r, r)))
}

/** A soft, many-puffed cumulus; [light] for a pale cloud, otherwise a heavy slate one. */
private fun DrawScope.cloud(light: Boolean) {
    val w = size.width
    val h = size.height
    val body = if (light) Color(0xFFE9EDF4) else Color(0xFF4E5666)
    val rim = if (light) Color(0xFFFFFFFF) else Color(0xFF8A93A6)
    val puffs = listOf(
        Offset(0.26f, 0.60f) to 0.20f,
        Offset(0.44f, 0.46f) to 0.25f,
        Offset(0.64f, 0.52f) to 0.22f,
        Offset(0.78f, 0.64f) to 0.15f,
        Offset(0.48f, 0.66f) to 0.22f,
    )
    puffs.forEach { (p, r) ->
        val c = Offset(p.x * w, p.y * h)
        val radius = r * w
        drawCircle(
            Brush.radialGradient(
                0f to body,
                0.72f to body.copy(alpha = 0.92f),
                1f to body.copy(alpha = 0f),
                center = c,
                radius = radius,
            ),
            radius = radius,
            center = c,
        )
    }
    // Sunlit tops.
    puffs.take(3).forEach { (p, r) ->
        val c = Offset(p.x * w - r * w * 0.25f, p.y * h - r * w * 0.35f)
        val radius = r * w * 0.6f
        drawCircle(
            Brush.radialGradient(0f to rim.copy(alpha = 0.7f), 1f to rim.copy(alpha = 0f), center = c, radius = radius),
            radius = radius,
            center = c,
        )
    }
}

private fun DrawScope.drops() {
    val w = size.width
    val h = size.height
    listOf(0.34f to 0.84f, 0.50f to 0.90f, 0.66f to 0.84f, 0.42f to 0.97f).forEach { (x, y) ->
        drawLine(
            Color(0xFF7FB2FF).copy(alpha = 0.8f),
            Offset(x * w, y * h - 6.dp.toPx()),
            Offset(x * w - 2.dp.toPx(), y * h),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.flakes() {
    listOf(0.34f to 0.86f, 0.52f to 0.92f, 0.68f to 0.85f).forEach { (x, y) ->
        drawCircle(Color.White, 2.5.dp.toPx(), Offset(x * size.width, y * size.height))
        drawCircle(Color(0xFFB9C4D8), 2.5.dp.toPx(), Offset(x * size.width, y * size.height), style = Stroke(0.8.dp.toPx()))
    }
}

private fun DrawScope.bolt() {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(0.52f * w, 0.70f * h)
        lineTo(0.44f * w, 0.86f * h)
        lineTo(0.52f * w, 0.86f * h)
        lineTo(0.46f * w, 0.99f * h)
        lineTo(0.60f * w, 0.80f * h)
        lineTo(0.52f * w, 0.80f * h)
        lineTo(0.58f * w, 0.70f * h)
        close()
    }
    drawPath(path, Color(0xFFFFC94A))
}
