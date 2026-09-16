# BRASEIRO WWN — Android Studio 1.0.0

Projeto Android Studio source-only do VTT mobile BRASEIRO WWN. A árvore de produção usa campanha/persistência canônicas, criação e progressão WWN atuais, mundo vivo, exploração, combate, facções, gestão do Mestre, diário e importação/exportação de personagens e backups.

## Toolchain declarado

- Android Gradle Plugin: 9.4.0
- Gradle: 9.6.0
- JDK: 17
- compileSdk / targetSdk: 37
- SDK Build Tools: 36.0.0
- minSdk: 28

## Abrir no Android Studio

1. Abra esta pasta como projeto no Android Studio.
2. Use JDK 17 para o Gradle.
3. Instale Android SDK API 37 e Build Tools 36.0.0.
4. Faça sync/build e testes Android em um ambiente que suporte o toolchain.

Esta entrega é **source-only**. Não inclui `local.properties`, `.env`, chaves, keystore, APK, AAB, diretórios `build/` ou caches de IDE/Gradle. A chave Gemini é configurada pelo usuário em runtime e não deve ser empacotada no fonte.

## Source exit gate — 2026-09-16

- 145 arquivos JavaScript: sintaxe PASS;
- 31 suítes QA: 31 PASS / 0 FAIL;
- 66 sessões simuladas acumuladas (33 fechamento + 33 fechamento profundo): PASS;
- criação manual de personagem de produção: PASS;
- bootstrap do personagem inicial → teste → encontro → combate → progressão: PASS;
- 398 imports ESM locais auditados: 0 alvo/símbolo ausente;
- 15 JSONs de assets: parse PASS;
- 14 HTMLs: referências locais válidas;
- bridge JS/Kotlin: 38 métodos nativos usados / 45 expostos / 0 ausentes;
- runtime de produção sem autoridade de fixtures Wave; SCR-015/SCR-016 permanecem apenas como legado isolado.

Relatório detalhado: `SOURCE_FINAL_EXIT_AUDIT_2026-09-16.md`.

## Fora do source exit gate

Gradle, APK/AAB, WebView em dispositivo, SAF, PDFBox, TTS/áudio, dados físicos 3D e chamadas externas Gemini exigem runtime Android/credenciais/hardware. Eles **não foram declarados PASS nesta entrega** e não são usados para mascarar ou reabrir o fechamento do fonte.
