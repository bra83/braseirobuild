# Handoff imersão e continuidade — verificação independente (16/11/2026)

## Base e ancestralidade
- Fonte solicitada: `fix/dnd-6659-action-status-stall-20261115`, SHA `39fbbd06a3c926e5904021901a8188843ec7f171`.
- Branch sucessor encontrado ANTES de editar: `fix/dnd-6661-immersao-continuidade-handoff-20261115`, HEAD inicial `1af746cbdb507ea6b2c3ede031a949e245d017ea`. Comparação GitHub: **ahead 33, behind 0**, merge-base exatamente o SHA da fonte. Nenhuma recriação de projeto.
- Versão atual do sucessor: `6.6.61-immersive-continuity-QA`, `VERSION_CODE=203900624`, `DB_SCHEMA=61`, `BACKUP_FORMAT=21`.
- Alterações existentes revisadas: `FictionTurnIntent`, `NpcPerceptionLedger`, `FictionTurnDirective`, `FictionTurnTrace`, `FictionTurnAudio`, `SceneChoiceCommitGate`, `MainActivity`, `MasterOrchestrator` e testes de contrato.

## Implementação adicional nesta verificação
- `FictionTurnIntent.resolveReferent` descartava qualquer NPC com `name=""`, mesmo que presente na cena e perceptível. Corrigido para permitir referente anônimo por índice público, sem inferir nome e sem revelar ID privado.
- Novo teste `T05b_anonymousPersonRemainsObservableWithoutInventedName`: verifica classificação, ausência de avanço, resolução do referente, descrição e não divulgação do ID.
- Workflow independente: `.github/workflows/dnd-6661-anonymous-npc-regression.yml`, run https://github.com/bra83/braseirobuild/actions/runs/43806616651. Exige suíte integral, 0 falhas e compilação Android com mapa Faerûn original.

## Limites e bloqueios objetivos
- **Não é certificação integral**: testes offline de intenção e prompt não demonstram qualidade narrativa de Gemini real nem funcionamento físico no Galaxy A57.
- A tentativa de promover `6.6.62` falhou no CI https://github.com/bra83/braseirobuild/actions/runs/43766444033: **3211 testes, 2 falhas** em `HandleAiResponseAsyncTest`, nas linhas 108 e 204. Não declarar 6.6.62 aprovada nem ocultar essas falhas.
- Replay de 40 turnos encontrado no teste T20 é **offline/determinístico**, não transcrição de 40 turnos reais de Gemini. Ele cobre observações e ticks explícitos, mas não demonstra todas as fases narrativas exigidas pelo handoff (conversa, investigação, testes, viagem, descanso, reencontro).
- Quatro transcrições de referência: **não localizadas/verificadas nesta inspeção**; não declarar incorporadas.
- Sem evidência de Gemini real, save pessoal do usuário ou upgrade físico nesta execução. T01–T20 existentes contêm testes de contrato, mas não comprovam todos os resultados semânticos exigidos no documento; em particular a resposta efetivamente narrada, conhecimento e consequências precisam de validação end-to-end.
- Antes de liberar APK novo, requer-se CI verde, teste de upgrade sem perda de dados, assinatura compatível e replay com Gemini real registrado. O workflow acima **não inclui** teste de upgrade e não deve ser tratado como homologação.

## Evidências
- Compare: https://github.com/bra83/braseirobuild/compare/39fbbd06a3c926e5904021901a8188843ec7f171...1af746cbdb507ea6b2c3ede031a949e245d017ea
- Branch: https://github.com/bra83/braseirobuild/tree/fix/dnd-6661-immersao-continuidade-handoff-20261115
- CI de regressão: https://github.com/bra83/braseirobuild/actions/runs/43806616651
- Falha 6.6.62: https://github.com/bra83/braseirobuild/actions/runs/43766444033

**Critério de entrega**: não confundir commit com aprovação. Os bloqueios permanecem abertos até haver logs e reprodução real.
