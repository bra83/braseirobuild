/** P150 — source-aligned AoR defenses, durable Android bridge, causal receipt isolation. */
import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { createCampaign, validateState } from '../app/src/main/assets/vtt/js/core/state.mjs';
import { createAdversary } from '../app/src/main/assets/vtt/js/rules/adversaries.mjs';
import { AOR_TALENT_TREES } from '../app/src/main/assets/vtt/js/expansion/aorTalentTreesData.mjs';
import { aorTalentCoverage, aorTalentCatalog } from '../app/src/main/assets/vtt/js/expansion/aorProgressionIntegration.mjs';
import { aorCanonicalTalentName } from '../app/src/main/assets/vtt/js/expansion/aorTalentRuntimeAliases.mjs';
import { defensiveSlicingDefense, talentPilotedVehicleSilhouette, talentRankByOriginal } from '../app/src/main/assets/vtt/js/rules/talents.mjs';
import { vehicleAttackPool, createVehicleFromProfile } from '../app/src/main/assets/vtt/js/rules/vehicles.mjs';
import { AdventureRuntime } from '../app/src/main/assets/vtt/js/adventure/adventureRuntime.mjs';
import { publishedAdventureCatalog, getPublishedAdventureAdapter } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import { publishedAdventureRuntimeModule } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';
import { validateAdventureCheckpoint } from '../app/src/main/assets/vtt/js/adventure/adventureCheckpointGate.mjs';
const copy=structuredClone;
class Repo { constructor(id){this.saved=createCampaign(id);this.fail=false;this.writes=0;} load(){return copy(this.saved);} save(s){if(this.fail)throw Error('P150 disk failure');this.saved=copy(s);this.writes++;} }
function setup(id){const repo=new Repo(id);const game=new GameController(repo);game.loadStarterCharacter('cael');return {repo,game};}
const all=Object.values(AOR_TALENT_TREES).flatMap(s=>Object.values(s).flatMap(t=>t.nodes));
function withTalents(...entries){const c=setup('p150:talent').game.state.player.character;c.creation={...(c.creation||{}),rulesProfile:'AOR_CORE'};c.talentPurchases=[];c.talents=[];
  for(const [original,rank] of entries){const nodes=all.filter(n=>n.originalName===original).slice(0,rank);assert.equal(nodes.length,rank);for(const node of nodes){c.talentPurchases.push({nodeId:node.id,originalName:node.originalName,name:node.name,source:node.source});c.talents.push(node.name);}}
  return c;}

test('P150: sete posições AoR têm consumidores reais; nomes não equivalentes permanecem bloqueados',()=>{
  const coverage=aorTalentCoverage();assert.equal(coverage.positions,360);assert.ok(coverage.mechanicalPositions>=236);
  for(const [name,canonical,count] of [['PROGRAMACIÓN DEFENSIVA','DEFSLI',3],['PROGRAMACIÓN DEFENSIVA MEJORADA','DEFSLIIMP',1],['ESCURRIDIZO','TRICK',3]]){
    assert.equal(aorCanonicalTalentName(name),canonical);
    assert.equal(aorTalentCatalog().filter(t=>t.originalName===name&&t.mechanicallyActive).length,count);
    assert.equal(talentRankByOriginal(withTalents([name,count]),canonical),count);
  }
  assert.equal(aorTalentCatalog().filter(n=>n.originalName==='GUARDAESPALDAS'&&n.mechanicallyActive).length,2); // P170
});

test('P150: programação defensiva normal concede um contratempo por rank apenas ao defender computador próprio',()=>{
  const c=withTalents(['PROGRAMACIÓN DEFENSIVA',3]);
  assert.deepEqual(defensiveSlicingDefense(c,{defendingOwnedSystem:true}),{rank:3,improved:false,applies:true,setback:3,difficultyUpgrades:0});
  assert.deepEqual(defensiveSlicingDefense(c,{defendingOwnedSystem:false}),{rank:3,improved:false,applies:false,setback:0,difficultyUpgrades:0});
  assert.equal(defensiveSlicingDefense(withTalents(),{defendingOwnedSystem:true}).setback,0);
});

test('P150: versão aprimorada SUBSTITUI contratempo por upgrades de dificuldade',()=>{
  const c=withTalents(['PROGRAMACIÓN DEFENSIVA',3],['PROGRAMACIÓN DEFENSIVA MEJORADA',1]);
  assert.deepEqual(defensiveSlicingDefense(c,{defendingOwnedSystem:true}),{rank:3,improved:true,applies:true,setback:0,difficultyUpgrades:3});
  assert.equal(defensiveSlicingDefense(c,{defendingOwnedSystem:false}).difficultyUpgrades,0);
});

