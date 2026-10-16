# SWE02 Old Lylek Den pp.15–18 scoped implementation handoff

Status: `IMPLEMENTADO_NAO_CERTIFICADO` for the listed source-specific paths. This is not certification of pp.15–18, New Meen, or SWE02 Act 1.

## Authority and scope

- Official source: *The Long Arm of the Hutt*, printed pp.15–18; Old Lylek source recheck in [`audits/SWE02_ACT1_DEEP_AUDIT.md`](../../audits/SWE02_ACT1_DEEP_AUDIT.md). The downloaded official PDF was the source for all sequence details.
- Printed pp.15–17 cover route, crew, lookout, ruse, crash, cover, hunter tactics, flank, retreat and repair. Printed p.18 covers the optional lylek event, warning/pursuit, loot and New Meen arrival. Developer details are on pp.18–19.
- T’rex carryover: SWE02 p.15, profile from *Edge of the Empire Adventure Book* p.24. Optional Captive Rancor profile: *Edge of the Empire Beginner Game* p.47, visually verified. No Core Rulebook replacement profile is used.
- All unspecified choices and mechanics remain explicit GM/player inputs; no Bluff test or numeric lylek-submission threshold is invented.

## Implemented and tested in this block

- p.17 cover: PC attacks against a selected target behind a rock pile add exactly two Setback dice for ranged weapons; hunters also receive two Setback dice for ranged attacks against a PC who selected rock cover. Both choices/effects are recorded in combat state.
- p.17 hunter intent: hunters prefer capture; when armed PCs resist, a hunter with the printed stun setting uses strain damage. Once any hunter is down, later hunter attacks use the weapon's lethal wound damage. Intent receipts are persisted.
- pp.16–17 approach: the eastern-cave flank starts combat at the source-described partially blocked east entrance. Frontal choice records the GM's fighting-retreat or fight-to-last outcome. A fighting retreat resolves active hunters along the dotted map route and records an escaped encounter.
- p.17 repair Advantage: about 30 minutes remains the default receipt. Advantage opens an explicit GM-selected reduction; because the text gives no amount, it stays `PENDING_GM_CHOICE` until the GM supplies a duration. The chosen reduction persists without implying a printed duration.
- p.18 lylek event: with the GM's `disturb_on_advantage` branch selected, a player check with Advantage immediately incapacitates/kills one active hunter, records the hunter as lylek prey returned to the nest, and persists a one-time receipt. Passive branch does not trigger it.
- pp.18–19 New Meen: arrival UI presents the mile of travel, cautious welcome/hero recognition, B’ura's aid, invitation to the larger dwelling and basic rations offered; no ration quantity is invented. Developer facts appear only after the PCs ask and persist with pp.18–19 citations.
- Earlier scoped pp.15–18 paths remain represented: explicit crew roster and stable PC IDs; fast-route Pilot failure strain to each onboard PC; per-PC crash checks and damage; fake-Imperial hail/recognition with no Bluff roll; T’rex history and slot replacement; optional verified lylek/Rancor branch; transactional loot grant; B’ura warning and GM-confirmed submission. See test names in the focused log.

## Remaining `PENDENTE`

- p.16 now has an interactive, persisted named-zone schematic for Heroes’ Journey, cave mouth, hunters’ speeder, eastern flank (p.17), and southern nest. It is explicitly not to scale and does not infer distances, range bands, cover geometry, or line of sight. The text gives no flank-specific modifier, so none is inferred.
- The source recheck does not specify a numerical reduction for repair Advantage. The controller leaves the duration at 30 minutes until the GM explicitly enters a reduction; this remains a runtime GM choice, not a printed value.
- A living T’rex competing with mandatory Gand for a one-hunter solo/unknown-size party remains unresolved by the source combination and is kept pending.
- The broader encounter and adventure remain uncertified. Other Old Lylek detail closure must follow the full source ledger and is outside this bounded proof claim.

## Verification

- Focused: `node --test --test-reporter=tap tests/swe02-old-lylek-encounter.test.mjs tests/swe02-trex-carryover.test.mjs` — 17/17 pass. Full output: [`focused-final.tap`](focused-final.tap).
- Affected sparse checkout sweep: `node --test --test-reporter=tap <files matching swe02|p235|p238|p239|p240|p241|p242|p243|p244>` — 187/187 pass, zero skips. Exact output: [`affected-sparse-final.tap`](affected-sparse-final.tap). Earlier full-project handoff sweep remains recorded separately as 117/117 in [`affected-swe02-sweep.log`](affected-swe02-sweep.log).
- Offline bridge rebuilt in the integrated sparse checkout: `node tools/build-ui-bridge-bundle.mjs` — `ok: true`, 133 modules. Output: [`bundle-final.log`](bundle-final.log).
- Tests cover the named source paths with saved state and three controller reopenings where state supports it. This does not establish complete page coverage or certify SWE02 Act 1.
