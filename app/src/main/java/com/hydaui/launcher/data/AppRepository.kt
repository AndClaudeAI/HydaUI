package com.hydaui.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.os.UserManager
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

@Immutable
data class AppEntry(
    val key: String,
    val label: String,
    val packageName: String,
    val component: ComponentName,
    val user: UserHandle,
    val icon: ImageBitmap,
)

/** Every launchable activity across the user's profiles (personal and work). */
class AppRepository(private val context: Context) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)

    suspend fun load(): List<AppEntry> = withContext(Dispatchers.IO) {
        val iconPx = (56 * context.resources.displayMetrics.density).toInt()
        userManager.userProfiles.flatMap { user ->
            launcherApps.getActivityList(null, user)
                .filter { it.componentName.packageName != context.packageName }
                .map { info ->
                    AppEntry(
                        key = "${info.componentName.flattenToShortString()}#${user.hashCode()}",
                        label = info.label.toString(),
                        packageName = info.componentName.packageName,
                        component = info.componentName,
                        user = user,
                        icon = info.getBadgedIcon(0).toBitmap(iconPx, iconPx).asImageBitmap(),
                    )
                }
        }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    }

    /** Emits whenever an app is installed, removed, updated or made (un)available. */
    fun changes(): Flow<Unit> = callbackFlow {
        val callback = object : LauncherApps.Callback() {
            override fun onPackageRemoved(packageName: String?, user: UserHandle?) {
                trySend(Unit)
            }

            override fun onPackageAdded(packageName: String?, user: UserHandle?) {
                trySend(Unit)
            }

            override fun onPackageChanged(packageName: String?, user: UserHandle?) {
                trySend(Unit)
            }

            override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) {
                trySend(Unit)
            }

            override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) {
                trySend(Unit)
            }
        }
        launcherApps.registerCallback(callback, Handler(Looper.getMainLooper()))
        awaitClose { launcherApps.unregisterCallback(callback) }
    }

    fun launch(app: AppEntry, sourceBounds: Rect?, options: Bundle?) {
        launcherApps.startMainActivity(app.component, app.user, sourceBounds, options)
    }

    fun openAppInfo(app: AppEntry) {
        launcherApps.startAppDetailsActivity(app.component, app.user, null, null)
    }

    fun uninstall(app: AppEntry) {
        context.startActivity(
            Intent(Intent.ACTION_DELETE, Uri.fromParts("package", app.packageName, null))
                .putExtra(Intent.EXTRA_USER, app.user)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
