# SWE02 Old Lylek Den pp.15–18 scoped implementation handoff

Status: `IMPLEMENTADO_NAO_CERTIFICADO` for the bounded items listed below. This is not certification of pp.15–18 or SWE02 Act 1.

## Source authority

- Official SWE02 PDF: *The Long Arm of the Hutt*, printed pp.15–18 (audit recheck identifies physical PDF pp.16–18 and SHA-256 `18FB9880242743D11765EACA442168FC3441A3B0D17AA92BC769057734B45489`). Source ledger: [`audits/SWE02_ACT1_DEEP_AUDIT.md`](../../audits/SWE02_ACT1_DEEP_AUDIT.md).
- T’rex carryover facts: SWE02 p.15; *Edge of the Empire Adventure Book* p.24 for profile. The verified profile is an existing hunter slot and is never an extra adversary. Unknown/single-PC composition when living T’rex would conflict with the mandatory Gand remains pending.
- Optional lylek stats: *Edge of the Empire Beginner Game*, SWE01, printed p.47. This exact profile was visually verified. No Core Rulebook profile substitution is made.
- Submission has no printed numeric threshold. The controller now requires an explicit GM confirmation when the chosen combat outcome is PC incapacitation and records `numericSubmissionThreshold: null`.

## Implemented and exercised

- Roster v15 migration promotes the legacy active sheet into a stable PC ID and keeps the active-sheet mirror synchronized. Only explicitly registered roster PCs can be selected as onboard crew; group companions are excluded.
- Fast route checks apply 1 strain to each onboard PC for each failed Pilot check. Crash checks resolve per onboard PC ID and apply the 6-damage failure effect to that character's sheet. Party receipts and state are persisted.
- Explicit ruse record stores the fake Imperial hail, absence of Imperial uniforms, whether PCs ask for identification, T’rex recognition if present, Oskara’s conditional recognition, presumed ruse failure and initiative continuation. No Bluff test is introduced.
- Route and tactical-choice receipts include the map labels, eastern flank/frontal approach, rock-cover reminder, frontal retreat/fight-to-last choice and GM lylek branch choice. Fast-route hunter initiative carries one setback.
- T’rex history selects escaped / defeated with death unconfirmed (10 wounds) / confirmed dead (absent); replaces one hunter slot, with source profile and reopen tests.
- Speeder search grants exactly two stimpaks, one medpac and one emergency repair kit to party inventory, with duplicate prevention.
- B’ura warning and optional verified SWE01 p.47 Captive Rancor path persist. GM submission confirmation is recorded without inventing a threshold.
- `reach-new-meen` records approximately one mile, B’ura’s aid explanation, cautious welcome/hero recognition, larger dwelling invitation and basic rations offered; no ration count is invented.

## Still PENDENTE (do not infer completion)

- p.16 map is a static labeled text rendering plus a receipt, not an interactive tactical map or runtime position model. Cover is recorded/reminded, but the +2 ranged setback is not enforced against selected targets.
- p.17 hunters’ capture preference, stun while PCs resist, switch to earnest/lethal combat after a hunter falls, actual eastern-flank mechanics, and source-directed frontal fighting retreat are not enforced by combat execution. Their GM choice receipts do not substitute for mechanics.
- Repair time records approximately 30 minutes, but Advantage does not yet reduce that time. Per-PC crash/test model exists, but no additional roster-driven combat attack selection was added.
- Lylek Advantage currently records the trigger/victim receipt at hunter combat closure; it is not yet delivered as an immediate combat event that removes/kills one hunter, returns prey to the nest and alters active adversary state.
- The p.18 arrival receipt is persisted, but not surfaced as a New Meen arrival UI interaction. Conditional developer details have only an `ONLY_IF_ASKED` marker; exact details and asked/not-asked interaction remain pending pending a source-text-backed implementation.
- Other Old Lylek scene details not specifically listed above remain pending under the deep source audit. No page or adventure certification is claimed.

## Verification

- Focused command: `node --test tests/swe02-old-lylek-encounter.test.mjs tests/swe02-trex-carryover.test.mjs` — 11/11 pass. Log: [`focused-tests.log`](focused-tests.log).
- Affected SWE02 sweep command covers Old Lylek/T’rex and p.235, p.238–244 page tests — 112/112 pass. Log: [`affected-swe02-sweep.log`](affected-swe02-sweep.log).
- Offline bridge build: `node tools/build-ui-bridge-bundle.mjs` — `ok: true`, 133 modules. Log: [`offline-bundle-build.log`](offline-bundle-build.log).
- Tests demonstrate state persistence through three reopenings for specified roster, loot, lylek, T’rex and arrival paths. This does not extend coverage to pending items.
