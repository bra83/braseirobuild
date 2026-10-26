# DUAL PROFILE BOUNDARY — Tales/Kids × Flood/Teens

Status: **MECHANICAL FIREWALL ESTABLISHED — ANDROID FLOOD WIRING IN PROGRESS**

Authority: `TFTL-TFTF-GRCS-003` candidate catalogue + `CANONICAL_RULE_LEDGER.csv`.

## Non-negotiable profile isolation

- `tales_kid`: Tales/Kids only. Luck + Pride belong here. No Shame, Scars or Friction.
- `flood_teen`: Flood/Teens only. Shame + Scars + Friction belong here. No Luck or Pride.
- Character JSON is profile-locked and cross-profile import is rejected.
- Conversion Kid → Teen must never silently copy incompatible resources.

## Android implementation state

### Tales/Kids
- Existing `TalesCore.kt` remains unchanged mechanically.
- Existing 113/113 engine regression remains PASS.

### Flood/Teens
- `FloodCore.kt` added as a separate pure-Kotlin mechanical module.
- 12 canonical Flood Skills and 10 Flood Types are represented.
- Creation: age 14–19, Attributes sum 14 (1..5), Skills sum 10 with Type key-skill limits.
- Resolution: minimum 1 die if not Broken; Broken auto-fails; Shame; Scars; Push; Help; Items.
- Scar progression / death-roll state is deterministic and accepts verified dice values rather than allowing the GM to invent a roll.
- Flood Extended Trouble uses multipliers 3× / 4× / 5×.
- Exact-half Extended Trouble remains explicitly unresolved for GM/source handling rather than fabricating a threshold.
- Recovery paths and XP/skill spending are represented.
- `FloodTeenJsonCodec.kt` added and hard-locks `profile=flood_teen`.

## Automated gates

- `tools/FloodCoreHarness.kt` → 46 PASS / 0 FAIL.
- `tools/profile_firewall_audit.py` → PASS.
- `tools/SceneIllustrationSelectorHarness.kt` → 48/48 PASS (24 contexts × both profiles, 24 unique assets per profile).
- Existing Android static audit → PASS.
- Existing product-feature audit → PASS.
- User visual-lock audit → PASS.
- Existing Tales engine → 113 PASS / 0 FAIL.

## Remaining dual-profile work

The Android Compose UI and Motor Barbara transport are still routed through the Tales/Kids `GameViewModel`. Flood mechanics are therefore **not yet exposed as a playable Android profile**. The next implementation block must add a profile router / Flood runtime binding while preserving every approved visual lock.
