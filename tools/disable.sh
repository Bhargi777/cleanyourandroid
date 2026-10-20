#!/usr/bin/env bash
# Disable every package in a debloat list for user 0. Nothing is uninstalled; tools/restore.sh undoes it.
#
# Usage: tools/disable.sh examples/<device>/debloat.txt
#   BATCH=5        packages per batch; a screenshot is saved and (on a terminal) you are asked to
#                  check the device before the next batch, so a batch that breaks something is small
#   DRY_RUN=1      print what would happen, change nothing
#   NO_PAUSE=1     do not wait between batches
#   FORCE_HOME=1   allow disabling the app that owns Home (only when another launcher is installed)
#   FORCE_RECENTS=1 allow disabling the app that provides Recents
set -euo pipefail
# shellcheck source=tools/lib.sh
. "$(dirname "$0")/lib.sh"
LIST="${1:?usage: tools/disable.sh <debloat list>}"
BATCH="${BATCH:-5}"
require_device

pkgs="$(read_list "$LIST")"
[ -n "$pkgs" ] || die "$LIST lists no packages"
sd="$(state_dir)"

# Record the device as it was before the first run, so restore.sh can leave alone what was already
# disabled and put Home back where it was. Later runs keep the first record.
if [ ! -f "$sd/pre-disabled.txt" ]; then
  ash pm list packages -d | sed 's/^package://' | sort > "$sd/pre-disabled.txt"
  home_component > "$sd/home.txt"
  note "recorded original state in $sd"
fi
touch "$sd/disabled.txt"

installed="$(ash pm list packages | sed 's/^package://' | sort)"
disabled_now="$(ash pm list packages -d | sed 's/^package://' | sort)"
home_pkg="$(home_component | cut -d/ -f1)"
recents_pkg="$(recents_package)"
ime_pkg="$(current_ime_package)"
protected="$(protected_packages)"

todo=()
while read -r p; do
  if grep -qxF "$p" <<< "$protected"; then
    note "skip $p: protected (the device needs it to boot, unlock, type or install)"; continue
  fi
  if [ "$p" = "$ime_pkg" ]; then
    note "skip $p: it is the current keyboard; switch keyboards first"; continue
  fi
  if [ "$p" = "$home_pkg" ] && [ -z "${FORCE_HOME:-}" ]; then
    note "skip $p: it owns Home; install another launcher first (FORCE_HOME=1 to override)"; continue
  fi
  if [ -n "$recents_pkg" ] && [ "$p" = "$recents_pkg" ] && [ -z "${FORCE_RECENTS:-}" ]; then
    note "skip $p: it provides the Recents screen; disabling it breaks app switching (FORCE_RECENTS=1)"; continue
  fi
  if ! grep -qxF "$p" <<< "$installed"; then
    note "skip $p: not installed on this device"; continue
  fi
  if grep -qxF "$p" <<< "$disabled_now"; then
    note "skip $p: already disabled"; continue
  fi
  todo+=("$p")
done <<< "$pkgs"

echo "${#todo[@]} package(s) to disable on $(serial)"
[ "${#todo[@]}" -gt 0 ] || exit 0

n=0
batch=1
for p in "${todo[@]}"; do
  if [ -n "${DRY_RUN:-}" ]; then
    echo "would disable $p"; continue
  fi
  # One pm call per adb shell: chained commands can stop silently after the first.
  out="$(ash pm disable-user --user 0 "$p" 2>&1 || true)"
  case "$out" in
    *"new state: disabled-user"*)
      echo "disabled $p"
      grep -qxF "$p" "$sd/disabled.txt" || echo "$p" >> "$sd/disabled.txt" ;;
    *) echo "FAILED  $p: $out" >&2 ;;
  esac
  n=$((n + 1))
  if [ $((n % BATCH)) -eq 0 ] && [ "$n" -lt "${#todo[@]}" ]; then
    "$ROOT/tools/screenshot.sh" "batch-$batch" >/dev/null || true
    echo "batch $batch done, screenshot in $sd/shots/batch-$batch.png"
    batch=$((batch + 1))
    if [ -t 0 ] && [ -z "${NO_PAUSE:-}" ]; then
      read -r -p "Check the device still works, then press Enter for the next batch (Ctrl-C to stop) "
    fi
  fi
done
[ -n "${DRY_RUN:-}" ] || "$ROOT/tools/screenshot.sh" "batch-$batch" >/dev/null || true
echo "Done. Undo with: tools/restore.sh"
