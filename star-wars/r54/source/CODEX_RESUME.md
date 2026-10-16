# Codex resume

- Captured: 2026-10-16 (America/Sao_Paulo)
- Repository: `bra83/braseirobuild`
- Branch: `codex/star-wars-hibrido-integral-20261016`
- Base HEAD: `98b6a391401818e74eaa53c752cc98dc9082fea9`
- Last verified local and remote checkpoint: `431050613940a418cea2add0c22e56171d5763ac`
- Source working copy: `work/star-wars/project`; sparse Git checkout: `work/star-wars/repo-sparse/star-wars/r54/source`
- P244 SHA-256: `6f3dca72ea55080b4ce7e6befeb5d4d84e51f63453ead6a5719495178a40d264`
- WIP5 MERGED SHA-256: `7615fc8d156f8909a7a31b0878d6e08715cd71867325c5ad6c97ac5dd830409e`
- SWE02 remains `IMPLEMENTADO_NAO_CERTIFICADO`; its three-act audits are pushed. Old Lylek pp.15–18 source-backed implementation is in progress; source, UI, party consequences, linked loot, focused tests, integration and regression remain open.
- Official source audits are pushed for all five authorized adventures: SWE02, SWEP01, SWE05, SWE09, SWE13. All five remain uncertified; the four non-SWE02 audits document substantial coverage gaps.
- AoR 360 proof rows: 0 certified, 360 pending. Twenty Gunner slots and two Sharpshooter Brace positions have source evidence. The source mapping, rank/purchase provenance, and helper arithmetic focused suite passes 18/18; Brace action/controller/UI consumption and save/reopen remain unproven; no row may be certified yet.
- Drive inventory: 527 records/44 folders; 500 records remain metadata-only/unreviewed.
- Fresh full regression predates current code changes: 376 files, 3,393 tests, 3,391 pass, zero fail, two explained JDK-dependent skips. Must rerun after current changes stabilize.
- No APK/Gradle/device gate, P245, Drive backup/readback, or PR has been run/created. No non-Star-Wars VTT files touched.

## Exact next actions

1. Wait for Act1 implementation edits to stabilize. Review source citations and all outstanding p.15–18 claims including route, pilot/failure consequences, optional lookout, combat profile from SWE01 Beginner Game p.47, loot, continuation, UI, and party effects.
2. Run focused Old Lylek tests, rebuild generated UI bridge bundle via `node tools/build-ui-bridge-bundle.mjs`, then run full per-file regression and six stress suites; preserve TAP/log evidence.
3. After Act1 releases shared `controller.mjs`, `uiBridge.mjs`, and `ui/app.mjs`, integrate Brace maneuver into real action/controller/UI with environment-origin setbacks and persisted one-next-action consumption, then run focused save/reopen tests.
4. Selectively copy reviewed project-tree changes into sparse checkout, update resume/status, commit and push without force, verify `git ls-remote`.
5. Continue complete SWE02 semantic coverage, then rules and other adventures. Do not create P245 or run Android release gates before all editorial/functional gates pass.

