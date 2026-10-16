# HANDOFF CANÔNICO — BRASEIRO STAR WARS EDGE R54 P227

## Base intocável

P226 Android Studio integral: `/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P226_SOURCE_CHECKPOINT.zip`, SHA256 `3c928003f87ed3d9a224b05a0984522102241a3393269f674d63d7c493170962`, 100.881.285 bytes, 9.097 arquivos. P227 é um avanço incremental sobre esta base, não uma reconstrução. Não excluir checkpoints antigos.

## Implementação real P227

- `app/src/main/assets/vtt/js/adventure/jewelCustodyTransfers.mjs`: nova cadeia física de custódia. O recibo inicial de aquisição `JEWEL_BESPIN_CUSTODY` P224 continua obrigatório; transferências `JEWEL_CUSTODY_TRANSFER` com IDs únicos, origem/destino reais, confirmação explícita de copresença e entrega pelo Mestre, carimbo do relógio persistente, sem combate ativo e sem duplicar o item.
- Transferência efetiva de `gear[]` entre o PJ ativo e membros de `group.companions[]` com ficha persistida; suporta PJ→companheiro, companheiro→PJ e companheiro→companheiro. O sistema rejeita destino fantasma, origem incorreta, transferência após embarque, cópia ou desaparecimento do item e exclusão de companheiro que carrega a Joia.
- `replayJewelCustodyTransfers` e `validateJewelCustodyTransfers`: recibos reexecutados na ordem, estado derivado validado, relógio auditado e exatamente uma Joia física no inventário do portador corrente. Não é assinatura criptográfica de eventos: é consistência/replay local.
- `jewelBespinDeparture.mjs` preserva os recibos P224 e a fuga real, mas verifica o portador atual no embarque. **Limite intencional:** embarque ainda exige Joia no inventário do PJ ativo; o Mestre deve devolvê-la ao PJ antes de embarcar. Nenhuma presença de companheiro a bordo é inventada.
- Controller `transferJewelCustody` com commits CHARACTER_UPDATED/GROUP_UPDATED/WORLD_UPDATED na mesma transação e rollback; bridge e bundle offline; validador de saves; interface Android PT-BR com portador e destinatário reais e duas confirmações GM.
- Android 1.0.227/versionCode10227. Oito arquivos Java/Kokoro preservados byte a byte do P226. Sem concessão de XP, sem teleporte, sem combate/segurança inventados.

A fonte narrativa subjacente é SWE09 *The Jewel of Yavin*, pp.92–95, já cotejada no P224. A transferência é um **mecanismo técnico de integridade do item físico**, não uma regra de recompensa específica atribuída ao livro. Nenhum novo cotejo integral de SWE09 foi realizado neste bloco.

## Testes verificados

- 346 arquivos regulares, **3.159/3.159** testes aprovados; seis arquivos stress **6/6**; total **3.165/3.165** em 352 arquivos. Cada arquivo executado separadamente via `node --test`, sem suíte monolítica. Nove focais P227: transferência PC→companheiro, retorno/embarque, recusas atômicas, três reaberturas, adulterações de recibo/inventário/tempo, bridge/UI/bundle, compatibilidade P224, companheiro→companheiro e bloqueio pós-embarque.
- Sintaxe **527 MJS, 2 JS runtime, 22 JSON, 2 XML** aprovada; **8/8 Java/Kokoro idênticos**; bundle **115 módulos**. Evidências `evidence/P227/TEST_SUMMARY.json`, `PER_FILE_RESULTS.json`, `SYNTAX_AND_JAVA.json`, logs TAP individuais.
- Sem Android SDK, Gradle, adb, APK, CI BRELGA, push GitHub ou homologação física. Não alegar essas etapas.

## Próximos blocos P228+

1. Segurança do porto baseada em cenas e fichas oficiais, com confirmação física, testes/combate reais e sem inventar dificuldade ou NPC.
2. Embarque real com Joia carregada por companheiro, se o sistema passar a registrar presença e embarque de cada membro, mantendo a cadeia de custódia.
3. Perseguidores fora das quatro visitas Arend; Holo-Park vertigem em outras ações e relógio narrativo de 2 minutos só com tempo real.
4. Arrow-7 avançado somente com ficha oficial, SWE02 New Meen/Geonosis/Teemo, cinco aventuras cena a cena e 360 consumidores EotE/AoR/expansões.

Não apagar checkpoints antigos. ZIP completo permanece no sandbox; Biblioteca com limite de armazenamento: não alegar upload/readback do ZIP. Gerar novo ZIP, SHA, delta e handoff somente com código/testes verificáveis.

## Artefatos

Ver relatório externo de integridade e hashes após empacotamento. Nunca usar hashes ou contagens presumidos.
