# Braseiro Star Wars VTT — R54 P157 | 12/10/2026

## Baseline e entrega

- Baseline exata: `BRASEIRO_STAR_WARS_EDGE_R54_P156_SOURCE_CHECKPOINT.zip` (SHA-256 `950d27aab69a706dab17a7ae0a21d7dded1d1b3299c5c278300eb9af648bd10f`).
- Versão Android Studio: `app/build.gradle.kts` → `versionCode=10157`, `versionName=1.0.157`; shell offline 1.0.157.
- Bundle offline recompilado de fontes canônicas `.mjs`: 79 módulos.
- P156 não foi sobrescrito; oito fontes Java Kokoro/TTS mantidas com hash idêntico ao baseline de P151.

## Código implementado

1. **AoR — oito posições novas**, cobertura 262/360 (72,8%). Talentos de Comandante/Diplomático: Finta (2), Don de gentes (2), Indistinguível (2), Chanchullero (2). Crosswalk e limites em `evidence/P157/AOR_SOURCE_CROSSWALK.md`.
2. **Finta em combate real**: manobra com custo de Tensão, orçamento por graduação, uso único por rodada, melhorias de dificuldade somente para ataques à distância, efeito válido até o próximo turno, integração com ataques NPC e persistência transacional. Bloqueia usos inválidos e reverte em erro de gravação.
3. **Diplomacia e identidade**: Don de gentes remove Contratempos de Carisma/Liderança; Indistinguível melhora dificuldade dos testes de NPC para reconhecer o personagem, com resultado registrado e reabertura.
4. **Economia estruturada**: Chanchullero aumenta o preço-base de vendas legais em 10% por graduação, sem bônus em venda ilegal/restrita; validação de `legalSale` no contrato do Mestre, crédito, remoção do ativo, histórico e recuperação. Preview `legalSaleQuote` não muta o estado.
5. **Android PT-BR**: ponte JS e contrato TypeScript expõem Finta, identificação e cotação; painel com três ações; preservados controles anteriores, inclusive relógio imperial, encontros e Kokoro.
6. **Cinco aventuras**: estrutura, semântica, escolha inicial, idempotência e três reaberturas por aventura; não substitui conferência editorial integral.

## Testes realmente executados

- **2035/2035** testes de integração/regressão Node em 272 arquivos: `evidence/P157/NODE_REGULAR.txt`.
- **6/6** testes de stress: `evidence/P157/NODE_STRESS.txt`.
- **54/54** testes focais P157 (incluídos nos 2035): `tests/uae-p157-diplomat-commander-economy.test.mjs`.
- **27/27** Kokoro/TTS (testes de código): `evidence/P157/KOKORO_TTS_REGRESSION.txt`.
- **8/8** fontes Java passaram no gate de sintaxe: `evidence/P157/JAVA_SYNTAX.txt` (símbolos Android não resolvidos sem SDK).
- `tools/audit-p157-release.mjs`: `allPassed=true`; 5 aventuras, 4 encontros, 79 módulos, 8 fontes Java preservadas.
- Build Android tentada com `bash gradlew :app:assembleDebug --offline --no-daemon`; falhou antes da compilação porque `downloads.gradle.org` não resolve no ambiente (`UnknownHostException`). Sem APK nem homologação física.

## Pendências honestas

- **AoR: 262/360 posições, 98 restantes.** Expansões EotE/AoR não certificadas integralmente.
- Cinco aventuras ainda sem comparação integral de cada cena/estatística/consequência com PDFs; *Além da Orla* mantém adversários provisórios.
- APK Android e teste de instalação no S23 Ultra/A57 não executados; áudio Kokoro não reproduzido fisicamente.
- Este ZIP é projeto Android Studio completo, não um APK e não homologação final.

## Reproduzir

```bash
node tools/build-ui-bridge-bundle.mjs
node --test tests/uae-p157-diplomat-commander-economy.test.mjs
python tools/run-node-suite.py --regular
python tools/run-node-suite.py --stress
node --test tests/*tts*.test.mjs tests/*kokoro*.test.mjs
node tools/java-syntax-gate.mjs
node tools/audit-p157-release.mjs
```
