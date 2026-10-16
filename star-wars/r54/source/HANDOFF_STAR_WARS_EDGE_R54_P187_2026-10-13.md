# HANDOFF CANÔNICO — BRASEIRO STAR WARS VTT R54 P187

**Base verificada:** P186 ZIP SHA256 `8fcd660751403092f0828c0abbd9050a6b55428177a2ec103b46a6173bd617ba` (97.590.861 bytes). Preservar todas as correções e oito fontes Java/Kokoro.

## Implementação real P187

- SWE09 *The Jewel of Yavin*, episódio II, Banco Central: módulo novo `app/src/main/assets/vtt/js/adventure/jewelBankingOperations.mjs`, com aquisição do droid bancário remoto G-100 em três postos oficiais (dificuldade por local, melhoria quando não contido), incapacitação por Computadores, hacking do banco contra a subrotina de Lobot (Intelecto 5, Computadores 2), upgrade por alarme e por tentativas falhas, máximo de quatro tentativas e 30 minutos por repetição, desmontagem da caixa-preta com Mecânica Desafiadora.
- Rolagens narrativas reais, pool positivo do personagem e pool negativo de oposição, dados e faces registrados, reexecução determinística dos recibos, rejeição de adulterações, actionId idempotente, operações atômicas com rollback e três reaberturas. O sucesso só marca desvio de transferência após leilão concluído; vencedor Pos Podura não rende fundos.
- Contrato P187 de novas partidas impede transformar um teste narrativo manual em dinheiro bancário; migração de saves anteriores sem `bankContractVersion` conserva o caminho antigo e seus resultados já registrados. P183 test fixture foi explicitamente marcado como legado, sem relaxar a regra das novas partidas.
- Integração `publishedAdventureRegistry.mjs`, `GameController.resolveJewelBankStep`, `GameController.adjudicateJewelOperation('bank_step')`, `BraseiroUiBridge.resolveJewelBankStep`; status Android exibe banco, bundle offline agora **88 módulos**.
- Android `versionCode=10187`, `versionName=1.0.187`; shell e diagnóstico sincronizados. Oito arquivos Java de Kokoro/TTS byte a byte idênticos P186.

## Evidências e testes

- `tests/uae-p187-bank-droid-slicing-integrated.test.mjs`: **13/13**, captura, dificuldade, alarme, hacking, retries/lockout, limpeza, fraude, flags, rollback, saves legados, três reaberturas e Android bridge.
- `evidence/P187/TEST_SUMMARY.json`: **2764/2764** testes regulares em 306 arquivos (6 shards por arquivo) + **6/6** stress = **2770/2770** em 312 arquivos; **não** é uma suíte monolítica certificada. Duas falhas de versão Android foram corrigidas e reexecutadas.
- Sintaxe: 122 MJS, 22 JSON, 2 XML. Bundle offline reconstruído a partir das fontes canônicas.
- `evidence/P187/PDF_TARGETED_SOURCE_CROSSCHECK.json`: cotejo **direcionado** do PDF do Drive SWE09, capítulo *Banking on a Break* pp.68–72; não é certificação integral do livro.

## Limites explícitos

- Restraining bolt é um sinal confirmado pelo Mestre; não há ainda verificação/consumo do item no inventário.
- Ainda não estão implementados os slicers NPC, computer spikes, amostras de criptografia, neutralização de Lobot e todas as opções de gastos de símbolos da Tabela 2-1.
- Nenhum crédito é concedido automaticamente; desvio bancário persiste como estado, sem liquidação financeira.
- Cinco aventuras e expansões EotE/AoR **não** foram cotejadas integralmente cena a cena. 360/360 consumidores AoR não equivale a certificação.
- Sem SDK/Gradle/adb funcional nem APK/CI BRELGA ou homologação Android comprovados neste ambiente.

## Próximo bloco P188+

1. Completar capítulo do Banco Central: NPC slicers, equipamento, criptografia, Lobot, consequências da Tabela 2-1 e limpeza de evidências; vincular item físico do restraining bolt.
2. Fechar SWE09 fuga, rivais e traição; SWE02 New Meen/Geonosis/Teemo; depois as outras três aventuras.
3. Cotejar cinco PDFs e expansões EotE/AoR semanticamente, corrigir divergências com provas individualizadas.
4. Testar regressão integral por arquivo + stress, preservar Kokoro, gerar ZIP completo Android Studio + SHA/delta/handoff, upload/readback Biblioteca.
5. Compilar APK via GitHub BRELGA somente quando CI realmente executado e instalar/testar em Android real. Nunca inventar aprovação ou conclusão.
