# HANDOFF CANÔNICO — BRASEIRO STAR WARS VTT R54 P191

**Base de origem preservada:** `BRASEIRO_STAR_WARS_EDGE_R54_P190_SOURCE_CHECKPOINT.zip`, SHA-256 `b1ce1fd8ade5e65f4e9f9a9f54c3743719d546ff37a120b18287bb22c0c3daaa`, 98.067.458 bytes, 5.973 arquivos. **Checkpoint novo:** `BRASEIRO_STAR_WARS_EDGE_R54_P191_SOURCE_CHECKPOINT.zip`; SHA, tamanho e contagem devem ser lidos do `.zip.sha256` externo e do manifesto delta. Não regredir a P190.

## Implementação REAL P191 — SWE09 *The Jewel of Yavin*, Tabela 2-1

**Fonte diretamente recuperada e visualmente conferida:** PDF oficial *The Jewel of Yavin* no Drive, ID `1z6Kh-0tqRxegzB_U7GL_WxicUQKIOU6K`, 98 páginas; Tabela 2-1 na página 70 do PDF, impressa 69. Evidência `evidence/P191/PDF_TARGETED_SOURCE_CROSSCHECK.json`. Não é certificação integral do livro.

1. **`jewelBankSlicingSpends.mjs` novo:** o jogador escolhe os gastos **depois de ver o resultado real** do slicing, nunca antes. Um resultado de Vantagem gera um dado de Reforço para a próxima tentativa; dois resultados de Vantagem permitem um upgrade positivo para a próxima tentativa; dois resultados de Ameaça acrescentam quinze minutos de slicing. Opções repetíveis somente dentro dos símbolos efetivamente rolados, com custos exatos e limites de dados.
2. **Motor bancário integrado:** `jewelBankingOperations.mjs` aplica os Reforços e upgrades ao **pool real** da próxima rolagem e os consome uma única vez; `BANK_SLICE_SPEND` mantém recibo verificável, rejeita faces adulteradas, escolha sem símbolos, etapa inexistente, ID duplicado, campos extras e falsificação de bônus. O atraso gera `task_jewel_slicing_delay_<id>` no `world.taskTimeApplications` e soma tempo efetivo em `world.clock.elapsedMinutes`; replay verifica o ledger. Não inventa pagamento, saldo bancário nem recompensa.
3. **Android PT-BR:** adapter UAE, `GameController.spendJewelBankSymbols`, `BraseiroUiBridge.spendJewelBankSymbols` e painel `final-runtime.js` oferecem escolhas explícitas após o slicing e mostram bônus pendentes/atrasos. Bundle offline **91 módulos**; Android **1.0.191**. Os oito fontes Java/Kokoro foram comparados byte a byte com P190, sem mudanças.
4. **Compatibilidade:** recibos e saves P190 sem `BANK_SLICE_SPEND` continuam válidos. O novo recibo tem replay determinístico e a operação do controlador usa rollback atômico; três reaberturas verificadas nos testes focais.

## Testes e limites

- `tests/uae-p191-bank-table-2-1-post-roll-backdoors-time.test.mjs`: **14 testes novos** cobrindo pool real, consumo, limites, tempo, adulteração, três reaberturas, saves legados e ponte Android.
- `evidence/P191/TEST_SUMMARY.json`: **2.818/2.818** testes regulares em **310** arquivos (seis shards), **6/6** stress, **2.824/2.824** no total em **316** arquivos; todos os 14 testes P191 aprovados; 125 MJS, 22 JSON, 2 XML sintaticamente válidos. Execução individual por arquivo, sem afirmar suíte monolítica verde.
- `evidence/P191/IMPLEMENTATION_EVIDENCE.json` e `PDF_TARGETED_SOURCE_CROSSCHECK.json`: rastreabilidade e limites.
- **Não implementados da Tabela 2-1:** +10.000 créditos por licitante adicional (3 Vantagens/Triunfo), Tensão do slicer (1 Ameaça), patrulha Wing Guard (3 Ameaças/Desespero), assinatura eletrônica (Desespero). Não declarar Tabela 2-1 integral.
- **Outras pendências:** restraining bolt com item de inventário verificado (não confundir com `restraining_device`); liquidação real e limpeza de evidências do banco, traição/fuga, SWE02 New Meen/Geonosis/Teemo, cotejo integral dos cinco PDFs e certificação semântica dos 360 AoR/EotE/expansões. Não houve APK, SDK/Gradle/adb, CI BRELGA ou homologação física.

**Próximo P192+:** completar o restante da Tabela 2-1 com aplicação de Tensão e eventos de patrulha, assinatura de slicer e fundos condicionados ao leilão; vincular bolt ao inventário somente após cotejo oficial; terminar banco/traição/fuga e SWE02; auditar cinco aventuras cena a cena e regras AoR/EotE. Rodar testes focais + globais por arquivo + seis stress, preservar Kokoro, ZIP Android Studio completo + SHA + delta + handoff, upload Biblioteca e readback SHA/CRC. Não inventar implementação, testes, CI, APK ou conclusão. Manter tarefa ativa até homologação comprovada.
