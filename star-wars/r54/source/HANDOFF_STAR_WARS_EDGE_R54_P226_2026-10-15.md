# HANDOFF CANÔNICO — BRASEIRO STAR WARS EDGE R54 P226

## Continuidade

Base P225 **preservada**: `/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P225_SOURCE_CHECKPOINT.zip`, SHA256 `5feff46606aece0f8a4703634c4bc876dfe8e15e0cf23e44581ccaf5b2f3f88f`, 100.576.841 bytes, 8.731 arquivos. Não reiniciar. Nenhum checkpoint anterior pode ser apagado sem autorização.

## Implementação real P226

Fonte consultada **diretamente**: PDF oficial SWE09 *The Jewel of Yavin*, Drive id `1z6Kh-0tqRxegzB_U7GL_WxicUQKIOU6K`, pp.90–91, parágrafo que exige teste Difícil de Engano, Furtividade ou Conhecimento das Ruas **a cada novo local** da busca por Arend. Falha gera possibilidade de uma facção perseguidora; falha com duas Ameaças, duas; falha com Desespero, três. Esta mecânica é **distinta** da patrulha Wing Guard de quinze minutos na rua da residência (P225).

- Novo `app/src/main/assets/vtt/js/adventure/jewelPursuitTravel.mjs`. Um teste real por recibo de visita oficial `JEWEL_AREND_VISIT` da busca P207, com ficha de PJ, pool, faces, símbolos, ID de local e minuto persistente. Recibo `JEWEL_PURSUIT_TRAVEL_CHECK` reexecutado e validado contra os dados, visita e cronologia. Não gera NPCs automaticamente.
- `JEWEL_PURSUIT_GROUP_ARRIVAL`: GM escolhe explicitamente Guarda Alada, segurança de Grayson ou capangas de Arend, confirma chegada **física**, distância, quantidade e indivíduos distintos. Fichas oficiais `bespin_wing_guard_officer`, `grayson_private_security_guard`, `shens_goons`. Limites por grupo conservadores: 1–8, 1–6, 1–4; não inventa Aris em combate, chefes, teleporte ou grupos extras. Uma facção por slot de cada deslocamento, sem repetir facção no mesmo deslocamento.
- Encontros sequenciais `jewel-pursuit-travel-<visita>-<slot>` via motor **real** `beginPublishedAdventureEncounter` e `finishCombat`, com iniciativa, dano, resultados e histórico persistente. Grupo seguinte só pode iniciar após o encontro anterior estar resolvido. A chegada não concede XP, dano ou vitória automaticamente.
- Validação de replay contra adulteração de pool, faces, símbolos, visitas, cronologia, grupos, quantidades e estado derivado; transação atômica e reaberturas. `jewelOfYavin.mjs` (status/encounter), `jewelOfYavinOperations.mjs` (ledger), `state.mjs` (save), controller, uiBridge, UI Android PT-BR e bundle offline (114 módulos).
- Android **1.0.226** / versionCode **10226**; 8 Java/Kokoro preservados byte a byte P225. Sem SDK/Gradle/adb/APK/CI BRELGA/GitHub push/homologação física.

## Testes e empacotamento

Preencher somente com evidências verificadas: `evidence/P226/TEST_SUMMARY.json`, `PER_FILE_RESULTS.json`, logs TAP individuais e `SYNTAX_AND_JAVA.json`. Não executar suíte monolítica; cada arquivo é um processo `node --test` separado, mais seis stress. Validar ZIP integral e delta por SHA256, CRC e replay dos hashes antes de promover P226 a checkpoint canônico.

## Próximos blocos P227+

- Cobrir perseguição fora das quatro visitas de Arend e condições especiais de talentos/ferimentos que modificam pools, sem perder prova original; evitar duplicação entre facções no Holo-Park e patrulhas da residência.
- Implementar segurança oficial do porto e transferência da Joia entre inventários reais, com cadeia de custódia e recibos.
- Holo-Park: vertigem em demais ações e relógio narrativo de dois minutos somente com tempo real persistido.
- Arrow-7 avançado somente com ficha oficial, SWE02 New Meen/Geonosis/Teemo, cinco aventuras cena a cena, 360 consumidores EotE/AoR/expansões.
- Compilação GitHub BRELGA, APK e homologação Android somente após execução real e provas. Não declarar conclusão integral.

## Artefatos

Serão registrados após empacotamento: ZIP P226, SHA256 sidecar, delta P225→P226, manifest de hashes e resultados. Se Biblioteca estiver cheia, preservar ZIP no sandbox e oferecer delta/handoff/sidecar; nunca alegar upload/readback não realizado.
