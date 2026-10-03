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

## How updates reach your phone

```
push ──▶ Build & preview ──▶ prerelease "build-n"      (screenshots, video, APK; phones ignore it)
                                    │
                         you approve│  Actions ▸ Approve build ▸ n
                                    ▼
                          latest release  ──▶  HydaUI checks every ~3 h (and on "Check now")
                                                ├─ look & feel changed → applied live, no install
                                                └─ code changed        → downloads, verifies, installs itself
```

- **Preview first.** Every push records the launcher on an emulator: screenshots of each
  surface and a video of the swipes and drawer. These are attached to a *prerelease* (see the
  [Releases](../../releases) page). Phones never see prereleases.
- **Approve to ship.** Run **Actions ▸ Approve build** with the build number. That build becomes
  the latest release and phones pick it up.
- **Look & feel without installing.** Colours, glass, motion tuning, wording and widget toggles
  live in [`config/hyda-config.json`](config/hyda-config.json). If an approved build changes only
  that file, phones repaint in place. Nothing is installed.
- **Code updates install themselves.** On Android 12+, HydaUI updates itself silently once
  you've allowed it to install apps. Older Android versions show one *Update* tap.

> The emulator video uses a software GPU, so it shows the *design* of the motion, not how
> smooth it will be. A real phone will be smoother.

## One-time setup

### 1. Signing key (GitHub secrets)

Android only accepts an update signed with the same key as the installed app, so the key must
stay the same forever and stay private. On any computer with Java:

```sh
keytool -genkeypair -keystore hyda-release.keystore -storetype PKCS12 \
  -alias hydaui -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=HydaUI"
base64 -w0 hyda-release.keystore > hyda-release.b64    # macOS: base64 -i hyda-release.keystore -o hyda-release.b64
```

In **Settings ▸ Secrets and variables ▸ Actions**, add:

| Secret | Value |
|---|---|
| `HYDA_KEYSTORE_BASE64` | contents of `hyda-release.b64` |
| `HYDA_KEYSTORE_PASSWORD` | the password you chose |

Keep `hyda-release.keystore` somewhere safe. If it's lost, phones can't update without an
uninstall. Until the secrets exist, previews still build, but they carry no installable update.

### 2. Your phone

1. Uninstall any earlier HydaUI. Older builds were signed with throwaway keys.
2. Approve a build (above), then download `HydaUI.apk` from that release and install it.
3. Set HydaUI as your home app (the banner on the home screen does this).
4. When the first update arrives, tap **Allow** on the banner. This lets HydaUI install its own
   updates from then on.

## Build

- Android Studio Ladybug or newer, or JDK 17 plus the Android SDK (platform 35).
- Kotlin 2.1, Jetpack Compose (BOM 2024.12), Material 3. Minimum Android 8.0 (API 26).

```
app/src/main/java/com/hydaui/launcher/
├── MainActivity.kt          # the HOME activity
├── LauncherViewModel.kt
├── data/                    # apps, calendar, weather, battery, prefs, self-updater
└── ui/
    ├── LauncherRoot.kt      # wiring: drawer, sheets, permissions, intents
    ├── components/          # glass() surface, aurora background, press springs
    ├── theme/               # palette + Look: applies config/hyda-config.json live
    ├── home/                # clock, weather, cards, dock
    ├── drawer/              # searchable app grid
    └── settings/
```

Local builds are signed with your machine's debug key and turn off self-updates, so a dev
build never replaces itself with a release.
