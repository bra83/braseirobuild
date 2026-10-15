# Sessão UI jogável — 14/10/2026

Entrega parcial: a missão inteira ainda não está fechada.

Produção: MainActivity, tela Heróis existente. Acrescentados controles de descanso curto e longo do grupo Em jogo, prévia/confirmacão e commit no WorldStateEngine existente. Cancelamento não altera estado. Combate ativo e grupo vazio bloqueiam descanso. Falha ou prévia obsoleta informa erro PT-BR. Commit limpa cache das telas. Escolha de Dados de Vida continua no controle existente. Nenhuma regra foi reimplementada.

XP: formulário dedicado rejeita vazio, zero, negativos e estouro de inteiro sem fechar diálogo ou chamar persistência. Confirmações de XP e descanso têm proteção local contra repetição de callback.

Testes novos: PlayableUiRobolectricTest, cinco cenários com botões reais da tela Heróis e diálogos de produção: descanso curto/cancelamento, descanso longo, XP inválido/válido/duplicado/subida de nível, dano e bloqueio de descanso em combate. SQLite é reaberto para verificar persistência. O harness omite onCreate e serviços de inicialização; injeta banco isolado e dependências, renderiza showHeroes de produção. Não certifica inicialização/navegação completa.

Validação executada: parser JDK, dois arquivos sem erro sintático; git diff --check passou. Isso não prova compilação Android nem sucesso dos testes.

Bloqueios de execução:
- Wrapper via bash: distribuição Gradle 8.11.1 ausente; download falha com java.net.SocketException: Operation not permitted.
- Gradle instalado 9.7.1 com GRADLE_USER_HOME isolado: serviço FileLockContentionHandler falha: Could not determine a usable wildcard IP for this machine.
- Robolectric NÃO EXECUTADO. Logs anexos. CI BRELGA deve executar o wrapper do projeto com :app:testDebugUnitTest -Pbraseiro.qaLiteAssets=true --tests '*PlayableUiRobolectricTest' e depois regressões existentes. Não houve build de APK.

Pendências da missão: validar/corrigir pela UI criação e escolhas de ficha, campanha 2024, exploração sandbox, ação e dados manuais, monstro real XMM, movimento/ataques/dano/condições, avanço de turno e restauração integral de campanha. Os testes novos não certificam esses fluxos. Também falta executar os novos cenários e regressões no Android/CI; nenhum resultado verde Robolectric é alegado.

Não foi identificado nesta sessão um defeito comprovado dependente dos PRs #33/#35. AppDb e motores de sopros intactos. Mystara, suplementos e outros VTTs não foram editados. Sem commit, push, merge ou APK.
