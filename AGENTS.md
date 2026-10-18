# TakeOff governance entrypoint — v1.0.0 CANDIDATE

ADOPTION_STATUS=CANDIDATE; OWNER_APPROVAL=APPROVED (owner explicitly ratified v1.0 locked rules on 2026-10-19; Q-01–Q-07 remain UNRESOLVED). Approval of product rules does NOT authorize skipping independent review/required CI checks, GitHub merges, feature changes or Production actions.

Canonical destination: [Engine Master Constitution](https://github.com/iranzakerb-ai/takeoff-virality-engine/blob/main/docs/TAKEOFF_MASTER_CONSTITUTION.md).
Review candidate: [existing Engine PR #78](https://github.com/iranzakerb-ai/takeoff-virality-engine/pull/78). Owner-ratified locked-rules snapshot (NOT merged/adopted on `main`): [c9739f115c3b21d6ee768188e0fc1fe9f08f1a0c](https://github.com/iranzakerb-ai/takeoff-virality-engine/blob/c9739f115c3b21d6ee768188e0fc1fe9f08f1a0c/docs/TAKEOFF_MASTER_CONSTITUTION.md). Before adoption, read that PR's exact head SHA and disclose candidate status. After independent review, required checks and reviewed merge, pin the operationally adopted SHA in this pointer; never label a moving branch as already merged.

Applicable product: `MANAGEMENT`. Applicable rule IDs: MGT-001, MGT-002, MGT-003, MGT-004, MGT-005, MGT-006, MGT-007; ENG-006; EXE-003. Read the canonical document before product work; no second canonical copy in this repository. Latest explicit owner-approved decisions outrank approved constitution, applicable technical contracts, implementation evidence, historical docs, then AI recommendations (GOV-001).

Owner locks: Customer has TWO services; Studio TEN without Reel; Viral Reel ONE from URL+niche; no mandatory visible Customer registration/phone/OTP/Google/login. Server-side ownership, security, quotas and isolation remain mandatory. Management's two learning missions (authorized Owner Outcomes and public Viral Reel Learning) are distinct from those two Customer services. Supporting Management tools remain governed by existing decisions; Q-01–Q-07 stay UNRESOLVED. Gemini verified Free Tier only; no paid or non-Gemini fallback.

Every task records one goal, product, allowed scope, rule IDs, exact baseline, expected files, non-goals, acceptance tests, time/work budget, stop conditions and deliverables (section 8B). Use small verifiable checkpoints. EXE-003: after TWO substantially identical failures stop that approach, choose ONE bounded alternative, then report any blocker and finish independent safe work. No unbounded loop or unnecessary broad suite/audit.

Governance-only checks: run the Engine `scripts/validate_takeoff_constitution.py` with the three checkout roots, and `tests/test_master_constitution_contract.py`. DOCUMENT_GATES do not prove PRODUCT_CONTRACT_GATES or LIVE_RELEASE_GATES. PRODUCT_RUNTIME_COMPLIANCE=NOT_TESTED.

Historical instructions below are retained evidence. Any incompatible visible-login, third-Customer-service, old design or provider requirement is a named SPEC_CONFLICT, not authority to restore it. Record known old behavior as PREEXISTING_PRODUCT_NONCOMPLIANCE; this mission does not repair it. No hook/check bypass or automatic merge. Product work needs its own scope.

---

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

For any ambiguity, `PRODUCT_CONSTITUTION.md` wins over implementation convenience, comments, old UI text, and stale tests.
