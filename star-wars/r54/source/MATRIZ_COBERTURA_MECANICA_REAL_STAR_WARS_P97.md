# MATRIZ CANÔNICA DE COBERTURA MECÂNICA REAL — STAR WARS VTT — P97

**Baseline:** P96 persistida no Google Drive, promovida a P97 após a onda de orçamento único de resultados e qualidades ativas source-bound.  
**Critério:** `UI R46 → bridge → controller/rules → estado → persistência/efeito → teste`, sem RAG obrigatório.

## Resultado P97
- **55** famílias canônicas.
- **31 JOGÁVEIS** (**56,4%**; inalterado em relação ao P96, por critério conservador).
- **4 ENGINE REAL** sem fluxo completo de UI.
- **3 PARCIAIS**.
- **17 RAG/DECLARAÇÃO**.
- **35/55 (63,6%)** têm engine determinístico completo sem RAG; P97 aprofundou correção mecânica sem promover família ainda incompleta.
- Regressão final: **1086/1086 não-stress + 6/6 stress = 1092/1092 PASS, 0 FAIL**.
- Aventuras preservadas: **Sob um Sol Negro 106/106** e **Além da Orla 57/57**.
- Android/Gradle: **NÃO EXECUTADO**, conforme instrução de compilar somente no fechamento final.

## Avanços reais P97
- **Orçamento único de resultados:** Vantagens/Triunfos do ataque agora alimentam um único ledger persistente. Automática, crítico e qualidades ativas não podem gastar os mesmos símbolos duas vezes.
- **Automática:** deixou de gerar acertos extras automaticamente; cada acerto adicional exige ativação explícita e 2 Vantagens do orçamento restante.
- **Explosão:** sucesso exige seleção explícita dos alvos realmente Engajados com o alvo original; em falha custa 3 Vantagens e afeta o alvo original. A UI não presume proximidade entre NPCs.
- **Desorientação / Concussiva / Enredante:** aplicam estados reais (`disoriented`, `staggered`, `immobilized`) com duração pelo rating; os efeitos entram no turno/rolagem real.
- **Atordoadora / Incendiária:** Atordoadora aplica Tensão direta; Incendiária persiste dano periódico pelo número de rodadas do rating.
- **Hendedura:** custa 1 Vantagem por ativação, avança dano do item por etapas e passa a afetar uso real da arma danificada (Contratempo/Dificuldade/inutilização).
- **Vinculada:** cada ativação custa 2 Vantagens, respeita o rating máximo e aplica acerto adicional real.
- **Modo atordoante:** toggle persistente; converte o dano em Tensão e limita o alcance a Curto.
- **Critérios conservadores:** `EQUIP.QUALITIES` continua PARCIAL porque Guiada, Derrubar, Iônica/Trator e alguns perfis passivos/veiculares ainda não têm cobertura integral e segura.

## Matriz — 55 famílias canônicas

