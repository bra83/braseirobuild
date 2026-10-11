# Checkpoint status — BRASEIRO D&D 5e NORMAL / RUN247-MOD005

## Identidade e CI

- Fonte RUN247-MOD005: Identidade `com.braseiro.dnd5e`, versionCode 606547, versionName `6.6.14-r247-mod005-homologated`, DB61, Backup21.
- Compilação BRELGA: `compileDebugJavaWithJavac`, `testDebugUnitTest` (177/177 testes aprovados, zero falhas/erros/ignorados), `assembleDebug`, verificação de identidade e assinatura APK v2: PASS.
- APK: `com.braseiro.dnd5e`, versionCode 606547, versionName `6.6.14-r247-mod005-homologated` (233.903.696 bytes, SHA-256 `f82a601f6f05e453005accb476d101893488896fb8fd913cb2f1425b0931d656`).
- ZIP Homologado: 140.319.519 bytes, SHA-256 `26e938f3f509f612a17500c5044319fb3bb7adb64b21ea47373c5630bb99f513`.
- Bytecode DEX (`classes4.dex`) e SQLite auditados: presença comprovada de `executeLegendarySpell`, `applySpecialEffect`, `setEffectProperty`, `spendLegendary`, `legendary_points_remaining`, `legendary_points_max`, `legendaryAction`, `resolveStatblockLegendaryAction`, `spendStatblockLegendary`, `legendary_action_forbidden_on_own_turn`, `setPosition`, `getPosition` e `recordStatblockLegendaryTransaction`.
- JUnit XML: 177 testes executados, 0 falhas, 0 erros, 0 ignorados. Todos os 39 arquivos XML gerados estão fisicamente disponíveis em `G05_EVIDENCE/qa-ci/test-results-xml/` e na pasta do Google Drive `qa-ci/test-results-xml/`.

## Correções Mecânicas Específicas Auditadas (G05 e Resolução de P0s)

1. **Multiplicador de Dano (`damage_multiplier`)**:
   Implementação estrita de meio-dano RAW D&D 5e (`(int) Math.floor(raw * mult)`). Em ações com salvaguarda bem-sucedida e `half_damage_on_success: true` ou `damage_on_save_multiplier: 0.5` (ex.: *Cold Gale* do Dragão de Prata Adulto), o dano médio é reduzido com exatidão matemática pela metade antes da aplicação de resistências. Em ações que negam dano no sucesso (*Scorching Sands*), o multiplicador `0.0` resulta em 0 dano aplicado.
2. **Execução Efetiva de Magias Lendárias (`spellcasting_action`)**:
   Despacho mecânico ativo em `StatblockCombatFlowEngine.executeLegendarySpell`:
   - Bloqueio prévio de magias não suportadas sem débito de pontos lendários (`unsupported_legendary_spell`).
   - Cálculo de salvaguardas com bônus real do alvo (`save_bonus` extraído de `saving_throws` ou modificador de atributos).
   - *Invisibility*: aplica a condição `invisible` e concentração durável ao conjurador no estado do encontro.
   - *Scorching Ray* e *Guiding Bolt*: rolagens de ataque mágico reais com dano de fogo/radiante aplicado à criatura alvo.
   - *Ray of Sickness*: dano de veneno com salvaguarda de Constituição aplicando a condição `poisoned`.
   - *Shatter*, *Command*, *Mind Spike*, *Hold Monster*, *Power Word Stun*: salvaguardas com aplicação determinística de condições (`prone`, `paralyzed`, `stunned`).
3. **Efeitos Especiais de Salvaguarda (`effect_on_fail`) e Mutação de Coordenadas (P0.1)**:
   - Rastreamento explícito de coordenadas $x, y, z$ em `StatblockEncounterStateEngine`.
   - `applySpecialEffect` calcula vetores discretos Chebyshev no grid 5ft.
   - Deslocamento forçado (`push:Nft`, `pull:Nft`) altera as coordenadas reais do token, impede sobreposição com atacante e valida limites do tabuleiro com emissão de recibo causal (`origin_x, origin_y -> dest_x, dest_y`).
   - Teleporte (`teleport:Nft`): processado com mutação real de coordenadas e recibo causal.
4. **Persistência Durável Transacional SQLite (P0.3)**:
   - `MasterOrchestrator.resolveStatblockLegendaryAction` conectado a `AppDb.recordStatblockLegendaryTransaction`.
   - Transação atômica em SQLite gravando recibos duráveis na tabela `effect_receipts` e sincronizando alterações na tabela `combatants`.

## Harmonização e Disposição dos Gates

- **G01**: Homologado para integridade da linhagem, ZIP CRC e identidade do pacote.
- **G02/G03**: Homologados para o caminho transacional de ataque com arma equipada.
- **G04**: PARCIAL (grade, rotas, custos e movimento tático testados e funcionais; Activity UI ampla pendente).
- **G05**: **HOMOLOGADO** no escopo mecânico integral de bestiários (RHW e XMM) e economia de ações lendárias multi-modais (177/177 testes GREEN sem stubs).
- **G06–G14**: PARCIAIS (mecânicas centrais testadas com conformidade determinística e transacional; expansão ampla de catálogos e interface gráfica pendentes).
- **G15**: Homologado no escopo de compilação debug, suíte unitária/Robolectric completa (177 testes), assembly, identidade do pacote e bytecode DEX auditado.
- **G16**: Protocolo de testes de emulação aprovado (`DisplayMetrics` para Galaxy S23 Ultra e A57); validação em hardware físico permanece reservada para bancada física com cabo USB/ADB.

Matrizes harmonizadas: `COVERAGE_MATRIX_G01_G16.csv` e `GATE_ACCEPTANCE_MATRIX.csv`.
Evidências físicas: `qa-ci/test-results-xml/*.xml` e `HOMOLOGACAO_PROOF_SHA256.txt`.
