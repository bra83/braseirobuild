# Project audit index

All audits below are evidence maps, not certification. A claim can move to `CERTIFICADO` only after an exact official source reference is bound to the deterministic effect, the real controller/UI path, persisted state after reopen, and an executed focused test. Existing test names or static code matches alone do not satisfy that gate.

## Intake and preserved source

- `BASELINE_INTAKE.json` records the verified P244 archive, WIP5 MERGED overlay, Drive inventory, and Git branch baseline.
- `audits/INVENTORY_FULL.json` records 527 Drive records / 44 folders; 498 records remain unreviewed after limited intake of the two Core Rulebook PDFs. Adventure scope is restricted to the five named PDFs; 22 other adventure PDFs are excluded.
- `audits/INITIAL_FULL_CODE_AUDIT.md` records the initial architecture and preservation scan.

## SWE02: The Long Arm of the Hutt

- `audits/SWE02_INITIAL_SOURCE_GAP_MATRIX.md` is the initial gap summary.
- `audits/SWE02_THREE_ACT_AUDIT_INDEX.md` links the three source-page audits.
- `audits/SWE02_ACT1_DEEP_AUDIT.md` covers printed pp.2–24.
- `audits/SWE02_ACT2_DEEP_AUDIT.md` covers printed pp.25–38.
- `audits/SWE02_ACT3_DEEP_AUDIT.md` covers printed pp.39–46.
- Each audited source claim remains pending implementation and full per-claim proof. The Act 1 Old Lylek Den pp.15–18 has a source-bounded WIP and passing focused/affected tests, but still has explicit pending gaps: T-rex, party-wide consequences, per-PC crash selection, bluff adjudication, inventory transfer, and lylek/Bura branch disposition. See the Act 1 audit addendum; no page is certified.

## Rules and mechanical authority

- `audits/AOR_360_INITIAL_AUDIT.md` is the structural AoR inventory audit.
- `audits/AOR_360_CONSUMER_CROSSWALK_AUDIT.md` statically maps all 360 positions to source labels and likely runtime references. It is not a semantic PDF review; 0/360 individual positions are certified.
- `audits/AOR_ACE_GUNNER_INDIVIDUAL_AUDIT.md` verifies all 20 `ace:gunner` rows against the official PDF (printed p.68). All remain pending; it found a printed-page metadata error and two Brace slots currently mapped to Grit.
- `validation/AOR_360_INDIVIDUAL_PROOFS.jsonl` is the position-level proof ledger; all 360 positions remain pending.
- `audits/MECHANICAL_AUTHORITY_AUDIT.md` traces rules, controller/UI, persistence, RAG, and Gemini/Barbara boundaries. All reviewed domains remain pending full executed end-to-end evidence.
- Narrator proposals are constrained and deterministic executors own mutations, but allowlisted economy/world operations can still persist state changes. Source-rule ID membership is not semantic proof that a specific operation is authorized.
- `audits/RULES_CORE_SOURCE_INTAKE_20261016.md` records local SHA-256, page count, cover/metadata and extraction quality for both EotE/AoR core PDFs; matching Drive byte sizes do not prove remote byte identity or semantic rule coverage. The other 17 source rows and final supplement selection remain unresolved.
- P.13's physician Critical Injury attempt was added after the P240 audit: `validation/P241_SWE02_SOURCE_PROOFS.json` and `tests/uae-p241-swe02-nyn-newmeen-social.test.mjs` document the exact pool, one-attempt gate, success/failure state, UI and three reopens. Printed p.13 remains pending for the other page-wide gaps.
- P.12 initiative now follows the printed awareness exception: Thwheek rolls Cool; each PC rolls Cool unless the GM records that they did not see the fighter emerge and turn to attack, in which case they roll Vigilance. Per-PC skill choice persists with the sorted turn order. Focused evidence: `evidence/SWE02_P12_INITIATIVE_EXCEPTION_20261016/` (20/20); printed p.12 remains pending for complete page claims and regression.
- P.12 crash crew checks snapshot the initiative roster and use stable PC IDs, with save/reopen validation and an explicit soft-landing upgrade proof. See `evidence/SWE02_P12_CRASH_CREW_IDS_20261016/` (18/18).

## Other required adventures

- `audits/SWEP01_DEEP_AUDIT.md` audits Under a Black Sun pp.18–39 against the official Drive PDF and recovered UI/state/tests. It finds major source and runtime gaps; status remains `PENDENTE`.
- `audits/SWE05_DEEP_AUDIT.md` audits Beyond the Rim pp.4–96 (PDF pp.5–97) against its official Drive PDF. It finds major source/effect/persistence gaps; status remains `PENDENTE`.
- `audits/SWE09_DEEP_AUDIT.md` audits The Jewel of Yavin against the official PDF, including printed/PDF page mapping and the race, bank, heist, escape, and aftermath surfaces. Major source-completeness gaps remain; status is `PENDENTE`.
- `audits/SWE13_DEEP_AUDIT.md` audits Mask of the Pirate Queen against the official Drive PDF, including printed/PDF page mapping and route, checks, space combat, boarding, and ending surfaces. Major source/effect/persistence gaps remain; status is `PENDENTE`.

## Current execution gate

- SWE02 is `IMPLEMENTADO_NAO_CERTIFICADO`.
- No P245 canonical checkpoint, APK/Gradle build, CI/device gate, or sixth adventure is authorized before all semantic gates close.
- The last full fresh regression passed 3,391 of 3,393 tests across 376 files; 2 offline wrapper tests were skipped because the JDK `jar` executable is absent. The regression predates the in-progress Old Lylek implementation, so it does not validate that work.