| ID | Domínio | Estado | Engine s/RAG | UI R46 | Persistência | Teste | Evidência/lacuna | Próximo passo |
|---|---|---|---|---|---|---|---|---|
| CORE.DICE.POOL | Core | JOGÁVEL | SIM | SIM | N/A (cálculo) | SIM | narrativeDice.mjs; r54-device-p6.js; rules.test.mjs | Manter |
| CORE.DICE.SYMBOLS | Core | JOGÁVEL | SIM | SIM | N/A (cálculo) | SIM | resolveSymbols()/rollPool(); 901/901 regressão | Manter |
| CORE.CHECKS.TYPES | Core | JOGÁVEL | SIM | SIM | SIM | SIM | P95: teste simples + assistência hábil/não-hábil + oposto + competitivo; oposição converte maior rank em pool negativo e menor em upgrades; competição desempata por sucessos, Triunfos e Vantagens; R46 rota 01; advancedChecks persiste e reabre | Manter; ampliar modificadores contextuais de cena sem alterar a regra-base |
| CORE.DESTINY | Core | JOGÁVEL | SIM | SIM | SIM | SIM | P94: destinySummary + spendDestiny(light) na R46; gasto converte luminoso em sombrio e persiste; dark reservado ao Mestre/engine | Manter; depois automatizar política de gasto do Mestre sem botão do jogador |
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
| EQUIP.QUALITIES | Equipamento | PARCIAL | PARCIAL | PARCIAL | SIM parcial | SIM | P92–P97: Precisa, Automática com gasto explícito, Brecha/Perfurante, Letal/Viciosa, Imprecisa, Aparatosa, Desajeitada, Defensiva, Deflexão, Preparação, Munição Limitada, Cadência Lenta, Explosão, Desorientação, Atordoadora, Incendiária, Concussiva, Enredante, Hendedura, Vinculada e Modo atordoante produzem efeitos reais. Um único ledger impede gasto duplicado de Vantagens/Triunfos. Ainda faltam Guiada, Derrubar, Iônica/Trator e cobertura integral de outros perfis. | Completar qualidades restantes e só então promover a JOGÁVEL |
| EQUIP.ARMOR | Equipamento | JOGÁVEL | SIM | SIM | SIM | SIM | equipArmor/unequipArmor + painel R46; normaliza armadura legada em gear | Manter |
| EQUIP.ATTACHMENTS | Equipamento | PARCIAL | PARCIAL | LEITURA | NÃO completo | PARCIAL | P94 mostra hard points e attachments existentes na R46; instalação/remover/modificar seguem bloqueados sem perfil mecânico autorizado | Implementar catálogo autorizado + instalar/remover/modificar + Mechanics + persistência |
| COMBAT.STRUCTURE | Combate | JOGÁVEL | SIM | SIM | SIM | SIM | painel R46 lê encontro real, turno, alvo e encerra turno/combate sem criar inimigos grátis | Expandir iniciativa/slot UX |
| COMBAT.MANEUVERS | Combate | ENGINE REAL | SIM | PARCIAL | SIM | SIM | P94/P96 expõem Aim, movimento, cobertura, sacar/trocar arma e Preparação source-bound com custo real. Ainda não há cobertura integral de todas as manobras oficiais/contextuais. | Completar manobras restantes sustentadas por fonte |
| COMBAT.ACTIONS | Combate | ENGINE REAL | SIM | PARCIAL | SIM | SIM | P92 expõe ataque/critical/end turn e usa engine real, mas não todas as ações pessoais/Medicine/utility. | Expor ações restantes por contexto |
| COMBAT.DAMAGE | Combate | JOGÁVEL | SIM | SIM | SIM | SIM | ataque pessoal aplica dano real ao adversário persistente; Breach/Pierce entram no soak | Manter |
| COMBAT.CRITICAL | Combate | JOGÁVEL | SIM | SIM | SIM | SIM | crítico pendente/ativação pela UI; Vicious entra no d100 real | Manter |
| COMBAT.CONDITIONS | Combate | JOGÁVEL | SIM | SIM | SIM | SIM | P94 exibe condições; disoriented aplica Setback e immobilized bloqueia movimento/manobras | Manter e melhorar nomes/descrições PT-BR |
| COMBAT.HEALING | Combate | JOGÁVEL | SIM | SIM | SIM | SIM | Stimpack, Medicine e descanso completo expostos na R46 usando recovery/medicine engine; não limpam estado diretamente | Manter e ampliar feedback de resultados |
| VEHICLE.STATS | Veículos | JOGÁVEL | SIM | SIM | SIM | SIM | painel R46 mostra nave/veículo ativo e estado real | Manter; não conceder veículo por bridge |
| VEHICLE.MANEUVERS | Veículos | JOGÁVEL | SIM | SIM | SIM | SIM | painel R46 expõe acelerar/frear, aproximar/recuar, evasivas, stay on target e full throttle | Adicionar manobras restantes apenas com fonte |
| VEHICLE.ACTIONS | Veículos | ENGINE REAL | SIM | PARCIAL | SIM | SIM | P92 expõe ataque e ações/pilotagem principais; P95 adiciona Controle de Danos real por Mecânica e consumo de ação. Ainda faltam ações oficiais como co-pilotagem, scan/slice/jamming/boost shields conforme fonte. | Implementar as ações veiculares restantes source-bound antes de promover a JOGÁVEL |
| VEHICLE.COMBAT | Veículos | JOGÁVEL | SIM | SIM | SIM | SIM | painel R46 executa combate veicular sobre veículo já existente | Manter e ampliar actions/critical UI |
| VEHICLE.CRITICAL | Veículos | JOGÁVEL | SIM | SIM | SIM | SIM | P96: ataque veicular real gera Critical Hit pendente no alvo quando elegível; ativação usa tabela d100 real, preserva a rolagem quando exige componente e persiste o crítico. P95 já havia fechado reparo por Mecânica na dificuldade do hit. | Manter; ampliar feedback visual e ataques NPC veiculares quando o fluxo NPC estiver ativo |
| TRAVEL.HYPERSPACE | Viagem | JOGÁVEL | SIM | SIM | SIM | SIM | P93 corrige leitura dos símbolos narrativos; salto bem-sucedido avança Mundo Vivo em ticks de 6h e muda local/cena somente se não sair da rota; falha não move nem avança tempo | Manter; ampliar destinos/feedback visual |
| ENV.HAZARDS | Ambiente | ENGINE REAL | SIM | NÃO | SIM | SIM | queda, sufocamento, fogo, radiação; environmental-hazards.test.mjs | Integrar a cenas/Mestre/UI |
| SOCIAL.ENCOUNTERS | Social | JOGÁVEL | SIM | SIM | SIM | SIM | P95: fluxo social dedicado na R46; Coerção/Engano/Liderança vs Disciplina, Carisma vs Frialdade, Negociação vs Negociação ou Frialdade; social.lastInfluence/history persistem; combinações inválidas são rejeitadas | Integrar consequências sociais específicas de aventuras/NPCs sem automatizar interpretação narrativa |
| AOR.DUTY | Campanha AoR | JOGÁVEL | SIM | SIM na criação; automático na sessão | SIM | SIM | duty.mjs; criação AoR | Adicionar painel de sessão |
| AOR.DUTY.ACTIVATION | Campanha AoR | JOGÁVEL | SIM | AUTOMÁTICA | SIM | SIM | dutyCheck/applyDutyBonus em startSession() | Mostrar gatilho/bônus na UI |
| AOR.CONTRIBUTION | Campanha AoR | JOGÁVEL | SIM | SIM | SIM | SIM | painel PROGRESSÃO mostra Duty/Contribution e resolve marco somente quando elegível via resolveCampaignMilestone | Manter sem expor mutação bruta de Duty |
| AOR.CAREERS | Personagem AoR | JOGÁVEL | SIM | SIM | SIM | SIM | 6 carreiras AoR + specs core na criação | Manter |
| AOR.BASE.REQUISITION | Campanha AoR | RAG/DECLARAÇÃO | NÃO dedicado | NÃO | NÃO dedicado | NÃO | Auditoria P94 não encontrou motor de requisição nem requisition/maxRarity/navalSupport; Contribution não equivale a requisição | Implementar somente com fonte autorizada |
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
As mecânicas adicionais registradas em P94 (Force Core, Sense/Influence/Move, Fear, aquisição/economia, sublight, gear effects, session lifecycle e cybernetics) permanecem sem regressão. A contagem principal continua limitada às 55 famílias canônicas para não inflar o progresso.

## Próxima onda recomendada (P98)
1. Fechar **Guiada, Derrubar e qualidades Iônica/Trator** somente onde o estado atual comportar os efeitos sem inventar silhueta/proximidade.
2. Estruturar **attachments/modificações** a partir de perfis mecânicos autorizados e machine-readable; manter bloqueado o que exigir adjudicação textual não estruturada.
3. Completar **Signature Abilities** por carreira e ativação real dos efeitos; não importar o catálogo antigo incompleto como se fosse integral.
4. Ampliar manobras/ações restantes e efeitos de estado que ainda não têm ciclo completo de remoção/escape.
5. Preservar aventuras, EotE/AoR/MIXED, Duty/Obligation e regressão global antes de qualquer nova promoção.
