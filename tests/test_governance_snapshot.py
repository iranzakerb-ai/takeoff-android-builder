from __future__ import annotations

import hashlib
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).parent.parent / "scripts"))
from publish_takeoff_governance_snapshot import SnapshotBlocked, publish

HELPER = Path(__file__).parent.parent / "scripts" / "publish_takeoff_governance_snapshot.py"


class GovernanceSnapshotTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.doc = self.root / "canonical.md"
        self.meta = self.root / "expected-source.txt"
        self.doc.write_text("constitution v1.1.0\nCR-20261107-01\n", encoding="utf-8")
        self.sha = hashlib.sha256(self.doc.read_bytes()).hexdigest()
        self.meta.write_text(
            "REPOSITORY=iranzakerb-ai/takeoff-virality-engine\n"
            "REF=refs/heads/main\nCOMMIT=abc123\n"
            "DOCUMENT=docs/TAKEOFF_MASTER_CONSTITUTION.md\n"
            f"DOCUMENT_SHA256={self.sha}\nSYNC_SCRIPT_SHA256=def456\n"
            "VALIDATOR=PASS\nCONTRACT_TESTS=PASS\n",
            encoding="utf-8",
        )
        self.snapshot = self.root / "snapshots" / "abc123"

    def tearDown(self):
        # Test fixtures only; no production snapshots are created by this suite.
        self.temp.cleanup()

    def run_publish(self):
        return publish(self.snapshot, self.doc, self.meta, self.sha)

    def test_first_snapshot_creation(self):
        self.assertEqual(self.run_publish(), "CREATED")
        self.assertEqual((self.snapshot / "TAKEOFF_MASTER_CONSTITUTION.md").read_bytes(), self.doc.read_bytes())
        self.assertEqual((self.snapshot / "SOURCE.txt").read_bytes(), self.meta.read_bytes())
        self.assertEqual((self.snapshot / "TAKEOFF_MASTER_CONSTITUTION.md").stat().st_mode & 0o222, 0)
        self.assertEqual((self.snapshot / "SOURCE.txt").stat().st_mode & 0o222, 0)
        self.assertEqual(self.snapshot.stat().st_mode & 0o222, 0)

    def test_repeated_synchronization_reuses_without_modification(self):
        self.assertEqual(self.run_publish(), "CREATED")
        before = {p.name: (p.stat().st_ino, p.stat().st_mtime_ns) for p in self.snapshot.iterdir()}
        self.assertEqual(self.run_publish(), "REUSED")
        after = {p.name: (p.stat().st_ino, p.stat().st_mtime_ns) for p in self.snapshot.iterdir()}
        self.assertEqual(before, after)

    def test_existing_snapshot_integrity_is_verified(self):
        self.assertEqual(self.run_publish(), "CREATED")
        self.assertEqual(self.run_publish(), "REUSED")

    def test_tampered_snapshot_is_rejected_and_not_repaired(self):
        self.assertEqual(self.run_publish(), "CREATED")
        stored = self.snapshot / "TAKEOFF_MASTER_CONSTITUTION.md"
        stored.chmod(0o644)
        stored.write_text("tampered\n", encoding="utf-8")
        stored.chmod(0o444)
        with self.assertRaisesRegex(SnapshotBlocked, "differs from canonical"):
            self.run_publish()
        self.assertEqual(stored.read_text(encoding="utf-8"), "tampered\n")

    def test_incomplete_snapshot_is_rejected(self):
        self.snapshot.mkdir(parents=True)
        (self.snapshot / "TAKEOFF_MASTER_CONSTITUTION.md").write_text("partial\n", encoding="utf-8")
        with self.assertRaisesRegex(SnapshotBlocked, "incomplete"):
            self.run_publish()

    def test_metadata_tampering_is_rejected(self):
        self.assertEqual(self.run_publish(), "CREATED")
        stored = self.snapshot / "SOURCE.txt"
        stored.chmod(0o644)
        stored.write_text("COMMIT=wrong\n", encoding="utf-8")
        stored.chmod(0o444)
        with self.assertRaisesRegex(SnapshotBlocked, "metadata differs"):
            self.run_publish()

    def test_completed_snapshot_is_reused_if_creator_left_a_stale_lock(self):
        self.assertEqual(self.run_publish(), "CREATED")
        stale_lock = self.snapshot.parent / ".locks" / f"{self.snapshot.name}.lock"
        stale_lock.mkdir(parents=True)
        self.assertEqual(self.run_publish(), "REUSED")
        self.assertTrue(stale_lock.is_dir())

    def test_concurrent_creation_has_one_creator_and_no_overwrite(self):
        command = [sys.executable, str(HELPER), str(self.snapshot), str(self.doc), str(self.meta), self.sha]
        processes = [subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True) for _ in range(12)]
        results = [process.communicate(timeout=40) + (process.returncode,) for process in processes]
        self.assertTrue(all(code == 0 for _, _, code in results), results)
        actions = [line.split("=", 1)[1] for out, _, _ in results for line in out.splitlines() if line.startswith("SNAPSHOT_ACTION=")]
        self.assertEqual(actions.count("CREATED"), 1, results)
        self.assertEqual(actions.count("REUSED"), 11, results)
        self.assertEqual((self.snapshot / "TAKEOFF_MASTER_CONSTITUTION.md").read_bytes(), self.doc.read_bytes())


if __name__ == "__main__":
    unittest.main(verbosity=2)
