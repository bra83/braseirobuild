# HANDOFF — BRASEIRO STAR WARS EDGE R54 P243 — 2026-10-15

**NÃO RECOMEÇAR.** Base canônica P242, ZIP Android Studio completo SHA256 `97a5c6165f6f7cb85948650240c61fb1ff21d8a68d1ee8d9cc1ff5b9100fd54b`, 15.752 arquivos, CRC PASS, backup remoto em quatro partes com readback anterior confirmado. O P243 preserva todos os arquivos da base, não apaga regras, aventuras, saves, Motor Bárbara, PT-BR ou Kokoro externo.

## Megalote real SWE02 — New Meen, pp.20–21

Fonte primária: https://images-cdn.fantasyflightgames.com/filer_public/80/93/80935c12-24ac-442b-b423-687d4e28ae89/long_arm_of_the_hutt_lr.pdf. **Dois subsistemas executáveis**:

1. **Escolta New Meen → Nabat:** `adventure/swe02EvacuationP243.mjs` e `adventure/longArmOfTheHutt.mjs` criam estados ASSEMBLED/EN_ROUTE/ARRIVED/ABANDONED; 20 adultos, crianças sem quantidade inventada; ticks de viagem **aprovados pelo Mestre** (o PDF não informa duração); executor `advanceLivingWorld` efetivamente avança o relógio e eventos mundiais. Chegada concede apenas o favor significativo de B'ura B'an, não `newMeenOutcome=SECURED`, créditos ou PE. A rota antiga de saída direta continua, agora explicitamente classificada como abandono sem favor. Recibos, validação e três reaberturas.
2. **Milícia Twi'lek armada:** transferência atômica de blaster real do inventário do personagem para miliciano, limitada aos 20 adultos; referência ao perfil dos capangas humanos/Aqualish oficiais. Ação opcional **de combate abstrato** descrita na p.21, acionada somente por aprovação do jogador: um Ferimento por aliado armado ao grupo de capangas ativo e um aliado perdido por fogo de resposta; apenas uma vez por rodada, não funciona em Drombb nem depois do desfecho. Sem fabricar rolagem, aliados não agem autonomamente. Não se trata de implementação completa de iniciativa de NPCs.

`core/controller.mjs`, `core/uiBridge.mjs`, `ui/app.mjs`, `index.html` conectam as operações ao VTT Android offline; painéis de escolta/milícia aparecem também sem nave ativa. `app/build.gradle.kts` e diagnóstico `r46-shell.html`/`r46/final-runtime.js` atualizados para **SOURCE 1.0.243/code 10243**. Bundle offline recompilado, **127 módulos**.

## Gates executados nesta sessão

- Teste focal `tests/uae-p243-swe02-escort-militia.test.mjs`: **16/16 PASS**, cobrindo preparação, partida, relógio, chegada, abandono, rota legada, adulteração, transferência de arma, grupo hostil, limite por rodada, rollback, bridge/UI e três reaberturas.
- Regressão GLOBAL por arquivo **368 arquivos**: 362 regulares/**3.334 PASS** + seis stress/**6 PASS** = **3.340 PASS, 0 FAIL, 0 SKIP**. Execução final repetida depois dos ajustes de UI e versão; logs `evidence/P243/test-results/*.tap`, resumo `evidence/P243/regression_summary.json`.
- Sintaxe **557 MJS, 4.221 JSON, 2 XML, 2 JS runtime** PASS; oito Java/Kokoro byte-idênticos ao P242. `evidence/P243/syntax_preservation.json`.
- **NÃO** houve APK, Gradle, CI BRELGA, GitHub push, ADB ou aparelho físico; isso só acontece nos gates finais.

## Pendências e critérios de não certificação

Ver `validation/P243_SWE02_SOURCE_PROOFS.json` e `validation/P243_CLOSURE_AND_BLOCKERS.md`. Ainda faltam os assédios/combates reais opcionais na rota de escolta; iniciativa completa e devolução de armas da milícia; concessões de Nyn, fichas EotE e defeitos na primeira utilização; Krayt Fang Beginner/combustível/carga/iniciativa; SWE02 atos 2/3 e finais, cotejo integral. Depois certificar as quatro outras aventuras (Under a Black Sun, Beyond the Rim, The Jewel of Yavin, Mask of the Pirate Queen) e EotE/AoR/expansões, incluindo 360 consumidores AoR individualmente (fonte→página→executor→UI→persistência/três reaberturas→teste). Não chamar testes estruturais de certificação editorial.

## Backup e retenção

Gerar ZIP fonte COMPLETO P243 + SHA sidecar + delta P242→P243 reconstruível + matriz/handoff/testes, enviar para Google Drive pasta `BRASEIRO_STAR_WARS_CHECKPOINTS`, ID `1qTEzIaJi1Ot3jqjVpKFpIvP4oRwJg1an`. Se ZIP exceder limite, dividir em quatro partes <=32MiB e gerar manifesto com hashes/tamanhos. **Exigir readback SHA de TODAS as partes e ZIP reconstruído idêntico antes de promover P243.** Até lá P242 é último canônico remoto; preservar P241/P240/P239/P238/P237/P236/P234/P233 até redundância inequívoca e P126/P113 históricos. Nunca excluir PDFs, saves ou outros VTTs. **Nenhuma exclusão autorizada neste lote.**

Autoridade mecânica exclusiva: código determinístico; RAG só consulta/cita; Gemini/Motor Bárbara narram/propõem sem aplicar estado. Falha contida. Manter tarefa horária existente, não criar outra, não pedir OK.
