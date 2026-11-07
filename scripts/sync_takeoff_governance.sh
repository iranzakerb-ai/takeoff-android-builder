#!/usr/bin/env bash
# Fetches the approved TAKEOFF master constitution from GitHub main into a
# read-only cache. It never changes an application worktree or checks out a branch.
set -Eeuo pipefail
umask 077

REPO_SLUG="iranzakerb-ai/takeoff-virality-engine"
DOC_PATH="docs/TAKEOFF_MASTER_CONSTITUTION.md"
MIN_VERSION="$(printenv TAKEOFF_GOVERNANCE_MIN_VERSION || true)"
if [[ -z "$MIN_VERSION" ]]; then MIN_VERSION="1.1.0"; fi
DEFAULT_REMOTE="git@github.com:iranzakerb-ai/takeoff-virality-engine.git"
REMOTE="$(printenv TAKEOFF_GOVERNANCE_REMOTE || true)"
if [[ -z "$REMOTE" ]]; then REMOTE="$DEFAULT_REMOTE"; fi
CACHE_ROOT="$HOME/.cache/takeoff-governance"
XDG_CACHE="$(printenv XDG_CACHE_HOME || true)"
if [[ -n "$XDG_CACHE" ]]; then CACHE_ROOT="$XDG_CACHE/takeoff-governance"; fi
CUSTOM_CACHE="$(printenv TAKEOFF_GOVERNANCE_CACHE || true)"
if [[ -n "$CUSTOM_CACHE" ]]; then CACHE_ROOT="$CUSTOM_CACHE"; fi
BARE_REPO="$CACHE_ROOT/engine.git"

fail() {
  printf 'GOVERNANCE_SYNC=BLOCKED\nREASON=%s\n' "$1" >&2
  if [[ "$#" -ge 2 ]]; then exit "$2"; fi
  exit 1
}

command -v git >/dev/null 2>&1 || fail "git is unavailable"
command -v python3 >/dev/null 2>&1 || fail "python3 is unavailable"
mkdir -p "$CACHE_ROOT"

if [[ ! -e "$BARE_REPO" ]]; then
  tmp_repo="$CACHE_ROOT/.engine.git.$$"
  rm -rf -- "$tmp_repo"
  if ! git clone --bare --filter=blob:none "$REMOTE" "$tmp_repo"; then
    rm -rf -- "$tmp_repo"
    fail "cannot clone the private canonical Engine repository; verify GitHub SSH/credential access"
  fi
  mv -- "$tmp_repo" "$BARE_REPO"
fi

[[ -d "$BARE_REPO" ]] || fail "governance cache path exists but is not a directory"
is_bare="$(git --git-dir="$BARE_REPO" rev-parse --is-bare-repository 2>/dev/null || true)"
[[ "$is_bare" == "true" ]] || fail "governance cache is not a bare Git repository; preserve it and inspect manually"

actual_remote="$(git --git-dir="$BARE_REPO" remote get-url origin 2>/dev/null || true)"
case "$actual_remote" in
  git@github.com:iranzakerb-ai/takeoff-virality-engine.git|ssh://git@github.com/iranzakerb-ai/takeoff-virality-engine.git|https://github.com/iranzakerb-ai/takeoff-virality-engine.git)
    ;;
  *)
    fail "cached origin is not the canonical GitHub repository; refusing to fetch from an unexpected remote"
    ;;
esac

if ! git --git-dir="$BARE_REPO" fetch --no-tags origin \
  "+refs/heads/main:refs/remotes/origin/main"; then
  fail "git fetch of canonical GitHub main failed; cached governance was not treated as current"
fi

main_sha="$(git --git-dir="$BARE_REPO" rev-parse refs/remotes/origin/main)"
tmp_doc="$(mktemp "$CACHE_ROOT/.constitution.XXXXXX")"
trap 'rm -f -- "$tmp_doc"' EXIT

if ! git --git-dir="$BARE_REPO" show "$main_sha:$DOC_PATH" >"$tmp_doc"; then
  fail "canonical constitution is absent from GitHub main"
fi

if ! python3 - "$tmp_doc" "$MIN_VERSION" <<'PY'
import re
import sys
from pathlib import Path

doc = Path(sys.argv[1]).read_text(encoding="utf-8")
minimum = tuple(int(x) for x in sys.argv[2].split("."))
match = re.search(r"(?m)^\*\*Version:\*\* .([0-9]+(?:\.[0-9]+){2})\b", doc)
if not match:
    raise SystemExit("canonical version metadata missing")
actual = tuple(int(x) for x in match.group(1).split("."))
if actual < minimum:
    raise SystemExit(f"GitHub main contains {match.group(1)}, below required {sys.argv[2]}; governance PR is not merged")
required = (
    "CR-20261107-01",
    "OWNER_APPROVAL=APPROVED",
    "GitHub is the single source of truth",
    "Supabase Self-Hosted",
    "shared learning memory",
    "GitHub Self-Hosted Runner",
)
missing = [item for item in required if item not in doc]
if missing:
    raise SystemExit("canonical main is missing approved governance clauses: " + ", ".join(missing))
PY
then
  fail "GitHub main does not yet contain the approved governance revision; do not use a stale or unmerged candidate"
fi

doc_sha256="$(sha256sum "$tmp_doc" | awk '{print $1}')"
snapshot_dir="$CACHE_ROOT/snapshots/$main_sha"
mkdir -p "$snapshot_dir"
install -m 0600 "$tmp_doc" "$snapshot_dir/TAKEOFF_MASTER_CONSTITUTION.md"
meta_tmp="$snapshot_dir/.SOURCE.tmp.$$"
{
  printf 'REPOSITORY=%s\n' "$REPO_SLUG"
  printf 'REF=refs/heads/main\n'
  printf 'COMMIT=%s\n' "$main_sha"
  printf 'DOCUMENT=%s\n' "$DOC_PATH"
  printf 'DOCUMENT_SHA256=%s\n' "$doc_sha256"
} >"$meta_tmp"
chmod 0600 "$meta_tmp"
mv -f -- "$meta_tmp" "$snapshot_dir/SOURCE.txt"

printf 'GOVERNANCE_SYNC=PASS\n'
printf 'REPOSITORY=%s\n' "$REPO_SLUG"
printf 'REF=main\n'
printf 'COMMIT=%s\n' "$main_sha"
printf 'DOCUMENT_SHA256=%s\n' "$doc_sha256"
printf 'CACHED_DOCUMENT=%s\n' "$snapshot_dir/TAKEOFF_MASTER_CONSTITUTION.md"
printf 'WORKTREE_MODIFIED=NO\n'
