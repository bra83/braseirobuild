# SWE02 three-act audit index

Captured 2026-10-16 from the P244 + WIP5 working tree. These are independent, source-led audits, not certification ledgers.

| Block | Page scope | Audit | Status |
|---|---:|---|---|
| Act 1: Ryloth, Nabat, New Meen, escort and militia | printed pp.2-24 | `SWE02_ACT1_DEEP_AUDIT.md` | PENDENTE |
| Act 2: Geonosis, Piddock, Anatta, Dimmock, Vrixx'tt and the Vios | printed pp.25-38; p.38 boundary noted | `SWE02_ACT2_DEEP_AUDIT.md` | PENDENTE |
| Act 3: Mos Shuuta return and Teemo's palace | printed pp.39-46 | `SWE02_ACT3_DEEP_AUDIT.md` | PENDENTE |

## Cross-cutting disposition

- All three acts remain `IMPLEMENTADO_NAO_CERTIFICADO`; the existence of WIP modules, graph nodes, historic test logs, and aggregate source claims does not certify editorial completeness.
- The audits map source pages to current implementation and identify incomplete routes, missing effects, persistence/reopen gaps, and tests that do not prove full source semantics.
- The Act 3 audit exposed a concrete page attribution defect: printed p.43 is the palace map; gladiator recruitment and most workshop/kitchen/bar facts are on p.44; the Bith interview is on p.45. The related current operation data, gladiator receipt validation, source proof, and assertions were corrected. The offline bundle was regenerated from `.mjs` sources.
- The complete exact-page source matrices remain PENDENTE until every claim has a verified source page, deterministic executor, controller/UI path, durable state, three reopens, and a fresh focused test.
- No P245 checkpoint, APK, Gradle, CI, or device gate was created or run.

## First implementation sequence

1. Close one complete Act 1 route family from its source ledger, then verify failure/alternate paths and persistence before moving on.
2. Continue through Act 2 and Act 3 without importing any of the other 22 adventure PDFs.
3. Re-run all tests individually after source changes; keep raw logs and explained skips.
4. Do not promote SWE02 or create P245 until the three acts, aftermath branches, and all cited rule effects are individually certified.
