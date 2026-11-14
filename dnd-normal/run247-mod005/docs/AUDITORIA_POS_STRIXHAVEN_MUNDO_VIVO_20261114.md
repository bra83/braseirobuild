# Auditoria de mundo aberto após Strixhaven — 14/11/2026

## Proveniência e escopo
- Código base: `antigravity/dnd-normal-fechamento-gates`; projeto existente `dnd-normal/run247-mod005`.
- Branch isolada: `fix/dnd-post-strixhaven-worldtime-20261114`.
- Não alterar Mystara, dados de campanha ou regras de 2014/2024 sem validação.
- Roteiro pessoal (somente quando o jogador decidir): Strixhaven → Faerûn → Yartar → mundo aberto → Marco do Percurso.
- Nunca confundir código presente, teste adicionado, teste executado, APK compilado e aparelho físico homologado.

## Evidências verificadas diretamente no código
| Área | Situação observada no código | Veredito |
|---|---|---|
| Tempo e dia | `WorldStateEngine.preview/commit`, persistência `day_number/clock_hour`, fingerprint, relógios e recibos SQL | Existe motor, mas ações comuns sem duração explícita avançavam **zero** |
| Integridade temporal | `SceneStateEngine.apply` aceitava `incoming.day_number` crescente, horário da IA e expressões temporais da narração | Risco concreto de salto de dias/horas sem regras nem consequências |
| Descanso | `applyLongRestBenefits/ShortRestBenefits` e recuperação de recursos | `long_rest` podia ser reconhecido sem checar se duração explícita era suficiente |
| NPCs e facções | `LivingWorldSimulationEngine`, agendas, consequência de clima e persistência | Caminhos implementados; integração E2E ainda não certificada |
| Economia e trabalho | `LivingWorldEngine`, `DowntimeEconomyRulesEngine`, guildas e atividades | Módulos existem; disponibilidade e conclusão em partida ainda a comprovar |
| Rumores e campanhas emergentes | `RumorNetworkEngine`, `EmergentCampaignDirector`, `WorldSixCoordinator` | Módulos presentes; a presença não comprova experiências variadas |
| Clima | `EnvironmentalDirector`, `WeatherCausalityKernel`, `WeatherWorldConsequences` | Fases e consequências existem; transição pelo relógio real ainda exige prova |
| Encontros aleatórios | `SpontaneousEventEngine` retorna `kind=suggestion_only`; `EnvironmentalDirector` gera propostas | Não certifica encontros efetivamente gerados/resolvidos em cada contexto |
| Mundo offline | `LivingWorldCatchUpEngine`; `background_simulation_enabled` começa desativado | Opcional; não alegar que o mundo avança automaticamente com app fechado |
| Caça/forrageamento | Regras de viagem, recursos e atividades estão presentes | Não há prova de ciclo completo caçar → checagem autorizada → alimento → inventário |

## Correção implementada **nesta branch**
1. `SandboxActionDuration.java`: estimativas **de design, não texto de regra oficial** para ações declaradas e concluídas: trabalho (4h), procura de contratos (1h), caça/forrageamento (2h), patrulha (2h), investigação (1h), comércio (30min), conversa (15min), observação (6min). Comandos no infinitivo e rótulos de sugestões aceitos. Perguntas, negações e deslocamento indefinido não fazem avançar.
2. `WorldStateEngine.java`: tempo fracionário e estimado calculado em prévia, apenas consolidado pelo `commit` transacional já existente; viagens continuam no `TravelRulesEngine`; combates e Mystara excluídos das estimativas. Duração explícita sobrepõe estimativa.
3. Descanso com horas declaradas inferiores a 8h (longo) ou 1h (curto) não concede benefício de descanso. `SandboxRestPolicy.java` separa critérios testáveis e avisos sem penalidades automáticas; inclui aviso sobre duração insuficiente.
4. `rest_watch`: referência persistida do último descanso longo e aviso a partir de 16h/24h desde sua conclusão. **Não aplica Exaustão automaticamente**; após 24h exige adjudicação de regra conforme edição/perfil.
5. Prévia fornece `time_transition_notice` com dia e hora canônica (HH:mm); resumo de auditoria inclui dia/hora e aviso de descanso. Não alegar que o HUD Android já mostra esse campo.
6. `SceneStateEngine.java`: envelopes do Gemini deixam de definir `day_number`, `clock_hour` e `time_of_day`; mensagens da IA não impõem relógio. O relógio da cena é conciliado exclusivamente com estado persistido.

