import test from 'node:test';
import assert from 'node:assert/strict';
import { AOR_TALENT_TREES } from '../app/src/main/assets/vtt/js/expansion/aorTalentTreesData.mjs';
import { AOR_RUNTIME_TALENT_ALIASES, AOR_RUNTIME_MECHANICAL_TALENTS } from '../app/src/main/assets/vtt/js/expansion/aorTalentRuntimeAliases.mjs';
import { aorTalentCatalog, aorTalentCoverage } from '../app/src/main/assets/vtt/js/expansion/aorProgressionIntegration.mjs';
import { createAorCoreCharacter } from '../app/src/main/assets/vtt/js/rules/characterCreation.mjs';
import { purchaseSpecialization } from '../app/src/main/assets/vtt/js/rules/progression.mjs';
import { eligibleTalents, purchaseTalent, talentRankByOriginal, fullThrottleTalentSpec,
  quickReactionOption, talentSkillPoolModifiers, secondWindOption, dodgeOption,
  lethalBlowsCriticalModifier, deadlyAccuracySpec, naturalRerollOption } from '../app/src/main/assets/vtt/js/rules/talents.mjs';
import { preparePlayerInitiative } from '../app/src/main/assets/vtt/js/rules/initiative.mjs';
import { recoverAfterEncounter } from '../app/src/main/assets/vtt/js/rules/recovery.mjs';
import { AdventureRuntime } from '../app/src/main/assets/vtt/js/adventure/adventureRuntime.mjs';
import { createCampaign } from '../app/src/main/assets/vtt/js/core/state.mjs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { BraseiroUiBridge } from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';
import { getPublishedAdventureAdapter, publishedAdventureCatalog } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import { publishedAdventureRuntimeModule, publishedAdventureUaeSnapshot, validatePublishedAdventureRuntimeState } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';

const clone = structuredClone;
const create = () => createAorCoreCharacter({name:'Rae Voss',speciesKey:'human',careerKey:'ace',specializationKey:'pilot',partySize:1,
  careerFreeSkills:['astronavegacion','frialdad','artilleria','mecanica'],specializationFreeSkills:['astronavegacion','artilleria'],
  speciesChoiceSkills:['atletismo','medicina'],motivation:'Liberdade',dutyType:'Superioridade espacial'});
function buyFirst(career,spec,originalName,options={}) {
  let c=create();
  if(career!=='ace'||spec!=='pilot') c=purchaseSpecialization(c,career,spec).character;
  const node=eligibleTalents(c,{careerKey:career,specializationKey:spec}).find(n=>n.row===1 && n.originalName===originalName);
  assert.ok(node,`${career}/${spec}/${originalName}`);
  const before=clone(c);
  const result=purchaseTalent(c,node.id,{...options,careerKey:career,specializationKey:spec});
  assert.deepEqual(c,before,'compra nunca modifica a ficha anterior');
  assert.equal(result.character.xp.available,c.xp.available-node.cost);
  return {character:result.character,node};
}

test('P132 AoR: catálogo de 18 árvores declara efeito somente para aliases com consumidores reais',()=>{
  const all=aorTalentCatalog(), coverage=aorTalentCoverage();
  assert.equal(coverage.positions,360);
  assert.equal(coverage.mechanicalPositions,all.filter(x=>x.mechanicallyActive).length);
  assert.ok(coverage.mechanicalPositions>72);
  const names=new Set(all.map(x=>x.originalName));
  for(const [name,engineKey] of Object.entries(AOR_RUNTIME_TALENT_ALIASES)){
    assert.ok(names.has(name),name);
    assert.equal(typeof engineKey,'string');
    if(engineKey==='BRACE'){
      assert.ok(!AOR_RUNTIME_MECHANICAL_TALENTS.includes(name));
      assert.ok(all.filter(x=>x.originalName===name).every(x=>!x.mechanicallyActive));
    }else{
      assert.ok(AOR_RUNTIME_MECHANICAL_TALENTS.includes(name));
      assert.ok(all.filter(x=>x.originalName===name).every(x=>x.mechanicallyActive));
    }
  }
  const bracePositions=all.filter(x=>x.originalName==='FIRMEZA');
  assert.deepEqual(bracePositions.map(x=>x.id).sort(),[
    'ace:gunner:r2c2','ace:gunner:r4c3','soldier:sharpshooter:r1c3','soldier:sharpshooter:r3c1'
  ]);
  assert.deepEqual([...new Set(bracePositions.map(x=>x.source.printedPage))].sort((a,b)=>a-b),[68,93]);
  assert.ok(all.filter(x=>x.originalName==='DISPARO CERTERO').every(x=>x.mechanicallyActive)); // P137 True Aim
  assert.equal(Object.values(AOR_TALENT_TREES).reduce((n,s)=>n+Object.keys(s).length,0),18);
});

