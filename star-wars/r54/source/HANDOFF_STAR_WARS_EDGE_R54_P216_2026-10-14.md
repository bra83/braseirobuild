# HANDOFF CANÔNICO — BRASEIRO STAR WARS EDGE R54 P216

## Autoridade de retomada

**P216 continua P215, NÃO recomeçar.** ZIP P215 `/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P215_SOURCE_CHECKPOINT.zip`, SHA256 `fbffef090496f4d80f3268484cce97d9a8df37acbcaad7f1fcde5fc25bf7bc8c`, 98.688.195 bytes, 6.529 arquivos. ZIP P216 completo, delta, sidecar SHA e resultados são entregues fora deste documento com hashes calculados do artefato final.

## Implementação REAL P216 — SWE09 pp.93–94

PDF oficial SWE09 `The Jewel of Yavin.pdf`, Google Drive ID `1z6Kh-0tqRxegzB_U7GL_WxicUQKIOU6K`, texto das páginas 93–94 consultado diretamente. Dois efeitos restantes de Kashyyyk integrados ao código existente:

1. **`kashyyyk_insect`** — o Mestre seleciona antes do ataque; uma rolagem real produz Triunfo original ou não. Com Triunfo, rolam-se dois dados de Benefício reais **na mesma rolagem**, antes de determinar acerto e dano; faces/pool base, faces extras e pool final são armazenados em `JEWEL_HOLO_EFFECT`. O Triunfo é gasto e não pode ser reutilizado em crítico/qualidade. Sem Triunfo não há bônus nem recibo. A interface principal de combate tem controle PT-BR `data-p216-insect`; é proibida ativação posterior no menu genérico de efeitos.
2. **`kashyyyk_vertigo`** — Desespero de ataque NPC realmente rolado, reservado e confirmado pelo Mestre; teste de Disciplina Difícil (3 dados de Dificuldade) usa Voluntad/Disciplina do PJ, dados narrativos e faces reais. Falha aplica 2 Tensão e um downgrade positivo (um dado de Proficiência para Habilidade, quando disponível) no próximo ataque ou teste de perícia que consome ação. A penalidade é consumida uma vez e registrada em `combat.log`. Sem teleporte ou turno fictício.

`jewelHoloParkEffects.mjs`, controller e UI PT-BR alterados; bundle offline reconstruído pelos dois builders locais, idênticos. Recibos verificáveis e rollback atômico. Testes de persistência após três reaberturas.

**Cobertura de efeitos Holo-Park: 16/16 em código**. Isso NÃO certifica os três encontros/facções nem toda a aventura. O downgrade foi testado em ataque e perícia que consome ação; outras categorias de ação ainda precisam de integração semântica.

## Testes finais

- 335 arquivos regulares, 3069/3069 testes; 6 stress, 6/6; TOTAL **3075/3075 em 341 arquivos**, cada arquivo executado individualmente, quatro shards não monolíticos.
- Cinco testes focais P216. Sintaxe 505 MJS, 2 JS runtime, 22 JSON, 2 XML; Java/Kokoro 8/8 byte-idênticos P215; bundle 104 módulos.
- Evidências `evidence/P216/TEST_SUMMARY.json`, `PER_FILE_RESULTS.json`, `SYNTAX_AND_JAVA.json`, `PDF_TARGETED_SOURCE_CROSSCHECK.json`, `IMPLEMENTATION_EVIDENCE.json`, `shard-*.json`.

## PRIORIDADE P217+

1. Integrar facções Arend e Grayson no Holo-Park com fichas/gatilhos oficiais, presença física confirmada e combate real, sem duplicar NPCs.
2. Desfechos Arend/Aris/Lando, `+5 Obligation Favor` só quando gatilhos oficiais.
3. Arrow-7 avançado só com ficha completa comprovada, nunca inventar stats; SWE02 New Meen/Geonosis/Teemo.
4. Cinco aventuras cena a cena e 360 consumidores EotE/AoR/expansões, homologação Android física, APK/Gradle/CI somente quando execução real comprovada.
5. Preservar saves/Kokoro, testes individuais+seis stress, ZIP completo+SHA+delta+handoff. Biblioteca pode estar cheia: se não aceitar ZIP, informar falha, não apagar checkpoints antigos.

**Não houve APK, Gradle, adb, CI BRELGA, push GitHub nem homologação Android física.**