test('P150: invasão NPC gera pool correto e persiste, reabre sem rerrolar',()=>{
  const {repo,game}=setup('p150:intrusion');game.setCharacter(withTalents(['PROGRAMACIÓN DEFENSIVA',3]));
  const result=game.resolveIncomingComputerIntrusionCheck({actorCharacteristic:3,actorSkill:2,baseDifficulty:2,defendingOwnedSystem:true,rng:()=>.5});
  assert.equal(result.check.pool.proficiency,2);assert.equal(result.check.pool.setback,3);
  assert.equal(result.check.pool.difficulty,2);assert.equal(result.check.pool.challenge,0);
  assert.equal(result.type,'incoming-computer-intrusion');assert.equal(validateState(repo.saved),true);
  assert.equal(new GameController(repo).state.advancedChecks.last.type,'incoming-computer-intrusion');
  assert.equal(new GameController(repo).state.advancedChecks.history.at(-1).check.pool.setback,3);
});

test('P150: invasão aprimorada modifica dificuldade sem somar Contratempos',()=>{
  const {game}=setup('p150:intrusion-improved');game.setCharacter(withTalents(['PROGRAMACIÓN DEFENSIVA',3],['PROGRAMACIÓN DEFENSIVA MEJORADA',1]));
  const result=game.resolveIncomingComputerIntrusionCheck({actorCharacteristic:4,actorSkill:2,baseDifficulty:2,baseChallenge:0,setback:1,defendingOwnedSystem:true,rng:()=>.5});
  assert.equal(result.check.pool.setback,1);assert.equal(result.check.pool.challenge,2);assert.equal(result.check.pool.difficulty,1);
});

test('P150: adversário real usa Computadores, não os dados forjados do formulário',()=>{
  const {game}=setup('p150:adversary');game.setCharacter(withTalents(['PROGRAMACIÓN DEFENSIVA',1]));
  const a=createAdversary('imperial_officer');game.state.combat.adversaries=[a];
  const r=game.resolveIncomingComputerIntrusionCheck({actorAdversaryId:a.id,actorCharacteristic:99,actorSkill:99,defendingOwnedSystem:true,rng:()=>.5});
  assert.equal(r.check.pool.setback,1);assert.equal(r.actorName,a.name);
});

test('P150: entradas inválidas e sistema não possuído não gravam dados',()=>{
  const {repo,game}=setup('p150:invalid');game.setCharacter(withTalents(['PROGRAMACIÓN DEFENSIVA',3]));
  const before=copy(repo.saved),writes=repo.writes;
  for(const args of [{defendingOwnedSystem:false,actorCharacteristic:3,actorSkill:2},
    {actorCharacteristic:Infinity,actorSkill:2},{actorCharacteristic:3,actorSkill:-1},
    {actorCharacteristic:3,actorSkill:2,baseDifficulty:NaN},
    {actorCharacteristic:3,actorSkill:2,baseDifficulty:8,baseChallenge:5},
    {actorAdversaryId:'fake'}]) assert.throws(()=>game.resolveIncomingComputerIntrusionCheck(args));
  assert.deepEqual(repo.saved,before);assert.equal(repo.writes,writes);
});

test('P150: erro de armazenamento reverte integralmente o histórico da invasão',()=>{
  const {repo,game}=setup('p150:disk');game.setCharacter(withTalents(['PROGRAMACIÓN DEFENSIVA',3]));
  const before=copy(game.state);repo.fail=true;
  assert.throws(()=>game.resolveIncomingComputerIntrusionCheck({actorCharacteristic:3,actorSkill:2,defendingOwnedSystem:true,rng:()=>.5}),/disk failure/);
  assert.deepEqual(game.state,before);assert.deepEqual(repo.saved,before);
});

test('P150: Escurridizo só reduz silhueta ao receber ataque; não altera nave nem acumula',()=>{
  const pilot=withTalents(['ESCURRIDIZO',3]);const target=createVehicleFromProfile('xwing_t65b');const attacker=createVehicleFromProfile('tie_ln');
  const shooter=withTalents();shooter.skills.artilleria=2;
  const base=vehicleAttackPool({character:shooter,attacker,target,weaponIndex:0,targetRange:'Bocajarro',rng:()=>.5});
  const defended=vehicleAttackPool({character:shooter,attacker,target,targetOperator:pilot,weaponIndex:0,targetRange:'Bocajarro',rng:()=>.5});
  assert.equal(talentPilotedVehicleSilhouette(pilot,target.silhouette),target.silhouette-1);
  assert.equal(defended.targetSilhouette,base.targetSilhouette-1);assert.equal(target.silhouette,base.targetSilhouette);
  assert.equal(talentPilotedVehicleSilhouette(pilot,0),0);
  assert.equal(talentPilotedVehicleSilhouette(withTalents(),target.silhouette),target.silhouette);
});

