# HANDOFF — BRASEIRO STAR WARS EDGE OF THE GALAXY — R54 P52
## BEYOND THE RIM EPISÓDIO I + UI MULTIAVENTURA + REGRESSÃO COMPLETA

Baseline autoritativa de entrada: **R54 P51**.
Trabalho incremental. **Não recriar o projeto. Não usar GitHub para correção.**

## Objetivo deste marco

Abrir a infraestrutura genérica criada no P51 para uma segunda aventura publicada real, sem regressão de *Sob um Sol Negro*, EotE/AoR/MIXED ou Duty/Obligation. A aventura escolhida foi **Beyond the Rim**, existente no Google Drive do projeto. Neste marco foi implementado **somente o Episódio I**, com fronteira explícita antes do Episódio II para não fingir completude nem revelar conteúdo futuro.

## Implementado

### 1. Beyond the Rim — Episódio I em PT-BR
Novo módulo:
`app/src/main/assets/vtt/js/adventure/beyondTheRim.mjs`

Metadados:
- id: `beyond-the-rim`
- título PT-BR: `Além da Orla`
- fonte: SWE05
- compatibilidade: EOTE e MIXED
- AOR puro continua bloqueado
- `episodesImplemented: [1]`
- `episodesTotal: 3`
- boundary: `episode1-boundary`

Fluxo implementado:
1. chegada à Roda;
2. reunião/proposta da IsoTech com Reom;
3. preparação na estação;
4. investigação nos níveis de hangares;
5. segurança da nave e risco de rastreamento imperial;
6. entrega de IT-3PO;
7. ação/perseguição contra os Yiyar;
8. resolução com a Segurança da Roda quando aplicável;
9. partida com IT-3PO recuperado OU partida com os Yiyar na dianteira;
10. fronteira persistente do Episódio I, sem conteúdo do Episódio II exposto ao jogador.

Locais persistentes do recorte:
- A Roda;
- escritórios da IsoTech;
- níveis de hangares;
- Blasted Asteroid.

NPCs do recorte:
- Reom;
- Waroon Dak;
- IT-3PO;
- Yav Yiyar.

Facções/pressões:
- IsoTech;
- Yiyar Salvage;
- ISB;
- Segurança da Roda.

Yiyar e ISB não são apresentados como conhecimento do jogador antes de serem revelados pelo estado da aventura.

### 2. Checks, consequências e ramificações
Foram ligados aos skills canônicos do motor, sem criar um sistema de dados paralelo:
- Streetwise;
- Perception;
- Negotiation;
- Knowledge (Underworld);
- Charm / Coercion;
- Mechanics;
- Leadership;
- Coordination;
- Athletics;
- Piloting (Planetary/Space);
- Astrogation.

Consequências persistentes incluem:
- rumor confirmado / inteligência de rota;
- termos de negócio melhorados;
- exposição imperial;
- rastreador imperial detectado/instalado/removido;
- estado e vantagem da perseguição;
- IT-3PO recuperado ou perdido;
- Yiyar com/sem vantagem para o Episódio II;
- partida rastreada ou limpa.

O resultado do Episódio I é persistido em:
`world.publishedAdventureConsequences['beyond-the-rim']`

Campos principais:
- `episode: 1`
- `partyAhead`
- `yiyarHasDroid`
- `departureTracked`
- `termsImproved`
- `routeIntel`

### 3. Anti-spoiler
- O status para jogador só contém pistas/NPCs já conhecidos.
- A requisição para Mestre trabalha com o nó atual e `antiSpoiler: true`.
- O início não revela IT-3PO, Yiyar ou ISB antes do momento apropriado.
- Ao chegar à fronteira implementada, a UI informa que o Episódio I foi concluído e salvo, que o Episódio II ainda não está habilitado nesta baseline e que conteúdo futuro não foi revelado.
- A aventura completa NÃO é marcada como concluída e nenhuma recompensa final foi inventada.

### 4. Registry de aventuras
`publishedAdventureRegistry.mjs` agora registra:
- `under-black-sun`
- `beyond-the-rim`

