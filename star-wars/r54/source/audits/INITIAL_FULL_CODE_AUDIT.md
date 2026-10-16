# Initial full code audit - 2026-10-16

This is an intake and structural audit, not a completion certificate.

## Verified recovery

- P244 SHA-256 `6f3dca72ea55080b4ce7e6befeb5d4d84e51f63453ead6a5719495178a40d264`, 111,402,494 bytes, 16,506 entries; archive CRC passed. Each of four source parts matched the checkpoint manifest.
- WIP5 MERGED SHA-256 `7615fc8d156f8909a7a31b0878d6e08715cd71867325c5ad6c97ac5dd830409e`, 1,987,299 bytes, 1,163 entries; CRC passed. Its base hash matches P244. All 1,162 manifest files verified; 7 replace P244 files and 1,155 add files. Applied to a separate working copy.
- Google Drive recursive inventory: 527 records across 44 folders. Five in-scope adventure PDFs were selected by exact file ID; 22 other adventure PDFs were excluded. 500 records remain metadata-only/unreviewed.
- GitHub `main` resolves to `98b6a391401818e74eaa53c752cc98dc9082fea9`, an older P79 state. Work is isolated on `codex/star-wars-hibrido-integral-20261016`, scoped to `star-wars/r54/source`.

## Semantic findings

- SWE02 has 46 physical PDF pages (cover plus printed pages 2-46). `audits/SWE02_INITIAL_SOURCE_GAP_MATRIX.md` records source-page implementation anchors and unresolved questions. All three acts remain IMPLEMENTADO_NAO_CERTIFICADO.
- The AoR tree has 360 catalog positions. `validation/AOR_360_INDIVIDUAL_PROOFS.jsonl` enumerates all positions and marks each PENDENTE; name-level runtime flags are not individual semantic proof.
- Five-adventure policy and Android architecture exist in the recovered tree, but presence is not certification. No P245 was created.

## Regression evidence

- - Corrected fresh regression covered 376 files (370 regular plus all six stress files), 3,393 tests, 3,391 passes, zero failures, and two skips. The skips are the two JDK `jar`-dependent offline Gradle-wrapper packaging checks; the reason is recorded in raw TAP. Full logs and summary are in `evidence/fresh-regression-20261016/`.
- The first-run stale catalog assertion was corrected to assert the electro-gate operation exists; the affected file passed individually and again in the full run.
- No APK, Gradle, CI, or device gate was run.

## Risk and next work

The project is not complete. Finish the fresh regression, push this recoverable WIP, and continue source comparison and implementation of SWE02 Act 1 as the first full semantic block.
