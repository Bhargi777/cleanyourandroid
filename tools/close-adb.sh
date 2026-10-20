#!/usr/bin/env bash
# Finish a session: stop the adb server on this computer and print what to switch off on the device.
# Wireless debugging and an authorised computer both leave a way in; switch them off when done.
set -euo pipefail
adb kill-server 2>/dev/null || true
cat <<'MSG'
adb server stopped. On the device:
  1. Settings > System > Developer options > Revoke USB debugging authorisations
  2. Switch off USB debugging (and Wireless debugging if you used it)
  3. Optional: switch Developer options off at the top of that screen
MSG
