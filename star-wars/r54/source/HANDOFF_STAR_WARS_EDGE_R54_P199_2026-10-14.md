# HANDOFF CANÔNICO — BRASEIRO STAR WARS VTT R54 P199

**BASE PRESERVADA:** P198, ZIP completo SHA-256 `f08e2852483f1e055a4da3e13f2901b5354df1dd96e997ac07e47810aa6460a2`, 98.320.543 bytes, 6.364 arquivos. P199 é uma continuação do mesmo projeto Android Studio, não uma recriação. Os hashes exatos do ZIP P199 e do delta P198→P199 são fornecidos nos arquivos `.zip.sha256` externos, no manifesto do delta e na resposta de entrega.

## P199 — IMPLEMENTAÇÃO REAL: equipamentos bancários com inventário e fonte oficial

**PDFs consultados diretamente no Google Drive em 14/10/2026:** `EotE Core Rulebook.pdf`, ID `1UO3Z35J2Dmkfvx1OLyPD3rzJon013EoX`, tabela 5-8, p. impressa 179 e descrições de equipamentos; `The Jewel of Yavin.pdf` SWE09, ID `1z6Kh-0tqRxegzB_U7GL_WxicUQKIOU6K`, seção *Banking on a Break*, p. impressa 69. **Cotejo direcionado, não certificação integral.**

1. `restraining_bolt` **não é** `restraining_device`. Novo item oficial: **35 créditos, carga 0, raridade 0**. Compra opcional aprovada pelo Mestre debita créditos de verdade, adiciona o bolt físico ao inventário e cria recibo `BANK_BOLT_BUY` validável por replay. Nenhum item é concedido gratuitamente. O catálogo EotE foi ampliado para 62 itens pessoais (31 armas + 7 armaduras + 24 equipamentos).
2. A captura do G-100 com o benefício do bolt exige **posse real de `restraining_bolt`**, e também soldador de ponto (o kit de ferramentas real contém soldadores; alternativamente o Mestre confirma um soldador externo). A captura usa os dados reais e, quando bem-sucedida, **retira uma unidade do bolt da mochila e a vincula ao G-100**, aplicando a redução oficial de duas melhorias de dificuldade na incapacitação. Falha na captura não inventa perda do item. Recibos `BANK_STEP` com `boltInventoryVersion=2`, ID, ator e origem do soldador são replay-validados. Contrato `bankEquipmentContractVersion=1` obriga novos saves a comprovar o item; **saves P198 sem o contrato/recibo novo permanecem válidos**, sem cobrança retroativa.
3. Novo item `slicer_gear` oficial: **500 créditos, carga 2, raridade 4**. Após a captura da assinatura eletrônica da Tabela 2-1, o jogador pode, com confirmação comercial do Mestre, **comprar um conjunto completamente novo**, debitar 500 créditos, substituir a unidade antiga se possuída, e registrar recibo `BANK_TOOL_REPLACE`. O efeito é real: o upgrade da assinatura deixa de afetar o próximo slicing G-100 e outros testes de slicing identificados de Cloud City. A assinatura capturada continua **histórica e potencialmente incriminadora**; a substituição não apaga as provas. Repetição, custo adulterado, recibos fora de ordem e créditos insuficientes são rejeitados com rollback.
4. Controller, adapter, bridge e UI Android PT-BR integrados: compra de bolt, confirmação do soldador, aplicação física, confirmação de disponibilidade de novo kit e substituição. Android **1.0.199**, `versionCode=10199`, bundle offline **95 módulos**. Os **8 arquivos Java incluindo Kokoro/TTS são byte a byte idênticos ao P198**.

## Evidências e testes P199

- Novo arquivo `tests/uae-p199-physical-bolt-and-slicer-tool-replacement.test.mjs`: **20 testes focais** (catálogo, compra, soldador, captura, custos, pools, falsificação, substituição, rollback, legados, 3 reaberturas, bridge/UI). Cinco testes legados de banco tiveram **somente fixtures** atualizadas para possuir bolt/kit físico, sem relaxar as expectativas mecânicas. Um teste de catálogo R37 teve contagens atualizadas de 60 para 62, pelos dois itens oficiais novos.
- **318 arquivos regulares: 2.926/2.926**; **6 stress executados separadamente: 6/6**; **TOTAL 2.932/2.932 em 324 arquivos**. Execução `node --test --test-reporter=tap` por arquivo, **NÃO suíte monolítica verde**. Resultados `evidence/P199/PER_FILE_RESULTS.json`, `TEST_SUMMARY.json`.
- Sintaxe: **129 MJS, 22 JSON, 2 XML, 2 JS** válidos. Preservação Java em `JAVA_PRESERVATION.json`; fonte direcionada em `PDF_TARGETED_SOURCE_CROSSCHECK.json`; mudanças e limites em `IMPLEMENTATION_EVIDENCE.json`.

## LIMITES NÃO HOMOLOGADOS

- A disponibilidade comercial exige confirmação do Mestre; não há geração arbitrária de loja. A instalação do bolt não cobra tempo artificial; fonte menciona segundos e soldador. Não se infere que `tool_kit` por si só fornece uma unidade separada de bolt em estoque.
- O mecanismo de substituição do kit é um caminho real de **compra**; ferramentas obtidas por saque/negociação sem recibo próprio ainda não podem ser usadas automaticamente para apagar a assinatura. A evidência da assinatura continua existindo. Não foi implementada captura de múltiplas assinaturas distintas após a primeira.
- Não há liquidação final dos créditos do banco, traição Naboka, fuga SWE09, conclusão SWE02 New Meen/Geonosis/Teemo ou certificação integral cena a cena das cinco aventuras e 360 consumidores AoR/EotE/expansões.
- **Não há APK, Gradle/SDK/adb, execução CI BRELGA nem homologação física Android comprovados.** O repositório GitHub `bra83/braseirobuild` é acessível, mas este checkpoint local não foi enviado a um branch nem houve CI disparado.

## P200+ PRIORIDADE

1. SWE09: **liquidação bancária real** baseada em PDF (conta de Arend, vencedor, licitantes adicionais, evidências e consequência de falha), sem creditar dinheiro automaticamente antes de prova de pagamento.
2. SWE09: traição de Naboka e Arend, consequências da assinatura, perseguição e fuga, integradas ao relógio e combate real.
3. SWE02: New Meen, Geonosis, Teemo, cenas e testes reais.
4. Comparar cinco aventuras cena a cena e 360 consumidores EotE/AoR/expansões individualmente, com evidência; preservar todos os saves legados e Kokoro.
5. Testar focal+regressão individual+seis stress, criar ZIP Android Studio completo+SHA+delta+handoff apenas após avanço real, salvar na Biblioteca e fazer readback SHA/CRC. Tentar BRELGA/Gradle apenas com fonte presente e acesso real; não inventar APK, CI, certificação ou conclusão.

**A tarefa horária deve permanecer ativa até homologação comprovada.**
