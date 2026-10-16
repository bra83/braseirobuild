from pathlib import Path
p=Path('app/src/main/assets/vtt/js/adventure/maskOfThePirateQueen.mjs')
s=p.read_text()
s=s.replace("  shipCorridors:'mpq.loc.renegades_blood_corridors',flagshipBridge:'mpq.loc.flagship_bridge'", "  shipCorridors:'mpq.loc.renegades_blood_corridors',flagshipBridge:'mpq.loc.flagship_bridge',\n  sororinet:'mpq.loc.sororinet_node'")
s=s.replace("  ryale:'mpq.npc.ryale_wei',noira:'mpq.npc.noira',sykes:'mpq.npc.jerid_sykes'", "  ryale:'mpq.npc.ryale_wei',noira:'mpq.npc.noira',sykes:'mpq.npc.jerid_sykes',\n  porel:'mpq.npc.porel_vakra',lathe:'mpq.npc.lathe'")
s=s.replace("  schemaVersion: 2,", "  schemaVersion: 3,",1)
s=s.replace("            {\n                  id: \"question-krezo\",\n                  label: \"Procurar o receptador Krezo\",\n                  to: \"krezo-fence\"\n            }", "            {\n                  id: \"question-krezo\",\n                  label: \"Procurar o receptador Krezo\",\n                  to: \"krezo-fence\"\n            },\n            {\n                  id: \"investigate-sororinet\",\n                  label: \"Investigar a rede SororiNet\",\n                  to: \"sororinet-entry\"\n            }")
anchor='    "graf-exile": {'
assert anchor in s
new_nodes='''    // SWE13 pp. 25-28: the third lead, SororiNet, was absent in P181.
    // A failed gateway never manufactures the Vault coordinates.
    "sororinet-entry": {
      episode: 1, title: 'Primeiro acesso à SororiNet', locationId: LOC.sororinet,
      summary: 'A rede pública de turismo esconde camadas de segurança incompatíveis com um serviço comum.',
      narration: 'Os terminais apresentam rotas de excursão, mas os pedidos de acesso revelam um gateway protegido.',
      visibleNpcIds: [], sourceSection: 'Episode I, SororiNet, pp. 25-27',
      checks: [{skill:'Computadores',difficulty:'Média',purpose:'localizar e autenticar a entrada do nó secundário'}],
      choices: [
        {id:'probe-sororinet',label:'Acessar o nó secundário',to:'sororinet-secondary',condition:'check-success:sororinet-entry:0'},
        {id:'withdraw-sororinet',label:'Desconectar e seguir outras pistas',to:'saleucami-leads'}
      ]
    },
    "sororinet-secondary": {
      episode: 1, title: 'Segurança da SororiNet', locationId: LOC.sororinet,
      summary: 'O segundo nó protege uma passagem para dados internos da Irmandade.',
      narration: 'As permissões ficam mais restritas; a equipe pode tentar ultrapassar o gateway ou recuar.',
      visibleNpcIds: [], sourceSection: 'Episode I, SororiNet, pp. 26-27',
      checks: [{skill:'Computadores',difficulty:'Difícil',purpose:'superar a segurança do gateway interno'}],
      choices: [
        {id:'breach-final-gateway',label:'Avançar para o nó restrito',to:'sororinet-final',condition:'check-success:sororinet-secondary:0'},
        {id:'disconnect-secondary',label:'Interromper a intrusão',to:'saleucami-leads'}
      ]
    },
    "sororinet-final": {
      episode: 1, title: 'Dados protegidos e rastreamento', locationId: LOC.sororinet,
      summary: 'Os arquivos restritos contêm as coordenadas do Vault; Lathe pode rastrear intrusões malsucedidas.',
      narration: 'A camada final guarda os dados que ligam os ataques à cratera Blackwind.',
      visibleNpcIds: [], sourceSection: 'Episode I, SororiNet / Alerting Lathe, pp. 27-28',
      checks: [{skill:'Computadores',difficulty:'Média',purpose:'extrair as coordenadas da base antes que o acesso seja encerrado'}],
      choices: [
        {id:'extract-blackwind',label:'Guardar as coordenadas e seguir para Blackwind',to:'scrap-zone-investigation',condition:'check-success:sororinet-final:0',effects:{sororinetCoordinates:true}},
        {id:'disconnect-final',label:'Encerrar a conexão e seguir as outras pistas',to:'saleucami-leads'}
      ]
    },
'''
s=s.replace(anchor,new_nodes+anchor,1)
s=s.replace('''                  to: "mpq-epilogue",
                  effects: {
                        queenCaptured: true,
                        resolutionMode: "capture"
                  }''','''                  to: "paradise-final-handover",
                  condition: "encounter-resolved:pirate-queen-duel",
                  effects: {
                        queenCaptured: true,
                        resolutionMode: "capture"
                  }''',1)
