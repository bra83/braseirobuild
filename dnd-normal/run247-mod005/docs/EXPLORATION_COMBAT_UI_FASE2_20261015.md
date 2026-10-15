# Exploração e combate pela UI — fase 2, 15/10/2026

Lidos o estado/diff acumulado, HANDOFF_NEXT_AGENT.md, CHECKPOINT_STATUS.md, docs/CORE_BASE_JOGAVEL_STATUS_20261014.md e docs/CHARACTER_UI_FASE1_20261015.md. O worktree já continha alterações de MainActivity, CharacterCreationUiRobolectricTest e evidências da fase 1; foram preservadas.

## Implementação em MainActivity

- Ao retornar por navegação no D&D normal, Combate, Heróis, Mestre IA, Aventura e Dados são reconstruídos do SQLite. Antes, a tela armazenada por até dois minutos podia apresentar PV, orçamento, cena e histórico antigos. O cache do mapa foi preservado, assim como os caminhos Mystara e Strixhaven.
- O contexto do combate agora abre diretamente as fichas pela navegação existente. Explica como registrar efeitos ainda manuais com DANO/AVANÇADO e o gasto confirmado de ação, bônus ou reação, sem delegar regras ao narrador.
- O seletor compilado filtra a fonte Monster Manual (2025) e source_fidelity.source_code=XMM, mantendo o registro global e os executores intactos. Usa um recibo estável por diálogo, valida iniciativa -20..100, impede nova aplicação e registra início no diário quando o executor inicia o encontro.
- A criação manual valida nome, iniciativa, PV e CA, sem transformar texto inválido em números padrão. Adição, ativação do encontro e turno inicial usam a transação checked existente. Falha mantém o diálogo; sucesso impede toque repetido.
- Dano manual valida inteiro positivo e identifica combatente removido. Usa um recibo por diálogo e impede aplicação repetida; falhas ficam visíveis sem fechar o formulário. Mantém applyCombatantDamage, inclusive PV temporários, resistências e nocaute.
- Dados manuais foram acrescentados ao painel de combate existente. DiceRollSpec valida faces/quantidade/ordem e calcula modificadores e vantagem/desvantagem. Histórico registra manual_user, faces, ator e modo. Não aplica dano ou gasta recurso implicitamente; Cancelar não grava. A exibição do histórico distingue origem manual.
- Nova condição com duração agora é também adicionada às condições ativas. O editor verifica se o combatente mudou desde a abertura para não sobrescrever dano, orçamento ou timers novos com uma cópia antiga. Usa updateCombatantAdvancedChecked e preserva chaves não editadas.
- O ataque direto XMM mantém applyCompiledStatblockAttack e seus bloqueios por fonte, geometria, dano e efeitos. O diálogo passou a rolar verticalmente e usa recibo estável/proteção contra confirmação repetida.
- Movimento mantém os executores de rota, orçamento, reação e fingerprint. O diálogo passou a rolar verticalmente e rejeita coordenadas não inteiras explicitamente.

Não há motor novo, segunda interface, automação inventada de efeitos ou autoridade mecânica nova para Gemini/RAG.

## Testes escritos

19 casos em ExplorationCombatUiRobolectricTest, com controles de MainActivity e SQLite reais, sem mocks de regras. Reflexão somente para injetar serviços reais/abrir tela/navegação sem startup de rede:

- fonte XMM e cancelamento do seletor;
- spawn real, iniciativa inválida/válida, início de turno, duplo toque e reabertura;
- criação manual com PV/CA inválidos e persistência;
- dano inválido, absorção por PV temporários, toque repetido, cancelamento e persistência;
- condição com duração ativa/persistida, duração inválida, cancelamento e proteção contra edição obsoleta;
- dados inválidos, histórico único, cancelamento, fórmula mista e vantagem com duas faces;
- rodada e restauração de ação;
- encerramento confirmado/cancelado e reabertura do encontro vazio;
- avanço de turno sem encontro;
- movimento cancelado, destino inválido, deslocamento real no grid, orçamento e reabertura;
- ataque direto real XMM com dado manual inválido/válido, aplicação de dano/ação e reabertura; cancelamento sem efeitos;
- retorno de fichas para combate reconstrói o conteúdo com os PV persistidos.

## Execução e infraestrutura

**Nenhum teste Robolectric executou nesta fase. Não há suíte Android verde nem compilação Android comprovada.**

Tentativas reais registradas em docs/evidence/exploration-combat-phase2-20261015/:

- Wrapper no diretório padrão: impossibilidade de criar lock em /home/codespace/.gradle (fora da raiz gravável).
- Wrapper com GRADLE_USER_HOME=/tmp/phase2-gradle: falha no download do Gradle 8.11.1 por SocketException: Operation not permitted.
- Gradle instalado no diretório padrão: falha ao carregar biblioteca nativa.
- Gradle instalado com home gravável, inclusive tentativa final com todas as suítes de UI: FileLockContentionHandler / Could not determine a usable wildcard IP, antes da configuração/compilação.
- Parser JDK dos dois Java desta fase: 2 arquivos, zero erros sintáticos. Não verifica símbolos Android nem substitui compilação.
- Smoke separado executado no DiceRollSpec real com javac/java: seis verificações passaram (fórmula mista, vantagem, desvantagem e três entradas inválidas). Não é execução da UI.
- git diff --check passou.

Reexecução necessária em ambiente Android/Gradle habilitado:

```sh
bash gradlew :app:testDebugUnitTest \
  --tests com.braseiro.dnd5e.ExplorationCombatUiRobolectricTest \
  --tests com.braseiro.dnd5e.CharacterCreationUiRobolectricTest \
  --tests com.braseiro.dnd5e.PlayableUiRobolectricTest \
  --tests com.braseiro.dnd5e.CoreRulesProfileDefaultRobolectricTest \
  -Pbraseiro.qaLiteAssets=true --no-daemon
```

## Dependências e fechamento ainda pendente

1. **PR #35 / AppDb, movimento de monstro compilado:** addCompiledStatblockCombatant preenche speed_ft lendo movement.walk_ft (linha 2023 na base), mas os blocos XMM declaram movement.walk. Exemplo: Aarakocra Skirmisher tem walk=20 e fly=50; o spawn cai no padrão 30. Não corrigido aqui para respeitar a reserva de AppDb. O teste de movimento desta fase confere débito/posição pelo orçamento efetivamente persistido, não homologa a velocidade da fonte. É necessária correção no PR responsável e teste de contrato da velocidade no spawn.
2. Persistência de efeitos, turnos e reconciliação segue dependendo dos executores/AppDb correntes. Testes devem ser reexecutados após integração do PR #35. O executor de sopros/PR #33 permanece intocado.
3. O fluxo sandbox existente continua em beginAdventure -> preflight -> seleção/sorteio de hex -> launchAdventure -> sendAi. Não houve execução integral da abertura com Atlas/índice/narrador neste ambiente; não há homologação de início sandbox, cena/mapa/ficha completos nem teste de hardware. A mudança de cache e o acesso a fichas resolvem partes independentes da navegação; o novo teste de navegação cobre fichas/combate, não toda a abertura e o mapa.
4. Efeitos XMM sem contrato automatizado continuam manuais. DANO/AVANÇADO e os controles de recursos permitem registrar decisões confirmadas; não há alegação de cobertura integral de todos os efeitos XMM.

AppDb.java, Mystara, outros VTTs, executores de sopros, DB61, Backup21, package, assinatura e versões não foram alterados. Sem commit, push, merge ou APK.
