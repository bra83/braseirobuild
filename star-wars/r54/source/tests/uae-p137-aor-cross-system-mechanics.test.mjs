import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {AOR_TALENT_TREES} from '../app/src/main/assets/vtt/js/expansion/aorTalentTreesData.mjs';
import {aorTalentCoverage,aorTalentCatalog} from '../app/src/main/assets/vtt/js/expansion/aorProgressionIntegration.mjs';
import {aorCanonicalTalentName} from '../app/src/main/assets/vtt/js/expansion/aorTalentRuntimeAliases.mjs';
import {createAorCoreCharacter} from '../app/src/main/assets/vtt/js/rules/characterCreation.mjs';
import {purchaseSpecialization} from '../app/src/main/assets/vtt/js/rules/progression.mjs';
import {eligibleTalents,purchaseTalent,talentRankByOriginal,talentSkillPoolModifiers,trueAimOption,durableCriticalModifier,confidenceFearDifficultyReduction,stimpackSpecializationBonus} from '../app/src/main/assets/vtt/js/rules/talents.mjs';
import {addStarterCritical} from '../app/src/main/assets/vtt/js/rules/critical.mjs';
import {resolveFear,rollFearCheck} from '../app/src/main/assets/vtt/js/rules/fear.mjs';
import {useStimpack} from '../app/src/main/assets/vtt/js/rules/health.mjs';
import {applyDamageControl,applyManualRepairs,createVehicleFromProfile} from '../app/src/main/assets/vtt/js/rules/vehicles.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {publishedAdventureCatalog,getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {publishedAdventureRuntimeModule,validatePublishedAdventureRuntimeState} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';
import {AdventureRuntime} from '../app/src/main/assets/vtt/js/adventure/adventureRuntime.mjs';

const aliases={
 'REPARACIÓN EFICAZ':'SOLREP','ROBUSTO':'DURA','CONFIANZA':'CONF',
 'DISPARO CERTERO':'TRUEAIM','ELUDIR SEGURIDAD':'BYP',
 'ESPECIALISTA EN ESTIMULANTES':'STIMSPEC'
};
const positions={
 'REPARACIÓN EFICAZ':7,'ROBUSTO':4,'CONFIANZA':6,
 'DISPARO CERTERO':4,'ELUDIR SEGURIDAD':3,
 'ESPECIALISTA EN ESTIMULANTES':3
};
function create(){
 const c=createAorCoreCharacter({name:'Piloto P137',speciesKey:'human',careerKey:'ace',specializationKey:'pilot',partySize:1,
 careerFreeSkills:['astronavegacion','frialdad','artilleria','mecanica'],specializationFreeSkills:['astronavegacion','artilleria'],
 speciesChoiceSkills:['atletismo','medicina'],motivation:'Liberdade',dutyType:'Superioridade espacial'});
 c.xp.available=4000;c.xp.total=4000;
 return c;
}
// Buy the real connected path, never mint a synthetic talent grant.
function buy(career,spec,originalName,ordinal=0,initial=null){
 let c=initial??create();if(!initial && (career!=='ace'||spec!=='pilot'))c=purchaseSpecialization(c,career,spec).character;
 const tree=AOR_TALENT_TREES[career][spec],target=tree.nodes.filter(n=>n.originalName===originalName)[ordinal];
 assert.ok(target,`${career}/${spec}/${originalName}#${ordinal}`);
 const neighbors=new Map(tree.nodes.map(n=>[n.id,[]]));for(const [a,b] of tree.edges){neighbors.get(a).push(b);neighbors.get(b).push(a);}
 const queue=tree.nodes.filter(n=>n.row===1).map(n=>[n.id]);const visited=new Set();let path;
 while(queue.length){const p=queue.shift(),id=p.at(-1);if(visited.has(id))continue;visited.add(id);if(id===target.id){path=p;break;}for(const n of neighbors.get(id))if(!visited.has(n))queue.push([...p,n]);}
 assert.ok(path,`Path to ${target.id}`);
 for(const id of path){const node=eligibleTalents(c,{careerKey:career,specializationKey:spec}).find(n=>n.id===id);
  assert.equal(node.unlocked,true,`path locked ${id}`);
  if(node.purchased)continue;
  const options={careerKey:career,specializationKey:spec};
  if(node.choiceType==='characteristic')options.characteristicKey='intelecto';
  if(node.choiceType==='combatSkill')options.skillKey='armas_distancia_ligeras';
  c=purchaseTalent(c,id,options).character;
 }
 return {character:c,target};
}
class Repo{constructor(){this.state=createCampaign('P137')}load(){return structuredClone(this.state)}save(s){this.state=structuredClone(s)}}
function game(character){const repo=new Repo(),g=new GameController(repo);g.setCharacter(character);return {g,repo};}

