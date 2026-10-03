#!/usr/bin/env bash
# Drives HydaUI on an emulator and captures the preview: screenshots of each surface plus a
# screen recording of the gestures and animations.
#   scripts/record-preview.sh <debug.apk> <out-dir>
set -uo pipefail

APK="$1"
OUT="$2"
PKG=com.hydaui.launcher
mkdir -p "$OUT"

log() { echo "[preview] $*"; }
alive() { [ "$(adb get-state 2>/dev/null)" = "device" ]; }
die() { log "$*"; exit 1; }
shot() {
  alive || die "emulator went away before screenshot $1"
  adb exec-out screencap -p > "$OUT/$1.png.tmp"
  if [ -s "$OUT/$1.png.tmp" ]; then mv "$OUT/$1.png.tmp" "$OUT/$1.png"; log "screenshot $1"; else rm -f "$OUT/$1.png.tmp"; log "screenshot $1 came back empty"; fi
}

# Centre of the first on-screen element whose text or description contains $1, as "x y".
find_text() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb shell cat /sdcard/ui.xml | tr '>' '\n' | grep -F "$1" | head -1 \
    | sed -n 's/.*bounds="\[\([0-9]*\),\([0-9]*\)\]\[\([0-9]*\),\([0-9]*\)\]".*/\1 \2 \3 \4/p' \
    | awk '{ printf "%d %d %d %d\n", ($1+$3)/2, ($2+$4)/2, $2, $4 }'
}

adb wait-for-device
adb shell settings put system screen_off_timeout 1800000
adb shell svc power stayon true
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard

log "installing"
adb install -r -g "$APK"
adb shell cmd package set-home-activity "$PKG/.MainActivity"
adb shell am start -n "$PKG/.MainActivity" --es demo_name "Rebecca" \
  -a android.intent.action.MAIN -c android.intent.category.HOME >/dev/null
sleep 10 # let first-run work (app list, aurora) settle
alive || die "emulator went away while HydaUI was starting"
adb logcat -d -b crash | tail -40 || true

read -r W H < <(adb shell wm size | grep -oE '[0-9]+x[0-9]+' | tail -1 | tr 'x' ' ')
CX=$((W / 2))
log "screen ${W}x${H}"

adb logcat -c || true
shot 01-home

log "recording"
adb shell screenrecord --bit-rate 8000000 --time-limit 120 /sdcard/preview.mp4 &
REC=$!
sleep 2

# 1. Slow, deliberate swipe up: the drawer should ride the finger.
adb shell input swipe $CX $((H * 82 / 100)) $CX $((H * 35 / 100)) 1100
sleep 2
shot 02-drawer

# 2. Drag it back down by the handle area.
adb shell input swipe $CX $((H * 9 / 100)) $CX $((H * 80 / 100)) 900
sleep 2

# 3. A quick flick up, then back to close.
adb shell input swipe $CX $((H * 80 / 100)) $CX $((H * 62 / 100)) 120
sleep 2
adb shell input keyevent KEYCODE_BACK
sleep 2

# 4. The assistant pill: drawer opens, then the keyboard, then we search.
read -r PX PY PTOP _ < <(find_text "How can I help" || true)
if [ -n "${PX:-}" ]; then
  adb shell input tap "$PX" "$PY"
  sleep 2.5
  adb shell input text "set"
  sleep 1.5
  shot 03-search
  adb shell input keyevent KEYCODE_BACK # keyboard
  sleep 0.6
  adb shell input keyevent KEYCODE_BACK # drawer
  sleep 2
else
  log "assistant pill not found; skipping search"
fi

# 5. Press feedback on the dock, without launching anything.
read -r DX DY _ < <(find_text "All apps" || true)
if [ -n "${DX:-}" ]; then
  adb shell input swipe "$DX" "$DY" "$DX" "$DY" 450
  sleep 2
  adb shell input keyevent KEYCODE_BACK
  sleep 1.5
fi

# 6. Long-press empty glass above the pill for settings.
if [ -n "${PTOP:-}" ]; then
  LY=$((PTOP - 60))
  adb shell input swipe $((W / 6)) $LY $((W / 6)) $LY 1200
  sleep 2.5
  shot 04-settings
  adb shell input keyevent KEYCODE_BACK
  sleep 2
fi

shot 05-home-again

adb logcat -d -s HydaDrawer:D > "$OUT/drawer-log.txt" 2>/dev/null || true
log "drawer decisions:"; cat "$OUT/drawer-log.txt" || true
log "stopping recording"
adb shell pkill -INT screenrecord || true
sleep 4
wait $REC 2>/dev/null || true
adb pull /sdcard/preview.mp4 "$OUT/preview-raw.mp4" >/dev/null && log "pulled recording"
ls -la "$OUT"
[ -s "$OUT/01-home.png" ] || die "no usable screenshots"
