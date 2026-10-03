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
import androidx.compose.ui.unit.dp
import com.hydaui.launcher.data.BatteryState
import com.hydaui.launcher.data.CalendarEvent
import com.hydaui.launcher.data.CalendarState
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
}

@Composable
fun HomeScreen(state: HomeState, actions: HomeActions, modifier: Modifier = Modifier) {
    val now by rememberNow()
    val currentActions by rememberUpdatedState(actions)
    val thresholdPx = with(LocalDensity.current) { 90.dp.toPx() }

    // Swipes that start on the scrolling widget area arrive here as overscroll.
    val overscroll = remember(thresholdPx) {
        object : NestedScrollConnection {
            var pulled = 0f

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.Drag) return Offset.Zero
                pulled += available.y
                when {
                    pulled < -thresholdPx -> { pulled = 0f; currentActions.openDrawer(false) }
                    pulled > thresholdPx -> { pulled = 0f; currentActions.expandNotifications() }
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                pulled = 0f
                return Velocity.Zero
            }
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .nestedScroll(overscroll)
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        when {
                            total < -thresholdPx -> currentActions.openDrawer(false)
                            total > thresholdPx -> currentActions.expandNotifications()
                        }
                    },
                ) { change, dy ->
                    total += dy
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
                    AnalogClock(now, Modifier.pointerInput(Unit) { detectTapGestures { currentActions.openClock() } })
                    Spacer(Modifier.weight(1f))
                    WeatherTile(state.weather, onClick = { currentActions.weatherTapped() })
                }
                Spacer(Modifier.height(30.dp))
                Greeting(state.name, onClick = { currentActions.editName() })
                Spacer(Modifier.height(22.dp))
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
                Row(Modifier.fillMaxWidth()) {
                    BatteryTile(state.battery)
                    Spacer(Modifier.width(14.dp))
                    AlarmTile(state.nextAlarm, now, onClick = { currentActions.openAlarms() }, modifier = Modifier.weight(1f))
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
