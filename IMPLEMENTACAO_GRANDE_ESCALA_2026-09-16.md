# WWN — implementação grande escala

Este checkpoint deixa de tratar um controle isolado por vez e conecta quatro subsistemas já existentes do motor à operação real da campanha.

## Entregue neste bloco
- Central `gestao.html` mobile-first, acessível por Jornada, Ficha, Mochila, Configurações e Início.
- Progressão: gasto de pontos em perícias e melhoria de atributos pelo `AdvancementSpendingService`, preservando contratos de treino/evidência.
- Magia: leitura do estado mágico, seleção/preparação a partir do grimório/known spells e recuperação diária pelo `MagicService`.
- Crafting: início de fabricação, acompanhamento/conclusão de jobs com evidência temporal e instalação de mods canônicos do catálogo pelo `CraftingService`.
- Facções/GM: projeção das facções e do turno; upkeep, especiais, ação canônica explícita e revisão de objetivo pelo `FactionTurnService`.
- Nenhuma regra foi reimplementada na UI: todos os botões delegam aos serviços canônicos existentes.
- Nova regressão `qa/full-systems-integration-regression.mjs` garante presença dos quatro bindings e navegação.

## Validação
- 138/138 arquivos JS: sintaxe válida.
- closure-regression: 33/33 PASS.
- deep-closure-regression: 33/33 PASS.
- large-scale-regression: PASS.
- turn-cycle-regression: PASS.
- world-dungeon-regression: PASS.
- full-systems-integration-regression: PASS.

Gradle/APK/device continuam deliberadamente fora deste gate.
