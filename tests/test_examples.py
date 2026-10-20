"""Checks every device list in examples/ and the table that indexes them.

Run from the repository root: python3 -m unittest discover -s tests -v
"""
import re
import subprocess
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
EXAMPLES = ROOT / "examples"
PACKAGE = re.compile(r"^[A-Za-z_][A-Za-z0-9_]*(\.[A-Za-z0-9_]+)+$")


def folders():
    return sorted(p for p in EXAMPLES.iterdir() if p.is_dir())


def packages(path):
    out = []
    for line in path.read_text(encoding="utf-8").split("\n"):
        body = line.split("#", 1)[0].strip()
        if body:
            out.append(body)
    return out


def protected():
    """The protected list from tools/lib.sh, read through bash so the test uses the real source."""
    result = subprocess.run(
        ["bash", "-c", ". tools/lib.sh; protected_packages"],
        cwd=ROOT, capture_output=True, text=True, check=True)
    return set(result.stdout.split())


class ExampleListsTest(unittest.TestCase):
    def test_every_folder_has_a_list(self):
        for folder in folders():
            with self.subTest(folder=folder.name):
                self.assertTrue((folder / "debloat.txt").is_file(), "no debloat.txt")

    def test_folder_names(self):
        for folder in folders():
            with self.subTest(folder=folder.name):
                self.assertRegex(folder.name, r"^[a-z0-9]+(-[a-z0-9]+)*$", "use lowercase-with-dashes")

    def test_list_format(self):
        for folder in folders():
            path = folder / "debloat.txt"
            with self.subTest(folder=folder.name):
                lines = path.read_text(encoding="utf-8").split("\n")
                self.assertTrue(lines[0].startswith("# "),
                                "first line should be a comment naming the device, Android version and build")
                self.assertIn("Android", lines[0], "first line should name the Android version")
                pkgs = packages(path)
                self.assertTrue(pkgs, "no packages")
                for p in pkgs:
                    self.assertRegex(p, PACKAGE, f"not a package name: {p!r}")
                dupes = sorted({p for p in pkgs if pkgs.count(p) > 1})
                self.assertFalse(dupes, f"listed twice: {dupes}")

    def test_lists_leave_protected_packages_alone(self):
        keep = protected()
        for folder in folders():
            with self.subTest(folder=folder.name):
                hits = sorted(set(packages(folder / "debloat.txt")) & keep)
                self.assertFalse(hits, f"protected packages in the list: {hits}")

    def test_examples_table_matches_folders(self):
        lines = (EXAMPLES / "README.md").read_text(encoding="utf-8").split("\n")
        start = next(i for i, l in enumerate(lines) if l.startswith("| Folder |"))
        listed = []
        for line in lines[start + 2:]:
            if not line.startswith("|"):
                break
            m = re.match(r"\|\s*`([^`/]+)/`\s*\|", line)
            self.assertIsNotNone(m, f"row does not start with a `folder/` cell: {line[:60]}")
            listed.append(m.group(1))
        self.assertEqual(sorted(listed), [f.name for f in folders()],
                         "examples/README.md rows and examples/ folders differ")


if __name__ == "__main__":
    unittest.main()
