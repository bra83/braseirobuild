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
3. Descanso com horas declaradas inferiores a 8h (longo) ou 1h (curto) não concede benefício de descanso. Inclui aviso sobre duração insuficiente; não autoriza efeitos automáticos não documentados.
4. `rest_watch`: referência persistida do último descanso longo e aviso a partir de 16h/24h desde sua conclusão. **Não aplica Exaustão automaticamente**; após 24h exige adjudicação de regra conforme edição/perfil.
5. Prévia fornece `time_transition_notice` com dia e hora canônica (HH:mm); resumo de auditoria inclui dia/hora e aviso de descanso. Não alegar que o HUD Android já mostra esse campo.
6. `SceneStateEngine.java`: envelopes do Gemini deixam de definir `day_number`, `clock_hour` e `time_of_day`; mensagens da IA não impõem relógio. O relógio da cena é conciliado exclusivamente com estado persistido.

## Evidência e pendências para revisão
- `SandboxActionDuration.java` compilado via `javac`, com 15 verificações locais em executável independente: **15/15 PASS** para a versão atualizada do estimador. Testes JUnit foram adicionados/atualizados, porém **a suíte JUnit do Android não foi executada** neste ambiente.
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
