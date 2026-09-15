# BRASEIRO WWN — POST071 FLOW INTEGRATION HOTFIX

Date: 2026-09-13
Authority: cumulative source after Closure 071 + audio + safe-area + state parity/POI hotfixes.

## Fixed
- Jornada exposes AJUSTES -> configuracoes.html.
- Configuracoes exposes Gemini API key/model, test, audio controls, campaign backup JSON and character JSON import/export.
- Jornada exposes MESTRE NARRAR AGORA for the current scene.
- Runtime Barbara uses Gemini when configured; without credentials, scene-open fallback is player-visible scene content instead of the old technical mock event string.
- Character drawer now uses the real CharacterRosterService on Android.
- Character drawer exposes:
  - CRIAR GUIADO (name + goal + legal WWN class path; remaining legal creation choices are deterministic/randomized by the canonical WWN creator),
  - GERAR ALEATORIO,
  - EXPORTAR JSON,
  - IMPORTAR JSON,
  - CONFIGURACOES / GEMINI.
- Character import/export remains portable WWN JSON and rejects credential-bearing content.
- Index shell now contains campaign listing/new-adventure controls expected by app.js.

## Preserved
- Closure 071 evidence and mechanics.
- Audio startup hotfix.
- Android safe area / width hotfix.
- Inventory/ficha state parity.
- Dynamic HP/System Strain bars.
- Structural POI rendering.

## Local gates
- JS syntax: 116/116 PASS.
- STATIC_GUARD PASS.
- RELEASE_SECURITY_GUARD PASS.
- POST071_CURRENT_CONTRACT PASS.
- POST071_INVENTORY_STATUS_POI_HOTFIX PASS.
- POST071_FLOW_INTEGRATION_CHECK PASS.
- Stress: 33 sessions x 144 turns = 4752 turns PASS, deterministic replay PASS.
