# Examples

One folder per device model, each with a `debloat.txt` listing the packages that are safe to disable
on that firmware, grouped and commented. These are starting points: comment out anything you use.

The first comment line names the device, Android version and build. The header says how the list was
tested and what was kept on purpose. Lines are `package.name  # why`.

| Folder | Device | OS | Notes |
|---|---|---|---|
| `lenovo-tab-m10-fhd-plus-tb-x606v/` | Lenovo Tab M10 FHD Plus LTE (TB-X606V, MediaTek) | Android 10 | Lenovo telemetry and push, content feed, OCZ auto-installers, retail demo; Kids Space, preloads and unused Google apps off by choice; stock launcher kept because it provides Recents; gesture nav needs the stock launcher |

## Adding a device

1. Run `tools/backup.sh`, then let your assistant work through [`PROMPT.md`](../PROMPT.md).
2. Copy the packages that ended up disabled into `examples/<brand>-<model>/debloat.txt`, with a short
   comment on each saying what it is.
3. Write the header: device, codename, Android version, build, how you tested, what you kept and why.
4. Add a row to the table above and run `python3 -m unittest discover -s tests`.

Never commit anything from `backup/` or `state/`: they hold your installed apps and settings.
