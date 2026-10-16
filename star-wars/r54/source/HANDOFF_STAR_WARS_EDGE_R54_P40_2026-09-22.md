# HANDOFF — BRASEIRO STAR WARS EDGE OF THE GALAXY — R54 P40
## Sob um Sol Negro → consequências autônomas por relógio

Baseline: P39 do Google Drive. Alteração incremental, sem GitHub.

## Concluído
- `publishedAdventureWorldPlans` agora é consumido pelo avanço diário do Mundo Vivo.
- Planos possuem `createdDay`, `lastAdvancedDay`, progresso determinístico, ativação e resolução persistentes.
- `pykes-aftermath` converte o desfecho em influência e relação persistente.
- `zelcomm-retaliation` sai de latente, progride e pode criar `pendingWorldConsequences` jogável e conhecida pelo grupo.
- `kaato-return` só existe no desfecho recorrente; pode reativar Kaa’to e cria consequência oculta (`playerKnown:false`) para não vazar o retorno antes de entrar em cena.
- Kaa’to morto continua sem plano de retorno.
- Corrigida uma falha de avanço: `lastTick=0` era tratado como falsy e podia impedir simulação ao primeiro salto do relógio.
- EotE/AoR/MIXED e Duty/Obligation não foram alterados.
- Bundle canônico reconstruído com 59 módulos.

## Evidência
- `P40_FOCAL_REGRESSION.txt`: 23/23 PASS.
- Inclui testes P40, Sob um Sol Negro P36-P39, Mundo Vivo e builder.
- `P40_BUNDLE.txt`: builder OK, 59 módulos; bundle passa `node --check`.
- Tentativa de regressão ampla: infraestrutura encerrou por timeout em 120s após alcançar 719 subtests sem FAIL visível; arquivo `P40_BROAD_REGRESSION.txt` preserva a execução incompleta. NÃO declarar regressão global PASS.

## Não declarar
- Não declarar Android/APK PASS: não houve mudança de camada Android nem marco que justificasse compilação.
- Não declarar regressão global PASS: suite ampla excedeu o limite de execução.

## Próximo bloco
1. Expandir Umbra/Spyder/Zelcomm com encontros intermediários opcionais e soluções sociais/investigativas.
2. Expor apenas consequências `playerKnown:true` na superfície do jogador; manter retornos/planos ocultos para o Mestre até gatilho narrativo.
3. Melhorar UI de cena/NPC/consequências em PT-BR.
4. Fatiar a regressão ampla em grupos para eliminar o timeout e obter evidência global completa.
