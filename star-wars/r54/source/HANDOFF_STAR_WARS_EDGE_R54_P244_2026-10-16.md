# HANDOFF — BRASEIRO STAR WARS EDGE R54 P244 — 2026-10-16

**NÃO RECOMEÇAR.** Base íntegra P243: ZIP completo 111.095.373 bytes, 16.130 arquivos, SHA256 `3358e4ee6d180757b842f15c30ace59578f787e4e5f922d8bc6d5a57bd7c85d6`. P243 é o último checkpoint remoto confirmado até que o backup e readback de P244 sejam comprovados.

## Implementação real P244 — custódia de armas da milícia de New Meen

Fonte primária: *The Long Arm of the Hutt* (SWE02), p.21, seção *Taking the Fight to Drombb* e opções *Running NPCs*. O livro permite emprestar armas aos Twi'leks e deixa a resolução do apoio aliado sob controle do Mestre. **A política de devolução é uma salvaguarda do inventário do VTT, não uma regra textual oficial que invente recompensas.**

- `swe02EvacuationP243.mjs`: rastreia cada empréstimo com `ACTIVE`, `LOST` (aliado eliminado no combate abstrato; não necessariamente morto) e `RETURNED`; identifica individualmente a baixa de cada rodada; devolução voluntária apenas da arma exata ainda com aliado sobrevivente, para o personagem proprietário, fora de combate e antes de deixar New Meen; armas de aliados eliminados **não** voltam automaticamente. Registra recibo de devolução e impede duplicações.
- Compatibilidade de saves P243: empréstimos antigos sem status são classificados determinística e conservadoramente conforme perdas já documentadas, antes de nova operação. Nunca converte baixa em recompensa. Validador confere soma de efetivos, recibos, IDs únicos, estados e propriedade quando há recibo P244.
- `core/controller.mjs` integra a transação ao `atomicAction`, `CHARACTER_UPDATED` e `WORLD_UPDATED`; `core/uiBridge.mjs`, `ui/app.mjs`, `index.html` oferecem controle PT-BR de devolução, incluindo seleção somente das armas recuperáveis do personagem.
- Testes específicos `tests/uae-p244-swe02-militia-custody.test.mjs`: devolução única, rollback, baixa, compatibilidade P243, adulteração, propriedade, UI/bridge e três reaberturas. Os 16 testes P243 foram preservados e reexecutados.
- Android somente **SOURCE 1.0.244 / versionCode 10244**. Bundle offline recompilado. **NÃO** houve APK/Gradle/CI/GitHub push/ADB ou aparelho físico.

## Limites e prioridades

1. A devolução cobre apenas **aliados sobreviventes**; recuperação de armas de aliados eliminados exigirá um evento de busca e validação separado, ainda não certificado. Iniciativa completa dos Twi'leks e os assédios da estrada também permanecem pendentes.
2. SWE02: Nyn granadas/fuzil e defeitos na primeira utilização, combustível/carga/relógio, Krayt Fang Beginner/iniciativa veicular, restante dos atos 2/3 e finais. Conferir integralmente PDF SWE02 e manter a matriz de cobertura.
3. Outras quatro aventuras: Under a Black Sun, Beyond the Rim, The Jewel of Yavin e Mask of the Pirate Queen. EotE/AoR/expansões e 360 consumidores AoR precisam de certificação individual fonte→página→executor→UI→persistência/três reaberturas→teste. **Testes estruturais não bastam para certificar conteúdo editorial.**
4. Nenhum APK ou teste físico antes de fechar regras e cinco aventuras. Preservar PT-BR, Motor Bárbara, TTS/Kokoro externo, mundo vivo, saves, companheiros, Galaxy A57/S23 Ultra.

## Integridade e backup

Consultar `evidence/P244/regression_summary.json`, `evidence/P244/syntax_preservation.json`, `validation/P244_SWE02_SOURCE_PROOFS.json` e `/mnt/data/P244_INTEGRITY_REPORT.json`. A promoção remota de P244 exige upload das partes <=32MiB ao Drive `BRASEIRO_STAR_WARS_CHECKPOINTS` ID `1qTEzIaJi1Ot3jqjVpKFpIvP4oRwJg1an`, listagem e **readback real das quatro partes com SHA individual, reconstrução ZIP SHA/CRC e comparação byte a byte**. Se falhar, manter P243 remoto como canônico. Nenhuma exclusão autorizada.
