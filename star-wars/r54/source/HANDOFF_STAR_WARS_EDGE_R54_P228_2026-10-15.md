# HANDOFF CANÔNICO — BRASEIRO STAR WARS EDGE R54 P228

## Base intocável

P227 Android Studio integral: `/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P227_SOURCE_CHECKPOINT.zip`, SHA-256 `81147b25158af35ac6d9170548a78011110bdc21b75cb982578ecd0749680797`, 101.663.958 bytes e 9.461 arquivos. P228 foi construído sobre essa base, não recomeçado. Não excluir checkpoints antigos.

## Implementação real P228

- `app/src/main/assets/vtt/js/adventure/jewelBespinDeparture.mjs`: o embarque `JEWEL_BESPIN_BOARD` agora aceita Joia fisicamente no inventário de um companheiro existente em `group.companions[]`, sem devolução artificial ao PJ. Exige confirmações **distintas** do Mestre: PJ embarcado, companheiro portador presente no porto, companheiro portador embarcado na mesma nave, segurança resolvida. O recibo guarda `carrierCompanionId`, `gmCompanionPresentConfirmed` e `gmCompanionBoardedConfirmed`, além de nave/porto/relógio. Não assume presença de NPC sem confirmação.
- A cadeia `JEWEL_CUSTODY_TRANSFER` P227 permanece a fonte de verdade do portador e garante exatamente uma Joia no inventário real. Validação cruzada entre recibo de embarque, portador atual, inventário e grupo; tentativa de apagar companheiro/Joia antes da fuga é rejeitada. Proteção de IDs, duplicações, recibos alterados, rollback atômico e três reaberturas.
- `prepareJewelEscape`/`finalizeJewelEscape` preservam o portador companheiro e exigem sua prova de embarque, mas mantêm os mesmos requisitos P224: nave hiperespacial ativa, destino real fora de Bespin, rolagem de Astronavegação com faces/símbolos e relógio persistido. Falha de salto não gera recibo de fuga nem XP. Não há XP automático.
- `controller.mjs`: propaga o grupo persistente ao embarque e à fuga. `uiBridge.mjs` existente permanece; bundle offline regenerado. `r46/final-runtime.js`: controles Android PT-BR para confirmar presença e embarque do companheiro; nave e segurança continuam sob confirmação do Mestre.
- Android 1.0.228/versionCode 10228, shell/diagnóstico sincronizados; bundle 115 módulos; oito arquivos Java/Kokoro byte-idênticos P227. Compatibilidade dos recibos legados sem campos de companheiro.

**Proveniência:** SWE09 *The Jewel of Yavin*, pp.92–95, já cotejado em P224 para fuga e recompensa. P228 adiciona um **mecanismo técnico de registro físico de passageiros e custódia**, não uma nova regra atribuída ao PDF. Nenhuma leitura integral nova do livro foi certificada neste bloco. **Segurança oficial do porto com fichas, posições e encontros continua pendente**: a confirmação GM de segurança resolvida não substitui sua implementação.

## Testes verificáveis

- 347 arquivos regulares, **3.167/3.167** testes; seis arquivos stress executados individualmente **6/6**; total **3.173/3.173 em 353 arquivos**. Oito testes focais P228 cobrem embarque físico, recusas, salto real, adulterações, exclusão de companheiro, três reaberturas, compatibilidade de embarque do PJ e interface/bundle.
- Uma falha inicial no teste legado `r54-p77-android-build-fix` identificou diagnóstico ainda em P227; o diagnóstico foi sincronizado e o arquivo reexecutado com sucesso. Teste legado P227 que exigia igualdade exata de versão foi corrigido para versão mínima e reexecutado. Logs individuais em `evidence/P228/test-results/` e resultados `TEST_SUMMARY.json`, `PER_FILE_RESULTS.json`.
- Sintaxe: **528 MJS, 2 JS runtime, 22 JSON, 2 XML** PASS; Java/Kokoro **8/8 idênticos** ao P227; bundle **115 módulos**. `evidence/P228/SYNTAX_AND_JAVA.json`.
- **Não executados:** SDK/Gradle, adb, APK, CI BRELGA, GitHub push, homologação física, cotejo integral das cinco aventuras, certificação semântica dos 360 consumidores.

## Próximo P229+

1. Implementar segurança do porto somente com fontes SWE09 e fichas oficiais verificadas, presença física, posições e combate/rolagens reais, sem inventar dificuldades, NPCs ou vitória. Não substituir por confirmações GM genéricas.
2. Perseguidores fora das quatro visitas Arend, sem duplicar facções; Holo-Park vertigem para outras ações e relógio narrativo de dois minutos apenas com tempo real.
3. Arrow-7 avançado apenas com ficha oficial; SWE02 New Meen/Geonosis/Teemo; cinco aventuras cena a cena e 360 consumidores EotE/AoR/expansões.

Preservar Kokoro, saves e checkpoints antigos. Fazer testes focais, regressão global por arquivo e seis stress antes de empacotar. ZIP completo no sandbox; Biblioteca com limite de armazenamento — não alegar upload/readback sem prova. Não inventar Gradle, APK, CI ou homologação.

## Artefatos

Após empacotamento, conferir o relatório externo de integridade e hashes. Nunca usar contagens ou hashes presumidos.
