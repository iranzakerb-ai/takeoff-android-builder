> **TAKEOFF GOVERNANCE — CURRENT OWNER DECISIONS (v1.0 candidate; merge-dependent)**
>
> Canonical source after its reviewed merge: [TakeOff Master Constitution](https://github.com/iranzakerb-ai/takeoff-virality-engine/blob/main/docs/TAKEOFF_MASTER_CONSTITUTION.md) (the Engine repository is the only authoritative copy).
> Review candidate: [v1.0 governance branch](https://github.com/iranzakerb-ai/takeoff-virality-engine/blob/governance/takeoff-constitution-v1-20261019/docs/TAKEOFF_MASTER_CONSTITUTION.md).
> Before changing product behavior, verify the canonical file exists at the selected commit, identify applicable `GOV-/PRD-/MGT-/CUS-/ENG-/INF-/EXE-` rule IDs, and report any `SPEC_CONFLICT` with older documents; do not guess unresolved `Q-01`–`Q-07`.
> New owner decisions: **Customer has two offerings only** (`CUS-001`): ordinary Studio creates **10** (`CUS-002`), Reel-to-niche creates **1** (`CUS-003`), and Customer service use has **no mandatory visible login/OTP/profile** (`CUS-004`); preserve secure server-side access. Gemini effective verified Free Tier only (`ENG-006`).
> These lines supplement historical AGENTS/PRODUCT_CONSTITUTION documentation; when product directions conflict, seek the current owner-approved decision and do not reinstate superseded features. This governance-only PR **does not authorize source changes, Production deployment, billing, credential rotation, or AI inference**.

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
