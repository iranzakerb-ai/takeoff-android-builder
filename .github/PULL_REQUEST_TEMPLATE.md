# TakeOff — bounded, rules-first change request

Read the [canonical constitution](https://github.com/iranzakerb-ai/takeoff-virality-engine/blob/main/docs/TAKEOFF_MASTER_CONSTITUTION.md) at a specific commit (before adoption use PR #78 exact head SHA and label it CANDIDATE, OWNER_APPROVAL=PENDING).

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
