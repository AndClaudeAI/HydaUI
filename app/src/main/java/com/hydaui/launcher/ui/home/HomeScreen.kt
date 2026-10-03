package com.hydaui.launcher.ui.home

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.input.pointer.util.VelocityTracker
import com.hydaui.launcher.ui.DrawerState
import com.hydaui.launcher.ui.theme.WidgetLook
import androidx.compose.ui.unit.dp
import com.hydaui.launcher.data.BatteryState
import com.hydaui.launcher.data.CalendarEvent
import com.hydaui.launcher.data.CalendarState
import com.hydaui.launcher.data.UpdateState
import com.hydaui.launcher.data.WhatsNew
import com.hydaui.launcher.data.WeatherState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HomeState(
    val name: String,
    val weather: WeatherState,
    val calendar: CalendarState,
    val battery: BatteryState,
    val nextAlarm: Long?,
    val isDefaultLauncher: Boolean,
    val update: UpdateState = UpdateState.Idle,
    val whatsNew: WhatsNew? = null,
)

interface HomeActions {
    fun openDrawer(withSearch: Boolean)
    fun openSettings()
    fun expandNotifications()
    fun editName()
    fun weatherTapped()
    fun connectCalendar()
    fun openCalendar()
    fun openEvent(event: CalendarEvent)
    fun openClock()
    fun openAlarms()
    fun setDefaultLauncher()
    fun phone()
    fun messages()
    fun camera()
    fun allowInstalls()
    fun dismissWhatsNew()
}

@Composable
fun HomeScreen(state: HomeState, actions: HomeActions, drawer: DrawerState, modifier: Modifier = Modifier) {
    val now by rememberMinute()
    val currentActions by rememberUpdatedState(actions)
    val shadeThresholdPx = with(LocalDensity.current) { 80.dp.toPx() }

    // Drags that start on the scrolling widgets reach us as nested scroll: once the widgets can't
    // scroll any further up, the rest of the finger's travel pulls the drawer up with it.
    val nested = remember(drawer, shadeThresholdPx) {
        object : NestedScrollConnection {
            var shadePull = 0f

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Mid-pull, the drawer owns the gesture in both directions.
                if (source != NestedScrollSource.Drag || drawer.progress <= 0f) return Offset.Zero
                drawer.dragBy(-available.y)
                return Offset(0f, available.y)
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.Drag) return Offset.Zero
                if (available.y < 0f) {
                    drawer.dragBy(-available.y)
                    return Offset(0f, available.y)
                }
                if (available.y > 0f && drawer.progress <= 0f) {
                    shadePull += available.y
                    if (shadePull > shadeThresholdPx) {
                        shadePull = Float.NEGATIVE_INFINITY // once per gesture
                        currentActions.expandNotifications()
                    }
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                shadePull = 0f
                if (drawer.progress <= 0f && !drawer.isOpen) return Velocity.Zero
                drawer.settle(-available.y)
                return available
            }
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .nestedScroll(nested)
            .pointerInput(drawer) {
                // Drags on the bare background and the dock: the drawer rides the finger 1:1.
                val tracker = VelocityTracker()
                var travelled = 0f
                var lastSample = 0L
                var steeringDrawer = false
                detectVerticalDragGestures(
                    onDragStart = {
                        tracker.resetTracking()
                        travelled = 0f
                        lastSample = 0L
                        steeringDrawer = false
                    },
                    onDragEnd = {
                        if (steeringDrawer) {
                            drawer.settle(-tracker.calculateVelocity().y)
                        } else if (travelled > shadeThresholdPx) {
                            currentActions.expandNotifications()
                        }
                    },
                    onDragCancel = { if (steeringDrawer) drawer.settle(0f) },
                ) { change, dy ->
                    travelled += dy
                    // Track our own running total, not positions in this (scaling) layer.
                    if (change.uptimeMillis > lastSample) {
                        lastSample = change.uptimeMillis
                        tracker.addPosition(change.uptimeMillis, Offset(0f, travelled))
                    }
                    if (!steeringDrawer && (dy < 0f || drawer.progress > 0f)) steeringDrawer = true
                    if (steeringDrawer) drawer.dragBy(-dy)
                    change.consume()
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { currentActions.openSettings() })
            },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    AnalogClock(onClick = { currentActions.openClock() })
                    Spacer(Modifier.weight(1f))
                    WeatherTile(state.weather, onClick = { currentActions.weatherTapped() })
                }
                Spacer(Modifier.height(30.dp))
                Greeting(state.name, onClick = { currentActions.editName() })
                Spacer(Modifier.height(22.dp))
                UpdateBanner(
                    update = state.update,
                    whatsNew = state.whatsNew,
                    onAllowInstalls = { currentActions.allowInstalls() },
                    onDismissWhatsNew = { currentActions.dismissWhatsNew() },
                )
                if (!state.isDefaultLauncher) {
                    DefaultLauncherBanner(onSetDefault = { currentActions.setDefaultLauncher() })
                    Spacer(Modifier.height(16.dp))
                }
                SectionHeader(
                    "Today",
                    SimpleDateFormat("EEEE, d MMM", Locale.getDefault()).format(Date(now)),
                )
                Spacer(Modifier.height(10.dp))
                TodayCard(
                    state = state.calendar,
                    now = now,
                    onConnect = { currentActions.connectCalendar() },
                    onOpenEvent = { currentActions.openEvent(it) },
                    onOpenCalendar = { currentActions.openCalendar() },
                )
                Spacer(Modifier.height(14.dp))
                if (WidgetLook.battery || WidgetLook.alarm) {
                    Row(Modifier.fillMaxWidth()) {
                        if (WidgetLook.battery) BatteryTile(state.battery)
                        if (WidgetLook.battery && WidgetLook.alarm) Spacer(Modifier.width(14.dp))
                        if (WidgetLook.alarm) {
                            AlarmTile(state.nextAlarm, now, onClick = { currentActions.openAlarms() }, modifier = Modifier.weight(1f))
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            AssistantPill(
                onClick = { currentActions.openDrawer(true) },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                DockButton(Icons.Rounded.Call, "Phone", { currentActions.phone() })
                DockButton(Icons.Rounded.ChatBubbleOutline, "Messages", { currentActions.messages() })
                DockButton(Icons.Rounded.PhotoCamera, "Camera", { currentActions.camera() })
                DockButton(Icons.Rounded.Apps, "All apps", { currentActions.openDrawer(false) })
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}
