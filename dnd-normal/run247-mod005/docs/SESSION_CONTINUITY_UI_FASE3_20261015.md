# Continuidade pela UI — fase 3 — 15/10/2026

Lidos status/diff acumulados e relatórios das fases 1 e 2 neste worktree. Preservadas suas mudanças e suítes.

Implementação adicional em MainActivity:

- Cura, dano da ficha, gasto de slot e recurso estruturado usam validação positiva estrita, mantendo o formulário aberto em erro e impedindo confirmação repetida. Tokens locais não utilizados foram removidos; o dispatcher compilado continua sendo a autoridade de gasto e recibos.
- Botões antigos de pacto e subida de nível rejeitam chamadas repetidas, inclusive quando ainda referenciados após reconstrução da tela. Salvamento da configuração da campanha D&D recebe guarda semelhante.
- Seleção de classe na subida de nível mantém o diálogo aberto enquanto nenhuma classe foi escolhida e impede aplicação repetida após confirmação.
- Descanso compara o grupo persistido com o grupo da prévia antes do commit. Mudanças em PV, ficha ou participantes exigem nova prévia, sem avançar tempo. Preservados os checks de combate e o WorldStateEngine existente.
- Callback de Dado de Vida não aplica duas vezes e rejeita ficha alterada durante a rolagem. A atomicidade do gasto permanece responsabilidade de AppDb.
- Importação validada e restauração de snapshot limpam cache, rascunho do narrador, referências de importação/exportação de personagem, alvo de token e referência visual da cena antes de recarregar a história no D&D normal. Os fluxos Mystara/Strixhaven mantêm seu comportamento de recarga anterior.

## Regressões escritas

SessionContinuityUiRobolectricTest: nove testes com SQLiteMode.NATIVE, arquivo de banco real, controles de produção e fechamento/reabertura de AppDb. Cobrem cura inválida/positiva/repetida, cancelamento de dano, XP inválido/positivo/repetido, cancelamento de descanso, alteração da ficha durante confirmação, descanso curto persistido, descanso longo com cura, subida de dois níveis mediante dois comandos distintos e round-trip do contrato de backup corrente com personagem/XP/recursos. Sem mocks de regras.

A fixture omite onCreate e injeta serviços reais por reflexão, como as suítes anteriores. Reabertura de AppDb comprova o contrato pretendido de disco quando executada; não representa teste de morte do processo Android. O round-trip usa exportAll/importAll reais, não o seletor de documentos Android. Não há teste novo integral de restauração pela UI SAF, inventário/localização/combate no mesmo backup, nem homologação de todos os recursos de classe.

## Verificação executada

- Wrapper: tentativa real de testDebugUnitTest com as três suítes novas das fases 1–3. Falhou no download Gradle 8.11.1: SocketException Operation not permitted.
- Gradle instalado 9.7.1, offline, home gravável: falhou antes da configuração em FileLockContentionHandler / Could not determine a usable wildcard IP.
- Nenhum teste Robolectric executou, nenhum XML novo, compilação Android não comprovada.
- Parser JDK: dois arquivos, zero erros sintáticos. Não verifica resolução de símbolos Android.
- git diff --check passou.
- Logs: docs/evidence/session-phase3-20261015/.

Reexecução necessária em ambiente habilitado:

```sh
bash gradlew :app:testDebugUnitTest \
 --tests com.braseiro.dnd5e.SessionContinuityUiRobolectricTest \
 --tests com.braseiro.dnd5e.CharacterCreationUiRobolectricTest \
 --tests com.braseiro.dnd5e.ExplorationCombatUiRobolectricTest \
 --tests com.braseiro.dnd5e.AppDbBackupRobolectricTest \
 -Pbraseiro.qaLiteAssets=true --no-daemon
```

## Dependências e limites

AppDb.java intocado (PR #35). applyCharacterShortRestHitDice não oferece token de operação na API atual; esta fase protege callback e ficha obsoleta na UI, mas não altera sua atomicidade/recibo em disco. XP, nível, save e importação continuam usando implementações correntes de AppDb; reexecutar regressões após PR #35. A dependência de velocidade de spawn descrita na fase 2 continua aberta. Executor de sopros/PR #33 intocado.

Não há homologação integral de continuidade: faltam execução Robolectric, morte/reabertura do processo e restauração SAF completa. DB61, Backup21, package, assinatura, motores e fontes não foram alterados. Sem suplementos/aventuras novos, alteração de Mystara ou outros VTTs, autoridade mecânica Gemini/RAG, commits, push, merge ou APK.
