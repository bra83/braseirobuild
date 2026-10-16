# Rules Corpus Reconciliation

**Audit type:** read-only source-to-project reference comparison  
**Scope:** the 19 rulebook/supplement-type rows in the master handoff’s “important PDFs found” table  
**Project checkout:** `star-wars/r54/source` in the sparse Git checkout  
**Evidence limit:** the original comparison was metadata/reference-only. A follow-up local intake examined the two Core Rulebook PDFs listed below; the other 17 rows remain without byte-level verification. Project references indicate catalog or rules artifacts only; they do not certify the underlying PDF.

The handoff describes these rows with an “initial role” column. The first 12 comprise two core books plus ten named supplements. Seven additional relevant entries remain unresolved. This reconciliation reports all 19 and does not settle which supplements the user selected for active implementation.

## Reconciliation matrix

| # | Exact handoff title | Drive ID | Initial handoff role | Matching project reference and path | Mapping status |
|---:|---|---|---|---|---|
| 1 | EotE Core Rulebook.pdf | `1UO3Z35J2Dmkfvx1OLyPD3rzJon013EoX` | EotE core — autoridade | ID occurs in `app/src/main/assets/vtt/js/expansion/galacticWorldCatalogWave6.json` for four source records (Tatooine, Bespin, Kessel, Ryloth); `rules/rules_matrix.json` has 33 EotE Core rows by edition/source label but no Drive ID on those rows. | **PRESENT** — exact ID in expansion catalog; matrix linkage is title/edition-based. |
| 2 | Star Wars - Age of Rebellion - Core Rulebook.pdf | `1w2M6f7R_dzaaHk6Q6R8Y7TIqAYM2MJIq` | AoR core — autoridade | Exact ID in `rules/sources.json` at `coreCandidate`; `rules/rules_matrix.json` has `SW-AOR-CHAR-DUTY-002` (printed pp. 46–50); `app/src/main/assets/vtt/js/expansion/aorTalentTreesData.mjs` uses the book title and page labels. | **PRESENT** — registered as a candidate, with partial title/page references. |
| 3 | Dangerous Covenants.pdf | `1K4hNuYFy5OUpZAOdJWuKY_4SfnHnEdw7` | Suplemento EotE | No matching title or ID found in the checked rules catalogs/manifests or expansion references. | **ABSENT** |
| 4 | Enter the Unknown.pdf | `1AjyaJtmD2I7-U81aI2lSP9_O8BG5Io5e` | Suplemento EotE | No matching title or ID found in the checked rules catalogs/manifests or expansion references. | **ABSENT** |
| 5 | Far Horizons.pdf | `1UCi6y2Fsf7-Vxun-nwpYvZrrCSXMj4DP` | Suplemento EotE | No matching title or ID found in the checked rules catalogs/manifests or expansion references. | **ABSENT** |
| 6 | Fly Casual.pdf | `13cJ3toYFHpjY24EbEA5TtjI0iPrNQUY-` | Suplemento EotE | `rules/rules_matrix.json`, record `SW-EOTE-FLY-CASUAL-VEHICLE-CATALOG-001`, cites PDF pp. 55–61 / printed pp. 54–60; `app/src/main/assets/vtt/js/expansion/equipmentCatalog.mjs` cites Fly Casual, printed p. 48. Neither reference carries the Drive ID. | **TITLE_ONLY** — project references the book by title, with no exact ID linkage. |
| 7 | Lords of Nal Hutta.pdf | `1zlE5YefFJfTjd-Eodx9FfiP-dmXCnwpx` | Suplemento EotE | No matching title or ID found in the checked rules catalogs/manifests or expansion references. | **ABSENT** |
| 8 | Suns of Fortune.pdf | `1J90hz3rQuaxqt_uHTnCF1z3HtAzK4Lve` | Suplemento EotE | Exact ID in `app/src/main/assets/vtt/js/expansion/galacticWorldCatalogWave6.json` for a Corellia source record, printed p. 10. | **PRESENT** — exact ID in expansion catalog. |
| 9 | Game Master's Kit.pdf | `1oV4qifN_H5Z_85ADOvU4dgla3LqZXGNc` | Regras e material de Mestre | No matching title or ID found in the checked rules catalogs/manifests or expansion references. | **ABSENT** |
| 10 | SWFFG - Allies and Adversaries.pdf | `1GQPawcJ-ly3Kfk8-HTIyh20H4t9zWDwn` | Estatísticas NPCs/ameaças | `rules/sources.json` lists “SWFFG - Allies and Adversaries.pdf” under `supplementsPending`; no Drive ID or content record is attached. | **UNRESOLVED** — pending title reference only. |
| 11 | SWFFG - Rise of the Seperatists.pdf | `1t9CQCpynfffjSD-Be-jr-SxKFgVkezda` | Expansão — verificar integração escolhida | `rules/sources.json` lists “SWFFG - Rise of the Seperatists.pdf” under `supplementsPending`; no Drive ID or content record is attached. | **UNRESOLVED** — pending title reference only. |
| 12 | SWFFG - Collapse of the Republic.pdf | `1eNHkn6y0LVLGIafsFlzyuR5YahqjNtXb` | Expansão — verificar integração escolhida | `rules/sources.json` lists “SWFFG - Collapse of the Republic.pdf” under `supplementsPending`; no Drive ID or content record is attached. | **UNRESOLVED** — pending title reference only. |
| 13 | Star Wars - Dawn of Rebellion.pdf | `1hwidmF7fHJg1UWEMFBdEb0MqRzMXjAYE` | Expansão — verificar integração escolhida | `rules/sources.json` lists “Star Wars - Dawn of Rebellion.pdf” under `supplementsPending`; no Drive ID or content record is attached. | **UNRESOLVED** — pending title reference only. |
| 14 | SWA42 Age Of Rebellion - Forged In Battle [2016].pdf | `1vEQfjDSjMnIfnlEc_dJ1bNNIe--7j6Od` | Suplemento AoR | No matching title or ID found in the checked rules catalogs/manifests or expansion references. | **ABSENT** |
| 15 | SWA53 Age Of Rebellion - Cyphers & Masks.pdf | `1u5WoDc3c4gtPbFB-C-5gz4P03ozZ04_N` | Suplemento AoR | No matching title or ID found in the checked rules catalogs/manifests or expansion references. | **ABSENT** |
| 16 | pdfcoffee.com_aor-fo-fully-operational-engineers-swa47-2-pdf-free.pdf | `1IQOFkHfDKB9pU6f8tjQI-0z50_Wdhm3q` | Suplemento AoR; verificar qualidade/proveniência | No matching title or ID found in the checked rules catalogs/manifests or expansion references. | **ABSENT** |
| 17 | pdfcoffee.com_special-modifications-pdf-free.pdf | `1mcLB4W2iszlO06AZYmIn6yL3oZUd-ni-` | Suplemento EotE; verificar qualidade/proveniência | No matching title or ID found in the checked rules catalogs/manifests or expansion references. | **ABSENT** |
| 18 | pdfcoffee.com_star-wars-gadgets-and-gear-4-pdf-free.pdf | `1G0_lRYsqhR0D0CaxJE4Ie11n1D_Ahblf` | Catálogo de equipamentos; verificar qualidade | `app/src/main/assets/vtt/js/expansion/gadgets_gear_mechanical_crosswalk_wave40.json` attributes 63 project records to “Gadgets and Gear” with authority `PRIMARY_PAGE_TEXT_PLUS_VISUAL_HEADER`; no Drive ID is attached. | **TITLE_ONLY** — expansion references are title-only; their claims do not establish file identity or source quality. |
| 19 | pdfcoffee.com_star-wars-starships-and-speeders-pdf-free.pdf | `16K2dHhoSoP7yPBB8NzNep7QEdgXmoj9j` | Catálogo de naves; verificar qualidade | `app/src/main/assets/vtt/js/expansion/starships_speeders_global_reconciliation_wave36.json` attributes 96 records to “Starships and Speeders” with `PRIMARY_VISUAL_VALIDATED`; no Drive ID is attached. | **TITLE_ONLY** — expansion references are title-only; their claims do not establish file identity or source quality. |

