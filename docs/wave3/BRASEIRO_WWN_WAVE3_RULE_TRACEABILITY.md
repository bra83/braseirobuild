# BRASEIRO WWN — WAVE 3 CORE RULE TRACEABILITY

STATUS: IMPLEMENTED_SUBSET
SCOPE: first end-to-end `SCR-015 / observe` PLAYER_ACTION only.

## Mechanical authorities

- `SRC-WWN-SRD-1 §1.1.2 Attribute Modifiers`
  - 3 => -2; 4–7 => -1; 8–13 => 0; 14–17 => +1; 18 => +2.
- `SRC-WWN-SRD-1 §2.3.0 Skill Checks`
  - 2d6 + relevant skill level + relevant attribute modifier; total >= difficulty succeeds.
  - no level-0 in pertinent skill => -1 contribution when the check is attemptable.
- `SRC-WWN-SRD-1 §2.3.1 Skill Check Difficulties`
  - difficulty is GM/context selected; ordinary situational adjustment is bounded -2..+2.

The scene contract chooses **difficulty 8** for `observe` as `CAMPAIGN_SCENE_DESIGN`; this is not represented as a universal WWN difficulty for observation.

## Implemented path

`SCR-015 OBSERVAR -> Notice + Wisdom -> deterministic 2d6 -> result -> atomic mechanical commit -> deterministic Barbara test narrative append -> reopen continuity`.

## Explicit deferrals

`approach`, `ambush`, `retreat`, and `start_combat` are UI intents but return `DEFERRED_NO_MUTATION` in Wave 3. No placeholder mutation or invented mechanics are executed for them.

## Character fixture boundary

The Wave 3 character is only validated for the core-check subset: level 1, six legal 3..18 attributes, and level-0/1 skills. Full class/Foci/HP/equipment/magic legality remains Wave 4 and is not claimed here.
