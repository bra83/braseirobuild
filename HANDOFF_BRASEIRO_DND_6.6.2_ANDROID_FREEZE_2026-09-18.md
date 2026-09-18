# HANDOFF_BRASEIRO_DND_6.6.2_ANDROID_FREEZE_2026-09-18.md

## CONTINUE EXATAMENTE DAQUI
Não recrie o projeto. Não volte para baseline antigo. Preserve mapas, assets, mecânicas e conteúdo já implementado.

**Projeto:** Braseiro DnD 6.6.2  
**Pacote obrigatório:** `com.braseiro.dnd5e`  
**Repo:** `bra83/braseirobuild`

## ESTADO ATUAL
O APK Android já foi **gerado, validado e publicado**.

- Release: `dnd-6.6.2-android-freeze`
- Página: https://github.com/bra83/braseirobuild/releases/tag/dnd-6.6.2-android-freeze
- APK: `BRASEIRO_DND_6.6.2_DEBUG.apk`
- Download direto: https://github.com/bra83/braseirobuild/releases/download/dnd-6.6.2-android-freeze/BRASEIRO_DND_6.6.2_DEBUG.apk
- Tamanho: `402434077` bytes
- SHA-256 APK: `58b0b695ba924d88a4b13c139e64c8ee6aeeb1e34ef3a4b84795db23190b6b2c`

Workflow que passou:
- `.github/workflows/android-dnd-diagnostico.yml`
- Run: `35308376396`
- Resultado: `SUCCESS`
- https://github.com/bra83/braseirobuild/actions/runs/35308376396

Commit alvo da Release:
`487fe53e8a5c919e60c1c99b20467f92d164ff83`

HEAD observado neste handoff:
`811a28fdf900428ba818b416cdac1a6ed18f09d9`

## APK VALIDADO
- package `com.braseiro.dnd5e`
- versionCode `606200`
- versionName `6.6.2-debug`
- compileSdk `35`
- targetSdk `35`
- minSdk `24`
- label `Braseiro D&D`
- launcher `com.braseiro.dnd5e.MainActivity`
- ABIs: `arm64-v8a`, `armeabi-v7a`, `x86_64`

Metadados persistidos:
- `deliverables/README.md`
- `deliverables/APK_BADGING.txt`
- `deliverables/APK_SIZE.txt`
- `deliverables/BRASEIRO_DND_6.6.2_DEBUG.apk.sha256`
- `deliverables/APK_RELEASE_URL.txt`
- `deliverables/BASELINE_SHA256.txt`

O APK grande fica como asset da GitHub Release.

## BASELINE ANDROID CONGELADO
Baseline que efetivamente gerou o APK:
`2acf32fe57437c5d2472f7a65104791828a5305f44fb722ee545af0a01a52937`

Estrutura:
- raiz: `BRASEIRO_DND_6.6.2_ANDROID_STUDIO/`
- Android: `BRASEIRO_DND_6.6.2_ANDROID_STUDIO/android/`
- 234 classes Java no pacote ativo
- 0 referências ativas a `package com.braseiro.dnd;`

Arquivo: `ci/android-freeze/BASELINE.md`

Chunks:
- P00 `7799a6d20fa5b0f362cf4ab386139c31130b5807da44afd4841b9b6bb8fe2ac6`
- P01 `06864dd74d1c3ef62a2010210cbd63275bd3046476d7c1e1af3b15ed6bdeb61d`
- P02 `86534362f7892834b7de98e154a6d122bf7e73f95cb2d6a80d8c2884c9434ebc`
- P03 `8ba61dc700efc75aa1499621cd502ed46bab5e0f59952850aad5c89af3f5d7d5`
- P04 `c3984239c2db218db020180c71a67069276abe6c0ea8f8085a0c1cee0131e5fc`

## RUN103 ORIGINAL — NÃO CONFUNDIR
SHA do RUN103 original:
`91ae5ea70b0e309b6c381b77fb63497e43fecd6eda2dd54974f49b2be04a06d5`

