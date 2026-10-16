import test from 'node:test';
import assert from 'node:assert/strict';
import { AdventureRuntime } from '../app/src/main/assets/vtt/js/adventure/adventureRuntime.mjs';
import { validateAdventureModule } from '../app/src/main/assets/vtt/js/adventure/adventureSchema.mjs';
import { validateAdventureCheckpoint } from '../app/src/main/assets/vtt/js/adventure/adventureCheckpointGate.mjs';
import { getPublishedAdventureAdapter, publishedAdventureCatalog } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import { publishedAdventureRuntimeModule, publishedAdventureUaeSnapshot, validatePublishedAdventureRuntimeState } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';
import { AOR_TALENT_TREES } from '../app/src/main/assets/vtt/js/expansion/aorTalentTreesData.mjs';
import { aorTalentCoverage } from '../app/src/main/assets/vtt/js/expansion/aorProgressionIntegration.mjs';
import { aorCanonicalTalentName } from '../app/src/main/assets/vtt/js/expansion/aorTalentRuntimeAliases.mjs';
import { createAorCoreCharacter } from '../app/src/main/assets/vtt/js/rules/characterCreation.mjs';
import { purchaseSpecialization } from '../app/src/main/assets/vtt/js/rules/progression.mjs';
import { eligibleTalents, purchaseTalent, talentRankByOriginal } from '../app/src/main/assets/vtt/js/rules/talents.mjs';

const adapter = getPublishedAdventureAdapter('under-black-sun');
const module = publishedAdventureRuntimeModule(adapter.meta);
const create = () => new AdventureRuntime(module);
const copy = structuredClone;

const corruptions = [
  ['evento com ID de outra campanha', s => { s.canonicalEvents[0].id = 'evt_other_1'; }],
  ['evento fora de ordem', s => { s.canonicalEvents.push(copy(s.canonicalEvents[0])); }],
  ['timestamp inválido', s => { s.canonicalEvents[0].timestamp = 'invalid'; }],
  ['evento sem payload', s => { s.canonicalEvents[0].payload = null; }],
  ['evento sem turno', s => { s.canonicalEvents[0].turn = 0; }],
  ['contador não numérico', s => { s.counters.test = '1'; }],
  ['contador booleano', s => { s.counters.test = false; }],
  ['gatilho inexistente', s => { s.triggersFired.notReal = 1; }],
  ['gatilho acima do limite', s => { s.triggersFired['p128-adventure-finale'] = 2; }],
  ['objetivo concluído sem histórico', s => { s.objectives['episode-1'].status = 'COMPLETED'; }],
  ['objetivo concluído com progresso impossível', s => { s.objectives['episode-1'].progress = 2; }],
  ['objetivo no histórico sem conclusão', s => { s.completedObjectives.push('episode-1'); }],
  ['histórico de objetivo duplicado', s => { s.completedObjectives.push('episode-1','episode-1'); }],
  ['relógio fora dos limites', s => { s.clocks[Object.keys(s.clocks)[0]].value = -1; }],
  ['relógio booleano', s => { s.clocks[Object.keys(s.clocks)[0]].value = true; }],
  ['cena atual inexistente', s => { s.currentNodeId = 'nonexistent'; }],
  ['visita duplicada', s => { s.visitedNodeIds.push(s.currentNodeId); }],
  ['consequência duplicada', s => { s.consequences = [{id:'x',resolved:false,recordedAt:new Date().toISOString()},{id:'x',resolved:false,recordedAt:new Date().toISOString()}]; }],
  ['consequência resolvida sem data', s => { s.consequences = [{id:'x',resolved:true,recordedAt:new Date().toISOString()}]; }],
];
for (const [name, corrupt] of corruptions) {
  test(`P136 ${name}: transação reverte e importação falha fechada`, () => {
    const runtime = create();
    const before = runtime.getState();
    assert.throws(() => runtime.transaction(r => corrupt(r.state)));
    assert.deepEqual(runtime.getState(), before);
    const imported = copy(before); corrupt(imported);
    assert.throws(() => AdventureRuntime.restore(module, imported));
    assert.deepEqual(runtime.getState(), before);
  });
}

test('P136 dados não numéricos em efeitos falham sem contaminar clocks, counters ou journal', () => {
  for (const invalid of [true,false,[],{},'0x10',' ',null,Infinity]) {
    const runtime = create(), before = runtime.serialize();
    assert.throws(() => runtime.applyEffects([{type:'set-counter',counter:'xp',value:invalid}]));
    assert.equal(runtime.serialize(),before);
    assert.throws(() => runtime.advanceClock('suspicion',invalid));
    assert.equal(runtime.serialize(),before);
  }
  const runtime = create();
  runtime.applyEffects([{type:'set-counter',counter:'xp',value:'2.5'}]);
  assert.equal(runtime.state.counters.xp,2.5);
  assert.equal(runtime.isConditionMet('counter:xp:>=:2.5'),true);
  assert.equal(runtime.isConditionMet('counter:xp:<=:0x10'),false);
  assert.equal(runtime.isConditionMet('counter:xp:<=:'),false);
});

test('P136 módulos com identificadores que colidem com protótipos são recusados', () => {
  for (const key of ['__proto__','constructor','prototype']) {
    const malicious = copy(module); Object.defineProperty(malicious.nodes,key,{value:{choices:[]},enumerable:true,configurable:true,writable:true});
    assert.throws(() => validateAdventureModule(malicious));
    const unsafe = copy(module); unsafe.triggers.push({id:key,event:'TEST'});
    assert.throws(() => validateAdventureModule(unsafe));
  }
});