test('P132 AoR: piloto compra aceleração máxima e ativa opção já integrada ao motor',()=>{
  const {character}=buyFirst('ace','pilot','ACELERACIÓN MÁXIMA');
  assert.equal(talentRankByOriginal(character,'FULLTH'),1);
  assert.equal(fullThrottleTalentSpec(character).available,true);
  assert.equal(fullThrottleTalentSpec(character).bonus,1);
  assert.equal(talentRankByOriginal(create(),'FULLTH'),0);
});

test('P132 AoR: reação rápida altera iniciativa e consome tensão de verdade',()=>{
  const {character}=buyFirst('commander','commodore','REACCIÓN RÁPIDA');
  const before=character.derived.strain.current;
  assert.equal(quickReactionOption(character,1).boost,1);
  const result=preparePlayerInitiative(character,{quickReactionStrain:1});
  assert.equal(result.boost,1);
  assert.equal(result.character.derived.strain.current,before+1);
  assert.equal(character.derived.strain.current,before);
  assert.throws(()=>preparePlayerInitiative(character,{quickReactionStrain:2}),/no máximo/);
});

test('P132 AoR: perícia piloto consumado remove setback na resolução de pool',()=>{
  const {character}=buyFirst('ace','pilot','PILOTO CONSUMADO');
  const result=talentSkillPoolModifiers(character,'pilotar_vehiculo_espacial',{setback:2});
  assert.equal(result.setback,1);
  assert.equal(result.setbackRemoved,1);
});

test('P132 AoR: segundo fôlego, esquiva e recuperação rápida reutilizam motores existentes',()=>{
  const wind=buyFirst('engineer','saboteur','FUERZAS DE FLAQUEZA').character;
  assert.equal(secondWindOption(wind).recover,1);
  const dodge=buyFirst('spy','infiltrator','ESQUIVA').character;
  assert.equal(dodgeOption(dodge,1).difficultyUpgrades,1);
  const quick=buyFirst('spy','scout','RECUPERACIÓN RÁPIDA').character;
  assert.equal(talentRankByOriginal(quick,'RAPREC'),1);
  const damaged=clone(quick);damaged.derived.strain.current=4;
  const recovered=recoverAfterEncounter(damaged,{rng:()=>0.99});
  assert.equal(recovered.talentBonus,recovered.basis>0?1:0);
});

test('P132 AoR: Sharpshooter Firmeza é Brace, não Grit, e permanece íntegra após restauração JSON',()=>{
  // Official Age of Rebellion Core Rulebook, printed p.93: both FIRMEZA
  // positions are the active BRACE card (remove environmental setback from
  // the next action); Grit is a separate passive card on the same tree.
  const {character,node}=buyFirst('soldier','sharpshooter','FIRMEZA');
  assert.equal(node.id,'soldier:sharpshooter:r1c3');
  assert.equal(node.source.printedPage,93);
  assert.equal(character.derived.strain.threshold,create().derived.strain.threshold);
  const restored=JSON.parse(JSON.stringify(character));
  assert.equal(talentRankByOriginal(restored,'BRACE'),1);
  assert.equal(talentRankByOriginal(restored,'GRIT'),0);
  assert.equal(talentRankByOriginal(restored,'FIRMEZA'),1);
});

test('P132 AoR: Precisão letal exige escolha, persiste a perícia e calcula bônus real',()=>{
  let c=purchaseSpecialization(create(),'soldier','sharpshooter').character;
  const path=['soldier:sharpshooter:r1c2','soldier:sharpshooter:r2c2',
    'soldier:sharpshooter:r3c2','soldier:sharpshooter:r3c3'];
  for(const id of path) c=purchaseTalent(c,id).character;
  const target='soldier:sharpshooter:r2c3';
  assert.equal(eligibleTalents(c,{careerKey:'soldier',specializationKey:'sharpshooter'}).find(n=>n.id===target).needsChoice,true);
  const before=clone(c);
  assert.throws(()=>purchaseTalent(c,target),/escolher uma perícia/);
  assert.deepEqual(c,before);
  c=purchaseTalent(c,target,{skillKey:'armas_distancia_ligeras'}).character;
  c.skills.armas_distancia_ligeras=3;
  const restored=JSON.parse(JSON.stringify(c));
  assert.equal(talentRankByOriginal(restored,'DEADACC'),1);
  assert.deepEqual(deadlyAccuracySpec(restored),{available:true,skillKey:'armas_distancia_ligeras',bonus:3});
  assert.equal(restored.talentPurchases.at(-1).originalName,'PRECISIÓN LETAL');
});

