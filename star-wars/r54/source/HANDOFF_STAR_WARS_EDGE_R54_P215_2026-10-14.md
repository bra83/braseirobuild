# HANDOFF CANÔNICO — BRASEIRO STAR WARS EDGE R54 P215

## Autoridade de retomada

**P215 continua P214, não reiniciar.** Base: `/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P214_SOURCE_CHECKPOINT.zip`, SHA-256 `3c82f8fb52adc8b531f211cdeb6515bf10f44ccc31d9fd8ff7240b42bd916639`, 98.657.623 bytes e 6.515 arquivos. O código-fonte Android Studio completo P215 e seu SHA/delta serão indicados no manifesto final de empacotamento. Todos os recibos e saves P214 anteriores continuam compatíveis.

## Implementação REAL P215 (SWE09, The Jewel of Yavin, pp. 93–94)

PDF oficial lido **direcionadamente** via conector Google Drive, ID `1z6Kh-0tqRxegzB_U7GL_WxicUQKIOU6K`, texto das páginas impressas 93–94. **Dois dos quatro efeitos pendentes foram implementados**, não declarar os outros dois como prontos:

1. `christophsis_fountain`: **Desespero REAL** de ataque NPC, reservado com dados/faces verificáveis, Mestre confirma NPC presente e a fonte. O NPC real ganha **Defesa à distância 2** até sua manobra de movimentação física confirmada pelo Mestre; a Defesa afeta o pool de dados dos ataques do jogador contra aquele NPC, não ataques corpo a corpo. `confirmJewelHoloNpcFountainMovement` remove o efeito e registra a movimentação. Não move NPC automaticamente.
2. `coruscant_vanish`: Desespero REAL de ataque NPC, Mestre seleciona NPC presente; teste de **Furtividade NPC contra Percepção do personagem jogador** utiliza características/perícias de fichas reais e rolagem de dados narrativos com faces verificadas. **Se o teste for bem-sucedido**, o NPC fica oculto na multidão holográfica e não pode ser escolhido como alvo de `resolveWeaponAttack` até `confirmJewelHoloNpcVisibility` receber confirmação do Mestre sobre linha de visão efetiva. Nenhum teleporte, desaparecimento físico ou sucesso automático. Se falhar, não há ocultação.

O motor reserva **Desespero** separadamente de **Ameaça** (`holoParkPendingNpcDespair`) e subtrai ambos do antigo fallback automático de Tensão/Ferimentos para evitar gasto duplo. Desespero não utilizado pode ser aplicado explicitamente pelo Mestre por `settleJewelHoloNpcDespair`, com efeito real no NPC, log persistente e bloqueio de duplicação. Recibos `JEWEL_HOLO_EFFECT` mantêm a mesma versão de contrato, agora validando `gmTargetPresentConfirmed`, identidade, pool, faces e símbolos do NPC e, no caso da Furtividade, características, pool e faces da oposição. Operações atômicas, rollback e três reaberturas comprovadas. UI Android PT-BR, controller, bridge e bundle offline integrados.

**Cobertura Holo-Park: 14/16 (87,5%).** Ainda faltam `kashyyyk_insect` (Triunfo do jogador, +2 Boost **na mesma rolagem de combate**, não inventar efeito no próximo ataque) e `kashyyyk_vertigo` (Desespero NPC, Disciplina Difícil, se falhar 2 Tensão e downgrade do pool da **próxima ação**). Exigem implementação na resolução efetiva de rolagem/ação, com recibos, sem adulterar resultado já resolvido.

## Testes P215

- **334 arquivos regulares, 3.064/3.064**, mais **6 stress, 6/6**: **3.070/3.070 em 340 arquivos**. Arquivos executados individualmente em quatro shards não monolíticos; os seis stress também reexecutados sequencialmente. Quatro testes focais P215. Falhas de testes de versão observadas antes de atualizar `r46-shell.html` foram corrigidas; regressão FINAL 100% verde.
- Sintaxe **504 MJS, 2 JS runtime, 22 JSON, 2 XML** aprovada. **8/8 Java/Kokoro byte-idênticos a P214**. Bundle offline **104 módulos**.
- Android `versionName=1.0.215`, `versionCode=10215`. Evidências `evidence/P215/TEST_SUMMARY.json`, `PER_FILE_RESULTS.json`, `SYNTAX_AND_JAVA.json`, `PDF_TARGETED_SOURCE_CROSSCHECK.json`, `IMPLEMENTATION_EVIDENCE.json`, quatro `shard-*.json`.

## Pendências e restrições

1. **P216 prioritário:** concluir os dois efeitos de Kashyyyk com pools de dados reais e sem usar bônus na rolagem errada; teste focal, regressão global individual e seis stress.
2. Integrar facções Arend e Grayson no Holo-Park apenas com fichas e presença reais confirmadas, sem teleporte ou NPC duplicado; desfechos Arend/Aris/Lando, `+5 Obligation Favor` somente com gatilhos oficiais.
3. Arrow-7 avançado apenas com ficha/atributos oficiais; SWE02 New Meen/Geonosis/Teemo; cotejo integral cena a cena de cinco aventuras e 360 consumidores EotE/AoR/expansões.
4. Preservar Kokoro e saves; gerar ZIP Android Studio completo+SHA+delta+handoff após implementação real. Não apagar checkpoints da Biblioteca sem autorização. Se Biblioteca continuar cheia, entregar ZIP sandbox e **não alegar upload/readback que não ocorreu**.
5. **Não houve SDK/Gradle/adb/APK/CI BRELGA/homologação física, push GitHub ou certificação integral dos PDFs.** P215 é checkpoint de código-fonte e testes, não VTT completo homologado.
