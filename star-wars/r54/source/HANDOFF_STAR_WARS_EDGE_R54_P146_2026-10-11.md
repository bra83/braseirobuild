# Braseiro Star Wars VTT — R54 P146 | 11/10/2026

## Base e checkpoint

- Base exata: `BRASEIRO_STAR_WARS_EDGE_R54_P145_SOURCE_CHECKPOINT.zip` (SHA-256 `e52c92daabba6648691cb6f0f01ee952c20fe13a6e2523ff45bef6a45b86a419`).
- Projeto Android Studio integral: `BRASEIRO_STAR_WARS_EDGE_R54_P146_SOURCE_CHECKPOINT.zip`.
- Versão Android: `versionCode=10146`, `versionName=1.0.146`; shell offline `data-version=1.0.146`.
- Bundle offline regenerado a partir das fontes `.mjs` canônicas: **78 módulos**.
- Nenhuma fonte Java/Kokoro/TTS foi modificada; nenhum modelo ONNX foi incorporado.

## Mudanças executadas

1. **Outbox durável de ações publicadas no Android/WebView**: `r46/published-command-client.js` grava `campaignId`, `actionId`, assinatura do comando e payload antes de executar. Após queda da WebView ou ACK perdido, reabre e repete o **mesmo ID**, recuperando o recibo sem repetir rolagem, escolha, encontro, recompensa ou finalização de combate. Bloqueia ação de outra campanha, comando pendente corrompido e novas ações enquanto uma anterior está ambígua. Falha de gravação do outbox impede execução. Falhas determinísticas sem recibo liberam nova tentativa.
2. **UI realmente conectada ao contrato P145**: os botões de início de qualquer uma das cinco aventuras, escolhas, testes e encontros agora chamam `executePublishedAdventureCommand` via outbox. Os dois botões de fim de combate publicado usam o mesmo protocolo, preservando a rota legada do combate sandbox. O shell carrega o cliente antes da UI; recuperações pendentes são exibidas no painel.
3. **Gate de causalidade entre duas persistências**: `validatePublishedActionCausality` verifica cada `PUBLISHED_COMMAND_COMMITTED` no journal UAE contra recibo único, tipo, aventura, chave idempotente e ordem. Importação e validação de campanha rejeitam eventos órfãos, recibos adulterados e sequências incompatíveis.
4. **Identidade da execução da aventura**: recibos P146 recebem `runKey` derivado do checkpoint corrente; um recibo desta execução sem evento causal correspondente é rejeitado. Recibos P145 legados permanecem aceitos, e reiniciar deliberadamente a aventura não reutiliza recibos antigos.
5. **Regressão de testes legados**: três testes estáticos antigos foram atualizados para reconhecer a rota de UI segura, sem restaurar chamadas não idempotentes.

## Evidências executadas

- Integração/regressão: **1696/1696 PASS** (260 arquivos).
- Stress: **6/6 PASS** (6 arquivos).
- Total: **1702/1702 PASS** (266 arquivos).
- Testes novos P146: **33/33 PASS**, incluindo as cinco aventuras, queda após commit/ACK perdido, importação adulterada, reinício, outbox e combate.
- Kokoro/TTS: **27/27 PASS** (testes de código, não reprodução física).
- Sintaxe: **128 JS/MJS**, **22 JSON** válidos; **8/8 fontes Java** byte a byte idênticas a P145.
- `evidence/P146/NODE_SUITE_FULL.txt`, `KOKORO_TTS_REGRESSION.txt`, `SYNTAX_AND_PRESERVATION.json` e `VALIDATION.json`.

## Limites reais / próximos gates

- **AoR: 224/360 posições com efeitos executáveis; 136 pendentes**. P146 não implementou novos talentos nem certificou os PDFs de expansões.
- **Cinco aventuras**: fluxo de UI e persistência integrados; a comparação integral de cada cena, consequência, recompensa e encontro com os PDFs oficiais ainda não foi concluída. *Além da Orla* não recebeu combate fictício.
- **Android**: não foi compilado APK nesta execução (SDK Android indisponível). Sem instalação, teste físico, áudio Kokoro real ou homologação S23 Ultra/A57. Não declarar 100%.
- O checkpoint P146 preserva integralmente o P145 e pode ser aberto no Android Studio; executar Gradle/SDK em BRELGA para homologação do APK.

## Como reproduzir

Na raiz do projeto: `node build-ui-bridge-bundle.mjs`; `python tools/run-node-suite.py all`; para TTS, executar os oito arquivos de teste listados em `evidence/P146/KOKORO_TTS_REGRESSION.txt`.
