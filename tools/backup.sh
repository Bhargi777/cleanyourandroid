#!/usr/bin/env bash
# Save everything needed to put the device back: package lists, disabled packages, getprop, all
# settings tables and the current Home owner. Run this before anything else.
#
# Usage: tools/backup.sh            -> backup/<serial>-<timestamp>/
set -euo pipefail
# shellcheck source=tools/lib.sh
. "$(dirname "$0")/lib.sh"
require_device

out="$ROOT/backup/$(serial)-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$out"
ash pm list packages -f > "$out/packages-all.txt"
ash pm list packages -s > "$out/packages-system.txt"
ash pm list packages -3 > "$out/packages-third-party.txt"
ash pm list packages -d > "$out/packages-disabled.txt"
ash getprop > "$out/getprop.txt"
for table in system secure global; do
  ash settings list "$table" > "$out/settings-$table.txt"
done
home_component > "$out/home.txt"
ash cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME \
  > "$out/home-claimants.txt"
recents_package > "$out/recents.txt"

model="$(grep -m1 'ro.product.model]' "$out/getprop.txt" | sed 's/.*: \[//; s/\]//')"
release="$(grep -m1 'ro.build.version.release]' "$out/getprop.txt" | sed 's/.*: \[//; s/\]//')"
sdk="$(grep -m1 'ro.build.version.sdk]' "$out/getprop.txt" | sed 's/.*: \[//; s/\]//')"
echo "Backed up $model (Android $release, SDK $sdk): $(wc -l < "$out/packages-all.txt" | tr -d ' ') packages"
echo "Home: $(cat "$out/home.txt")"
echo "Recents provided by: $(cat "$out/recents.txt")"
echo "$out"
