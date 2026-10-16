# MATRIZ CANÔNICA DE COBERTURA MECÂNICA REAL — STAR WARS VTT — P94

**Baseline:** P93 persistida no Google Drive, promovida a P94 após o terceiro grande bloco P0.  
**Critério:** `UI R46 → bridge → controller/rules → estado → persistência/efeito → teste`, sem RAG obrigatório.

## Resultado P94
- **55** famílias canônicas.
- **28 JOGÁVEIS** (**50,9%**; P93 tinha 26/55 = 47,3%).
- **5 ENGINE REAL** sem fluxo completo de UI.
- **5 PARCIAIS**.
- **17 RAG/DECLARAÇÃO**.
- **33/55 (60,0%)** continuam com engine determinístico real sem RAG; P94 aumentou alcance jogável sem transformar catálogo em regra.
- Regressão final: **1050/1050 não-stress + 6/6 stress = 1056/1056 PASS, 0 FAIL**.
- Aventuras preservadas: **Sob um Sol Negro 106/106** e **Além da Orla 57/57**.
- Android/Gradle: **NÃO EXECUTADO**, conforme instrução de compilar somente no fechamento final.

## Avanços reais P94
- **Destino:** reserva persistente visível na R46; gasto legítimo de ponto luminoso passa pelo Rules Engine e o converte em sombrio. O lado sombrio continua reservado ao Mestre/engine, sem botão do jogador.
- **Condições:** estados ativos passaram a aparecer na tela e continuam produzindo o efeito mecânico real; `disoriented` adiciona Setback e `immobilized` bloqueia movimento/manobras.
- **Manobras:** movimento entre faixas usa manobra real; a segunda manobra exige 2 de Tensão ou troca da ação; cobertura dá +1 Setback contra ataque ranged e é perdida ao mover/encerrar combate; sacar/trocar arma consome manobra.
- **Attachments:** hard points e attachments existentes ficam auditáveis na UI, mas instalação/modificação segue bloqueada sem perfil mecânico autorizado. Nenhum attachment é inventado.
- **Correção de matriz:** `AOR.BASE.REQUISITION` foi rebaixado para **RAG/DECLARAÇÃO** porque a auditoria da árvore P94 não encontrou motor dedicado de requisição (`requisition`, `maxRarity` ou `navalSupport`) apesar da descrição antiga da matriz.

## Matriz — 55 famílias canônicas

