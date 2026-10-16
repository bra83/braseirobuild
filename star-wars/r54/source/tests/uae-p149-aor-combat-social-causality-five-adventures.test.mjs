/** P149 — AoR active talents, Android bridge, five adventure journal invariants. */
import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { createCampaign, validateState } from '../app/src/main/assets/vtt/js/core/state.mjs';
import { createAdversary } from '../app/src/main/assets/vtt/js/rules/adversaries.mjs';
import { statusModifiers } from '../app/src/main/assets/vtt/js/rules/statusEffects.mjs';
import { AOR_TALENT_TREES } from '../app/src/main/assets/vtt/js/expansion/aorTalentTreesData.mjs';
import { aorTalentCoverage, aorTalentCatalog } from '../app/src/main/assets/vtt/js/expansion/aorProgressionIntegration.mjs';
import { aorCanonicalTalentName } from '../app/src/main/assets/vtt/js/expansion/aorTalentRuntimeAliases.mjs';
import { nobodyFoolDefense, disorientTalentOption, talentRankByOriginal } from '../app/src/main/assets/vtt/js/rules/talents.mjs';
import { AdventureRuntime } from '../app/src/main/assets/vtt/js/adventure/adventureRuntime.mjs';
import { validateAdventureCheckpoint } from '../app/src/main/assets/vtt/js/adventure/adventureCheckpointGate.mjs';
import { validateAdventureSemantics } from '../app/src/main/assets/vtt/js/adventure/adventureSemanticGate.mjs';
import { getPublishedAdventureAdapter, publishedAdventureCatalog } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import { publishedAdventureRuntimeModule } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';

const copy=structuredClone;
class Repo {
  constructor(key) { this.saved=createCampaign(key);this.writes=0;this.fail=false; }
  load() {return copy(this.saved);}
  save(value) {if(this.fail) throw Error('P149 simulated storage failure');this.saved=copy(value);this.writes++;}
}
function makeGame(key) {const repo=new Repo(key);const g=new GameController(repo);g.loadStarterCharacter('tendaar');return {g,repo};}
function withTalent(name,rank=1) {
  const nodes=Object.values(AOR_TALENT_TREES).flatMap(specs=>Object.values(specs).flatMap(tree=>tree.nodes))
    .filter(n=>n.originalName===name).slice(0,rank);
  assert.equal(nodes.length,rank,`AoR source slot ${name}`);
  const c=makeGame(`p149:${name}:${rank}`).g.state.player.character;
  c.creation={...(c.creation??{}),rulesProfile:'AOR_CORE'};
  c.talentPurchases=nodes.map(n=>({nodeId:n.id,originalName:n.originalName,name:n.name,source:n.source,cost:n.cost}));
  c.talents.push(...nodes.map(n=>n.name));
  return c;
}

test('P149: 5 source-backed AoR slots have active consumers; all others stay fail-closed',()=>{
  const coverage=aorTalentCoverage();
  assert.equal(coverage.positions,360);
  assert.ok(coverage.mechanicalPositions>=229);
  for(const [name,canonical,count] of [['NI UN PELO DE TONTO','NOBODYSFOOL',3],['DESORIENTAR','DISOR',2]]) {
    assert.equal(aorCanonicalTalentName(name),canonical);
    assert.equal(aorTalentCatalog().filter(x=>x.originalName===name&&x.mechanicallyActive).length,count);
    assert.equal(talentRankByOriginal(withTalent(name,count),canonical),count);
  }
  assert.equal(aorCanonicalTalentName('GUARDAESPALDAS'),'BOD'); // P170 implementation
});

test('P149: Nobody\'s Fool upgrades only incoming social checks, never outgoing or unrelated skills',()=>{
  const c=withTalent('NI UN PELO DE TONTO',3);
  for(const skill of ['carisma','coaccion','engano'])
    assert.deepEqual(nobodyFoolDefense(c,skill),{available:true,rank:3,difficultyUpgrades:3,skillKey:skill});
  for(const skill of ['liderazgo','negociacion','computadoras'])
    assert.equal(nobodyFoolDefense(c,skill).difficultyUpgrades,0);
  assert.equal(nobodyFoolDefense(makeGame('p149:none').g.state.player.character,'carisma').rank,0);
});

