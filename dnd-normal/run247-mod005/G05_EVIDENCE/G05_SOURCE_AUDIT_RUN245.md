# G05 source audit — RUN245

## Scope and source finding

The RUN245 issue inventory has 280 rows: 149 cite *Ravenloft: The Horrors Within* (RHW, 2026) and 131 cite the 2025 *Monster Manual* (XMM), for 266 unique creature/action pairs. All 280 rows were individually compared with auxiliary structured 5e.tools records. This is not primary-book proof and does not approve an executor.

| Edition | Issue rows | Unique creature/action pairs | Individual 5e.tools record review | Primary PDF |
|---|---:|---:|---|---|
| *Ravenloft: The Horrors Within* (2026; RHW) | 149 | 135 | 149/149 rows matched and mechanics fields reviewed where exposed | Exact PDF not found among 256 direct Drive-folder items |
| *Monster Manual* (2025; XMM) | 131 | 131 | 131/131 rows matched and mechanics fields reviewed where exposed | Raw binary exceeds connector limit; OCR returned but is noisy |
| **Total** | **280** | **266** | **280/280 rows individually reviewed** | Primary-source validation remains incomplete |

## Method and edition safeguards

- RHW rows were individually matched by creature, action, edition/source code and printed page against the 5e.tools RHW structured bestiary. `https://5e.tools/books.html` plus the cited publisher record identifies *Ravenloft: The Horrors Within* (2026), separate from *Van Richten’s Guide to Ravenloft* (2021). No Van Richten entry was used as a substitute.
- XMM rows were matched against the XMM structured bestiary. Review captures action section/trigger/usage, attack or save type, range/target, damage, save or escape DC, conditions/effects/duration and recharge/use limits when present.
- The user-provided G05 addendum was incorporated: https://drive.google.com/file/d/1FC8tF_FIt6tHT8d0f2IpY7JmgpRZCGNR/view. The XMM Drive PDF is `Manual dos Monstros 2024.pdf`, ID `1iqPMW_ljJPWZMs3jYVUagceeZ6xe7N9o`, 277,647,654 bytes. Raw binary transfer returned HTTP 413 because that is larger than the connector limit 268,435,456 bytes. Default Drive text extraction separately returned 1,066,072 characters of OCR.
- OCR was queried for each XMM row near estimated text offsets for creature/action labels. The audit reports 87 rows with both labels near the estimated page, 20 with the creature label only, and 24 without a reliable local creature match. Because OCR contains misread terms and estimated offsets are imperfect, these are locator signals only. OCR did not validate DC, save, duration, conditions or effect mechanics; all remain not homologated against the printed page. The 131-row CSV combines the paraphrased auxiliary 5e.tools fields (edition, section/trigger, attack/damage, save or escape DC, duration/conditions/effects, use limit and RUN245 gap) with OCR offsets and label-only match results; it contains no copied OCR excerpts.
- The RHW Drive-folder search returned 256 direct items and no exact RHW PDF. Other Ravenloft PDFs were not substituted. Review CSVs paraphrase facts and do not copy book text.

## Source/code findings

- The review distinguishes saves from grapple escape checks and ability checks. Examples and per-row differences are in the evidence files. XMM Aberrant Cultist Tentacle Lash, for example, uses escape DC 14 and imposes Grappled and Restrained until the grapple ends; that escape DC is not a saving throw.
- XMM Bugbear Stalker Morningstar and Bugbear Warrior Light Hammer receive advantage against a target already grappled by that bugbear; they do not themselves inflict Grappled. Quick Grapple is separate.
- 18 rows retain spell expansion or ambiguity: 7, 11, 12, 19, 30, 36–39, 50, 77, 111, 114, 115, 124, 125, 148 and 149. Their mechanics were not inferred.
- The code contains four source-bound handler entries: Brain-Rending Bite, Soothing Tentacle, Radiant Flame and Crushing Stone. The focused tests passed in BRELGA run 38061684317 as part of its 76/76 green suite. These slices do not cover the full 280 actions. The other 276 issue rows have no action-specific deterministic executor approved.
- Row facts are in `G05_AUXILIARY_REVIEW_5ETOOLS_280.csv` and six detailed row-range review sheets. The 280-row source/code crosswalk is `G05_SOURCE_TO_EXECUTOR_MATRIX.csv`.

## Gate disposition

G05 remains **open and not homologated**. Auxiliary 5e.tools structured records provide row-level review but do not replace the RHW primary source or readable XMM pages. OCR only gives label-location hints. Four tested handler entries are present; 276 rows have no approved executor; 18 retain expansion or ambiguity. The absent source evidence prevents safe expansion without inventing mechanics.
