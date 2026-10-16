# HANDOFF — BRASEIRO STAR WARS EDGE OF THE GALAXY — R54 P50
## PERSISTÊNCIA DE RAMIFICAÇÕES / REGRESSÃO SHARDADA

Baseline: R54 P49. Incremental, sem GitHub e sem recriação.

## Concluído
- Cobertura persistente adicional para Sob um Sol Negro em save/reopen.
- Umbra: rota social discreta preserva `umbraQuiet`, pista conhecida e anti-spoiler.
- Spyder: sabacc discreto preserva `spyderQuiet`, pista da dívida e não inventa hostilidade Black Sun.
- Zelcomm: solução social preserva `zelcommQuiet` sem armas e sem Obligation pendente.
- Kaa’to: negociação bem-sucedida persiste após reopen, mantém rendição desbloqueada e não cria histórico falso do showdown de combate.
- P49 foi mantido como teste histórico separado; P50 ganhou arquivo próprio.
- Nenhuma alteração em EotE/AoR/MIXED ou Duty/Obligation.

## Evidência
- Focal P49+P50: 8/8 PASS, 0 FAIL (`P50_FOCAL.txt`).
- Regressão shardada: sete shards completos somaram 750 testes, todos PASS; tail não-stress somou mais 113 testes, todos PASS.
- Stress isolado: faction-plan PASS; faction-plans PASS; factions PASS; persistent-adversary PASS; stress-33-sessions PASS em ~17s.
- `stress-33-aftermath-r20.test.mjs` excedeu 20s no ambiente e não concluiu; portanto NÃO declarar regressão ampla total PASS.
- Syntax de `underBlackSun.mjs`: PASS.
- Java/Gradle não alterados; Android/APK não recompilado e não declarado PASS.

## Próxima prioridade
1. Isolar/otimizar ou executar `stress-33-aftermath-r20` em ambiente com teto maior, sem mascarar timeout.
2. Expandir persistência para falhas/Despair nas rotas e consequências pós-epílogo.
3. Iniciar arquitetura da próxima aventura mantendo catálogo/estado/anti-spoiler genéricos.