| ID | Domínio | Estado | Engine s/RAG | UI R46 | Persistência | Teste | Evidência/lacuna | Próximo passo |
|---|---|---|---|---|---|---|---|---|
| CORE.DICE.POOL | Core | JOGÁVEL | SIM | SIM | N/A (cálculo) | SIM | narrativeDice.mjs; r54-device-p6.js; rules.test.mjs | Manter |
| CORE.DICE.SYMBOLS | Core | JOGÁVEL | SIM | SIM | N/A (cálculo) | SIM | resolveSymbols()/rollPool(); 901/901 regressão | Manter |
| CORE.CHECKS.TYPES | Core | PARCIAL | PARCIAL | PARCIAL | SIM quando via controller | PARCIAL | Teste simples funciona; assistência hábil/não-hábil funciona no engine; não há implementação dedicada de teste oposto/competitivo | Implementar opostos/competitivos e expor assistência na UI |
| CORE.DESTINY | Core | JOGÁVEL | SIM | SIM | SIM | SIM | P94: destinySummary + spendDestiny(light) na R46; gasto converte ponto luminoso em sombrio e persiste. Dark permanece reservado ao Mestre/engine. | Manter; depois automatizar política de gasto do Mestre sem expor botão ao jogador |
| CHAR.CHARACTERISTICS | Personagem | JOGÁVEL | SIM | SIM | SIM | SIM | criação manual EotE/AoR/MIXED na shell R46 | Manter |
| CHAR.ATTRIBUTES | Personagem | JOGÁVEL | SIM | SIM (leitura/efeitos) | SIM | SIM | health.mjs/character.mjs; criação e efeitos de sessão | Manter; depois expor saúde ativa |
| CHAR.SPECIES | Personagem | JOGÁVEL | SIM | SIM | SIM | SIM | 8 espécies EotE + 8 AoR; character-creation tests | Manter |
| CHAR.CAREERS.EOTE | Personagem | JOGÁVEL | SIM | SIM | SIM | SIM | 6 carreiras EotE + specs core | Manter |
| CHAR.OBLIGATION | Campanha EotE | JOGÁVEL | SIM | SIM na criação; automático na sessão | SIM | SIM | obligation.mjs; startSession(); obligation.test.mjs | Adicionar painel de ativação/penalidade na UI |
| SKILLS.GENERAL | Perícias | JOGÁVEL | SIM | SIM via teste sugerido/Mestre | SIM | SIM | resolveSkillCheck(); suggested checks | Manter |
| SKILLS.COMBAT | Perícias | JOGÁVEL | SIM | SIM | SIM | SIM | resolveWeaponAttack + painel R46 COMBATE PESSOAL; P92 runtime/final-shell tests | Manter e ampliar manobras/ações |
| SKILLS.KNOWLEDGE | Perícias | JOGÁVEL | SIM | SIM via teste sugerido/Mestre | SIM | SIM | resolveSkillCheck() | Manter |
| TALENTS.TREES | Talentos | JOGÁVEL | SIM | SIM | SIM | SIM | talentProgressionStatus + buyTalent expostos na R46; compra só árvore possuída/nó desbloqueado; PE e efeitos persistem | Manter; depois melhorar UX de especializações |
| EQUIP.WEAPONS | Equipamento | JOGÁVEL | SIM | SIM | SIM | SIM | equipmentOwnedSummary/equipWeapon + painel EQUIPAMENTO ATIVO; ataque usa arma equipada | Manter |
| EQUIP.QUALITIES | Equipamento | PARCIAL | PARCIAL | NÃO | PARCIAL | PARCIAL | P92 aplica Precisa no pool antes da rolagem, Automática +1 dificuldade e hits extras, Brecha/Perfurante no soak e Letal/Viciosa no crítico. Ainda faltam qualidades e ativações restantes (ex.: Explosão e ciclo completo de todas as qualities). | Completar todas as qualidades oficiais e seus custos/ativações; manter como PARCIAL até cobertura completa |
| EQUIP.ARMOR | Equipamento | JOGÁVEL | SIM | SIM | SIM | SIM | equipArmor/unequipArmor + painel R46; normaliza armadura legada em gear | Manter |
| EQUIP.ATTACHMENTS | Equipamento | PARCIAL | PARCIAL | LEITURA | NÃO completo | PARCIAL | P94 mostra hard points e attachments existentes na R46 e preserva canInstallAttachment; instalação/remover/modificar continuam bloqueados sem perfil mecânico autorizado. | Implementar catálogo autorizado + instalar/remover/modificar + Mechanics + persistência |
| COMBAT.STRUCTURE | Combate | JOGÁVEL | SIM | SIM | SIM | SIM | painel R46 lê encontro real, turno, alvo e encerra turno/combate sem criar inimigos grátis | Expandir iniciativa/slot UX |
| COMBAT.MANEUVERS | Combate | ENGINE REAL | SIM | PARCIAL | SIM | SIM | P94 expõe Aim existente, movimento entre faixas, cobertura e sacar/trocar arma com custo real de manobra/2 Tensão/ação. Ainda falta Prepare e cobertura total das manobras oficiais. | Implementar Prepare e manobras restantes sustentadas por fonte |
| COMBAT.ACTIONS | Combate | ENGINE REAL | SIM | PARCIAL | SIM | SIM | P92 expõe ataque/critical/end turn e usa engine real, mas não todas as ações pessoais/Medicine/utility. | Expor ações restantes por contexto |
| COMBAT.DAMAGE | Combate | JOGÁVEL | SIM | SIM | SIM | SIM | ataque pessoal aplica dano real ao adversário persistente; Breach/Pierce entram no soak | Manter |
| COMBAT.CRITICAL | Combate | JOGÁVEL | SIM | SIM | SIM | SIM | crítico pendente/ativação pela UI; Vicious entra no d100 real | Manter |
| COMBAT.CONDITIONS | Combate | JOGÁVEL | SIM | SIM | SIM | SIM | P94 exibe condições ativas na R46; statusModifiers continua impondo bloqueios/Setback. Testes provam disoriented→Setback e immobilized→bloqueio de movimento. | Manter e melhorar nomes/descrições PT-BR |
| COMBAT.HEALING | Combate | JOGÁVEL | SIM | SIM | SIM | SIM | Stimpack, Medicine e descanso completo expostos na R46 usando recovery/medicine engine; não limpam estado diretamente | Manter e ampliar feedback de resultados |
| VEHICLE.STATS | Veículos | JOGÁVEL | SIM | SIM | SIM | SIM | painel R46 mostra nave/veículo ativo e estado real | Manter; não conceder veículo por bridge |
| VEHICLE.MANEUVERS | Veículos | JOGÁVEL | SIM | SIM | SIM | SIM | painel R46 expõe acelerar/frear, aproximar/recuar, evasivas, stay on target e full throttle | Adicionar manobras restantes apenas com fonte |
| VEHICLE.ACTIONS | Veículos | ENGINE REAL | SIM | PARCIAL | SIM | SIM | P92 expõe parte das ações veiculares e ataque; nem todas (ex.: damage control completo) estão no painel. | Completar ações veiculares na UI |
| VEHICLE.COMBAT | Veículos | JOGÁVEL | SIM | SIM | SIM | SIM | painel R46 executa combate veicular sobre veículo já existente | Manter e ampliar actions/critical UI |
| VEHICLE.CRITICAL | Veículos | ENGINE REAL | SIM | NÃO | SIM | SIM | vehicle critical table + repair | Ligar à UI |
| TRAVEL.HYPERSPACE | Viagem | JOGÁVEL | SIM | SIM | SIM | SIM | P93 corrige leitura dos símbolos narrativos; salto bem-sucedido avança Mundo Vivo em ticks de 6h e muda local/cena somente se não sair da rota; falha não move nem avança tempo | Manter; ampliar destinos/feedback visual |
| ENV.HAZARDS | Ambiente | ENGINE REAL | SIM | NÃO | SIM | SIM | queda, sufocamento, fogo, radiação; environmental-hazards.test.mjs | Integrar a cenas/Mestre/UI |
| SOCIAL.ENCOUNTERS | Social | PARCIAL | PARCIAL | PARCIAL via perícia genérica | SIM | NÃO dedicado | Charm/Coercion/Negotiation etc. existem como skills, mas não há motor dedicado de conflito social/oposto | Implementar resolução social oficial |
| AOR.DUTY | Campanha AoR | JOGÁVEL | SIM | SIM na criação; automático na sessão | SIM | SIM | duty.mjs; criação AoR | Adicionar painel de sessão |
| AOR.DUTY.ACTIVATION | Campanha AoR | JOGÁVEL | SIM | AUTOMÁTICA | SIM | SIM | dutyCheck/applyDutyBonus em startSession() | Mostrar gatilho/bônus na UI |
| AOR.CONTRIBUTION | Campanha AoR | JOGÁVEL | SIM | SIM | SIM | SIM | painel PROGRESSÃO mostra Duty/Contribution e resolve marco somente quando elegível via resolveCampaignMilestone | Manter sem expor mutação bruta de Duty |
| AOR.CAREERS | Personagem AoR | JOGÁVEL | SIM | SIM | SIM | SIM | 6 carreiras AoR + specs core na criação | Manter |
| AOR.BASE.REQUISITION | Campanha AoR | RAG/DECLARAÇÃO | NÃO dedicado | NÃO | NÃO dedicado | NÃO | Auditoria P94 não encontrou motor de requisição nem símbolos requisition/maxRarity/navalSupport no runtime; Contribution existe, mas não equivale a requisição. | Implementar somente quando fonte autorizada definir fluxo/valores |
| EXP.EOTE.HIREDGUN | Expansão | RAG/DECLARAÇÃO | NÃO | NÃO | NÃO | NÃO | Demolitionist/Heavy/Enforcer não estão nas árvores de especialização | Implementar ou retirar alegação |
| EXP.EOTE.SIGNATURE | Expansão | PARCIAL | PARCIAL | NÃO | SIM compra | PARCIAL | 7 abilities para apenas 4 carreiras EotE; Colonist/Explorer e AoR faltam; não há runtime de ativação dos efeitos | Completar catálogo + ativação real |
| EXP.EOTE.PAY_GUN | Expansão | RAG/DECLARAÇÃO | NÃO | NÃO | NÃO | NÃO | Sem motor de contratos/equipes mercenárias | Implementar |
| EXP.EOTE.EXPLORER | Expansão | RAG/DECLARAÇÃO | NÃO | NÃO | NÃO | NÃO | Archeologist/Big Game Hunter/Driver ausentes | Implementar |
| EXP.EOTE.SURVIVAL | Expansão | RAG/DECLARAÇÃO | NÃO específico | NÃO | NÃO específico | NÃO | Há Survival genérico, mas não o subsistema avançado declarado | Implementar regras específicas |
| EXP.EOTE.COLONIST | Expansão | RAG/DECLARAÇÃO | NÃO | NÃO | NÃO | NÃO | Doctor/Marshal/Performer de Far Horizons não integrados como expansão | Implementar |
| EXP.EOTE.HOMESTEADS | Expansão | RAG/DECLARAÇÃO | NÃO | NÃO | NÃO | NÃO | Mundo/economia genéricos não equivalem ao sistema oficial de Homesteads | Implementar |
| EXP.EOTE.SMUGGLER | Expansão | RAG/DECLARAÇÃO | NÃO | NÃO | NÃO | NÃO | Charmer/Gambler/Gunslinger ausentes | Implementar |
| EXP.EOTE.GAMBLING | Expansão | JOGÁVEL | SIM | SIM dentro de Sob um Sol Negro | SIM | SIM | Sabacc persistente em underBlackSun.mjs | Separar como subsistema reutilizável se desejado |
| EXP.EOTE.SMUGGLING | Expansão | RAG/DECLARAÇÃO | NÃO | NÃO | NÃO | NÃO | Sem inspeção alfandegária/compartimentos/suborno estruturado | Implementar |
| EXP.EOTE.TECHNICIAN | Expansão | RAG/DECLARAÇÃO | NÃO | NÃO | NÃO | NÃO | Cyber Tech/Droid Tech/Modder ausentes | Implementar |
| EXP.EOTE.CRAFTING | Expansão | RAG/DECLARAÇÃO | NÃO | NÃO | NÃO | NÃO | Sem crafting de armas/armaduras/droides/gadgets | Implementar |
| EXP.AOR.SOLDIER | Expansão | RAG/DECLARAÇÃO | NÃO | NÃO | NÃO | NÃO | Heavy/Vanguard de Forged in Battle não integrados | Implementar |
| EXP.AOR.ENTRENCHED | Expansão | RAG/DECLARAÇÃO | NÃO | NÃO | NÃO | NÃO | Sem fortificação/fogo supressivo específico | Implementar |
| EXP.AOR.SPY | Expansão | RAG/DECLARAÇÃO | NÃO | NÃO | NÃO | NÃO | Sleeper Agent/Interrogator de Cyphers & Masks ausentes | Implementar |
| EXP.AOR.ESPIONAGE | Expansão | RAG/DECLARAÇÃO | NÃO | NÃO | NÃO | NÃO | Sem identidade falsa/grampos/contraespionagem estruturada | Implementar |
| EXP.AOR.ENGINEER | Expansão | RAG/DECLARAÇÃO | NÃO | NÃO | NÃO | NÃO | Droid Specialist e conteúdo expansion-specific não integrados | Implementar |
| EXP.AOR.VEHICLE_CRAFTING | Expansão | RAG/DECLARAÇÃO | NÃO | NÃO | NÃO | NÃO | Sem construção/customização oficial de veículos/bases | Implementar |