test('P150: UI, bridge e bundle offline contêm as rotas novas; Java Kokoro intocado',()=>{
  const root=new URL('../',import.meta.url),read=path=>readFileSync(new URL(path,root),'utf8');
  assert.match(read('app/src/main/assets/vtt/r46/final-runtime.js'),/data-p150-hack/);
  assert.match(read('app/src/main/assets/vtt/r46/final-runtime.js'),/resolveIncomingComputerIntrusionCheck/);
  assert.match(read('app/src/main/assets/vtt/js/core/uiBridge.mjs'),/resolveIncomingComputerIntrusionCheck/);
  assert.match(read('app/src/main/assets/vtt/js/core/uiBridge.bundle.js'),/defensiveSlicingDefense/);
  assert.match(read('shell-r46-run87/gameBridgeClient.ts'),/resolveIncomingComputerIntrusionCheck/);
  assert.ok(Number(read('app/build.gradle.kts').match(/versionName = "1\.0\.(\d+)"/)?.[1])>=151);
});

for(const {id} of publishedAdventureCatalog()){
  const module=publishedAdventureRuntimeModule(getPublishedAdventureAdapter(id).meta);
  test(`P150 ${id}: prova do controlador é reservada; replay idempotente e 3 reaberturas`,()=>{
    let runtime=new AdventureRuntime(module);const before=runtime.serialize();
    assert.throws(()=>runtime.dispatchEvent('PUBLISHED_COMMAND_COMMITTED',{actionId:'forged'},'controller'),/reservada/);
    assert.throws(()=>runtime.dispatchEvent('PUBLISHED_COMMAND_COMMITTED',{actionId:'forged'},'adapter'),/reservada/);
    assert.throws(()=>runtime.commitAction(`p150:${id}:forged`,[{type:'event',eventType:'PUBLISHED_COMMAND_COMMITTED',source:'controller',payload:{}}]),/reservada/);
    assert.deepEqual(runtime.serialize(),before);
    const commands=[{type:'event',eventType:'P150_AUDIT',payload:{adventureId:id}}];
    assert.equal(runtime.commitAction(`p150:${id}:audit`,commands).applied,true);
    for(let n=0;n<3;n++){
      runtime=AdventureRuntime.restore(module,runtime.serialize());
      assert.equal(runtime.commitAction(`p150:${id}:audit`,commands).applied,false);
      assert.equal(validateAdventureCheckpoint(module,runtime.getState()),true);
    }
  });
  test(`P150 ${id}: START do controlador registra prova válida e reabre sem escrita`,()=>{
    const {repo,game}=setup(`p150:published:${id}`);
    const start=game.executePublishedAdventureCommand({actionId:`p150:${id}:start`,kind:'start',adventureId:id});
    assert.equal(start.applied,true);
    assert.equal(validateState(repo.saved),true);
    const writes=repo.writes,before=copy(repo.saved),reopened=new GameController(repo);
    assert.equal(reopened.executePublishedAdventureCommand({actionId:`p150:${id}:start`,kind:'start',adventureId:id}).duplicate,true);
    assert.equal(repo.writes,writes);assert.deepEqual(repo.saved,before);
  });
}

function vehicleGame(name,{talent=false,range='Bocajarro'}={}) {
  const {repo,game}=setup(name);
  if(talent) game.setCharacter(withTalents(['ESCURRIDIZO',1]));
  game.addVehicle('xwing_t65b');game.beginVehicleCombat({range});
  return {repo,game};
}
function seeded(seed){let state=seed;return ()=>{state=(state*1664525+1013904223)>>>0;return state/4294967296;};}

test('P150: ataque NPC usa defesa Escurridizo da nave do jogador e persiste dano/rolagem',()=>{
  const {repo,game}=vehicleGame('p150:vehicle-incoming',{talent:true});
  const active=game.state.vehicles.activeVehicleId;
  const original=game.state.vehicles.vehicles[active];
  const r=game.resolveIncomingVehicleAttack({npcAgility:3,npcGunnery:2,rng:()=>.2});
  assert.equal(r.attack.targetSilhouette,original.silhouette-1);
  assert.equal(r.attack.hit,true);
  assert.equal(r.vehicle.hullDamage,original.hullDamage+r.attack.targetDamage.suffered);
  assert.equal(repo.saved.vehicles.combat.npcAttackResolvedRound,1);
  assert.equal(repo.saved.vehicles.combat.lastIncomingAttack.targetSilhouette,original.silhouette-1);
  assert.equal(new GameController(repo).state.vehicles.vehicles[active].hullDamage,r.vehicle.hullDamage);
  assert.equal(validateState(repo.saved),true);
});

test('P150: talento veicular não é aplicado quando personagem não está pilotando',()=>{
  const {game}=vehicleGame('p150:vehicle-passenger',{talent:true});
  const target=game.activeVehicle();
  const r=game.resolveIncomingVehicleAttack({npcAgility:3,npcGunnery:2,playerPiloting:false,rng:()=>.2});
  assert.equal(r.attack.targetSilhouette,target.silhouette);
});

