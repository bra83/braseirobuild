# HANDOFF CANÔNICO — BRASEIRO STAR WARS VTT R54 P189

**Base:** P188 integral, SHA256 `bdafd40f5ca232f81382c00525fa77ba1888c7dbd1d7b12cd21e50cc604ffcee`, 97.833.506 bytes, 5.327 arquivos. **Destino:** P189 integral em `/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P189_SOURCE_CHECKPOINT.zip` e SHA externo. Não regredir ao P188.

## Implementação real P189

1. **Tempo de fabricação do computer spike:** `jewelBankPreparations.mjs` integra as 10 horas da aventura SWE09 ao relógio persistente `world.clock.elapsedMinutes` e ao registro canônico `world.taskTimeApplications` via `recordResolvedTaskTime`. Cada fabricação gera `task_jewel_spike_<actionId>`, 600 minutos, inclusive quando a rolagem de fabricação falha; o custo existente de 300 créditos permanece atômico. Recibo `BANK_PREP` contém `timeCheckId` auditável e é validado contra o ledger. Sem dupla cobrança em reenvios.
2. **Retentativas do slicing G-100:** `jewelBankingOperations.mjs` passa a contabilizar 30 minutos reais de tempo de tarefa em cada nova tentativa após a primeira, com `task_jewel_retry_<actionId>`, validação cruzada do recibo `BANK_STEP` e ledger. Primeira tentativa não cobra esses 30 minutos; as três retentativas máximas mantêm os limites anteriores. Falhas continuam consumindo tempo.
3. **Integridade, rollback e compatibilidade:** recibos de P189 são validados no status da aventura com contexto do mundo. Alterações em `timeCheckId`, ledger ou `elapsedMinutes` são rejeitadas; reaberturas persistem tempo, custo e recibos. Saves P188 sem os novos vínculos continuam válidos sem cobranças retroativas. `world.clock.tick` representa turnos abstratos e **não** foi alterado artificialmente; `elapsedMinutes` é o relógio canônico de tempo de tarefas, não o simulador de dias.
4. **Android:** `versionCode=10189`, `versionName=1.0.189`, shell e diagnóstico sincronizados, bundle offline reconstruído com 89 módulos. **8/8 arquivos Java**, incluindo Kokoro/TTS, idênticos byte a byte ao P188.

## Evidência de testes

- `tests/uae-p189-spike-time-ledger-world-clock.test.mjs`: **12/12** testes novos (custo e tempo em sucesso/falha, adulteração, dupla cobrança, três reaberturas, legado P188, retentativas de 30 minutos).
- `evidence/P189/TEST_SUMMARY.json`: **2.788/2.788** testes regulares em **308** arquivos; **6/6** stress; total **2.794/2.794** em **314** arquivos. Cada arquivo foi executado individualmente; **não** declarar suíte monolítica verde.
- Sintaxe JS, JSON e XML verificada. Java 8/8 idênticos. ZIP integral, CRC, SHA256 e delta verificados na etapa de empacotamento.

## Limites e prioridades

- O **restraining bolt** ainda é confirmado pelo Mestre e não foi associado a item possuído; não confundir `restraining_device` (controle de retenção do catálogo) com bolt sem cotejo oficial.
- `elapsedMinutes` agora avança com fabricação e retentativas; o relógio de turnos (`tick`) e o período do dia continuam independentes. Falta integrar avanço cronológico entre ambos sem inventar equivalência de turnos.
- Faltam contratação dos slicers NPC R3-8C/Naboka, Tabela 2-1, liquidação, provas, traição/fuga, rivais da corrida, SWE02 New Meen/Geonosis/Teemo.
- Cinco aventuras **não** certificadas cena a cena. AoR 360/360 consumidores declarados **não** é certificação semântica EotE/AoR/expansões.
- **Sem APK, Gradle/SDK/adb, CI BRELGA ou homologação física**. O conector GitHub acessível não oferece upload de release ZIP + workflow dispatch completo. Não declarar execução inexistente.

**Próximo P190+:** priorizar bolt com item real verificado no PDF/inventário, contratação NPC e Tabela 2-1, banco/liquidação, fuga, SWE02, cotejo editorial e semântico. Preservar Kokoro, executar regressão completa e stress, gerar ZIP Android Studio completo + SHA + delta + handoff, salvar na Biblioteca com readback. Nunca inventar regras, fontes, testes ou APK.
