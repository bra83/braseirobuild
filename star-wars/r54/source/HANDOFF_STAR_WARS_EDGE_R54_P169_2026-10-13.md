# BRASEIRO STAR WARS VTT — R54 P169 — 13/10/2026

## Base e entrega

- Entrada: `BRASEIRO_STAR_WARS_EDGE_R54_P168_SOURCE_CHECKPOINT.zip` (SHA-256 `27d230300bd84b1c5d7518c05fc02f5e7e3e59e96477ea5e82e83ec5c4fcb8f2`, conferido antes de editar).
- Saída: `BRASEIRO_STAR_WARS_EDGE_R54_P169_SOURCE_CHECKPOINT.zip`, projeto **completo** para Android Studio, não APK. `versionCode=10169`, `versionName=1.0.169`.
- O bundle offline `js/core/uiBridge.bundle.js` foi reconstruído com 80 módulos.

## Código integrado

1. **UAE / origem de consequências:** consequências definidas no módulo autoral agora só podem ser registradas por um disparo real de gatilho com token privado. Um `record-consequence` externo não pode forjar o ID e liberar efeitos `onResolveEffects`. A origem de cada registro novo inclui `authored`, `triggerId` e `causeEventId`, auditados contra o evento causal e o catálogo de gatilhos. Os efeitos continuam somente na definição autoral, nunca persistidos no registro. Consequências improvisadas continuam funcionando sem ganhar recompensas publicadas.
2. **Compatibilidade P168:** `consequenceProvenanceFromSequence` começa em 1 para campanhas novas e, para saves P168 sem esse campo, passa a valer a partir do primeiro novo commit. Eventos antigos permanecem inalterados. Validação do marco no checkpoint e reversão atômica em erro.
3. **Cinco aventuras:** 25 cenários de integração (5 aventuras × 5 gates) cobrindo rejeição de consequência forjada, dois gatilhos autorais para o mesmo ID, recibo idempotente, resolução com objetivo e relógio, três reaberturas, journal adulterado e consequências avulsas. Outros cinco cenários verificam uso e persistência de Sombra furtiva em campanhas mistas.
4. **AoR:** `SOMBRA FURTIVA` usa o consumidor já executável `MASSHAD`: custo de 2 Tensão, dificuldade reduzida em 1, apenas Sigilo/Atividade Criminal, limites de rodada e rollback. Somente compra original autenticada habilita a ação. Interface existente de talentos ativos mostra também o nome AoR.
5. **Cobertura AoR:** 314/360 (87,22%). Restam 46 posições listadas em `evidence/P169/AOR_PENDING_46_POSITIONS.json`. Não declarar as expansões certificadas.

## Evidência reproduzível

- `evidence/P169/NODE_REGULAR.txt`: **2387/2387**, 287 arquivos.
- `evidence/P169/NODE_FOCAL.txt`: **32/32** cenários P169.
- `evidence/P169/NODE_STRESS.txt`: **6/6**.
- `evidence/P169/KOKORO_TTS_REGRESSION.txt`: **27/27**.
- `evidence/P169/JAVA_SYNTAX.txt`: **8/8 fontes**, análise sintática, não compilação Android.
- `evidence/P169/JAVA_HASH_COMPARE.txt`: oito arquivos Java byte a byte idênticos aos de P168.
- `evidence/P169/ANDROID_GRADLE_ATTEMPT.txt`: `gradlew :app:assembleDebug --offline` falha com `UnknownHostException: downloads.gradle.org` e não há distribuição Gradle local. Sem APK, instalação ou áudio físico homologados.

## Reproduzir

```bash
node tools/build-ui-bridge-bundle.mjs
node --test tests/uae-p169-provenance-five-adventures.test.mjs
python tools/run-node-suite.py --regular
python tools/run-node-suite.py --stress
node --test tests/*tts*.test.mjs tests/*kokoro*.test.mjs
node tools/java-syntax-gate.mjs
bash gradlew :app:assembleDebug --offline
```

## Próximo bloco

Implementar as 46 posições AoR restantes com consumidores reais e testes; cotejar cada expansão e aventura com os PDFs, sem inferir regras não verificadas; compilar em runner com Gradle e Android SDK, testar instalação, 3 reaberturas e Kokoro em aparelhos físicos. Preservar P168 e P169; não reescrever o projeto a partir de template.
