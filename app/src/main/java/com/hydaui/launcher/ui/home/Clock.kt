package com.hydaui.launcher.ui.home

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
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
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
import com.hydaui.launcher.ui.theme.Hyda
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/** Wall-clock time that ticks on the second boundary. */
@Composable
fun rememberNow(): State<Long> = produceState(System.currentTimeMillis()) {
    while (true) {
        value = System.currentTimeMillis()
        delay(1_000 - value % 1_000)
    }
}

@Composable
fun AnalogClock(now: Long, modifier: Modifier = Modifier, size: Dp = 148.dp) {
    val cal = Calendar.getInstance().apply { timeInMillis = now }
    val hours = cal.get(Calendar.HOUR) + cal.get(Calendar.MINUTE) / 60f
    val minutes = cal.get(Calendar.MINUTE) + cal.get(Calendar.SECOND) / 60f
    val seconds = cal.get(Calendar.SECOND).toFloat()
    val weekday = SimpleDateFormat("EEE", Locale.getDefault()).format(Date(now)).uppercase()
    val day = cal.get(Calendar.DAY_OF_MONTH).toString()

    Box(modifier.size(size).glass(CircleShape, GlassTone.Milk, elevation = 14.dp)) {
        Text(
            "12",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = Hyda.InkSoft,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 14.dp),
        )
        Row(
            Modifier.align(Alignment.Center).offset(x = (-14).dp, y = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(weekday, fontSize = 9.sp, color = Hyda.Ember, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.width(4.dp))
            Text(day, fontSize = 9.sp, color = Hyda.InkSoft, style = MaterialTheme.typography.labelSmall)
        }
        Canvas(Modifier.fillMaxSize().padding(12.dp)) {
            val r = this.size.minDimension / 2
            for (i in 0 until 12) {
                if (i == 0) continue // "12" is lettered
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
            hand(seconds * 6f, r * 0.80f, 1.2.dp.toPx(), Hyda.Ember, tail = r * 0.14f)
            drawCircle(Hyda.Ink, 3.5.dp.toPx())
            drawCircle(Color.White, 1.5.dp.toPx())
        }
    }
}

private fun DrawScope.hand(angleDeg: Float, length: Float, width: Float, color: Color, tail: Float = 0f) {
    val a = Math.toRadians(angleDeg - 90.0)
    val dir = Offset(cos(a).toFloat(), sin(a).toFloat())
    drawLine(color, center - dir * tail, center + dir * length, strokeWidth = width, cap = StrokeCap.Round)
}