test('P149: incoming social check uses NPC positive dice and player negative pool; saved/reopened',()=>{
  const {g,repo}=makeGame('p149:incoming');
  const c=withTalent('NI UN PELO DE TONTO',3);g.setCharacter(c);
  const result=g.resolveIncomingSocialInfluenceCheck({skillKey:'engano',actorCharacteristic:3,actorSkill:2,rng:()=>0.5});
  assert.equal(result.talent.rank,3);
  assert.equal(result.check.pool.proficiency,2);
  assert.equal(result.check.pool.challenge+result.check.pool.difficulty,result.opposition.total);
  assert.equal(result.check.pool.challenge,Math.min(result.opposition.total,result.opposition.upgrades+3));
  assert.equal(validateState(repo.saved),true);
  assert.equal(new GameController(repo).state.social.history.at(-1)?.type,'incoming-social');
});

test('P149: incoming social rejects forged skill/defense/NPC and does not persist invalid requests',()=>{
  const {g,repo}=makeGame('p149:incoming-invalid');g.setCharacter(withTalent('NI UN PELO DE TONTO'));
  const before=copy(repo.saved),writes=repo.writes;
  assert.throws(()=>g.resolveIncomingSocialInfluenceCheck({skillKey:'mecanica',actorCharacteristic:3,actorSkill:2}),/social/i);
  assert.throws(()=>g.resolveIncomingSocialInfluenceCheck({skillKey:'carisma',opponentSkillKey:'disciplina',actorCharacteristic:3,actorSkill:2}),/defensiva/i);
  assert.throws(()=>g.resolveIncomingSocialInfluenceCheck({skillKey:'carisma',actorAdversaryId:'forged'}),/não encontrado/i);
  assert.deepEqual(repo.saved,before);assert.equal(repo.writes,writes);
});

test('P149: incoming social storage failure is atomic',()=>{
  const {g,repo}=makeGame('p149:incoming-failure');g.setCharacter(withTalent('NI UN PELO DE TONTO'));
  const before=copy(g.state);repo.fail=true;
  assert.throws(()=>g.resolveIncomingSocialInfluenceCheck({skillKey:'coaccion',actorCharacteristic:3,actorSkill:2,rng:()=>0.5}),/storage failure/);
  assert.deepEqual(g.state,before);assert.deepEqual(repo.saved,before);
});

test('P149: Disorient option is contextual, rank-bound and rejects invalid budgets',()=>{
  const c=withTalent('DESORIENTAR',2);
  assert.equal(disorientTalentOption(c,{hit:true,advantage:2}).rounds,2);
  assert.equal(disorientTalentOption(c,{hit:false,advantage:3}).available,false);
  assert.equal(disorientTalentOption(c,{hit:true,advantage:1}).available,false);
  assert.throws(()=>disorientTalentOption(c,{hit:true,advantage:Infinity}),/inválidas/);
});

function prepareDisorient({rank=2,advantage=4,hit=true}={}) {
  const {g,repo}=makeGame(`p149:disor:${rank}:${advantage}:${hit}`);
  g.setCharacter(withTalent('DESORIENTAR',rank));
  const started=g.beginEncounter({adversaries:[createAdversary('imperial_officer')],rng:()=>0});
  const target=started.combat.adversaries[0];
  g.state.combat.pendingPlayerSpend={attackId:'p149-attack',adversaryId:target.id,weaponIndex:0,weapon:'P149',
    remainingAdvantage:advantage,remainingTriumph:0,qualities:[],activations:{},
    attackSnapshot:{hit,damage:4,pierce:0,damageType:'wounds',baseDamage:4,netSuccess:hit?1:0}};
  return {g,repo,target};
}

