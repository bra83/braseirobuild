# MATRIZ CANÔNICA DE COBERTURA MECÂNICA REAL — STAR WARS VTT — P100

**Baseline:** P99 persistida no Google Drive, promovida a P100 após fechar Mods source-bound e Postura Defensiva com persistência/testes reais.  
**Critério:** `UI R46 → bridge → controller/rules → estado → persistência/efeito → teste`, sem RAG obrigatório.

## Resultado P100
- **55** famílias canônicas.
- **31 JOGÁVEIS** (**56,4%**; inalterado por critério conservador).
- **4 ENGINE REAL** sem fluxo completo de UI.
- **3 PARCIAIS**.
- **17 RAG/DECLARAÇÃO**.
- **35/55 (63,6%)** têm engine determinístico completo sem RAG; P100 aprofundou attachments e manobras sem promover famílias ainda incompletas.
- Regressão final: **1118/1118 não-stress + 6/6 stress = 1124/1124 PASS, 0 FAIL**.
- Aventuras preservadas: **Sob um Sol Negro 106/106** e **Além da Orla 57/57**.
- Android/Gradle: **NÃO EXECUTADO**, conforme instrução de compilar somente no fechamento final.

## Avanços reais P100
- **Mods de attachments source-bound:** para os cinco perfis já autorizados em P99, cada tentativa exige propriedade + instalação + Kit de ferramentas + 100 créditos + teste de Mecânica; a primeira dificuldade é 3 e cada Mod já instalado aumenta a dificuldade em +1.
- **Falha e slots:** falha não concede efeito e consome aquela opção de Mod; opções repetidas mantêm seus slots independentes.
- **Persistência correta:** Mods/falhas agora pertencem ao attachment adquirido, não à cópia instalada na arma; remover, salvar/reabrir e reinstalar preservam o estado e os efeitos.
- **Efeito real no ataque:** duas reduções de Imprecisa do Rapid-Recharge reduzem em 2 os Setbacks do mesmo ataque real em comparação ao attachment não modificado.
- **Postura Defensiva:** consome uma manobra real; adiciona Setback aos testes de combate do personagem e a ataques corpo a corpo contra ele; não afeta ataques ranged contra o personagem; não acumula; é limpa ao fim do combate.
- **Signature Abilities:** nenhuma promoção artificial; a cobertura continua incompleta no pacote antigo.
- **Contrato de versão:** teste legado P77 foi corrigido para aceitar numericamente versões `1.0.x` com `x >= 77`, incluindo `1.0.100`.

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
| EQUIP.QUALITIES | Equipamento | PARCIAL | PARCIAL | PARCIAL | SIM parcial | SIM | P92–P99: qualidades anteriores + Derrubar completo com custo por Silhueta, estado prone, modificadores melee/ranged e levantar por manobra; attachments source-bound já propagam Automática/Imprecisa/Explosão/Guiada ao perfil efetivo. A família permanece parcial porque a cobertura oficial/derivada ainda não é integral. | Completar perfis/qualidades restantes e só então promover a JOGÁVEL |
| EQUIP.ARMOR | Equipamento | JOGÁVEL | SIM | SIM | SIM | SIM | equipArmor/unequipArmor + painel R46; normaliza armadura legada em gear | Manter |
| EQUIP.ATTACHMENTS | Equipamento | PARCIAL | PARCIAL | SIM parcial | SIM source-bound | SIM | P99 fecha o ciclo base de 5 perfis source-bound; P100 adiciona Mods reais para esses mesmos perfis: Kit de ferramentas + 100 créditos por tentativa + teste de Mecânica Difícil inicial, dificuldade +1 por Mod já instalado, falha consome a opção, efeitos persistem no attachment e sobrevivem remover/reabrir/reinstalar. Rapid-Recharge modificado altera o pool de ataque real. A família segue parcial porque o catálogo executável ainda cobre só um subconjunto autorizado. | Ampliar catálogo e Mods somente quando cada perfil tiver regra source-bound completa; depois promover |
| COMBAT.STRUCTURE | Combate | JOGÁVEL | SIM | SIM | SIM | SIM | painel R46 lê encontro real, turno, alvo e encerra turno/combate sem criar inimigos grátis | Expandir iniciativa/slot UX |
| COMBAT.MANEUVERS | Combate | ENGINE REAL | SIM | PARCIAL | SIM | SIM | P94–P99 expõem Aim, movimento, cobertura, sacar/trocar arma, Preparação e Levantar-se; P100 adiciona Postura Defensiva como manobra real, com Setback nos próprios testes de combate e nos ataques melee/Brawl contra o personagem até o fim do próximo turno. Ainda falta cobertura integral de manobras contextuais. | Completar manobras restantes sustentadas por fonte |
| COMBAT.ACTIONS | Combate | ENGINE REAL | SIM | PARCIAL | SIM | SIM | P92 expõe ataque/critical/end turn e usa engine real, mas não todas as ações pessoais/Medicine/utility. | Expor ações restantes por contexto |
| COMBAT.DAMAGE | Combate | JOGÁVEL | SIM | SIM | SIM | SIM | ataque pessoal aplica dano real ao adversário persistente; Breach/Pierce entram no soak | Manter |
| COMBAT.CRITICAL | Combate | JOGÁVEL | SIM | SIM | SIM | SIM | crítico pendente/ativação pela UI; Vicious entra no d100 real | Manter |
| COMBAT.CONDITIONS | Combate | JOGÁVEL | SIM | SIM | SIM | SIM | P94 exibe condições; disoriented aplica Setback e immobilized bloqueia movimento/manobras | Manter e melhorar nomes/descrições PT-BR |
| COMBAT.HEALING | Combate | JOGÁVEL | SIM | SIM | SIM | SIM | Stimpack, Medicine e descanso completo expostos na R46 usando recovery/medicine engine; não limpam estado diretamente | Manter e ampliar feedback de resultados |
| VEHICLE.STATS | Veículos | JOGÁVEL | SIM | SIM | SIM | SIM | painel R46 mostra nave/veículo ativo e estado real | Manter; não conceder veículo por bridge |
| VEHICLE.MANEUVERS | Veículos | JOGÁVEL | SIM | SIM | SIM | SIM | R46 expõe manobras existentes; P98 preserva bloqueio de movimento por Raio Trator e oferece apenas teste legítimo de fuga por Pilotagem | Adicionar manobras restantes apenas com fonte |
| VEHICLE.ACTIONS | Veículos | ENGINE REAL | SIM | PARCIAL | SIM | SIM | P92 expõe ataque e ações/pilotagem principais; P95 adiciona Controle de Danos real por Mecânica e consumo de ação. Ainda faltam ações oficiais como co-pilotagem, scan/slice/jamming/boost shields conforme fonte. | Implementar as ações veiculares restantes source-bound antes de promover a JOGÁVEL |
| VEHICLE.COMBAT | Veículos | JOGÁVEL | SIM | SIM | SIM | SIM | P98: ataque veicular usa ledger único de Vantagens/Triunfos para Critical/Guiada/Vinculada; Iônica afeta System Strain; munição/cadência e Guided/Tractor persistem pela R46 | Manter; ampliar ações restantes e feedback visual |
| VEHICLE.CRITICAL | Veículos | JOGÁVEL | SIM | SIM | SIM | SIM | P96 fechou geração/reparo real; P98 integra o Critical Hit ao mesmo orçamento de Vantagens/Triunfos usado por Guiada/Vinculada, impedindo gasto duplicado | Manter; ampliar feedback visual e ataques NPC veiculares quando o fluxo NPC estiver ativo |
| TRAVEL.HYPERSPACE | Viagem | JOGÁVEL | SIM | SIM | SIM | SIM | P93 corrige leitura dos símbolos narrativos; salto bem-sucedido avança Mundo Vivo em ticks de 6h e muda local/cena somente se não sair da rota; falha não move nem avança tempo | Manter; ampliar destinos/feedback visual |
| ENV.HAZARDS | Ambiente | ENGINE REAL | SIM | NÃO | SIM | SIM | queda, sufocamento, fogo, radiação; environmental-hazards.test.mjs | Integrar a cenas/Mestre/UI |
| SOCIAL.ENCOUNTERS | Social | JOGÁVEL | SIM | SIM | SIM | SIM | P95: fluxo social dedicado na R46; Coerção/Engano/Liderança vs Disciplina, Carisma vs Frialdade, Negociação vs Negociação ou Frialdade; social.lastInfluence/history persistem; combinações inválidas são rejeitadas | Integrar consequências sociais específicas de aventuras/NPCs sem automatizar interpretação narrativa |
| AOR.DUTY | Campanha AoR | JOGÁVEL | SIM | SIM na criação; automático na sessão | SIM | SIM | duty.mjs; criação AoR | Adicionar painel de sessão |
| AOR.DUTY.ACTIVATION | Campanha AoR | JOGÁVEL | SIM | AUTOMÁTICA | SIM | SIM | dutyCheck/applyDutyBonus em startSession() | Mostrar gatilho/bônus na UI |
| AOR.CONTRIBUTION | Campanha AoR | JOGÁVEL | SIM | SIM | SIM | SIM | painel PROGRESSÃO mostra Duty/Contribution e resolve marco somente quando elegível via resolveCampaignMilestone | Manter sem expor mutação bruta de Duty |
| AOR.CAREERS | Personagem AoR | JOGÁVEL | SIM | SIM | SIM | SIM | 6 carreiras AoR + specs core na criação | Manter |
| AOR.BASE.REQUISITION | Campanha AoR | RAG/DECLARAÇÃO | NÃO dedicado | NÃO | NÃO dedicado | NÃO | Auditoria P94 não encontrou motor de requisição nem requisition/maxRarity/navalSupport; Contribution não equivale a requisição | Implementar somente com fonte autorizada |
| EXP.EOTE.HIREDGUN | Expansão | RAG/DECLARAÇÃO | NÃO | NÃO | NÃO | NÃO | Demolitionist/Heavy/Enforcer não estão nas árvores de especialização | Implementar ou retirar alegação |
| EXP.EOTE.SIGNATURE | Expansão | PARCIAL | PARCIAL | NÃO | SIM compra | PARCIAL | P100 manteve a auditoria P99: pacote antigo tem 7 abilities para apenas 4 carreiras EotE; Colonist/Explorer e AoR faltam e não há runtime integral de ativação. Nada foi promovido ou importado por catálogo. | Completar catálogo source-bound + ativação real antes de expor na UI |
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

## Próxima onda recomendada (P100)
1. Estruturar **Mods de attachments** somente com dificuldade/custo/efeitos source-bound completos; manter bloqueados os demais.
2. Fechar **Signature Abilities** por carreira e runtime real de ativação, começando apenas pelas abilities com fonte/efeito inequívocos.
3. Completar **manobras/ações pessoais** e efeitos ambientais ainda com engine sem UI.
4. Auditar **qualidades pessoais/veiculares restantes** contra os perfis do catálogo; promover `EQUIP.QUALITIES` somente quando cobertura integral estiver comprovada.
5. Continuar sem Android/Gradle até o fechamento final.
