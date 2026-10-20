# Contributing

The most useful contribution is a **tested list for a device that is not in `examples/` yet**.

## Adding a device list

1. Run the prompt on your device, or `tools/backup.sh` and `tools/disable.sh` by hand.
2. Create `examples/<brand>-<model>/debloat.txt` (lowercase, dashes). Start with a header:
   - first line: device name, model number, chipset, Android version, SDK, RAM
   - build number and the date you tested
   - how you tested (batches, what you opened between them, reboot or not)
   - stock launcher, HOME claimants and the Recents provider (`tools/backup.sh` prints them)
   - what you kept on purpose and why
3. One package per line with a short `# why` comment, grouped under `# Telemetry`,
   `# Ads, feeds and auto-installers`, `# Demo`, `# Off by choice` and similar headings.
4. Add a row to the table in `examples/README.md`.
5. Run the checks: `python3 -m unittest discover -s tests -v`.

Only list packages you actually disabled and lived with. Mark anything optional as `off by choice`.

**Never** include files from `backup/` or `state/`, screenshots showing your accounts, notifications,
contacts, network names or apps that identify you.

## Code

- Shell: `set -euo pipefail`, pass ShellCheck (`shellcheck -x -s bash tools/*.sh`), one `pm` call per
  `adb shell`, and validate anything that ends up in a device command.
- Launcher: keep it light (no blur, no video, no polling, no network). Put logic that can be tested
  without Android in plain classes with JUnit tests (`./gradlew testDebugUnitTest`).

## Commit messages

[Conventional Commits](https://www.conventionalcommits.org/): `feat:`, `fix:`, `docs:`, `test:`,
`refactor:`, `ci:`, `chore:`, with an optional scope, e.g. `feat(examples): add Pixel 7a list`.
