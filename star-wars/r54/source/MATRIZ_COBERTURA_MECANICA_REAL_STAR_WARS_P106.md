# MATRIZ CANÔNICA DE COBERTURA MECÂNICA REAL — STAR WARS VTT — P106

**Baseline:** P105 canônica persistida no Google Drive. P106 preserva integralmente P105 e liga talentos ativos EotE Core ao fluxo real de teste/turno/recuperação sem RAG.  
**Critério:** `UI R46 → bridge → controller/rules → estado → persistência/efeito → teste`, sem RAG obrigatório.

## Resultado P106

- **55** famílias canônicas.
- **31 JOGÁVEIS** (**56,4%**).
- **3 ENGINE REAL** sem fluxo completo de UI.
- **3 PARCIAIS**.
- **18 RAG/DECLARAÇÃO**.
- **34/55 (61,8%)** têm engine determinístico real sem RAG.
- Regressão final: **1170/1170 não-stress + 6/6 stress = 1176/1176 PASS, 0 FAIL**.
- Focal P106: **7/7 PASS**; regressões P105 e anteriores permanecem dentro da suíte global.
- Aventuras preservadas: **Sob um Sol Negro 106/106** e **Além da Orla 57/57**.
- Android/Gradle: **NÃO EXECUTADO**.

### Avanços P104 — manobras pessoais EotE Core

- custo de manobra unificado para Mirar, Cobertura, Postura Defensiva, Ficar/Levantar-se de prone, sacar/trocar, Preparação, recarga, Stimpack e movimento;
- carga severa e Ferimentos Críticos que removem a manobra gratuita agora afetam todos esses caminhos igualmente;
- Ficar Caído / prone exposto pela R46 e persistido no mesmo estado usado por Derrubar;
- Mirar corrige cancelamento por outra manobra/ação e por dano que ultrapassa soak;
- Mirar em item/parte específica aplica 2 Setbacks com uma mira e 1 com duas miras consecutivas;
- Médio↔Longo e Longo↔Extremo passam a exigir duas manobras e só alteram a faixa após a segunda, com progresso persistente.

### Correção conservadora de auditoria

`ENV.HAZARDS` foi rebaixada de ENGINE REAL para RAG/DECLARAÇÃO: a evidência anterior citava `environmental-hazards.test.mjs`, mas esse arquivo/módulo não existe no checkpoint canônico. `pilotingHazardDifficulty()` cobre perigo de Pilotagem veicular e não comprova, por si, queda/fogo/sufocamento/radiação gerais. A queda de 35 para 34 famílias determinísticas é correção de evidência, não remoção de mecânica.

### Avanços P102–P103 — ações veiculares EotE Core

- Ganhar a Vantagem.
- Copiloto.
- Traçar Curso.
- Reforçar Escudos.
- Disciplina de Fogo.
- Escanear Inimigo.
- Fatiar Sistemas.
- Despistar Mísseis.
- **P103:** Controle de Danos separado entre Tensão de Sistemas e Trauma de Casco.
- **P103:** Reparo Manual por Atletismo, Kit de ferramentas e permissão contextual, compartilhando o limite de Casco por encontro.
- **P103:** gastos opcionais de Escanear/Fatiar e escalas corretas de Disciplina de Fogo/Despistar Mísseis.

`VEHICLE.ACTIONS` continua ENGINE REAL / UI PARCIAL somente porque **Jamming** ainda não possui runtime genérico real de comunicações. P103 fechou Reparo Manual, limite compartilhado de Trauma de Casco, gastos opcionais de Escanear/Fatiar e correções de Disciplina de Fogo/Despistar Mísseis.

### Avanços P106 — ações da Força EotE Core

