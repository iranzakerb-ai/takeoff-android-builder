# TakeOff Insights — Android

> **Start here:** read [`PRODUCT_CONSTITUTION.md`](PRODUCT_CONSTITUTION.md) before changing this product. AI agents must also read [`AGENTS.md`](AGENTS.md).

TakeOff Insights is intentionally centered on **two learning loops only**:

1. **Owner Outcome Capture** — capture authorized Instagram Owner Insights for the agency's own pages and client pages (Retention, Watch Time, Share/Save and other available outcome evidence) as a practical substitute when Meta API access is unavailable, then connect those outcomes back to generated scenarios so the system learns what actually worked.
2. **Viral Reel Learning** — manually Share a high-performing/million-view public Instagram Reel/post to TakeOff; the server retrieves and analyzes the media, speech/dialogue, hook, scenario, CTA, visuals, editing, audio and behavioral mechanisms, then stores evidence-backed reusable learning for future original scenario generation.

The product owner's ambition is to drive generated scenarios toward extremely high and ideally million-view performance. That ambition is a **north star, not a guaranteed probability**. The system must not fabricate evidence or promise reach.

## UX rule

The primary app experience must remain simple, Persian-first and glassmorphism-based, with exactly two obvious primary actions matching the two missions above. Backend/provider/queue details are secondary diagnostics.

## Repository role

This is the public, secret-free Android build mirror for TakeOff Insights. Backend code, credentials, private Owner Insight data and production secrets are intentionally excluded.

The release pipeline validates package/version, live backend health, unit tests, lint, APK integrity/signature, manifest contracts, mission-lock source checks and secret scans before publishing an artifact.
