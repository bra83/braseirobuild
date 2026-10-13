# Kingmaker R36 — Remaining Verification & Homologation Scope

## 1. Completed & Verified in Source, Assets, JVM, SQLite & Gradle (`compileDebugJavaWithJavac`)
All code, asset, database, and rules-engine items across the 4 implementation fronts have been implemented and verified:

1. **Front D — Kingdom Rules, 73 Structures, New Leadership (p. 520) & Warfare**:
   - Full 73-structure catalog (`kingmaker_official_structures_p544_552.json`) with 0 numerical/skill/DC mismatches.
   - Special structure semantics: Paved Streets (first per turn in settlement costs 0 RP / 0 Lumber), Museum (Magic Item Vault +1 item bonus to Lore/Research & Fame/Infamy display), Menagerie (exhibit creatures with Ruin reduction, Fame/Infamy, and +1 Unrest per sapient creature at start of turn), and PC item bonuses from settlement structures (`pcStructureItemBonus` & `settlementPcItemBonuses`).
   - Official **New Leadership** activity (`KingmakerLeadershipSemantics` + `KingmakerRepository.performNewLeadership`): 4 degrees of success, Invested Roles (up to 4 roles, +1 status bonus to skills linked to invested roles), automatic companion investment (`automaticInvestment`), role assignment/reassignment, vacancy penalties, and VIP once-per-turn tracking.
   - Atomic SQLite transactions for Consumption (`patch_consumption_atomic.py`), Kingdom Turn phases (`patch_turn_phase_atomic.py`), and Warfare encounters (`patch_war_atomic.py`, 9/9 warfare audit checks passed).

2. **Front C — Companion Guide (12 Companions, 7 Personal Adventures, 5 Secondary Profiles)**:
   - All 7 published personal adventures (`q_cg_amiri`, `q_cg_ekundayo`, `q_cg_jubilost`, `q_cg_linzi`, `q_cg_nok_nok`, `q_cg_tristian`, `q_cg_valerie`) populated with multi-stage objectives, canonical scenes (`A1`, `B1–B3`, `C1–C4`, `D1–D7`, `E1–E4`), creature stat blocks, hazards, skill checks/DCs, and branching outcomes.
   - Executable JVM and SQLite repository methods (`resolveCompanionAdventureCheck` and `resolveCompanionAdventureBranch` in `KingmakerCompanionSemantics` and `KingmakerRepository`) handling Feast Points (Ekundayo), Inconsequent Debates (Jubilost), Research Points & Curse Countdown (Tristian), Honor Duel & permanent departure if Sir Fredero is denied mercy (Valerie), Nok-Nok alignment shifts, and structure gifts (`alchemist`, `orphanage`, `shrine`).
   - All 5 secondary companions (`Harrim`, `Jaethal`, `Kalikke and Kanerah`, `Octavia`, `Regongar`) linked to canonical campaign quests (`no_literal_quest_links = 0`) with influence, automatic leadership investment, twin persona switching, and camping activities.

3. **Fronts A & B — Chapters 1 to 11 (40 Official Parts, Candlemere Chapter 9, Lantern King Chapter 11 Gate)**:
   - All 40 official parts across Chapters 1–11 mapped and audited (`missing_named_anchors = 0`, `review_needed = 0`).
   - Chapter 9 (*They Lurk Below* — Candlemere, pp. 394–423) populated with 5 quests, 2 POIs (`poi_candlemere_contested_level`, `poi_candlemere_spiral_yog_sothoth`), 2 tactical scenes (`A1–A17`, `B1–B14`), 3 complex hazards (`hazard_candlemere_praise`, `hazard_candlemere_spiral_seal`, `hazard_candlemere_orb_blast`), 13 NPCs/creatures (`npc_foras`, `npc_korog`, `npc_kyetriel`, `npc_niodrhast`, `npc_leng_envoy`, `npc_candlemere_shoggoth`, etc.), and the Helix Theorem 9-room sequence (`B14 -> B13 -> B12 -> B10 -> B8 -> B4 -> B3 -> B2 -> B1`, Occultism DC 36) granting a +4 status bonus to saves against teleportation/banishment in Chapter 11.

---

## 2. Remaining Homologation Items (Not Fabricated Locally)

In strict compliance with mission constraints against fabricating runtime device evidence:

1. **Android Emulator / Physical Device E2E & 3-Restart `AppDb` Endurance (`android_ui_endurance.py`)**:
   - Must complete on the self-hosted `BRELGA` runner (`pf2-r36-brelga-independent.yml`) or a physical Android device with `adb` and an active API 30+ AVD/device.
2. **Full Interactive Campaign Playthrough**:
   - Automated JVM/SQLite harnesses (404 assertions in `R36BookSemanticsHarness`, 434 passed pytest suites, 20-cycle SQLite persistence fixture across 3,640 objectives) verify deterministic rules, persistence, and state transitions, but do not substitute a human or live-device UI playthrough from Chapter 1 through Chapter 11.
