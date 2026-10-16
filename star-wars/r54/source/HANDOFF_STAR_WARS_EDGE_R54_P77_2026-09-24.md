# STAR WARS EDGE R54 — P77 — 2026-09-24

## Baseline
- Entrada exata: P76 SOURCE CHECKPOINT do Google Drive.
- Projeto não recriado.
- GitHub não foi usado para corrigir fonte; apenas como esteira de compilação Android no BRELGA.
- EotE / AoR / MIXED e Duty / Obligation preservados.

## Correção P77
- Corrigido bloqueio real de compilação Android encontrado no primeiro build P76 no BRELGA:
  `TextToSpeech.getCurrentEngine()` não existe na API pública Android.
- `TtsBridge` agora rastreia explicitamente o pacote usado para construir a instância TextToSpeech e usa `getDefaultEngine()` apenas quando a instância foi aberta com o motor padrão.
- A seleção dinâmica/persistida de engines e vozes foi preservada.
- Pipeline Kokoro P76 integralmente preservado: chunking 250–650/alvo 450, onDone real, QUEUE_ADD, sem watchdog entre blocos, parâmetros Sherpa, prewarm, velocidade única e fallback Samsung/Google.
- Android: versionCode 10077 / versionName 1.0.77.
- Shell/runtime/bundle alinhados em 1.0.77 / R54 P77.
- CAMPANHA e lifecycle P74 preservados; Sob um Sol Negro P75 preservado.

## Evidência local pré-build
- Focal P76+P77: 8/8 PASS.
- Regressão canônica: 970/970 PASS, 0 FAIL.
- `node --check` em final-runtime.js e uiBridge.bundle.js: PASS.
- Bundle reconstruído por ambos builders canônicos e byte-a-byte idêntico.

## Evidência de falha anterior não mascarada
- Build P76 no BRELGA chegou a `:app:compileDebugJavaWithJavac` e falhou em `TtsBridge.java:303` por `getCurrentEngine()` inexistente.
- Download exato, extração, Android SDK e source gates tinham passado antes da falha.

## Gate de entrega P77
Somente considerar APK entregue após:
1. `assembleDebug` PASS no BRELGA;
2. aapt confirma package `com.braseiro.starwarsedge`, versionCode 10077, versionName 1.0.77;
3. apksigner verify PASS;
4. APK empacotado contém CAMPANHA, UI dinâmica TTS e Kokoro;
5. artefato e prova salvos no Google Drive.
