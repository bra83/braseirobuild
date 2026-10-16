import test from 'node:test';
import assert from 'node:assert/strict';
import { AOR_TALENT_TREES } from '../app/src/main/assets/vtt/js/expansion/aorTalentTreesData.mjs';
import { aorTalentCatalog, aorTalentCoverage } from '../app/src/main/assets/vtt/js/expansion/aorProgressionIntegration.mjs';
import { createAorCoreCharacter } from '../app/src/main/assets/vtt/js/rules/characterCreation.mjs';
import { treeForCharacter, eligibleTalents, purchaseTalent, talentRankByOriginal } from '../app/src/main/assets/vtt/js/rules/talents.mjs';
import { publishedAdventureCatalog, getPublishedAdventureAdapter } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import { publishedAdventureRuntimeModule, validatePublishedAdventureRuntimeState } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';
import { effectiveWeaponWithAttachments, attachmentInstallPayload } from '../app/src/main/assets/vtt/js/rules/attachments.mjs';
import { weaponAttackModifiers } from '../app/src/main/assets/vtt/js/rules/weapons.mjs';
import { createCampaign } from '../app/src/main/assets/vtt/js/core/state.mjs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { BraseiroUiBridge } from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';

const clone = structuredClone;
const base = { name: 'Nira Valen', speciesKey: 'human', careerKey: 'ace', specializationKey: 'pilot', partySize: 1,
  careerFreeSkills: ['astronavegacion', 'frialdad', 'artilleria', 'mecanica'],
  specializationFreeSkills: ['astronavegacion', 'artilleria'],
  speciesChoiceSkills: ['atletismo', 'medicina'], motivation: 'Libertar mundos ocupados.', dutyType: 'Superioridade espacial' };
const character = () => createAorCoreCharacter(base);
class Repo {
  constructor() { this.state=createCampaign('P130'); }
  load() { return clone(this.state); }
  save(value) { this.state=clone(value); }
}

test('P130 AoR: todas as 18 árvores de origem ficam navegáveis e a primeira linha desbloqueada', () => {
  const c=character();
  let total=0;
  for (const [career, specs] of Object.entries(AOR_TALENT_TREES)) for (const [spec, tree] of Object.entries(specs)) {
    const selected=clone(c);
    selected.creation.careerKey=career;
    selected.creation.specializationKey=spec;
    const view=treeForCharacter(selected);
    assert.equal(view.nodes.length,20,`${career}:${spec}`);
    assert.deepEqual(view.edges,tree.edges);
    const eligible=eligibleTalents(selected);
    assert.equal(eligible.length,20);
    assert.ok(eligible.filter(n=>n.row===1).every(n=>n.unlocked));
    assert.ok(eligible.filter(n=>n.row>1).every(n=>!n.unlocked));
    total++;
  }
  assert.equal(total,18);
});

test('P130 AoR: compra real desconta PE, preserva fonte, posição e não modifica entrada', () => {
  const c=character();
  const node=eligibleTalents(c).find(n=>n.row===1);
  const before=clone(c);
  const result=purchaseTalent(c,node.id);
  assert.deepEqual(c,before);
  assert.equal(result.character.xp.available,c.xp.available-node.cost);
  assert.equal(result.character.talentPurchases[0].nodeId,node.id);
  assert.equal(result.character.talentPurchases[0].source.book,node.source.book);
  assert.equal(result.character.talentPurchases[0].careerKey,'ace');
  assert.equal(result.character.talentPurchases[0].specializationKey,'pilot');
  assert.equal(talentRankByOriginal(result.character,node.originalName),1);
  assert.throws(()=>purchaseTalent(result.character,node.id),/já foi adquirido/);
});