## Evidência e pendências para revisão
- `SandboxActionDuration.java` compilado via `javac`, com **15/15 PASS** em executável independente. `SandboxRestPolicy.java` compilado via `javac`, com **14/14 PASS** em executável independente; **29/29 verificações locais**. Testes JUnit correspondentes foram adicionados, mas **a suíte JUnit do Android não foi executada** neste ambiente.
- Teste Android Gradle, Robolectric, SQLite e aparelho Galaxy A57 **NÃO EXECUTADOS**; estes itens não estão certificados.
- Verificar em integração se `WorldStateEngine.preview` e `commit` são invocados em toda ação concluída, inclusive ações sugeridas.
- Verificar se duração de ação deve ser declarada/ajustada pelo mestre antes do commit em tarefas interrompidas. Estimativa fixa não autoriza declarar sucesso, duração final sem revisão em casos ambíguos ou recursos encontrados.
- Revisar relógio de cena com `SceneTemporalResolver`, `EnvironmentalDirector` e UI; impedir avanço duplo, salto para horário narrado e clima rerrolado arbitrariamente.
- Verificar regras reais sobre descanso interrompido, sono, vigília, recuperação e privação de descanso conforme perfil 2014/2024; `rest_watch` é **aviso**, não executor de Exaustão.
- Confirmar empregos, trabalhos eventuais, caça, alimentos, dinheiro, rumor, POIs e encontro com NPCs como ciclos completos de proposta → escolha → teste quando aplicável → recibo/commit → save/reopen.
- Não autorizar merges e APK até evidência de regressão completa e homologação no Android.

## Cenários E2E obrigatórios
1. Começar jogo fora de Strixhaven, em Faerûn; confirmar regras e continuidade de mapa, companheiros e NPCs.
2. Às 08:00 procurar contrato, caçar, conversar, comprar mantimentos, trabalhar: conferir cada delta, transição manhã→tarde→noite e estado após reabrir.
3. `Durmo por 2 horas`: +2h, **sem** benefícios de descanso longo; `durmo 8 horas`: verificar elegibilidade/recuperação com regras ativas.
4. `Descanso curto de 30 minutos`: +30min, sem benefícios de descanso curto; 1h: operação válida.
5. Perguntar ao mestre “Posso caçar?” ou consultar regra: nenhuma passagem de tempo.
6. Viajar com rota no mapa; custo, relógio e clima só avançam após aceite canônico; mapa sem segmento não teleporta nem consome horas.
7. Pedir ao Gemini para narrar “amanhece no dia 98” durante dia 2: não pode mudar o contador canônico.
8. Reenviar ação, simular falha de IA, duplicar preview: sem ganho de provisões, recompensa ou tempo em dobro.
9. Completar período de 24h sem descanso: mostrar aviso sem aplicar automaticamente efeito mecânico não validado.
10. Submeter caça com falha e sucesso; resultados, consumo e XP somente por gateway mecânico; recompensas nunca criadas diretamente pela prosa.
11. Ativar e desativar Mundo Vivo offline e verificar limites/catch-up, separando relógio de campanha de tempo real.
12. Clima severo: manter estágio, região, duração, atraso da caravana e efeitos persistentes após melhorar.
13. Trabalho e facção: verificar remuneração, prazos e alteração real de estado, sem trabalhos infinitos repetidos.
14. Mundo aberto: jogador pode ignorar boatos, mudar de rumo e escolher ficar em uma cidade, sem roteiro forçado.

