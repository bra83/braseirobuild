# MATRIZ CANÔNICA DE COBERTURA MECÂNICA REAL — STAR WARS VTT — P107

**Baseline:** P106 canônica persistida no Google Drive. P107 preserva integralmente P106 e amplia talentos pessoais de combate do EotE Core no fluxo real de combate, sem RAG.  
**Fonte primária desta onda:** `EotE Core Rulebook.pdf`, Google Drive file ID `1UO3Z35J2Dmkfvx1OLyPD3rzJon013EoX`.  
**Critério:** `UI R46 → bridge → controller/rules → estado → persistência/efeito → teste`, sem RAG obrigatório.

## Resultado P107

- **55** famílias canônicas.
- **31 JOGÁVEIS** (**56,4%**).
- **3 ENGINE REAL** sem fluxo completo de UI.
- **3 PARCIAIS**.
- **18 RAG/DECLARAÇÃO**.
- **34/55 (61,8%)** têm engine determinístico real sem RAG.
- As contagens de famílias foram mantidas deliberadamente: P107 aprofunda `TALENTS.TREES`/combate, não cria uma família nova nem promove cobertura sem evidência.
- Regressão final: **1179/1179 não-stress + 6/6 stress = 1185/1185 PASS, 0 FAIL**.
- Focal P107: **9/9 PASS**.
- Aventuras preservadas: **Sob um Sol Negro 106/106** e **Além da Orla 57/57**.
- Bundle offline: **64 módulos**; builders raiz/tools byte-idênticos.
- Sintaxe/verificação de fonte: **PASS**.
- Android/Gradle/APK: **NÃO EXECUTADO**, conforme regra de retomada do P106.

## Avanços P107 — talentos de combate EotE Core

- **Ataque Frenético (Frenzied Attack):** incidental; sofre Tensão até o rank e aprimora a habilidade do próximo ataque de Briga/Corpo a Corpo pelo mesmo valor; consumo real no próximo ataque.
- **Força Feroz (Feral Strength):** +1 dano por rank em um acerto bem-sucedido de Briga/Corpo a Corpo.
- **Rajada (Barrage):** +1 dano por rank em um acerto de Armas à Distância (Pesadas)/Artilharia pessoal, não veicular, a Longo/Extremo.
- **Passo Lateral (Side Step):** manobra 1×/rodada; sofre Tensão até o rank; ataques à distância contra o personagem recebem upgrades até o início do próximo turno do jogador.
- **Postura Defensiva (Defensive Stance):** manobra 1×/rodada; sofre Tensão até o rank; ataques corpo a corpo contra o personagem recebem upgrades até o início do próximo turno do jogador.
- **Segundo Fôlego (Second Wind):** incidental 1×/encontro; recupera Tensão igual ao rank.
- **Saque Rápido (Quick Draw):** no fluxo atual de armas, sacar uma arma acessível como incidental 1×/rodada, sem consumir manobra.
- **Golpes Letais (Lethal Blows):** +10 por rank aplicado à rolagem real de Ferimento Crítico infligido.

### Conservadorismo P107

