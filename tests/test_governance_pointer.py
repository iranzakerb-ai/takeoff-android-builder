from __future__ import annotations

import importlib.util
from pathlib import Path
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location(
    "governance_pointer_validator",
    ROOT / "scripts/validate_governance_pointer.py",
)
module = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(module)


class GovernancePointerTests(unittest.TestCase):
    def test_current_pointer_and_helper_hashes_pass(self):
        self.assertEqual(module.validate_root(ROOT), [])

    def test_stale_pr_pointer_is_rejected(self):
        with tempfile.TemporaryDirectory() as td:
            root = Path(td)
            (root / ".github").mkdir()
            (root / "scripts").mkdir()
            (root / "tests").mkdir()
            (root / "docs").mkdir()
            for relative in (
                "scripts/sync_takeoff_governance.sh",
                "scripts/publish_takeoff_governance_snapshot.py",
                "tests/test_governance_snapshot.py",
            ):
                source = ROOT / relative
                target = root / relative
                target.write_bytes(source.read_bytes())
            (root / "AGENTS.md").write_text(
                (ROOT / "AGENTS.md").read_text(encoding="utf-8").replace("/pull/84", "/pull/78"),
                encoding="utf-8",
            )
            (root / ".github/PULL_REQUEST_TEMPLATE.md").write_text(
                (ROOT / ".github/PULL_REQUEST_TEMPLATE.md").read_text(encoding="utf-8"),
                encoding="utf-8",
            )
            self.assertTrue(any("PR #84" in e for e in module.validate_root(root)))


if __name__ == "__main__":
    unittest.main(verbosity=2)
