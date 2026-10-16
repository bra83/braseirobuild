# Braseiro Star Wars VTT — R54 P155 | 12/10/2026

## Checkpoint

- Baseline **P154**, ZIP `BRASEIRO_STAR_WARS_EDGE_R54_P154_SOURCE_CHECKPOINT.zip`, SHA-256 `09461347fa825216516ce74ba3bd134d93c46791a3cfdadd0602427b60c5fd9e` (verificado antes da extração).
- Entrega: `BRASEIRO_STAR_WARS_EDGE_R54_P155_SOURCE_CHECKPOINT.zip` — **projeto Android Studio completo**, não APK.
- `app/build.gradle.kts`: `versionCode=10155`, `versionName=1.0.155`. Shell offline `1.0.155`, bundle gerado a partir das fontes `.mjs` com **79 módulos**.
- Nenhuma fonte Java/TTS alterada. Oito SHA-256 comparados com `evidence/P151/JAVA_P150_BASELINE_SHA256.json`.

## Implementação real integrada

### 1. Além da Orla — combate jogável, com autoridade provisória

- Três encontros com `beginPublishedAdventureEncounter`, iniciativa, adversários reais, ações de combate, encerramento, histórico, flags e recuperação:
  - `btr-yiyar-hangar`: interceptação nos hangares da Roda; perfil funcional Yiyar, 2 oponentes.
  - `btr-yiyar-jungle`: confronto com rivais em Cholganna; 3 oponentes.
  - `btr-imperial-interception`: intervenção imperial em Raxus Prime; 3 stormtroopers.
- O registro expõe `capabilities.encounters=true` apenas agora que há especificações consumidas pelo controlador. Encontros são restritos à cena, não podem ser repetidos após resolução e não permitem mudar de cena enquanto estão ativos.
- Resultados de vitória, retirada, incapacitação e encerramento neutro produzem consequências distintas e persistentes. Recuperar IT-3PO, rivalidade Yiyar e exposição imperial não são inferidos em encerramentos neutros.
- Adicionado `yiyar_salvager` ao catálogo de adversários publicados, com `authority=BRASEIRO_PROVISIONAL_COMBAT_PROFILE`.
- **Limitação importante:** esses são perfis de implementação jogável; **não** foram cotejados com os blocos estatísticos do PDF oficial de *Beyond the Rim*. A certificação editorial de combate continua pendente.

### 2. Age of Rebellion — Cinto de acessórios (Utility Belt)

- Duas posições da árvore AoR agora possuem consumidor mecânico: `CINTURÓN DE ACCESORIOS` → `UTILITYBELT`.
- Ação `GameController.useUtilityBelt({itemId,actionId})`: gasta **um Ponto de Destino luminoso**, vira-o para sombrio e adiciona um item pequeno não registrado, com raridade até 4, a partir do catálogo permitido.
- Armas são permitidas apenas quando têm `Munição limitada 1`, respeitando tamanho/raridade; armas normais, itens grandes, raros ou restritos são recusados. Sem custo de créditos, sem alterar catálogos globais.
- Recibos persistentes por `actionId`: repetição não duplica item nem gasta Destino; IDs conflitantes e histórico adulterado são rejeitados. Falha de gravação reverte inventário e Destino juntos.
- Android/WebView: bridge `utilityBeltStatus/useUtilityBelt` e seletor/botão na tela **Equipamento ativo**, em PT-BR, condicionado à posse do talento e a Destino luminoso.
- Cobertura AoR mecânica: **254/360** (antes 252), **106 posições pendentes**. Fonte editorial completa ainda não certificada; a implementação segue a descrição do talento e o catálogo existente.

### 3. Universal Adventure Engine / cinco aventuras

- Preservadas as correções P146–P154 de gatilhos, consequências, objetivos, rollback, recibos idempotentes e journal causal.
- As cinco aventuras passam por início, escolha, validação estrutural/semântica e três reaberturas cada, sem gravação duplicada no replay.
- *Além da Orla* agora participa do motor de combate publicado sem contaminar as quatro aventuras previamente integradas.

## Gates efetivamente executados

- Node regular: **1953/1953 PASS** em 270 arquivos — `evidence/P155/NODE_REGULAR.txt`.
- Stress: **6/6 PASS** — `evidence/P155/NODE_STRESS.txt`.
- Focais P155 + regressão P154: **57/57 PASS** — `evidence/P155/P155_FOCAL_TESTS.txt`.
- Kokoro/TTS: **27/27 PASS** de código — `evidence/P155/KOKORO_TTS_REGRESSION.txt`.
- Sintaxe Java: **8/8 PASS** — `evidence/P155/JAVA_SYNTAX.txt` (símbolos Android não resolvidos sem SDK).
- Auditoria integrada: `allPassed=true`, 5 aventuras, 3 encontros, 8 fontes Java preservadas, AoR 254/360 — `evidence/P155/RELEASE_AUDIT.json`.
- Bundle offline: **79 módulos** — `evidence/P155/BUNDLE_BUILD.txt`.

## Bloqueios e pendências (não confundir com testes aprovados)

- **106 posições AoR** ainda sem consumidor mecânico; expansões EotE/AoR não cotejadas integralmente com os PDFs.
- Cinco aventuras ainda não comparadas integralmente cena a cena com os livros; três encontros de *Além da Orla* são funcionais **mas provisórios** até a conferência de estatísticas e estrutura oficial.
- Gradle `:app:assembleDebug --offline` falhou **antes de compilar**: wrapper tenta baixar distribuição indisponível, sem cache/rede local. Log `evidence/P155/ANDROID_GRADLE_ATTEMPT.txt`.
- APK não produzido; sem instalação e três reaberturas físicas Galaxy S23 Ultra/A57; sem reprodução real Kokoro.

## Reproduzir

```bash
node tools/build-ui-bridge-bundle.mjs
node tools/audit-p155-release.mjs
node --test tests/uae-p155-beyond-rim-combat-integration.test.mjs tests/uae-p155-utility-belt-destiny.test.mjs
python tools/run-node-suite.py --regular
python tools/run-node-suite.py --stress
node --test tests/*tts*.test.mjs tests/*kokoro*.test.mjs
node tools/java-syntax-gate.mjs
```

Próxima prioridade: comparar os três encontros com o PDF oficial de *Além da Orla*, completar as 106 posições AoR e as expansões EotE/AoR, compilar e testar APK em runner Android com SDK/Gradle disponível. P155 é recuperável, mas não é homologação final.
