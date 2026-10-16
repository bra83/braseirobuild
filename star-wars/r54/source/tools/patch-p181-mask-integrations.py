from pathlib import Path
p=Path('app/src/main/assets/vtt/js/adventure/maskOfThePirateQueen.mjs')
s=p.read_text().replace('NPC.pirateQueen','NPC.noira').replace('"Vigilância"','"Percepção"').replace('"Persuasão"','"Encanto"')
needle='\nfunction conditionOk(choice, progress) {'
seed='''
// Source-anchored locations and persistent NPCs. Existing user edits always win.
function seedMaskWorld(world) {
  const next = clone(world || {});
  next.locations = { ...(next.locations || {}) };
  next.npcs = { ...(next.npcs || {}) };
  next.factions = { ...(next.factions || {}) };
  const places = [
    [LOC.saleucamiPort,'Taleucema, Saleucami','Capital construída dentro de uma cratera; encontro inicial com o Consórcio.'],
    [LOC.paradise,'Cantina Paradise','Local da reunião com Venlana, próximo ao porto de Taleucema.'],
    [LOC.krezoShop,'Krezo’s Imports','Loja e ponto de revenda de mercadorias em Saleucami.'],
    [LOC.blackwind,'Blackwind Crater','Cratera remota com acesso ao esconderijo da Irmandade.'],
    [LOC.vault,'The Vault','Instalação fortificada da Irmandade em Blackwind Crater.'],
    [LOC.worlport,'Worlport, Ord Mantell','Capital de Ord Mantell, sede de cassinos e intermediários.'],
    [LOC.vandinCasino,'Círculo de Illo Vandin','Rede de jogos e apostas usada por Illo Vandin.'],
    [LOC.rustyReach,'Rusty Reach','Assentamento nas terras de sucata de Ord Mantell.'],
    [LOC.queenPalace,'Palácio subterrâneo','Esconderijo da Irmandade sob Worlport.'],
    [LOC.spaceBattle,'Batalha no espaço','Região da interceptação à Renegade’s Blood.'],
    [LOC.shipCorridors,'Corredores da Renegade’s Blood','Passagens avariadas e defendidas por corsários.'],
    [LOC.flagshipBridge,'Ponte da Renegade’s Blood','Centro de comando da verdadeira Rainha Pirata.']
  ];
  for (const [id,name,description] of places) next.locations[id] ??= loc(id,name,description);
  next.factions[FACTION.veiledSorority] ??= {id:FACTION.veiledSorority,name:'Veiled Sorority',objective:'Preservar a frota e a identidade da Rainha.',status:'active',knownToPlayer:false,tags:['published-adventure',ID]};
  next.factions[FACTION.zannConsortium] ??= {id:FACTION.zannConsortium,name:'Zann Consortium',objective:'Encerrar os ataques aos seus carregamentos.',status:'active',knownToPlayer:true,tags:['published-adventure',ID]};
  const people = [
    [NPC.venlana,'Venlana Sipal',LOC.paradise,FACTION.zannConsortium,{species:'Pantorana',occupation:'Subchefe do Consórcio',knownToPlayer:true,goal:'Conduzir a contratação sem expor os próprios interesses.'}],
    [NPC.graf,'Graf',LOC.saleucamiPort,FACTION.veiledSorority,{occupation:'Ex-pirata',goal:'Voltar a ter influência entre os corsários.'}],
    [NPC.krezo,'Krezo Wasanti',LOC.krezoShop,null,{species:'Rodiano',occupation:'Receptador',goal:'Evitar represálias pelo comércio de cargas roubadas.'}],
    [NPC.revah,'Revah',LOC.vault,FACTION.veiledSorority,{species:'Humana',occupation:'Isca mascarada',goal:'Proteger a identidade da verdadeira Rainha.'}],
    [NPC.jander,'Jander',LOC.worlport,FACTION.zannConsortium,{species:'Toydariano',occupation:'Contato local do Consórcio',goal:'Orientar a investigação sem se expor.'}],
    [NPC.illo,'Illo Vandin',LOC.vandinCasino,null,{species:'Herglic',occupation:'Magnata do jogo',goal:'Cobrar favores antes de compartilhar informações.'}],
    [NPC.ryale,'Ryale Wei',LOC.queenPalace,FACTION.veiledSorority,{species:'Humana',occupation:'Oficial da Irmandade',goal:'Defender a liderança da Rainha.'}],
    [NPC.noira,'Noira',LOC.flagshipBridge,FACTION.veiledSorority,{species:'Ubese',occupation:'Verdadeira Rainha Pirata',goal:'Preservar a autonomia da frota.'}],
    [NPC.sykes,'Jerid Sykes',LOC.spaceBattle,FACTION.zannConsortium,{species:'Humano',occupation:'Comandante da frota do Consórcio',goal:'Proteger os comboios de Tyber Zann.'}]
  ];
  for (const [id,name,locationId,factionId,profile] of people) next.npcs[id] ??= npc(id,name,locationId,factionId,profile);
  try { ensureLivingWorldDepth(next); } catch { /* engine without living-depth service remains playable */ }
  return next;
}

// P180 saves retain history, choices, checks and event IDs. Only a v1
// introductory Ord Mantell scene is re-homed to its actual Saleucami start.
// Both legacy progress and UAE snapshot must agree before reconciliation.
export function migrateMaskOfThePirateQueenWorld(world) {
  if (world?.publishedAdventure?.id !== ID || Number(world.publishedAdventure.schemaVersion || 1) >= 2) return world;
  const next = clone(world);
  const p = next.publishedAdventure;
  const previousNode = p.currentNodeId;
  const remap = id => id === 'ord-mantell-arrival' ? 'saleucami-contract-offer' : id;
  p.currentNodeId = remap(p.currentNodeId);
  p.visitedNodeIds = [...new Set((p.visitedNodeIds || []).map(remap))];
  if (!p.visitedNodeIds.includes(p.currentNodeId)) p.visitedNodeIds.push(p.currentNodeId);
  p.schemaVersion = 2;
  p.migration = {from:1,to:2,previousNodeId:previousNode,kind:'source-correction-saleucami',migratedAt:now()};
  const r = next.publishedAdventureRuntime;
  if (r?.adventureId === ID) {
    r.currentNodeId = remap(r.currentNodeId);
    r.visitedNodeIds = [...new Set((r.visitedNodeIds || []).map(remap))];
    r.currentLocationId = MASK_OF_THE_PIRATE_QUEEN.nodes[r.currentNodeId]?.locationId ?? r.currentLocationId;
    // Canonical history is immutable; old events still refer to their original source node.
  }
  return next;
}
'''
s=s.replace(needle,'\n'+seed+needle)
s=s.replace('  const next = clone(world || {});\n  next.publishedAdventure = baseProgress();','  const next = seedMaskWorld(world);\n  next.publishedAdventure = baseProgress();',1)
s=s.replace('  const next = clone(world || {});\n  const p = next.publishedAdventure;\n  if (!p || p.id !== ID || p.status !== \'ACTIVE\') throw new Error(\'Aventura não está ativa.\');\n  const node = MASK_OF_THE_PIRATE_QUEEN.nodes[p.currentNodeId];',"  const next = seedMaskWorld(migrateMaskOfThePirateQueenWorld(world));\n  const p = next.publishedAdventure;\n  if (!p || p.id !== ID || p.status !== 'ACTIVE') throw new Error('Aventura não está ativa.');\n  const node = MASK_OF_THE_PIRATE_QUEEN.nodes[p.currentNodeId];",1)
s=s.replace('    checkHistory: [],\n    consequencesApplied:', '    checkHistory: [],\n    encounterHistory: [],\n    schemaVersion: 2,\n    consequencesApplied:',1)
# enrich checks with outcome flags, check history without false narrative assumptions
s=s.replace("  const entry = { at: now(), nodeId: p.currentNodeId, checkIndex, success: Boolean(result.success) };\n  p.checkHistory.push(entry);", "  if (typeof result.success !== 'boolean') throw new Error('Resultado de teste deve informar sucesso ou falha.');\n  p.checkHistory = Array.isArray(p.checkHistory) ? p.checkHistory : [];\n  const entry = { at: now(), nodeId: p.currentNodeId, checkIndex, skill: check.skill, success: result.success,\n    netAdvantage: Number.isFinite(Number(result.netAdvantage)) ? Number(result.netAdvantage) : 0 };\n  p.checkHistory.push(entry);\n  p.flags = { ...(p.flags || {}), [`check:${p.currentNodeId}:${checkIndex}`]: result.success };\n  if (!result.success) {\n    const alert = p.clocks?.pirateAlert;\n    if (alert && ['scrap-zone-investigation','veiled-stronghold','palace-infiltration','renegades-blood-boarding'].includes(p.currentNodeId)) {\n      alert.value = Math.min(alert.max, (Number(alert.value) || 0) + 1);\n      alert.status = alert.value >= alert.max ? 'COMPLETED' : 'ACTIVE';\n    }\n  }",1)
# change final encounter to Noira, add earlier corsair encounters, restrict outcome
start=s.index('export function currentMaskOfThePirateQueenEncounter(world) {')
end=s.index('\nexport function markMaskOfThePirateQueenRewardsApplied',start)
block='''export function currentMaskOfThePirateQueenEncounter(world) {
  const p = world?.publishedAdventure;
  if (!p || p.id !== ID || p.status !== 'ACTIVE') return null;
  const scenes = {
    'veiled-stronghold':['vault-sorority-guards','Guardas do Vault'],
    'palace-infiltration':['worlport-sorority-guards','Defensores do palácio'],
    'renegades-blood-boarding':['renegades-blood-corsairs','Corsários a bordo'],
    'flagship-showdown':['pirate-queen-duel','Confronto com Noira']
  };
  const pair = scenes[p.currentNodeId];
  if (!pair) return null;
  const [id,label] = pair;
  const resolved = (p.encounterHistory || []).some(x => x.encounterId === id && x.outcome);
  return {id,label,available:!resolved,status:resolved?'resolved':p.activeEncounterId===id?'active':'ready'};
}

export function maskOfThePirateQueenEncounterSpec(world, encounterId = null) {
  const current = currentMaskOfThePirateQueenEncounter(world);
  if (!current || !current.available || (encounterId && encounterId !== current.id))
    throw new Error('Encontro publicado indisponível na cena atual.');
  const final = current.id === 'pirate-queen-duel';
  return {id:current.id,label:current.label,playerInitiativeSkill:'frialdad',npcInitiativeSkill:'alerta',
    // Generic corsair combat profile is a fallback; this is not an editorial stat-block certification.
    adversaries:[{profileKey:'veiled_sorority_corsair',count:final?2:3,range:'Curto'},
      ...(final?[{profileKey:'pirate_queen_nemesis',worldNpcId:NPC.noira,range:'Médio'}]:[])]};
}

export function markMaskOfThePirateQueenEncounterActive(world, encounterId) {
  const next = clone(world || {}),p = next.publishedAdventure;
  const encounter = currentMaskOfThePirateQueenEncounter(next);
  if (!encounter || !encounter.available || encounter.id !== encounterId || p.activeEncounterId)
    throw new Error('Encontro não pode ser iniciado nesta cena.');
  p.activeEncounterId = encounterId;
  p.updatedAt = now();
  return next;
}

export function recordMaskOfThePirateQueenEncounterOutcome(world, {encounterId,outcome,adversaries=[]} = {}) {
  const next = clone(world || {}),p = next.publishedAdventure;
  const encounter = currentMaskOfThePirateQueenEncounter(next);
  if (!encounter || !encounter.available || encounter.id !== encounterId ||
      (p.activeEncounterId && p.activeEncounterId !== encounterId))
    throw new Error('Resultado de encontro incompatível com a cena.');
  if (!['adversaries_defeated','players_retreat','negotiated','ended'].includes(outcome))
    throw new Error('Resultado de encontro inválido.');
  p.encounterHistory = Array.isArray(p.encounterHistory) ? p.encounterHistory : [];
  const record = {at:now(),nodeId:p.currentNodeId,encounterId,outcome,
    adversaries:(Array.isArray(adversaries)?adversaries:[]).slice(0,24).map(a=>({id:a.id??null,profileKey:a.profileKey??null,status:a.status??null}))};
  p.encounterHistory.push(record);
  p.activeEncounterId = null;
  p.flags = {...(p.flags||{}),[`encounter:${encounterId}`]:outcome};
  if (encounterId === 'pirate-queen-duel' && outcome === 'adversaries_defeated') p.flags.noiraDefeated = true;
  p.updatedAt = now();
  return {world:next,record:clone(record)};
}
'''
s=s[:start]+block+s[end:]
p.write_text(s)
# registry: normalize before adapter execution, for imported v1 saves with saved UAE state
reg=Path('app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs')
r=reg.read_text()
r=r.replace("function integratedAdapter(adapter) {\n  const mapWorld", "function integratedAdapter(adapter) {\n  const normalize = world => adapter.id === 'mask-of-the-pirate-queen' ? maskOfThePirateQueen.migrateMaskOfThePirateQueenWorld(world) : world;\n  const mapWorld")
r=r.replace('    const result = method(world, ...args);\n    if (!result', '    const baseline = normalize(world);\n    const result = method(baseline, ...args);\n    if (!result')
r=r.replace('world: mapWorld(world, result.world, kind, options(...args))','world: mapWorld(baseline, result.world, kind, options(...args))')
r=r.replace('status: world => ({ ...adapter.status(world), uae: publishedAdventureUaePublicStatus(world, adapter) }),', 'status: world => { const w=normalize(world); return { ...adapter.status(w), uae: publishedAdventureUaePublicStatus(w, adapter) }; },')
r=r.replace('      const result = adapter.start(world);','      const result = adapter.start(normalize(world));')
r=r.replace("markEncounterActive: (world, encounterId) => mapWorld(world, adapter.markEncounterActive(world, encounterId), 'ENCOUNTER_STARTED', { payload: { encounterId } }),", "markEncounterActive: (world, encounterId) => { const w=normalize(world); return mapWorld(w, adapter.markEncounterActive(w, encounterId), 'ENCOUNTER_STARTED', { payload: { encounterId } }); },")
r=r.replace("markRewardsApplied: (world, details) => mapWorld(world, adapter.markRewardsApplied(world, details), 'REWARDS_APPLIED', { payload: { credits: Number(details?.credits) || 0 } }),", "markRewardsApplied: (world, details) => { const w=normalize(world); return mapWorld(w, adapter.markRewardsApplied(w, details), 'REWARDS_APPLIED', { payload: { credits: Number(details?.credits) || 0 } }); },")
r=r.replace("archive: world => mapWorld(world, adapter.archive(world), 'ARCHIVED'),", "archive: world => { const w=normalize(world); return mapWorld(w, adapter.archive(w), 'ARCHIVED'); },")
r=r.replace('uaeSnapshot: world => publishedAdventureUaeSnapshot(world, adapter)', 'uaeSnapshot: world => publishedAdventureUaeSnapshot(normalize(world), adapter)')
reg.write_text(r)
print('Integrated NPCs, migration, checks, encounters, bridge')
