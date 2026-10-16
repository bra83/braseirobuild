# HANDOFF — BRASEIRO STAR WARS EDGE R54 P93

Data: 27/09/2026
Baseline de entrada: `BRASEIRO_STAR_WARS_EDGE_R54_P92_SOURCE_CHECKPOINT.zip` materializada do Google Drive.
Regras de execução: projeto não recriado; GitHub não usado; Android/Gradle não executado por instrução explícita do usuário; EotE/AoR/MIXED, Duty/Obligation, Sob um Sol Negro, Além da Orla, Mundo Vivo, anti-spoiler e P92 preservados.

## OBJETIVO DO BLOCO
Segundo grande bloco P0 da matriz mecânica: converter engines já reais em fluxo jogável seguro, sem atalhos que concedam recursos ou contornem economia.

## IMPLEMENTADO

### Hiperespaço → tempo → localização/cena
- Corrigido bug real em `resolveHyperspaceJump`: o controller lia campos inexistentes `successes/failures/advantages/threats`; agora usa os símbolos reais de `rollPool()` (`raw.success`, `raw.failure`, `raw.advantage`, `raw.threat`, Triumph/Despair).
- Salto bem-sucedido converte horas em ticks de 6h e avança o Mundo Vivo.
- Se o salto chega ao destino sem desvio, atualiza local/cena de verdade.
- Se há desvio/off-course, o tempo avança mas o VTT NÃO teleporta ao destino pretendido.
- Se a rolagem falha, posição e relógio não avançam.
- `vehicles.travel.lastHyperspaceJump` persiste origem, destino, resultado, ticks avançados e chegada.
- Painel P93 substitui o antigo painel P92 na rota 04 para evitar controles concorrentes.

### Cibernéticos transacionais
- Cibernéticos entraram no catálogo econômico transacional.
- Compra/proposta usa o mesmo fluxo de aquisição/raridade/créditos; adquirir NÃO instala efeitos automaticamente.
- Propriedade é registrada em `group.ownedAssets`.
- Instalação só aceita `assetId` cybernetic realmente possuído.
- Remoção desfaz efeitos mecânicos e mantém propriedade do ativo.
- Perda/venda do ativo instalado remove o efeito corretamente.
- Métodos internos legados de install/remove foram preservados para compatibilidade, mas continuam fora do bridge público bruto.

### Talentos / especializações
- `talentProgressionStatus()` expõe XP, especializações possuídas, árvore e nós elegíveis.
- R46 mostra apenas árvores realmente possuídas.
- Compra passa por `buyTalent()`, consome PE, respeita unlock/pré-requisitos e persiste efeitos.
- Dedication solicita escolha de característica quando necessária.

### Recuperação / condições
- Painel R46 mostra Ferimentos/Tensão, combate e efeitos ativos.
- Stimpack usa `useStimpack` real.
- Medicine usa o engine real de recuperação.
- Descanso completo usa o fluxo de descanso e só é oferecido fora de combate.
- Nenhum botão limpa Ferimentos/Tensão diretamente.

### Bridge / segurança
- Novas superfícies legítimas: `proposeCyberneticAcquisition`, `installOwnedCybernetic`, `removeOwnedCybernetic`, `talentProgressionStatus`, `recoveryRuntimeStatus`, `usePlayerStimpack`, `useMedicine`, `restFullNight`.
- Nenhuma operação pública concede cibernético, Force Rating, veículo ou outro ativo gratuitamente.
- `UI_BRIDGE_VERSION` permanece 3 por compatibilidade de contrato; bundle final identifica `1.0.93-r54-p93`.

## MATRIZ MECÂNICA P93
- 55 famílias canônicas.
- JOGÁVEL: **26/55 = 47,3%** (P92: 23/55 = 41,8%).
- ENGINE REAL: 7.
- PARCIAL: 6.
- RAG/DECLARAÇÃO: 16.
- Engine determinístico real sem RAG continua **33/55 = 60,0%**; P93 converteu alcance real, não inflou cobertura por catálogo.
- Promovidas a JOGÁVEL: `TALENTS.TREES`, `COMBAT.HEALING`, `TRAVEL.HYPERSPACE`.
- Fora das 55 originais: `CYBERNETICS.CORE` passa a JOGÁVEL com aquisição → propriedade → instalação/remoção segura.

## EVIDÊNCIA FINAL
- P92+P93 focal: **21/21 PASS**.
- Sob um Sol Negro: **106/106 PASS**.
- Além da Orla: **57/57 PASS**.
- Regressão não-stress final: **1040/1040 PASS, 0 FAIL**.
- Stress final: **6/6 PASS, 0 FAIL**.
- Total automatizado final: **1046/1046 PASS, 0 FAIL**.
- Sintaxe dos módulos alterados/runtime/bundle: PASS.
- Bundle offline regenerado: **63 módulos**.

## ANDROID
NÃO EXECUTADO. O usuário determinou que Gradle/Android/APK seja compilado somente no fechamento final do projeto.

## VERSÃO
- Android versionCode: 10093
- Android versionName: 1.0.93
- Runtime/shell: 1.0.93
- Bundle: 1.0.93-r54-p93

## PENDÊNCIAS REAIS / P94
1. `EQUIP.ATTACHMENTS`: implementar instalação/modificação/removal source-bound, hard points, custo/teste, persistência e UI.
2. `EQUIP.QUALITIES`: completar qualidades/ativações restantes antes de promover de PARCIAL.
3. Expor `COMBAT.CONDITIONS`, manobras/ações pessoais e veiculares restantes usando engines existentes.
4. Expor Destiny na UI e fechar AoR Requisition transacional.
5. Implementar checks opostos/competitivos e Social Encounters dedicados.
6. Completar Signature Abilities (catálogo + ativação real) antes de expandir novas career books.
