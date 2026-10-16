# HANDOFF CANÔNICO — BRASEIRO STAR WARS EDGE R54 P213

## 1. Continuidade e base

**P213 deriva diretamente de P212. NÃO RECOMEÇAR.** ZIP base `/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P212_SOURCE_CHECKPOINT.zip`, SHA-256 `68d698c5987dbfbcec1d85abe61718a87f361ecd6e7e80d6a339e01b7221ed29`, 98.613.972 bytes, 6.496 arquivos. Preservar EotE/AoR, saves, oito arquivos Java/Kokoro e todos os módulos anteriores.

## 2. Implementação P213 comprovada

Fonte oficial lida diretamente do Google Drive: SWE09 *The Jewel of Yavin*, ID `1z6Kh-0tqRxegzB_U7GL_WxicUQKIOU6K`, páginas impressas 93–94. Os resultados de **Ameaça do ataque NPC** são diferentes de Vantagem/Triunfo do jogador.

1. `christophsis_spotlight`: uma Ameaça NPC REAL e ainda reservada causa um dado de Contratempo nos ataques contra o personagem até o fim de seu próximo turno real. O dado modifica `_resolveNpcTurn`; `finishPlayerInitiativeTurn` encerra o efeito. Não se concede bônus defensivo permanente.
2. `coruscant_transport`: uma Ameaça NPC REAL causa um dado de Contratempo aos testes de combate do jogador e dos companheiros fisicamente presentes, individualmente confirmados pelo Mestre, até que cada um gaste manobra real de movimento. O modificador altera `resolveWeaponAttack`, `resolveSkillCheck` para perícias de combate e `resolveCompanionSkillCheck`; `changeCombatRange`/`useManeuver` limpam apenas o jogador, e `confirmJewelHoloCompanionMovement` registra a manobra confirmada de cada companheiro sem teleporte ou avanço fictício.

`reserveJewelHoloNpcThreat` retira UMA Ameaça real do fallback genérico de Tensão NPC para impedir gasto duplo. Guarda `attackId`, `attackerId`, rodada, paisagem, pool, faces e símbolos no estado persistente; `applyJewelHoloEffect` compara prova com `combat.log`, consome a reserva, grava recibo `JEWEL_HOLO_EFFECT` com replay de faces e bloqueia duplicações. Operações são atômicas e saves P212 sem campos novos permanecem válidos. **Limite explícito:** uma Ameaça reservada e não usada fica aguardando decisão do Mestre; ainda não há opção de converter essa reserva tardiamente para a antiga Tensão NPC.

Arquivos centrais: `app/src/main/assets/vtt/js/adventure/jewelHoloParkEffects.mjs`, `js/core/controller.mjs`, `js/core/uiBridge.mjs`, `r46/final-runtime.js`, `r46-shell.html`, `js/core/uiBridge.bundle.js`; `tests/uae-p213-holopark-npc-threats.test.mjs` (9 testes). Android `1.0.213`, `versionCode=10213`, bundle offline 104 módulos. O teste legado P212 foi ajustado apenas para aceitar versões futuras (>=10212); dois testes históricos P74/P77 voltaram a passar após atualizar `data-version` do shell.

## 3. Evidências e limites

- Regressão final **332 arquivos regulares: 3.056/3.056**, seis arquivos stress **6/6**, total **3.062/3.062 em 338 arquivos individuais**; nove testes focais P213. NÃO foi usada suíte monolítica. Falhas intermediárias de versão/shell corrigidas antes da regressão final, sem falhas restantes.
- Sintaxe **502 MJS, dois JS runtime, 22 JSON e dois XML** PASS. Java/Kokoro **8/8 byte-idênticos** P212. Evidências `evidence/P213/TEST_SUMMARY.json`, `PER_FILE_RESULTS.json`, `SYNTAX_AND_JAVA.json`, `PDF_TARGETED_SOURCE_CROSSCHECK.json`, `IMPLEMENTATION_EVIDENCE.json`.
- Cobertura Holo-Park: **10/16 efeitos**. Seis pendentes: Alderaan arma perdida, Christophsis fonte/cobertura, Coruscant desaparecimento furtivo, Kashyyyk inseto aliado, arma perdida e vertigem.
- NÃO houve APK/SDK/Gradle/adb, CI BRELGA, homologação física Android, push GitHub ou certificação integral de livros. O ZIP Android Studio é fonte, não APK.

## 4. Próximas prioridades P214+

1. Implementar os seis efeitos restantes do Holo-Park SWE09 pp.93–94, inclusive Ameaça/Desespero NPC, perda/recuperação física de arma, Furtividade oposta e Disciplina Difícil com dados reais. Evitar gastar símbolos duas vezes; tratar reservas NPC não gastas com fallback explícito e verificável.
2. Facções Arend/Grayson no Holo-Park com fichas e gatilhos publicados, presença física confirmada, sem duplicar NPCs.
3. Desfechos Arend/Aris/Lando, Obligation Favor +5 somente sob gatilhos oficiais; modo narrativo de dois minutos somente com relógio confirmado.
4. Arrow-7 avançado apenas com atributos comprovados; SWE02 New Meen/Geonosis/Teemo; cotejo cena a cena das cinco aventuras e certificação individual dos 360 consumidores EotE/AoR/expansões.
5. Preservar Kokoro/saves; executar testes focais, regressão por arquivo, seis stress; gerar ZIP completo+SHA+delta+handoff **somente com código real**. Biblioteca cheia: não apagar checkpoints sem autorização; se ZIP integral não couber, entregar no sandbox e não afirmar upload/readback.

**Não declarar conclusão nem homologação até comprovação real.**
