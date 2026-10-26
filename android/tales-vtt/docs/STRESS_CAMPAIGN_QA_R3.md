# BRASEIRO Tales VTT — Stress / Campaign QA R3

## Scope actually executed in this environment

This report covers source-level deterministic execution of the Tales/Kids engine, Motor Barbara firewall, public/private snapshot codecs, randomized reducer fuzzing, campaign progression and static/product audits.

It does **not** claim Android runtime, Gradle, device, Gemini network, Android SAF, PDFBox/OCR or Android TTS execution; those require an Android/Gradle environment.

## Baseline contract regression

- Engine contract checks: **113 PASS / 0 FAIL**.
- One new regression was added after stress testing found a snapshot defect involving an intentionally empty choice list.

## Campaign simulation batch A — broad randomized campaigns

Command shape: 150 campaigns × 10 sessions × 75 turns.

- Campaigns: **150**
- Sessions: **1,500**
- Turns: **112,500**
- Standard tests resolved: **36,000**
  - successes: 14,868
  - failures: 21,132
- Luck uses: **4,276**
- Pushes: **7,118**
- Pride uses: **1,403**
- Recoveries: **5,229**
- Extended Troubles: **4,500**
  - full success path: 1,500
  - compromise path: 1,500
  - complete failure path: 1,500
- Public/private snapshot round-trips: **24,000**
- Motor Barbara packets: **254,227**
- Private clues created: **10,500**
- Clues revealed: **6,000**
- Runtime invariant checks: **374,227**
- Invalid firewall rounds: **30,000**, all rejected as expected
- Corrupted snapshot fuzz rounds: **30,000**, no decoder exception
- Result: **STRESS_PASS**

## Campaign simulation batch B — long-run campaigns

Command shape: 20 campaigns × 33 sessions × 90 turns.

- Campaigns: **20**
- Sessions: **660**
- Turns: **59,400**
- Standard tests resolved: **19,140**
  - successes: 7,993
  - failures: 11,147
- Luck uses: **1,774**
- Pushes: **3,847**
- Pride uses: **638**
- Recoveries: **2,881**
- Extended Troubles: **1,980**
  - full success path: 660
  - compromise path: 660
  - complete failure path: 660
- Snapshot round-trips: **12,540**
- Motor Barbara packets: **133,439**
- Private clues created: **5,940**
- Clues revealed: **3,300**
- Runtime invariant checks: **196,139**
- Invalid firewall rounds: **30,000**, all rejected as expected
- Corrupted snapshot fuzz rounds: **30,000**, no decoder exception
- Result: **STRESS_PASS**

## Aggregate campaign exercise

Across both campaign batches:

- **170 campaigns**
- **2,160 sessions**
- **171,900 turns**
- **55,140 standard tests**
- **6,480 Extended Troubles**
- **36,540 snapshot round-trips**
- **387,666 Motor Barbara packets**
- **570,366 invariant checks**
- **60,000 invalid-firewall rounds**
- **60,000 corrupted-snapshot fuzz rounds**

## Reducer fuzz

Randomized event-stream fuzz of the pure Tales reducer:

- rounds: **250,000**
- valid events accepted: **140,782**
- invalid/out-of-context events rejected safely: **109,218**
- exceptions: **0**
- forced snapshot round-trips during fuzz: **250**
- result: **REDUCER_FUZZ_PASS**

The fuzzer intentionally mixes legal and illegal sequences such as invalid dice counts, invented Skills, Help at the wrong time, XP spending during a session, malformed reveal connections, duplicate Conditions and invalid item bonuses.

## Snapshot scale test

A large-state round-trip was executed with:

- 10,000 public clues
- 2,000 public locations
- 5,000 public connections
- 20,000 private GM clues
- 10,000 private GM connections
- 2,000 private GM locations
- 20k-character narrative
- UTF-8 / accented / non-Latin text

Result:

- public snapshot: 1,787,647 bytes; encode 61 ms; decode 185 ms
- GM-private snapshot: 4,531,618 bytes; encode 86 ms; decode 224 ms
- exact state equality after round-trip: **PASS**

## Defect found by stress testing and fixed

### SNAPSHOT-CHOICES-001

Stress fuzzing found that `GameSnapshotCodec.decode()` treated an intentionally empty `scene.choices` list as if the field were absent and restored the default two choices.

Impact before fix:
- a GM narration state with no suggested actions could be saved;
- loading that save would incorrectly resurrect the default "OBSERVAR ANTES / IR ATÉ O PORTÃO" choices.

Fix:
- distinguish **field absent** from **field present but empty** for `scene.narrative` and `scene.choices`;
- add permanent regression check: `snapshot preserves intentionally empty scene choices`.

Post-fix:
- engine contract suite: **113/113 PASS**
- reducer fuzz: **250,000 rounds PASS**
- campaign stress batches: **PASS**
- snapshot scale: **PASS**

## Static audit after the fix

- 13/13 expected screens present
- 90 drawables present / 0 missing drawable references
- 0 `???` secret placeholders
- 0 tactical radar
- 0 HP
- 0 initiative
- 0 encumbrance
- 0 demo triggers
- 0 placeholder screens
- 0 old-package hits
- static audit: **PASS**

## Product feature source audit

Source checks remain PASS for:

- secure API key store
- Gemini text model config
- Gemini voice/TTS config
- Android TTS fallback
- portable campaign backup
- Kid JSON import/export
- scene prompt export
- scene-image share/import
- imported scene-image persistence
- local PDF indexer
- RAG search UI
- RAG context into Motor Barbara
- Menu data tools
- Android share receiver manifest
- no D&D package/rules-engine transplant

These are **source/static PASS**, not Android runtime proof.

## Current verdict

**PURE ENGINE / FIREWALL / SAVE STATE: STRESS PASS**

**ANDROID RUNTIME / GEMINI NETWORK / SAF / PDF OCR / DEVICE TTS: NOT EXECUTED IN THIS ENVIRONMENT**
