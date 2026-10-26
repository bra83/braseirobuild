# DrOgres/tftloop comparative implementation audit — BRASEIRO 1.3.3

Date: 2026-09-09

## Scope and authority

Repository reviewed: `DrOgres/tftloop`, a Foundry VTT implementation of Tales from the Loop and Things from the Flood.

This repository is used only as a **secondary implementation/regression reference**. It does not override the project's canonical TFTL/TFTF source ledger or the Tales/Flood mechanical firewall. No repository code or assets were copied into BRASEIRO.

No explicit root `LICENSE` file was observed in the reviewed repository tree, so this pass deliberately reimplemented only independently supported behavior and regression tests.

## Useful implementation cross-checks

### Actor/profile split

The Foundry system declares both Kid and Teen actors. BRASEIRO already keeps Tales/Kids and Flood/Teens as mechanically separate cores and JSON formats.

### Type/key-skill cross-check

Seven Tales Type mappings exposed by the repository overlap the BRASEIRO creation catalog and match its three key skills:

- Computer Geek / Informático
- Hick / Caipira
- Jock / Esportista
- Popular Kid / Popular
- Rocker
- Troublemaker / Encrenqueiro
- Weirdo / Estranho

The repository's `config.js` lists Bookworm, but the reviewed `module/actor/types.json` does not provide its default entry. BRASEIRO therefore does **not** use the repository to fill or replace the Bookworm mapping.

All ten Flood/Teen Type key-skill sets exposed by the repository match BRASEIRO's isolated Flood catalog: Hacker, Jock, Lone Wolf, Motorhead, Party Animal, Raver, Rocker, Seeker, Snob and Street Kid.

## Historical bug classes turned into BRASEIRO regressions

The repository changelog documents fixes involving Conditions, negative pools plus bonus dice, XP, Iconic Items, relationship persistence and rerolls. BRASEIRO 1.3.3 now has an explicit secondary regression harness covering those classes.

### Push / reroll

The reviewed Foundry `chat.js` contains a TODO about preserving first-roll successes during Push. BRASEIRO's reducer already preserves sixes and recalculates accumulated successes; 1.3.3 adds an explicit regression for this and for using Luck after a pushed failure.

### Zero/negative pool firewall

The Foundry implementation floors a non-Broken pool at one die and its changelog attributes that behavior to a Flood clarification. The BRASEIRO canonical ledger explicitly blocks importing the Flood minimum-one-die rule into Tales/Kids (`TAL-EDGE-001`).

**1.3.3 correction:** a non-Broken Tales Kid whose final pool reaches zero or less can no longer silently roll zero dice or inherit Flood's one-die floor. The reducer returns an explicit `TAL-EDGE-001` error. Flood/Teens keeps its own minimum-one-die behavior.

### Persisted Kid validation

Kid JSON import now validates that:

- age remains 10..15 for an active Tales Kid;
- four canonical attributes remain 1..5;
- attribute total equals current age;
- all 12 canonical skills exist and remain 0..5 at runtime;
- Luck is within `0..(15-age)`;
- XP is within 0..10 rather than being silently clamped.

This preserves advancement while rejecting malformed/imported state that could bypass creation/aging invariants.

### Player-visible reducer failures

The Trouble screen now surfaces reducer errors in a visible warning panel. This is especially important for the `TAL-EDGE-001` gate instead of making a Roll button appear to do nothing.

## Test results

- Primary Tales engine contract suite: **113 PASS / 0 FAIL**
- Flood isolated core suite: **46 PASS / 0 FAIL**
- DrOgres comparative regression suite: **33 PASS / 0 FAIL**
- Random Kid validation inside comparative suite: **10,000/10,000 valid**
- Static source audit: PASS
- Product feature audit: PASS
- Profile firewall audit: PASS
- User visual locks: PASS

The DrOgres suite is intentionally secondary. A mismatch never automatically changes BRASEIRO rules; the canonical ledger/source wins.
