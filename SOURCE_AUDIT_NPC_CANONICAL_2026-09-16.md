# WWN source audit — canonical NPC registry

A global state-path audit found a real split-brain defect introduced by the living-world layer: combat, spells, projections and starter content use `npcState`, while the newer world-life service used `worldState.npcs`.

Fixed:
- `npcState` is the canonical NPC registry again.
- NPC world upsert/move/memory/remove now mutate the canonical registry.
- living-world selection reads canonical NPCs.
- Gestão reads canonical NPCs.
- campaign integrity validates canonical NPCs.
- safe maintenance migrates legacy `worldState.npcs` records into `npcState` and removes the obsolete registry.
- regression coverage updated and expanded.

This prevents NPCs created/moved by world-life tools from becoming invisible to combat, spells, player projection, narrative and other canonical systems.
