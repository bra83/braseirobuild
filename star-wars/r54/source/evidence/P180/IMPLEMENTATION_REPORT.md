# P180 — implementação e evidências

## Código executável

- `adventureRuntime.mjs`: efeitos de lista malformados passam a falhar com rollback, inclusive após efeitos prévios; `condition:false` não contorna o filtro; tipos de efeito inválidos não são tratados como `set-flag`.
- `adventureSemanticGate.mjs`: validação de condições com limite de profundidade (16), comprimento (2048), listas (64) e compostos (32), igual ao avaliador Android; rejeita condições vazias dentro de compostos; limites de lotes de efeitos (128); bloqueia eventos internos também na sintaxe objeto e origens forjadas; valida tipos e campos de pistas/segredos.
- `tools/audit-adventure-source-p180.mjs`: cotejo reexecutável do sumário de três PDFs com os módulos de cinco aventuras, sem alegar equivalência editorial completa.
- `tools/run-node-suite-p180.py`: executor reprodutível por arquivo, com relatórios JSON individuais e identificação de timeouts.
- `tests/uae-p180-condition-effects-contract-five-adventures.test.mjs`: 30 testes novos nos cinco módulos, incluindo gatilhos, objetivos, consequências, relógios, rollback e três restaurações.
- Android `versionCode 10180`, `versionName 1.0.180`, bundle offline reconstruído com 85 módulos; oito fontes Java preservadas byte a byte.

## Validação

- 299 arquivos regulares + 6 de stress, **2664/2664 testes aprovados** em execução por arquivo e shards sequenciais. Não é aprovação de uma única execução monolítica; `evidence/P180/test-results` contém resultados por arquivo.
- 113/113 testes focais/de integração em quatro arquivos, incluindo 30 novos.
- Cinco aventuras: preflight semântico/estrutural, testes transacionais, rollback, 3 restaurações nos testes P180.
- Sintaxe: 135 JavaScript, 475 JSON no gate inicial, 2 XML; 8 fontes Java inalteradas. `JAVA_SYNTAX_PASS files=8` com símbolos Android não resolvidos por ausência de SDK.

## PDFs e pendências reais

- Três PDFs do Drive recuperados e sumários comparados: `Beyond the Rim` (98 páginas), `The Jewel of Yavin` (98 páginas), `Mask of the Pirate Queen` (98 páginas). **Não é comparação integral de cenas, encontros ou NPCs.**
- **Divergência confirmada**: o livro `Mask of the Pirate Queen`, Episódio I, chama-se *Search on Saleucami*; o módulo inicia em `ord-mantell-arrival` (Chegada a Ord Mantell). Não corrigir narrativamente sem adaptar todo o capítulo e preservar saves antigos.
- `Under a Black Sun` e `Long Arm of the Hutt`: PDFs ainda não recuperados nesta execução.
- AoR: 360/360 posições com consumidores declarados desde P179; sem certificação semântica integral contra todos os livros.
- APK, teste em dispositivo Android, Kokoro físico e CI BRELGA **não executados**.
