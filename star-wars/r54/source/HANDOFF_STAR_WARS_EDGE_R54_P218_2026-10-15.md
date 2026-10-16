# HANDOFF CANÔNICO — BRASEIRO STAR WARS EDGE R54 P218

## Base preservada

P218 foi implementado diretamente sobre **P217**, ZIP Android Studio integral `BRASEIRO_STAR_WARS_EDGE_R54_P217_SOURCE_CHECKPOINT.zip`, SHA256 `79ec8f9b81dab13be0fc895cf134c50513471b9090487691042adf75958abc4b` (6.555 arquivos). Nenhuma reconstrução do zero; P217 e seus testes permanecem. Não declarar GitHub/CI/APK/homologação inexistentes.

## Fonte oficial cotejada diretamente

PDF Google Drive `1z6Kh-0tqRxegzB_U7GL_WxicUQKIOU6K`, **SWE09 The Jewel of Yavin**, p.90, seção **Extended Campaign: Going Respectable**. A aventura permite que o grupo procure a Guarda Alada ou Lando antes ou depois de capturar Arend. A joia pode ser oferecida como prova, mas não é uma condição obrigatória do perdão. Lando só concede perdão se aceitar que os PJs ajudarão a reparar a reputação turística de Cloud City. Cada PJ incorre em **5 pontos de Obligation: Favor para Lando**. Não aplicar automaticamente XP, captura de Arend, aliança com Aris ou devolução da joia.

## Implementação real

- Novo `app/src/main/assets/vtt/js/adventure/jewelLandoPardon.mjs`: `grantJewelLandoPardon`, `replayJewelLandoPardon`, `validateJewelLandoPardon`. Recibo `JEWEL_LANDO_PARDON`, versão 1, fonte, lista de PJs, valor fixo 5 e cinco confirmações explícitas do Mestre.
- Pré-condições: aventura iniciada (ativa ou concluída), EotE/misto, sem combate, audiência real, plano de restauração aceito, perdão efetivamente concedido e lista integral de PJs reais confirmada. A ficha ativa precisa estar incluída; nunca inventar integrantes, adicionar NPCs ou presumir sucesso social.
- O grupo recebe entradas reais em `group.obligationEntries` de **5 Favor** por PJ, preservando todas as Obrigações existentes; se só havia Obrigações legadas na ficha pessoal, elas são migradas ao livro de grupo antes do Favor, sem desaparecerem do total.
- Proteção contra execução duplicada mesmo com outro actionId, nomes repetidos e adulteração de recibos; transação atômica, rollback, validação no carregamento do save e três reaberturas testadas.
- Controller `grantJewelLandoPardon`, bridge `grantJewelLandoPardon`, painel Android PT-BR com confirmação separada de audiência/plano/perdão/lista e nomes dos PJs, status SWE09; bundle offline reconstruído com **106 módulos**.
- Android `1.0.218`, versionCode `10218`. Os oito arquivos Java/Kokoro do P217 não foram alterados.

## Testes e evidências

Regressão final executada **individualmente por arquivo** em quatro workers, não suíte monolítica: **337 arquivos regulares / 3.086 testes aprovados; 6 arquivos stress / 6 testes aprovados; TOTAL 3.092/3.092 em 343 arquivos**. Oito testes focais P218. Evidências `evidence/P218/` com resultados por arquivo, integridade, sintaxe, fonte e logs focais. Regressões intermediárias de versão no shell corrigidas e regressão global reexecutada integralmente sem falhas.

## Limites e próximos passos P219+

1. Desfechos oficiais de Arend (capturado, morto, sobrevivente, inimigo recorrente) e aliança real com Aris, cotejados com SWE09 pp.90–95. Nenhum XP antecipado.
2. Holo-Park: 16/16 efeitos em código não equivale a homologação integral. Integrar vertigem em outras ações com gasto real de ação e modo narrativo de dois minutos somente com relógio comprovado.
3. Arrow-7: não inventar velocidade, casco, blindagem, piloto ou manobras sem ficha oficial.
4. SWE02 New Meen/Geonosis/Teemo; cotejo das cinco aventuras cena a cena e 360 consumidores EotE/AoR/expansões.
5. Preservar Kokoro/saves, testes focais + regressão global por arquivo + 6 stress, ZIP Android Studio completo + SHA + delta + handoff somente após implementação real.

## Distribuição e integridade

Os hashes do ZIP completo P218 e do delta P217→P218 estão no handoff **externo** após empacotamento, evitando circularidade de hash dentro do ZIP. Se a Biblioteca continuar em `library_storage_limit_exceeded`, preservar delta/handoff/sidecar e fornecer ZIP sandbox sem alegar upload/readback do ZIP. Não apagar checkpoints antigos sem autorização. Não houve SDK, Gradle, adb, APK, CI BRELGA, push GitHub nem homologação física Android.
