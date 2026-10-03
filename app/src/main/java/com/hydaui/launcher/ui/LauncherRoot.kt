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
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
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
import com.hydaui.launcher.ui.theme.MotionLook
import kotlinx.coroutines.flow.Flow

@Composable
fun LauncherRoot(homePresses: Flow<Unit>, vm: LauncherViewModel = viewModel()) {
    val context = LocalContext.current
    val view = LocalView.current
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    val apps by vm.apps.collectAsStateWithLifecycle()
    val name by vm.prefs.name.collectAsStateWithLifecycle()
    val useSystemWallpaper by vm.prefs.useSystemWallpaper.collectAsStateWithLifecycle()
    val weather by vm.weather.collectAsStateWithLifecycle()
    val calendar by vm.calendar.collectAsStateWithLifecycle()
    val battery by vm.battery.collectAsStateWithLifecycle()
    val nextAlarm by vm.nextAlarm.collectAsStateWithLifecycle()
    val isDefault by vm.isDefaultLauncher.collectAsStateWithLifecycle()
    val update by vm.updater.state.collectAsStateWithLifecycle()
    val whatsNew by vm.updater.whatsNew.collectAsStateWithLifecycle()

    val drawer = rememberDrawerState()
    val gridState = rememberLazyGridState()
    val focusManager = LocalFocusManager.current
    var wantsSearch by remember { mutableStateOf(false) }
    var focusSearch by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var settingsOpen by remember { mutableStateOf(false) }
    var nameDialogOpen by remember { mutableStateOf(false) }

    // Zooms the home screen in from slightly behind the glass whenever you come back to it.
    val homeEntrance = remember { Animatable(1f) }
    var cameBack by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        // An app now covers us: tidy the drawer away instantly instead of animating it out
        // underneath the app's own opening animation.
        drawer.snapClosed()
        cameBack = true
    }
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        if (cameBack) {
            cameBack = false
            scope.launch {
                homeEntrance.snapTo(0f)
                homeEntrance.animateTo(1f, spring(dampingRatio = 0.82f, stiffness = 260f))
            }
        }
    }
    LaunchedEffect(homePresses) {
        homePresses.collect {
            drawer.close()
            settingsOpen = false
        }
    }
    // Raise the keyboard only once the drawer has landed; resizing mid-flight makes it stutter.
    LaunchedEffect(drawer) {
        snapshotFlow { drawer.isOpen && drawer.progress > 0.97f }
            .distinctUntilChanged()
            .collect { landed ->
                if (landed && wantsSearch) focusSearch = true
            }
    }
    // As soon as the drawer starts to leave, the keyboard goes first; once it's gone, reset.
    LaunchedEffect(drawer) {
        snapshotFlow { drawer.isOpen to (drawer.progress <= 0f) }
            .distinctUntilChanged()
            .collect { (open, closed) ->
                if (!open) {
                    keyboard?.hide()
                    focusManager.clearFocus()
                    focusSearch = false
                    wantsSearch = false
                }
                if (!open && closed) {
                    query = ""
                    gridState.scrollToItem(0)
                }
            }
    }
    // A launcher never "goes back" anywhere; back only closes what's open.
    BackHandler { if (drawer.isOpen) drawer.close() }

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
        keyboard?.hide()
        // On success the drawer stays put and is tidied away when the app covers us (ON_STOP).
        runCatching { vm.appRepository.launch(app, bounds, options) }
            .onFailure {
                Toast.makeText(context, "Couldn't open ${app.label}", Toast.LENGTH_SHORT).show()
                drawer.close()
            }
    }

    val actions = object : HomeActions {
        override fun openDrawer(withSearch: Boolean) {
            wantsSearch = withSearch
            drawer.open()
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
        override fun allowInstalls() = context.safeStart(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")),
        )

        override fun dismissWhatsNew() = vm.updater.dismissWhatsNew()
    }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { drawer.heightPx = it.height.toFloat() },
    ) {
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
                update = update,
                whatsNew = whatsNew,
            ),
            actions = actions,
            drawer = drawer,
            // Everything below reads progress inside graphicsLayer, so the whole open/close
            // runs in the draw phase without a single recomposition.
            modifier = Modifier.graphicsLayer {
                val p = drawer.progress.coerceIn(0f, 1f)
                val e = homeEntrance.value
                val scale = (1f - 0.07f * p) * (0.94f + 0.06f * e)
                scaleX = scale
                scaleY = scale
                alpha = (1f - 0.75f * p) * (0.4f + 0.6f * e)
                translationY = -24.dp.toPx() * p
                val blur = MotionLook.homeBlurDp.dp.toPx() * p
                renderEffect = if (blur > 0.5f) BlurEffect(blur, blur, TileMode.Decal) else null
            },
        )

        AppDrawer(
            apps = apps,
            query = query,
            onQueryChange = { query = it },
            focusSearch = focusSearch,
            onLaunch = ::launchApp,
            onAppInfo = { runCatching { vm.appRepository.openAppInfo(it) } },
            onUninstall = { runCatching { vm.appRepository.uninstall(it) } },
            onWebSearch = { q ->
                keyboard?.hide()
                context.safeStart(Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, q))
            },
            drawer = drawer,
            gridState = gridState,
            modifier = Modifier.graphicsLayer {
                val p = drawer.progress
                if (p <= 0.001f) {
                    // Parked fully below the screen: invisible and untouchable, but already
                    // composed so the first frame of a swipe has nothing to build.
                    translationY = size.height
                    alpha = 0f
                } else {
                    translationY = (1f - p.coerceAtMost(1f)) * drawer.travelPx
                    alpha = (p * 1.8f).coerceIn(0f, 1f)
                }
            },
        )
    }

    if (settingsOpen) {
        SettingsSheet(
            name = name,
            useSystemWallpaper = useSystemWallpaper,
            isDefaultLauncher = isDefault,
            update = update,
            updatesEnabled = vm.updater.enabled,
            onCheckForUpdates = vm::checkForUpdatesNow,
            onEditName = { nameDialogOpen = true },
            onUseSystemWallpaperChange = vm.prefs::setUseSystemWallpaper,
            onPickWallpaper = {
                context.safeStart(Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Choose wallpaper"))
            },
            onSetDefault = { requestDefaultLauncher() },
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
