# WWN source runtime authority audit — 2026-09-16

Production screens no longer bootstrap from Wave-era character fixtures.

- Added `data/wwn.starter-character.v1.json` as the production starter character seed.
- Jornada, Diário, Mochila, Ficha and SCR-022 now use the production seed instead of Wave3/4/6 fixtures.
- Wave fixture files remain only for QA/recovery/reference paths and legacy SCR-015/SCR-016 visual references.
- Removed stale Wave 1 wording from Android shrinker configuration.
- Added `qa/production-runtime-authority-regression.mjs` to prevent production screens from regressing to fixture-backed runtime state.

This change does not alter WWN mechanics; it removes a legacy data-authority dependency from the live product path.
