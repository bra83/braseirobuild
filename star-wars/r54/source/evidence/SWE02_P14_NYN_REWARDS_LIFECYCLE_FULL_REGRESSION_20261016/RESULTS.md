# SWE02 p.14 Nyn rewards — lifecycle evidence

- Deterministic code owns dice, reward claims, stable PC recipient assignment, equipment profiles, use records and inventory removal. RAG retrieves the cited adventure/rule text; Gemini may narrate or suggest typed actions only.
- Primary equipment source: EotE Core Rulebook, Drive ID `1UO3Z35J2Dmkfvx1OLyPD3rzJon013EoX`, SHA-256 `AFCF0FE3B5C9FC132DD7B641B54DCCE703BEE2FB01EE8F9EBE213F2CD3BB85C6`; printed pp.162–163, 165, 169 (profile map in `validation/P241_SWE02_SOURCE_PROOFS.json`).
- Focused P241 suite: 17 tests, 17 passed, 0 failed, 0 skipped (`p241-focused.tap`). Covers recipient distribution, profile stats/source, two-Threat defective rifle, removal after a weapon attack at combat closure, preserving an unused award, UI/bridge and save/reopen.
- Full per-file regression: 382 files (376 regular, 6 stress), 3,455 tests, 3,453 passed, 0 failed, 2 skipped. JDK `jar` was unavailable for two offline Gradle-wrapper packaging checks. See `summary.json` and per-file TAP logs.
- Scope remains `IMPLEMENTADO_NAO_CERTIFICADO`: Nabat p.14 dialogue and service-tunnel transition prerequisites are not fully source-mapped. The armor-hit use marker is implemented; the relevant p.14 defect outcome principally applies to the Triumph rifle after symbols cancel.
- No source PDF or full-book extraction was committed.
