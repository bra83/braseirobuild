# HANDOFF CANÔNICO — BRASEIRO STAR WARS EDGE R54 P223

## Continuidade e integridade

P223 é uma evolução DIRETA do ZIP integral canônico P222: `/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P222_SOURCE_CHECKPOINT.zip`, SHA256 `c967d720902103bcdc66f7083e84f68fcb79d7b16874bf93e8029cf3279c1ea3`, 99.681.224 bytes, 7.655 arquivos, CRC PASS. NÃO reiniciar, NÃO apagar saves, checkpoints anteriores nem alterar Kokoro.

## Implementação REAL P223 — SWE09 The Jewel of Yavin

Fonte oficial consultada diretamente no Drive `1z6Kh-0tqRxegzB_U7GL_WxicUQKIOU6K`, páginas impressas 90–91 e 95; PDF página 96 renderizada para conferir os blocos gráficos que não aparecem integralmente no texto extraído. Storm IV: Silhueta 2, Velocidade 4, Manejo +1, Defesa 0, Blindagem 2, Casco 5, Tensão de sistemas 7, arma frontal 4/crítico 4/Linked 1. Piloto minion Bespin Defense Force: atributos 2/3/2/2/2/2, absorção 2, Ferimentos 12, perícias de grupo Frieza/Artilharia/Pilotagem Planetária. Não inventar graduação individual de perícia; um minion sozinho usa graduação 0.

Arquivos reais:
- `app/src/main/assets/vtt/js/rules/vehicles.mjs`: perfil **SWE09_ADVENTURE_VEHICLE_PROFILES.storm_iv_cloud_car** separado do catálogo EotE regular, sem contaminar os 25 veículos de catálogo.
- `app/src/main/assets/vtt/js/adventure/jewelStormIvPatrol.mjs`: exige alerta e chegada P221, nave real do PJ, Mestre confirmar quatro carros/pilotos e posições físicas; cria quatro IDs fixos, dois voos de dois, cada veículo com casco, tensão, armamento e piloto próprios. Sem teleporte, sem encontro automático.
- Um carro de cada vez: Mestre confirma alcance real, motor veicular existente executa ataque, dano e dados; piloto inimigo usa ficha oficial (sem Gunnery fictícia). Desfecho `DISABLED` exige casco ultrapassar limiar e ataques reais no log. `EVADED` exige teste REAL de Pilotagem Planetária com dificuldade 1–5 definida explicitamente pelo Mestre, faces/pool/símbolos conferidos e sucesso; o livro não fixa dificuldade única para essa manobra. Cada carro só pode ser resolvido uma vez.
- Após quatro desfechos distintos, `JEWEL_STORM_IV_CLEARED` fecha a patrulha. Os 5 PE oficiais `CLOUD_CAR_PATROLS` só podem ser concedidos ao PJ ativo após essa prova, por confirmação GM, `XP_GRANTED` e recibo `JEWEL_EP3_XP`. Nenhum PE por chegada narrativa. Fuga de Bespin com a joia (+5 PE) ainda BLOQUEADA.
- Controller, bridge, interface Android PT-BR, status, validador de saves, bundle offline **111 módulos** e Android **1.0.223** / versionCode **10223** integrados. Recibos `JEWEL_STORM_IV_DEPLOY/ENGAGE/OUTCOME/CLEARED`, replay de ordem, anti-duplicação, rollback atômico, três reaberturas, dados de evasão validados.

## Testes e limites

Testes executados individualmente, não suíte monolítica: **342 arquivos regulares, 3126/3126 testes; 6 stress, 6/6; TOTAL 3132/3132 em 348 arquivos individuais**. Nove testes focais P223. Logs TAP individuais `evidence/P223/test-results/`, sumário e resultado por arquivo. Sintaxe 519 MJS, 2 JS runtime, 22 JSON, 2 XML PASS; 8/8 Java/Kokoro byte-idênticos P222. O teste legado R36 de contagem de veículos detectou catálogo contaminado: perfil Storm IV foi isolado em catálogo de aventura e todos os testes passaram. Diagnóstico de versão Android sincronizado para P223.

**Não houve** SDK/Gradle/adb/APK, CI BRELGA, homologação Android física, GitHub push ou certificação integral. Não inventar resultados. A validação de recibos impede inconsistências observáveis mas NÃO é assinatura criptográfica; a dificuldade da evasão é adjudicada pelo Mestre. A implantação depende de nave já selecionada e chegada confirmada, não cria nave do jogador. O motor mantém um PJ ativo; não premiar automaticamente outros personagens ou NPCs.

## Próximos P224+

1. Fuga efetiva de Bespin com a Joia: posse física e cadeia de custódia, nave real, embarque, segurança/checagens oficiais, pilotagem e deslocamento persistido; +5 PE somente após recibo verificável de fuga.
2. Consequências reais de falhas nas patrulhas Wing Guard da residência com perfis oficiais e quantidade/posição confirmadas, sem teleporte.
3. Vertigem do Holo-Park em todas as ações aplicáveis; relógio narrativo de 2 minutos somente com minutos reais persistidos.
4. Arrow-7 avançado somente com ficha oficial comprovada; SWE02 New Meen/Geonosis/Teemo; cinco aventuras cena a cena; certificação dos 360 consumidores EotE/AoR/expansões.
5. Preservar saves/Kokoro, testes focais+regressão por arquivo+seis stress; ZIP integral Android Studio+SHA+delta+handoff apenas após implementação real. Biblioteca cheia: não apagar checkpoints antigos; fornecer ZIP sandbox, delta/handoff/sidecar e não alegar upload/readback não realizado.

## Artefatos

ZIP integral, delta P222→P223, sidecar SHA256 e handoff externo disponíveis no sandbox. Consulte o handoff EXTERNO para checksums finais gerados após empacotamento.
