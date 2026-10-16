# HANDOFF — BRASEIRO STAR WARS EDGE OF THE GALAXY — R54 P51
## PERFORMANCE / RAMIFICAÇÕES NEGATIVAS / REGISTRY GENÉRICO DE AVENTURAS

Baseline: **R54 P50**. Trabalho incremental, sem recriação e sem GitHub.

## 1. Correção de performance e persistência
- Reproduzido o bloqueio real de `tests/stress-33-aftermath-r20.test.mjs`: no P50 a execução ultrapassava 120 s.
- Causa identificada: eventos `WORLD_UPDATED` guardavam snapshots integrais de `world` dentro de `state.events`; esses snapshots continham históricos crescentes e eram clonados/serializados de novo a cada commit/reopen.
- `core/events.mjs` agora usa `compactStoredEvent(event)` para manter no audit log apenas um resumo estrutural do `WORLD_UPDATED` (schema/tick/contagens), aplicando ainda o payload integral ao estado autoritativo `state.world`.
- Compactação é idempotente; reabrir um save já compactado não zera contagens.
- `core/state.mjs` compacta também eventos legados durante `migrateState`, sem alterar o schema 14 nem quebrar compatibilidade de saves.
- Resultado: o stress de 33 sessões volta a concluir (~11–14 s nas execuções observadas), preservando 33 sessões/histories/aftermaths e reaberturas repetidas. Não houve aumento de timeout nem skip.

## 2. Sob um Sol Negro — falhas, Threat/Despair e anti-spoiler
- A negociação final com Kaa’to agora pode ser quebrada de forma persistente: falha com Desespero ou Threat severo marca `parleyBroken` + `kaatoAlerted`.
- Depois disso, a via diplomática deixa de expor checks/sugestões de rendição; a UI fornece feedback PT-BR player-safe e resta a rota de confronto.
- Save/reopen preserva o rompimento da negociação.
- NovaCORE possui cobertura explícita de falha + Threat + Despair: risco industrial crítico e consequência mecânica de strain persistem após reabertura.
- Gating anti-spoiler continua baseado em estado conhecido pelo jogador; ações futuras bloqueadas não são oferecidas ao Mestre/Gemini.

## 3. Arquitetura genérica para aventuras publicadas
- Adicionado `adventure/publishedAdventureRegistry.mjs`.
- O registry/adapters agora concentram catálogo, compatibilidade de modo, status, início, avanço, cena atual, request do Mestre, testes, adjudicação, encontros, resultados, recompensas, arquivamento e continuação.
- `core/controller.mjs` foi desacoplado das chamadas diretas de Under a Black Sun para essas operações e passa pelo adapter da aventura ativa.
- Under a Black Sun continua compatível com **EotE e MIXED**, não AOR puro; contratos de **Duty/Obligation** foram preservados.
- Recompensas e continuação continuam iguais para UABS, mas agora são política do adapter e não regra hardcoded exclusiva do controller.
- O desenho permite inserir a próxima aventura sem duplicar o motor principal.

## 4. Bundle e gates
- `uiBridge.bundle.js` reconstruído: **60 módulos**.
- Syntax gates PASS para `events.mjs`, `state.mjs`, `underBlackSun.mjs`, `publishedAdventureRegistry.mjs`, `controller.mjs` e bundle.
- Novo teste `r54-p51-persistence-performance-and-adventure-registry.test.mjs`: 5/5 PASS.
- Suíte focal P51: **71/71 PASS, 0 FAIL** (`P51_FOCAL.txt`).
- Regressão ampla: **176 arquivos, 877/877 PASS, 0 FAIL** (`P51_BROAD_SUMMARY.txt`).
  - O primeiro shard 6 encontrou 1 falha: teste estrutural P33 ainda exigia a antiga chamada direta `adjudicateUnderBlackSunCheck`.
  - O teste foi atualizado para validar o novo contrato registry/adapter e o shard foi rerodado: 111/111 PASS.
  - O primeiro loop externo atingiu o limite da ferramenta durante o shard 8; ele foi rerodado isoladamente e confirmado 81/81 PASS. Não houve conversão de timeout em PASS.

## 5. Android/Gradle
- Nenhum Java/Gradle foi alterado neste checkpoint.
- Não foi feita compilação Android desnecessária.
- O ambiente continua sem Gradle local; a evidência anterior de build (`R54_P21_ANDROID_BUILD.txt`) registra `UnknownHostException: services.gradle.org`, e o wrapper requer Gradle 8.13.
- Portanto **APK/Android build NÃO está declarado PASS**. Os gates estáticos Android do repositório passaram dentro da suíte focal, mas não substituem compilação nativa.

## 6. Material disponível para próxima aventura no Google Drive
Localizados no acervo Star Wars acessível:
- `Beyond the Rim.pdf`
- `The Jewel of Yavin.pdf`
- `mask of the pirate queen.pdf`
Também existem suplementos EotE associados. Nenhuma dessas aventuras foi declarada como próxima escolha do usuário neste checkpoint.

## Próxima prioridade
1. Implementar a primeira fatia da próxima aventura através do novo registry, mantendo anti-spoiler e persistência genéricos.
2. Continuar testes de consequências/Threat/Despair em perseguições, rotas sociais e transições entre aventura e mundo livre.
3. Compilar Android somente quando houver mudança nativa ou marco de release e houver toolchain/rede funcional.