- **Sentir / defesa contínua:** exige poder + aprimoramento comprados; ativação consome a ação, compromete 1 dado da Força e aprimora de verdade a dificuldade dos ataques recebidos, respeitando Strength/Duration já adquiridos.
- **Sentir / ofensiva contínua:** exige toda a cadeia comprada; compromete 1 dado e aprimora de verdade o pool do ataque do jogador quando o gatilho válido ocorre.
- **Influência básica:** exige poder comprado, alvo persistente do encontro e alcance válido; rola os dados da Força, consome a ação mesmo na falha e aplica Tensão real (ou conversão em Ferimentos para adversários sem strain) apenas se os pontos cobrirem o custo.
- A R46 não usa resultados sombrios automaticamente. O jogador não recebe conversão automática nem efeito grátis.
- **Mover** não foi falsamente exposto: o runtime ainda não possui um objeto de cena persistente seguro para mover/manipular/arremessar.


### Avanços P106 — talentos ativos EotE Core

- **Foco Intenso:** manobra real, 1 Tensão e 1 upgrade de habilidade no próximo teste.
- **Sentir o Perigo:** remove até 2 Setbacks de um teste, limitado e persistido em 1 uso por sessão.
- **Toque do Destino:** adiciona 2 Boost a um teste, limitado e persistido em 1 uso por sessão.
- **Levantar Rápido:** levantar de prone como incidental uma vez por turno, sem consumir manobra.
- **Equilíbrio:** no fim do encontro, rola Force Rating e recupera Tensão adicional igual aos resultados luminosos.
- **Bridge/R46:** teste simples agora é diretamente executável com esses talentos; aquisição real continua obrigatória.

## Matriz — 55 famílias canônicas

