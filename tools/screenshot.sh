#!/usr/bin/env bash
# Save a screenshot from the device to state/<serial>/shots/<name>.png and print its path.
# Wakes the screen first; a sleeping screen gives a blank image that looks like a broken device.
#
# Usage: tools/screenshot.sh <name>
set -euo pipefail
# shellcheck source=tools/lib.sh
. "$(dirname "$0")/lib.sh"
name="${1:-shot-$(date +%H%M%S)}"
[[ $name =~ ^[A-Za-z0-9._-]+$ ]] || die "name may only use letters, digits, . _ -"
require_device

power="$(ash dumpsys power)"
if grep -q 'mWakefulness=Asleep\|mWakefulness=Dozing' <<< "$power"; then
  adb shell input keyevent KEYCODE_WAKEUP
  sleep 1
fi
window="$(ash dumpsys window)"
if grep -q 'isStatusBarKeyguard=true\|mShowingLockscreen=true' <<< "$window"; then
  note "the lock screen is showing; unlock the device or the screenshot shows only the lock screen"
fi

dir="$(state_dir)/shots"
mkdir -p "$dir"
adb exec-out screencap -p > "$dir/$name.png"
size=$(wc -c < "$dir/$name.png" | tr -d ' ')
if [ "$size" -lt 20000 ]; then
  note "screenshot is only $size bytes: the screen is probably off, or the app blocks capture (FLAG_SECURE)"
fi
echo "$dir/$name.png"