test('P149: Disorient consumes the same advantage budget, persists status and prevents double activation',()=>{
  const {g,repo,target}=prepareDisorient();
  const effect=g.activatePendingDisorientTalent();
  assert.equal(effect.rounds,2);assert.equal(effect.remainingAdvantage,2);
  const persisted=repo.saved.combat.adversaries.find(a=>a.id===target.id);
  assert.equal(persisted.statusEffects.find(x=>x.type==='disoriented').roundsRemaining,2);
  assert.equal(statusModifiers(persisted).setback,1);
  assert.equal(new GameController(repo).state.combat.pendingPlayerSpend.remainingAdvantage,2);
  const before=copy(repo.saved),writes=repo.writes;
  assert.throws(()=>g.activatePendingDisorientTalent(),/já foi ativado/);
  assert.deepEqual(repo.saved,before);assert.equal(repo.writes,writes);
});

test('P149: Disorient expires on real NPC turns',()=>{
  const {g,target}=prepareDisorient();g.activatePendingDisorientTalent();
  const turn=g._resolveNpcTurn(g.state.combat,g.state.player.character,target.id,()=>0.2);
  assert.ok(turn.attack.check.pool.setback>=1);
  const after=turn.combat.adversaries.find(a=>a.id===target.id);
  assert.equal(after.statusEffects.find(x=>x.type==='disoriented').roundsRemaining,1);
});

test('P149: Disorient rejects miss, low advantage, missing talent and incapacitated target',()=>{
  for(const opts of [{hit:false,advantage:4},{hit:true,advantage:1}]) {
    const {g,repo}=prepareDisorient(opts);const before=copy(repo.saved),writes=repo.writes;
    assert.throws(()=>g.activatePendingDisorientTalent(),/bem-sucedido|insuficientes/);
    assert.deepEqual(repo.saved,before);assert.equal(repo.writes,writes);
  }
  const {g}=prepareDisorient();const c=copy(g.state.player.character);c.talentPurchases=[];c.talents=[];g.setCharacter(c);
  assert.throws(()=>g.activatePendingDisorientTalent(),/não foi adquirido/);
});

test('P149: Disorient storage failure rolls back target, advantage and log',()=>{
  const {g,repo}=prepareDisorient();const before=copy(g.state);
  repo.fail=true;assert.throws(()=>g.activatePendingDisorientTalent(),/storage failure/);
  assert.deepEqual(g.state,before);assert.equal(repo.saved.combat.pendingPlayerSpend,undefined);
});

for(const {id} of publishedAdventureCatalog()) {
  const module=publishedAdventureRuntimeModule(getPublishedAdventureAdapter(id).meta);
  test(`P149 ${id}: duplicate/overlapping UAE action receipts rejected on import`,()=>{
    const runtime=new AdventureRuntime(module);
    runtime.commitAction(`p149:${id}:a`,[{type:'event',eventType:'P149_A',payload:{a:1}}]);
    runtime.commitAction(`p149:${id}:b`,[{type:'event',eventType:'P149_B',payload:{b:1}}]);
    const original=runtime.getState();assert.equal(validateAdventureCheckpoint(module,original),true);
    const tampered=copy(original);
    const receipts=tampered.canonicalEvents.filter(e=>e.type==='ADVENTURE_ACTION_COMMITTED');
    receipts[1].payload.actionId=receipts[0].payload.actionId;
    receipts[1].idempotencyKey=receipts[0].idempotencyKey;
    assert.throws(()=>AdventureRuntime.restore(module,tampered),/duplicado|causal/i);
    const overlap=copy(original);
    const [first,second]=overlap.canonicalEvents.filter(e=>e.type==='ADVENTURE_ACTION_COMMITTED');
    second.payload.firstSequence=first.payload.firstSequence;
    second.payload.appliedEventIds=[...first.payload.appliedEventIds,first.id,...second.payload.appliedEventIds];
    assert.throws(()=>AdventureRuntime.restore(module,overlap),/sobrepostas|causal/i);
    assert.deepEqual(runtime.getState(),original);
  });
  test(`P149 ${id}: two durable commands, three reopens and no double rewards`,()=>{
    const runtime=new AdventureRuntime(module);
    const key=`p149:${id}:persist`;
    const commands=[{type:'effects',effects:[{type:'set-flag',flag:'p149Persisted',value:true}]},
      {type:'event',eventType:'P149_PERSIST',payload:{id}}];
    assert.equal(runtime.commitAction(key,commands).applied,true);
    let restored=runtime;
    for(let n=0;n<3;n++) {
      restored=AdventureRuntime.restore(module,JSON.parse(restored.serialize()));
      assert.equal(restored.commitAction(key,commands).applied,false);
      assert.equal(restored.getState().flags.p149Persisted,true);
      assert.equal(validateAdventureCheckpoint(module,restored.getState()),true);
    }
    assert.equal(restored.getState().canonicalEvents.filter(e=>e.type==='ADVENTURE_ACTION_COMMITTED').length,1);
  });
  test(`P149 ${id}: authored check schema invalid fails preflight`,()=>{
    const invalid=copy(module);
    invalid.nodes[invalid.startNodeId].checks=[{skill:'',difficulty:'Média',purpose:'Teste'}];
    assert.throws(()=>validateAdventureSemantics(invalid),/checks\[0\]/);
    invalid.nodes[invalid.startNodeId].checks={skill:'Manha'};
    assert.throws(()=>validateAdventureSemantics(invalid),/deve ser lista/);
  });
}

