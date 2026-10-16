# HANDOFF CANÔNICO — BRASEIRO STAR WARS VTT R54 P194

**Base:** ZIP integral P193 `BRASEIRO_STAR_WARS_EDGE_R54_P193_SOURCE_CHECKPOINT.zip`, SHA256 `4ab8e39b8401a0bbfd5022f51184f5ab146d6da9fa906e66fefd8407063ee45a`, 98.227.998 bytes, 6.316 arquivos ZIP CRC aprovado. **Nunca reconstruir de P192 ou anterior.**

**Entregáveis P194:** `BRASEIRO_STAR_WARS_EDGE_R54_P194_SOURCE_CHECKPOINT.zip` (Android Studio completo), `.zip.sha256`, `BRASEIRO_STAR_WARS_EDGE_R54_P193_TO_P194_DELTA.zip` (manifesto/hash de reconstrução) e este handoff. SHA final do ZIP está no arquivo externo `.zip.sha256`; não embutir hash autorreferente.

## Implementação real P194 — SWE09 "Leave No Trace"

1. **Desmontagem do G-100:** `jewelBankingOperations.mjs` mantém teste de Mecânica Desafiadora (4 dados de Dificuldade), mas novas tentativas agora registram **120 minutos** no `world.clock.elapsedMinutes` e `world.taskTimeApplications`, inclusive falha. Recibo `BANK_STEP` possui `cleanupVersion:2` e `timeCheckId=task_jewel_cleanup_<actionId>`; replay valida versão/tempo, ausência de tarefas órfãs, faces e símbolos. P193 sem `cleanupVersion` mantém comportamento legado sem débito retroativo. O livro diz duas ou mais horas; 120 minutos é o mínimo adotado, sem prometer sincronização dia/noite.
2. **Destino real da caixa-preta e das peças:** novo `jewelBankEvidence.mjs` implementa recibos `BANK_EVIDENCE`: `destroy_box`, `take_box` (risco de prova incriminadora), `dump_parts` (exige confirmação de viagem até a borda de Cloud City, **não** teleporta/avança viagem) e `fence_parts` (Manha/Streetwise Difícil, rolagem narrativa real, sem créditos inventados). Escolhas exigem caixa-preta previamente extraída; falha ao vender peças mantém a pendência. Validação de ordem, ação duplicada, campos indevidos, faces e resultados. `bank.evidenceCleared` é nome legado de extração: não prova que todas as evidências foram destruídas; usar `operations.bankEvidence` para decisões de destino.
3. **Integração:** `jewelOfYavin.mjs`, `publishedAdventureRegistry.mjs`, `GameController.resolveJewelBankEvidence` e `uiBridge.mjs` expõem as decisões; `r46/final-runtime.js` apresenta botões PT-BR, risco e confirmação GM. `jewelOfYavinOperations.mjs` aceita novo tipo de recibo sem relaxar replay. Android **1.0.194**, versionCode **10194**, bundle offline reconstruído **92 módulos**. Oito Java, incluindo TTS/Kokoro, byte a byte idênticos P193.
4. **PDF oficial:** *The Jewel of Yavin* SWE09, Google Drive ID `1z6Kh-0tqRxegzB_U7GL_WxicUQKIOU6K`, seção "Leave No Trace" junto de "Banking on a Break". Conferência **direcionada**, não integral: Mecânica Desafiadora e >=2h; caixa-preta roubada/destruída; descarte em receptadores com Manha Difícil ou na borda de Cloud City. Alternativa do droid vivo (Charme vs Frieza e alojamento na nave) ainda **não implementada**. `evidence/P194/PDF_TARGETED_SOURCE_CROSSCHECK.json`.

## Testes e limites

- **313 arquivos regulares: 2855/2855**; **6 stress sequenciais: 6/6**; **total 2861/2861 em 319 arquivos por execução individual**, seis workers regulares. **Não declarar suíte monolítica verde.**
- Nove testes novos P194 (`tests/uae-p194-jewel-g100-black-box-evidence.test.mjs`): sucesso/falha e tempo, ordem de evidências, dados, ausência de pagamentos fictícios, corrupção, reaberturas e compatibilidade P193. Duas asserções de versões obsoletas nos testes P191/P192 foram ajustadas para aceitar versões posteriores sem alterar semântica.
- Sintaxe: **126 MJS, 22 JSON, 2 XML**; nenhum erro. Evidências: `evidence/P194/TEST_SUMMARY.json`, `PER_FILE_RESULTS.json`, `IMPLEMENTATION_EVIDENCE.json`, `PDF_TARGETED_SOURCE_CROSSCHECK.json`.
- **Sem SDK/Gradle/adb, CI BRELGA, APK ou homologação física.** Não há cotejo editorial integral das cinco aventuras nem certificação semântica dos 360 consumidores AoR/EotE/expansões.

## Próxima prioridade P195+

1. Integrar alternativa do droid vivo com oposição Charme vs Frieza e berth confirmado; manter mutuamente exclusiva com desmontagem, com recibos, custo/tempo apenas quando comprovados pelo PDF.
2. Substituição integral das ferramentas de slicing usando itens reais de inventário e fonte; **não** confundir `restraining_device` com restraining bolt. Implementar Covering Fire/Tactical Direction no combate com testes.
3. Finalizar liquidação bancária, consequências da prova/risco Naboka, traição e fuga SWE09; SWE02 New Meen/Geonosis/Teemo com encontros oficiais.
4. Comparar cinco PDFs cena a cena e certificar semanticamente os 360 consumidores AoR/EotE/expansões individualmente; não declarar certificação por contagem.
5. Rodar testes focais, regressão global por arquivo e seis stress; preservar Java Kokoro. Só gerar novo ZIP completo + SHA256 + delta + handoff após avanço real. Salvar Biblioteca e conferir readback SHA/CRC. GitHub BRELGA apenas com execução verificável; jamais inventar CI/APK.

**Manter continuidade horária até homologação comprovada. Não regredir P193.**