test('P130 AoR: somente vizinhos de slots comprados são liberados, mesmo após JSON restore', () => {
  const c=character();
  const tree=treeForCharacter(c);
  const first=tree.nodes.find(n=>n.row===1 && tree.edges.some(edge=>edge.includes(n.id) && edge.some(id=>tree.nodes.find(n=>n.id===id)?.row===2)));
  const bought=purchaseTalent(c,first.id).character;
  const restored=JSON.parse(JSON.stringify(bought));
  const neighbors=new Set(tree.edges.filter(e=>e.includes(first.id)).flat());
  for(const node of eligibleTalents(restored)) {
    if(node.row>1) assert.equal(node.unlocked,neighbors.has(node.id),node.id);
  }
  const next=eligibleTalents(restored).find(n=>n.row===2 && n.unlocked);
  assert.ok(next);
  const second=purchaseTalent(restored,next.id).character;
  assert.equal(second.talentPurchases.length,2);
  assert.equal(second.xp.available,c.xp.available-first.cost-next.cost);
});

test('P130 AoR: bloqueia aquisição de outra especialização, árvore EotE e saldo insuficiente', () => {
  const c=character();
  const first=eligibleTalents(c)[0];
  const forbidden=AOR_TALENT_TREES.soldier.commando.nodes[0];
  assert.equal(treeForCharacter(c,{careerKey:'soldier',specializationKey:'commando'}),null);
  assert.deepEqual(eligibleTalents(c,{careerKey:'soldier',specializationKey:'commando'}),[]);
  assert.throws(()=>purchaseTalent(c,forbidden.id),/não pertence/);
  assert.throws(()=>purchaseTalent(c,first.id,{careerKey:'soldier',specializationKey:'commando'}),/não pertence/);
  const poor=clone(c);poor.xp.available=0;
  assert.throws(()=>purchaseTalent(poor,first.id),/PE insuficientes/);
  assert.deepEqual(c,character());
});

test('P130 AoR: efeitos passivos já implementados são aplicados à ficha sem duplicação', () => {
  const c=character();c.creation.specializationKey='gunner';
  const first=eligibleTalents(c).find(n=>n.originalName==='APLOMO');
  assert.ok(first);
  const after=purchaseTalent(c,first.id).character;
  assert.equal(after.derived.strain.threshold,c.derived.strain.threshold+1);
  assert.equal(talentRankByOriginal(after,'APLOMO'),1);
  assert.equal(after.talents.filter(t=>t===first.name).length,1);
});

test('P130 AoR: compra no controlador persiste após reiniciar e passa pelo bridge Android', () => {
  const repo=new Repo();
  let game=new GameController(repo);
  game.setCampaignMode('AOR');
  game.createAorCharacter(base);
  const before=game.state.player.character;
  const node=game.eligibleTalents().find(n=>n.row===1);
  assert.ok(node);
  game.buyTalent(node.id);
  assert.equal(game.state.player.character.xp.available,before.xp.available-node.cost);
  game=new GameController(repo);
  assert.equal(game.eligibleTalents().find(n=>n.id===node.id).purchased,true);
  const bridge=new BraseiroUiBridge({repository:repo});
  assert.equal(bridge.eligibleTalents().ok,true);
  assert.equal(bridge.eligibleTalents().data.find(n=>n.id===node.id).purchased,true);
  assert.equal(bridge.buyTalent(node.id).ok,false);
});

