# HANDOFF — BRASEIRO STAR WARS EDGE R54 P112

**Data:** 29/09/2026  
**Baseline de entrada:** `BRASEIRO_STAR_WARS_EDGE_R54_P107_SOURCE_CHECKPOINT.zip`  
**Nova versão:** `1.0.112`  
**versionCode:** `10112`

## REGRA DE RETOMADA
Não recriar o projeto, não voltar para P107 ou anteriores como baseline operacional, não usar GitHub como base de correção e não compilar Android/Gradle/APK até o fechamento final solicitado explicitamente pelo usuário. O P112 consolida P108, P109, P110 e P112 em um único source checkpoint.

## P108 — BLOCO ASSASSINO / COMBATE
Fonte primária: EotE Core Rulebook, Drive ID `1UO3Z35J2Dmkfvx1OLyPD3rzJon013EoX`.
- Esquiva: reação preparada, Tensão real e upgrades no próximo ataque recebido.
- Mira Precisa: manobra + Tensão; reduz defesa pessoal persistente do alvo no próximo ataque.
- Tiro de Precisão: manobra; estende alcance e aprimora dificuldade pelo mesmo número de bandas.
- Golpe Direcionado / Anatomia: Destiny luminoso só é gasto após acerto; adicionam Agilidade/Intelecto ao dano; respeitam 1 Destiny do jogador por check.
- Precisão Letal: escolha de perícia de combate persistida na compra e bônus de dano real igual aos ranks dessa perícia.

## P109 — PASSIVOS CORE
- Armeiro: +1 soak enquanto armadura estiver realmente vestida.
- Armeiro Aprimorado: +1 defesa quando armadura vestida possuir soak >= 2.
- Determinação/Resolve: reduz apenas Tensão involuntária, mínimo 1; custos voluntários continuam integrais.
- Recuperação Rápida, Cirurgião, Ajuste Fino e Reparos Sólidos permanecem integrados nos motores já existentes e cobertos pela regressão.

## P110 — TALENTOS ATIVOS DE PERÍCIA
- Mestre das Sombras: 2 Tensão, reduz dificuldade de Sigilo/Atividade Criminal em 1, mínimo Fácil.
- Médico Mestre: 2 Tensão, reduz Medicina em 1, mínimo Fácil.
- Slicer Mestre: 2 Tensão, reduz Computadores em 1, mínimo Fácil.
- Intimidador: Tensão até o rank e downgrades reais na dificuldade de Coerção do jogador.
- Limite de uso por rodada preservado quando há combate estruturado.

## P112 — TALENTOS NATO / RERROLAGEM
Motor reutilizável para 13 famílias EotE Core, todas 1x por sessão e somente se adquiridas:
1. Brigão nato — Briga/Corpo a Corpo.
2. Encantador nato — Carisma/Engano.
3. Médico nato — Medicina.
4. Impositor nato — Coerção/Streetwise.
5. Caçador nato — Percepção/Vigilância.
6. Atirador nato — Ranged Light/Heavy.
7. Negociador nato — Cool/Negociação.
8. Sobrevivente nato — Resiliência/Sobrevivência.
9. Piloto nato — Piloting Space/Artilharia.
10. Programador nato — Computadores/Astronavegação.
11. Trapaceiro nato — Skulduggery/Sigilo.
12. Acadêmico nato — qualquer Knowledge.
13. Funileiro nato — Mecânica.

A rerrolagem usa exatamente o mesmo pool do primeiro teste e preserva o primeiro resultado como evidência no retorno do engine. Nova sessão limpa o ledger.

## CONSERVADORISMO
- Nenhum talento é concedido sem aquisição real.
- Resolve não reduz custos voluntários.
- Destiny não é gasto em Golpe Direcionado/Anatomia se o ataque falhar.
- A metade defensiva de Intimidador contra Coerção recebida ainda não foi promovida.
- Mover da Força, Jamming e `ENV.HAZARDS` continuam sem promoção artificial.
- Contagens de famílias não foram infladas por profundidade dentro de `TALENTS.TREES`/`COMBAT.ACTIONS`.

## COBERTURA CANÔNICA
- 55 famílias.
- 32/55 JOGÁVEIS = 58,2%.
- 35/55 com engine determinístico real = 63,6%.
- O núcleo EotE/AoR ganhou profundidade substancial; o grande saldo restante continua concentrado em expansões e subsistemas contextuais.

## EVIDÊNCIA FINAL
- focal consolidado P108–P112: **24/24 PASS**;
- regressão global incluindo stress: **1215/1215 PASS, 0 FAIL**;
- stress contido na suíte global: **6/6 PASS**;
- bundle offline: **64 módulos**;
- builders raiz/tools: **byte-idênticos**;
- sintaxe JS/MJS/bundle: **PASS**.

## ANDROID
**NÃO EXECUTADO.** Preserva a regra canônica de não compilar Android/Gradle/APK até solicitação explícita de fechamento.

## P112 — RECOMPENSA / REQUISIÇÃO DA ALIANÇA AoR
- Ao atingir 100 Duty, o marco de Contribution agora cria uma recompensa pendente canônica além de subir o rank e zerar Duty.
- Limite de raridade implementado: **3 + Contribution Rank**.
- Equipamento elegível é concedido gratuitamente e entra no inventário mecânico real.
- Veículo/nave elegível entra no registro de ativos e runtime veicular real.
- Ativo estratégico persiste como recurso da base, com escolha do jogador/GM.
- A UI de PROGRESSÃO/CONTRIBUIÇÃO lista apenas recompensas dentro do limite atual.
- Recompensa anterior pendente bloqueia um novo marco para impedir perda silenciosa.
- Fonte recuperada: Age of Rebellion Core Rulebook, seção Contribution/Alliance Rewards; apoio editorial oficial FFG sobre Duty/Contribution.

## COBERTURA APÓS P112
- 55 famílias.
- **32/55 JOGÁVEIS = 58,2%**.
- **35/55 com engine determinístico real = 63,6%**.
- `AOR.BASE.REQUISITION` promovida de RAG/DECLARAÇÃO para JOGÁVEL.

## EVIDÊNCIA P112
- focal P112: **6/6 PASS**;
- regressão global consolidada P108–P112, incluindo stress: **1215/1215 PASS, 0 FAIL**;
- bundle offline: **64 módulos**, builders byte-idênticos;
- Android continua NÃO EXECUTADO.

## PRÓXIMA ONDA RECOMENDADA — P113+
Executar em blocos grandes, não micro-checkpoints:
1. terminar talentos Core restantes que já encaixem em motores existentes;
2. fechar metade defensiva de Intimidador e outros talentos de oposição;
3. auditar qualidades/attachments restantes;
4. fechar requisição/base AoR e Jamming somente com runtime persistente real;
5. depois atacar expansões por pacote completo (especializações + regras + UI + testes), priorizando cobertura mecânica e não catálogo decorativo.
