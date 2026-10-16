# Braseiro Star Wars VTT — R54 P165 | 12/10/2026

## Checkpoint canônico

- **Base:** `BRASEIRO_STAR_WARS_EDGE_R54_P164_SOURCE_CHECKPOINT.zip` (preservado).
- **Novo checkpoint:** `BRASEIRO_STAR_WARS_EDGE_R54_P165_SOURCE_CHECKPOINT.zip`, fonte integral para Android Studio, não APK.
- **Versão Android:** `versionCode=10165`, `versionName=1.0.165`, `r46-shell.html` e diagnóstico sincronizados.
- **Bundle offline:** 79 módulos, reconstruído a partir dos fontes locais.
- **Kokoro/TTS:** oito arquivos Java originais preservados sem alteração.

## Implementação efetiva

1. **AoR Gunner:** 3 posições adicionais com consumidor real — `ABRUMAR DEFENSAS` (2 graduações) e `DISPARO DEBILITANTE` (1). Cobertura AoR: **298/360 = 82,78%**; **62 pendentes**, enumeradas em `evidence/P165/AOR_PENDING_62_POSITIONS.json`. Nenhuma certificação editorial integral declarada.
2. **Sobrecarregar defesas:** somente após ataque veicular sem acerto; consome duas Vantagens por ativação, limitada pelas graduações autenticadas e defesa restante na zona atingida; efeito persistente até fim do encontro; não reduz abaixo de zero.
3. **Disparo debilitante:** somente após acerto veicular; consome duas Vantagens; reduz em 1 a velocidade máxima efetiva até o final da rodada seguinte, sem reduzi-la abaixo de 1; ajusta velocidade atual se necessário. A duração sobrevive à restauração.
4. **Orçamento único de resultados:** Guiada, Vinculada, críticos e os novos talentos compartilham Vantagens reais do ataque; impedido gasto duplicado, uso sem talento adquirido ou alteração retroativa do registro de compra.
5. **Persistência transacional:** `resolveVehicleAttack` e `activatePendingVehicleQuality` passaram a usar `atomicAction` para reverter estado em falha de gravação. Importação de efeitos AoR veiculares malformados é rejeitada.
6. **Android PT-BR:** painel de qualidades oferece botões contextuais para os novos talentos, exibe velocidade máxima efetiva e defesa do alvo, e atualiza automaticamente após cada gasto.
7. **Cinco aventuras:** testes dos cinco adaptadores publicados para gatilhos, consequências, relógios, objetivos, idempotência, rollback e três restaurações. Isto **não** substitui conferência textual contra os PDFs oficiais.

## Evidências

- `evidence/P165/NODE_REGULAR.txt`: **2.272/2.272**, 280 arquivos, regressão e integração.
- `evidence/P165/NODE_FOCAL.txt`: **18/18** testes novos P165.
- `evidence/P165/NODE_STRESS.txt`: **6/6** suítes de stress, executadas isoladamente em paralelo.
- `evidence/P165/KOKORO_TTS_REGRESSION.txt`: **27/27** verificações de código, não teste físico de áudio.
- `evidence/P165/JAVA_SYNTAX.txt`: **8/8** fontes Java sintaticamente analisadas; não é compilação Android.
- `evidence/P165/ANDROID_GRADLE_ATTEMPT.txt`: Gradle não compilou; `UnknownHostException: downloads.gradle.org` mesmo em modo offline.

## Pendências e próximos blocos

1. **62 posições AoR** ainda sem consumidores executáveis; priorizar os grupos por especialização e validar as regras com PDFs oficiais antes de certificá-las.
2. Certificar as expansões **EotE/AoR** e confrontar integralmente as **cinco aventuras** com os livros (conteúdo, estatísticas, consequências, progressão, recompensas).
3. Executar `:app:assembleDebug` em ambiente com distribuição Gradle e SDK (runner self-hosted BREGA/BRELGA), instalar no Galaxy S23 Ultra/A57 e testar três reaberturas, TTS Kokoro real e fluxo offline.
4. Não regredir para P164 nem contabilizar testes de código como homologação física ou editorial.

## Reproduzir

```bash
node tools/build-ui-bridge-bundle.mjs
node --test tests/uae-p165-aor-gunner-vehicle-five-adventures.test.mjs
python tools/run-node-suite.py --regular
python tools/run-node-suite.py --stress
node --test tests/*tts*.test.mjs tests/*kokoro*.test.mjs
node tools/java-syntax-gate.mjs
bash gradlew :app:assembleDebug
```

**P165 é o novo checkpoint canônico recuperável.**
