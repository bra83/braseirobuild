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
| `ci/oua-4.9.212/harden-rules-guard.mjs` | Fechar bypass pela segunda rota, o command bus, e impedir elevação de `GM_HELP` pelo próprio payload |
| `ci/oua-4.9.212/guard-hardening.test.mjs` | Três testes de fraude no command bus e reabertura do estado |
| `ci/oua-4.9.212/stage-v9-in-android.mjs` | Copiar somente código de módulos para namespace isolado, sem substituir interface ou incluir chaves no WebView |
| `.github/workflows/oua-v9-hardening.yml` | Validação sintática automática; suíte funcional e build Android apenas após disponibilizar a fonte privada V9 no runner |

## Execução local, com os ZIPs adquiridos legalmente
Extraia o ZIP V9 em `V9_ROOT`, e o fonte Android 4.9.211 em `ANDROID_ROOT` (a pasta que contém `package.json` e `android/`).

```bash
node ci/oua-4.9.212/harden-v9.mjs "$V9_ROOT"
node ci/oua-4.9.212/harden-rules-guard.mjs "$V9_ROOT"
(cd "$V9_ROOT" && npm test)
OUA_MODULE_ROOT="$V9_ROOT" node --test ci/oua-4.9.212/hardening.test.mjs ci/oua-4.9.212/guard-hardening.test.mjs
node ci/oua-4.9.212/stage-v9-in-android.mjs "$V9_ROOT" "$ANDROID_ROOT"
(cd "$ANDROID_ROOT" && npm ci && npm run lint && npm run build)
(cd "$ANDROID_ROOT/android" && ./gradlew :app:assembleDebug)
```

O workflow permite `workflow_dispatch` em BRELGA com `v9_zip_path` (caminho do ZIP privado **no runner**). Ele restaura a fonte Android a partir da release e executa as verificações. Não publica APK automaticamente. Não envie livros ou a V9 com trechos de livros para este repositório público.

## Gate adicional: proteção da superfície cliente
- `stage-v9-in-android.mjs` **não** copia `src/security` nem `src/host-authority` para o WebView.
- `staging-safety.test.mjs` exercita isso com fonte sintética; também verifica preservação da UI, projeto Android e bloqueio de sobrescrita.
- Esses módulos ainda dependem de uma ponte Host/native confiável. **Não importar o RulesGuard/RAG da pasta de staging para o WebView até implementar e testar a ponte.**
- A ausência das bibliotecas de segurança no cliente é intencional e não certifica o funcionamento de RAG/segredos em produção.
- A verificação acima é independente dos dez testes adversariais que **ainda exigem a V9 privada** no runner BRELGA.

## Gates obrigatórios — estado factual (atualizado após Megalote 01: ETAPA 00 + ETAPA 01)
1. **G01 — Preservação da 4.9.211 (`PASS`):** `verify-strider-opening-activation-4.9.211.mjs` (`9/9 PASS`), `manual-roll-once.test.ts` (`4/4 PASS`), `audit-turn-flow.ts` + `verify-audit-result.mjs` (`62 sessões, 982/982 asserções PASS`), `npm run lint` (`PASS`) e `npm run build` (`PASS`, 2110 módulos) comprovados sobre `ANDROID_49212_WORK`.
2. **G08 — Endurecimento V9 e Regressões (`PASS`):**
   - Corrigida regressão em `harden-v9.mjs` que quebrava `EST-12` na suíte original de 70 testes (`69 pass, 1 fail` -> `70/70 PASS`).
   - Fortalecido `hardening.test.mjs` (`HARD-04`, `HARD-05`, `HARD-06`) com teste explícito de rejeição de `Promise` assíncrona no `persistenceStore.save`, garantia de consequência única (`conseq:<checkId>`), Fadiga sem duplicação em retry (`0 -> 2`, nunca `4`) e bloqueio de conclusão prematura.
   - Na `V9_RAW_READONLY`: `10/10 FAIL` nos testes adversariais (comprovando vulnerabilidade real antes do patch).
   - Na `V9_PATCHED` (com idempotência 2x validada): `70/70 PASS` (suíte original V9) + `11/11 PASS` (`7` `hardening.test.mjs` + `3` `guard-hardening.test.mjs` + `1` `staging-safety.test.mjs`) = **`81/81 PASS`**.
3. **Cópia não destrutiva do source (`PASS` isolado):** `stage-v9-in-android.mjs` testado sobre `ANDROID_49212_WORK` e validado por `staging-safety.test.mjs` (`OUA-STAGE-01 PASS`), sem copiar `src/security` nem `src/host-authority` para o WebView.
4. **G02 / G09 / G10 — Acoplamento real da UI, Ingresso único e Ponte Host/nativo (`FAIL` — em execução na ETAPA 02):** Copiar para `src/tor2e-v49212` não liga botões, cenas, callbacks ou salvamento ao motor. Além disso, `Tor2eRAGService.mjs` e `Tor2eAdventureController.mjs` exigem ponte Host/Android sem `src/security` no WebView e `Tor2eRulesGuard` como único ingresso.
5. **G07 — Motor Bárbara 1.1.3 / TTS (`PARTIAL` — ETAPA 04):** Artefatos de `Motor Barbara/LATEST` verificados por SHA-256 (`SHA256SUMS-LATEST.txt`); integração final com novos módulos pendente.
6. **G03 / G04 / G05 / G06 — Auditoria livro a livro contra PDFs (`PARTIAL` — ETAPA 03):** 44 cenas dos Contos, 5 de A Estrela da Névoa, 11 culturas e 182 entradas RAG ainda exigem matriz completa contra os PDFs em `oua-private/pdfs_oua_2e`.
7. **G11 / G12 — Assinatura, APK 4.9.212 e Físico (`PARTIAL` / `NOT_TESTED`):**
   - Certificado do APK `4.9.211` auditado via `apksigner`: SHA-256 `b1471859ec0a4838ec5dff4f18f77bacdc99d535de968d261a7fe4008213dd27` (`CN=Android Debug`), distinto do `debug.keystore` local (`6aa87a24a2eb4762c3f493c9d5fddbf10b60cc8659a90423aed7cde8ae582bd3`). **Promessa de update in-place sobre 4.9.211 instalado está BLOQUEADA** sem a chave original.
   - Sem dispositivo físico conectado via `adb devices`: **`ANDROID_FISICO_NOT_TESTED`**.
8. **Entrega final:** **bloqueada** até aprovação dos gates pendentes, sem declarar `DONE` ou `100%`.

## Requisitos não negociáveis
- Preservar o APK e o package ID, assinatura original, dados salvos, layout fixo, TTS Charon/Kokoro, trilhas, tokens e imagens da 4.9.211.
- Não tocar em D&D, Mystara, Pathfinder, Star Wars ou Forbidden Lands no monorepositório.
- Correções estruturais devem entrar com testes que reproduzam falha anterior antes da correção.
- Uma transação durável só é atômica se o repositório de salvamento confirmar a gravação; o módulo síncrono rejeita promessa assíncrona, exigindo que a camada de Android forneça um adaptador transacional apropriado.
- Não declarar `100%` antes de teste final funcional, livros, Android, segurança e persistência.
