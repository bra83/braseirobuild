# HANDOFF — BRASEIRO STAR WARS EDGE R54 P94

Data: 27/09/2026
Baseline de entrada: `BRASEIRO_STAR_WARS_EDGE_R54_P93_SOURCE_CHECKPOINT.zip` materializada do Google Drive.
Princípios: projeto não recriado; GitHub não usado; Android/Gradle não executado; EotE/AoR/MIXED, Duty/Obligation, Sob um Sol Negro, Além da Orla, Mundo Vivo, anti-spoiler, TTS/Android P93 preservados.

## OBJETIVO DESTE BLOCO
Executar a terceira onda P0 da matriz mecânica, priorizando mecânicas já sustentadas pelo engine e evitando inventar regras ausentes: Destino, condições e manobras pessoais; auditoria visível de attachments/hard points; correção de classificação da requisição AoR.

## IMPLEMENTADO

### Destino
- `destinySummary()` expõe a reserva persistente real à R46.
- jogador pode gastar somente ponto luminoso pela shell; `spendDestiny('light')` converte-o em sombrio pelo Rules Engine e persiste.
- pontos sombrios aparecem como reserva do Mestre; não foi criado botão para o jogador gastar recurso do Mestre.

### Condições
- tela de combate mostra condições realmente presentes em `statusEffects` e os modificadores ativos.
- `disoriented` continua impondo Setback real aos testes/ataques; gate P94 prova o dado adicional no pool.
- `immobilized` bloqueia manobra de movimento real.
- duração/expiração continuam no engine existente; não foi criado botão para aplicar condição arbitrariamente.

### Manobras pessoais
- movimento entre faixas Curto/Médio/Longo/Extremo/Interação usa `spendManeuver` real.
- segunda manobra exige o método legítimo já suportado: 2 de Tensão ou troca da ação.
- Buscar Cobertura consome manobra e concede `coverRangedDefense=1`.
- cobertura adiciona +1 Setback real a ataques NPC à distância.
- mover-se ou encerrar combate remove cobertura.
- Sacar/Trocar Arma consome manobra e só permite arma realmente possuída.
- Aim existente + movimento + cobertura + sacar/trocar estão alcançáveis; `Prepare` e o restante das manobras não foram inventados e mantêm a família abaixo de JOGÁVEL total.

### Attachments / hard points
- `equipmentOwnedSummary()` agora inclui attachments existentes e hard points consumidos/disponíveis.
- R46 exibe hard points/attachments reais em modo de auditoria.
- instalação, remoção e modding continuam bloqueados porque o projeto não contém ainda um catálogo mecânico autorizado completo com efeitos/custos/testes de mod; nenhuma regra foi fabricada.

### Requisição AoR — correção de auditoria
- busca na árvore P94 não encontrou implementação de `requisition`, `maxRarity` ou `navalSupport`.
- `AOR.BASE.REQUISITION` foi corrigido de PARCIAL para `RAG/DECLARAÇÃO` na matriz.
- Contribution continua JOGÁVEL e não foi confundido com requisição.

### Bridge/UI
- `uiBridge.mjs`: destinySummary, changeCombatRange, takeCover e drawWeaponInCombat.
- contratos TypeScript raiz e shell R46 atualizados.
- `final-runtime.js`: painéis P94 em combate, Destino e equipamento/hard points.
- `uiBridge.bundle.js` regenerado com 63 módulos.

## MATRIZ MECÂNICA P94
- 55 famílias canônicas.
- JOGÁVEL: **28/55 = 50,9%** (P93: 26/55 = 47,3%).
- ENGINE REAL sem UI completa: 5.
- PARCIAL: 5.
- RAG/DECLARAÇÃO: 17.
- Engine determinístico real total continua **33/55 = 60,0%**; o bloco aumentou alcance real, não catálogo.
- `CORE.DESTINY` e `COMBAT.CONDITIONS` promovidos a JOGÁVEL.
- `COMBAT.MANEUVERS` continua ENGINE REAL/PARCIAL na UI porque `Prepare` e outras manobras ainda faltam.
- `EQUIP.ATTACHMENTS` continua PARCIAL.

## EVIDÊNCIA FINAL
- P94 focal final + P92/P93 preservados + combate/recovery: **48/48 PASS**.
- Sob um Sol Negro: **106/106 PASS**.
- Além da Orla: **57/57 PASS**.
- Regressão não-stress: **1050/1050 PASS, 0 FAIL**.
- Stress: **6/6 PASS, 0 FAIL**.
- Total automatizado: **1056/1056 PASS, 0 FAIL**.
- Sintaxe controller/uiBridge/runtime/bundle: PASS.
- Bundle: 63 módulos, `1.0.94-r54-p94`.

## ANDROID
**NÃO EXECUTADO**, conforme instrução explícita do usuário: compilar Android/APK somente no fechamento final do projeto.

## VERSÃO
- Android versionCode: 10094
- Android versionName: 1.0.94
- Runtime/shell: 1.0.94
- Bundle label: 1.0.94-r54-p94

## PRÓXIMO BLOCO P95
1. Fechar `EQUIP.ATTACHMENTS` somente após localizar/estruturar perfis mecânicos autorizados; implementar instalar/remover/modificar + hard points + Mechanics + persistência + UI.
2. Completar qualidades oficiais restantes com ativação real, sem inferir regras ausentes.
3. Implementar `Prepare` e ações/manobras pessoais restantes sustentadas pelas fontes do projeto.
4. Implementar checks opostos/competitivos e resolução social dedicada.
5. Completar Signature Abilities por carreira + ativação dos efeitos; manter incompleto enquanto catálogo/runtime não forem integrais.
6. Implementar requisição AoR somente quando o material autorizado fornecer o fluxo/valores necessários.
