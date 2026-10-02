# BRASEIRO D&D — QA-001 FINDINGS

**AGENTE:** QA  
**BLOCO:** QA-001  
**BASE_BRANCH:** `dnd/master-run164-integration-candidate`  
**BASE_COMMIT:** `b8e59f6c0a8d35bbc93865830359afecd0642d10`  
**RESULTADO FINAL:** **FAIL**

Este arquivo registra somente findings/evidências. Nenhuma correção ou feature foi aplicada.

## BLOCKERS — 4

### B-001 — Chassis Android integrado estruturalmente incompleto
A reconstrução exata do release referencia 25 classes `*Engine` do próprio projeto ausentes do source-set auditado e sem JAR/AAR fornecedor localizado. O gate Gradle compile-only/resource/manifest foi repetido, mas foi bloqueado antes da execução por `UnknownHostException: services.gradle.org`; `ANDROID_HOME`, `ANDROID_SDK_ROOT` e cache do wrapper estavam ausentes. Não foi declarado PASS.

### B-002 — Auto-roll proibido de iniciativa
Usos de `Random` foram separados entre rolagens explicitamente solicitadas pelo usuário e rolagens automáticas. Foram encontrados caminhos de iniciativa automática em `MainActivity`, incluindo `addActiveHeroesToCombat`, sincronização de NPC hostil e `applyCombatPlan`. Viola `auto_rolls=false`.

### B-003 — Atomicidade quebrada RUN149/RUN143
RUN149, slot2 inicial 8: payload incompleto reduziu para 7; cancelamento e erro mantiveram o débito; nova submissão válida consumiu outro slot. RUN143: Grand Finale sem targets e Cold-Blooded sem confirmação obrigatória reduziram recurso 1→0. Há débito antes da validação completa.

### B-004 — Mystara ativo
A UI/roteamento ainda permitem selecionar, persistir e entrar em Mystara. O gate exigia Mystara fora do fluxo corrente; código histórico poderia permanecer.

## MAJORS — 1

### M-001 — Fallback de ação inexistente em Heroes
`acao_inventada_qa` pôde retornar resolução manual genérica equivalente a `resolved=true` em sistemas Heroes conhecidos, incluindo Banneret, Harper Agent e Bright Fungal Cloak. O retorno pode seguir para persistência, mascarando ausência de mecânica.

## MINORS — 1

### N-001 — PT-BR incompleto
Termos/labels user-facing em inglês encontrados: `Student Dice`, `Beloved Inspiration`, `Student Die` e labels canônicos expostos em pontos da UI.

## SOURCE_BLOCKED / PARTIAL — 5 grupos
- Eberron Bestiary: 20/27 cartões numéricos.
- Heroes of Faerûn: auditoria fina/source-fidelity PARTIAL.
- Runeterra: Pistoleiro/Armeiro bloqueados por fonte.
- Grung bloqueado.
- Xamã/Ritualista bloqueado.

Não houve promoção indevida a COMPLETE. `BOOK_COMPLETE=false` permanece obrigatório.

## Evidência positiva preservada
- 13/13 suítes herdadas: PASS.
- 3/3 GAMEPLAY-001: PASS.
- total integrado: 16/16 PASS.
- TTS RUN110: 8/8 PASS.
- nenhum APK/AAB gerado.
- Gemini GAMEPLAY-001 sem autoridade mecânica direta nos testes executados.
