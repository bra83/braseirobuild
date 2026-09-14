# STAR WARS R38 — BLOCK 1

Preserve R46 layout and mechanics. No image generation.

## Proven gaps to correct
- ILUSTRAÇÃO: change behavior from internal generation to opening Gemini with the current scene prompt prepared for the user; keep import-back workflow manual.
- Scene frame: use existing scene/location imagery as contextual fallback before user imports a replacement.
- Android viewport: apply system-bar/safe-area insets so content does not render beneath phone navigation/status areas.
- Bottom navigation: restore existing icons without redesigning the menu.
- Continue regression audit after these fixes.

## Guardrails
- No R46 redesign.
- No mechanics changes.
- No inferred visual/mechanical bindings.
- Reuse only existing visual assets with evidence in the project.
- Run focused R38 tests after each patch block.
