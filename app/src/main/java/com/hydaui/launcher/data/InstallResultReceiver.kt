package com.hydaui.launcher.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log
import androidx.core.content.IntentCompat

/**
 * Hears back from PackageInstaller. When Android insists on a confirmation (Android 11 and
 * older, or a first self-update), it hands us the dialog to show; on success the system
 * restarts HydaUI on the new version by itself.
 */
class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)
                confirm?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)?.let { runCatching { context.startActivity(it) } }
            }

            PackageInstaller.STATUS_SUCCESS -> Unit

            else -> Log.w(
                "HydaUI",
                "Self-update failed ($status): ${intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)}",
            )
        }
    }
}
