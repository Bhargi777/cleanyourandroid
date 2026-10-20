# Security

## What this project does to your device

- It uses ADB, which gives a shell on your phone or tablet. Only authorise computers you own, and
  run `tools/close-adb.sh` when finished: revoke USB debugging authorisations and switch debugging off.
- Wireless debugging listens on your network while it is on. Switch it off after use.
- Packages are only disabled, never uninstalled, and `tools/restore.sh` re-enables them.
- Debloat lists come from contributors. `tools/disable.sh` refuses any list containing anything but
  bare package names, so a list cannot inject shell commands.

## Things worth telling the user about

If you notice any of these during a session, say so plainly:

- the bootloader is unlocked (`Bootloader is already unlocked` in Developer options, or
  `ro.boot.flash.locked=0`)
- `ro.adb.secure=0` (ADB without the authorisation prompt)
- device admin or accessibility apps the owner does not recognise

## Reporting a vulnerability

Please open a private security advisory on GitHub (Security → Report a vulnerability) rather than a
public issue.
