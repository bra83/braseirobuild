# G05 — family implementation of size-bound, untimed Prone on hit

**Base**: RUN246 full-source ZIP (SHA256 `9b05165519289a76a20d86ad9da8cbb26e6bce91816c6c3ebb3868891e53c38b`). 2026-10-10. This is **not** G05 closure and **not** an Android-approved build.

## Implemented

- Twenty-six individual XMM monster actions have verified auxiliary source notes stating that a hit knocks a target of no more than an explicitly listed size Prone **without a save**. The previous `condition_on_fail` was incorrect; these entries now declare `condition_on_hit=prone`, `condition_ends_when=stands_up` and `condition_max_target_size=medium|large|huge`. Unsupported charge-only, conditional, mixed push+prone and form-specific actions are excluded.
- `MonsterOnHitProneRules` validates compiled source metadata and rejects unknown target sizes; known oversized targets take damage but receive no knockdown. No default Medium, saving throw or duration is invented. `MonsterActionEconomyGuard` authorizes only this explicit untimed condition type and continues to reject multiattack components that need separate rider commits.
- `AppDb` rechecks the source action and target size **before spending an action or applying damage**. Size must come from the saved character sheet, a valid statblock attribute, or an explicitly persisted combatant size. Unknown size refuses the whole attack; a known too-large target can be attacked and takes damage without Prone. Source-bound Prone is persisted through the existing transactional condition/receipt executor; standing up removes it through the existing guarded movement path.

## Executed locally

- 26 source rows reconciled and checked for absence of save, bogus duration or extra effect; 503 XMM statblock records parsed.
- `MonsterOnHitProneRules` compiled with Java and a minimal offline JSONObject contract stub; 9 behavioral assertions PASS. The stub test is **not** Robolectric/Android or integration coverage.
- Java AST parse: five changed/new Java source/test files, zero parser errors.
- Existing RUN244/RUN245 static tests: PASS (9+27 assertions).

## Not executed / limitations

- `Run247G05UntimedProneRobolectricTest` written but **NOT RUN**; Gradle/Android SDK not available here.
- End-to-end UI/SQLite/Backup21/Gradle APK testing **NOT RUN** for this change.
- Published book pages not inspected; 5e.tools-derived auxiliary source mechanics remain the source of these 26 rows. No official-source homologation is claimed.
- Most XMM statblocks omit size and some saved characters may lack an explicit size field, so an action targeting such a creature will be refused until an authoritative size is persisted. We intentionally did not invent creature sizes from portraits, names or hit dice.
- Legendary and reaction economy, grapple escape DCs, multi-effect action selection, and source-specific recharge/shape/charge triggers remain separate unresolved families.

## Gate

RUN247 advances 26 additional rows from `SOURCE_REVIEWED_ONLY` to `SOURCE_BOUND_IMPLEMENTED_UNTESTED`. Total issue rows: 280; 26 new, 15 prior RUN246, 4 prior source-bound entries, 235 rows not yet action-bound. These are **audit rows** and may refer to duplicate actions. No number above means G05 passed Android QA.
