# MATRIZ CANÔNICA DE COBERTURA MECÂNICA REAL — STAR WARS VTT — P101

**Baseline:** P99 persistida no Google Drive, promovida a P101 após fechar Mods source-bound de attachments e a reachability de Fear na R46.  
**Critério:** `UI R46 → bridge → controller/rules → estado → persistência/efeito → teste`, sem RAG obrigatório.

## Resultado P101
- **55** famílias canônicas.
- **31 JOGÁVEIS** (**56,4%**; inalterado por critério conservador).
- **4 ENGINE REAL** sem fluxo completo de UI.
- **3 PARCIAIS**.
- **17 RAG/DECLARAÇÃO**.
- **35/55 (63,6%)** têm engine determinístico completo sem RAG; P101 aprofundou attachments e tornou Fear jogável sem promover famílias canônicas ainda incompletas.
- Regressão final: **1126/1126 não-stress + 6/6 stress = 1132/1132 PASS, 0 FAIL**.
- Aventuras preservadas: **Sob um Sol Negro 106/106** e **Além da Orla 57/57**.
- Android/Gradle: **NÃO EXECUTADO**, conforme instrução de compilar somente no fechamento final.

## Avanços reais P101
- **Encumbrance/levantamento:** carga real ficou acessível na R46; `encumbranceStatus()` mostra total/limite/excesso e penalidades, e `liftingDifficulty()` calcula dificuldade com ajuda real.
- **Gear assist auditado:** Kit de ferramentas remove somente Setback declarado por falta de ferramentas; Eletrobinóculos/Macrobinóculos só removem os Setbacks auditados de alcance/iluminação e apenas quando o item é realmente possuído.
- **Remendo de reparo de emergência:** uso exclusivo de droide, consome item possuído, cura 3 Ferimentos, respeita limite diário, persiste em save/reopen e consome ação em combate.
- **Fear preservado:** continua source-bound, com efeitos persistentes e bloqueio de rerrolagem automática da mesma origem.
- **Segurança:** bridge/R46 não concedem item, não aplicam status bruto e não expõem cura direta; todos os efeitos passam pelo controller/rules.
- **Signature Abilities:** continuam PARCIAIS; nenhum catálogo incompleto foi promovido.

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
| EQUIP.ATTACHMENTS | Equipamento | PARCIAL | PARCIAL | SIM parcial | SIM parcial | SIM | P99–P101 fecham aquisição→propriedade→compatibilidade→HP→instalação→efeito→remoção e Mods estruturados para 5 perfis source-bound; Mod exige kit+bancada+créditos+Mecânica, persiste sucesso/falha e afeta ataque real. Família continua parcial porque o catálogo source-bound executável ainda não cobre todos os attachments/armaduras/veículos. | Ampliar catálogo somente com perfis mecânicos completos; manter bloqueado o que depender de adjudicação textual |
| COMBAT.STRUCTURE | Combate | JOGÁVEL | SIM | SIM | SIM | SIM | painel R46 lê encontro real, turno, alvo e encerra turno/combate sem criar inimigos grátis | Expandir iniciativa/slot UX |
| COMBAT.MANEUVERS | Combate | ENGINE REAL | SIM | PARCIAL | SIM | SIM | P94–P99 expõem Aim, movimento, cobertura, sacar/trocar arma, Preparação e Levantar-se de prone com custo real de manobra/Tensão/ação. Ainda não há cobertura integral de todas as manobras oficiais/contextuais. | Completar manobras restantes sustentadas por fonte |
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
| EXP.EOTE.SIGNATURE | Expansão | PARCIAL | PARCIAL | NÃO | SIM compra | PARCIAL | Auditoria P99 confirma 7 abilities para apenas 4 carreiras EotE no pacote antigo; Colonist/Explorer e AoR faltam e não há runtime completo de ativação dos efeitos. Nada foi importado cegamente. | Completar catálogo source-bound + ativação real antes de expor na UI |
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
- **FEAR.CHECKS — JOGÁVEL:** painel R46, engine real, persistência por sourceId e consequências.
- **ENCUMBRANCE.CORE — JOGÁVEL:** carga, limite, excesso, Setbacks e levantamento com ajuda chegam à R46.
- **GEAR.ACTIVE_EFFECTS — JOGÁVEL parcial auditado:** kit de ferramentas, óptica e remendo de reparo têm propriedade real + efeito determinístico; outros itens continuam abaixo de promoção.
- Force Sense/Influence/Move e sublight continuam engine real sem UI completa.
A contagem principal permanece limitada às 55 famílias canônicas.

## Próxima onda recomendada (P102)
1. Completar **manobras/ações pessoais e veiculares** restantes já sustentadas pelo engine.
2. Ampliar **gear effects** somente para itens com efeito determinístico e propriedade real.
3. Fechar **Signature Abilities** apenas com catálogo source-bound completo + runtime real.
4. Ampliar **attachments/Mods** apenas onde a fonte trouxer efeito determinístico.
5. Continuar sem Android/Gradle até o fechamento final.
