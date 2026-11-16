# Handoff de imersão e continuidade — matriz de aceite por função

Branch de implementação: `fix/dnd-6662-handoff-full-gates-20261116`, derivado do sucessor `fix/dnd-6661-immersao-continuidade-handoff-20261115`. Não construir projeto novo nem regredir as versões 6.6.54–6.6.61.

> IMPORTANTE: **correção de NPC não equivale a conclusão do handoff**. Somente provas efetivamente realizadas são marcadas como verificadas; testes de intenção não são qualidade narrativa ao vivo.

| Critério exigido | Onde foi implementado | Evidência atual | Limite |
|---|---|---|---|
| Perguntas sobre tempo, aparência, roupa e armas no campo de ação | `FictionTurnIntent`, `FictionTurnDirective`, `MasterOrchestrator` | `FictionTurnIntentContractRobolectricTest.T01,T02,T03,T18` | Resposta textual Gemini ao vivo ainda não verificada |
| Separação entre diálogo, pergunta de regras, investigação e ação | `FictionTurnIntent.classify` e entrada do `MasterOrchestrator` | `T06–T08`, `NarratorConsultationRoutingRegressionTest`, `FictionMixedFortyTurnRobolectricTest` | Não substituir por um classificador lexical genérico sem auditoria |
| Pergunta + comando condicional na ordem correta | `FictionTurnIntent.condition`, `FictionTurnDirective` | `T09,T16`, replay misto | O estado verdadeiro da chuva não autoriza inventar abrigo nem declarar sucesso |
| Não revelar nome oculto, profissão, arma ou segredo de NPC | `NpcPerceptionLedger`, `FictionObserverProjection`, `FictionTurnIntent.resolveReferent` | `T03,T04,T05,T05b,T05c,T10,T11,T17` | Necessita conferir o prompt final e todas as fontes de contexto |
| Identidade de NPC persistida apenas após apresentação aceita | `NpcPerceptionLedger.commitAcceptedPresentation` e `AppDb` | `T10`, `FictionMixedFortyTurnRobolectricTest` | Não é substituto de campanha real com Gemini |
| Perguntas perceptivas não gastam turno/minutos nem alteram cena | `FictionTurnIntent.freezesWorld`, `MasterOrchestrator` | `T01,T12–T15,T19`, replay misto | Verificar UI e voz quando o Gemini efetivamente responde |
| Mestre conversa, descreve com naturalidade e não repete abertura | `HumanMasterNarrativeProfile`, `FictionTurnDirective` | Inspeção de instruções e testes de contrato | **Qualidade narrativa ainda não homologada por transcrições reais** |
| Regras determinísticas, pistas, rolagem e rejeição de sucessos fabricados | `MasterOrchestrator`, `SceneChoiceCommitGate` e motores de regras existentes | Testes antigos, `T07,T19` | E2E de rolagens e consequência no jogo real pendente |
| Quatro escolhas ancoradas, incluindo iniciativa, sem troca de cena espúria | `SceneOptionDiversifier`, `GroundedSceneActionSlots`, `StoryMomentumAction`, `SceneChoiceCommitGate` | `SceneScreenshotFourActionsRegressionTest`, `SceneActualActionMenuUiRobolectricTest`, `T12–T14` | Faltam experiências repetidas de sessão real com Gemini |
| Preservação de relógio, mundo, missões, conhecimento, SQLite | `AppDb`, `WorldStateEngine`, sistema de persistência existente | `T20`, `FictionMixedFortyTurnRobolectricTest`, regressão antiga | Replay OFFLINE e com apenas efeitos autorizados; não é transcrição de campanha completa |
| Áudio corresponde exclusivamente à resposta aceita e não à última cena antiga | `FictionTurnAudio` e patch de TTS da 6.6.62 | `FictionTurnAudioTraceRobolectricTest`, replay misto | TTS físico/latência/vozes de NPC não verificados no Galaxy A57 |
| Diagnóstico privado por opt-in, com chaves ocultadas | `FictionTurnTrace` e controles da 6.6.62 | `FictionTurnAudioTraceRobolectricTest` | Inspecionar exportação no aparelho; não registrar segredos reais sem consentimento |
| Aceitação T01–T20 e 40 turnos diversos | `FictionTurnIntentContractRobolectricTest` e replay misto adicional | CI https://github.com/bra83/braseirobuild/actions/runs/43808627639 | **Não afirmar 40 turnos reais Gemini**: os testes são determinísticos |
| Atualização APK 6.6.61→6.6.62 preservando backup e fichas | Script de upgrade em emulador Android 35 com canários de cena, ficha sintética, inventário e diário | [Run 43809825164](https://github.com/bra83/braseirobuild/actions/runs/43809825164) **FAIL na primeira execução**: cena voltou de dia 17/18h45 ao dia 1/9h. [Diagnóstico ampliado 43865743746](https://github.com/bra83/braseirobuild/actions/runs/43865743746) **PASS**: cena, ficha sintética, inventário, diário, integridade e arquivo privado preservados; possível intermitência não isolada | **QA sintético aprovado em repetição**, mas a falha inicial ainda requer investigação de intermitência e campanha real não testada |
| Quatro transcrições de referência exigidas no handoff | Ainda sem material verificável em teste | Nenhuma | **BLOQUEADO**: não inventar transcrições nem dizer que foram comparadas |
| Certificação no Galaxy A57 e Gemini API real | Precisa de dispositivo e credenciais no ambiente de QA | Nenhuma execução direta aqui | **BLOQUEADO**: não reivindicar certificado físico |

## CI e qualidade

- Base sucessora 6.6.61, versãoCode 203900624: https://github.com/bra83/braseirobuild/actions/runs/43806616651 — 3213/3213 testes PASS, APK compilado.
- Primeira integração 6.6.62: https://github.com/bra83/braseirobuild/actions/runs/43766444033 — 2 testes assíncronos falharam, logo reprovação integral.
- Nova integração com correção das fixtures de cena, NPC desconhecido, replay misto e TTS: https://github.com/bra83/braseirobuild/actions/runs/43808907278 — **3.215/3.215 PASS**, 240 suítes, 0 falhas/errors/skips, compilação Android e ZIP do Atlas integral; artefato GitHub 14199518474; SHA256 do APK `39a4a5d2cdf8fee07a097d2db1cb11bc8f0bd18f114423cb405fe7f2fc419216`.
- O upgrade sem desinstalação 6.6.61→6.6.62 [run 43809825164](https://github.com/bra83/braseirobuild/actions/runs/43809825164) **falhou**: antes, cena em dia 17 às 18h45; depois, local vazio, dia 1 às 9h; arquivo privado preservado, integridade SQLite ok e schema 61. **Não aprovar atualização de usuário.**
- Diagnóstico de regressão acrescentado à mesma branch QA: compara SQLite depois da reinjeção do canário, depois de `adb install -r` mas antes do lançamento da nova Activity e depois do primeiro lançamento; inclui ficha, inventário e diário sintéticos. [Nova execução 43865743746](https://github.com/bra83/braseirobuild/actions/runs/43865743746) **SUCCESS/PASS**, com `failure_stage=none`, cena idêntica ao canário, ficha sintética com inventário e diário preservados. A repetição mudou os pontos de observação do harness, não o APK; primeira falha não está explicada e deve ser tratada como possível intermitência. Não extrapolar para campanhas reais/Galaxy A57.
- A suíte passou sem testes ignorados. APK continua QA até concluir integração e validações externas.

## Resultado da auditoria

Implementação e aprovação automatizada podem reduzir erros comprovados, mas qualidade de narrativa, 40 turnos reais e experiência física não podem ser inferidas de 3213/3213 testes unitários. Exigir evidência em vez de rótulo `100%`.

- Gate adicional de estabilidade: [run 43974963290](https://github.com/bra83/braseirobuild/actions/runs/43974963290) executa **três ciclos isolados** de atualização em emulador, cada um preservando estado de mundo, ficha, inventário, diário e arquivo privado. Os ciclos começam com instalação limpa da **fixture sintética antiga**, mas a transição 6.6.61→6.6.62 ocorre sempre via `adb install -r`, sem apagar os dados no momento do upgrade. A conclusão ainda deve ser confirmada por `qa-6661-to-6662/summary.json`; não marcar PASS por antecipação. A primeira execução com perda de cena usou fixture sem personagens; não assumir causalidade demonstrada até confirmar via teste diferencial.
