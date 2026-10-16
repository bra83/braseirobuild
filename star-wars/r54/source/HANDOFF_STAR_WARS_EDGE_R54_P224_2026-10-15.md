# HANDOFF CANÔNICO — BRASEIRO STAR WARS EDGE R54 P224

## Base e preservação

P224 deriva **diretamente** do ZIP Android Studio P223 `/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P223_SOURCE_CHECKPOINT.zip`, SHA256 `0a7b43601d0e4354d055c3fd81c601f7a899d9e0017dc2f912e94caab57f972e`, 99.982.188 bytes, 8.012 arquivos. Não reiniciar, não apagar checkpoints antigos, não alterar Kokoro ou saves.

## Implementação REAL P224 — SWE09 pp.92–95

- Fonte oficial The Jewel of Yavin (Google Drive id `1z6Kh-0tqRxegzB_U7GL_WxicUQKIOU6K`), seção **Flight over Fight**, pp.92–94, e **Episode III XP Rewards**, p.95. O livro não determina uma dificuldade única de segurança/astrogacão: segurança fica sob confirmação do Mestre; a rota selecionada usa o motor real de Astronavegação.
- Novo `app/src/main/assets/vtt/js/adventure/jewelBespinDeparture.mjs`. Três recibos **JEWEL_BESPIN_CUSTODY**, **JEWEL_BESPIN_BOARD**, **JEWEL_BESPIN_ESCAPE** com replay, ordem, IDs, cronologia, faces/símbolos e estado derivado. Não são assinaturas criptográficas.
- **Posse física**: requer flag real de aquisição no cofre, histórico de teste bem-sucedido, visita ao cofre e Mestre confirmar posse. Adiciona item único `swe09-jewel-of-yavin-gem` à ficha do PJ ativo, não a NPCs. Sem inventar valor de venda ou encumbrance.
- **Embarque**: nave real selecionada, hiperpropulsor funcional, Joia presente no inventário, porto e segurança resolvidos fisicamente com confirmação GM. Sem teleporte ou confronto fictício.
- **Fuga**: destino existente no Mundo Vivo fora de Bespin, autorização GM e rolagem REAL de Astronavegação com `resolveHyperspaceJump`; o motor preserva faces, símbolos, consequência na nave e avanço real de `world.clock.tick` por horas convertidas em ticks. Falha/desvio preserva rolagem, mas NÃO cria recibo de fuga nem concede PE. `elapsedMinutes` de tarefas não é artificialmente incrementado.
- **Epílogo**: a opção `reach-hangar` agora exige `bespinEscapeVerified` derivada de recibo válido, não apenas a flag de aquisição da Joia. Saves antigos concluídos permanecem legíveis, mas não liberam os PE sem recibo novo.
- **5 PE oficiais** `BESPIN_ESCAPE`: `JEWEL_EP3_XP` somente após prova de fuga e confirmação do Mestre, `XP_GRANTED` na ficha do PJ ativo, sem repetição. Os cinco marcos do Episódio III têm rotinas com gatilhos, não recompensas automáticas.
- Controller/bridge/UI Android PT-BR/status/validador de saves integrados; Android **1.0.224**, versionCode **10224**, bundle offline **112 módulos**; **8/8 Java/Kokoro byte-idênticos P223**.

## Testes e limitações

- **343 arquivos regulares, 3.134/3.134 testes + 6 stress 6/6 = 3.140/3.140 em 349 arquivos individuais**, sem suíte monolítica; oito testes focais P224. `evidence/P224/test-results/`, `PER_FILE_RESULTS.json`, `TEST_SUMMARY.json`.
- Sintaxe **521 MJS, 2 JS runtime, 22 JSON, 2 XML** PASS; 8 Java/Kokoro idênticos. Testes legados P183 (epílogo indevido), P222 (novo marco) e P223 (versionCode futuro) atualizados para preservar intenção sem exigir comportamento incorreto.
- Limites: segurança no porto exige confirmação GM, não existe combate de segurança automático; transferência física da Joia entre personagens não implementada; motor de um PJ ativo; nenhum SDK/Gradle/adb/APK/CI BRELGA/GitHub push/homologação física/certificação integral. Não inventar.

## Prioridades P225+

1. Consequências reais das falhas de patrulha Wing Guard na residência, com ficha oficial, presença, posição e encontros persistentes sem teleporte.
2. Estender vertigem Holo-Park a demais ações e relógio narrativo de 2 minutos somente com tempo persistido real.
3. Transferência física da Joia entre PJ/companheiros presentes com cadeia de custódia e inventários reais; segurança do porto com encontros reais oficiais.
4. Arrow-7 avançado apenas com ficha oficial comprovada; SWE02 New Meen/Geonosis/Teemo; cinco aventuras cena a cena e 360 consumidores EotE/AoR/expansões.
5. Preservar Kokoro/saves; testar focal+regressão por arquivo+seis stress, empacotar ZIP Android Studio completo+SHA+delta+handoff apenas após implementação real. Se Biblioteca cheia, fornecer ZIP sandbox e não alegar upload/readback do ZIP.

## Artefatos

ZIP completo P224, delta P223→P224, SHA sidecar e handoff externo em `/mnt/data/`. Consulte handoff externo para hashes finais, gerados após empacotamento.
