package com.hydaui.launcher.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.compose.runtime.Immutable
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.roundToInt

@Immutable
data class Weather(
    val temperature: Int,
    val feelsLike: Int,
    val code: Int,
    val isDay: Boolean,
) {
    val condition: WeatherCondition get() = WeatherCondition.fromWmo(code)
}

/** WMO weather codes folded into what we can draw. */
enum class WeatherCondition(val label: String) {
    Clear("Clear"),
    PartlyCloudy("Partly cloudy"),
    Cloudy("Cloudy"),
    Fog("Fog"),
    Rain("Rain"),
    Snow("Snow"),
    Storm("Thunderstorm");

    companion object {
        fun fromWmo(code: Int): WeatherCondition = when (code) {
            0 -> Clear
            1, 2 -> PartlyCloudy
            3 -> Cloudy
            45, 48 -> Fog
            in 51..67, in 80..82 -> Rain
            in 71..77, 85, 86 -> Snow
            in 95..99 -> Storm
            else -> Cloudy
        }
    }
}

sealed interface WeatherState {
    data object NoPermission : WeatherState
    data object Loading : WeatherState
    data object Unavailable : WeatherState
    data class Ready(val weather: Weather) : WeatherState
}

/** Current conditions from Open-Meteo — free, keyless, and only coarse location leaves the phone. */
class WeatherRepository(private val context: Context) {
    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    suspend fun fetch(): WeatherState {
        if (!hasPermission()) return WeatherState.NoPermission
        val location = location() ?: return WeatherState.Unavailable
        return withContext(Dispatchers.IO) {
            try {
                WeatherState.Ready(request(location.latitude, location.longitude))
            } catch (_: Exception) {
                WeatherState.Unavailable
            }
        }
    }

    private fun request(lat: Double, lon: Double): Weather {
        val fahrenheit = Locale.getDefault().country in setOf("US", "LR", "MM", "BS", "BZ", "KY", "PW")
        val url = URL(
            String.format(
                Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.3f&longitude=%.3f" +
                    "&current=temperature_2m,apparent_temperature,weather_code,is_day%s",
                lat,
                lon,
                if (fahrenheit) "&temperature_unit=fahrenheit" else "",
            ),
        )
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 8_000
        conn.readTimeout = 8_000
        try {
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val current = JSONObject(body).getJSONObject("current")
            return Weather(
                temperature = current.getDouble("temperature_2m").roundToInt(),
                feelsLike = current.getDouble("apparent_temperature").roundToInt(),
                code = current.getInt("weather_code"),
                isDay = current.optInt("is_day", 1) == 1,
            )
        } finally {
            conn.disconnect()
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun location(): Location? {
        val lm = context.getSystemService(LocationManager::class.java) ?: return null
        val lastKnown = lm.getProviders(true)
            .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
        if (lastKnown != null) return lastKnown
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null

        val provider = when {
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            else -> return null
        }
        return withTimeoutOrNull(10_000) {
            suspendCancellableCoroutine<Location?> { cont ->
                val signal = CancellationSignal()
                cont.invokeOnCancellation { signal.cancel() }
                runCatching {
                    lm.getCurrentLocation(provider, signal, context.mainExecutor) { cont.resume(it) }
                }.onFailure { cont.resume(null) }
            }
        }
    }
}
