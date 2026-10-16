# Braseiro Star Wars VTT — R54 P138 (11/10/2026)

## Baseline

Base canônica exclusiva: `BRASEIRO_STAR_WARS_EDGE_R54_P137_SOURCE_CHECKPOINT.zip`, SHA-256 `e3eaee0ac7a8c592b77dd5fe413437a87bd5dc5ecb9adc5bb38efb89974f2f37`. O ZIP P137 original não foi alterado. Versão Android `versionCode=10138`, `versionName=1.0.138`; shell offline `1.0.138`. Bundle offline recompilado com **74 módulos**.

## Código implementado nesta rodada

1. **Universal Adventure Engine: idempotência persistente de comandos.** `AdventureRuntime.dispatchEvent(eventType, payload, source, {idempotencyKey})` agora impede a reaplicação do mesmo comando após crash/reabertura; rejeita reutilização divergente da chave; trata corretamente diferenças de ordem das propriedades do payload; verifica duplicatas em eventos pendentes e no journal persistido; e preserva rollback transacional em erro. Eventos sem chave continuam compatíveis com snapshots e chamadas anteriores. `createCanonicalEvent` armazena a chave somente quando fornecida. `validateAdventureCheckpoint` rejeita chaves inválidas e identidades duplicadas no journal.
2. **Cinco aventuras:** regressões de idempotência e persistência sobre `under-black-sun`, `beyond-the-rim`, `long-arm-of-the-hutt`, `jewel-of-yavin` e `mask-of-the-pirate-queen`. Três reaberturas e replay por aventura; flags, contadores, objetivos, journal e triggers preservados. Os módulos narrativos oficiais não foram alterados e nenhum encontro/recompensa foi inventado.
3. **AoR — seis posições adicionais, três famílias:** `INVESTIGADOR` (Researcher, 2 posições: remoção de Contratempos por nível em perícias de Conhecimento; pesquisa na metade do tempo), `RASTREADOR EXPERTO` (Expert Tracker, 2 posições: remoção de Contratempos por nível apenas no contexto de rastreamento em Sobrevivência; tempo pela metade), `APTITUD TECNOLÓGICA` (Technical Aptitude, 2 posições: 25% menos tempo por nível em tarefas de Computadores). Os efeitos são calculados a partir de compras legítimas nas árvores e persistidos no resultado do teste. A API retorna a duração ajustada; **não** altera silenciosamente o relógio global do mundo.
4. **Interface Android:** novo painel contextual de tarefas AoR, disponível apenas quando o personagem adquiriu um dos três talentos, com seleção de perícia/contexto, dificuldade, Contratempo e duração; resultados exibem tempo estimado e Contratempos removidos. O fluxo Kokoro/TTS não foi alterado.
5. **Cobertura AoR:** **211/360 posições mecanicamente ativas** (205 no P137); **149 pendentes**. A suíte histórica P137 mantém suas validações específicas, alterando somente a asserção de contagem fixa para um piso `>=205` para permitir evolução posterior.

## Testes executados

- `python3 tools/run-node-suite.py --regular`: **1.452/1.452 PASS**, 251 arquivos, incluindo 18 novos testes P138.
- `python3 tools/run-node-suite.py --stress`: **6/6 PASS**.
- `node --test --test-reporter=tap tests/*tts*.test.mjs tests/*kokoro*.test.mjs`: **27/27 PASS**.
- `node tools/build-ui-bridge-bundle.mjs`: **74 módulos**; `node --check` no bundle e scripts alterados: PASS.
- `node tools/java-syntax-gate.mjs`: PASS sintático para 8 fontes Java; 699 símbolos Android não resolvidos neste ambiente sem SDK.
- JSON: 22 arquivos parseados; XML: 2 arquivos parseados — PASS.
- Checkpoint e patch: ZIP completo, SHA-256 e aplicação do patch sobre P137 verificados na entrega.

## Limites e retomada

- A idempotência depende de o chamador fornecer a **mesma** `idempotencyKey` em retries; chamadas legadas sem chave preservam comportamento anterior. Integrações Android que reenviam ações devem passar um ID persistente por ação, não por tipo de evento.
- A duração de tarefas AoR é estimativa retornada ao Mestre; ainda falta integrar o débito de tempo ao relógio do mundo, com controle contra dupla aplicação.
- **149 posições AoR** ainda sem mecânica completa; expansões EotE/AoR ainda sem certificação integral contra PDFs oficiais, não presentes no ZIP.
- **Além da Orla**: combate dos encontros oficiais ainda não certificado/implementado; `encounters:false` permanece. Cinco aventuras possuem integração UAE, não certificação integral de conteúdo narrativo.
- **Android**: APK não compilado; falta execução no runner BRELGA/Android SDK, instalação em Galaxy S23 Ultra e A57 e teste físico de Kokoro/TTS.

Retomar **P138** como base, **P137** como fallback; não declarar 100% antes da cobertura oficial de regras/aventuras e homologação do APK.
