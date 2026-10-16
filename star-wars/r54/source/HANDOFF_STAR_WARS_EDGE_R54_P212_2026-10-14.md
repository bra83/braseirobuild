# HANDOFF CANÔNICO — BRASEIRO STAR WARS EDGE R54 P212

## Base imutável e continuidade

**P212 continua P211, sem recomeçar.** Base `/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P211_SOURCE_CHECKPOINT.zip`, SHA-256 `ed4dc7b3bda799375daa74fa7ac9be7507ed629ea2f003e0eaae8b0333d63a2d`, 6.489 arquivos. Novo checkpoint `/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P212_SOURCE_CHECKPOINT.zip`; delta `/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P211_TO_P212_DELTA.zip`; SHA sidecar. Preservar EotE/AoR, todos os saves e Kokoro.

## P212: implementação REAL source-backed

PDF oficial *The Jewel of Yavin* (SWE09), Drive `1z6Kh-0tqRxegzB_U7GL_WxicUQKIOU6K`, páginas impressas **93–94**, consultado diretamente pelo conector Google Drive.

1. **Coruscant — `coruscant_building`:** o Triunfo ainda disponível de ataque acertado é consumido. Abrigo holográfico concede **Defesa à distância +2** ao personagem; `_resolveNpcTurn` passa a incluir os dois dados de Contratempo reais no ataque à distância, não em corpo a corpo. O benefício é encerrado ao executar movimento real via `changeCombatRange` ou manobra explicitamente rotulada de movimentação em `useManeuver`. Não se inventa duração por rodada; duração publicada é **até mover**.
2. **Alderaan — `alderaan_nerfs`:** Triunfo ainda disponível de ataque acertado é consumido, presença da manada confirmada pelo Mestre. `GameController.applyJewelHoloParkEffect` consome uma manobra real usando o mecanismo de manobras e faz teste de **Furtividade Média** (`sigilo`/Agilidade, dificuldade 2) com faces reais de dados narrativos. O recibo contém pool, faces, símbolos, sucesso e rodada; `replayJewelHoloEffects` reconstrói as faces e verifica símbolos. **Não declara desaparecimento nem fuga**: `gmEscapePending=true` aguarda adjudicação do Mestre.

Ambos em `app/src/main/assets/vtt/js/adventure/jewelHoloParkEffects.mjs`, controller, interface `r46/final-runtime.js` e bundle offline; recibos `JEWEL_HOLO_EFFECT` v1 compatíveis com P210/P211, bloqueio de duplicação, rollback atômico e três reaberturas. UI Android PT-BR oferece confirmação da manada e modo de manobra. **Cobertura 8/16**, oito efeitos restantes manuais. Nenhum NPC foi teletransportado.

## Testes finais

- `tests/uae-p212-holopark-cover-nerfs.test.mjs`: **8/8** focais, incluindo pool de dados NPC real, manobra real, consumo de Triunfo, rolagem de Furtividade, três reaberturas, rejeição de símbolos inventados e adulteração de faces.
- Regressão global **por arquivo**, NÃO suíte monolítica: **331 arquivos regulares, 3.047/3.047**; **6 arquivos stress, 6/6**. **TOTAL 3.053/3.053 em 337 arquivos individuais**, falhas finais zero. Evidências `evidence/P212/TEST_SUMMARY.json`, `PER_FILE_RESULTS.json`.
- Sintaxe **501 MJS**, dois JS runtime, 22 JSON e dois XML, erros zero. Java/Kokoro **8/8 byte-idênticos P211**; bundle offline **104 módulos**. Android `versionName=1.0.212`, `versionCode=10212`. Evidências `SYNTAX_AND_JAVA.json`, `PDF_TARGETED_SOURCE_CROSSCHECK.json`, `IMPLEMENTATION_EVIDENCE.json`.

## Próximas prioridades P213+

1. Implementar **oito efeitos restantes** SWE09 pp.93–94 com origem correta de Ameaça/Desespero, verificações de NPC e de fichas, dados reais, estado, duração e recibos; não inventar resultados de símbolos. Cuidar de movimento e cobertura de Coruscant.
2. Integrar facções **Arend/Grayson** ao Holo-Park com perfis oficiais e presença física confirmada; sem teleporte, NPC duplicado ou substituição da Guarda Alada.
3. Desfechos Arend/Aris/Lando, Favor +5 somente sob gatilhos publicados; modo narrativo 2 minutos apenas com relógio real.
4. Arrow-7 avançado somente com atributos publicados, nunca inventar velocidade/casco/blindagem; SWE02 New Meen/Geonosis/Teemo; cinco aventuras cena a cena e 360 consumidores EotE/AoR/expansões.

**Não executado:** SDK/Gradle/adb/APK, CI BRELGA, homologação física Android, push GitHub, certificação integral. Biblioteca anteriormente atingiu limite de armazenamento em P211: não apagar checkpoints sem autorização; se ZIP completo for recusado, manter no sandbox e entregar delta/handoff/sidecar, sem afirmar upload/readback do ZIP.
