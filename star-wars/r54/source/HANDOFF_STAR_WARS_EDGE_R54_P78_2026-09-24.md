# STAR WARS EDGE R54 — P78 — 2026-09-24

## Baseline
- Entrada exata: `BRASEIRO_STAR_WARS_EDGE_R54_P77_SOURCE_CHECKPOINT.zip` do Google Drive.
- Projeto não recriado.
- Correções de fonte feitas localmente sobre P77; GitHub não foi usado como fonte nem para correção.
- EotE / AoR / MIXED e separação Duty / Obligation preservados.
- Sob um Sol Negro P75+ preservado, incluindo estado persistente e anti-spoiler.

## Defeitos de aparelho reportados e causas encontradas
1. **Novo mundo / voltar à seleção não funcionava**
   - `setupOverlay()` ignorava `?campaignSetup=1` quando o sandbox antigo ainda parecia pronto.
   - Correção: `campaignSetup=1` agora é um comando one-shot de entrada forçada em `screen=00`; estado transitório de sessão/mundo é zerado preservando personagem, grupo, veículos e modo EotE/AoR/MIXED.

2. **Missão começava sem comando do jogador**
   - `adventureStarted()` inferia início a partir de qualquer narração não vazia, inclusive texto inicial/fallback do Mestre.
   - Correção: início agora depende apenas de estado explícito (`publishedAdventureView().active/completed` ou `session.status === 'ativa'`).

3. **Botões difíceis de tocar / texto minúsculo**
   - Existiam alvos de 34 px e pontos de mapa sobrescritos para 13x13 px; textos de testes chegavam a 0.56rem.
   - Correção mobile: ações principais e escolhas >=48 px; pontos de mapa têm hit-target 44x44 com marcador visual separado; textos de narração, situação, testes e resultados ampliados; `touch-action: manipulation` aplicado aos controles.

4. **Teste ficava em “Rolando...” sem resultado**
   - O resultado mecânico era calculado antes da animação, mas ficava oculto aguardando uma Promise de dados que podia demorar/ficar sem evento.
   - Correção: resultado mecânico é mostrado imediatamente e persistido antes da animação; visual de dados usa espera limitada (`boundedVisualRoll`) e não bloqueia a resolução.

5. **Resultado desaparecia em testes da aventura publicada**
   - O handler escrevia o resultado e depois redesenhava o painel ~650 ms depois, apagando o `<output>`.
   - Correção: último teste fica persistido em `status.progress.checkHistory` e é renderizado como `final-published-last-roll`; removido o redraw destrutivo por timeout.

6. **Mestre aparecia OFFLINE de forma enganosa**
   - `sourceInfo()` rotulava o fallback local como OFFLINE e não reconhecia Gemini configurado antes do primeiro turno.
   - Correção: fallback é `MESTRE LOCAL`; Gemini configurado aparece como `GEMINI` imediatamente; HTTP remoto aparece como `MESTRE REMOTO`.

7. **Abertura publicada curta mesmo com Gemini disponível**
   - Começar Sob um Sol Negro não disparava turno do Mestre; a UI exibia apenas o texto autoral curto da cena.
   - Correção: após clique explícito de início, `requestPublishedMasterNarration()` solicita narração PT-BR detalhada da cena atual, mantendo anti-spoiler e sem revelar nós/fatos futuros. Consequências de testes também podem ser narradas pelo Mestre depois da resolução mecânica, sem rerrolar nem ocultar o resultado.

## Arquivos principais alterados
- `app/src/main/assets/vtt/r46/final-runtime.js`
- `app/src/main/assets/vtt/r46/final-runtime.css`
- `app/src/main/assets/vtt/js/core/uiBridge.bundle.js` (rebuild canônico)
- `app/build.gradle.kts`
- `app/src/main/assets/vtt/r46-shell.html`
- `build-ui-bridge-bundle.mjs`
- `tools/build-ui-bridge-bundle.mjs`
- `tests/r54-p78-mobile-session-repair.test.mjs` (novo)
- testes históricos P58/P77/completude ajustados apenas para o contrato/versionamento P78, preservando o comportamento protegido.

## Versão
- Android `versionCode = 10078`
- Android `versionName = 1.0.78`
- shell/runtime/bundle: `1.0.78 / R54 P78`

## Evidência P78
### Gate específico
- `tests/r54-p78-mobile-session-repair.test.mjs`: **8/8 PASS, 0 FAIL**.
- Cobre início explícito, retorno forçado à seleção, reset funcional preservando party/MIXED, resultados persistentes, visual bounded, status do Mestre e legibilidade/touch targets.

### Regressão protegida
- P58/P61/P62/P70/P74/P75/P76/P77/P78 + Duty/Rebellion/completude: **54/54 PASS, 0 FAIL**.

### Regressão canônica final
- `node --test --test-reporter=tap tests/*.test.mjs`: **978/978 PASS, 0 FAIL**.
- Primeira passagem após P78: 977/978, com única falha de consistência porque o builder da raiz ainda gerava marcador 1.0.77. O builder da raiz foi alinhado ao builder em `tools/`; a suíte completa foi repetida e fechou 978/978.

### Bundle / sintaxe
- Builder da raiz e `tools/build-ui-bridge-bundle.mjs`: **byte-a-byte idênticos**.
- Bundle reconstruído: **61 módulos**.
- `node --check` em `final-runtime.js`: PASS.
- `node --check` em `uiBridge.bundle.js`: PASS.

## Android — status de compilação neste ambiente
- `./gradlew --no-daemon :app:assembleDebug` foi tentado no marco P78.
- A compilação **não chegou ao Gradle**: wrapper falhou ao baixar Gradle 8.13 por `java.net.UnknownHostException: github.com`.
- Não existe distribuição Gradle 8.13 em cache local neste ambiente.
- Portanto **Android/APK NÃO é declarado PASS aqui**. O log integral está em `P78_ANDROID_BUILD.txt`.
- Este é bloqueio de infraestrutura, não evidência de erro de compilação da fonte.

## Gates que continuam obrigatórios para APK
1. `assembleDebug` PASS em ambiente com Gradle disponível (BRELGA pode ser usado somente como esteira de build, sem alterar fonte).
2. package `com.braseiro.starwarsedge`.
3. versionCode `10078` / versionName `1.0.78`.
4. `apksigner verify` PASS.
5. APK contém bundle P78, CAMPANHA, Sob um Sol Negro e pipeline TTS/Kokoro P76/P77.
6. Teste físico dos cinco defeitos reportados: seleção, touch targets, início explícito, Mestre/narração e teste→resultado persistente.

## Próximo trabalho real
- Validar P78 em aparelho Android e corrigir qualquer comportamento que dependa de WebView/TTS físico.
- Depois continuar a expansão jogável de Sob um Sol Negro (mais ramificações/estados/consequências) sem regredir o lifecycle P78.
