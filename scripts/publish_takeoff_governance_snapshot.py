#!/usr/bin/env python3
"""Create or verify a read-only, commit-addressed, hash-verified governance snapshot."""
from __future__ import annotations

import hashlib
import os
from pathlib import Path
import stat
import sys
import time


class SnapshotBlocked(Exception):
    pass


def _sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def _readonly(path: Path) -> bool:
    return not (stat.S_IMODE(path.stat(follow_symlinks=False).st_mode) & 0o222)


def _verify(snapshot: Path, document: Path, metadata: Path, expected_sha: str) -> None:
    if snapshot.is_symlink() or not snapshot.is_dir():
        raise SnapshotBlocked("snapshot path is not a real directory")
    expected_names = {"TAKEOFF_MASTER_CONSTITUTION.md", "SOURCE.txt"}
    try:
        actual_names = {entry.name for entry in snapshot.iterdir()}
    except OSError as exc:
        raise SnapshotBlocked(f"cannot inspect snapshot: {exc}") from exc
    if actual_names != expected_names:
        raise SnapshotBlocked("snapshot is incomplete or contains unexpected entries")
    stored_doc = snapshot / "TAKEOFF_MASTER_CONSTITUTION.md"
    stored_meta = snapshot / "SOURCE.txt"
    for path in (stored_doc, stored_meta):
        if path.is_symlink() or not path.is_file():
            raise SnapshotBlocked(f"snapshot entry is not a regular file: {path.name}")
        if not _readonly(path):
            raise SnapshotBlocked(f"snapshot entry is writable: {path.name}")
    if _sha256(stored_doc) != expected_sha or stored_doc.read_bytes() != document.read_bytes():
        raise SnapshotBlocked("existing snapshot document differs from canonical content")
    if stored_meta.read_bytes() != metadata.read_bytes():
        raise SnapshotBlocked("existing snapshot metadata differs from canonical metadata")
    if not _readonly(snapshot):
        raise SnapshotBlocked("completed snapshot directory is writable")


def publish(snapshot: Path, document: Path, metadata: Path, expected_sha: str,
            wait_seconds: float = 30.0) -> str:
    if _sha256(document) != expected_sha:
        raise SnapshotBlocked("canonical document SHA-256 does not match metadata")
    snapshot.parent.mkdir(mode=0o700, parents=True, exist_ok=True)
    lock_root = snapshot.parent / ".locks"
    lock_root.mkdir(mode=0o700, exist_ok=True)
    lock = lock_root / f"{snapshot.name}.lock"
    deadline = time.monotonic() + wait_seconds
    while True:
        try:
            lock.mkdir(mode=0o700)
            break
        except FileExistsError:
            if snapshot.exists() or snapshot.is_symlink():
                try:
                    _verify(snapshot, document, metadata, expected_sha)
                    return "REUSED"
                except SnapshotBlocked:
                    # A live writer may still be publishing. Keep waiting; a
                    # completed snapshot with a stale lock is reusable above.
                    pass
            if time.monotonic() >= deadline:
                raise SnapshotBlocked("another synchronizer holds the snapshot lock")
            time.sleep(0.05)
    try:
        if snapshot.exists() or snapshot.is_symlink():
            _verify(snapshot, document, metadata, expected_sha)
            return "REUSED"
        # mkdir is exclusive. A crash leaves a detectable incomplete directory;
        # it is intentionally never deleted or overwritten automatically.
        snapshot.mkdir(mode=0o700)
        doc_target = snapshot / "TAKEOFF_MASTER_CONSTITUTION.md"
        meta_target = snapshot / "SOURCE.txt"
        for source, target in ((document, doc_target), (metadata, meta_target)):
            fd = os.open(target, os.O_WRONLY | os.O_CREAT | os.O_EXCL | getattr(os, "O_NOFOLLOW", 0), 0o600)
            try:
                with os.fdopen(fd, "wb") as dest, source.open("rb") as src:
                    while True:
                        chunk = src.read(1024 * 1024)
                        if not chunk:
                            break
                        dest.write(chunk)
                    dest.flush()
                    os.fsync(dest.fileno())
            except BaseException:
                raise
        _verify_writable_content(snapshot, document, metadata, expected_sha)
        doc_target.chmod(0o444)
        meta_target.chmod(0o444)
        snapshot.chmod(0o555)
        dir_fd = os.open(snapshot, os.O_RDONLY | getattr(os, "O_DIRECTORY", 0))
        try:
            os.fsync(dir_fd)
        finally:
            os.close(dir_fd)
        _verify(snapshot, document, metadata, expected_sha)
        return "CREATED"
    except FileExistsError as exc:
        raise SnapshotBlocked("snapshot appeared concurrently; refusing to overwrite") from exc
    except OSError as exc:
        raise SnapshotBlocked(f"snapshot publication failed: {exc}") from exc
    finally:
        try:
            lock.rmdir()
        except OSError:
            pass


def _verify_writable_content(snapshot: Path, document: Path, metadata: Path, expected_sha: str) -> None:
    stored_doc = snapshot / "TAKEOFF_MASTER_CONSTITUTION.md"
    stored_meta = snapshot / "SOURCE.txt"
    if _sha256(stored_doc) != expected_sha or stored_doc.read_bytes() != document.read_bytes():
        raise SnapshotBlocked("new snapshot document failed content verification")
    if stored_meta.read_bytes() != metadata.read_bytes():
        raise SnapshotBlocked("new snapshot metadata failed content verification")


def main(argv: list[str]) -> int:
    if len(argv) != 5:
        print("usage: publish_takeoff_governance_snapshot.py SNAPSHOT DOCUMENT METADATA DOCUMENT_SHA256", file=sys.stderr)
        return 2
    snapshot, document, metadata = map(Path, argv[1:4])
    try:
        action = publish(snapshot, document, metadata, argv[4])
    except (SnapshotBlocked, OSError, ValueError) as exc:
        print(f"GOVERNANCE_SYNC=BLOCKED\nREASON={exc}", file=sys.stderr)
        return 1
    print(f"SNAPSHOT_ACTION={action}")
    print(f"SNAPSHOT={snapshot}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
