# G05 — RUN245: source review, executor and test disposition

## Current source audit

- 280 issue rows reviewed in structured auxiliary 5e.tools data: 149 *Ravenloft: The Horrors Within* (RHW, 2026) and 131 *Monster Manual* (XMM, 2025), 266 distinct creature/action pairs. RHW 2026 is not *Van Richten’s Guide to Ravenloft* (2021).
- Fields captured per record where exposed: section/action/trigger/use budget, attack or saving throw, range/targets, damage, save or escape DC, condition/effect/duration, and source-vs-RUN245 gap. References are paraphrased; book contents were not copied.
- 5e.tools is auxiliary. No exact RHW PDF was found in 256 direct Drive-folder items. The 277,647,654-byte XMM PDF raw download returned HTTP 413 (connector limit 268,435,456 bytes).
- Drive readable OCR returned 1,066,072 characters. The combined per-row review in `G05_EVIDENCE/qa-ci/G05_XMM_PDF_OCR_CROSSCHECK_131.csv` contains edition and paraphrased 5e.tools action/save-or-escape/DC/effect facts plus OCR page-local label signals: creature+action found 87, creature-only 20, no reliable locator 24. OCR text is noisy and pagination estimates are imperfect. It supplies locator hints only; DCs, saves, durations, conditions and effects are **not homologated from OCR**.
- 18 rows still require spell expansion or ambiguity resolution; see row evidence.

## Code and BRELGA

Four source-bound handler entries are present for Brain-Rending Bite, Soothing Tentacle, Radiant Flame and Crushing Stone. Focused cases and the complete 76-test Android suite passed on BRELGA [run 38061684317](https://github.com/bra83/braseirobuild/actions/runs/38061684317), commit `24364e46d74768131ab569989ba64ab402efd6d3`. The latest evidence release ZIP SHA-256 is `b4dd27e40341b0783cd7c23f7a5ec8c8846056f2bb2ee7b0758930c64e7058fa`.

These tests validate only implemented slices. The other 276 issue rows have no approved action-specific deterministic executor. G05 remains **open and not homologated**. No rules were filled by inference.

## Evidence index

- `G05_EVIDENCE/G05_AUXILIARY_REVIEW_5ETOOLS_280.csv`: consolidated source facts.
- `G05_EVIDENCE/G05_SOURCE_TO_EXECUTOR_MATRIX.csv`: per-row code gap.
- `G05_EVIDENCE/qa-ci/G05_REVIEW_*.csv`: six row-range review sheets.
- `G05_EVIDENCE/qa-ci/G05_XMM_PDF_OCR_CROSSCHECK_131.csv`: combined auxiliary mechanic review and OCR locator audit for all XMM rows; no copied OCR excerpts.
- `G05_EVIDENCE/qa-ci/REPORT.md`: BRELGA suite chronology and evidence.
