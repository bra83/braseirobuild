# MATRIZ CANÔNICA DE COBERTURA MECÂNICA REAL — STAR WARS VTT — P93

**Baseline:** P92 persistida no Google Drive, promovida a P93 após o segundo grande bloco P0.  
**Critério:** `UI R46 → bridge → controller/rules → estado → persistência/efeito → teste`, sem RAG obrigatório.

## Resultado P93
- **55** famílias canônicas.
- **26 JOGÁVEIS** (**47,3%**; P92 tinha 23/55 = 41,8%).
- **7 ENGINE REAL** sem fluxo completo de UI.
- **6 PARCIAIS**.
- **16 RAG/DECLARAÇÃO**.
- **33/55 (60,0%)** continuam com engine determinístico real sem RAG; P93 novamente aumentou jogabilidade, não inflou cobertura por catálogo.
- Regressão final: **1040/1040 não-stress + 6/6 stress = 1046/1046 PASS, 0 FAIL**.
- Aventuras preservadas: **Sob um Sol Negro 106/106** e **Além da Orla 57/57**.
- Android/Gradle: **NÃO EXECUTADO**, conforme instrução de compilar somente no fim.

## Avanços reais P93
- **Hiperespaço:** corrigida leitura dos símbolos narrativos; sucesso avança tempo e local/cena, desvio avança tempo sem teleporte, falha não altera tempo/local.
- **Talentos:** árvores possuídas e nós elegíveis aparecem na R46; compra consome PE e persiste efeitos via engine.
- **Recuperação:** Stimpack, Medicine e descanso completo agora são executáveis na R46 sem mutação direta de HP/strain.
- **Cibernéticos:** aquisição econômica real → propriedade → instalação/remoção somente de ativo possuído; capacidade/efeitos persistem; sem concessão gratuita.
- **UX:** painel P93 de hiperespaco substitui o painel antigo P92 para evitar dois controles concorrentes.

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
| TALENTS.TREES | Talentos | JOGÁVEL | SIM | SIM | SIM | SIM | talentProgressionStatus + buyTalent expostos na R46; compra só árvore possuída/nó desbloqueado; PE e efeitos persistem | Manter; depois melhorar UX de especializações |
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
| CYBERNETICS.CORE | JOGÁVEL | Aquisição transacional desconta créditos e gera ativo possuído; instalação/remoção só aceita asset cybernetic realmente possuído; capacidade e efeitos persistem; nenhum bridge concede implante grátis. | P93 fecha propriedade -> instalação -> remoção com segurança econômica; aquisição pode passar por proposta/Mestre conforme disponibilidade. |

## Próxima onda recomendada (P94)
1. Fechar **attachments/modificações** ponta a ponta: catálogo source-bound, hard points, instalação, custo/teste, efeitos, persistência e UI.
2. Completar **qualidades de armas** restantes e só então promover `EQUIP.QUALITIES` de PARCIAL.
3. Expor **condições, manobras e ações pessoais/veiculares restantes** usando os engines já existentes.
4. Fechar **Destiny** na UI e **AoR requisition** transacional.
5. Depois atacar **checks opostos/competitivos, social encounters e Signature Abilities** antes de abrir nova onda de suplementos.