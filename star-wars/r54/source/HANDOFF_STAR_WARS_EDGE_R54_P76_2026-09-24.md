# STAR WARS EDGE R54 — P76 — 2026-09-24

## Baseline
- Entrada exata: `BRASEIRO_STAR_WARS_EDGE_R54_P75_SOURCE_CHECKPOINT.zip` do Google Drive.
- SHA-256 P75 conferido antes da edição: `0f09f7b101425571d9a6a0b1a18895455024e1580937e93f920f97486feaff00`.
- Projeto não recriado.
- GitHub não foi usado para corrigir fonte; somente poderá ser usado como esteira BRELGA para compilar o checkpoint já fechado.
- EotE / AoR / MIXED e Duty / Obligation preservados.

## Correção de entrega Android
- `versionCode = 10076` e `versionName = 1.0.76`.
- shell/runtime alinhados em `1.0.76 / R54 P76`.
- Mantidas as correções P74 de insets físicos e hub `CAMPANHA` (reiniciar aventura, novo mundo/voltar à seleção, zerar campanha inteira).
- Mantida a correção P75 de persistência/anti-spoiler de Sob um Sol Negro (`zelcomm-frontdesk`).

## TTS Android / Kokoro
### Engines e vozes
- `TtsBridge` agora enumera dinamicamente `TextToSpeech.getEngines()` e as vozes de cada engine.
- Engine e voz selecionadas são persistidas no aparelho.
- Manifest ganhou `<queries>` para `android.intent.action.TTS_SERVICE`, necessário para visibilidade de engines em Android moderno.
- A engine ativa é lida diretamente da instância persistente; não é aberta/fechada por um probe adicional durante refresh do catálogo.
- Sherpa/Kokoro reconhecido inclusive pelo pacote `com.k2fsa.sherpa.onnx.tts.engine`.
- Samsung/Google/outros continuam no fluxo Android normal existente.

### Perfil `TTS local neural / Kokoro`
- Novo `TtsChunker` puro Java.
- Blocos naturais: alvo 450 chars, faixa 250–650.
- Prioridade de corte `. ! ?` → `;` → `,` → espaço somente em último caso.
- Múltiplas frases permanecem juntas quando cabem; o texto visual nunca é alterado.
- Sequência neural usa `QUEUE_ADD`; não usa `stop()` nem `QUEUE_FLUSH` entre blocos.
- Próximo bloco somente depois de `UtteranceProgressListener.onDone()` real do anterior.
- Nenhum watchdog / timeout de síntese neural foi introduzido.
- Uma nova narração pode interromper a anterior; essa é a única parada automática deliberada.

### Contrato Sherpa/Kokoro
Passado ao motor pelo Bundle por bloco:
- `maxNumSentences = 2`
- `silenceScale = 0.06`
- `numThreads = 4`
- `leadingSilenceMs = 50`
- `streamingAudioChunkMs = 125`
- `streamingMode = generateWithConfigAndCallback`
- `audioDelivery = SynthesisCallback.audioAvailable`

O VTT mantém sua instância `TextToSpeech` viva entre blocos/narrações; não recria a engine no encadeamento. Prewarm assíncrono é disparado ao selecionar voz/engine neural e ao abrir uma nova cena.

IMPORTANTE: `OfflineTts`, `generateWithConfigAndCallback` e a chamada física a `SynthesisCallback.audioAvailable()` pertencem ao serviço TTS Sherpa/Kokoro instalado. O VTT envia e testa o contrato/encadeamento do lado cliente; a validação de streaming PCM real precisa ocorrer no APK + engine instalada no aparelho.

### Velocidade
- Kokoro: Android global fica explicitamente neutro em `setSpeechRate(1.0f)`; a velocidade escolhida é enviada exatamente uma vez em `braseiro.kokoro.speed` / `speed` ao motor.
- Samsung/Google: preservado `setSpeechRate(rate)` Android normal.

## Evidência P76
- Gate puro Java do chunker: PASS; texto obrigatório chega em 1 bloco; narração longa gerou 10 blocos válidos e reconstruiu o texto.
- Gate de contrato TTS: 21/21 PASS.
- Focal P74/P75/P76: 12/12 PASS.
- Regressão canônica `node --test tests/*.test.mjs`: **968/968 PASS, 0 FAIL**.
- `node --check` em `final-runtime.js` e `uiBridge.bundle.js`: PASS.
- Bundle reconstruído pelos dois builders canônicos: 61 módulos.

## Falha preservada, não mascarada
Existe `P76_NONCANONICAL_ROOT_PLUS_TESTS_FAILURE.txt`: uma invocação `node --test` incluindo simultaneamente testes duplicados da raiz + `tests/` produziu 15 falhas. As cópias da raiz resolvem caminhos a partir do diretório pai (`/mnt/data/starwars_p75`) e não representam o runner canônico deste checkpoint. A suíte canônica em `tests/*.test.mjs`, executada depois das alterações finais, é 968/968 PASS. O log da invocação incorreta foi mantido para auditoria em vez de apagado.

## Android / aparelho físico
- Compilação Android P76: ainda não declarada PASS neste handoff-fonte. Deve ser feita no BRELGA e validada com `aapt`/`apksigner`.
- Gates obrigatórios do APK: package `com.braseiro.starwarsedge`, `versionName 1.0.76`, `versionCode 10076`, assinatura válida, APK ZIP íntegro.
- Teste físico TTS obrigatório: catálogo de engines/vozes, persistência, primeira palavra, pausa curta em pontos, narração longa 1→2→3 sem cortes, Kokoro sem travamento/watchdog e Samsung/Google sem regressão.
