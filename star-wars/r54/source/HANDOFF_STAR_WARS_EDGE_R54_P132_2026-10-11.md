# BRASEIRO STAR WARS — R54 P132 | checkpoint Android Studio

## Fonte e continuidade

- Base canônica: `BRASEIRO_STAR_WARS_EDGE_R54_P131_SOURCE_CHECKPOINT.zip` (SHA-256 `102cef1165eadbb41796c0787d2246e1ed8f24797834b1f6e746e4c5bdad8f32`).
- Entrega: `BRASEIRO_STAR_WARS_EDGE_R54_P132_SOURCE_CHECKPOINT.zip`; `app/build.gradle.kts`: `versionCode=10132`, `versionName=1.0.132`; shell offline `data-version=1.0.132`.
- **Projeto-fonte, não APK**: requer Android Studio/SDK ou runner self-hosted BRELGA para compilação e testes reais em Galaxy S23 Ultra/A57.
- Não foi refeito o conteúdo das aventuras nem substituídas regras por RAG. A base P131 permanece recuperável separadamente.

## Implementação integrada real

1. **AoR Core / efeitos mecânicos conectados ao motor**: `expansion/aorTalentRuntimeAliases.mjs` é a lista explícita de 36 equivalências entre nomes impressos AoR e IDs internos já consumidos pelo motor EotE. Reutiliza implementações existentes em iniciativa, combate, testes de perícia, veículos, recuperação e ações, sem declarar talentos sem consumidor como concluídos. `talents.mjs` contabiliza ranks AoR por alias sem duplicar o nome exibido, mantém a origem impressa na compra e preserva isolamento EotE. `aorProgressionIntegration.mjs` anuncia apenas os efeitos conectados. Cobertura mecânica declarada no catálogo AoR passou de **72/360 para 156/360 posições**; aquisição das 360 posições segue funcional. **204 posições permanecem sem efeito mecânico completo.**
2. **Passivos e escolhas AoR**: Firmeza aplica +1 ao limiar de Tensão; Sexto Sentido/Reflexos Superiores usam defesa já implementada; Precisão Letal agora exige perícia de combate válida, persiste a escolha e concede bônus calculado pelo motor existente. Dedicação, Endurecido, Resistente e Compostura permanecem preservados.
3. **UAE / atomicidade**: `AdventureRuntime.applyEffects` agora reverte toda a operação em erro, mesmo quando chamada diretamente, e não converte `delta=0` em `+1`. Valores de contador não finitos são rejeitados, evitando corrupção silenciosa. A lógica de cascatas e rollback já validada no P131 permanece.
4. **Cinco aventuras / persistência**: `validatePublishedAdventureRuntimeState` bloqueia gatilhos desconhecidos ou acima de `maxFires`, contadores inválidos, objetivos injetados, histórico de objetivos sem referência, consequências resolvidas sem timestamp e estruturas malformadas antes da restauração. As cinco aventuras passam por testes de ida e volta JSON sem mutação de entrada.
5. **Compatibilidade Android e Kokoro**: bundle offline regenerado dos módulos canônicos; Java/TTS/Kokoro não alterados. Testes de integração cobrem compra real via controlador/bridge e opção de iniciativa, recuperação e combate.

## Evidências de teste

- `python3 tools/run-node-suite.py`: **1321/1321 PASS**, 250 arquivos (244 regulares + 6 stress), zero falhas/pulados. Ver `validation/P132_NODE_SUITE.txt`.
- `node --test tests/*tts*.test.mjs tests/*kokoro*.test.mjs`: **27/27 PASS**. Não é teste de voz física no Android.
- `node tools/build-ui-bridge-bundle.mjs` e `node build-ui-bridge-bundle.mjs`: PASS, 73 módulos. Sintaxe de `talents.mjs`, `adventureRuntime.mjs`, `publishedAdventureUaeBridge.mjs`: PASS.
- `node tools/verify-final-source.mjs`: **PASS** (1321 testes, 122 arquivos JS, 8 Java no gate sintático, 77 JSON, 2 XML, zero segredos), após reexecução isolada. A primeira tentativa concorrente falhou no gate de testes, mas a suíte separada e a reexecução isolada passaram. Ver `validation/P132_SOURCE_GATE.txt`. Android SDK/ADB não estão instalados neste ambiente.

## Pendências reais (não marcar como concluídas)

- Concluir os efeitos das **204 posições AoR ainda sem consumidor mecânico**, sem inferir efeitos a partir de títulos. Implementar famílias de expansões EotE/AoR contra fontes primárias.
- Confrontar os cinco adaptadores com os PDFs de aventura e completar o combate de *Além da Orla* sem inventar encontros. O adaptador continua declarando `encounters:false`.
- Compilar APK no runner self-hosted BRELGA e testar abertura, persistência, narração, Kokoro e navegação em aparelhos Android reais.

## Retomada segura

Extrair o ZIP P132 integralmente no Android Studio; não combinar arquivos avulsos de P130/P131 com P132. Executar a suíte Node e gate de fonte antes de alterações. Para publicação, usar o repositório `bra83/braseirobuild` e o runner self-hosted BRELGA, sem enviar o ZIP binário para o histórico Git. Gerar checkpoint P133 somente após testes passarem e preservar o P132.
