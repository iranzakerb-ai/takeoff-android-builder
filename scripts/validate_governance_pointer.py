#!/usr/bin/env python3
"""Validate that this repository pins the exact Engine PR #84 candidate helpers."""
from __future__ import annotations

import hashlib
from pathlib import Path

EXPECTED_SYNC_BLOB = "f5c971aaea19fcb1c596445cb912535350ca5b9a"
EXPECTED_SNAPSHOT_BLOB = "568b9d677e9db3254d2d3312bcddc4191dcbd8b6"
EXPECTED_SNAPSHOT_TEST_BLOB = "fce71c8ebb2a70fdf41fd75671911b5695cb412a"
EXPECTED_DOCUMENT_SHA256 = "ac3d6014698b2341348bf28d0fb7338f2ec0acc5c6d8d756eb736eec170493d3"


def git_blob_sha(path: Path) -> str:
    data = path.read_bytes()
    header = b"blob " + str(len(data)).encode("ascii") + b"\0"
    return hashlib.sha1(header + data).hexdigest()


def validate_root(root: Path) -> list[str]:
    errors = []
    agents_path = root / "AGENTS.md"
    template_path = root / ".github" / "PULL_REQUEST_TEMPLATE.md"
    sync_path = root / "scripts" / "sync_takeoff_governance.sh"
    snapshot_path = root / "scripts" / "publish_takeoff_governance_snapshot.py"
    snapshot_test_path = root / "tests" / "test_governance_snapshot.py"
    for path in (agents_path, template_path, sync_path, snapshot_path, snapshot_test_path):
        if not path.is_file():
            errors.append(f"missing required governance file: {path.relative_to(root)}")
    if errors:
        return errors

    agents = agents_path.read_text(encoding="utf-8")
    template = template_path.read_text(encoding="utf-8")
    if "/pull/84" not in agents or "/pull/78" in agents:
        errors.append("AGENTS.md must point to current Engine PR #84, not the historical PR #78")
    if EXPECTED_DOCUMENT_SHA256 not in agents:
        errors.append("AGENTS.md does not pin the exact current Engine candidate document SHA-256")
    if "/pull/84" not in template or "/pull/78" in template:
        errors.append("PR template must point to current Engine PR #84")
    if "PRODUCTION_CHANGE_AUTHORIZATION=NOT GRANTED" not in template:
        errors.append("PR template must retain the default per-change Production denial")
    if "Authorization default:" not in template or "D13 authorization is limited to the Customer migration" not in template:
        errors.append("PR template must distinguish default denial from scoped D13 authority")
    if (root / "docs" / "TAKEOFF_MASTER_CONSTITUTION.md").exists():
        errors.append("dependent repository must not create a competing canonical constitution copy")

    expected = (
        (sync_path, EXPECTED_SYNC_BLOB),
        (snapshot_path, EXPECTED_SNAPSHOT_BLOB),
        (snapshot_test_path, EXPECTED_SNAPSHOT_TEST_BLOB),
    )
    for path, expected_sha in expected:
        actual = git_blob_sha(path)
        if actual != expected_sha:
            errors.append(f"{path.relative_to(root)} differs from the exact Engine candidate helper ({actual})")
    return errors


def main() -> int:
    root = Path(__file__).resolve().parents[1]
    errors = validate_root(root)
    for error in errors:
        print("ERROR:", error)
    print("GOVERNANCE_POINTER=" + ("FAIL" if errors else "PASS"))
    return 1 if errors else 0


if __name__ == "__main__":
    raise SystemExit(main())
