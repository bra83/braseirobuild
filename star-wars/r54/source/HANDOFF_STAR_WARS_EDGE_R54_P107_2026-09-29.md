# HANDOFF — BRASEIRO STAR WARS EDGE R54 P107

**Data:** 29/09/2026  
**Baseline de entrada:** `BRASEIRO_STAR_WARS_EDGE_R54_P106_SOURCE_CHECKPOINT.zip`  
**Nova versão:** `1.0.107`  
**versionCode:** `10107`

## REGRA DE RETOMADA
Não recriar o projeto, não voltar para P106 ou anteriores como baseline operacional, não usar GitHub como base de correção e não compilar Android/Gradle/APK até o fechamento final solicitado explicitamente pelo usuário. O checkpoint P107 no Google Drive passa a ser a autoridade de retomada após sua promoção.

## P107 — TALENTOS DE COMBATE EotE CORE
Fonte primária auditada: `EotE Core Rulebook.pdf`, Google Drive file ID `1UO3Z35J2Dmkfvx1OLyPD3rzJon013EoX`.

### Implementado ponta a ponta
1. **Ataque Frenético** — incidental real; Tensão até o rank; upgrades reais no próximo ataque de Briga/Corpo a Corpo.
2. **Força Feroz** — dano passivo real por rank em Briga/Corpo a Corpo.
3. **Rajada** — dano passivo real por rank em ataques pessoais Pesados/Artilharia a Longo/Extremo.
4. **Passo Lateral** — manobra real, 1×/rodada, Tensão até o rank; upgrades reais na dificuldade de ataques ranged contra o jogador até o próximo turno.
5. **Postura Defensiva** — manobra real, 1×/rodada, Tensão até o rank; upgrades reais em ataques melee contra o jogador até o próximo turno.
6. **Segundo Fôlego** — incidental 1×/encontro, recupera Tensão igual ao rank.
7. **Saque Rápido** — sacar arma acessível como incidental 1×/rodada no fluxo atual de armas, sem gastar manobra.
8. **Golpes Letais** — +10 por rank na rolagem real de Ferimento Crítico infligido.
9. **Bridge/R46** — controles só aparecem quando o talento foi realmente adquirido; custo/limite/duração ficam no engine, não na UI.

## CONSERVADORISMO
- Nenhum talento é concedido sem aquisição real.
- **Dodge** não foi automatizado porque é reação fora do turno e requer decisão do jogador antes do ataque NPC.
- **Precise Aim** não foi exposto sem uma redução segura de defesa do alvo persistente.
- **Targeted Blow / Anatomy Lessons** ficaram para a próxima onda porque precisam respeitar o limite de 1 Destiny Point investido pelo jogador por check.
- **Deadly Accuracy** aguarda persistência da perícia escolhida na aquisição.
- Force Move e `ENV.HAZARDS` continuam sem promoção artificial.

## MATRIZ P107
- 55 famílias canônicas.
- **31/55 JOGÁVEIS = 56,4%**.
- **34/55 com engine determinístico real = 61,8%**.
- Contagens de famílias deliberadamente inalteradas: P107 aprofunda execução dentro de famílias já reais.

## EVIDÊNCIA FINAL
- focal P107: **9/9 PASS**;
- regressão regular global: **1179/1179 PASS**;
- stress: **6/6 PASS**;
- total: **1185/1185 PASS, 0 FAIL**;
- Sob um Sol Negro: **106/106 PASS**;
- Além da Orla: **57/57 PASS**;
- bundle offline: **64 módulos**;
- builders raiz/tools: **byte-idênticos**;
- sintaxe/verificação reproduzível: **PASS**.

## ANDROID
**NÃO EXECUTADO.** O source gate confirma que não há APK/AAB no checkpoint e que o Android gate está externo/bloqueado sem SDK nesta execução; isso também preserva a instrução canônica de não compilar agora.

## PRÓXIMO GRANDE BLOCO — P108
1. Fechar Dodge com reação explícita do jogador, não automática.
2. Ligar Precise Aim/Sniper Shot somente com alvo, defesa e alcance persistentes corretos.
3. Criar ledger por check para Destiny e então ligar Targeted Blow/Anatomy Lessons sem gasto duplicado.
4. Persistir a escolha de perícia de Deadly Accuracy antes de aplicar o passivo.
5. Preservar as duas aventuras e continuar sem Android/Gradle.