## Coverage summary

- **PRESENT:** 3 entries (EotE Core, AoR Core, Suns of Fortune).
- **TITLE_ONLY:** 3 entries (Fly Casual, Gadgets and Gear, Starships and Speeders).
- **UNRESOLVED:** 4 entries (Allies and Adversaries, Rise of the Seperatists, Collapse of the Republic, Dawn of Rebellion).
- **ABSENT:** 9 entries (Dangerous Covenants, Enter the Unknown, Far Horizons, Lords of Nal Hutta, Game Master's Kit, Forged in Battle, Cyphers & Masks, Fully Operational, Special Modifications).

Thus the seven additional entries beyond the 12 initial roles include **six with some title/pending reference** (Allies, Rise, Collapse, Dawn, Gadgets, Starships) and **one absent** (Game Master's Kit). This does not determine whether those seven, or any specific subset, are in the user's selected implementation scope.

## Core PDF byte intake follow-up (2026-10-16)

Two local PDFs in the user-provided source workspace were hashed and inspected with PDF metadata/page-count tools. Their byte sizes exactly match the Drive records for the corresponding IDs; Drive did not provide a content checksum, so byte-for-byte identity against a fresh Drive download is not independently proven.

| Source | Drive ID | Local size | Local SHA-256 | Pages | Visual/metadata | Text extraction |
|---|---|---:|---|---:|---|---|
| EotE Core Rulebook.pdf | `1UO3Z35J2Dmkfvx1OLyPD3rzJon013EoX` | 207,887,235 | `AFCF0FE3B5C9FC132DD7B641B54DCCE703BEE2FB01EE8F9EBE213F2CD3BB85C6` | 457 | Cover confirms *Edge of the Empire Roleplaying Game — Core Rulebook*; PDF metadata names Fantasy Flight Games. | First pages extract some text but the OCR is degraded; page 3 yielded no text. Full-book extraction quality is not established. |
| Age of Rebellion Core Rulebook.pdf | `1w2M6f7R_dzaaHk6Q6R8Y7TIqAYM2MJIq` | 278,764,931 | `25087E5B43C289EF556A365EEC1177123055DFB75C3ECF03481CD66044CACE50` | 466 | Cover confirms *Age of Rebellion Roleplaying Game — Core Rulebook*; PDF metadata names Fantasy Flight Games. | First three pages yielded no extractable text; this appears image-only and needs page-level visual/OCR workflow for semantic audits. |

This intake verifies local file identity by title/cover, metadata, exact byte-size agreement with Drive, and local SHA-256. It does **not** verify all contents, errata, printed-to-PDF page mapping, or semantic coverage. It does not select supplements or change rules/AoR certification status.

## Remaining verification limits

For the 17 remaining corpus rows, no PDF bytes were examined. For all 19 rows, the following are still not verified corpus-wide:

- edition and printing;
- PDF hash (SHA-256) or identity of the bytes behind the Drive ID;
- page count or whether page references match that exact PDF;
- language;
- text extraction/OCR quality, completeness, or visual legibility;
- tables, mechanics, or other page content;
- official status or provenance.

`rules/rag_manifest.json` summarizes 54 matrix records and includes a hash of `rules/rules_matrix.json`; it does not contain per-PDF hashes or establish PDF content quality. The expansion record counts above are references found in project artifacts, not independent certification of source files. No catalog or source status was changed as part of this audit.

