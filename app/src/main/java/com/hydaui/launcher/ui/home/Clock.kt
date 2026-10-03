package com.hydaui.launcher.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hydaui.launcher.ui.components.GlassTone
import com.hydaui.launcher.ui.components.glass
import com.hydaui.launcher.ui.components.pressable
import com.hydaui.launcher.ui.theme.Hyda
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/** Wall-clock time that changes on the minute — enough for everything but the second hand. */
@Composable
fun rememberMinute(): State<Long> = produceState(System.currentTimeMillis()) {
    while (true) {
        delay(60_000 - System.currentTimeMillis() % 60_000)
        value = System.currentTimeMillis()
    }
}

/**
 * The dial redraws every second but never recomposes: time lives in state read only while
 * drawing, and the second hand springs into each new second like a quartz movement.
 */
@Composable
fun AnalogClock(onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 148.dp) {
    val time = remember { mutableLongStateOf(System.currentTimeMillis()) }
    val secondAngle = remember { Animatable(secondOf(System.currentTimeMillis()) * 6f) }
    var dateLabel by remember { mutableStateOf(formatDate(System.currentTimeMillis())) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000 - System.currentTimeMillis() % 1_000)
            val now = System.currentTimeMillis()
            time.longValue = now
            dateLabel = formatDate(now) // Same string most ticks, so no recomposition.
            val second = secondOf(now)
            launch {
                if (second == 0) {
                    // Finish the lap at 360°, then quietly reset, so the hand never spins backwards.
                    secondAngle.animateTo(360f, TICK)
                    secondAngle.snapTo(0f)
                } else {
                    if (secondAngle.value > second * 6f) secondAngle.snapTo(0f)
                    secondAngle.animateTo(second * 6f, TICK)
                }
            }
        }
    }

    Box(modifier.size(size).pressable(onClick = onClick).glass(CircleShape, GlassTone.Milk, elevation = 14.dp)) {
        Text(
            "12",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = Hyda.InkSoft,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 14.dp),
        )
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 30.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(dateLabel.first, fontSize = 9.sp, color = Hyda.Ember, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.width(4.dp))
            Text(dateLabel.second, fontSize = 9.sp, color = Hyda.InkSoft, style = MaterialTheme.typography.labelSmall)
        }
        Canvas(Modifier.fillMaxSize().padding(12.dp)) {
            val cal = Calendar.getInstance().apply { timeInMillis = time.longValue }
            val minutes = cal.get(Calendar.MINUTE) + cal.get(Calendar.SECOND) / 60f
            val hours = cal.get(Calendar.HOUR) + minutes / 60f
            val r = this.size.minDimension / 2
            for (i in 1 until 12) { // "12" is lettered
                val major = i % 3 == 0
                val outer = r - 2.dp.toPx()
                val inner = outer - (if (major) 5.dp else 2.5.dp).toPx()
                val a = Math.toRadians(i * 30.0 - 90.0)
                drawLine(
                    color = if (major) Hyda.InkSoft else Hyda.InkFaint.copy(alpha = 0.7f),
                    start = center + Offset((cos(a) * inner).toFloat(), (sin(a) * inner).toFloat()),
                    end = center + Offset((cos(a) * outer).toFloat(), (sin(a) * outer).toFloat()),
                    strokeWidth = (if (major) 1.6.dp else 1.dp).toPx(),
                    cap = StrokeCap.Round,
                )
            }
            hand(hours * 30f, r * 0.42f, 4.dp.toPx(), Hyda.Ink)
            hand(minutes * 6f, r * 0.68f, 3.dp.toPx(), Hyda.Ink)
            hand(secondAngle.value, r * 0.80f, 1.2.dp.toPx(), Hyda.Ember, tail = r * 0.14f)
            drawCircle(Hyda.Ink, 3.5.dp.toPx())
            drawCircle(Color.White, 1.5.dp.toPx())
        }
    }
}

private val TICK = spring<Float>(dampingRatio = 0.42f, stiffness = 900f)

private fun secondOf(millis: Long): Int =
    Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.SECOND)

private fun formatDate(millis: Long): Pair<String, String> {
    val date = Date(millis)
    val weekday = SimpleDateFormat("EEE", Locale.getDefault()).format(date).uppercase()
    val day = SimpleDateFormat("d", Locale.getDefault()).format(date)
    return weekday to day
}

private fun DrawScope.hand(angleDeg: Float, length: Float, width: Float, color: Color, tail: Float = 0f) {
    val a = Math.toRadians(angleDeg - 90.0)
    val dir = Offset(cos(a).toFloat(), sin(a).toFloat())
    drawLine(color, center - dir * tail, center + dir * length, strokeWidth = width, cap = StrokeCap.Round)
}
