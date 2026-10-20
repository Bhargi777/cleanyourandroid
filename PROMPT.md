# The prompt

Paste everything below the line into a coding assistant that can run shell commands (Claude Code,
Codex, Gemini CLI, ...) with this repository open. Fill in your device model first.

Before you start, on the phone or tablet: **Settings → About phone/tablet → tap Build number 7 times**,
then **Settings → System → Developer options → USB debugging** on. Plug it into the computer with a
cable that carries data and keep the screen unlocked.

---

I want to debloat my Android phone/tablet and replace its launcher with a clean iPadOS-style home
screen. My device is: **<MODEL NAME HERE>**. Work on the real device, step by step, and verify
everything you do with screenshots pulled from it (`tools/screenshot.sh`). Ask me before anything that
depends on my preferences. Use the scripts in `tools/` where they fit; read them first.

## Connect

- Run `adb devices -l`. If the device shows as `unauthorized`, tell me to unlock it and tap **Allow** on
  the "Allow USB debugging?" prompt with **Always allow from this computer** ticked. If it keeps
  dropping back to `unauthorized` or the transport id keeps climbing, the cable or port is flaky: say so.
- No cable? On Android 11+ use Wireless debugging: ask me for the pairing code and port under
  Developer options → Wireless debugging → Pair device with pairing code, run `adb pair`, then
  `adb connect` and set `ADB_HOST` for the tools. Android switches it off whenever Wi-Fi drops.
- Read the model, Android version, SDK and skin (One UI, HyperOS/MIUI, ColorOS, ZUI, ...) from `getprop`.
- If `adb install` hangs on Android 14+, it is waiting for an on-screen confirmation: push the APK to
  `/data/local/tmp` and use `pm install -r` instead.
- Long sessions: the screen sleeps and screenshots come back blank. `tools/screenshot.sh` wakes it.
  Never try to get past the lock screen; ask me to unlock.

## Back up first

- Run `tools/backup.sh` before changing anything. It saves package lists, disabled packages, getprop,
  all settings tables, the Home owner and the Recents provider.
- Check `examples/` for my model. If a list exists, start from it, but still confirm each group with me.
- Ask me what else is plugged in (OTG drive, keyboard, hub). Some devices cut USB power to accessories
  while debugging is on.

## Debloat

- List installed packages (`pm list packages -f`) and sort them into: manufacturer telemetry and
  analytics, manufacturer ads/content feeds and app auto-installers, retail demo, duplicate
  manufacturer apps (browser, store, gallery, music, cleaner), preloaded third-party apps (games,
  social, trials), carrier apps, Google apps, and system packages. Show me the table.
- Disable with `pm disable-user --user 0` through `tools/disable.sh`. Never uninstall system packages.
- Never disable what the device needs: SystemUI, Settings, the phone/telephony stack (on anything with
  a SIM), Bluetooth, NFC, the current keyboard, the package installer and permission controller, Play
  Store, Play Services, WebView, face/fingerprint unlock, and the camera. `tools/lib.sh` holds the
  protected list; `disable.sh` skips those whatever the list says.
- Manufacturer package names often do not say what they do: check activities and services with
  `dumpsys package <pkg>` first, and ask me if it is still unclear.
- Before touching preloaded apps, Google apps (Phone, Messages, Assistant, the Google app), the voice
  assistant, Nearby/Quick Share, the manufacturer account and the system update service, ask me with
  a checklist which ones I use. Disable only what I don't tick. Recommend keeping system updates.
- Warn me that disabling the Phone app removes the emergency-call dialer on a device with a SIM.
- Run `pm` commands one per shell call; chained commands may silently stop after the first.
- Disable in batches of about five and check between them (screenshot, open Settings, open one app):
  a batch can take something load-bearing with it and you cannot tell which.
- Offer ad-blocking DNS as an optional extra: `tools/private-dns.sh on` (AdGuard by default). Tell me it
  does not block YouTube ads. It tests name resolution and switches itself back off when the network
  blocks DNS-over-TLS (port 853: common on campus, hotel and office Wi-Fi). Android uses one setting for
  every network, so if it fails on one network I use daily, leave it off.

## Launcher

- Build `launcher/` (Slate Home, Java, minSdk 29) and install it with `launcher/build-and-install.sh`.
  Needs JDK 17 and an Android SDK with platform 35; ask me before installing either. If Homebrew needs
  sudo, a user-local JDK tarball from Adoptium avoids it.
- Check the device accepts the APK before going further. If it refuses sideloads or refuses another
  app claiming HOME, finish the debloat, say plainly that the launcher half is not possible here, and
  stop retrying.
- Ask me which 4–6 apps go in the dock and in what order, and whether I want pitch black or a
  wallpaper. The default dock is in `launcher/app/src/main/res/values/config.xml`; a wallpaper goes in
  `Android/data/dev.slate.launcher/files/wallpaper.jpg` on the device.
- Set it as Home with `cmd package set-home-activity dev.slate.launcher/.MainActivity`.
- **Do not disable the stock launcher** without checking `dumpsys activity recents` for
  `mRecentsComponent`: on Android 10+ the stock launcher usually provides Recents (Quickstep), and
  disabling it breaks the Recents button and app switching. Also tell me that gesture navigation only
  works with the stock launcher on many Android 10–12 builds and the device may switch to 3 buttons.
- Check that the Wi-Fi pill shows the network name; Android only reveals it while Location is on. If
  Location is off, leave it off unless I ask.

## Verify

- After each change, take a screenshot and look at it. Test both orientations on a tablet. Tap a dock
  app, long-press an app (the menu must be readable), press Home while home.
- Banking and DRM apps may block screenshots (FLAG_SECURE): a black capture there is not a bug.

## Finish

- Give me: the list of what was disabled and why, `tools/restore.sh` to undo it, how to re-enable one
  app from Settings on the device without a computer, and how to change the dock and wallpaper.
- If my model has no list in `examples/`, write one in the same format so it can be contributed.
- Close up with `tools/close-adb.sh`: revoke USB debugging authorisations and switch debugging off.
- If you notice anything security-relevant (unlocked bootloader, `ro.adb.secure=0`, unknown device
  admin apps), tell me plainly.
