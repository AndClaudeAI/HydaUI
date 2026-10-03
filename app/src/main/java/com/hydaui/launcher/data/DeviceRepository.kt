package com.hydaui.launcher.data

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import androidx.compose.runtime.Immutable
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

@Immutable
data class BatteryState(val level: Int, val charging: Boolean)

/** Small, permission-free facts about the phone itself. */
class DeviceRepository(private val context: Context) {

    fun battery(): Flow<BatteryState> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.let { trySend(it.toBattery()) }
            }
        }
        val sticky = ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        sticky?.let { trySend(it.toBattery()) }
        awaitClose { context.unregisterReceiver(receiver) }
    }

    /** Trigger time of the next alarm the user set in their clock app, if any. */
    fun nextAlarm(): Long? =
        context.getSystemService(AlarmManager::class.java)?.nextAlarmClock?.triggerTime

    fun isDefaultLauncher(): Boolean {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = context.packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
        return resolved?.activityInfo?.packageName == context.packageName
    }

    /** Pulls down the notification shade. Hidden API, so it's best-effort. */
    @SuppressLint("WrongConstant", "PrivateApi")
    fun expandNotifications() {
        runCatching {
            val service = context.getSystemService("statusbar")
            Class.forName("android.app.StatusBarManager")
                .getMethod("expandNotificationsPanel")
                .invoke(service)
        }
    }

    private fun Intent.toBattery(): BatteryState {
        val level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        val status = getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        return BatteryState(
            level = if (level >= 0) level * 100 / scale else 0,
            charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL,
        )
    }
}
