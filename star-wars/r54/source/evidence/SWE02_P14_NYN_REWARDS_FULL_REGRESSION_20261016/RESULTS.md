# SWE02 p.14 Nyn rewards — evidence

- Implementation: deterministic reward claims and recipient-specific grants are recorded by the adventure module/controller; Gemini narration does not assign inventory or alter rolls. Source text remains a cited RAG concern.
- Primary equipment source: EotE Core Rulebook, Drive ID `1UO3Z35J2Dmkfvx1OLyPD3rzJon013EoX`, SHA-256 `AFCF0FE3B5C9FC132DD7B641B54DCCE703BEE2FB01EE8F9EBE213F2CD3BB85C6`; printed pp.162–163, 165, 169 (profile map in `validation/P241_SWE02_SOURCE_PROOFS.json`).
- Focused regression: 15 tests, 15 passed, 0 failed, 0 skipped (`p241-focused.tap`). Covers the verified profiles, stable roster recipient selection, per-PC grenade limit, unique armor/rifle, defect marking for Triumph plus two Threat, UI/bridge, and save/reopen.
- Full per-file regression: 382 files (376 regular, 6 stress), 3,453 tests, 3,451 passed, 0 failed, 2 skipped. JDK `jar` was unavailable for two offline Gradle-wrapper packaging checks. See `summary.json` and per-file TAP logs.
- Scope remains `IMPLEMENTADO_NAO_CERTIFICADO`: defective gear is marked on the item, but automatic discard after the first encounter in which it is used has not yet been connected to encounter closure. Remaining p.14 dialogue/travel gates are also open.
- No source PDF or full-book extraction was committed.