- Nenhum talento é concedido sem compra real na árvore.
- Não foi implementado **Dodge** ainda: é uma reação fora do turno e exige uma decisão do jogador antes de resolver o ataque NPC, não uma automação silenciosa.
- Não foi implementado **Precise Aim** ainda: requer redução segura da defesa corpo a corpo/à distância do alvo persistente.
- Não foram implementados **Targeted Blow / Anatomy Lessons** ainda: ambos gastam Destiny após um ataque bem-sucedido e precisam respeitar a regra de que cada jogador investe no máximo 1 Destiny Point em um único check.
- **Deadly Accuracy** ainda requer persistência explícita da perícia escolhida ao adquirir o talento.
- `ENV.HAZARDS` continua sem promoção artificial; Force Move continua sem UI mecânica enquanto não houver objeto persistente de cena adequado.

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
| SKILLS.COMBAT | Perícias | JOGÁVEL | SIM | SIM | SIM | SIM | resolveWeaponAttack + painel R46 COMBATE PESSOAL; P107 integra Ataque Frenético ao pool e Força Feroz/Rajada ao dano real, além dos fluxos anteriores. | Manter e ampliar manobras/ações |
| SKILLS.KNOWLEDGE | Perícias | JOGÁVEL | SIM | SIM via teste sugerido/Mestre | SIM | SIM | resolveSkillCheck() | Manter |
| TALENTS.TREES | Talentos | JOGÁVEL | SIM | SIM | SIM | SIM | talentProgressionStatus + buyTalent na R46; P106 já executa Foco Intenso, Sentir o Perigo, Toque do Destino, Levantar Rápido e Equilíbrio. P107 acrescenta Ataque Frenético, Força Feroz, Rajada, Passo Lateral, Postura Defensiva, Segundo Fôlego, Saque Rápido (arma no fluxo atual) e Golpes Letais com custo, limite, duração e efeito reais. | Fechar talentos source-bound restantes sem automatizar decisão narrativa: Dodge, Precise Aim, Sniper Shot, Targeted Blow/Anatomy Lessons respeitando a regra de 1 Destiny por check e Deadly Accuracy com escolha persistida. |
| EQUIP.WEAPONS | Equipamento | JOGÁVEL | SIM | SIM | SIM | SIM | equipmentOwnedSummary/equipWeapon + painel EQUIPAMENTO ATIVO; ataque usa arma equipada | Manter |
| EQUIP.QUALITIES | Equipamento | PARCIAL | PARCIAL | PARCIAL | SIM parcial | SIM | P92–P104: qualidades pessoais/veiculares já implementadas incluem Derrubar/Prone completo, Iônica veicular, Guiada, Raio Trator, Vinculada, Munição Limitada, Cadência Lenta e efeitos source-bound de attachments. A família permanece parcial porque a cobertura integral dos perfis oficiais/derivados ainda não foi comprovada. | Auditar perfis/qualidades restantes source-bound; só então promover a JOGÁVEL |
| EQUIP.ARMOR | Equipamento | JOGÁVEL | SIM | SIM | SIM | SIM | equipArmor/unequipArmor + painel R46; normaliza armadura legada em gear | Manter |
| EQUIP.ATTACHMENTS | Equipamento | PARCIAL | PARCIAL | SIM parcial | SIM parcial | SIM | P99–P100 fecha ciclo base e Mods source-bound para 5 perfis; cobertura total de attachments ainda incompleta. | Ampliar somente perfis source-bound completos. |
| COMBAT.STRUCTURE | Combate | JOGÁVEL | SIM | SIM | SIM | SIM | painel R46 lê encontro real, turno, alvo e encerra turno/combate sem criar inimigos grátis | Expandir iniciativa/slot UX |
| COMBAT.MANEUVERS | Combate | ENGINE REAL | SIM | PARCIAL | SIM | SIM | P94/P96/P104/P106/P107: Aim, movimento por faixas, cobertura, sacar/trocar, Preparação, prone/levantar, Levantar Rápido, Passo Lateral e Postura Defensiva; Saque Rápido usa incidental para arma. Orçamento único de manobras/carga/críticos permanece preservado. Ainda há manobras contextuais sem estado de cena dedicado. | Assistir / Interagir / Montar-Desmontar somente quando houver alvo/estado contextual real; Precise Aim quando a defesa do alvo puder ser reduzida de forma segura. |
| COMBAT.ACTIONS | Combate | ENGINE REAL | SIM | PARCIAL | SIM | SIM | P92–P107: ataque, crítico, Medicine/utility, ações da Força Sentir/Influência, teste de perícia com talentos e, no P107, Ataque Frenético/Segundo Fôlego integrados ao encontro. Ainda não cobre todas as ações/talentos oficiais. | Fechar próximos talentos/ações source-bound com custo e timing determinísticos, preservando escolhas do jogador e a regra de Destiny. |
| COMBAT.DAMAGE | Combate | JOGÁVEL | SIM | SIM | SIM | SIM | ataque pessoal aplica dano real ao adversário persistente; Breach/Pierce entram no soak | Manter |
| COMBAT.CRITICAL | Combate | JOGÁVEL | SIM | SIM | SIM | SIM | Crítico pendente/ativação pela UI; Vicious entra no d100 real e P107 soma +10 por rank de Golpes Letais apenas quando realmente adquirido. | Manter; depois integrar Targeted Blow/Anatomy Lessons sem permitir gasto duplicado de Destiny no mesmo check. |
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
- **FORCE.ACTIONS — ENGINE REAL / UI PARCIAL:** P106 torna Sentir defensivo/ofensivo e Influência básica executáveis pela R46; Mover continua sem UI mecânica por falta de objeto de cena persistente.

## Próxima onda recomendada (P108)

1. Completar talentos de combate/Assassin source-bound que ainda exigem timing/estado: Dodge, Precise Aim e Sniper Shot.
2. Implementar Targeted Blow/Anatomy Lessons somente com ledger de Destiny por check, impedindo gasto duplo.
3. Persistir a escolha de perícia de Deadly Accuracy na aquisição antes de aplicar dano.
4. Não automatizar reações ou decisões de gasto que pertencem ao jogador.
5. Preservar integralmente os gates das duas aventuras e continuar sem Android/Gradle até fechamento final explícito.
