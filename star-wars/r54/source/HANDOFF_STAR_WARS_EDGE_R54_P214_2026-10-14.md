# HANDOFF CANÔNICO — BRASEIRO STAR WARS EDGE R54 P214

## Base obrigatória

**P214 é uma alteração incremental real de P213; NÃO recomeçar.** Base: `/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P213_SOURCE_CHECKPOINT.zip`, SHA-256 `7c9a663cc25c6eb9444d596b6ba4d2c23540d8a74fd2b22c7d5d5d33db19e305`, 98.637.217 bytes, 6.505 arquivos. O P213 e todos os saves legados permanecem preservados. Não houve acesso nem modificação no GitHub.

## Código novo — SWE09, The Jewel of Yavin, pp. 93–94

Consulta direcionada ao PDF oficial do Drive `1z6Kh-0tqRxegzB_U7GL_WxicUQKIOU6K`. **Dois resultados oficiais adicionais foram implementados**, ambos dependentes de Ameaça REAL do ataque NPC e confirmação explícita do Mestre:

1. `alderaan_lost_weapon`: ataque inimigo derruba arma efetivamente empunhada, que fica ocultada na grama holográfica. O Mestre seleciona o índice da arma real da ficha; a arma permanece no inventário, mas `resolveWeaponAttack` rejeita seu uso enquanto caída. Recuperação imediata consome **1 Tensão real**, ou o personagem pode gastar **manobra real na rodada seguinte**. Recuperação exige confirmação física, grava o evento no log persistido e não inventa deslocamento.
2. `kashyyyk_lost_weapon`: pássaros agr holográficos derrubam a arma efetivamente empunhada; mesmo bloqueio/recuperação com confirmação do Mestre. A fonte não estabelece uma penalidade numérica adicional para os pássaros, portanto não foi inventada.

Ambos geram recibos `JEWEL_HOLO_EFFECT` com replay de pool/faces/símbolos, identidade do ataque NPC, índice/nome da arma e bloqueio de duplicatas; o estado `combat.holoParkDroppedWeapon` persiste. Novo método `settleJewelHoloNpcThreat`: a Ameaça NPC que foi reservada, mas não usada no efeito, pode ser **explicitamente** aplicada como a antiga Tensão (ou Ferimentos de minions/rivais) ao NPC atacante, sem duplicar gasto. O fallback é registrado no `combat.log`, não em um novo recibo de operações. Somente a Ameaça da rodada/paisagem/ataque real pode ser consumida. Não há fallback automático silencioso.

Arquivos principais: `app/src/main/assets/vtt/js/adventure/jewelHoloParkEffects.mjs`, `js/core/controller.mjs`, `js/core/uiBridge.mjs`, `js/core/uiBridge.bundle.js`, `r46/final-runtime.js`, `r46-shell.html`, `app/build.gradle.kts`. UI Android PT-BR expõe escolha da arma, confirmação do Mestre, recuperação e fallback. Teste focal: `tests/uae-p214-holopark-lost-weapons.test.mjs` (4 testes). Android **1.0.214**, `versionCode=10214`, bundle offline **104 módulos**. Oito arquivos Java/Kokoro byte-idênticos P213.

## Verificações P214

- **333 arquivos regulares: 3.060/3.060**, seis stress **6/6**, total **3.066/3.066 em 339 arquivos individuais**; quatro testes focais P214. NÃO foi usada suíte monolítica.
- Sintaxe **503 MJS, 2 JS runtime, 22 JSON, 2 XML** PASS. Oito Java/Kokoro idênticos.
- Evidências `evidence/P214/TEST_SUMMARY.json`, `PER_FILE_RESULTS.json`, `SYNTAX_AND_JAVA.json`, `PDF_TARGETED_SOURCE_CROSSCHECK.json`, `IMPLEMENTATION_EVIDENCE.json`.
- **12/16 efeitos do Holo-Park implementados**. Faltam quatro: Christophsis fonte/cobertura, Coruscant Furtividade oposta, Kashyyyk inseto aliado e vertigem.
- Não houve SDK/Gradle/adb/APK, CI BRELGA, homologação física Android, push GitHub ou certificação integral dos PDFs. O ZIP é projeto-fonte Android Studio, NÃO APK.

## Próximas prioridades P215+

1. Implementar quatro efeitos restantes SWE09 pp. 93–94 com símbolos/rolagens/estado reais: Christophsis cobertura na fonte, Coruscant desaparecimento com Furtividade oposta, Kashyyyk inseto (+2 Boost conforme publicação) e vertigem com Disciplina Difícil. Não inventar dados, fichas, deslocamentos ou duração.
2. Integrar facções Arend e Grayson no Holo-Park com fichas/gatilhos oficiais e presença física GM confirmada, sem duplicação ou teleporte. Concluir desfechos Arend/Aris/Lando, `+5 Obligation Favor` somente se gatilhos oficiais.
3. Arrow-7 avançado somente com atributos comprovados; SWE02 New Meen/Geonosis/Teemo; cinco aventuras cena a cena e 360 consumidores EotE/AoR/expansões.
4. Preservar Kokoro e saves; testes focais + regressão global por arquivo + seis stress; ZIP completo+SHA+delta+handoff só após implementação real. Se a Biblioteca permanecer cheia, **não apagar checkpoints sem autorização**, entregar ZIP via sandbox e registrar exatamente o que foi ou não enviado.

**P214 é um checkpoint de fonte e testes, não uma homologação de jogo completo.**
