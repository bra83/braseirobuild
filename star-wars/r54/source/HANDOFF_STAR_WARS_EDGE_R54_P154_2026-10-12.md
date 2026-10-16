# Braseiro Star Wars VTT — R54 P154 | 12/10/2026

## Baseline e integridade

- Base imutável: `BRASEIRO_STAR_WARS_EDGE_R54_P153_SOURCE_CHECKPOINT.zip`, SHA-256 `660dab26e3bda0f3a6a5040d24baadad4de802eca3442641a7a7fab6c18295ce`.
- Checkpoint produzido: `BRASEIRO_STAR_WARS_EDGE_R54_P154_SOURCE_CHECKPOINT.zip` (projeto completo Android Studio; não é APK).
- `app/build.gradle.kts`: `versionCode=10154`, `versionName=1.0.154`. Shell e bundle offline alinhados em 1.0.154; bundle de 79 módulos.
- Fontes Java/TTS permanecem idênticas ao baseline P150 (8 SHA-256 verificados no audit). Modelos ONNX não foram incluídos.

## Implementação real P154

1. **Age of Rebellion — cinco novas posições mecânicas** (de 247/360 para **252/360**): `EXPLOSIÓN POTENTE` (2 posições), `DERRIBO` (2), `BRAZO FUERTE` (1). Nenhum dos outros 108 slots foi marcado como implementado sem consumidor funcional.
2. **Explosão Potente:** dano adicional por rank exclusivamente na ativação da qualidade `Blast` no mesmo orçamento de Vantagens, em acerto ou erro, aplicado às vítimas corretas. Não altera permanentemente os dados da arma. Uma ativação não pode ser repetida; rollback integral se falhar a gravação.
3. **Derrubada:** ação de talento no controlador e no Android, condicionada a acerto corpo a corpo e gasto de 1 Triunfo do recibo de ataque. Aplica `prone` no alvo, consome Triunfo no orçamento compartilhado e persiste. Reabertura não permite ativar o mesmo ataque duas vezes. Qualidade de arma `Knockdown` continua com orçamento de Vantagens independente.
4. **Braço Forte:** aumenta em uma faixa o alcance efetivo de granadas e armas arremessáveis de ataque pessoal, apenas para personagens com o talento e sem modificar o alcance salvo no inventário. A dificuldade de ataque usa a faixa efetiva.
5. **Universal Adventure Engine:** preservados os gatilhos com contexto, objetivos, consequências, recibos idempotentes, validação causal e rollback P153. Cinco aventuras testadas com início, escolha, replay de ação e três reaberturas cada.
6. **Interface:** botão de Derrubada condicionado a talento, acerto corpo a corpo e Triunfo restante; método correspondente exposto no bridge WebView. UI PT-BR mantida.

## Evidências executadas

- `evidence/P154/NODE_REGULAR.txt`: **1914/1914 PASS**, 268 arquivos.
- `evidence/P154/NODE_STRESS.txt`: **6/6 PASS**.
- `evidence/P154/P154_FOCAL_TESTS.txt`: **18/18 PASS**, incluindo combate real, rollback de falha de gravação, orçamento único, UI, cinco aventuras e reaberturas.
- `evidence/P154/KOKORO_TTS_REGRESSION.txt`: **27/27 PASS** (testes de código, não áudio físico).
- `evidence/P154/JAVA_SYNTAX.txt`: **8/8 PASS** em sintaxe, sem compilação Android por falta de SDK/Gradle baixado.
- `evidence/P154/RELEASE_AUDIT.json`: `allPassed=true`, cinco aventuras e cobertura AoR 252/360.
- `evidence/P154/BUNDLE_BUILD.txt`: **79 módulos**.
- `evidence/P154/AOR_PENDING_SLOTS.json`: 108 posições restantes agrupadas por talento e fonte.

## Bloqueios reais e pendências

- **AoR: 108/360 posições mecânicas pendentes.** Outras expansões EotE/AoR não foram cotejadas integralmente com os PDFs.
- Cinco aventuras: validação estrutural e de fluxo testada, mas **não houve comparação integral cena a cena com os PDFs oficiais**. Combate oficial de *Além da Orla* continua sem certificação.
- **APK não compilado**: tentativa local `sh gradlew :app:assembleDebug --offline` falhou antes da compilação porque o Gradle wrapper tentou baixar `downloads.gradle.org` e não havia rede/cache local. Ver `evidence/P154/ANDROID_GRADLE_ATTEMPT.txt`.
- Sem instalação e homologação física Galaxy S23 Ultra/A57; sem reprodução real de Kokoro.

## Comandos de reprodução

```bash
node tools/build-ui-bridge-bundle.mjs
node tools/audit-p154-release.mjs
node --test tests/uae-p154-aor-combat-talents-five-adventures.test.mjs
python tools/run-node-suite.py --regular
python tools/run-node-suite.py --stress
node --test tests/*tts*.test.mjs tests/*kokoro*.test.mjs
node tools/java-syntax-gate.mjs
```

**Próximo bloco:** priorizar as 108 posições AoR com fonte, depois cotejo de PDFs e combate de *Além da Orla*, e compilar APK no runner GitHub self-hosted com Android SDK. Não confundir os gates de código com certificação de livros ou dispositivo.
