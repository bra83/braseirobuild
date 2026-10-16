# HANDOFF — BRASEIRO STAR WARS EDGE R54 P113

**Data:** 29/09/2026  
**Baseline de entrada:** `BRASEIRO_STAR_WARS_EDGE_R54_P112_SOURCE_CHECKPOINT.zip`  
**Nova versão:** `1.0.113`  
**versionCode:** `10113`

## REGRA DE RETOMADA
Não recriar o projeto, não voltar para P112 ou anteriores, não usar GitHub como base de correção e não compilar Android/Gradle/APK até o fechamento final solicitado explicitamente pelo usuário. Continuar em blocos grandes e priorizar famílias inteiras ainda RAG/PARCIAL.

## P113 — ENVIRONMENTAL HAZARDS EotE CORE
Fonte primária: `EotE Core Rulebook.pdf`, Drive ID `1UO3Z35J2Dmkfvx1OLyPD3rzJon013EoX`, capítulo VI, Environmental Effects.

### Implementado ponta a ponta
1. Terreno difícil e água dobram manobras; gravidade zero também conta como difícil; Swift/Veloz ignora a penalidade quando adquirido.
2. Terreno intransponível exige Atletismo/Coordenação real antes de liberar movimento.
3. Gravidade alta/baixa e ocultação alteram os pools reais de perícia/ataque.
4. Água submersa e vácuo usam reserva de respiração igual a Fortaleza; depois entram em sufocamento.
5. Vácuo causa 3 Ferimentos/rodada e críticos adicionais após incapacitação contínua.
6. Fogo/ácido/atmosfera corrosiva aplicam Ferimentos por rating a cada início de turno.
7. Sufocamento causa 3 Tensão/turno e críticos nos turnos subsequentes já incapacitados.
8. Quedas Curta/Média/Longa/Extrema seguem dano/Tensão/crítico canônicos; teste Médio de Atletismo/Coordenação reduz dano por Sucesso e Tensão por Vantagem.
9. Tick ambiental integrado ao começo real do turno do jogador, inclusive iniciativa estruturada.
10. R46 possui painel mecânico para ambiente, passagem intransponível e quedas.

## MATRIZ P113
- 55 famílias canônicas.
- **33/55 JOGÁVEIS = 60,0%**.
- **36/55 com engine determinístico real = 65,5%**.
- `ENV.HAZARDS` foi promovida de RAG/DECLARAÇÃO para JOGÁVEL.

## EVIDÊNCIA FINAL
- focal P113: **10/10 PASS**;
- regressão regular: **1219/1219 PASS**;
- stress: **6/6 PASS**;
- total: **1225/1225 PASS, 0 FAIL**;
- Sob um Sol Negro: **106/106 PASS**;
- Além da Orla: **57/57 PASS**;
- bundle offline: **65 módulos**, builders raiz/tools byte-idênticos;
- verify-final-source: **PASS**, 0 APK/AAB.

## ANDROID
**NÃO EXECUTADO.**

## PRÓXIMO BLOCO RECOMENDADO — P114+
Priorizar outra família inteira ainda RAG/PARCIAL, preferencialmente expansão com fonte presente e infraestrutura já reutilizável. Evitar ondas de talentos isolados quando uma família completa puder ser fechada.