test('P149: Android bridge and TypeScript contract expose both new mechanical actions',()=>{
  const bridge=readFileSync(new URL('../app/src/main/assets/vtt/js/core/uiBridge.mjs',import.meta.url),'utf8');
  const contract=readFileSync(new URL('../gameBridgeClient.ts',import.meta.url),'utf8');
  for(const method of ['activatePendingDisorientTalent','resolveIncomingSocialInfluenceCheck']) {
    assert.match(bridge,new RegExp(`${method}\\(`));
    assert.match(contract,new RegExp(`${method}\\?`));
  }
});

test('P149: R46 combat and social panels expose real buttons wired to bridge actions',()=>{
  const shell=readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');
  for(const token of ['data-p149-disorient','data-p149-incoming','activatePendingDisorientTalent','resolveIncomingSocialInfluenceCheck'])
    assert.ok(shell.includes(token),token);
  assert.match(shell,/remainingAdvantage\|\|0\)<2/);
  assert.match(shell,/spend\.attackSnapshot\?\.hit/);
});

test('P149: Android bridge actually invokes incoming social talent and persists result',async()=>{
  const {BraseiroUiBridge}=await import('../app/src/main/assets/vtt/js/core/uiBridge.mjs');
  const {g,repo}=makeGame('p149:bridge-social');g.setCharacter(withTalent('NI UN PELO DE TONTO',2));
  const bridge=new BraseiroUiBridge({repository:repo,storage:null});
  const result=bridge.resolveIncomingSocialInfluenceCheck({skillKey:'carisma',actorCharacteristic:3,actorSkill:2,rng:()=>0.5});
  assert.equal(result.ok,true,result.message);
  assert.equal(result.data.talent.rank,2);
  assert.equal(new GameController(repo).state.social.lastInfluence.type,'incoming-social');
});

test('P149: Android bridge actually activates Disorient on pending attack and survives reopening',async()=>{
  const {BraseiroUiBridge}=await import('../app/src/main/assets/vtt/js/core/uiBridge.mjs');
  const {g,repo,target}=prepareDisorient({rank:2,advantage:3});
  repo.save(g.state);
  const bridge=new BraseiroUiBridge({repository:repo,storage:null});
  const result=bridge.activatePendingDisorientTalent();
  assert.equal(result.ok,true,result.message);
  assert.equal(result.data.remainingAdvantage,1);
  const reopened=new GameController(repo);
  assert.equal(reopened.state.combat.pendingPlayerSpend.remainingAdvantage,1);
  assert.equal(reopened.state.combat.adversaries.find(a=>a.id===target.id).statusEffects[0].type,'disoriented');
});