for(const {id} of publishedAdventureCatalog()) {
  test(`P130 ${id}: journal íntegro aceita save e rejeita reordenação/IDs cruzados`, () => {
    const adapter=getPublishedAdventureAdapter(id);
    const world=adapter.start({}).world;
    const module=publishedAdventureRuntimeModule(adapter.meta);
    const state=world.publishedAdventureRuntime;
    assert.equal(validatePublishedAdventureRuntimeState(module,state,world.publishedAdventure),true);
    const swapped=clone(state);
    [swapped.canonicalEvents[0],swapped.canonicalEvents[1]]=[swapped.canonicalEvents[1],swapped.canonicalEvents[0]];
    assert.throws(()=>validatePublishedAdventureRuntimeState(module,swapped,world.publishedAdventure),/regressiva/);
    const foreign=clone(state);
    foreign.canonicalEvents[0].id='evt_other-adventure_1';
    assert.throws(()=>validatePublishedAdventureRuntimeState(module,foreign,world.publishedAdventure),/estrangeiro/);
    const malformed=clone(state);
    malformed.canonicalEvents[0].payload='corrupted';
    assert.throws(()=>validatePublishedAdventureRuntimeState(module,malformed,world.publishedAdventure),/malformado/);
    const before=clone(world);
    world.publishedAdventureRuntime.canonicalEvents=swapped.canonicalEvents;
    assert.throws(()=>adapter.markRewardsApplied(world,{credits:123}),/regressiva/);
    assert.equal(before.publishedAdventureRewards?.[id],undefined);
  });
}

test('P130 UAE: objetivo histórico contraditório e consequência duplicada são rejeitados', () => {
  const adapter=getPublishedAdventureAdapter('under-black-sun');
  const world=adapter.start({}).world;
  const module=publishedAdventureRuntimeModule(adapter.meta);
  const bad=clone(world.publishedAdventureRuntime);
  bad.completedObjectives.push('episode-1');
  assert.throws(()=>validatePublishedAdventureRuntimeState(module,bad,world.publishedAdventure),/inconsistente/);
  const duplicate=clone(world.publishedAdventureRuntime);
  duplicate.consequences=[{id:'same'},{id:'same'}];
  assert.throws(()=>validatePublishedAdventureRuntimeState(module,duplicate,world.publishedAdventure),/duplicadas/);
});

test('P130 equipamentos: tentativa de mod falhada consome slot também na resolução de combate', () => {
  const rapid=attachmentInstallPayload('rapid_recharge_xciter','a1');
  rapid.modAttempts=[{modId:'reduce_inaccurate',status:'failed'}];
  rapid.mods=[{modId:'reduce_inaccurate',status:'installed'},{modId:'reduce_inaccurate',status:'installed'}];
  const weapon={id:'blaster_rifle',name:'Blaster rifle',damage:9,range:'Longo',qualities:[],attachments:[rapid]};
  const before=clone(weapon);
  const result=effectiveWeaponWithAttachments(weapon);
  assert.equal(weaponAttackModifiers(result).inaccurate,2);
  assert.equal(result.attachmentEffects.ignoredMods,1);
  assert.deepEqual(weapon,before);
});


test('P130 AoR: catálogo distingue aquisição de efeito mecânico realmente implementado', () => {
  const all=aorTalentCatalog();
  const coverage=aorTalentCoverage();
  assert.equal(coverage.positions,360);
  assert.equal(coverage.purchasePositions,360);
  assert.equal(coverage.mechanicalPositions,all.filter(n=>n.mechanicallyActive).length);
  assert.ok(coverage.mechanicalPositions<=coverage.positions); // P179 now closes the remaining AoR consumers
  assert.ok(coverage.mechanicalPositions>0);
  assert.ok(all.every(n=>n.purchaseActive===true));
  assert.ok(all.filter(n=>n.originalName==='APLOMO').every(n=>n.mechanicallyActive));
  assert.ok(all.filter(n=>n.originalName==='DISPARO CERTERO').every(n=>n.mechanicallyActive)); // P137 True Aim
});


test('P130 AoR: talento racial e compra do mesmo nome acumulam graus sem duplicação', () => {
  const c=character();
  c.creation.specializationKey='gunner';
  c.talents.push('Compostura');
  const node=eligibleTalents(c).find(n=>n.originalName==='APLOMO');
  const after=purchaseTalent(c,node.id).character;
  assert.equal(talentRankByOriginal(after,'APLOMO'),2);
  assert.equal(talentRankByOriginal(c,'APLOMO'),1);
});
