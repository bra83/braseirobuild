# MATRIZ CANÔNICA DE COBERTURA MECÂNICA REAL — STAR WARS VTT — P92

**Baseline auditada:** P91 persistida no Google Drive, promovida localmente para P92 após o bloco P0.  
**Regra de contagem:** uma família só é JOGÁVEL quando o caminho real é `UI R46 → bridge → controller/rules → estado → persistência/efeito → teste`, sem RAG obrigatório.

## Resultado P92
- **55** famílias canônicas.
- **23 JOGÁVEIS** (41.8%).
- **10 ENGINE REAL** sem fluxo completo de UI.
- **6 PARCIAIS**.
- **16 RAG/DECLARAÇÃO**.
- **33/55 (60.0%)** têm engine determinístico real sem RAG; P92 não inflou esse total, apenas converteu engine já existente em jogabilidade real.
- Regressão final: **1031/1031 não-stress + 6/6 stress = 1037/1037 PASS, 0 FAIL**.
- Aventuras preservadas: **Sob um Sol Negro 106/106**, **Além da Orla 57/57**.
- Android: **SEM EVIDÊNCIA DE BUILD**; tentativa parou antes da compilação por `UnknownHostException: downloads.gradle.org`.

## O que P92 mudou de verdade
- Qualidades: Precisa entra no pool; Automática aumenta dificuldade e aplica hits extras; Brecha/Perfurante afetam soak; Letal/Viciosa modifica crítico real.
- Equipamento ativo: arma e armadura equipáveis/persistentes pela R46; armadura inicial legada em `gear` é normalizada sem duplicar inventário.
- Combate pessoal: painel usa encontro/alvos reais; não cria inimigo grátis; ataque, crítico, turno e encerramento passam pelo engine.
- Veículos: painel usa veículo existente; não existe bridge público para conceder veículo.
- Força: painel somente para personagem realmente Force-sensitive; não concede Force Rating.
- Contribution: marco só é resolvido quando o estado real permite; não existe mutação bruta de Duty pela UI.
- Cibernéticos: engine real e persistente, mas instalação ficou deliberadamente fora do bridge público até aquisição/custo estar transacional.
- Hiperespaço: cálculo/rolagem/mishap/persistência agora têm UI, mas ainda falta deslocar relógio e localização; portanto não foi promovido a JOGÁVEL.

## Matriz — 55 famílias canônicas