Não confundir:
- RUN103 original = `91ae5e...`
- freeze compilado = `2acf32...`

## GOOGLE DRIVE — AUTORIDADE FUNCIONAL
Para desenvolvimento funcional, o **LATEST autoritativo do Google Drive** continua sendo a referência.

Pasta:
`DND_SANDBOX_WORKING`
Folder ID:
`1v_KCxY1KNhazW1pqJTHgUSfs1yyuH_wp`

O freeze Android é autoridade para reproduzir o APK validado; não deve substituir silenciosamente o LATEST funcional.

## PRIORIDADE FUNCIONAL OBRIGATÓRIA
### Canal azul de conversa com o Mestre
Separar:
1. pergunta/conversa;
2. ação narrativa que altera a história.

Casos obrigatórios:
- “o que estou vendo?”
- “consigo subir nesse telhado?”

Comportamento esperado:
- pergunta recebe resposta do Mestre **fora da história**;
- se necessário, Mestre orienta rolagem/teste;
- pergunta não vira ação automática;
- só ação efetivamente resolvida entra na narrativa;
- resultado resolvido pode gerar `resolution_excerpt` / “NA HISTÓRIA”;
- preservar canal azul distinto da narração.

Não declarar o projeto funcionalmente 100% concluído sem validar esse fluxo real.

## LOCKS
- NÃO recriar projeto.
- manter package `com.braseiro.dnd5e`.
- não voltar para `com.braseiro.dnd`.
- não mexer nos mapas sem pedido explícito.
- não remover/trocar assets para facilitar build.
- não alterar gameplay para facilitar compilação.
- correções Android devem ser de compatibilidade/compilação.
- preservar Motor Barbara e conteúdo existente.
- não usar sanity check sem classpath Android como prova de erro.
- mapas/assets/gameplay ficaram fora do escopo dos reparos de compilação.

## HISTÓRICO DOS BLOQUEIOS
1. downloader falhava antes do Gradle (`download failed 0`);
2. links temporários/Firestorage expiravam/403;
3. chave `}` extra em `MainActivity.java` fechava classe cedo;
4. vários `JSONObject.put()` exigiam tratamento de `JSONException`;
5. tentativas amplas de wrapper foram descartadas quando quebravam estrutura Java;
6. foi criada baseline Android congelada e validada em gates;
7. cota de `actions/upload-artifact` ficou cheia;
8. solução final: publicar **direto em GitHub Release**.

## WORKFLOW AUTORITATIVO DE BUILD
Usar:
`.github/workflows/android-dnd-diagnostico.yml`

Ele valida chunks/SHA, estrutura, package/namespace, JDK 17, Android SDK 35, Manifest, AAPT/resources, Java/Kotlin, `assembleDebug`, badging, package, integridade APK e SHA; depois publica direto na Release.

Run verde confirmado: `35308376396`.

## PRÓXIMO CHAT
1. Ler este handoff primeiro.
2. Não recriar nada.
3. Confirmar HEAD e Release.
4. Não gerar APK de novo se a intenção for só obter o APK: ele já existe.
5. Para mudanças funcionais, voltar ao LATEST do Drive.
6. Preservar o freeze Android como checkpoint seguro.
7. Priorizar teste real do canal azul Mestre/pergunta vs ação.
8. Mudança funcional real => novo RUN acumulado.
9. Só substituir LATEST após validação.
10. Manter SHA + state + manifest.
11. Novo APK => nova tag/versionamento; não sobrescrever silenciosamente o freeze atual.

## FRASE DE RETOMADA
“Continue exatamente do `HANDOFF_BRASEIRO_DND_6.6.2_ANDROID_FREEZE_2026-09-18.md`. Não recrie nada. O APK já está validado na Release `dnd-6.6.2-android-freeze`. Para desenvolvimento funcional, use o LATEST autoritativo do Google Drive. Preserve mapas/assets/gameplay e priorize a validação do canal azul de conversa com o Mestre.”

## STATUS
Android: **CONCLUÍDO E VALIDADO**.  
Funcional: **não declarar 100% concluído apenas porque o APK compila**.
