# BRASEIRO WWN — MINIMAL SHARED CHROME V3

STATUS: ACTIVE — SUPERSEDES V2

## Shared visual elements only
Gameplay screens do not inherit the body/layout of SCR-021. The globally shared visual components are only:

1. the approved five-slot bottom navigation family;
2. compact red/blue character status bars when mechanically applicable.

## Bottom navigation
At 415×915 the approved family keeps the fixed order:
`JORNADA / PERSONAGEM / MOCHILA / MAPA / DIÁRIO`.
The active destination may change, but the order/family does not.

## Red bar — HP
Canonical binding: `current HP / maximum HP` for the character represented by the bar.
The fill is remaining HP: `current / max`.

## Blue bar — System Strain ACCUMULATED
Canonical binding: `current System Strain / maximum System Strain` for the character represented by the bar.
The fill is ACCUMULATED strain: `current / max`.
A healthy character normally begins at 0, therefore the blue fill begins empty and grows as System Strain accumulates.
It is not mana, not Effort, and not “remaining strain capacity”.

Example: `System Strain 0/11 => 0% blue fill`; `3/13 => 23.08% blue fill`.

## Per-character scope
HP and System Strain are individual character state, never a single party resource. On an Atlas group popup, each hero gets their own bars. On a character-focused surface, the bars refer to that character.

## Visibility
Bars appear only when an actual character with canonical HP/System Strain evidence is bound and the information is useful for the surface.

## Mechanical firewall
UI projects canonical state only. It cannot fabricate values or independently resolve mechanics.
