# Braseiro O Um Anel 2e — estado de integração V9 (2026-10-17)

> **INSTRUÇÃO CANÔNICA PARA O ANTIGRAVITY:** [ANTIGRAVITY_ORDEM_MESTRA_FONTES_UNICAS_E_GATES.md](./ANTIGRAVITY_ORDEM_MESTRA_FONTES_UNICAS_E_GATES.md). Este documento estabelece precedência entre Android 4.9.211, V9 e PR #59, endereços de TODAS as pastas de origem relevantes, contratos de módulos, fluxo único de integração e G01–G12 obrigatórios. Qualquer plano conflitante anterior fica subordinado a ele. **NÃO fechar o PR nem publicar APK final antes de comprovação dos 12 gates.**


## Fontes verificadas
- Último APK estável: [OUA-v4.9.211](https://github.com/bra83/braseirobuild/releases/tag/OUA-v4.9.211).
- Fonte Android Studio 4.9.211: [ZIP completo da release](https://github.com/bra83/braseirobuild/releases/download/OUA-v4.9.211/BRASEIRO_O_UM_ANEL_4.9.211_ANDROID_STUDIO_COMPLETE.zip).
- Fonte privada dos módulos 4.9.212 V9 (NÃO distribuir em GitHub público): [Google Drive V9](https://drive.google.com/file/d/1w9U-z2qWi1l4PBxaPSL0OFgH-_R6Ybn-/view).
- PDFs e assets: [pasta O Um Anel](https://drive.google.com/drive/folders/15YC5VQ-PLn0o9b41obP3_JessSDuvFZd).
- Motor Bárbara 1.1.3: [pasta LATEST](https://drive.google.com/drive/folders/1oVAD0khD3dqNF1gu6EmjjBi2MSAWxplG).
- Branch de trabalho: `integration/oua-49212-v9-hardened-20261017`.

## Código acrescentado nesta branch
| Arquivo | Finalidade |
|---|---|
| `ci/oua-4.9.212/harden-v9.mjs` | Corrigir autoridade da ficha e combate, alvo inexistente, rolagens simuladas, repetição fornecida pelo chamador e durabilidade de `commandId` no controlador |
| `ci/oua-4.9.212/hardening.test.mjs` | Sete regressões adversariais que devem falhar na V9 sem patch |
| `ci/oua-4.9.212/harden-rules-guard.mjs` | Fechar bypass pela segunda rota (o command bus), encapsular `Tor2eAdventureController` como componente interno do `Tor2eRulesGuard` (ingress único) e impedir elevação de `GM_HELP` pelo próprio payload |
| `ci/oua-4.9.212/guard-hardening.test.mjs` | Quatro testes de fraude no command bus, reabertura do estado e ingress único (`OUA GUARD-01..04`) |
| `ci/oua-4.9.212/stage-v9-in-android.mjs` | Copiar somente código de módulos para namespace isolado (`src/tor2e-v49212`), sem substituir interface ou incluir chaves no WebView, gerando `Tor2eHostGmBridge.mjs` WebView-safe e reescrevendo imports de `Tor2eGmSecretsVault` |
| `ci/oua-4.9.212/integration-contract-map.md` | Mapa físico de contratos e fluxo único (UI `LoneLandsPanel.tsx` → `App.tsx` → `applyMasterCommands` → `lib/tor2eV9HostBridge.ts` → `Tor2eRulesGuard` → `MainActivity.kt` `@JavascriptInterface`) |
| `ci/oua-4.9.212/v9-android-host-bridge.test.mjs` | Quatro testes end-to-end (`OUA E2E-01..04`) provando ingress único, clique UI → `applyMasterCommands` → `Tor2eRulesGuard` → persistência → 3 ciclos salvar–fechar–reabrir, bloqueio de dados/fichas forjados e auditoria do bundle Vite |
| `ci/oua-4.9.212/harden-v9-canon-books.mjs` | Enriquecimento idempotente de `Tor2eContentCatalog.mjs` (`MARCO_A_ESTRELA_DA_NEVOA` pp. 223–230), `ValfendaExpansion.mjs` (13 locais do mapa, 15 salas da Casa de Elrond, regras p. 3–12), `PovosTerrasErmasExpansion.mjs` (pp. 1–10), `TrilhasPersonagensModule.mjs` (36 antecedentes + 12 faces de Eventos Importantes pp. 9–10) e `ModoPeregrinoModule.mjs` (tabelas completas pp. 1–28) |
| `ci/oua-4.9.212/canonical-books-traceability-matrix.md` | Matriz livro a livro (`livro, edição, página impressa/PDF, entidade, regra, arquivo implementado, teste, resultado, evidência`) cobrindo todos os PDFs 2e e as 182 entradas do corpus RAG |
| `ci/oua-4.9.212/canonical-books-audit.test.mjs` | Quatro suítes automatizadas (`OUA G03-01`, `OUA G04-01`, `OUA G04-02`, `OUA G05-01`) verificando regras livro a livro, walkthrough completo das 7 aventuras canônicas até `ADVENTURE_CONCLUDED` e firewall das 182 entradas RAG |
| `.github/workflows/oua-v9-hardening.yml` | Validação sintática automática; suíte funcional e build Android apenas após disponibilizar a fonte privada V9 no runner |

## Execução local, com os ZIPs adquiridos legalmente
Extraia o ZIP V9 em `V9_ROOT`, e o fonte Android 4.9.211 em `ANDROID_ROOT` (a pasta que contém `package.json` e `android/`).

```bash
node ci/oua-4.9.212/harden-v9.mjs "$V9_ROOT"
node ci/oua-4.9.212/harden-rules-guard.mjs "$V9_ROOT"
node ci/oua-4.9.212/harden-v9-canon-books.mjs "$V9_ROOT"
(cd "$V9_ROOT" && npm test)
OUA_MODULE_ROOT="$V9_ROOT" node --test ci/oua-4.9.212/hardening.test.mjs ci/oua-4.9.212/guard-hardening.test.mjs
node ci/oua-4.9.212/stage-v9-in-android.mjs "$V9_ROOT" "$ANDROID_ROOT"
node ci/oua-4.9.212/wire-v9-host-bridge.mjs "$ANDROID_ROOT"
(cd "$ANDROID_ROOT" && npm run lint && npm run build)
(cd "$ANDROID_ROOT" && npx tsx --test ../braseirobuild/ci/oua-4.9.212/canonical-books-audit.test.mjs ../braseirobuild/ci/oua-4.9.212/v9-android-host-bridge.test.mjs)
```

O workflow permite `workflow_dispatch` em BRELGA com `v9_zip_path` (caminho do ZIP privado **no runner**). Ele restaura a fonte Android a partir da release e executa as verificações. Não publica APK automaticamente. Não envie livros ou a V9 com trechos de livros para este repositório público.

## Gate adicional: proteção da superfície cliente
- `stage-v9-in-android.mjs` **não** copia `src/security` nem `src/host-authority` para o WebView e gera `src/tor2e-v49212/ui/bridges/Tor2eHostGmBridge.mjs` sem segredos, delegando para `@JavascriptInterface` nativo em `MainActivity.kt` (`verifyTor2eGmAccess`, `setTor2eGmPin`, `authorizeTor2eCombatAndSheet`).
- `staging-safety.test.mjs` exercita isso com fonte sintética; também verifica preservação da UI, projeto Android e bloqueio de sobrescrita.
- `v9-android-host-bridge.test.mjs` (`OUA E2E-04`) audita o bundle compilado em `dist/assets/*.js` garantindo ausência de `gm-master-pin-2026`, `Tor2eGmSecretsVault` e `node:fs`.

## Gates obrigatórios — estado factual (atualizado após Megalote 03: ETAPA 03)
1. **G01 — Origem (`PASS`):** Checksums SHA-256 validados para `4.9.211` e `V9` (`98305f2c67b227c017deaeec45e73863776b53444f020b5b9a9917311ea75d35`).
2. **G02 — Integridade do app (`PASS`):** Pós-integração da ponte V9 em `ANDROID_49212_WORK`: `verify-strider-opening-activation-4.9.211.mjs` (`9/9 PASS`), `manual-roll-once.test.ts` (`4/4 PASS`), `audit-turn-flow.ts` + `verify-audit-result.mjs` (`62 sessões, 982/982 asserções PASS`), `npm run lint` (`PASS`) e `npm run build` (`PASS`, 2125 módulos transformados).
3. **G03 — Regras (`PASS`):** Matriz canônica livro a livro (`canonical-books-traceability-matrix.md`), enriquecimento determinístico (`harden-v9-canon-books.mjs`) e suíte `OUA G03-01` (`PASS`) cobrindo Livro Básico 2e, Modo Peregrino (pp. 1–28), Valfenda (pp. 1–16), Povos das Terras Ermas (pp. 1–10) e Trilhas de Personagens V2 (pp. 1–10, 36 antecedentes + 12 faces de Eventos Importantes).
4. **G04 — Aventuras (`PASS`):** Walkthrough completo de ponta a ponta das **7 aventuras canônicas** (`core-star-of-the-mist` pp. 223–230 com todas as condições de cenário, Senhora Elwen NA 5 / Resistência 24 / Poder 2 / Ódio 5, coroa de ferro de Lorde Hadirion na Torre da Estrela + as 6 aventuras de `Contos das Terras Solitárias` pp. 9–112) até `ADVENTURE_CONCLUDED` (`OUA G04-01`, `OUA G04-02`).
5. **G05 — RAG (`PASS`):** `182/182` entradas públicas ancoradas nos 8 livros/partes com `driveFileId`, `printedPage`, `sha256Anchor` e `sourcePdfFileSha256`, mais cofre `GM_SECRET` protegido por envelope AES-256-GCM DEK/KEK no Host (`OUA G05-01`).
6. **G06 — Host e Superfície Cliente (`PASS`):** `Tor2eRulesGuard` atua como único gateway mutável encapsulando `Tor2eAdventureController`; apenas `lib/tor2eV9HostBridge.ts` importa `Tor2eRulesGuard` (`OUA E2E-01`, `OUA E2E-04`); `MainActivity.kt` provê autoridade nativa Android; bundle `dist/assets` auditado sem segredos (`OUA E2E-03`, `OUA E2E-04`).
7. **G07 — Idempotência (`PASS`):** `ouaExecutedCommandIds` persistido em `loneLands`, rollback atômico em erro síncrono ou `Promise` no `persistenceHook`, e sobrevivência comprovada a 3 ciclos completos de salvar–fechar–reabrir com rejeição de replay (`OUA E2E-02`, `OUA E2E-03`).
8. **G08 — Endurecimento V9 e Regressões (`PASS`):** Na `V9_PATCHED`: `70/70 PASS` (suíte original V9) + `12/12 PASS` (`7` `hardening.test.mjs` + `4` `guard-hardening.test.mjs` + `1` `staging-safety.test.mjs`) = **`82/82 PASS`**.
9. **G09 — Integração UI (`PASS`):** `LoneLandsPanel.tsx` conectado ao comando canônico `v9_dispatch_adventure_action` em `lib/master/commands.ts` (`applyOne`) → `dispatchV9AdventureCommand` → `Tor2eRulesGuard`, com persistência real em `campaign.masterState.company.loneLands` (`OUA E2E-02`, `OUA G04-02`).
10. **G10 — Motor Bárbara 1.1.3 / Gemini / TTS (`PARTIAL` — ETAPA 04):** Artefatos de `Motor Barbara/LATEST` verificados por SHA-256 (`SHA256SUMS-LATEST.txt`); validação integrada de TTS/companheiros na ETAPA 04.
11. **G11 / G12 — Assinatura, APK 4.9.212 e Físico (`PARTIAL` / `NOT_TESTED`):**
   - Certificado do APK `4.9.211` auditado via `apksigner`: SHA-256 `b1471859ec0a4838ec5dff4f18f77bacdc99d535de968d261a7fe4008213dd27` (`CN=Android Debug`), distinto do `debug.keystore` local (`6aa87a24a2eb4762c3f493c9d5fddbf10b60cc8659a90423aed7cde8ae582bd3`). **Promessa de update in-place sobre 4.9.211 instalado está BLOQUEADA** sem a chave original.
   - Sem dispositivo físico conectado via `adb devices`: **`ANDROID_FISICO_NOT_TESTED`**.
12. **Entrega final:** **bloqueada** até aprovação dos gates pendentes, sem declarar `DONE` ou `100%`.

## Requisitos não negociáveis
- Preservar o APK e o package ID, assinatura original, dados salvos, layout fixo, TTS Charon/Kokoro, trilhas, tokens e imagens da 4.9.211.
- Não tocar em D&D, Mystara, Pathfinder, Star Wars ou Forbidden Lands no monorepositório.
- Correções estruturais devem entrar com testes que reproduzam falha anterior antes da correção.
- Uma transação durável só é atômica se o repositório de salvamento confirmar a gravação; o módulo síncrono rejeita promessa assíncrona, exigindo que a camada de Android forneça um adaptador transacional apropriado.
- Não declarar `100%` antes de teste final funcional, livros, Android, segurança e persistência.
