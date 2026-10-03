package com.hydaui.launcher.ui.home

import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import com.hydaui.launcher.ui.components.pressable
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hydaui.launcher.data.BatteryState
import com.hydaui.launcher.data.CalendarEvent
import com.hydaui.launcher.data.CalendarState
import com.hydaui.launcher.ui.components.GlassTone
import com.hydaui.launcher.ui.components.glass
import com.hydaui.launcher.ui.theme.Hyda
import java.util.Calendar
import java.util.Date

@Composable
fun Greeting(name: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.pressable(pressedScale = 0.97f, onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Hello,", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.width(10.dp))
            if (name.isNotBlank()) {
                Avatar(name.first().uppercaseChar().toString())
                Spacer(Modifier.width(10.dp))
                Text(name, style = MaterialTheme.typography.headlineMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            } else {
                Text("there", style = MaterialTheme.typography.headlineMedium)
            }
        }
        Text("Your summary for today", style = MaterialTheme.typography.headlineSmall, color = Hyda.InkFaint)
    }
}

@Composable
private fun Avatar(initial: String) {
    Box(
        Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Color(0xFFFFC7A8), Hyda.Ember, Hyda.Lilac)))
            .border(2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(initial, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

@Composable
fun SectionHeader(title: String, trailing: String?, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Hyda.InkFaint, modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, style = MaterialTheme.typography.labelMedium, color = Hyda.InkFaint)
    }
}

@Composable
fun TodayCard(
    state: CalendarState,
    now: Long,
    onConnect: () -> Unit,
    onOpenEvent: (CalendarEvent) -> Unit,
    onOpenCalendar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Crossfade(targetState = state, animationSpec = tween(380), label = "today", modifier = modifier.animateContentSize(spring(stiffness = 500f))) { s ->
        TodayContent(s, now, onConnect, onOpenEvent, onOpenCalendar)
    }
}

@Composable
private fun TodayContent(
    state: CalendarState,
    now: Long,
    onConnect: () -> Unit,
    onOpenEvent: (CalendarEvent) -> Unit,
    onOpenCalendar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        CalendarState.Loading -> Spacer(modifier.height(120.dp))
        CalendarState.NoPermission -> PromptCard(
            icon = Icons.Rounded.Event,
            title = "Bring in your day",
            body = "Let HydaUI read your calendar to show what's next, right here.",
            action = "Connect calendar",
            onAction = onConnect,
            modifier = modifier,
        )

        is CalendarState.Loaded -> if (state.events.isEmpty()) {
            PromptCard(
                icon = Icons.Rounded.Event,
                title = "A clear horizon",
                body = "Nothing on the calendar for the next two days.",
                action = "Open calendar",
                onAction = onOpenCalendar,
                modifier = modifier,
            )
        } else {
            EventStack(state.events.first(), state.events.drop(1).take(3), now, onOpenEvent, modifier)
        }
    }
}

/** The headline event on milky glass, with its details and what follows on frost beneath. */
@Composable
private fun EventStack(
    event: CalendarEvent,
    next: List<CalendarEvent>,
    now: Long,
    onOpen: (CalendarEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Column(modifier.fillMaxWidth().glass(RoundedCornerShape(32.dp)).padding(6.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .pressable(pressedScale = 0.97f) { onOpen(event) }
                .glass(RoundedCornerShape(26.dp), GlassTone.Milk, elevation = 6.dp)
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Text(event.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(Hyda.Accent))
                Spacer(Modifier.width(6.dp))
                Text(timeRange(context, event), style = MaterialTheme.typography.labelLarge, color = Hyda.InkSoft)
                Spacer(Modifier.weight(1f))
                Text(relative(event, now), style = MaterialTheme.typography.labelLarge, color = Hyda.Accent)
            }
        }
        val detail = event.location ?: event.description
        if (detail != null) {
            Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = if (next.isEmpty()) 14.dp else 4.dp)) {
                Icon(
                    if (event.location != null) Icons.Rounded.Place else Icons.Rounded.Event,
                    null,
                    tint = Hyda.InkFaint,
                    modifier = Modifier.padding(top = 2.dp).size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(detail, style = MaterialTheme.typography.bodyLarge, color = Hyda.InkSoft, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (next.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                next.forEach { e ->
                    Row(
                        Modifier.weight(1f, fill = false).pressable { onOpen(e) }.padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(9.dp).border(2.dp, Color(e.color).copy(alpha = 1f), CircleShape))
                        Spacer(Modifier.width(6.dp))
                        Text(e.title, style = MaterialTheme.typography.labelLarge, color = Hyda.InkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
fun PromptCard(
    icon: ImageVector,
    title: String,
    body: String,
    action: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().glass(RoundedCornerShape(32.dp)).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).glass(CircleShape, GlassTone.Milk, elevation = 2.dp), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Hyda.InkSoft, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(10.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge, color = Hyda.InkSoft)
        Spacer(Modifier.height(16.dp))
        PillButton(action, onAction)
    }
}

/** The solid blue call-to-action pill. */
@Composable
fun PillButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .pressable(pressedScale = 0.93f, onClick = onClick)
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(Color(0xFF3B8DFF), Hyda.Accent)))
            .padding(horizontal = 20.dp, vertical = 11.dp),
    ) {
        Text(text, color = Color.White, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun DefaultLauncherBanner(onSetDefault: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().glass(RoundedCornerShape(28.dp), GlassTone.Milk).padding(start = 18.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Home, null, tint = Hyda.Accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            "Make HydaUI your home screen",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(1f),
        )
        PillButton("Set", onSetDefault)
    }
}

@Composable
fun BatteryTile(battery: BatteryState, modifier: Modifier = Modifier, size: Dp = 112.dp) {
    Box(modifier.size(size)) {
        Box(Modifier.size(size).glass(RoundedCornerShape(34.dp)), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(72.dp)) {
                val stroke = 6.dp.toPx()
                drawArc(Hyda.InkFaint.copy(alpha = 0.18f), 0f, 360f, false, style = Stroke(stroke))
                drawArc(
                    Brush.sweepGradient(listOf(Color(0xFFFFA27A), Hyda.Ember, Color(0xFFFFA27A))),
                    -90f,
                    360f * battery.level / 100f,
                    false,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
            Text("${battery.level}%", style = MaterialTheme.typography.titleMedium)
        }
        if (battery.charging) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 6.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Hyda.Ember)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Bolt, "Charging", tint = Color.White, modifier = Modifier.size(13.dp))
            }
        }
    }
}

