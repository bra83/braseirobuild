# BRASEIRO WWN — VISUAL SESSION 1.0.4 + RULES TRUTH

## Visual session
- Character visual profile persists per character: canonical description, must-keep traits, palette notes, portrait reference and portrait Gemini interaction id.
- Character portrait generation uses the player's exact freeform prompt; no hidden creative prompt is added.
- Scene generation uses a local-only Scene Visual Packet plus a Character Bible for the active party.
- Party count is explicit; the generator is forbidden to invent extra party members.
- Approved character portrait references are resent on each scene generation when available.
- Campaign scene image generation reuses Gemini `previous_interaction_id`, but canonical character/scene facts are resent every time so conversation memory never becomes authority.
- Scene prompt excludes generic broad world-context prose by design.
- Scene prompt forbids HUD, text, hex grid, undiscovered secrets and invented hidden threats.
- Image model is configurable; default is `gemini-3.1-flash-image`.

## Mechanical authority / RAG boundary
RAG is not a mechanical rules engine. The native capability remains `ragMechanicalAuthority=false`. Canonical mechanical state changes must go through deterministic/stateful WWN services or explicit adjudication contracts.

## Important completeness truth
Closure 071 terminally classified the finite content, but terminal classification is not synonymous with a deterministic resolver for every rule.
- Spells: 83/83 terminal contracts; 19 executable closed resolvers, 64 explicit adjudication/context contracts.
- Arts: 202/202 terminal contracts; 20 resolver/passive contracts, 182 explicit adjudication/context contracts.
- Faction special effects: 59/59 terminally classified.
- Equipment/travel/services/crafting received substantial executable coverage, but no claim is made here that every WWN SRD content row and every open-ended magic-item/Working/item-creation procedure is a fully automated deterministic subsystem.
- Where WWN itself requires GM judgment, the correct implementation is an explicit-context contract routed to the Master, not invented automation and not blind RAG authority.

## Gates on exact 1.0.4 tree
- Production JS syntax: 119/119 PASS.
- POST071_VISUAL_SESSION_CHECK PASS.
- POST071_CURRENT_CONTRACT PASS.
- inventory/status/POI contract PASS.
- flow integration PASS.
- playable finalization 6/6 PASS.
- gameflow closure 7/7 PASS.
- static guard PASS.
- release/security/UTF-8 guard PASS.
- SQLite contract PASS.
- Closure 065 native contract 35/35 PASS.
- Closure 069 audio/TTS contract 19/19 PASS.
- Stress: 33 sessions x 144 turns = 4,752 PASS, deterministic replay digest e44f81c9530e12bc80b9616b2f7181221008b26c04f448beadd0806e47f1aaca.

Android/Kotlin compilation and live Gemini image generation are certified separately by GitHub Actions/device execution.
