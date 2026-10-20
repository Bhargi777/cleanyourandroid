#!/usr/bin/env bash
# Build Slate Home and install it on the connected device as the default home screen.
#
# Needs JDK 17 (JAVA_HOME) and an Android SDK with platform 35 (ANDROID_HOME or ANDROID_SDK_ROOT).
# Usage: launcher/build-and-install.sh
set -euo pipefail
cd "$(dirname "$0")"

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
[ -d "$SDK" ] || { echo "Android SDK not found: set ANDROID_HOME" >&2; exit 1; }
echo "sdk.dir=$SDK" > local.properties
java -version 2>&1 | grep -q 'version "17' \
  || echo "warning: JDK 17 recommended; set JAVA_HOME if the build fails" >&2

./gradlew --no-daemon -q assembleDebug
APK=app/build/outputs/apk/debug/app-debug.apk

adb wait-for-device
# On Android 14+ `adb install` can wait for an on-screen confirmation forever; fall back to pm install.
TIMEOUT="$(command -v timeout || command -v gtimeout || true)"
if ! ${TIMEOUT:+$TIMEOUT 120} adb install -r "$APK"; then
  adb push "$APK" /data/local/tmp/slate.apk
  adb shell pm install -r /data/local/tmp/slate.apk
  adb shell rm /data/local/tmp/slate.apk
fi

# Lets the Wi-Fi pill show the network name (Android only reveals it while Location is on).
adb shell pm grant dev.slate.launcher android.permission.ACCESS_FINE_LOCATION 2>/dev/null || true
adb shell cmd package set-home-activity dev.slate.launcher/.MainActivity
adb shell input keyevent KEYCODE_HOME
echo "Slate Home installed and set as the default home screen."
