"""Checks the shell tools without a device: list parsing, refusal of unsafe lists, script hygiene.

Run from the repository root: python3 -m unittest discover -s tests -v
"""
import os
import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"


def read_list(content: bytes):
    with tempfile.NamedTemporaryFile(delete=False, suffix=".txt") as f:
        f.write(content)
        path = f.name
    try:
        return subprocess.run(["bash", "-c", '. tools/lib.sh; read_list "$1"', "_", path],
                              cwd=ROOT, capture_output=True, text=True)
    finally:
        os.unlink(path)


class ReadListTest(unittest.TestCase):
    def test_comments_blank_lines_and_inline_notes(self):
        r = read_list(b"# Device\n\ncom.a.b   # note\n  com.c.d\n")
        self.assertEqual(r.returncode, 0, r.stderr)
        self.assertEqual(r.stdout.split(), ["com.a.b", "com.c.d"])

    def test_windows_bom_and_crlf(self):
        r = read_list(b"\xef\xbb\xbf# Device\r\ncom.a.b\r\n")
        self.assertEqual(r.returncode, 0, r.stderr)
        self.assertEqual(r.stdout.split(), ["com.a.b"])

    def test_refuses_anything_but_package_names(self):
        for bad in (b"com.a.b\nrm -rf /\n", b"com.a.b;reboot\n", b"$(reboot)\n", b"noDots\n"):
            with self.subTest(bad=bad):
                r = read_list(bad)
                self.assertNotEqual(r.returncode, 0, "unsafe list was accepted")
                self.assertEqual(r.stdout, "", "a refused list must print no packages")


class ScriptHygieneTest(unittest.TestCase):
    def scripts(self):
        return sorted(TOOLS.glob("*.sh")) + [ROOT / "launcher" / "build-and-install.sh"]

    def test_executable_and_strict(self):
        for path in self.scripts():
            if path.name == "lib.sh":
                continue
            with self.subTest(script=path.name):
                self.assertTrue(os.access(path, os.X_OK), "not executable")
                self.assertIn("set -euo pipefail", path.read_text(encoding="utf-8"))

    def test_bash_syntax(self):
        for path in self.scripts():
            with self.subTest(script=path.name):
                r = subprocess.run(["bash", "-n", str(path)], capture_output=True, text=True)
                self.assertEqual(r.returncode, 0, r.stderr)

    def test_disable_uses_disable_user_never_uninstall(self):
        text = (TOOLS / "disable.sh").read_text(encoding="utf-8")
        self.assertIn("pm disable-user --user 0", text)
        self.assertNotIn("pm uninstall", text)


if __name__ == "__main__":
    unittest.main()
