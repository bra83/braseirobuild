# WWN source closure — encounter reachability

This pass closes a source-level gameplay blocker found by global reachability audit: fresh/procedural campaigns could expose combat controls while having zero combat-capable NPCs in `npcState` (starter social NPCs intentionally have no fabricated combat statistics).

Implemented `EncounterBuilderService`, using the existing WWN foe catalog as mechanical authority. It materializes selected opposition with deterministic WWN HD hit points, combat statistics and attack contracts, persists them in canonical `npcState`, and exposes them immediately to the existing combat facade. The Journey combat pane can now choose a foe profile, choose 1–12 opponents, add them to the encounter, start combat, and clean defeated generated opposition after combat.

No starter social NPC is silently converted into a combatant and no private NPC fact is used as a combat statistic.

Validation: 143 JavaScript modules syntax-valid; 17 QA suites PASS; 66 accumulated simulated closure sessions PASS; encounter regression exposes 35 usable WWN foe profiles.
