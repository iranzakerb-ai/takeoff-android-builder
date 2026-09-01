# TakeOff Product Constitution — TWO-MISSION LOCK

> **NON-NEGOTIABLE PRODUCT CONTRACT**
>
> Every human contributor, AI coding agent, Builder, Auditor, DADSTAN reviewer, CI job, backend change and Android release must preserve and improve the two missions below. If a proposed feature does not directly support one of these two learning loops, it is out of scope unless the product owner explicitly changes this constitution.

## Mission 1 — Owner Outcome Capture (Meta API substitute)

TakeOff Insights exists to capture **authorized Instagram Insights from the agency's own pages and client/employer pages** when direct Meta API access is unavailable.

The app must make it practical to connect a published Reel/post back to the scenario that produced it and capture the real post-publication outcome, including whenever Instagram exposes it on screen:

- retention / retention curve;
- average and total watch time;
- completion or equivalent retention signals;
- views/reach;
- shares, saves and comments;
- other useful Owner Insights and outcome evidence.

This evidence is not a vanity dashboard. It must feed the TakeOff learning system so the scenario agent can learn which generated hooks, structures, dialogue, CTA and mechanisms actually performed well or poorly on real agency/client accounts.

Because the product is a Meta-API substitute, it may use on-device capture/OCR and user-authorized flows. It must never fabricate unavailable Owner metrics or silently convert missing data to zero.

## Mission 2 — Viral Reel Learning: Learn From Million-View / High-Performing Reels

The user can manually **Share a public Instagram Reel/post to TakeOff**. The server must then, as available and lawful:

1. retrieve the public media;
2. inspect the video/audio and timeline;
3. transcribe intelligible speech using the configured speech/transcription path (Whisper or an equivalent configured model);
4. perform multimodal visual analysis;
5. reconstruct and store evidence about the content, including:
   - first-second / first-three-second hook;
   - dialogue and on-screen text;
   - scenario/story structure;
   - CTA;
   - visual grammar, camera, pacing and editing;
   - music/audio role;
   - behavioral mechanisms;
   - retention hypotheses;
   - share/save/comment hypotheses;
   - reusable patterns and uncertainties;
6. promote only evidence that passes the learning/evidence rules into the reusable memory used by scenario generation.

The purpose is not to copy a viral Reel. The purpose is to understand reusable mechanisms and use them together with real Owner outcomes to improve original scenario generation.

## The Combined Learning Loop

`Scenario -> Publish -> Owner Outcome Capture -> Learn`

plus

`High-performing public Reel -> Server analysis -> Extract mechanisms -> Learn`

both feed

`Evidence-backed memory -> Better original scenarios`

These two loops are the product. The Android UI must make them obvious and must not bury them under engineering terminology.

## North Star, not a false guarantee

The product owner's ambition is to push generated scenarios toward extremely high and ideally million-view performance, with a stated aspirational target of approaching 99% million-view outcomes. This is a **north-star optimization target, not a promise, probability claim, SLA or causal guarantee**. Instagram distribution cannot be guaranteed before publication.

Agents must optimize for measurable improvement while preserving scientific honesty: correlation is not causation, unavailable evidence is not zero, and no release may claim a guaranteed reach number.

## UI / UX Lock

The home screen must expose exactly two primary actions:

1. **Owner stats / outcome capture**
2. **Share and analyze viral Reels**

Default UI is simple, Persian-first and modern glassmorphism. Internal terms such as queues, lanes, provider names, evidence plumbing and backend topology belong in secondary diagnostics, not the primary experience.

## DADSTAN enforcement

DADSTAN is adversarial to the Builder and must reject a release when any of the following is true:

- either mission is broken, fake, disconnected or reduced to a visual placeholder;
- public Reel learning incorrectly requires an Owner/Companion credential;
- Owner metrics are fabricated or missing values are treated as observed zero;
- viral analysis does not preserve evidence/uncertainty boundaries;
- the primary UI drifts into a technical console instead of the two user jobs;
- a release claims a guaranteed million-view outcome;
- code changes materially drift from this constitution without an explicit product-owner change.

**When in doubt, preserve the two learning loops and reject scope drift.**
