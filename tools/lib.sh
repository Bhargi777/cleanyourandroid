#!/usr/bin/env bash
# Shared helpers for the tools/ scripts. Source it, do not run it.
#
# Device selection: set ANDROID_SERIAL when more than one device is attached (adb reads it itself).
# For Wireless debugging set ADB_HOST=ip:port and the scripts run `adb connect` first.

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PACKAGE_RE='^[A-Za-z_][A-Za-z0-9_]*(\.[A-Za-z0-9_]+)+$'

die() { echo "error: $*" >&2; exit 1; }
note() { echo "--> $*" >&2; }

# Run one command on the device and strip the CR that some adb builds add.
ash() { adb shell "$@" | tr -d '\r'; }

require_device() {
  command -v adb >/dev/null || die "adb not found (macOS: brew install android-platform-tools)"
  if [ -n "${ADB_HOST:-}" ]; then
    adb connect "$ADB_HOST" >/dev/null || die "adb connect $ADB_HOST failed"
    export ANDROID_SERIAL="$ADB_HOST"
  fi
  local state
  state="$(adb get-state 2>&1 || true)"
  case "$state" in
    device) ;;
    *unauthorized*) die "device is unauthorized: unlock it and tap Allow on the 'Allow USB debugging?' prompt (tick 'Always allow')" ;;
    *more\ than\ one*) die "more than one device attached: set ANDROID_SERIAL (see adb devices)" ;;
    *) die "no device: plug in USB with debugging on, or set ADB_HOST for Wireless debugging ($state)" ;;
  esac
}

serial() { adb get-serialno | tr -d '\r'; }

# Folder for this device's recorded state (what was disabled before us, who owned Home, ...).
state_dir() {
  local d
  d="$ROOT/state/$(serial)"
  mkdir -p "$d"
  echo "$d"
}

# Print the package names in a debloat list: one per line, # comments allowed.
# Refuses the whole list if any entry is not a bare package name, because each entry ends up in a
# shell command on the device. Handles CRLF and a UTF-8 BOM from Windows editors.
read_list() {
  local list="$1" pkgs bad
  [ -f "$list" ] || die "no such list: $list"
  pkgs="$(LC_ALL=C awk 'NR==1{sub(/^\357\273\277/,"")} {sub(/\r$/,""); print}' "$list" \
    | sed -E 's/#.*$//' | awk 'NF{print $1}')"
  bad="$(printf '%s\n' "$pkgs" | grep -vE "$PACKAGE_RE" | grep -v '^$' || true)"
  [ -z "$bad" ] || die "refusing $list, not a package name: $(echo "$bad" | tr '\n' ' ')"
  printf '%s\n' "$pkgs" | grep -v '^$' || true
}

# Component that answers the HOME intent right now (e.g. com.example.launcher/.Main).
home_component() {
  ash cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME \
    | tail -n 1
}

# Package that provides the Recents (overview) screen. On Android 10+ this is usually the stock
# launcher's Quickstep, so disabling that launcher breaks the Recents button and app switching.
recents_package() {
  ash dumpsys activity recents | grep -m1 -o 'mRecentsComponent=ComponentInfo{[^/]*' \
    | sed 's/.*{//' || true
}

# Packages the phone needs to boot, unlock, type, install and get updates. disable.sh never touches
# these, whatever the list says.
protected_packages() {
  cat <<'PROTECTED'
android
com.android.systemui
com.android.settings
com.android.phone
com.android.providers.settings
com.android.providers.telephony
com.android.providers.media
com.android.providers.downloads
com.android.externalstorage
com.android.documentsui
com.android.packageinstaller
com.google.android.packageinstaller
com.android.permissioncontroller
com.google.android.permissioncontroller
com.android.bluetooth
com.android.nfc
com.android.networkstack
com.google.android.networkstack
com.android.shell
com.android.keychain
com.android.certinstaller
com.google.android.gms
com.google.android.gsf
com.android.vending
com.google.android.webview
com.android.webview
com.google.android.ext.services
com.android.ext.services
com.google.android.inputmethod.latin
com.samsung.android.honeyboard
PROTECTED
}

# Default keyboard; disabling it leaves no way to type the PIN after a reboot.
current_ime_package() {
  ash settings get secure default_input_method | cut -d/ -f1
}
