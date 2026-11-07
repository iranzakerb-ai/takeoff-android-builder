# TakeOff governance entrypoint — v1.1.0 CANDIDATE

GOVERNANCE_MERGE=PENDING
PRODUCTION_CHANGE_AUTHORIZATION=NOT GRANTED

ADOPTION_STATUS=CANDIDATE; OWNER_APPROVAL=APPROVED (v1.0 product locks ratified 2026-10-19; infrastructure amendment CR-20261107-01 approved 2026-11-07; Q-01–Q-07 remain unresolved except the recorded infrastructure/provider decisions). This does NOT authorize bypassing CI/review, unreviewed merges, DNS/traffic cutover, destructive decommission or unscoped Production changes.

Canonical destination: [Engine Master Constitution](https://github.com/iranzakerb-ai/takeoff-virality-engine/blob/main/docs/TAKEOFF_MASTER_CONSTITUTION.md).
Review candidate: [Engine PR #78](https://github.com/iranzakerb-ai/takeoff-virality-engine/pull/78). Exact current candidate snapshot: [f29b6a9782067f745c626c849ad0d02634618eca](https://github.com/iranzakerb-ai/takeoff-virality-engine/blob/f29b6a9782067f745c626c849ad0d02634618eca/docs/TAKEOFF_MASTER_CONSTITUTION.md). This is an unmerged candidate, not `main`; use it only for review until PR #78 is independently reviewed and merged.

Applicable product: `MANAGEMENT`. Applicable rule IDs: MGT-001, MGT-002, MGT-003, MGT-004, MGT-005, MGT-006, MGT-007; ENG-006; INF-001, INF-002, INF-003, INF-005; EXE-003. Read the canonical document before product work; no second canonical copy in this repository. Latest explicit owner-approved decisions outrank approved constitution, applicable technical contracts, implementation evidence, historical docs, then AI recommendations (GOV-001).

Owner locks: Customer has TWO services; Studio TEN without Reel; Viral Reel ONE from URL+niche; no mandatory visible Customer registration/phone/OTP/Google/login. Server-side ownership, security, quotas and isolation remain mandatory. Management's two learning missions (authorized Owner Outcomes and public Viral Reel Learning) are distinct from those two Customer services. Supporting Management tools remain governed by existing decisions. Infrastructure target is Ubuntu + Coolify + Docker Compose + Supabase Self-Hosted; PostgreSQL is the shared learning-memory store; GitHub is the sole governance authority; all three repositories use dedicated GitHub Self-Hosted Runners; Cloudflare, Space Iran and Gemini remain; Vercel, Supabase Cloud, Forgejo and Woodpecker are excluded operationally. Q-01–Q-07 remain unresolved except the approved infrastructure/provider decisions. Gemini remains the only AI provider; no paid or non-Gemini fallback.

Every task records one goal, product, allowed scope, rule IDs, exact baseline, expected files, non-goals, acceptance tests, time/work budget, stop conditions and deliverables (section 8B). Use small verifiable checkpoints. EXE-003: after TWO substantially identical failures stop that approach, choose ONE bounded alternative, then report any blocker and finish independent safe work. No unbounded loop or unnecessary broad suite/audit.

Governance-only checks: run the Engine `scripts/validate_takeoff_constitution.py` with the three checkout roots, and `tests/test_master_constitution_contract.py`. DOCUMENT_GATES do not prove PRODUCT_CONTRACT_GATES or LIVE_RELEASE_GATES. PRODUCT_RUNTIME_COMPLIANCE=NOT_TESTED.

SPEC_CONFLICT (authority precedence): `PRODUCT_CONSTITUTION.md` remains the product-scope contract for the two Management learning missions. The latest explicit owner decisions and canonical Master Constitution govern cross-repository authority and infrastructure; the local product constitution cannot restore superseded Vercel/Supabase Cloud/Forgejo/Woodpecker targets or override INF-001/INF-002.

Historical instructions below are retained evidence. Any incompatible visible-login, third-Customer-service, old design or provider requirement is a named SPEC_CONFLICT, not authority to restore it. Record known old behavior as PREEXISTING_PRODUCT_NONCOMPLIANCE; this mission does not repair it. No hook/check bypass or automatic merge. Product work needs its own scope.

---

## Mandatory GitHub governance sync — before every task

Before any Android source, signing, release, CI or governance work, run:

```bash
bash scripts/sync_takeoff_governance.sh
```

The script fetches canonical Engine `main` into a separate bare Git cache, verifies the owner-approved minimum version and change ID, and writes an immutable SHA-addressed snapshot under `~/.cache/takeoff-governance/`. It does not modify the Android worktree.

Read the exact cached master constitution and record its Git commit and SHA-256. If sync returns `GOVERNANCE_SYNC=BLOCKED`, stop governance-sensitive work; do not use a stale cache or unmerged candidate as active authority. PR #78's exact candidate SHA may be used for review only when clearly labelled CANDIDATE.

# AI / AGENT START HERE

Before reading implementation details or changing this repository, read **`PRODUCT_CONSTITUTION.md` in full**.

That constitution is the product owner's authoritative scope lock. The Android application exists for exactly two primary user jobs:

1. capture authorized Owner Insights / post-publication outcomes for agency and client pages so scenario generation can learn from real retention, watch time, shares, saves and related evidence when Meta API access is unavailable;
2. receive manually shared high-performing Instagram Reels/posts and send them through the server-side viral-learning pipeline so TakeOff can understand hooks, dialogue, scenario structure, CTA, visuals, editing, audio and reusable behavioral mechanisms.

## Mandatory agent behavior

- Do not turn the app into a generic analytics dashboard or engineering console.
- Keep the home screen centered on exactly those two primary actions.
- Prefer a simple Persian-first glassmorphism UX; technical diagnostics are secondary.
- Never reintroduce a Companion credential requirement for public viral-Reel learning.
- Never fabricate unavailable Owner metrics or treat missing values as zero.
- Never promise a guaranteed million-view result. The product owner's 99% ambition is a north-star optimization target, not a factual probability or guarantee.
- DADSTAN must oppose the Builder and reject releases that violate the constitution, even if they compile successfully.

For product scope and user-facing behavior, `PRODUCT_CONSTITUTION.md` remains the Management product contract, subject to later explicit owner decisions and the canonical Master Constitution. For shared governance and infrastructure, the Master Constitution is authoritative. Neither local file nor historical notes may override the latest owner-approved infrastructure amendment.
