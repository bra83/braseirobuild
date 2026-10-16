# STAR WARS EDGE R54 P91 — Auditoria funcional rápida

Baseline auditada: P90 materializada do Google Drive, sem recriação e sem GitHub para correção.
Versão após correções: `1.0.91` / `versionCode 10091` / `r54-p91`.

## Matriz compacta de evidência

| Área | Entrada / comando | Caminho real | Estado / efeito visível | Evidência | Estado |
|---|---|---|---|---|---|
| EotE/AoR/MIXED | criação / modo de campanha | controller + criação + regras Duty/Obligation | modo, Duty e Obligation persistem | `mixed-campaign-sandbox-r33`, `aor-duty-r37`, `r54-p33-mixed-creation-ui`, regressão P91 | PASSOU |
| Duty/Obligation | escolhas, Spyder, Zelcomm | engine da aventura → ledger → save | total e origem acumulam e sobrevivem save/reopen | `r54-p90-under-black-sun-obligation-accumulation`, regressão P91 | PASSOU |
| Sob um Sol Negro | cena → escolha/teste | `underBlackSun.mjs` → controller → save → UI | 17/17 nós alcançáveis; efeitos, branches, aftermath e anti-spoiler persistentes | gate P91 106/106 + reachability | PASSOU |
| Além da Orla | cena → escolha/teste | `beyondTheRim.mjs` → controller → mundo vivo → UI | 26/26 nós alcançáveis; branches/aftermath e anti-spoiler | gate P91 57/57 + reachability | PASSOU |
| Campanha / novo mundo / reset | seleção → reiniciar/zerar | `final-runtime.js` → `GameController` → save | retorna ao setup, preserva ou zera conforme comando | `r54-p74-campaign-lifecycle-insets-version` + high-risk P91 | PASSOU |
| Dados/testes | sugestão/teste de aventura | runtime → roller/adjudication → history → UI | resultado e efeitos aparecem e persistem | `r80-gemini-external-scene-npc-dice`, P81/P83/P84, regressão P91 | PASSOU |
| Mapa/viagem | mapa/rota | UI → controller → travel/world state | posição/viagem/mundo persistem | `map-ui`, `map-viewer-p13`, `map-provenance-p12`, high-risk P91 | PASSOU |
| Combate/veículos/inventário | ações e itens | UI/controller → rules → state | dano/veículo/itens refletem estado | `combat`, `vehicle-controller`, `vehicle-ui`, regressão P91 | PASSOU |
| Mundo Vivo | avanço temporal/eventos | world engines → pending consequences → save | eventos e consequências persistem sem duplicação coberta | `living-world-depth-p9`, `causal-world-r16`, P85/P86/P89, regressão P91 | PASSOU |
| Cena: ILUSTRAÇÃO Gemini | botão ILUSTRAÇÃO | `final-runtime.js` → `GeminiBridge.openGeminiScenePrompt` → app Gemini → ACTION_SEND → `MainActivity` → `importSharedSceneImage` | alvo da cena persiste; imagem compartilhada é roteada para a cena | P91 focal 6/6 | CORRIGIDO E RETESTADO |
| NPC: retrato Gemini | gerar retrato | `NpcTokenBridge.openGeminiTokenPrompt` → alvo persistido → ACTION_SEND → `MainActivity` → `importSharedTokenImage` | retorno é roteado para NPC individual; import assíncrono | P91 focal 6/6 | CORRIGIDO E RETESTADO |
| Android share-back | Compartilhar imagem para Star Wars | Manifest `ACTION_SEND image/*` + `singleTop` + `onNewIntent` | app agora pode ser destino de compartilhamento e retomar alvo pendente | P91 focal + Manifest XML OK | CORRIGIDO E RETESTADO |
| TTS Braseiro Kokoro | engine/voz configurada | `TtsBridge` → engines instaladas → `getVoices()` | pacote legado/DB Next-gen migra para Sherpa; UI mostra `Braseiro Kokoro PT-BR · neural`; sem vozes fabricadas | P91 focal 6/6 + high-risk | CORRIGIDO E RETESTADO |
| TTS neural | narrar cena | chunker 250–650 → fila → `onDone` → próximo bloco | pipeline mantém blocos naturais, prewarm e parâmetros neural existentes | `r54-p76-tts-kokoro-pipeline` + high-risk P91 | PASSOU |
| Gradle wrapper | `./gradlew assembleDebug` | wrapper → Gradle 8.13 | removida dependência de `github.com`; usa `downloads.gradle.org` | P91 focal; tentativa de build registrada | CORRIGIDO E RETESTADO |
| Build Android real | `./gradlew assembleDebug` | wrapper precisa obter Gradle | não chegou à compilação por `UnknownHostException: downloads.gradle.org` | `P91_ANDROID_BUILD_RETRY.txt` | SEM EVIDÊNCIA |
| Share-back em aparelho | Gemini → Compartilhar → Star Wars | Android Activity/URI real | código/contrato cobertos, mas não executado em dispositivo nesta rodada | sem aparelho | SEM EVIDÊNCIA |
| Reprodução Kokoro em aparelho | engine Sherpa instalada → áudio real | Android TextToSpeech/Sherpa | contrato coberto; engine real não disponível no ambiente de auditoria | sem engine/aparelho | SEM EVIDÊNCIA |
| Gemini remoto real | chamada/app/credencial externa | bridge + app/rede | bridge coberto; resposta real depende de app/rede/chave | sem credencial/aparelho | SEM EVIDÊNCIA |