## Mecânicas reais adicionais fora das 55 do índice original

| ID | Estado | Implementação real | Observação |
|---|---|---|---|
| FORCE.CORE | JOGÁVEL | Force Rating, dado da Força, compra de poderes/upgrades, commit/release e conversão light/dark; P92 expõe painel apenas para personagem Force-sensitive | Sem grant artificial de sensibilidade à Força |
| FORCE.SENSE | ENGINE REAL | Sense básico + upgrades/alcance/magnitude/pensamentos | Sem UI atual |
| FORCE.INFLUENCE | ENGINE REAL | Influence básico, strain e regra de emoção por lado | Sem UI atual |
| FORCE.MOVE | ENGINE REAL | Move básico, Strength/Range/Magnitude e Hurl damage | Sem UI atual |
| FEAR.CHECKS | ENGINE REAL | Dificuldade, rolagem e consequências de medo | Bridge existe; shell principal não usa o adapter |
| ECONOMY.ACQUISITION | JOGÁVEL | Raridade/disponibilidade, pedido pendente e decisão estruturada do Mestre | UI atual permite propor armas ao Mestre |
| ECONOMY.ASSET_LIFECYCLE | ENGINE REAL | grant/lose/damage/repair/sell catalog asset | Pode ser acionado por proposal do Mestre; sem painel direto |
| TRAVEL.SUBLIGHT | ENGINE REAL | Tempo subluz e planejamento interstelar | Sem UI dedicada |
| GEAR.ACTIVE_EFFECTS | ENGINE REAL | óptica, tool kit, patch, reload, restraints | Aplicação parcial via engine; sem UI dedicada |
| SESSION.LIFECYCLE | JOGÁVEL | start/close, Destiny, Obligation/Duty, XP, histórico | Start é usado automaticamente por sandbox/aventura |
| CYBERNETICS.CORE | JOGÁVEL | Aquisição transacional desconta créditos e gera ativo possuído; instalação/remoção só aceita asset cybernetic realmente possuído; capacidade e efeitos persistem; nenhum bridge concede implante grátis. | P93 fecha propriedade -> instalação -> remoção com segurança econômica; aquisição pode passar por proposta/Mestre conforme disponibilidade. |

## Próxima onda recomendada (P95)
1. Fechar **attachments/modificações** ponta a ponta: catálogo source-bound, hard points, instalação, custo/teste, efeitos, persistência e UI.
2. Completar **qualidades de armas** restantes e só então promover `EQUIP.QUALITIES` de PARCIAL.
3. Expor **condições, manobras e ações pessoais/veiculares restantes** usando os engines já existentes.
4. Fechar **Destiny** na UI e **AoR requisition** transacional.
5. Depois atacar **checks opostos/competitivos, social encounters e Signature Abilities** antes de abrir nova onda de suplementos.