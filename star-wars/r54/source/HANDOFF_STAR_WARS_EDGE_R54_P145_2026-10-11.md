# Braseiro Star Wars VTT — R54 P145 (11/10/2026)

## Checkpoint e compatibilidade

**Base integral:** `BRASEIRO_STAR_WARS_EDGE_R54_P144_SOURCE_CHECKPOINT.zip`.
**Saída:** `BRASEIRO_STAR_WARS_EDGE_R54_P145_SOURCE_CHECKPOINT.zip`, `versionCode=10145`, `versionName=1.0.145`, shell offline `data-version=1.0.145`.
Bundle Android offline regenerado de fontes canônicos com **78 módulos** (P144: 77). Oito fontes Java Android e módulos Kokoro/TTS preservados byte a byte; nenhum modelo TTS foi incorporado.

## Implementação real P145

1. **Recibos de comandos persistentes, exatamente uma vez:** novo módulo `js/adventure/publishedActionReceipts.mjs` valida e armazena recibos de início, escolha, adjudicação de perícia, abertura e encerramento de combate publicado. Cada recibo contém ID fornecido pelo chamador, assinatura canônica do comando, resposta compacta, sequência e data. Reutilização do mesmo ID com outro comando falha fechada; IDs inválidos, recibos duplicados, assinaturas divergentes e sequência corrompida são rejeitados. Limite explícito de 4096 recibos por campanha; não há descarte silencioso.
2. **Integração transacional:** `GameController.executePublishedAdventureCommand` confirma cena, recompensas, personagem, mundo, combate, recibo e journal UAE numa única gravação; falhas de armazenamento revertem o estado. O mesmo `actionId` repetido após falha/reabertura retorna a resposta persistida sem nova rolagem, recompensa, recuperação de Tensão, transição ou gravação.
3. **Prova causal UAE:** cada confirmação emite `PUBLISHED_COMMAND_COMMITTED` no journal UAE com ID idempotente, tipo e aventura. Repetição de recibo após reinício deliberado da aventura é rejeitada quando a prova causal não pertence ao journal atual.
4. **Cinco aventuras integradas:** rotas existentes `startPublishedAdventureSession`, `advancePublishedAdventure`, `resolvePublishedAdventureCheck`, `beginPublishedAdventureEncounter` e `finishCombat` aceitam `actionId` opcional e encaminham ao executor idempotente. `BraseiroUiBridge.executePublishedAdventureCommand` expõe o contrato diretamente ao Android/WebView. Os chamadores antigos sem `actionId` permanecem compatíveis, mas não ganham idempotência de comando automaticamente.
5. **Importação e restauração:** `migrateState`, `validateState` e reconciliação de aventura validam o livro de recibos antes de normalizar/aceitar o mundo. A reabertura do `GameController` deixa de regravar campanhas já existentes, evitando eventos de storage redundantes.

### Contrato de integração Android

Enviar um `actionId` único, estável e persistido no lado do chamador para cada ação do usuário, reutilizando-o em retries do mesmo comando. O novo método do bridge recebe `{actionId,kind,adventureId,choiceId,checkIndex,encounterId,options}`; `kind` ∈ `start`, `choice`, `check`, `encounter`, `finish-combat`. Também se pode passar `actionId` em `options` aos métodos existentes. Retornos incluem `applied`, `duplicate` e `receiptId`. **Não gerar novo ID ao repetir a mesma operação.**

## Testes e evidência

- Node integração/regressão: **1663/1663 PASS**; stress: **6/6 PASS**; total **1669/1669 PASS** em 265 arquivos.
- Novos testes P145: **26/26 PASS**, cobrindo as cinco aventuras, três reaberturas, replays de escolhas, início, testes automatizados disponíveis, testes manuais bloqueados sem invenção, falhas de disco, conflitos de ID, corrupção, restart, bridge Android e combate publicado.
- Kokoro/TTS: **27/27 PASS** (testes de código; áudio físico não homologado).
- JavaScript: **113 arquivos** verificados sintaticamente; JSON: **22 arquivos** parseados; **8 Java** Android idênticos ao P144; bundle **78 módulos**.
- Evidência: `evidence/P145/NODE_SUITE_FULL.txt`, `KOKORO_TTS_REGRESSION.txt`, `OFFLINE_BUNDLE_BUILD.txt`, `VALIDATION.json`.

## Pendências honestas e prioridades P146

- AoR: **224/360 posições** com efeito executável; **136 ainda pendentes**. P145 não adicionou efeitos AoR nem certificou expansões. Exige PDFs oficiais para mapear e implementar sem inventar regras.
- Cinco aventuras: persistência e comandos testados; homologação de todas as cenas, encontros, recompensas e ramificações **contra PDFs** não concluída. *Além da Orla* continua sem combate oficial certificado; não criar adversários fictícios.
- Android: **APK não compilado**, sem SDK Android nesta execução; BRELGA/instalação e três reaberturas físicas em Galaxy S23 Ultra/A57, além de teste real Kokoro, ainda pendentes.
- Próximo passo: validar as fontes oficiais, implementar o combate publicado de *Além da Orla* e AoR pendente, compilar APK em BRELGA e executar gates Android. Não declarar 100% antes da certificação.
