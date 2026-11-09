# TakeOff — bounded, rules-first change request

Read the [canonical constitution](https://github.com/iranzakerb-ai/takeoff-virality-engine/blob/main/docs/TAKEOFF_MASTER_CONSTITUTION.md) at an exact commit. Current review candidate is [Engine PR #84](https://github.com/iranzakerb-ai/takeoff-virality-engine/pull/84), v1.1.0; OWNER_APPROVAL=APPROVED for v1.0 product locks and infrastructure amendment CR-20261107-01; Q-01–Q-07 remain unresolved except the explicitly recorded infrastructure/provider decisions; GOVERNANCE_MERGE=PENDING; PRODUCTION_CHANGE_AUTHORIZATION=NOT GRANTED.

> **Authorization default:** `PRODUCTION_CHANGE_AUTHORIZATION=NOT GRANTED` is the default for this individual change request. The separately approved D13 authorization is limited to the Customer migration and conditional `app.takeoffads.ir` cutover after every D13 acceptance and rollback gate passes; it does not authorize this governance PR to merge, deploy, or change Production.

**PRODUCT:** MANAGEMENT | CUSTOMER | SHARED_ENGINE | GOVERNANCE

**GOAL (one scope only):**

**RULE_IDS:** GOV-... / CUS-... / MGT-... / ENG-... / INF-... / EXE-...

**OWNER_CHANGE_ID:** NONE (preserves rules) | CR-YYYYMMDD-NN (requires explicit approval)

**BASELINE:** exact repository, branch and SHA; prior verified evidence.

**CHANGED_PATHS:**

**NON_GOALS / PROTECTED_PATHS:**

**ACCEPTANCE_GATES:** G-CUS-... / G-MGT-... / G-ENG-... / G-SEC-... / G-REL-...

**TIME_WORK_BUDGET:**

**STOP_CONDITIONS:** two substantially identical failures → one bounded alternative; stop at scope/budget/security boundary.

**EXPECTED_DELIVERABLES / CHECKPOINTS:**

**DOCUMENT_GATES:** command + actual result; not product compliance.

**PRODUCT_CONTRACT_GATES / LIVE_RELEASE_GATES:** actual environment/SHA or NOT_TESTED.

**PROOF:** local test output, exact SHA, Preview results if applicable; mark fixture separately from live E2E.

**SECURITY & COST:** No secrets/paid API/billing activation/unauthorized authentication changes.

**ROLLBACK / MIGRATION:** N/A or exact plan.

**SPEC_CONFLICTS / UNRESOLVED_QUESTIONS:** NONE or exact conflicting documents and Q-xx.

**OWNER APPROVAL:** Required only for new/changed product DNA, security boundary, paid service or Production promotion.

> **Non-negotiable Customer checks:** 2 services, Studio=10, Viral Reel=1, no visible mandatory login. This review form does not prove code implements those behaviors.
