# Braseiro Universal — Auditoria GitHub e aproveitamento REAL

Data: 2026-10-10. Baseline: Braseiro D&D 5e NORMAL RUN245 + Manual 001 (sem HYBRID FASTPLAY). Objetivo: integrar componentes executáveis, preservando o aplicativo Android.

## Repositórios inspecionados e decisão

| Repositório | Licença e origem | Achado auditado | Status nesta revisão |
|---|---|---|---|
| [Caryyon/tome](https://github.com/Caryyon/tome) | MIT | `src/Entity.ts`, `src/validation.ts`, conversor D&D, envelopes de fichas/propriedades/recursos | **PORTA JAVA** `TomeEntityBridge.java`; modelo JSON e alteração de recurso com revisão |
| [1A7432/loreweaver](https://github.com/1A7432/loreweaver) | MIT | `core/check_outcome.py`, `docs/protocol.md`, semântica de resultados neutros independente do RPG | **PORTA JAVA** `LoreweaverOutcomePort.java`, consumida pelos resolvedores |
| [IPedrax/AetherTable](https://github.com/IPedrax/AetherTable) | MIT | Contrato `[[ROLL:1d20+5]]`; separação proposta de IA / resultado factual do dado; documentação e API | **PORTA JAVA** `AetherTableRollIntentPort.java`, que transforma tag em proposta d20; **não** importa seu servidor Python |
| [Stefouch/foundry-year-zero-roller](https://github.com/Stefouch/foundry-year-zero-roller) | MIT | Dados pool Year Zero, pressão/forçar rolagens, consequências de 1/6 | **PORTA JAVA** `YearZeroDicePort.java`, isolada de Foundry |
| [swrpg-online/dice](https://github.com/swrpg-online/dice) | MIT | Faces exatas em `src/diceFaces.ts` e cancelamento de símbolos de sucesso, ameaça e triunfo | **PORTA JAVA** `StarWarsNarrativeDicePort.java` |
| [Jason-Holt-Digital/mythic-dice-parser](https://github.com/Jason-Holt-Digital/mythic-dice-parser) | MIT | Parser Dart de expressões | NÃO integrado. Substituí-lo por outro parser sem contrato de uso duplicaria o trabalho |
| [Kruptein/PlanarAlly](https://github.com/Kruptein/PlanarAlly) | MIT | Mapas, visão, FOW e iluminação no cliente TS/canvas | NÃO integrado: exige ponte com `MapBoardView` e provas gráficas Android, não é biblioteca drop-in |
| [Khazlor/Open-VTT](https://github.com/Khazlor/Open-VTT) | MIT | Editor e renderização Godot | NÃO integrado: engine Godot não pode simplesmente ser copiada para `MainActivity.java` |
| [Gravewright/gravewright](https://github.com/Gravewright/gravewright) | GPL-3.0 | ZIP de módulos assinados, backend Django, cliente web | CONTRATO estudado; **nenhum código incorporado**, por licença e acoplamento |
| [pangolinsec/Steward](https://github.com/pangolinsec/Steward) | Restrição não comercial/pessoal | Gatilhos, mundo vivo, regras declarativas | **Nenhum código copiado**; restrição de licença |
| [fvtt-fria-ligan/forbidden-lands-foundry-vtt](https://github.com/fvtt-fria-ligan/forbidden-lands-foundry-vtt) | GPL-3.0 + direitos Free League sobre conteúdo | Fichas e mecânicas Foundry | **Nenhum código/arte/trecho editorial copiado**; verificar licenças separadamente |

### Achados de qualidade e riscos

- **Tome**: `validateResources` usa `current === undefined && current === null` no trecho consultado, condição logicamente impossível. A porta Java impõe limites efetivos e revisões. O conversor de exemplo D&D tem um identificador com espaço no arquivo consultado; não é seguro copiá-lo integralmente.
- **Year Zero Roller**: depende das classes Foundry (`Roll`, `DiceTerm`). A porta Java suporta somente dados d6, pool atributo/perícia/equipamento, dados negativos e um *push*; artefatos de dados superiores e variações de outros Year Zero **não** foram implementados.
- **SWRPG Dice**: tabelas `src/diceFaces.ts` foram portadas; montagem de pool por perícia/talento, custos, Force powers e outros subsistemas **não** vieram juntos. A porta separa pool de Força.
- **Loreweaver**: não transporta Python, protocolo de sala e LLM Keeper; adota unicamente `outcome` semântico para que sistemas não compartilhem uma escada de dificuldades incompatível.
- **AetherTable**: a porta usa uma gramática intencionalmente restrita do contrato `[[ROLL:...]]`. Não realiza chamada externa a Gemini; a CD ainda deve ser fornecida/validada e o resultado é lançado pelo jogador.

## Fluxo realmente conectado ao Braseiro

`MainActivity` > botão **INTEGRAÇÃO GITHUB · TESTE REAL** > `UniversalGithubLab` > `AdventureEngine.processUniversal` > `TomeEntityBridge` > adaptador AetherTable/Year Zero/SWRPG > `LoreweaverOutcomePort` > recibo em `AdventureState.universal_rpg_state` > `AdventurePersistenceAdapter.saveState` na tabela SQLite `adventure_state_journal` > reabrir e conferir.

O laboratório usa módulo isolado `braseiro-universal-github-integration-v1`, com `party_id=0`, para não trocar o módulo da campanha D&D do usuário. A operação no app invoca SQLite real; **ainda não foi executada em um aparelho Android** nesta sessão. Seus resultados só contam após uso nesse ambiente. O código também expõe o fluxo a outras telas pelo objeto `params.universal` de `AdventureEngine.processAction`.

## Limites da entrega

- A integração não substitui os motores Android existentes, o Gemini nem o sistema de regras D&D.
- Os cinco módulos demonstram **cinco repositórios trabalhando juntos** num mesmo journal, mas não significam que suas funcionalidades completas tenham sido migradas.
- Não foi feito build Gradle, teste Robolectric, execução do botão no aparelho ou revalidação do Backup21.
- As seis verificações JUnit-like e duas execuções de integração foram feitas em Java/JDK com um substituto de `org.json` e persistência em memória **somente para QA de contrato**. Não devem ser confundidas com SQLite real ou testes Android.
- As bibliotecas originais continuam disponíveis nos endereços acima. Não armazenamos dependências TypeScript/Python/Foundry desnecessárias dentro do APK.

## Política de preservação

Este ZIP é uma cópia integral por overlay da fonte **RUN245 Universal Manual 001**. Nenhum outro arquivo existente foi apagado. O original permanece intacto. Não houve escrita em branches do GitHub e não há claim de APK novo.