s=s.replace('''    "mpq-epilogue": {''','''    // SWE13 pp. 93-95: Porel exposes Venlana as the Sorority mole.
    // The final Consortium payment must not be issued before this choice.
    "paradise-final-handover": {
      episode: 3, title: 'A disputa pela máscara na Paradise', locationId: LOC.paradise,
      summary: 'Porel Vakra e Venlana Sipal acusam um ao outro de traição; a escolha altera a recompensa e a perseguição futura.',
      narration: 'O retorno à Paradise revela duas versões incompatíveis sobre a lealdade de Venlana. A tripulação decide a quem entregar a máscara.',
      visibleNpcIds: [NPC.porel,NPC.venlana], sourceSection: 'Episode III, Showdown at the Paradise, pp. 93-95',
      checks: [{skill:'Disciplina',difficulty:'Difícil',purpose:'perceber a falsidade da acusação feita por Venlana'}],
      choices: [
        {id:'side-porel',label:'Apoiar Porel e entregar Venlana ao Consórcio',to:'mpq-epilogue',effects:{resolutionMode:'porel',venlanaExposed:true,porelLoyal:true}},
        {id:'side-venlana',label:'Apoiar Venlana contra Porel',to:'mpq-epilogue',effects:{resolutionMode:'venlana',porelBetrayed:true,consortiumRetaliation:true}},
        {id:'reject-both',label:'Recusar ambos e abandonar a recompensa',to:'mpq-epilogue',effects:{resolutionMode:'independent',consortiumRetaliation:true}}
      ]
    },
    "mpq-epilogue": {''',1)
s=s.replace("    schemaVersion: 2,\n    consequencesApplied:","    schemaVersion: 3,\n    consequencesApplied:",1)
s=s.replace("    [LOC.flagshipBridge,'Ponte da Renegade’s Blood','Centro de comando da verdadeira Rainha Pirata.']", "    [LOC.flagshipBridge,'Ponte da Renegade’s Blood','Centro de comando da verdadeira Rainha Pirata.'],\n    [LOC.sororinet,'Nó SororiNet','Acesso público que esconde dados de navegação da Irmandade.']")
s=s.replace("[NPC.graf,'Graf',LOC.saleucamiPort", "[NPC.graf,'Graf Lind',LOC.saleucamiPort")
s=s.replace("[NPC.sykes,'Jerid Sykes',LOC.spaceBattle,FACTION.zannConsortium,{species:'Humano',occupation:'Comandante da frota do Consórcio',goal:'Proteger os comboios de Tyber Zann.'}]", "[NPC.sykes,'Jerid Sykes',LOC.spaceBattle,FACTION.zannConsortium,{species:'Humano',occupation:'Comandante da frota do Consórcio',goal:'Proteger os comboios de Tyber Zann.'}],\n    [NPC.porel,'Porel Vakra',LOC.paradise,FACTION.zannConsortium,{species:'Twi’lek',occupation:'Subchefe do Consórcio',goal:'Investigar a lealdade de Venlana sem revelar sua missão.'}],\n    [NPC.lathe,'Lathe',LOC.sororinet,FACTION.veiledSorority,{species:'Humana',occupation:'Slicer da Irmandade',goal:'Rastrear intrusos na SororiNet.'}]")
# migrate v1 only, v2 remains valid; upgrading active v2 should not mutate historical event ids
s=s.replace("  if (world?.publishedAdventure?.id !== ID || Number(world.publishedAdventure.schemaVersion || 1) >= 2) return world;", "  if (world?.publishedAdventure?.id !== ID || Number(world.publishedAdventure.schemaVersion || 1) >= 2) return world;")
old='''  if (!c) return true;
  if (c.startsWith('unvisited:')) return !progress.visitedNodeIds.includes(c.slice(10));
  if (c.startsWith('visited:')) return progress.visitedNodeIds.includes(c.slice(8));
  if (c.startsWith('flag:')) return Boolean(progress.flags?.[c.slice(5)]);
  return true;'''