test('P132 AoR: talentos não adquiridos ou sem mecânica não concedem bônus',()=>{
  const c=create();
  assert.equal(lethalBlowsCriticalModifier(c).modifier,0);
  assert.equal(naturalRerollOption(c,'medicina').available,false);
  assert.equal(deadlyAccuracySpec(c).available,false);
  assert.equal(talentRankByOriginal(c,'SECWIND'),0);
});

test('P132 AoR: compra pelo controlador persiste e desbloqueia ação real na UI Android',()=>{
  class Repo { constructor(){this.state=createCampaign('P132');} load(){return clone(this.state);} save(v){this.state=clone(v);} }
  const repo=new Repo();const game=new GameController(repo);
  game.setCampaignMode('AOR');game.setCharacter(create());
  const node=game.eligibleTalents().find(n=>n.originalName==='ACELERACIÓN MÁXIMA');
  assert.ok(node);
  game.buyTalent(node.id);
  const reopened=new GameController(repo);
  assert.equal(reopened.talentRank('FULLTH'),1);
  assert.equal(reopened.eligibleTalents().find(n=>n.id===node.id).purchased,true);
  const bridge=new BraseiroUiBridge({repository:repo});
  assert.equal(bridge.eligibleTalents().data.find(n=>n.id===node.id).mechanicallyActive,true);
  assert.equal(bridge.buyTalent(node.id).ok,false);
});

test('P132 UAE: efeitos diretos são atômicos e respeitam delta zero sem criar avanço falso',()=>{
  const adapter=getPublishedAdventureAdapter('under-black-sun');
  const runtime=new AdventureRuntime(publishedAdventureRuntimeModule(adapter.meta));
  const before=runtime.getState();
  assert.throws(()=>runtime.applyEffects([{type:'set-flag',flag:'partial'},
    {type:'adjust-counter',counter:'threat',delta:'bad'}]),/contador inválida/);
  assert.deepEqual(runtime.getState(),before);
  runtime.applyEffects([{type:'set-counter',counter:'threat',value:2},
    {type:'adjust-counter',counter:'threat',delta:0},
    {type:'advance-clock',clockId:'alertClock',delta:0},
    {type:'progress-objective',objectiveId:'episode-1',delta:0}]);
  const after=runtime.getState();
  assert.equal(after.counters.threat,2);
  assert.equal(after.clocks.alertClock.value,0);
  assert.equal(after.objectives['episode-1'].progress,0);
  assert.throws(()=>runtime.applyEffects({ 'set-counter': {counter:'threat',value:Infinity}}),/contador inválido/);
  assert.equal(runtime.getState().counters.threat,2);
});

for(const {id} of publishedAdventureCatalog()) {
  test(`P132 ${id}: integridade do journal e gatilhos preservada após persistir/restaurar`,()=>{
    const adapter=getPublishedAdventureAdapter(id),world=adapter.start({}).world;
    const module=publishedAdventureRuntimeModule(adapter.meta);
    const saved=JSON.parse(JSON.stringify(world));
    assert.deepEqual(publishedAdventureUaeSnapshot(saved,adapter),world.publishedAdventureRuntime);
    assert.equal(validatePublishedAdventureRuntimeState(module,saved.publishedAdventureRuntime,saved.publishedAdventure),true);
    const badTriggers=clone(saved);badTriggers.publishedAdventureRuntime.triggersFired['trigger-estrangeiro']=1;
    assert.throws(()=>publishedAdventureUaeSnapshot(badTriggers,adapter),/gatilho UAE desconhecido/);
    const overFired=clone(saved);
    overFired.publishedAdventureRuntime.triggersFired[module.triggers.find(t=>t.maxFires===1).id]=2;
    assert.throws(()=>publishedAdventureUaeSnapshot(overFired,adapter),/acima do limite/);
    const badCounter=clone(saved);badCounter.publishedAdventureRuntime.counters['threat']='NaN';
    assert.throws(()=>publishedAdventureUaeSnapshot(badCounter,adapter),/Contadores UAE malformados/);
    const badObjectives=clone(saved);badObjectives.publishedAdventureRuntime.objectives['gm-fabricated']={status:'ACTIVE',progress:0,target:1};
    assert.throws(()=>publishedAdventureUaeSnapshot(badObjectives,adapter),/não definido pela aventura/);
    const badConsequence=clone(saved);badConsequence.publishedAdventureRuntime.consequences=[{id:'broken',resolved:true}];
    assert.throws(()=>publishedAdventureUaeSnapshot(badConsequence,adapter),/consequência UAE malformado/);
    assert.deepEqual(saved,JSON.parse(JSON.stringify(world)));
  });
}
