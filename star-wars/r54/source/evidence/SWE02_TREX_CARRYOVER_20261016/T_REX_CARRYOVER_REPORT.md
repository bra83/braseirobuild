# SWE02 T’rex Carryover — Scoped Implementation Report

## Scope and source authority

This block implements only T’rex’s prior-adventure carryover into the SWE02 Old Lylek Den encounter.

- **SWE02 printed p.15:** one hunter per PC; one is a Gand; T’rex may occupy another existing hunter slot. He is present if he escaped the previous adventure, absent if his death was confirmed, and present with 10 wounds if he was defeated without confirmation of death.
- **Edge of the Empire Adventure Book printed p.24:** source profile shown on `work/star-wars/source/beg_29.png` (profile crop `work/star-wars/source/beg_29_trex_profile.png`). The exact profile values are recorded in the adversary definition and checked by the focused tests.

## Implemented behavior

The GM explicitly chooses one of three prior-history results before the Old Lylek route begins. The selection is persisted on the adventure state and validated against its canonical presence/wound result. The source encounter requires that this history be selected before combat can start.

| Prior result | T’rex in encounter | Starting wounds |
|---|---:|---:|
| Escaped without a fight | Yes, replaces one human hunter slot | 0 |
| Defeated, death not confirmed | Yes, replaces one human hunter slot | 10 |
| Killed with confirmation | No | — |

The Gand remains mandatory. For parties of two or more, T’rex replaces one human hunter, so the total remains exactly one hunter per PC. A living T’rex with a solo or unknown-size group stays pending: the source requires a Gand, and the recovered source does not resolve how to fit both into one slot. No extra adversary is added.

T’rex’s verified profile is Brawn 4, Agility 3, Intellect 2, Cunning 3, Willpower 3, Presence 2; Brawl 1, Cool 1, Discipline 1, Mechanics 3, Negotiation 1, Perception 2, Piloting (Planetary) 1, Ranged (Light) 2, Vigilance 1; Soak 5, Wound Threshold 16, Strain Threshold 11; heavy blaster pistol (Damage 7, Critical 3) and Trandoshan claws (Damage 5, Critical 3). The tested definition includes the source gear and cites Adventure Book p.24.

The existing generic encounter initialization path now applies an encounter entry’s `preWounds` when no world-NPC record exists. The focused regression asserts that other hunters keep 0 starting wounds.

## Verification

- `node --test tests/swe02-trex-carryover.test.mjs` — **5/5 passed**.
- Affected Old Lylek / SWE02 downstream sweep — **117/117 passed**. Exact command and TAP output are in `affected-tests.tap`.
- `node tools/build-ui-bridge-bundle.mjs` — **passed**, 133 modules; output in `bundle-build.log`.

Tests cover all three history outcomes; exact profile values and source identity; slot replacement and hunter count at party sizes 1, 2, 3, and 4; 10 wounds only for unconfirmed death; no pre-wounds on other hunters; persisted history and combat state across three reopenings; invalid/duplicate choices; and the pending solo/unknown-size case.

This report certifies neither the full Old Lylek sequence nor other SWE02 pages.
