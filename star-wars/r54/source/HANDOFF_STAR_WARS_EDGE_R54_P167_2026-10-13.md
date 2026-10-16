# Braseiro Star Wars VTT — R54 P167 | 13/10/2026

## Base e artefatos

- **Base canônica imutável:** `BRASEIRO_STAR_WARS_EDGE_R54_P166_SOURCE_CHECKPOINT.zip`.
- **Checkpoint de continuidade:** `BRASEIRO_STAR_WARS_EDGE_R54_P167_SOURCE_CHECKPOINT.zip`, projeto completo Android Studio, **não é APK**.
- **Android:** `versionCode=10167`, `versionName=1.0.167`, shell e diagnóstico sincronizados; bundle offline reconstruído a partir de **79 módulos**.
- **Kokoro/TTS:** oito fontes Java originais idênticas por SHA-256 às de P166. Testes de código aprovados; **áudio físico não homologado**.

## Código implementado em P167

### Universal Adventure Engine (UAE)

1. **Objetivos com dependências declarativas:** `initialObjectives[id].requires` suporta lista de objetivos precedentes, verificação de existência, duplicação e ciclos em pré-validação. `completeObjective` e `updateObjectiveProgress` impedem conclusão antecipada e revertem a transação inteira; o checkpoint rejeita objetivos concluídos sem pré-requisitos.
2. **Consequências com resolução declarativa:** `trigger.consequence.resolveWhen` exige condição de resolução, e `onResolveEffects` aplica efeitos oficiais de resolução, com relógios, flags, objetivos e eventos na **mesma transação atômica**. Repetições não reaplicam efeitos; falhas desfazem a resolução e os efeitos anteriores.
3. **Proveniência:** scripts de resolução são recuperados do módulo da aventura já validado; **não** são executados a partir de metadados injetados no save. Não persistir `onResolveEffects` ou `resolveWhen` no registro da consequência. Preservada compatibilidade com módulos antigos sem esses campos.
4. **Cinco aventuras:** 25 testes de integração novos com os adaptadores *Under a Black Sun*, *Beyond the Rim*, *Long Arm of the Hutt*, *Jewel of Yavin* e *Mask of the Pirate Queen*, abrangendo rollback, pré-requisitos, gatilhos, resolução, três reaberturas e tentativa de falsificação. **Não equivale a cotejo editorial integral dos PDFs.**

### Age of Rebellion (AoR)

5. **Sóis Familiares / Familiar Suns (2 posições):** apenas compras autênticas das árvores AoR ativam uma manobra por sessão. Faz teste Difícil (3) de Conhecimento Borda Exterior ou Mundos do Núcleo, consome o uso mesmo na falha, salva o resultado e sinaliza ao Mestre quais informações ambientais revelar somente em caso de sucesso. Não inventa fatos planetários. Controle Android PT-BR e ponte offline integrados.
6. **Fortaleza Mental / Mental Fortress (2 posições):** 1 Destino luminoso, encontro ativo, suprime penalidades de Ferimentos Críticos apenas em testes de Intelecto/Astúcia até o fim do encontro; não cura ferimentos nem remove incapacitação. Integrado a críticos, combate, persistência, encerramento/reinício de encontro, UI e ponte Android. Compra falsificada não concede efeito.
7. **Cobertura executável AoR:** **304/360 = 84,44%**; **56 posições** ainda sem consumidores completos (`evidence/P167/AOR_PENDING_56_POSITIONS.json`). Não declarar certificação por PDF sem cotejo.

## Evidência e gates

- `evidence/P167/NODE_REGULAR.txt`: **2.320/2.320**, 284 arquivos de testes.
- `evidence/P167/NODE_FOCAL.txt`: **35/35** novos testes P167.
- `evidence/P167/NODE_STRESS.txt`: **6/6** suítes.
- `evidence/P167/KOKORO_TTS_REGRESSION.txt`: **27/27** verificações de código.
- `evidence/P167/JAVA_SYNTAX.txt`: **8/8** fontes Java analisadas sintaticamente; não substitui Gradle/Android.
- `evidence/P167/JAVA_HASH_COMPARE.txt`: **8/8** Java idênticos a P166.
- `evidence/P167/ANDROID_GRADLE_ATTEMPT.txt`: compilação **não concluída** porque o wrapper não conseguiu obter a distribuição Gradle neste ambiente. Nenhum APK foi produzido/homologado.
- Os testes legados P154, P160 e P166 tiveram somente as **asserções históricas de cobertura negativa/exata** ajustadas para aceitar talentos agora implementados; os demais testes e verificações continuam ativos.

## Pendências reais

1. Implementar e validar as **56 posições AoR** restantes com efeitos reais, não apenas aliases; certificar AoR/EotE e expansões diretamente contra PDFs fornecidos.
2. Comparar cenas, escolhas, estatísticas, encontros, recompensas e progressão das cinco aventuras com os PDFs oficiais. Testes de runtime **não** são certificação editorial.
3. Compilar Android com Gradle e SDK em ambiente disponível (GitHub runner BREGA/BRELGA), instalar em Galaxy S23 Ultra/A57, verificar Kokoro/TTS real, offline e três reaberturas.

## Reproduzir

```bash
node tools/build-ui-bridge-bundle.mjs
node --test tests/uae-p167-*.test.mjs
python tools/run-node-suite.py --regular
python tools/run-node-suite.py --stress
node --test tests/*tts*.test.mjs tests/*kokoro*.test.mjs
node tools/java-syntax-gate.mjs
bash gradlew :app:assembleDebug --offline
```

**Continuar de P167, sem substituir implementação por relatório e sem regredir P166.**
