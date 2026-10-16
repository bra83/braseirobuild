# HANDOFF CANÔNICO — BRASEIRO STAR WARS EDGE R54 P204

## Continuidade obrigatória

**Base P203 preservada**, SHA-256 `cfd56cd7dc516f8f8586e69c3ef6b3c19c58a75feafbd372a4aa78239d737f99`, 6.411 arquivos. Não recomeçar, não reescrever a arquitetura, não declarar APK ou homologação Android com testes Node.

## Implementação REAL P204 — impactos individuais em companheiros

SWE09 *The Jewel of Yavin*, Episode III (*Countdown to Disaster*), e EotE Core Tabela 6-9, página impressa 215. As regras de explosão e queda foram cotejadas nos checkpoints P201–P203; **P204 reutiliza exatamente o motor anterior, sem alegar nova leitura integral dos PDFs**.

Novo módulo `app/src/main/assets/vtt/js/adventure/jewelCompanionImpact.mjs` executa explosão e queda individualmente em membros **existentes** de `group.companions`, sem afetar quem está ausente. O Mestre deve selecionar o companheiro e confirmar expressamente que estava dentro do airspeeder na explosão ou na trajetória de queda; para queda, também confirma faixa Curta/Média/Longa/Extrema. `performJewelEscapeImpact` P203 é reutilizado em contexto isolado por alvo: 10 dano bruto de explosão, Absorção real, quedas com ferimentos/Tensão/críticos +50/+75 e Triunfo com autorização do Mestre. Nenhum alvo é inferido automaticamente.

Novo recibo `JEWEL_COMPANION_IMPACT` contém `companionId`, `actorId`, `mode`, `position`, `gmPresenceConfirmed` e o recibo interno integral `JEWEL_ESCAPE_IMPACT`. `operations.escapeCompanionImpact` persiste `impactVersion:3`, histórico por companheiro, uma explosão e uma queda por alvo. Replay reexecuta os dados e as regras do P203, rejeita alteração de posição, dano, faces, ator, ordem, duplicatas e ficha divergente entre eventos. Rollback atômico via `GameController.atomicAction` e persistência `GROUP_UPDATED` + `WORLD_UPDATED`; três reaberturas testadas. Recibos legados P203 do jogador permanecem intocados e independentes. `jewelOfYavinStatus` e avanço validam os recibos novos.

UI Android PT-BR: seleção de companheiro, confirmação de presença, faixa/perícia/autorizações de queda independentes das do jogador, botões separados de explosão e queda; bridge `resolveJewelCompanionImpact`. `versionName 1.0.204`, `versionCode 10204`, bundle offline **99 módulos**; **8/8 Java/Kokoro byte-idênticos ao P203**.

## Testes e limites

**329 arquivos de teste executados individualmente**: 323 regulares, **2.974/2.974**, mais seis stress, **6/6** = **2.980/2.980**. Oito testes focais novos. Sintaxe **133 MJS, 16 JS, 22 JSON, 2 XML** sem erros. Evidências `evidence/P204/TEST_SUMMARY.json`, `PER_FILE_RESULTS.json`, `SYNTAX_AND_JAVA.json`, `IMPLEMENTATION_EVIDENCE.json`, `PDF_TARGETED_SOURCE_CROSSCHECK.json`. **NÃO suíte monolítica**. Sem SDK/Gradle/adb/APK/CI BRELGA/homologação física. Sem envio de P204 ao GitHub.

Limites explícitos: confirmação espacial é do Mestre, não posicionamento automático; não há dano automático em companheiros ausentes, nem consumo automático de Destino. Não foram implementados Arrow-7, perseguição veicular, desfechos Arend/Naboka, SWE02, certificação das cinco aventuras ou dos 360 consumidores EotE/AoR/expansões.

## Próxima prioridade P205+

1. Perseguição real Arrow-7 e quatro capangas com escala pessoal/planetária, ações, distâncias, perseguição e persistência, cotejando SWE09; sem teleporte, turnos fictícios ou mecânicas inventadas.
2. Desfechos Arend/Naboka com gatilhos publicados, sem resultados automáticos não previstos.
3. SWE02 New Meen/Geonosis/Teemo; cotejo das cinco aventuras cena a cena e certificação semântica individual das 360 regras/consumidores AoR/EotE/expansões.
4. Testar focal + regressão global por arquivo + seis stress, preservar Java/Kokoro e saves, gerar ZIP Android Studio integral + SHA + delta verificável + handoff **somente com avanço real**; upload Biblioteca e readback SHA/CRC. Tentar GitHub `bra83/braseirobuild`/BRELGA somente com acesso real e fonte correta. Não inventar CI/APK/certificação.