new='''  if (!c) return true;
  if (typeof c !== 'string') return false;
  if (c.startsWith('unvisited:')) return !progress.visitedNodeIds.includes(c.slice(10));
  if (c.startsWith('visited:')) return progress.visitedNodeIds.includes(c.slice(8));
  if (c.startsWith('flag:')) return Boolean(progress.flags?.[c.slice(5)]);
  if (c.startsWith('check-success:')) return progress.flags?.[`check:${c.slice(14)}`] === true;
  if (c.startsWith('check-failure:')) return progress.flags?.[`check:${c.slice(14)}`] === false;
  if (c.startsWith('encounter-resolved:')) return (progress.encounterHistory || []).some(x =>
    x.encounterId === c.slice(19) && ['adversaries_defeated','negotiated'].includes(x.outcome));
  return false; // Unknown conditions must never unlock a scene or reward.
'''
assert old in s
s=s.replace(old,new,1)
# ensure no skipping while encounter active, new progress world consequences
s=s.replace("  const node = MASK_OF_THE_PIRATE_QUEEN.nodes[p.currentNodeId];\n  const choice = (node?.choices || []).find(c => c.id === choiceId);", "  if (p.activeEncounterId) throw new Error('Encerre o encontro ativo antes de avançar de cena.');\n  const node = MASK_OF_THE_PIRATE_QUEEN.nodes[p.currentNodeId];\n  const choice = (node?.choices || []).find(c => c.id === choiceId);",1)
s=s.replace("  if (!target.choices || target.choices.length === 0) {\n    p.status = 'COMPLETED';\n    p.completedAt = now();\n  }", """  if (!target.choices || target.choices.length === 0) {
    p.status = 'COMPLETED';
    p.completedAt = now();
    // Store post-adventure consequences as data, never inventing a paid reward.
    const mode = p.flags?.resolutionMode || 'unknown';
    next.publishedAdventureConsequences = {...(next.publishedAdventureConsequences||{}),[ID]:{
      resolutionMode:mode,venlanaExposed:Boolean(p.flags?.venlanaExposed),
      consortiumRetaliation:Boolean(p.flags?.consortiumRetaliation || mode === 'alliance'),
      noiraCaptured:Boolean(p.flags?.queenCaptured),recordedAt:p.completedAt,
      source:'SWE13 Episode III conclusion',editoriallyCertified:false
    }};
    if (mode === 'porel' && next.npcs?.[NPC.venlana]) next.npcs[NPC.venlana].status='captured';
    if (mode === 'venlana' && next.npcs?.[NPC.porel]) next.npcs[NPC.porel].status='fugitive';
    if (mode === 'alliance' && next.npcs?.[NPC.noira]) next.npcs[NPC.noira].relation='ally';
  }""",1)
# source verified SororiNet failure increases alert, but no automatic combat unless GM
s=s.replace("    if (alert && ['scrap-zone-investigation','veiled-stronghold','palace-infiltration','renegades-blood-boarding'].includes(p.currentNodeId)) {", "    if (alert && ['scrap-zone-investigation','veiled-stronghold','palace-infiltration','renegades-blood-boarding','sororinet-entry','sororinet-secondary','sororinet-final'].includes(p.currentNodeId)) {",1)
s=s.replace("      alert.status = alert.value >= alert.max ? 'COMPLETED' : 'ACTIVE';\n    }", "      alert.status = alert.value >= alert.max ? 'COMPLETED' : 'ACTIVE';\n      if (p.currentNodeId.startsWith('sororinet-')) p.flags.sororinetIntrusionDetected = true;\n    }",1)
# prevent false reward marking on active worlds and negative credit
s=s.replace("export function markMaskOfThePirateQueenRewardsApplied(world, details = {}) {\n  const next", "export function markMaskOfThePirateQueenRewardsApplied(world, details = {}) {\n  if (world?.publishedAdventure?.id !== ID || world.publishedAdventure.status !== 'COMPLETED') throw new Error('Recompensa exige conclusão da aventura.');\n  if (world?.publishedAdventureRewards?.[ID]?.applied) throw new Error('Recompensa já aplicada.');\n  const next",1)
p.write_text(s)
# Registry source verified bounty per controlled PC; only one controlled character receives this via controller.
r=Path('app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs')
s=r.read_text()
s=s.replace("  rewardPolicy: Object.freeze({ credits: 4000, obligationReduction: 5 }),\n  // The Consortium does not pay a capture bounty if the crew joins the Sorority.\n  rewardPolicyForWorld: world => world?.publishedAdventure?.flags?.resolutionMode === 'alliance'\n    ? { credits: 0, obligationReduction: 0 } : { credits: 4000, obligationReduction: 5 },", """  // SWE13 pp. 18-19, 94-95: 10,000 credits per PC, less a 500-credit
  // retainer if taken; Porel awards 1,000 extra, Venlana promises double.
  // Obligation reduction was an unsourced legacy placeholder; do not apply it.
  rewardPolicy: Object.freeze({ credits: 10000, obligationReduction: 0 }),
  rewardPolicyForWorld: world => {
    const flags = world?.publishedAdventure?.flags || {};
    const mode = flags.resolutionMode;
    const retainer = flags.contractRetainerAccepted ? 500 : 0;
    if (['alliance','independent'].includes(mode)) return {credits:0,obligationReduction:0};
    if (mode === 'porel') return {credits:10500-retainer,obligationReduction:0};
    if (mode === 'venlana') return {credits:20000-retainer,obligationReduction:0};
    // Old completed P181 saves keep their previously recorded receipts.
    return {credits:10000-retainer,obligationReduction:0};
  },""")
r.write_text(s)
# Retainer flag when accepting contact, but not credit transfer until controller code handles it.
s=p.read_text().replace('''                        contractAccepted: true''','''                        contractAccepted: true,
                        contractRetainerAccepted: true''',1)
p.write_text(s)
