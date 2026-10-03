package com.hydaui.launcher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hydaui.launcher.data.AppEntry
import com.hydaui.launcher.data.AppRepository
import com.hydaui.launcher.data.BatteryState
import com.hydaui.launcher.data.CalendarRepository
import com.hydaui.launcher.data.CalendarState
import com.hydaui.launcher.data.DeviceRepository
import com.hydaui.launcher.data.Prefs
import com.hydaui.launcher.data.UpdateState
import com.hydaui.launcher.data.Updater
import com.hydaui.launcher.data.WeatherRepository
import com.hydaui.launcher.data.WeatherState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LauncherViewModel(app: Application) : AndroidViewModel(app) {
    val appRepository = AppRepository(app)
    val device = DeviceRepository(app)
    val prefs = Prefs(app)
    private val calendarRepository = CalendarRepository(app)
    private val weatherRepository = WeatherRepository(app)
    val updater = Updater(app)

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps.asStateFlow()

    private val _calendar = MutableStateFlow<CalendarState>(CalendarState.Loading)
    val calendar: StateFlow<CalendarState> = _calendar.asStateFlow()

    private val _weather = MutableStateFlow<WeatherState>(WeatherState.Loading)
    val weather: StateFlow<WeatherState> = _weather.asStateFlow()

    private val _nextAlarm = MutableStateFlow<Long?>(null)
    val nextAlarm: StateFlow<Long?> = _nextAlarm.asStateFlow()

    private val _isDefaultLauncher = MutableStateFlow(true)
    val isDefaultLauncher: StateFlow<Boolean> = _isDefaultLauncher.asStateFlow()

    val battery: StateFlow<BatteryState> = device.battery()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BatteryState(0, false))

    private var weatherFetchedAt = 0L

    init {
        updater.noteLaunch()
        viewModelScope.launch {
            appRepository.changes()
                .onStart { emit(Unit) }
                .conflate()
                .collect { _apps.value = appRepository.load() }
        }
    }

    /** Called every time the home screen comes back to the foreground. */
    fun refresh() {
        _nextAlarm.value = device.nextAlarm()
        _isDefaultLauncher.value = device.isDefaultLauncher()
        refreshCalendar()
        refreshWeather(force = false)
        // Coming back from "Install unknown apps" with permission granted: try again right away.
        val waitingOnPermission = updater.state.value is UpdateState.NeedsPermission &&
            getApplication<Application>().packageManager.canRequestPackageInstalls()
        viewModelScope.launch { updater.checkForUpdate(force = waitingOnPermission) }
    }

    fun checkForUpdatesNow() {
        viewModelScope.launch { updater.checkForUpdate(force = true) }
    }

    fun refreshCalendar() {
        viewModelScope.launch { _calendar.value = calendarRepository.upcoming() }
    }

    fun refreshWeather(force: Boolean) {
        val fresh = System.currentTimeMillis() - weatherFetchedAt < WEATHER_TTL_MS
        if (!force && fresh && _weather.value is WeatherState.Ready) return
        viewModelScope.launch {
            if (_weather.value !is WeatherState.Ready) _weather.value = WeatherState.Loading
            val result = weatherRepository.fetch()
            if (result is WeatherState.Ready) weatherFetchedAt = System.currentTimeMillis()
            // Keep showing the last good reading rather than blanking on a flaky network.
            if (result is WeatherState.Ready || _weather.value !is WeatherState.Ready) _weather.value = result
        }
    }

    fun refreshDefaultLauncher() {
        _isDefaultLauncher.value = device.isDefaultLauncher()
    }

    private companion object {
        const val WEATHER_TTL_MS = 30 * 60 * 1000L
    }
}
