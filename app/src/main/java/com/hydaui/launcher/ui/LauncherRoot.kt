package com.hydaui.launcher.ui

import android.Manifest
import android.app.ActivityOptions
import android.app.role.RoleManager
import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hydaui.launcher.LauncherViewModel
import com.hydaui.launcher.data.AppEntry
import com.hydaui.launcher.data.CalendarEvent
import com.hydaui.launcher.ui.components.AuroraBackground
import com.hydaui.launcher.ui.drawer.AppDrawer
import com.hydaui.launcher.ui.home.HomeActions
import com.hydaui.launcher.ui.home.HomeScreen
import com.hydaui.launcher.ui.home.HomeState
import com.hydaui.launcher.ui.settings.NameDialog
import com.hydaui.launcher.ui.settings.SettingsSheet
import kotlinx.coroutines.flow.Flow

@Composable
fun LauncherRoot(homePresses: Flow<Unit>, vm: LauncherViewModel = viewModel()) {
    val context = LocalContext.current
    val view = LocalView.current
    val keyboard = LocalSoftwareKeyboardController.current

    val apps by vm.apps.collectAsStateWithLifecycle()
    val name by vm.prefs.name.collectAsStateWithLifecycle()
    val useSystemWallpaper by vm.prefs.useSystemWallpaper.collectAsStateWithLifecycle()
    val weather by vm.weather.collectAsStateWithLifecycle()
    val calendar by vm.calendar.collectAsStateWithLifecycle()
    val battery by vm.battery.collectAsStateWithLifecycle()
    val nextAlarm by vm.nextAlarm.collectAsStateWithLifecycle()
    val isDefault by vm.isDefaultLauncher.collectAsStateWithLifecycle()

    var drawerOpen by rememberSaveable { mutableStateOf(false) }
    var focusSearch by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var settingsOpen by remember { mutableStateOf(false) }
    var nameDialogOpen by remember { mutableStateOf(false) }

    fun closeDrawer() {
        keyboard?.hide()
        drawerOpen = false
        focusSearch = false
        query = ""
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }
    LaunchedEffect(homePresses) {
        homePresses.collect {
            closeDrawer()
            settingsOpen = false
        }
    }
    // A launcher never "goes back" anywhere; back only closes what's open.
    BackHandler { if (drawerOpen) closeDrawer() }

    val calendarPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        vm.refreshCalendar()
    }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        vm.refreshWeather(force = true)
    }
    val homeRole = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        vm.refreshDefaultLauncher()
    }

    fun requestDefaultLauncher() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roles = context.getSystemService(RoleManager::class.java)
            if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_HOME) && !roles.isRoleHeld(RoleManager.ROLE_HOME)) {
                runCatching { homeRole.launch(roles.createRequestRoleIntent(RoleManager.ROLE_HOME)) }
                    .onSuccess { return }
            }
        }
        context.safeStart(Intent(Settings.ACTION_HOME_SETTINGS))
    }

    fun launchApp(app: AppEntry, bounds: Rect?) {
        val options = bounds?.let {
            ActivityOptions.makeClipRevealAnimation(view, it.left, it.top, it.width(), it.height()).toBundle()
        }
        runCatching { vm.appRepository.launch(app, bounds, options) }
            .onFailure { Toast.makeText(context, "Couldn't open ${app.label}", Toast.LENGTH_SHORT).show() }
        closeDrawer()
    }

    val actions = object : HomeActions {
        override fun openDrawer(withSearch: Boolean) {
            focusSearch = withSearch
            drawerOpen = true
        }

        override fun openSettings() {
            settingsOpen = true
        }

        override fun expandNotifications() = vm.device.expandNotifications()
        override fun editName() {
            nameDialogOpen = true
        }

        override fun weatherTapped() {
            if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                vm.refreshWeather(force = true)
            } else {
                locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }

        override fun connectCalendar() = calendarPermission.launch(Manifest.permission.READ_CALENDAR)

        override fun openCalendar() {
            val uri = CalendarContract.CONTENT_URI.buildUpon().appendPath("time")
                .also { ContentUris.appendId(it, System.currentTimeMillis()) }.build()
            context.safeStart(Intent(Intent.ACTION_VIEW, uri))
        }

        override fun openEvent(event: CalendarEvent) {
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.id)
            context.safeStart(
                Intent(Intent.ACTION_VIEW, uri)
                    .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.begin)
                    .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.end),
            )
        }

        override fun openClock() = context.safeStart(Intent(AlarmClock.ACTION_SHOW_ALARMS))
        override fun openAlarms() = context.safeStart(Intent(AlarmClock.ACTION_SHOW_ALARMS))
        override fun setDefaultLauncher() = requestDefaultLauncher()
        override fun phone() = context.safeStart(Intent(Intent.ACTION_DIAL))
        override fun messages() = context.safeStart(
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MESSAGING),
        )

        override fun camera() = context.safeStart(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
    }

    val drawerProgress by animateFloatAsState(
        targetValue = if (drawerOpen) 1f else 0f,
        animationSpec = tween(320),
        label = "drawer",
    )

    Box(Modifier.fillMaxSize()) {
        if (useSystemWallpaper) {
            // A whisper of frost so dark wallpapers still read under the glass.
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.18f)))
        } else {
            AuroraBackground()
        }

        HomeScreen(
            state = HomeState(
                name = name,
                weather = weather,
                calendar = calendar,
                battery = battery,
                nextAlarm = nextAlarm,
                isDefaultLauncher = isDefault,
            ),
            actions = actions,
            modifier = Modifier.graphicsLayer {
                val p = drawerProgress
                alpha = 1f - 0.7f * p
                scaleX = 1f - 0.06f * p
                scaleY = 1f - 0.06f * p
            },
        )

        AnimatedVisibility(
            visible = drawerOpen,
            enter = slideInVertically(spring(dampingRatio = 0.86f, stiffness = 420f)) { it / 3 } + fadeIn(tween(220)),
            exit = slideOutVertically(tween(260)) { it / 3 } + fadeOut(tween(200)),
        ) {
            AppDrawer(
                apps = apps,
                query = query,
                onQueryChange = { query = it },
                focusSearch = focusSearch,
                onLaunch = ::launchApp,
                onAppInfo = { runCatching { vm.appRepository.openAppInfo(it) } },
                onUninstall = { runCatching { vm.appRepository.uninstall(it) } },
                onWebSearch = { q ->
                    context.safeStart(Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, q))
                    closeDrawer()
                },
                onClose = ::closeDrawer,
            )
        }
    }

    if (settingsOpen) {
        SettingsSheet(
            name = name,
            useSystemWallpaper = useSystemWallpaper,
            isDefaultLauncher = isDefault,
            onEditName = { nameDialogOpen = true },
            onUseSystemWallpaperChange = vm.prefs::setUseSystemWallpaper,
            onPickWallpaper = {
                context.safeStart(Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Choose wallpaper"))
            },
            onSetDefault = ::requestDefaultLauncher,
            onSystemSettings = { context.safeStart(Intent(Settings.ACTION_SETTINGS)) },
            onDismiss = { settingsOpen = false },
        )
    }

    if (nameDialogOpen) {
        NameDialog(
            initial = name,
            onSave = {
                vm.prefs.setName(it)
                nameDialogOpen = false
            },
            onDismiss = { nameDialogOpen = false },
        )
    }
}

private fun Context.safeStart(intent: Intent) {
    try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, "No app found for that", Toast.LENGTH_SHORT).show()
    } catch (_: SecurityException) {
        Toast.makeText(this, "That app isn't available", Toast.LENGTH_SHORT).show()
    }
}