/** Next alarm, with the week laid out underneath and today lit. */
@Composable
fun AlarmTile(nextAlarm: Long?, now: Long, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier.height(112.dp).pressable(onClick = onClick).glass(RoundedCornerShape(34.dp)).padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Alarm, null, tint = Hyda.Ember, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text("Next alarm", style = MaterialTheme.typography.labelMedium, color = Hyda.InkSoft)
        }
        Spacer(Modifier.height(2.dp))
        if (nextAlarm != null) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(DateFormat.getTimeFormat(context).format(Date(nextAlarm)), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(6.dp))
                Text(
                    DateFormat.format("EEE", nextAlarm).toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Hyda.InkFaint,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
        } else {
            Text("None set", style = MaterialTheme.typography.titleLarge, color = Hyda.InkSoft)
        }
        Spacer(Modifier.weight(1f))
        WeekStrip(now)
    }
}

@Composable
private fun WeekStrip(now: Long) {
    val cal = Calendar.getInstance().apply { timeInMillis = now }
    val today = cal.get(Calendar.DAY_OF_WEEK)
    cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        repeat(7) {
            val dow = cal.get(Calendar.DAY_OF_WEEK)
            val letter = DateFormat.format("EEEEE", cal).toString()
            val isToday = dow == today
            Box(
                Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (isToday) Hyda.Accent else Color.Transparent),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    letter,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isToday) Color.White else Hyda.InkFaint,
                )
            }
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
    }
}

/** The gentle invitation to search — tap to open the drawer with the keyboard up. */
@Composable
fun AssistantPill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    // Read only in graphicsLayer, so the orb turns without recomposing anything.
    val spin = rememberInfiniteTransition(label = "orb").animateFloat(
        0f,
        360f,
        infiniteRepeatable(tween(6_000, easing = LinearEasing)),
        label = "spin",
    )
    Row(
        modifier
            .pressable(onClick = onClick)
            .glass(CircleShape, elevation = 8.dp)
            .padding(start = 10.dp, end = 20.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(22.dp).graphicsLayer { rotationZ = spin.value }) {
            drawCircle(Brush.sweepGradient(listOf(Hyda.Accent, Hyda.Lilac, Hyda.Ember.copy(alpha = 0.8f), Hyda.Ice, Hyda.Accent)))
            drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.85f), Color.Transparent)), radius = size.minDimension * 0.42f)
        }
        Spacer(Modifier.width(10.dp))
        Text("How can I help?", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun DockButton(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.size(60.dp).pressable(pressedScale = 0.88f, onClick = onClick).glass(CircleShape, elevation = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, label, tint = Hyda.InkSoft, modifier = Modifier.size(25.dp))
    }
}

private fun timeRange(context: android.content.Context, e: CalendarEvent): String {
    if (e.allDay) return "All day"
    val f = DateFormat.getTimeFormat(context)
    return "${f.format(Date(e.begin))} – ${f.format(Date(e.end))}"
}

private fun relative(e: CalendarEvent, now: Long): String = when {
    e.allDay -> if (DateUtils.isToday(e.begin)) "Today" else DateFormat.format("EEE", e.begin).toString()
    now in e.begin..e.end -> "Now"
    else -> DateUtils.getRelativeTimeSpanString(e.begin, now, DateUtils.MINUTE_IN_MILLIS).toString()
}
