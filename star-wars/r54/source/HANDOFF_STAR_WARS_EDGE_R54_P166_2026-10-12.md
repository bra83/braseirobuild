# Braseiro Star Wars VTT — R54 P166 | 12/10/2026

## Checkpoint canônico

- **Base imutável:** `BRASEIRO_STAR_WARS_EDGE_R54_P165_SOURCE_CHECKPOINT.zip`.
- **Novo checkpoint:** `BRASEIRO_STAR_WARS_EDGE_R54_P166_SOURCE_CHECKPOINT.zip`, projeto-fonte completo para Android Studio; **não é APK**.
- **Android:** `versionCode=10166`, `versionName=1.0.166`, HTML e painel de diagnóstico sincronizados.
- **Bundle offline:** 79 módulos reconstruídos dos fontes; oito Java originais de Kokoro/TTS têm SHA-256 idêntico ao P165.

## Implementação real P166

1. **AoR: Faz-tudo / Gearhead (MANITAS, 2 posições).** Identidade dos talentos vinculada aos IDs e nomes originais da árvore `engineer:scientist:r2c4` e `engineer:scientist:r4c4`; remove um dado de Contratempo por graduação em testes de Mecânica; em tentativa de Mod de attachment, o preço de 100 créditos é reduzido uma única vez para 50 créditos, sem desconto adicional por segunda graduação. Nomes exibidos e compras falsificadas não conferem efeito. **Cobertura: 300/360 = 83,33%; restam 60 posições** (`evidence/P166/AOR_PENDING_60_POSITIONS.json`). Cobertura de execução, **não** certificação editorial por PDF.
2. **Transações de oficina:** instalação, remoção e tentativa de Mod são atômicas em memória e no repositório; falha de gravação reverte créditos, arma, registro de propriedade e histórico de tentativas. Sucesso e falha de rolagem continuam consumindo exatamente uma tentativa e o custo autorizado.
3. **Android PT-BR:** painel de Mods mostra custo efetivo por opção, inclusive desconto Gearhead, e a ponte offline expõe `gearhead` e `modStatus.cost`. Mantidas compatibilidade, limites de hard points e catálogos mecânicos source-bound.
4. **Cinco aventuras:** testes específicos de gatilhos, objetivos, consequências, relógios, rollback, idempotência e três reaberturas para *Under a Black Sun*, *Beyond the Rim*, *Long Arm of the Hutt*, *Jewel of Yavin* e *Mask of the Pirate Queen*. **Não** substituem cotejo editorial integral dos PDFs.

## Testes efetivamente executados

- **Integração/regressão:** 2.285/2.285, 281 arquivos (`evidence/P166/NODE_REGULAR.txt`). O teste histórico P165 foi atualizado apenas para exigir no mínimo sua cobertura de 298, preservando seus demais asserts.
- **Focais P166:** 13/13 (`evidence/P166/NODE_FOCAL.txt`).
- **Stress:** 6/6 suítes independentes (`evidence/P166/NODE_STRESS.txt`, logs individuais).
- **Kokoro/TTS:** 27/27 verificações de código (`evidence/P166/KOKORO_TTS_REGRESSION.txt`).
- **Java:** 8/8 fontes analisadas sintaticamente, sem compilar Android (`evidence/P166/JAVA_SYNTAX.txt`).
- **APK:** `:app:assembleDebug --offline` não concluiu; wrapper não consegue obter a distribuição Gradle (`evidence/P166/ANDROID_GRADLE_ATTEMPT.txt`). Não declarar APK ou homologação física.

## Próximas ações, sem regressão

1. Implementar e testar as **60 posições AoR** restantes com consumidores reais e cotejo contra livros; depois certificar expansões EotE/AoR.
2. Comparar cada cena, estatística, escolha, recompensa e progressão das cinco aventuras com PDFs oficiais. Não confundir testes estruturais com fidelidade editorial.
3. Compilar com Gradle/SDK disponível (GitHub runner BREGA/BRELGA), instalar e homologar Galaxy S23 Ultra/A57, áudio Kokoro real, offline e três reaberturas.
4. Continuar sempre de P166, preservando P165 como rollback. Não renumerar cobertura sem testes.

## Reproduzir

```bash
node tools/build-ui-bridge-bundle.mjs
node --test tests/uae-p166-gearhead-atomic-five-adventures.test.mjs
python tools/run-node-suite.py --regular
# Stress: rodar cada tests/stress*.test.mjs isoladamente
node --test tests/*tts*.test.mjs tests/*kokoro*.test.mjs
node tools/java-syntax-gate.mjs
bash gradlew :app:assembleDebug --offline
```
