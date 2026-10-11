# Official rule gaps

Quell Unrest: reject same skill in consecutive turns.
Claim Hex: restrict to 1/2/3 attempts per turn at kingdom levels 1/4/9.
Collect Taxes: Trade check only.

Validation pending on the canonical R35 Android project.

## Structure catalog crosswalk — NOT CERTIFIED

- Official source: Kingmaker.pdf, Google Drive file 1hHjK7DjhdDFvejYk70NNxO0zMp7Rnsn3, settlement structures in Appendix 2, pages 544–552.
- 73 structural records extracted as machine-readable mechanical data; per-entry PDF parsing and alternative commodity costs still require review.
- The **verified, published R35** catalogue was compared against 18 mapped canonical structures in hosted CI 38176766679; **18/18 have at least one discrepancy**, including missing official construction skill/DC metadata. This is not evidence that every numeric field is wrong.
- Next gate: apply reviewed authoritative costs, levels, lot sizes, skill, proficiency and structure DC in R36; enforce nonstacking bonuses and validate Android database/gameplay behavior before release.
- Read-only crosswalk: ci/pf2-kingmaker-certification/audit_structure_catalog.py
- Official numeric reference: ci/pf2-kingmaker-certification/kingmaker_official_structures_p544_552.json
- CI reference: https://github.com/bra83/braseirobuild/actions/runs/38176766679
