# Project audit index

All audits below are evidence maps, not certification. A claim can move to `CERTIFICADO` only after an exact official source reference is bound to the deterministic effect, the real controller/UI path, persisted state after reopen, and an executed focused test. Existing test names or static code matches alone do not satisfy that gate.

## Intake and preserved source

- `BASELINE_INTAKE.json` records the verified P244 archive, WIP5 MERGED overlay, Drive inventory, and Git branch baseline.
- `audits/INVENTORY_FULL.json` records 527 Drive records / 44 folders; 500 records are metadata-only and remain unreviewed. Adventure scope is restricted to the five named PDFs; 22 other adventure PDFs are excluded.
- `audits/INITIAL_FULL_CODE_AUDIT.md` records the initial architecture and preservation scan.

## SWE02: The Long Arm of the Hutt

- `audits/SWE02_INITIAL_SOURCE_GAP_MATRIX.md` is the initial gap summary.
- `audits/SWE02_THREE_ACT_AUDIT_INDEX.md` links the three source-page audits.
- `audits/SWE02_ACT1_DEEP_AUDIT.md` covers printed pp.2–24.
- `audits/SWE02_ACT2_DEEP_AUDIT.md` covers printed pp.25–38.
- `audits/SWE02_ACT3_DEEP_AUDIT.md` covers printed pp.39–46.
- Each audited source claim remains pending implementation and full per-claim proof. The Act 1 Old Lylek Den block (pp.15–18) is currently in implementation review.

## Rules and mechanical authority

- `audits/AOR_360_INITIAL_AUDIT.md` is the structural AoR inventory audit.
- `audits/AOR_360_CONSUMER_CROSSWALK_AUDIT.md` statically maps all 360 positions to source labels and likely runtime references. It is not a semantic PDF review; 0/360 individual positions are certified.
- `audits/AOR_ACE_GUNNER_INDIVIDUAL_AUDIT.md` verifies all 20 `ace:gunner` rows against the official PDF (printed p.68). All remain pending; it found a printed-page metadata error and two Brace slots currently mapped to Grit.
- `validation/AOR_360_INDIVIDUAL_PROOFS.jsonl` is the position-level proof ledger; all 360 positions remain pending.
- `audits/MECHANICAL_AUTHORITY_AUDIT.md` traces rules, controller/UI, persistence, RAG, and Gemini/Barbara boundaries. All reviewed domains remain pending full executed end-to-end evidence.
- Narrator proposals are constrained and deterministic executors own mutations, but allowlisted economy/world operations can still persist state changes. Source-rule ID membership is not semantic proof that a specific operation is authorized.

## Other required adventures

- `audits/SWEP01_DEEP_AUDIT.md` audits Under a Black Sun pp.18–39 against the official Drive PDF and recovered UI/state/tests. It finds major source and runtime gaps; status remains `PENDENTE`.
- `audits/SWE05_DEEP_AUDIT.md` audits Beyond the Rim pp.4–96 (PDF pp.5–97) against its official Drive PDF. It finds major source/effect/persistence gaps; status remains `PENDENTE`.
- `audits/SWE09_DEEP_AUDIT.md` audits The Jewel of Yavin against the official PDF, including printed/PDF page mapping and the race, bank, heist, escape, and aftermath surfaces. Major source-completeness gaps remain; status is `PENDENTE`.
- `audits/SWE13_DEEP_AUDIT.md` audits Mask of the Pirate Queen against the official Drive PDF, including printed/PDF page mapping and route, checks, space combat, boarding, and ending surfaces. Major source/effect/persistence gaps remain; status is `PENDENTE`.

## Current execution gate

- SWE02 is `IMPLEMENTADO_NAO_CERTIFICADO`.
- No P245 canonical checkpoint, APK/Gradle build, CI/device gate, or sixth adventure is authorized before all semantic gates close.
- The last full fresh regression passed 3,391 of 3,393 tests across 376 files; 2 offline wrapper tests were skipped because the JDK `jar` executable is absent. The regression predates the in-progress Old Lylek implementation, so it does not validate that work.

