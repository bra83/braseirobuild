# BRASEIRO WWN — Android Studio 1.0.0

Projeto Android Studio do VTT mobile BRASEIRO WWN. Esta árvore consolida as closures funcionais e visuais aprovadas até a linha 065/063 sem redesenho das telas aprovadas.

## Toolchain canônico

- Android Gradle Plugin: 9.4.0
- Gradle: 9.6.0
- JDK: 17
- compileSdk / targetSdk: 37
- SDK Build Tools: 36.0.0
- minSdk: 28

## Build

1. Abra esta pasta no Android Studio com suporte a AGP 9.4.
2. Selecione JDK 17 para o Gradle.
3. Instale Android SDK API 37 e Build Tools 36.0.0.
4. Aguarde a sincronização do Gradle.
5. Execute os testes e então rode o app em emulador/dispositivo Android.

O projeto não contém `local.properties`, chave Gemini, keystore, APK ou AAB. A chave Gemini é configurada pelo usuário em runtime e não deve ser empacotada na árvore.

## Gates locais já executados

- regressões atuais das closures posteriores: PASS;
- sintaxe/imports JS e guards de segurança/UTF-8: PASS;
- 33 sessões / 4.752 turnos determinísticos: PASS;
- persistência/Room, bridge, RAG/PDFBox e Gemini: compile-contract PASS;
- browser smoke 415×915 e gates visuais aprovados permanecem preservados.

## Gate que exige ambiente Android real

A promoção para `BUILD_PASS / DEVICE_PASS` só é legítima depois de sync/build AGP real e execução de WebView, SAF, PDFBox, TTS, importação de imagem e Gemini em emulador/dispositivo.
