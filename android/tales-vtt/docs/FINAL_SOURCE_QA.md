# BRASEIRO TALES VTT — FINAL SOURCE QA

Data: 2026-09-07
Baseline técnica/visual: `braseiro-tales-vtt(2).zip` (shell AI Studio) + biblioteca oficial de assets TFTL/TFTF + contratos Tales/Kids.
Visual baseline: **APROVADA** pelo usuário.

## Gates

- RULES_CONTRACT_GATE: **PASS**
- VISUAL_BASELINE_GATE: **PASS**
- SOURCE_SCREEN_GATE: **PASS — 13/13**
- ASSET_REFERENCE_GATE: **PASS — 0 faltando**
- PLAYER_GM_VISIBILITY_GATE: **PASS**
- MECHANICAL_REGRESSION_GATE: **PASS — 113/113**
- STATIC_SOURCE_GATE: **PASS**
- ANDROID_RUNTIME_GATE: **EXTERNO A ESTE AMBIENTE**

## 13 telas/estados

1. CENA
2. ENCRENCA
3. KID
4. GRUPO
5. MISTÉRIO
6. MAPA
7. MOCHILA
8. CONEXÕES
9. LOCAL
10. COTIDIANO
11. CLÍMAX / PROBLEMA ESTENDIDO
12. ENCERRAMENTO
13. MENU

## Fechamentos desta passagem

- Problema Estendido agora abre a cena do Kid ativo usando a própria ficha e liga o teste ao índice real da cena.
- O resultado final escolhido pelo jogador é contabilizado uma única vez no progresso coletivo.
- O cliente nunca inventa a rolagem de outro Kid; resultados externos só entram por evento validado.
- Compromisso exige exatamente o custo necessário, pode ser aceito ou recusado e não pode ser encerrado antes da decisão.
- Teste de Problema Estendido retorna ao CLÍMAX após `test_clear`.
- Encerramento de sessão não cura Condições automaticamente.
- Encerramento de Mistério cura Condições conforme o contrato.
- Sorte e Orgulho são restaurados no início da próxima sessão conforme o fluxo implementado.
- Aniversário é revisão explícita; +1 em um Atributo, máximo 5; idade sobe 1; Sorte máxima acompanha `15 - idade`.
- Aos 16 anos, o estado bloqueia continuar com o mesmo Kid e exige criação de novo Kid.
- Snapshot v7 preserva ligação do teste com Problema Estendido, modo de encerramento e estado de aniversário.
- Pacote/namespace deixou de usar identificadores `com.example`/AI Studio: `com.braseiro.talesvtt`.
- Removidos placeholders antigos, testes-example, Roborazzi do módulo, dependências de teste não usadas e signing config local inexistente.
- Backup Android foi desabilitado para não exportar saves/chave local por Auto Backup.

## Regressão

`docs/ENGINE_CONTRACT_CHECKS_LATEST.txt`

Resultado: **113 PASS / 0 FAIL**.

Cobertura inclui: pool, Condições, Quebrado, Sorte, Forçar, Orgulho, Ajuda, Item Icônico, visibilidade, save/load, Motor Barbara firewall, Problema Estendido, Compromisso, sessão vs Mistério, XP, aniversário, limite de Atributo, idade 16 e rotas de fluxo.

## Auditoria estática

`docs/STATIC_AUDIT_LATEST.json`

- 13/13 telas presentes
- 0 drawable referenciado ausente
- 0 `???` secreto
- 0 radar tático
- 0 HP
- 0 iniciativa
- 0 encumbrance
- 0 demo trigger
- 0 placeholder de tela
- 0 pacote antigo
- backup desabilitado
- estruturas mecânicas obrigatórias presentes

## Limite conhecido

Nenhuma afirmação de APK/captura Android foi feita nesta entrega porque o ambiente de produção não executa Android/Gradle. O source está preparado para a etapa de importação/execução em Android Studio.

## Revision 1.1 — product infrastructure transplant

Added from the BRASEIRO D&D Gold Standard architecture, adapted without importing D&D mechanics:

- secure Gemini API key store (Android Keystore/AES-GCM);
- separate text + voice/TTS model configuration;
- Gemini TTS with Android TTS fallback;
- portable JSON campaign backup/restore + optional automatic SAF backup;
- Kid JSON import/export;
- external scene prompt/share + same-scene image return flow;
- local PDF library, PDFBox extraction, OCR fallback and local RAG search;
- RAG evidence injected into Motor Barbara requests under Tales/Kids firewall.

Current regression: **113/113 PASS**. `STATIC_AUDIT_LATEST.json` and `PRODUCT_FEATURE_AUDIT.json` are PASS.
Android/Gradle runtime remains outside this environment and is not claimed.

## R3 stress addendum

Post-R2 campaign stress executed at source level:
- 170 simulated campaigns
- 2,160 sessions
- 171,900 turns
- 55,140 standard tests
- 6,480 Extended Troubles across full-success / compromise / failure paths
- 36,540 public/private snapshot round-trips
- 387,666 Motor Barbara packets
- 250,000 reducer-fuzz events
- 60,000 invalid-firewall rounds
- 60,000 corrupted-snapshot fuzz rounds
- large snapshot scale test to ~1.8 MB public / ~4.5 MB GM-private state

Stress found and fixed `SNAPSHOT-CHOICES-001`: empty scene choices no longer restore default choices after load. Contract suite is now **113/113 PASS**.

See `docs/STRESS_CAMPAIGN_QA_R3.md` for exact scope and limitations.


## Revision 1.2 — user-approved visual locks

- Footer Android now uses the same seven approved cyan line-icon shapes as the accepted runtime footer; legacy nav PNGs are no longer referenced by `BottomNavigation`.
- Internal routes keep a meaningful parent tab active instead of showing no active tab.
- MAPA remains frozen on `map_malaren_clean3` + 9 canonical POIs.
- Header weather remains unboxed.
- KID keeps direct portrait + JSON actions.
- `docs/USER_VISUAL_LOCK_AUDIT.json`: **PASS**.
- Dual-profile runtime QA: 24 screen/state captures (Tales/Kids + Flood/Teens), 415×915 shell, 0 page errors, 0 failed assets.

## Revision 1.3.3 — external implementation regression audit

Reviewed `DrOgres/tftloop` as a secondary Foundry implementation reference without treating it as rules authority and without copying repository code/assets.

Added `tools/DrOgresRegressionHarness.kt` and `docs/DROGRES_COMPARATIVE_AUDIT_1.3.3.md`.

Corrections derived from the comparison + canonical firewall:
- Tales zero/non-positive pool no longer silently rolls 0 dice and does not inherit Flood's one-die minimum; it stops with explicit `TAL-EDGE-001`.
- Flood retains its isolated minimum-one-die behavior.
- Push now has explicit regression proof for preserving first-roll sixes and accumulated successes.
- Luck-after-Push regression added.
- imported Kid JSON validates age/attribute coherence and rejects invalid XP instead of silently clamping it.
- Trouble screen visibly displays reducer failures.

Regression results: Tales **113/113**, Flood **46/46**, DrOgres comparative **33/33**.
