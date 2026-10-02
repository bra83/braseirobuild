# BRASEIRO D&D — QA-001 — RUN164 INTEGRATED BREAK TEST

AGENTE: QA  
BLOCO: QA-001  
BASE_BRANCH: `dnd/master-run164-integration-candidate`  
BASE_COMMIT: `b8e59f6c0a8d35bbc93865830359afecd0642d10`  
CHECKPOINT DE REFERÊNCIA: RUN163 — `a8ba3d0d18ce5e9488d02a051366c73c9955b85fceccf1301d61ddf29f9a1813`  
CHECKPOINT DE INTEGRAÇÃO: `BRASEIRO_DND_6.6.2_RUN164_INTEGRATION_CANDIDATE_OVERLAY.zip` — `bb62fbbf5a818fd48197b086141b8ee6dfe8f395e697c297dc6300cee27470ce`

## SUÍTES HERDADAS EXECUTADAS

13/13 PASS:
- RUN141 Compact
- RUN142 Reconciled
- RUN143 Forgotten Secrets
- RUN143 Arcana Unleashed
- RUN144 Arcana Systems
- RUN145 Arcana Bestiary
- RUN145 Arcana Items
- RUN146 Reconciliation
- RUN147 Evolving Items
- RUN148 Reconciliation
- RUN149 Forgotten Secrets Spells
- RUN162 Eberron Mega
- RUN163 Heroes of Faerûn Mega

## TESTES NOVOS EXECUTADOS

GAMEPLAY-001: 3/3 PASS.
Total integrado repetido: 16/16 PASS.
TTS RUN110: 8/8 PASS.
Harnesses adversariais adicionais foram executados para atomicidade RUN149/RUN143, subclasses Heroes, persistência/world-state e contratos dos nove hooks GAMEPLAY-001.

## BLOCKERS

4.

### B-001 — Composição Android estruturalmente incompleta
A árvore reconstruída exatamente segundo o release do MASTER referencia 25 classes `*Engine` do próprio projeto que não estão presentes no source-set Android auditado e não possuem JAR/AAR fornecedor localizado. Exemplos observados incluem `HomebrewClassRuntimeEngine`, `HomebrewSubclassRuntimeEngine`, `Core2024EberronCharacterEngine`, `ConditionRulesEngine`, `CrossBookSubclassRuntimeEngine`, `DndProgressionEngine`, `InventoryLedgerEngine` e `XanatharRacialFeatRuntimeEngine`.

O gate compile-only/resource/manifest foi repetido sem assemble/bundle:
`./gradlew --no-daemon :app:compileDebugJavaWithJavac :app:processDebugResources :app:processDebugMainManifest`

Resultado: bloqueado antes da execução das tarefas por `java.net.UnknownHostException: services.gradle.org`. O ambiente não possuía `ANDROID_HOME`, `ANDROID_SDK_ROOT` nem cache do wrapper. Não foi inventado PASS.

### B-002 — Auto-roll proibido de iniciativa
Usos históricos de `Random` foram classificados caso a caso. Rolagens vinculadas a controles explicitamente acionados pelo usuário não foram tratadas como defeito. Porém foram identificados caminhos de iniciativa automática em `MainActivity`, incluindo `addActiveHeroesToCombat`, sincronização de NPC hostil e `applyCombatPlan`. Isso viola `auto_rolls=false` e o contrato `PEDIR INPUT → RECEBER → VALIDAR → RESOLVER`.

### B-003 — Atomicidade quebrada em RUN149/RUN143
Harness RUN149 com slot 2 inicial = 8:
- payload incompleto: slot 2 caiu para 7;
- cancelamento: permaneceu 7;
- campo obrigatório ausente: permaneceu 7;
- resolução válida posterior: permaneceu 7;
- nova submissão completa: caiu para 6.

Harness RUN143:
- Grand Finale sem targets: recurso 1→0;
- Cold-Blooded sem confirmação obrigatória: recurso 1→0.

O recurso é debitado antes de toda entrada manual necessária ser validada.

### B-004 — Mystara permanece selecionável/executável
Mystara não está apenas preservado como código histórico. A UI/roteamento auditados permitem selecionar, persistir e entrar no fluxo Mystara. O gate exige Mystara fora do fluxo atual, sem exigir deleção do código histórico.

## MAJORS

1.

### M-001 — Fallback manual genérico aceita ação inexistente em Heroes
A ação deliberadamente inexistente `acao_inventada_qa` foi enviada a sistemas Heroes conhecidos. Subclasse, feat e item Aurora puderam retornar estado equivalente a `resolved=true` via fallback `*_feature_manual`. Casos reproduzidos incluíram Banneret, Harper Agent e Bright Fungal Cloak. A cadeia UI → gateway → state → persistence pode, portanto, mascarar ausência de mecânica.

## MINORS

1.

### N-001 — Superfície PT-BR incompleta
Foram encontrados termos/labels mecânicos user-facing em inglês, incluindo `Student Dice`, `Beloved Inspiration` e `Student Die`, além de labels canônicos exibidos diretamente. IDs internos canônicos não foram tratados como problema.