test('P150: ataque NPC não consome ação do jogador e só ocorre uma vez por rodada',()=>{
  const {repo,game}=vehicleGame('p150:vehicle-once');
  const first=game.resolveIncomingVehicleAttack({rng:()=>.2});
  assert.equal(first.attack.hit,true);assert.equal(game.state.vehicles.combat.actionUsed,false);
  const before=copy(repo.saved),writes=repo.writes;
  assert.throws(()=>game.resolveIncomingVehicleAttack({rng:()=>.2}),/já atacou/);
  assert.deepEqual(repo.saved,before);assert.equal(repo.writes,writes);
  game.nextVehicleRound();assert.equal(game.state.vehicles.combat.round,2);
  assert.ok(game.resolveIncomingVehicleAttack({rng:()=>.2}).attack);
});

test('P150: ataque NPC inválido, fora de alcance e sem combate não alteram a campanha',()=>{
  const {repo,game}=vehicleGame('p150:vehicle-invalid',{range:'Curto'});
  const before=copy(repo.saved),writes=repo.writes;
  for(const args of [{},{npcAgility:NaN},{npcAgility:7},{npcGunnery:-1},
    {weaponIndex:42},{facing:'diagonal'},{playerPiloting:'yes'}])
    assert.throws(()=>game.resolveIncomingVehicleAttack(args));
  assert.deepEqual(repo.saved,before);assert.equal(repo.writes,writes);
});

test('P150: falha de armazenamento reverte ataque NPC, dano, munição e turno',()=>{
  const {repo,game}=vehicleGame('p150:vehicle-disk',{talent:true});
  const before=copy(game.state),writes=repo.writes;
  repo.fail=true;assert.throws(()=>game.resolveIncomingVehicleAttack({rng:()=>.2}),/disk failure/);
  assert.deepEqual(game.state,before);assert.deepEqual(repo.saved,before);assert.equal(repo.writes,writes);
});

test('P150: crítico inimigo consome resultado real, permite escolha de componente e não duplica',()=>{
  const {repo,game}=vehicleGame('p150:vehicle-critical');
  const attack=game.resolveIncomingVehicleAttack({rng:seeded(6)});
  assert.equal(attack.attack.criticalAvailable,true);
  assert.ok(attack.pendingCritical);
  const first=game.activateIncomingVehicleCritical({rng:()=>.5});
  assert.equal(first.activated,false);assert.equal(first.componentRequired,true);
  assert.ok(first.componentChoices.length);
  const raw=repo.saved.vehicles.combat.pendingIncomingCritical.rawRoll;
  assert.equal(raw,51);
  const applied=game.activateIncomingVehicleCritical({component:first.componentChoices[0],rng:()=>.99});
  assert.equal(applied.activated,true);
  assert.equal(repo.saved.vehicles.combat.pendingIncomingCritical,null);
  assert.ok(game.activeVehicle().criticalHits.length>=1);
  const before=copy(repo.saved),writes=repo.writes;
  assert.throws(()=>game.activateIncomingVehicleCritical({rng:()=>.4}),/Nenhum crítico/);
  assert.deepEqual(repo.saved,before);assert.equal(repo.writes,writes);
  assert.equal(validateState(repo.saved),true);
});

test('P150: crítico inimigo pode ser dispensado sem rolar nem aplicar dano adicional',()=>{
  const {repo,game}=vehicleGame('p150:vehicle-decline');
  game.resolveIncomingVehicleAttack({rng:seeded(6)});
  const beforeDamage=game.activeVehicle().hullDamage;
  assert.equal(game.declineIncomingVehicleCritical().declined,true);
  assert.equal(game.activeVehicle().hullDamage,beforeDamage);
  assert.equal(repo.saved.vehicles.combat.pendingIncomingCritical,null);
  assert.throws(()=>game.declineIncomingVehicleCritical(),/Nenhum crítico/);
});

test('P150: interface expõe combate veicular inimigo, custos e escolha de crítico',()=>{
  const root=new URL('../',import.meta.url),read=path=>readFileSync(new URL(path,root),'utf8');
  const ui=read('app/src/main/assets/vtt/r46/final-runtime.js');
  for(const term of ['data-p150-vincoming','data-p150-vnpc-agi','data-p150-vnpc-gun',
    'data-p150-crit','data-p150-crit-decline','activateIncomingVehicleCritical'])
    assert.ok(ui.includes(term),term);
  for(const term of ['resolveIncomingVehicleAttack','activateIncomingVehicleCritical','declineIncomingVehicleCritical'])
    assert.ok(read('app/src/main/assets/vtt/js/core/uiBridge.bundle.js').includes(term),term);
});