| ID | Domínio | Estado | Engine s/RAG | UI R46 | Persistência | Teste | Evidência/lacuna | Próximo passo |
|---|---|---|---|---|---|---|---|---|
| CORE.DICE.POOL | Core | JOGÁVEL | SIM | SIM | N/A (cálculo) | SIM | narrativeDice.mjs; r54-device-p6.js; rules.test.mjs | Manter |
| CORE.DICE.SYMBOLS | Core | JOGÁVEL | SIM | SIM | N/A (cálculo) | SIM | resolveSymbols()/rollPool(); 901/901 regressão | Manter |
| CORE.CHECKS.TYPES | Core | PARCIAL | PARCIAL | PARCIAL | SIM quando via controller | PARCIAL | Teste simples funciona; assistência hábil/não-hábil funciona no engine; não há implementação dedicada de teste oposto/competitivo | Implementar opostos/competitivos e expor assistência na UI |
| CORE.DESTINY | Core | ENGINE REAL | SIM | NÃO | SIM | SIM | destiny.mjs; startSession(); eote-core-systemic-r30.test.mjs | Expor gasto/visualização na shell R46 |
| CHAR.CHARACTERISTICS | Personagem | JOGÁVEL | SIM | SIM | SIM | SIM | criação manual EotE/AoR/MIXED na shell R46 | Manter |
| CHAR.ATTRIBUTES | Personagem | JOGÁVEL | SIM | SIM (leitura/efeitos) | SIM | SIM | health.mjs/character.mjs; criação e efeitos de sessão | Manter; depois expor saúde ativa |
| CHAR.SPECIES | Personagem | JOGÁVEL | SIM | SIM | SIM | SIM | 8 espécies EotE + 8 AoR; character-creation tests | Manter |
| CHAR.CAREERS.EOTE | Personagem | JOGÁVEL | SIM | SIM | SIM | SIM | 6 carreiras EotE + specs core | Manter |
| CHAR.OBLIGATION | Campanha EotE | JOGÁVEL | SIM | SIM na criação; automático na sessão | SIM | SIM | obligation.mjs; startSession(); obligation.test.mjs | Adicionar painel de ativação/penalidade na UI |
| SKILLS.GENERAL | Perícias | JOGÁVEL | SIM | SIM via teste sugerido/Mestre | SIM | SIM | resolveSkillCheck(); suggested checks | Manter |
| SKILLS.COMBAT | Perícias | JOGÁVEL | SIM | SIM | SIM | SIM | resolveWeaponAttack + painel R46 COMBATE PESSOAL; P92 runtime/final-shell tests | Manter e ampliar manobras/ações |
| SKILLS.KNOWLEDGE | Perícias | JOGÁVEL | SIM | SIM via teste sugerido/Mestre | SIM | SIM | resolveSkillCheck() | Manter |
| TALENTS.TREES | Talentos | ENGINE REAL | SIM | NÃO | SIM | SIM | talents.mjs/progression.mjs; talents.test.mjs | Criar UI de árvore/compra |
| EQUIP.WEAPONS | Equipamento | JOGÁVEL | SIM | SIM | SIM | SIM | equipmentOwnedSummary/equipWeapon + painel EQUIPAMENTO ATIVO; ataque usa arma equipada | Manter |
| EQUIP.QUALITIES | Equipamento | PARCIAL | PARCIAL | NÃO | PARCIAL | PARCIAL | P92 aplica Precisa no pool antes da rolagem, Automática +1 dificuldade e hits extras, Brecha/Perfurante no soak e Letal/Viciosa no crítico. Ainda faltam qualidades e ativações restantes (ex.: Explosão e ciclo completo de todas as qualities). | Completar todas as qualidades oficiais e seus custos/ativações; manter como PARCIAL até cobertura completa |
| EQUIP.ARMOR | Equipamento | JOGÁVEL | SIM | SIM | SIM | SIM | equipArmor/unequipArmor + painel R46; normaliza armadura legada em gear | Manter |
| EQUIP.ATTACHMENTS | Equipamento | PARCIAL | PARCIAL | NÃO | NÃO completo | PARCIAL | hard points e canInstallAttachment existem; não há instalar/remover/modificar + Mechanics + persistência + UI | Implementar subsistema completo |
| COMBAT.STRUCTURE | Combate | JOGÁVEL | SIM | SIM | SIM | SIM | painel R46 lê encontro real, turno, alvo e encerra turno/combate sem criar inimigos grátis | Expandir iniciativa/slot UX |
| COMBAT.MANEUVERS | Combate | ENGINE REAL | SIM | PARCIAL | SIM | SIM | Engine completo existente; P92 expõe MIRAR e ações básicas de posicionamento/turno, mas não todo o conjunto de manobras pessoais. | Expor manobras restantes sem criar atalhos de custo |
| COMBAT.ACTIONS | Combate | ENGINE REAL | SIM | PARCIAL | SIM | SIM | P92 expõe ataque/critical/end turn e usa engine real, mas não todas as ações pessoais/Medicine/utility. | Expor ações restantes por contexto |
| COMBAT.DAMAGE | Combate | JOGÁVEL | SIM | SIM | SIM | SIM | ataque pessoal aplica dano real ao adversário persistente; Breach/Pierce entram no soak | Manter |
| COMBAT.CRITICAL | Combate | JOGÁVEL | SIM | SIM | SIM | SIM | crítico pendente/ativação pela UI; Vicious entra no d100 real | Manter |
| COMBAT.CONDITIONS | Combate | ENGINE REAL | SIM | NÃO | SIM | SIM | conditions.mjs/statusEffects.mjs | Expor estados na UI |
| COMBAT.HEALING | Combate | ENGINE REAL | SIM | NÃO | SIM | SIM | medicine/stimpack/rest/bacta/oil; recovery.test.mjs | Expor recuperação na shell |
| VEHICLE.STATS | Veículos | JOGÁVEL | SIM | SIM | SIM | SIM | painel R46 mostra nave/veículo ativo e estado real | Manter; não conceder veículo por bridge |
| VEHICLE.MANEUVERS | Veículos | JOGÁVEL | SIM | SIM | SIM | SIM | painel R46 expõe acelerar/frear, aproximar/recuar, evasivas, stay on target e full throttle | Adicionar manobras restantes apenas com fonte |
| VEHICLE.ACTIONS | Veículos | ENGINE REAL | SIM | PARCIAL | SIM | SIM | P92 expõe parte das ações veiculares e ataque; nem todas (ex.: damage control completo) estão no painel. | Completar ações veiculares na UI |
| VEHICLE.COMBAT | Veículos | JOGÁVEL | SIM | SIM | SIM | SIM | painel R46 executa combate veicular sobre veículo já existente | Manter e ampliar actions/critical UI |
| VEHICLE.CRITICAL | Veículos | ENGINE REAL | SIM | NÃO | SIM | SIM | vehicle critical table + repair | Ligar à UI |
| TRAVEL.HYPERSPACE | Viagem | ENGINE REAL | SIM | PARCIAL | SIM | SIM | P92 tem painel R46, cálculo de rota/astrogation, rolagem, mishap e persistência em vehicles.travel.lastHyperspaceJump. Ainda não avança relógio/move cena/local ao destino. | Ligar salto bem-sucedido ao relógio do Mundo Vivo e mudança real de local/cena |
| ENV.HAZARDS | Ambiente | ENGINE REAL | SIM | NÃO | SIM | SIM | queda, sufocamento, fogo, radiação; environmental-hazards.test.mjs | Integrar a cenas/Mestre/UI |
| SOCIAL.ENCOUNTERS | Social | PARCIAL | PARCIAL | PARCIAL via perícia genérica | SIM | NÃO dedicado | Charm/Coercion/Negotiation etc. existem como skills, mas não há motor dedicado de conflito social/oposto | Implementar resolução social oficial |
| AOR.DUTY | Campanha AoR | JOGÁVEL | SIM | SIM na criação; automático na sessão | SIM | SIM | duty.mjs; criação AoR | Adicionar painel de sessão |
| AOR.DUTY.ACTIVATION | Campanha AoR | JOGÁVEL | SIM | AUTOMÁTICA | SIM | SIM | dutyCheck/applyDutyBonus em startSession() | Mostrar gatilho/bônus na UI |
| AOR.CONTRIBUTION | Campanha AoR | JOGÁVEL | SIM | SIM | SIM | SIM | painel PROGRESSÃO mostra Duty/Contribution e resolve marco somente quando elegível via resolveCampaignMilestone | Manter sem expor mutação bruta de Duty |
| AOR.CAREERS | Personagem AoR | JOGÁVEL | SIM | SIM | SIM | SIM | 6 carreiras AoR + specs core na criação | Manter |
| AOR.BASE.REQUISITION | Campanha AoR | PARCIAL | PARCIAL | NÃO | PARCIAL | PARCIAL | Contribution rank calcula modifier/max rarity/navalSupport, mas não existe fluxo real de requisição/base | Implementar requisição transacional e UI |
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
| CYBERNETICS.CORE | ENGINE REAL | Capacidade por Brawn/Fortaleza, instalação/remoção e efeitos persistentes no controller; bridge público é somente leitura por segurança. | Não é JOGÁVEL ainda: instalar/remover ficou propositalmente fora da UI até existir aquisição/custo transacional. |

## Próxima onda recomendada (P93)
1. Completar o ciclo **hiperespaço → tempo → posição/local/cena**, sem teleporte indevido e com persistência.
2. Fechar **attachments/modificações** ponta a ponta, incluindo hard points, custo/teste quando aplicável, persistência e UI.
3. Expor **talentos/especializações** e ações pessoais restantes (Medicine, Stimpack, condições/manobras) usando o engine existente.
4. Criar aquisição transacional de **cibernéticos** antes de permitir instalar/remover na UI.
5. Completar qualidades de arma restantes e só então promover `EQUIP.QUALITIES` de PARCIAL.