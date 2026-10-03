package com.hydaui.launcher.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract.Instances
import androidx.compose.runtime.Immutable
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Immutable
data class CalendarEvent(
    val id: Long,
    val title: String,
    val begin: Long,
    val end: Long,
    val location: String?,
    val description: String?,
    val allDay: Boolean,
    val color: Int,
)

sealed interface CalendarState {
    data object Loading : CalendarState
    data object NoPermission : CalendarState
    data class Loaded(val events: List<CalendarEvent>) : CalendarState
}

class CalendarRepository(private val context: Context) {
    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    /** Events that haven't ended yet, within the next [windowHours]. */
    suspend fun upcoming(windowHours: Int = 48): CalendarState = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext CalendarState.NoPermission
        val now = System.currentTimeMillis()
        val uri = Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, now)
            ContentUris.appendId(it, now + windowHours * 3_600_000L)
        }.build()
        val projection = arrayOf(
            Instances.EVENT_ID,
            Instances.TITLE,
            Instances.BEGIN,
            Instances.END,
            Instances.EVENT_LOCATION,
            Instances.DESCRIPTION,
            Instances.ALL_DAY,
            Instances.DISPLAY_COLOR,
        )
        val events = mutableListOf<CalendarEvent>()
        try {
            context.contentResolver.query(
                uri,
                projection,
                "${Instances.END} >= ?",
                arrayOf(now.toString()),
                "${Instances.ALL_DAY} ASC, ${Instances.BEGIN} ASC",
            )?.use { c ->
                while (c.moveToNext() && events.size < 12) {
                    events += CalendarEvent(
                        id = c.getLong(0),
                        title = c.getString(1)?.takeIf { it.isNotBlank() } ?: "Untitled event",
                        begin = c.getLong(2),
                        end = c.getLong(3),
                        location = c.getString(4)?.takeIf { it.isNotBlank() },
                        description = c.getString(5)?.trim()?.takeIf { it.isNotBlank() },
                        allDay = c.getInt(6) == 1,
                        color = c.getInt(7),
                    )
                }
            }
        } catch (_: SecurityException) {
            return@withContext CalendarState.NoPermission
        }
        // Timed events first, in order; all-day events after them.
        CalendarState.Loaded(events.sortedWith(compareBy({ it.allDay }, { it.begin })))
    }
}
