# SWE02 p.11 cataloged cargo transfer — focused proof

Date: 2026-10-16
Status: implemented; focused proof passes; p.11 and SWE02 remain uncertified.

## Source basis

- *The Long Arm of the Hutt*, printed p.11: ship inventory and discovery locations.
- *Edge of the Empire Core Rulebook*: electrobinoculars and general-purpose scanner p.175; hand scanner p.176; medpac p.177; breath mask and ration pack p.179; glow rod and tool kit p.181. Medicine's check effect is sourced separately to p.112.
- Source metadata was retrieved from Google Drive records for EotE Core (`1UO3Z35J2Dmkfvx1OLyPD3rzJon013EoX`) and Long Arm (`1Lm26krELYpWU0Rx0WgPj__g72YU_F2lC`).

## Behavior proved

- A GM/player action selects a discovered manifest item, an active roster PC by stable ID, and a positive integer quantity.
- Only exact catalog mappings with source-backed quantities are offered. Remaining quantity is conserved across transfers.
- The controller validates the rulebook page and catalog entry, updates the selected PC's gear and persists a transfer receipt transactionally.
- Duplicate PC display names do not confuse ownership. Save/reopen preserves one transfer without duplicating it.
- Unsupported story cargo, unknown quantities, droid parts, pelts, crates, credits, and unverified power-pack semantics cannot be transferred through this feature.
- No price, resale value, droid build, fuel quantity, or automatic ownership is inferred.

## Verification

- Focused command: `node --test --test-reporter=tap tests/eote-personal-catalog-r37.test.mjs tests/swe02-krayt-fang-pp06-11-family.test.mjs tests/uae-p238-swe02-krayt-fang-ship-source.test.mjs tests/ui-bridge-r22.test.mjs`
- Result: 32 tests, 32 pass, 0 fail, 0 skipped. Raw TAP: `focused.tap`.
- Official UI bridge bundle rebuilt successfully: 135 modules.
- Changed module syntax checks and `git diff --check` pass.

## Remaining gates

This proves a focused catalog-transfer path only. Printed p.11 remains pending for full inventory/source reconciliation, unresolved cargo categories, GM rest qualification, and page-wide UI/persistence coverage. Full SWE02 audit and global certification gates remain open.