O adapter de Beyond the Rim implementa o mesmo contrato genérico de catálogo, start, status, current scene, checks, adjudicação, encontro, archive/continue.

### 5. UI deixou de ser hardcoded em Sob um Sol Negro
Arquivo principal:
`app/src/main/assets/vtt/r46/final-runtime.js`

Correções:
- novo `publishedAdventureCatalogView()`;
- novo `activePublishedAdventureId()`;
- tela de início lê o catálogo do registry;
- EOTE/MIXED exibem as aventuras publicadas compatíveis + Mundo Livre;
- checks, choices, encounters, restart e continue usam o ID da aventura ativa;
- cabeçalho usa o título da aventura ativa;
- widgets de sistema suportam perseguição Yiyar e exposição imperial;
- nenhum restart/check da segunda aventura cai acidentalmente em `under-black-sun`.

### 6. Bundle
`uiBridge.bundle.js` reconstruído com **61 módulos**.
O teste de completude foi atualizado para exigir 61 módulos e a presença explícita de `beyondTheRim.mjs`; não foi relaxado para aceitar qualquer contagem.

## Evidência final

### Focal pós-patch
`P52_FOCAL_FINAL.txt`
- 63 testes
- 63 PASS
- 0 FAIL

### Sintaxe
`P52_SYNTAX_FINAL.txt`
- Beyond the Rim: PASS
- registry: PASS
- controller: PASS
- uiBridge.bundle.js: PASS
- final-runtime.js: PASS

### Regressão ampla
`P52_BROAD_FINAL_SUMMARY.txt`
- 177 arquivos de teste
- 886 testes
- 886 PASS
- 0 FAIL

A primeira regressão ampla encontrou 1 falha real de contrato de teste: o gate de completude ainda exigia exatamente 60 módulos. Como o novo adapter elevou legitimamente o bundle para 61, o gate foi atualizado para 61 + presença explícita do novo módulo e o shard foi rerodado. Depois, todos os 8 shards foram executados e ficaram verdes. Não houve conversão de timeout/falha em PASS.

## Android / Gradle
Foi feita tentativa de build de marco porque assets runtime/bundle mudaram:
`./gradlew assembleDebug --no-daemon`

Resultado: **BLOCKED_BY_INFRASTRUCTURE**.
Erro: `java.net.UnknownHostException: services.gradle.org` durante o download do Gradle Wrapper.

- Java/Gradle nativos não foram alterados neste marco.
- **NÃO declarar APK PASS.**
- O bloqueio está registrado em `P52_ANDROID_BUILD.txt` e `P52_ANDROID_STATUS.txt`.

## Contratos preservados
- Sob um Sol Negro continua funcional e coberto pela regressão.
- EotE preservado.
- AoR preservado; aventura EotE pura não aparece em campanha AOR pura.
- MIXED preservado e aceita as aventuras EotE compatíveis.
- Duty/Obligation preservados.
- Persistência do mundo e compactação de histórico do P51 preservadas.

## Próxima prioridade — P53
Continuar **Beyond the Rim — Episódio II: Welcome to the Jungle** sobre este mesmo estado persistente, sem refazer Episódio I:
1. viagem para Cholganna e dificuldade/rota condicionada pelas pistas do Episódio I;
2. chegada e busca planetária;
3. detritos/escape pods/seções do Sa Nalaor como descoberta progressiva;
4. posição dos Yiyar derivada de `partyAhead`/`yiyarHasDroid`;
5. encontros/predadores com consequência persistente;
6. chegada ao casco principal;
7. introdução de Harsol/Cratala somente quando revelados;
8. manter ISB como ameaça progressiva sem spoiler;
9. preparar Retreat e contingência imperial em etapas;
10. não implementar recompensa final da aventura antes do Episódio III estar realmente concluído.

## Regra de continuidade
**P52 é a nova baseline fonte. Não voltar ao P51. Não recriar projeto. Não usar GitHub para correção.**
