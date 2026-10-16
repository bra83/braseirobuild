# HANDOFF CANÔNICO — BRASEIRO STAR WARS EDGE R54 P225

## Base, fonte e escopo REAL

Base: ZIP P224 `/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P224_SOURCE_CHECKPOINT.zip`, SHA256 `ac1503e3ccc7de3afeeae01fb52c473f7da8f697c5ae2385dbad29e9ab146856`, 100.281.227 bytes e 8.371 arquivos. Nenhum checkpoint anterior apagado. Fonte consultada diretamente: Google Drive PDF oficial SWE09 *The Jewel of Yavin* id `1z6Kh-0tqRxegzB_U7GL_WxicUQKIOU6K`, páginas impressas 90–91, e perfil oficial Wing Guard pp.12–13.

## Implementação P225 — consequência de falha de patrulha na residência

- Novo `app/src/main/assets/vtt/js/adventure/jewelResidenceWingResponse.mjs`. Após teste Difícil real de Engano/Furtividade/Conhecimento das Ruas falhar durante os intervalos de quinze minutos registrados por `jewelArendPatrols.mjs`, uma falha fica pendente até a confirmação do Mestre. Não há spawn automático nem teleporte.
- `prepareJewelResidenceWingResponse` exige primeiro check fracassado ainda pendente, relógio persistido, presença física da Guarda Alada, seleção da facção, indivíduos distintos, quantidade real (1–8) e alcance real confirmados pelo Mestre. O livro não fixa a quantidade exata deste grupo na residência: o Mestre informa o número observado. **Não presume automaticamente as outras facções que também podem alcançar os PJs.**
- `JEWEL_RESIDENCE_WING_RESPONSE` guarda ID do check de dados original, sequência, minuto real, perfil oficial `bespin_wing_guard_officer` (SWE09 p.12), quantidade, distância e confirmações. Replay e validador de estado detectam adulteração, duplicação, ordem, futuro e falsificação de falha.
- `beginJewelResidenceWingResponse` no controller chama o motor **real** `beginPublishedAdventureEncounter`: minions oficiais, iniciativa, turnos, ataque/dano do motor; `finishCombat` registra resultado no histórico publicado. Sem concessão de XP, ferimentos ou resultado automático.
- Integração em `jewelOfYavin.mjs` (encounterSpec/status/prioridade), `jewelOfYavinOperations.mjs` (kind no ledger), `state.mjs` (reabertura/saves), `uiBridge.mjs`, UI Android PT-BR `r46/final-runtime.js` e bundle offline regenerado (113 módulos).
- Android **1.0.225**, versionCode **10225**, shell sincronizado. **8/8 Java/Kokoro byte-idênticos P224**. Nenhum arquivo legado removido.

## Testes reais P225

- `tests/uae-p225-residence-wing-response.test.mjs`: **7/7**. Casos: falha real não cria NPCs; presença física obrigatória; quantidade/distância válidas; ficha e iniciativa reais; combate encerrado no histórico; replay/tamper/rollback; interface/bridge/bundle.
- Regressão final por arquivo, NÃO suíte monolítica: **344 arquivos regulares, 3.141/3.141 testes + 6 stress 6/6 = 3.147/3.147 em 350 arquivos**. O runner usou oito trabalhadores paralelos, mas invocou `node --test` em cada arquivo individualmente. Evidências: `evidence/P225/test-results/`, `PER_FILE_RESULTS.json`, `TEST_SUMMARY.json`, `runner.log`.
- Sintaxe: **523 MJS, 2 JS runtime, 22 JSON, 2 XML PASS**; oito arquivos Java/Kokoro idênticos. `evidence/P225/SYNTAX_AND_JAVA.json`.
- Teste legado P224 que exigia exatamente versionCode 10224 atualizado para >=10224, mantendo a garantia mínima original.

## Limites e próximos blocos

- P225 cobre **um grupo Wing Guard confirmado** após falha. A possibilidade editorial de 2–3 grupos perseguidores (Arend/Grayson/Guarda Alada), sua coordenação e a resolução de todas as falhas ainda exigem implementação; não inventar líderes, tamanho dos outros grupos nem chegada simultânea.
- Prioridades P226+: outras facções de perseguição com confirmação real, Holo-Park vertigem para demais ações e janela narrativa de dois minutos com relógio real, transferência da Joia entre inventários físicos, segurança do porto com encontros, Arrow-7 apenas com ficha oficial, SWE02 New Meen/Geonosis/Teemo, auditoria das cinco aventuras cena a cena e 360 consumidores EotE/AoR/expansões.
- **Não houve SDK, Gradle, adb, APK, CI BRELGA, push GitHub nem homologação física Android.** Não declarar conclusão integral.
- Empacotar ZIP Android Studio integral + SHA256 sidecar + delta P224→P225, validar CRC e replay de todos os hashes. Se Biblioteca estiver cheia, manter ZIP sandbox e tentar preservar delta/handoff/sidecar sem alegar upload/readback não realizado. Não apagar checkpoints antigos sem autorização.

## Artefatos finais

Os hashes, tamanhos e contagens definitivos são preenchidos após a verificação do empacotamento; ver `PACKAGING_P225.json` em `/mnt/data` quando disponível.
