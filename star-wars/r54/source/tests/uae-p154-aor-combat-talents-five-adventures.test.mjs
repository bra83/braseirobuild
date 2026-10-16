/** P154 — five newly active AoR source slots, same-budget combat, rollback,
 * persisted reload and five published-adventure smoke gates. */
import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign,validateState} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {createAdversary} from '../app/src/main/assets/vtt/js/rules/adversaries.mjs';
import {statusModifiers} from '../app/src/main/assets/vtt/js/rules/statusEffects.mjs';
import {AOR_TALENT_TREES} from '../app/src/main/assets/vtt/js/expansion/aorTalentTreesData.mjs';
import {aorTalentCatalog,aorTalentCoverage} from '../app/src/main/assets/vtt/js/expansion/aorProgressionIntegration.mjs';
import {aorCanonicalTalentName} from '../app/src/main/assets/vtt/js/expansion/aorTalentRuntimeAliases.mjs';
import {talentRankByOriginal,powerfulBlastDamageBonus,strongArmRangeBonus,knockdownTalentOption} from '../app/src/main/assets/vtt/js/rules/talents.mjs';
import {publishedAdventureCatalog,getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {publishedAdventureRuntimeModule} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';
import {validatePublishedAdventureGraph} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureGraphGate.mjs';
import {validateAdventureSemantics} from '../app/src/main/assets/vtt/js/adventure/adventureSemanticGate.mjs';

const nodes=Object.values(AOR_TALENT_TREES).flatMap(x=>Object.values(x).flatMap(t=>t.nodes));
const copy=structuredClone;
class Repo {constructor(id='p154'){this.saved=createCampaign(id);this.writes=0;this.fail=false;}load(){return copy(this.saved);}save(s){if(this.fail)throw Error('P154 storage failure');this.saved=copy(s);this.writes++;}}
function fixture(names=[],id='p154'){
 const repo=new Repo(id),g=new GameController(repo);g.loadStarterCharacter('tendaar');
 const c=copy(g.state.player.character);c.creation={...c.creation,rulesProfile:'AOR_CORE'};
 c.talents=[];c.talentPurchases=[];
 for(const [name,count] of names){const purchased=nodes.filter(x=>x.originalName===name).slice(0,count);assert.equal(purchased.length,count,name);
  for(const n of purchased){c.talents.push(n.name);c.talentPurchases.push({nodeId:n.id,originalName:n.originalName,name:n.name,source:n.source});}}
 g.setCharacter(c);return {g,repo};
}
function encounter(names,targets=2){const x=fixture(names);const enemies=Array.from({length:targets},(_,i)=>createAdversary('imperial_officer',{name:`Alvo ${i}`,range:'Interação'}));x.g.beginEncounter({adversaries:enemies,rng:()=>0});return {...x,enemies};}
function pending({g,repo},target,{quality='blast',rank=6,hit=true,advantage=4,triumph=1,skillKey='pelea',netSuccess=1}={}){
 g.state.combat.pendingPlayerSpend={attackId:'p154-test-attack',adversaryId:target.id,weaponIndex:0,weapon:'P154',
  remainingAdvantage:advantage,remainingTriumph:triumph,qualities:[{key:quality,label:quality,rank,cost:quality==='blast'&&!hit?3:2}],
  activations:{},attackSnapshot:{hit,skillKey,damage:7,pierce:0,damageType:'wounds',baseDamage:7,netSuccess}};
 repo.save(g.state);
}
test('P154: 5 AoR source slots have real consumers and other incomplete slots remain fail-closed',()=>{
 const cov=aorTalentCoverage();assert.equal(cov.positions,360);assert.ok(cov.mechanicalPositions>=252);
 for(const [original,canonical,count] of [['EXPLOSIÓN POTENTE','POWERBLAST',2],['BRAZO FUERTE','STRONGARM',1],['DERRIBO','KNOCKDOWN_TALENT',2]]){
  assert.equal(aorCanonicalTalentName(original),canonical);
  assert.equal(aorTalentCatalog().filter(n=>n.originalName===original&&n.mechanicallyActive).length,count);
  assert.equal(talentRankByOriginal(fixture([[original,count]]).g.state.player.character,canonical),count);
 }
 assert.equal(aorTalentCatalog().filter(n=>n.originalName==='TENACIDAD'&&n.mechanicallyActive).length,2); // P170
});
test('P154: Powerful Blast ranks are contextual; only activated Blast gets bonus',()=>{
 const c=fixture([['EXPLOSIÓN POTENTE',2]]).g.state.player.character;
 assert.equal(powerfulBlastDamageBonus(c,{weapon:'P154'}),2);
 assert.equal(powerfulBlastDamageBonus(c),0);
 assert.equal(powerfulBlastDamageBonus(fixture().g.state.player.character,{weapon:'P154'}),0);
});
for(const hit of [true,false]) test(`P154: Blast ${hit?'hit':'miss'} gets two bonus damage, once, with persisted shared Advantage`,()=>{
 const x=encounter([['EXPLOSIÓN POTENTE',2]]),[primary,secondary]=x.enemies;
 pending(x,primary,{hit,advantage:hit?2:3,netSuccess:hit?1:0});
 const r=x.g.activatePendingWeaponQuality({qualityKey:'blast',secondaryTargetIds:hit?[secondary.id]:[]});
 assert.equal(r.effect.talentBlastBonus,2);assert.equal(r.effect.damage,hit?9:8);assert.equal(r.remainingAdvantage,0);
 assert.equal(x.repo.writes>=1,true);assert.equal(validateState(x.repo.saved),true);
 const reopened=new GameController(x.repo);assert.equal(reopened.state.combat.pendingPlayerSpend.remainingAdvantage,0);
 assert.throws(()=>reopened.activatePendingWeaponQuality({qualityKey:'blast',secondaryTargetIds:[secondary.id]}),/já foi ativada|insuficientes/);
});
test('P154: Blast bonus is not applied to unrelated weapon qualities or characters without talent',()=>{
 const x=encounter(),[a,b]=x.enemies;pending(x,a,{rank:6,advantage:2});
 const r=x.g.activatePendingWeaponQuality({qualityKey:'blast',secondaryTargetIds:[b.id]});
 assert.equal(r.effect.talentBlastBonus,0);assert.equal(r.effect.damage,7);
});
test('P154: Blast activation is atomic when repository rejects a write',()=>{
 const x=encounter([['EXPLOSIÓN POTENTE',2]]),[a,b]=x.enemies;pending(x,a,{advantage:2});
 const before=copy(x.g.state),saved=copy(x.repo.saved),writes=x.repo.writes;x.repo.fail=true;
 assert.throws(()=>x.g.activatePendingWeaponQuality({qualityKey:'blast',secondaryTargetIds:[b.id]}),/storage failure/);
 assert.deepEqual(x.g.state,before);assert.deepEqual(x.repo.saved,saved);assert.equal(x.repo.writes,writes);
});
test('P154: Strong Arm only extends thrown personal ranged weapons by one band',()=>{
 const c=fixture([['BRAZO FUERTE',1]]).g.state.player.character;
 const grenade={id:'frag_grenade',name:'Granada de fragmentação',skill:'armas_distancia_ligeras',range:'Curto'};
 assert.equal(strongArmRangeBonus(c,grenade),1);
 assert.equal(strongArmRangeBonus(c,{...grenade,id:'rifle',name:'Rifle blaster'}),0);
 assert.equal(strongArmRangeBonus(c,{...grenade,skill:'artilleria'}),0);
 assert.equal(strongArmRangeBonus(fixture().g.state.player.character,grenade),0);
});
test('P154: Strong Arm is used in actual attack range validation and survives reopening',()=>{
 const x=encounter([['BRAZO FUERTE',1]],1),target=x.enemies[0];
 const c=copy(x.g.state.player.character);
 c.weapons=[{id:'frag_grenade',name:'Granada de fragmentação',skill:'armas_distancia_ligeras',damage:8,crit:4,range:'Curto',qualities:['Explosão 6','Munição limitada 1']}];
 x.g.setCharacter(c);
 const a=x.g.resolveWeaponAttack({weaponIndex:0,targetRange:'Médio',rng:()=>0.5});
 assert.equal(a.check.strongArmBands,1);assert.equal(validateState(x.repo.saved),true);
 assert.equal(new GameController(x.repo).state.player.character.weapons[0].range,'Curto');
 const y=encounter([],1);const c2=copy(y.g.state.player.character);c2.weapons=copy(c.weapons);y.g.setCharacter(c2);
 assert.throws(()=>y.g.resolveWeaponAttack({weaponIndex:0,targetRange:'Médio',rng:()=>0.5}),/além do alcance/);
});
test('P154: Knockdown talent requires melee hit and exactly one Triumph',()=>{
 const c=fixture([['DERRIBO',2]]).g.state.player.character;
 assert.equal(knockdownTalentOption(c,{hit:true,skillKey:'pelea',triumph:1}).available,true);
 assert.equal(knockdownTalentOption(c,{hit:true,skillKey:'armas_distancia_ligeras',triumph:2}).available,false);
 assert.equal(knockdownTalentOption(c,{hit:false,skillKey:'pelea',triumph:1}).available,false);
 assert.throws(()=>knockdownTalentOption(c,{hit:true,skillKey:'pelea',triumph:-1}),/inválidos/);
});
test('P154: Knockdown consumes Triumph and persists prone without spending Advantage',()=>{
 const x=encounter([['DERRIBO',2]],1),target=x.enemies[0];pending(x,target,{quality:'knockdown',advantage:2,triumph:1,skillKey:'pelea'});
 const r=x.g.activatePendingKnockdownTalent();assert.equal(r.remainingTriumph,0);
 assert.equal(x.g.state.combat.pendingPlayerSpend.remainingAdvantage,2);
 assert.equal(statusModifiers(r.target).prone,true);
 const reopened=new GameController(x.repo);assert.equal(statusModifiers(reopened.state.combat.adversaries[0]).prone,true);
 assert.throws(()=>reopened.activatePendingKnockdownTalent(),/já foi ativada/);
 assert.equal(reopened.state.combat.pendingPlayerSpend.remainingAdvantage,2);
});
test('P154: Knockdown rejects missing talent, miss, ranged hit and empty Triumph without writes',()=>{
 for(const [names,opts] of [ [[],{}],[[['DERRIBO',1]],{hit:false}],[[['DERRIBO',1]],{skillKey:'armas_distancia_ligeras'}],[[['DERRIBO',1]],{triumph:0}] ]){
  const x=encounter(names,1);pending(x,x.enemies[0],opts);
  const saved=copy(x.repo.saved),writes=x.repo.writes;
  assert.throws(()=>x.g.activatePendingKnockdownTalent());assert.deepEqual(x.repo.saved,saved);assert.equal(x.repo.writes,writes);
 }
});
test('P154: Knockdown rolls back on storage failure, including status and Triumph',()=>{
 const x=encounter([['DERRIBO',1]],1);pending(x,x.enemies[0]);const before=copy(x.g.state),saved=copy(x.repo.saved);
 x.repo.fail=true;assert.throws(()=>x.g.activatePendingKnockdownTalent(),/storage failure/);
 assert.deepEqual(x.g.state,before);assert.deepEqual(x.repo.saved,saved);
});
test('P154: bridge and Android combat UI expose the actual Knockdown action',()=>{
 const bridge=readFileSync(new URL('../app/src/main/assets/vtt/js/core/uiBridge.mjs',import.meta.url),'utf8');
 const runtime=readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');
 const bundle=readFileSync(new URL('../app/src/main/assets/vtt/js/core/uiBridge.bundle.js',import.meta.url),'utf8');
 for(const text of [bridge,runtime,bundle])assert.ok(text.includes('activatePendingKnockdownTalent'));
 assert.ok(runtime.includes('data-p154-knockdown'));assert.match(bundle,/FINAL 1\.0\.(?:15[5-9]|1[6-9]\d|[2-9]\d{2,}) offline engine bundle/);
});
for(const {id} of publishedAdventureCatalog())test(`P154: ${id} start/choice + 3 reopens keep UAE graph valid`,()=>{
 const adapter=getPublishedAdventureAdapter(id),module=publishedAdventureRuntimeModule(adapter.meta);
 assert.ok(validatePublishedAdventureGraph(module));assert.ok(validateAdventureSemantics(module));
 const {g,repo}=fixture([['EXPLOSIÓN POTENTE',2],['DERRIBO',1]],`p154:${id}`);
 const actionId=`p154:${id}:start`,start=g.executePublishedAdventureCommand({actionId,kind:'start',adventureId:id});assert.equal(start.applied,true);
 const choiceId=adapter.status(repo.saved.world).node?.choices?.[0]?.id;
 assert.ok(choiceId);const move=g.executePublishedAdventureCommand({actionId:`p154:${id}:choice`,kind:'choice',adventureId:id,choiceId});assert.equal(move.applied,true);
 const before=copy(repo.saved),writes=repo.writes;
 for(let i=0;i<3;i++){
  const reopened=new GameController(repo);
  const again=reopened.executePublishedAdventureCommand({actionId,kind:'start',adventureId:id});
  assert.equal(again.duplicate,true);assert.equal(repo.writes,writes);assert.deepEqual(repo.saved,before);assert.equal(validateState(repo.saved),true);
 }
});
