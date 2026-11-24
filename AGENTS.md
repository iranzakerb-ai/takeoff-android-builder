# AI / AGENT START HERE

Before reading implementation details or changing this repository, read **`PRODUCT_CONSTITUTION.md` in full**.

That constitution is the product owner's authoritative scope lock. The Android application exists for exactly two primary user jobs:

1. capture authorized Owner Insights / post-publication outcomes for agency and client pages so scenario generation can learn from real retention, watch time, shares, saves and related evidence when Meta API access is unavailable;
2. receive manually shared high-performing Instagram Reels/posts and send them through the server-side viral-learning pipeline so TakeOff can understand hooks, dialogue, scenario structure, CTA, visuals, editing, audio and reusable behavioral mechanisms.

## Shared infrastructure governance — mandatory

Before any infrastructure, deployment, CI/CD, database, API endpoint, networking or release-architecture task, read and follow the canonical Engine governance file:
`https://github.com/iranzakerb-ai/takeoff-virality-engine/blob/main/AGENT_LOCKS.md`.

Current owner-locked target: VPS-based Production with independent PostgreSQL. Local Windows/Hyper-V is optional for development/testing, never a mandatory Production host. Vercel and Supabase are not approved runtime dependencies; Cloudflare is prohibited and must not be accessed. Production is the only deployable environment: no Staging or Preview. This policy does not claim that a VPS has already been provisioned or authorize a deployment by itself.

## Mandatory agent behavior

- Do not turn the app into a generic analytics dashboard or engineering console.
- Keep the home screen centered on exactly those two primary actions.
- Prefer a simple Persian-first glassmorphism UX; technical diagnostics are secondary.
- Never reintroduce a Companion credential requirement for public viral-Reel learning.
- Never fabricate unavailable Owner metrics or treat missing values as zero.
- Never promise a guaranteed million-view result. The product owner's 99% ambition is a north-star optimization target, not a factual probability or guarantee.
- DADSTAN must oppose the Builder and reject releases that violate the constitution, even if they compile successfully.

For any ambiguity, `PRODUCT_CONSTITUTION.md` wins over implementation convenience, comments, old UI text, and stale tests.