for (const {id} of publishedAdventureCatalog()) {
  test(`P136 ${id}: evento, consequência, objetivo, clock, restauração e projeção em um commit`, () => {
    const a = getPublishedAdventureAdapter(id);
    const world = a.start({}).world;
    const m = publishedAdventureRuntimeModule(a.meta);
    const runtime = AdventureRuntime.restore(m,world.publishedAdventureRuntime);
    const firstClock = Object.keys(runtime.state.clocks)[0];
    runtime.transaction(r => {
      r.dispatchEvent('P136_TURN', {turn:1,source:'regression'});
      r.recordConsequence('p136-consequence');
      r.updateObjectiveProgress('episode-1',0.5);
      r.advanceClock(firstClock,0.5);
    });
    const checkpoint = JSON.parse(runtime.serialize());
    assert.equal(validateAdventureCheckpoint(m,checkpoint),true);
    assert.equal(validatePublishedAdventureRuntimeState(m,checkpoint),true);
    assert.equal(AdventureRuntime.restore(m,checkpoint).serialize(),runtime.serialize());
    const updated = copy(world);updated.publishedAdventureRuntime = checkpoint;
    assert.deepEqual(publishedAdventureUaeSnapshot(updated,a),checkpoint);
    assert.equal(checkpoint.objectives['episode-1'].progress,0.5);
    assert.ok(checkpoint.consequences.some(c=>c.id==='p136-consequence'));
    assert.ok(checkpoint.canonicalEvents.some(e=>e.type==='P136_TURN'));
  });
}

test('P136 AoR Jump Up compartilha o efeito JUMP sem criar efeito para talento não mapeado',()=>{
  assert.equal(aorCanonicalTalentName('LEVANTARSE DE UN SALTO'),'JUMP');
  assert.equal(aorCanonicalTalentName('DISPARO CERTERO'),'TRUEAIM'); // P137 True Aim
  assert.ok(aorTalentCoverage().mechanicalPositions>=178);
  const character=createAorCoreCharacter({name:'P136',speciesKey:'human',careerKey:'ace',specializationKey:'pilot',partySize:1,
    careerFreeSkills:['astronavegacion','frialdad','artilleria','mecanica'],specializationFreeSkills:['astronavegacion','artilleria'],
    speciesChoiceSkills:['atletismo','medicina'],motivation:'Liberdade',dutyType:'Superioridade espacial'});
  character.xp.available=1500;character.xp.total=1500;
  const matching=[];
  for(const [career,specs] of Object.entries(AOR_TALENT_TREES))for(const [spec,tree] of Object.entries(specs)){
    const node=tree.nodes.find(n=>n.originalName==='LEVANTARSE DE UN SALTO');if(node)matching.push({career,spec,tree,node});
  }
  assert.ok(matching.length>0);
  const {career,spec,tree,node}=matching[0];
  let c=(career==='ace'&&spec==='pilot')?character:purchaseSpecialization(character,career,spec).character;
  const adjacent=new Map(tree.nodes.map(n=>[n.id,[]]));
  for(const [a,b] of tree.edges){adjacent.get(a).push(b);adjacent.get(b).push(a);}
  const queue=tree.nodes.filter(n=>n.row===1).map(n=>[n.id]);const seen=new Set();let path=null;
  while(queue.length){const p=queue.shift(),id=p.at(-1);if(seen.has(id))continue;seen.add(id);if(id===node.id){path=p;break;}
    for(const n of adjacent.get(id))if(!seen.has(n))queue.push([...p,n]);}
  assert.ok(path);
  for(const id of path){const talent=eligibleTalents(c,{careerKey:career,specializationKey:spec}).find(t=>t.id===id);
    assert.equal(talent.unlocked,true);const options={careerKey:career,specializationKey:spec};
    if(talent.choiceType==='characteristic')options.characteristicKey='intelecto';
    if(talent.choiceType==='combatSkill')options.skillKey='armas_distancia_ligeras';
    c=purchaseTalent(c,id,options).character;}
  assert.equal(talentRankByOriginal(c,'JUMP'),1);
  assert.equal(talentRankByOriginal(JSON.parse(JSON.stringify(c)),'JUMP'),1);
});

for (const {id} of publishedAdventureCatalog()) {
  test(`P136 ${id}: três reaberturas e 100 turnos preservam journal, objetivos e sequência`, () => {
    const a=getPublishedAdventureAdapter(id),m=publishedAdventureRuntimeModule(a.meta);
    const original=a.start({}).world.publishedAdventureRuntime;
    let r=AdventureRuntime.restore(m,original);
    const firstId=r.state.canonicalEvents.at(-1).id;
    for(let reopening=0;reopening<3;reopening++) {
      for(let turn=0;turn<100;turn++)r.dispatchEvent('WORLD_TURN',{reopening,turn});
      const json=r.serialize();
      r=AdventureRuntime.restore(m,json);
      assert.equal(r.serialize(),json);
      assert.equal(validatePublishedAdventureRuntimeState(m,r.getState()),true);
    }
    assert.equal(r.state.canonicalEvents.length,original.canonicalEvents.length+300);
    assert.equal(new Set(r.state.canonicalEvents.map(e=>e.id)).size,r.state.canonicalEvents.length);
    assert.equal(r.state.canonicalEvents[original.canonicalEvents.length-1].id,firstId);
    assert.equal(r.state.eventSequence,original.eventSequence+300);
    assert.deepEqual(r.state.objectives,original.objectives);
    assert.deepEqual(r.state.clocks,original.clocks);
  });
}
