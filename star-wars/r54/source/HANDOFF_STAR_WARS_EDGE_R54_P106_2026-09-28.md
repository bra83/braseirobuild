# HANDOFF — BRASEIRO STAR WARS EDGE R54 P106

**Data:** 28/09/2026  
**Baseline de entrada:** `BRASEIRO_STAR_WARS_EDGE_R54_P105_SOURCE_CHECKPOINT.zip`  
**Nova versão:** `1.0.106`  
**versionCode:** `10106`

## REGRA DE RETOMADA
Não recriar o projeto, não voltar para P105 ou anteriores, não usar GitHub para correção e não compilar Android/Gradle/APK até o fechamento final solicitado explicitamente pelo usuário.

## P106 — TALENTOS ATIVOS EotE CORE
Fonte primária auditada: `EotE Core Rulebook.pdf`, Google Drive file ID `1UO3Z35J2Dmkfvx1OLyPD3rzJon013EoX`.

### Implementado ponta a ponta
1. **Foco Intenso** — manobra real, sofre 1 Tensão e aprimora a habilidade do próximo teste uma vez. Usa o mesmo orçamento P104 de manobras, carga e Ferimentos Críticos.
2. **Sentir o Perigo** — incidental; remove até 2 Setbacks de um teste; uso único por sessão persistido em `sessionEffects.talentUses`; nova sessão limpa o ledger.
3. **Toque do Destino** — incidental; +2 Boost a um teste; uso único por sessão persistido no mesmo ledger.
4. **Levantar Rápido** — se adquirido, levanta de prone como incidental uma vez no turno sem gastar manobra.
5. **Equilíbrio** — ao fim do encontro, opcionalmente rola Force Rating e recupera Tensão adicional igual aos resultados luminosos.
6. **Bridge/R46** — `resolveSkillCheck` ficou diretamente exposto; painel de teste simples permite escolher os talentos realmente adquiridos; painel de combate oferece Levantar Rápido/Equilíbrio somente quando aplicáveis.

## CONSERVADORISMO
- Nenhum talento é concedido sem aquisição real.
- Assistir, Interagir com Ambiente e Montar/Desmontar não foram automatizados sem alvo/estado contextual.
- Move da Força não ganhou UI porque ainda não há objeto persistente de cena adequado para alvo/posição/silhueta.
- `ENV.HAZARDS` continua sem promoção artificial.

## MATRIZ P106
- 55 famílias canônicas.
- **31/55 JOGÁVEIS = 56,4%**.
- **34/55 com engine determinístico real = 61,8%**.
- Contagens conservadoras mantidas: P106 melhora profundidade/reachability, não infla cobertura.

## EVIDÊNCIA FINAL
- focal P106: **7/7 PASS**;
- regressão afetada: **54/54 PASS**;
- regressão regular global: **1170/1170 PASS**;
- stress: **6/6 PASS**;
- total: **1176/1176 PASS, 0 FAIL**;
- Sob um Sol Negro: **106/106 PASS**;
- Além da Orla: **57/57 PASS**;
- bundle offline: **64 módulos**;
- sintaxe: **PASS**.

## ANDROID
**NÃO EXECUTADO.**

## PRÓXIMO GRANDE BLOCO — P107
1. Continuar talentos/ações pessoais source-bound que já possam produzir efeito verdadeiro.
2. Priorizar talentos de combate com custo/efeito determinísticos antes de sistemas contextuais.
3. Só implementar Assist/Interagir/Montar quando houver estado persistente suficiente.
4. Preservar as aventuras e continuar sem Android/Gradle.