| ID | Domínio | Estado | Engine s/RAG | UI R46 | Persistência | Teste | Evidência/lacuna | Próximo passo |
|---|---|---|---|---|---|---|---|---|
| CORE.DICE.POOL | Core | JOGÁVEL | SIM | SIM | N/A (cálculo) | SIM | narrativeDice.mjs; r54-device-p6.js; rules.test.mjs | Manter |
| CORE.DICE.SYMBOLS | Core | JOGÁVEL | SIM | SIM | N/A (cálculo) | SIM | resolveSymbols()/rollPool(); 901/901 regressão | Manter |
| CORE.CHECKS.TYPES | Core | JOGÁVEL | SIM | SIM | SIM | SIM | P95 + P106: teste simples agora exposto diretamente no bridge/R46, além de assistência hábil/não-hábil, oposto, competitivo e social; pools e oposição continuam no engine determinístico. | Manter; ampliar modificadores contextuais de cena sem alterar a regra-base |
| CORE.DESTINY | Core | JOGÁVEL | SIM | SIM | SIM | SIM | P94: destinySummary + spendDestiny(light) na R46; gasto converte luminoso em sombrio e persiste; dark reservado ao Mestre/engine | Manter; depois automatizar política de gasto do Mestre sem botão do jogador |
| CHAR.CHARACTERISTICS | Personagem | JOGÁVEL | SIM | SIM | SIM | SIM | criação manual EotE/AoR/MIXED na shell R46 | Manter |
| CHAR.ATTRIBUTES | Personagem | JOGÁVEL | SIM | SIM (leitura/efeitos) | SIM | SIM | health.mjs/character.mjs; criação e efeitos de sessão | Manter; depois expor saúde ativa |
| CHAR.SPECIES | Personagem | JOGÁVEL | SIM | SIM | SIM | SIM | 8 espécies EotE + 8 AoR; character-creation tests | Manter |
| CHAR.CAREERS.EOTE | Personagem | JOGÁVEL | SIM | SIM | SIM | SIM | 6 carreiras EotE + specs core | Manter |
| CHAR.OBLIGATION | Campanha EotE | JOGÁVEL | SIM | SIM na criação; automático na sessão | SIM | SIM | obligation.mjs; startSession(); obligation.test.mjs | Adicionar painel de ativação/penalidade na UI |
| SKILLS.GENERAL | Perícias | JOGÁVEL | SIM | SIM via teste sugerido/Mestre | SIM | SIM | resolveSkillCheck(); suggested checks | Manter |
| SKILLS.COMBAT | Perícias | JOGÁVEL | SIM | SIM | SIM | SIM | resolveWeaponAttack + painel R46 COMBATE PESSOAL; P92 runtime/final-shell tests | Manter e ampliar manobras/ações |
| SKILLS.KNOWLEDGE | Perícias | JOGÁVEL | SIM | SIM via teste sugerido/Mestre | SIM | SIM | resolveSkillCheck() | Manter |
| TALENTS.TREES | Talentos | JOGÁVEL | SIM | SIM | SIM | SIM | talentProgressionStatus + buyTalent na R46; P106 executa Foco Intenso, Sentir o Perigo, Toque do Destino, Levantar Rápido e Equilíbrio com custo/limite/persistência reais. | Ampliar talentos ativos source-bound restantes sem automatizar decisões narrativas. |
| EQUIP.WEAPONS | Equipamento | JOGÁVEL | SIM | SIM | SIM | SIM | equipmentOwnedSummary/equipWeapon + painel EQUIPAMENTO ATIVO; ataque usa arma equipada | Manter |
| EQUIP.QUALITIES | Equipamento | PARCIAL | PARCIAL | PARCIAL | SIM parcial | SIM | P92–P104: qualidades pessoais/veiculares já implementadas incluem Derrubar/Prone completo, Iônica veicular, Guiada, Raio Trator, Vinculada, Munição Limitada, Cadência Lenta e efeitos source-bound de attachments. A família permanece parcial porque a cobertura integral dos perfis oficiais/derivados ainda não foi comprovada. | Auditar perfis/qualidades restantes source-bound; só então promover a JOGÁVEL |
| EQUIP.ARMOR | Equipamento | JOGÁVEL | SIM | SIM | SIM | SIM | equipArmor/unequipArmor + painel R46; normaliza armadura legada em gear | Manter |
| EQUIP.ATTACHMENTS | Equipamento | PARCIAL | PARCIAL | SIM parcial | SIM parcial | SIM | P99–P100 fecha ciclo base e Mods source-bound para 5 perfis; cobertura total de attachments ainda incompleta. | Ampliar somente perfis source-bound completos. |
| COMBAT.STRUCTURE | Combate | JOGÁVEL | SIM | SIM | SIM | SIM | painel R46 lê encontro real, turno, alvo e encerra turno/combate sem criar inimigos grátis | Expandir iniciativa/slot UX |
| COMBAT.MANEUVERS | Combate | ENGINE REAL | SIM | PARCIAL | SIM | SIM | P94/P96/P104/P106: Aim completo, movimento por faixas, cobertura, sacar/trocar, Preparação, prone/levantar e Levantar Rápido incidental; orçamento único de manobras/carga/críticos. Ainda há manobras contextuais sem estado de cena dedicado. | Assistir / Interagir / Montar-Desmontar somente quando houver alvo/estado contextual real. |
| COMBAT.ACTIONS | Combate | ENGINE REAL | SIM | PARCIAL | SIM | SIM | P92–P106: ataque, crítico, Medicine/utility, ações da Força Sentir/Influência e teste de perícia com talentos ativos source-bound. Ainda não cobre todas as ações/talentos oficiais. | Fechar próximos talentos/ações com fonte e efeito determinístico. |
| COMBAT.DAMAGE | Combate | JOGÁVEL | SIM | SIM | SIM | SIM | ataque pessoal aplica dano real ao adversário persistente; Breach/Pierce entram no soak | Manter |
| COMBAT.CRITICAL | Combate | JOGÁVEL | SIM | SIM | SIM | SIM | crítico pendente/ativação pela UI; Vicious entra no d100 real | Manter |
| COMBAT.CONDITIONS | Combate | JOGÁVEL | SIM | SIM | SIM | SIM | P94 exibe condições; disoriented aplica Setback e immobilized bloqueia movimento/manobras | Manter e melhorar nomes/descrições PT-BR |
| COMBAT.HEALING | Combate | JOGÁVEL | SIM | SIM | SIM | SIM | Stimpack, Medicine e descanso completo expostos na R46 usando recovery/medicine engine; não limpam estado diretamente | Manter e ampliar feedback de resultados |
| VEHICLE.STATS | Veículos | JOGÁVEL | SIM | SIM | SIM | SIM | painel R46 mostra nave/veículo ativo e estado real | Manter; não conceder veículo por bridge |
| VEHICLE.MANEUVERS | Veículos | JOGÁVEL | SIM | SIM | SIM | SIM | R46 expõe manobras existentes; P98 preserva bloqueio de movimento por Raio Trator e oferece apenas teste legítimo de fuga por Pilotagem | Adicionar manobras restantes apenas com fonte |
| VEHICLE.ACTIONS | Veículos | ENGINE REAL | SIM | PARCIAL | SIM | SIM | P103 corrige Controle de Danos para reparar Tensão ou Casco separadamente, aplica dificuldade pelo tipo, limita a tentativa de Casco por encontro e compartilha esse limite com Reparo Manual. Também corrige Disciplina de Fogo/Despistar Mísseis e abre gastos opcionais de Escanear/Fatiar. As ações P102 seguem íntegras. Falta apenas Jamming com runtime genérico real de comunicações para cobertura integral da família. | Implementar Jamming apenas quando houver runtime genérico real de comunicações; então reavaliar promoção. |
| VEHICLE.COMBAT | Veículos | JOGÁVEL | SIM | SIM | SIM | SIM | P98: ataque veicular usa ledger único de Vantagens/Triunfos para Critical/Guiada/Vinculada; Iônica afeta System Strain; munição/cadência e Guided/Tractor persistem pela R46 | Manter; ampliar ações restantes e feedback visual |
| VEHICLE.CRITICAL | Veículos | JOGÁVEL | SIM | SIM | SIM | SIM | P96 fechou geração/reparo real; P98 integra o Critical Hit ao mesmo orçamento de Vantagens/Triunfos usado por Guiada/Vinculada, impedindo gasto duplicado | Manter; ampliar feedback visual e ataques NPC veiculares quando o fluxo NPC estiver ativo |
| TRAVEL.HYPERSPACE | Viagem | JOGÁVEL | SIM | SIM | SIM | SIM | P93 corrige leitura dos símbolos narrativos; salto bem-sucedido avança Mundo Vivo em ticks de 6h e muda local/cena somente se não sair da rota; falha não move nem avança tempo | Manter; ampliar destinos/feedback visual |
| ENV.HAZARDS | Ambiente | RAG/DECLARAÇÃO | NÃO geral | NÃO | NÃO geral | NÃO geral | Correção P102: a evidência antiga citava environmental-hazards.test.mjs, mas esse arquivo/módulo não existe no checkpoint P100/P101. O engine possui perigo de Pilotagem veicular, o que não comprova queda, sufocamento, fogo ou radiação gerais. | Implementar perigos ambientais gerais somente com fonte primária, regra determinística, estado/persistência e testes. |
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

- **FEAR.CHECKS — JOGÁVEL:** preservado do P101; painel R46, engine real, persistência por sourceId e consequências.
- **ENCUMBRANCE.CORE — JOGÁVEL:** preservado do P101; carga, limite, excesso, penalidades e levantamento com ajuda chegam à R46.
- **GEAR.ACTIVE_EFFECTS — JOGÁVEL parcial auditado:** preservado do P101; kit de ferramentas, óptica e remendo de reparo exigem propriedade real e usam efeito determinístico.
- **FORCE.ACTIONS — ENGINE REAL / UI PARCIAL:** P106 torna Sentir defensivo/ofensivo e Influência básica executáveis pela R46; Mover continua sem UI mecânica por falta de objeto de cena persistente, e sublight permanece engine sem UI completa.

## Próxima onda recomendada (P107)

1. Continuar talentos/ações pessoais source-bound com efeito determinístico real.
2. Implementar manobras contextuais apenas quando houver estado de cena/veículo/aliado que as sustente.
3. Não expor Move da Força até existir objeto persistente de cena.
4. Não reabrir ENV.HAZARDS por declaração.
5. Preservar integralmente os gates das duas aventuras e continuar sem Android/Gradle.
