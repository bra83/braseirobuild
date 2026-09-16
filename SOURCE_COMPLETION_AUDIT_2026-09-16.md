# WWN — Source Completion Audit — 2026-09-16

## Resultado deste passe
- Central operacional adicionada para regras que existiam no motor sem superfície operacional direta.
- Reação, Moral e Instinto ligados a dados físicos 3D e persistência canônica.
- Perseguições a pé e montadas ligadas ao RuleRuntimeService.
- Queda/dano ambiental ligado ao personagem e dados físicos 3D.
- Efeitos especiais de ativos de facção expostos operacionalmente (automáticos, Farmers, Manufactory, Ancient Workshop, Trade Company e Shapeshifters).
- Auditoria de campanha e reparo conservador persistente adicionados.
- Ajuda de Mestre somente leitura exposta sem mutação de estado.
- Acesso à nova central ligado às superfícies principais.

## Validação
- 141 módulos JavaScript passam `node --check`.
- Referências JS ao bridge Android: 44; referências sem implementação Kotlin: 0.
- Todos os IDs DOM usados pela nova central existem.
- Todas as suítes QA existentes passam.
- Nova suíte `operations-completion-regression.mjs` passa: reaction + morale + instinct + chase + falling + integrity + read-only GM help.
- 66 sessões simuladas acumuladas continuam PASS nas suítes de fechamento.

## Restante fora deste passe
- Gradle/build/install/device continuam deliberadamente fora do gate por instrução do projeto.
- Recursos que dependem de Android real (dados físicos, seletor de arquivos, TTS, Gemini, áudio) têm bridge implementado, mas a validação física depende do gate de dispositivo.
- Serviços runtime 071 mantidos como compatibilidade interna não foram promovidos a novas telas quando já existe fachada/motor canônico mais recente.


## 2026-09-16 — Faction full-cycle reachability correction

Global reachability audit found that `WorldService.startFactionTurn()` existed in the canonical engine but the GM UI could not invoke it. The same panel hard-coded `resolutions: []` for faction specials, making any turn with required asset specials impossible to complete from the product UI.

Corrected as one end-to-end cycle: initiative/tie-break → income → upkeep → special resolutions → canonical faction action → goal review → next faction. No automatic GM evidence is fabricated. Regression: `qa/faction-full-cycle-reachability-regression.mjs`.

## Global state integrity pass
- Added validation for campaign memory facts/threads/reveals and scheduled world events.
- Detects duplicate scheduled event IDs and dangling memory references.
- Conservative repair removes dangling thread/reveal references and duplicate scheduled events without inventing facts.
- Added `qa/global-state-integrity-regression.mjs` and re-ran the full QA suite.
