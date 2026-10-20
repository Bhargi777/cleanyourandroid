#!/usr/bin/env bash
# Undo tools/disable.sh: re-enable what it disabled, leave alone what was already disabled before the
# first run, and give Home back to the app that owned it then.
#
# Usage: tools/restore.sh              re-enable everything recorded in state/<serial>/disabled.txt
#        tools/restore.sh <list>       re-enable only the packages in that list
#        tools/restore.sh <package>    re-enable one package
set -euo pipefail
# shellcheck source=tools/lib.sh
. "$(dirname "$0")/lib.sh"
require_device
sd="$(state_dir)"

arg="${1:-}"
if [ -z "$arg" ]; then
  [ -f "$sd/disabled.txt" ] || die "nothing recorded for $(serial); pass a list or a package name"
  pkgs="$(cat "$sd/disabled.txt")"
elif [ -f "$arg" ]; then
  pkgs="$(read_list "$arg")"
elif [[ $arg =~ $PACKAGE_RE ]]; then
  pkgs="$arg"
else
  die "not a list file or package name: $arg"
fi

pre=""
[ -f "$sd/pre-disabled.txt" ] && pre="$(cat "$sd/pre-disabled.txt")"
while read -r p; do
  [ -n "$p" ] || continue
  if grep -qxF "$p" <<< "$pre"; then
    echo "leave $p: it was disabled before tools/disable.sh ran"; continue
  fi
  out="$(ash pm enable --user 0 "$p" 2>&1 || true)"
  case "$out" in
    *"new state: enabled"*)
      echo "enabled $p"
      if [ -f "$sd/disabled.txt" ]; then
        grep -vxF "$p" "$sd/disabled.txt" > "$sd/disabled.tmp" || true
        mv "$sd/disabled.tmp" "$sd/disabled.txt"
      fi ;;
    *) echo "FAILED  $p: $out" >&2 ;;
  esac
done <<< "$pkgs"

if [ -z "$arg" ] && [ -s "$sd/home.txt" ]; then
  home="$(cat "$sd/home.txt")"
  ash pm enable --user 0 "${home%%/*}" >/dev/null 2>&1 || true
  ash cmd package set-home-activity "$home" >/dev/null && echo "Home back to $home"
fi
echo "Done. A reboot makes sure every re-enabled app starts cleanly: adb reboot"
