# Correção das regressões de interface do PR #37

## Código

- A fixture SessionContinuity fornece CharacterSheetGenerator e Handler principal,
  dependências de onCreate que a fixture deliberadamente não executa. A fixture
  ExplorationCombat também fornece ambas para a navegação real entre telas.
- As fixtures de criação e combate substituem WorldStateEngine ao reabrir AppDb,
  para não manter o helper SQLite anterior fechado.
- Os diálogos de quantidades, descanso, atributos de origem, dados manuais,
  dano, criação de combatentes, ataque compilado, edição avançada e movimento
  instalam seus callbacks imediatamente depois de show(). O OnShowListener do
  Android é entregue por mensagem; antes dessa entrega, o botão padrão podia
  fechar o diálogo sem executar validação nem persistência.
- Aplicar nome, inventário, magias e confirmar encerramento de combate passam
  a concluir o callback no clique. Isso impede salvar uma ficha antes de uma
  edição pendente ser aplicada. A confirmação é de uso único.
- O botão retido do seletor de atributos de origem não reaplica o bônus após
  fechar o diálogo. O teste existente de não acumulação foi reforçado para
  exercitar esse segundo clique, mantendo todas as asserções anteriores.

As regras, transações, snapshots preventivos, validações e recibos existentes
continuam sendo usados. Nenhuma alteração em AppDb, DB61, Backup21, Mystara,
suplementos, outros VTTs ou workflows. Nenhum commit ou push.

## Verificação concluída

- `git diff --check`: passou.
- Parser Java do JDK (`JavacTask.parse`, sem resolução de dependências): passou
  em MainActivity e nos quatro testes. Isso verifica sintaxe, não compilação
  Android nem execução Robolectric.
- As 51 funções de teste das quatro classes foram mantidas (5 + 18 + 19 + 9).
- Revisão dos callbacks: preservados os guardas contra repetição, a rejeição
  de grupo alterado durante a prévia de descanso, a rejeição de editor avançado
  obsoleto e os fluxos existentes de reabertura e exportação/importação SQLite.

## Bloqueios de infraestrutura (separados do resultado funcional)

1. `gh run view 38659906484 --repo bra83/braseirobuild --log-failed`:
   falhou ao conectar a api.github.com; ver github-cli.log. Não foi possível
   obter os nomes e stack traces individuais das 21 falhas do BRELGA.
2. Gradle instalado 9.7.1 com diretório gravável:
   `GRADLE_USER_HOME=/tmp/pr37-gradle gradle --offline :app:testDebugUnitTest -Pbraseiro.qaLiteAssets=true --tests '*UiRobolectricTest'`.
   Falhou antes de configurar o projeto: não conseguiu determinar um wildcard
   IP para FileLockContentionHandler; ver gradle-installed.log.
3. Wrapper 8.11.1 com diretório gravável:
   `GRADLE_USER_HOME=/tmp/pr37-gradle bash gradlew --offline :app:testDebugUnitTest -Pbraseiro.qaLiteAssets=true --tests '*UiRobolectricTest'`.
   A distribuição não está em cache; o download falhou com
   `java.net.SocketException: Operation not permitted`; ver gradle-wrapper.log.

Os testes Robolectric não chegaram a executar. Portanto, a eliminação de todas
as 21 falhas e o round trip SQLite/Backup21 permanecem sem confirmação dinâmica
neste ambiente. Reexecutar as quatro classes e a regressão completa no BRELGA.
