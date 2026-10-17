# Braseiro O Um Anel 2e — estado de integração V9 (2026-10-17)

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

## Gates obrigatórios — estado factual
1. **Sintaxe dos scripts:** CI automático em execução; só considerar aprovado após conferir o run verde.
2. **Regressões antigas:** 70/70 aprovados na V9 original na inspeção local anterior; devem passar **novamente depois dos patches**.
3. **Regressões adversariais novas:** 10 testes adicionados, **aguardando execução com ZIP privado**.
4. **Cópia não destrutiva do source:** script criado, **aguarda execução em ambiente com ambos os ZIPs extraídos**.
5. **Acoplamento real da UI:** **não feito**. Copiar para `src/tor2e-v49212` não liga botões, cenas, callbacks ou salvamento ao motor. Precisa mapear os adaptadores reais no source Android.
6. **Autoridade Host/nativo:** o `host-authority` **não pode** ser copiado para assets/WebView. Ainda exige serviço seguro e testes de confiança; identidade da ficha e do equipamento deve vir do estado persistido, não da UI.
7. **Motor Bárbara/Java/Kotlin:** integração nativa **não feita**; o script só copia módulos JavaScript de forma isolada, sem sobrescrever gateway TTS.
8. **PDFs:** 44 cenas dos Contos, 5 de A Estrela da Névoa, 11 culturas, 182 entradas RAG **não equivalem a certificação integral** dos livros.
9. **Assinatura e atualização:** APK 4.9.212, certificado de assinatura, migração de salvamentos e teste físico Galaxy A57/S23 Ultra **não homologados**.
10. **Entrega final:** **bloqueada** até aprovação de todos os gates, sem renomear BUILD WEB como APK pronto.

## Requisitos não negociáveis
- Preservar o APK e o package ID, assinatura original, dados salvos, layout fixo, TTS Charon/Kokoro, trilhas, tokens e imagens da 4.9.211.
- Não tocar em D&D, Mystara, Pathfinder, Star Wars ou Forbidden Lands no monorepositório.
- Correções estruturais devem entrar com testes que reproduzam falha anterior antes da correção.
- Uma transação durável só é atômica se o repositório de salvamento confirmar a gravação; o módulo síncrono rejeita promessa assíncrona, exigindo que a camada de Android forneça um adaptador transacional apropriado.
- Não declarar `100%` antes de teste final funcional, livros, Android, segurança e persistência.