// Real source tree coverage is additive, with a fail-closed boundary for unimplemented names.
test('P137 six AoR talent families remain active with at least 205/360 source-backed positions',()=>{
 const all=aorTalentCatalog(),coverage=aorTalentCoverage();assert.equal(coverage.positions,360);assert.ok(coverage.mechanicalPositions>=205);
 for(const [name,key] of Object.entries(aliases)){
  assert.equal(aorCanonicalTalentName(name),key);
  const slots=all.filter(n=>n.originalName===name);
  assert.equal(slots.length,positions[name]);assert.ok(slots.every(n=>n.mechanicallyActive&&n.purchaseActive));
 }
 // P170: the previously inactive Body Guard now has an authenticated consumer.
 assert.equal(aorCanonicalTalentName('GUARDAESPALDAS'),'BOD');
 assert.equal(all.filter(n=>n.originalName==='GUARDAESPALDAS'&&n.mechanicallyActive).length,2);
});
for(const [career,spec,name,canonical] of [
 ['engineer','mechanic','REPARACIÓN EFICAZ','SOLREP'],
 ['ace','gunner','ROBUSTO','DURA'],
 ['diplomat','ambassador','CONFIANZA','CONF'],
 ['ace','gunner','DISPARO CERTERO','TRUEAIM'],
 ['spy','slicer','ELUDIR SEGURIDAD','BYP'],
 ['soldier','medic','ESPECIALISTA EN ESTIMULANTES','STIMSPEC']
]){
 test(`P137 ${name} bought through actual tree: rank, XP, immutability and JSON persistence`,()=>{
  const before=create(),{character,target}=buy(career,spec,name);
  assert.equal(talentRankByOriginal(character,canonical),1);
  assert.ok(character.talentPurchases.some(n=>n.nodeId===target.id));
  assert.ok(character.xp.available<before.xp.available);
  assert.equal(talentRankByOriginal(JSON.parse(JSON.stringify(character)),canonical),1);
  assert.equal(talentRankByOriginal(before,canonical),0);
 });
}
test('P137 ranked SOLREP repairs hull, never system strain, and persists vehicle result',()=>{
 const first=buy('engineer','mechanic','REPARACIÓN EFICAZ').character;
 const {character}=buy('engineer','mechanic','REPARACIÓN EFICAZ',1,first);
 assert.equal(talentRankByOriginal(character,'SOLREP'),2);
 const vehicle=createVehicleFromProfile('yt1300');
 vehicle.hullDamage=9;vehicle.systemStrain=7;
 const hull=applyDamageControl(vehicle,{successes:1,operator:character,mode:'hull'});
 assert.equal(hull.hullRecovered,3);assert.equal(hull.vehicle.systemStrain,7);
 const system=applyDamageControl(vehicle,{successes:1,operator:character,mode:'system'});
 assert.equal(system.systemStrainRecovered,1);assert.equal(system.vehicle.hullDamage,9);
 const manual=applyManualRepairs(vehicle,{successes:3,operator:character});
 assert.equal(manual.hullRecovered,4); // 2 base + 2 SOLREP
 assert.equal(applyManualRepairs(vehicle,{successes:0,operator:character}).hullRecovered,0);
 assert.equal(applyManualRepairs(vehicle,{successes:3}).hullRecovered,2);
});
test('P137 ROBUSTO lowers incoming critical result 10 per rank, minimum 1, without mutating input',()=>{
 const {character}=buy('ace','gunner','ROBUSTO');
 assert.equal(durableCriticalModifier(character),-10);
 const before=structuredClone(character);
 const result=addStarterCritical(character,'teste',{roll:50});
 assert.equal(result.total,40);assert.equal(result.injury.durableModifier,-10);
 assert.deepEqual(character,before);
 const min=addStarterCritical(character,'teste',{roll:1});assert.equal(min.total,1);assert.equal(min.injury.code,'minor_nick');
 const baseline=addStarterCritical(create(),'teste',{roll:50});assert.equal(baseline.total,50);
});
test('P137 CONFIANZA reduces Discipline fear, never Cool, never below difficulty 1',()=>{
 const first=buy('diplomat','ambassador','CONFIANZA').character;
 const {character}=buy('diplomat','ambassador','CONFIANZA',1,first);
 assert.equal(talentRankByOriginal(character,'CONF'),2);
 assert.equal(confidenceFearDifficultyReduction(character,'disciplina'),2);
 assert.equal(confidenceFearDifficultyReduction(character,'frialdad'),0);
 const discipline=resolveFear(character,{level:'mortal',rng:()=>0.4});
 const cool=resolveFear(character,{level:'mortal',prepared:true,rng:()=>0.4});
 assert.equal(discipline.difficulty,2);assert.equal(discipline.fearTalentReduction,2);
 assert.equal(cool.difficulty,4);assert.equal(cool.fearTalentReduction,0);
 const low=rollFearCheck(character,{difficulty:1,rng:()=>0.4});
 assert.equal(low.pool.difficulty+low.pool.challenge,1);
});
test('P137 BYP removes setback only for security bypass Computers/Skulduggery checks',()=>{
 const first=buy('spy','slicer','ELUDIR SEGURIDAD').character;
 const second=buy('spy','slicer','ELUDIR SEGURIDAD',1,first).character;
 const {character}=buy('spy','slicer','ELUDIR SEGURIDAD',2,second);
 assert.equal(talentRankByOriginal(character,'BYP'),3);
 assert.equal(talentSkillPoolModifiers(character,'computadoras',{setback:4,securityBypass:true}).setback,1);
 assert.equal(talentSkillPoolModifiers(character,'actividad_criminal',{setback:2,securityBypass:true}).setback,0);
 assert.equal(talentSkillPoolModifiers(character,'computadoras',{setback:4}).setback,4);
 assert.equal(talentSkillPoolModifiers(character,'medicina',{setback:4,securityBypass:true}).setback,4);
 const {g,repo}=game(character);
 const r=g.resolveSkillCheck({skillKey:'computadoras',difficulty:2,setback:3,securityBypass:true,rng:()=>0.4});
 assert.equal(r.pool.setback,0);
 const reopened=new GameController(repo);
 assert.equal(talentRankByOriginal(reopened.state.player.character,'BYP'),3);
});
test('P137 STIMSPEC increases healing and retains daily diminishing returns and item consumption',()=>{
 const firstOwned=buy('soldier','medic','ESPECIALISTA EN ESTIMULANTES').character;
 const {character}=buy('soldier','medic','ESPECIALISTA EN ESTIMULANTES',1,firstOwned);
 assert.equal(stimpackSpecializationBonus(character),2);
 character.gear.push({id:'stimpack_injector',name:'Injetor de estimulantes',quantity:8});
 character.derived.wounds.current=20;
 const first=useStimpack(character);
 assert.equal(first.healingValue,7);assert.equal(first.healed,7);assert.equal(first.remaining,7);
 let c=first.character; c.derived.wounds.current=20;
 for(let i=0;i<4;i++){const r=useStimpack(c);c=r.character;c.derived.wounds.current=20;}
 const sixth=useStimpack(c);assert.equal(sixth.healingValue,2);
 assert.equal(sixth.remaining,2);
 assert.equal(useStimpack(createWithStimpack()).healingValue,5);
});
function createWithStimpack(){const c=create();c.gear.push({id:'stimpack_injector',name:'Injetor de estimulantes',quantity:2});c.derived.wounds.current=9;return c;}
test('P137 TRUEAIM consumes one real maneuver and upgrades ranged pool; once per round and persistent',()=>{
 const {character}=buy('ace','gunner','DISPARO CERTERO');
 assert.equal(trueAimOption(character).rank,1);
 character.weapons=[{id:'p137-blaster',name:'Blaster',skill:'armas_distancia_ligeras',damage:6,crit:3,range:'Curto',encumbrance:0,qualities:[],active:true,equipped:true}];
 character.skills.armas_distancia_ligeras=2;
 const {g,repo}=game(character);g.beginCombat();
 const before=structuredClone(g.state.combat);
 const aim=g.aim({mode:'true_aim'});
 assert.equal(aim.combat.pendingAim.mode,'true_aim');assert.equal(aim.combat.playerTurn.maneuversUsed,1);
 assert.equal(aim.combat.pendingAttackBoost,1);assert.equal(aim.combat.playerTurn.trueAimUsed,true);
 assert.throws(()=>g.aim({mode:'true_aim',maneuverMethod:'strain'}),/uma vez por rodada/);
 const reopened=new GameController(repo);
 assert.equal(reopened.state.combat.pendingAim.mode,'true_aim');
 const attack=reopened.resolveWeaponAttack({weaponIndex:0,targetRange:'Curto',rng:()=>0.4});
 assert.equal(reopened.state.lastCheck.trueAimUpgrades,1);
 assert.equal(reopened.state.lastCheck.pool.proficiency,2);
 assert.equal(reopened.state.lastCheck.pool.ability,1);
 assert.equal(reopened.state.lastCheck.pool.boost,1);
 assert.equal(reopened.state.combat.pendingAim,undefined);
 assert.equal(reopened.state.combat.playerTurn.trueAimUsed,true);
 assert.throws(()=>reopened.aim({mode:'true_aim',maneuverMethod:'strain'}),/uma vez por rodada/);
 assert.equal(before.playerTurn.trueAimUsed,undefined);
 reopened.advancePlayerTurn();
 assert.equal(reopened.state.combat.playerTurn.trueAimUsed,undefined);
 assert.equal(reopened.aim({mode:'true_aim'}).combat.playerTurn.trueAimUsed,true);
});
test('P137 TRUEAIM rejects unowned talent and melee attacks without committing illegal effects',()=>{
 const c=create();c.weapons=[{id:'p137-knife',name:'Faca',skill:'armas_cuerpo_a_cuerpo',damage:4,crit:3,range:'Interação',encumbrance:0,qualities:[],active:true,equipped:true}];
 const {g}=game(c);g.beginCombat();const snapshot=structuredClone(g.state);
 assert.throws(()=>g.aim({mode:'true_aim'}),/não foi adquirida/);assert.deepEqual(g.state,snapshot);
 const owned=buy('ace','gunner','DISPARO CERTERO').character;owned.weapons=c.weapons;
 const other=game(owned).g;other.beginCombat();other.aim({mode:'true_aim'});
 assert.throws(()=>other.resolveWeaponAttack({weaponIndex:0,targetRange:'Interação',rng:()=>0.4}),/arma de ataque à distância/);
});
test('P137 five adventures still restore canonical UAE state with new AoR rules loaded',()=>{
 const catalog=publishedAdventureCatalog();assert.equal(catalog.length,5);
 for(const {id} of catalog){
  const adapter=getPublishedAdventureAdapter(id),world=adapter.start({}).world;
  const module=publishedAdventureRuntimeModule(adapter.meta);
  const runtime=AdventureRuntime.restore(module,world.publishedAdventureRuntime);
  runtime.transaction(r=>{r.dispatchEvent('P137_TURN',{source:'regression'});r.updateObjectiveProgress('episode-1',0.25);});
  const checkpoint=JSON.parse(runtime.serialize());
  assert.equal(validatePublishedAdventureRuntimeState(module,checkpoint),true);
  assert.equal(AdventureRuntime.restore(module,checkpoint).serialize(),runtime.serialize());
 }
});
test('P137 Android UI offers true aim and keeps Kokoro untouched',()=>{
 const runtime=readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');
 assert.match(runtime,/value="true_aim"/);assert.match(runtime,/mode:p\.querySelector\('\[data-p104-aim-mode\]'\)/);
 assert.match(runtime,/securityBypass:true/);
});