## Erros comprovados encontrados e corrigidos

1. `final-runtime.js` chamava `BraseiroGemini.openGeminiScenePrompt(...)`, mas o método não existia em `GeminiBridge.java`; o botão ILUSTRAÇÃO falhava para fallback.
2. O Manifest não registrava `ACTION_SEND image/*` e `MainActivity` não tratava `onNewIntent`; portanto o fluxo Gemini → Compartilhar → Star Wars não podia retornar imagens ao VTT.
3. O alvo de cena/NPC para share-back não tinha persistência própria entre Activity/recriação. Criado `ExternalImageShareState` com SharedPreferences.
4. Importação de retrato de NPC podia executar cópia/IO no fluxo da Activity. Foi movida para executor dedicado.
5. TTS mantinha preferência antiga `com.braseiro.kokoro.ptbr`/DB Next-gen sem migração para o Sherpa atual, podendo inicializar engine inexistente. Agora migra dinamicamente e só lista `TextToSpeech.getVoices()`.
6. O wrapper Gradle apontava para distribuição hospedada em `github.com`, contrariando a restrição operacional e causando falha imediata no ambiente. Alterado para `downloads.gradle.org` com o SHA já fixado.

## Gates executados

- P91 focal Android/share/Kokoro: **6/6 PASS**.
- Fluxos de alto risco (63 arquivos de teste): **256/256 PASS**.
- Sob um Sol Negro: **106/106 PASS**.
- Além da Orla: **57/57 PASS**.
- Regressão completa em 4 shards: **268 + 228 + 249 + 280 = 1025/1025 PASS, 0 FAIL**.
- Grafo de aventuras: Sob um Sol Negro **17/17** nós alcançáveis; Além da Orla **26/26**; zero destinos quebrados.
- `node --check`: runtime/bundle PASS.
- Manifest XML: parse PASS; sanity estrutural dos Java modificados PASS.
- Build Android completo: **SEM EVIDÊNCIA**, bloqueado antes da compilação por ausência de DNS/rede para `downloads.gradle.org`.

## Arquivos principais alterados

- `app/src/main/java/com/braseiro/starwarsedge/GeminiBridge.java`
- `app/src/main/java/com/braseiro/starwarsedge/MainActivity.java`
- `app/src/main/java/com/braseiro/starwarsedge/NpcTokenBridge.java`
- `app/src/main/java/com/braseiro/starwarsedge/TtsBridge.java`
- `app/src/main/java/com/braseiro/starwarsedge/ExternalImageShareState.java` (novo)
- `app/src/main/AndroidManifest.xml`
- `app/src/main/assets/vtt/r46/final-runtime.js`
- `gradle/wrapper/gradle-wrapper.properties`
- `tests/gradle-wrapper-bootstrap.test.mjs`
- `tests/r54-p91-android-share-kokoro-audit.test.mjs` (novo)
- builders/version metadata para 1.0.91.
