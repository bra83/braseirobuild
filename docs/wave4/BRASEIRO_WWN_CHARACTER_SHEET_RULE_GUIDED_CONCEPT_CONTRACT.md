# BRASEIRO WWN — WAVE 4 — CHARACTER SHEET RULE-GUIDED CONCEPT CONTRACT

STATUS: CONCEPT_GATE_READY
VIEWPORT: 415×915 portrait
SYSTEM: Worlds Without Number
MECHANICAL_AUTHORITY: SRC-WWN-SRD-1
VISUAL_AUTHORITY: approved BRASEIRO WWN refined direction / Arqueologia do Crepúsculo / SCR-015 visual grammar

## Purpose
Define the first player-facing Character Sheet / Summary concept before implementing the Wave 4 visual layer. The concept may represent fixture values, but every represented mechanic must be legal under the active WWN authority. Runtime implementation must later bind these fields to canonical character state; the concept itself is never a runtime screenshot or whole-screen asset.

## Rule-guided content contract
- Six attributes: Strength, Dexterity, Constitution, Intelligence, Wisdom, Charisma; show score and modifier.
- Core status: HP, System Strain current/max, Armor Class, base Attack Bonus.
- Saving throws: Physical, Evasion, Mental, Luck.
- Skills shown with canonical WWN skill identifiers/levels.
- Class identity and level.
- Full Expert class abilities: Masterful Expertise and Quick Learner.
- Foci shown separately from skills/abilities.
- Readied/Stowed capacity summarized, with detailed inventory deferred to the equipment tab/surface.
- XP shown as current state only; no invented threshold on this summary concept.
- Effort is omitted/marked N/A for a Full Expert fixture because this fixture has no Effort pool.
- Magic panel is present only as a navigation/tab affordance; no spells/Arts invented for the Expert fixture.

## Legally coherent visual fixture
This is a deterministic visual fixture, not a committed campaign save.

- Name: IRIA
- Class: Full Expert
- Level: 1
- Attributes: STR 7 (-1), DEX 12 (+0), CON 11 (+0), INT 10 (+0), WIS 14 (+1), CHA 9 (+0)
- HP: 5 / 5 (legal example d6 result 5 + CON mod 0)
- Attack Bonus: +0
- AC: 10, unarmored, DEX mod 0
- System Strain: 0 / 11
- Saves: Physical 15; Evasion 15; Mental 14; Luck 15
- Background path used for fixture legality: Barbarian -> Survive-0; chosen Learning skills Sneak-0 and Notice-0.
- Expert Focus pick: Specialist (Survive) level 1 -> Survive becomes level 1; checks in that skill use the Focus die method.
- Any Focus pick: Alert level 1 -> Notice becomes level 1.
- Final free skill: Exert-0.
- Displayed skills: Survive-1, Notice-1, Sneak-0, Exert-0.
- Readied capacity: 3; Stowed capacity: 7. Concept may show a current usage below those caps.

## Information hierarchy at 415×915
1. Header + compact character identity.
2. Vital/mechanical status strip: HP / Strain / AC / Attack.
3. Six-attribute grid with modifier prominence.
4. Save row.
5. Tabs: RESUMO / PERÍCIAS / EQUIP. / MAGIA.
6. Summary content: key skills, class abilities, Foci, compact load state.
7. Persistent global bottom navigation using the corrected menu grammar; no change to the approved SCR-015 menu geometry is implied by this concept.

## Visual grammar
- Arqueologia do Crepúsculo: blackened green/charcoal, oxidized bronze/gold, restrained warm parchment only as inner information surface, subtle Legacy teal accents.
- Dense but trustworthy mechanical information; no generic D&D sheet, no cyberpunk console, no ornate Pathfinder chrome.
- Angular clipped frames and fine engraved linework consistent with the approved encounter screen.
- Serif display/body paired with compact mechanical numerals/labels.
- Touch targets readable at 415×915.
- No whole-screen baked text in implementation; mutable data must remain DOM/native/runtime state.

## Gate
The generated image is CONCEPT_ONLY. After user approval, implementation must be decomposed into assets/native regions and then captured for overlay/diff. No Visual Lock may be claimed from the generated concept itself.
