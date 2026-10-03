package com.hydaui.launcher

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hydaui.launcher.data.Prefs
import com.hydaui.launcher.data.Updater
import com.hydaui.launcher.ui.LauncherRoot
import com.hydaui.launcher.ui.theme.HydaTheme
import kotlinx.coroutines.flow.MutableSharedFlow

class MainActivity : ComponentActivity() {
    /** Fires when Home is pressed while we're already the visible home screen. */
    private val homePresses = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Paint the newest look from the very first frame.
        Updater(this).loadLook()
        // Lets the CI preview recorder fill in a name for screenshots.
        if (BuildConfig.DEBUG) intent?.getStringExtra("demo_name")?.let { Prefs(this).setName(it) }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            HydaTheme {
                LauncherRoot(homePresses)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            homePresses.tryEmit(Unit)
        }
    }
}
