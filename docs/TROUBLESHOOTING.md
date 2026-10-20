# Troubleshooting

Things that went wrong on real devices, and what fixed them.

## The device shows as `unauthorized`

Unlock the screen and look for **Allow USB debugging?**. Tick **Always allow from this computer**
and tap **Allow**. Without the tick, the device asks again after every reconnect.

If the prompt never appears: unplug, replug, and if needed Developer options →
**Revoke USB debugging authorisations**, then replug.

If `adb devices -l` shows the `transport_id` climbing (1, 31, 33, ...) the USB link is resetting:
try another cable or port and keep the cable still. A data-capable cable matters; many charging
cables carry no data.

## Screenshots come back blank (white or black, a few KB)

The screen went to sleep. `tools/screenshot.sh` wakes it and warns about tiny captures. For long
sessions you can keep it awake while charging:

```sh
adb shell settings put global stay_on_while_plugged_in 7   # 0 to switch back off
```

Some apps (banking, DRM video) set FLAG_SECURE and always capture black. That is the app, not the
debloat; check `dumpsys activity activities | grep mResumedActivity` instead.

## Recents button does nothing after disabling the stock launcher

On Android 10 and newer, the Recents (overview) screen usually comes from the stock launcher's
Quickstep:

```sh
adb shell dumpsys activity recents | grep mRecentsComponent
# mRecentsComponent=ComponentInfo{com.tblenovo.launcher/com.android.quickstep.RecentsActivity}
```

Re-enable it (`tools/restore.sh com.tblenovo.launcher`). It is harmless enabled: a custom launcher
owns Home, and the stock one only starts when you open Recents. `tools/disable.sh` skips the Recents
provider unless `FORCE_RECENTS=1`.

## Gesture navigation switched to three buttons

Android 10–12 only allow gesture navigation with the default (stock) launcher. When another launcher
owns Home, the system switches to 3-button navigation and shows "System navigation updated". That is
expected. Set Home back to the stock launcher to get gestures again.

## Websites stop loading after turning on Private DNS

The network blocks DNS-over-TLS (port 853), so every lookup fails. Common on campus, hotel and office
Wi-Fi. Switch it off:

```sh
tools/private-dns.sh off
# or: Settings → Network & internet → Private DNS → Automatic
```

`tools/private-dns.sh on` tests resolution and does this by itself. Android has one Private DNS
setting for every network, so if it breaks a network you use daily, leave it off.

`ping` failing to 8.8.8.8 is not proof of anything: many networks block ICMP. Test a hostname
(`ping -c1 www.google.com` resolving to an address) instead.

## The Wi-Fi pill says "Wi-Fi" without the network name

Android 10+ only reveals the SSID to apps with location permission **while Location is switched on**.
`launcher/build-and-install.sh` grants the permission; turning Location on is your choice.

## `adb install` hangs (Android 14+)

It is waiting for a confirmation on the device. Push and install from the shell:

```sh
adb push app-debug.apk /data/local/tmp/slate.apk
adb shell pm install -r /data/local/tmp/slate.apk
```

## The app grid is empty (Android 11+)

Package visibility: a launcher must declare `<queries>` for `android.intent.action.MAIN` +
`android.intent.category.LAUNCHER`, or Android hides every other app from it. Slate Home declares it;
keep it if you fork.

## Homebrew asks for a password while installing the JDK

The cask installer needs sudo. Use a user-local JDK instead:

```sh
curl -L -o jdk17.tar.gz "https://api.adoptium.net/v3/binary/latest/17/ga/mac/aarch64/jdk/hotspot/normal/eclipse"
mkdir -p ~/.jdk17 && tar -xzf jdk17.tar.gz -C ~/.jdk17 --strip-components=1
export JAVA_HOME=~/.jdk17/Contents/Home
```

On Apple Silicon, an x86_64 JDK fails with `Bad CPU type in executable` unless Rosetta is installed;
use the `aarch64` build.

## Re-enable a single app without a computer

Settings → Apps → See all apps → ⋮ → **Show system** → the app (disabled apps are listed at the
bottom) → **Enable**.
