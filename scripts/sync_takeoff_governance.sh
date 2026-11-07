#!/usr/bin/env bash
# Fetch and validate the owner-approved TAKEOFF master constitution from GitHub main.
# Uses a separate bare cache and temporary review tree; never changes the application worktree.
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
command -v sha256sum >/dev/null 2>&1 || fail "sha256sum is unavailable"
command -v readlink >/dev/null 2>&1 || fail "readlink is unavailable"
command -v tar >/dev/null 2>&1 || fail "tar is unavailable"

CURRENT_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || true)"
[[ -n "$CURRENT_ROOT" ]] || fail "run this script from inside one of the three TAKEOFF Git worktrees"
[[ -f "$CURRENT_ROOT/AGENTS.md" ]] || fail "current repository has no root AGENTS.md"
CURRENT_SCRIPT="$(readlink -f "$0")"
[[ -f "$CURRENT_SCRIPT" ]] || fail "cannot resolve the current governance sync script"
grep -Fq "TAKEOFF_MASTER_CONSTITUTION.md" "$CURRENT_ROOT/AGENTS.md" || fail "current AGENTS.md does not point to the master constitution"
grep -Fq "scripts/sync_takeoff_governance.sh" "$CURRENT_ROOT/AGENTS.md" || fail "current AGENTS.md does not require governance sync"

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
tmp_review="$(mktemp -d "$CACHE_ROOT/.review.XXXXXX")"
trap 'rm -rf -- "$tmp_review"' EXIT

if ! git --git-dir="$BARE_REPO" archive --format=tar "$main_sha" \
  "$DOC_PATH" \
  "AGENTS.md" \
  ".github/PULL_REQUEST_TEMPLATE.md" \
  "scripts/validate_takeoff_constitution.py" \
  "scripts/sync_takeoff_governance.sh" \
  "tests/test_master_constitution_contract.py" | tar -xf - -C "$tmp_review"; then
  fail "cannot extract the governance review files from canonical main"
fi

review_doc="$tmp_review/$DOC_PATH"
review_script="$tmp_review/scripts/sync_takeoff_governance.sh"
[[ -s "$review_doc" ]] || fail "canonical constitution is missing or empty"
[[ -s "$review_script" ]] || fail "canonical sync script is missing or empty"

if ! python3 - "$review_doc" "$MIN_VERSION" <<'PY'
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

current_script_sha="$(sha256sum "$CURRENT_SCRIPT" | awk '{print $1}')"
canonical_script_sha="$(sha256sum "$review_script" | awk '{print $1}')"
[[ "$current_script_sha" == "$canonical_script_sha" ]] || fail "this repository's sync script differs from the canonical Engine main copy; update the repo pointer PR"

if ! (cd "$tmp_review" && python3 scripts/validate_takeoff_constitution.py); then
  fail "canonical Master Constitution validator failed"
fi
if ! (cd "$tmp_review" && python3 tests/test_master_constitution_contract.py); then
  fail "canonical Master Constitution contract tests failed or the standard-library contract test failed"
fi

doc_sha256="$(sha256sum "$review_doc" | awk '{print $1}')"
snapshot_dir="$CACHE_ROOT/snapshots/$main_sha"
mkdir -p "$snapshot_dir"
install -m 0600 "$review_doc" "$snapshot_dir/TAKEOFF_MASTER_CONSTITUTION.md"
meta_tmp="$snapshot_dir/.SOURCE.tmp.$$"
{
  printf 'REPOSITORY=%s\n' "$REPO_SLUG"
  printf 'REF=refs/heads/main\n'
  printf 'COMMIT=%s\n' "$main_sha"
  printf 'DOCUMENT=%s\n' "$DOC_PATH"
  printf 'DOCUMENT_SHA256=%s\n' "$doc_sha256"
  printf 'SYNC_SCRIPT_SHA256=%s\n' "$canonical_script_sha"
  printf 'VALIDATOR=PASS\n'
  printf 'CONTRACT_TESTS=PASS\n'
} >"$meta_tmp"
chmod 0600 "$meta_tmp"
mv -f -- "$meta_tmp" "$snapshot_dir/SOURCE.txt"

printf 'GOVERNANCE_SYNC=PASS\n'
printf 'REPOSITORY=%s\n' "$REPO_SLUG"
printf 'REF=main\n'
printf 'COMMIT=%s\n' "$main_sha"
printf 'DOCUMENT_SHA256=%s\n' "$doc_sha256"
printf 'SYNC_SCRIPT_SHA256=%s\n' "$canonical_script_sha"
printf 'VALIDATOR=PASS\n'
printf 'CONTRACT_TESTS=PASS\n'
printf 'CACHED_DOCUMENT=%s\n' "$snapshot_dir/TAKEOFF_MASTER_CONSTITUTION.md"
printf 'WORKTREE_MODIFIED=NO\n'
