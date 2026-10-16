# HANDOFF CANÔNICO — BRASEIRO STAR WARS VTT R54 P190

**Base canônica:** P189 ZIP integral SHA256 `33e22affb6812d6b9d54ea28da8f9fb9aec3c04a54d7264d2098d0cde73349e5`, 97.947.071 bytes, 5.649 arquivos. **Destino:** `BRASEIRO_STAR_WARS_EDGE_R54_P190_SOURCE_CHECKPOINT.zip` integral; tamanho, contagem e SHA-256 constam do arquivo `.zip.sha256` externo e do manifesto delta. Não regredir ao P189.

## Implementação real P190 — SWE09 *The Jewel of Yavin*, slicers NPC de Port Town

1. **Fonte consultada diretamente no Google Drive:** PDF oficial SWE09, arquivo `1z6Kh-0tqRxegzB_U7GL_WxicUQKIOU6K`, seção sobre contratar slicers e preparação da rede, em torno da página impressa 40. O livro estabelece teste de **Streetwise Médio** para localizar contatos em Port Town; **R3-8C** tem Intelecto/Computadores **3/3**, custo **500–1.000 créditos** e Negociação **Difícil** com o proprietário; **Naboka** tem **4/5**, proposta inicial de **3.000** e mínimo de **2.000 créditos**. O livro não estabelece fórmula fixa preço/sucessos: preço final deve ser confirmado pelo Mestre dentro do intervalo oficial. A possível traição de Naboka NÃO foi automatizada.
2. **`jewelHiredSlicers.mjs` novo:** teste narrativo real de Manha/Streetwise, Negociação real para R3-8C após encontrar o contato, resultado derivado de faces verificadas; falha na busca não contrata nem cobra. Confirmação explícita de contato em Port Town e preço pelo Mestre. Débito atômico de créditos, recibo `BANK_SLICER_HIRE`, replay determinístico, proteção de ID e validação contra adulteração de preço, faces, custo, atributos, origem e histórico.
3. **Slicing efetivo pelo NPC:** `jewelBankingOperations.mjs` aceita `useHiredSlicer` apenas na etapa `slice`, usa de verdade o pool de Computadores/Intelecto publicado de R3-8C ou Naboka e registra identidade e atributos no recibo `BANK_STEP`. O replay rejeita NPC não contratado, atributos/identidade falsificados, resultados forjados; não permite NPC em captura/neutralização/limpeza. Fluxo legado P189 continua sem contratar ninguém.
4. **Integração Android PT-BR:** `GameController.hireJewelBankSlicer`, `uiBridge.hireJewelBankSlicer`, operação `bank_hire_slicer`, UI na cena bancária com seleção de NPC, preço, confirmação de contato/Mestre e opção explícita de usar o contratado no slicing. Status mostra o slicer e o valor pago. Bundle offline **90 módulos**; Android **1.0.190**. **8/8 arquivos Java/Kokoro byte a byte idênticos ao P189**.
5. **Compatibilidade:** ação atômica, idempotência, persistência do recibo, três reaberturas e saves P189 sem contratação preservados. O pagamento é debitado no personagem real, sem gerar créditos ou recompensas fictícias.

## Testes e limites

- `tests/uae-p190-npc-slicer-hiring-and-bank-integrity.test.mjs`: **16/16** testes novos.
- `evidence/P190/TEST_SUMMARY.json`: **2.804/2.804** regulares em **309** arquivos, **6/6** stress sequenciais, **2.810/2.810** em **315** arquivos por execução individual. Duas regressões de testes obsoletos (versão fixa P189 e limite de 89 módulos) foram corrigidas e reexecutadas. **NÃO** declarar suíte monolítica verde.
- Sintaxe: **124 MJS, 22 JSON, 2 XML**; Java/Kokoro **8/8 idênticos**.
- **Ainda pendente:** restraining bolt com item realmente possuído (não confundir com `restraining_device`); Tabela 2-1, pagamento bancário, prova de rastreamento, traição/fuga, SWE02 New Meen/Geonosis/Teemo, cotejo integral das cinco aventuras e certificação semântica dos 360 talentos AoR/EotE/expansões. Não houve APK, SDK/Gradle/adb, CI BRELGA ou homologação física.

**Próximo P191+:** seguir com restraining bolt apenas após cotejo com catálogo oficial; Tabela 2-1 com custos narrativos e consequências reais; operação bancária e fuga; SWE02; cinco aventuras cena a cena; regras AoR/EotE/expansões. Preservar código e Kokoro, testes focal + global por arquivo + stress, ZIP Android Studio integral + SHA + delta + handoff e upload/readback Biblioteca. Nunca inventar fontes, testes, CI, APK ou conclusão.
