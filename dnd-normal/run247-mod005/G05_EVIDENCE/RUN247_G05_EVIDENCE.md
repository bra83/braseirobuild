# G05 — Homologação da Economia de Ações Lendárias e Bestiários RHW e XMM (RUN247-MOD005)

**Base**: RUN247-MOD005 Homologado — 11/10/2026.
**Ambiente**: BRELGA (OpenJDK 17, Android SDK API 35, Gradle 8.11.1).
**Status G05**: **HOMOLOGADO** (177/177 testes automatizados aprovados, 0 falhas, 0 erros).

---

## 1. Implementações Concluídas e Resolução dos Defeitos P0

1. **Despacho Multi-modal de Ações Lendárias (`StatblockCombatFlowEngine.legendaryAction`)**:
   - Resolução determinística e tipada para todas as modalidades do D&D 5e:
     - **Jogada de Ataque** (quando `to_hit` presente): cálculo contra CA do alvo, crítico em d20=20, dano composto via `applyAttackDamage`.
     - **Teste de Resistência (Saving Throw)** (quando `save_dc` e `save_ability` presentes): avaliação contra CD do monstro, cálculo de salvaguarda com bônus real do alvo (`save_bonus` extraído de `saving_throws` ou modificador de atributo), cálculo de dano escalonado (sucesso vs falha), aplicação de condições em `StatblockEncounterStateEngine.condition()` e registro de `effect_on_fail` (push, pull, teleporte).
     - **Feitiços / Magias** (quando `spells` presentes): resolução com envelope tipado `spellcasting_action`, validação prévia de suporte (rejeita magias desconhecidas com `unsupported_legendary_spell` antes do débito de pontos) e concentração durável vinculada.
     - **Ações Especiais / Manobras** (quando `category=legendary_action` sem ataque direto): resolução como `special_legendary_action`.

2. **Resolução de Defeitos P0 (Execução Real sem Stubs)**:
   - **P0.1 (Movimento Forçado e Teleporte com Mutação de Coordenadas Observáveis)**:
     - Rastreamento explícito de coordenadas $x, y, z$ em `StatblockEncounterStateEngine` (`setPosition`, `getPosition`, `spawn(token, id, x, y, z)`).
     - `applySpecialEffect` calcula vetores direcionais discretos de Chebyshev no grid 5ft.
     - Validação de limites $[0, 99]$ e colisão: impede sobreposição do atacante em `pull`, detecta bordas com `destination_blocked_or_boundary` e grava recibos causais com `origin_x, origin_y -> dest_x, dest_y`.
   - **P0.2 (Magias Lendárias e Bônus Real de Salvaguarda)**:
     - Rejeição estrita de magias desconhecidas/não implementadas com bloqueio prévio sem débito de pontos lendários (`unsupported_legendary_spell`).
     - Cálculo de bônus de salvaguarda do alvo (`calculateTargetSaveBonus`) utilizando `saving_throws` compilado e modificadores de atributos $\lfloor (\text{score}-10)/2 \rfloor$.
     - Aplicação de concentração durável na ficha do conjurador (`concentration: <spell>`).
   - **P0.3 (Persistência Durável SQLite no Fluxo Lendário)**:
     - `AppDb.recordStatblockLegendaryTransaction`: transação atômica em SQLite (`combatAtomic`) gravando recibos imutáveis na tabela `effect_receipts` (`legendary-action-v1:...`).
     - Sincronização automática de redução de PV e adição de condições na tabela `combatants`.
     - `MasterOrchestrator.resolveStatblockLegendaryAction` conectado diretamente à persistência durável.

3. **Atomicidade Transacional e Rollback Estrito**:
   - Captura de snapshots atômicos (`StatblockEncounterStateEngine.snapshot()` e `StatblockTurnEngine.snapshot()`) antes de qualquer mutação em memória.
   - Pré-validação estrita: se atacante ou alvo forem inválidos/derrotados, se o combate não tiver iniciado, se a ação for invocada durante o próprio turno do monstro lendário (`legendary_action_forbidden_on_own_turn`), ou se o orçamento lendário estiver esgotado (`legendary_points_exhausted`), a operação é bloqueada sem consumir pontos e sem tocar nos pontos de vida.
   - Débito transacional dos pontos lendários via `spendLegendary(attacker, cost)` executado apenas após a confirmação da legalidade da ação.
   - Em caso de exceção durante a mutação, restauração garantida de ambos os estados através do bloco `catch`.

4. **Integração Total dos Catálogos RHW e XMM (`StatblockCombatRuntimeEngine`)**:
   - O resolvedor em tempo de execução busca de forma unificada tanto em `actions` (ex: catálogo Ravenloft RHW, 50 ações lendárias baseadas em saves como `Hand of Fate` e `Sands of Time` de Ankhtepot) quanto em `special_actions` (ex: catálogo Monster Manual XMM 2025, 107 ações lendárias como `Scorching Sands` e `Blazing Light` dos dragões).
   - Suporte transparente em `attack`, `savingThrowAction`, `rangeProfile`, `attackRiderSavingThrow` e `rechargeCheck`.

---

## 2. Evidências de Execução no BRELGA

- **Compilação e Bytecode**:
  - `javac` de produção e testes: 0 erros, 0 warnings não tratados.
  - APK debug montado com sucesso via `:app:assembleDebug`.
  - Bytecode DEX e SQLite auditados: confirmação de métodos lendários, mutação de coordenadas e transações duráveis em `effect_receipts`.
- **Suíte de Testes Automatizados**:
  - `:app:testDebugUnitTest`: 177 testes executados com 100% de aprovação (0 falhas, 0 erros, 0 pulados).
  - Nova suíte `Run247P0RegressionRobolectricTest`:
    - `testP01_ForcedMovementAndTeleportCoordinatesMutation`: mutação real de coordenadas no grid, deslocamentos de 10ft/15ft, proteção de limites de tabuleiro e recibos causais com `origin_x, origin_y -> dest_x, dest_y`.
    - `testP02_LegendarySpellsPreflightRejectionAndRealSaveBonus`: bloqueio prévio de magia não suportada sem débito de pontos lendários, aplicação do bônus de salvaguarda de CON (+5) em Shatter convertendo d20=10 em sucesso (15 >= 14), e concentração durável em Invisibility.
    - `testP03_MasterOrchestratorDurablePersistence`: transação durável via MasterOrchestrator persistindo em `effect_receipts` e debitando HP do combatente em SQLite.
  - Cobertura E2E direta em `E2ETier1FeatureCoverageRobolectricTest` e `E2EGatesIntegrationRobolectricTest`: 16/16 testes de gates aprovados.
  - Ausência de `assertTrue(true)` ou stubs vazios em toda a suíte de testes.