## SOURCE_BLOCKED

5 grupos explicitamente preservados:
1. Eberron Bestiary: 20/27 cartões numéricos bloqueados por fonte.
2. Heroes of Faerûn: auditoria fina/source-fidelity permanece PARTIAL.
3. Runeterra: Pistoleiro/Armeiro bloqueados por fonte aprovada.
4. Grung bloqueado por fonte.
5. Xamã/Ritualista bloqueado por texto mecânico autoritativo.

Não houve promoção indevida para COMPLETE. `BOOK_COMPLETE=false` permanece obrigatório.

## AUTO_ROLL AUDIT

FAIL. Os nove hooks GAMEPLAY-001 preservam input manual nos cenários testados, mas a camada Android contém iniciativa automática proibida.

## ATOMICITY AUDIT

FAIL. Os testes gerais de transação existentes passaram, porém RUN149 e RUN143 apresentaram débito prematuro reproduzível.

## PERSISTENCE AUDIT

PARCIAL / NÃO ELEGÍVEL A PASS GLOBAL. GAMEPLAY executou save/read-back e os testes de persistence/world-state disponíveis passaram; porém não foi possível completar ciclo real de fechamento/reabertura em Android e M-001 permite persistência de resolução desconhecida.

## UI/ENTRYPOINT AUDIT

FAIL. Existe cadeia real UI → handler → gateway → engine → state → persistence para sistemas amostrados, mas M-001 permite ação inventada atravessar a cadeia, B-004 mantém entrypoint Mystara e B-001 impede comprovação do app completo compilado.

## CHARACTER CREATION AUDIT

PARCIAL / NÃO COMPROVADO END-TO-END. Catálogo/wiring e representantes dos livros exigidos estão presentes, mas o fluxo LISTAR → SELECIONAR → CONFIRMAR → SALVAR → REABRIR → FICHA não pôde ser comprovado em runtime Android no ambiente disponível.

## GAMEPLAY AUDIT

PASS para GAMEPLAY-001; FAIL global do VTT integrado pelos blockers acima. Os nove hooks do `MasterOrchestrator` foram localizados e testados quanto a perguntas não mutarem estado, ações aguardarem input manual, viagem não cobrar/mover prematuramente e gateways de facção/renome/framework não persistirem mutação enquanto pendentes.

## GEMINI AUDIT

PASS para GAMEPLAY-001. Propostas Gemini de dano/PV, renown, item, condição e localização foram bloqueadas sem gateway mecânico. Não foi detectada autoridade mecânica direta no novo sandbox.

## PT-BR AUDIT

FAIL por N-001.

## TTS/KOKORO AUDIT

PASS estático/regressivo, com limitação de hardware. RUN110: 8/8 PASS; enumeração/integração Sherpa/Kokoro e `Braseiro Kokoro PT-BR · neural` estão presentes; chunks usam `QUEUE_ADD` após o primeiro; `onDone()` avança a fila; não foram encontrados acessos a `Android/data` nem referências a `model.onnx`/`voices.bin` nos sources/resources auditados. Enumeração real Samsung/Google/Kokoro não foi possível sem dispositivo.

## ANDROID STRUCTURAL AUDIT

FAIL por B-001. Projeto, wrapper, Manifest, resources e package `com.braseiro.dnd5e` estão presentes. Compile Java, resource processing e manifest merge não receberam PASS devido ao bloqueio de rede/SDK e à ausência estática de engines referenciados. Nenhum assemble/bundle foi invocado; nenhum APK/AAB foi produzido.

## REGRESSÕES

13/13 suítes herdadas: PASS. 3/3 GAMEPLAY-001: PASS. TTS RUN110: 8/8 PASS. Não foi observada regressão TTS/Kokoro nos testes disponíveis. Os findings acima são defeitos não cobertos pelas suítes verdes e foram registrados como evidência, não corrigidos.

## RESULTADO FINAL: FAIL

## JUSTIFICATIVA

QA-001 exige zero BLOCKER e zero MAJOR aberto. A integração apresenta 4 BLOCKERs, 1 MAJOR e 1 MINOR reproduzíveis/demonstráveis. Há violações diretas de `auto_rolls=false`, atomicidade de recursos e exclusão de Mystara, além de composição Android estruturalmente incompleta. Por isso o candidato RUN164 não atende ao critério de PASS, mesmo com 16/16 suítes prescritas verdes.

`BOOK_COMPLETE=false` está correto e deve permanecer assim.

## RECOMENDAÇÃO PARA O MASTER

Não promover RUN164 candidate. Tratar os findings fora do QA e publicar uma nova base auditável; repetir QA-001 integralmente, incluindo compile-only/resource/manifest em ambiente com rede/cache/Android SDK, ainda sem `assemble`/`bundle` salvo ordem explícita. Manter Eberron 20/27, Heroes source-fidelity, Pistoleiro/Armeiro, Grung e Xamã/Ritualista como `PARTIAL/BLOCKED_SOURCE` e manter `BOOK_COMPLETE=false`.
