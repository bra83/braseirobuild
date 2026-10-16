# R54 P60 — Sob um Sol Negro: tracker de sessão anti-spoiler
Baseline: R54 P59 do Google Drive.

## Implementado
- tracker público de aventura em PT-BR, derivado apenas do estado persistente;
- objetivo atual contextual por fase/cena;
- progresso da investigação (0–3) e readiness após 2 pistas;
- pressão visível: alerta, momentum e Kaa’to alertado;
- milestones visitados: fuga, investigação, endgame, Kaa’to encontrado, conclusão;
- nenhum conteúdo futuro é serializado pelo tracker;
- tracker sobrevive a save/reopen e não muta flags de regras.

## Gates
- focal P60 + journal P55 + branches P41: 12/12 PASS.
- regressão total: 910/910 PASS, 0 FAIL.
- bundle uiBridge regenerado depois da alteração.

## Preservado
EotE/AoR/MIXED; Duty/Obligation; cenas/testes/consequências/ramificações de Sob um Sol Negro; journal, encontros e epílogo.

## Próximo
Ligar tracker à superfície visual da sessão sem expor estado de Mestre; depois fazer passe de UX 415x915 e só compilar em marco relevante.
