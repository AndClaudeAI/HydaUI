# HydaUI

A frosted-glass home screen for Android. Pale pearl light, milky cards, one blue for action
and one ember for life. Your day reads at a glance, and every app is one swipe away.

## What's on the glass

| | |
|---|---|
| **Analog clock** | Milk-glass dial with an ember second hand and today's date. Tap for alarms. |
| **Weather** | Smoked-glass tile with a hand-drawn sky (sun, moon, cloud, rain, snow, storm) and "feels like". Uses [Open-Meteo](https://open-meteo.com): no API key, coarse location only. |
| **Greeting** | "Hello, *you*. Your summary for today." Tap to set your name. |
| **Today** | Your next calendar event on white glass, with where/notes beneath and what follows as chips. |
| **Battery & alarm** | An ember ring for charge, plus your next alarm with the week laid out underneath. |
| **"How can I help?"** | Opens the app drawer with the keyboard ready. Search apps, or press Go to search the web. |
| **Dock** | Phone, Messages, Camera, All apps, as frosted circles. |

### Gestures

- **Swipe up** for the app drawer. Pull down past the top of the list to close it.
- **Swipe down** for the notification shade.
- **Long-press** an empty area for settings: your name, aurora or system wallpaper, default home.
- **Long-press an app** for *App info* and *Uninstall*.
- **Home** always brings you back and closes whatever's open.

Work-profile apps show up too, with their badge.

## Install

1. Grab the APK from the latest **Build APK** run in the
   [Actions tab](../../actions/workflows/build.yml) (artifact `HydaUI-apk`), or build it yourself:
   ```sh
   ./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
   ```
2. Install it (`adb install app-debug.apk`, or open the file on the phone and allow the install).
3. Open **HydaUI** and tap **Set** on the "Make HydaUI your home screen" banner. Or go to
   *Settings → Apps → Default apps → Home app* and pick HydaUI.

Calendar and location permissions are both optional. Each is asked for only when you tap the card that needs it.

## Build

- Android Studio Ladybug or newer, or JDK 17 plus the Android SDK (platform 35).
- Kotlin 2.1, Jetpack Compose (BOM 2024.12), Material 3. Minimum Android 8.0 (API 26).

```
app/src/main/java/com/hydaui/launcher/
├── MainActivity.kt          # the HOME activity
├── LauncherViewModel.kt
├── data/                    # apps (LauncherApps), calendar, weather, battery, prefs
└── ui/
    ├── LauncherRoot.kt      # wiring: drawer, sheets, permissions, intents
    ├── components/          # glass() surface, aurora background
    ├── home/                # clock, weather, cards, dock
    ├── drawer/              # searchable app grid
    └── settings/
```

The release build is signed with the debug key so it installs straight from CI.
Add your own signing config before you publish it anywhere.