**Critério de aceite:** apenas marcar cada linha PASS quando existir evidência do fluxo real no SHA preciso; código e relatório sozinhos não são prova.


## Evidência incremental verificada no GitHub Actions — 14/11/2026
- RUN [43558580714](https://github.com/bra83/braseirobuild/actions/runs/43558580714): 5 XML suites, 18 testes, 0 failures, 0 errors, 0 skipped; Gradle \`BUILD SUCCESSFUL\`, SHA \`4e825a5fff666d8933f14b1e48d7553c66d05a71\`. Inclui regras puras, SQLite, passagem temporal, cena, aventura em Faerûn, persistência.
- RUN [43558908873](https://github.com/bra83/braseirobuild/actions/runs/43558908873): 6 XML suites, 22 testes, 0 failures, 0 errors, 0 skipped; Gradle \`BUILD SUCCESSFUL\`, SHA \`a42dd9e77b33c8649c6d76ca00e2df11136e0750\`. Inclui guildas, contratos, caça/trabalho persistentes, eventos opcionais, diretor climático, Mundo Vivo 6.0.
- Regressão BRELGA full suite: **pendente** por runner de serviço. Nem um run focado nem 29 verificações \`javac\` independentes certificam 100% das regras ou comportamento Android físico.
- Os testes de trabalho/caça comprovaram **persistência + duração + proibição de recompensa imaginária**, e **NÃO** a concessão mecânica de pagamento, carne ou água.
- Os encontros testados foram propostas opcionais; ainda **NÃO** o ciclo completo de encontros com seleção, rolagens, consequências e persistência.
- O clima testado foi snapshot de funcionamento do diretor; ainda **NÃO** trajetória climática completa sob campanhas longas e aparelho físico.

## Terceiro pacote: integração de viagem no mapa e HUD canônico
- \`WorldStateEngine.previewConfirmedMapElapsed\`: conecta viagem mecânica já confirmada ao \`WorldStateEngine.commit\` para atualizar NPCs, facções, relógios, calendários e agendamento de eventos uma única vez; não aplica recursos que \`TravelRulesEngine.commit\` já contabilizou.
- \`WorldTickFingerprint\`: sela que recursos da viagem já foram contabilizados, proibindo alteração do modo de cobrança após gerar prévia.
- \`MainActivity.applyDndTravel\`: chama o avanço de mundo canônico para D&D antes de consolidar a rota; preserva snapshot/restore do fluxo existente e não altera o trajeto Mystara.
- \`MainActivity.addSceneStatusStrip\`: exibe dia, hora/minuto e período de dia do estado persistente; renderização do D&D não regrava a hora com inferência da narração; avisos de descanso não aplicam penalidade.
- \`PostStrixhavenWorldLifecycleRobolectricTest\`: cenário de 1h45 de viagem confirmada, nenhum consumo duplicado, replay rejeitado. \`CoreGameLaunchRobolectricTest\` integra o gate da interface.
**Aceitação comprovada:** terceira execução hosted [43559304481](https://github.com/bra83/braseirobuild/actions/runs/43559304481): 7 suítes Robolectric/JUnit, **24 testes, 0 falhas, 0 erros, 0 ignorados; BUILD SUCCESSFUL**. GitHub Actions checkout/merge SHA `b58d0fe8894cdaff83fde33b40941f49c93f5408`, PR código `5f0dcf56135683cf50d1001268811f51956e09b4` nesse pacote. Não é homologação de todas as regras nem do aparelho físico.

## Matriz de declaração auditável por capacidade

| Capacidade | Evidência | Estado |
| --- | --- | --- |
| Ação sandbox → tempo → SQLite → reabertura | RUN 43559304481 / PostStrixhavenWorldLifecycleRobolectricTest | **PASS no cenário automatizado** |
| Dia/noite, descanso mínimo e não avanço por pergunta | RUN 43559304481 / SandboxRestPolicyTest + integração | **PASS no cenário automatizado** |
| Viagem de mapa → Mundo Vivo sem dupla cobrança | RUN 43559304481 / previewConfirmedMapElapsed + replay | **PASS no cenário automatizado** |
| Estado de cena: IA sem autoridade para mudar relógio | RUN 43559304481 / SceneStateEngine com estado real | **PASS no cenário automatizado** |
| Inicialização e navegação básica do Android | RUN 43559304481 / CoreGameLaunchRobolectricTest | **PASS no Robolectric; físico não testado** |
| Contrato de guilda: aceitar → quest → completar → reabrir | RUN 43559304481 / PostStrixhavenSandboxSubsystemRobolectricTest | **PASS no cenário automatizado** |
| Caça e trabalho: atividade persistida sem recompensa fictícia | RUN 43559304481 / PostStrixhavenSandboxSubsystemRobolectricTest | **PASS de persistência e bloqueio** |
| Proposta espontânea opcional | RUN 43559304481 / SpontaneousEventEngine | **PASS de proposta, não encontro completo** |
| Motor de clima + direção emergente | RUN 43559304481 / EnvironmentalDirector + WorldSixCoordinator | **PASS de snapshot, não evolução completa** |
| Caça com rolagem, alimento/inventário/recursos autorizados | Sem E2E completo de recompensa | **NÃO HOMOLOGADO** |
| Trabalho remunerado com salário e saldo auditáveis | Sem E2E completo de pagamento | **NÃO HOMOLOGADO** |
| Encontro completo com combate e consequências | Sem E2E pós-Strixhaven de combate | **NÃO HOMOLOGADO** |
| Clima evoluindo por vários dias e reabertura | Sem cobertura longa completa | **NÃO HOMOLOGADO** |
| Suíte de regressão geral BRELGA | Runner self-hosted ainda sem concluir | **PENDENTE / INFRA** |
| APK + Galaxy A57 físico | Nenhuma validação física nesta branch | **NÃO HOMOLOGADO** |

**Critério para declarar tudo:** CI de regressão geral no SHA exato, E2E completos restantes, APK compatível e Android físico sem regressão. A certificação parcial não pode ser apresentada como lançamento integral.

## Complemento de 14/11/2026 — clima e descanso explícito

- GitHub Actions [43571624597](https://github.com/bra83/braseirobuild/actions/runs/43571624597): 25 testes PASS em 7 suítes, 0 falhas, 0 erros, 0 ignorados, `BUILD SUCCESSFUL`. Valida `ConfirmedMapTravelWorldGateway`: viagem confirmada → relógio + memória climática de forma transacional → mudança de dia/noite → reabertura SQLite sem perder clima. Check-out SHA `f10a1a6d64c9ecf6ba711216f6f04e2dfe7ba636`.
- GitHub Actions [43572081517](https://github.com/bra83/braseirobuild/actions/runs/43572081517): **26 testes PASS** em 7 suítes, 0 falhas/erros/ignorados, `BUILD SUCCESSFUL`. Check-out SHA `a58883e37d867a620886c8bb5d93393a55d2db9d`. Acrescentou teste de descanso de 8 horas e intervalo de 16 horas antes de iniciar outro com benefícios; `CoreGameLaunchRobolectricTest` verifica os dois botões de descanso na interface Mundo Vivo.
- As ações diretas `DESCANSO CURTO • 1H` e `DORMIR • 8H` passam pela prévia/commit do `WorldStateEngine` e confirmam avanço climático na mesma transação SQLite. Os botões aparecem apenas em D&D, preservando Mystara.
- Regra-base 2024 consultada em `https://www.dndbeyond.com/sources/dnd/br-2024/rules-glossary`: descanso longo requer pelo menos 8h, com mínimo 6h de sono, e intervalo mínimo de 16h antes de iniciar o próximo. Interrupções e casos extraordinários ainda exigem validação específica; não certificar sem teste.
- **Nota de QA:** CI 26/26 não certifica geração de recompensas de caça e trabalho, ciclo completo de encontro/combate, regressão global BRELGA, assinatura/instalação APK ou hardware Galaxy A57. O projeto permanece sem liberação integral.
